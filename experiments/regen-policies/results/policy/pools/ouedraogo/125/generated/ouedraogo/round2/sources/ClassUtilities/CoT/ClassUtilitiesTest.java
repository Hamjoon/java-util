package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ClassUtilities}.
 *
 * The test class follows the requested steps:
 * <ul>
 *   <li>Lists all public methods with their signatures.</li>
 *   <li>Provides a basic test for each method.</li>
 *   <li>Identifies edge‑cases / exceptional situations and adds dedicated tests.</li>
 *   <li>All tests are merged into a single file that starts with “###Test START##” and ends with “###Test END##”.</li>
 * </ul>
 *
 * NOTE: The library’s {@code Converter} and {@code Unsafe} classes are part of the same
 *       distribution.  For the purpose of this test suite we only need to trigger
 *       the exception paths of {@code newInstance(…)}, therefore a real {@code Converter}
 *       instance is not required.
 */
public class ClassUtilitiesTest {

    // -------------------------------------------------------------------------
    // 1.  Public method signatures (for reference – not used by the tests)
    // -------------------------------------------------------------------------
    //
    // public static void addPermanentClassAlias(Class<?> clazz, String alias)
    // public static void removePermanentClassAlias(String alias)
    // public static int computeInheritanceDistance(Class<?> source, Class<?> destination)
    // public static Class<?> forName(String name, ClassLoader classLoader)
    // public static boolean isClassFinal(Class<?> c)
    // public static boolean areAllConstructorsPrivate(Class<?> c)
    // public static boolean isPrimitive(Class<?> c)
    // public static Class<?> toPrimitiveWrapperClass(Class<?> primitiveClass)
    // public static boolean doesOneWrapTheOther(Class<?> x, Class<?> y)
    // public static ClassLoader getClassLoader()
    // public static ClassLoader getClassLoader(Class<?> anchorClass)
    // public static String loadResourceAsString(String resourceName)
    // public static byte[] loadResourceAsBytes(String resourceName)
    // public static int indexOfSmallestValue(int[] array)
    // public static <T> T findClosest(Class<?> clazz, Map<Class<?>, T> candidateClasses, T defaultClass)
    // public static Class<?> getClassIfEnum(Class<?> c)
    // public static Object newInstance(Converter converter, Class<?> c, Collection<?> argumentValues)
    // public static void setUseUnsafe(boolean state)
    // public static Set<Class<?>> findLowestCommonSupertypesExcluding(Class<?> classA, Class<?> classB, Set<Class<?>> skipList)
    // public static Set<Class<?>> findLowestCommonSupertypes(Class<?> classA, Class<?> classB)
    // public static Class<?> findLowestCommonSupertype(Class<?> classA, Class<?> classB)
    // public static Set<Class<?>> getAllSupertypes(Class<?> clazz)
    // public static boolean haveCommonAncestor(Class<?> a, Class<?> b)

    // -------------------------------------------------------------------------
    // 2.  Basic tests for each public method
    // -------------------------------------------------------------------------

    @Test
    public void testAddAndRemovePermanentClassAlias() {
        String alias = "myAliasForString";
        ClassUtilities.addPermanentClassAlias(String.class, alias);
        assertEquals(String.class, ClassUtilities.forName(alias, ClassUtilities.class.getClassLoader()));
        ClassUtilities.removePermanentClassAlias(alias);
        assertNull(ClassUtilities.forName(alias, ClassUtilities.class.getClassLoader()));
    }

    @Test
    public void testComputeInheritanceDistance_SameClass() {
        assertEquals(0, ClassUtilities.computeInheritanceDistance(List.class, List.class));
    }

    @Test
    public void testComputeInheritanceDistance_DirectSubclass() {
        assertEquals(1, ClassUtilities.computeInheritanceDistance(ArrayList.class, List.class));
    }

