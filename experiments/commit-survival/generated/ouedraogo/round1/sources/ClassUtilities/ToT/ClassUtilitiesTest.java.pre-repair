package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit 4 test suite for {@link ClassUtilities}.
 *
 * The tests cover:
 * <ul>
 *   <li>Primitive handling and wrapper conversion</li>
 *   <li>Inheritance distance calculations (including interfaces, super‑classes and primitives)</li>
 *   <li>Alias handling via {@code addPermanentClassAlias} / {@code removePermanentClassAlias}</li>
 *   <li>Class loading via {@code forName} and the internal caching logic</li>
 *   <li>Utility methods: {@code isClassFinal}, {@code areAllConstructorsPrivate},
 *       {@code toPrimitiveWrapperClass}, {@code doesOneWrapTheOther},
 *       {@code getClassLoader}</li>
 *   <li>Edge cases such as null arguments, empty strings, security restrictions and
 *       non‑existent classes.</li>
 * </ul>
 *
 * No external mocking frameworks are used – all objects are created using the
 * public API of {@link ClassUtilities} and standard JDK classes.
 */
public class ClassUtilitiesTest {

    private static final String TEST_ALIAS = "myDateAlias";

    private ClassLoader originalContextClassLoader;

    @Before
    public void setUp() {
        // Preserve the original context class loader so we can restore it after tests.
        originalContextClassLoader = Thread.currentThread().getContextClassLoader();
    }

    @After
    public void tearDown() {
        // Clean up any alias that may have been added during a test.
        ClassUtilities.removePermanentClassAlias(TEST_ALIAS);
        // Restore original context class loader.
        Thread.currentThread().setContextClassLoader(originalContextClassLoader);
    }

