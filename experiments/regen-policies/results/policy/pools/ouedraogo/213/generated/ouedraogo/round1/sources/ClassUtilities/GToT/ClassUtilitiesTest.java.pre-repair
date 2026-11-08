package com.cedarsoftware.util;

import org.junit.*;
import static org.junit.Assert.*;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

import com.cedarsoftware.util.convert.Converter;

/**
 * Comprehensive JUnit‑4 test suite for {@link ClassUtilities}.
 * <p>
 * The suite covers:
 * <ul>
 *   <li>basic functionality of each public method</li>
 *   <li>edge‑case handling (null arguments, empty collections, etc.)</li>
 *   <li>exception scenarios (security blocks, illegal arguments, etc.)</li>
 *   <li>behaviour of helper methods that are used internally (e.g. {@code findClosest})</li>
 * </ul>
 * </p>
 */
public class ClassUtilitiesTest {

    /* -------------------------------------------------------------
     *  Helper types used by several tests
     * ------------------------------------------------------------- */
    private static final class PrivateCtor {
        private PrivateCtor() {}
    }

    private static final class MixedCtor {
        public MixedCtor() {}
        protected MixedCtor(String s) {}
        MixedCtor(int i) {}
        private MixedCtor(double d) {}
    }

    private static final class InnerNonStatic {
        final String value;
        InnerNonStatic(Outer outer) { this.value = outer != null ? "inner" : null; }
    }

    private static final class Outer {
        final String name = "outer";
    }

    private enum SampleEnum { ONE, TWO }

