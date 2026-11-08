package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.io.InputStream;
import java.net.URL;
import java.util.*;

import static org.junit.Assert.*;

public class ClassUtilitiesTest {

    private static final String TEST_RESOURCE = "java/lang/Object.class";

    @Before
    public void setUp() {
        // Ensure any previous aliases are cleared for a clean start
        ClassUtilities.removePermanentClassAlias("myStringAlias");
    }

    @After
    public void tearDown() {
        ClassUtilities.removePermanentClassAlias("myStringAlias");
    }

    /* -------------------------------------------------------------
     *  Tests for class alias registration (addPermanentClassAlias / removePermanentClassAlias)
     * ------------------------------------------------------------- */
    @Test
    public void testAddAndRemovePermanentClassAlias() {
        // Register an alias for String
        ClassUtilities.addPermanentClassAlias(String.class, "myStringAlias");
        Class<?> c = ClassUtilities.forName("myStringAlias", ClassUtilities.getClassLoader());
        assertEquals(String.class, c);

        // Remove the alias and verify that lookup fails (returns null)
        ClassUtilities.removePermanentClassAlias("myStringAlias");
        Class<?> removed = ClassUtilities.forName("myStringAlias", ClassUtilities.getClassLoader());
        assertNull(removed);
    }

    /* -------------------------------------------------------------
     *  Tests for computeInheritanceDistance
     * ------------------------------------------------------------- */
    @Test
    public void testComputeInheritanceDistanceSimple() {
        // Direct superclass
        assertEquals(1, ClassUtilities.computeInheritanceDistance(ArrayList.class, List.class));

        // Interface to super‑interface
        assertEquals(1, ClassUtilities.computeInheritanceDistance(List.class, Collection.class));

        // No relationship
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(Object.class, String.class));
    }

    @Test
    public void testComputeInheritanceDistanceWithPrimitives() {
        // Primitive to wrapper
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, Integer.class));
        // Wrapper to primitive (reverse)
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Integer.class, int.class));

        // Different primitive types
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, Long.class));
    }

    /* -------------------------------------------------------------
     *  Tests for isPrimitive and related wrapper handling
     * ------------------------------------------------------------- */
    @Test
    public void testIsPrimitive() {
        assertTrue(ClassUtilities.isPrimitive(int.class));
        assertTrue(ClassUtilities.isPrimitive(Integer.class));
        assertFalse(ClassUtilities.isPrimitive(String.class));
    }

    @Test
    public void testToPrimitiveWrapperClass() {
        // Primitive conversion
        assertEquals(Integer.class, ClassUtilities.toPrimitiveWrapperClass(int.class));
        assertEquals(Void.class, ClassUtilities.toPrimitiveWrapperClass(void.class));

        // Non‑primitive returns itself
        assertEquals(String.class, ClassUtilities.toPrimitiveWrapperClass(String.class));

        // Unknown primitive should throw
        try {
            ClassUtilities.toPrimitiveWrapperClass(void.class); // void is known, so use a non‑primitive class that is not in map
            // No exception expected here
        } catch (IllegalArgumentException e) {
            fail("Should not have thrown for void.class");
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToPrimitiveWrapperClassWithInvalidPrimitive() {
        // Pass a class that is not a primitive but is also not in the map
        ClassUtilities.toPrimitiveWrapperClass(Object.class);
    }

    @Test
    public void testDoesOneWrapTheOther() {
        assertTrue(ClassUtilities.doesOneWrapTheOther(Integer.class, int.class));
        assertTrue(ClassUtilities.doesOneWrapTheOther(int.class, Integer.class));
        assertFalse(ClassUtilities.doesOneWrapTheOther(String.class, int.class));
    }

    /* -------------------------------------------------------------
     *  Tests for class finality and constructor visibility
     * ------------------------------------------------------------- */
    @Test
    public void testIsClassFinal() {
        assertTrue(ClassUtilities.isClassFinal(String.class)); // String is final
        assertFalse(ClassUtilities.isClassFinal(ArrayList.class)); // not final
    }

    // Helper class with a private constructor for testing
    private static final class PrivateConstructorClass {
        private PrivateConstructorClass() {}
    }

    @Test
    public void testAreAllConstructorsPrivate() {
        assertTrue(ClassUtilities.areAllConstructorsPrivate(PrivateConstructorClass.class));
        assertFalse(ClassUtilities.areAllConstructorsPrivate(String.class));
    }

    /* -------------------------------------------------------------
     *  Tests for findClosest
     * ------------------------------------------------------------- */
    @Test
    public void testFindClosestExactMatch() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(Number.class, "Number");
        map.put(Integer.class, "Integer");

        String result = ClassUtilities.findClosest(Integer.class, map, "default");
        assertEquals("Integer", result); // exact match should win
    }

    @Test
    public void testFindClosestByInheritanceDistance() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(Number.class, "Number");
        map.put(List.class, "List");

        // Long extends Number, but does not implement List
        String result = ClassUtilities.findClosest(Long.class, map, "default");
        assertEquals("Number", result);
    }

    /* -------------------------------------------------------------
     *  Tests for resource loading utilities
     * ------------------------------------------------------------- */
    @Test
    public void testLoadResourceAsBytesExisting() {
        byte[] data = ClassUtilities.loadResourceAsBytes(TEST_RESOURCE);
        assertNotNull(data);
        assertTrue(data.length > 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadResourceAsBytesNonExisting() {
        ClassUtilities.loadResourceAsBytes("nonexistent/resource.xyz");
    }

    @Test
    public void testLoadResourceAsStringExisting() {
        // The .class file is binary, but loading as a string should still produce a non‑null result
        String content = ClassUtilities.loadResourceAsString(TEST_RESOURCE);
        assertNotNull(content);
        assertFalse(content.isEmpty());
    }

    /* -------------------------------------------------------------
     *  Tests for indexOfSmallestValue
     * ------------------------------------------------------------- */
    @Test
    public void testIndexOfSmallestValue() {
        int[] array = {5, 3, 9, 1, 4};
        assertEquals(3, ClassUtilities.indexOfSmallestValue(array));

        // All negative values (should return -1 because of the > -1 check)
        int[] negatives = {-5, -2, -8};
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(negatives));

        // Empty array
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(new int[0]));
    }

    /* -------------------------------------------------------------
     *  Tests for enum detection
     * ------------------------------------------------------------- */
    private enum MyEnum { A, B }

    @Test
    public void testGetClassIfEnum() {
        assertEquals(MyEnum.class, ClassUtilities.getClassIfEnum(MyEnum.class));
        assertNull(ClassUtilities.getClassIfEnum(String.class));
    }

    /* -------------------------------------------------------------
     *  Tests for lowest common supertype utilities
     * ------------------------------------------------------------- */
    @Test
    public void testFindLowestCommonSupertypesExcluding() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(
                TreeSet.class,
                HashSet.class,
                Collections.singleton(Object.class));

        // Both are Set implementations; Set should be the lowest common supertype (excluding Object)
        assertTrue(result.contains(Set.class));
        assertFalse(result.contains(Object.class));
    }

    @Test
    public void testFindLowestCommonSupertypes() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(Integer.class, Double.class);
        // Number, Comparable and Serializable are common (Object is excluded automatically)
        assertTrue(result.contains(Number.class));
        assertTrue(result.contains(Comparable.class));
        assertTrue(result.contains(Serializable.class));
        assertFalse(result.contains(Object.class));
    }

    @Test
    public void testFindLowestCommonSupertype() {
        Class<?> supertype = ClassUtilities.findLowestCommonSupertype(Integer.class, Double.class);
        // Could be any of the common supertypes; ensure it is one of the expected ones
        Set<Class<?>> expected = new HashSet<>(Arrays.asList(Number.class, Comparable.class, Serializable.class));
        assertTrue(expected.contains(supertype));
    }

    @Test
    public void testGetAllSupertypes() {
        Set<Class<?>> supertypes = ClassUtilities.getAllSupertypes(ArrayList.class);
        // Must contain the class itself, its superclasses, and interfaces
        assertTrue(supertypes.contains(ArrayList.class));
        assertTrue(supertypes.contains(AbstractList.class));
        assertTrue(supertypes.contains(List.class));
        assertTrue(supertypes.contains(RandomAccess.class));
        assertTrue(supertypes.contains(Cloneable.class));
        assertTrue(supertypes.contains(Serializable.class));
    }

    @Test
    public void testHaveCommonAncestor() {
        // String and Integer share Serializable and Comparable (Object excluded)
        assertTrue(ClassUtilities.haveCommonAncestor(String.class, Integer.class));

        // Two unrelated classes that only share Object should return false
        class Unrelated1 {}
        class Unrelated2 {}
        assertFalse(ClassUtilities.haveCommonAncestor(Unrelated1.class, Unrelated2.class));
    }

    /* -------------------------------------------------------------
     *  Tests for forName and internal class loading
     * ------------------------------------------------------------- */
    @Test
    public void testForNameWithAlias() {
        ClassUtilities.addPermanentClassAlias(List.class, "myListAlias");
        Class<?> clazz = ClassUtilities.forName("myListAlias", ClassUtilities.getClassLoader());
        assertEquals(List.class, clazz);
    }

    @Test
    public void testForNameUnknownReturnsNull() {
        assertNull(ClassUtilities.forName("com.unknown.NonExistentClass", ClassUtilities.getClassLoader()));
    }

    @Test
    public void testGetClassLoaderReturnsNonNull() {
        ClassLoader cl = ClassUtilities.getClassLoader();
        assertNotNull(cl);
    }
}
