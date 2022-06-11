/*
 * @(#)HashCollisionNode.java
 * Copyright © 2022 The authors and contributors of JHotDraw. MIT License.
 */

package org.jhotdraw8.collection.champset;

import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.annotation.Nullable;
import org.jhotdraw8.collection.ArrayHelper;
import org.jhotdraw8.collection.UniqueId;

import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Represents a hash-collision node in a CHAMP trie.
 *
 * @param <K> the key type
 */
class HashCollisionNode<K> extends Node<K> {
    private final int hash;
    @NonNull Object[] keys;

    HashCollisionNode(final int hash, final Object @NonNull [] keys) {
        this.keys = keys;
        this.hash = hash;
    }

    @Override
    int dataArity() {
        return keys.length;
    }

    @Override
    boolean hasDataArityOne() {
        return false;
    }

    @Override
    boolean equivalent(@NonNull Object other) {
        if (this == other) {
            return true;
        }
        HashCollisionNode<?> that = (HashCollisionNode<?>) other;
        @NonNull Object[] thatEntries = that.keys;
        if (hash != that.hash
                || thatEntries.length != keys.length) {
            return false;
        }

        // Linear scan for each key, because of arbitrary element order.
        @NonNull Object[] thatEntriesCloned = thatEntries.clone();
        int remainingLength = thatEntriesCloned.length;
        outerLoop:
        for (int i = 0; i < keys.length; i += 1) {
            final Object key = keys[i];
            for (int j = 0; j < remainingLength; j += 1) {
                final Object todoKey = thatEntriesCloned[j];
                if (Objects.equals(todoKey, key)) {
                    // We have found an equal entry. We do not need to compare
                    // this entry again. So we replace it with the last entry
                    // from the array and reduce the remaining length.
                    System.arraycopy(thatEntriesCloned, remainingLength - 1, thatEntriesCloned, j, 1);
                    remainingLength -= 1;

                    continue outerLoop;
                }
            }
            return false;
        }

        return true;
    }

    @Override
    @Nullable
    Object findByKey(final K key, final int keyHash, final int shift) {
        for (Object entry : keys) {
            if (Objects.equals(key, entry)) {
                return entry;
            }
        }
        return NO_VALUE;
    }

    @Override
    @SuppressWarnings("unchecked")
    @NonNull
    K getKey(final int index) {
        return (K) keys[index];
    }

    @Override
    @NonNull
    Node<K> getNode(int index) {
        throw new IllegalStateException("Is leaf node.");
    }


    @Override
    boolean hasData() {
        return true;
    }

    @Override
    boolean hasNodes() {
        return false;
    }

    @Override
    int nodeArity() {
        return 0;
    }


    @Override
    @Nullable
    Node<K> remove(final @Nullable UniqueId mutator, final K key,
                   final int keyHash, final int shift, final @NonNull ChangeEvent<K> details) {
        for (int idx = 0, i = 0; i < keys.length; i += 1, idx++) {
            if (Objects.equals(keys[i], key)) {
                @SuppressWarnings("unchecked") final K currentVal = (K) keys[i];
                details.updated(currentVal);

                if (keys.length == 1) {
                    return BitmapIndexedNode.emptyNode();
                } else if (keys.length == 2) {
                    // Create root node with singleton element.
                    // This node will be a) either be the new root
                    // returned, or b) unwrapped and inlined.
                    final Object[] theOtherEntry = {getKey(idx ^ 1)};
                    return ChampTrie.newBitmapIndexedNode(mutator, 0, bitpos(mask(keyHash, 0)), theOtherEntry);
                } else {
                    // copy keys and vals and remove entryLength elements at position idx
                    final Object[] entriesNew = ArrayHelper.copyComponentRemove(this.keys, idx, 1);
                    if (isAllowedToEdit(mutator)) {
                        this.keys = entriesNew;
                        return this;
                    }
                    return ChampTrie.newHashCollisionNode(mutator, keyHash, entriesNew);
                }
            }
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    @Nullable
    Node<K> update(final @Nullable UniqueId mutator, final K key,
                   final int keyHash, final int shift, final @NonNull ChangeEvent<K> details,
                   final @NonNull BiFunction<K, K, K> updateFunction) {
        assert this.hash == keyHash;

        for (int i = 0; i < keys.length; i++) {
            Object currentKey = keys[i];
            if (Objects.equals(currentKey, key)) {
                K updatedKey = updateFunction.apply((K) currentKey, key);
                if (updatedKey == currentKey) {
                    details.found(key);
                    return this;
                } else {
                    // copy entries and replace the entry
                    details.modified();
                    if (isAllowedToEdit(mutator)) {
                        this.keys[i] = updatedKey;
                        return this;
                    } else {
                        final Object[] newKeys = ArrayHelper.copySet(this.keys, i, updatedKey);
                        return ChampTrie.newHashCollisionNode(mutator, keyHash, newKeys);
                    }
                }
            }
        }

        // copy entries and add 1 more at the end
        final Object[] entriesNew = ArrayHelper.copyComponentAdd(this.keys, this.keys.length, 1);
        entriesNew[this.keys.length] = key;
        details.modified();
        if (isAllowedToEdit(mutator)) {
            this.keys = entriesNew;
            return this;
        } else {
            return ChampTrie.newHashCollisionNode(mutator, keyHash, entriesNew);
        }
    }
}