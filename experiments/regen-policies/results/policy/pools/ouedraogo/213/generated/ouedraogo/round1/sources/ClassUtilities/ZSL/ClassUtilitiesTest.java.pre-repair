package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Assert;

import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.cedarsoftware.util.convert.Converter;

/**
 * JUnit 4 test suite for {@link ClassUtilities}.
 *
 * The tests cover:
 * <ul>
 *   <li>Primitive / wrapper handling</li>
 *   <li>Class loading and aliasing</li>
 *   <li>Inheritance distance calculations</li>
 *   <li>Resource loading</li>
 *   <li>Enum detection</li>
 *   <li>Constructor matching and instance creation</li>
 *   <li>Lowest common supertypes utilities</li>
 *   <li>Security checks (positive path)</li>
 * </ul>
 */
public class ClassUtilitiesTest {

    /* -------------------------------------------------------------
     *  Helper classes / enums used by several tests
     * ------------------------------------------------------------- */
    private static final class PrivateCtorClass {
        private PrivateCtorClass() { }
    }

    private static final class MixedCtorClass {
        public MixedCtorClass() { }
        public MixedCtorClass(String s) { this.s = s; }
        public MixedCtorClass(int i, String s) { this.i = i; this.s = s; }

        int i;
        String s;
    }

    private static final class NoPublicCtorClass {
        private NoPublicCtorClass() { }
    }

    private enum SampleEnum { ONE, TWO }

    private static class EnumHolder {
        enum InnerEnum { A, B }
    }

