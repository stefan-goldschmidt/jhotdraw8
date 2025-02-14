/*
 * @(#)GloballyArbitraryIndexedVertexPathSearchAlgo.java
 * Copyright © 2022 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.graph.path.algo;

import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.annotation.Nullable;
import org.jhotdraw8.collection.function.AddToIntSet;
import org.jhotdraw8.collection.primitive.GrowableIntSet8Bit;
import org.jhotdraw8.graph.path.backlink.IndexedVertexBackLink;
import org.jhotdraw8.graph.path.backlink.IndexedVertexBackLinkWithCost;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Spliterator;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;


/**
 * See {@link GloballyArbitraryArcPathSearchAlgo} for a description of this
 * algorithm.
 * <p>
 * This implementation is optimized for {@link org.jhotdraw8.graph.IndexedDirectedGraph}.
 *
 * @param <C> the cost number type
 */
public class GloballyArbitraryIndexedVertexPathSearchAlgo<C extends Number & Comparable<C>>
        implements IndexedVertexPathSearchAlgo<C> {
    public GloballyArbitraryIndexedVertexPathSearchAlgo() {
    }

    private static class MyIntConsumer implements IntConsumer {
        int value;

        @Override
        public void accept(int value) {
            this.value = value;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param startVertices        the set of start vertices
     * @param goalPredicate        the goal predicate
     * @param nextVerticesFunction the next vertices function
     * @param maxDepth             the maximal depth (inclusive) of the search
     *                             Must be {@literal >= 0}.
     * @param zero                 the zero cost value
     * @param costLimit            the cost limit is <b>ignored</b>
     * @param costFunction         the cost function
     * @param sumFunction          the sum function for adding two cost values
     * @param visited
     * @return
     */
    @Override
    public @Nullable IndexedVertexBackLinkWithCost<C> search(
            @NonNull Iterable<Integer> startVertices,
            @NonNull IntPredicate goalPredicate,
            @NonNull Function<Integer, Spliterator.OfInt> nextVerticesFunction,
            int maxDepth, @NonNull C zero, @NonNull C costLimit,
            @NonNull BiFunction<Integer, Integer, C> costFunction,
            @NonNull BiFunction<C, C, C> sumFunction, @NonNull AddToIntSet visited) {
        AlgoArguments.checkZero(zero);
        return IndexedVertexBackLink.toIndexedVertexBackLinkWithCost(
                search(startVertices, goalPredicate, nextVerticesFunction,
                        new GrowableIntSet8Bit()::addAsInt, maxDepth),
                zero, costFunction, sumFunction
        );
    }

    /**
     * Search engine method.
     *
     * @param startVertices        the set of start vertices
     * @param goalPredicate        the goal predicate
     * @param nextVerticesFunction the next vertices function
     * @param visited              the set of visited vertices (see {@link AddToIntSet})
     * @param maxDepth             the maximal depth (inclusive) of the search.
     * @return
     * @return on success: a back link, otherwise: null
     */
    public @Nullable IndexedVertexBackLink search(@NonNull Iterable<Integer> startVertices,
                                                  @NonNull IntPredicate goalPredicate,
                                                  @NonNull Function<Integer, Spliterator.OfInt> nextVerticesFunction,
                                                  @NonNull AddToIntSet visited,
                                                  int maxDepth) {
        AlgoArguments.checkMaxDepth(maxDepth);

        Queue<IndexedVertexBackLink> queue = new ArrayDeque<>(32);
        MyIntConsumer consumer = new MyIntConsumer();
        for (Integer s : startVertices) {
            if (visited.addAsInt(s)) {
                queue.add(new IndexedVertexBackLink(s, null));
            }
        }

        while (!queue.isEmpty()) {
            IndexedVertexBackLink u = queue.remove();
            int vertex = u.getVertex();
            if (goalPredicate.test(vertex)) {
                return u;
            }

            if (u.getDepth() < maxDepth) {
                Spliterator.OfInt spliterator = nextVerticesFunction.apply(vertex);
                while (spliterator.tryAdvance(consumer)) {
                    if (visited.addAsInt(consumer.value)) {
                        queue.add(new IndexedVertexBackLink(consumer.value, u));
                    }
                }
            }
        }

        return null;
    }
}
