package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.lang.reflect.Modifier;
import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit 4 test suite for {@link ClassUtilities}.
 *
 * The test cases are divided into three sections:
 *   1. Basic functionality tests – one test per public method that verifies the
 *      normal (happy‑path) behaviour.
 *   2. Edge‑case / exception tests – tests that exercise null arguments,
 *      primitive / wrapper mismatches, unknown class names, etc.
 *   3. Interaction tests – e.g. adding and then removing a class alias and
 *      checking that {@code forName} reacts accordingly.
 *
 * The suite does **not** use any mocking framework; all objects are created
 * using the constructors that are visible in the source code of {@code ClassUtilities}
 * or standard JDK classes.
 */
public class ClassUtilitiesTest
{
    private static final ClassLoader TEST_CLASS_LOADER = ClassUtilitiesTest.class.getClassLoader();

    /* ---------------------------------------------------------------------- */
    /*  Helper classes used only by the test suite                           */
    /* ---------------------------------------------------------------------- */

    /** A simple final class – used to test {@code isClassFinal}. */
    public static final class FinalTestClass { }

    /** A class with a *single* private constructor – used for {@code areAllConstructorsPrivate}. */
    public static class PrivateCtorOnly
    {
        private PrivateCtorOnly() { }
    }

    /** A class with a public constructor – used for {@code areAllConstructorsPrivate}. */
    public static class PublicCtor
    {
        public PublicCtor() { }
    }

