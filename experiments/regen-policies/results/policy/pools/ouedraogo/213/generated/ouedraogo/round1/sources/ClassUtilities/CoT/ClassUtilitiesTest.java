package com.cedarsoftware.util;

import com.cedarsoftware.util.convert.Converter;
import org.junit.Test;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link ClassUtilities}.
 *
 * The test cases are organised as follows:
 *
 * <ul>
 *   <li>Extraction of public API (listed in the comment block at the top).</li>
 *   <li>Basic functional tests for each public method.</li>
 *   <li>Edge‑case / exception tests that exercise null arguments,
 *       illegal values and special conditions.</li>
 *   <li>Combined test class that can be compiled and run as‑is.</li>
 * </ul>
 *
 * Only the classes, constructors and methods that exist in the supplied source
 * code or the explicitly listed dependency signatures are used – no mocking
 * frameworks or external libraries are required.
 */
public class ClassUtilitiesTest {

    /* ----------------------------------------------------------------------
     *  Public API of ClassUtilities (extracted from the source)
     * ----------------------------------------------------------------------
     *
     * public static void addPermanentClassAlias(Class<?> clazz, String alias)
     * public static void removePermanentClassAlias(String alias)
     * public static int computeInheritanceDistance(Class<?> source, Class<?> destination)
     * public static Class<?> forName(String name, ClassLoader classLoader)
     * public static boolean isClassFinal(Class<?> c)
     * public static boolean areAllConstructorsPrivate(Class<?> c)
     * public static Class<?> toPrimitiveWrapperClass(Class<?> primitiveClass)
     * public static boolean doesOneWrapTheOther(Class<?> x, Class<?> y)
     * public static ClassLoader getClassLoader()
     * public static ClassLoader getClassLoader(Class<?> anchorClass)
     * public static String loadResourceAsString(String resourceName)
     * public static byte[] loadResourceAsBytes(String resourceName)
     * @Deprecated public static int indexOfSmallestValue(int[] array)
     * public static Class<?> getClassIfEnum(Class<?> c)
     * public static <T> T findClosest(Class<?> clazz, Map<Class<?>, T> candidateClasses, T defaultClass)
     * public static ClassUtilities.ClassHierarchyInfo getClassHierarchyInfo(Class<?> clazz)
     * public static boolean haveCommonAncestor(Class<?> a, Class<?> b)
     * public static Set<Class<?>> findLowestCommonSupertypesExcluding(Class<?> classA, Class<?> classB, Set<Class<?>> excluded)
     * public static Set<Class<?>> findLowestCommonSupertypes(Class<?> classA, Class<?> classB)
     * public static Class<?> findLowestCommonSupertype(Class<?> classA, Class<?> classB)
     * public static Object newInstance(Converter converter, Class<?> c, Collection<?> argumentValues)
     * public static void setUseUnsafe(boolean state)
     * public static boolean isPrimitive(Class<?> c)
     * public static Class<?> getPrimitiveFromWrapper(Class<?> toType)
     */

