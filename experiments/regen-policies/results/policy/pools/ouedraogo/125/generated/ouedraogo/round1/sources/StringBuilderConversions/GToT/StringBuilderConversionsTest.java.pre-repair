package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link StringBuilderConversions}.
 *
 * The class under test contains a single static method:
 * <pre>
 *     static String toString(Object from, Converter converter)
 * </pre>
 *
 * The method simply returns {@code from.toString()}, ignoring the {@code Converter}
 * argument.  The tests therefore focus on:
 * <ul>
 *   <li>Typical usage with common object types (e.g., {@link StringBuilder}).</li>
 *   <li>Behaviour when the supplied object overrides {@code toString()}.</li>
 *   <li>Edge‑cases such as a {@code null} source object (which should throw NPE)
 *       and a source whose {@code toString()} deliberately returns {@code null}.</li>
 *   <li>Confirming that the {@code Converter} argument is truly ignored (passing
 *       {@code null} or a real {@code Converter} does not affect the result).</li>
 * </ul>
 *
 * The test class lives in the same package as the class under test so it can
 * access the package‑private static method.
 */
public class StringBuilderConversionsTest {

    /*** 1. Basic functionality with a StringBuilder ***/
    @Test
    public void testToString_WithStringBuilder_ReturnsCorrectString() {
        StringBuilder sb = new StringBuilder("hello");
        // Converter is not used – passing null is sufficient.
        String result = StringBuilderConversions.toString(sb, null);
        assertEquals("hello", result);
    }

    /*** 2. Custom object whose toString() returns a specific value ***/
    private static class Person {
        private final String name;
        Person(String name) { this.name = name; }

        @Override
        public String toString() {
            return "Person:" + name;
        }
    }

    @Test
    public void testToString_WithCustomObject_ReturnsCustomToString() {
        Person p = new Person("John");
        String result = StringBuilderConversions.toString(p, null);
        assertEquals("Person:John", result);
    }

    /*** 3. Null source object – should throw NullPointerException ***/
    @Test(expected = NullPointerException.class)
    public void testToString_NullSource_ThrowsNPE() {
        // The method will attempt to call .toString() on a null reference.
        StringBuilderConversions.toString(null, null);
    }

    /*** 4. Source object whose toString() deliberately returns null ***/
    private static class NullToString {
        @Override
        public String toString() {
            return null;
        }
    }

    @Test
    public void testToString_ToStringReturnsNull_ReturnsNull() {
        NullToString nts = new NullToString();
        String result = StringBuilderConversions.toString(nts, null);
        assertNull("Expected null because the object's toString() returned null", result);
    }

    /*** 5. Verify that the Converter argument is ignored ***/
    @Test
    public void testToString_WithNonNullConverter_IgnoresConverter() {
        // We do not need a real Converter instance – passing null is enough,
        // but we also want to confirm that a non‑null instance does not affect
        // the outcome.  The library provides a public constructor that requires
        // a ConverterOptions instance; however, the test does not need any
        // conversion capabilities, so we can safely pass null for the options
        // (the constructor accepts it).
        Converter dummyConverter = new Converter(null);
        StringBuilder sb = new StringBuilder("world");
        String result = StringBuilderConversions.toString(sb, dummyConverter);
        assertEquals("world", result);
    }

    /*** 6. Additional typical case: passing a plain String object ***/
    @Test
    public void testToString_WithString_ReturnsSameString() {
        String source = "plain string";
        String result = StringBuilderConversions.toString(source, null);
        assertSame("The method should return the exact String instance when given a String",
                source, result);
    }
}
