package com.cedarsoftware.util;

import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.ConverterOptions;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.*;

import static org.junit.Assert.*;

public class ClassUtilitiesTest {

    private Converter converter;

    /** -----------------------------------------------------------------------
     *  Simple helper classes used in many tests
     *  -------------------------------------------------------------------- */
    // class with a public no‑arg constructor
    public static class PublicNoArg {
        public PublicNoArg() {}
    }

    // class with several constructors of different visibility
    public static class MixedCtor {
        private MixedCtor() {}
        protected MixedCtor(String s) {}
        public MixedCtor(int i, String s) {}
    }

    // utility class with *only* private constructors
    public static final class PrivateUtility {
        private PrivateUtility() {}
    }

    // class with primitive arguments (used for newInstance)
    public static class PrimitiveCtor {
        public PrimitiveCtor(int i, boolean b) {
            this.i = i;
            this.b = b;
        }
        final int i;
        final boolean b;
    }

    // simple enum for getClassIfEnum tests
    public enum SimpleEnum { ONE, TWO }

    // enum nested inside a class (to test enclosing‑class detection)
    public static class Outer {
        public enum NestedEnum { A, B }
    }

    @Before
    public void setUp() {
        // Converter can be instantiated with a null options object – it will use defaults.
        converter = new Converter((ConverterOptions) null);
    }

    /** -----------------------------------------------------------------------
     *  Tests for primitive handling
     *  -------------------------------------------------------------------- */
    @Test
    public void testIsPrimitive() {
        assertTrue(ClassUtilities.isPrimitive(int.class));
        assertTrue(ClassUtilities.isPrimitive(Integer.class));
        assertTrue(ClassUtilities.isPrimitive(boolean.class));
        assertTrue(ClassUtilities.isPrimitive(Boolean.class));
        assertFalse(ClassUtilities.isPrimitive(String.class));
        assertFalse(ClassUtilities.isPrimitive(void.class)); // void is primitive but not in prims set
    }

