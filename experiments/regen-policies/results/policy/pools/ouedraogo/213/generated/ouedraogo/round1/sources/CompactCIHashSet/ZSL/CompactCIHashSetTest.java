package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.*;

import static org.junit.Assert.*;

public class CompactCIHashSetTest {

    private CompactCIHashSet<String> set;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        set = new CompactCIHashSet<>();
    }

    /*** Constructor tests ***/

    @Test
    public void testDefaultConstructorCreatesEmptySet() {
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    public void testCollectionConstructorCopiesElementsCaseInsensitively() {
        List<String> source = Arrays.asList("One", "TWO", "three", "ONE");
        CompactCIHashSet<String> fromCollection = new CompactCIHashSet<>(source);
        // "ONE" duplicate (different case) should be ignored
        assertEquals(3, fromCollection.size());
        assertTrue(fromCollection.contains("one"));
        assertTrue(fromCollection.contains("two"));
        assertTrue(fromCollection.contains("THREE"));
    }

    @Test(expected = NullPointerException.class)
    public void testCollectionConstructorThrowsOnNull() {
        new CompactCIHashSet<String>(null);
    }

    /*** Basic Set operations ***/

    @Test
    public void testAddAndContainsCaseInsensitive() {
        assertTrue(set.add("Hello"));
        assertTrue(set.contains("hello"));
        assertTrue(set.contains("HELLO"));
        // Adding duplicate (different case) should return false and not increase size
        assertFalse(set.add("HeLLo"));
        assertEquals(1, set.size());
    }

    @Test
    public void testRemoveCaseInsensitive() {
        set.addAll(Arrays.asList("Alpha", "Beta", "Gamma"));
        assertTrue(set.remove("beta"));
        assertFalse(set.contains("BETA"));
        assertEquals(2, set.size());
        // Removing non‑existent element returns false
        assertFalse(set.remove("delta"));
    }

    @Test
    public void testClearAndIsEmpty() {
        set.addAll(Arrays.asList("X", "Y"));
        assertFalse(set.isEmpty());
        set.clear();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    /*** Bulk operations ***/

    @Test
    public void testAddAllAndContainsAllCaseInsensitive() {
        List<String> list = Arrays.asList("A", "b", "C");
        assertTrue(set.addAll(list));
        assertEquals(3, set.size());

        // containsAll should be case‑insensitive
        List<String> query = Arrays.asList("a", "B", "c");
        assertTrue(set.containsAll(query));

        // Adding the same collection again returns false (no new elements)
        assertFalse(set.addAll(list));
    }

    @Test
    public void testRetainAllKeepsOnlySpecifiedElementsCaseInsensitive() {
        set.addAll(Arrays.asList("One", "Two", "Three"));
        List<String> toRetain = Arrays.asList("ONE", "three");
        assertTrue(set.retainAll(toRetain));
        assertEquals(2, set.size());
        assertTrue(set.contains("one"));
        assertTrue(set.contains("THREE"));
        assertFalse(set.contains("two"));
    }

    @Test
    public void testRemoveAllRemovesElementsCaseInsensitive() {
        set.addAll(Arrays.asList("Red", "Green", "Blue"));
        List<String> toRemove = Arrays.asList("GREEN", "blue");
        assertTrue(set.removeAll(toRemove));
        assertEquals(1, set.size());
        assertTrue(set.contains("red"));
        assertFalse(set.contains("green"));
        assertFalse(set.contains("BLUE"));
    }

    /*** Iterator and Array conversion ***/

    @Test
    public void testIteratorTraversesAllElements() {
        set.addAll(Arrays.asList("cat", "dog", "bird"));
        Set<String> collected = new HashSet<>();
        for (String s : set) {
            collected.add(s);
        }
        assertEquals(3, collected.size());
        assertTrue(collected.containsAll(Arrays.asList("cat", "dog", "bird")));
    }

    @Test
    public void testToArrayReturnsCorrectLengthAndElements() {
        set.addAll(Arrays.asList("x", "y"));
        Object[] arr = set.toArray();
        assertEquals(2, arr.length);
        List<Object> list = Arrays.asList(arr);
        assertTrue(list.containsAll(Arrays.asList("x", "y")));

        String[] typed = set.toArray(new String[0]);
        assertEquals(2, typed.length);
        assertTrue(Arrays.asList(typed).containsAll(Arrays.asList("x", "y")));
    }

    /*** Equality, hashCode and toString ***/

    @Test
    public void testEqualsAndHashCodeCaseInsensitive() {
        CompactCIHashSet<String> a = new CompactCIHashSet<>();
        CompactCIHashSet<String> b = new CompactCIHashSet<>();

        a.addAll(Arrays.asList("Apple", "Banana"));
        b.addAll(Arrays.asList("apple", "BANANA"));

        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertEquals(a.hashCode(), b.hashCode());

        // Different element => not equal
        b.add("Cherry");
        assertFalse(a.equals(b));
    }

    @Test
    public void testEqualsWithOtherSetImplementations() {
        Set<String> hashSet = new HashSet<>(Arrays.asList("one", "TWO"));
        CompactCIHashSet<String> ciSet = new CompactCIHashSet<>();
        ciSet.addAll(Arrays.asList("ONE", "two"));

        // Equality should be true because both sets contain the same case‑insensitive strings
        assertTrue(ciSet.equals(hashSet));
        assertTrue(hashSet.equals(ciSet));
    }

    @Test
    public void testToStringContainsElements() {
        set.addAll(Arrays.asList("Alpha", "Beta"));
        String s = set.toString();
        assertTrue(s.contains("Alpha"));
        assertTrue(s.contains("Beta"));
        // The string representation of a set typically starts with '[' and ends with ']'
        assertTrue(s.startsWith("["));
        assertTrue(s.endsWith("]"));
    }

    /*** Configuration related methods ***/

    @Test
    public void testGetConfigReturnsNonNullMap() {
        Map<String, Object> config = set.getConfig();
        assertNotNull(config);
        // The default compact size is defined in CompactMap; we simply verify the map is mutable
        config.put("testKey", "testValue");
        assertEquals("testValue", set.getConfig().get("testKey"));
    }

    @Test
    public void testWithConfigPreservesElementsAndAppliesConfig() {
        set.addAll(Arrays.asList("a", "b"));
        Map<String, Object> newConfig = new HashMap<>();
        newConfig.put("custom", 123);
        CompactCIHashSet<String> newSet = set.withConfig(newConfig);
        assertEquals(set, newSet);
        assertEquals(123, newSet.getConfig().get("custom"));
    }

    /*** isCaseInsensitive flag ***/

    @Test
    public void testIsCaseInsensitiveReturnsTrue() {
        assertTrue(set.isCaseInsensitive());
    }
}
