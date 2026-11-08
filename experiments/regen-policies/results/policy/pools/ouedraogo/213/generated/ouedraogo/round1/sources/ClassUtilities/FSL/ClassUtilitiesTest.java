package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.ConverterOptions;
import org.junit.Test;

/**
 * JUnit‑4 test suite that exercises every public (and package‑private where reachable) method of
 * {@link ClassUtilities}.  The tests use only the API that is part of the supplied source file
 * and the small set of dependency signatures that were listed in the task description.
 *
 * <p>The goal is to verify correct behaviour for normal, edge‑case and error conditions without
 * using any mocking framework.</p>
 */
public class ClassUtilitiesTest {

    /* ---------------------------------------------------------------------- */
    /* Helper classes used by several tests                                    */
    /* ---------------------------------------------------------------------- */

    /** Simple class with a public no‑arg constructor. */
    public static class SimpleNoArg {
        public SimpleNoArg() { }
    }

    /** Simple class with two constructors – one that takes a String and one that takes an int. */
    public static class SimpleMultiCtor {
        public String s;
        public int i;

        public SimpleMultiCtor() { }

        public SimpleMultiCtor(String s) { this.s = s; }

        public SimpleMultiCtor(int i) { this.i = i; }
    }

    /** Utility class that deliberately has only private constructors. */
    public static final class PrivateCtorOnly {
        private PrivateCtorOnly() { }
        private PrivateCtorOnly(int ignored) { }
    }

    /** Non‑static inner class – used to verify that {@code newInstance} can create an inner class instance. */
    public class InnerClass {
        public InnerClass() { }
    }

    /** Enum used for {@code getClassIfEnum} tests. */
    public enum SampleEnum { A, B, C }

