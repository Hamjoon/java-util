package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link CaseInsensitiveSet}.
 * The tests exercise every public method, including constructors,
 * mutating operations, queries, and the deprecated convenience methods.
 */
public class CaseInsensitiveSetTest {

    private CaseInsensitiveSet<String> emptySet;
    private CaseInsensitiveSet<String> populatedSet;

    @Before
    public void setUp() {
        emptySet = new CaseInsensitiveSet<>();

        // Populate with a mixture of upper‑ and lower‑case strings
        populatedSet = new CaseInsensitiveSet<>();
        populatedSet.add("Apple");
        populatedSet.add("banana");
        populatedSet.add("Cherry");
    }

    /* --------------------------------------------------------------
     *  Constructor tests
     * -------------------------------------------------------------- */
    @Test
    public void testDefaultConstructorCreatesEmptySet() {
        assertTrue(emptySet.isEmpty());
        assertEquals(0, emptySet.size());
    }

    @Test
    public void testCollectionConstructorCopiesElementsCaseInsensitively() {
        List<String> source = Arrays.asList("One", "TWO", "three", "ONE");
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(source);
        // "ONE" should be considered duplicate of "One"
        assertEquals(3, set.size());
        assertTrue(set.contains("one"));
        assertTrue(set.contains("Two"));
        assertTrue(set.contains("THREE"));
    }

    @Test
    public void testCollectionAndBackingMapConstructorUsesProvidedMap() {
        // Use a plain HashMap as backing map (any Map works because the
        // implementation only cares about Map contract)
        java.util.HashMap<String, Object> backing = new java.util.HashMap<>();
        List<String> src = Arrays.asList("Alpha", "beta");
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(src, backing);
        assertEquals(2, set.size());
        assertTrue(set.contains("ALPHA"));
        assertTrue(set.contains("Beta"));
        // Verify that the backing map is indeed the one we passed
        assertSame(backing, getInternalMap(set));
    }

    /* --------------------------------------------------------------
     *  Basic Set contract tests (size, isEmpty, contains, iterator)
     * -------------------------------------------------------------- */
    @Test
    public void testSizeAndIsEmpty() {
        assertTrue(emptySet.isEmpty());
        assertEquals(0, emptySet.size());

        assertFalse(populatedSet.isEmpty());
        assertEquals(3, populatedSet.size());
    }

    @Test
    public void testContainsIsCaseInsensitive() {
        assertTrue(populatedSet.contains("apple"));
        assertTrue(populatedSet.contains("BANANA"));
        assertTrue(populatedSet.contains("cHeRrY"));
        assertFalse(populatedSet.contains("Durian"));
    }

    @Test
    public void testIteratorReturnsOriginalCaseValues() {
        Set<String> seen = new HashSet<>();
        for (String s : populatedSet) {
            seen.add(s);
        }
        // The iterator must return exactly the originally added strings
        assertEquals(new HashSet<>(Arrays.asList("Apple", "banana", "Cherry")), seen);
    }

    @Test
    public void testToArrayAndToArrayTyped() {
        Object[] objArray = populatedSet.toArray();
        assertEquals(3, objArray.length);
        assertTrue(Arrays.asList(objArray).containsAll(Arrays.asList("Apple", "banana", "Cherry")));

        String[] typed = populatedSet.toArray(new String[0]);
        assertEquals(3, typed.length);
        assertTrue(Arrays.asList(typed).containsAll(Arrays.asList("Apple", "banana", "Cherry")));
    }

