package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

public class ClassConversionsTest {

    // -----------------------------------------------------------------
    // Public methods of ClassConversions (extracted)
    // -----------------------------------------------------------------
    // static String toString(Object from, Converter converter)

    /**
     * Basic functionality test – verifies that a normal Class object is
     * converted to its fully‑qualified name.
     */
    @Test
    public void testToStringReturnsClassName() {
        String result = ClassConversions.toString(String.class, null);
        assertEquals("java.lang.String", result);
    }

    /**
     * Verify that primitive type Class objects are handled correctly.
     */
    @Test
    public void testToStringWithPrimitiveClass() {
        String result = ClassConversions.toString(int.class, null);
        assertEquals("int", result);
    }

    /**
     * Edge case: the {@code from} argument is {@code null}.
     * Expected behaviour – a {@link NullPointerException} is thrown
     * when {@code cls.getName()} is invoked on a null reference.
     */
    @Test(expected = NullPointerException.class)
    public void testToStringWithNullFrom() {
        ClassConversions.toString(null, null);
    }

    /**
     * Edge case: the {@code from} argument is not a {@code Class} instance.
     * Expected behaviour – a {@link ClassCastException} is thrown
     * during the cast to {@code Class<?>}.
     */
    @Test(expected = ClassCastException.class)
    public void testToStringWithNonClassObject() {
        Object notAClass = new Object();
        ClassConversions.toString(notAClass, null);
    }

    /**
     * Although the {@code converter} argument is not used by the method,
     * we confirm that passing a non‑null instance does not affect the result.
     * (We use a minimal {@code Converter} instance created with default
     * options – the actual conversion logic is irrelevant for this test.)
     */
    @Test
    public void testToStringWithNonNullConverter() {
        // The Converter class requires a ConverterOptions instance.
        // Assuming a no‑arg constructor exists for demonstration; if not,
        // replace with an appropriate way to obtain a valid Converter.
        ConverterOptions options = new ConverterOptions();
        Converter converter = new Converter(options);

        String result = ClassConversions.toString(Long.class, converter);
        assertEquals("java.lang.Long", result);
    }
}
