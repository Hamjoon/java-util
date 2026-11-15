package com.cedarsoftware.util;

import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.ConverterOptions;
import org.junit.Before;
import org.junit.Test;

import java.io.UncheckedIOException;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.time.LocalDate;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ClassUtilities}.
 * <p>
 * The suite follows the steps described in the prompt:
 * <ol>
 *   <li>Extract public method signatures (see the {@code testMethodSignatures} comment).</li>
 *   <li>Provide a basic test for each public method.</li>
 *   <li>Identify edge‑case / exception scenarios.</li>
 *   <li>Add dedicated tests for those edge cases.</li>
 *   <li>All tests are merged into this single file.</li>
 * </ol>
 */
public class ClassUtilitiesTest {

    /* -------------------------------------------------------------
     *  Public method signatures extracted from ClassUtilities
     * -------------------------------------------------------------
     *
     * 1. void addPermanentClassAlias(Class<?> clazz, String alias)
     * 2. void removePermanentClassAlias(String alias)
     * 3. int computeInheritanceDistance(Class<?> source, Class<?> destination)
     * 4. boolean isPrimitive(Class<?> c)
     * 5. Class<?> forName(String name, ClassLoader classLoader)
     * 6. boolean isClassFinal(Class<?> c)
     * 7. boolean areAllConstructorsPrivate(Class<?> c)
     * 8. Class<?> toPrimitiveWrapperClass(Class<?> primitiveClass)
     * 9. boolean doesOneWrapTheOther(Class<?> x, Class<?> y)
     *10. ClassLoader getClassLoader()
     *11. ClassLoader getClassLoader(Class<?> anchorClass)
     *12. Class<?> getClassIfEnum(Class<?> c)
     *13. <T> T findClosest(Class<?> clazz, Map<Class<?>, T> candidateClasses, T defaultClass)
     *14. String loadResourceAsString(String resourceName)
     *15. byte[] loadResourceAsBytes(String resourceName)
     *16. int indexOfSmallestValue(int[] array)
     *17. Set<Class<?>> findLowestCommonSupertypesExcluding(Class<?> a, Class<?> b, Set<Class<?>> skipList)
     *18. Set<Class<?>> findLowestCommonSupertypes(Class<?> a, Class<?> b)
     *19. Class<?> findLowestCommonSupertype(Class<?> a, Class<?> b)
     *20. Set<Class<?>> getAllSupertypes(Class<?> clazz)
     *21. boolean haveCommonAncestor(Class<?> a, Class<?> b)
     *
     * ------------------------------------------------------------- */

    private static final String TEST_RESOURCE = "test_resource.txt";
    private static final String TEST_RESOURCE_CONTENT = "Hello ClassUtilities!";

    @Before
    public void setUp() throws Exception {
        // Ensure the test resource is available via the classloader.
        // The resource is placed under src/test/resources/
        // (Maven/Gradle automatically adds it to the test classpath).
        URL url = getClass().getClassLoader().getResource(TEST_RESOURCE);
        assertNotNull("Test resource must exist on classpath", url);
    }

    /* -------------------------------------------------------------
     *  Basic functionality tests
     * ------------------------------------------------------------- */

    @Test
    public void testAddAndRemovePermanentClassAlias() {
        // Alias a known class
        ClassUtilities.addPermanentClassAlias(List.class, "myListAlias");
        Class<?> aliasClass = ClassUtilities.forName("myListAlias", ClassUtilities.class.getClassLoader());
        assertEquals(List.class, aliasClass);

        // Remove the alias and verify it is no longer resolved
        ClassUtilities.removePermanentClassAlias("myListAlias");
        Class<?> afterRemoval = ClassUtilities.forName("myListAlias", ClassUtilities.class.getClassLoader());
        assertNull(afterRemoval);
    }

    @Test
    public void testComputeInheritanceDistance_SameClass() {
        assertEquals(0, ClassUtilities.computeInheritanceDistance(String.class, String.class));
    }

    @Test
    public void testComputeInheritanceDistance_DirectSubclass() {
        assertEquals(1, ClassUtilities.computeInheritanceDistance(ArrayList.class, List.class));
    }

    @Test
    public void testComputeInheritanceDistance_InterfaceImplementation() {
        assertEquals(1, ClassUtilities.computeInheritanceDistance(HashSet.class, Set.class));
    }

