package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.net.URL;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link ClassUtilities}.
 *
 * <p>The suite follows the “three‑expert” scenario described in the prompt:
 * each public method is exercised with at least one typical use‑case,
 * one edge‑case and one error‑scenario (where applicable).  Helper
 * classes are defined as static inner classes so that no external
 * resources or mocking frameworks are required.</p>
 *
 * @author  OpenAI ChatGPT
 */
public class ClassUtilitiesTest {

    /* --------------------------------------------------------------------- */
    /*  Helper classes used throughout the test suite                        */
    /* --------------------------------------------------------------------- */

    /** Simple POJO with a public no‑arg constructor. */
    public static class SimpleBean {
        public String value = "default";
    }

    /** Bean with multiple constructors of varying visibility. */
    public static class MultiCtorBean {
        public MultiCtorBean() { }
        protected MultiCtorBean(String s) { }
        private MultiCtorBean(int i) { }
    }

    /** Utility class that follows the “all constructors private” pattern. */
    public static final class PrivateUtility {
        private PrivateUtility() { }
    }

    /** Enum used for {@link ClassUtilities#getClassIfEnum(Class)} tests. */
    public enum SampleEnum { A, B }

    /** Inner (non‑static) class to verify inner‑class handling in {@code newInstance}. */
    public class InnerBean {
        public InnerBean() { }
    }

    /** Class that should trigger the security check in {@code newInstance}. */
    public static class ForbiddenClass extends ProcessBuilder {
        public ForbiddenClass() { super(); }
    }

    /* --------------------------------------------------------------------- */
    /*  Test fixtures                                                       */
    /* --------------------------------------------------------------------- */

    private Converter converter;

    @Before
    public void setUp() {
        // The real library expects a ConverterOptions instance; passing null works for the tests.
        converter = new Converter(null);
    }

