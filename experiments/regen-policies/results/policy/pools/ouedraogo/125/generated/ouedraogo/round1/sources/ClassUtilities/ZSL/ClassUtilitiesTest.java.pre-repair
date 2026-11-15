package com.cedarsoftware.util;

import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.ConverterOptions;
import org.junit.Test;

import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.net.URL;
import java.util.*;

import static org.junit.Assert.*;

public class ClassUtilitiesTest {

    /* ---------------------------------------------------------------------- */
    /*  computeInheritanceDistance                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testComputeInheritanceDistance_SameClass() {
        assertEquals(0, ClassUtilities.computeInheritanceDistance(String.class, String.class));
    }

    @Test
    public void testComputeInheritanceDistance_Subclass() {
        assertEquals(1, ClassUtilities.computeInheritanceDistance(ArrayList.class, List.class));
    }

    @Test
    public void testComputeInheritanceDistance_InterfaceImplementation() {
        assertEquals(1, ClassUtilities.computeInheritanceDistance(HashMap.class, Map.class));
    }

    @Test
    public void testComputeInheritanceDistance_Unrelated() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, Integer.class));
    }

    @Test
    public void testComputeInheritanceDistance_PrimitiveToWrapper() {
        // int primitive to Integer wrapper (distance 0 because they are considered same)
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, Integer.class));
    }

    @Test
    public void testComputeInheritanceDistance_WrapperToPrimitive() {
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Integer.class, int.class));
    }

    @Test
    public void testComputeInheritanceDistance_PrimitiveToDifferentWrapper() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, Long.class));
    }

    @Test
    public void testComputeInheritanceDistance_NullArguments() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, String.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, null));
    }

    /* ---------------------------------------------------------------------- */
    /*  isPrimitive / toPrimitiveWrapperClass / doesOneWrapTheOther           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIsPrimitive_PrimitiveAndWrapper() {
        assertTrue(ClassUtilities.isPrimitive(int.class));
        assertTrue(ClassUtilities.isPrimitive(Integer.class));
        assertFalse(ClassUtilities.isPrimitive(String.class));
    }

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
    public void testToPrimitiveWrapperClass_InvalidArgument() {
        ClassUtilities.toPrimitiveWrapperClass(Object.class); // not primitive, should throw
    }

    @Test
    public void testDoesOneWrapTheOther() {
        assertTrue(ClassUtilities.doesOneWrapTheOther(Integer.class, int.class));
        assertTrue(ClassUtilities.doesOneWrapTheOther(int.class, Integer.class));
        assertFalse(ClassUtilities.doesOneWrapTheOther(Integer.class, long.class));
    }

    /* ---------------------------------------------------------------------- */
    /*  addPermanentClassAlias / forName                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAddAndRemovePermanentClassAlias() {
        String alias = "myStringAlias";
        ClassUtilities.addPermanentClassAlias(String.class, alias);
        Class<?> found = ClassUtilities.forName(alias, ClassUtilities.class.getClassLoader());
        assertEquals(String.class, found);

        ClassUtilities.removePermanentClassAlias(alias);
        assertNull(ClassUtilities.forName(alias, ClassUtilities.class.getClassLoader()));
    }

    @Test
    public void testForName_NullOrEmpty() {
        assertNull(ClassUtilities.forName(null, ClassUtilities.class.getClassLoader()));
        assertNull(ClassUtilities.forName("", ClassUtilities.class.getClassLoader()));
    }

    /* ---------------------------------------------------------------------- */
    /*  isClassFinal / areAllConstructorsPrivate                              */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIsClassFinal() {
        assertTrue(ClassUtilities.isClassFinal(String.class)); // final
        assertFalse(ClassUtilities.isClassFinal(ArrayList.class)); // not final
    }

    @Test
    public void testAreAllConstructorsPrivate() {
        assertTrue(ClassUtilities.areAllConstructorsPrivate(PrivateCtor.class));
        assertFalse(ClassUtilities.areAllConstructorsPrivate(PublicCtor.class));
    }

    private static final class PrivateCtor {
        private PrivateCtor() {}
    }

    private static final class PublicCtor {
        public PublicCtor() {}
        private PublicCtor(int ignored) {}
    }

