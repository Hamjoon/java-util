package com.cedarsoftware.util;

import com.cedarsoftware.util.convert.Converter;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.net.URL;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ClassUtilities}.
 *
 * <p>
 * The suite is organised by the public API surface of {@code ClassUtilities}.
 * For each method a single test method is provided that exercises:
 * <ul>
 *   <li>Typical (happy‑path) usage.</li>
 *   <li>Boundary / edge conditions.</li>
 *   <li>Expected exception throwing.</li>
 * </ul>
 * The tests use only the classes, constructors and methods that are present in the
 * source file (or in the allowed dependency signatures) – no mocking frameworks are used.
 * </p>
 */
public class ClassUtilitiesTest {

    private Converter converter;

    @Before
    public void setUp() {
        // The default Converter works for all conversions used in the tests.
        converter = new Converter(null);
    }

    /* --------------------------------------------------------------------- *
     *  isPrimitive / getPrimitiveFromWrapper / toPrimitiveWrapperClass
     * --------------------------------------------------------------------- */

    @Test
    public void testIsPrimitiveAndWrapperConversions() {
        // primitives themselves
        assertTrue(ClassUtilities.isPrimitive(int.class));
        assertTrue(ClassUtilities.isPrimitive(boolean.class));

        // wrapper classes are considered primitive by the utility
        assertTrue(ClassUtilities.isPrimitive(Integer.class));
        assertTrue(ClassUtilities.isPrimitive(Boolean.class));

        // non‑primitive, non‑wrapper
        assertFalse(ClassUtilities.isPrimitive(String.class));

        // wrapper → primitive
        assertEquals(int.class, ClassUtilities.getPrimitiveFromWrapper(Integer.class));
        assertEquals(boolean.class, ClassUtilities.getPrimitiveFromWrapper(Boolean.class));

        // primitive → wrapper
        assertEquals(Integer.class, ClassUtilities.toPrimitiveWrapperClass(int.class));
        assertEquals(Boolean.class, ClassUtilities.toPrimitiveWrapperClass(boolean.class));

        // passing a non‑primitive to toPrimitiveWrapperClass must return the same class
        assertEquals(String.class, ClassUtilities.toPrimitiveWrapperClass(String.class));

        // illegal argument – null
        try {
            ClassUtilities.getPrimitiveFromWrapper(null);
            fail("Expected IllegalArgumentException for null argument");
        } catch (IllegalArgumentException ignored) {
        }

        // illegal argument – unknown wrapper
        assertNull(ClassUtilities.getPrimitiveFromWrapper(UUID.class));
    }

    /* --------------------------------------------------------------------- *
     *  doesOneWrapTheOther
     * --------------------------------------------------------------------- */

    @Test
    public void testDoesOneWrapTheOther() {
        assertTrue(ClassUtilities.doesOneWrapTheOther(Integer.class, int.class));
        assertTrue(ClassUtilities.doesOneWrapTheOther(int.class, Integer.class));
        assertFalse(ClassUtilities.doesOneWrapTheOther(Integer.class, long.class));
        assertFalse(ClassUtilities.doesOneWrapTheOther(String.class, int.class));
    }

    /* --------------------------------------------------------------------- *
     *  computeInheritanceDistance
     * --------------------------------------------------------------------- */

