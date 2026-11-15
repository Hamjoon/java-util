package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

public class ClassConversionsTest {

    private Converter dummyConverter;

    @Before
    public void setUp() {
        // The Converter constructor requires a ConverterOptions instance.
        // The implementation details of ConverterOptions are not part of this
        // test suite, and the Converter instance is not used by ClassConversions.
        // Passing null is acceptable for the purpose of these tests.
        dummyConverter = new Converter(null);
    }

    /**
     * Verify that a normal class is converted to its fully‑qualified name.
     */
    @Test
    public void testToString_WithRegularClass_ReturnsClassName() {
        String result = ClassConversions.toString(String.class, dummyConverter);
        assertEquals("java.lang.String", result);
    }

    /**
     * Verify that a primitive class (e.g., int) is handled correctly.
     */
    @Test
    public void testToString_WithPrimitiveClass_ReturnsPrimitiveName() {
        String result = ClassConversions.toString(int.class, dummyConverter);
        assertEquals("int", result);
    }

    /**
     * Verify that an array class returns the name as produced by Class#getName().
     */
    @Test
    public void testToString_WithArrayClass_ReturnsArrayName() {
        String result = ClassConversions.toString(String[].class, dummyConverter);
        // For a one‑dimensional object array the name is "[Ljava.lang.String;"
        assertEquals("[Ljava.lang.String;", result);
    }

    /**
     * Verify that a multi‑dimensional array class name is returned correctly.
     */
    @Test
    public void testToString_WithMultiDimArrayClass_ReturnsArrayName() {
        String result = ClassConversions.toString(int[][].class, dummyConverter);
        // For a two‑dimensional int array the name is "[[I"
        assertEquals("[[I", result);
    }

    /**
     * Verify that passing {@code null} as the source object causes a
     * {@link NullPointerException}. The method casts the argument to {@code Class<?>}
     * before dereferencing it, so a null reference triggers the exception.
     */
    @Test(expected = NullPointerException.class)
    public void testToString_WithNullSource_ThrowsNullPointerException() {
        ClassConversions.toString(null, dummyConverter);
    }

    /**
     * Verify that passing an object that is not a {@code Class} causes a
     * {@link ClassCastException}. The method unconditionally casts the argument,
     * so any non‑Class instance results in this runtime exception.
     */
    @Test(expected = ClassCastException.class)
    public void testToString_WithNonClassObject_ThrowsClassCastException() {
        ClassConversions.toString("I am not a Class", dummyConverter);
    }

    /**
     * Verify that the {@code Converter} argument is truly ignored.
     * We supply a Converter that would throw an exception if any of its
     * methods were invoked; since {@code ClassConversions.toString}
     * never calls the converter, the test passes.
     */
    @Test
    public void testToString_IgnoresConverterParameter() {
        // Create a Converter that throws if any method is called.
        Converter hostileConverter = new Converter(null) {
            @Override
            public <T> T convert(Object from, Class<T> toType) {
                throw new AssertionError("Converter.convert should not be called");
            }
        };

        // The method should succeed and return the class name despite the
        // hostile converter.
        String result = ClassConversions.toString(Boolean.class, hostileConverter);
        assertEquals("java.lang.Boolean", result);
    }
}
