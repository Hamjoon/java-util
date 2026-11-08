package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.cedarsoftware.util.convert.Converter;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link ClassUtilities}.
 *
 * The tests cover:
 * <ul>
 *   <li>Primitive / wrapper handling</li>
 *   <li>Class loading and aliasing</li>
 *   <li>Inheritance distance calculations</li>
 *   <li>Class hierarchy utilities</li>
 *   <li>Resource loading (positive &amp; negative cases)</li>
 *   <li>Finding the closest matching class</li>
 *   <li>Enum detection</li>
 *   <li>Lowest common supertypes calculations</li>
 *   <li>Instantiation of classes with various constructor visibility</li>
 *   <li>Utility methods such as {@code indexOfSmallestValue}</li>
 * </ul>
 *
 * All helper classes are defined as static inner classes to avoid external dependencies.
 */
public class ClassUtilitiesTest {

    /* --------------------------------------------------------------------- */
    /*  Primitive / Wrapper handling                                         */
    /* --------------------------------------------------------------------- */

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

    @Test
    public void testIsPrimitive() {
        assertTrue(ClassUtilities.isPrimitive(int.class));
        assertTrue(ClassUtilities.isPrimitive(Integer.class));
        assertTrue(ClassUtilities.isPrimitive(boolean.class));
        assertTrue(ClassUtilities.isPrimitive(Boolean.class));
        assertFalse(ClassUtilities.isPrimitive(String.class));
    }

    @Test
    public void testToPrimitiveWrapperClass() {
        assertEquals(Integer.class, ClassUtilities.toPrimitiveWrapperClass(int.class));
        assertEquals(Boolean.class, ClassUtilities.toPrimitiveWrapperClass(boolean.class));
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
        assertFalse(ClassUtilities.doesOneWrapTheOther(Integer.class, long.class));
    }

    /* --------------------------------------------------------------------- */
    /*  Class loading / aliasing                                              */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddAndRemovePermanentClassAlias() {
        ClassUtilities.addPermanentClassAlias(String.class, "myStringAlias");
        assertEquals(String.class, ClassUtilities.forName("myStringAlias", ClassUtilities.class.getClassLoader()));
        ClassUtilities.removePermanentClassAlias("myStringAlias");
        assertNull(ClassUtilities.forName("myStringAlias", ClassUtilities.class.getClassLoader()));
    }

    @Test
    public void testForName_EmptyOrNull() {
        assertNull(ClassUtilities.forName("", ClassUtilities.class.getClassLoader()));
        assertNull(ClassUtilities.forName(null, ClassUtilities.class.getClassLoader()));
    }

    @Test
    public void testForName_NonExistent() {
        assertNull(ClassUtilities.forName("com.cedarsoftware.util.DoesNotExist", ClassUtilities.class.getClassLoader()));
    }

    @Test
    public void testIsClassFinal() {
        assertTrue(ClassUtilities.isClassFinal(String.class)); // String is final
        assertFalse(ClassUtilities.isClassFinal(ArrayList.class));
    }

    @Test
    public void testAreAllConstructorsPrivate() {
        assertTrue(ClassUtilities.areAllConstructorsPrivate(PrivateCtor.class));
        assertFalse(ClassUtilities.areAllConstructorsPrivate(PublicCtor.class));
    }

    /* --------------------------------------------------------------------- */
    /*  Inheritance distance & hierarchy                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testComputeInheritanceDistance_ClassHierarchy() {
        // Direct subclass
        assertEquals(1, ClassUtilities.computeInheritanceDistance(ArrayList.class, List.class));
        // Same class
        assertEquals(0, ClassUtilities.computeInheritanceDistance(List.class, List.class));
        // Unrelated classes
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, Integer.class));
    }

    @Test
    public void testComputeInheritanceDistance_PrimitiveWrapper() {
        // Same primitive
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, int.class));
        // Primitive ↔ wrapper
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, Integer.class));
        // Different primitives
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, long.class));
    }

    @Test
    public void testGetClassHierarchyInfo_Depth() {
        ClassUtilities.ClassHierarchyInfo info = ClassUtilities.getClassHierarchyInfo(ArrayList.class);
        // ArrayList -> AbstractList -> AbstractCollection -> Object = depth 3
        assertEquals(3, info.getDepth());
        // Ensure distance map contains expected entries
        Map<Class<?>, Integer> distances = info.getDistanceMap();
        assertEquals(Integer.valueOf(1), distances.get(AbstractList.class));
        assertEquals(Integer.valueOf(2), distances.get(AbstractCollection.class));
        assertEquals(Integer.valueOf(3), distances.get(Object.class));
    }

    @Test
    public void testHaveCommonAncestor() {
        assertTrue(ClassUtilities.haveCommonAncestor(ArrayList.class, LinkedList.class));
        assertFalse(ClassUtilities.haveCommonAncestor(String.class, Integer.class));
    }

    /* --------------------------------------------------------------------- */
    /*  Enum utilities                                                       */
    /* --------------------------------------------------------------------- */