    /* --------------------------------------------------------------
     *  Mutating operations (add, remove, clear)
     * -------------------------------------------------------------- */
    @Test
    public void testAddIsCaseInsensitiveAndReturnsCorrectFlag() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        assertTrue(set.add("Test"));
        assertFalse(set.add("test")); // duplicate, should return false
        assertEquals(1, set.size());
        assertTrue(set.contains("TEST"));
    }

    @Test
    public void testAddNullElement() {
        assertTrue(emptySet.add(null));
        assertTrue(emptySet.contains(null));
        assertEquals(1, emptySet.size());

        // Adding null again must not increase size
        assertFalse(emptySet.add(null));
        assertEquals(1, emptySet.size());
    }

    @Test
    public void testRemoveIsCaseInsensitive() {
        assertTrue(populatedSet.remove("BANANA"));
        assertFalse(populatedSet.contains("banana"));
        assertEquals(2, populatedSet.size());

        // Removing an element that does not exist returns false
        assertFalse(populatedSet.remove("durian"));
        assertEquals(2, populatedSet.size());
    }

    @Test
    public void testRemoveNullElement() {
        emptySet.add(null);
        assertTrue(emptySet.remove(null));
        assertFalse(emptySet.contains(null));
        assertTrue(emptySet.isEmpty());
    }

    @Test
    public void testClearEmptiesSet() {
        populatedSet.clear();
        assertTrue(populatedSet.isEmpty());
        assertEquals(0, populatedSet.size());
    }

    /* --------------------------------------------------------------
     *  Bulk operations (containsAll, addAll, retainAll, removeAll)
     * -------------------------------------------------------------- */
    @Test
    public void testContainsAllIsCaseInsensitive() {
        List<String> list = Arrays.asList("apple", "BANANA");
        assertTrue(populatedSet.containsAll(list));

        List<String> missing = Arrays.asList("apple", "durian");
        assertFalse(populatedSet.containsAll(missing));
    }

    @Test
    public void testAddAllAddsAllElementsCaseInsensitively() {
        List<String> list = Arrays.asList("date", "ELDERBERRY", "apple"); // "apple" duplicate
        assertTrue(populatedSet.addAll(list));
        assertEquals(5, populatedSet.size()); // only two new elements added
        assertTrue(populatedSet.contains("DATE"));
        assertTrue(populatedSet.contains("elderberry"));
    }

    @Test
    public void testRetainAllKeepsOnlySpecifiedElementsCaseInsensitively() {
        List<String> keep = Arrays.asList("APPLE", "banana", "fig");
        assertTrue(populatedSet.retainAll(keep));
        // "fig" was not present, so result should be just Apple and banana
        assertEquals(2, populatedSet.size());
        assertTrue(populatedSet.contains("apple"));
        assertTrue(populatedSet.contains("BANANA"));
        assertFalse(populatedSet.contains("Cherry"));
    }

    @Test
    public void testRetainAllNoChangeReturnsFalse() {
        List<String> same = Arrays.asList("Apple", "banana", "Cherry");
        assertFalse(populatedSet.retainAll(same));
    }

    @Test
    public void testRemoveAllRemovesElementsCaseInsensitively() {
        List<String> toRemove = Arrays.asList("BANANA", "cherry", "grape");
        assertTrue(populatedSet.removeAll(toRemove));
        assertEquals(1, populatedSet.size());
        assertTrue(populatedSet.contains("Apple"));
        assertFalse(populatedSet.contains("banana"));
        assertFalse(populatedSet.contains("Cherry"));
    }

    @Test
    public void testRemoveAllNoChangeReturnsFalse() {
        List<String> none = Arrays.asList("durian", "elderberry");
        assertFalse(populatedSet.removeAll(none));
        assertEquals(3, populatedSet.size());
    }

    /* --------------------------------------------------------------
     *  Equality and hashCode
     * -------------------------------------------------------------- */
    @Test
    public void testEqualsWithSameElementsDifferentCase() {
        Set<String> other = new HashSet<>(Arrays.asList("apple", "BANANA", "cherry"));
        assertTrue(populatedSet.equals(other));
    }

    @Test
    public void testEqualsReflexiveAndSymmetric() {
        assertTrue(populatedSet.equals(populatedSet)); // reflexive
        Set<String> copy = new CaseInsensitiveSet<>(populatedSet);
        assertTrue(populatedSet.equals(copy));
        assertTrue(copy.equals(populatedSet)); // symmetric
    }

    @Test
    public void testEqualsWithDifferentSizeOrElements() {
        Set<String> smaller = new HashSet<>(Collections.singletonList("Apple"));
        assertFalse(populatedSet.equals(smaller));

        Set<String> different = new HashSet<>(Arrays.asList("Apple", "Banana", "Durian"));
        assertFalse(populatedSet.equals(different));
    }

    @Test
    public void testHashCodeMatchesContract() {
        // Compute expected hash using StringUtilities.hashCodeIgnoreCase for strings
        int expected = 0;
        for (String s : populatedSet) {
            expected += hashCodeIgnoreCase(s);
        }
        assertEquals(expected, populatedSet.hashCode());

        // Adding a duplicate with different case must not change hash
        populatedSet.add("APPLE");
        assertEquals(expected, populatedSet.hashCode());
    }

    /* --------------------------------------------------------------
     *  Deprecated convenience methods (minus, plus)
     * -------------------------------------------------------------- */
    @Test
    @SuppressWarnings("deprecation")
    public void testMinusIterableRemovesElementsCaseInsensitively() {
        List<String> toMinus = Arrays.asList("APPLE", "banana");
        populatedSet.minus(toMinus);
        assertEquals(1, populatedSet.size());
        assertTrue(populatedSet.contains("Cherry"));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testMinusSingleElementRemovesCaseInsensitively() {
        populatedSet.minus("CHERRY");
        assertEquals(2, populatedSet.size());
        assertFalse(populatedSet.contains("cherry"));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testPlusIterableAddsElementsCaseInsensitively() {
        List<String> toAdd = Arrays.asList("date", "BANANA", "ELDERBERRY");
        populatedSet.plus(toAdd);
        // "BANANA" already existed, so only two new items
        assertEquals(5, populatedSet.size());
        assertTrue(populatedSet.contains("date"));
        assertTrue(populatedSet.contains("elderberry"));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testPlusSingleObjectAddsCaseInsensitively() {
        populatedSet.plus("fig");
        assertEquals(4, populatedSet.size());
        assertTrue(populatedSet.contains("FIG"));
    }

    /* --------------------------------------------------------------
     *  toString representation
     * -------------------------------------------------------------- */
    @Test
    public void testToStringReflectsCurrentElements() {
        String str = populatedSet.toString();
        // Order is not guaranteed; verify that each element appears.
        assertTrue(str.contains("Apple"));
        assertTrue(str.contains("banana"));
        assertTrue(str.contains("Cherry"));
        // Ensure it looks like a Set (starts with '[' and ends with ']')
        assertTrue(str.startsWith("["));
        assertTrue(str.endsWith("]"));
    }

    /* --------------------------------------------------------------
     *  Helper to expose the internal map for a single verification test.
     * -------------------------------------------------------------- */
    @SuppressWarnings("unchecked")
    private static <K> java.util.Map<K, Object> getInternalMap(CaseInsensitiveSet<K> set) {
        try {
            java.lang.reflect.Field f = CaseInsensitiveSet.class.getDeclaredField("map");
            f.setAccessible(true);
            return (java.util.Map<K, Object>) f.get(set);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
