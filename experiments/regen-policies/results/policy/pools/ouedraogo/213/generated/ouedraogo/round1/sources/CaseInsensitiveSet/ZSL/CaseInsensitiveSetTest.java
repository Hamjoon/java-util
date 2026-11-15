package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link CaseInsensitiveSet}.
 *
 * The tests exercise every public method, all constructors, and the
 * case‑insensitive behaviour for {@link String} elements while also
 * checking that non‑String elements are handled normally.
 */
public class CaseInsensitiveSetTest {

    private CaseInsensitiveSet<String> stringSet;
    private CaseInsensitiveSet<Object> mixedSet;

    @Before
    public void setUp() {
        stringSet = new CaseInsensitiveSet<>();
        mixedSet = new CaseInsensitiveSet<>();
    }

    /* --------------------------------------------------------------------- *
     *  Constructors
     * --------------------------------------------------------------------- */

    @Test
    public void defaultConstructorCreatesEmptySet() {
        assertTrue(stringSet.isEmpty());
        assertEquals(0, stringSet.size());
    }

    @Test
    public void collectionConstructorHandlesCaseInsensitiveDuplicates() {
        List<String> source = Arrays.asList("Alpha", "ALPHA", "Beta");
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(source);
        assertEquals(2, set.size());
        assertTrue(set.contains("alpha"));
        assertTrue(set.contains("BETA"));
        // Original case is preserved in iteration
        Iterator<String> it = set.iterator();
        assertEquals("Alpha", it.next()); // first inserted version kept
        assertEquals("Beta", it.next());
    }

    @Test
    public void collectionConstructorWithSortedSetUsesTreeMapBranch() {
        SortedSet<String> treeSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        treeSet.addAll(Arrays.asList("cat", "Dog", "CAT"));
        // The TreeSet already enforces case‑insensitive uniqueness, but we are testing the
        // constructor path that detects a SortedSet.
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(treeSet);
        assertEquals(2, set.size());
        assertTrue(set.contains("dog"));
        assertTrue(set.contains("cat"));
    }

    @Test
    public void collectionAndBackingMapConstructorSharesProvidedMap() {
        Map<String, Object> backing = new HashMap<>();
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(Collections.emptyList(), backing);
        assertTrue(set.isEmpty());

        // add an element and verify that the external map sees it
        set.add("Zebra");
        assertTrue(backing.containsKey("Zebra"));
        assertEquals(1, backing.size());
    }

