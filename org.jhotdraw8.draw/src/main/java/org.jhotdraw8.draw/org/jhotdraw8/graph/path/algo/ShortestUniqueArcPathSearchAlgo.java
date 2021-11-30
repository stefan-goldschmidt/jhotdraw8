package org.jhotdraw8.graph.path.algo;

import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.annotation.Nullable;
import org.jhotdraw8.graph.Arc;
import org.jhotdraw8.graph.path.backlink.ArcBackLink;
import org.jhotdraw8.util.TriFunction;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Searches a unique shortest path from a set of start vertices to a set of goal
 * vertices using Dijkstra's algorithm.
 * <p>
 * The provided cost function must return values {@literal >= 0} for all arrows.
 * <p>
 * References:
 * <dl>
 * <dt>Esger W. Dijkstra (1959), A note on two problems in connexion with graphs,
 * Problem 2.
 * </dt>
 * <dd><a href="https://www-m3.ma.tum.de/twiki/pub/MN0506/WebHome/dijkstra.pdf">tum.de</a></dd>
 * </dl>
 * </dl>
 * Performance characteristics:
 * <dl>
 *     <dt>When a path can be found</dt><dd>less or equal {@literal O( |A| + |V|*log|V| )} within max cost</dd>
 *     <dt>When no path can be found</dt><dd>exactly {@literal O( |A| + |V|*log|V| )} within max cost</dd>
 * </dl>
 *
 * @param <V> the vertex data type
 * @param <A> the arrow data type
 * @param <C> the cost number type
 */
public class ShortestUniqueArcPathSearchAlgo<V, A, C extends Number & Comparable<C>> implements ArcPathSearchAlgo<V, A, C> {
    private static class CostData<C> {
        private final C cost;
        private int visiCount;

        public CostData(C cost, int visiCount) {
            this.cost = cost;
            this.visiCount = visiCount;
        }

        public C getCost() {
            return cost;
        }

        public int getVisiCount() {
            return visiCount;
        }

        public void increaseVisitCount() {
            visiCount++;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param startVertices    the set of start vertices
     * @param goalPredicate    the goal predicate
     * @param nextArcsFunction the next arcs function
     * @param zero             the zero cost value
     * @param positiveInfinity the positive infinity value
     * @param searchLimit      the maximal cost (inclusive) of a path.
     *                         Set this value as small as you can, to prevent
     *                         long search times if the goal can not be reached.
     * @param costFunction     the cost function
     * @param sumFunction      the sum function for adding two cost values
     * @return on success: a back link, otherwise: null
     */
    @Override
    public @Nullable ArcBackLink<V, A, C> search(
            final @NonNull Iterable<V> startVertices,
            final @NonNull Predicate<V> goalPredicate,
            final @NonNull Function<V, Iterable<Arc<V, A>>> nextArcsFunction,
            final @NonNull C zero,
            final @NonNull C positiveInfinity,
            final @NonNull C searchLimit,
            final @NonNull TriFunction<V, V, A, C> costFunction,
            final @NonNull BiFunction<C, C, C> sumFunction) {

        // Priority queue: back-links by lower cost and shallower depth.
        //          Ordering by depth prevents that the algorithm
        //          unnecessarily follows zero-length arrows.
        PriorityQueue<ArcBackLink<V, A, C>> queue = new PriorityQueue<>(
                Comparator.<ArcBackLink<V, A, C>, C>comparing(ArcBackLink::getCost).thenComparing(ArcBackLink::getDepth)
        );

        // Map with best known costs from start to a vertex and with the number
        // of times we have reached the map.
        // If an entry is missing, we assume infinity.
        Map<V, CostData<C>> costMap = new HashMap<>();

        CostData<C> infiniteCost = new CostData<>(positiveInfinity, 0);

        // Insert start itself in priority queue and initialize its cost as 0,
        // and number of paths with 1.
        for (V start : startVertices) {
            queue.add(new ArcBackLink<>(start, null, null, zero));
            costMap.put(start, new CostData<>(zero, 1));
        }

        // Loop until we have reached the goal, or queue is exhausted.
        C maxCost = searchLimit;
        ArcBackLink<V, A, C> found = null;
        while (!queue.isEmpty()) {
            ArcBackLink<V, A, C> node = queue.remove();
            final V u = node.getVertex();
            C costToU = node.getCost();
            if (goalPredicate.test(u)) {
                if (found == null) {
                    // We have found a shortest path for the first time.
                    // We can now limit the maxCost of further searches.
                    found = node;
                    maxCost = costToU;
                } else if (costToU.compareTo(maxCost) == 0) {
                    // We have found another shortest path with exactly
                    // the same cost!
                    return null;
                }
            }

            if (found != null && costToU.compareTo(maxCost) > 0) {
                // Once we have found a shortest path, we are only interested
                // in other paths that have the same cost.
                break;
            }

            for (Arc<V, A> arc : nextArcsFunction.apply(u)) {
                V v = arc.getEnd();
                A a = arc.getData();
                CostData<C> costDataV = costMap.getOrDefault(v, infiniteCost);
                final C bestKnownCost = costDataV.getCost();
                C cost = sumFunction.apply(costToU, costFunction.apply(u, v, a));

                // If there is a shorter path to v through u.
                if (cost.compareTo(maxCost) <= 0) {
                    final int compare = cost.compareTo(bestKnownCost);
                    if (compare < 0) {
                        // Update cost data to v.
                        costMap.put(v, new CostData<>(cost, 1));
                        queue.add(new ArcBackLink<>(v, a, node, cost));
                    } else if (compare == 0) {
                        // There is more than one shortest path to v!
                        costDataV.increaseVisitCount();
                    }
                }
            }
        }

        // The shortest path to the goal is only unique, if all vertices on the
        // path have been visited only once.
        for (ArcBackLink<V, A, C> node = found; node != null; node = node.getParent()) {
            if (costMap.get(node.getVertex()).getVisiCount() != 1) {
                return null;
            }
        }

        return found;
    }
}