    /* ---------------------------------------------------------------------- */
    /*  Primitive / Wrapper handling                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIsPrimitive_WithPrimitivesAndWrappers() {
        // Primitive types
        assertTrue(ClassUtilities.isPrimitive(int.class));
        assertTrue(ClassUtilities.isPrimitive(boolean.class));
        assertTrue(ClassUtilities.isPrimitive(void.class));

        // Wrapper types (treated as primitives by this utility)
        assertTrue(ClassUtilities.isPrimitive(Integer.class));
        assertTrue(ClassUtilities.isPrimitive(Boolean.class));
        assertTrue(ClassUtilities.isPrimitive(Void.class));

        // Non‑primitive, non‑wrapper
        assertFalse(ClassUtilities.isPrimitive(String.class));
        assertFalse(ClassUtilities.isPrimitive(Date.class));
    }

    @Test
    public void testToPrimitiveWrapperClass_ValidPrimitives() {
        assertEquals(Integer.class, ClassUtilities.toPrimitiveWrapperClass(int.class));
        assertEquals(Long.class, ClassUtilities.toPrimitiveWrapperClass(long.class));
        assertEquals(Boolean.class, ClassUtilities.toPrimitiveWrapperClass(boolean.class));
        assertEquals(Void.class, ClassUtilities.toPrimitiveWrapperClass(void.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToPrimitiveWrapperClass_NonPrimitiveThrows() {
        ClassUtilities.toPrimitiveWrapperClass(String.class);
    }

    @Test
    public void testDoesOneWrapTheOther() {
        // Positive cases
        assertTrue(ClassUtilities.doesOneWrapTheOther(int.class, Integer.class));
        assertTrue(ClassUtilities.doesOneWrapTheOther(Integer.class, int.class));
        assertTrue(ClassUtilities.doesOneWrapTheOther(char.class, Character.class));
        assertTrue(ClassUtilities.doesOneWrapTheOther(Boolean.class, boolean.class));

        // Negative cases
        assertFalse(ClassUtilities.doesOneWrapTheOther(int.class, Long.class));
        assertFalse(ClassUtilities.doesOneWrapTheOther(String.class, String.class));
        assertFalse(ClassUtilities.doesOneWrapTheOther(void.class, Void.class)); // void is not in wrapperMap
    }

    /* ---------------------------------------------------------------------- */
    /*  computeInheritanceDistance                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testComputeInheritanceDistance_SameClass() {
        assertEquals(0, ClassUtilities.computeInheritanceDistance(String.class, String.class));
    }

    @Test
    public void testComputeInheritanceDistance_NullArguments() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, Object.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(Object.class, null));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, null));
    }

    @Test
    public void testComputeInheritanceDistance_SuperclassChain() {
        // Integer -> Number -> Object
        assertEquals(1, ClassUtilities.computeInheritanceDistance(Integer.class, Number.class));
        assertEquals(2, ClassUtilities.computeInheritanceDistance(Integer.class, Object.class));

        // ArrayList -> AbstractList -> AbstractCollection -> Object
        assertEquals(1, ClassUtilities.computeInheritanceDistance(java.util.ArrayList.class,
                java.util.AbstractList.class));
        assertEquals(3, ClassUtilities.computeInheritanceDistance(java.util.ArrayList.class,
                Object.class));
    }

    @Test
    public void testComputeInheritanceDistance_InterfaceImplementation() {
        // ArrayList implements List and RandomAccess (both direct)
        assertEquals(1, ClassUtilities.computeInheritanceDistance(java.util.ArrayList.class,
                java.util.List.class));
        assertEquals(1, ClassUtilities.computeInheritanceDistance(java.util.ArrayList.class,
                java.util.RandomAccess.class));

        // HashSet implements Set -> Collection -> Iterable
        assertEquals(1, ClassUtilities.computeInheritanceDistance(HashSet.class, Set.class));
        assertEquals(2, ClassUtilities.computeInheritanceDistance(HashSet.class, java.util.Collection.class));
        assertEquals(3, ClassUtilities.computeInheritanceDistance(HashSet.class, Iterable.class));
    }

    @Test
    public void testComputeInheritanceDistance_NoRelation() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, Integer.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(java.util.Map.class, java.util.Set.class));
    }

    @Test
    public void testComputeInheritanceDistance_PrimitiveAndWrapper() {
        // Primitive source, wrapper destination
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, Integer.class));
        // Wrapper source, primitive destination
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Integer.class, int.class));

        // Different primitive/wrapper pairs should be -1
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, Long.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(double.class, Integer.class));
    }

    /* ---------------------------------------------------------------------- */
    /*  Alias handling (addPermanentClassAlias / removePermanentClassAlias)   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAddAndRemovePermanentClassAlias() {
        // Ensure alias does not exist initially
        assertNull(ClassUtilities.forName(TEST_ALIAS, this.getClass().getClassLoader()));

        // Add alias for java.util.Date
        ClassUtilities.addPermanentClassAlias(Date.class, TEST_ALIAS);
        Class<?> loaded = ClassUtilities.forName(TEST_ALIAS, this.getClass().getClassLoader());
        assertNotNull(loaded);
        assertEquals(Date.class, loaded);

        // Remove alias and verify it is no longer resolvable
        ClassUtilities.removePermanentClassAlias(TEST_ALIAS);
        assertNull(ClassUtilities.forName(TEST_ALIAS, this.getClass().getClassLoader()));
    }

    /* ---------------------------------------------------------------------- */
    /*  forName and internal caching                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testForName_StandardClasses() {
        // Normal class name
        Class<?> stringCls = ClassUtilities.forName("java.lang.String", this.getClass().getClassLoader());
        assertEquals(String.class, stringCls);

        // Primitive alias (registered in static block)
        Class<?> intCls = ClassUtilities.forName("int", this.getClass().getClassLoader());
        assertEquals(int.class, intCls);

        // Wrapper alias via nameToClass map ("string" -> String.class)
        Class<?> aliasCls = ClassUtilities.forName("string", this.getClass().getClassLoader());
        assertEquals(String.class, aliasCls);
    }

    @Test
    public void testForName_ArrayNotation() {
        // int[]
        Class<?> intArray = ClassUtilities.forName("[I", this.getClass().getClassLoader());
        assertTrue(intArray.isArray());
        assertEquals(int.class, intArray.getComponentType());

        // String[] (object array)
        Class<?> stringArray = ClassUtilities.forName("[Ljava.lang.String;", this.getClass().getClassLoader());
        assertTrue(stringArray.isArray());
        assertEquals(String.class, stringArray.getComponentType());

        // Multi‑dimensional array: int[][]
        Class<?> int2D = ClassUtilities.forName("[[I", this.getClass().getClassLoader());
        assertTrue(int2D.isArray());
        assertTrue(int2D.getComponentType().isArray());
        assertEquals(int.class, int2D.getComponentType().getComponentType());
    }

    @Test
    public void testForName_NullOrEmpty() {
        assertNull(ClassUtilities.forName(null, this.getClass().getClassLoader()));
        assertNull(ClassUtilities.forName("", this.getClass().getClassLoader()));
        assertNull(ClassUtilities.forName("   ", this.getClass().getClassLoader()));
    }

    @Test
    public void testForName_NonExistentClassReturnsNull() {
        assertNull(ClassUtilities.forName("com.this.does.not.Exist", this.getClass().getClassLoader()));
    }

    @Test
    public void testForName_SecurityRestrictedClassThrows() {
        // The internalClassForName method forbids loading certain core JDK classes.
        // Attempt to load java.lang.Class (which is allowed) – should succeed.
        Class<?> clazz = ClassUtilities.forName("java.lang.Class", this.getClass().getClassLoader());
        assertEquals(Class.class, clazz);

        // Attempt to load java.lang.reflect.Constructor – should result in null because
        // internalClassForName throws a SecurityException which is caught and converted
        // to null by the public forName method.
        Class<?> ctor = ClassUtilities.forName("java.lang.reflect.Constructor", this.getClass().getClassLoader());
        assertNull(ctor);
    }

    /* ---------------------------------------------------------------------- */
    /*  isClassFinal                                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIsClassFinal() {
        // java.lang.String is final
        assertTrue(ClassUtilities.isClassFinal(String.class));

        // java.util.ArrayList is not final
        assertFalse(ClassUtilities.isClassFinal(java.util.ArrayList.class));
    }

    /* ---------------------------------------------------------------------- */
    /*  areAllConstructorsPrivate                                              */
    /* ---------------------------------------------------------------------- */

