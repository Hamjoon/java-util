/*
 * JUnit 4 test suite for {@code com.cedarsoftware.util.convert.ClassConversions}.
 *
 * This test class verifies the behaviour of the static {@code toString(Object, Converter)}
 * method. The method simply casts the first argument to {@code Class<?>} and returns the
 * result of {@code Class#getName()}. The tests therefore focus on:
 *
 * 1. Correct conversion of normal {@code Class} objects (including primitives and arrays).
 * 2. Proper handling of {@code null} for the {@code from} argument (expected {@code NPE}).
 * 3. Proper handling of non‑{@code Class} objects (expected {@code ClassCastException}).
 * 4. The fact that the {@code Converter} argument is ignored (it may be {@code null} or a real instance).
 *
 * No mocking frameworks are used; a real {@code Converter} instance is created using the
 * public constructor that requires a {@code ConverterOptions} instance.
 */
package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

public class ClassConversionsTest {

    private Converter dummyConverter;

    @Before
    public void setUp() {
        // The Converter class requires a ConverterOptions instance.
        // The library provides a public no‑arg constructor for ConverterOptions,
        // so we can create a real Converter object without any special configuration.
        dummyConverter = new Converter(new ConverterOptions());
    }

    /**
     * Verify that the fully‑qualified class name is returned for a regular class.
     */
    @Test
    public void testToStringReturnsClassName() {
        String result = ClassConversions.toString(String.class, dummyConverter);
        assertEquals("java.lang.String", result);
    }

    /**
     * Verify that primitive types are handled correctly – {@code Class#getName()}
     * returns the primitive keyword.
     */
    @Test
    public void testToStringWithPrimitiveClass() {
        String result = ClassConversions.toString(int.class, dummyConverter);
        assertEquals("int", result);
    }

    /**
     * Verify that array classes are handled correctly – the name follows the JVM
     * internal array descriptor format.
     */
    @Test
    public void testToStringWithArrayClass() {
        String result = ClassConversions.toString(String[].class, dummyConverter);
        // The expected name for a one‑dimensional array of java.lang.String is "[Ljava.lang.String;"
        assertEquals("[Ljava.lang.String;", result);
    }

    /**
     * The method does not use the {@code Converter} argument, so passing {@code null}
     * should still work as long as the {@code from} argument is a valid {@code Class}.
     */
    @Test
    public void testToStringIgnoresConverterArgument() {
        String result = ClassConversions.toString(Boolean.class, null);
        assertEquals("java.lang.Boolean", result);
    }

    /**
     * Passing {@code null} as the {@code from} argument should result in a
     * {@link NullPointerException} when {@code getName()} is invoked.
     */
    @Test(expected = NullPointerException.class)
    public void testToStringWithNullFromThrowsNPE() {
        ClassConversions.toString(null, dummyConverter);
    }

    /**
     * Passing an object that is not a {@code Class} should cause a {@link ClassCastException}
     * because the method casts the argument to {@code Class<?>} without checking.
     */
    @Test(expected = ClassCastException.class)
    public void testToStringWithInvalidFromThrowsClassCastException() {
        Object notAClass = new Object();
        ClassConversions.toString(notAClass, dummyConverter);
    }
}
