/* @(#)AnyPathBuilderTest.java
 * Copyright (c) 2017 The authors and contributors of JHotDraw. MIT License.
 */

package org.jhotdraw8.graph;

import org.jhotdraw8.annotation.NonNull;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * AnyShortestPathBuilderTest.
 *
 * @author Werner Randelshofer
 */
public class AnyShortestPathBuilderTest {

    public AnyShortestPathBuilderTest() {
    }

    private @NonNull DirectedGraph<Integer, Double> createGraph() {
        DirectedGraphBuilder<Integer, Double> builder = new DirectedGraphBuilder<>();

        // __|  1  |  2  |  3  |  4  |  5  |   6
        // 1 |       7.0   9.0               14.0
        // 2 | 7.0        10.0  15.0
        // 3 |                  11.0          2.0
        // 4 |                         6.0
        // 5 |                                9.0
        // 6 |14.0                     9.0
        //
        //

        builder.addVertex(1);
        builder.addVertex(2);
        builder.addVertex(3);
        builder.addVertex(4);
        builder.addVertex(5);
        builder.addVertex(6);
        builder.addBidiArrow(1, 2, 7.0);
        builder.addArrow(1, 3, 9.0);
        builder.addBidiArrow(1, 6, 14.0);
        builder.addArrow(2, 3, 10.0);
        builder.addArrow(2, 4, 15.0);
        builder.addArrow(3, 4, 11.0);
        builder.addArrow(3, 6, 2.0);
        builder.addArrow(4, 5, 6.0);
        builder.addBidiArrow(5, 6, 9.0);
        return builder;
    }


    @Test
    public void testCreateGraph() {
        final DirectedGraph<Integer, Double> graph = createGraph();

        final String expected
                = "1 -> 2, 3, 6.\n"
                + "2 -> 1, 3, 4.\n"
                + "3 -> 4, 6.\n"
                + "4 -> 5.\n"
                + "5 -> 6.\n"
                + "6 -> 1, 5.";

        final String actual = DumpGraphs.dumpAsAdjacencyList(graph);

        assertEquals(expected, actual);
    }


    @TestFactory
    public @NonNull List<DynamicTest> dynamicTestsFindShortestVertexPath() {
        return Arrays.asList(
                dynamicTest("0", () -> doFindShortestVertexPath(1, 1, VertexSequence.of(1), 0.0)),
                dynamicTest("1", () -> doFindShortestVertexPath(1, 5, VertexSequence.of(1, 3, 6, 5), 20.0)),
                dynamicTest("2", () -> doFindShortestVertexPath(1, 4, VertexSequence.of(1, 3, 4), 20.0)),
                dynamicTest("3", () -> doFindShortestVertexPath(2, 6, VertexSequence.of(2, 3, 6), 12.0))
        );
    }

    /**
     * Test of findAnyPath method, of class AnyShortestPathBuilder.
     */
    public void doFindShortestVertexPath(@NonNull Integer start, @NonNull Integer goal, VertexSequence<Integer> expPath, double expCost) throws Exception {
        DirectedGraph<Integer, Double> graph = createGraph();
        ToDoubleFunction<Double> costf = arg -> arg;
        AnyDoubleShortestPathBuilder<Integer, Double> instance = new AnyDoubleShortestPathBuilder<>(graph::getNextArcs, costf);
        Map.Entry<VertexSequence<Integer>, Double> result = instance.findVertexPath(start, goal::equals);
        if (result == null) {
            assertNull(expPath);
        } else {
            assertEquals(expPath, result.getKey());
            assertEquals(expCost, result.getValue().doubleValue());
        }
    }

