/*
 * @(#)SeqTrieMap.java
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
import org.jhotdraw8.collection.champ.SequencedTrieIterator;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

/**
 * Implements a mutable map using a Compressed Hash-Array Mapped Prefix-tree
 * (CHAMP), with predictable iteration order.
 * <p>
 * Features:
 * <ul>
 *     <li>allows null keys and null values</li>
 *     <li>is mutable</li>
 *     <li>is not thread-safe</li>
 *     <li>iterates in the order, in which keys were inserted</li>
 * </ul>
 * <p>
 * Performance characteristics:
 * <ul>
 *     <li>put: O(1) amortized</li>
 *     <li>remove: O(1)</li>
 *     <li>containsKey: O(1)</li>
 *     <li>toImmutable: O(1) + O(log n) distributed across subsequent updates</li>
 *     <li>clone: O(log n) distributed across subsequent updates</li>
 *     <li>iterator.next(): O(log n)</li>
 * </ul>
 * <p>
 * Implementation details:
 * <p>
 * This map performs read and write operations of single elements in O(1) time,
 * and in O(1) space.
 * <p>
 * The CHAMP tree contains nodes that may be shared with other map, and nodes
 * that are exclusively owned by this map.
 * <p>
 * If a write operation is performed on an exclusively owned node, then this
 * map is allowed to mutate the node (mutate-on-write).
 * If a write operation is performed on a potentially shared node, then this
 * map is forced to create an exclusive copy of the node and of all not (yet)
 * exclusively owned parent nodes up to the root (copy-path-on-write).
 * Since the CHAMP tree has a fixed maximal height, the cost is O(1) in either
 * case.
 * <p>
 * This map can create an immutable copy of itself in O(1) time and O(0) space
 * using method {@link #toImmutable()}. This map loses exclusive ownership of
 * all its tree nodes.
 * Thus, creating an immutable copy increases the constant cost of
 * subsequent writes, until all shared nodes have been gradually replaced by
 * exclusively owned nodes again.
 * <p>
 * Insertion Order:
 * <p>
 * This map uses a counter to keep track of the insertion order.
 * It stores the current value of the counter in the sequence number
 * field of each data entry. If the counter wraps around, it must renumber all
 * sequence numbers.
 * <p>
 * The renumbering is why the {@code copyPut} is O(1) only in an amortized sense.
 * <p>
 * The iterator of the map is a priority queue, that orders the entries by
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
 * @param <K> the key type
 * @param <V> the value type
 */
public class SeqChampMap<K, V> extends AbstractMap<K, V> implements Serializable, Cloneable, SequencedMap<K, V> {
    private final static long serialVersionUID = 0L;
    private final static int ENTRY_LENGTH = 3;
    private transient UniqueId mutator;
    private transient BitmapIndexedNode<K, V> root;
    private transient int size;
    private transient int modCount;
    /**
     * Counter for the sequence number of the last element. The counter is
     * incremented when a new entry is added to the end of the sequence.
     * <p>
     * The counter is in the range from {@code 0} to
     * {@link Integer#MAX_VALUE} - 1.
     * When the counter reaches {@link Integer#MAX_VALUE}, all
     * sequence numbers are renumbered, and the counter is reset to
     * {@code size}.
     */
    private transient int lastSequenceNumber = 0;

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
    private int firstSequenceNumber = 0;

    public SeqChampMap() {
        this.root = BitmapIndexedNode.emptyNode();
    }

    public SeqChampMap(@NonNull Map<? extends K, ? extends V> m) {
        if (m instanceof SeqChampMap) {
            @SuppressWarnings("unchecked")
            SeqChampMap<K, V> trieMap = (SeqChampMap<K, V>) m;
            this.mutator = null;
            trieMap.mutator = null;
            this.root = trieMap.root;
            this.size = trieMap.size;
            this.modCount = 0;
        } else {
            this.root = BitmapIndexedNode.emptyNode();
            this.putAll(m);
        }
    }

    public SeqChampMap(@NonNull Iterable<? extends Entry<? extends K, ? extends V>> m) {
        this.root = BitmapIndexedNode.emptyNode();
        for (Entry<? extends K, ? extends V> e : m) {
            this.put(e.getKey(), e.getValue());
        }

    }

    public SeqChampMap(@NonNull ReadOnlyMap<? extends K, ? extends V> m) {
        if (m instanceof ImmutableSeqChampMap) {
            @SuppressWarnings("unchecked")
            ImmutableSeqChampMap<K, V> trieMap = (ImmutableSeqChampMap<K, V>) m;
            this.root = trieMap;
            this.size = trieMap.size;
        } else {
            this.root = BitmapIndexedNode.emptyNode();
            this.putAll(m.asMap());
        }
    }

    @Override
    public void clear() {
        root = BitmapIndexedNode.emptyNode();
        size = 0;
        modCount++;
        lastSequenceNumber = 0;
    }

