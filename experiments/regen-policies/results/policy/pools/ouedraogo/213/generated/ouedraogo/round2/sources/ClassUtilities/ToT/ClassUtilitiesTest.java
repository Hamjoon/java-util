package com.cedarsoftware.util;

import com.cedarsoftware.util.convert.Converter;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ClassUtilities}.
 *
 * <p>This test class covers:
 * <ul>
 *   <li>Primitive / wrapper handling</li>
 *   <li>Class loading (including aliases and security blocks)</li>
 *   <li>Inheritance distance calculations</li>
 *   <li>Utility methods such as {@code isClassFinal}, {@code areAllConstructorsPrivate},
 *       {@code findClosest}, {@code loadResourceAsString/Bytes}, etc.</li>
 *   <li>Enum detection and lowest‑common‑supertype logic</li>
 *   <li>Instance creation via {@code newInstance}</li>
 * </ul>
 * </p>
 *
 * <p>All tests use only the public API of {@code ClassUtilities} and the
 * supporting classes listed in the problem statement.</p>
 */
public class ClassUtilitiesTest {

    private Converter converter;

    @Before
    public void setUp() {
        // Use a default Converter (no special options needed for these tests)
        converter = new Converter(null);
    }

    /* --------------------------------------------------------------
     *  Primitive / Wrapper utilities
     * -------------------------------------------------------------- */

