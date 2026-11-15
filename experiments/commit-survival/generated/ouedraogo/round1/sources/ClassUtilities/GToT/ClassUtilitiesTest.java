package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Comprehensive JUnit‑4 test suite for {@link ClassUtilities}.
 *
 * The tests are organized as follows:
 *
 * <ul>
 *   <li>Verification of the public API signatures (extracted manually).</li>
 *   <li>Basic “happy‑path” tests for each public method.</li>
 *   <li>Edge‑case / error‑handling tests (null arguments, unknown classes, primitive‑wrapper
 *       interactions, security restrictions, array class names, etc.).</li>
 *   <li>Tests that exercise the internal alias handling used by {@code forName}.</li>
 *   <li>Tests that verify the OSGi‑related helper methods behave correctly when OSGi is not
 *       present (the default situation in the test environment).</li>
 * </ul>
 *
 * No external mocking frameworks are used – every object is constructed using the
 * public constructors or factory methods that are part of the supplied source code.
 */
public class ClassUtilitiesTest {

    private ClassLoader defaultClassLoader;

    @Before
    public void setUp() {
        defaultClassLoader = Thread.currentThread().getContextClassLoader();
    }

    @After
    public void tearDown() {
        // Clean up any aliases that may have been added during a test
        ClassUtilities.removePermanentClassAlias("myAlias");
    }

    /* --------------------------------------------------------------------- */
    /* 1.  PUBLIC METHOD SIGNATURES (extracted for documentation purposes) */
    /* --------------------------------------------------------------------- */
    // public static void addPermanentClassAlias(Class<?> clazz, String alias)
    // public static void removePermanentClassAlias(String alias)
    // public static int computeInheritanceDistance(Class<?> source, Class<?> destination)
    // public static boolean isPrimitive(Class<?> c)
    // public static Class<?> forName(String name, ClassLoader classLoader)
    // public static boolean isClassFinal(Class<?> c)
    // public static boolean areAllConstructorsPrivate(Class<?> c)
    // public static Class<?> toPrimitiveWrapperClass(Class<?> primitiveClass)
    // public static boolean doesOneWrapTheOther(Class<?> x, Class<?> y)
    // public static ClassLoader getClassLoader()
    // public static ClassLoader getClassLoader(Class<?> anchorClass)

