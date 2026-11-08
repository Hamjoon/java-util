package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link ClassUtilities}.
 *
 * The tests aim to cover every public static method in the class,
 * exercising normal, edge and error cases.  The suite never uses any
 * external mocking framework – it creates real objects and relies only
 * on the APIs that are part of the supplied source code.
 */
public class ClassUtilitiesTest {

    /* --------------------------------------------------------------------- *
     *  Helper types used by several tests                                   *
     * --------------------------------------------------------------------- */

    /** Simple interface hierarchy for inheritance‑distance tests. */
    interface A {}

    interface B extends A {}

    /** Concrete class implementing B. */
    static class C implements B {}

    /** Class with only private constructors – used for areAllConstructorsPrivate(). */
    static final class PrivateCtorOnly {
        private PrivateCtorOnly() {}
        private PrivateCtorOnly(int ignored) {}
    }

    /** A non‑final class for isClassFinal() tests. */
    static class NonFinalClass {}

    /* --------------------------------------------------------------------- *
     *  Test addPermanentClassAlias / removePermanentClassAlias / forName      *
     * --------------------------------------------------------------------- */

    @Test
    public void testAddAndRemovePermanentClassAlias() {
        // Alias a known class (Date) with a custom name
        String alias = "myDateAlias";
        ClassUtilities.addPermanentClassAlias(Date.class, alias);

        try {
            // forName should resolve the alias to the Date class
            Class<?> resolved = ClassUtilities.forName(alias, this.getClass().getClassLoader());
            assertNotNull("Alias should resolve to a class", resolved);
            assertEquals(Date.class, resolved);
        } finally {
            // Clean up to avoid side effects on other tests
            ClassUtilities.removePermanentClassAlias(alias);
        }

        // After removal the alias is unknown → forName returns null
        assertNull("Removed alias should no longer resolve",
                ClassUtilities.forName(alias, this.getClass().getClassLoader()));
    }

    @Test
    public void testForNameWithNullOrEmpty() {
        assertNull("null name should return null", ClassUtilities.forName(null, getClass().getClassLoader()));
        assertNull("empty string should return null", ClassUtilities.forName("", getClass().getClassLoader()));
    }