    @TestFactory
    public @NonNull List<DynamicTest> testFindShortestEdgeMultiGoalPath() throws Exception {
        return Arrays.asList(
                dynamicTest("0", () -> doFindShortestEdgeMultiGoalPath(1, Arrays.asList(1, 6), ArrowSequence.of())),
                dynamicTest("1", () -> doFindShortestEdgeMultiGoalPath(1, Arrays.asList(5, 6), ArrowSequence.of(9.0, 2.0))),
                dynamicTest("2", () -> doFindShortestEdgeMultiGoalPath(1, Arrays.asList(4, 5), ArrowSequence.of(9.0, 11.0))),
                dynamicTest("3", () -> doFindShortestEdgeMultiGoalPath(2, Arrays.asList(3, 6), ArrowSequence.of(10.0))),
                dynamicTest("4", () -> doFindShortestEdgeMultiGoalPath(1, Arrays.asList(6, 5), ArrowSequence.of(9.0, 2.0))),
                dynamicTest("5", () -> doFindShortestEdgeMultiGoalPath(1, Arrays.asList(5, 4), ArrowSequence.of(9.0, 11.0))),
                dynamicTest("6", () -> doFindShortestEdgeMultiGoalPath(2, Arrays.asList(6, 3), ArrowSequence.of(10.0)))
        );
    }

    /**
     * Test of findAnyPath method, of class AnyShortestPathBuilder.
     */
    public void doFindShortestEdgeMultiGoalPath(@NonNull Integer start, @NonNull List<Integer> multiGoal, ArrowSequence<Double> expResult) throws Exception {
        DirectedGraph<Integer, Double> graph = createGraph();
        ToDoubleFunction<Double> costf = arg -> arg;
        AnyDoubleShortestPathBuilder<Integer, Double> instance = new AnyDoubleShortestPathBuilder<>(graph::getNextArcs, costf);

        // Find a path for each individual goal, and remember the shortest path
        ArrowSequence<Double> individualShortestPath = null;
        double individualShortestCost = Double.POSITIVE_INFINITY;
        for (Integer goal : multiGoal) {
            Map.Entry<ArrowSequence<Double>, Double> resultEntry = instance.findArrowPath(start, goal::equals);
            ArrowSequence<Double> result = resultEntry.getKey();
            double resultLength = result.getArrows().stream().mapToDouble(Double::doubleValue).sum();
            if (resultLength < individualShortestCost
                    || resultLength == individualShortestCost && result.size() < individualShortestPath.size()
            ) {
                individualShortestCost = resultLength;
                individualShortestPath = result;
            }
        }

        // Find shortest path to any of the goals
        Map.Entry<ArrowSequence<Double>, Double> actualShortestPath = instance.findArrowPath(start, multiGoal::contains);
        double actualCost = actualShortestPath.getValue();

        assertEquals(individualShortestCost, actualCost);
        assertEquals(expResult, actualShortestPath.getKey());
    }

    @TestFactory
    public @NonNull List<DynamicTest> testFindShortestArrowPath() throws Exception {
        return Arrays.asList(
                dynamicTest("1", () -> doFindShortestArrowPath(1, 5, ArrowSequence.of(9.0, 2.0, 9.0))),
                dynamicTest("2", () -> doFindShortestArrowPath(1, 4, ArrowSequence.of(9.0, 11.0))),
                dynamicTest("3", () -> doFindShortestArrowPath(2, 6, ArrowSequence.of(10.0, 2.0)))
        );
    }

    /**
     * Test of findAnyPath method, of class AnyShortestPathBuilder.
     */
    private void doFindShortestArrowPath(@NonNull Integer start, @NonNull Integer goal, ArrowSequence<Double> expResult) throws Exception {
        DirectedGraph<Integer, Double> graph = createGraph();
        ToDoubleFunction<Double> costf = arg -> arg;
        AnyDoubleShortestPathBuilder<Integer, Double> instance = new AnyDoubleShortestPathBuilder<>(graph::getNextArcs, costf);
        Map.Entry<ArrowSequence<Double>, Double> result = instance.findArrowPath(start, goal::equals);
        assertEquals(expResult, result.getKey());
    }

