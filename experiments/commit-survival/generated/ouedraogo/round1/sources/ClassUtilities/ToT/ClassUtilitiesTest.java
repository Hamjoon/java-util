package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ClassUtilities}.
 *
 * The tests cover:
 * <ul>
 *   <li>Alias handling (add / remove)</li>
 *   <li>Inheritance distance calculations (including primitives, wrappers, arrays, interfaces, unrelated classes)</li>
 *   <li>Primitive detection</li>
 *   <li>Class loading via {@code forName} (normal classes, arrays, security‑restricted classes, unknown names)</li>
 *   <li>Class‑modifier utilities (final, private constructors)</li>
 *   <li>Primitive‑wrapper conversion utilities</li>
 *   <li>ClassLoader resolution (including OSGi‑fallback path)</li>
 * </ul>
 *
 * No mocking frameworks are used; only real objects and reflection where necessary.
 */
public class ClassUtilitiesTest {

    private static final String TEST_ALIAS = "myDateAlias";

    /** Preserve the original state of the alias map so that tests are isolated. */
    @Before
    public void setUp() {
        // Ensure the alias does not exist before each test
        ClassUtilities.removePermanentClassAlias(TEST_ALIAS);
    }

    @After
    public void tearDown() {
        // Clean up the alias after each test
        ClassUtilities.removePermanentClassAlias(TEST_ALIAS);
    }

    /* -------------------------------------------------------------
     *  Alias handling
     * ------------------------------------------------------------- */

    @Test
    public void testAddAndRemovePermanentClassAlias() {
        // Alias does not exist initially
        assertNull(ClassUtilities.forName(TEST_ALIAS, ClassUtilities.class.getClassLoader()));

        // Add alias for java.util.Date
        ClassUtilities.addPermanentClassAlias(Date.class, TEST_ALIAS);
        Class<?> aliasResult = ClassUtilities.forName(TEST_ALIAS, ClassUtilities.class.getClassLoader());
        assertNotNull(aliasResult);
        assertEquals(Date.class, aliasResult);

        // Remove alias and verify it is no longer resolvable
        ClassUtilities.removePermanentClassAlias(TEST_ALIAS);
        assertNull(ClassUtilities.forName(TEST_ALIAS, ClassUtilities.class.getClassLoader()));
    }

    /* -------------------------------------------------------------
     *  computeInheritanceDistance
     * ------------------------------------------------------------- */

    @Test
    public void testComputeInheritanceDistance_SameClass() {
        assertEquals(0, ClassUtilities.computeInheritanceDistance(String.class, String.class));
    }

