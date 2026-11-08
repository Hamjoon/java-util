package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.util.*;

import com.cedarsoftware.util.convert.Converter;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link ClassUtilities}.
 *
 * The tests deliberately use only the public API (or package‑private static methods) that are
 * present in the supplied source file and its declared dependencies.
 *
 * <p>All tests are self‑contained – no external libraries, no mocking frameworks and no
 * subclassing of internal classes.  Resources are created at runtime in the test output
 * directory so that {@code loadResourceAsString/Bytes} can be exercised.</p>
 */
public class ClassUtilitiesTest {

    /** A simple enum used for {@link ClassUtilities#getClassIfEnum} tests. */
    private enum SampleEnum { ONE, TWO }

    /** A non‑enum class that contains an enum as an inner class. */
    private static class OuterWithEnum {
        enum InnerEnum { A, B }
    }

    /** Utility class with a private constructor – used for {@link ClassUtilities#areAllConstructorsPrivate}. */
    private static final class PrivateCtorUtil {
        private PrivateCtorUtil() {}
    }

    /** Class with mixed visibility constructors – used for {@link ClassUtilities#newInstance}. */
    public static class MixedCtor {
        public MixedCtor() {}
        protected MixedCtor(String s) {}
        private MixedCtor(int i) {}
    }

    /** Simple POJO for newInstance default construction. */
    public static class SimplePojo {
        public SimplePojo() {}
    }

    /** A resource file name that will be written to the test output directory before tests run. */
    private static final String TEST_RESOURCE_NAME = "classutilities_test_resource.txt";

    /** Content written to the temporary resource file. */
    private static final String TEST_RESOURCE_CONTENT = "Hello‑World";

    @BeforeClass
    public static void createTestResource() throws IOException {
        // Locate the directory that corresponds to the package of this test class.
        String pkgPath = ClassUtilitiesTest.class.getPackage().getName().replace('.', '/');
        URL pkgUrl = ClassUtilitiesTest.class.getClassLoader().getResource(pkgPath);
        assertNotNull("Package URL should not be null", pkgUrl);
        Path dir = Path.of(pkgUrl.getPath());
        Files.writeString(dir.resolve(TEST_RESOURCE_NAME), TEST_RESOURCE_CONTENT);
    }