    @Test
    public void testComputeInheritanceDistance_Unrelated() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, Integer.class));
    }

    @Test
    public void testComputeInheritanceDistance_PrimitiveWrapper() {
        // int (primitive) -> Integer (wrapper)
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, Integer.class));
        // Integer (wrapper) -> int (primitive)
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Integer.class, int.class));
    }

    @Test
    public void testIsPrimitive() {
        assertTrue(ClassUtilities.isPrimitive(int.class));
        assertTrue(ClassUtilities.isPrimitive(Integer.class));
        assertFalse(ClassUtilities.isPrimitive(String.class));
        assertFalse(ClassUtilities.isPrimitive(void.class)); // void is primitive but not in prims set
    }

    @Test
    public void testForName_ValidAndInvalid() {
        // Valid class name
        Class<?> cls = ClassUtilities.forName("java.util.ArrayList", ClassUtilities.class.getClassLoader());
        assertEquals(ArrayList.class, cls);

        // Invalid name returns null
        assertNull(ClassUtilities.forName("non.existing.ClassXYZ", ClassUtilities.class.getClassLoader()));

        // Null or empty returns null
        assertNull(ClassUtilities.forName(null, ClassUtilities.class.getClassLoader()));
        assertNull(ClassUtilities.forName("", ClassUtilities.class.getClassLoader()));
    }

    @Test
    public void testIsClassFinal() {
        assertTrue(ClassUtilities.isClassFinal(String.class)); // String is final
        assertFalse(ClassUtilities.isClassFinal(ArrayList.class)); // not final
    }

    @Test
    public void testAreAllConstructorsPrivate() {
        // Utility class (all static) – private constructor
        assertTrue(ClassUtilities.areAllConstructorsPrivate(ClassUtilities.class));

        // Regular class with public constructor
        assertFalse(ClassUtilities.areAllConstructorsPrivate(String.class));
    }

    @Test
    public void testToPrimitiveWrapperClass() {
        assertEquals(Integer.class, ClassUtilities.toPrimitiveWrapperClass(int.class));
        assertEquals(void.class, ClassUtilities.toPrimitiveWrapperClass(void.class).getClass().getSuperclass()); // void maps to Void
        // Non‑primitive returns itself
        assertEquals(String.class, ClassUtilities.toPrimitiveWrapperClass(String.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToPrimitiveWrapperClass_NonPrimitiveException() {
        // Passing a non‑primitive class should trigger IllegalArgumentException
        ClassUtilities.toPrimitiveWrapperClass(String.class);
    }

    @Test
    public void testDoesOneWrapTheOther() {
        assertTrue(ClassUtilities.doesOneWrapTheOther(Integer.class, int.class));
        assertTrue(ClassUtilities.doesOneWrapTheOther(int.class, Integer.class));
        assertFalse(ClassUtilities.doesOneWrapTheOther(Integer.class, long.class));
    }

    @Test
    public void testGetClassLoader_NoAnchorNull() {
        // getClassLoader() without argument must never return null
        assertNotNull(ClassUtilities.getClassLoader());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetClassLoader_NullAnchor() {
        ClassUtilities.getClassLoader(null);
    }

    @Test
    public void testGetClassIfEnum() {
        // Direct enum
        assertEquals(TestEnum.class, ClassUtilities.getClassIfEnum(TestEnum.class));

        // Non‑enum returns null
        assertNull(ClassUtilities.getClassIfEnum(String.class));

        // Enum defined as an inner class
        class Outer {
            enum InnerEnum { A, B }
        }
        // Use reflection to obtain the inner enum class
        Class<?> inner = Outer.class.getDeclaredClasses()[0];
        assertEquals(inner, ClassUtilities.getClassIfEnum(inner));
    }

    @Test
    public void testFindClosest_Basic() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(Number.class, "Number");
        map.put(Integer.class, "Integer");
        map.put(Object.class, "Object");

        // Exact match
        assertEquals("Integer", ClassUtilities.findClosest(Integer.class, map, "default"));

        // Closest superclass (Long extends Number)
        assertEquals("Number", ClassUtilities.findClosest(Long.class, map, "default"));
    }

    @Test
    public void testLoadResourceAsStringAndBytes() {
        String content = ClassUtilities.loadResourceAsString(TEST_RESOURCE);
        assertEquals(TEST_RESOURCE_CONTENT, content);

        byte[] bytes = ClassUtilities.loadResourceAsBytes(TEST_RESOURCE);
        assertArrayEquals(TEST_RESOURCE_CONTENT.getBytes(StandardCharsets.UTF_8), bytes);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadResourceAsBytes_NotFound() {
        ClassUtilities.loadResourceAsBytes("nonexistent_resource.txt");
    }

    @Test(expected = UncheckedIOException.class)
    public void testLoadResourceAsBytes_NullName() {
        ClassUtilities.loadResourceAsBytes(null);
    }

    @Test
    public void testIndexOfSmallestValue() {
        int[] array = {5, 2, 9, 2, 7};
        assertEquals(1, ClassUtilities.indexOfSmallestValue(array)); // first occurrence of smallest value (2)

        assertEquals(-1, ClassUtilities.indexOfSmallestValue(new int[0])); // empty array
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(null)); // null array
    }

    @Test
    public void testFindLowestCommonSupertypesExcluding() {
        Set<Class<?>> skip = Collections.singleton(Object.class);
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(ArrayList.class, LinkedList.class, skip);
        // Both are List implementations, so List should be the lowest common supertype (Object is skipped)
        assertTrue(result.contains(List.class));
        assertFalse(result.contains(Object.class));
    }

    @Test
    public void testFindLowestCommonSupertypesAndSupertype() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(Integer.class, Double.class);
        // Expected common supertypes: Number, Comparable, Serializable (Object is excluded by the method)
        assertTrue(result.contains(Number.class));
        assertTrue(result.contains(Comparable.class));
        assertTrue(result.contains(Serializable.class));

        Class<?> single = ClassUtilities.findLowestCommonSupertype(Integer.class, Double.class);
        // May return any of the lowest supertypes; verify it is one of them
        assertTrue(result.contains(single));
    }

    @Test
    public void testGetAllSupertypes() {
        Set<Class<?>> supertypes = ClassUtilities.getAllSupertypes(HashMap.class);
        // Verify a few expected entries
        assertTrue(supertypes.contains(AbstractMap.class));
        assertTrue(supertypes.contains(Map.class));
        assertTrue(supertypes.contains(Cloneable.class));
        assertTrue(supertypes.contains(Serializable.class));
        // Object should always be present
        assertTrue(supertypes.contains(Object.class));
    }

    @Test
    public void testHaveCommonAncestor() {
        assertTrue(ClassUtilities.haveCommonAncestor(ArrayList.class, LinkedList.class)); // both share List, Collection, Object
        assertFalse(ClassUtilities.haveCommonAncestor(String.class, Integer.class)); // only common ancestor is Object, which is excluded by findLowestCommonSupertypes, but haveCommonAncestor returns true because Object counts
    }

    /* -------------------------------------------------------------
     *  Edge‑case / exception specific tests
     * ------------------------------------------------------------- */

    @Test
    public void testComputeInheritanceDistance_NullArguments() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, String.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, null));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, null));
    }

    @Test
    public void testComputeInheritanceDistance_ArrayTypes() throws ClassNotFoundException {
        // Using internal loadClass via forName to resolve an array descriptor
        Class<?> intArray = ClassUtilities.forName("[I", ClassUtilities.class.getClassLoader());
        assertArrayEquals(int[].class, intArray);
        // Distance between int[] and Object (should be -1 because they are unrelated in our algorithm)
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(intArray, Object.class));
    }

    @Test
    public void testFindClosest_NoMatchReturnsDefault() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(List.class, "List");
        String result = ClassUtilities.findClosest(Set.class, map, "default");
        assertEquals("default", result);
    }

    @Test
    public void testLoadResourceAsString_NullName() {
        try {
            ClassUtilities.loadResourceAsString(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
    }

    @Test
    public void testIndexOfSmallestValue_NegativeValuesIgnored() {
        int[] arr = {10, -5, 3, -1};
        // The algorithm ignores negative values when looking for the smallest positive integer
        assertEquals(2, ClassUtilities.indexOfSmallestValue(arr));
    }

    @Test
    public void testFindLowestCommonSupertypesExcluding_AllSkipped() {
        Set<Class<?>> skip = new HashSet<>(Arrays.asList(Object.class, Number.class, Serializable.class, Comparable.class));
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(Integer.class, Double.class, skip);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testAreAllConstructorsPrivate_UtilityClass() {
        // ClassUtilities itself is a utility class with a private constructor
        assertTrue(ClassUtilities.areAllConstructorsPrivate(ClassUtilities.class));
    }

    @Test
    public void testIsClassFinal_NullPointer() {
        try {
            ClassUtilities.isClassFinal(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
    }

    @Test
    public void testGetClassIfEnum_NullInput() {
        assertNull(ClassUtilities.getClassIfEnum(null));
    }

    /* -------------------------------------------------------------
     *  Helper enum for enum‑related tests
     * ------------------------------------------------------------- */
    private enum TestEnum { ONE, TWO }
}
