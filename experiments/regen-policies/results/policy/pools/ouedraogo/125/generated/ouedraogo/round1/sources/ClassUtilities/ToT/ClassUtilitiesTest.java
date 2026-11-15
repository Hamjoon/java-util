package com.cedarsoftware.util;

import com.cedarsoftware.util.convert.Converter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.*;

import static org.junit.Assert.*;

public class ClassUtilitiesTest {

    /* ---------------------------------------------------------------------- */
    /* Helper classes used by several test cases                              */
    /* ---------------------------------------------------------------------- */

    // Simple class with a public no‑arg constructor
    public static class SimpleBean {
        public String value = "default";
    }

    // Class with multiple constructors of different visibility
    public static class MultiCtor {
        public MultiCtor() {}
        protected MultiCtor(String s) {}
        private MultiCtor(Integer i) {}
    }

    // Utility class with a **private** constructor – used for areAllConstructorsPrivate()
    public static final class PrivateUtility {
        private PrivateUtility() {}
    }

    // Enum for getClassIfEnum() tests
    public enum SampleEnum { ONE, TWO }

    // Inner (non‑static) class – used for newInstance() inner‑class handling
    public class InnerClass {
        public final String outerValue;
        public InnerClass() { this.outerValue = "outer"; }
    }

    // Class with no accessible constructors – used for unsafe instantiation
    public static class NoPublicCtor {
        private NoPublicCtor() {}
    }

    // Simple enum nested inside a class (to test enclosing enum detection)
    public static class EnclosingWithEnum {
        public enum NestedEnum { A, B }
    }

    private Converter converter;

    @Before
    public void setUp() {
        converter = new Converter(null);
    }

    @After
    public void tearDown() {
        // Reset any permanent aliases that may have been added during a test
        ClassUtilities.removePermanentClassAlias("AliasForSimpleBean");
        ClassUtilities.setUseUnsafe(false);
    }

