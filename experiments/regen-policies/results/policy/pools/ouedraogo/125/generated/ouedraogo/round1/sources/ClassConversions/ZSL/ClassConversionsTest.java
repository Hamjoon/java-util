package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

public class ClassConversionsTest {

    /**
     * Helper method to create a minimal {@link Converter} instance.
     * The {@code Converter} constructor requires a {@link ConverterOptions} argument.
     * For the purpose of these tests the actual options are irrelevant because
     * {@link ClassConversions#toString(Object, Converter)} never uses the converter.
     */
    private static Converter createConverter() {
        // Assuming ConverterOptions has a public no‑arg constructor.
        // If it does not, passing {@code null} is also acceptable for most implementations.
        try {
            Class<?> optionsClass = Class.forName("com.cedarsoftware.util.convert.ConverterOptions");
            Object optionsInstance = optionsClass.getDeclaredConstructor().newInstance();
            return new Converter((ConverterOptions) optionsInstance);
        } catch (Exception e) {
            // Fallback: use null if the options class cannot be instantiated.
            return new Converter(null);
        }
    }

    @Test
    public void testToStringWithRegularClass() {
        String result = ClassConversions.toString(String.class, createConverter());
        assertEquals("java.lang.String", result);
    }

    @Test
    public void testToStringWithPrimitiveClass() {
        String result = ClassConversions.toString(int.class, createConverter());
        assertEquals("int", result);
    }

    @Test
    public void testToStringIgnoresConverterArgument() {
        // Pass a null converter – the method should still succeed because it never uses it.
        String result = ClassConversions.toString(Double.class, null);
        assertEquals("java.lang.Double", result);
    }

    @Test(expected = ClassCastException.class)
    public void testToStringWithNonClassObjectThrowsClassCastException() {
        // Passing an object that is not a Class should cause a ClassCastException
        ClassConversions.toString(new Object(), createConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToStringWithNullFromThrowsNullPointerException() {
        // The method casts null to Class and then calls getName(), which throws NPE.
        ClassConversions.toString(null, createConverter());
    }
}
