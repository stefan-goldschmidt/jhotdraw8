/*
 * @(#)DefaultConverterTest.java
 * Copyright © 2022 The authors and contributors of JHotDraw. MIT License.
 */

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jhotdraw8.text;

import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.base.converter.DefaultConverter;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * @author Werner Randelshofer
 */
public class DefaultConverterTest {


    public static void testFromString(String expectedOutput, @NonNull String input) throws Exception {
        DefaultConverter c = new DefaultConverter();

        Object actualOutput = c.fromString(input);

        assertEquals(actualOutput, expectedOutput);
    }


    public static void testToString(String input, String expectedOutput) throws Exception {
        DefaultConverter c = new DefaultConverter();

        Object actualOutput = c.toString(input);

        assertEquals(actualOutput, expectedOutput);
    }

    public static @NonNull Object[][] textData() {
        return new Object[][]{
                {"hello world", "hello world"},
        };

    }

    @TestFactory
    public @NonNull List<DynamicTest> dynamicTestsFromString() {
        return Arrays.asList(
                dynamicTest("1", () -> testFromString("hello", "hello"))
        );

    }

    @TestFactory
    public @NonNull List<DynamicTest> dynamicTestsToString() {
        return Arrays.asList(
                dynamicTest("1", () -> testToString("hello", "hello"))
        );

    }

}
