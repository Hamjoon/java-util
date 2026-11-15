package com.cedarsoftware.util;

import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.ConverterOptions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.*;

import static org.junit.Assert.*;

public class ClassUtilitiesTest {

    /* ---------------------------------------------------------------------- */
    /*  Helper classes used by several tests                                   */
    /* ---------------------------------------------------------------------- */

    // Simple class with a public no‑arg constructor
    public static class SimplePojo {
        public String value = "default";
    }

    // Class with a private constructor (utility‑style)
    public static class PrivateCtor {
        private PrivateCtor() {}
        public String get() { return "ok"; }
    }

    // Abstract class – cannot be instantiated
    public abstract static class AbstractPojo { }

    // Interface – cannot be instantiated
    public interface SomeInterface { }

    // Class with multiple constructors to test argument matching
    public static class MultiCtor {
        public String s;
        public int i;
        public MultiCtor() { this("zero", 0); }
        public MultiCtor(String s) { this(s, 0); }
        public MultiCtor(String s, int i) { this.s = s; this.i = i; }
    }

    /* ---------------------------------------------------------------------- */
    /*  Setup / Teardown                                                       */
    /* ---------------------------------------------------------------------- */

    @Before
    public void setUp() {
        // Ensure unsafe is disabled for the majority of tests
        ClassUtilities.setUseUnsafe(false);
    }

    @After
    public void tearDown() {
        // Clean any aliases that may have been added during a test
        ClassUtilities.removePermanentClassAlias("AliasForString");
        ClassUtilities.removePermanentClassAlias("AliasForList");
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for primitive / wrapper handling                                  */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIsPrimitive() {
        assertTrue(ClassUtilities.isPrimitive(int.class));
        assertTrue(ClassUtilities.isPrimitive(Integer.class));
        assertFalse(ClassUtilities.isPrimitive(String.class));
    }

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
    public void testToPrimitiveWrapperClass() {
        assertEquals(Integer.class, ClassUtilities.toPrimitiveWrapperClass(int.class));
        assertEquals(void.class, ClassUtilities.toPrimitiveWrapperClass(void.class)); // returns void primitive unchanged? actually primitive -> wrapper returns Void.class
        assertEquals(Void.class, ClassUtilities.toPrimitiveWrapperClass(void.class));
        assertEquals(String.class, ClassUtilities.toPrimitiveWrapperClass(String.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToPrimitiveWrapperClass_NotPrimitive() {
        ClassUtilities.toPrimitiveWrapperClass(String.class);
    }

    @Test(expected = NullPointerException.class)
    public void testToPrimitiveWrapperClass_Null() {
        ClassUtilities.toPrimitiveWrapperClass(null);
    }

    @Test
    public void testDoesOneWrapTheOther() {
        assertTrue(ClassUtilities.doesOneWrapTheOther(Integer.class, int.class));
        assertTrue(ClassUtilities.doesOneWrapTheOther(int.class, Integer.class));
        assertFalse(ClassUtilities.doesOneWrapTheOther(Integer.class, Long.class));
    }

    @Test(expected = NullPointerException.class)
    public void testDoesOneWrapTheOther_NullFirst() {
        ClassUtilities.doesOneWrapTheOther(null, int.class);
    }

    @Test(expected = NullPointerException.class)
    public void testDoesOneWrapTheOther_NullSecond() {
        ClassUtilities.doesOneWrapTheOther(Integer.class, null);
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for inheritance distance                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testComputeInheritanceDistance_Simple() {
        assertEquals(1, ClassUtilities.computeInheritanceDistance(ArrayList.class, List.class));
        assertEquals(2, ClassUtilities.computeInheritanceDistance(ArrayList.class, Collection.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, List.class));
    }

    @Test
    public void testComputeInheritanceDistance_Primitives() {
        // Same primitive
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, int.class));

        // Primitive vs wrapper – considered the same primitive type
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, Integer.class));
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Integer.class, int.class));

