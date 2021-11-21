package org.jhotdraw8.graph.path;

import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.annotation.Nullable;
import org.jhotdraw8.collection.ImmutableList;
import org.jhotdraw8.collection.ImmutableLists;
import org.jhotdraw8.collection.OrderedPair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * Defines an API for finding vertex sequences associated with a cost through a
 * directed graph.
 *
 * @param <V> the vertex data type
 * @param <C> the cost number type
 */
public interface VertexSequenceBuilder<V, C extends Number & Comparable<C>> {


    /**
     * Finds a vertex sequence from a set of start vertices to a vertex
     * that satisfies the goal predicate.
     *
     * @param startVertices the start vertices
     * @param goalPredicate the goal vertex
     * @param maxCost       the maximal cost of the path
     * @return an ordered pair (vertex sequence, cost),
     * or null if no sequence was found.
     */
    @Nullable OrderedPair<ImmutableList<V>, C> findVertexSequence(@NonNull Iterable<V> startVertices,
                                                                  @NonNull Predicate<V> goalPredicate, @NonNull C maxCost);

    /**
     * Finds a vertex sequence from a start vertex to a vertex
     * that satisfies the goal predicate.
     *
     * @param start         the start vertex
     * @param goalPredicate the goal vertex
     * @param maxCost       the maximal cost of the path
     * @return an ordered pair (vertex sequence, cost),
     * or null if no sequence was found.
     */
    default @Nullable OrderedPair<ImmutableList<V>, C> findVertexSequence(@NonNull V start,
                                                                          @NonNull Predicate<V> goalPredicate, @NonNull C maxCost) {
        return findVertexSequence(Collections.singletonList(start), goalPredicate, maxCost);
    }

    /**
     * Finds a vertex sequence from start to goal.
     *
     * @param start   the start vertex
     * @param goal    the goal vertex
     * @param maxCost the maximal cost of the path
     * @return an ordered pair (vertex sequence, cost),
     * or null if no sequence was found.
     */
    @Nullable
    default OrderedPair<ImmutableList<V>, C> findVertexSequence(@NonNull V start,
                                                                @NonNull V goal, @NonNull C maxCost) {
        return findVertexSequence(List.of(start), goal::equals, maxCost);
    }

    /**
     * Finds a vertex walk through the given waypoints.
     *
     * @param waypoints               a list of waypoints
     * @param maxCostBetweenWaypoints the maximal cost for paths between waypoints
     * @return an ordered pair (vertex sequence, cost),
     * or null if no sequence was found.
     */
    OrderedPair<ImmutableList<V>, C> findVertexSequenceOverWaypoints(@NonNull Iterable<V> waypoints, @NonNull C maxCostBetweenWaypoints);

    /**
     * Helper function for implementing {@link #findVertexSequenceOverWaypoints(Iterable, Number)}.
     *
     * @param waypoints                  the waypoints
     * @param findVertexSequenceFunction the search function, for example {@code this::findVertexSequence}
     * @param zero                       the zero value
     * @param sumFunction                the sum function
     * @param <VV>                       the vertex type
     * @param <CC>                       the number type
     * @return an ordered pair with the combined sequence
     */
    static <VV, CC extends Number & Comparable<CC>> OrderedPair<ImmutableList<VV>, CC> findVertexSequenceOverWaypoints(
            @NonNull Iterable<VV> waypoints,
            @NonNull BiFunction<VV, VV, OrderedPair<ImmutableList<VV>, CC>> findVertexSequenceFunction,
            @NonNull CC zero,
            @NonNull BiFunction<CC, CC, CC> sumFunction) {
        List<VV> sequence = new ArrayList<>();
        VV prev = null;
        CC sum = zero;
        int count = 0;
        for (VV next : waypoints) {
            if (prev != null) {
                final OrderedPair<ImmutableList<VV>, CC> result = findVertexSequenceFunction.apply(prev, next);
                if (result == null) {
                    return null;
                } else {
                    final List<VV> nextSequence = result.first().asList();
                    sequence.addAll(sequence.isEmpty() ? nextSequence : nextSequence.subList(1, nextSequence.size()));
                    sum = sumFunction.apply(sum, result.second());
                }
            }
            prev = next;
            count++;
        }

        if (count == 1) {
            // the set of waypoints is degenerate
            return new OrderedPair<>(ImmutableLists.of(prev), zero);
        }

        return new OrderedPair<>(ImmutableLists.copyOf(sequence), sum);
    }

}