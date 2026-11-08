package com.cedarsoftware.util;

import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.ConverterOptions;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ClassUtilities}.
 *
 * <p>Each public method of {@code ClassUtilities} is exercised with:
 * <ul>
 *   <li>Typical usage</li>
 *   <li>Edge‑case handling (nulls, empty collections, primitive vs wrapper, etc.)</li>
 *   <li>Error scenarios (illegal arguments, security restrictions, missing resources)</li>
 * </ul>
 * The tests avoid any mocking – only real classes from the library and the JDK are used.
 */
public class ClassUtilitiesTest {

    /** A simple POJO used for {@code newInstance()} tests. */
    public static class SimplePojo {
        public String name;
        public int value;

        public SimplePojo() { }

        public SimplePojo(String name) {
            this.name = name;
        }

        public SimplePojo(String name, int value) {
            this.name = name;
            this.value = value;
        }
    }

    /** A non‑static inner class – used to verify the inner‑class handling path of {@code newInstance()}. */
    public class InnerPojo {
        public InnerPojo() { }
    }

    /** A utility class with a private constructor – used for {@code areAllConstructorsPrivate()} tests. */
    private static final class PrivateCtorUtil {
        private PrivateCtorUtil() { }
    }

    /** An enum for {@code getClassIfEnum()} tests. */
    private enum SampleEnum { A, B }

    private static Converter converter;

    @BeforeClass
    public static void setUpConverter() {
        // The library provides a no‑arg constructor for options; use the default.
        converter = new Converter(new ConverterOptions());
    }

    @AfterClass
    public static void resetUnsafe() {
        // Ensure unsafe is disabled for the rest of the suite.
        ClassUtilities.setUseUnsafe(false);
    }

