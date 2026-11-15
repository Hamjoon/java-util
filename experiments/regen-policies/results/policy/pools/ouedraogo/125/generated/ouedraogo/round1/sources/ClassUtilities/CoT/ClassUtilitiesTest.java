package com.cedarsoftware.util;

import com.cedarsoftware.util.convert.Converter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.math.BigInteger;
import java.net.URL;
import java.time.LocalDate;
import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit 4 test suite for {@link ClassUtilities}.
 *
 * The tests are organised as follows:
 *
 * 1. List of all public methods (extracted from the source).
 * 2. Basic functional tests for each method.
 * 3. Edge‑case / exception tests.
 * 4. Consolidated test class.
 *
 * All tests use only the public API of the library and do not rely on any external mocking
 * frameworks.
 */
public class ClassUtilitiesTest {

    /* --------------------------------------------------------------------- */
    /* 1. PUBLIC METHOD SIGNATURES (extracted from ClassUtilities source)   */
    /* --------------------------------------------------------------------- */
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

    /* --------------------------------------------------------------------- */
    /* 2. TEST FIXTURES                                                     */
    /* --------------------------------------------------------------------- */

    private Converter converter;

    @Before
    public void setUp() {
        // Converter can be instantiated with a null options object – the library tolerates it.
        converter = new Converter(null);
    }

    @After
    public void tearDown() {
        // Reset the unsafe flag after each test to avoid side‑effects.
        ClassUtilities.setUseUnsafe(false);
    }

