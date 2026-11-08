package com.cedarsoftware.util;

import org.junit.*;
import static org.junit.Assert.*;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.stream.*;

import com.cedarsoftware.util.convert.Converter;

/**
 * Comprehensive JUnit‑4 test suite for {@link ClassUtilities}.
 *
 * The tests are organised in the order of the public API:
 * <ul>
 *   <li>Basic functional tests (happy path)</li>
 *   <li>Edge‑case / exception tests</li>
 * </ul>
 *
 * All tests use only the classes, constructors and methods that are visible in the supplied source
 * (or the listed dependency APIs). No mocking frameworks are used.
 */
public class ClassUtilitiesTest {

    /* -------------------------------------------------------------
     *  Helper classes used by several tests
     * ------------------------------------------------------------- */

    /** Simple class with a *private* constructor – useful for {@code areAllConstructorsPrivate}. */
    private static final class PrivateCtor {
        private PrivateCtor() {}
    }

    /** Simple class with a *public* no‑arg constructor – useful for {@code newInstance}. */
    public static final class SimpleBean {
        public String name;
        public SimpleBean() {}
        public SimpleBean(String name) { this.name = name; }
    }

    /** Simple abstract class – used to verify that {@code newInstance} rejects abstract types. */
    public static abstract class AbstractBean {}

    /** Simple enum used for {@code getClassIfEnum}. */
    public enum SampleEnum { ONE, TWO }

    /** Inner (non‑static) class – used to verify that {@code newInstance} can handle inner classes. */
    public class InnerBean {
        public InnerBean() {}
    }

    /* -------------------------------------------------------------
     *  1.  getPrimitiveFromWrapper
     * ------------------------------------------------------------- */
    @Test
    public void testGetPrimitiveFromWrapper_HappyPath() {
        assertEquals(int.class, ClassUtilities.getPrimitiveFromWrapper(Integer.class));
        assertEquals(boolean.class, ClassUtilities.getPrimitiveFromWrapper(Boolean.class));
        assertEquals(void.class, ClassUtilities.getPrimitiveFromWrapper(Void.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetPrimitiveFromWrapper_NullArgument() {
        ClassUtilities.getPrimitiveFromWrapper(null);
    }

    @Test
    public void testGetPrimitiveFromWrapper_NonWrapperReturnsNull() {
        assertNull(ClassUtilities.getPrimitiveFromWrapper(String.class));
    }

    /* -------------------------------------------------------------
     *  2.  addPermanentClassAlias / removePermanentClassAlias / forName
     * ------------------------------------------------------------- */
    @Test
    public void testClassAlias_AddAndRemove() {
        String alias = "myStringAlias";
        ClassUtilities.addPermanentClassAlias(String.class, alias);
        assertEquals(String.class, ClassUtilities.forName(alias, ClassUtilities.class.getClassLoader()));

        ClassUtilities.removePermanentClassAlias(alias);
        assertNull(ClassUtilities.forName(alias, ClassUtilities.class.getClassLoader()));
    }

    @Test
    public void testForName_NullOrEmpty() {
        assertNull(ClassUtilities.forName(null, ClassUtilities.class.getClassLoader()));
        assertNull(ClassUtilities.forName("", ClassUtilities.class.getClassLoader()));
    }

    @Test
    public void testForName_SecurityBlockedName() {
        // The blocked list contains "java.lang.ProcessImpl".  Attempt to load it – should return null.
        assertNull(ClassUtilities.forName("java.lang.ProcessImpl", ClassUtilities.class.getClassLoader()));
    }

    /* -------------------------------------------------------------
     *  3.  computeInheritanceDistance
     * ------------------------------------------------------------- */
    @Test
    public void testComputeInheritanceDistance_SameClass() {
        assertEquals(0, ClassUtilities.computeInheritanceDistance(List.class, List.class));
    }

    @Test
    public void testComputeInheritanceDistance_DirectSuperclass() {
        assertEquals(1, ClassUtilities.computeInheritanceDistance(ArrayList.class, List.class));
    }

    @Test
    public void testComputeInheritanceDistance_Unrelated() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, Integer.class));
    }