    /* ---------------------------------------------------------------------- */
    /*  1. Basic functionality tests                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAddAndRemovePermanentClassAlias()
    {
        // Ensure the alias does not exist initially
        assertNull(ClassUtilities.forName("myAlias", TEST_CLASS_LOADER));

        // Add an alias for java.util.Date
        ClassUtilities.addPermanentClassAlias(Date.class, "myAlias");
        Class<?> loaded = ClassUtilities.forName("myAlias", TEST_CLASS_LOADER);
        assertNotNull(loaded);
        assertEquals(Date.class, loaded);

        // Remove the alias and verify it is gone
        ClassUtilities.removePermanentClassAlias("myAlias");
        assertNull(ClassUtilities.forName("myAlias", TEST_CLASS_LOADER));
    }

    @Test
    public void testComputeInheritanceDistance_SameClass()
    {
        assertEquals(0, ClassUtilities.computeInheritanceDistance(String.class, String.class));
    }

    @Test
    public void testComputeInheritanceDistance_Superclass()
    {
        // Integer -> Number
        assertEquals(1, ClassUtilities.computeInheritanceDistance(Integer.class, Number.class));
    }

    @Test
    public void testComputeInheritanceDistance_Interface()
    {
        // ArrayList implements List
        assertEquals(1, ClassUtilities.computeInheritanceDistance(ArrayList.class, List.class));
    }

    @Test
    public void testComputeInheritanceDistance_Unrelated()
    {
        // String and Integer are unrelated (except Object)
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, Integer.class));
    }

    @Test
    public void testComputeInheritanceDistance_PrimitiveAndWrapper()
    {
        // int (primitive) ↔ Integer (wrapper) distance is defined as 0
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, Integer.class));
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Integer.class, int.class));
    }

    @Test
    public void testComputeInheritanceDistance_PrimitiveMismatch()
    {
        // int primitive to Long wrapper – not the same primitive type
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, Long.class));
        // Wrapper to unrelated primitive
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(Long.class, int.class));
    }

    @Test
    public void testIsPrimitive()
    {
        assertTrue(ClassUtilities.isPrimitive(int.class));
        assertTrue(ClassUtilities.isPrimitive(Integer.class));
        assertFalse(ClassUtilities.isPrimitive(String.class));
    }

    @Test
    public void testForName_KnownClasses()
    {
        // Built‑in alias for "string"
        assertEquals(String.class, ClassUtilities.forName("string", TEST_CLASS_LOADER));

        // Regular class name – should be found via the class loader
        assertEquals(Date.class, ClassUtilities.forName("java.util.Date", TEST_CLASS_LOADER));
    }

    @Test
    public void testForName_NullOrEmpty()
    {
        assertNull(ClassUtilities.forName(null, TEST_CLASS_LOADER));
        assertNull(ClassUtilities.forName("", TEST_CLASS_LOADER));
        assertNull(ClassUtilities.forName("   ", TEST_CLASS_LOADER));
    }

    @Test
    public void testForName_UnknownClass()
    {
        // A class that does not exist – method should return null, not throw
        assertNull(ClassUtilities.forName("com.this.does.not.Exist", TEST_CLASS_LOADER));
    }

    @Test
    public void testIsClassFinal()
    {
        assertTrue(ClassUtilities.isClassFinal(String.class));      // String is final
        assertTrue(ClassUtilities.isClassFinal(FinalTestClass.class));
        assertFalse(ClassUtilities.isClassFinal(Object.class));    // Object is not final
        assertFalse(ClassUtilities.isClassFinal(ArrayList.class));
    }

    @Test
    public void testAreAllConstructorsPrivate()
    {
        assertTrue(ClassUtilities.areAllConstructorsPrivate(PrivateCtorOnly.class));
        assertFalse(ClassUtilities.areAllConstructorsPrivate(PublicCtor.class));
        // ClassUtilities itself has a public default constructor → false
        assertFalse(ClassUtilities.areAllConstructorsPrivate(ClassUtilities.class));
    }

    @Test
    public void testToPrimitiveWrapperClass()
    {
        // Primitive → wrapper
        assertEquals(Integer.class, ClassUtilities.toPrimitiveWrapperClass(int.class));
        assertEquals(Boolean.class, ClassUtilities.toPrimitiveWrapperClass(boolean.class));

        // Non‑primitive → unchanged
        assertEquals(String.class, ClassUtilities.toPrimitiveWrapperClass(String.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToPrimitiveWrapperClass_NonPrimitiveThrows()
    {
        // Passing a non‑primitive class that is not in the map should trigger the exception
        ClassUtilities.toPrimitiveWrapperClass(void.class); // void is primitive but mapped to Void, so no exception
        // The next line forces the exception path (null is not allowed, but we test a class that is not primitive)
        ClassUtilities.toPrimitiveWrapperClass(Object.class);
    }

    @Test
    public void testDoesOneWrapTheOther()
    {
        // Direct wrapper ↔ primitive relationships
        assertTrue(ClassUtilities.doesOneWrapTheOther(int.class, Integer.class));
        assertTrue(ClassUtilities.doesOneWrapTheOther(Integer.class, int.class));

        // Negative case
        assertFalse(ClassUtilities.doesOneWrapTheOther(String.class, Integer.class));
    }

    @Test
    public void testGetClassLoader()
    {
        // The utility should never return null
        ClassLoader cl = ClassUtilities.getClassLoader();
        assertNotNull(cl);

        // When we pass an explicit anchor class we should receive a loader that can load that class
        ClassLoader cl2 = ClassUtilities.getClassLoader(ClassUtilitiesTest.class);
        assertNotNull(cl2);
        assertTrue(cl2.loadClass(ClassUtilitiesTest.class.getName()) == ClassUtilitiesTest.class);
    }

    /* ---------------------------------------------------------------------- */
    /*  2. Edge‑case / exception tests                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testComputeInheritanceDistance_NullArguments()
    {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, String.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, null));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, null));
    }

    @Test
    public void testComputeInheritanceDistance_ArrayTypes()
    {
        // int[] and int[] are the same class → distance 0
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int[].class, int[].class));

        // int[] does not inherit from Object[] (different component types)
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int[].class, Object[].class));
    }

    @Test
    public void testForName_ArrayNotation()
    {
        // "[I" denotes int[] – should be resolved correctly
        assertEquals(int[].class, ClassUtilities.forName("[I", TEST_CLASS_LOADER));

        // "[[Ljava.lang.String;" denotes String[][]
        assertEquals(String[][].class,
                ClassUtilities.forName("[[Ljava.lang.String;", TEST_CLASS_LOADER));
    }

    @Test
    public void testAddPermanentClassAlias_OverridesExisting()
    {
        // Add an alias for java.util.Date
        ClassUtilities.addPermanentClassAlias(Date.class, "tempAlias");
        assertEquals(Date.class, ClassUtilities.forName("tempAlias", TEST_CLASS_LOADER));

        // Override the alias with a different class (String)
        ClassUtilities.addPermanentClassAlias(String.class, "tempAlias");
        assertEquals(String.class, ClassUtilities.forName("tempAlias", TEST_CLASS_LOADER));

        // Clean‑up
        ClassUtilities.removePermanentClassAlias("tempAlias");
    }

    @Test
    public void testIsPrimitive_WithCustomWrapperSet()
    {
        // The internal set contains wrapper classes; ensure a custom wrapper is NOT considered primitive
        assertFalse(ClassUtilities.isPrimitive(Queue.class));
    }

    @Test
    public void testToPrimitiveWrapperClass_NullArgument()
    {
        try {
            ClassUtilities.toPrimitiveWrapperClass(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  3. Interaction / combined scenario tests                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAliasImpactOnComputeInheritanceDistance()
    {
        // Create an alias for java.util.ArrayList and then compute distance via the alias name
        ClassUtilities.addPermanentClassAlias(ArrayList.class, "myList");
        Class<?> aliasClass = ClassUtilities.forName("myList", TEST_CLASS_LOADER);
        assertNotNull(aliasClass);
        assertEquals(ArrayList.class, aliasClass);

        // Distance from alias (ArrayList) to List should be 1
        assertEquals(1,
                ClassUtilities.computeInheritanceDistance(aliasClass, List.class));

        // Clean‑up
        ClassUtilities.removePermanentClassAlias("myList");
    }

    @Test
    public void testDoesOneWrapTheOther_WithPrimitiveArrays()
    {
        // Primitive array types are NOT considered wrappers of each other
        assertFalse(ClassUtilities.doesOneWrapTheOther(int[].class, Integer[].class));
    }
}
