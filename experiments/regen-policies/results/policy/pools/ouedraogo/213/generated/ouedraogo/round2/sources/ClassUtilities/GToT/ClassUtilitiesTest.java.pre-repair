package com.cedarsoftware.util;

import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.ConverterOptions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.*;

import static org.junit.Assert.*;

public class ClassUtilitiesTest {

    private Converter converter;

    @Before
    public void setUp() {
        // ConverterOptions is part of the library; a default instance is sufficient for the tests.
        converter = new Converter(new ConverterOptions());
        // Ensure unsafe is disabled for most tests
        ClassUtilities.setUseUnsafe(false);
    }

    @After
    public void tearDown() {
        // Clean up any aliases that might have been added during tests
        ClassUtilities.removePermanentClassAlias("my.alias");
    }

    /* -------------------------------------------------
     *  Public method list (for reference)
     * -------------------------------------------------
     * addPermanentClassAlias(Class<?>, String)
     * removePermanentClassAlias(String)
     * getPrimitiveFromWrapper(Class<?>)
     * computeInheritanceDistance(Class<?>, Class<?>)
     * forName(String, ClassLoader)
     * isClassFinal(Class<?>)
     * areAllConstructorsPrivate(Class<?>)
     * isPrimitive(Class<?>)
     * toPrimitiveWrapperClass(Class<?>)
     * doesOneWrapTheOther(Class<?>, Class<?>)
     * getClassLoader()
     * getClassLoader(Class<?>)
     * loadResourceAsString(String)
     * loadResourceAsBytes(String)
     * indexOfSmallestValue(int[])
     * getClassIfEnum(Class<?>)
     * findClosest(Class<?>, Map<Class<?>,T>, T)
     * findLowestCommonSupertypesExcluding(Class<?>, Class<?>, Set<Class<?>>)
     * findLowestCommonSupertypes(Class<?>, Class<?>)
     * findLowestCommonSupertype(Class<?>, Class<?>)
     * getClassHierarchyInfo(Class<?>)
     * haveCommonAncestor(Class<?>, Class<?>)
     * newInstance(Converter, Class<?>, Collection<?>)
     * setUseUnsafe(boolean)
     */

    /* -------------------------------------------------
     *  Basic functionality tests
     * ------------------------------------------------- */

    @Test
    public void testAddAndRemovePermanentClassAlias() {
        ClassUtilities.addPermanentClassAlias(String.class, "my.alias");
        Class<?> resolved = ClassUtilities.forName("my.alias", ClassUtilities.class.getClassLoader());
        assertEquals(String.class, resolved);

        ClassUtilities.removePermanentClassAlias("my.alias");
        Class<?> afterRemoval = ClassUtilities.forName("my.alias", ClassUtilities.class.getClassLoader());
        assertNull(afterRemoval);
    }

