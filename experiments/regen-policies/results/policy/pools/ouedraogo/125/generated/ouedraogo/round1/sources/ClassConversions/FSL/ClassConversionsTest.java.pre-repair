package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import org.junit.Test;

/**
 * Unit tests for {@link ClassConversions}.
 */
public class ClassConversionsTest {

    /**
     * Verify that a normal {@link Class} instance is correctly converted to its fully‑qualified name.
     */
    @Test
    public void testToStringWithRegularClass() {
        // The method does not use the Converter argument, so we can safely pass {@code null}.
        String result = ClassConversions.toString(String.class, null);
        assertEquals("java.lang.String", result);
    }

    /**
     * Verify that primitive class objects are handled correctly.
     */
    @Test
    public void testToStringWithPrimitiveClass() {
        String result = ClassConversions.toString(int.class, null);
        assertEquals("int", result);
    }

    /**
     * Verify that array class objects are handled correctly (the JVM's internal name is expected).
     */
    @Test
    public void testToStringWithArrayClass() {
        String result = ClassConversions.toString(String[].class, null);
        // The JVM encodes array types as "[L<component-type>;".
        assertEquals("[Ljava.lang.String;", result);
    }

    /**
     * Verify that passing {@code null} as the source object results in a {@link NullPointerException}.
     */
    @Test(expected = NullPointerException.class)
    public void testToStringWithNullSource() {
        ClassConversions.toString(null, null);
    }

    /**
     * Verify that passing an object that is not a {@link Class} results in a {@link ClassCastException}.
     */
    @Test(expected = ClassCastException.class)
    public void testToStringWithInvalidSourceType() {
        ClassConversions.toString("not a class", null);
    }

    /**
     * The class has a private constructor. This test confirms that the constructor is indeed private
     * and cannot be accessed without reflection.
     */
    @Test
    public void testConstructorIsPrivate() throws Exception {
        Constructor<ClassConversions> ctor = ClassConversions.class.getDeclaredConstructor();
        assertTrue("Constructor should be private", Modifier.isPrivate(ctor.getModifiers()));

        // Attempt to instantiate via reflection – it should be possible only after making it accessible.
        // The test ensures the constructor does not perform any unexpected logic.
        ctor.setAccessible(true);
        ClassConversions instance = ctor.newInstance();
        assertNotNull("Instance created via reflection should not be null", instance);
    }
}
