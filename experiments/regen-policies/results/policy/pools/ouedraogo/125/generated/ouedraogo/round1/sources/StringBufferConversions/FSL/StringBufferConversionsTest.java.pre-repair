package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for {@link StringBufferConversions}.
 *
 * The class under test only contains a single static method:
 * <pre>
 *     static String toString(Object from, Converter converter)
 * </pre>
 * which simply returns {@code from.toString()} and does not use the {@code Converter}
 * argument. The tests below verify the contract for a variety of input objects,
 * confirm that the {@code Converter} parameter is truly ignored, and assert that a
 * {@code NullPointerException} is thrown when {@code from} is {@code null}.
 *
 * No mocking frameworks are used – only real objects that can be instantiated
 * via public constructors (or via reflection when a constructor requires a
 * type not available in the test classpath, such as {@code ConverterOptions}).
 */
public class StringBufferConversionsTest {

    /**
     * Helper class that overrides {@code toString()} to return a known value.
     */
    private static final class CustomObject {
        private final String value;

        CustomObject(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "Custom:" + value;
        }
    }

    /**
     * Creates a {@link Converter} instance using reflection.
     * The {@code Converter} constructor requires a {@code ConverterOptions} argument,
     * which is not part of the test source. By passing {@code null} we can still
     * obtain a usable instance because the {@code StringBufferConversions.toString}
     * method never dereferences the converter.
     *
     * @return a {@code Converter} instance (or {@code null} if reflection fails)
     */
    private static Converter createConverterViaReflection() {
        try {
            Class<?> converterClass = Class.forName("com.cedarsoftware.util.convert.Converter");
            java.lang.reflect.Constructor<?> ctor = converterClass.getDeclaredConstructor(Class.forName("com.cedarsoftware.util.convert.ConverterOptions"));
            ctor.setAccessible(true);
            // Pass null for the ConverterOptions – the constructor should accept it.
            return (Converter) ctor.newInstance((Object) null);
        } catch (Exception e) {
            // If anything goes wrong, fall back to null. The tests that depend on a non‑null
            // converter will simply skip the reflection part.
            return null;
        }
    }

    @Test
    public void testToString_WithStringObject() {
        String input = "hello world";
        Converter converter = createConverterViaReflection(); // may be null, which is fine
        String result = StringBufferConversions.toString(input, converter);
        assertEquals("The method should return the original string unchanged.", input, result);
    }

    @Test
    public void testToString_WithIntegerObject() {
        Integer input = 42;
        // Use a null converter to prove that the argument is irrelevant.
        String result = StringBufferConversions.toString(input, null);
        assertEquals("Integer.toString() should be returned.", "42", result);
    }

    @Test
    public void testToString_WithCustomObject() {
        CustomObject input = new CustomObject("payload");
        Converter converter = createConverterViaReflection(); // non‑null if reflection succeeded
        String result = StringBufferConversions.toString(input, converter);
        assertEquals("Custom object's toString() must be propagated.", "Custom:payload", result);
    }

    @Test(expected = NullPointerException.class)
    public void testToString_NullInputThrowsNPE() {
        // The method directly calls from.toString(); passing null must raise NPE.
        StringBufferConversions.toString(null, null);
    }

    @Test
    public void testConverterArgumentIsIgnored() {
        // Prepare two different Converter instances (one null, one via reflection).
        Converter nullConverter = null;
        Converter realConverter = createConverterViaReflection();

        String input = "ignore-me";

        String resultWithNull = StringBufferConversions.toString(input, nullConverter);
        String resultWithReal = StringBufferConversions.toString(input, realConverter);

        assertEquals("Result must be identical regardless of the converter (null).", input, resultWithNull);
        assertEquals("Result must be identical regardless of the converter (real).", input, resultWithReal);
    }
}
