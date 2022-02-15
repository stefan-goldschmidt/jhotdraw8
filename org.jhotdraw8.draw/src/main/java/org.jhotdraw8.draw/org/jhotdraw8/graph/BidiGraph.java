/*
 * @(#)BidiGraph.java
 * Copyright © 2021 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.graph;

import org.jhotdraw8.annotation.NonNull;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

/**
 * Adds convenience methods to the API defined in {@link BareBidiGraph}.
 *
 * @param <V> the vertex data type
 * @param <A> the arrow data type
 * @author Werner Randelshofer
 */
public interface BidiGraph<V, A> extends DirectedGraph<V, A>, BareBidiGraph<V, A> {

    /**
     * Returns the direct predecessor arrows of the specified vertex.
     *
     * @param vertex a vertex
     * @return a collection view on the direct predecessor arrows of vertex
     */
    default @NonNull Collection<A> getPrevArrows(@NonNull V vertex) {
        class PrevArrowIterator implements Iterator<A> {

            private int index;
            private final @NonNull V vertex;
            private final int prevCount;

            public PrevArrowIterator(@NonNull V vertex) {
                this.vertex = vertex;
                this.prevCount = getPrevCount(vertex);
            }

            @Override
            public boolean hasNext() {
                return index < prevCount;
            }

            @Override
            public @NonNull A next() {
                return getPrevArrow(vertex, index++);
            }
        }

        return new AbstractCollection<A>() {
            @Override
            public @NonNull Iterator<A> iterator() {
                return new PrevArrowIterator(vertex);
            }

            @Override
            public int size() {
                return getPrevCount(vertex);
            }
        };
    }

    /**
     * Returns the direct predecessor vertices of the specified vertex.
     *
     * @param vertex a vertex
     * @return a collection view on the direct predecessor vertices of vertex
     */
    default @NonNull Collection<V> getPrevVertices(@NonNull V vertex) {
        class PrevVertexIterator implements Iterator<V> {

            private int index;
            private final @NonNull V vertex;
            private final int prevCount;

            public PrevVertexIterator(@NonNull V vertex) {
                this.vertex = vertex;
                this.prevCount = getPrevCount(vertex);
            }

            @Override
            public boolean hasNext() {
                return index < prevCount;
            }

            @Override
            public @NonNull V next() {
                return getPrev(vertex, index++);
            }

        }
        return new AbstractCollection<V>() {
            @Override
            public @NonNull Iterator<V> iterator() {
                return new PrevVertexIterator(vertex);
            }

            @Override
            public int size() {
                return getPrevCount(vertex);
            }
        };
    }

    /**
     * Returns the index of vertex b in the list of previous vertices
     * of vertex a.
     *
     * @param a a vertex
     * @param b another vertex
     * @return previouis index of vertex b. Returns -1 if b is not next index of a.
     */
    default int findIndexOfPrev(final V a, final @NonNull V b) {
        for (int i = 0, n = getPrevCount(a); i < n; i++) {
            if (b.equals(getPrev(a, i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns true if b is a previous vertex of a.
     *
     * @param a a vertex
     * @param b another vertex
     * @return true if b is next of a.
     */
    default boolean isPrev(final V a, final @NonNull V b) {
        return findIndexOfPrev(a, b) != -1;
    }

}