    /* ---------------------------------------------------------------------- */
    /*                     Primitive / Wrapper handling tests                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIsPrimitive() {
        assertTrue(ClassUtilities.isPrimitive(int.class));
        assertTrue(ClassUtilities.isPrimitive(Integer.class));
        assertTrue(ClassUtilities.isPrimitive(boolean.class));
        assertTrue(ClassUtilities.isPrimitive(Boolean.class));
        assertFalse(ClassUtilities.isPrimitive(String.class));
        assertFalse(ClassUtilities.isPrimitive(void.class)); // void is not in prims set
    }

    @Test
    public void testToPrimitiveWrapperClass() {
        assertEquals(Integer.class, ClassUtilities.toPrimitiveWrapperClass(int.class));
        assertEquals(Boolean.class, ClassUtilities.toPrimitiveWrapperClass(boolean.class));
        assertEquals(void.class, ClassUtilities.toPrimitiveWrapperClass(void.class).getClass()); // wrapper of void is Void.class
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
        assertFalse(ClassUtilities.doesOneWrapTheOther(String.class, int.class));
    }

    /* ---------------------------------------------------------------------- */
    /*                     Inheritance distance tests                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testComputeInheritanceDistanceSameClass() {
        assertEquals(0, ClassUtilities.computeInheritanceDistance(String.class, String.class));
    }

    @Test
    public void testComputeInheritanceDistanceSubclass() {
        assertEquals(1, ClassUtilities.computeInheritanceDistance(ArrayList.class, List.class));
        assertEquals(2, ClassUtilities.computeInheritanceDistance(TreeMap.class, Map.class));
    }

    @Test
    public void testComputeInheritanceDistanceInterface() {
        assertEquals(1, ClassUtilities.computeInheritanceDistance(ArrayList.class, Collection.class));
        assertEquals(2, ClassUtilities.computeInheritanceDistance(TreeSet.class, Iterable.class));
    }

    @Test
    public void testComputeInheritanceDistanceUnrelated() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, Integer.class));
    }

    @Test
    public void testComputeInheritanceDistancePrimitiveWrapper() {
        // primitive source, wrapper destination
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, Integer.class));
        // wrapper source, primitive destination
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Integer.class, int.class));
        // primitive to unrelated wrapper
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, Long.class));
    }

    @Test
    public void testComputeInheritanceDistanceNulls() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, String.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, null));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, null));
    }

    /* ---------------------------------------------------------------------- */
    /*                     Class alias handling tests                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAddAndRemovePermanentClassAlias() {
        Class<?> original = String.class;
        String alias = "myStringAlias";

        // ensure alias not present
        assertNull(ClassUtilities.forName(alias, ClassUtilities.class.getClassLoader()));

        // add alias and verify lookup works
        ClassUtilities.addPermanentClassAlias(original, alias);
        assertEquals(original, ClassUtilities.forName(alias, ClassUtilities.class.getClassLoader()));

        // remove alias and verify it disappears
        ClassUtilities.removePermanentClassAlias(alias);
        assertNull(ClassUtilities.forName(alias, ClassUtilities.class.getClassLoader()));
    }

    @Test
    public void testForNameWithInvalidInput() {
        assertNull(ClassUtilities.forName(null, ClassUtilities.class.getClassLoader()));
        assertNull(ClassUtilities.forName("", ClassUtilities.class.getClassLoader()));
        // non‑existent class name
        assertNull(ClassUtilities.forName("com.nonexistent.FooBar", ClassUtilities.class.getClassLoader()));
    }

    /* ---------------------------------------------------------------------- */
    /*                     Class final / constructor visibility tests         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIsClassFinal() {
        assertTrue(ClassUtilities.isClassFinal(String.class)); // String is final
        assertFalse(ClassUtilities.isClassFinal(ArrayList.class)); // not final
    }

    @Test
    public void testAreAllConstructorsPrivate() {
        assertTrue(ClassUtilities.areAllConstructorsPrivate(PrivateCtorUtil.class));
        assertFalse(ClassUtilities.areAllConstructorsPrivate(String.class));
    }

    /* ---------------------------------------------------------------------- */
    /*                     findClosest tests                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testFindClosestExactMatch() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(Number.class, "Number");
        map.put(Integer.class, "Integer");
        map.put(Object.class, "Object");

        assertEquals("Integer", ClassUtilities.findClosest(Integer.class, map, "default"));
    }

    @Test
    public void testFindClosestInheritanceMatch() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(List.class, "List");
        map.put(Collection.class, "Collection");
        map.put(Object.class, "Object");

        // ArrayList is a List -> should pick List (distance 1) over Collection (distance 2)
        assertEquals("List", ClassUtilities.findClosest(ArrayList.class, map, "default"));
    }

    @Test
    public void testFindClosestTieBreakingPrefersClassOverInterface() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(Serializable.class, "Serializable"); // interface
        map.put(Cloneable.class, "Cloneable");       // interface
        map.put(String.class, "String");             // concrete class, same distance as interfaces (0 for String)

        // For CharSequence (implemented by String) both interfaces and String are distance 1.
        // Tie‑breaker should prefer the concrete class.
        assertEquals("String", ClassUtilities.findClosest(CharSequence.class, map, "default"));
    }

    @Test
    public void testFindClosestReturnsDefaultWhenNoMatch() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(Set.class, "Set");
        assertEquals("fallback", ClassUtilities.findClosest(Integer.class, map, "fallback"));
    }

    /* ---------------------------------------------------------------------- */
    /*                     Resource loading tests                             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testLoadResourceAsBytesAndString() {
        byte[] bytes = ClassUtilities.loadResourceAsBytes(TEST_RESOURCE_NAME);
        assertNotNull(bytes);
        assertEquals(TEST_RESOURCE_CONTENT, new String(bytes, java.nio.charset.StandardCharsets.UTF_8));

        String content = ClassUtilities.loadResourceAsString(TEST_RESOURCE_NAME);
        assertEquals(TEST_RESOURCE_CONTENT, content);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadResourceAsBytesMissingResource() {
        ClassUtilities.loadResourceAsBytes("non_existent_resource_12345.txt");
    }

    @Test(expected = NullPointerException.class)
    public void testLoadResourceAsBytesNullName() {
        ClassUtilities.loadResourceAsBytes(null);
    }

    /* ---------------------------------------------------------------------- */
    /*                     indexOfSmallestValue tests                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIndexOfSmallestValue() {
        int[] data = {5, 3, 9, 1, 4};
        assertEquals(3, ClassUtilities.indexOfSmallestValue(data));

        int[] withNegatives = {10, -2, 7};
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(withNegatives)); // negative values ignored

        assertEquals(-1, ClassUtilities.indexOfSmallestValue(new int[0]));
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(null));
    }

    /* ---------------------------------------------------------------------- */
    /*                     getClassIfEnum tests                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetClassIfEnumDirect() {
        assertEquals(SampleEnum.class, ClassUtilities.getClassIfEnum(SampleEnum.class));
    }

    @Test
    public void testGetClassIfEnumEnclosing() {
        // InnerEnum is inside OuterWithEnum; the method should discover it via enclosing class traversal.
        assertEquals(OuterWithEnum.InnerEnum.class,
                ClassUtilities.getClassIfEnum(OuterWithEnum.InnerEnum.class));
    }

    @Test
    public void testGetClassIfEnumNone() {
        assertNull(ClassUtilities.getClassIfEnum(String.class));
        assertNull(ClassUtilities.getClassIfEnum(null));
    }

    /* ---------------------------------------------------------------------- */
    /*                     Lowest common supertypes tests                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testFindLowestCommonSupertypesExcluding() {
        Set<Class<?>> skip = new HashSet<>(Collections.singleton(Object.class));
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(ArrayList.class,
                HashSet.class, skip);
        // Both implement java.util.Collection; Set is more specific than Collection after skipping Object.
        assertTrue(result.contains(Set.class));
        assertFalse(result.contains(Collection.class));
    }

    @Test
    public void testFindLowestCommonSupertypes() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(Integer.class, Double.class);
        // Expected common supertypes include Number, Comparable, Serializable (Object is excluded)
        assertTrue(result.contains(Number.class));
        assertTrue(result.contains(Comparable.class));
        assertTrue(result.contains(Serializable.class));
        assertFalse(result.contains(Object.class));
    }

    @Test
    public void testFindLowestCommonSupertypeSingleResult() {
        Class<?> common = ClassUtilities.findLowestCommonSupertype(ArrayList.class, LinkedList.class);
        // Both are List implementations; the most specific common supertype is List
        assertEquals(List.class, common);
    }

    @Test
    public void testHaveCommonAncestor() {
        assertTrue(ClassUtilities.haveCommonAncestor(ArrayList.class, LinkedList.class));
        assertFalse(ClassUtilities.haveCommonAncestor(String.class, Integer.class));
    }

    /* ---------------------------------------------------------------------- */
    /*                     newInstance tests                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testNewInstanceSimplePojo() {
        Converter converter = new Converter(new com.cedarsoftware.util.convert.ConverterOptions());
        Object obj = ClassUtilities.newInstance(converter, SimplePojo.class, null);
        assertNotNull(obj);
        assertTrue(obj instanceof SimplePojo);
    }

    @Test
    public void testNewInstanceWithArguments() {
        Converter converter = new Converter(new com.cedarsoftware.util.convert.ConverterOptions());
        List<Object> args = Arrays.asList("test", 42);
        // MixedCtor has a protected String constructor; newInstance should match it.
        Object instance = ClassUtilities.newInstance(converter, MixedCtor.class, args);
        assertNotNull(instance);
        assertTrue(instance instanceof MixedCtor);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstanceSecuritySensitiveClass() {
        Converter converter = new Converter(new com.cedarsoftware.util.convert.ConverterOptions());
        // Attempt to instantiate a prohibited class (ProcessBuilder)
        ClassUtilities.newInstance(converter, ProcessBuilder.class, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstanceInterface() {
        Converter converter = new Converter(new com.cedarsoftware.util.convert.ConverterOptions());
        ClassUtilities.newInstance(converter, List.class, null);
    }

    /* ---------------------------------------------------------------------- */
    /*                     ClassLoader utilities tests                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetClassLoaderNonNull() {
        ClassLoader cl = ClassUtilities.getClassLoader(ClassUtilitiesTest.class);
        assertNotNull(cl);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetClassLoaderNullAnchor() {
        ClassUtilities.getClassLoader(null);
    }

    /* ---------------------------------------------------------------------- */
    /*                     Miscellaneous utility tests                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetAllSupertypesIncludesSelf() {
        Set<Class<?>> supertypes = ClassUtilities.getAllSupertypes(ArrayList.class);
        assertTrue(supertypes.contains(ArrayList.class));
        assertTrue(supertypes.contains(List.class));
        assertTrue(supertypes.contains(Collection.class));
        assertTrue(supertypes.contains(Object.class));
    }

    @Test
    public void testGetDepth() throws Exception {
        // Use reflection to access the private static method getDepth
        java.lang.reflect.Method m = ClassUtilities.class.getDeclaredMethod("getDepth", Class.class);
        m.setAccessible(true);
        int depthObject = (int) m.invoke(null, Object.class);
        int depthString = (int) m.invoke(null, String.class);
        assertTrue(depthString > depthObject);
    }
}
