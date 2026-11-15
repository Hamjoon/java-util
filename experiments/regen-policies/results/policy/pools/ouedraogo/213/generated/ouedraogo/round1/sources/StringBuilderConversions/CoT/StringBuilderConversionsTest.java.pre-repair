package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

public class StringBuilderConversionsTest {

    // -----------------------------------------------------------------
    // Helper classes used in the tests
    // -----------------------------------------------------------------
    /** Simple POJO with a custom toString implementation. */
    private static class Person {
        private final String name;
        private final int age;

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public String toString() {
            return "Person{name='" + name + "', age=" + age + '}';
        }
    }

    /** An object whose toString deliberately returns null. */
    private static class NullStringObject {
        @Override
        public String toString() {
            return null;
        }
    }

    // -----------------------------------------------------------------
    // Factory method to obtain a Converter instance (the Converter API
    // is part of the library and requires a ConverterOptions object).
    // -----------------------------------------------------------------
    private static Converter createConverter() {
        // Assuming ConverterOptions has a public no‑arg constructor.
        return new Converter(new ConverterOptions());
    }

    // -----------------------------------------------------------------
    // 1. Basic functional tests for StringBuilderConversions.toString(...)
    // -----------------------------------------------------------------

    @Test
    public void testToString_WithStringObject() {
        String source = "hello world";
        Converter converter = createConverter();

        String result = StringBuilderConversions.toString(source, converter);

        assertEquals("The method should return the original string's toString()", source, result);
    }

    @Test
    public void testToString_WithIntegerObject() {
        Integer source = 42;
        Converter converter = createConverter();

        String result = StringBuilderConversions.toString(source, converter);

        assertEquals("Integer.toString() should be used", "42", result);
    }

    @Test
    public void testToString_WithCustomObject() {
        Person source = new Person("Alice", 30);
        Converter converter = createConverter();

        String result = StringBuilderConversions.toString(source, converter);

        assertEquals("Custom toString should be returned",
                "Person{name='Alice', age=30}", result);
    }

    @Test
    public void testToString_IgnoresConverterArgument() {
        String source = "ignore me";
        // Pass a null converter – the method does not use it.
        Converter nullConverter = null;

        String result = StringBuilderConversions.toString(source, nullConverter);

        assertEquals("Converter argument should be ignored", source, result);
    }

    // -----------------------------------------------------------------
    // 2. Edge‑case and exception handling tests
    // -----------------------------------------------------------------

    @Test(expected = NullPointerException.class)
    public void testToString_NullSourceObject_ThrowsNPE() {
        Object source = null;
        Converter converter = createConverter();

        // This call must throw NullPointerException because the method
        // invokes from.toString() without a null‑check.
        StringBuilderConversions.toString(source, converter);
    }

    @Test
    public void testToString_SourceToStringReturnsNull() {
        NullStringObject source = new NullStringObject();
        Converter converter = createConverter();

        String result = StringBuilderConversions.toString(source, converter);

        // The contract of the method is to return whatever from.toString()
        // returns, even if it is null.
        assertNull("Result should be null when source.toString() returns null", result);
    }

    @Test
    public void testToString_WithDifferentConverterInstances() {
        String source = "test";
        // Create two distinct Converter instances to prove the method does not
        // depend on any state of the converter.
        Converter converterA = createConverter();
        Converter converterB = createConverter();

        String resultA = StringBuilderConversions.toString(source, converterA);
        String resultB = StringBuilderConversions.toString(source, converterB);

        assertEquals("Result should be identical regardless of which Converter instance is supplied", resultA, resultB);
    }
}
