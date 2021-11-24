package org.jhotdraw8.graph.path;

import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.annotation.Nullable;
import org.jhotdraw8.collection.ImmutableList;
import org.jhotdraw8.collection.ImmutableLists;
import org.jhotdraw8.collection.OrderedPair;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * Defines an API for finding arrow sequences associated with a cost
 * through a directed graph.
 *
 * @param <V> the vertex data type
 * @param <A> the arrow data type
 * @param <C> the cost number type
 */
public interface ArrowSequenceFinder<V, A, C extends Number & Comparable<C>> {


    /**
     * Finds an arrow sequence from a set of start vertices to a vertex
     * that satisfies the goal predicate.
     *
     * @param startVertices the start vertex
     * @param goalPredicate the goal vertex
     * @param maxCost       the maximal cost of the path
     * @return an ordered pair (arrow sequence, cost),
     * or null if no sequence was found.
     */
    @Nullable
    OrderedPair<ImmutableList<A>, C> findArrowSequence(@NonNull Iterable<V> startVertices,
                                                       @NonNull Predicate<V> goalPredicate, @NonNull C maxCost);

    /**
     * Finds an arrow sequence from start to goal.
     *
     * @param start   the start vertex
     * @param goal    the goal vertex
     * @param maxCost the maximal cost of the path
     * @return an ordered pair (arrow sequence, cost),
     * or null if no sequence was found.
     */
    @Nullable
    default OrderedPair<ImmutableList<A>, C> findArrowSequence(@NonNull V start,
                                                               @NonNull V goal, @NonNull C maxCost) {
        return findArrowSequence(List.of(start), goal::equals, maxCost);
    }

    /**
     * Finds an arrow walk through the given waypoints.
     *
     * @param waypoints               a list of waypoints
     * @param maxCostBetweenWaypoints the maximal cost for paths between waypoints
     * @return an ordered pair (arrow sequence, cost),
     * or null if no sequence was found.
     */
    OrderedPair<ImmutableList<A>, C> findArrowSequenceOverWaypoints(@NonNull Iterable<V> waypoints, @NonNull C maxCostBetweenWaypoints);

    /**
     * Helper function for implementing {@link #findArrowSequenceOverWaypoints(Iterable, Number)}.
     *
     * @param waypoints                 the waypoints
     * @param findArrowSequenceFunction the search function, for example {@code this::findArrowSequence}
     * @param zero                      the zero value
     * @param sumFunction               the sum function
     * @param <VV>                      the vertex type
     * @param <CC>                      the number type
     * @return an ordered pair with the combined sequence
     */
    static <VV, AA, CC extends Number & Comparable<CC>> OrderedPair<ImmutableList<AA>, CC> findArrowSequenceOverWaypoints(
            @NonNull Iterable<VV> waypoints,
            @NonNull BiFunction<VV, VV, OrderedPair<ImmutableList<AA>, CC>> findArrowSequenceFunction,
            @NonNull CC zero,
            @NonNull BiFunction<CC, CC, CC> sumFunction) {
        List<AA> sequence = new ArrayList<>();
        CC sum = zero;
        VV prev = null;
        int count = 0;
        for (VV next : waypoints) {
            if (prev != null) {
                final OrderedPair<ImmutableList<AA>, CC> result = findArrowSequenceFunction.apply(prev, next);
                if (result == null) {
                    return null;
                } else {
                    final List<AA> nextSequence = result.first().asList();
                    sequence.addAll(nextSequence);
                    sum = sumFunction.apply(sum, result.second());
                }
            }
            prev = next;
            count++;
        }

        if (count == 1) {
            // the set of waypoints is degenerate
            return new OrderedPair<>(ImmutableLists.of(), zero);
        }

        return new OrderedPair<>(ImmutableLists.copyOf(sequence), sum);
    }

}