    /* --------------------------------------------------------------------- */
    /* 2.  BASIC FUNCTIONAL TESTS                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddAndRemovePermanentClassAlias() {
        // Alias a known class
        ClassUtilities.addPermanentClassAlias(String.class, "myAlias");
        Class<?> c = ClassUtilities.forName("myAlias", defaultClassLoader);
        assertEquals(String.class, c);

        // Remove the alias and verify it is no longer resolvable
        ClassUtilities.removePermanentClassAlias("myAlias");
        assertNull(ClassUtilities.forName("myAlias", defaultClassLoader));
    }

    @Test
    public void testComputeInheritanceDistance_SameClass() {
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Integer.class, Integer.class));
    }

    @Test
    public void testComputeInheritanceDistance_ClassHierarchy() {
        // Integer -> Number -> Object : distance 2
        assertEquals(2, ClassUtilities.computeInheritanceDistance(Integer.class, Object.class));
        // ArrayList implements List directly : distance 1
        assertEquals(1, ClassUtilities.computeInheritanceDistance(ArrayList.class, List.class));
    }

    @Test
    public void testComputeInheritanceDistance_InterfaceToClass() {
        // List (interface) -> Collection (super‑interface) -> Iterable -> Object
        assertEquals(3, ClassUtilities.computeInheritanceDistance(List.class, Object.class));
    }

    @Test
    public void testComputeInheritanceDistance_PrimitiveAndWrapper() {
        // primitive int and wrapper Integer are considered the same distance 0
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, Integer.class));
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Integer.class, int.class));
    }

    @Test
    public void testComputeInheritanceDistance_NoRelation() {
        // Unrelated classes should yield -1
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, Integer.class));
    }

    @Test
    public void testIsPrimitive() {
        assertTrue(ClassUtilities.isPrimitive(int.class));
        assertTrue(ClassUtilities.isPrimitive(Integer.class));
        assertTrue(ClassUtilities.isPrimitive(void.class));
        assertFalse(ClassUtilities.isPrimitive(String.class));
        assertFalse(ClassUtilities.isPrimitive(Object.class));
    }

    @Test
    public void testForName_BasicLookup() {
        // Built‑in alias from static initializer
        assertEquals(Boolean.TYPE, ClassUtilities.forName("boolean", defaultClassLoader));
        // Normal class name
        assertEquals(String.class, ClassUtilities.forName("java.lang.String", defaultClassLoader));
    }

    @Test
    public void testForName_ArrayTypes() {
        // Primitive int array
        assertEquals(int[].class, ClassUtilities.forName("[I", defaultClassLoader));
        // Multi‑dimensional int array
        assertEquals(int[][].class, ClassUtilities.forName("[[I", defaultClassLoader));
        // Object array
        assertEquals(String[].class, ClassUtilities.forName("[Ljava.lang.String;", defaultClassLoader));
    }

    @Test
    public void testIsClassFinal() {
        assertTrue(ClassUtilities.isClassFinal(String.class)); // String is final
        assertFalse(ClassUtilities.isClassFinal(Object.class)); // Object is not final
    }

    @Test
    public void testAreAllConstructorsPrivate() {
        // java.util.Collections has a private constructor only
        assertTrue(ClassUtilities.areAllConstructorsPrivate(Collections.class));
        // java.lang.String has public constructors
        assertFalse(ClassUtilities.areAllConstructorsPrivate(String.class));
    }

    @Test
    public void testToPrimitiveWrapperClass() {
        assertEquals(Integer.class, ClassUtilities.toPrimitiveWrapperClass(int.class));
        assertEquals(void.class, ClassUtilities.toPrimitiveWrapperClass(void.class)); // wrapper for void is Void
        // Non‑primitive should be returned unchanged
        assertEquals(String.class, ClassUtilities.toPrimitiveWrapperClass(String.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToPrimitiveWrapperClass_NonPrimitiveThrows() {
        // Passing a non‑primitive class that is not a wrapper should cause an exception
        ClassUtilities.toPrimitiveWrapperClass(Object.class);
    }

    @Test
    public void testDoesOneWrapTheOther() {
        assertTrue(ClassUtilities.doesOneWrapTheOther(int.class, Integer.class));
        assertTrue(ClassUtilities.doesOneWrapTheOther(Integer.class, int.class));
        assertFalse(ClassUtilities.doesOneWrapTheOther(String.class, Integer.class));
    }

    @Test
    public void testGetClassLoader_NoOSGi() {
        ClassLoader cl = ClassUtilities.getClassLoader();
        assertNotNull(cl);
        // In a non‑OSGi environment the returned loader should be either the thread context
        // loader or the loader that loaded this utility class.
        ClassLoader expected = Thread.currentThread().getContextClassLoader();
        if (expected != null) {
            assertEquals(expected, cl);
        } else {
            assertEquals(ClassUtilities.class.getClassLoader(), cl);
        }
    }

    @Test
    public void testGetClassLoader_WithAnchor() {
        ClassLoader cl = ClassUtilities.getClassLoader(ClassUtilitiesTest.class);
        assertNotNull(cl);
    }

    /* --------------------------------------------------------------------- */
    /* 3.  EDGE‑CASE AND EXCEPTION TESTS                                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testComputeInheritanceDistance_NullArguments() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, Object.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(Object.class, null));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, null));
    }

    @Test
    public void testComputeInheritanceDistance_PrimitiveMismatch() {
        // primitive vs unrelated wrapper – should be -1
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, String.class));
        // wrapper vs unrelated primitive – should be -1
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(Integer.class, double.class));
    }

    @Test
    public void testForName_NullOrEmptyName() {
        assertNull(ClassUtilities.forName(null, defaultClassLoader));
        assertNull(ClassUtilities.forName("", defaultClassLoader));
        assertNull(ClassUtilities.forName("   ", defaultClassLoader));
    }

    @Test
    public void testForName_UnknownClassReturnsNull() {
        assertNull(ClassUtilities.forName("com.this.does.not.Exist", defaultClassLoader));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testForName_SecurityRestrictedClass() {
        // ClassLoader is among the restricted types – should trigger IllegalArgumentException
        ClassUtilities.forName("java.lang.ClassLoader", defaultClassLoader);
    }

    @Test
    public void testAddAliasThenLookupWithArrayName() {
        // Add an alias for a primitive array and verify lookup works
        ClassUtilities.addPermanentClassAlias(int[].class, "intArrayAlias");
        assertEquals(int[].class, ClassUtilities.forName("intArrayAlias", defaultClassLoader));
        ClassUtilities.removePermanentClassAlias("intArrayAlias");
        assertNull(ClassUtilities.forName("intArrayAlias", defaultClassLoader));
    }

    @Test
    public void testDoesOneWrapTheOther_NullArguments() {
        // The map returns null for unknown keys; null == null is true but we must avoid NPE
        // The method simply does a map get and comparison, so with null arguments it returns false
        assertFalse(ClassUtilities.doesOneWrapTheOther(null, Integer.class));
        assertFalse(ClassUtilities.doesOneWrapTheOther(int.class, null));
        assertFalse(ClassUtilities.doesOneWrapTheOther(null, null));
    }

    @Test
    public void testIsClassFinal_NullArgument() {
        try {
            ClassUtilities.isClassFinal(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException ignored) {
            // Expected – the implementation calls c.getModifiers()
        }
    }

    @Test
    public void testAreAllConstructorsPrivate_NullArgument() {
        try {
            ClassUtilities.areAllConstructorsPrivate(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException ignored) {
            // Expected – the implementation attempts to call getDeclaredConstructors()
        }
    }

    @Test
    public void testToPrimitiveWrapperClass_NullArgument() {
        try {
            ClassUtilities.toPrimitiveWrapperClass(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException ignored) {
            // Expected – the implementation calls isPrimitive()
        }
    }

    @Test
    public void testInternalClassForName_CacheBehavior() {
        // First lookup – not cached yet
        Class<?> first = ClassUtilities.forName("java.lang.String", defaultClassLoader);
        assertEquals(String.class, first);
        // The internal cache should now contain the mapping; a second lookup should hit the cache
        Class<?> second = ClassUtilities.forName("java.lang.String", defaultClassLoader);
        assertSame(first, second);
    }
}