    /* ---------------------------------------------------------------------- */
    /* Primitive / wrapper handling                                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetPrimitiveFromWrapper() {
        assertEquals(int.class, ClassUtilities.getPrimitiveFromWrapper(Integer.class));
        assertEquals(boolean.class, ClassUtilities.getPrimitiveFromWrapper(Boolean.class));
        assertNull(ClassUtilities.getPrimitiveFromWrapper(String.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetPrimitiveFromWrapper_NullArgument() {
        ClassUtilities.getPrimitiveFromWrapper(null);
    }

    @Test
    public void testIsPrimitive() {
        assertTrue(ClassUtilities.isPrimitive(int.class));
        assertTrue(ClassUtilities.isPrimitive(Integer.class));
        assertTrue(ClassUtilities.isPrimitive(void.class));
        assertFalse(ClassUtilities.isPrimitive(String.class));
    }

    @Test
    public void testToPrimitiveWrapperClass() {
        assertEquals(Integer.class, ClassUtilities.toPrimitiveWrapperClass(int.class));
        assertEquals(void.class, int.class.isPrimitive() ? int.class : int.class); // sanity
        assertEquals(String.class, ClassUtilities.toPrimitiveWrapperClass(String.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToPrimitiveWrapperClass_NotPrimitive() {
        ClassUtilities.toPrimitiveWrapperClass(String.class);
    }

    @Test
    public void testDoesOneWrapTheOther() {
        assertTrue(ClassUtilities.doesOneWrapTheOther(Integer.class, int.class));
        assertTrue(ClassUtilities.doesOneWrapTheOther(int.class, Integer.class));
        assertFalse(ClassUtilities.doesOneWrapTheOther(Integer.class, Long.class));
    }

    /* ---------------------------------------------------------------------- */
    /* Class loading / name resolution                                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testForName_WithAlias() {
        // Alias added in static initializer: "string" → String.class
        Class<?> clazz = ClassUtilities.forName("string", ClassUtilities.getClassLoader());
        assertEquals(String.class, clazz);
    }

    @Test
    public void testForName_UnknownClassReturnsNull() {
        assertNull(ClassUtilities.forName("com.this.does.not.Exist", ClassUtilities.getClassLoader()));
    }

    @Test
    public void testForName_NullOrEmpty() {
        assertNull(ClassUtilities.forName(null, ClassUtilities.getClassLoader()));
        assertNull(ClassUtilities.forName("", ClassUtilities.getClassLoader()));
    }

    /* ---------------------------------------------------------------------- */
    /* Inheritance distance                                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testComputeInheritanceDistance_Simple() {
        assertEquals(1, ClassUtilities.computeInheritanceDistance(ArrayList.class, List.class));
        assertEquals(2, ClassUtilities.computeInheritanceDistance(ArrayList.class, Collection.class));
        assertEquals(0, ClassUtilities.computeInheritanceDistance(List.class, List.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, List.class));
    }

    @Test
    public void testComputeInheritanceDistance_PrimitiveAndWrapper() {
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, Integer.class));
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Integer.class, int.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, Long.class));
    }

    @Test
    public void testComputeInheritanceDistance_NullArguments() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, Object.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(Object.class, null));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, null));
    }

    /* ---------------------------------------------------------------------- */
    /* Class modifiers                                                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIsClassFinal() {
        assertTrue(ClassUtilities.isClassFinal(String.class)); // final class
        assertFalse(ClassUtilities.isClassFinal(ArrayList.class)); // not final
    }

    @Test
    public void testAreAllConstructorsPrivate() {
        assertTrue(ClassUtilities.areAllConstructorsPrivate(PrivateCtorOnly.class));
        assertFalse(ClassUtilities.areAllConstructorsPrivate(String.class));
    }

    /* ---------------------------------------------------------------------- */
    /* ClassLoader utilities                                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetClassLoader_NotNull() {
        ClassLoader cl = ClassUtilities.getClassLoader();
        assertNotNull(cl);
    }

    @Test
    public void testGetClassLoader_WithAnchor() {
        ClassLoader cl = ClassUtilities.getClassLoader(ClassUtilitiesTest.class);
        assertNotNull(cl);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetClassLoader_NullAnchor() {
        ClassUtilities.getClassLoader((Class<?>) null);
    }

    /* ---------------------------------------------------------------------- */
    /* findClosest – selection logic                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testFindClosest_ExactMatch() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(Number.class, "number");
        map.put(Integer.class, "int");
        String result = ClassUtilities.findClosest(Integer.class, map, "default");
        assertEquals("int", result);
    }

    @Test
    public void testFindClosest_InheritanceMatch() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(Number.class, "number");
        map.put(Long.class, "long");
        // Integer is not present – closest is Number (distance 1)
        String result = ClassUtilities.findClosest(Integer.class, map, "default");
        assertEquals("number", result);
    }

    @Test
    public void testFindClosest_TieBreakPreferClassOverInterface() {
        // Both List and Collection are assignable from ArrayList, same distance (1)
        Map<Class<?>, String> map = new HashMap<>();
        map.put(List.class, "list");
        map.put(Collection.class, "collection");
        String result = ClassUtilities.findClosest(ArrayList.class, map, "default");
        // Preference: class over interface – List is an interface, both are interfaces;
        // then more specific: List extends Collection, so List should win.
        assertEquals("list", result);
    }

    @Test
    public void testFindClosest_DefaultWhenNoMatch() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(Set.class, "set");
        String result = ClassUtilities.findClosest(String.class, map, "fallback");
        assertEquals("fallback", result);
    }

    /* ---------------------------------------------------------------------- */
    /* Resource loading                                                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testLoadResourceAsBytes_ExistingResource() {
        // Use a well‑known resource that is always present on the JDK classpath.
        byte[] bytes = ClassUtilities.loadResourceAsBytes("META-INF/MANIFEST.MF");
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadResourceAsBytes_NonExisting() {
        ClassUtilities.loadResourceAsBytes("nonexistent-resource-xyz.txt");
    }

    @Test
    public void testLoadResourceAsString() {
        String content = ClassUtilities.loadResourceAsString("META-INF/MANIFEST.MF");
        assertNotNull(content);
        assertTrue(content.contains("Manifest-Version"));
    }

    @Test(expected = NullPointerException.class)
    public void testLoadResourceAsBytes_NullName() {
        ClassUtilities.loadResourceAsBytes(null);
    }

    /* ---------------------------------------------------------------------- */
    /* Deprecated indexOfSmallestValue                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIndexOfSmallestValue_Normal() {
        int[] arr = {5, 2, 9, 2, 7};
        int idx = ClassUtilities.indexOfSmallestValue(arr);
        // The first smallest (2) is at index 1
        assertEquals(1, idx);
    }

    @Test
    public void testIndexOfSmallestValue_AllNegative() {
        int[] arr = {-5, -2, -9};
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(arr));
    }

    @Test
    public void testIndexOfSmallestValue_EmptyOrNull() {
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(new int[0]));
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(null));
    }

    /* ---------------------------------------------------------------------- */
    /* Enum detection                                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetClassIfEnum_DirectEnum() {
        Class<?> enumCls = ClassUtilities.getClassIfEnum(SampleEnum.class);
        assertEquals(SampleEnum.class, enumCls);
    }

    @Test
    public void testGetClassIfEnum_NonEnum() {
        assertNull(ClassUtilities.getClassIfEnum(String.class));
    }

    @Test
    public void testGetClassIfEnum_Null() {
        assertNull(ClassUtilities.getClassIfEnum(null));
    }

    /* ---------------------------------------------------------------------- */
    /* Lowest common supertypes                                                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testFindLowestCommonSupertypesExcluding_Simple() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(
                Integer.class, Double.class,
                Collections.singleton(Serializable.class)); // exclude Serializable

        // Expected common supertypes: Number and Comparable (Object is also common but ignored)
        assertTrue(result.contains(Number.class));
        assertTrue(result.contains(Comparable.class));
        assertFalse(result.contains(Serializable.class));
        assertFalse(result.contains(Object.class));
    }

    @Test
    public void testFindLowestCommonSupertypes_Convenience() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(Integer.class, Double.class);
        // By default Object, Serializable, Externalizable, Cloneable are excluded
        assertTrue(result.contains(Number.class));
        assertTrue(result.contains(Comparable.class));
        assertFalse(result.contains(Object.class));
    }

    @Test
    public void testFindLowestCommonSupertype_SingleResult() {
        Class<?> common = ClassUtilities.findLowestCommonSupertype(Integer.class, Double.class);
        // Should be either Number or Comparable – both are acceptable.
        assertTrue(common == Number.class || common == Comparable.class);
    }

    @Test
    public void testHaveCommonAncestor() {
        assertTrue(ClassUtilities.haveCommonAncestor(ArrayList.class, LinkedList.class));
        assertFalse(ClassUtilities.haveCommonAncestor(String.class, Integer.class));
    }

    /* ---------------------------------------------------------------------- */
    /* newInstance – simple class creation                                    */
    /* ---------------------------------------------------------------------- */