    /* --------------------------------------------------------------------- */
    /*  1. addPermanentClassAlias / removePermanentClassAlias                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddAndRemoveClassAlias() {
        // Alias a known class
        ClassUtilities.addPermanentClassAlias(ArrayList.class, "MyArrayList");
        assertEquals(ArrayList.class, ClassUtilities.forName("MyArrayList", ClassUtilities.class.getClassLoader()));

        // Remove the alias and verify it is no longer resolvable
        ClassUtilities.removePermanentClassAlias("MyArrayList");
        assertNull(ClassUtilities.forName("MyArrayList", ClassUtilities.class.getClassLoader()));
    }

    /* --------------------------------------------------------------------- */
    /*  2. computeInheritanceDistance                                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void testComputeInheritanceDistanceSimple() {
        assertEquals(1, ClassUtilities.computeInheritanceDistance(ArrayList.class, List.class));
        assertEquals(2, ClassUtilities.computeInheritanceDistance(ArrayList.class, Collection.class));
    }

    @Test
    public void testComputeInheritanceDistanceInterfaceToClass() {
        // Interface to concrete class (no path) → -1
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(List.class, ArrayList.class));
    }

    @Test
    public void testComputeInheritanceDistancePrimitiveWrapper() {
        // Primitive → wrapper (distance 0 because they are considered the same)
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, Integer.class));
        // Wrapper → primitive
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Integer.class, int.class));
    }

    @Test
    public void testComputeInheritanceDistanceUnrelated() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, Integer.class));
    }

    /* --------------------------------------------------------------------- */
    /*  3. isPrimitive                                                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void testIsPrimitiveTrue() {
        assertTrue(ClassUtilities.isPrimitive(int.class));
        assertTrue(ClassUtilities.isPrimitive(Integer.class));
        assertTrue(ClassUtilities.isPrimitive(boolean.class));
        assertTrue(ClassUtilities.isPrimitive(Boolean.class));
    }

    @Test
    public void testIsPrimitiveFalse() {
        assertFalse(ClassUtilities.isPrimitive(String.class));
        assertFalse(ClassUtilities.isPrimitive(void.class)); // void is primitive but not in the wrapper set
    }

    /* --------------------------------------------------------------------- */
    /*  4. forName (and internalClassForName)                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testForNameValid() {
        // Alias added in previous test may still be present – use a built‑in class.
        Class<?> cls = ClassUtilities.forName("java.lang.String", ClassUtilities.class.getClassLoader());
        assertEquals(String.class, cls);
    }

    @Test
    public void testForNameInvalidReturnsNull() {
        assertNull(ClassUtilities.forName("non.existing.ClassXYZ", ClassUtilities.class.getClassLoader()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testForNameSecurityExceptionIsWrapped() {
        // Simulate a security exception by passing a null ClassLoader (which forces a SecurityException
        // inside internalClassForName when it attempts to load a prohibited class). The exact class
        // does not matter – the method will wrap the SecurityException.
        ClassUtilities.forName("java.lang.ClassLoader", null);
    }

    /* --------------------------------------------------------------------- */
    /*  5. isClassFinal                                                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void testIsClassFinal() {
        assertTrue(ClassUtilities.isClassFinal(String.class)); // String is final
        assertFalse(ClassUtilities.isClassFinal(ArrayList.class));
    }

    @Test(expected = NullPointerException.class)
    public void testIsClassFinalNull() {
        ClassUtilities.isClassFinal(null);
    }

    /* --------------------------------------------------------------------- */
    /*  6. areAllConstructorsPrivate                                          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAllConstructorsPrivateTrue() {
        assertTrue(ClassUtilities.areAllConstructorsPrivate(PrivateUtility.class));
    }

    @Test
    public void testAllConstructorsPrivateFalse() {
        assertFalse(ClassUtilities.areAllConstructorsPrivate(MultiCtorBean.class));
    }

    @Test(expected = NullPointerException.class)
    public void testAllConstructorsPrivateNull() {
        ClassUtilities.areAllConstructorsPrivate(null);
    }

    /* --------------------------------------------------------------------- */
    /*  7. toPrimitiveWrapperClass                                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToPrimitiveWrapperClass() {
        assertEquals(Integer.class, ClassUtilities.toPrimitiveWrapperClass(int.class));
        assertEquals(Boolean.class, ClassUtilities.toPrimitiveWrapperClass(boolean.class));
        // Non‑primitive should be returned unchanged
        assertEquals(String.class, ClassUtilities.toPrimitiveWrapperClass(String.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToPrimitiveWrapperClassInvalid() {
        // Void is a primitive, but it is present in the map; use a non‑primitive that is not primitive.
        ClassUtilities.toPrimitiveWrapperClass(Object.class);
    }

    @Test(expected = NullPointerException.class)
    public void testToPrimitiveWrapperClassNull() {
        ClassUtilities.toPrimitiveWrapperClass(null);
    }

    /* --------------------------------------------------------------------- */
    /*  8. doesOneWrapTheOther                                                */
    /* --------------------------------------------------------------------- */

    @Test
    public void testDoesOneWrapTheOtherTrue() {
        assertTrue(ClassUtilities.doesOneWrapTheOther(Integer.class, int.class));
        assertTrue(ClassUtilities.doesOneWrapTheOther(int.class, Integer.class));
    }

    @Test
    public void testDoesOneWrapTheOtherFalse() {
        assertFalse(ClassUtilities.doesOneWrapTheOther(Integer.class, long.class));
        assertFalse(ClassUtilities.doesOneWrapTheOther(String.class, int.class));
    }

    @Test(expected = NullPointerException.class)
    public void testDoesOneWrapTheOtherNull() {
        ClassUtilities.doesOneWrapTheOther(null, int.class);
    }