    @Override
    public SeqChampMap<K, V> clone() {
        try {
            @SuppressWarnings("unchecked") final SeqChampMap<K, V> that = (SeqChampMap<K, V>) super.clone();
            that.mutator = null;
            this.mutator = null;
            return that;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    boolean containsEntry(final @Nullable Object o) {
        if (o instanceof Entry) {
            @SuppressWarnings("unchecked") Entry<K, V> entry = (Entry<K, V>) o;
            return containsKey(entry.getKey())
                    && Objects.equals(entry.getValue(), get(entry.getKey()));
        }
        return false;
    }

    @Override
    public boolean containsKey(final @NonNull Object o) {
        @SuppressWarnings("unchecked") final K key = (K) o;
        return root.findByKey(key, Objects.hashCode(key), 0, ENTRY_LENGTH, ENTRY_LENGTH - 1) != Node.NO_VALUE;
    }

    /**
     * Dumps the internal structure of this map in the Graphviz DOT Language.
     *
     * @return a dump of the internal structure
     */
    public String dump() {
        return new ChampTrieGraphviz<K, V>().dumpTrie(root, ENTRY_LENGTH, true, true);
    }

    @Override
    public Entry<K, V> firstEntry() {
        return new EntryIterator(false).nextEntry();
    }

    @Override
    public Entry<K, V> lastEntry() {
        return new EntryIterator(true).nextEntry();
    }

    @Override
    public V putFirst(K key, V value) {
        return putFirstAndGiveDetails(key, value).getOldValue();
    }


    @Override
    public V putLast(K key, V value) {
        return putLastAndGiveDetails(key, value).getOldValue();
    }

    @NonNull ChangeEvent<V> putLastAndGiveDetails(final K key, final V val) {
        final int keyHash = Objects.hashCode(key);
        final ChangeEvent<V> details = new ChangeEvent<>();

        final BitmapIndexedNode<K, V> newRootNode =
                root.update(getOrCreateMutator(), key, val, keyHash, 0, details, ENTRY_LENGTH, lastSequenceNumber,
                        ENTRY_LENGTH - 1);

        if (details.isModified()) {
            if (details.hasReplacedValue()) {
                root = newRootNode;
            } else {
                root = newRootNode;
                size += 1;
                lastSequenceNumber++;
                if (lastSequenceNumber == Node.NO_SEQUENCE_NUMBER) {
                    renumberSequenceNumbers();
                }
                modCount++;
            }
        }

        return details;
    }

    @NonNull ChangeEvent<V> putFirstAndGiveDetails(final K key, final V val) {
        final int keyHash = Objects.hashCode(key);
        final ChangeEvent<V> details = new ChangeEvent<>();

        final BitmapIndexedNode<K, V> newRootNode =
                root.update(getOrCreateMutator(), key, val, keyHash, 0, details, ENTRY_LENGTH, firstSequenceNumber - 1,
                        ENTRY_LENGTH - 1);

        if (details.isModified()) {
            if (details.hasReplacedValue()) {
                root = newRootNode;
            } else {
                root = newRootNode;
                size += 1;
                firstSequenceNumber--;
                if (firstSequenceNumber == Node.NO_SEQUENCE_NUMBER) {
                    renumberSequenceNumbers();
                }
                modCount++;
            }
        }

        return details;
    }

    @Override
    @SuppressWarnings("unchecked")
    public SequencedSet<K> keySet() {
        return new WrappedSequencedSet<>(
                SeqChampMap.KeyIterator::new,
                SeqChampMap.this::size,
                SeqChampMap.this::containsKey,
                SeqChampMap.this::clear,
                SeqChampMap.this::removeKey,
                SeqChampMap.this::firstKey,
                SeqChampMap.this::lastKey
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public SequencedSet<Entry<K, V>> entrySet() {
        return new WrappedSequencedSet<>(
                SeqChampMap.EntryIterator::new,
                SeqChampMap.this::size,
                SeqChampMap.this::containsEntry,
                SeqChampMap.this::clear,
                SeqChampMap.this::removeEntry,
                SeqChampMap.this::firstEntry,
                SeqChampMap.this::lastEntry
        );
    }

    @Override
    public SequencedCollection<V> values() {
        return new WrappedSequencedCollection<>(
                SeqChampMap.ValueIterator::new,
                SeqChampMap.this::size,
                SeqChampMap.this::containsValue,
                SeqChampMap.this::clear,
                SeqChampMap.this::removeValue,
                SeqChampMap.this::firstValue,
                SeqChampMap.this::lastValue
        );
    }

    V firstValue() {
        return firstEntry().getValue();
    }

    V lastValue() {
        return lastEntry().getValue();
    }

    @Override
    @SuppressWarnings("unchecked")
    public V get(final @NonNull Object o) {
        final K key = (K) o;
        Object result = root.findByKey(key, Objects.hashCode(key), 0, ENTRY_LENGTH, ENTRY_LENGTH - 1);
        return result == Node.NO_VALUE ? null : (V) result;
    }

    private @NonNull UniqueId getOrCreateMutator() {
        if (mutator == null) {
            mutator = new UniqueId();
        }
        return mutator;
    }

    @Override
    public V put(K key, V value) {
        return putLast(key, value);
    }

    /**
     * If the specified key is present, associates it with the
     * given value.
     *
     * @param k a key
     * @param v a value
     */
    public void putIfPresent(K k, V v) {
        if (containsKey(k)) {
            put(k, v);
        }
    }

    @Override
    public V remove(Object o) {
        @SuppressWarnings("unchecked") final K key = (K) o;
        return removeAndGiveDetails(key).getOldValue();
    }

    @NonNull ChangeEvent<V> removeAndGiveDetails(final K key) {
        final int keyHash = Objects.hashCode(key);
        final ChangeEvent<V> details = new ChangeEvent<>();
        final BitmapIndexedNode<K, V> newRootNode =
                root.remove(getOrCreateMutator(), key, keyHash, 0, details, ENTRY_LENGTH, ENTRY_LENGTH - 1);
        if (details.isModified()) {
            assert details.hasReplacedValue();
            root = newRootNode;
            size = size - 1;
            modCount++;
            if (size == 0) {
                lastSequenceNumber = Integer.MIN_VALUE;
            }
        }
        return details;
    }

    boolean removeEntry(final @Nullable Object o) {
        if (o instanceof Entry) {
            @SuppressWarnings("unchecked")
            Entry<K, V> entry = (Entry<K, V>) o;
            K key = entry.getKey();
            Object result = root.findByKey(key, Objects.hashCode(key), 0, ENTRY_LENGTH, ENTRY_LENGTH - 1);
            if (Objects.equals(result, entry.getValue())) {
                removeAndGiveDetails(key);
                return true;
            }
        }
        return false;
    }

    boolean removeKey(final @Nullable Object o) {
        @SuppressWarnings("unchecked") K key = (K) o;
        return removeAndGiveDetails(key).isModified();
    }

    boolean removeValue(final @Nullable Object o) {
        for (Entry<K, V> entry : entrySet()) {
            if (Objects.equals(entry.getValue(), o)) {
                remove(entry.getKey());
                return true;
            }
        }
        return false;
    }

    private void renumberSequenceNumbers() {
        root = ChampTrie.renumber(size, root, getOrCreateMutator(), ENTRY_LENGTH);
        lastSequenceNumber = size;
        firstSequenceNumber = 0;
    }

    @Override
    public int size() {
        return size;
    }

    /**
     * Returns an immutable copy of this map.
     *
     * @return an immutable copy
     */
    public ImmutableSeqChampMap<K, V> toImmutable() {
        if (size == 0) {
            return ImmutableSeqChampMap.of();
        }
        mutator = null;
        return new ImmutableSeqChampMap<>(root, size, lastSequenceNumber);
    }

    private abstract class AbstractMapIterator extends SequencedTrieIterator<K, V> {
        protected int expectedModCount;

        public AbstractMapIterator(boolean reversed) {
            super(SeqChampMap.this.size, SeqChampMap.this.root, ENTRY_LENGTH, reversed);
            this.expectedModCount = SeqChampMap.this.modCount;
        }

        @Override
        public boolean hasNext() {
            if (expectedModCount != SeqChampMap.this.modCount) {
                throw new ConcurrentModificationException();
            }
            return super.hasNext();
        }

        @Override
        public Entry<K, V> nextEntry() {
            if (expectedModCount != SeqChampMap.this.modCount) {
                throw new ConcurrentModificationException();
            }
            return super.nextEntry();
        }


        public void remove() {
            if (expectedModCount != SeqChampMap.this.modCount) {
                throw new ConcurrentModificationException();
            }
            removeEntry(k -> {
                SeqChampMap.this.remove(k);
                expectedModCount = SeqChampMap.this.modCount;
                return SeqChampMap.this.root;
            });
        }
    }

    private class EntryIterator extends AbstractMapIterator implements Iterator<Entry<K, V>> {
        public EntryIterator() {
            super(false);
        }

        public EntryIterator(boolean reversed) {
            super(reversed);
        }

        @Override
        public Entry<K, V> next() {
            //FIXME support mutable map entry
            return nextEntry();
        }
    }

    private class ValueIterator extends AbstractMapIterator implements Iterator<V> {
        public ValueIterator() {
            super(false);
        }

        public ValueIterator(boolean reversed) {
            super(reversed);
        }

        @Override
        public V next() {
            //FIXME support mutable map entry
            return nextEntry().getValue();
        }
    }

    private class KeyIterator extends AbstractMapIterator implements Iterator<K> {
        public KeyIterator() {
            super(false);
        }

        public KeyIterator(boolean reversed) {
            super(reversed);
        }

        @Override
        public K next() {
            return nextEntry().getKey();
        }
    }

    private static class SerializationProxy<K, V> extends MapSerializationProxy<K, V> {
        private final static long serialVersionUID = 0L;

        protected SerializationProxy(Map<K, V> target) {
            super(target);
        }

        protected Object readResolve() {
            return new SeqChampMap<>(deserialized);
        }
    }

    private Object writeReplace() {
        return new SerializationProxy<K, V>(this);
    }
}


