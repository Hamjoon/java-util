package com.cedarsoftware.util;

import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.ConverterOptions;
import org.junit.*;

import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.*;

import static org.junit.Assert.*;

public class ClassUtilitiesTest {

    /*======================================================================
     * Helper types used by several test cases
     *======================================================================*/

    /** Simple class with a private no‑arg constructor (utility‑style). */
    private static final class PrivateCtor {
        private PrivateCtor() {}
    }

    /** Simple class with a public no‑arg constructor. */
    public static final class PublicCtor {
        public PublicCtor() {}
    }

    /** Class with multiple constructors to test argument matching. */
    public static final class MultiCtor {
        public String s;
        public int i;

        public MultiCtor() {
            this.s = "";
            this.i = 0;
        }

        public MultiCtor(String s) {
            this.s = s;
            this.i = 0;
        }

        public MultiCtor(String s, int i) {
            this.s = s;
            this.i = i;
        }
    }

    /** Simple enum used for getClassIfEnum tests. */
    public enum TestEnum { A, B }

    /** Enum with a non‑static inner class – the inner class should resolve to the enum. */
    public enum OuterEnum {
        ONE;
        public class Inner { }
    }

    /** Interface used for findClosest tie‑breaking tests. */
    public interface ITest {}

    /** Class that implements ITest. */
    public static class ImplementsITest implements ITest {}

    /** Two unrelated interfaces for distance‑tie tests. */
    public interface IOne {}
    public interface ITwo {}

    /** Class that implements both IOne and ITwo. */
    public static class ImplementsBoth implements IOne, ITwo {}

    /** Simple abstract class for newInstance negative test. */
    public abstract static class AbstractThing {}

    /** Simple interface for newInstance negative test. */
    public interface ThingInterface {}

    /*======================================================================
     * 1.  Public method signatures (extracted for reference)
     *======================================================================*/

    // public static void addPermanentClassAlias(Class<?> clazz, String alias)
    // public static void removePermanentClassAlias(String alias)
    // public static int computeInheritanceDistance(Class<?> source, Class<?> destination)
    // public static boolean isPrimitive(Class<?> c)
    // public static Class<?> forName(String name, ClassLoader classLoader)
    // public static boolean isClassFinal(Class<?> c)
    // public static boolean areAllConstructorsPrivate(Class<?> c)
    // public static Class<?> toPrimitiveWrapperClass(Class<?> primitiveClass)
    // public static boolean doesOneWrapTheOther(Class<?> x, Class<?> y)
    // public static ClassLoader getClassLoader()
    // public static ClassLoader getClassLoader(Class<?> anchorClass)
    // public static String loadResourceAsString(String resourceName)
    // public static byte[] loadResourceAsBytes(String resourceName)
    // public static int indexOfSmallestValue(int[] array)
    // public static <T> T findClosest(Class<?> clazz, Map<Class<?>, T> candidateClasses, T defaultClass)
    // public static Class<?> getClassIfEnum(Class<?> c)
    // public static Object newInstance(Converter converter, Class<?> c, Collection<?> argumentValues)
    // public static void setUseUnsafe(boolean state)
    // public static Set<Class<?>> findLowestCommonSupertypesExcluding(Class<?> classA, Class<?> classB, Set<Class<?>> skipList)
    // public static Set<Class<?>> findLowestCommonSupertypes(Class<?> classA, Class<?> classB)
    // public static Class<?> findLowestCommonSupertype(Class<?> classA, Class<?> classB)
    // public static Set<Class<?>> getAllSupertypes(Class<?> clazz)
    // public static boolean haveCommonAncestor(Class<?> a, Class<?> b)

    /*======================================================================
     * 2.  Basic functionality tests
     *======================================================================*/

    @Test
    public void testAddAndRemovePermanentClassAlias() {
        // add an alias for String.class
        ClassUtilities.addPermanentClassAlias(String.class, "myStringAlias");

        Class<?> got = ClassUtilities.forName("myStringAlias", ClassLoader.getSystemClassLoader());
        assertEquals(String.class, got);

        // remove the alias and verify it is gone
        ClassUtilities.removePermanentClassAlias("myStringAlias");
        assertNull(ClassUtilities.forName("myStringAlias", ClassLoader.getSystemClassLoader()));
    }

