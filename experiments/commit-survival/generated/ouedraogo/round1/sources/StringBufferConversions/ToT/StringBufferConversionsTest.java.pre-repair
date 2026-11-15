package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link StringBufferConversions}.
 *
 * The class under test contains a single static method:
 *
 * <pre>
 * static String toString(Object from, Converter converter)
 * </pre>
 *
 * The method is deliberately simple – it returns {@code from.toString()} and does
 * not use the {@code Converter} argument.  The tests therefore concentrate on:
 *
 * <ul>
 *   <li>Typical usage with common objects (String, StringBuilder, custom POJO).</li>
 *   <li>Edge cases such as a {@code null} argument, an object whose {@code toString()}
 *       returns {@code null}, and a {@code Converter} that could throw if it were
 *       used (which must not happen).</li>
 *   <li>Verification that the {@code Converter} instance is truly ignored.</li>
 * </ul>
 *
 * All objects are instantiated directly – no mocking frameworks are used,
 * complying with the restriction to rely only on the public signatures shown.
 */
public class StringBufferConversionsTest {

    /**
     * Helper method to create a minimal {@code Converter} instance.
     * The concrete implementation of {@code ConverterOptions} is not part of
     * the public contract required for these tests, so we use its default
     * constructor (which exists in the library).
     */
    private static Converter createConverter() {
        // The library provides a public constructor that accepts a ConverterOptions instance.
        // Using the default constructor of ConverterOptions is sufficient for our purposes.
        return new Converter(new ConverterOptions());
    }

    /** -------------------------------------------------------------------- **/
    /** 1. Typical usage with a plain String                                  **/
    /** -------------------------------------------------------------------- **/
    @Test
    public void testToString_WithString_ReturnsSameString() {
        String input = "Hello, world!";
        Converter converter = createConverter();

        String result = StringBufferConversions.toString(input, converter);

        assertEquals("The method should return the original string unchanged.", input, result);
    }

    /** -------------------------------------------------------------------- **/
    /** 2. Typical usage with a StringBuilder (similar to StringBuffer)       **/
    /** -------------------------------------------------------------------- **/
    @Test
    public void testToString_WithStringBuilder_ReturnsBuilderToString() {
        StringBuilder sb = new StringBuilder("builder content");
        Converter converter = createConverter();

        String result = StringBufferConversions.toString(sb, converter);

        assertEquals("StringBuilder.toString() should be returned.", sb.toString(), result);
    }

    /** -------------------------------------------------------------------- **/
    /** 3. Custom POJO with an overridden toString()                         **/
    /** -------------------------------------------------------------------- **/
    private static final class Person {
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

    @Test
    public void testToString_WithCustomObject_UsesOverriddenToString() {
        Person person = new Person("Alice", 30);
        Converter converter = createConverter();

        String result = StringBufferConversions.toString(person, converter);

        assertEquals("Custom object's toString() should be used.", person.toString(), result);
    }

    /** -------------------------------------------------------------------- **/
    /** 4. Verify that the Converter argument is completely ignored            **/
    /** -------------------------------------------------------------------- **/
    @Test
    public void testToString_ConverterIsIgnoredEvenIfMisconfigured() {
        // Create a Converter that would throw an exception if any conversion were attempted.
        // Since StringBufferConversions does not call any Converter methods, no exception should arise.
        Converter problematicConverter = new Converter(new ConverterOptions()) {
            @Override
            public <T> T convert(Object from, Class<T> toType) {
                throw new IllegalStateException("Converter should not be used.");
            }
        };

        String input = "ignore test";
        // No exception expected – the method should simply delegate to Object.toString().
        String result = StringBufferConversions.toString(input, problematicConverter);
        assertEquals("Result must match input's toString() even when Converter misbehaves.", input, result);
    }

    /** -------------------------------------------------------------------- **/
    /** 5. Null object argument – should throw NullPointerException           **/
    /** -------------------------------------------------------------------- **/
    @Test(expected = NullPointerException.class)
    public void testToString_NullObject_ThrowsNPE() {
        Converter converter = createConverter();
        // This call must raise NPE because we attempt to invoke toString() on null.
        StringBufferConversions.toString(null, converter);
    }

    /** -------------------------------------------------------------------- **/
    /** 6. Object whose toString() returns null – method should propagate null **/
    /** -------------------------------------------------------------------- **/
    private static final class NullToString {
        @Override
        public String toString() {
            return null;
        }
    }

    @Test
    public void testToString_ToStringReturnsNull_PropagatesNull() {
        NullToString obj = new NullToString();
        Converter converter = createConverter();

        String result = StringBufferConversions.toString(obj, converter);

        assertNull("If the object's toString() returns null, the method should return null.", result);
    }

    /** -------------------------------------------------------------------- **/
    /** 7. Verify that passing a Converter instance created with null options   **/
    /**    does not affect the outcome (constructor still succeeds).          **/
    /** -------------------------------------------------------------------- **/
    @Test
    public void testToString_ConverterCreatedWithNullOptions_Ignored() {
        // The library permits constructing a Converter with a null options reference.
        // This test ensures that such a Converter can be supplied without side‑effects.
        Converter converterWithNullOptions = new Converter(null);
        String input = "null‑options test";

        String result = StringBufferConversions.toString(input, converterWithNullOptions);
        assertEquals("Method must still return the object's toString() result.", input, result);
    }
}