    /* ---------------------------------------------------------------------- */
    /* 1. addPermanentClassAlias / removePermanentClassAlias / forName          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAddAndRemovePermanentClassAlias() {
        // Alias for SimpleBean
        ClassUtilities.addPermanentClassAlias(SimpleBean.class, "AliasForSimpleBean");
        Class<?> aliased = ClassUtilities.forName("AliasForSimpleBean", ClassUtilities.class.getClassLoader());
        assertEquals(SimpleBean.class, aliased);

        // Remove alias and verify forName returns null
        ClassUtilities.removePermanentClassAlias("AliasForSimpleBean");
        assertNull(ClassUtilities.forName("AliasForSimpleBean", ClassUtilities.class.getClassLoader()));
    }

    @Test
    public void testForNameWithNullOrEmpty() {
        assertNull(ClassUtilities.forName(null, ClassUtilities.class.getClassLoader()));
        assertNull(ClassUtilities.forName("", ClassUtilities.class.getClassLoader()));
    }

    @Test
    public void testForNameSecurityException() throws Exception {
        // Attempt to load a prohibited class (java.lang.ClassLoader)
        Method internal = ClassUtilities.class.getDeclaredMethod("internalClassForName", String.class, ClassLoader.class);
        internal.setAccessible(true);
        try {
            internal.invoke(null, "java.lang.ClassLoader", ClassUtilities.class.getClassLoader());
            fail("Expected SecurityException");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof SecurityException);
        }
    }

    /* ---------------------------------------------------------------------- */
    /* 2. computeInheritanceDistance                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testComputeInheritanceDistanceSimple() {
        assertEquals(0, ClassUtilities.computeInheritanceDistance(String.class, String.class));
        assertEquals(1, ClassUtilities.computeInheritanceDistance(ArrayList.class, List.class));
        assertEquals(2, ClassUtilities.computeInheritanceDistance(ArrayList.class, Collection.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, Integer.class));
    }

    @Test
    public void testComputeInheritanceDistanceWithInterfaces() {
        assertEquals(1, ClassUtilities.computeInheritanceDistance(HashMap.class, Map.class));
        assertEquals(1, ClassUtilities.computeInheritanceDistance(TreeSet.class, SortedSet.class));
    }

    @Test
    public void testComputeInheritanceDistancePrimitivesAndWrappers() {
        // Primitive → wrapper (distance 0)
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, Integer.class));
        // Wrapper → primitive (distance 0)
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Integer.class, int.class));
        // Different primitives → -1
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, long.class));
    }

    @Test
    public void testComputeInheritanceDistanceNulls() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, String.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, null));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, null));
    }

    /* ---------------------------------------------------------------------- */
    /* 3. isPrimitive                                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIsPrimitive() {
        assertTrue(ClassUtilities.isPrimitive(int.class));
        assertTrue(ClassUtilities.isPrimitive(Integer.class));
        assertTrue(ClassUtilities.isPrimitive(boolean.class));
        assertTrue(ClassUtilities.isPrimitive(Boolean.class));
        assertFalse(ClassUtilities.isPrimitive(String.class));
    }

    /* ---------------------------------------------------------------------- */
    /* 4. isClassFinal                                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIsClassFinal() {
        assertTrue(ClassUtilities.isClassFinal(String.class)); // String is final
        assertFalse(ClassUtilities.isClassFinal(ArrayList.class));
    }

    /* ---------------------------------------------------------------------- */
    /* 5. areAllConstructorsPrivate                                             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAreAllConstructorsPrivate() {
        assertTrue(ClassUtilities.areAllConstructorsPrivate(PrivateUtility.class));
        assertFalse(ClassUtilities.areAllConstructorsPrivate(MultiCtor.class));
    }

    /* ---------------------------------------------------------------------- */
    /* 6. toPrimitiveWrapperClass                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToPrimitiveWrapperClass() {
        assertEquals(Integer.class, ClassUtilities.toPrimitiveWrapperClass(int.class));
        assertEquals(Boolean.class, ClassUtilities.toPrimitiveWrapperClass(boolean.class));
        // Non‑primitive should be returned unchanged
        assertEquals(String.class, ClassUtilities.toPrimitiveWrapperClass(String.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToPrimitiveWrapperClassWithNonPrimitive() {
        ClassUtilities.toPrimitiveWrapperClass(String.class);
    }

    /* ---------------------------------------------------------------------- */
    /* 7. doesOneWrapTheOther                                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDoesOneWrapTheOther() {
        assertTrue(ClassUtilities.doesOneWrapTheOther(Integer.class, int.class));
        assertTrue(ClassUtilities.doesOneWrapTheOther(int.class, Integer.class));
        assertFalse(ClassUtilities.doesOneWrapTheOther(Integer.class, long.class));
    }

    /* ---------------------------------------------------------------------- */
    /* 8. getClassLoader                                                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetClassLoader() {
        ClassLoader cl = ClassUtilities.getClassLoader();
        assertNotNull(cl);
        // The returned loader should be able to load this test class
        assertEquals(ClassUtilitiesTest.class, ClassUtilities.forName(ClassUtilitiesTest.class.getName(), cl));
    }

    /* ---------------------------------------------------------------------- */
    /* 9. findClosest                                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testFindClosest() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(Number.class, "Number");
        map.put(Integer.class, "Integer");
        map.put(Object.class, "Object");

        // Exact match
        assertEquals("Integer", ClassUtilities.findClosest(Integer.class, map, "Default"));

        // Closest superclass (Double → Number)
        assertEquals("Number", ClassUtilities.findClosest(Double.class, map, "Default"));

        // No match → default
        assertEquals("Default", ClassUtilities.findClosest(String.class, Collections.emptyMap(), "Default"));
    }

    /* ---------------------------------------------------------------------- */
    /* 10. loadResourceAsString / loadResourceAsBytes                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testLoadResourceAsStringAndBytes() throws Exception {
        // The test resource is located in the same package as this test class.
        String resourceName = "test.txt";
        InputStream is = this.getClass().getResourceAsStream(resourceName);
        assertNotNull("Test resource not found on classpath", is);
        is.close();

        String content = ClassUtilities.loadResourceAsString(resourceName);
        assertEquals("hello world", content.trim());

        byte[] bytes = ClassUtilities.loadResourceAsBytes(resourceName);
        assertArrayEquals("hello world".getBytes("UTF-8"), bytes);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadResourceAsBytesNotFound() {
        ClassUtilities.loadResourceAsBytes("nonexistent_resource_xyz.txt");
    }

    /* ---------------------------------------------------------------------- */
    /* 11. indexOfSmallestValue                                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIndexOfSmallestValue() {
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(null));
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(new int[0]));
        assertEquals(2, ClassUtilities.indexOfSmallestValue(new int[]{5, 3, 1, 4}));
        // Negative values are ignored
        assertEquals(1, ClassUtilities.indexOfSmallestValue(new int[]{10, 2, -5, 2}));
    }

    /* ---------------------------------------------------------------------- */
    /* 12. getClassIfEnum                                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetClassIfEnum() {
        assertEquals(SampleEnum.class, ClassUtilities.getClassIfEnum(SampleEnum.class));
        // Non‑enum returns null
        assertNull(ClassUtilities.getClassIfEnum(String.class));
        // Enclosing enum detection
        assertEquals(EnclosingWithEnum.NestedEnum.class,
                ClassUtilities.getClassIfEnum(EnclosingWithEnum.NestedEnum.class));
    }

    /* ---------------------------------------------------------------------- */
    /* 13. newInstance – various scenarios                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testNewInstanceSimple() {
        Object o = ClassUtilities.newInstance(converter, SimpleBean.class, null);
        assertTrue(o instanceof SimpleBean);
        assertEquals("default", ((SimpleBean) o).value);
    }

    @Test
    public void testNewInstanceWithArguments() {
        List<Object> args = Arrays.asList("test", 42);
        // Use MultiCtor which has a public no‑arg constructor; arguments will be ignored
        Object o = ClassUtilities.newInstance(converter, MultiCtor.class, args);
        assertTrue(o instanceof MultiCtor);
    }

    @Test
    public void testNewInstanceInnerClass() {
        Object o = ClassUtilities.newInstance(converter, InnerClass.class, null);
        assertTrue(o instanceof InnerClass);
        assertEquals("outer", ((InnerClass) o).outerValue);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstanceSecurityRestrictedClass() {
        // Attempt to instantiate a prohibited class
        ClassUtilities.newInstance(converter, ProcessBuilder.class, null);
    }

    @Test
    public void testNewInstanceUsingUnsafe() {
        // Enable unsafe path and try to instantiate a class with only private ctor
        ClassUtilities.setUseUnsafe(true);
        Object o = ClassUtilities.newInstance(converter, NoPublicCtor.class, null);
        assertNotNull(o);
        assertTrue(o instanceof NoPublicCtor);
    }

    /* ---------------------------------------------------------------------- */
    /* 14. findLowestCommonSupertypesExcluding / findLowestCommonSupertypes   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testFindLowestCommonSupertypesExcluding() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(
                TreeSet.class, HashSet.class,
                Collections.singleton(Object.class));
        // Both are Sets; Set should be the only lowest common supertype after skipping Object
        assertEquals(Collections.singleton(Set.class), result);
    }

    @Test
    public void testFindLowestCommonSupertypes() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(Integer.class, Double.class);
        // Expected common supertypes: Number, Comparable, Serializable (order not important)
        assertTrue(result.contains(Number.class));
        assertTrue(result.contains(Comparable.class));
        assertTrue(result.contains(Serializable.class));
        assertFalse(result.contains(Object.class));
    }

    @Test
    public void testFindLowestCommonSupertype() {
        Class<?> t = ClassUtilities.findLowestCommonSupertype(ArrayList.class, LinkedList.class);
        // Both extend AbstractList, which is the most specific common class
        assertEquals(AbstractList.class, t);
    }

    /* ---------------------------------------------------------------------- */
    /* 15. haveCommonAncestor                                                  */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testHaveCommonAncestor() {
        assertTrue(ClassUtilities.haveCommonAncestor(ArrayList.class, LinkedList.class));
        assertFalse(ClassUtilities.haveCommonAncestor(String.class, Integer.class));
    }