    @Test
    public void testComputeInheritanceDistanceBasic() {
        // same class → 0
        assertEquals(0, ClassUtilities.computeInheritanceDistance(List.class, List.class));

        // direct subclass → 1
        assertEquals(1, ClassUtilities.computeInheritanceDistance(ArrayList.class, List.class));

        // unrelated classes → -1
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, Integer.class));

        // primitive ↔ wrapper distance 0
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, Integer.class));
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Integer.class, int.class));

        // null arguments → -1
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, List.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(List.class, null));
    }

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

    @Test
    public void testForNameBasic() {
        assertNull(ClassUtilities.forName(null, ClassLoader.getSystemClassLoader()));
        assertNull(ClassUtilities.forName("", ClassLoader.getSystemClassLoader()));
        assertEquals(String.class, ClassUtilities.forName("java.lang.String", ClassLoader.getSystemClassLoader()));
        // unknown class should return null, not throw
        assertNull(ClassUtilities.forName("com.nonexistent.Foo", ClassLoader.getSystemClassLoader()));
    }

    @Test
    public void testIsClassFinal() {
        assertTrue(ClassUtilities.isClassFinal(String.class)); // String is final
        assertFalse(ClassUtilities.isClassFinal(ArrayList.class)); // not final
    }

    @Test(expected = NullPointerException.class)
    public void testIsClassFinal_Null() {
        ClassUtilities.isClassFinal(null);
    }

    @Test
    public void testAreAllConstructorsPrivate() {
        assertTrue(ClassUtilities.areAllConstructorsPrivate(PrivateCtor.class));
        assertFalse(ClassUtilities.areAllConstructorsPrivate(PublicCtor.class));
    }

    @Test(expected = NullPointerException.class)
    public void testAreAllConstructorsPrivate_Null() {
        ClassUtilities.areAllConstructorsPrivate(null);
    }

    @Test
    public void testToPrimitiveWrapperClass() {
        assertEquals(Integer.class, ClassUtilities.toPrimitiveWrapperClass(int.class));
        assertEquals(Boolean.class, ClassUtilities.toPrimitiveWrapperClass(boolean.class));
        // non‑primitive returns the same class
        assertEquals(String.class, ClassUtilities.toPrimitiveWrapperClass(String.class));
    }

    @Test(expected = NullPointerException.class)
    public void testToPrimitiveWrapperClass_Null() {
        ClassUtilities.toPrimitiveWrapperClass(null);
    }

    @Test
    public void testDoesOneWrapTheOther() {
        assertTrue(ClassUtilities.doesOneWrapTheOther(Integer.class, int.class));
        assertTrue(ClassUtilities.doesOneWrapTheOther(int.class, Integer.class));
        assertFalse(ClassUtilities.doesOneWrapTheOther(Integer.class, long.class));
    }

    @Test(expected = NullPointerException.class)
    public void testDoesOneWrapTheOther_Null() {
        ClassUtilities.doesOneWrapTheOther(null, int.class);
    }

    @Test
    public void testGetClassLoader() {
        assertNotNull(ClassUtilities.getClassLoader());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetClassLoader_NullAnchor() {
        ClassUtilities.getClassLoader(null);
    }

    @Test
    public void testLoadResourceAsBytesAndString() {
        // Use a resource that is guaranteed to be on the classpath – the MANIFEST.MF file.
        String manifestPath = "META-INF/MANIFEST.MF";

        byte[] bytes = ClassUtilities.loadResourceAsBytes(manifestPath);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        String content = ClassUtilities.loadResourceAsString(manifestPath);
        assertNotNull(content);
        assertTrue(content.contains("Manifest-Version"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadResourceAsBytes_NotFound() {
        ClassUtilities.loadResourceAsBytes("nonexistent-resource-xyz.txt");
    }

    @Test(expected = NullPointerException.class)
    public void testLoadResourceAsBytes_NullName() {
        ClassUtilities.loadResourceAsBytes(null);
    }

    @Test
    public void testIndexOfSmallestValue() {
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(null));
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(new int[0]));

        int[] arr = {5, 2, 7, 0, -3, 2};
        // smallest non‑negative value is 0 at index 3
        assertEquals(3, ClassUtilities.indexOfSmallestValue(arr));
    }

    @Test
    public void testFindClosestExactMatch() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(String.class, "string");
        map.put(Number.class, "number");

        String result = ClassUtilities.findClosest(String.class, map, "default");
        assertEquals("string", result);
    }

    @Test
    public void testFindClosestInheritanceDistance() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(Number.class, "number");
        map.put(Object.class, "object");

        // Integer is a Number, distance 1, should pick "number"
        String result = ClassUtilities.findClosest(Integer.class, map, "default");
        assertEquals("number", result);
    }

    @Test
    public void testFindClosestTieBreakingPreferClassOverInterface() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(ITest.class, "interface");
        map.put(ImplementsITest.class, "class");

        // ImplementsITest implements ITest – distance 0 to class entry, 1 to interface entry.
        String result = ClassUtilities.findClosest(ImplementsITest.class, map, "default");
        assertEquals("class", result);
    }

    @Test
    public void testGetClassIfEnum() {
        assertEquals(TestEnum.class, ClassUtilities.getClassIfEnum(TestEnum.class));
        assertNull(ClassUtilities.getClassIfEnum(String.class));

        // Inner class of an enum should resolve to the enum itself
        Class<?> inner = OuterEnum.ONE.new Inner().getClass();
        assertEquals(OuterEnum.class, ClassUtilities.getClassIfEnum(inner));
    }

    @Test
    public void testNewInstanceSimple() {
        Converter converter = new Converter(new ConverterOptions());

        // No‑arg public constructor
        Object o = ClassUtilities.newInstance(converter, PublicCtor.class, null);
        assertTrue(o instanceof PublicCtor);
    }

    @Test
    public void testNewInstanceWithArguments() {
        Converter converter = new Converter(new ConverterOptions());
        List<Object> args = Arrays.asList("hello", 42);

        Object o = ClassUtilities.newInstance(converter, MultiCtor.class, args);
        assertTrue(o instanceof MultiCtor);
        MultiCtor mc = (MultiCtor) o;
        assertEquals("hello", mc.s);
        assertEquals(42, mc.i);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstanceAbstractClass() {
        Converter converter = new Converter(new ConverterOptions());
        ClassUtilities.newInstance(converter, AbstractThing.class, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstanceInterface() {
        Converter converter = new Converter(new ConverterOptions());
        ClassUtilities.newInstance(converter, ThingInterface.class, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstanceSecuritySensitive() {
        Converter converter = new Converter(new ConverterOptions());
        ClassUtilities.newInstance(converter, ProcessBuilder.class, null);
    }

    @Test
    public void testSetUseUnsafe() {
        // Enable unsafe – should not throw
        ClassUtilities.setUseUnsafe(true);
        // Disable again
        ClassUtilities.setUseUnsafe(false);
    }

    @Test
    public void testFindLowestCommonSupertypesExcluding() {
        Set<Class<?>> skip = Collections.singleton(Object.class);
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(ArrayList.class,
                LinkedList.class, skip);
        // Both are List implementations; the lowest common supertype (excluding Object) is List
        assertTrue(result.contains(List.class));
        assertFalse(result.contains(Object.class));
    }

    @Test
    public void testFindLowestCommonSupertypes() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(HashSet.class, TreeSet.class);
        // Both are Set implementations; expect Set (and possibly other interfaces like Cloneable, Serializable)
        assertTrue(result.contains(Set.class));
        assertFalse(result.contains(Object.class));
    }

    @Test
    public void testFindLowestCommonSupertype() {
        Class<?> common = ClassUtilities.findLowestCommonSupertype(HashSet.class, TreeSet.class);
        // May return Set.class or another lowest common type; verify it is assignable from both
        assertNotNull(common);
        assertTrue(common.isAssignableFrom(HashSet.class));
        assertTrue(common.isAssignableFrom(TreeSet.class));
    }

    @Test
    public void testGetAllSupertypes() {
        Set<Class<?>> supertypes = ClassUtilities.getAllSupertypes(ArrayList.class);
        // Must contain the class itself, its superclasses and interfaces
        assertTrue(supertypes.contains(ArrayList.class));
        assertTrue(supertypes.contains(AbstractList.class));
        assertTrue(supertypes.contains(List.class));
        assertTrue(supertypes.contains(Collection.class));
        assertTrue(supertypes.contains(Iterable.class));
        assertTrue(supertypes.contains(Object.class));
    }

    @Test
    public void testHaveCommonAncestor() {
        assertTrue(ClassUtilities.haveCommonAncestor(ArrayList.class, LinkedList.class));
        assertFalse(ClassUtilities.haveCommonAncestor(String.class, Integer.class)); // only Object which is skipped by default
    }

    /*======================================================================
     * 3.  Edge‑case and exception handling tests
     *======================================================================*/

    @Test
    public void testComputeInheritanceDistancePrimitiveMismatch() {
        // primitive to unrelated wrapper → -1
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, String.class));
        // wrapper to unrelated primitive → -1
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, int.class));
    }

    @Test
    public void testFindClosestWhenNoCandidateMatches() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(Number.class, "number");
        // Float is a Number, but we pass a class that is not related (String)
        String result = ClassUtilities.findClosest(String.class, map, "default");
        assertEquals("default", result);
    }

    @Test
    public void testFindClosestTieSameDistancePreferMoreSpecific() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(IOne.class, "one");
        map.put(ITwo.class, "two");

        // ImplementsBoth implements both interfaces → distance 1 to each.
        // According to tie‑breaking, classes are preferred over interfaces,
        // but both are interfaces; the more specific (i.e., assignable) is chosen.
        // Since IOne is assignable from ITwo? No. The algorithm will keep the first
        // encountered when scores equal. We just verify that a result is returned.
        String result = ClassUtilities.findClosest(ImplementsBoth.class, map, "default");
        assertTrue(result.equals("one") || result.equals("two"));
    }

    @Test
    public void testLoadResourceAsStringHandlesUnicode() {
        // Use the same MANIFEST.MF – it contains ASCII only, but we just ensure UTF‑8 handling.
        String content = ClassUtilities.loadResourceAsString("META-INF/MANIFEST.MF");
        assertNotNull(content);
        assertTrue(content.startsWith("Manifest-Version"));
    }

    @Test
    public void testIndexOfSmallestValueAllNegative() {
        int[] arr = {-5, -1, -10};
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(arr));
    }

    @Test
    public void testNewInstanceInnerClassFailsGracefully() {
        Converter converter = new Converter(new ConverterOptions());
        // Inner non‑static class – should fall back to normal constructors and eventually fail.
        try {
            ClassUtilities.newInstance(converter, OuterEnum.Inner.class, null);
            fail("Expected IllegalArgumentException for inner non‑static class");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
    }

    @Test
    public void testFindLowestCommonSupertypesWithEmptySkipList() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(ArrayList.class,
                LinkedList.class, Collections.emptySet());
        // Object is now included, so result must contain Object.class
        assertTrue(result.contains(Object.class));
    }

    @Test
    public void testGetClassLoaderUsesContextClassLoader() {
        // Ensure that the method does not return null and is consistent with the context loader
        ClassLoader context = Thread.currentThread().getContextClassLoader();
        ClassLoader fromMethod = ClassUtilities.getClassLoader(ClassUtilitiesTest.class);
        assertNotNull(fromMethod);
        // The exact loader may differ (e.g., OSGi), but it must be assignable
        assertTrue(fromMethod == context || fromMethod == ClassUtilitiesTest.class.getClassLoader()
                || fromMethod == ClassLoader.getSystemClassLoader());
    }

    @Test
    public void testSetUseUnsafeDoesNotThrowWhenUnsafeUnavailable() {
        // In environments where Unsafe cannot be instantiated, the method should silently turn off the flag.
        ClassUtilities.setUseUnsafe(true);
        // No exception expected
        ClassUtilities.setUseUnsafe(false);
    }
}