    @Test
    public void initialCapacityConstructorCreatesUsableSet() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(10);
        assertTrue(set.isEmpty());
        assertTrue(set.add("one"));
        assertTrue(set.add("two"));
        assertEquals(2, set.size());
    }

    @Test
    public void capacityAndLoadFactorConstructorCreatesUsableSet() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(5, 0.75f);
        assertTrue(set.isEmpty());
        assertTrue(set.add("x"));
        assertTrue(set.add("y"));
        assertEquals(2, set.size());
    }

    /* --------------------------------------------------------------------- *
     *  Basic Set Operations – case‑insensitive behaviour for Strings
     * --------------------------------------------------------------------- */

    @Test
    public void addAndContainsAreCaseInsensitive() {
        assertTrue(stringSet.add("Hello"));
        assertFalse(stringSet.add("hello")); // duplicate (case‑insensitive)
        assertEquals(1, stringSet.size());

        assertTrue(stringSet.contains("HELLO"));
        assertTrue(stringSet.contains("hello"));
    }

    @Test
    public void removeIsCaseInsensitive() {
        stringSet.add("World");
        assertTrue(stringSet.remove("world"));
        assertFalse(stringSet.contains("WORLD"));
        assertTrue(stringSet.isEmpty());
    }

    @Test
    public void iteratorPreservesOriginalCaseAndInsertionOrder() {
        stringSet.add("First");
        stringSet.add("second");
        stringSet.add("THIRD");

        List<String> iterated = new ArrayList<>();
        for (String s : stringSet) {
            iterated.add(s);
        }

        assertEquals(Arrays.asList("First", "second", "THIRD"), iterated);
    }

    @Test
    public void toArrayReturnsElementsInInsertionOrder() {
        stringSet.add("A");
        stringSet.add("b");
        stringSet.add("C");

        Object[] arr = stringSet.toArray();
        assertArrayEquals(new Object[]{"A", "b", "C"}, arr);
    }

    @Test
    public void toArrayTypedReturnsElementsInInsertionOrder() {
        stringSet.add("X");
        stringSet.add("y");
        String[] arr = stringSet.toArray(new String[0]);
        assertArrayEquals(new String[]{"X", "y"}, arr);
    }

    /* --------------------------------------------------------------------- *
     *  Bulk Operations
     * --------------------------------------------------------------------- */

    @Test
    public void addAllAddsOnlyNewCaseInsensitiveElements() {
        List<String> list = Arrays.asList("one", "Two", "ONE", "three");
        assertTrue(stringSet.addAll(list));
        assertEquals(3, stringSet.size());
        assertTrue(stringSet.containsAll(Arrays.asList("ONE", "two", "THREE")));
    }

    @Test
    public void containsAllIsCaseInsensitive() {
        stringSet.addAll(Arrays.asList("alpha", "beta"));
        assertTrue(stringSet.containsAll(Arrays.asList("ALPHA", "Beta")));
        assertFalse(stringSet.containsAll(Arrays.asList("ALPHA", "Gamma")));
    }

    @Test
    public void retainAllKeepsOnlySpecifiedElementsCaseInsensitively() {
        stringSet.addAll(Arrays.asList("One", "Two", "Three"));
        boolean changed = stringSet.retainAll(Arrays.asList("ONE", "four"));
        assertTrue(changed);
        assertEquals(1, stringSet.size());
        assertTrue(stringSet.contains("one"));
    }

    @Test
    public void removeAllRemovesElementsCaseInsensitively() {
        stringSet.addAll(Arrays.asList("A", "B", "C"));
        boolean changed = stringSet.removeAll(Arrays.asList("b", "c"));
        assertTrue(changed);
        assertEquals(1, stringSet.size());
        assertTrue(stringSet.contains("a"));
    }

    @Test
    public void clearEmptiesTheSet() {
        stringSet.addAll(Arrays.asList("x", "y"));
        stringSet.clear();
        assertTrue(stringSet.isEmpty());
        assertEquals(0, stringSet.size());
    }

    /* --------------------------------------------------------------------- *
     *  Equality, Hashing and toString
     * --------------------------------------------------------------------- */

    @Test
    public void equalsIsCaseInsensitiveAndConsistentWithHashCode() {
        CaseInsensitiveSet<String> set1 = new CaseInsensitiveSet<>();
        CaseInsensitiveSet<String> set2 = new CaseInsensitiveSet<>();

        set1.addAll(Arrays.asList("Apple", "Banana"));
        set2.addAll(Arrays.asList("apple", "BANANA"));

        assertEquals(set1, set2);
        assertEquals(set1.hashCode(), set2.hashCode());

        // Different content – not equal
        set2.add("Cherry");
        assertNotEquals(set1, set2);
    }

    @Test
    public void toStringPreservesOriginalCaseAndOrder() {
        stringSet.addAll(Arrays.asList("first", "Second", "THIRD"));
        String txt = stringSet.toString();
        // Expected format is the one produced by LinkedHashMap.keySet().toString()
        assertEquals("[first, Second, THIRD]", txt);
    }

    /* --------------------------------------------------------------------- *
     *  Mixed Type Behaviour (non‑String elements)
     * --------------------------------------------------------------------- */

    @Test
    public void mixedSetHandlesNonStringElementsNormally() {
        mixedSet.add("Apple");
        mixedSet.add(123);
        mixedSet.add("apple"); // duplicate string, should not be added
        mixedSet.add(123L);    // distinct non‑String element

        assertEquals(3, mixedSet.size());
        assertTrue(mixedSet.contains(123));
        assertTrue(mixedSet.contains(123L));
        assertTrue(mixedSet.contains("Apple"));
    }

    /* --------------------------------------------------------------------- *
     *  Deprecated plus/minus helpers
     * --------------------------------------------------------------------- */

    @Test
    @SuppressWarnings("deprecation")
    public void deprecatedPlusAddsElements() {
        stringSet.plus("delta");
        assertTrue(stringSet.contains("DELTA"));
        assertEquals(1, stringSet.size());

        List<String> more = Arrays.asList("epsilon", "ZETA");
        stringSet.plus(more);
        assertTrue(stringSet.containsAll(Arrays.asList("epsilon", "zeta")));
        assertEquals(3, stringSet.size());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void deprecatedMinusRemovesElements() {
        stringSet.addAll(Arrays.asList("theta", "iota"));
        stringSet.minus("THETA");
        assertFalse(stringSet.contains("theta"));
        assertEquals(1, stringSet.size());

        List<String> toRemove = Arrays.asList("IOTA", "nonexistent");
        stringSet.minus(toRemove);
        assertTrue(stringSet.isEmpty());
    }
}