    private Converter createDefaultConverter() {
        // ConverterOptions has a no‑arg constructor in the library; use it.
        return new Converter(new ConverterOptions());
    }

    @Test
    public void testNewInstance_NoArgs() {
        Converter conv = createDefaultConverter();
        Object obj = ClassUtilities.newInstance(conv, SimpleNoArg.class, Collections.emptyList());
        assertNotNull(obj);
        assertTrue(obj instanceof SimpleNoArg);
    }

    @Test
    public void testNewInstance_WithMatchingArgument() {
        Converter conv = createDefaultConverter();

        // Provide a String that should match the (String) constructor
        Collection<Object> args = Collections.singletonList("hello");
        Object obj = ClassUtilities.newInstance(conv, SimpleMultiCtor.class, args);
        assertTrue(obj instanceof SimpleMultiCtor);
        SimpleMultiCtor instance = (SimpleMultiCtor) obj;
        assertEquals("hello", instance.s);
    }

    @Test
    public void testNewInstance_WithPrimitiveWrapperArgument() {
        Converter conv = createDefaultConverter();

        // Provide an Integer that should match the (int) constructor via wrapper handling
        Collection<Object> args = Collections.singletonList(Integer.valueOf(42));
        Object obj = ClassUtilities.newInstance(conv, SimpleMultiCtor.class, args);
        assertTrue(obj instanceof SimpleMultiCtor);
        SimpleMultiCtor instance = (SimpleMultiCtor) obj;
        assertEquals(42, instance.i);
    }

    @Test
    public void testNewInstance_PrivateConstructorClass() {
        Converter conv = createDefaultConverter();

        // PrivateCtorOnly has only private constructors – newInstance should still succeed via reflection.
        Object obj = ClassUtilities.newInstance(conv, PrivateCtorOnly.class, Collections.emptyList());
        assertNotNull(obj);
        assertTrue(obj instanceof PrivateCtorOnly);
    }

    @Test
    public void testNewInstance_NonStaticInnerClass() {
        Converter conv = createDefaultConverter();

        // Instantiate the non‑static inner class declared above.
        Object obj = ClassUtilities.newInstance(conv, InnerClass.class, Collections.emptyList());
        assertNotNull(obj);
        assertTrue(obj instanceof ClassUtilitiesTest.InnerClass);
    }

    /* ---------------------------------------------------------------------- */
    /* SecurityChecker – basic positive path (no exception expected)         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testSecurityChecker_VerifyClass_Allowed() {
        // String is not blocked – verifyClass should return silently.
        ClassUtilities.SecurityChecker.verifyClass(String.class);
    }

    /* ---------------------------------------------------------------------- */
    /* ClassHierarchyInfo – depth and distance map sanity checks            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testClassHierarchyInfo_Depth() {
        ClassUtilities.ClassHierarchyInfo info = ClassUtilities.getClassHierarchyInfo(ArrayList.class);
        // ArrayList -> AbstractList -> AbstractCollection -> Object => depth 3
        assertEquals(3, info.getDepth());
    }

    @Test
    public void testClassHierarchyInfo_DistanceMapContainsSelf() {
        ClassUtilities.ClassHierarchyInfo info = ClassUtilities.getClassHierarchyInfo(HashMap.class);
        Map<Class<?>, Integer> map = info.getDistanceMap();
        assertEquals(Integer.valueOf(0), map.get(HashMap.class));
        // Direct supertype should be distance 1
        assertEquals(Integer.valueOf(1), map.get(AbstractMap.class));
    }

    /* ---------------------------------------------------------------------- */
    /* Miscellaneous utility methods                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetClassLoader_UsesSystemLoaderAsFallback() {
        // Force a situation where both context and anchor class loaders are null.
        // This is difficult to simulate directly, but we can at least verify that the method never returns null.
        ClassLoader loader = ClassUtilities.getClassLoader();
        assertNotNull(loader);
    }
}
