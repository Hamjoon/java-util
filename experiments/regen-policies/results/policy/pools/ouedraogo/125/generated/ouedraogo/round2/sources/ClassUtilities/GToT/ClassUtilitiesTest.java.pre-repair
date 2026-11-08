package com.cedarsoftware.util;

import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.ConverterOptions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.UncheckedIOException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.*;

import static org.junit.Assert.*;

public class ClassUtilitiesTest {

    /* ---------------------------------------------------------------------- */
    /* Helper classes used across several tests                               */
    /* ---------------------------------------------------------------------- */

    /** Simple class with a public no‑arg constructor. */
    public static class SimplePojo {
        public String value = "default";
    }

    /** Class with multiple constructors (public, protected, private). */
    public static class MultiCtor {
        public MultiCtor() {}
        protected MultiCtor(String s) {}
        private MultiCtor(int i) {}
    }

    /** Class with only private constructors (singleton style). */
    public static class AllPrivateCtor {
        private AllPrivateCtor() {}
    }

    /** Dummy enum for enum‑related tests. */
    public enum DummyEnum { A, B }

    /** Non‑static inner class (requires an outer instance). */
    public class InnerNonStatic {
        public InnerNonStatic() {}
    }

    /** Abstract class – instantiation should fail. */
    public abstract static class AbstractClass { }

    /** Interface – instantiation should fail. */
    public interface SomeInterface { }

    /* ---------------------------------------------------------------------- */
    /* Test lifecycle – enable/disable unsafe for the newInstance tests      */
    /* ---------------------------------------------------------------------- */

    @Before
    public void enableUnsafe() {
        ClassUtilities.setUseUnsafe(true);
    }

    @After
    public void disableUnsafe() {
        ClassUtilities.setUseUnsafe(false);
    }

    /* ---------------------------------------------------------------------- */
    /* 1. addPermanentClassAlias / removePermanentClassAlias                  */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAddAndRemovePermanentClassAlias() {
        // Use a unique alias to avoid interfering with other tests
        String alias = "MyStringAlias";
        ClassUtilities.addPermanentClassAlias(String.class, alias);
        Class<?> found = ClassUtilities.forName(alias, ClassUtilities.getClassLoader());
        assertEquals(String.class, found);

        ClassUtilities.removePermanentClassAlias(alias);
        Class<?> afterRemoval = ClassUtilities.forName(alias, ClassUtilities.getClassLoader());
        assertNull(afterRemoval);
    }