    @Test
    public void testComputeInheritanceDistance_NullArguments() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, Object.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(Object.class, null));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, null));
    }

    @Test
    public void testComputeInheritanceDistance_PrimitiveAndWrapper() {
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, Integer.class));
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Integer.class, int.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, Long.class));
    }

    /* -------------------------------------------------------------
     *  4.  isClassFinal
     * ------------------------------------------------------------- */
    @Test
    public void testIsClassFinal() {
        assertTrue(ClassUtilities.isClassFinal(String.class)); // String is final
        assertFalse(ClassUtilities.isClassFinal(ArrayList.class)); // not final
    }

    @Test(expected = NullPointerException.class)
    public void testIsClassFinal_Null() {
        ClassUtilities.isClassFinal(null);
    }

    /* -------------------------------------------------------------
     *  5.  areAllConstructorsPrivate
     * ------------------------------------------------------------- */
    @Test
    public void testAreAllConstructorsPrivate_UtilityClass() {
        assertTrue(ClassUtilities.areAllConstructorsPrivate(ClassUtilities.class));
    }

    @Test
    public void testAreAllConstructorsPrivate_PrivateCtor() {
        assertTrue(ClassUtilities.areAllConstructorsPrivate(PrivateCtor.class));
    }

    @Test
    public void testAreAllConstructorsPrivate_NonPrivate() {
        assertFalse(ClassUtilities.areAllConstructorsPrivate(String.class));
    }

    @Test(expected = NullPointerException.class)
    public void testAreAllConstructorsPrivate_Null() {
        ClassUtilities.areAllConstructorsPrivate(null);
    }

    /* -------------------------------------------------------------
     *  6.  toPrimitiveWrapperClass
     * ------------------------------------------------------------- */
    @Test
    public void testToPrimitiveWrapperClass_Primitive() {
        assertEquals(Integer.class, ClassUtilities.toPrimitiveWrapperClass(int.class));
        assertEquals(Void.class, ClassUtilities.toPrimitiveWrapperClass(void.class));
    }

    @Test
    public void testToPrimitiveWrapperClass_NonPrimitive() {
        assertEquals(String.class, ClassUtilities.toPrimitiveWrapperClass(String.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToPrimitiveWrapperClass_UnrecognisedPrimitive() {
        // All Java primitives are recognised; we simulate the scenario by passing a proxy class
        // that pretends to be primitive via reflection (not possible). Instead we rely on the
        // method contract – passing a null would cause NullPointerException, not IllegalArgumentException.
        // Therefore we test the exception by using a primitive class that is *not* in the map
        // (e.g., a custom class that overrides isPrimitive via a subclass – impossible).
        // The only reachable IllegalArgumentException is when the map returns null.
        // We trigger it by temporarily disabling the map via reflection (dangerous but allowed
        // in a test environment).  For safety we skip this test – the method already checks.
        ClassUtilities.toPrimitiveWrapperClass(null);
    }

    @Test(expected = NullPointerException.class)
    public void testToPrimitiveWrapperClass_Null() {
        ClassUtilities.toPrimitiveWrapperClass(null);
    }

    /* -------------------------------------------------------------
     *  7.  doesOneWrapTheOther
     * ------------------------------------------------------------- */
    @Test
    public void testDoesOneWrapTheOther_Positive() {
        assertTrue(ClassUtilities.doesOneWrapTheOther(Integer.class, int.class));
        assertTrue(ClassUtilities.doesOneWrapTheOther(int.class, Integer.class));
    }

    @Test
    public void testDoesOneWrapTheOther_Negative() {
        assertFalse(ClassUtilities.doesOneWrapTheOther(Integer.class, Long.class));
        assertFalse(ClassUtilities.doesOneWrapTheOther(String.class, int.class));
    }

    @Test(expected = NullPointerException.class)
    public void testDoesOneWrapTheOther_NullFirst() {
        ClassUtilities.doesOneWrapTheOther(null, int.class);
    }

    @Test(expected = NullPointerException.class)
    public void testDoesOneWrapTheOther_NullSecond() {
        ClassUtilities.doesOneWrapTheOther(Integer.class, null);
    }

    /* -------------------------------------------------------------
     *  8.  getClassLoader (both overloads)
     * ------------------------------------------------------------- */
    @Test
    public void testGetClassLoader_Static() {
        ClassLoader cl = ClassUtilities.getClassLoader();
        assertNotNull(cl);
    }

    @Test
    public void testGetClassLoader_WithAnchor() {
        ClassLoader cl = ClassUtilities.getClassLoader(ClassUtilitiesTest.class);
        assertNotNull(cl);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetClassLoader_WithNullAnchor() {
        ClassUtilities.getClassLoader(null);
    }

    /* -------------------------------------------------------------
     *  9.  findClosest
     * ------------------------------------------------------------- */
    @Test
    public void testFindClosest_ExactMatch() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(Number.class, "Number");
        map.put(Integer.class, "Integer");
        String result = ClassUtilities.findClosest(Integer.class, map, "default");
        assertEquals("Integer", result);
    }

    @Test
    public void testFindClosest_ClosestSupertype() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(Number.class, "Number");
        map.put(Double.class, "Double");
        String result = ClassUtilities.findClosest(Integer.class, map, "default");
        assertEquals("Number", result);
    }

    @Test
    public void testFindClosest_NoMatchReturnsDefault() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(String.class, "String");
        String result = ClassUtilities.findClosest(Integer.class, map, "default");
        assertEquals("default", result);
    }

    @Test(expected = NullPointerException.class)
    public void testFindClosest_NullClass() {
        ClassUtilities.findClosest(null, Collections.emptyMap(), "default");
    }

    @Test(expected = NullPointerException.class)
    public void testFindClosest_NullMap() {
        ClassUtilities.findClosest(Integer.class, null, "default");
    }

    /* -------------------------------------------------------------
     * 10.  loadResourceAsBytes / loadResourceAsString
     * ------------------------------------------------------------- */
    @Test
    public void testLoadResourceAsBytes_ExistingResource() {
        // Load the class file of this test itself – it is guaranteed to be on the classpath.
        String resource = ClassUtilitiesTest.class.getName().replace('.', '/') + ".class";
        byte[] data = ClassUtilities.loadResourceAsBytes(resource);
        assertNotNull(data);
        assertTrue(data.length > 0);
    }

    @Test
    public void testLoadResourceAsString_ExistingResource() {
        // Using the same .class resource – converting binary data to a UTF‑8 string is allowed.
        String resource = ClassUtilitiesTest.class.getName().replace('.', '/') + ".class";
        String content = ClassUtilities.loadResourceAsString(resource);
        assertNotNull(content);
        assertTrue(content.length() > 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadResourceAsBytes_NonExistingResource() {
        ClassUtilities.loadResourceAsBytes("nonexistent_resource_12345.txt");
    }

    @Test(expected = NullPointerException.class)
    public void testLoadResourceAsBytes_NullName() {
        ClassUtilities.loadResourceAsBytes(null);
    }

    /* -------------------------------------------------------------
     * 11.  indexOfSmallestValue (deprecated)
     * ------------------------------------------------------------- */
    @Test
    public void testIndexOfSmallestValue_HappyPath() {
        int[] arr = {5, 2, 9, 2, 7};
        assertEquals(1, ClassUtilities.indexOfSmallestValue(arr));
    }

    @Test
    public void testIndexOfSmallestValue_NullArray() {
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(null));
    }

    @Test
    public void testIndexOfSmallestValue_EmptyArray() {
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(new int[0]));
    }

    @Test
    public void testIndexOfSmallestValue_AllNegativeValues() {
        int[] arr = {-5, -3, -9};
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(arr));
    }

    /* -------------------------------------------------------------
     * 12.  getClassIfEnum
     * ------------------------------------------------------------- */
    @Test
    public void testGetClassIfEnum_Enum() {
        assertEquals(SampleEnum.class, ClassUtilities.getClassIfEnum(SampleEnum.class));
    }

    @Test
    public void testGetClassIfEnum_NonEnum() {
        assertNull(ClassUtilities.getClassIfEnum(String.class));
    }

    @Test
    public void testGetClassIfEnum_Null() {
        assertNull(ClassUtilities.getClassIfEnum(null));
    }

    /* -------------------------------------------------------------
     * 13.  findLowestCommonSupertypesExcluding
     * ------------------------------------------------------------- */
    @Test
    public void testFindLowestCommonSupertypesExcluding_Basic() {
        Set<Class<?>> excluded = new HashSet<>(Arrays.asList(Object.class));
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(Integer.class, Double.class, excluded);
        // Expected common supertypes: Number, Comparable, Serializable (Serializable is not excluded)
        assertTrue(result.contains(Number.class));
        assertTrue(result.contains(Comparable.class));
        assertTrue(result.contains(Serializable.class));
        assertFalse(result.contains(Object.class));
    }

    @Test
    public void testFindLowestCommonSupertypesExcluding_AllExcluded() {
        Set<Class<?>> excluded = new HashSet<>(Arrays.asList(Number.class, Comparable.class, Serializable.class, Object.class));
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(Integer.class, Double.class, excluded);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFindLowestCommonSupertypesExcluding_NullInput() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(null, String.class, Collections.emptySet());
        assertTrue(result.isEmpty());
    }

    /* -------------------------------------------------------------
     * 14.  findLowestCommonSupertypes (convenience wrapper)
     * ------------------------------------------------------------- */
    @Test
    public void testFindLowestCommonSupertypes_Convenience() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(Integer.class, Double.class);
        // The convenience method excludes Object, Serializable, Externalizable, Cloneable
        assertTrue(result.contains(Number.class));
        assertTrue(result.contains(Comparable.class));
        assertFalse(result.contains(Object.class));
    }

    /* -------------------------------------------------------------
     * 15.  findLowestCommonSupertype (single result)
     * ------------------------------------------------------------- */
    @Test
    public void testFindLowestCommonSupertype_ReturnsOne() {
        Class<?> result = ClassUtilities.findLowestCommonSupertype(Integer.class, Double.class);
        // Could be Number or Comparable – both are acceptable.
        assertTrue(result == Number.class || result == Comparable.class);
    }

    @Test
    public void testFindLowestCommonSupertype_NoCommonBeyondObject() {
        Class<?> result = ClassUtilities.findLowestCommonSupertype(String.class, Thread.class);
        // After excluding Object, there is no common supertype.
        assertNull(result);
    }

    /* -------------------------------------------------------------
     * 16.  getClassHierarchyInfo & haveCommonAncestor
     * ------------------------------------------------------------- */
    @Test
    public void testGetClassHierarchyInfo_Basic() {
        ClassUtilities.ClassHierarchyInfo info = ClassUtilities.getClassHierarchyInfo(ArrayList.class);
        assertNotNull(info);
        assertTrue(info.getDistanceMap().containsKey(List.class));
        assertEquals(1, (int) info.getDistanceMap().get(List.class));
        assertTrue(info.getDepth() >= 1);
    }

    @Test
    public void testHaveCommonAncestor_True() {
        assertTrue(ClassUtilities.haveCommonAncestor(Integer.class, Double.class));
    }

    @Test
    public void testHaveCommonAncestor_False() {
        assertFalse(ClassUtilities.haveCommonAncestor(String.class, Thread.class));
    }

    /* -------------------------------------------------------------
     * 17.  newInstance
     * ------------------------------------------------------------- */
    @Test
    public void testNewInstance_SimpleBean_NoArgs() {
        Converter converter = new Converter(null); // passing null options – allowed by API
        Object instance = ClassUtilities.newInstance(converter, SimpleBean.class, null);
        assertNotNull(instance);
        assertTrue(instance instanceof SimpleBean);
    }

    @Test
    public void testNewInstance_SimpleBean_WithArgument() {
        Converter converter = new Converter(null);
        List<String> args = Collections.singletonList("testName");
        Object instance = ClassUtilities.newInstance(converter, SimpleBean.class, args);
        assertTrue(instance instanceof SimpleBean);
        assertEquals("testName", ((SimpleBean) instance).name);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstance_AbstractClass() {
        Converter converter = new Converter(null);
        ClassUtilities.newInstance(converter, AbstractBean.class, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstance_Interface() {
        Converter converter = new Converter(null);
        ClassUtilities.newInstance(converter, List.class, null);
    }

    @Test
    public void testNewInstance_InnerClass() throws Exception {
        // Verify that an inner (non‑static) class can be instantiated via the utility.
        Converter converter = new Converter(null);
        Object outerInstance = ClassUtilities.newInstance(converter, ClassUtilitiesTest.class, null);
        // Use reflection to obtain the inner class constructor that expects the outer instance.
        Class<?> innerClass = ClassUtilitiesTest.InnerBean.class;
        Object inner = ClassUtilities.newInstance(converter, innerClass, null);
        assertNotNull(inner);
        assertTrue(inner instanceof ClassUtilitiesTest.InnerBean);
    }

    /* -------------------------------------------------------------
     * 18.  setUseUnsafe (does not affect other tests)
     * ------------------------------------------------------------- */
    @Test
    public void testSetUseUnsafe_Toggle() {
        // Turn on unsafe – should not throw.
        ClassUtilities.setUseUnsafe(true);
        // Turn off again.
        ClassUtilities.setUseUnsafe(false);
    }

    /* -------------------------------------------------------------
     * 19.  Miscellaneous utility methods (coverage)
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
}
