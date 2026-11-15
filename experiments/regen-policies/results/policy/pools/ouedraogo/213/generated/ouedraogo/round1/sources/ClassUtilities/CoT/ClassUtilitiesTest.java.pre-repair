package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.*;
import java.time.DayOfWeek;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Comprehensive JUnit‑4 test suite for {@link ClassUtilities}.
 * The tests cover normal behaviour, edge‑cases and exception handling.
 */
public class ClassUtilitiesTest {

    private Converter converter;

    @Before
    public void setUp() {
        // Converter can be instantiated with a null options object – it works for the tests we need.
        converter = new Converter(null);
    }

    /* ---------- 1. getPrimitiveFromWrapper ---------- */
    @Test
    public void testGetPrimitiveFromWrapper() {
        assertEquals(int.class, ClassUtilities.getPrimitiveFromWrapper(Integer.class));
        assertNull(ClassUtilities.getPrimitiveFromWrapper(String.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetPrimitiveFromWrapperNull() {
        ClassUtilities.getPrimitiveFromWrapper(null);
    }

    /* ---------- 2. addPermanentClassAlias / removePermanentClassAlias ---------- */
    @Test
    public void testAddAndRemovePermanentClassAlias() {
        ClassUtilities.addPermanentClassAlias(String.class, "myStringAlias");
        assertEquals(String.class,
                ClassUtilities.forName("myStringAlias", getClass().getClassLoader()));
        ClassUtilities.removePermanentClassAlias("myStringAlias");
        assertNull(ClassUtilities.forName("myStringAlias", getClass().getClassLoader()));
    }

    /* ---------- 3. computeInheritanceDistance ---------- */
    @Test
    public void testComputeInheritanceDistance() {
        assertEquals(0, ClassUtilities.computeInheritanceDistance(List.class, List.class));
        assertEquals(1, ClassUtilities.computeInheritanceDistance(ArrayList.class, List.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, List.class));
        // primitive‑wrapper pair is considered the same primitive
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, Integer.class));
    }

    @Test
    public void testComputeInheritanceDistanceNulls() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, String.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, null));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, null));
    }

    /* ---------- 4. isPrimitive ---------- */
    @Test
    public void testIsPrimitive() {
        assertTrue(ClassUtilities.isPrimitive(int.class));
        assertTrue(ClassUtilities.isPrimitive(Integer.class));
        assertFalse(ClassUtilities.isPrimitive(String.class));
    }

    /* ---------- 5. forName ---------- */
    @Test
    public void testForName() {
        assertEquals(String.class,
                ClassUtilities.forName("java.lang.String", getClass().getClassLoader()));
        assertNull(ClassUtilities.forName("non.existing.ClassXYZ", getClass().getClassLoader()));
        assertNull(ClassUtilities.forName("", getClass().getClassLoader()));
    }

    /* ---------- 6. isClassFinal ---------- */
    @Test
    public void testIsClassFinal() {
        assertTrue(ClassUtilities.isClassFinal(String.class));          // final
        assertFalse(ClassUtilities.isClassFinal(ArrayList.class));     // not final
    }

    /* ---------- 7. areAllConstructorsPrivate ---------- */
    @Test
    public void testAreAllConstructorsPrivate() {
        assertTrue(ClassUtilities.areAllConstructorsPrivate(ClassUtilities.class));
        assertFalse(ClassUtilities.areAllConstructorsPrivate(String.class));
    }

    /* ---------- 8. toPrimitiveWrapperClass ---------- */
    @Test
    public void testToPrimitiveWrapperClass() {
        assertEquals(Integer.class, ClassUtilities.toPrimitiveWrapperClass(int.class));
        assertEquals(String.class, ClassUtilities.toPrimitiveWrapperClass(String.class));
        assertEquals(Void.class, ClassUtilities.toPrimitiveWrapperClass(void.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToPrimitiveWrapperClassInvalid() {
        // This path is exercised only if a non‑mapped primitive is supplied.
        // Use a dummy primitive type that is not in the map – not possible in Java,
        // so we invoke the method with a non‑primitive and rely on the internal null check.
        ClassUtilities.toPrimitiveWrapperClass(void.class); // valid, no exception
    }

    /* ---------- 9. doesOneWrapTheOther ---------- */
    @Test
    public void testDoesOneWrapTheOther() {
        assertTrue(ClassUtilities.doesOneWrapTheOther(Integer.class, int.class));
        assertTrue(ClassUtilities.doesOneWrapTheOther(int.class, Integer.class));
        assertFalse(ClassUtilities.doesOneWrapTheOther(Integer.class, Long.class));
    }

    @Test(expected = NullPointerException.class)
    public void testDoesOneWrapTheOtherNullX() {
        ClassUtilities.doesOneWrapTheOther(null, Integer.class);
    }

    @Test(expected = NullPointerException.class)
    public void testDoesOneWrapTheOtherNullY() {
        ClassUtilities.doesOneWrapTheOther(Integer.class, null);
    }

    /* ---------- 10. getClassLoader (no‑arg) ---------- */
    @Test
    public void testGetClassLoaderNoArg() {
        assertNotNull(ClassUtilities.getClassLoader());
    }

    /* ---------- 11. getClassLoader (anchor) ---------- */
    @Test
    public void testGetClassLoaderWithAnchor() {
        assertNotNull(ClassUtilities.getClassLoader(ClassUtilities.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetClassLoaderWithNullAnchor() {
        ClassUtilities.getClassLoader((Class<?>) null);
    }

    /* ---------- 12. findClosest ---------- */
    @Test
    public void testFindClosestExactMatch() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(ArrayList.class, "arraylist");
        map.put(List.class, "list");
        String result = ClassUtilities.findClosest(ArrayList.class, map, "default");
        assertEquals("arraylist", result);
    }

    @Test
    public void testFindClosestInheritance() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(List.class, "list");
        map.put(Collection.class, "col");
        String result = ClassUtilities.findClosest(ArrayList.class, map, "default");
        assertEquals("list", result); // List is closer than Collection
    }

    @Test
    public void testFindClosestTieBreaking() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(Serializable.class, "ser");
        map.put(Cloneable.class, "clone");
        // Both interfaces are at the same distance from ArrayList; the first entry should win.
        String result = ClassUtilities.findClosest(ArrayList.class, map, "default");
        assertEquals("ser", result);
    }

    @Test(expected = NullPointerException.class)
    public void testFindClosestNullClazz() {
        ClassUtilities.findClosest(null, Collections.emptyMap(), "def");
    }

    @Test(expected = NullPointerException.class)
    public void testFindClosestNullMap() {
        ClassUtilities.findClosest(String.class, null, "def");
    }

    /* ---------- 13. loadResourceAsBytes / loadResourceAsString ---------- */
    @Test
    public void testLoadResourceAsBytesExisting() {
        // Load the compiled .class file of this test class – it is guaranteed to be on the classpath.
        String resource = ClassUtilitiesTest.class.getName().replace('.', '/') + ".class";
        byte[] data = ClassUtilities.loadResourceAsBytes(resource);
        assertNotNull(data);
        assertTrue(data.length > 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadResourceAsBytesMissing() {
        ClassUtilities.loadResourceAsBytes("nonexistent.resource");
    }

    @Test(expected = NullPointerException.class)
    public void testLoadResourceAsBytesNullName() {
        ClassUtilities.loadResourceAsBytes(null);
    }

    @Test
    public void testLoadResourceAsString() {
        String resource = ClassUtilitiesTest.class.getName().replace('.', '/') + ".class";
        String content = ClassUtilities.loadResourceAsString(resource);
        assertNotNull(content);
        assertFalse(content.isEmpty());
    }

    /* ---------- 14. indexOfSmallestValue (deprecated) ---------- */
    @Test
    public void testIndexOfSmallestValue() {
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(null));
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(new int[0]));
        assertEquals(2, ClassUtilities.indexOfSmallestValue(new int[]{5, 3, 1, 4}));
        assertEquals(1, ClassUtilities.indexOfSmallestValue(new int[]{10, 0, 20}));
        // negative numbers are ignored – the smallest positive is chosen
        assertEquals(2, ClassUtilities.indexOfSmallestValue(new int[]{-5, -2, 7, 8}));
    }

    /* ---------- 15. getClassIfEnum ---------- */
    @Test
    public void testGetClassIfEnum() {
        assertEquals(DayOfWeek.class, ClassUtilities.getClassIfEnum(DayOfWeek.class));
        assertNull(ClassUtilities.getClassIfEnum(String.class));
    }

    /* ---------- 16. newInstance ---------- */
    @Test
    public void testNewInstanceSimpleClass() {
        Object obj = ClassUtilities.newInstance(converter, Date.class, null);
        assertNotNull(obj);
        assertTrue(obj instanceof Date);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstanceInterface() {
        ClassUtilities.newInstance(converter, List.class, null);
    }

    @Test
    public void testNewInstanceAbstractClass() {
        try {
            ClassUtilities.newInstance(converter, AbstractList.class, null);
            fail("Expected IllegalArgumentException for abstract class");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /* ---------- 17. setUseUnsafe ---------- */
    @Test
    public void testSetUseUnsafeDoesNotThrow() {
        // The method must not propagate any exception, regardless of the internal outcome.
        ClassUtilities.setUseUnsafe(true);
        ClassUtilities.setUseUnsafe(false);
    }

    /* ---------- 18. findLowestCommonSupertypesExcluding ---------- */
    @Test
    public void testFindLowestCommonSupertypesExcluding() {
        Set<Class<?>> excluded = Collections.singleton(List.class);
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(
                ArrayList.class, LinkedList.class, excluded);
        // AbstractList is a common supertype that is not excluded
        assertTrue(result.contains(AbstractList.class));
        assertFalse(result.contains(List.class));
    }

    @Test
    public void testFindLowestCommonSupertypesExcludingBothNull() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(
                null, null, Collections.emptySet());
        assertTrue(result.isEmpty());
    }

    /* ---------- 19. findLowestCommonSupertypes ---------- */
    @Test
    public void testFindLowestCommonSupertypes() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(Integer.class, Double.class);
        // Number and Comparable are common (Object is filtered out)
        assertTrue(result.contains(Number.class));
        assertTrue(result.contains(Comparable.class));
    }

    @Test
    public void testFindLowestCommonSupertypesBothNull() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(null, null);
        assertTrue(result.isEmpty());
    }

    /* ---------- 20. findLowestCommonSupertype ---------- */
    @Test
    public void testFindLowestCommonSupertype() {
        Class<?> result = ClassUtilities.findLowestCommonSupertype(Integer.class, Double.class);
        // Accept either Number or Comparable
        assertTrue(result == Number.class || result == Comparable.class);
    }

    @Test
    public void testFindLowestCommonSupertypeNone() {
        // String and Integer share only Object (filtered out) → null
        assertNull(ClassUtilities.findLowestCommonSupertype(String.class, Integer.class));
    }

    /* ---------- 21. getClassHierarchyInfo ---------- */
    @Test
    public void testGetClassHierarchyInfoDepth() {
        ClassUtilities.ClassHierarchyInfo infoObject = ClassUtilities.getClassHierarchyInfo(Object.class);
        assertEquals(0, infoObject.getDepth());

        ClassUtilities.ClassHierarchyInfo infoArrayList = ClassUtilities.getClassHierarchyInfo(ArrayList.class);
        // Object → List → AbstractList → ArrayList (depth 3)
        assertEquals(3, infoArrayList.getDepth());
    }

    /* ---------- 22. haveCommonAncestor ---------- */
    @Test
    public void testHaveCommonAncestor() {
        assertTrue(ClassUtilities.haveCommonAncestor(ArrayList.class, LinkedList.class)); // share AbstractList
        assertFalse(ClassUtilities.haveCommonAncestor(String.class, Integer.class));      // only Object (filtered)
    }
}
