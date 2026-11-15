package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.*;

/**
 * Comprehensive tests for {@link StringBuilderConversions}.
 *
 * The class under test has a single static method:
 *   {@code static String toString(Object from, Converter converter)}
 *
 * The method simply returns {@code from.toString()} and does **not**
 * use the supplied {@link Converter} instance.  Consequently the tests
 * focus on:
 *   • Normal objects (StringBuilder, custom types, wrappers, arrays)
 *   • Edge conditions (null arguments, {@code toString()} returning null)
 *   • The fact that the {@code Converter} argument can be {@code null}
 *     or any valid instance without influencing the result.
 *
 * No mocking frameworks are used – real objects are constructed
 * directly as required by the problem statement.
 */
public class StringBuilderConversionsTest {

    /** A dummy Converter instance – the conversion logic is irrelevant for the tests. */
    private Converter dummyConverter;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        // Converter has a public constructor that takes a ConverterOptions argument.
        // The concrete type of ConverterOptions is not part of the public API shown,
        // but we can instantiate it via its public no‑arg constructor (if any).
        // If ConverterOptions does not have a public no‑arg constructor, the test
        // can simply pass null because the Converter constructor accepts a
        // ConverterOptions instance (null is allowed by the library).
        dummyConverter = new Converter(new ConverterOptions());
    }

    /* --------------------------------------------------------------
     *  Normal usage tests
     * -------------------------------------------------------------- */

    @Test
    public void toString_WithStringBuilder_ReturnsBuilderContent() {
        StringBuilder sb = new StringBuilder("builder text");
        String result = StringBuilderConversions.toString(sb, dummyConverter);
        assertEquals("builder text", result);
    }

    @Test
    public void toString_WithIntegerWrapper_ReturnsNumberString() {
        Integer value = Integer.valueOf(42);
        String result = StringBuilderConversions.toString(value, dummyConverter);
        assertEquals("42", result);
    }

    @Test
    public void toString_WithCustomObject_ReturnsCustomToString() {
        class Custom {
            @Override
            public String toString() {
                return "CustomRepresentation";
            }
        }
        Custom custom = new Custom();
        String result = StringBuilderConversions.toString(custom, dummyConverter);
        assertEquals("CustomRepresentation", result);
    }

    @Test
    public void toString_WithArrayObject_ReturnsArrayToString() {
        Object[] array = new Object[] { 1, "two", null };
        // The expected value is whatever Object[].toString() produces.
        String expected = array.toString();
        String result = StringBuilderConversions.toString(array, dummyConverter);
        assertEquals(expected, result);
    }

    @Test
    public void toString_WithNullConverter_IgnoresConverterArgument() {
        StringBuilder sb = new StringBuilder("no converter");
        // Pass null for the Converter – method must still work.
        String result = StringBuilderConversions.toString(sb, null);
        assertEquals("no converter", result);
    }

    /* --------------------------------------------------------------
     *  Edge‑case tests
     * -------------------------------------------------------------- */

    @Test
    public void toString_WithObjectHavingNullToString_ReturnsNull() {
        class NullString {
            @Override
            public String toString() {
                return null;   // technically allowed, though discouraged.
            }
        }
        NullString obj = new NullString();
        String result = StringBuilderConversions.toString(obj, dummyConverter);
        assertNull(result);
    }

    @Test
    public void toString_WithFromArgumentNull_ThrowsNullPointerException() {
        thrown.expect(NullPointerException.class);
        // The method calls from.toString(); a null argument must raise NPE.
        StringBuilderConversions.toString(null, dummyConverter);
    }

    /* --------------------------------------------------------------
     *  Additional sanity checks
     * -------------------------------------------------------------- */

    @Test
    public void toString_DoesNotInteractWithConverter() {
        // Build a Converter that would throw if any of its public methods were called.
        // Since the method under test never invokes the converter, no exception should be thrown.
        Converter hostileConverter = new Converter(new ConverterOptions()) {
            @Override
            public <T> T convert(Object from, Class<T> toType) {
                throw new IllegalStateException("Converter should not be used");
            }
        };
        StringBuilder sb = new StringBuilder("safe");
        // No exception – the converter is ignored.
        String result = StringBuilderConversions.toString(sb, hostileConverter);
        assertEquals("safe", result);
    }
}