    /* ---------------------------------------------------------------------- */
    /*  Primitive / Wrapper utilities                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetPrimitiveFromWrapper() {
        assertEquals(int.class, ClassUtilities.getPrimitiveFromWrapper(Integer.class));
        assertEquals(void.class, ClassUtilities.getPrimitiveFromWrapper(Void.class));
        assertNull(ClassUtilities.getPrimitiveFromWrapper(String.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetPrimitiveFromWrapper_NullArgument() {
        ClassUtilities.getPrimitiveFromWrapper(null);
    }

    @Test
    public void testIsPrimitive() {
        assertTrue(ClassUtilities.isPrimitive(int.class));
        assertTrue(ClassUtilities.isPrimitive(Integer.class));
        assertFalse(ClassUtilities.isPrimitive(String.class));
    }

    @Test
    public void testToPrimitiveWrapperClass() {
        assertEquals(Integer.class, ClassUtilities.toPrimitiveWrapperClass(int.class));
        assertEquals(String.class, ClassUtilities.toPrimitiveWrapperClass(String.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToPrimitiveWrapperClass_NotAPrimitive() {
        ClassUtilities.toPrimitiveWrapperClass(void.class); // void is primitive but mapped, ok
        ClassUtilities.toPrimitiveWrapperClass(Object.class); // should throw
    }

    @Test
    public void testDoesOneWrapTheOther() {
        assertTrue(ClassUtilities.doesOneWrapTheOther(Integer.class, int.class));
        assertTrue(ClassUtilities.doesOneWrapTheOther(int.class, Integer.class));
        assertFalse(ClassUtilities.doesOneWrapTheOther(Integer.class, long.class));
    }

    /* ---------------------------------------------------------------------- */
    /*  Class loading utilities                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testForName_ValidClass() {
        Class<?> cls = ClassUtilities.forName("java.util.ArrayList", ClassUtilities.class.getClassLoader());
        assertEquals(ArrayList.class, cls);
    }

    @Test
    public void testForName_NullOrEmpty() {
        assertNull(ClassUtilities.forName(null, ClassUtilities.class.getClassLoader()));
        assertNull(ClassUtilities.forName("", ClassUtilities.class.getClassLoader()));
    }

    @Test
    public void testForName_SecurityBlockedClass() {
        try {
            ClassUtilities.forName("java.lang.ProcessBuilder", ClassUtilities.class.getClassLoader());
            fail("Expected IllegalArgumentException because the class is security‑blocked");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Security exception"));
        }
    }

    @Test
    public void testGetClassLoader_NotNull() {
        ClassLoader cl = ClassUtilities.getClassLoader();
        assertNotNull(cl);
        // The returned loader must be able to load this test class.
        assertEquals(ClassUtilitiesTest.class, ClassUtilities.forName(ClassUtilitiesTest.class.getName(), cl));
    }

    /* ---------------------------------------------------------------------- */
    /*  Modifiers / constructor utilities                                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIsClassFinal() {
        assertTrue(ClassUtilities.isClassFinal(String.class)); // final
        assertFalse(ClassUtilities.isClassFinal(ArrayList.class)); // not final
    }

    @Test
    public void testAreAllConstructorsPrivate() {
        assertTrue(ClassUtilities.areAllConstructorsPrivate(PrivateCtorUtil.class));
        assertFalse(ClassUtilities.areAllConstructorsPrivate(String.class));
    }

    /* ---------------------------------------------------------------------- */
    /*  Inheritance distance / hierarchy utilities                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testComputeInheritanceDistance() {
        assertEquals(1, ClassUtilities.computeInheritanceDistance(ArrayList.class, List.class));
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Integer.class, Integer.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, Integer.class));
    }

    @Test
    public void testGetClassHierarchyInfoDepth() {
        ClassUtilities.ClassHierarchyInfo info = ClassUtilities.getClassHierarchyInfo(ArrayList.class);
        // ArrayList -> AbstractList -> AbstractCollection -> Object -> (depth 3)
        assertEquals(3, info.getDepth());
    }

    /* ---------------------------------------------------------------------- */
    /*  findClosest – most specific matching class                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testFindClosest_ExactMatch() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(List.class, "list");
        map.put(Collection.class, "col");
        assertEquals("list", ClassUtilities.findClosest(ArrayList.class, map, "default"));
    }

    @Test
    public void testFindClosest_InheritanceMatch() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(Collection.class, "col");
        map.put(Iterable.class, "iter");
        // ArrayList is a Collection and also an Iterable; Collection is closer (distance 1 vs 2)
        assertEquals("col", ClassUtilities.findClosest(ArrayList.class, map, "default"));
    }

    @Test
    public void testFindClosest_NoMatchReturnsDefault() {
        Map<Class<?>, String> map = Collections.singletonMap(Set.class, "set");
        assertEquals("default", ClassUtilities.findClosest(ArrayList.class, map, "default"));
    }

    /* ---------------------------------------------------------------------- */
    /*  Resource loading utilities                                            */
    /* ---------------------------------------------------------------------- */

    /**
     * A tiny text file placed in {@code src/test/resources/test-resource.txt}
     * with the exact content {@code "resource‑content"}.
     */
    private static final String TEST_RESOURCE_NAME = "test-resource.txt";
    private static final String TEST_RESOURCE_CONTENT = "resource-content";

    @Test
    public void testLoadResourceAsString() {
        String content = ClassUtilities.loadResourceAsString(TEST_RESOURCE_NAME);
        assertEquals(TEST_RESOURCE_CONTENT, content);
    }

    @Test
    public void testLoadResourceAsBytes() {
        byte[] bytes = ClassUtilities.loadResourceAsBytes(TEST_RESOURCE_NAME);
        assertArrayEquals(TEST_RESOURCE_CONTENT.getBytes(StandardCharsets.UTF_8), bytes);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadResourceAsBytes_NotFound() {
        ClassUtilities.loadResourceAsBytes("non‑existent‑resource.txt");
    }

    /* ---------------------------------------------------------------------- */
    /*  Deprecated utility – indexOfSmallestValue                              */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIndexOfSmallestValue_Typical() {
        int[] arr = {5, 2, 9, 2, 7};
        assertEquals(1, ClassUtilities.indexOfSmallestValue(arr)); // first 2
    }

    @Test
    public void testIndexOfSmallestValue_AllNegative() {
        int[] arr = {-5, -3, -10};
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(arr));
    }