    @Test
    public void testToPrimitiveWrapperClass() {
        assertEquals(Integer.class, ClassUtilities.toPrimitiveWrapperClass(int.class));
        assertEquals(Boolean.class, ClassUtilities.toPrimitiveWrapperClass(boolean.class));
        // non‑primitive should be returned unchanged
        assertEquals(String.class, ClassUtilities.toPrimitiveWrapperClass(String.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToPrimitiveWrapperClassWithNonPrimitiveThrows() {
        ClassUtilities.toPrimitiveWrapperClass(String.class);
    }

    @Test
    public void testDoesOneWrapTheOther() {
        assertTrue(ClassUtilities.doesOneWrapTheOther(Integer.class, int.class));
        assertTrue(ClassUtilities.doesOneWrapTheOther(int.class, Integer.class));
        assertFalse(ClassUtilities.doesOneWrapTheOther(Integer.class, long.class));
    }

    /** -----------------------------------------------------------------------
     *  Tests for inheritance distance calculations
     *  -------------------------------------------------------------------- */
    @Test
    public void testComputeInheritanceDistanceSimple() {
        // direct superclass
        assertEquals(1, ClassUtilities.computeInheritanceDistance(ArrayList.class, List.class));
        // same class
        assertEquals(0, ClassUtilities.computeInheritanceDistance(List.class, List.class));
        // unrelated classes
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, Integer.class));
    }

    @Test
    public void testComputeInheritanceDistanceWithInterfaces() {
        assertEquals(1, ClassUtilities.computeInheritanceDistance(HashSet.class, Set.class));
        assertEquals(2, ClassUtilities.computeInheritanceDistance(TreeMap.class, Map.class)); // TreeMap → AbstractMap → Map
    }

    @Test
    public void testComputeInheritanceDistancePrimitivesAndWrappers() {
        // primitive source, wrapper destination
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, Integer.class));
        // wrapper source, primitive destination
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Integer.class, int.class));
        // different primitive types
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, long.class));
    }

    @Test
    public void testComputeInheritanceDistanceNulls() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, String.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, null));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, null));
    }

    /** -----------------------------------------------------------------------
     *  Tests for class alias registration
     *  -------------------------------------------------------------------- */
    @Test
    public void testAddAndRemovePermanentClassAlias() {
        ClassUtilities.addPermanentClassAlias(List.class, "myListAlias");
        assertEquals(List.class, ClassUtilities.forName("myListAlias", ClassUtilities.class.getClassLoader()));

        ClassUtilities.removePermanentClassAlias("myListAlias");
        assertNull(ClassUtilities.forName("myListAlias", ClassUtilities.class.getClassLoader()));
    }

    /** -----------------------------------------------------------------------
     *  Tests for class loading via forName()
     *  -------------------------------------------------------------------- */
    @Test
    public void testForNameValid() {
        assertEquals(String.class, ClassUtilities.forName("java.lang.String", ClassUtilities.class.getClassLoader()));
    }

    @Test
    public void testForNameInvalidReturnsNull() {
        assertNull(ClassUtilities.forName("non.existent.ClassXYZ", ClassUtilities.class.getClassLoader()));
    }

    @Test
    public void testForNameEmptyOrNullReturnsNull() {
        assertNull(ClassUtilities.forName("", ClassUtilities.class.getClassLoader()));
        assertNull(ClassUtilities.forName(null, ClassUtilities.class.getClassLoader()));
    }

    /** -----------------------------------------------------------------------
     *  Tests for final‑class detection
     *  -------------------------------------------------------------------- */
    @Test
    public void testIsClassFinal() {
        assertTrue(ClassUtilities.isClassFinal(String.class)); // String is final
        assertFalse(ClassUtilities.isClassFinal(ArrayList.class));
    }

    /** -----------------------------------------------------------------------
     *  Tests for constructor‑privacy detection
     *  -------------------------------------------------------------------- */
    @Test
    public void testAreAllConstructorsPrivate() {
        assertTrue(ClassUtilities.areAllConstructorsPrivate(PrivateUtility.class));
        assertFalse(ClassUtilities.areAllConstructorsPrivate(PublicNoArg.class));
        assertFalse(ClassUtilities.areAllConstructorsPrivate(MixedCtor.class));
    }

    /** -----------------------------------------------------------------------
     *  Tests for findClosest()
     *  -------------------------------------------------------------------- */
    @Test
    public void testFindClosestExactMatch() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(ArrayList.class, "ArrayList");
        map.put(List.class, "List");
        assertEquals("ArrayList", ClassUtilities.findClosest(ArrayList.class, map, "default"));
    }

    @Test
    public void testFindClosestInheritanceMatch() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(List.class, "List");
        map.put(Collection.class, "Collection");
        // ArrayList is a List -> distance 1, Collection distance 2
        assertEquals("List", ClassUtilities.findClosest(ArrayList.class, map, "default"));
    }

    @Test
    public void testFindClosestTieBreakingPrefersClassOverInterface() {
        Map<Class<?>, String> map = new LinkedHashMap<>();
        map.put(Set.class, "SetInterface");
        map.put(HashSet.class, "HashSetClass"); // same distance 1 from LinkedHashSet
        assertEquals("HashSetClass",
                ClassUtilities.findClosest(LinkedHashSet.class, map, "default"));
    }

    /** -----------------------------------------------------------------------
     *  Tests for resource loading
     *  -------------------------------------------------------------------- */
    @Test
    public void testLoadResourceAsStringAndBytes() {
        // The test resource is placed in src/test/resources/com/cedarsoftware/util/test.txt
        String expected = "Hello Resource!";
        String content = ClassUtilities.loadResourceAsString("com/cedarsoftware/util/test.txt");
        assertEquals(expected, content);

        byte[] bytes = ClassUtilities.loadResourceAsBytes("com/cedarsoftware/util/test.txt");
        assertArrayEquals(expected.getBytes(StandardCharsets.UTF_8), bytes);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadResourceAsBytesNotFound() {
        ClassUtilities.loadResourceAsBytes("nonexistent.resource");
    }

    @Test(expected = NullPointerException.class)
    public void testLoadResourceAsBytesNullName() {
        ClassUtilities.loadResourceAsBytes(null);
    }

    /** -----------------------------------------------------------------------
     *  Tests for indexOfSmallestValue()
     *  -------------------------------------------------------------------- */
    @Test
    public void testIndexOfSmallestValueNormal() {
        int[] arr = {5, 2, 8, 2, 7};
        assertEquals(1, ClassUtilities.indexOfSmallestValue(arr)); // first 2
    }

    @Test
    public void testIndexOfSmallestValueAllNegative() {
        int[] arr = {-5, -2, -8};
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(arr));
    }

    @Test
    public void testIndexOfSmallestValueEmptyOrNull() {
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(new int[0]));
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(null));
    }

    /** -----------------------------------------------------------------------
     *  Tests for enum detection
     *  -------------------------------------------------------------------- */
    @Test
    public void testGetClassIfEnumDirect() {
        assertEquals(SimpleEnum.class, ClassUtilities.getClassIfEnum(SimpleEnum.class));
    }

    @Test
    public void testGetClassIfEnumEnclosing() {
        assertEquals(Outer.NestedEnum.class,
                ClassUtilities.getClassIfEnum(Outer.NestedEnum.class));
        // A non‑enum inner class should return null
        class Inner {}
        assertNull(ClassUtilities.getClassIfEnum(Inner.class));
    }

    @Test
    public void testGetClassIfEnumNull() {
        assertNull(ClassUtilities.getClassIfEnum(null));
    }

    /** -----------------------------------------------------------------------
     *  Tests for newInstance()
     *  -------------------------------------------------------------------- */
    @Test
    public void testNewInstanceNoArgs() {
        Object o = ClassUtilities.newInstance(converter, PublicNoArg.class, null);
        assertTrue(o instanceof PublicNoArg);
    }

    @Test
    public void testNewInstanceWithMatchingArgs() {
        List<Object> args = Arrays.asList(42, true);
        PrimitiveCtor instance = (PrimitiveCtor) ClassUtilities.newInstance(converter, PrimitiveCtor.class, args);
        assertEquals(42, instance.i);
        assertTrue(instance.b);
    }

    @Test
    public void testNewInstanceUsesDefaultsWhenArgsMissing() {
        // No args supplied – primitive defaults should be 0 / false
        PrimitiveCtor instance = (PrimitiveCtor) ClassUtilities.newInstance(converter, PrimitiveCtor.class, Collections.emptyList());
        assertEquals(0, instance.i);
        assertFalse(instance.b);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstanceSecurityRestrictedClass() {
        ClassUtilities.newInstance(converter, ProcessBuilder.class, Collections.emptyList());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstanceInterface() {
        ClassUtilities.newInstance(converter, List.class, Collections.emptyList());
    }

    /** -----------------------------------------------------------------------
     *  Tests for lowest common supertypes utilities
     *  -------------------------------------------------------------------- */
    @Test
    public void testFindLowestCommonSupertypesExcluding() {
        Set<Class<?>> skip = Collections.singleton(Object.class);
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(TreeSet.class, HashSet.class, skip);
        // Both are Sets; Set is the lowest common supertype (Object is skipped)
        assertEquals(Collections.singleton(Set.class), result);
    }

    @Test
    public void testFindLowestCommonSupertypesMultiple() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(Integer.class, Double.class);
        // Number, Comparable and Serializable are the lowest common supertypes (Object is excluded automatically)
        assertTrue(result.contains(Number.class));
        assertTrue(result.contains(Comparable.class));
        assertTrue(result.contains(Serializable.class));
        assertFalse(result.contains(Object.class));
    }

    @Test
    public void testHaveCommonAncestor() {
        assertTrue(ClassUtilities.haveCommonAncestor(ArrayList.class, LinkedList.class));
        assertFalse(ClassUtilities.haveCommonAncestor(String.class, Integer.class));
    }

    /** -----------------------------------------------------------------------
     *  Tests for getClassLoader() – ensure it never returns null
     *  -------------------------------------------------------------------- */
    @Test
    public void testGetClassLoaderNeverNull() {
        ClassLoader cl = ClassUtilities.getClassLoader(ClassUtilitiesTest.class);
        assertNotNull(cl);
    }

    /** -----------------------------------------------------------------------
     *  Tests for internal helper pickBestValue (indirectly via matchArgumentsToParameters)
     *  -------------------------------------------------------------------- */
    @Test
    public void testMatchArgumentsToParametersExactAndConversion() {
        // PrimitiveCtor expects (int, boolean)
        // Provide a String that can be converted to int and a Boolean (exact match)
        List<Object> supplied = Arrays.asList("123", Boolean.TRUE);
        Parameter[] params = PrimitiveCtor.class.getConstructors()[0].getParameters();

        // matchArgumentsToParameters is private; we invoke via reflection
        try {
            java.lang.reflect.Method m = ClassUtilities.class.getDeclaredMethod(
                    "matchArgumentsToParameters", Converter.class, Collection.class, Parameter[].class, boolean.class);
            m.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Object> matched = (List<Object>) m.invoke(null, converter, supplied, params, false);
            // First arg should be converted to Integer (123), second is Boolean.TRUE unchanged
            assertEquals(2, matched.size());
            assertEquals(Integer.valueOf(123), matched.get(0));
            assertEquals(Boolean.TRUE, matched.get(1));
        } catch (Exception e) {
            fail("Reflection call failed: " + e);
        }
    }
}
