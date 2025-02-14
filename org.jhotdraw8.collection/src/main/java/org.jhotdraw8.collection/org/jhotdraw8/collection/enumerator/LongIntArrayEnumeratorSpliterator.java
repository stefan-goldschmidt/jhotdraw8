/*
 * @(#)LongIntArrayEnumerator.java
 * Copyright © 2022 The authors and contributors of JHotDraw. MIT License.
 */

package org.jhotdraw8.collection.enumerator;

import org.jhotdraw8.annotation.Nullable;

/**
 * A {@link LongEnumeratorSpliterator} over a {@code int}-array.
 * Supports shifting and masking of the {@code int}-values.
 *
 * @author Werner Randelshofer
 */
public class LongIntArrayEnumeratorSpliterator extends AbstractIntEnumeratorSpliterator {
    private final int limit;
    private final long[] a;
    private int index;
    private final int shift;
    private final long mask;

    public LongIntArrayEnumeratorSpliterator(long[] a, int from, int to, int shift, long mask) {
        super(to - from, ORDERED | NONNULL | SIZED | SUBSIZED);
        limit = to;
        index = from;
        this.a = a;
        this.shift = shift;
        this.mask = mask;
    }

    private int toIntValue(long longValue) {
        return (int) ((longValue >>> shift) & mask);
    }

    @Override
    public boolean moveNext() {
        if (index < limit) {
            current = toIntValue(a[index++]);
            return true;
        }
        return false;
    }

    @Override
    public @Nullable LongIntArrayEnumeratorSpliterator trySplit() {
        int lo = index, mid = (lo + limit) >>> 1;
        return (lo >= mid) ? null : // divide range in half unless too small
                new LongIntArrayEnumeratorSpliterator(a, lo, index = mid, shift, mask);
    }

}
