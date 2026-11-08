package com.cedarsoftware.util;

import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.ConverterOptions;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.*;

import static org.junit.Assert.*;

public class ClassUtilitiesTest {

    private Converter converter;

    @Before
    public void setUp() {
        // ConverterOptions has a public no‑arg constructor in the library (not shown here)
        // but we can safely create a default instance.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    /* --------------------------------------------------------------------- */
    /*  computeInheritanceDistance                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testComputeInheritanceDistance_SameClass() {
        assertEquals(0, ClassUtilities.computeInheritanceDistance(String.class, String.class));
    }

    @Test
    public void testComputeInheritanceDistance_SuperClass() {
        assertEquals(1, ClassUtilities.computeInheritanceDistance(ArrayList.class, List.class));
    }

    @Test
    public void testComputeInheritanceDistance_InterfaceImplementation() {
        assertEquals(1, ClassUtilities.computeInheritanceDistance(HashSet.class, Set.class));
    }

    @Test
    public void testComputeInheritanceDistance_NoRelation() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, Integer.class));
    }

    @Test
    public void testComputeInheritanceDistance_PrimitiveToWrapper() {
        // int (primitive) -> Integer (wrapper) should be distance 0
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, Integer.class));
        // wrapper -> primitive also distance 0
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Integer.class, int.class));
    }

    @Test
    public void testComputeInheritanceDistance_UnrelatedPrimitives() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, double.class));
    }

    /* --------------------------------------------------------------------- */
    /*  isPrimitive / toPrimitiveWrapperClass / doesOneWrapTheOther           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testIsPrimitive_PrimitiveAndWrapper() {
        assertTrue(ClassUtilities.isPrimitive(int.class));
        assertTrue(ClassUtilities.isPrimitive(Integer.class));
        assertFalse(ClassUtilities.isPrimitive(String.class));
    }

    @Test
    public void testToPrimitiveWrapperClass() {
        assertEquals(Integer.class, ClassUtilities.toPrimitiveWrapperClass(int.class));
        assertEquals(Boolean.class, ClassUtilities.toPrimitiveWrapperClass(boolean.class));
        // non‑primitive returns itself
        assertEquals(String.class, ClassUtilities.toPrimitiveWrapperClass(String.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToPrimitiveWrapperClass_NotAPrimitive() {
        ClassUtilities.toPrimitiveWrapperClass(String.class);
    }

    @Test
    public void testDoesOneWrapTheOther() {
        assertTrue(ClassUtilities.doesOneWrapTheOther(Integer.class, int.class));
        assertTrue(ClassUtilities.doesOneWrapTheOther(int.class, Integer.class));
        assertFalse(ClassUtilities.doesOneWrapTheOther(Integer.class, long.class));
    }

    /* --------------------------------------------------------------------- */
    /*  areAllConstructorsPrivate & isClassFinal                              */
    /* --------------------------------------------------------------------- */

    private static final class PrivateCtorOnly {
        private PrivateCtorOnly() {}
    }

    private static class MixedCtor {
        public MixedCtor() {}
        private MixedCtor(int x) {}
    }

    @Test
    public void testAreAllConstructorsPrivate_AllPrivate() {
        assertTrue(ClassUtilities.areAllConstructorsPrivate(PrivateCtorOnly.class));
    }

    @Test
    public void testAreAllConstructorsPrivate_NotAllPrivate() {
        assertFalse(ClassUtilities.areAllConstructorsPrivate(MixedCtor.class));
    }

    @Test
    public void testIsClassFinal() {
        assertTrue(ClassUtilities.isClassFinal(String.class)); // String is final
        assertFalse(ClassUtilities.isClassFinal(ArrayList.class));
    }

    /* --------------------------------------------------------------------- */
    /*  Class aliasing (addPermanentClassAlias / forName)                     */
    /* --------------------------------------------------------------------- */

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
    public void testForName_NonExistingReturnsNull() {
        assertNull(ClassUtilities.forName("com.non.existing.FooBar", ClassUtilities.class.getClassLoader()));
    }

    /* --------------------------------------------------------------------- */
    /*  getClassLoader                                                       */
    /* --------------------------------------------------------------------- */

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

    /* --------------------------------------------------------------------- */
    /*  loadResourceAsBytes / loadResourceAsString                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testLoadResourceAsBytes_ExistingClassFile() {
        // Load the .class file of this test class – it must exist on the classpath.
        String resourceName = ClassUtilitiesTest.class.getSimpleName() + ".class";
        byte[] data = ClassUtilities.loadResourceAsBytes(resourceName);
        assertNotNull(data);
        assertTrue(data.length > 0);
    }

    @Test
    public void testLoadResourceAsString_ExistingClassFile() {
        String resourceName = ClassUtilitiesTest.class.getSimpleName() + ".class";
        String content = ClassUtilities.loadResourceAsString(resourceName);
        assertNotNull(content);
        // The binary content will contain the string "ClassUtilitiesTest"
        assertTrue(content.contains("ClassUtilitiesTest"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadResourceAsBytes_NotFound() {
        ClassUtilities.loadResourceAsBytes("nonexistent_resource_12345.txt");
    }

    /* --------------------------------------------------------------------- */
    /*  findClosest                                                          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testFindClosest_ExactMatch() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(Integer.class, "int");
        map.put(Number.class, "num");
        map.put(Object.class, "obj");
        String result = ClassUtilities.findClosest(Integer.class, map, "default");
        assertEquals("int", result);
    }

    @Test
    public void testFindClosest_ClosestSuper() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(Number.class, "num");
        map.put(Object.class, "obj");
        // Double extends Number, so Number should be chosen.
        String result = ClassUtilities.findClosest(Double.class, map, "default");
        assertEquals("num", result);
    }

    @Test
    public void testFindClosest_NoMatchReturnsDefault() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(String.class, "str");
        String result = ClassUtilities.findClosest(Integer.class, map, "default");
        assertEquals("default", result);
    }

    /* --------------------------------------------------------------------- */
    /*  indexOfSmallestValue                                                  */
    /* --------------------------------------------------------------------- */

    @Test
    public void testIndexOfSmallestValue_Normal() {
        int[] arr = {5, 2, 9, 2, 7};
        assertEquals(1, ClassUtilities.indexOfSmallestValue(arr));
    }

    @Test
    public void testIndexOfSmallestValue_AllNegative() {
        int[] arr = {-3, -1, -7};
        // All values are <= -1, method ignores negatives, returns -1
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(arr));
    }