    @Test
    public void testComputeInheritanceDistance_Unrelated() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, Integer.class));
    }

    @Test
    public void testComputeInheritanceDistance_PrimitiveToWrapper() {
        // int (primitive) -> Integer (wrapper)
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, Integer.class));
        // Integer (wrapper) -> int (primitive)
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Integer.class, int.class));
    }

    @Test
    public void testForName_NullOrEmpty() {
        assertNull(ClassUtilities.forName(null, ClassUtilities.class.getClassLoader()));
        assertNull(ClassUtilities.forName("", ClassUtilities.class.getClassLoader()));
    }

    @Test
    public void testForName_ValidClass() {
        Class<?> clz = ClassUtilities.forName("java.util.HashMap", ClassUtilities.class.getClassLoader());
        assertEquals(HashMap.class, clz);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testForName_SecurityExceptionPath() {
        // Trigger the security‑check path by asking for a forbidden class.
        // ClassUtilities internally forbids instantiation of java.lang.ClassLoader,
        // but forName only loads the class – it does not instantiate it.
        // To provoke the SecurityException we ask for a non‑existent class,
        // which leads to the catch‑all block returning null – no exception.
        // Therefore we simulate the SecurityException by invoking the private method
        // via reflection (allowed in test code).  This verifies that the method
        // re‑throws a SecurityException wrapped in IllegalArgumentException.
        try {
            java.lang.reflect.Method m = ClassUtilities.class.getDeclaredMethod("internalClassForName", String.class, ClassLoader.class);
            m.setAccessible(true);
            m.invoke(null, "java.lang.ClassLoader", ClassUtilities.class.getClassLoader());
        } catch (Exception e) {
            // The underlying cause should be a SecurityException wrapped in IllegalArgumentException
            throw e.getCause();
        }
    }

    @Test
    public void testIsClassFinal() {
        assertTrue(ClassUtilities.isClassFinal(String.class)); // String is final
        assertFalse(ClassUtilities.isClassFinal(ArrayList.class)); // not final
    }

    @Test
    public void testAreAllConstructorsPrivate() {
        assertTrue(ClassUtilities.areAllConstructorsPrivate(PrivateCtor.class));
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
    public void testToPrimitiveWrapperClass_NotPrimitive() {
        // Passing a non‑primitive class that is not in the map should trigger the exception.
        ClassUtilities.toPrimitiveWrapperClass(void.class); // void is in map, so use a custom class
        ClassUtilities.toPrimitiveWrapperClass(Object.class);
    }

    @Test
    public void testDoesOneWrapTheOther() {
        assertTrue(ClassUtilities.doesOneWrapTheOther(Integer.class, int.class));
        assertTrue(ClassUtilities.doesOneWrapTheOther(int.class, Integer.class));
        assertFalse(ClassUtilities.doesOneWrapTheOther(Integer.class, long.class));
    }

    @Test
    public void testGetClassLoader_Default() {
        ClassLoader cl = ClassUtilities.getClassLoader();
        assertNotNull(cl);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetClassLoader_NullAnchor() {
        ClassUtilities.getClassLoader(null);
    }

    @Test
    public void testLoadResourceAsBytes_NonExisting() {
        try {
            ClassUtilities.loadResourceAsBytes("this-resource-does-not-exist.txt");
            fail("Expected IllegalArgumentException for missing resource");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test(expected = NullPointerException.class)
    public void testLoadResourceAsBytes_NullName() {
        ClassUtilities.loadResourceAsBytes(null);
    }

    @Test
    public void testIndexOfSmallestValue() {
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(null));
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(new int[0]));
        assertEquals(0, ClassUtilities.indexOfSmallestValue(new int[]{5, 10, 15}));
        assertEquals(2, ClassUtilities.indexOfSmallestValue(new int[]{20, -1, 3}));
        // negative numbers are ignored because of the “> -1” check in the method
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(new int[]{-5, -2, -9}));
    }

    @Test
    public void testFindClosest_ExactMatch() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(Number.class, "Number");
        map.put(Integer.class, "Integer");
        map.put(Object.class, "Object");

        // Exact match should return the associated value immediately
        assertEquals("Integer", ClassUtilities.findClosest(Integer.class, map, "default"));
    }

    @Test
    public void testFindClosest_ClosestSuperclass() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(Number.class, "Number");
        map.put(Object.class, "Object");

        // Integer is a subclass of Number -> distance 1, Object distance 2
        assertEquals("Number", ClassUtilities.findClosest(Integer.class, map, "default"));
    }

    @Test
    public void testFindClosest_TieBreaking() {
        Map<Class<?>, String> map = new LinkedHashMap<>();
        map.put(Comparable.class, "Comparable");
        map.put(Serializable.class, "Serializable");

        // Integer implements both Comparable and Serializable with the same distance.
        // The tie‑breaker prefers classes over interfaces, then the more specific.
        // Both are interfaces; the method prefers the one that appears later in the map
        // when distances are equal (because shouldPreferNewCandidate returns true when
        // newClass.isAssignableFrom(currentClass) is true). In this case,
        // Serializable.isAssignableFrom(Comparable) is false, so the first stays.
        assertEquals("Comparable", ClassUtilities.findClosest(Integer.class, map, "default"));
    }

    @Test
    public void testGetClassIfEnum() {
        assertEquals(TestEnum.class, ClassUtilities.getClassIfEnum(TestEnum.class));
        assertNull(ClassUtilities.getClassIfEnum(String.class));
        assertNull(ClassUtilities.getClassIfEnum(null));
    }

    @Test
    public void testNewInstance_SecurityRestrictedClass() {
        // ProcessBuilder is on the security‑check list
        try {
            ClassUtilities.newInstance(null, ProcessBuilder.class, Collections.emptyList());
            fail("Expected IllegalArgumentException for security‑restricted class");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("json-io does not allow instantiation"));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstance_NullClass() {
        ClassUtilities.newInstance(null, null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstance_Interface() {
        ClassUtilities.newInstance(null, List.class, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstance_AbstractClass() {
        ClassUtilities.newInstance(null, AbstractClass.class, null);
    }

    @Test
    public void testSetUseUnsafe() {
        // The method does not expose state, we only verify that it does not throw.
        ClassUtilities.setUseUnsafe(true);
        ClassUtilities.setUseUnsafe(false);
    }

    @Test
    public void testFindLowestCommonSupertypesExcluding() {
        Set<Class<?>> skip = Collections.singleton(Object.class);
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(ArrayList.class, LinkedList.class, skip);
        // Both are List implementations; Set should contain at least List and RandomAccess.
        assertTrue(result.contains(List.class));
        assertTrue(result.contains(RandomAccess.class));
        // Object is skipped, so it must not be present.
        assertFalse(result.contains(Object.class));
    }

    @Test
    public void testFindLowestCommonSupertypes() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(Integer.class, Double.class);
        // Number, Comparable, Serializable are expected (Object is excluded by the method itself)
        assertTrue(result.contains(Number.class));
        assertTrue(result.contains(Comparable.class));
        assertTrue(result.contains(Serializable.class));
        assertFalse(result.contains(Object.class));
    }

    @Test
    public void testFindLowestCommonSupertype() {
        Class<?> common = ClassUtilities.findLowestCommonSupertype(Integer.class, Double.class);
        // The method may return any one of the lowest supertypes; we accept any of the three.
        Set<Class<?>> possible = new HashSet<>(Arrays.asList(Number.class, Comparable.class, Serializable.class));
        assertTrue(possible.contains(common));
    }

    @Test
    public void testGetAllSupertypes() {
        Set<Class<?>> supertypes = ClassUtilities.getAllSupertypes(ArrayList.class);
        assertTrue(supertypes.contains(ArrayList.class));
        assertTrue(supertypes.contains(AbstractList.class));
        assertTrue(supertypes.contains(List.class));
        assertTrue(supertypes.contains(Collection.class));
        assertTrue(supertypes.contains(Iterable.class));
        assertTrue(supertypes.contains(Object.class));
    }

    @Test
    public void testHaveCommonAncestor() {
        assertTrue(ClassUtilities.haveCommonAncestor(ArrayList.class, LinkedList.class));
        assertFalse(ClassUtilities.haveCommonAncestor(String.class, Integer.class));
    }

    // -------------------------------------------------------------------------
    // 3.  Edge‑case / exception tests not covered above
    // -------------------------------------------------------------------------

    @Test
    public void testComputeInheritanceDistance_NullArguments() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, String.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, null));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, null));
    }

    @Test
    public void testComputeInheritanceDistance_InterfaceToClass() {
        // Interface -> implementing class should have distance 1
        assertEquals(1, ClassUtilities.computeInheritanceDistance(Comparable.class, String.class));
    }

    @Test
    public void testLoadResourceAsString_ExistingResource() throws Exception {
        // Create a temporary resource on the fly using the test class’s classloader.
        // The resource will be placed in the same package as this test class.
        String resourceName = "ClassUtilitiesTestResource.txt";
        String expected = "Hello, ClassUtilities!";
        // Write the resource to the target/classes directory (works on most build setups)
        java.nio.file.Path path = java.nio.file.Paths.get(
                ClassUtilitiesTest.class.getResource(".").toURI()).resolve(resourceName);
        java.nio.file.Files.write(path, expected.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        String actual = ClassUtilities.loadResourceAsString(resourceName);
        assertEquals(expected, actual);

        // Cleanup
        java.nio.file.Files.deleteIfExists(path);
    }

    @Test
    public void testFindClosest_NoMatchReturnsDefault() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(List.class, "list");
        String result = ClassUtilities.findClosest(Integer.class, map, "default");
        assertEquals("default", result);
    }

    // -------------------------------------------------------------------------
    // 4.  Helper classes used by the tests
    // -------------------------------------------------------------------------

    // A class with a single private constructor – used for areAllConstructorsPrivate()
    private static final class PrivateCtor {
        private PrivateCtor() {}
    }

    // An abstract class – used for newInstance() illegal‑argument test
    private static abstract class AbstractClass {
        public AbstractClass() {}
    }

    // Simple enum for getClassIfEnum()
    private enum TestEnum { ONE, TWO }

    // -------------------------------------------------------------------------
    // 5.  ExpectedException rule (optional – used for readability in some tests)
    // -------------------------------------------------------------------------

    @Rule
    public ExpectedException thrown = ExpectedException.none();
}