    @Test
    public void testIndexOfSmallestValue_EmptyOrNull() {
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(new int[0]));
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(null));
    }

    /* ---------------------------------------------------------------------- */
    /*  Enum handling utilities                                               */
    /* ---------------------------------------------------------------------- */

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
    /*  Lowest common supertypes utilities                                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testFindLowestCommonSupertypesExcluding_Basic() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(
                Integer.class, Double.class,
                Collections.singleton(Object.class));
        // Expected: Number and Comparable (both not excluded)
        assertTrue(result.contains(Number.class));
        assertTrue(result.contains(Comparable.class));
        assertEquals(2, result.size());
    }

    @Test
    public void testFindLowestCommonSupertypes_ExcludesStandard() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(String.class, StringBuilder.class);
        // Object, Serializable, Cloneable, Externalizable are excluded automatically.
        // The remaining common supertype is CharSequence.
        assertEquals(Collections.singleton(CharSequence.class), result);
    }

    @Test
    public void testFindLowestCommonSupertype_SingleResult() {
        Class<?> common = ClassUtilities.findLowestCommonSupertype(ArrayList.class, LinkedList.class);
        // Both share AbstractList as the most specific common class.
        assertEquals(AbstractList.class, common);
    }

    @Test
    public void testHaveCommonAncestor() {
        assertTrue(ClassUtilities.haveCommonAncestor(ArrayList.class, HashSet.class));
        assertFalse(ClassUtilities.haveCommonAncestor(String.class, int.class));
    }

    /* ---------------------------------------------------------------------- */
    /*  newInstance – various construction scenarios                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testNewInstance_NoArgs() {
        Object o = ClassUtilities.newInstance(converter, SimplePojo.class, null);
        assertTrue(o instanceof SimplePojo);
    }

    @Test
    public void testNewInstance_WithMatchingArgs() {
        List<Object> args = Arrays.asList("name", 42);
        Object o = ClassUtilities.newInstance(converter, SimplePojo.class, args);
        SimplePojo pojo = (SimplePojo) o;
        assertEquals("name", pojo.name);
        assertEquals(42, pojo.value);
    }

    @Test
    public void testNewInstance_PrimitiveWrapperMatching() {
        // Provide Integer where int is expected; wrapper handling should succeed.
        List<Object> args = Collections.singletonList(99);
        Object o = ClassUtilities.newInstance(converter, SimplePojo.class, args);
        SimplePojo pojo = (SimplePojo) o;
        assertEquals(99, pojo.value);
    }

    @Test
    public void testNewInstance_InnerNonStaticClass() {
        // The inner class requires an enclosing instance; newInstance should create it recursively.
        Object inner = ClassUtilities.newInstance(converter, InnerPojo.class, null);
        assertTrue(inner instanceof InnerPojo);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstance_AbstractClassFails() {
        ClassUtilities.newInstance(converter, AbstractList.class, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstance_InterfaceFails() {
        ClassUtilities.newInstance(converter, List.class, null);
    }

    @Test(expected = SecurityException.class)
    public void testNewInstance_SecurityBlockedClass() {
        // ProcessBuilder is in the security‑blocked list.
        ClassUtilities.newInstance(converter, ProcessBuilder.class, null);
    }

    /* ---------------------------------------------------------------------- */
    /*  SecurityChecker direct verification                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testSecurityChecker_IsSecurityBlocked() {
        assertTrue(ClassUtilities.SecurityChecker.isSecurityBlocked(ProcessBuilder.class));
        assertFalse(ClassUtilities.SecurityChecker.isSecurityBlocked(String.class));
    }

    @Test
    public void testSecurityChecker_IsSecurityBlockedName() {
        assertTrue(ClassUtilities.SecurityChecker.isSecurityBlockedName("java.lang.ProcessImpl"));
        assertFalse(ClassUtilities.SecurityChecker.isSecurityBlockedName("java.lang.String"));
    }

    /* ---------------------------------------------------------------------- */
    /*  Unsafe path – enable and verify that unsafe is used only when needed. */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testUnsafeInstantiationEnabled() {
        // Enable unsafe; instantiate a class with only private constructors.
        ClassUtilities.setUseUnsafe(true);
        // PrivateCtorUtil has only a private constructor, so unsafe should be exercised.
        Object instance = ClassUtilities.newInstance(converter, PrivateCtorUtil.class, null);
        assertNotNull(instance);
        assertTrue(instance instanceof PrivateCtorUtil);
        // Disable for other tests.
        ClassUtilities.setUseUnsafe(false);
    }
}
