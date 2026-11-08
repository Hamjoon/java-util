package com.cedarsoftware.util;

import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.ConverterOptions;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.*;

import static org.junit.Assert.*;

public class ClassUtilitiesTest {

    private Converter converter;

    @Before
    public void setUp() {
        // ConverterOptions exists in the library – a default instance is sufficient for the tests.
        converter = new Converter(new ConverterOptions());
    }

    /* ----------------------------------------------------------------------
     * Primitive / Wrapper utilities
     * ---------------------------------------------------------------------- */

    @Test
    public void testGetPrimitiveFromWrapper() {
        assertEquals(int.class, ClassUtilities.getPrimitiveFromWrapper(Integer.class));
        assertEquals(void.class, ClassUtilities.getPrimitiveFromWrapper(Void.class));
        assertNull(ClassUtilities.getPrimitiveFromWrapper(String.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetPrimitiveFromWrapperNull() {
        ClassUtilities.getPrimitiveFromWrapper(null);
    }

    @Test
    public void testIsPrimitive() {
        assertTrue(ClassUtilities.isPrimitive(int.class));
        assertTrue(ClassUtilities.isPrimitive(Integer.class));
        assertFalse(ClassUtilities.isPrimitive(String.class));
    }

    @Test
    public void testToPrimitiveWrapperClass() {
        assertEquals(Integer.class, ClassUtilities.toPrimitiveWrapperClass(int.class));
        assertEquals(String.class, ClassUtilities.toPrimitiveWrapperClass(String.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToPrimitiveWrapperClassInvalid() {
        ClassUtilities.toPrimitiveWrapperClass(void.class); // void is primitive but mapped, should work
        // Use a non‑primitive class that is not primitive – this should throw
        ClassUtilities.toPrimitiveWrapperClass(Object.class);
    }

    @Test
    public void testDoesOneWrapTheOther() {
        assertTrue(ClassUtilities.doesOneWrapTheOther(Integer.class, int.class));
        assertTrue(ClassUtilities.doesOneWrapTheOther(int.class, Integer.class));
        assertFalse(ClassUtilities.doesOneWrapTheOther(Integer.class, long.class));
    }

    /* ----------------------------------------------------------------------
     * Class loading utilities
     * ---------------------------------------------------------------------- */

    @Test
    public void testForNameExistingAlias() {
        // "string" is pre‑registered in the static initializer
        Class<?> cls = ClassUtilities.forName("string", ClassUtilities.class.getClassLoader());
        assertEquals(String.class, cls);
    }

    @Test
    public void testForNameNonExisting() {
        Class<?> cls = ClassUtilities.forName("com.cedarsoftware.util.NonExistingClass", ClassUtilities.class.getClassLoader());
        assertNull(cls);
    }

    @Test
    public void testIsClassFinal() {
        assertTrue(ClassUtilities.isClassFinal(String.class)); // final class
        assertFalse(ClassUtilities.isClassFinal(ArrayList.class)); // not final
    }

    @Test
    public void testAreAllConstructorsPrivate() {
        // Utility class with a private constructor
        assertTrue(ClassUtilities.areAllConstructorsPrivate(ClassUtilities.class));

        // Regular class with a public constructor
        assertFalse(ClassUtilities.areAllConstructorsPrivate(ArrayList.class));
    }

    /* ----------------------------------------------------------------------
     * Inheritance distance
     * ---------------------------------------------------------------------- */

    @Test
    public void testComputeInheritanceDistance() {
        // Direct interface implementation
        int distance = ClassUtilities.computeInheritanceDistance(ArrayList.class, List.class);
        assertEquals(1, distance);

        // Superclass chain
        distance = ClassUtilities.computeInheritanceDistance(ArrayList.class, Object.class);
        assertEquals(1, distance); // ArrayList → Object (via superclass)

        // No relationship
        distance = ClassUtilities.computeInheritanceDistance(String.class, Integer.class);
        assertEquals(-1, distance);
    }

    @Test
    public void testComputeInheritanceDistancePrimitives() {
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, int.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, long.class));
        // Wrapper vs primitive – considered same primitive type
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Integer.class, int.class));
    }

    /* ----------------------------------------------------------------------
     * findClosest
     * ---------------------------------------------------------------------- */

    @Test
    public void testFindClosestExactMatch() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(ArrayList.class, "arraylist");
        map.put(List.class, "list");

        String result = ClassUtilities.findClosest(ArrayList.class, map, "default");
        assertEquals("arraylist", result);
    }

    @Test
    public void testFindClosestInheritanceMatch() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(List.class, "list");
        map.put(Collection.class, "collection");

        // ArrayList implements List directly, so List is closer
        String result = ClassUtilities.findClosest(ArrayList.class, map, "default");
        assertEquals("list", result);
    }

    @Test(expected = NullPointerException.class)
    public void testFindClosestNullClass() {
        ClassUtilities.findClosest(null, Collections.emptyMap(), null);
    }

    @Test(expected = NullPointerException.class)
    public void testFindClosestNullMap() {
        ClassUtilities.findClosest(String.class, null, null);
    }

    /* ----------------------------------------------------------------------
     * Resource loading
     * ---------------------------------------------------------------------- */