    /* ---------------------------------------------------------------------- */
    /* 16. getAllSupertypes – sanity check                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetAllSupertypes() {
        Set<Class<?>> supertypes = ClassUtilities.getAllSupertypes(HashMap.class);
        assertTrue(supertypes.contains(Map.class));
        assertTrue(supertypes.contains(Cloneable.class));
        assertTrue(supertypes.contains(Serializable.class));
        assertTrue(supertypes.contains(Object.class));
    }

    /* ---------------------------------------------------------------------- */
    /* 17. Private helper: invoke internalClassForName to ensure caching works   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testInternalClassForNameCaching() throws Exception {
        // First call – loads and caches
        Class<?> first = ClassUtilities.forName("java.util.ArrayList", ClassUtilities.class.getClassLoader());
        assertEquals(ArrayList.class, first);

        // Remove from cache via reflection and ensure subsequent call still works
        java.lang.reflect.Field cacheField = ClassUtilities.class.getDeclaredField("nameToClass");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Class<?>> cache = (Map<String, Class<?>>) cacheField.get(null);
        cache.remove("java.util.ArrayList");

        // Second call – should reload and re‑cache
        Class<?> second = ClassUtilities.forName("java.util.ArrayList", ClassUtilities.class.getClassLoader());
        assertEquals(ArrayList.class, second);
    }
}