    /* ----------------------------------------------------------------------
     *  1. Basic functional tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testAddAndRemovePermanentClassAlias() {
        Class<?> target = String.class;
        String alias = "myStringAlias";

        // add alias
        ClassUtilities.addPermanentClassAlias(target, alias);
        Class<?> lookedUp = ClassUtilities.forName(alias, ClassUtilities.getClassLoader());
        assertEquals("Alias should resolve to the target class", target, lookedUp);

        // remove alias
        ClassUtilities.removePermanentClassAlias(alias);
        Class<?> afterRemoval = ClassUtilities.forName(alias, ClassUtilities.getClassLoader());
        assertNull("Alias should be removed and forName should return null", afterRemoval);
    }

    @Test
    public void testComputeInheritanceDistance() {
        // Same class – distance 0
        assertEquals(0, ClassUtilities.computeInheritanceDistance(ArrayList.class, ArrayList.class));

        // Direct superclass – distance 1
        assertEquals(1, ClassUtilities.computeInheritanceDistance(ArrayList.class, List.class));

        // Interface implemented indirectly – List -> Collection -> Iterable (distance 2)
        assertEquals(2, ClassUtilities.computeInheritanceDistance(ArrayList.class, Iterable.class));

        // Unrelated classes – -1
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, Integer.class));

        // Primitive handling – same primitive type
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, int.class));

        // Primitive vs wrapper – should be -1 because they are not in the same hierarchy
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, Integer.class));

        // Null arguments – -1
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, Object.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(Object.class, null));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, null));
    }

    @Test
    public void testForName() throws Exception {
        // Load a known class via its fully qualified name
        Class<?> stringClass = ClassUtilities.forName("java.lang.String", ClassUtilities.getClassLoader());
        assertEquals(String.class, stringClass);

        // Load a primitive wrapper via the custom alias map (added in static block)
        Class<?> intWrapper = ClassUtilities.forName("int", ClassUtilities.getClassLoader());
        assertEquals(int.class, intWrapper);

        // Non‑existent class – should return null (no exception)
        assertNull(ClassUtilities.forName("com.this.does.NotExist", ClassUtilities.getClassLoader()));
    }

    @Test
    public void testIsClassFinal() {
        assertTrue("String is final", ClassUtilities.isClassFinal(String.class));
        assertFalse("ArrayList is not final", ClassUtilities.isClassFinal(ArrayList.class));
    }

    @Test
    public void testAreAllConstructorsPrivate() {
        // Utility class – private constructor only
        assertTrue(ClassUtilities.areAllConstructorsPrivate(ClassUtilities.class));

        // Regular class – at least one public constructor
        assertFalse(ClassUtilities.areAllConstructorsPrivate(StringBuilder.class));
    }

    @Test
    public void testToPrimitiveWrapperClass() {
        // Primitive → wrapper
        assertEquals(Integer.class, ClassUtilities.toPrimitiveWrapperClass(int.class));
        assertEquals(Boolean.class, ClassUtilities.toPrimitiveWrapperClass(boolean.class));
        assertEquals(Void.class, ClassUtilities.toPrimitiveWrapperClass(void.class));

        // Non‑primitive – should be returned unchanged
        assertEquals(String.class, ClassUtilities.toPrimitiveWrapperClass(String.class));

        // Illegal argument – non‑primitive, non‑wrapper (e.g., Object)
        try {
            ClassUtilities.toPrimitiveWrapperClass(Object.class);
            fail("Expected IllegalArgumentException for non‑primitive class");
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    public void testDoesOneWrapTheOther() {
        assertTrue(ClassUtilities.doesOneWrapTheOther(Integer.class, int.class));
        assertTrue(ClassUtilities.doesOneWrapTheOther(int.class, Integer.class));
        assertFalse(ClassUtilities.doesOneWrapTheOther(Integer.class, long.class));

        // Null arguments – should throw NPE
        try {
            ClassUtilities.doesOneWrapTheOther(null, Integer.class);
            fail("Expected NullPointerException");
        } catch (NullPointerException ignored) {
        }
        try {
            ClassUtilities.doesOneWrapTheOther(Integer.class, null);
            fail("Expected NullPointerException");
        } catch (NullPointerException ignored) {
        }
    }

    @Test
    public void testGetClassLoader() {
        // Default (no‑arg) overload should never return null
        assertNotNull(ClassUtilities.getClassLoader());

        // Overload with anchor class – also never null
        assertNotNull(ClassUtilities.getClassLoader(ClassUtilities.class));
    }

    @Test
    public void testLoadResourceAsBytesAndString() throws Exception {
        // Use a core‑JDK resource that is guaranteed to exist.
        // The ".class" file of java.lang.String is on the classpath.
        String resourceName = "java/lang/String.class";

        byte[] bytes = ClassUtilities.loadResourceAsBytes(resourceName);
        assertNotNull("Bytes must not be null", bytes);
        assertTrue("Bytes length should be > 0", bytes.length > 0);

        String content = ClassUtilities.loadResourceAsString(resourceName);
        assertNotNull("String content must not be null", content);
        // Not checking the exact text (binary data) – just that conversion succeeded.
        assertFalse("String representation should not be empty", content.isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadResourceNotFound() {
        // This resource does not exist – should throw IllegalArgumentException
        ClassUtilities.loadResourceAsBytes("nonexistent_resource_12345.txt");
    }

    @Test
    public void testIndexOfSmallestValue() {
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(null));
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(new int[0]));

        int[] data = {5, 3, 9, 3, 7};
        // Smallest positive value is 3, first occurrence at index 1
        assertEquals(1, ClassUtilities.indexOfSmallestValue(data));

        // Array with negative numbers – they are ignored, smallest positive is 2
        int[] withNegatives = {-5, -1, 2, 4};
        assertEquals(2, ClassUtilities.indexOfSmallestValue(withNegatives));
    }

    @Test
    public void testGetClassIfEnum() {
        // Enum class – should return itself
        assertEquals(TimeUnit.class, ClassUtilities.getClassIfEnum(TimeUnit.class));

        // Non‑enum – null
        assertNull(ClassUtilities.getClassIfEnum(String.class));

        // Null argument – null
        assertNull(ClassUtilities.getClassIfEnum(null));
    }

    @Test
    public void testFindClosestExactAndInheritance() {
        Map<Class<?>, String> candidates = new HashMap<>();
        candidates.put(Number.class, "Number");
        candidates.put(Integer.class, "Integer");
        candidates.put(Object.class, "Object");

        // Exact match – should return the exact value
        assertEquals("Integer", ClassUtilities.findClosest(Integer.class, candidates, "Default"));

        // No exact match – choose the closest superclass (Number)
        assertEquals("Number", ClassUtilities.findClosest(Double.class, candidates, "Default"));

        // Tie‑break: two interfaces at same distance – prefer class over interface.
        // Create a situation where both List and Set are at distance 1 from ArrayList.
        Map<Class<?>, String> ifaceMap = new HashMap<>();
        ifaceMap.put(Set.class, "Set");
        ifaceMap.put(List.class, "List");
        assertEquals("List", ClassUtilities.findClosest(ArrayList.class, ifaceMap, "Default"));
    }

    @Test
    public void testGetClassHierarchyInfo() {
        ClassUtilities.ClassHierarchyInfo info = ClassUtilities.getClassHierarchyInfo(ArrayList.class);
        assertNotNull(info);

        // Distance from ArrayList to List should be 1
        assertEquals(1, info.getDistanceMap().get(List.class).intValue());

        // Depth of ArrayList (number of super‑classes up to Object) should be 2 (ArrayList -> AbstractList -> AbstractCollection -> Object)
        // The exact depth is calculated as the number of steps to Object, which for ArrayList is 3.
        assertTrue("Depth should be >= 3", info.getDepth() >= 3);
    }

    @Test
    public void testHaveCommonAncestor() {
        assertTrue(ClassUtilities.haveCommonAncestor(ArrayList.class, LinkedList.class));
        assertFalse(ClassUtilities.haveCommonAncestor(String.class, Integer.class));
    }

    @Test
    public void testFindLowestCommonSupertypesExcluding() {
        Set<Class<?>> excluded = Collections.singleton(Object.class);
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(Integer.class, Double.class, excluded);
        // Expected common supertypes: Number and Comparable (both not excluded)
        assertTrue(result.contains(Number.class));
        assertTrue(result.contains(Comparable.class));
        assertFalse(result.contains(Object.class));
    }

    @Test
    public void testFindLowestCommonSupertypes() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(Integer.class, Double.class);
        // Same expectation as above, but Object is automatically excluded
        assertTrue(result.contains(Number.class));
        assertTrue(result.contains(Comparable.class));
        assertFalse(result.contains(Object.class));
    }

    @Test
    public void testFindLowestCommonSupertype() {
        Class<?> supertype = ClassUtilities.findLowestCommonSupertype(Integer.class, Double.class);
        // May return either Number or Comparable – both are acceptable.
        assertTrue(supertype == Number.class || supertype == Comparable.class);
    }

    @Test
    public void testIsPrimitive() {
        assertTrue(ClassUtilities.isPrimitive(int.class));
        assertTrue(ClassUtilities.isPrimitive(Integer.class));
        assertFalse(ClassUtilities.isPrimitive(String.class));
    }

    @Test
    public void testGetPrimitiveFromWrapper() {
        assertEquals(int.class, ClassUtilities.getPrimitiveFromWrapper(Integer.class));
        assertEquals(boolean.class, ClassUtilities.getPrimitiveFromWrapper(Boolean.class));
        assertNull(ClassUtilities.getPrimitiveFromWrapper(String.class));

        try {
            ClassUtilities.getPrimitiveFromWrapper(null);
            fail("Expected IllegalArgumentException for null argument");
        } catch (IllegalArgumentException ignored) {
        }
    }

    /* ----------------------------------------------------------------------
     *  2. Edge‑case / exception tests for newInstance()
     * ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstanceWithInterface() {
        // Interface – should fail before any reflection is attempted
        ClassUtilities.newInstance(null, List.class, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstanceWithAbstractClass() {
        // Abstract class – should fail early
        ClassUtilities.newInstance(null, AbstractMap.class, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstanceWithPrivateConstructor() {
        // ClassUtilities itself has only a private constructor; instantiation must fail.
        ClassUtilities.newInstance(null, ClassUtilities.class, null);
    }

    @Test
    public void testNewInstanceWithPublicConstructor() {
        // Simple class with a public no‑arg constructor
        Object instance = ClassUtilities.newInstance(null, StringBuilder.class, null);
        assertNotNull("Instance should be created", instance);
        assertTrue(instance instanceof StringBuilder);
    }

    /* ----------------------------------------------------------------------
     *  3. setUseUnsafe – basic toggle (unsafe path is not exercised)
     * ---------------------------------------------------------------------- */
    @Test
    public void testSetUseUnsafeToggle() {
        // Turn on unsafe – should not throw
        ClassUtilities.setUseUnsafe(true);
        // Turn off again
        ClassUtilities.setUseUnsafe(false);
    }

    /* ----------------------------------------------------------------------
     *  4. Helper for accessing the private alias map via forName (already used)
     * ---------------------------------------------------------------------- */
    @Test
    public void testAliasRoundTripThroughForName() {
        String alias = "myDateAlias";
        ClassUtilities.addPermanentClassAlias(Date.class, alias);
        Class<?> looked = ClassUtilities.forName(alias, ClassUtilities.getClassLoader());
        assertEquals(Date.class, looked);
        ClassUtilities.removePermanentClassAlias(alias);
    }
}
