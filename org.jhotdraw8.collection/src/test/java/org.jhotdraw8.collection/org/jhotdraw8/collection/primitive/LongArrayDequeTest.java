/*
 * @(#)LongArrayDequeTest.java
 * Copyright © 2022 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.collection.primitive;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author werni
 */
public class LongArrayDequeTest {

    public LongArrayDequeTest() {
    }

    /**
     * Test of addFirst method, of class LongArrayDeque.
     */
    @Test
    public void testAddFirst() {
        int e = 1;
        LongArrayDeque instance = new LongArrayDeque();
        instance.addFirstAsLong(e);
        assertFalse(instance.isEmpty());

        assertEquals(1, instance.getFirstAsLong());

        instance.addFirstAsLong(2);
        assertEquals(2, instance.getFirstAsLong());
        assertEquals(2, instance.size());
    }

    /**
     * Test of addLast method, of class LongArrayDeque.
     */
    @Test
    public void testAddLast() {
        int e = 1;
        LongArrayDeque instance = new LongArrayDeque();
        instance.addLastAsLong(e);
        assertFalse(instance.isEmpty());

        assertEquals(1, instance.getLastAsLong());

        instance.addLastAsLong(2);
        assertEquals(2, instance.getLastAsLong());
        assertEquals(2, instance.size());
    }

    @Test
    public void testAddAll() {
        LongArrayDeque instance = new LongArrayDeque(4);
        instance.addLastAllAsLong(new long[]{1, 2, 3});
        instance.addLastAllAsLong(new long[]{4, 5});
        instance.addLastAllAsLong(new long[]{0, 6, 7}, 1, 2);
        assertEquals(1, instance.removeFirstAsLong());
        assertEquals(2, instance.removeFirstAsLong());
        instance.addLastAllAsLong(new long[]{0, 8, 9, 0}, 1, 2);
        instance.addLastAllAsLong(new long[]{0, 10, 11, 0}, 1, 2);

        LongArrayDeque expected = new LongArrayDeque(0);
        expected.addLastAllAsLong(new long[]{3, 4, 5, 6, 7, 8, 9, 10, 11});
        assertEquals(expected, instance);
        assertEquals(expected.hashCode(), instance.hashCode());

        assertEquals(3, instance.removeFirstAsLong());
        assertEquals(4, instance.removeFirstAsLong());
        assertEquals(5, instance.removeFirstAsLong());
        assertEquals(11, instance.removeLastAsLong());
        assertEquals(10, instance.removeLastAsLong());
        instance.addLastAllAsLong(new long[]{10, 11, 12, 13, 14, 15, 16, 17, 18, 19});

        expected.clear();
        expected.addLastAllAsLong(new long[]{6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19});
        assertEquals(expected, instance);
        assertEquals(expected.hashCode(), instance.hashCode());
    }
}