        // Different primitives
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, long.class));
    }

    @Test
    public void testComputeInheritanceDistance_NullArguments() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, String.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, null));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, null));
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for class final / private‑constructor detection                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIsClassFinal() {
        assertTrue(ClassUtilities.isClassFinal(String.class));
        assertFalse(ClassUtilities.isClassFinal(ArrayList.class));
    }

    @Test(expected = NullPointerException.class)
    public void testIsClassFinal_Null() {
        ClassUtilities.isClassFinal(null);
    }

    @Test
    public void testAreAllConstructorsPrivate() {
        assertTrue(ClassUtilities.areAllConstructorsPrivate(ClassUtilities.class));
        assertFalse(ClassUtilities.areAllConstructorsPrivate(PrivateCtor.class));
    }

    @Test(expected = NullPointerException.class)
    public void testAreAllConstructorsPrivate_Null() {
        ClassUtilities.areAllConstructorsPrivate(null);
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for class aliasing (add/remove + forName)                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAddAndRemovePermanentClassAlias() {
        ClassUtilities.addPermanentClassAlias(String.class, "AliasForString");
        Class<?> found = ClassUtilities.forName("AliasForString", ClassUtilities.getClassLoader());
        assertEquals(String.class, found);

        ClassUtilities.removePermanentClassAlias("AliasForString");
        assertNull(ClassUtilities.forName("AliasForString", ClassUtilities.getClassLoader()));
    }

    @Test
    public void testForName_StandardClass() {
        Class<?> cls = ClassUtilities.forName("java.util.ArrayList", ClassUtilities.getClassLoader());
        assertEquals(ArrayList.class, cls);
    }

    @Test
    public void testForName_NullOrEmpty() {
        assertNull(ClassUtilities.forName(null, ClassUtilities.getClassLoader()));
        assertNull(ClassUtilities.forName("", ClassUtilities.getClassLoader()));
    }

    @Test
    public void testForName_SecurityBlockedName() {
        // "java.lang.ProcessImpl" is in the blocked name list – should return null
        assertNull(ClassUtilities.forName("java.lang.ProcessImpl", ClassUtilities.getClassLoader()));
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for class loader utilities                                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetClassLoader_NoArg() {
        assertNotNull(ClassUtilities.getClassLoader());
    }

    @Test
    public void testGetClassLoader_WithAnchor() {
        ClassLoader cl = ClassUtilities.getClassLoader(ClassUtilitiesTest.class);
        assertNotNull(cl);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetClassLoader_NullAnchor() {
        ClassUtilities.getClassLoader(null);
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for resource loading                                              */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testLoadResourceAsBytes_Existing() {
        // The compiled class file for java.lang.String is always present
        byte[] data = ClassUtilities.loadResourceAsBytes("java/lang/String.class");
        assertNotNull(data);
        assertTrue(data.length > 0);
    }

    @Test
    public void testLoadResourceAsString_Existing() {
        // Load the same class file as a String – the exact content is not important,
        // we only verify that something was returned.
        String content = ClassUtilities.loadResourceAsString("java/lang/String.class");
        assertNotNull(content);
        assertFalse(content.isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadResourceAsBytes_NotFound() {
        ClassUtilities.loadResourceAsBytes("nonexistent/resource.txt");
    }

    @Test(expected = NullPointerException.class)
    public void testLoadResourceAsBytes_NullName() {
        ClassUtilities.loadResourceAsBytes(null);
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for the deprecated indexOfSmallestValue method                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIndexOfSmallestValue_Normal() {
        int[] a = {5, 2, 9, 2, 7};
        assertEquals(1, ClassUtilities.indexOfSmallestValue(a)); // first 2 at index 1
    }

    @Test
    public void testIndexOfSmallestValue_NullOrEmpty() {
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(null));
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(new int[0]));
    }

    @Test
    public void testIndexOfSmallestValue_AllNegative() {
        int[] a = {-5, -3, -10};
        // All values are <= -1, method should ignore them and return -1
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(a));
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for enum handling                                                 */
    /* ---------------------------------------------------------------------- */

    private enum SampleEnum { ONE, TWO }

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

    /* ---------------------------------------------------------------------- */
    /*  Tests for findClosest                                                   */
    /* ---------------------------------------------------------------------- */

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
        // Collection is closer (distance 1) than Iterable (distance 2)
        assertEquals("collection", result);
    }

    @Test
    public void testFindClosest_NoMatchReturnsDefault() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(Set.class, "set");
        String result = ClassUtilities.findClosest(String.class, map, "default");
        assertEquals("default", result);
    }

    @Test(expected = NullPointerException.class)
    public void testFindClosest_NullClass() {
        ClassUtilities.findClosest(null, Collections.emptyMap(), "x");
    }

    @Test(expected = NullPointerException.class)
    public void testFindClosest_NullMap() {
        ClassUtilities.findClosest(String.class, null, "x");
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for lowest common supertypes (excluding)                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testFindLowestCommonSupertypesExcluding_WithExclude() {
        Set<Class<?>> excluded = new HashSet<>(Collections.singletonList(List.class));
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(
                ArrayList.class, LinkedList.class, excluded);
        // AbstractList is a common supertype and is not excluded
        assertTrue(result.contains(AbstractList.class));
        // List is excluded
        assertFalse(result.contains(List.class));
    }

    @Test
    public void testFindLowestCommonSupertypesExcluding_NoCommon() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(
                String.class, Integer.class, Collections.emptySet());
        // The only common ancestor is Object, which is not excluded, but it is the highest
        // and will be filtered out by the “lowest” algorithm (no lower types).
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFindLowestCommonSupertypes_DefaultExclusions() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(ArrayList.class, LinkedList.class);
        // AbstractList is the most specific common supertype (List is an ancestor of it)
        assertEquals(Collections.singleton(AbstractList.class), result);
    }

    @Test
    public void testFindLowestCommonSupertype() {
        Class<?> result = ClassUtilities.findLowestCommonSupertype(ArrayList.class, LinkedList.class);
        assertEquals(AbstractList.class, result);
    }

    @Test
    public void testHaveCommonAncestor() {
        assertTrue(ClassUtilities.haveCommonAncestor(ArrayList.class, LinkedList.class));
        assertFalse(ClassUtilities.haveCommonAncestor(String.class, Integer.class));
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for ClassHierarchyInfo                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetClassHierarchyInfo_Depth() {
        ClassUtilities.ClassHierarchyInfo info = ClassUtilities.getClassHierarchyInfo(Integer.class);
        // Integer -> Number -> Object => depth 2
        assertEquals(2, info.getDepth());

        // Distance map should contain self distance 0 and Number distance 1
        Map<Class<?>, Integer> distances = info.getDistanceMap();
        assertEquals(Integer.valueOf(0), distances.get(Integer.class));
        assertEquals(Integer.valueOf(1), distances.get(Number.class));
    }

    @Test
    public void testGetClassHierarchyInfo_CacheReuse() {
        ClassUtilities.ClassHierarchyInfo first = ClassUtilities.getClassHierarchyInfo(String.class);
        ClassUtilities.ClassHierarchyInfo second = ClassUtilities.getClassHierarchyInfo(String.class);
        // Should be the same instance because of caching
        assertSame(first, second);
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for newInstance (instantiation logic)                            */
    /* ---------------------------------------------------------------------- */

    private Converter createSimpleConverter() {
        // ConverterOptions is a simple POJO; passing null uses defaults
        return new Converter(new ConverterOptions());
    }

    @Test
    public void testNewInstance_NoArgs() {
        Object o = ClassUtilities.newInstance(createSimpleConverter(), SimplePojo.class, null);
        assertTrue(o instanceof SimplePojo);
    }

    @Test
    public void testNewInstance_WithArgs_MatchingConstructor() {
        List<Object> args = Arrays.asList("hello", 42);
        Object o = ClassUtilities.newInstance(createSimpleConverter(), MultiCtor.class, args);
        assertTrue(o instanceof MultiCtor);
        MultiCtor mc = (MultiCtor) o;
        assertEquals("hello", mc.s);
        assertEquals(42, mc.i);
    }

    @Test
    public void testNewInstance_PrivateConstructor() {
        Object o = ClassUtilities.newInstance(createSimpleConverter(), PrivateCtor.class, null);
        assertTrue(o instanceof PrivateCtor);
        assertEquals("ok", ((PrivateCtor) o).get());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstance_Interface() {
        ClassUtilities.newInstance(createSimpleConverter(), SomeInterface.class, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstance_AbstractClass() {
        ClassUtilities.newInstance(createSimpleConverter(), AbstractPojo.class, null);
    }

    @Test
    public void testSetUseUnsafe_Toggle() {
        // Enable unsafe – should not throw
        ClassUtilities.setUseUnsafe(true);
        // Disable again
        ClassUtilities.setUseUnsafe(false);
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for the deprecated indexOfSmallestValue (covered above)          */
    /* ---------------------------------------------------------------------- */

    /* ---------------------------------------------------------------------- */
    /*  Additional edge‑case checks                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAddPermanentClassAlias_Overwrite() {
        ClassUtilities.addPermanentClassAlias(String.class, "AliasForString");
        // Overwrite with a different class
        ClassUtilities.addPermanentClassAlias(Integer.class, "AliasForString");
        Class<?> found = ClassUtilities.forName("AliasForString", ClassUtilities.getClassLoader());
        assertEquals(Integer.class, found);
    }

    @Test
    public void testRemovePermanentClassAlias_NonExisting() {
        // Removing a non‑existing alias should be a no‑op and not throw
        ClassUtilities.removePermanentClassAlias("NonExistingAlias");
    }

    @Test
    public void testLoadResourceAsString_NullPointerOnNullName() {
        try {
            ClassUtilities.loadResourceAsString(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException ignored) { }
    }

    @Test
    public void testLoadResourceAsBytes_NullPointerOnNullName() {
        try {
            ClassUtilities.loadResourceAsBytes(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException ignored) { }
    }

    @Test
    public void testGetClassIfEnum_InnerEnum() {
        class Outer {
            enum Inner { A, B }
        }
        // The enum class is Outer.Inner
        Class<?> result = ClassUtilities.getClassIfEnum(Outer.Inner.class);
        assertEquals(Outer.Inner.class, result);
    }
}