    @Test
    public void testComputeInheritanceDistance() {
        // direct superclass / interface
        assertEquals(1, ClassUtilities.computeInheritanceDistance(ArrayList.class, List.class));
        assertEquals(2, ClassUtilities.computeInheritanceDistance(ArrayList.class, Collection.class));

        // same class → distance 0
        assertEquals(0, ClassUtilities.computeInheritanceDistance(HashMap.class, HashMap.class));

        // unrelated classes → -1
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, Integer.class));

        // primitive handling – same primitive
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, int.class));

        // primitive vs wrapper – considered same primitive
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, Integer.class));
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Integer.class, int.class));

        // primitive vs unrelated wrapper → -1
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, Long.class));

        // null arguments → -1
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, Object.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(Object.class, null));
    }

    /* --------------------------------------------------------------------- *
     *  addPermanentClassAlias / removePermanentClassAlias / forName
     * --------------------------------------------------------------------- */

    @Test
    public void testClassAliasAndForName() {
        // alias an existing class
        ClassUtilities.addPermanentClassAlias(ArrayList.class, "MyListAlias");
        Class<?> got = ClassUtilities.forName("MyListAlias", ClassUtilities.getClassLoader());
        assertEquals(ArrayList.class, got);

        // remove alias and verify that it is no longer resolved
        ClassUtilities.removePermanentClassAlias("MyListAlias");
        assertNull(ClassUtilities.forName("MyListAlias", ClassUtilities.getClassLoader()));

        // built‑in alias for primitive name
        Class<?> intClass = ClassUtilities.forName("int", ClassUtilities.getClassLoader());
        assertEquals(int.class, intClass);

        // unknown name returns null (no exception)
        assertNull(ClassUtilities.forName("com.nonexistent.FooBar", ClassUtilities.getClassLoader()));
    }

    /* --------------------------------------------------------------------- *
     *  areAllConstructorsPrivate
     * --------------------------------------------------------------------- */

    private static final class PrivateCtorOnly {
        private PrivateCtorOnly() {}
    }

    private static final class MixedCtor {
        public MixedCtor() {}
        private MixedCtor(String ignored) {}
    }

    @Test
    public void testAreAllConstructorsPrivate() {
        assertTrue(ClassUtilities.areAllConstructorsPrivate(PrivateCtorOnly.class));
        assertFalse(ClassUtilities.areAllConstructorsPrivate(MixedCtor.class));
        assertFalse(ClassUtilities.areAllConstructorsPrivate(String.class)); // public constructors
    }

    /* --------------------------------------------------------------------- *
     *  isClassFinal
     * --------------------------------------------------------------------- */

    @Test
    public void testIsClassFinal() {
        assertTrue(ClassUtilities.isClassFinal(String.class)); // final
        assertFalse(ClassUtilities.isClassFinal(ArrayList.class)); // not final
        assertTrue(ClassUtilities.isClassFinal(Integer.class)); // final
    }

    /* --------------------------------------------------------------------- *
     *  findClosest
     * --------------------------------------------------------------------- */

    @Test
    public void testFindClosest() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(Number.class, "number");
        map.put(Integer.class, "integer");
        map.put(Object.class, "object");

        // exact match
        assertEquals("integer", ClassUtilities.findClosest(Integer.class, map, "default"));

        // closest superclass (Integer → Number)
        assertEquals("number", ClassUtilities.findClosest(Double.class, map, "default"));

        // no match → default
        assertEquals("default", ClassUtilities.findClosest(String.class, Collections.emptyMap(), "default"));
    }

    /* --------------------------------------------------------------------- *
     *  loadResourceAsString / loadResourceAsBytes
     * --------------------------------------------------------------------- */

    @Test
    public void testLoadResourceAsStringAndBytes() throws Exception {
        // Create a temporary resource on the classpath using the test class's package.
        // The resource is stored in the same package as this test class.
        String resourceName = "test-resource.txt";
        String expectedContent = "Hello World!";

        // Write the resource into the same package directory (using the class loader's resource URL)
        URL url = ClassUtilities.getClassLoader(ClassUtilitiesTest.class).getResource("");
        assertNotNull("Could not locate package directory for test resources", url);
        java.nio.file.Path dir = java.nio.file.Paths.get(url.toURI());
        java.nio.file.Path file = dir.resolve(resourceName);
        java.nio.file.Files.write(file, expectedContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        // Verify byte loading
        byte[] bytes = ClassUtilities.loadResourceAsBytes(resourceName);
        assertArrayEquals(expectedContent.getBytes(java.nio.charset.StandardCharsets.UTF_8), bytes);

        // Verify string loading
        String loaded = ClassUtilities.loadResourceAsString(resourceName);
        assertEquals(expectedContent, loaded);

        // Clean up
        java.nio.file.Files.deleteIfExists(file);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadResourceAsBytesMissing() {
        ClassUtilities.loadResourceAsBytes("non‑existent‑resource.txt");
    }

    /* --------------------------------------------------------------------- *
     *  getClassIfEnum / computeEnum (indirectly)
     * --------------------------------------------------------------------- */

    private enum SampleEnum { A, B }

    private static class EnumHolder {
        enum InnerEnum { X, Y }
    }

    @Test
    public void testGetClassIfEnum() {
        // direct enum
        assertEquals(SampleEnum.class, ClassUtilities.getClassIfEnum(SampleEnum.class));

        // enum via inner class
        assertEquals(EnumHolder.InnerEnum.class,
                ClassUtilities.getClassIfEnum(EnumHolder.InnerEnum.class));

        // non‑enum returns null
        assertNull(ClassUtilities.getClassIfEnum(String.class));
        assertNull(ClassUtilities.getClassIfEnum(null));
    }

    /* --------------------------------------------------------------------- *
     *  findLowestCommonSupertypes & findLowestCommonSupertype
     * --------------------------------------------------------------------- */

    @Test
    public void testFindLowestCommonSupertypes() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(Integer.class, Double.class);
        // Number and Comparable are the most specific common supertypes (Object is filtered out)
        assertTrue(result.contains(Number.class));
        assertTrue(result.contains(Comparable.class));
        assertFalse(result.contains(Object.class));

        // When both classes are the same, the set contains only that class (if not excluded)
        Set<Class<?>> same = ClassUtilities.findLowestCommonSupertypes(String.class, String.class);
        assertEquals(Collections.singleton(String.class), same);
    }

    @Test
    public void testFindLowestCommonSupertype() {
        // Returns one of the lowest supertypes – we only check that it is a member of the set.
        Class<?> common = ClassUtilities.findLowestCommonSupertype(Integer.class, Double.class);
        Set<Class<?>> all = ClassUtilities.findLowestCommonSupertypes(Integer.class, Double.class);
        assertTrue(all.contains(common));
    }

    /* --------------------------------------------------------------------- *
     *  getClassHierarchyInfo (depth & distance)
     * --------------------------------------------------------------------- */

    @Test
    public void testClassHierarchyInfoDepthAndDistance() {
        ClassUtilities.ClassHierarchyInfo info = ClassUtilities.getClassHierarchyInfo(ArrayList.class);
        // Depth of ArrayList → Object is 2 (ArrayList → AbstractList → Object)
        assertEquals(2, info.getDepth());

        // Distance from ArrayList to List should be 1 (ArrayList implements List directly)
        assertEquals(1, info.getDistance(List.class));

        // Distance to a non‑related type should be -1
        assertEquals(-1, info.getDistance(String.class));
    }

    /* --------------------------------------------------------------------- *
     *  newInstance – successful construction paths
     * --------------------------------------------------------------------- */

    private static final class SimplePojo {
        private final String text;
        private final int number;

        public SimplePojo() {
            this.text = "";
            this.number = 0;
        }

        public SimplePojo(String text, int number) {
            this.text = text;
            this.number = number;
        }

        public String getText() { return text; }
        public int getNumber() { return number; }
    }

    @Test
    public void testNewInstanceWithNoArgs() {
        Object o = ClassUtilities.newInstance(converter, SimplePojo.class, Collections.emptyList());
        assertTrue(o instanceof SimplePojo);
        SimplePojo p = (SimplePojo) o;
        assertEquals("", p.getText());
        assertEquals(0, p.getNumber());
    }

    @Test
    public void testNewInstanceWithMatchingArgs() {
        List<Object> args = Arrays.asList("hello", 42);
        Object o = ClassUtilities.newInstance(converter, SimplePojo.class, args);
        assertTrue(o instanceof SimplePojo);
        SimplePojo p = (SimplePojo) o;
        assertEquals("hello", p.getText());
        assertEquals(42, p.getNumber());
    }

    @Test
    public void testNewInstanceWithPrimitiveWrapperMismatch() {
        // Provide wrapper for primitive parameter – matching should succeed via wrapper handling
        List<Object> args = Arrays.asList("test", Integer.valueOf(7));
        SimplePojo pojo = (SimplePojo) ClassUtilities.newInstance(converter, SimplePojo.class, args);
        assertEquals("test", pojo.getText());
        assertEquals(7, pojo.getNumber());
    }

    @Test
    public void testNewInstanceWithMissingArgsAllowsNulls() {
        // Provide only one argument; the second primitive int should be filled with default 0
        List<Object> args = Collections.singletonList("onlyText");
        SimplePojo pojo = (SimplePojo) ClassUtilities.newInstance(converter, SimplePojo.class, args);
        assertEquals("onlyText", pojo.getText());
        assertEquals(0, pojo.getNumber()); // default for int
    }

    /* --------------------------------------------------------------------- *
     *  newInstance – error handling
     * --------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstanceWithInterface() {
        ClassUtilities.newInstance(converter, List.class, Collections.emptyList());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstanceWithAbstractClass() {
        abstract class AbstractThing {}
        ClassUtilities.newInstance(converter, AbstractThing.class, Collections.emptyList());
    }

    @Test(expected = SecurityException.class)
    public void testNewInstanceSecurityBlocked() {
        // ProcessBuilder is on the security‑blocked list.
        ClassUtilities.newInstance(converter, ProcessBuilder.class, Collections.emptyList());
    }

    /* --------------------------------------------------------------------- *
     *  getClassLoader variations
     * --------------------------------------------------------------------- */

    @Test
    public void testGetClassLoaderFallbacks() {
        // Anchor class is non‑null, so we should never get an IllegalArgumentException
        ClassLoader cl = ClassUtilities.getClassLoader(ClassUtilitiesTest.class);
        assertNotNull(cl);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetClassLoaderNullAnchor() {
        ClassUtilities.getClassLoader(null);
    }

    /* --------------------------------------------------------------------- *
     *  SecurityChecker direct API (blocked class detection)
     * --------------------------------------------------------------------- */

    @Test
    public void testSecurityCheckerBlockedClass() {
        assertTrue(ClassUtilities.SecurityChecker.isSecurityBlocked(ClassLoader.class));
        assertFalse(ClassUtilities.SecurityChecker.isSecurityBlocked(String.class));
    }

    @Test
    public void testSecurityCheckerBlockedName() {
        assertTrue(ClassUtilities.SecurityChecker.isSecurityBlockedName("java.lang.ProcessImpl"));
        assertFalse(ClassUtilities.SecurityChecker.isSecurityBlockedName("java.lang.String"));
    }

    /* --------------------------------------------------------------------- *
     *  indexOfSmallestValue (deprecated method)
     * --------------------------------------------------------------------- */

    @Test
    public void testIndexOfSmallestValue() {
        assertEquals(2, ClassUtilities.indexOfSmallestValue(new int[]{5, 3, 1, 4}));
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(new int[]{}));
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(null));
        // negative numbers are ignored, so the smallest non‑negative is 0 at index 1
        assertEquals(1, ClassUtilities.indexOfSmallestValue(new int[]{10, 0, -5, 2}));
    }

    /* --------------------------------------------------------------------- *
     *  haveCommonAncestor convenience method
     * --------------------------------------------------------------------- */

    @Test
    public void testHaveCommonAncestor() {
        assertTrue(ClassUtilities.haveCommonAncestor(Integer.class, Double.class));
        assertFalse(ClassUtilities.haveCommonAncestor(String.class, int.class));
    }

    /* --------------------------------------------------------------------- *
     *  Direct class mapping utilities (used indirectly by newInstance)
     * --------------------------------------------------------------------- */

    @Test
    public void testDirectClassMappingProvidesDefaultInstances() {
        // Date is mapped to Date::new
        Object date = ClassUtilities.newInstance(converter, Date.class, Collections.emptyList());
        assertTrue(date instanceof Date);

        // URL is mapped via a supplier that swallows MalformedURLException
        Object url = ClassUtilities.newInstance(converter, URL.class, Collections.emptyList());
        assertTrue(url instanceof URL);
    }

    @Test
    public void testAssignableClassMappingProvidesDefaults() {
        // List.class should resolve to an ArrayList instance via ASSIGNABLE_CLASS_MAPPING
        Object list = ClassUtilities.newInstance(converter, List.class, Collections.emptyList());
        assertTrue(list instanceof ArrayList);
        // Deque.class should resolve to ArrayDeque
        Object deque = ClassUtilities.newInstance(converter, Deque.class, Collections.emptyList());
        assertTrue(deque instanceof ArrayDeque);
    }

    /* --------------------------------------------------------------------- *
     *  Unsafe instantiation path (enabled / disabled)
     * --------------------------------------------------------------------- */

    private static final class NoConstructor {
        private NoConstructor() { throw new AssertionError("Should not be called"); }
    }

    @Test
    public void testUnsafeInstantiationWhenEnabled() {
        // Enable unsafe mode – the class has a private constructor that throws
        ClassUtilities.setUseUnsafe(true);
        Object o = ClassUtilities.newInstance(converter, NoConstructor.class, Collections.emptyList());
        assertNotNull(o);
        assertTrue(o instanceof NoConstructor);
        // Reset for other tests
        ClassUtilities.setUseUnsafe(false);
    }

    @Test
    public void testUnsafeInstantiationWhenDisabledFallsBackToError() {
        // Ensure unsafe mode is off – constructor will throw, leading to IllegalArgumentException
        ClassUtilities.setUseUnsafe(false);
        try {
            ClassUtilities.newInstance(converter, NoConstructor.class, Collections.emptyList());
            fail("Expected IllegalArgumentException because unsafe is disabled and constructor fails");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Unable to instantiate"));
        }
    }
}