    /* --------------------------------------------------------------------- */
    /*  9. getClassLoader                                                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetClassLoaderNotNull() {
        ClassLoader cl = ClassUtilities.getClassLoader(ClassUtilities.class);
        assertNotNull(cl);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetClassLoaderNullAnchor() {
        ClassUtilities.getClassLoader(null);
    }

    /* --------------------------------------------------------------------- */
    /* 10. findClosest                                                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void testFindClosestExactMatch() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(List.class, "list");
        map.put(Collection.class, "collection");
        String result = ClassUtilities.findClosest(ArrayList.class, map, "default");
        assertEquals("list", result);
    }

    @Test
    public void testFindClosestInheritanceDistance() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(Set.class, "set");
        map.put(Collection.class, "collection");
        // ArrayList implements Collection, not Set → should return "collection"
        String result = ClassUtilities.findClosest(ArrayList.class, map, "default");
        assertEquals("collection", result);
    }

    @Test
    public void testFindClosestTieBreakingPrefersClassOverInterface() {
        Map<Class<?>, String> map = new LinkedHashMap<>();
        map.put(Iterable.class, "iterable");   // interface
        map.put(List.class, "list");           // class (more specific)
        // Both have distance 1 from ArrayList; class should win.
        String result = ClassUtilities.findClosest(ArrayList.class, map, "default");
        assertEquals("list", result);
    }

    @Test
    public void testFindClosestDefaultWhenNoMatch() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(Map.class, "map");
        String result = ClassUtilities.findClosest(String.class, map, "default");
        assertEquals("default", result);
    }

    /* --------------------------------------------------------------------- */
    /* 11. loadResourceAsBytes / loadResourceAsString                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testLoadResourceAsBytesAndString() throws IOException {
        // Use a core JDK resource that is always present.
        String resource = "java/lang/String.class";
        byte[] bytes = ClassUtilities.loadResourceAsBytes(resource);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        String content = ClassUtilities.loadResourceAsString(resource);
        assertNotNull(content);
        // The .class file is binary; converting to UTF‑8 may produce garbled characters,
        // but we can at least assert that the string length matches the byte length.
        assertEquals(bytes.length, content.getBytes(StandardCharsets.UTF_8).length);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadResourceAsBytesNotFound() {
        ClassUtilities.loadResourceAsBytes("nonexistent.resource");
    }

    @Test(expected = NullPointerException.class)
    public void testLoadResourceAsBytesNullName() {
        ClassUtilities.loadResourceAsBytes(null);
    }

    /* --------------------------------------------------------------------- */
    /* 12. indexOfSmallestValue                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testIndexOfSmallestValueTypical() {
        int[] arr = {5, 2, 9, 2, 7};
        assertEquals(1, ClassUtilities.indexOfSmallestValue(arr)); // first occurrence of 2
    }

    @Test
    public void testIndexOfSmallestValueAllNegative() {
        int[] arr = {-5, -2, -9};
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(arr));
    }

    @Test
    public void testIndexOfSmallestValueEmpty() {
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(new int[0]));
    }

    @Test
    public void testIndexOfSmallestValueNull() {
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(null));
    }

    /* --------------------------------------------------------------------- */
    /* 13. getClassIfEnum                                                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetClassIfEnumDirect() {
        assertEquals(SampleEnum.class, ClassUtilities.getClassIfEnum(SampleEnum.class));
    }

    @Test
    public void testGetClassIfEnumSuperclass() {
        class EnumSub extends Enum<EnumSub> {
            private EnumSub(String name, int ordinal) { super(name, ordinal); }
        }
        // The subclass itself is not an enum, but its superclass is Enum – should return null
        assertNull(ClassUtilities.getClassIfEnum(EnumSub.class));
    }

    @Test
    public void testGetClassIfEnumEnclosing() {
        class Enclosing {
            enum InnerEnum { X }
        }
        assertEquals(Enclosing.InnerEnum.class,
                ClassUtilities.getClassIfEnum(Enclosing.InnerEnum.class));
    }

    @Test
    public void testGetClassIfEnumNull() {
        assertNull(ClassUtilities.getClassIfEnum(null));
    }

    /* --------------------------------------------------------------------- */
    /* 14. newInstance                                                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void testNewInstanceSimpleBean() {
        Object obj = ClassUtilities.newInstance(converter, SimpleBean.class, null);
        assertTrue(obj instanceof SimpleBean);
        assertEquals("default", ((SimpleBean) obj).value);
    }

    @Test
    public void testNewInstanceWithArguments() {
        List<Object> args = Arrays.asList("hello");
        Object obj = ClassUtilities.newInstance(converter, MultiCtorBean.class, args);
        assertTrue(obj instanceof MultiCtorBean);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstanceForbiddenClass() {
        // ProcessBuilder (and subclasses) are blocked by security check.
        ClassUtilities.newInstance(converter, ForbiddenClass.class, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstanceAbstractClass() {
        ClassUtilities.newInstance(converter, AbstractList.class, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstanceInterface() {
        ClassUtilities.newInstance(converter, List.class, null);
    }

    @Test
    public void testNewInstanceInnerClass() {
        // The inner class requires an enclosing instance; newInstance should handle it.
        Object obj = ClassUtilities.newInstance(converter, InnerBean.class, null);
        assertTrue(obj instanceof InnerBean);
    }

    /* --------------------------------------------------------------------- */
    /* 15. findLowestCommonSupertypesExcluding / findLowestCommonSupertypes */
    /* --------------------------------------------------------------------- */