    @Test
    public void testLoadResourceAsBytesAndString() {
        // The test resource file is placed under src/test/resources/com/cedarsoftware/util/testResource.txt
        String resourceName = "com/cedarsoftware/util/testResource.txt";

        byte[] bytes = ClassUtilities.loadResourceAsBytes(resourceName);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
        String content = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        assertEquals("Hello, ClassUtilities!\n", content);

        String stringContent = ClassUtilities.loadResourceAsString(resourceName);
        assertEquals(content, stringContent);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadResourceAsBytesNotFound() {
        ClassUtilities.loadResourceAsBytes("nonexistent_resource.txt");
    }

    /* ----------------------------------------------------------------------
     * Deprecated method
     * ---------------------------------------------------------------------- */

    @Test
    public void testIndexOfSmallestValue() {
        int[] data = {5, 2, 9, 1, 3};
        assertEquals(3, ClassUtilities.indexOfSmallestValue(data));

        // No positive values
        int[] allNegative = {-5, -2, -9};
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(allNegative));

        // Null / empty
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(null));
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(new int[0]));
    }

    /* ----------------------------------------------------------------------
     * Enum handling
     * ---------------------------------------------------------------------- */

    private enum SampleEnum { A, B }

    @Test
    public void testGetClassIfEnum() {
        Class<?> enumClass = ClassUtilities.getClassIfEnum(SampleEnum.class);
        assertEquals(SampleEnum.class, enumClass);

        // Non‑enum returns null
        assertNull(ClassUtilities.getClassIfEnum(String.class));
    }

    /* ----------------------------------------------------------------------
     * Lowest common supertypes
     * ---------------------------------------------------------------------- */

    @Test
    public void testFindLowestCommonSupertypesExcluding() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(
                Integer.class, Double.class,
                Collections.singleton(Object.class));

        // Number and Comparable should be present, Object is excluded
        assertTrue(result.contains(Number.class));
        assertTrue(result.contains(Comparable.class));
        assertFalse(result.contains(Object.class));
    }

    @Test
    public void testFindLowestCommonSupertypes() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(Integer.class, Double.class);
        // The same as the previous test but with the default exclusion set
        assertTrue(result.contains(Number.class));
        assertTrue(result.contains(Comparable.class));
        assertFalse(result.contains(Object.class));
    }

    @Test
    public void testFindLowestCommonSupertype() {
        Class<?> common = ClassUtilities.findLowestCommonSupertype(Integer.class, Double.class);
        // Could be Number or Comparable – we only assert that it is one of them
        assertTrue(common == Number.class || common == Comparable.class);
    }

    @Test
    public void testHaveCommonAncestor() {
        assertTrue(ClassUtilities.haveCommonAncestor(Integer.class, Double.class));
        assertFalse(ClassUtilities.haveCommonAncestor(String.class, Integer.class));
    }

    /* ----------------------------------------------------------------------
     * Class hierarchy info
     * ---------------------------------------------------------------------- */

    @Test
    public void testGetClassHierarchyInfoDepth() {
        ClassUtilities.ClassHierarchyInfo info = ClassUtilities.getClassHierarchyInfo(ArrayList.class);
        // Depth should be at least 2 (ArrayList → AbstractList → AbstractCollection → Object)
        assertTrue(info.getDepth() >= 3);
    }

    @Test
    public void testGetClassHierarchyInfoDistanceMap() {
        ClassUtilities.ClassHierarchyInfo info = ClassUtilities.getClassHierarchyInfo(HashMap.class);
        Map<Class<?>, Integer> map = info.getDistanceMap();

        // Direct super‑class
        assertEquals(Integer.valueOf(1), map.get(AbstractMap.class));
        // Interface
        assertTrue(map.containsKey(Map.class));
        assertTrue(map.containsKey(Cloneable.class));
    }

    /* ----------------------------------------------------------------------
     * newInstance
     * ---------------------------------------------------------------------- */

    @Test
    public void testNewInstanceSimpleClass() {
        Object instance = ClassUtilities.newInstance(converter, ArrayList.class, Collections.emptyList());
        assertNotNull(instance);
        assertTrue(instance instanceof ArrayList);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstanceInterface() {
        ClassUtilities.newInstance(converter, List.class, Collections.emptyList());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstanceAbstractClass() {
        ClassUtilities.newInstance(converter, AbstractList.class, Collections.emptyList());
    }

    @Test
    public void testNewInstanceWithArguments() {
        // Provide a collection containing a single Integer to match the constructor of
        // java.util.ArrayDeque (which has a constructor that takes a Collection)
        Collection<Integer> argument = Collections.singletonList(42);
        Object instance = ClassUtilities.newInstance(converter, ArrayDeque.class, argument);
        assertTrue(instance instanceof ArrayDeque);
        // The deque should contain the element we passed
        assertEquals(1, ((ArrayDeque<?>) instance).size());
        assertEquals(Integer.valueOf(42), ((ArrayDeque<?>) instance).peek());
    }

    /* ----------------------------------------------------------------------
     * Security checker (indirectly exercised)
     * ---------------------------------------------------------------------- */

    @Test
    public void testSecurityCheckerBlocksBlockedClass() {
        try {
            // ProcessBuilder is in the blocked list – verifyClass should throw
            ClassUtilities.SecurityChecker.verifyClass(ProcessBuilder.class);
            fail("Expected SecurityException");
        } catch (SecurityException expected) {
            // expected
        }
    }

    /* ----------------------------------------------------------------------
     * getClassLoader
     * ---------------------------------------------------------------------- */

    @Test
    public void testGetClassLoaderReturnsNonNull() {
        ClassLoader cl = ClassUtilities.getClassLoader();
        assertNotNull(cl);
    }

    @Test
    public void testGetClassLoaderWithAnchor() {
        ClassLoader cl = ClassUtilities.getClassLoader(ClassUtilities.class);
        assertNotNull(cl);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetClassLoaderNullAnchor() {
        ClassUtilities.getClassLoader(null);
    }
}
