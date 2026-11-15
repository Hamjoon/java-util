package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Assert;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

/**
 * Unit tests for {@link ClassConversions}.
 * These tests cover normal operation, edge cases, and reflection based verification of the
 * private constructor.
 */
public class ClassConversionsTest {

    // Helper method to invoke ClassConversions.toString without needing a real Converter.
    private static String invokeToString(Object from) {
        return ClassConversions.toString(from, null);
    }

    @Test
    public void testToStringReturnsFullyQualifiedName() {
        // java.lang.String
        String result = invokeToString(String.class);
        Assert.assertEquals("java.lang.String", result);

        // java.util.ArrayList
        result = invokeToString(java.util.ArrayList.class);
        Assert.assertEquals("java.util.ArrayList", result);
    }

    @Test
    public void testToStringWithPrimitiveTypes() {
        // int primitive
        String result = invokeToString(int.class);
        Assert.assertEquals("int", result);

        // boolean primitive
        result = invokeToString(boolean.class);
        Assert.assertEquals("boolean", result);
    }

    @Test
    public void testToStringWithArrayClasses() {
        // Single‑dimensional int array
        String result = invokeToString(int[].class);
        // Class.getName() for int[] is "[I"
        Assert.assertEquals("[I", result);

        // Single‑dimensional String array
        result = invokeToString(String[].class);
        // Class.getName() for String[] is "[Ljava.lang.String;"
        Assert.assertEquals("[Ljava.lang.String;", result);

        // Multi‑dimensional array
        result = invokeToString(double[][][].class);
        // Class.getName() for double[][][] is "[[[D"
        Assert.assertEquals("[[[D", result);
    }

    @Test(expected = NullPointerException.class)
    public void testToStringWithNullFromThrowsNPE() {
        // The method casts null to Class<?> and then calls getName(),
        // which should result in a NullPointerException.
        invokeToString(null);
    }

    @Test(expected = ClassCastException.class)
    public void testToStringWithNonClassObjectThrowsClassCastException() {
        // Passing an object that is not a Class should cause a ClassCastException
        // during the cast inside the method.
        invokeToString("I am not a Class object");
    }

    @Test
    public void testPrivateConstructorIsInaccessible() throws Exception {
        Constructor<ClassConversions> ctor = ClassConversions.class.getDeclaredConstructor();
        // Verify the constructor is indeed private
        Assert.assertTrue("Constructor should be private",
                Modifier.isPrivate(ctor.getModifiers()));

        // Even though it's private, we can make it accessible for coverage purposes.
        // Instantiating the class should not throw an exception because the constructor
        // does nothing, but the class is not meant to be instantiated.
        ctor.setAccessible(true);
        ClassConversions instance = ctor.newInstance();
        Assert.assertNotNull("Instance should be created via reflection", instance);
    }
}
