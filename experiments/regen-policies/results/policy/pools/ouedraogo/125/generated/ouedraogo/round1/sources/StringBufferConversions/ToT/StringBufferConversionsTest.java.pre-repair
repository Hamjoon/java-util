package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Assert;

/**
 * Comprehensive JUnit‑4 test suite for {@link StringBufferConversions}.
 *
 * The class under test contains a single static method:
 * <pre>
 *     static String toString(Object from, Converter converter)
 * </pre>
 * which simply returns {@code from.toString()}.  The tests therefore concentrate on
 * the contract of {@code Object.toString()} for a variety of inputs, the handling of
 * {@code null}, and the fact that the {@code Converter} argument is ignored.
 *
 * The tests are written without any mocking frameworks – only real objects that can be
 * constructed from the public API are used, as required by the exercise.
 */
public class StringBufferConversionsTest {

    /**
     * Helper method to create a minimal {@link Converter} instance.
     *
     * The {@link Converter} constructor requires a {@link ConverterOptions} object.
     * The concrete implementation of {@code ConverterOptions} is not part of the
     * public API shown, but it is assumed to have a public no‑arg constructor
     * (as it does in the actual library).  This method centralises its creation
     * so that each test can obtain a distinct, valid instance.
     *
     * @return a new {@link Converter}
     */
    private static Converter createConverter() {
        // The real library provides a public no‑arg constructor for ConverterOptions.
        // If the constructor ever changes, this method can be updated in a single place.
        return new Converter(new ConverterOptions());
    }

    /**
     * Typical case – passing a regular Java object.
     */
    @Test
    public void testToString_WithStandardObject_ReturnsObjectToString() {
        Object obj = new Object();               // default Object.toString()
        String expected = obj.toString();

        String result = StringBufferConversions.toString(obj, createConverter());

        Assert.assertEquals("The method should return the object's toString()", expected, result);
    }

    /**
     * Object with an overridden {@code toString()} implementation.
     */
    @Test
    public void testToString_WithCustomToStringObject() {
        class Person {
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

        Person person = new Person("Alice", 30);
        String expected = "Person{name='Alice', age=30}";

        String result = StringBufferConversions.toString(person, createConverter());

        Assert.assertEquals("Custom toString() must be returned unchanged", expected, result);
    }

    /**
     * Passing {@code null} as the source object must throw {@link NullPointerException},
     * because the implementation dereferences {@code from} directly.
     */
    @Test(expected = NullPointerException.class)
    public void testToString_WithNullObject_ThrowsNullPointerException() {
        StringBufferConversions.toString(null, createConverter());
    }

    /**
     * Verify that the {@code Converter} argument is truly ignored.
     *
     * The test creates a {@link Converter} that is deliberately mis‑configured
     * (e.g., with a null options object).  If the method ever started using the
     * converter, an exception would be thrown.  The successful execution of the
     * method proves that the converter is not consulted.
     */
    @Test
    public void testToString_IgnoresConverterArgument() {
        // Create a Converter with a null options reference – this would normally
        // cause a NullPointerException if the converter were used.
        Converter brokenConverter = new Converter(null);

        String source = "ignore-me";
        String result = StringBufferConversions.toString(source, brokenConverter);

        Assert.assertEquals("The converter must be ignored; result should be source.toString()",
                source, result);
    }

    /**
     * Arrays have a default {@code toString()} that prints the type and hash code.
     * This test confirms that the method forwards that exact string.
     */
    @Test
    public void testToString_WithArrayObject_ReturnsArrayIdentityString() {
        String[] array = {"one", "two"};
        String expected = array.toString(); // e.g. "[Ljava.lang.String;@1a2b3c4d"

        String result = StringBufferConversions.toString(array, createConverter());

        Assert.assertEquals("Array's default toString() must be returned unchanged", expected, result);
    }

    /**
     * Verify that primitive wrapper objects are handled correctly.
     */
    @Test
    public void testToString_WithIntegerWrapper() {
        Integer number = Integer.valueOf(42);
        String expected = "42";

        String result = StringBufferConversions.toString(number, createConverter());

        Assert.assertEquals("Integer wrapper's toString() must be returned", expected, result);
    }

    /**
     * Verify that a {@link StringBuilder} (which implements CharSequence) works as expected.
     */
    @Test
    public void testToString_WithStringBuilder() {
        StringBuilder sb = new StringBuilder("builder");
        String expected = "builder";

        String result = StringBufferConversions.toString(sb, createConverter());

        Assert.assertEquals("StringBuilder's toString() must be returned", expected, result);
    }
}
