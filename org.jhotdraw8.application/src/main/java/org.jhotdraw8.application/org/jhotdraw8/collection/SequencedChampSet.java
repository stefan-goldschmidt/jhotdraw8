/*
 * @(#)SeqChampSet.java
 * Copyright © 2022 The authors and contributors of JHotDraw. MIT License.
 */

package org.jhotdraw8.collection;

import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.annotation.Nullable;
import org.jhotdraw8.collection.champ.BitmapIndexedNode;
import org.jhotdraw8.collection.champ.ChampTrie;
import org.jhotdraw8.collection.champ.ChampTrieGraphviz;
import org.jhotdraw8.collection.champ.ChangeEvent;
import org.jhotdraw8.collection.champ.Node;
import org.jhotdraw8.collection.champ.SequencedEntryIterator;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Implements a mutable set using a Compressed Hash-Array Mapped Prefix-tree
 * (CHAMP), with predictable iteration order.
 * <p>
 * Features:
 * <ul>
 *     <li>allows null elements</li>
 *     <li>is mutable</li>
 *     <li>is not thread-safe</li>
 *     <li>iterates in the order, in which elements were inserted</li>
 * </ul>
 * <p>
 * Performance characteristics:
 * <ul>
 *     <li>add: O(1) amortized due to renumbering</li>
 *     <li>remove: O(1)</li>
 *     <li>contains: O(1)</li>
 *     <li>toImmutable: O(1) + a constant cost distributed across subsequent updates</li>
 *     <li>clone: O(1) + a constant cost distributed across subsequent updates</li>
 *     <li>iterator.next(): O(log n)</li>
 * </ul>
 * <p>
 * Implementation details:
 * <p>
 * This set performs read and write operations of single elements in O(1) time,
 * and in O(1) space.
 * <p>
 * The CHAMP tree contains nodes that may be shared with other sets, and nodes
 * that are exclusively owned by this set.
 * <p>
 * If a write operation is performed on an exclusively owned node, then this
 * set is allowed to mutate the node (mutate-on-write).
 * If a write operation is performed on a potentially shared node, then this
 * set is forced to create an exclusive copy of the node and of all not (yet)
 * exclusively owned parent nodes up to the root (copy-path-on-write).
 * Since the CHAMP tree has a fixed maximal height, the cost is O(1) in either
 * case.
 * <p>
 * This set can create an immutable copy of itself in O(1) time and O(0) space
 * using method {@link #toImmutable()}. This set loses exclusive ownership of
 * all its tree nodes.
 * Thus, creating an immutable copy increases the constant cost of
 * subsequent writes, until all shared nodes have been gradually replaced by
 * exclusively owned nodes again.
 * Calculation of the constant cost that is distributed over subsequent
 * updates: Assuming that there are no hash collisions, and the trie is fully
 * populated with {@code 2^32} elements, then there are
 * {@code ((32^7)−1)÷(32-1) = 1,108,378,657} nodes, that need to be owned again.
 *
 * <p>
 * <strong>Note that this implementation is not synchronized.</strong>
 * If multiple threads access this set concurrently, and at least
 * one of the threads modifies the set, it <em>must</em> be synchronized
 * externally.  This is typically accomplished by synchronizing on some
 * object that naturally encapsulates the set.
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
public class SequencedChampSet<E> extends AbstractSet<E> implements Serializable, Cloneable, SequencedSet<E> {
    private final static long serialVersionUID = 0L;
    private final static int ENTRY_LENGTH = 2;
    private transient @Nullable UniqueId mutator;
    private transient @Nullable BitmapIndexedNode<E, Void> root;
    private transient int size;
    private transient int modCount;

    /**
     * Counter for the sequence number of the last element. The counter is
     * incremented after a new entry is added to the end of the sequence.
     * <p>
     * The counter is in the range from {@code 0} to
     * {@link Integer#MAX_VALUE} - 1.
     * When the counter reaches {@link Integer#MAX_VALUE}, all
     * sequence numbers are renumbered, and the counter is reset to
     * {@code size}.
     */
    private int last = 0;
    /**
     * Counter for the sequence number of the first element. The counter is
     * decrement before a new entry is added to the start of the sequence.
     * <p>
     * The counter is in the range from {@code 0} to
     * {@link Integer#MIN_VALUE}.
     * When the counter is about to wrap over to {@link Integer#MAX_VALUE}, all
     * sequence numbers are renumbered, and the counter is reset to
     * {@code 0}.
     */
    private int first = 0;

    /**
     * Constructs an empty set.
     */
    public SequencedChampSet() {
        this.root = BitmapIndexedNode.emptyNode();
    }

    /**
     * Constructs a set containing the elements in the specified iterable.
     *
     * @param c an iterable
     */
    @SuppressWarnings("unchecked")
    public SequencedChampSet(Iterable<? extends E> c) {
        if (c instanceof SequencedChampSet<?>) {
            c = ((SequencedChampSet<? extends E>) c).toImmutable();
        }
        if (c instanceof ImmutableSequencedChampSet<?>) {
            ImmutableSequencedChampSet<E> that = (ImmutableSequencedChampSet<E>) c;
            this.root = that;
            this.size = that.size;
        } else {
            this.root = BitmapIndexedNode.emptyNode();
            addAll(c);
        }
    }

    @Override
    public boolean add(final @Nullable E e) {
        return addLastIfAbsent(e);
    }

    /**
     * Adds all specified elements that are not already in this set.
     *
     * @param c a collection of elements
     * @return {@code true} if this set changed
     */
    @Override
    public boolean addAll(@NonNull Collection<? extends E> c) {
        return addAll((Iterable<? extends E>) c);
    }

    /**
     * Adds all specified elements that are not already in this set.
     *
     * @param c an iterable of elements
     * @return {@code true} if this set changed
     */
    public boolean addAll(@NonNull Iterable<? extends E> c) {
        if (c == this) {
            return false;
        }

        boolean modified = false;
        for (E e : c) {
            modified |= add(e);
        }
        return modified;
    }

    @Override
    public boolean addFirst(E e) {
        boolean isPresent = remove(e);
        addFirstIfAbsent(e);
        return !isPresent;
    }

    private boolean addFirstIfAbsent(E e) {
        final ChangeEvent<Void> changeEvent = new ChangeEvent<>();
        final BitmapIndexedNode<E, Void> newRoot = root.update(getOrCreateMutator(), e, null, Objects.hashCode(e), 0, changeEvent, ENTRY_LENGTH,
                first - 1, ENTRY_LENGTH - 1);
        if (changeEvent.isModified) {
            root = newRoot;
            size++;
            modCount++;
            first--;
            if (first == Node.NO_SEQUENCE_NUMBER - 1) {
                renumber();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean addLast(E e) {
        boolean isPresent = remove(e);
        return !isPresent;
    }

    private boolean addLastIfAbsent(final @Nullable E e) {
        final ChangeEvent<Void> changeEvent = new ChangeEvent<>();
        final BitmapIndexedNode<E, Void> newRoot = root.update(
                getOrCreateMutator(), e, null, Objects.hashCode(e), 0,
                changeEvent, ENTRY_LENGTH,
                last, ENTRY_LENGTH - 1);
        if (changeEvent.isModified) {
            root = newRoot;
            size++;
            modCount++;
            last++;
            if (last == Node.NO_SEQUENCE_NUMBER) {
                renumber();
            }

            return true;
        }
        return false;
    }

    /**
     * Removes all elements from this set.
     */
    @Override
    public void clear() {
        root = BitmapIndexedNode.emptyNode();
        size = 0;
        modCount++;
    }

    /**
     * Returns a shallow copy of this set.
     */
    @Override
    public @NonNull SequencedChampSet<E> clone() {
        try {
            @SuppressWarnings("unchecked") final SequencedChampSet<E> that = (SequencedChampSet<E>) super.clone();
            that.mutator = null;
            this.mutator = null;
            return that;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean contains(@Nullable final Object o) {
        @SuppressWarnings("unchecked") final E key = (E) o;
        return root.findByKey(key, Objects.hashCode(key), 0, ENTRY_LENGTH,
                ENTRY_LENGTH - 1) != Node.NO_VALUE;
    }

    /**
     * Dumps the internal structure of this set in the Graphviz DOT Language.
     *
     * @return a dump of the internal structure
     */
    public @NonNull String dump() {
        return new ChampTrieGraphviz<E, Void>().dumpTrie(root, ENTRY_LENGTH, false, false);
    }

    /**
     * Gets an element from this set. Throws an exception if the set is empty.
     *
     * @return an element
     * @throws java.util.NoSuchElementException if this set is empty
     */
    public E element() {
        return iterator().next();
    }

    @Override
    public E getLast() {
        return iterator(true).next();
    }

    private @NonNull UniqueId getOrCreateMutator() {
        if (mutator == null) {
            mutator = new UniqueId();
        }
        return mutator;
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
    public @Nullable Iterator<E> iterator(boolean reversed) {
        return new FailFastIterator<>(new MappedIterator<>(new SequencedEntryIterator<>(
                size, root, ENTRY_LENGTH, ENTRY_LENGTH - 1, reversed,
                this::persistentRemove, null),
                Map.Entry::getKey), () -> SequencedChampSet.this.modCount);
    }

    private void persistentRemove(E e) {
        mutator = null;
        remove(e);
    }

    /**
     * Removes the specified element if it is in this set.
     *
     * @param o an element
     * @return {@code true} if this set changed
     */
    @Override
    public boolean remove(final Object o) {
        @SuppressWarnings("unchecked")
        E key = (E) o;
        final ChangeEvent<Void> changeEvent = new ChangeEvent<>();
        final BitmapIndexedNode<E, Void> newRoot = root.remove(
                getOrCreateMutator(), key, Objects.hashCode(key), 0,
                changeEvent, ENTRY_LENGTH, ENTRY_LENGTH - 1);
        if (changeEvent.isModified) {
            root = newRoot;
            size--;
            modCount++;
            return true;
        }
        return false;
    }

    /**
     * Removes an element from this set and returns it.
     * Throws an exception if the set is empty.
     *
     * @return an element
     * @throws java.util.NoSuchElementException if this set is empty
     */
    public E remove() {
        Iterator<E> iterator = iterator();
        E e = iterator.next();
        iterator.remove();
        return e;
    }

    /**
     * Removes all specified elements that are in this set.
     *
     * @param c a collection of elements
     * @return {@code true} if this set changed
     */
    @Override
    public boolean removeAll(@NonNull Collection<?> c) {
        return removeAll((Iterable<?>) c);
    }

    /**
     * Removes all specified elements that are in this set.
     *
     * @param c an iterable of elements
     * @return {@code true} if this set changed
     */
    public boolean removeAll(@NonNull Iterable<?> c) {
        if (isEmpty()) {
            return false;
        }
        if (c == this) {
            clear();
            return true;
        }
        boolean modified = false;
        for (Object o : c) {
            modified |= remove(o);
        }
        return modified;
    }

    @Override
    public E removeLast() {
        Iterator<E> iterator = iterator(true);
        E e = iterator.next();
        iterator.remove();
        return e;
    }

    private void renumber() {
        root = ChampTrie.renumber(size, root, getOrCreateMutator(), ENTRY_LENGTH);
        last = size;
        first = 0;
    }

    @Override
    public int size() {
        return size;
    }

    /**
     * Returns an immutable copy of this set.
     *
     * @return an immutable copy
     */
    public @Nullable ImmutableSequencedChampSet<E> toImmutable() {
        mutator = null;
        return size == 0 ? ImmutableSequencedChampSet.of() : new ImmutableSequencedChampSet<>(root, size, first, last);
    }

    private @NonNull Object writeReplace() {
        return new SerializationProxy<E>(this);
    }

    private static class SerializationProxy<E> extends SetSerializationProxy<E> {
        private final static long serialVersionUID = 0L;

        protected SerializationProxy(Set<E> target) {
            super(target);
        }

        @Override
        protected @NonNull Object readResolve() {
            return new SequencedChampSet<>(deserialized);
        }
    }
}