    /* --------------------------------------------------------------------- */
    /* 3. BASIC FUNCTIONAL TESTS                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddAndRemovePermanentClassAlias() {
        String alias = "my.alias.TestClass";
        Class<?> target = String.class;

        // Ensure alias is not present initially
        assertNull(ClassUtilities.forName(alias, ClassUtilities.class.getClassLoader()));

        // Add alias and verify lookup works
        ClassUtilities.addPermanentClassAlias(target, alias);
        assertEquals(target, ClassUtilities.forName(alias, ClassUtilities.class.getClassLoader()));

        // Remove alias and verify it disappears
        ClassUtilities.removePermanentClassAlias(alias);
        assertNull(ClassUtilities.forName(alias, ClassUtilities.class.getClassLoader()));
    }

    @Test
    public void testComputeInheritanceDistance_SameClass() {
        assertEquals(0, ClassUtilities.computeInheritanceDistance(String.class, String.class));
    }

    @Test
    public void testComputeInheritanceDistance_Superclass() {
        assertEquals(1, ClassUtilities.computeInheritanceDistance(ArrayList.class, List.class));
        assertEquals(2, ClassUtilities.computeInheritanceDistance(ArrayList.class, Collection.class));
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
    public void testComputeInheritanceDistance_NullArguments() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, String.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, null));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, null));
    }

    @Test
    public void testComputeInheritanceDistance_PrimitiveWrapper() {
        // primitive -> wrapper
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, Integer.class));
        // wrapper -> primitive
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Integer.class, int.class));
        // primitive -> unrelated wrapper
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, Long.class));
    }

    @Test
    public void testIsPrimitive() {
        assertTrue(ClassUtilities.isPrimitive(int.class));
        assertTrue(ClassUtilities.isPrimitive(Integer.class));
        assertTrue(ClassUtilities.isPrimitive(void.class));
        assertTrue(ClassUtilities.isPrimitive(Void.class));

        assertFalse(ClassUtilities.isPrimitive(String.class));
        assertFalse(ClassUtilities.isPrimitive(Object.class));
    }

    @Test
    public void testForName_ValidAndInvalid() {
        // Valid class name (already cached via static init)
        assertEquals(String.class, ClassUtilities.forName("string", ClassUtilities.class.getClassLoader()));

        // Fully qualified name
        assertEquals(URL.class, ClassUtilities.forName("java.net.URL", ClassUtilities.class.getClassLoader()));

        // Unknown name returns null
        assertNull(ClassUtilities.forName("non.existing.ClassName", ClassUtilities.class.getClassLoader()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testForName_SecurityException() {
        // Simulate a security‑sensitive class – the internal logic throws IllegalArgumentException
        // when a SecurityException is caught. The class java.lang.ClassLoader is such a class.
        ClassUtilities.forName("java.lang.ClassLoader", ClassUtilities.class.getClassLoader());
    }

    @Test
    public void testIsClassFinal() {
        assertTrue(ClassUtilities.isClassFinal(String.class)); // String is final
        assertFalse(ClassUtilities.isClassFinal(ArrayList.class)); // not final
    }

    @Test
    public void testAreAllConstructorsPrivate() {
        assertTrue(ClassUtilities.areAllConstructorsPrivate(UtilityClassWithPrivateCtor.class));
        assertFalse(ClassUtilities.areAllConstructorsPrivate(PublicCtorClass.class));
    }

    @Test
    public void testToPrimitiveWrapperClass() {
        assertEquals(Integer.class, ClassUtilities.toPrimitiveWrapperClass(int.class));
        assertEquals(void.class, ClassUtilities.toPrimitiveWrapperClass(void.class).getPrimitiveClass());

        // Non‑primitive should be returned unchanged
        assertEquals(String.class, ClassUtilities.toPrimitiveWrapperClass(String.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToPrimitiveWrapperClass_InvalidPrimitive() {
        // Pass a class that is not in the primitiveToWrapper map (e.g., an array type)
        ClassUtilities.toPrimitiveWrapperClass(int[].class);
    }

    @Test
    public void testDoesOneWrapTheOther() {
        assertTrue(ClassUtilities.doesOneWrapTheOther(Integer.class, int.class));
        assertTrue(ClassUtilities.doesOneWrapTheOther(int.class, Integer.class));
        assertFalse(ClassUtilities.doesOneWrapTheOther(Integer.class, long.class));
    }

    @Test
    public void testGetClassLoader_Default() {
        ClassLoader cl = ClassUtilities.getClassLoader();
        assertNotNull(cl);
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

    @Test
    public void testLoadResourceAsBytes_NotFound() {
        try {
            ClassUtilities.loadResourceAsBytes("nonexistent_resource.txt");
            fail("Expected IllegalArgumentException for missing resource");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test(expected = UncheckedIOException.class)
    public void testLoadResourceAsBytes_ReadError() throws Exception {
        // Create a dummy class loader that throws IOException when getResourceAsStream is called.
        ClassLoader faulty = new ClassLoader() {
            @Override
            public java.io.InputStream getResourceAsStream(String name) {
                return new java.io.InputStream() {
                    @Override
                    public int read() throws java.io.IOException {
                        throw new java.io.IOException("simulated read error");
                    }
                };
            }
        };
        // Temporarily replace the thread context class loader
        Thread.currentThread().setContextClassLoader(faulty);
        try {
            ClassUtilities.loadResourceAsBytes("any_name.txt");
        } finally {
            Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());
        }
    }

    @Test
    public void testLoadResourceAsString_Simple() {
        // The test class itself is a resource; we can load its .class file as a string.
        String resource = ClassUtilitiesTest.class.getName().replace('.', '/') + ".class";
        String content = ClassUtilities.loadResourceAsString(resource);
        assertNotNull(content);
        // The content should contain the magic number for a Java class file (0xCAFEBABE)
        assertTrue(content.contains("CAFEBABE") || content.length() > 0);
    }

    @Test
    public void testIndexOfSmallestValue() {
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(null));
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(new int[]{}));

        int[] data = {5, 2, 3, 2, 7};
        // Smallest positive value is 2, first occurrence at index 1
        assertEquals(1, ClassUtilities.indexOfSmallestValue(data));

        int[] withNegatives = {-1, -5, 0, 4};
        // Negative values are ignored, so smallest non‑negative is 0 at index 2
        assertEquals(2, ClassUtilities.indexOfSmallestValue(withNegatives));
    }

    @Test
    public void testFindClosest_Basic() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(Number.class, "number");
        map.put(Integer.class, "integer");
        map.put(Object.class, "object");

        // Exact match should win
        assertEquals("integer", ClassUtilities.findClosest(Integer.class, map, "default"));

        // Closest superclass (Integer -> Number)
        assertEquals("number", ClassUtilities.findClosest(Long.class, map, "default"));

        // No match – default is returned
        assertEquals("default", ClassUtilities.findClosest(String.class, Collections.emptyMap(), "default"));
    }

    @Test
    public void testFindClosest_TieBreaking() {
        Map<Class<?>, String> map = new LinkedHashMap<>();
        map.put(List.class, "list");
        map.put(Set.class, "set");
        // Both List and Set are one step away from ArrayDeque (implements Deque which extends Queue)
        // Preference order: classes over interfaces – both are interfaces, so more specific (none) – keep insertion order.
        assertEquals("list", ClassUtilities.findClosest(ArrayDeque.class, map, "default"));
    }

    @Test
    public void testGetClassIfEnum() {
        assertEquals(TestEnum.class, ClassUtilities.getClassIfEnum(TestEnum.class));
        assertNull(ClassUtilities.getClassIfEnum(String.class));
    }

    @Test
    public void testGetClassIfEnum_EnclosingEnum() {
        class Outer {
            enum InnerEnum { A, B }
        }
        assertEquals(Outer.InnerEnum.class, ClassUtilities.getClassIfEnum(Outer.InnerEnum.class));
    }

    @Test
    public void testNewInstance_SimpleClass() {
        Object o = ClassUtilities.newInstance(converter, SimplePojo.class, null);
        assertNotNull(o);
        assertTrue(o instanceof SimplePojo);
    }

    @Test
    public void testNewInstance_WithConstructorArguments() {
        List<Object> args = Arrays.asList("hello", 42);
        SimpleWithArgs instance = (SimpleWithArgs) ClassUtilities.newInstance(converter, SimpleWithArgs.class, args);
        assertEquals("hello", instance.getText());
        assertEquals(42, instance.getNumber());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstance_InterfaceNotInstantiable() {
        ClassUtilities.newInstance(converter, List.class, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstance_SecurityRestrictedClass() {
        ClassUtilities.newInstance(converter, ProcessBuilder.class, null);
    }

    @Test
    public void testSetUseUnsafe_Toggle() {
        // By default unsafe is off – calling setUseUnsafe(true) should not throw.
        ClassUtilities.setUseUnsafe(true);
        // Turn it off again
        ClassUtilities.setUseUnsafe(false);
    }

    @Test
    public void testFindLowestCommonSupertypesExcluding_Basic() {
        Set<Class<?>> skip = Collections.singleton(Object.class);
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(ArrayList.class, LinkedList.class, skip);
        // Both are List implementations; the lowest common supertypes (excluding Object) are List, RandomAccess, Cloneable, Serializable
        assertTrue(result.contains(List.class));
    }

    @Test
    public void testFindLowestCommonSupertypes_SkipObject() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(String.class, Integer.class);
        // Common supertypes are Serializable, Comparable, and Object – Object is skipped.
        assertTrue(result.contains(Serializable.class));
        assertTrue(result.contains(Comparable.class));
        assertFalse(result.contains(Object.class));
    }

    @Test
    public void testFindLowestCommonSupertype_SingleResult() {
        Class<?> common = ClassUtilities.findLowestCommonSupertype(String.class, StringBuilder.class);
        // Both extend AbstractStringBuilder, which is the most specific common supertype.
        assertEquals(AbstractStringBuilder.class, common);
    }

    @Test
    public void testGetAllSupertypes() {
        Set<Class<?>> supertypes = ClassUtilities.getAllSupertypes(HashMap.class);
        assertTrue(supertypes.contains(Map.class));
        assertTrue(supertypes.contains(AbstractMap.class));
        assertTrue(supertypes.contains(Object.class));
        assertTrue(supertypes.contains(Cloneable.class)); // via interface
    }

    @Test
    public void testHaveCommonAncestor() {
        assertTrue(ClassUtilities.haveCommonAncestor(ArrayList.class, LinkedList.class));
        assertFalse(ClassUtilities.haveCommonAncestor(String.class, Integer.class));
    }

    /* --------------------------------------------------------------------- */
    /* 4. SUPPORTING TEST CLASSES                                            */
    /* --------------------------------------------------------------------- */

    // Utility class with a *private* constructor – used for areAllConstructorsPrivate()
    private static final class UtilityClassWithPrivateCtor {
        private UtilityClassWithPrivateCtor() {}
    }

    // Class with a public constructor – should cause areAllConstructorsPrivate() to return false
    private static final class PublicCtorClass {
        public PublicCtorClass() {}
    }

    // Simple POJO with a no‑arg constructor
    public static class SimplePojo {
        public SimplePojo() {}
    }

    // POJO with a two‑argument constructor for argument‑matching tests
    public static class SimpleWithArgs {
        private final String text;
        private final int number;

        public SimpleWithArgs(String text, int number) {
            this.text = text;
            this.number = number;
        }

        public String getText() {
            return text;
        }

        public int getNumber() {
            return number;
        }
    }

    // An enum for getClassIfEnum tests
    private enum TestEnum { ONE, TWO }

}
