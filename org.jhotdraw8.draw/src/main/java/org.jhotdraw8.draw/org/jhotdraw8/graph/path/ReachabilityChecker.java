package org.jhotdraw8.graph.path;

import org.jhotdraw8.annotation.NonNull;

import java.util.function.Predicate;

/**
 * Defines an API for checking if a set of vertices can be reached from
 * a set of start vertices with a shortest path.
 *
 * @param <V>
 * @param <C>
 */
public interface ReachabilityChecker<V, C extends Number & Comparable<C>> {
    /**
     * Checks if a vertex sequence from a set of start vertices to a vertex
     * that satisfies the goal predicate exists.
     *
     * @param startVertices the start vertices
     * @param goalPredicate the goal vertex
     * @param maxCost
     * @return an ordered pair (vertex sequence, cost),
     * or null if no sequence was found.
     */
    boolean isReachable(@NonNull Iterable<V> startVertices,
                        @NonNull Predicate<V> goalPredicate,
                        @NonNull C maxCost);

    /**
     * Checks if a vertex sequence from a start vertex to a vertex
     * that satisfies the goal predicate exists.
     *
     * @param start         the start vertex
     * @param goalPredicate the goal vertex
     * @param maxCost
     * @return an ordered pair (vertex sequence, cost),
     * or null if no sequence was found.
     */
    boolean isReachable(@NonNull V start,
                        @NonNull Predicate<V> goalPredicate, @NonNull C maxCost);

    /**
     * Checks if a vertex sequence from start to goal exists.
     *
     * @param start   the start vertex
     * @param goal    the goal vertex
     * @param maxCost
     * @return an ordered pair (vertex sequence, cost),
     * or null if no sequence was found.
     */
    boolean isReachable(@NonNull V start, @NonNull V goal, @NonNull C maxCost);


}