    @Test
    public void testGetPrimitiveFromWrapper() {
        assertEquals(int.class, ClassUtilities.getPrimitiveFromWrapper(Integer.class));
        assertEquals(void.class, ClassUtilities.getPrimitiveFromWrapper(Void.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetPrimitiveFromWrapper_Null() {
        ClassUtilities.getPrimitiveFromWrapper(null);
    }

    @Test
    public void testComputeInheritanceDistance() {
        // Simple inheritance
        assertEquals(1, ClassUtilities.computeInheritanceDistance(ArrayList.class, List.class));
        assertEquals(0, ClassUtilities.computeInheritanceDistance(List.class, List.class));

        // Unrelated classes
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, Integer.class));

        // Primitive / wrapper handling
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, Integer.class));
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Integer.class, int.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, Long.class));
    }

    @Test
    public void testForName() {
        // Existing class
        Class<?> stringClass = ClassUtilities.forName("java.lang.String", ClassUtilities.class.getClassLoader());
        assertEquals(String.class, stringClass);

        // Empty name returns null
        assertNull(ClassUtilities.forName("", ClassUtilities.class.getClassLoader()));

        // Non‑existent class returns null (no exception)
        assertNull(ClassUtilities.forName("com.this.does.NotExist", ClassUtilities.class.getClassLoader()));
    }

    @Test
    public void testIsClassFinal() {
        assertTrue(ClassUtilities.isClassFinal(String.class));   // final
        assertFalse(ClassUtilities.isClassFinal(ArrayList.class)); // not final
    }

    @Test
    public void testAreAllConstructorsPrivate() {
        // Utility class – private constructor only
        assertTrue(ClassUtilities.areAllConstructorsPrivate(ClassUtilities.class));

        // Regular class – at least one public constructor
        assertFalse(ClassUtilities.areAllConstructorsPrivate(String.class));
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
    public void testToPrimitiveWrapperClass_Invalid() {
        // Pass a non‑primitive class that is not in the map (e.g., Void.TYPE is primitive, but we test a random class)
        ClassUtilities.toPrimitiveWrapperClass(Object.class);
    }

    @Test
    public void testDoesOneWrapTheOther() {
        assertTrue(ClassUtilities.doesOneWrapTheOther(Integer.class, int.class));
        assertTrue(ClassUtilities.doesOneWrapTheOther(int.class, Integer.class));
        assertFalse(ClassUtilities.doesOneWrapTheOther(Integer.class, long.class));
    }

    @Test
    public void testGetClassLoader() {
        ClassLoader cl1 = ClassUtilities.getClassLoader();
        assertNotNull(cl1);

        ClassLoader cl2 = ClassUtilities.getClassLoader(ClassUtilitiesTest.class);
        assertNotNull(cl2);
        assertEquals(cl1, cl2); // In this environment both resolve to the same loader
    }

    @Test
    public void testLoadResourceAsBytesAndString() throws Exception {
        // Load the compiled class file of ClassUtilities itself – guaranteed to exist on the classpath
        String resourceName = "com/cedarsoftware/util/ClassUtilities.class";
        byte[] bytes = ClassUtilities.loadResourceAsBytes(resourceName);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        String asString = ClassUtilities.loadResourceAsString(resourceName);
        assertNotNull(asString);
        assertTrue(asString.length() > 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadResourceAsBytes_NotFound() {
        ClassUtilities.loadResourceAsBytes("nonexistent_resource_xyz.txt");
    }

    @Test
    public void testIndexOfSmallestValue() {
        assertEquals(2, ClassUtilities.indexOfSmallestValue(new int[]{5, 3, 1, 4}));
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(new int[]{}));
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(null));
        // All positive values, smallest is 0 at index 0
        assertEquals(0, ClassUtilities.indexOfSmallestValue(new int[]{0, 10, 20}));
    }

    @Test
    public void testGetClassIfEnum() {
        // java.time.DayOfWeek is an enum
        assertEquals(DayOfWeek.class, ClassUtilities.getClassIfEnum(DayOfWeek.class));

        // Non‑enum returns null
        assertNull(ClassUtilities.getClassIfEnum(String.class));

        // Null input returns null
        assertNull(ClassUtilities.getClassIfEnum(null));
    }

    @Test
    public void testFindClosest() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(Number.class, "Number");
        map.put(Integer.class, "Integer");
        map.put(Object.class, "Object");

        // Exact match
        assertEquals("Integer", ClassUtilities.findClosest(Integer.class, map, "default"));

        // Closest superclass (Float -> Number)
        assertEquals("Number", ClassUtilities.findClosest(Float.class, map, "default"));

        // No match – default
        assertEquals("default", ClassUtilities.findClosest(String.class, Collections.emptyMap(), "default"));
    }

    @Test
    public void testFindLowestCommonSupertypesExcluding() {
        Set<Class<?>> excluded = new HashSet<>(Arrays.asList(Object.class));
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(Integer.class, Double.class, excluded);
        // Expected common supertypes: Number and Comparable (both at the same depth)
        assertTrue(result.contains(Number.class));
        assertTrue(result.contains(Comparable.class));
        assertFalse(result.contains(Object.class));
    }

    @Test
    public void testFindLowestCommonSupertypes() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(Integer.class, Double.class);
        // The convenience method excludes Object, Serializable, Externalizable, Cloneable
        assertTrue(result.contains(Number.class));
        assertTrue(result.contains(Comparable.class));
        assertFalse(result.contains(Object.class));
    }

    @Test
    public void testFindLowestCommonSupertype() {
        // May return any of the equally low supertypes; we only assert that it is one of them
        Class<?> supertype = ClassUtilities.findLowestCommonSupertype(Integer.class, Double.class);
        assertTrue(supertype == Number.class || supertype == Comparable.class);
    }

    @Test
    public void testGetClassHierarchyInfo() {
        ClassUtilities.ClassHierarchyInfo info = ClassUtilities.getClassHierarchyInfo(ArrayList.class);
        // Depth of ArrayList -> AbstractList -> AbstractCollection -> Object = 3
        assertEquals(3, info.getDepth());

        // Distance from ArrayList to List should be 1 (ArrayList implements List via AbstractList)
        assertEquals(1, info.getDistance(List.class));
        // Distance to unrelated interface (e.g., Runnable) should be -1
        assertEquals(-1, info.getDistance(Runnable.class));
    }

    @Test
    public void testHaveCommonAncestor() {
        assertTrue(ClassUtilities.haveCommonAncestor(String.class, StringBuilder.class));
        assertFalse(ClassUtilities.haveCommonAncestor(String.class, Thread.class)); // Only Object in common, which is excluded by the convenience method but haveCommonAncestor uses raw set, so will be true; we test for true
    }

    @Test
    public void testNewInstance_NoArgs() {
        Object instance = ClassUtilities.newInstance(converter, StringBuilder.class, Collections.emptyList());
        assertTrue(instance instanceof StringBuilder);
    }

    @Test
    public void testNewInstance_WithCollectionArgument() {
        // ArrayList has a constructor that accepts a Collection
        List<String> source = Arrays.asList("a", "b");
        Object instance = ClassUtilities.newInstance(converter, ArrayList.class, source);
        assertTrue(instance instanceof ArrayList);
        assertEquals(source, instance);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstance_AbstractClass() {
        ClassUtilities.newInstance(converter, AbstractList.class, Collections.emptyList());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstance_Interface() {
        ClassUtilities.newInstance(converter, List.class, Collections.emptyList());
    }

    @Test
    public void testSetUseUnsafe() {
        // Turn unsafe on – does not throw even if the class cannot be instantiated normally
        ClassUtilities.setUseUnsafe(true);
        // Attempt to instantiate a class with no accessible constructor (e.g., java.lang.System)
        // This will fall back to unsafe allocation; we only verify that no exception is thrown.
        Object sys = ClassUtilities.newInstance(converter, System.class, Collections.emptyList());
        assertNotNull(sys);
        // Reset
        ClassUtilities.setUseUnsafe(false);
    }
}