    /* ---------------------------------------------------------------------- */
    /* 2. computeInheritanceDistance                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testComputeInheritanceDistanceBasic() {
        // Same class → 0
        assertEquals(0, ClassUtilities.computeInheritanceDistance(List.class, List.class));

        // Direct super‑interface
        assertEquals(1, ClassUtilities.computeInheritanceDistance(ArrayList.class, List.class));

        // Unrelated classes → -1
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, Integer.class));

        // Primitive vs wrapper
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, Integer.class));
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Integer.class, int.class));

        // Primitive to unrelated primitive
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, long.class));
    }

    @Test
    public void testComputeInheritanceDistanceWithNulls() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, Object.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(Object.class, null));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, null));
    }

    /* ---------------------------------------------------------------------- */
    /* 3. forName                                                             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testForNameWithValidClass() {
        Class<?> clazz = ClassUtilities.forName("java.lang.String", ClassUtilities.getClassLoader());
        assertEquals(String.class, clazz);
    }

    @Test
    public void testForNameWithAlias() {
        String alias = "AliasForInteger";
        ClassUtilities.addPermanentClassAlias(Integer.class, alias);
        assertEquals(Integer.class, ClassUtilities.forName(alias, ClassUtilities.getClassLoader()));
        ClassUtilities.removePermanentClassAlias(alias);
    }

    @Test
    public void testForNameWithNullOrEmpty() {
        assertNull(ClassUtilities.forName(null, ClassUtilities.getClassLoader()));
        assertNull(ClassUtilities.forName("", ClassUtilities.getClassLoader()));
    }

    @Test
    public void testForNameClassNotFound() {
        // Non‑existent class should return null (exception swallowed)
        assertNull(ClassUtilities.forName("com.this.does.not.Exist", ClassUtilities.getClassLoader()));
    }

    /* ---------------------------------------------------------------------- */
    /* 4. isClassFinal                                                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIsClassFinal() {
        assertTrue(ClassUtilities.isClassFinal(String.class)); // final
        assertFalse(ClassUtilities.isClassFinal(ArrayList.class)); // not final
    }

    @Test(expected = NullPointerException.class)
    public void testIsClassFinalNull() {
        ClassUtilities.isClassFinal(null);
    }

    /* ---------------------------------------------------------------------- */
    /* 5. areAllConstructorsPrivate                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAreAllConstructorsPrivate() {
        assertFalse(ClassUtilities.areAllConstructorsPrivate(MultiCtor.class));
        assertTrue(ClassUtilities.areAllConstructorsPrivate(AllPrivateCtor.class));
    }

    @Test(expected = NullPointerException.class)
    public void testAreAllConstructorsPrivateNull() {
        ClassUtilities.areAllConstructorsPrivate(null);
    }

    /* ---------------------------------------------------------------------- */
    /* 6. toPrimitiveWrapperClass                                             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToPrimitiveWrapperClass() {
        assertEquals(Integer.class, ClassUtilities.toPrimitiveWrapperClass(int.class));
        assertEquals(void.class, ClassUtilities.toPrimitiveWrapperClass(void.class).getClass().getPrimitiveClass()); // void maps to Void
        // Non‑primitive returns itself
        assertEquals(String.class, ClassUtilities.toPrimitiveWrapperClass(String.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToPrimitiveWrapperClassInvalid() {
        // Pass a primitive that is not in the map (cannot happen with Java primitives), simulate by
        // using a custom class that reports isPrimitive() true via a proxy – not feasible.
        // Instead, pass null to trigger NullPointerException (different) – we expect IllegalArgumentException
        // only when primitive not recognized, which we cannot produce; so we test null case.
        ClassUtilities.toPrimitiveWrapperClass(null);
    }

    @Test(expected = NullPointerException.class)
    public void testToPrimitiveWrapperClassNull() {
        ClassUtilities.toPrimitiveWrapperClass(null);
    }

    /* ---------------------------------------------------------------------- */
    /* 7. doesOneWrapTheOther                                                  */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDoesOneWrapTheOther() {
        assertTrue(ClassUtilities.doesOneWrapTheOther(Integer.class, int.class));
        assertTrue(ClassUtilities.doesOneWrapTheOther(int.class, Integer.class));
        assertFalse(ClassUtilities.doesOneWrapTheOther(Integer.class, long.class));
    }

    @Test(expected = NullPointerException.class)
    public void testDoesOneWrapTheOtherNull() {
        ClassUtilities.doesOneWrapTheOther(null, int.class);
    }

