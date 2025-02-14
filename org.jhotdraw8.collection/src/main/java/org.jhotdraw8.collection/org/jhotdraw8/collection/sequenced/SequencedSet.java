/*
 * @(#)SequencedSet.java
 * Copyright © 2022 The authors and contributors of JHotDraw. MIT License.
 */

package org.jhotdraw8.collection.sequenced;

import java.util.Set;

/**
 * Interface for a set with a well-defined iteration order.
 * <p>
 * References:
 * <dl>
 *     <dt>JEP draft: Sequenced Collections</dt>
 *     <dd><a href="https://openjdk.java.net/jeps/8280836">java.ne</a></dd>
 * </dl>
 *
 * @param <E> the element type
 */
public interface SequencedSet<E> extends Set<E>, SequencedCollection<E> {
    @Override
    SequencedSet<E> reversed();
}