    enum SampleEnum { A, B }

    static class EnumHolder {
        enum InnerEnum { X, Y }
    }

    @Test
    public void testGetClassIfEnum() {
        assertEquals(SampleEnum.class, ClassUtilities.getClassIfEnum(SampleEnum.class));
        assertEquals(EnumHolder.InnerEnum.class, ClassUtilities.getClassIfEnum(EnumHolder.InnerEnum.class));
        assertNull(ClassUtilities.getClassIfEnum(String.class));
        assertNull(ClassUtilities.getClassIfEnum(null));
    }

    /* --------------------------------------------------------------------- */
    /*  Lowest common supertypes                                              */
    /* --------------------------------------------------------------------- */

    @Test
    public void testFindLowestCommonSupertypesExcluding() {
        Set<Class<?>> excluded = new HashSet<>(Arrays.asList(Object.class, Serializable.class));
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(Integer.class, Double.class, excluded);
        // Expected common supertypes (ignoring Object & Serializable) are Number and Comparable
        assertTrue(result.contains(Number.class));
        assertTrue(result.contains(Comparable.class));
        assertFalse(result.contains(Object.class));
        assertFalse(result.contains(Serializable.class));
    }

    @Test
    public void testFindLowestCommonSupertypes() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(Integer.class, Double.class);
        // By default Object, Serializable, Externalizable, Cloneable are excluded
        assertTrue(result.contains(Number.class));
        assertTrue(result.contains(Comparable.class));
        assertFalse(result.contains(Object.class));
    }

    @Test
    public void testFindLowestCommonSupertype() {
        Class<?> common = ClassUtilities.findLowestCommonSupertype(ArrayList.class, LinkedList.class);
        // Both share AbstractList as the most specific common class (ignoring List, Collection)
        assertEquals(AbstractList.class, common);
    }

    /* --------------------------------------------------------------------- */
    /*  Resource loading                                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testLoadResourceAsBytes_Existing() {
        // The MANIFEST.MF file is present in most classpaths.
        byte[] bytes = ClassUtilities.loadResourceAsBytes("META-INF/MANIFEST.MF");
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadResourceAsBytes_NonExisting() {
        ClassUtilities.loadResourceAsBytes("nonexistent_resource_12345.txt");
    }

    @Test
    public void testLoadResourceAsString_Existing() {
        String content = ClassUtilities.loadResourceAsString("META-INF/MANIFEST.MF");
        assertNotNull(content);
        assertTrue(content.length() > 0);
        // The manifest typically contains the string "Manifest-Version"
        assertTrue(content.contains("Manifest-Version"));
    }

    /* --------------------------------------------------------------------- */
    /*  indexOfSmallestValue (deprecated)                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testIndexOfSmallestValue() {
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(null));
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(new int[0]));
        assertEquals(2, ClassUtilities.indexOfSmallestValue(new int[]{5, 8, 1, 3}));
        // Negative numbers are ignored for the "smallest" logic
        assertEquals(1, ClassUtilities.indexOfSmallestValue(new int[]{10, 0, -5}));
    }

    /* --------------------------------------------------------------------- */
    /*  findClosest – map of candidate classes                               */
    /* --------------------------------------------------------------------- */

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
        map.put(Set.class, "set");
        map.put(Collection.class, "collection");
        // ArrayDeque implements Deque -> Queue -> Collection, but not Set.
        String result = ClassUtilities.findClosest(ArrayDeque.class, map, "default");
        assertEquals("collection", result);
    }

    @Test
    public void testFindClosest_NoMatchReturnsDefault() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(Map.class, "map");
        String result = ClassUtilities.findClosest(String.class, map, "default");
        assertEquals("default", result);
    }

    /* --------------------------------------------------------------------- */
    /*  newInstance – various constructor visibility scenarios                */
    /* --------------------------------------------------------------------- */

    // Simple class with a public no‑arg constructor
    public static class SimplePublic {
        public String value = "public";
    }

    // Class with only private constructors (utility pattern)
    public static final class PrivateCtor {
        private PrivateCtor() {}
        private PrivateCtor(int x) {}
    }

    // Class with a mixed set of constructors
    public static class MixedCtors {
        public MixedCtors() {}
        protected MixedCtors(String s) {}
        MixedCtors(int i) {}
        private MixedCtors(double d) {}
    }

    // Non‑static inner class (requires enclosing instance)
    public class InnerNonStatic {
        public String greet() { return "inner"; }
    }

    @Test
    public void testNewInstance_PublicNoArg() {
        Converter converter = new Converter(null);
        Object instance = ClassUtilities.newInstance(converter, SimplePublic.class, null);
        assertTrue(instance instanceof SimplePublic);
        assertEquals("public", ((SimplePublic) instance).value);
    }

    @Test
    public void testNewInstance_PrivateConstructors() {
        Converter converter = new Converter(null);
        // PrivateCtor has only private constructors; newInstance should succeed via reflection
        Object instance = ClassUtilities.newInstance(converter, PrivateCtor.class, null);
        assertTrue(instance instanceof PrivateCtor);
    }

    @Test
    public void testNewInstance_MixedConstructors_WithArgs() {
        Converter converter = new Converter(null);
        // Provide a matching argument for the protected String constructor
        List<Object> args = Collections.singletonList("test");
        Object instance = ClassUtilities.newInstance(converter, MixedCtors.class, args);
        assertTrue(instance instanceof MixedCtors);
    }

    @Test
    public void testNewInstance_NonStaticInnerClass() {
        Converter converter = new Converter(null);
        // InnerNonStatic requires an instance of the outer test class
        Object instance = ClassUtilities.newInstance(converter, InnerNonStatic.class, null);
        assertTrue(instance instanceof InnerNonStatic);
        assertEquals("inner", ((InnerNonStatic) instance).greet());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstance_Interface() {
        Converter converter = new Converter(null);
        ClassUtilities.newInstance(converter, List.class, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstance_AbstractClass() {
        Converter converter = new Converter(null);
        ClassUtilities.newInstance(converter, AbstractList.class, null);
    }

    /* --------------------------------------------------------------------- */
    /*  ClassLoader utilities                                                */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetClassLoader_DefaultsToSystem() {
        ClassLoader cl = ClassUtilities.getClassLoader(ClassUtilities.class);
        assertNotNull(cl);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetClassLoader_NullAnchor() {
        ClassUtilities.getClassLoader(null);
    }

    /* --------------------------------------------------------------------- */
    /*  setUseUnsafe – ensure no exception when disabling                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testSetUseUnsafe_Disable() {
        // Turning off unsafe should never throw
        ClassUtilities.setUseUnsafe(false);
        // Re‑enable and then disable again to cover both paths
        ClassUtilities.setUseUnsafe(true);
        ClassUtilities.setUseUnsafe(false);
    }

    /* --------------------------------------------------------------------- */
    /*  Helper methods for internal verification (not part of public API)   */
    /* --------------------------------------------------------------------- */

    // Verify that the internal cache for class hierarchy works by calling twice.
    @Test
    public void testClassHierarchyCacheIsUsed() {
        ClassUtilities.ClassHierarchyInfo first = ClassUtilities.getClassHierarchyInfo(HashMap.class);
        ClassUtilities.ClassHierarchyInfo second = ClassUtilities.getClassHierarchyInfo(HashMap.class);
        // The two instances should be the same (cached)
        assertSame(first, second);
    }
}