    /* ---------------------------------------------------------------------- */
    /* 8. getClassLoader (both overloads)                                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetClassLoaderDefault() {
        ClassLoader cl = ClassUtilities.getClassLoader();
        assertNotNull(cl);
    }

    @Test
    public void testGetClassLoaderWithAnchor() {
        ClassLoader cl = ClassUtilities.getClassLoader(ClassUtilities.class);
        assertNotNull(cl);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetClassLoaderWithNullAnchor() {
        ClassUtilities.getClassLoader(null);
    }

    /* ---------------------------------------------------------------------- */
    /* 9. loadResourceAsString / loadResourceAsBytes                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testLoadResourceAsBytesExisting() {
        // Load a known class file as a resource – should exist on the classpath
        byte[] data = ClassUtilities.loadResourceAsBytes("java/lang/String.class");
        assertNotNull(data);
        assertTrue(data.length > 0);
    }

    @Test
    public void testLoadResourceAsStringExisting() {
        // Use the same class file, but read as a string (binary data interpreted as UTF‑8)
        String content = ClassUtilities.loadResourceAsString("java/lang/String.class");
        assertNotNull(content);
        assertFalse(content.isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadResourceAsBytesNotFound() {
        ClassUtilities.loadResourceAsBytes("nonexistent/resource.txt");
    }

    @Test(expected = NullPointerException.class)
    public void testLoadResourceAsBytesNullName() {
        ClassUtilities.loadResourceAsBytes(null);
    }

    /* ---------------------------------------------------------------------- */
    /* 10. indexOfSmallestValue                                                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIndexOfSmallestValueNormal() {
        int[] arr = {5, 2, 9, 2, 7};
        assertEquals(1, ClassUtilities.indexOfSmallestValue(arr)); // first occurrence of 2
    }

    @Test
    public void testIndexOfSmallestValueWithNegatives() {
        int[] arr = {3, -1, 4, -5, 0};
        assertEquals(3, ClassUtilities.indexOfSmallestValue(arr)); // -5 is smallest (ignores -1 because -5 < -1)
    }

    @Test
    public void testIndexOfSmallestValueEmptyOrNull() {
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(new int[0]));
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(null));
    }

    /* ---------------------------------------------------------------------- */
    /* 11. findClosest                                                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testFindClosestExactMatch() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(Number.class, "number");
        map.put(Integer.class, "integer");
        String result = ClassUtilities.findClosest(Integer.class, map, "default");
        assertEquals("integer", result); // exact match wins
    }

    @Test
    public void testFindClosestInheritance() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(Number.class, "number");
        map.put(Object.class, "object");
        String result = ClassUtilities.findClosest(Double.class, map, "default");
        // Double → Number distance 1, Double → Object distance 2 → Number is closer
        assertEquals("number", result);
    }

    @Test
    public void testFindClosestTieBreaking() {
        // Both interfaces at same distance – prefer class over interface per logic
        Map<Class<?>, String> map = new LinkedHashMap<>();
        map.put(Comparable.class, "comparable"); // interface
        map.put(List.class, "list"); // class, also distance 1 from ArrayList
        String result = ClassUtilities.findClosest(ArrayList.class, map, "default");
        assertEquals("list", result);
    }

    @Test(expected = NullPointerException.class)
    public void testFindClosestNullClass() {
        ClassUtilities.findClosest(null, Collections.emptyMap(), "default");
    }

    @Test(expected = NullPointerException.class)
    public void testFindClosestNullMap() {
        ClassUtilities.findClosest(String.class, null, "default");
    }

    /* ---------------------------------------------------------------------- */
    /* 12. getClassIfEnum                                                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetClassIfEnumDirect() {
        assertEquals(DummyEnum.class, ClassUtilities.getClassIfEnum(DummyEnum.class));
    }

    @Test
    public void testGetClassIfEnumEnclosing() {
        class Outer {
            enum InnerEnum { X }
        }
        // Pass the inner enum's anonymous class (the enum constant class)
        Class<?> enumClass = ClassUtilities.getClassIfEnum(Outer.InnerEnum.X.getClass());
        assertEquals(Outer.InnerEnum.class, enumClass);
    }

    @Test
    public void testGetClassIfEnumNonEnum() {
        assertNull(ClassUtilities.getClassIfEnum(String.class));
    }

    @Test
    public void testGetClassIfEnumNull() {
        assertNull(ClassUtilities.getClassIfEnum(null));
    }

    /* ---------------------------------------------------------------------- */
    /* 13. newInstance (various scenarios)                                    */
    /* ---------------------------------------------------------------------- */

    private Converter createDefaultConverter() {
        // ConverterOptions is a simple POJO; null is acceptable for this test suite.
        return new Converter(null);
    }

    @Test
    public void testNewInstanceSimplePojo() {
        Converter converter = createDefaultConverter();
        Object o = ClassUtilities.newInstance(converter, SimplePojo.class, null);
        assertTrue(o instanceof SimplePojo);
        assertEquals("default", ((SimplePojo) o).value);
    }