    private static final class PrivateCtorOnly {
        private PrivateCtorOnly() {}
    }

    private static class MixedCtor {
        public MixedCtor() {}
        private MixedCtor(int ignored) {}
    }

    @Test
    public void testAreAllConstructorsPrivate_AllPrivate() {
        assertTrue(ClassUtilities.areAllConstructorsPrivate(PrivateCtorOnly.class));
    }

    @Test
    public void testAreAllConstructorsPrivate_NotAllPrivate() {
        assertFalse(ClassUtilities.areAllConstructorsPrivate(MixedCtor.class));
    }

    /* ---------------------------------------------------------------------- */
    /*  getClassLoader (basic non‑OSGi scenarios)                             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetClassLoader_UsesThreadContextIfAvailable() {
        // Replace the thread context class loader with a custom one.
        ClassLoader custom = new ClassLoader() {};
        Thread.currentThread().setContextClassLoader(custom);

        ClassLoader result = ClassUtilities.getClassLoader();
        assertSame(custom, result);
    }

    @Test
    public void testGetClassLoader_FallbackToClassLoaderOfAnchor() {
        // Remove the thread context class loader.
        Thread.currentThread().setContextClassLoader(null);

        ClassLoader result = ClassUtilities.getClassLoader(ClassUtilities.class);
        assertSame(ClassUtilities.class.getClassLoader(), result);
    }

    @Test
    public void testGetClassLoader_FallbackToSystemClassLoader() {
        // Null out both thread context and anchor class loader.
        Thread.currentThread().setContextClassLoader(null);
        // Create a dummy class loaded by the bootstrap (null) loader.
        class BootstrapLoaded {}
        ClassLoader anchorLoader = BootstrapLoaded.class.getClassLoader(); // should be null

        // Use reflection to call the private overload that takes an anchor class.
        // Since the method is public we can call it directly.
        ClassLoader result = ClassUtilities.getClassLoader(BootstrapLoaded.class);
        // If both previous steps were null, result must be the system class loader.
        assertSame(ClassLoader.getSystemClassLoader(), result);
    }

    /* ---------------------------------------------------------------------- */
    /*  Helper to expose internal cache for verification (optional)           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testInternalCacheIsPopulatedAfterForName() throws Exception {
        // Ensure cache is empty for a fresh name
        String freshName = "java.util.HashSet";
        // Remove any prior entry that might have been cached by other tests
        ClassUtilities.removePermanentClassAlias(freshName);

        // Load via forName – this should populate nameToClass cache
        Class<?> cls = ClassUtilities.forName(freshName, this.getClass().getClassLoader());
        assertEquals(HashSet.class, cls);

        // Directly access the private static map via reflection to verify caching
        java.lang.reflect.Field mapField = ClassUtilities.class.getDeclaredField("nameToClass");
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Class<?>> map = (java.util.Map<String, Class<?>>) mapField.get(null);
        assertTrue("Cache should contain the loaded class", map.containsKey(freshName));
        assertEquals(HashSet.class, map.get(freshName));
    }
}