    @Test
    public void testIndexOfSmallestValue_EmptyArray() {
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(new int[0]));
    }

    @Test
    public void testIndexOfSmallestValue_NullArray() {
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(null));
    }

    /* --------------------------------------------------------------------- */
    /*  getClassIfEnum                                                       */
    /* --------------------------------------------------------------------- */

    private enum MyEnum { A, B }

    private static class NonEnum { }

    @Test
    public void testGetClassIfEnum_DirectEnum() {
        assertEquals(MyEnum.class, ClassUtilities.getClassIfEnum(MyEnum.class));
    }

    @Test
    public void testGetClassIfEnum_SubclassOfEnum() {
        class EnumSubclass extends MyEnum {
            EnumSubclass(String name) { super(name, 0); }
        }
        // The subclass itself is not an enum, but its superclass is.
        assertEquals(MyEnum.class, ClassUtilities.getClassIfEnum(EnumSubclass.class));
    }

    @Test
    public void testGetClassIfEnum_NoEnum() {
        assertNull(ClassUtilities.getClassIfEnum(NonEnum.class));
    }

    @Test
    public void testGetClassIfEnum_Null() {
        assertNull(ClassUtilities.getClassIfEnum(null));
    }

    /* --------------------------------------------------------------------- */
    /*  findLowestCommonSupertypesExcluding / findLowestCommonSupertypes    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testFindLowestCommonSupertypesExcluding_TreeSetHashSet() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(
                TreeSet.class, HashSet.class,
                Collections.singleton(Object.class));
        // Both implement Set; Set should be the only lowest common supertype.
        assertEquals(Collections.singleton(Set.class), result);
    }

    @Test
    public void testFindLowestCommonSupertypes_TwoNumbers() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(Integer.class, Double.class);
        // Number, Comparable, Serializable are common (Object excluded by method)
        assertTrue(result.contains(Number.class));
        assertTrue(result.contains(Comparable.class));
        assertTrue(result.contains(Serializable.class));
        assertFalse(result.contains(Object.class));
    }

    @Test
    public void testHaveCommonAncestor_Positive() {
        assertTrue(ClassUtilities.haveCommonAncestor(ArrayList.class, LinkedList.class));
    }

    @Test
    public void testHaveCommonAncestor_Negative() {
        assertFalse(ClassUtilities.haveCommonAncestor(String.class, Integer.class));
    }

    /* --------------------------------------------------------------------- */
    /*  getAllSupertypes                                                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetAllSupertypes_IncludesSuperclassAndInterfaces() {
        Set<Class<?>> supertypes = ClassUtilities.getAllSupertypes(ArrayList.class);
        assertTrue(supertypes.contains(AbstractList.class));
        assertTrue(supertypes.contains(List.class));
        assertTrue(supertypes.contains(RandomAccess.class));
        assertTrue(supertypes.contains(Cloneable.class));
        assertTrue(supertypes.contains(Serializable.class));
        assertTrue(supertypes.contains(Object.class));
    }

    /* --------------------------------------------------------------------- */
    /*  Miscellaneous – ensure no unexpected exceptions                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetClassLoader_WithNullAnchorThrows() {
        try {
            ClassUtilities.getClassLoader(null);
            fail("Expected IllegalArgumentException for null anchor");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }
}