    @Test
    public void testFindLowestCommonSupertypesSimple() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(
                ArrayList.class, LinkedList.class,
                Collections.singleton(Object.class));
        // Both are List implementations; the lowest common supertype should be List.
        assertEquals(Collections.singleton(List.class), result);
    }

    @Test
    public void testFindLowestCommonSupertypesMultiple() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(
                Integer.class, Double.class);
        // Expected common supertypes (ignoring Object) are Number, Comparable, Serializable.
        Set<Class<?>> expected = new HashSet<>(Arrays.asList(Number.class, Comparable.class, Serializable.class));
        assertEquals(expected, result);
    }

    @Test
    public void testFindLowestCommonSupertypeSingle() {
        Class<?> result = ClassUtilities.findLowestCommonSupertype(ArrayDeque.class, LinkedList.class);
        // Both share Deque as the most specific common supertype.
        assertEquals(Deque.class, result);
    }

    @Test
    public void testFindLowestCommonSupertypesWithNull() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(null, String.class);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFindLowestCommonSupertypesExcludingSkipAll() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(
                String.class, Integer.class,
                new HashSet<>(Arrays.asList(Object.class, Number.class, Serializable.class, Comparable.class)));
        // All common types are skipped → empty set.
        assertTrue(result.isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /* 16. haveCommonAncestor                                                */
    /* --------------------------------------------------------------------- */

    @Test
    public void testHaveCommonAncestorTrue() {
        assertTrue(ClassUtilities.haveCommonAncestor(ArrayList.class, HashSet.class));
    }

    @Test
    public void testHaveCommonAncestorFalse() {
        // Primitive types have no common ancestor other than Object, which is excluded.
        assertFalse(ClassUtilities.haveCommonAncestor(int.class, double.class));
    }

    /* --------------------------------------------------------------------- */
    /* 17. getAllSupertypes                                                  */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetAllSupertypesIncludesSelf() {
        Set<Class<?>> supertypes = ClassUtilities.getAllSupertypes(ArrayList.class);
        assertTrue(supertypes.contains(ArrayList.class));
        assertTrue(supertypes.contains(List.class));
        assertTrue(supertypes.contains(AbstractList.class));
        assertTrue(supertypes.contains(Object.class));
    }

    @Test
    public void testGetAllSupertypesForInterface() {
        Set<Class<?>> supertypes = ClassUtilities.getAllSupertypes(Comparable.class);
        assertTrue(supertypes.contains(Comparable.class));
        assertTrue(supertypes.contains(Object.class));
    }

    @Test
    public void testGetAllSupertypesNull() {
        Set<Class<?>> result = ClassUtilities.getAllSupertypes(null);
        assertTrue(result.isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /*  18. Private helper – shouldPreferNewCandidate (indirectly via findClosest) */
    /* --------------------------------------------------------------------- */

    @Test
    public void testShouldPreferNewCandidateTieBreak() {
        // Create a map where two candidates have the same distance from the target.
        // One is an interface, the other a concrete class.
        Map<Class<?>, String> map = new LinkedHashMap<>();
        map.put(Iterable.class, "iterable");
        map.put(List.class, "list"); // List is a class that also implements Iterable
        // Target: ArrayList (distance 1 to both). Expect class (List) to win.
        String result = ClassUtilities.findClosest(ArrayList.class, map, "default");
        assertEquals("list", result);
    }
}