    @Test
    public void testGetPrimitiveFromWrapper_Valid() {
        assertEquals(int.class, ClassUtilities.getPrimitiveFromWrapper(Integer.class));
        assertEquals(void.class, ClassUtilities.getPrimitiveFromWrapper(Void.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetPrimitiveFromWrapper_NullArgument() {
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
    public void testToPrimitiveWrapperClass_NotPrimitive() {
        ClassUtilities.toPrimitiveWrapperClass(void.class); // void is primitive but mapped, ok
        // Force the error by passing a non‑primitive that is not in the map
        ClassUtilities.toPrimitiveWrapperClass(Object.class);
    }

    @Test
    public void testDoesOneWrapTheOther() {
        assertTrue(ClassUtilities.doesOneWrapTheOther(Integer.class, int.class));
        assertTrue(ClassUtilities.doesOneWrapTheOther(int.class, Integer.class));
        assertFalse(ClassUtilities.doesOneWrapTheOther(Integer.class, long.class));
    }

    /* --------------------------------------------------------------
     *  Class loading utilities
     * -------------------------------------------------------------- */

    @Test
    public void testForName_WithAlias() {
        // "string" is pre‑populated in nameToClass map
        Class<?> cls = ClassUtilities.forName("string", ClassUtilities.class.getClassLoader());
        assertEquals(String.class, cls);
    }

    @Test
    public void testForName_StandardClass() {
        Class<?> cls = ClassUtilities.forName("java.util.ArrayList", ClassUtilities.class.getClassLoader());
        assertEquals(ArrayList.class, cls);
    }

    @Test(expected = SecurityException.class)
    public void testForName_SecurityBlockedName() {
        // ProcessBuilder is in the security blocked list
        ClassUtilities.forName("java.lang.ProcessBuilder", ClassUtilities.class.getClassLoader());
    }

    @Test
    public void testInternalClassForName_CacheWorks() throws Exception {
        // First call loads and caches
        Class<?> first = ClassUtilities.forName("java.util.HashMap", ClassUtilities.class.getClassLoader());
        // Second call should hit the cache (no exception expected)
        Class<?> second = ClassUtilities.forName("java.util.HashMap", ClassUtilities.class.getClassLoader());
        assertSame(first, second);
    }

    /* --------------------------------------------------------------
     *  Miscellaneous class queries
     * -------------------------------------------------------------- */

    @Test
    public void testIsClassFinal() {
        assertTrue(ClassUtilities.isClassFinal(String.class)); // String is final
        assertFalse(ClassUtilities.isClassFinal(ArrayList.class));
    }

    @Test
    public void testAreAllConstructorsPrivate() {
        assertTrue(ClassUtilities.areAllConstructorsPrivate(UtilityWithPrivateCtor.class));
        assertFalse(ClassUtilities.areAllConstructorsPrivate(String.class));
    }

    private static final class UtilityWithPrivateCtor {
        private UtilityWithPrivateCtor() {}
    }

    @Test
    public void testComputeInheritanceDistance() {
        // Direct super‑type
        assertEquals(1, ClassUtilities.computeInheritanceDistance(ArrayList.class, List.class));
        // Same class
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Integer.class, Integer.class));
        // Unrelated types
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, Integer.class));
        // Primitive ↔ wrapper (same primitive)
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, Integer.class));
        // Primitive ↔ different primitive
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, long.class));
    }

    /* --------------------------------------------------------------
     *  findClosest()
     * -------------------------------------------------------------- */

    @Test
    public void testFindClosest_ExactMatch() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(List.class, "list");
        map.put(Collection.class, "collection");
        String result = ClassUtilities.findClosest(ArrayList.class, map, "default");
        assertEquals("list", result);
    }

    @Test
    public void testFindClosest_InheritanceMatch() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(Collection.class, "collection");
        map.put(Set.class, "set");
        // ArrayList implements List → Collection, but not Set
        String result = ClassUtilities.findClosest(ArrayList.class, map, "default");
        assertEquals("collection", result);
    }

    @Test
    public void testFindClosest_NoMatchReturnsDefault() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(Set.class, "set");
        String result = ClassUtilities.findClosest(ArrayList.class, map, "default");
        assertEquals("default", result);
    }

    /* --------------------------------------------------------------
     *  Resource loading
     * -------------------------------------------------------------- */

    @Test
    public void testLoadResourceAsBytes_AndString() throws Exception {
        // Load the compiled class file of this test as a resource – guaranteed to exist.
        String resourceName = ClassUtilitiesTest.class.getSimpleName() + ".class";
        byte[] bytes = ClassUtilities.loadResourceAsBytes(resourceName);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        String asString = ClassUtilities.loadResourceAsString(resourceName);
        assertNotNull(asString);
        // The class file is binary; converting to UTF‑8 will produce a non‑empty string.
        assertTrue(asString.length() > 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadResourceAsBytes_NotFound() {
        ClassUtilities.loadResourceAsBytes("nonexistent_resource_xyz.txt");
    }

    @Test(expected = NullPointerException.class)
    public void testLoadResourceAsBytes_NullArgument() {
        ClassUtilities.loadResourceAsBytes(null);
    }

    /* --------------------------------------------------------------
     *  Deprecated method indexOfSmallestValue
     * -------------------------------------------------------------- */

    @Test
    public void testIndexOfSmallestValue_Typical() {
        int[] array = {5, 2, 9, 2, 7};
        int idx = ClassUtilities.indexOfSmallestValue(array);
        // First occurrence of the smallest positive value (2) is index 1
        assertEquals(1, idx);
    }

    @Test
    public void testIndexOfSmallestValue_AllNegativeOrEmpty() {
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(new int[]{-5, -2, -9}));
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(new int[]{}));
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(null));
    }

    /* --------------------------------------------------------------
     *  Enum detection
     * -------------------------------------------------------------- */

    private enum SampleEnum { A, B }

    @Test
    public void testGetClassIfEnum_DetectsEnum() {
        assertEquals(SampleEnum.class, ClassUtilities.getClassIfEnum(SampleEnum.class));
    }

    @Test
    public void testGetClassIfEnum_NonEnumReturnsNull() {
        assertNull(ClassUtilities.getClassIfEnum(String.class));
        assertNull(ClassUtilities.getClassIfEnum(null));
    }

    /* --------------------------------------------------------------
     *  Lowest common supertypes
     * -------------------------------------------------------------- */

    @Test
    public void testFindLowestCommonSupertypesExcluding_Basic() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(Integer.class, Double.class,
                Collections.singleton(Object.class));
        // Expected: Number and Comparable are the two lowest common supertypes (Object excluded)
        assertTrue(result.contains(Number.class));
        assertTrue(result.contains(Comparable.class));
        assertFalse(result.contains(Object.class));
    }

    @Test
    public void testFindLowestCommonSupertypes_ConvenienceMethod() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(Integer.class, Double.class);
        // Object is automatically excluded, so we should still have Number and Comparable
        assertTrue(result.contains(Number.class));
        assertTrue(result.contains(Comparable.class));
        assertFalse(result.contains(Object.class));
    }

    @Test
    public void testFindLowestCommonSupertype_SingleResult() {
        Class<?> cls = ClassUtilities.findLowestCommonSupertype(ArrayList.class, LinkedList.class);
        // Both share AbstractList as the most specific common class
        assertEquals(AbstractList.class, cls);
    }

    @Test
    public void testHaveCommonAncestor_TrueAndFalse() {
        assertTrue(ClassUtilities.haveCommonAncestor(Integer.class, Double.class)); // Number
        assertFalse(ClassUtilities.haveCommonAncestor(String.class, Integer.class)); // only Object (filtered out)
    }

    /* --------------------------------------------------------------
     *  Class hierarchy info
     * -------------------------------------------------------------- */

    @Test
    public void testGetClassHierarchyInfo_DistancesAndDepth() {
        ClassUtilities.ClassHierarchyInfo info = ClassUtilities.getClassHierarchyInfo(ArrayList.class);
        // Direct superclass is AbstractList → distance 1
        assertEquals(1, info.getDistanceMap().get(AbstractList.class).intValue());
        // Interfaces List, RandomAccess, Cloneable, Serializable, etc. should be present
        assertTrue(info.getDistanceMap().containsKey(List.class));
        // Depth for ArrayList should be > 0 (Object → List → AbstractList → ArrayList)
        assertTrue(info.getDepth() > 0);
    }

    /* --------------------------------------------------------------
     *  Security checker (direct usage)
     * -------------------------------------------------------------- */

    @Test(expected = SecurityException.class)
    public void testSecurityChecker_VerifyClass_Blocked() {
        // ClassLoader is a blocked class
        ClassUtilities.SecurityChecker.verifyClass(ClassLoader.class);
    }

    @Test
    public void testSecurityChecker_VerifyClass_Allowed() {
        // Normal class should pass without exception
        ClassUtilities.SecurityChecker.verifyClass(String.class);
    }

    /* --------------------------------------------------------------
     *  newInstance() – various scenarios
     * -------------------------------------------------------------- */

    /** Simple class with several constructors. */
    public static class MultiCtor {
        public String text;
        public int number;
        public boolean flag;

        public MultiCtor() {
            this("default", 0, false);
        }

        public MultiCtor(String text) {
            this(text, 0, false);
        }

        public MultiCtor(String text, int number) {
            this(text, number, false);
        }

        public MultiCtor(String text, int number, boolean flag) {
            this.text = text;
            this.number = number;
            this.flag = flag;
        }
    }

    @Test
    public void testNewInstance_NoArgs_UsesNoArgConstructor() {
        Object obj = ClassUtilities.newInstance(converter, MultiCtor.class, Collections.emptyList());
        assertTrue(obj instanceof MultiCtor);
        MultiCtor mc = (MultiCtor) obj;
        assertEquals("default", mc.text);
        assertEquals(0, mc.number);
        assertFalse(mc.flag);
    }

    @Test
    public void testNewInstance_WithMatchingArgs() {
        List<Object> args = Arrays.asList(123, "hello"); // unordered on purpose
        Object obj = ClassUtilities.newInstance(converter, MultiCtor.class, args);
        assertTrue(obj instanceof MultiCtor);
        MultiCtor mc = (MultiCtor) obj;
        // The best match is the (String, int) constructor
        assertEquals("hello", mc.text);
        assertEquals(123, mc.number);
        assertFalse(mc.flag); // default for unmatched boolean
    }

    @Test
    public void testNewInstance_PrimitiveWrapperConversion() {
        List<Object> args = Arrays.asList(Integer.valueOf(42)); // wrapper for int
        Object obj = ClassUtilities.newInstance(converter, MultiCtor.class, args);
        MultiCtor mc = (MultiCtor) obj;
        // Should match the (String, int) constructor with default String
        assertEquals("", mc.text); // default from DIRECT_CLASS_MAPPING for String
        assertEquals(42, mc.number);
        assertFalse(mc.flag);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstance_AbstractClassFails() {
        ClassUtilities.newInstance(converter, AbstractList.class, Collections.emptyList());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstance_InterfaceFails() {
        ClassUtilities.newInstance(converter, List.class, Collections.emptyList());
    }

    /** Non‑static inner class used to verify inner‑class handling. */
    public class InnerClass {
        public final String outerValue;

        public InnerClass() {
            this.outerValue = ClassUtilitiesTest.this.toString();
        }
    }

    @Test
    public void testNewInstance_NonStaticInnerClass() {
        Object inner = ClassUtilities.newInstance(converter, InnerClass.class, Collections.emptyList());
        assertTrue(inner instanceof InnerClass);
        InnerClass ic = (InnerClass) inner;
        assertNotNull(ic.outerValue);
    }

    /* --------------------------------------------------------------
     *  Class loader retrieval
     * -------------------------------------------------------------- */

    @Test
    public void testGetClassLoader_NotNull() {
        ClassLoader cl = ClassUtilities.getClassLoader();
        assertNotNull(cl);
        // The returned loader should be able to load a known class
        assertNotNull(cl.getResource("java/lang/String.class"));
    }

    @Test
    public void testGetClassLoader_WithAnchorClass() {
        ClassLoader cl = ClassUtilities.getClassLoader(ClassUtilitiesTest.class);
        assertNotNull(cl);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetClassLoader_NullAnchor() {
        ClassUtilities.getClassLoader(null);
    }

    /* --------------------------------------------------------------
     *  Additional sanity checks for static caches (no side‑effects)
     * -------------------------------------------------------------- */

    @Test
    public void testAddAndRemovePermanentClassAlias() {
        ClassUtilities.addPermanentClassAlias(BigDecimal.class, "bigdec");
        Class<?> loaded = ClassUtilities.forName("bigdec", ClassUtilities.class.getClassLoader());
        assertEquals(BigDecimal.class, loaded);

        ClassUtilities.removePermanentClassAlias("bigdec");
        assertNull(ClassUtilities.forName("bigdec", ClassUtilities.class.getClassLoader()));
    }
}