    /* -------------------------------------------------------------
     *  Primitive / Wrapper handling
     * ------------------------------------------------------------- */
    @Test
    public void testGetPrimitiveFromWrapper() {
        Assert.assertEquals(int.class, ClassUtilities.getPrimitiveFromWrapper(Integer.class));
        Assert.assertEquals(void.class, ClassUtilities.getPrimitiveFromWrapper(Void.class));
        Assert.assertNull(ClassUtilities.getPrimitiveFromWrapper(String.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetPrimitiveFromWrapper_Null() {
        ClassUtilities.getPrimitiveFromWrapper(null);
    }

    @Test
    public void testIsPrimitive() {
        Assert.assertTrue(ClassUtilities.isPrimitive(int.class));
        Assert.assertTrue(ClassUtilities.isPrimitive(Integer.class));
        Assert.assertFalse(ClassUtilities.isPrimitive(String.class));
    }

    @Test
    public void testToPrimitiveWrapperClass() {
        Assert.assertEquals(Integer.class, ClassUtilities.toPrimitiveWrapperClass(int.class));
        Assert.assertEquals(String.class, ClassUtilities.toPrimitiveWrapperClass(String.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToPrimitiveWrapperClass_NotPrimitive() {
        ClassUtilities.toPrimitiveWrapperClass(void.class); // void is primitive but mapped, should work
        // Force error with a non‑primitive, non‑wrapper class
        ClassUtilities.toPrimitiveWrapperClass(Object.class);
    }

    @Test
    public void testDoesOneWrapTheOther() {
        Assert.assertTrue(ClassUtilities.doesOneWrapTheOther(Integer.class, int.class));
        Assert.assertTrue(ClassUtilities.doesOneWrapTheOther(int.class, Integer.class));
        Assert.assertFalse(ClassUtilities.doesOneWrapTheOther(Integer.class, long.class));
    }

    /* -------------------------------------------------------------
     *  Class loading and aliasing
     * ------------------------------------------------------------- */
    @Test
    public void testAddAndRemovePermanentClassAlias() {
        ClassUtilities.addPermanentClassAlias(List.class, "myListAlias");
        Assert.assertEquals(List.class, ClassUtilities.forName("myListAlias", ClassUtilities.class.getClassLoader()));
        ClassUtilities.removePermanentClassAlias("myListAlias");
        Assert.assertNull(ClassUtilities.forName("myListAlias", ClassUtilities.class.getClassLoader()));
    }

    @Test
    public void testForName_ExistingClass() {
        Class<?> clazz = ClassUtilities.forName("java.util.ArrayList", ClassUtilities.class.getClassLoader());
        Assert.assertEquals(ArrayList.class, clazz);
    }

    @Test
    public void testForName_NonExisting() {
        Assert.assertNull(ClassUtilities.forName("com.non.existing.FooBar", ClassUtilities.class.getClassLoader()));
    }

    /* -------------------------------------------------------------
     *  Final / private constructor detection
     * ------------------------------------------------------------- */
    @Test
    public void testIsClassFinal() {
        Assert.assertTrue(ClassUtilities.isClassFinal(String.class));
        Assert.assertFalse(ClassUtilities.isClassFinal(ArrayList.class));
    }

    @Test
    public void testAreAllConstructorsPrivate() {
        Assert.assertTrue(ClassUtilities.areAllConstructorsPrivate(PrivateCtorClass.class));
        Assert.assertFalse(ClassUtilities.areAllConstructorsPrivate(MixedCtorClass.class));
    }

    /* -------------------------------------------------------------
     *  Inheritance distance and closest‑match utilities
     * ------------------------------------------------------------- */
    @Test
    public void testComputeInheritanceDistance() {
        // Simple interface distance
        int dist = ClassUtilities.computeInheritanceDistance(ArrayList.class, List.class);
        Assert.assertEquals(1, dist);

        // Primitive vs wrapper – should be zero (treated as same)
        Assert.assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, Integer.class));

        // Unrelated types → -1
        Assert.assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, Integer.class));
    }

    @Test
    public void testFindClosest() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(Number.class, "number");
        map.put(Integer.class, "integer");
        map.put(Object.class, "object");

        // Exact match
        Assert.assertEquals("integer", ClassUtilities.findClosest(Integer.class, map, "default"));

        // Closest by inheritance (Long extends Number)
        Assert.assertEquals("number", ClassUtilities.findClosest(Long.class, map, "default"));
    }

    /* -------------------------------------------------------------
     *  Resource loading
     * ------------------------------------------------------------- */
    @Test
    public void testLoadResourceAsBytes_Existing() {
        // Load a core JDK resource – it exists on the classpath.
        byte[] bytes = ClassUtilities.loadResourceAsBytes("java/lang/String.class");
        Assert.assertNotNull(bytes);
        Assert.assertTrue(bytes.length > 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadResourceAsBytes_NotFound() {
        ClassUtilities.loadResourceAsBytes("nonexistent_resource_12345.txt");
    }

    @Test
    public void testLoadResourceAsString() {
        String content = ClassUtilities.loadResourceAsString("java/lang/String.class");
        Assert.assertNotNull(content);
        Assert.assertTrue(content.length() > 0);
    }

    /* -------------------------------------------------------------
     *  Deprecated utility
     * ------------------------------------------------------------- */
    @Test
    public void testIndexOfSmallestValue() {
        int[] data = {5, 2, 9, 2, 7};
        int idx = ClassUtilities.indexOfSmallestValue(data);
        // The smallest positive value is 2, first occurrence at index 1
        Assert.assertEquals(1, idx);
    }

    @Test
    public void testIndexOfSmallestValue_Empty() {
        Assert.assertEquals(-1, ClassUtilities.indexOfSmallestValue(new int[0]));
    }

    /* -------------------------------------------------------------
     *  Enum detection utilities
     * ------------------------------------------------------------- */
    @Test
    public void testGetClassIfEnum_DirectEnum() {
        Assert.assertEquals(SampleEnum.class, ClassUtilities.getClassIfEnum(SampleEnum.class));
    }

    @Test
    public void testGetClassIfEnum_InnerEnum() {
        Assert.assertEquals(EnumHolder.InnerEnum.class,
                ClassUtilities.getClassIfEnum(EnumHolder.InnerEnum.class));
    }

    @Test
    public void testGetClassIfEnum_NonEnum() {
        Assert.assertNull(ClassUtilities.getClassIfEnum(String.class));
    }

    /* -------------------------------------------------------------
     *  Instance creation via newInstance()
     * ------------------------------------------------------------- */
    private Converter createDefaultConverter() {
        // The Converter constructor accepts a ConverterOptions; passing null yields default behaviour.
        return new Converter(null);
    }

    @Test
    public void testNewInstance_NoArgs() throws Exception {
        Object instance = ClassUtilities.newInstance(createDefaultConverter(),
                MixedCtorClass.class,
                Collections.emptyList());
        Assert.assertTrue(instance instanceof MixedCtorClass);
        MixedCtorClass mc = (MixedCtorClass) instance;
        // No‑arg constructor leaves fields at default values
        Assert.assertEquals(0, mc.i);
        Assert.assertNull(mc.s);
    }

    @Test
    public void testNewInstance_WithMatchingArgs() throws Exception {
        List<Object> args = Arrays.asList(42, "hello");
        Object instance = ClassUtilities.newInstance(createDefaultConverter(),
                MixedCtorClass.class,
                args);
        Assert.assertTrue(instance instanceof MixedCtorClass);
        MixedCtorClass mc = (MixedCtorClass) instance;
        Assert.assertEquals(42, mc.i);
        Assert.assertEquals("hello", mc.s);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstance_InterfaceNotAllowed() {
        ClassUtilities.newInstance(createDefaultConverter(), List.class, Collections.emptyList());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstance_AbstractClassNotAllowed() {
        ClassUtilities.newInstance(createDefaultConverter(), AbstractList.class, Collections.emptyList());
    }

    @Test
    public void testAreAllConstructorsPrivate_SingletonUtility() {
        // Private constructor class should be creatable via newInstance using unsafe fallback (disabled by default)
        // Since unsafe is off, we expect an IllegalArgumentException
        try {
            ClassUtilities.newInstance(createDefaultConverter(), PrivateCtorClass.class, Collections.emptyList());
            Assert.fail("Expected IllegalArgumentException because constructors are private and unsafe is disabled.");
        } catch (IllegalArgumentException e) {
            // Expected path
        }
    }

    /* -------------------------------------------------------------
     *  Common ancestor utilities
     * ------------------------------------------------------------- */
    @Test
    public void testHaveCommonAncestor() {
        Assert.assertTrue(ClassUtilities.haveCommonAncestor(Integer.class, Double.class));
        Assert.assertFalse(ClassUtilities.haveCommonAncestor(String.class, Integer.class));
    }

    @Test
    public void testFindLowestCommonSupertypesExcluding() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(
                Integer.class,
                Double.class,
                Collections.singleton(Number.class)); // exclude Number
        // Expected to contain Comparable (both implement) but not Number
        Assert.assertTrue(result.contains(Comparable.class));
        Assert.assertFalse(result.contains(Number.class));
    }

    @Test
    public void testFindLowestCommonSupertypes_DefaultExclusions() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(Integer.class, Double.class);
        // Default exclusions are Object, Serializable, Externalizable, Cloneable
        // The result should contain Number and Comparable
        Assert.assertTrue(result.contains(Number.class));
        Assert.assertTrue(result.contains(Comparable.class));
    }

    @Test
    public void testFindLowestCommonSupertype() {
        Class<?> supertype = ClassUtilities.findLowestCommonSupertype(Integer.class, Double.class);
        // Could be Number or Comparable – both are valid; we just verify it's one of them.
        Assert.assertTrue(supertype == Number.class || supertype == Comparable.class);
    }

    /* -------------------------------------------------------------
     *  Class hierarchy information
     * ------------------------------------------------------------- */
    @Test
    public void testGetClassHierarchyInfoDepth() {
        ClassUtilities.ClassHierarchyInfo info = ClassUtilities.getClassHierarchyInfo(ArrayList.class);
        // Depth of ArrayList → Object is 2 (ArrayList -> AbstractList -> Object)
        Assert.assertTrue(info.getDepth() >= 2);
        // Distance map must contain self with distance 0
        Assert.assertEquals(Integer.valueOf(0), info.getDistanceMap().get(ArrayList.class));
    }

    /* -------------------------------------------------------------
     *  Security checker positive path (non‑blocked class)
     * ------------------------------------------------------------- */
    @Test
    public void testSecurityChecker_VerifySafeClass() {
        // String class is not blocked – verifyClass should complete silently.
        ClassUtilities.SecurityChecker.verifyClass(String.class);
    }

    /* -------------------------------------------------------------
     *  ClassLoader utilities
     * ------------------------------------------------------------- */
    @Test
    public void testGetClassLoader_NotNull() {
        ClassLoader cl = ClassUtilities.getClassLoader();
        Assert.assertNotNull(cl);
    }

    @Test
    public void testGetClassLoader_WithAnchor() {
        ClassLoader cl = ClassUtilities.getClassLoader(ClassUtilitiesTest.class);
        Assert.assertNotNull(cl);
    }

    /* -------------------------------------------------------------
     *  Additional sanity checks for internal helpers
     * ------------------------------------------------------------- */
    @Test
    public void testShouldPreferNewCandidate() throws Exception {
        // Use reflection to access the private method.
        java.lang.reflect.Method m = ClassUtilities.class.getDeclaredMethod(
                "shouldPreferNewCandidate", Class.class, Class.class);
        m.setAccessible(true);

        // Preference: class over interface
        Assert.assertTrue((Boolean) m.invoke(null, ArrayList.class, List.class));
        // Preference: more specific (String extends CharSequence)
        Assert.assertTrue((Boolean) m.invoke(null, String.class, CharSequence.class));
        // Same class – should not prefer new over current
        Assert.assertFalse((Boolean) m.invoke(null, List.class, List.class));
    }

    @Test
    public void testMatchArgumentsToParameters_ExactAndAssignable() throws Exception {
        // Validate the private matching algorithm via reflection.
        java.lang.reflect.Method matchMethod = ClassUtilities.class.getDeclaredMethod(
                "matchArgumentsToParameters", Converter.class, Collection.class,
                java.lang.reflect.Parameter[].class, boolean.class);
        matchMethod.setAccessible(true);

        // Prepare a constructor with (int, CharSequence)
        Constructor<MixedCtorClass> ctor = MixedCtorClass.class.getDeclaredConstructor(int.class, String.class);
        java.lang.reflect.Parameter[] params = ctor.getParameters();

        // Provide values that require both exact and assignable matches.
        List<Object> values = Arrays.asList("text", 123);
        Object[] matched = (Object[]) matchMethod.invoke(null,
                createDefaultConverter(),
                values,
                params,
                false);

        // Expected order: int first (123), CharSequence second ("text")
        Assert.assertEquals(123, matched[0]);
        Assert.assertEquals("text", matched[1]);
    }

    /* -------------------------------------------------------------
     *  Verify that indexOfSmallestValue ignores negative numbers
     * ------------------------------------------------------------- */
    @Test
    public void testIndexOfSmallestValue_IgnoresNegatives() {
        int[] data = { -1, -5, 3, 2 };
        // The method only considers values >= 0, so smallest is 2 at index 3
        Assert.assertEquals(3, ClassUtilities.indexOfSmallestValue(data));
    }
}
