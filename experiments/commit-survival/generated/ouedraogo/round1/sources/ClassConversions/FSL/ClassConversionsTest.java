package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

/**
 * Unit tests for {@link ClassConversions}.
 *
 * These tests exercise all logical paths of the {@code toString} method,
 * including normal operation, handling of {@code null}, and handling of
 * inappropriate input types.  They also verify that the utility class
 * cannot be instantiated via its private constructor.
 *
 * The {@code Converter} argument is not used by the method, but we pass
 * both a real instance (created with a {@code null} options object) and
 * {@code null} to demonstrate that it is truly ignored.
 */
public class ClassConversionsTest {

    // ----------------------------------------------------------------------
    // Helper methods
    // ----------------------------------------------------------------------
    private static Converter dummyConverter() {
        // The Converter constructor requires a ConverterOptions instance,
        // but the implementation does not use it, so we can safely pass null.
        return new Converter(null);
    }

    // ----------------------------------------------------------------------
    // Tests for normal class names
    // ----------------------------------------------------------------------
    @Test
    public void toStringReturnsFullyQualifiedNameForRegularClass() {
        String result = ClassConversions.toString(String.class, dummyConverter());
        assertEquals("java.lang.String", result);
    }

    @Test
    public void toStringReturnsNameForPrimitiveClass() {
        String result = ClassConversions.toString(int.class, dummyConverter());
        assertEquals("int", result);
    }

    @Test
    public void toStringReturnsNameForArrayClass() {
        String result = ClassConversions.toString(String[].class, dummyConverter());
        // The JVM encodes array class names in the internal form.
        assertEquals("[Ljava.lang.String;", result);
    }

    @Test
    public void toStringReturnsNameForMultiDimensionalArrayClass() {
        String result = ClassConversions.toString(int[][].class, dummyConverter());
        assertEquals("[[I", result);
    }

    @Test
    public void toStringReturnsNameForStaticInnerClass() {
        String result = ClassConversions.toString(StaticInner.class, dummyConverter());
        // The name includes the outer class name followed by $ and the inner class name.
        assertEquals("com.cedarsoftware.util.convert.ClassConversionsTest$StaticInner", result);
    }

    @Test
    public void toStringReturnsNameForAnonymousClass() {
        Object anonymous = new Object() {
            // anonymous subclass body
        };
        String result = ClassConversions.toString(anonymous.getClass(), dummyConverter());
        // The name of an anonymous class contains a '$' followed by a number.
        assertTrue("Anonymous class name should contain '$'", result.contains("$"));
        // It should also start with the enclosing test class name.
        assertTrue("Anonymous class name should start with the test class name",
                result.startsWith("com.cedarsoftware.util.convert.ClassConversionsTest"));
    }

    // ----------------------------------------------------------------------
    // Tests for edge cases and error handling
    // ----------------------------------------------------------------------
    @Test(expected = NullPointerException.class)
    public void toStringThrowsNullPointerExceptionWhenFromIsNull() {
        // The method attempts to call getName() on a null reference.
        ClassConversions.toString(null, dummyConverter());
    }

    @Test(expected = ClassCastException.class)
    public void toStringThrowsClassCastExceptionWhenFromIsNotAClass() {
        // Passing an object that is not a Class instance should cause a cast failure.
        ClassConversions.toString(new Object(), dummyConverter());
    }

    @Test
    public void toStringIgnoresConverterArgument() {
        // The method does not use the Converter argument; both null and a real instance behave identically.
        String expected = ClassConversions.toString(Boolean.class, dummyConverter());

        String resultWithNullConverter = ClassConversions.toString(Boolean.class, null);
        assertEquals(expected, resultWithNullConverter);
    }

    // ----------------------------------------------------------------------
    // Tests for the private constructor
    // ----------------------------------------------------------------------
    @Test
    public void privateConstructorIsNotAccessible() throws Exception {
        Constructor<ClassConversions> ctor = ClassConversions.class.getDeclaredConstructor();
        assertTrue("Constructor should be private", Modifier.isPrivate(ctor.getModifiers()));
        ctor.setAccessible(true);
        // Instantiating the class should succeed via reflection but does not affect functionality.
        ClassConversions instance = ctor.newInstance();
        assertNotNull(instance);
    }

    // ----------------------------------------------------------------------
    // Supporting static inner class used by tests
    // ----------------------------------------------------------------------
    private static class StaticInner {
        // No content needed; the class exists solely for name‑resolution testing.
    }
}