    private @NonNull DirectedGraph<Integer, Double> createGraph2() {
        DirectedGraphBuilder<Integer, Double> b = new DirectedGraphBuilder<>();
        b.addVertex(1);
        b.addVertex(2);
        b.addVertex(3);
        b.addVertex(4);
        b.addVertex(5);

        b.addArrow(1, 2, 1.0);
        b.addArrow(1, 3, 1.0);
        b.addArrow(2, 3, 1.0);
        b.addArrow(3, 4, 1.0);
        b.addArrow(3, 5, 1.0);
        b.addArrow(4, 5, 1.0);
        return b;
    }


    @TestFactory
    public @NonNull List<DynamicTest> testFindVertexPathOverWaypoints() throws Exception {
        return Arrays.asList(
                dynamicTest("1", () -> doFindShortestVertexPathOverWaypoints(Arrays.asList(1, 5), VertexSequence.of(1, 3, 6, 5), 20.0)),
                dynamicTest("2", () -> doFindShortestVertexPathOverWaypoints(Arrays.asList(1, 4), VertexSequence.of(1, 3, 4), 20.0)),
                dynamicTest("3", () -> doFindShortestVertexPathOverWaypoints(Arrays.asList(2, 6), VertexSequence.of(2, 3, 6), 12.0)),
                dynamicTest("4", () -> doFindShortestVertexPathOverWaypoints(Arrays.asList(1, 6, 5), VertexSequence.of(1, 3, 6, 5), 20.0))
        );
    }

    /**
     * Test of findAnyVertexPath method, of class AnyPathBuilder.
     */
    private void doFindShortestVertexPathOverWaypoints(@NonNull List<Integer> waypoints, VertexSequence<Integer> expResult, double expCost) throws Exception {
        ToDoubleFunction<Double> costf = arg -> arg;
        DirectedGraph<Integer, Double> graph = createGraph();
        AnyDoubleShortestPathBuilder<Integer, Double> instance = new AnyDoubleShortestPathBuilder<>(graph::getNextArcs, costf);
        Map.Entry<VertexSequence<Integer>, Double> actual = instance.findVertexPathOverWaypoints(waypoints);
        assertEquals(expResult, actual.getKey());
        assertEquals(expCost, actual.getValue().doubleValue());
    }

    @TestFactory
    public @NonNull List<DynamicTest> testFindArrowPathOverWaypoints() throws Exception {
        return Arrays.asList(
                dynamicTest("1", () -> doFindArrowPathOverWaypoints(Arrays.asList(1, 5), ArrowSequence.of(9.0, 2.0, 9.0), 20.0)),
                dynamicTest("2", () -> doFindArrowPathOverWaypoints(Arrays.asList(1, 4), ArrowSequence.of(9.0, 11.0), 20.0)),
                dynamicTest("3", () -> doFindArrowPathOverWaypoints(Arrays.asList(2, 6), ArrowSequence.of(10.0, 2.0), 12.0)),
                dynamicTest("4", () -> doFindArrowPathOverWaypoints(Arrays.asList(1, 6, 5), ArrowSequence.of(9.0, 2.0, 9.0), 20.0))
        );
    }

    /**
     * Test of findAnyVertexPath method, of class AnyPathBuilder.
     */
    private void doFindArrowPathOverWaypoints(@NonNull List<Integer> waypoints, ArrowSequence<Double> expResult, double expCost) throws Exception {
        ToDoubleFunction<Double> costf = arg -> arg;
        DirectedGraph<Integer, Double> graph = createGraph();
        AnyDoubleShortestPathBuilder<Integer, Double> instance = new AnyDoubleShortestPathBuilder<>(graph::getNextArcs, costf);
        Map.Entry<ArrowSequence<Double>, Double> actual = instance.findArrowPathOverWaypoints(waypoints, Integer.MAX_VALUE);
        assertEquals(expResult, actual.getKey());
        assertEquals(expCost, actual.getValue().doubleValue());
    }
}