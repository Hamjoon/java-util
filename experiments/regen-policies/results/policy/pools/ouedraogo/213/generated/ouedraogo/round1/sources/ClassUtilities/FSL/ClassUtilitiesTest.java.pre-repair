package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Assert;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.util.*;

public class ClassUtilitiesTest {

    /* ----------------------------------------------------------------------
     *  Primitive / Wrapper utilities
     * ---------------------------------------------------------------------- */

    @Test
    public void testGetPrimitiveFromWrapper() {
        Assert.assertEquals(int.class, ClassUtilities.getPrimitiveFromWrapper(Integer.class));
        Assert.assertEquals(void.class, ClassUtilities.getPrimitiveFromWrapper(Void.class));
        Assert.assertNull(ClassUtilities.getPrimitiveFromWrapper(String.class));

        try {
            ClassUtilities.getPrimitiveFromWrapper(null);
            Assert.fail("Expected IllegalArgumentException for null argument");
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    public void testIsPrimitive() {
        Assert.assertTrue(ClassUtilities.isPrimitive(int.class));
        Assert.assertTrue(ClassUtilities.isPrimitive(Integer.class));
        Assert.assertTrue(ClassUtilities.isPrimitive(boolean.class));
        Assert.assertTrue(ClassUtilities.isPrimitive(Boolean.class));
        Assert.assertFalse(ClassUtilities.isPrimitive(String.class));
        Assert.assertFalse(ClassUtilities.isPrimitive(void.class)); // void is not considered a primitive by this method
    }

    @Test
    public void testToPrimitiveWrapperClass() {
        Assert.assertEquals(Integer.class, ClassUtilities.toPrimitiveWrapperClass(int.class));
        Assert.assertEquals(Boolean.class, ClassUtilities.toPrimitiveWrapperClass(boolean.class));
        Assert.assertEquals(String.class, ClassUtilities.toPrimitiveWrapperClass(String.class));

        try {
            ClassUtilities.toPrimitiveWrapperClass(void.class);
            // void has a wrapper (Void), should succeed
            Assert.assertEquals(Void.class, ClassUtilities.toPrimitiveWrapperClass(void.class));
        } catch (IllegalArgumentException e) {
            Assert.fail("Void should be a recognized primitive");
        }

        try {
            ClassUtilities.toPrimitiveWrapperClass(Object.class);
            // non‑primitive, should be returned unchanged
        } catch (Exception e) {
            Assert.fail("Non‑primitive should not throw");
        }

        try {
            ClassUtilities.toPrimitiveWrapperClass(null);
            Assert.fail("Expected NullPointerException for null");
        } catch (NullPointerException ignored) {
        }
    }

    @Test
    public void testDoesOneWrapTheOther() {
        Assert.assertTrue(ClassUtilities.doesOneWrapTheOther(Integer.class, int.class));
        Assert.assertTrue(ClassUtilities.doesOneWrapTheOther(int.class, Integer.class));
        Assert.assertFalse(ClassUtilities.doesOneWrapTheOther(Integer.class, Long.class));
    }

    /* ----------------------------------------------------------------------
     *  Class final / private‑constructor checks
     * ---------------------------------------------------------------------- */

    @Test
    public void testIsClassFinal() {
        Assert.assertTrue(ClassUtilities.isClassFinal(String.class)); // String is final
        Assert.assertFalse(ClassUtilities.isClassFinal(ArrayList.class));
    }

    @Test
    public void testAreAllConstructorsPrivate() {
        Assert.assertTrue(ClassUtilities.areAllConstructorsPrivate(PrivateUtility.class));
        Assert.assertFalse(ClassUtilities.areAllConstructorsPrivate(String.class));
    }

    private static final class PrivateUtility {
        private PrivateUtility() {
        }
    }

    /* ----------------------------------------------------------------------
     *  Inheritance distance and hierarchy information
     * ---------------------------------------------------------------------- */

    @Test
    public void testComputeInheritanceDistance() {
        // direct super‑interface
        Assert.assertEquals(1, ClassUtilities.computeInheritanceDistance(ArrayList.class, List.class));
        // two steps up the hierarchy
        Assert.assertEquals(2, ClassUtilities.computeInheritanceDistance(ArrayList.class, Collection.class));
        // same class
        Assert.assertEquals(0, ClassUtilities.computeInheritanceDistance(HashMap.class, HashMap.class));
        // unrelated types
        Assert.assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, Integer.class));
        // primitive handling (same primitive)
        Assert.assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, int.class));
        // primitive vs wrapper (treated as same primitive)
        Assert.assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, Integer.class));
        // primitive vs different primitive
        Assert.assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, long.class));
    }

    @Test
    public void testClassHierarchyInfoDepthAndDistanceMap() {
        ClassUtilities.ClassHierarchyInfo info = ClassUtilities.getClassHierarchyInfo(ArrayList.class);
        // depth of ArrayList = number of superclasses up to Object (ArrayList -> AbstractList -> AbstractCollection -> Object)
        Assert.assertEquals(3, info.getDepth());

        // distance to List should be 1
        Assert.assertEquals(1, info.getDistance(List.class));
        // distance to Collection should be 2
        Assert.assertEquals(2, info.getDistance(Collection.class));
        // distance to Object should be 3
        Assert.assertEquals(3, info.getDistance(Object.class));
    }

    /* ----------------------------------------------------------------------
     *  Class alias (forName) handling
     * ---------------------------------------------------------------------- */

    @Test
    public void testAddAndRemovePermanentClassAlias() {
        ClassUtilities.addPermanentClassAlias(String.class, "myStringAlias");
        Class<?> aliasClass = ClassUtilities.forName("myStringAlias", ClassUtilities.getClassLoader());
        Assert.assertEquals(String.class, aliasClass);

        ClassUtilities.removePermanentClassAlias("myStringAlias");
        Assert.assertNull(ClassUtilities.forName("myStringAlias", ClassUtilities.getClassLoader()));
    }

    @Test
    public void testForNameWithInvalidInput() {
        Assert.assertNull(ClassUtilities.forName(null, ClassUtilities.getClassLoader()));
        Assert.assertNull(ClassUtilities.forName("", ClassUtilities.getClassLoader()));
        // a class that does not exist should also return null
        Assert.assertNull(ClassUtilities.forName("com.cedarsoftware.util.DoesNotExist", ClassUtilities.getClassLoader()));
    }

    /* ----------------------------------------------------------------------
     *  Class loader utilities
     * ---------------------------------------------------------------------- */

    @Test
    public void testGetClassLoader() {
        ClassLoader loader = ClassUtilities.getClassLoader();
        Assert.assertNotNull(loader);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetClassLoaderWithNullAnchor() {
        ClassUtilities.getClassLoader(null);
    }

    /* ----------------------------------------------------------------------
     *  findClosest – closest match in a candidate map
     * ---------------------------------------------------------------------- */

    @Test
    public void testFindClosestExactAndInheritanceMatch() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(List.class, "list");
        map.put(Collection.class, "collection");
        map.put(Object.class, "object");

        // Exact match – should return the exact entry without distance calculation
        Assert.assertEquals("list", ClassUtilities.findClosest(List.class, map, "default"));
        // Subclass – ArrayList should prefer List over Collection
        Assert.assertEquals("list", ClassUtilities.findClosest(ArrayList.class, map, "default"));
        // Unrelated class – fallback to default
        Assert.assertEquals("default", ClassUtilities.findClosest(String.class, map, "default"));
    }

    /* ----------------------------------------------------------------------
     *  indexOfSmallestValue (deprecated)
     * ---------------------------------------------------------------------- */

    @Test
    public void testIndexOfSmallestValue() {
        int[] data = {5, 3, 9, 1, 4};
        Assert.assertEquals(3, ClassUtilities.indexOfSmallestValue(data));

        int[] empty = {};
        Assert.assertEquals(-1, ClassUtilities.indexOfSmallestValue(empty));

        Assert.assertEquals(-1, ClassUtilities.indexOfSmallestValue(null));
    }

    /* ----------------------------------------------------------------------
     *  Enum detection utilities
     * ---------------------------------------------------------------------- */

    private enum SampleEnum { A, B }

    @Test
    public void testGetClassIfEnum() {
        Assert.assertEquals(SampleEnum.class, ClassUtilities.getClassIfEnum(SampleEnum.class));
        // a non‑enum returns null
        Assert.assertNull(ClassUtilities.getClassIfEnum(String.class));
        // null input returns null
        Assert.assertNull(ClassUtilities.getClassIfEnum(null));
    }

    /* ----------------------------------------------------------------------
     *  Lowest common supertypes utilities
     * ---------------------------------------------------------------------- */

    @Test
    public void testFindLowestCommonSupertypes() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(Integer.class, Double.class);
        // Expected common supertypes (excluding Object, Serializable, etc.) are Number and Comparable
        Assert.assertTrue(result.contains(Number.class));
        Assert.assertTrue(result.contains(Comparable.class));
        Assert.assertFalse(result.contains(Object.class));
    }

    @Test
    public void testFindLowestCommonSupertypesExcluding() {
        Set<Class<?>> excluded = Collections.singleton(Number.class);
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(Integer.class, Double.class, excluded);
        // Number is excluded, so only Comparable should remain
        Assert.assertEquals(1, result.size());
        Assert.assertTrue(result.contains(Comparable.class));
    }

    @Test
    public void testHaveCommonAncestor() {
        Assert.assertTrue(ClassUtilities.haveCommonAncestor(Integer.class, Double.class));
        Assert.assertFalse(ClassUtilities.haveCommonAncestor(String.class, Integer.class));
    }

    /* ----------------------------------------------------------------------
     *  Resource loading
     * ---------------------------------------------------------------------- */

    @Test
    public void testLoadResourceAsBytesAndString() throws UnsupportedEncodingException {
        // The test resource is placed in the same package as this test class.
        // Its content is the literal string "hello world".
        byte[] bytes = ClassUtilities.loadResourceAsBytes("testResource.txt");
        Assert.assertNotNull(bytes);
        Assert.assertEquals("hello world", new String(bytes, "UTF-8"));

        String content = ClassUtilities.loadResourceAsString("testResource.txt");
        Assert.assertEquals("hello world", content);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadResourceAsBytesWhenMissing() {
        ClassUtilities.loadResourceAsBytes("nonexistent.txt");
    }

    @Test(expected = NullPointerException.class)
    public void testLoadResourceAsBytesNullName() {
        ClassUtilities.loadResourceAsBytes(null);
    }

    /* ----------------------------------------------------------------------
     *  Security checking
     * ---------------------------------------------------------------------- */

    @Test(expected = SecurityException.class)
    public void testSecurityCheckerBlocksProcessBuilder() {
        // ProcessBuilder is on the blocked list
        ClassUtilities.SecurityChecker.verifyClass(ProcessBuilder.class);
    }

    @Test
    public void testSecurityCheckerAllowsSafeClass() {
        // String is not blocked
        ClassUtilities.SecurityChecker.verifyClass(String.class);
        // No exception means the test passes
    }

    /* ----------------------------------------------------------------------
     *  newInstance – object creation with constructor matching
     * ---------------------------------------------------------------------- */

    @Test
    public void testNewInstanceSimpleNoArg() {
        Converter converter = new Converter(null); // using null options – the library tolerates this
        Object instance = ClassUtilities.newInstance(converter, SimpleNoArg.class, Collections.emptyList());
        Assert.assertTrue(instance instanceof SimpleNoArg);
    }

    @Test
    public void testNewInstanceWithMatchingArguments() {
        Converter converter = new Converter(null);
        List<Object> args = Arrays.asList("test", 42);
        Object instance = ClassUtilities.newInstance(converter, SimpleTwoArg.class, args);
        Assert.assertTrue(instance instanceof SimpleTwoArg);
        SimpleTwoArg s = (SimpleTwoArg) instance;
        Assert.assertEquals("test", s.str);
        Assert.assertEquals(42, s.num);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstanceWithInterface() {
        Converter converter = new Converter(null);
        ClassUtilities.newInstance(converter, List.class, Collections.emptyList());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstanceWithAbstractClass() {
        Converter converter = new Converter(null);
        ClassUtilities.newInstance(converter, AbstractBase.class, Collections.emptyList());
    }

    // Helper classes for newInstance tests
    public static class SimpleNoArg {
        public SimpleNoArg() {
        }
    }

    public static class SimpleTwoArg {
        public final String str;
        public final int num;

        public SimpleTwoArg(String s, int n) {
            this.str = s;
            this.num = n;
        }
    }

    public static abstract class AbstractBase {
    }

    /* ----------------------------------------------------------------------
     *  Miscellaneous – setUseUnsafe does not throw when disabled
     * ---------------------------------------------------------------------- */

    @Test
    public void testSetUseUnsafe() {
        // Turn off unsafe – should not throw
        ClassUtilities.setUseUnsafe(false);
        // Turn on – may fail on platforms without sun.misc.Unsafe, but we guard with try/catch
        try {
            ClassUtilities.setUseUnsafe(true);
        } catch (Throwable ignored) {
            // If unsafe cannot be instantiated, the method will silently leave it disabled.
        }
    }
}
