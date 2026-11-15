package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;

public class CompactCIHashSetTest {

    private CompactCIHashSet<String> ciSet;

    @Before
    public void setUp() {
        ciSet = new CompactCIHashSet<>();
    }

    /*** Constructor Tests ***/

    @Test
    public void testDefaultConstructorCreatesEmptySet() {
        assertTrue("Newly created set should be empty", ciSet.isEmpty());
        assertEquals("Size of new set should be 0", 0, ciSet.size());
    }

    @Test
    public void testConstructorWithCollectionCopiesElementsCaseInsensitively() {
        List<String> source = Arrays.asList("Apple", "Banana", "Cherry");
        CompactCIHashSet<String> set = new CompactCIHashSet<>(source);
        assertEquals("All elements should be copied", 3, set.size());
        assertTrue(set.contains("apple"));
        assertTrue(set.contains("BANANA"));
        assertTrue(set.contains("cHeRrY"));
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullCollectionThrowsNPE() {
        new CompactCIHashSet<String>(null);
    }

    /*** Core Set Behaviour Tests ***/

    @Test
    public void testCaseInsensitiveAddAndContains() {
        assertTrue(ciSet.add("Apple"));
        assertTrue(ciSet.contains("APPLE"));
        assertTrue(ciSet.contains("apple"));
        // Adding same value with different case should not increase size
        assertFalse("Duplicate (different case) should not be added", ciSet.add("aPPle"));
        assertEquals(1, ciSet.size());
    }

    @Test
    public void testCaseInsensitiveRemove() {
        ciSet.add("Orange");
        assertTrue(ciSet.contains("orange"));
        assertTrue(ciSet.remove("ORANGE"));
        assertFalse(ciSet.contains("orange"));
        assertTrue(ciSet.isEmpty());
    }

    @Test
    public void testAddAllAndSize() {
        List<String> items = Arrays.asList("One", "Two", "Three");
        assertTrue(ciSet.addAll(items));
        assertEquals(3, ciSet.size());

        // Adding a collection with different case should not increase size
        List<String> dupes = Arrays.asList("ONE", "two");
        assertFalse(ciSet.addAll(dupes));
        assertEquals(3, ciSet.size());
    }

    @Test
    public void testRetainAllKeepsOnlySpecifiedElements() {
        ciSet.addAll(Arrays.asList("Alpha", "Beta", "Gamma"));
        List<String> toRetain = Arrays.asList("beta", "DELTA");
        assertTrue(ciSet.retainAll(toRetain));
        assertEquals(1, ciSet.size());
        assertTrue(ciSet.contains("BETA"));
        assertFalse(ciSet.contains("Alpha"));
    }

    @Test
    public void testRemoveAllEliminatesSpecifiedElements() {
        ciSet.addAll(Arrays.asList("Cat", "Dog", "Elephant"));
        List<String> toRemove = Arrays.asList("DOG", "fish");
        assertTrue(ciSet.removeAll(toRemove));
        assertEquals(2, ciSet.size());
        assertFalse(ciSet.contains("dog"));
        assertTrue(ciSet.contains("cat"));
        assertTrue(ciSet.contains("elephant"));
    }

    @Test
    public void testClearAndIsEmpty() {
        ciSet.addAll(Arrays.asList("X", "Y", "Z"));
        assertFalse(ciSet.isEmpty());
        ciSet.clear();
        assertTrue(ciSet.isEmpty());
        assertEquals(0, ciSet.size());
    }

    @Test
    public void testIteratorTraversesAllElements() {
        ciSet.addAll(Arrays.asList("Red", "Green", "Blue"));
        Iterator<String> it = ciSet.iterator();
        List<String> collected = new ArrayList<>();
        while (it.hasNext()) {
            collected.add(it.next());
        }
        assertEquals(3, collected.size());
        assertTrue(collected.containsAll(Arrays.asList("red", "green", "blue")));
    }

    @Test
    public void testToArrayReturnsAllElements() {
        ciSet.addAll(Arrays.asList("Sun", "Moon"));
        Object[] array = ciSet.toArray();
        assertEquals(2, array.length);
        assertTrue(Arrays.asList(array).containsAll(Arrays.asList("sun", "moon")));
    }

    @Test
    public void testEqualsAndHashCode() {
        CompactCIHashSet<String> setA = new CompactCIHashSet<>();
        CompactCIHashSet<String> setB = new CompactCIHashSet<>();

        setA.addAll(Arrays.asList("a", "b", "c"));
        setB.addAll(Arrays.asList("C", "B", "A"));

        assertEquals("Sets with same elements (different case) should be equal", setA, setB);
        assertEquals("Hash codes should match for equal sets", setA.hashCode(), setB.hashCode());
    }

    @Test
    public void testToStringContainsAllElements() {
        ciSet.addAll(Arrays.asList("One", "Two"));
        String str = ciSet.toString().toLowerCase();
        assertTrue(str.contains("one"));
        assertTrue(str.contains("two"));
        // Ensure brackets are present (standard Set toString format)
        assertTrue(str.startsWith("["));
        assertTrue(str.endsWith("]"));
    }

    /*** Protected Method Access via Reflection ***/

    @Test
    public void testIsCaseInsensitiveViaReflection() throws Exception {
        Method method = CompactCIHashSet.class.getDeclaredMethod("isCaseInsensitive");
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(ciSet);
        assertTrue("isCaseInsensitive() should return true for CI set", result);
    }

    @Test
    public void testGetNewSetViaReflectionReturnsNull() throws Exception {
        Method method = CompactCIHashSet.class.getDeclaredMethod("getNewSet");
        method.setAccessible(true);
        Object result = method.invoke(ciSet);
        assertNull("getNewSet() is deprecated and should return null", result);
    }
}