    @Test
    public void testComputeInheritanceDistance_PrimitiveToWrapper() {
        // int primitive -> Integer wrapper (distance 0 because they are considered the same)
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, Integer.class));
        // Wrapper -> primitive
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Integer.class, int.class));
    }

    @Test
    public void testComputeInheritanceDistance_UnrelatedPrimitives() {
        // int primitive vs long primitive – not the same and not wrappers of each other
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, long.class));
    }

    @Test
    public void testComputeInheritanceDistance_InterfaceImplementation() {
        // ArrayList implements List – distance should be 1 (direct interface)
        assertEquals(1, ClassUtilities.computeInheritanceDistance(ArrayList.class, List.class));
    }

    @Test
    public void testComputeInheritanceDistance_SuperclassChain() {
        // Integer extends Number extends Object – distance from Integer to Object is 2
        assertEquals(2, ClassUtilities.computeInheritanceDistance(Integer.class, Object.class));
    }

    @Test
    public void testComputeInheritanceDistance_UnrelatedClasses() {
        // Date and String share only Object as ancestor, but we ask for direct path → should be -1
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(Date.class, String.class));
    }

    @Test
    public void testComputeInheritanceDistance_ArrayTypes() throws Exception {
        // int[][] (class name "[[I") should resolve to int[][].class and have distance 0 to itself
        Class<?> int2D = ClassUtilities.forName("[[I", ClassUtilities.class.getClassLoader());
        assertNotNull(int2D);
        assertEquals(int[][].class, int2D);
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int2D, int2D));
    }

    @Test
    public void testComputeInheritanceDistance_NullParameters() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, String.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, null));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, null));
    }

    /* -------------------------------------------------------------
     *  isPrimitive
     * ------------------------------------------------------------- */

    @Test
    public void testIsPrimitive_PrimitiveAndWrapper() {
        assertTrue(ClassUtilities.isPrimitive(int.class));
        assertTrue(ClassUtilities.isPrimitive(Integer.class));
        assertTrue(ClassUtilities.isPrimitive(void.class));
        assertTrue(ClassUtilities.isPrimitive(Void.class));
    }

    @Test
    public void testIsPrimitive_NonPrimitive() {
        assertFalse(ClassUtilities.isPrimitive(String.class));
        assertFalse(ClassUtilities.isPrimitive(Object.class));
    }

    /* -------------------------------------------------------------
     *  forName and internal class loading
     * ------------------------------------------------------------- */

    @Test
    public void testForName_StandardClass() {
        Class<?> clazz = ClassUtilities.forName("java.util.Date", ClassUtilities.class.getClassLoader());
        assertEquals(Date.class, clazz);
    }

    @Test
    public void testForName_ArrayClass() {
        Class<?> clazz = ClassUtilities.forName("[[Ljava.lang.String;", ClassUtilities.class.getClassLoader());
        assertEquals(String[][].class, clazz);
    }

    @Test
    public void testForName_UnknownClassReturnsNull() {
        assertNull(ClassUtilities.forName("com.this.does.not.Exist", ClassUtilities.class.getClassLoader()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testForName_SecurityRestrictedClassThrows() {
        // java.lang.Class is on the blacklist inside internalClassForName
        ClassUtilities.forName("java.lang.Class", ClassUtilities.class.getClassLoader());
    }

    @Test
    public void testForName_NullOrEmptyNameReturnsNull() {
        assertNull(ClassUtilities.forName(null, ClassUtilities.class.getClassLoader()));
        assertNull(ClassUtilities.forName("", ClassUtilities.class.getClassLoader()));
        assertNull(ClassUtilities.forName("   ", ClassUtilities.class.getClassLoader()));
    }

    /* -------------------------------------------------------------
     *  isClassFinal
     * ------------------------------------------------------------- */

    @Test
    public void testIsClassFinal_FinalAndNonFinal() {
        assertTrue(ClassUtilities.isClassFinal(String.class)); // String is final
        assertFalse(ClassUtilities.isClassFinal(ArrayList.class)); // ArrayList is not final
    }

    /* -------------------------------------------------------------
     *  areAllConstructorsPrivate
     * ------------------------------------------------------------- */

    @Test
    public void testAreAllConstructorsPrivate_PrivateOnly() {
        // java.lang.Math has a single private constructor
        assertTrue(ClassUtilities.areAllConstructorsPrivate(Math.class));
    }

    @Test
    public void testAreAllConstructorsPrivate_PublicExists() {
        // java.util.Date has a public constructor
        assertFalse(ClassUtilities.areAllConstructorsPrivate(Date.class));
    }

    /* -------------------------------------------------------------
     *  toPrimitiveWrapperClass
     * ------------------------------------------------------------- */

    @Test
    public void testToPrimitiveWrapperClass_PrimitiveToWrapper() {
        assertEquals(Integer.class, ClassUtilities.toPrimitiveWrapperClass(int.class));
        assertEquals(Void.class, ClassUtilities.toPrimitiveWrapperClass(void.class));
    }

    @Test
    public void testToPrimitiveWrapperClass_NonPrimitiveReturnsSame() {
        assertEquals(String.class, ClassUtilities.toPrimitiveWrapperClass(String.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToPrimitiveWrapperClass_InvalidPrimitiveThrows() {
        // An artificial primitive class that is not in the map (e.g., char[] is not primitive)
        ClassUtilities.toPrimitiveWrapperClass(char[].class);
    }

    /* -------------------------------------------------------------
     *  doesOneWrapTheOther
     * ------------------------------------------------------------- */

    @Test
    public void testDoesOneWrapTheOther_WrappingPairs() {
        assertTrue(ClassUtilities.doesOneWrapTheOther(int.class, Integer.class));
        assertTrue(ClassUtilities.doesOneWrapTheOther(Integer.class, int.class));
        assertFalse(ClassUtilities.doesOneWrapTheOther(int.class, Long.class));
    }

    /* -------------------------------------------------------------
     *  getClassLoader resolution (non‑OSGi environment)
     * ------------------------------------------------------------- */

    @Test
    public void testGetClassLoader_NoOSGi_ReturnsNonNull() {
        ClassLoader cl = ClassUtilities.getClassLoader();
        assertNotNull(cl);
    }

    @Test
    public void testGetClassLoader_WithAnchorClass_ReturnsSameOrCompatible() {
        ClassLoader cl = ClassUtilities.getClassLoader(ClassUtilities.class);
        assertNotNull(cl);
        // The returned loader should be able to load this test class itself
        try {
            Class<?> loaded = cl.loadClass(this.getClass().getName());
            assertEquals(this.getClass(), loaded);
        } catch (ClassNotFoundException e) {
            fail("Returned ClassLoader could not load the test class");
        }
    }

    /* -------------------------------------------------------------
     *  Internal helper reflection tests (optional but increase coverage)
     * ------------------------------------------------------------- */

    @Test
    public void testInternalClassForName_ReflectionAccess() throws Exception {
        // Use reflection to invoke the private method internalClassForName
        java.lang.reflect.Method m = ClassUtilities.class.getDeclaredMethod(
                "internalClassForName", String.class, ClassLoader.class);
        m.setAccessible(true);
        Class<?> result = (Class<?>) m.invoke(null, "java.util.Date", ClassUtilities.class.getClassLoader());
        assertEquals(Date.class, result);
    }

    @Test
    public void testLoadClass_ArraySignature() throws Exception {
        java.lang.reflect.Method m = ClassUtilities.class.getDeclaredMethod(
                "loadClass", String.class, ClassLoader.class);
        m.setAccessible(true);
        // "[[D" = double[][]
        Class<?> result = (Class<?>) m.invoke(null, "[[D", ClassUtilities.class.getClassLoader());
        assertEquals(double[][].class, result);
    }

    @Test
    public void testGetOSGiClassLoader_ReturnsNullWhenNoOSGi() throws Exception {
        java.lang.reflect.Method m = ClassUtilities.class.getDeclaredMethod(
                "getOSGiClassLoader", Class.class);
        m.setAccessible(true);
        ClassLoader result = (ClassLoader) m.invoke(null, ClassUtilities.class);
        assertNull(result);
    }

    @Test
    public void testGetOSGiClassLoader0_ReturnsNullWhenOSGiAbsent() throws Exception {
        java.lang.reflect.Method m = ClassUtilities.class.getDeclaredMethod(
                "getOSGiClassLoader0", Class.class);
        m.setAccessible(true);
        ClassLoader result = (ClassLoader) m.invoke(null, ClassUtilities.class);
        assertNull(result);
    }

    /* -------------------------------------------------------------
     *  Concurrency map sanity check (ensures class does not corrupt shared state)
     * ------------------------------------------------------------- */

    @Test
    public void testOsgiClassLoaderCachesAreThreadSafe() {
        // The internal caches are ConcurrentHashMap / ConcurrentSet.
        // Access them from multiple threads to ensure no exceptions are thrown.
        Runnable task = () -> {
            ClassLoader cl = ClassUtilities.getClassLoader(ClassUtilities.class);
            assertNotNull(cl);
        };

        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(task);
            threads[i].start();
        }
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                fail("Thread interrupted");
            }
        }
    }
}
