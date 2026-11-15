package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.*;

import com.cedarsoftware.util.convert.Converter;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Comprehensive JUnit 4 tests for {@link ClassUtilities}.
 * <p>
 * The tests cover:
 * <ul>
 *   <li>Class alias registration (add/remove)</li>
 *   <li>Inheritance distance calculations (including primitives)</li>
 *   <li>Primitive / wrapper utilities</li>
 *   <li>Final‑class detection</li>
 *   <li>All‑constructors‑private detection</li>
 *   <li>Class‑loader retrieval</li>
 *   <li>Closest‑candidate lookup</li>
 *   <li>Resource loading (String / byte[])</li>
 *   <li>Index‑of‑smallest helper</li>
 *   <li>Enum detection helper</li>
 *   <li>Lowest common super‑type utilities</li>
 *   <li>Common‑ancestor predicate</li>
 * </ul>
 * </p>
 */
public class ClassUtilitiesTest {

    /* -------------------------------------------------------
     *  Helper types used by several test cases
     * ------------------------------------------------------- */

    // Simple enum for getClassIfEnum tests
    private enum SimpleEnum { ONE, TWO }

    // Enum that encloses a non‑static inner class
    private enum OuterEnum {
        ;
        class InnerNonStatic { }
    }

    // Class with only private constructors (utility pattern)
    private static final class PrivateCtorUtil {
        private PrivateCtorUtil() {}
        private PrivateCtorUtil(int ignored) {}
    }

    // Class with mixed visibility constructors
    public static class MixedCtorUtil {
        public MixedCtorUtil() {}
        protected MixedCtorUtil(String s) {}
        private MixedCtorUtil(int i) {}
    }

    // Simple class hierarchy for inheritance‑distance tests
    private static class A {}
    private static class B extends A {}
    private static class C extends B {}
    private interface I1 {}
    private interface I2 extends I1 {}
    private static class D implements I2 {}

    // Simple class with a public static final field for toPrimitiveWrapperClass negative test
    private static class NotAPrimitive {}

    /* -------------------------------------------------------
     *  Test fixtures
     * ------------------------------------------------------- */

    @BeforeClass
    public static void setUp() {
        // Ensure unsafe is disabled for tests that rely on normal constructor paths
        ClassUtilities.setUseUnsafe(false);
    }

    /* -------------------------------------------------------
     *  1. addPermanentClassAlias / removePermanentClassAlias / forName
     * ------------------------------------------------------- */

    @Test
    public void testAddAndRemovePermanentClassAlias() {
        // Register an alias for java.util.ArrayList
        ClassUtilities.addPermanentClassAlias(ArrayList.class, "MyArrayList");
        Class<?> clazz = ClassUtilities.forName("MyArrayList", ClassLoader.getSystemClassLoader());
        assertEquals(ArrayList.class, clazz);

        // Remove the alias and verify lookup fails (returns null)
        ClassUtilities.removePermanentClassAlias("MyArrayList");
        assertNull(ClassUtilities.forName("MyArrayList", ClassLoader.getSystemClassLoader()));
    }

    @Test
    public void testForNameWithInvalidInput() {
        assertNull(ClassUtilities.forName(null, ClassLoader.getSystemClassLoader()));
        assertNull(ClassUtilities.forName("", ClassLoader.getSystemClassLoader()));
        // Non‑existent class name should return null, not throw
        assertNull(ClassUtilities.forName("com.nonexistent.FooBar", ClassLoader.getSystemClassLoader()));
    }

    /* -------------------------------------------------------
     *  2. computeInheritanceDistance
     * ------------------------------------------------------- */