    @Test
    public void testForNameWithStandardClass() {
        Class<?> clazz = ClassUtilities.forName("java.util.Date", getClass().getClassLoader());
        assertEquals(Date.class, clazz);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testForNameSecurityExceptionIsWrapped() {
        // ClassLoader, Process, Constructor, etc. are blocked by internalClassForName()
        // The call must result in an IllegalArgumentException wrapping a SecurityException.
        ClassUtilities.forName("java.lang.ClassLoader", getClass().getClassLoader());
    }

    @Test
    public void testForNameArrayTypes() {
        // Primitive int[] – descriptor "[I"
        Class<?> intArray = ClassUtilities.forName("[I", getClass().getClassLoader());
        assertEquals(int[].class, intArray);

        // Multi‑dimensional int[][] – descriptor "[[I"
        Class<?> int2dArray = ClassUtilities.forName("[[I", getClass().getClassLoader());
        assertEquals(int[][].class, int2dArray);

        // Object array String[][] – descriptor "[[Ljava.lang.String;"
        Class<?> str2dArray = ClassUtilities.forName("[[Ljava.lang.String;", getClass().getClassLoader());
        assertEquals(String[][].class, str2dArray);
    }

    /* --------------------------------------------------------------------- *
     *  Test computeInheritanceDistance                                         *
     * --------------------------------------------------------------------- */

    @Test
    public void testComputeInheritanceDistanceSameClass() {
        assertEquals(0, ClassUtilities.computeInheritanceDistance(String.class, String.class));
    }

    @Test
    public void testComputeInheritanceDistanceSuperclass() {
        // String extends Object
        assertEquals(1, ClassUtilities.computeInheritanceDistance(String.class, Object.class));
    }

    @Test
    public void testComputeInheritanceDistanceInterface() {
        // C implements B, B extends A → distance from C to A should be 2
        assertEquals(2, ClassUtilities.computeInheritanceDistance(C.class, A.class));
        // Direct interface implementation distance is 1
        assertEquals(1, ClassUtilities.computeInheritanceDistance(C.class, B.class));
    }

    @Test
    public void testComputeInheritanceDistanceUnrelated() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, Integer.class));
    }

    @Test
    public void testComputeInheritanceDistanceWithPrimitivesAndWrappers() {
        // Primitive to wrapper (int -> Integer) – should be 0 per implementation
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, Integer.class));
        // Wrapper to primitive (Integer -> int) – also 0
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Integer.class, int.class));

        // Different primitive types – no relationship
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, long.class));
    }

    @Test
    public void testComputeInheritanceDistanceNullArguments() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, Object.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(Object.class, null));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, null));
    }

    /* --------------------------------------------------------------------- *
     *  Test isPrimitive                                                       *
     * --------------------------------------------------------------------- */

    @Test
    public void testIsPrimitiveWithPrimitiveAndWrapper() {
        assertTrue(ClassUtilities.isPrimitive(int.class));
        assertTrue(ClassUtilities.isPrimitive(Integer.class));
        assertTrue(ClassUtilities.isPrimitive(void.class));
        assertTrue(ClassUtilities.isPrimitive(Void.class));
        assertFalse(ClassUtilities.isPrimitive(String.class));
        assertFalse(ClassUtilities.isPrimitive(Object.class));
    }

    /* --------------------------------------------------------------------- *
     *  Test isClassFinal                                                      *
     * --------------------------------------------------------------------- */

    @Test
    public void testIsClassFinal() {
        // String is final
        assertTrue(ClassUtilities.isClassFinal(String.class));
        // Non‑final class
        assertFalse(ClassUtilities.isClassFinal(NonFinalClass.class));
        // Primitive types are not final in the Java sense (they are not classes)
        assertFalse(ClassUtilities.isClassFinal(int.class));
    }

    /* --------------------------------------------------------------------- *
     *  Test areAllConstructorsPrivate                                          *
     * --------------------------------------------------------------------- */

    @Test
    public void testAreAllConstructorsPrivate() {
        // ClassUtilities has a public default constructor → should be false
        assertFalse(ClassUtilities.areAllConstructorsPrivate(ClassUtilities.class));

        // PrivateCtorOnly has only private constructors → true
        assertTrue(ClassUtilities.areAllConstructorsPrivate(PrivateCtorOnly.class));
    }

    /* --------------------------------------------------------------------- *
     *  Test toPrimitiveWrapperClass                                            *
     * --------------------------------------------------------------------- */

    @Test
    public void testToPrimitiveWrapperClass() {
        // Primitive -> wrapper
        assertEquals(Integer.class, ClassUtilities.toPrimitiveWrapperClass(int.class));
        assertEquals(void.class, ClassUtilities.toPrimitiveWrapperClass(void.class)); // void maps to Void, but method returns Void.class; test both
        assertEquals(Void.class, ClassUtilities.toPrimitiveWrapperClass(void.class));

        // Non‑primitive is returned unchanged
        assertEquals(String.class, ClassUtilities.toPrimitiveWrapperClass(String.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToPrimitiveWrapperClassWithNonPrimitive() {
        // Passing a non‑primitive that is not in the map should throw
        ClassUtilities.toPrimitiveWrapperClass(Object.class);
    }

    /* --------------------------------------------------------------------- *
     *  Test doesOneWrapTheOther                                                *
     * --------------------------------------------------------------------- */

    @Test
    public void testDoesOneWrapTheOther() {
        assertTrue(ClassUtilities.doesOneWrapTheOther(int.class, Integer.class));
        assertTrue(ClassUtilities.doesOneWrapTheOther(Integer.class, int.class));

        assertFalse(ClassUtilities.doesOneWrapTheOther(String.class, String.class));
        assertFalse(ClassUtilities.doesOneWrapTheOther(int.class, String.class));
    }

    /* --------------------------------------------------------------------- *
     *  Test getClassLoader                                                    *
     * --------------------------------------------------------------------- */

    @Test
    public void testGetClassLoaderReturnsNonNull() {
        ClassLoader cl = ClassUtilities.getClassLoader();
        assertNotNull("getClassLoader() must never return null", cl);
    }

    @Test
    public void testGetClassLoaderPrefersThreadContext() {
        // Install a custom ThreadContextClassLoader and verify it is chosen
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try {
            ClassLoader custom = new ClassLoader() {};
            Thread.currentThread().setContextClassLoader(custom);
            ClassLoader result = ClassUtilities.getClassLoader();
            assertSame("Thread context ClassLoader should be preferred", custom, result);
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

    /* --------------------------------------------------------------------- *
     *  Additional sanity checks for internal caches (optional)                *
     * --------------------------------------------------------------------- */

    @Test
    public void testAliasCacheDoesNotLeakBetweenCalls() {
        // Ensure that adding an alias does not affect unrelated look‑ups
        String alias = "tempAlias";
        ClassUtilities.addPermanentClassAlias(String.class, alias);

        try {
            // Resolve a different name – should still be the original class
            assertEquals(Integer.class,
                    ClassUtilities.forName("java.lang.Integer", getClass().getClassLoader()));
        } finally {
            ClassUtilities.removePermanentClassAlias(alias);
        }
    }
}
