package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

public class StringBuilderConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // Converter requires a ConverterOptions instance; the constructor tolerates null options.
        // Using null keeps the test independent of the internal configuration of ConverterOptions.
        this.converter = new Converter(null);
    }

    /**
     * Typical case – converting a {@link StringBuilder} to its {@code String}
     * representation.  The {@code toString} implementation of {@code StringBuilder}
     * returns the current character sequence.
     */
    @Test
    public void testToString_WithStringBuilder() {
        StringBuilder sb = new StringBuilder();
        sb.append("Hello");
        sb.append(' ');
        sb.append("World");

        String result = StringBuilderConversions.toString(sb, converter);
        assertEquals("Hello World", result);
    }

    /**
     * Typical case – converting a plain {@link Integer} to its {@code String}
     * representation.
     */
    @Test
    public void testToString_WithInteger() {
        Integer value = 42;
        String result = StringBuilderConversions.toString(value, converter);
        assertEquals("42", result);
    }

    /**
     * Edge case – the {@code from} argument is {@code null}.
     * The method directly invokes {@code from.toString()}, therefore a
     * {@link NullPointerException} must be thrown.
     */
    @Test(expected = NullPointerException.class)
    public void testToString_NullFrom() {
        StringBuilderConversions.toString(null, converter);
    }

    /**
     * Edge case – the {@code converter} argument is {@code null}.
     * The implementation does not use the converter, so the call should succeed.
     */
    @Test
    public void testToString_NullConverter() {
        StringBuilder sb = new StringBuilder("NoConverter");
        String result = StringBuilderConversions.toString(sb, null);
        assertEquals("NoConverter", result);
    }

    /**
     * Edge case – an object whose {@code toString()} implementation throws a
     * runtime exception.  The exception must propagate unchanged.
     */
    @Test(expected = IllegalStateException.class)
    public void testToString_ToStringThrowsException() {
        Object bad = new Object() {
            @Override
            public String toString() {
                throw new IllegalStateException("Broken toString");
            }
        };
        StringBuilderConversions.toString(bad, converter);
    }

    /**
     * Verify that the method works with a custom class that overrides {@code toString()}
     * to return a non‑trivial value.
     */
    @Test
    public void testToString_CustomObject() {
        class Person {
            private final String name;
            private final int age;

            Person(String name, int age) {
                this.name = name;
                this.age = age;
            }

            @Override
            public String toString() {
                return name + ":" + age;
            }
        }

        Person p = new Person("Alice", 30);
        String result = StringBuilderConversions.toString(p, converter);
        assertEquals("Alice:30", result);
    }

    /**
     * Verify that the method does not accidentally use the {@code Converter}
     * parameter by checking that a freshly created {@code Converter} with
     * default (empty) options still yields the correct result.
     */
    @Test
    public void testToString_ConverterIgnored() {
        // Create a brand‑new Converter instance – its internal state is irrelevant.
        Converter freshConverter = new Converter(null);
        StringBuilder sb = new StringBuilder("IgnoredConverter");
        String result = StringBuilderConversions.toString(sb, freshConverter);
        assertEquals("IgnoredConverter", result);
    }
}