    @Test
    public void testComputeInheritanceDistanceSimpleHierarchy() {
        assertEquals(0, ClassUtilities.computeInheritanceDistance(A.class, A.class));
        assertEquals(1, ClassUtilities.computeInheritanceDistance(B.class, A.class));
        assertEquals(2, ClassUtilities.computeInheritanceDistance(C.class, A.class));
        // No relationship
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(A.class, B.class));
    }

    @Test
    public void testComputeInheritanceDistanceWithInterfaces() {
        // D implements I2 which extends I1
        assertEquals(0, ClassUtilities.computeInheritanceDistance(I2.class, I2.class));
        assertEquals(1, ClassUtilities.computeInheritanceDistance(D.class, I2.class));
        assertEquals(2, ClassUtilities.computeInheritanceDistance(D.class, I1.class));
        // Unrelated interface
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(I1.class, Runnable.class));
    }

    @Test
    public void testComputeInheritanceDistancePrimitivesAndWrappers() {
        // Primitive to wrapper (int -> Integer) should be 0
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, Integer.class));
        // Wrapper to primitive (Integer -> int) also 0
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Integer.class, int.class));

        // Different primitive/wrapper pair → -1
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, Long.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(Double.class, double.class));
    }

    @Test
    public void testComputeInheritanceDistanceNulls() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, String.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, null));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, null));
    }

    /* -------------------------------------------------------
     *  3. isPrimitive
     * ------------------------------------------------------- */

    @Test
    public void testIsPrimitiveWithPrimitivesAndWrappers() {
        assertTrue(ClassUtilities.isPrimitive(int.class));
        assertTrue(ClassUtilities.isPrimitive(Integer.class));
        assertTrue(ClassUtilities.isPrimitive(boolean.class));
        assertTrue(ClassUtilities.isPrimitive(Boolean.class));
        assertFalse(ClassUtilities.isPrimitive(String.class));
        assertFalse(ClassUtilities.isPrimitive(Object.class));
    }

    /* -------------------------------------------------------
     *  4. toPrimitiveWrapperClass
     * ------------------------------------------------------- */

    @Test
    public void testToPrimitiveWrapperClassConversions() {
        assertEquals(Integer.class, ClassUtilities.toPrimitiveWrapperClass(int.class));
        assertEquals(Boolean.class, ClassUtilities.toPrimitiveWrapperClass(boolean.class));
        assertEquals(Void.class, ClassUtilities.toPrimitiveWrapperClass(void.class));
        // Non‑primitive should be returned unchanged
        assertEquals(String.class, ClassUtilities.toPrimitiveWrapperClass(String.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToPrimitiveWrapperClassWithNonPrimitiveThrows() {
        // Not a primitive type – should throw IllegalArgumentException
        ClassUtilities.toPrimitiveWrapperClass(NotAPrimitive.class);
    }

    /* -------------------------------------------------------
     *  5. doesOneWrapTheOther
     * ------------------------------------------------------- */

    @Test
    public void testDoesOneWrapTheOther() {
        assertTrue(ClassUtilities.doesOneWrapTheOther(Integer.class, int.class));
        assertTrue(ClassUtilities.doesOneWrapTheOther(int.class, Integer.class));
        assertFalse(ClassUtilities.doesOneWrapTheOther(Integer.class, long.class));
        assertFalse(ClassUtilities.doesOneWrapTheOther(String.class, int.class));
    }

    /* -------------------------------------------------------
     *  6. isClassFinal
     * ------------------------------------------------------- */

    @Test
    public void testIsClassFinal() {
        assertTrue(ClassUtilities.isClassFinal(String.class)); // String is final
        assertFalse(ClassUtilities.isClassFinal(ArrayList.class)); // Not final
    }

    /* -------------------------------------------------------
     *  7. areAllConstructorsPrivate
     * ------------------------------------------------------- */

    @Test
    public void testAreAllConstructorsPrivate() {
        assertTrue(ClassUtilities.areAllConstructorsPrivate(PrivateCtorUtil.class));
        assertFalse(ClassUtilities.areAllConstructorsPrivate(MixedCtorUtil.class));
        assertFalse(ClassUtilities.areAllConstructorsPrivate(String.class)); // Public constructors
    }

    /* -------------------------------------------------------
     *  8. getClassLoader
     * ------------------------------------------------------- */

    @Test
    public void testGetClassLoaderReturnsNonNull() {
        ClassLoader cl = ClassUtilities.getClassLoader(ClassUtilities.class);
        assertNotNull(cl);
        // The returned loader should be able to load this test class itself
        Class<?> loaded = cl.loadClass(ClassUtilitiesTest.class.getName());
        assertEquals(ClassUtilitiesTest.class, loaded);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetClassLoaderWithNullAnchor() {
        ClassUtilities.getClassLoader(null);
    }

    /* -------------------------------------------------------
     *  9. findClosest
     * ------------------------------------------------------- */

    @Test
    public void testFindClosestExactMatch() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(List.class, "list");
        map.put(Collection.class, "collection");
        // Exact match should short‑circuit
        assertEquals("list", ClassUtilities.findClosest(ArrayList.class, map, "default"));
    }

    @Test
    public void testFindClosestWithTieBreaking() {
        // Both List and Set are at distance 1 from HashSet (implements Set, extends AbstractSet → Object)
        Map<Class<?>, String> map = new LinkedHashMap<>();
        map.put(Set.class, "set");
        map.put(List.class, "list");
        // According to tie‑break logic, classes are preferred over interfaces;
        // Set is an interface, List is also an interface – the more specific (assignable) wins.
        // Since neither is assignable from the other, insertion order is used; we expect "set"
        // because shouldPreferNewCandidate will keep the first when distances equal.
        assertEquals("set", ClassUtilities.findClosest(HashSet.class, map, "default"));
    }

    @Test
    public void testFindClosestReturnsDefaultWhenNoMatch() {
        Map<Class<?>, String> empty = Collections.emptyMap();
        assertEquals("fallback", ClassUtilities.findClosest(String.class, empty, "fallback"));
    }

    /* -------------------------------------------------------
     * 10. loadResourceAsString / loadResourceAsBytes
     * ------------------------------------------------------- */

    @Test
    public void testLoadResourceAsBytesAndString() throws IOException {
        // Use a known resource that is always present: the class file of this test.
        String resourceName = ClassUtilitiesTest.class.getName().replace('.', '/') + ".class";
        byte[] bytes = ClassUtilities.loadResourceAsBytes(resourceName);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        // Convert to string (binary data) – just ensure the method works without exception.
        String content = ClassUtilities.loadResourceAsString(resourceName);
        assertNotNull(content);
        assertFalse(content.isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadResourceAsBytesNotFound() {
        ClassUtilities.loadResourceAsBytes("nonexistent_resource_12345.txt");
    }

    @Test(expected = NullPointerException.class)
    public void testLoadResourceAsBytesNullName() {
        ClassUtilities.loadResourceAsBytes(null);
    }

    /* -------------------------------------------------------
     * 11. indexOfSmallestValue
     * ------------------------------------------------------- */

    @Test
    public void testIndexOfSmallestValueVariousScenarios() {
        assertEquals(2, ClassUtilities.indexOfSmallestValue(new int[]{5, 3, 1, 4}));
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(new int[]{-1, -2, -3}));
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(new int[]{}));
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(null));
        // Duplicate smallest value – first occurrence is returned
        assertEquals(1, ClassUtilities.indexOfSmallestValue(new int[]{10, 2, 2, 5}));
    }

    /* -------------------------------------------------------
     * 12. getClassIfEnum
     * ------------------------------------------------------- */

    @Test
    public void testGetClassIfEnumDirectEnum() {
        assertEquals(SimpleEnum.class, ClassUtilities.getClassIfEnum(SimpleEnum.class));
    }

    @Test
    public void testGetClassIfEnumEnclosingEnum() {
        // InnerNonStatic is non‑static, its enclosing class is an enum
        Class<?> inner = OuterEnum.InnerNonStatic.class;
        assertEquals(OuterEnum.class, ClassUtilities.getClassIfEnum(inner));
    }

    @Test
    public void testGetClassIfEnumNone() {
        assertNull(ClassUtilities.getClassIfEnum(String.class));
        assertNull(ClassUtilities.getClassIfEnum(null));
    }

    /* -------------------------------------------------------
     * 13. findLowestCommonSupertypesExcluding / findLowestCommonSupertypes / findLowestCommonSupertype
     * ------------------------------------------------------- */

    @Test
    public void testFindLowestCommonSupertypesExcludingSkipsObject() {
        Set<Class<?>> skip = Collections.singleton(Object.class);
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(TreeSet.class, HashSet.class, skip);
        // Both are sets; the most specific common supertype should be Set
        assertEquals(Collections.singleton(Set.class), result);
    }

    @Test
    public void testFindLowestCommonSupertypesMultipleResults() {
        // Integer and Double share Number, Comparable, Serializable (excluding Object)
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(Integer.class, Double.class);
        // Order is not guaranteed, but the set must contain exactly those three classes
        Set<Class<?>> expected = new HashSet<>(Arrays.asList(Number.class, Comparable.class, java.io.Serializable.class));
        assertEquals(expected, result);
    }

    @Test
    public void testFindLowestCommonSupertypeReturnsOne() {
        // For Integer and Double, any of the three common supertypes is acceptable
        Class<?> supertype = ClassUtilities.findLowestCommonSupertype(Integer.class, Double.class);
        Set<Class<?>> allowed = new HashSet<>(Arrays.asList(Number.class, Comparable.class, java.io.Serializable.class));
        assertTrue(allowed.contains(supertype));
    }

    @Test
    public void testFindLowestCommonSupertypesWhenClassesEqual() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(String.class, String.class,
                Collections.emptySet());
        assertEquals(Collections.singleton(String.class), result);
    }

    @Test
    public void testFindLowestCommonSupertypesWithSkipListFilteringAll() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(String.class, Integer.class,
                new HashSet<>(Arrays.asList(Object.class, CharSequence.class, Number.class, Comparable.class,
                        java.io.Serializable.class)));
        // After skipping all common ancestors, result should be empty
        assertTrue(result.isEmpty());
    }

    /* -------------------------------------------------------
     * 14. haveCommonAncestor
     * ------------------------------------------------------- */

    @Test
    public void testHaveCommonAncestorPositiveAndNegative() {
        assertTrue(ClassUtilities.haveCommonAncestor(ArrayList.class, LinkedList.class)); // Both are List
        assertFalse(ClassUtilities.haveCommonAncestor(String.class, Integer.class)); // Only common is Object (skipped)
    }

    /* -------------------------------------------------------
     * 15. getAllSupertypes
     * ------------------------------------------------------- */

    @Test
    public void testGetAllSupertypesIncludesSelfAndAncestors() {
        Set<Class<?>> supertypes = ClassUtilities.getAllSupertypes(C.class);
        // Should contain C, B, A, Object
        assertTrue(supertypes.contains(C.class));
        assertTrue(supertypes.contains(B.class));
        assertTrue(supertypes.contains(A.class));
        assertTrue(supertypes.contains(Object.class));
        // No unrelated classes
        assertFalse(supertypes.contains(String.class));
    }

    @Test
    public void testGetAllSupertypesWithInterface() {
        Set<Class<?>> supertypes = ClassUtilities.getAllSupertypes(D.class);
        assertTrue(supertypes.contains(D.class));
        assertTrue(supertypes.contains(I2.class));
        assertTrue(supertypes.contains(I1.class));
        assertTrue(supertypes.contains(Object.class));
    }
}