    /* -------------------------------------------------------------
     *  1. getPrimitiveFromWrapper
     * ------------------------------------------------------------- */
    @Test
    public void testGetPrimitiveFromWrapper() {
        assertEquals(int.class, ClassUtilities.getPrimitiveFromWrapper(Integer.class));
        assertEquals(void.class, ClassUtilities.getPrimitiveFromWrapper(Void.class));
        assertNull(ClassUtilities.getPrimitiveFromWrapper(String.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetPrimitiveFromWrapper_Null() {
        ClassUtilities.getPrimitiveFromWrapper(null);
    }

    /* -------------------------------------------------------------
     *  2. addPermanentClassAlias / removePermanentClassAlias / forName
     * ------------------------------------------------------------- */
    @Test
    public void testAliasAndForName() {
        ClassUtilities.addPermanentClassAlias(String.class, "myStringAlias");
        Class<?> c = ClassUtilities.forName("myStringAlias", ClassUtilities.getClassLoader());
        assertEquals(String.class, c);

        ClassUtilities.removePermanentClassAlias("myStringAlias");
        assertNull(ClassUtilities.forName("myStringAlias", ClassUtilities.getClassLoader()));
    }

    @Test
    public void testForName_WithStandardClass() {
        Class<?> c = ClassUtilities.forName("java.lang.String", ClassUtilities.getClassLoader());
        assertEquals(String.class, c);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testForName_SecurityBlockedName() {
        // "java.lang.ProcessImpl" is in the security blocked name set
        ClassUtilities.forName("java.lang.ProcessImpl", ClassUtilities.getClassLoader());
    }

    @Test
    public void testForName_NullOrEmpty() {
        assertNull(ClassUtilities.forName(null, ClassUtilities.getClassLoader()));
        assertNull(ClassUtilities.forName("", ClassUtilities.getClassLoader()));
    }

    /* -------------------------------------------------------------
     *  3. isClassFinal
     * ------------------------------------------------------------- */
    @Test
    public void testIsClassFinal() {
        assertTrue(ClassUtilities.isClassFinal(String.class)); // final
        assertFalse(ClassUtilities.isClassFinal(ArrayList.class)); // not final
    }

    @Test(expected = NullPointerException.class)
    public void testIsClassFinal_Null() {
        ClassUtilities.isClassFinal(null);
    }

    /* -------------------------------------------------------------
     *  4. areAllConstructorsPrivate
     * ------------------------------------------------------------- */
    @Test
    public void testAreAllConstructorsPrivate() {
        assertTrue(ClassUtilities.areAllConstructorsPrivate(PrivateCtor.class));
        assertFalse(ClassUtilities.areAllConstructorsPrivate(MixedCtor.class));
    }

    @Test(expected = NullPointerException.class)
    public void testAreAllConstructorsPrivate_Null() {
        ClassUtilities.areAllConstructorsPrivate(null);
    }

    /* -------------------------------------------------------------
     *  5. isPrimitive
     * ------------------------------------------------------------- */
    @Test
    public void testIsPrimitive() {
        assertTrue(ClassUtilities.isPrimitive(int.class));
        assertTrue(ClassUtilities.isPrimitive(Integer.class));
        assertFalse(ClassUtilities.isPrimitive(String.class));
    }

    @Test(expected = NullPointerException.class)
    public void testIsPrimitive_Null() {
        ClassUtilities.isPrimitive(null);
    }

    /* -------------------------------------------------------------
     *  6. computeInheritanceDistance
     * ------------------------------------------------------------- */
    @Test
    public void testComputeInheritanceDistance_Simple() {
        assertEquals(1, ClassUtilities.computeInheritanceDistance(ArrayList.class, List.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(List.class, ArrayList.class));
        assertEquals(0, ClassUtilities.computeInheritanceDistance(String.class, String.class));
    }

    @Test
    public void testComputeInheritanceDistance_PrimitiveAndWrapper() {
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, Integer.class));
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Integer.class, int.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, Long.class));
    }

    @Test
    public void testComputeInheritanceDistance_NullArgs() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, String.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, null));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, null));
    }

    /* -------------------------------------------------------------
     *  7. toPrimitiveWrapperClass
     * ------------------------------------------------------------- */
    @Test
    public void testToPrimitiveWrapperClass() {
        assertEquals(Integer.class, ClassUtilities.toPrimitiveWrapperClass(int.class));
        assertEquals(String.class, ClassUtilities.toPrimitiveWrapperClass(String.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToPrimitiveWrapperClass_UnknownPrimitive() {
        // void is mapped, so use a non‑primitive class that is NOT primitive (e.g., Boolean)
        // Passing a non‑primitive will just return the class, so we need a primitive without mapping.
        // All Java primitives are mapped; therefore we use a custom primitive class via reflection is impossible.
        // Instead we trigger the exception by passing a primitive array (which is not in the map).
        ClassUtilities.toPrimitiveWrapperClass(int[].class);
    }

    @Test(expected = NullPointerException.class)
    public void testToPrimitiveWrapperClass_Null() {
        ClassUtilities.toPrimitiveWrapperClass(null);
    }

    /* -------------------------------------------------------------
     *  8. doesOneWrapTheOther
     * ------------------------------------------------------------- */
    @Test
    public void testDoesOneWrapTheOther() {
        assertTrue(ClassUtilities.doesOneWrapTheOther(Integer.class, int.class));
        assertTrue(ClassUtilities.doesOneWrapTheOther(int.class, Integer.class));
        assertFalse(ClassUtilities.doesOneWrapTheOther(Integer.class, Long.class));
    }

    @Test(expected = NullPointerException.class)
    public void testDoesOneWrapTheOther_NullFirst() {
        ClassUtilities.doesOneWrapTheOther(null, Integer.class);
    }

    @Test(expected = NullPointerException.class)
    public void testDoesOneWrapTheOther_NullSecond() {
        ClassUtilities.doesOneWrapTheOther(Integer.class, null);
    }

    /* -------------------------------------------------------------
     *  9. getClassLoader (two overloads)
     * ------------------------------------------------------------- */
    @Test
    public void testGetClassLoader() {
        ClassLoader cl = ClassUtilities.getClassLoader();
        assertNotNull(cl);
    }

    @Test
    public void testGetClassLoader_WithAnchor() {
        ClassLoader cl = ClassUtilities.getClassLoader(ClassUtilities.class);
        assertNotNull(cl);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetClassLoader_NullAnchor() {
        ClassUtilities.getClassLoader(null);
    }

    /* -------------------------------------------------------------
     * 10. loadResourceAsBytes / loadResourceAsString
     * ------------------------------------------------------------- */
    @Test
    public void testLoadResourceAsBytesAndString() throws IOException {
        // The test resource is placed in src/test/resources/com/cedarsoftware/util/sample.txt
        String resourceName = "com/cedarsoftware/util/sample.txt";
        byte[] bytes = ClassUtilities.loadResourceAsBytes(resourceName);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        String content = ClassUtilities.loadResourceAsString(resourceName);
        assertEquals(new String(bytes, java.nio.charset.StandardCharsets.UTF_8), content);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadResourceAsBytes_NotFound() {
        ClassUtilities.loadResourceAsBytes("nonexistent/resource.txt");
    }

    @Test(expected = NullPointerException.class)
    public void testLoadResourceAsBytes_NullName() {
        ClassUtilities.loadResourceAsBytes(null);
    }

    /* -------------------------------------------------------------
     * 11. indexOfSmallestValue
     * ------------------------------------------------------------- */
    @Test
    public void testIndexOfSmallestValue() {
        assertEquals(2, ClassUtilities.indexOfSmallestValue(new int[]{5, 3, 1, 4}));
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(new int[]{-1, -2, -3}));
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(new int[]{}));
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(null));
    }

    /* -------------------------------------------------------------
     * 12. getClassIfEnum
     * ------------------------------------------------------------- */
    @Test
    public void testGetClassIfEnum() {
        assertEquals(SampleEnum.class, ClassUtilities.getClassIfEnum(SampleEnum.class));
        assertNull(ClassUtilities.getClassIfEnum(String.class));
        assertNull(ClassUtilities.getClassIfEnum(null));
    }

    /* -------------------------------------------------------------
     * 13. findClosest
     * ------------------------------------------------------------- */
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
        map.put(Iterable.class, "iterable");

        String result = ClassUtilities.findClosest(ArrayList.class, map, "default");
        // Collection is closer than Iterable
        assertEquals("collection", result);
    }

    @Test
    public void testFindClosest_TieBreakingPrefersClassOverInterface() {
        interface I1 {}
        interface I2 extends I1 {}
        class C implements I2 {}

        Map<Class<?>, String> map = new HashMap<>();
        map.put(I1.class, "i1");
        map.put(I2.class, "i2");

        String result = ClassUtilities.findClosest(C.class, map, "default");
        // Both are distance 1, but I2 is a sub‑interface of I1, so it should be preferred
        assertEquals("i2", result);
    }

    @Test(expected = NullPointerException.class)
    public void testFindClosest_NullClass() {
        ClassUtilities.findClosest(null, Collections.emptyMap(), "default");
    }

    @Test(expected = NullPointerException.class)
    public void testFindClosest_NullMap() {
        ClassUtilities.findClosest(String.class, null, "default");
    }

    /* -------------------------------------------------------------
     * 14. findLowestCommonSupertypesExcluding
     * ------------------------------------------------------------- */
    @Test
    public void testFindLowestCommonSupertypesExcluding() {
        Set<Class<?>> excluded = new HashSet<>(Arrays.asList(Number.class));
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(Integer.class, Double.class, excluded);
        // Number is excluded, so only Comparable should remain
        assertTrue(result.contains(Comparable.class));
        assertFalse(result.contains(Number.class));
    }

    @Test
    public void testFindLowestCommonSupertypesExcluding_SameClass() {
        Set<Class<?>> excluded = Collections.singleton(String.class);
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(String.class, String.class, excluded);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFindLowestCommonSupertypesExcluding_NullInputs() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(null, String.class, Collections.emptySet());
        assertTrue(result.isEmpty());

        result = ClassUtilities.findLowestCommonSupertypesExcluding(String.class, null, Collections.emptySet());
        assertTrue(result.isEmpty());
    }

    /* -------------------------------------------------------------
     * 15. findLowestCommonSupertypes (default exclusion)
     * ------------------------------------------------------------- */
    @Test
    public void testFindLowestCommonSupertypes_DefaultExcludes() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(Integer.class, Double.class);
        // Object, Serializable, Externalizable, Cloneable are filtered out.
        // Expected common supertypes: Number and Comparable
        assertTrue(result.contains(Number.class));
        assertTrue(result.contains(Comparable.class));
        assertFalse(result.contains(Object.class));
    }

    /* -------------------------------------------------------------
     * 16. findLowestCommonSupertype (single result)
     * ------------------------------------------------------------- */
    @Test
    public void testFindLowestCommonSupertype() {
        Class<?> result = ClassUtilities.findLowestCommonSupertype(Integer.class, Double.class);
        // Could be Number or Comparable; both are valid. We only assert that it is one of them.
        assertTrue(result == Number.class || result == Comparable.class);
    }

    /* -------------------------------------------------------------
     * 17. getClassHierarchyInfo
     * ------------------------------------------------------------- */
    @Test
    public void testGetClassHierarchyInfo() {
        ClassUtilities.ClassHierarchyInfo info = ClassUtilities.getClassHierarchyInfo(ArrayList.class);
        Map<Class<?>, Integer> distances = info.getDistanceMap();

        // Direct superclass
        assertEquals(Integer.valueOf(1), distances.get(AbstractList.class));
        // Interface
        assertEquals(Integer.valueOf(1), distances.get(List.class));
        // Self distance is 0
        assertEquals(Integer.valueOf(0), distances.get(ArrayList.class));

        // Depth should reflect inheritance chain length (ArrayList -> AbstractList -> AbstractCollection -> Object)
        assertEquals(3, info.getDepth());
    }

    /* -------------------------------------------------------------
     * 18. haveCommonAncestor
     * ------------------------------------------------------------- */
    @Test
    public void testHaveCommonAncestor() {
        assertTrue(ClassUtilities.haveCommonAncestor(Integer.class, Double.class)); // Number
        assertFalse(ClassUtilities.haveCommonAncestor(String.class, Integer.class)); // only Object (filtered)
    }

    /* -------------------------------------------------------------
     * 19. newInstance
     * ------------------------------------------------------------- */
    @Test
    public void testNewInstance_NoArgs() {
        Converter converter = new Converter(null);
        Object obj = ClassUtilities.newInstance(converter, StringBuilder.class, Collections.emptyList());
        assertTrue(obj instanceof StringBuilder);
    }

    @Test
    public void testNewInstance_WithArgs_MatchingConstructor() {
        Converter converter = new Converter(null);
        List<Object> args = Arrays.asList("hello", 42);
        // ArrayList has a constructor (Collection<? extends E>) but not (String, int)
        // We'll use java.util.HashMap which has a (int) constructor.
        Object map = ClassUtilities.newInstance(converter, HashMap.class, Collections.singleton(16));
        assertTrue(map instanceof HashMap);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstance_Interface() {
        Converter converter = new Converter(null);
        ClassUtilities.newInstance(converter, List.class, Collections.emptyList());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstance_AbstractClass() {
        Converter converter = new Converter(null);
        ClassUtilities.newInstance(converter, AbstractList.class, Collections.emptyList());
    }

    @Test
    public void testNewInstance_InnerNonStatic() {
        Converter converter = new Converter(null);
        // First instantiate the outer class
        Outer outer = (Outer) ClassUtilities.newInstance(converter, Outer.class, Collections.emptyList());
        assertNotNull(outer);

        // Now instantiate the inner non‑static class using the outer instance as argument
        List<Object> args = Collections.singletonList(outer);
        Object inner = ClassUtilities.newInstance(converter, InnerNonStatic.class, args);
        assertTrue(inner instanceof InnerNonStatic);
    }

    /* -------------------------------------------------------------
     * 20. setUseUnsafe (basic toggle)
     * ------------------------------------------------------------- */
    @Test
    public void testSetUseUnsafeToggle() {
        // By default unsafe is disabled – we just verify that calling the method does not throw.
        ClassUtilities.setUseUnsafe(true);
        ClassUtilities.setUseUnsafe(false);
    }

    /* -------------------------------------------------------------
     * 21. SecurityChecker integration (indirect)
     * ------------------------------------------------------------- */
    @Test(expected = SecurityException.class)
    public void testSecurityChecker_VerifyBlockedClass() {
        // ProcessBuilder is in the blocked list
        ClassUtilities.SecurityChecker.verifyClass(ProcessBuilder.class);
    }

    @Test
    public void testSecurityChecker_VerifySafeClass() {
        // String is not blocked
        ClassUtilities.SecurityChecker.verifyClass(String.class);
    }
}