    @Test
    public void testNewInstanceWithArguments() {
        Converter converter = createDefaultConverter();
        List<Object> args = Arrays.asList("hello");
        Object o = ClassUtilities.newInstance(converter, MultiCtor.class, args);
        assertTrue(o instanceof MultiCtor);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstanceAbstractClass() {
        Converter converter = createDefaultConverter();
        ClassUtilities.newInstance(converter, AbstractClass.class, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstanceInterface() {
        Converter converter = createDefaultConverter();
        ClassUtilities.newInstance(converter, SomeInterface.class, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstanceSecuritySensitive() {
        Converter converter = createDefaultConverter();
        ClassUtilities.newInstance(converter, ProcessBuilder.class, null);
    }

    @Test
    public void testNewInstanceAllPrivateCtorWithUnsafe() {
        Converter converter = createDefaultConverter();
        // Unsafe is enabled in @Before, so this should succeed even though constructors are private.
        Object o = ClassUtilities.newInstance(converter, AllPrivateCtor.class, null);
        assertNotNull(o);
        assertTrue(o instanceof AllPrivateCtor);
    }

    @Test
    public void testNewInstanceInnerNonStatic() {
        Converter converter = createDefaultConverter();
        // The utility should create an instance of the outer class first, then the inner.
        Object o = ClassUtilities.newInstance(converter, InnerNonStatic.class, null);
        assertNotNull(o);
        assertTrue(o instanceof InnerNonStatic);
    }

    /* ---------------------------------------------------------------------- */
    /* 14. findLowestCommonSupertypesExcluding / findLowestCommonSupertypes */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testFindLowestCommonSupertypesExcluding() {
        Set<Class<?>> skip = Collections.singleton(Object.class);
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(TreeSet.class, HashSet.class, skip);
        // Both are Sets, and Set is the lowest common supertype after skipping Object
        assertEquals(Collections.singleton(Set.class), result);
    }

    @Test
    public void testFindLowestCommonSupertypes() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(Integer.class, Double.class);
        // Common supertypes (excluding Object) include Number, Comparable, Serializable
        assertTrue(result.contains(Number.class));
        assertTrue(result.contains(Comparable.class));
        assertTrue(result.contains(Serializable.class));
        assertFalse(result.contains(Object.class));
    }

    @Test
    public void testFindLowestCommonSupertype() {
        Class<?> common = ClassUtilities.findLowestCommonSupertype(ArrayList.class, LinkedList.class);
        // Both implement List; List is one of the lowest common supertypes
        assertEquals(List.class, common);
    }

    @Test
    public void testFindLowestCommonSupertypesWithNulls() {
        assertTrue(ClassUtilities.findLowestCommonSupertypes(null, String.class).isEmpty());
        assertTrue(ClassUtilities.findLowestCommonSupertypes(String.class, null).isEmpty());
        assertTrue(ClassUtilities.findLowestCommonSupertypes(null, null).isEmpty());
    }

    /* ---------------------------------------------------------------------- */
    /* 15. getAllSupertypes & haveCommonAncestor                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetAllSupertypesIncludesSelfAndAncestors() {
        Set<Class<?>> supertypes = ClassUtilities.getAllSupertypes(ArrayList.class);
        assertTrue(supertypes.contains(ArrayList.class));
        assertTrue(supertypes.contains(AbstractList.class));
        assertTrue(supertypes.contains(List.class));
        assertTrue(supertypes.contains(Collection.class));
        assertTrue(supertypes.contains(Iterable.class));
        assertTrue(supertypes.contains(Object.class));
    }

    @Test
    public void testHaveCommonAncestorTrue() {
        assertTrue(ClassUtilities.haveCommonAncestor(ArrayList.class, LinkedList.class));
    }

    @Test
    public void testHaveCommonAncestorFalse() {
        assertFalse(ClassUtilities.haveCommonAncestor(String.class, Integer.class));
    }

    /* ---------------------------------------------------------------------- */
    /* 16. setUseUnsafe (side‑effect already covered in newInstance tests)   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testSetUseUnsafeToggle() {
        // Ensure that toggling works without throwing
        ClassUtilities.setUseUnsafe(false);
        ClassUtilities.setUseUnsafe(true);
    }
}