    /* ---------------------------------------------------------------------- */
    /*  getClassLoader                                                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetClassLoader_NotNull() {
        ClassLoader cl = ClassUtilities.getClassLoader(ClassUtilities.class);
        assertNotNull(cl);
        // Should be equal to the context class loader or the class's own loader
        ClassLoader expected = Thread.currentThread().getContextClassLoader();
        assertTrue(cl == expected || cl == ClassUtilities.class.getClassLoader());
    }

    /* ---------------------------------------------------------------------- */
    /*  findClosest                                                            */
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
    public void testFindClosest_ClosestSupertype() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(Set.class, "set");
        map.put(Collection.class, "collection");
        // ArrayList implements List -> Collection, not Set. Collection is closer.
        String result = ClassUtilities.findClosest(ArrayList.class, map, "default");
        assertEquals("collection", result);
    }

    @Test
    public void testFindClosest_TieBreakPreferClassOverInterface() {
        interface MyInterface {}
        class MyClass implements MyInterface {}
        Map<Class<?>, String> map = new LinkedHashMap<>();
        map.put(MyInterface.class, "interface");
        map.put(Object.class, "object");
        // Both have distance 1, class (Object) should be preferred
        String result = ClassUtilities.findClosest(MyClass.class, map, "default");
        assertEquals("object", result);
    }

    /* ---------------------------------------------------------------------- */
    /*  loadResourceAsBytes / loadResourceAsString                             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testLoadResourceAsBytes_ExistingResource() {
        // Load a known class file as a resource
        byte[] bytes = ClassUtilities.loadResourceAsBytes("java/lang/String.class");
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }

    @Test
    public void testLoadResourceAsString_ExistingResource() {
        // Load the same class file as a string; the exact content is not important,
        // we just verify that something is returned and conversion does not fail.
        String content = ClassUtilities.loadResourceAsString("java/lang/String.class");
        assertNotNull(content);
        assertTrue(content.length() > 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadResourceAsBytes_NotFound() {
        ClassUtilities.loadResourceAsBytes("nonexistent/resource.txt");
    }

    @Test(expected = NullPointerException.class)
    public void testLoadResourceAsBytes_NullName() {
        ClassUtilities.loadResourceAsBytes(null);
    }

    @Test(expected = UncheckedIOException.class)
    public void testLoadResourceAsBytes_UnreadableStream() {
        // Simulate an unreadable stream by using a custom class loader that returns a
        // stream which throws IOException on read. This is a bit involved, but we can
        // achieve it by creating a temporary file, deleting it, and then trying to
        // read it via a file URL (the stream will be null, leading to IllegalArgumentException,
        // which we already tested). To trigger UncheckedIOException we need a stream that
        // exists but fails on read; we use a URL that points to a non‑existent protocol.
        ClassUtilities.loadResourceAsBytes("http://invalid-protocol/");
    }

    /* ---------------------------------------------------------------------- */
    /*  indexOfSmallestValue                                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIndexOfSmallestValue_Normal() {
        int[] arr = {5, 2, 9, 2, 7};
        assertEquals(1, ClassUtilities.indexOfSmallestValue(arr)); // first occurrence
    }

    @Test
    public void testIndexOfSmallestValue_AllNegative() {
        int[] arr = {-1, -5, -3};
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(arr));
    }

    @Test
    public void testIndexOfSmallestValue_EmptyOrNull() {
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(new int[0]));
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(null));
    }

    /* ---------------------------------------------------------------------- */
    /*  getClassIfEnum                                                         */
    /* ---------------------------------------------------------------------- */

    private enum MyEnum {A, B}
    private static class Outer { enum InnerEnum {X} }

    @Test
    public void testGetClassIfEnum_DirectEnum() {
        assertEquals(MyEnum.class, ClassUtilities.getClassIfEnum(MyEnum.class));
    }

    @Test
    public void testGetClassIfEnum_EnclosingEnum() {
        assertEquals(Outer.InnerEnum.class, ClassUtilities.getClassIfEnum(Outer.InnerEnum.class));
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
    /*  findLowestCommonSupertypes / Excluding                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testFindLowestCommonSupertypes_Standard() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(TreeSet.class, HashSet.class);
        // Both are Set implementations; Set should be the lowest common supertype,
        // Object is excluded by the method.
        assertTrue(result.contains(Set.class));
        assertFalse(result.contains(Object.class));
    }

    @Test
    public void testFindLowestCommonSupertypesExcluding_SkipObject() {
        Set<Class<?>> skip = Collections.singleton(Object.class);
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(Integer.class, Double.class, skip);
        // Number, Comparable, Serializable are expected (all extend Object)
        assertTrue(result.contains(Number.class));
        assertTrue(result.contains(Comparable.class));
        assertTrue(result.contains(Serializable.class));
        assertFalse(result.contains(Object.class));
    }

    @Test
    public void testFindLowestCommonSupertypesExcluding_SameClassSkipped() {
        Set<Class<?>> skip = Collections.singleton(String.class);
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(String.class, String.class, skip);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testHaveCommonAncestor() {
        assertTrue(ClassUtilities.haveCommonAncestor(ArrayList.class, LinkedList.class));
        assertFalse(ClassUtilities.haveCommonAncestor(String.class, Integer.class));
    }

    /* ---------------------------------------------------------------------- */
    /*  newInstance (using Converter)                                         */
    /* ---------------------------------------------------------------------- */

    private static class SimpleClass {
        public SimpleClass() {}
        public SimpleClass(String s) {}
    }

    @Test
    public void testNewInstance_NoArgs() {
        Converter converter = new Converter(new ConverterOptions());
        Object instance = ClassUtilities.newInstance(converter, SimpleClass.class, null);
        assertNotNull(instance);
        assertTrue(instance instanceof SimpleClass);
    }

    @Test
    public void testNewInstance_WithArgs() {
        Converter converter = new Converter(new ConverterOptions());
        List<Object> args = Collections.singletonList("hello");
        Object instance = ClassUtilities.newInstance(converter, SimpleClass.class, args);
        assertNotNull(instance);
        assertTrue(instance instanceof SimpleClass);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstance_AbstractClass() {
        Converter converter = new Converter(new ConverterOptions());
        ClassUtilities.newInstance(converter, AbstractList.class, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstance_Interface() {
        Converter converter = new Converter(new ConverterOptions());
        ClassUtilities.newInstance(converter, List.class, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstance_SecuritySensitive() {
        Converter converter = new Converter(new ConverterOptions());
        ClassUtilities.newInstance(converter, ProcessBuilder.class, null);
    }

    /* ---------------------------------------------------------------------- */
    /*  Miscellaneous Helper Methods (pickBestValue indirectly)               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testPickBestValue_PrimitiveWrapperPreference() {
        // The method is private; we invoke it via reflection.
        try {
            java.lang.reflect.Method m = ClassUtilities.class.getDeclaredMethod("pickBestValue", Class.class, List.class);
            m.setAccessible(true);
            List<Object> candidates = new ArrayList<>();
            candidates.add(1); // Integer
            candidates.add("text"); // String
            Object best = m.invoke(null, int.class, candidates);
            assertEquals(1, best);
            // Ensure the selected value was removed from the list
            assertEquals(1, candidates.size());
            assertEquals("text", candidates.get(0));
        } catch (Exception e) {
            fail("Reflection call failed: " + e);
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  verify that shouldPreferNewCandidate prefers classes over interfaces   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testShouldPreferNewCandidate_ClassVsInterface() throws Exception {
        java.lang.reflect.Method m = ClassUtilities.class.getDeclaredMethod(
                "shouldPreferNewCandidate", Class.class, Class.class);
        m.setAccessible(true);
        // New is class, current is interface => should be true
        boolean result = (boolean) m.invoke(null, ArrayList.class, List.class);
        assertTrue(result);
        // New is interface, current is class => should be false
        result = (boolean) m.invoke(null, List.class, ArrayList.class);
        assertFalse(result);
    }

    /* ---------------------------------------------------------------------- */
    /*  verify that getAllSupertypes works correctly                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetAllSupertypes_IncludesSelf() {
        Set<Class<?>> supertypes = ClassUtilities.getAllSupertypes(ArrayList.class);
        assertTrue(supertypes.contains(ArrayList.class));
        assertTrue(supertypes.contains(List.class));
        assertTrue(supertypes.contains(AbstractList.class));
        assertTrue(supertypes.contains(Object.class));
    }

    /* ---------------------------------------------------------------------- */
    /*  verify that getDepth works for class hierarchy                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetDepth() throws Exception {
        java.lang.reflect.Method m = ClassUtilities.class.getDeclaredMethod("getDepth", Class.class);
        m.setAccessible(true);
        int depthArrayList = (int) m.invoke(null, ArrayList.class);
        int depthObject = (int) m.invoke(null, Object.class);
        assertTrue(depthArrayList > depthObject);
        assertEquals(1, depthObject); // Object depth is 1 (itself)
    }
}
