/*
 * @(#)SequencedChampSet.java
 * Copyright © 2022 The authors and contributors of JHotDraw. MIT License.
 */

package org.jhotdraw8.collection.champ;

import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.annotation.Nullable;
import org.jhotdraw8.collection.FailFastIterator;
import org.jhotdraw8.collection.facade.ReadOnlySequencedSetFacadeFacade;
import org.jhotdraw8.collection.facade.SequencedSetFacadeFacade;
import org.jhotdraw8.collection.readonly.ReadOnlySequencedSet;
import org.jhotdraw8.collection.sequenced.SequencedSet;
import org.jhotdraw8.collection.serialization.SetSerializationProxy;

import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * Implements a mutable set using a Compressed Hash-Array Mapped Prefix-tree
 * (CHAMP), with predictable iteration order.
 * <p>
 * Features:
 * <ul>
 *     <li>supports up to 2<sup>30</sup> elements</li>
 *     <li>allows null elements</li>
 *     <li>is mutable</li>
 *     <li>is not thread-safe</li>
 *     <li>iterates in the order, in which elements were inserted</li>
 * </ul>
 * <p>
 * Performance characteristics:
 * <ul>
 *     <li>add: O(1) amortized</li>
 *     <li>remove: O(1)</li>
 *     <li>contains: O(1)</li>
 *     <li>toImmutable: O(1) + O(log N) distributed across subsequent updates in
 *     this set</li>
 *     <li>clone: O(1) + O(log N) distributed across subsequent updates in this
 *     set and in the clone</li>
 *     <li>iterator creation: O(N)</li>
 *     <li>iterator.next: O(1) with bucket sort, O(log N) with heap sort</li>
 *     <li>getFirst, getLast: O(N)</li>
 * </ul>
 * <p>
 * Implementation details:
 * <p>
 * This set performs read and write operations of single elements in O(1) time,
 * and in O(1) space.
 * <p>
 * The CHAMP trie contains nodes that may be shared with other sets, and nodes
 * that are exclusively owned by this set.
 * <p>
 * If a write operation is performed on an exclusively owned node, then this
 * set is allowed to mutate the node (mutate-on-write).
 * If a write operation is performed on a potentially shared node, then this
 * set is forced to create an exclusive copy of the node and of all not (yet)
 * exclusively owned parent nodes up to the root (copy-path-on-write).
 * Since the CHAMP trie has a fixed maximal height, the cost is O(1) in either
 * case.
 * <p>
 * This set can create an immutable copy of itself in O(1) time and O(1) space
 * using method {@link #toImmutable()}. This set loses exclusive ownership of
 * all its tree nodes.
 * Thus, creating an immutable copy increases the constant cost of
 * subsequent writes, until all shared nodes have been gradually replaced by
 * exclusively owned nodes again.
 * <p>
 * Insertion Order:
 * <p>
 * This set uses a counter to keep track of the insertion order.
 * It stores the current value of the counter in the sequence number
 * field of each data entry. If the counter wraps around, it must renumber all
 * sequence numbers.
 * <p>
 * The renumbering is why the {@code add} is O(1) only in an amortized sense.
 * <p>
 * The iterator of the set is a priority queue, that orders the entries by
 * their stored insertion counter value. This is why {@code iterator.next()}
 * is O(log n).
 * <p>
 * <strong>Note that this implementation is not synchronized.</strong>
 * If multiple threads access this set concurrently, and at least
 * one of the threads modifies the set, it <em>must</em> be synchronized
 * externally.  This is typically accomplished by synchronizing on some
 * object that naturally encapsulates the set.
 * <p>
 * References:
 * <dl>
 *      <dt>Michael J. Steindorfer (2017).
 *      Efficient Immutable Collections.</dt>
 *      <dd><a href="https://michael.steindorfer.name/publications/phd-thesis-efficient-immutable-collections">michael.steindorfer.name</a>
 *
 *      <dt>The Capsule Hash Trie Collections Library.
 *      <br>Copyright (c) Michael Steindorfer. BSD-2-Clause License</dt>
 *      <dd><a href="https://github.com/usethesource/capsule">github.com</a>
 * </dl>
 *
 * @param <E> the element type
 */
@SuppressWarnings("exports")
public class ChampSequencedSet<E> extends AbstractChampSet<E, SequencedElement<E>> implements ReadOnlySequencedSet<E>,
        SequencedSet<E> {
    private final static long serialVersionUID = 0L;

    /**
     * Counter for the sequence number of the last element. The counter is
     * incremented after a new entry is added to the end of the sequence.
     */
    private int last = 0;
    /**
     * Counter for the sequence number of the first element. The counter is
     * decrement before a new entry is added to the start of the sequence.
     */
    private int first = -1;

    /**
     * Constructs an empty set.
     */
    public ChampSequencedSet() {
        root = BitmapIndexedNode.emptyNode();
    }

    /**
     * Constructs a set containing the elements in the specified
     * {@link Iterable}.
     *
     * @param c an iterable
     */
    @SuppressWarnings("unchecked")
    public ChampSequencedSet(Iterable<? extends E> c) {
        if (c instanceof ChampSequencedSet<?>) {
            c = ((ChampSequencedSet<? extends E>) c).toImmutable();
        }
        if (c instanceof ChampImmutableSequencedSet<?>) {
            ChampImmutableSequencedSet<E> that = (ChampImmutableSequencedSet<E>) c;
            this.root = that;
            this.size = that.size;
            this.first = that.first;
            this.last = that.last;
        } else {
            this.root = BitmapIndexedNode.emptyNode();
            addAll(c);
        }
    }

    @Override
    public boolean add(@Nullable E e) {
        return addLast(e, false);
    }

    @Override
    public void addFirst(@Nullable E e) {
        addFirst(e, true);
    }

    private boolean addFirst(@Nullable E e, boolean moveToFirst) {
        ChangeEvent<SequencedElement<E>> details = new ChangeEvent<>();
        root = root.update(getOrCreateMutator(), new SequencedElement<>(e, first - 1),
                Objects.hashCode(e), 0, details,
                moveToFirst ? getUpdateAndMoveToFirstFunction() : getUpdateFunction(),
                Objects::equals, Objects::hashCode);
        if (details.isModified()) {
            if (details.isUpdated()) {
                first = details.getData().getSequenceNumber() == first ? first : first - 1;
                last = details.getData().getSequenceNumber() == last ? last - 1 : last;
            } else {
                modCount++;
                first--;
                size++;
            }
            renumber();
        }
        return details.isModified();
    }

    @Override
    public void addLast(@Nullable E e) {
        addLast(e, true);
    }

    private boolean addLast(@Nullable E e, boolean moveToLast) {
        ChangeEvent<SequencedElement<E>> details = new ChangeEvent<>();
        root = root.update(
                getOrCreateMutator(), new SequencedElement<>(e, last), Objects.hashCode(e), 0,
                details,
                moveToLast ? getUpdateAndMoveToLastFunction() : getUpdateFunction(),
                Objects::equals, Objects::hashCode);
        if (details.isModified()) {
            if (details.isUpdated()) {
                first = details.getData().getSequenceNumber() == first - 1 ? first - 1 : first;
                last = details.getData().getSequenceNumber() == last ? last : last + 1;
            } else {
                modCount++;
                size++;
                last++;
            }
            renumber();
        }
        return details.isModified();
    }

    @Override
    public void clear() {
        root = BitmapIndexedNode.emptyNode();
        size = 0;
        modCount++;
        first = -1;
        last = 0;
    }

    /**
     * Returns a shallow copy of this set.
     */
    @Override
    public @NonNull ChampSequencedSet<E> clone() {
        return (ChampSequencedSet<E>) super.clone();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(@Nullable final Object o) {
        return Node.NO_DATA != root.find(new SequencedElement<>((E) o),
                Objects.hashCode((E) o), 0, Objects::equals);
    }

    @Override
    public E getFirst() {
        return HeapSequencedIterator.getFirst(root, first, last).getElement();
    }

    @Override
    public E getLast() {
        return HeapSequencedIterator.getLast(root, first, last).getElement();
    }

    @NonNull
    private BiFunction<SequencedElement<E>, SequencedElement<E>, SequencedElement<E>> getUpdateAndMoveToFirstFunction() {
        return (oldK, newK) -> oldK.getSequenceNumber() == newK.getSequenceNumber() + 1 ? oldK : newK;
    }

    @NonNull
    private BiFunction<SequencedElement<E>, SequencedElement<E>, SequencedElement<E>> getUpdateAndMoveToLastFunction() {
        return (oldK, newK) -> oldK.getSequenceNumber() == newK.getSequenceNumber() - 1 ? oldK : newK;
    }

    @NonNull
    private BiFunction<SequencedElement<E>, SequencedElement<E>, SequencedElement<E>> getUpdateFunction() {
        return (oldK, newK) -> oldK;
    }

    @Override
    public @NonNull Iterator<E> iterator() {
        return iterator(false);
    }

    /**
     * Returns an iterator over the elements of this set, that optionally
     * iterates in reversed direction.
     *
     * @param reversed whether to iterate in reverse direction
     * @return an iterator
     */
    public @NonNull Iterator<E> iterator(boolean reversed) {
        Iterator<E> i = BucketSequencedIterator.isSupported(size, first, last)
                ? new BucketSequencedIterator<>(size, first, last, root, reversed,
                this::iteratorRemove, SequencedElement::getElement)
                : new HeapSequencedIterator<>(size, root, reversed,
                this::iteratorRemove, SequencedElement::getElement);
        return new FailFastIterator<>(i,
                () -> ChampSequencedSet.this.modCount);
    }

    private void iteratorRemove(SequencedElement<E> element) {
        remove(element.getElement());
    }

    @Override
    public @NonNull ReadOnlySequencedSet<E> readOnlyReversed() {
        return new ReadOnlySequencedSetFacadeFacade<>(reversed());
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean remove(Object o) {
        ChangeEvent<SequencedElement<E>> details = new ChangeEvent<>();
        root = root.remove(
                getOrCreateMutator(), new SequencedElement<>((E) o),
                Objects.hashCode(o), 0, details, Objects::equals);
        if (details.isModified()) {
            size--;
            modCount++;
            int seq = details.getData().getSequenceNumber();
            if (seq == last - 1) {
                last--;
            }
            if (seq == first) {
                first++;
            }
            renumber();
        }
        return details.isModified();
    }

    @Override
    public E removeFirst() {
        SequencedElement<E> k = HeapSequencedIterator.getFirst(root, first, last);
        remove(k.getElement());
        first = k.getSequenceNumber();
        renumber();
        return k.getElement();
    }

    @Override
    public E removeLast() {
        SequencedElement<E> k = HeapSequencedIterator.getLast(root, first, last);
        remove(k.getElement());
        last = k.getSequenceNumber();
        renumber();
        return k.getElement();
    }

    /**
     * Renumbers the sequence numbers if they have overflown,
     * or if the extent of the sequence numbers is more than
     * 4 times the size of the set.
     */
    private void renumber() {
        if (SequencedData.mustRenumber(size, first, last)) {
            root = SequencedElement.renumber(size, root, getOrCreateMutator(),
                    Objects::hashCode, Objects::equals);
            last = size;
            first = -1;
        }
    }

    @Override
    public SequencedSet<E> reversed() {
        return new SequencedSetFacadeFacade<>(
                () -> iterator(true),
                () -> iterator(false),
                this::size,
                this::contains,
                this::clear,
                this::remove,
                this::getLast, this::getFirst,
                e -> addFirst(e, false), this::add,
                this::addLast, this::addFirst
        );
    }

    /**
     * Returns an immutable copy of this set.
     *
     * @return an immutable copy
     */
    public @NonNull ChampImmutableSequencedSet<E> toImmutable() {
        mutator = null;
        return size == 0 ? ChampImmutableSequencedSet.of() : new ChampImmutableSequencedSet<>(root, size, first, last);
    }

    private @NonNull Object writeReplace() {
        return new SerializationProxy<>(this);
    }

    private static class SerializationProxy<E> extends SetSerializationProxy<E> {
        private final static long serialVersionUID = 0L;

        protected SerializationProxy(Set<E> target) {
            super(target);
        }

        @Override
        protected @NonNull Object readResolve() {
            return new ChampSequencedSet<>(deserialized);
        }
    }
}