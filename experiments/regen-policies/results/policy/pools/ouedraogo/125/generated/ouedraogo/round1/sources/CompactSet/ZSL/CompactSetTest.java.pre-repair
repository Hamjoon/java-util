package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class CompactSetTest {

    private CompactSet<String> defaultSet;
    private CompactSet<Integer> sortedSet;
    private CompactSet<Integer> reverseSet;
    private CompactSet<String> insertionSet;
    private CompactSet<String> caseInsensitiveSet;

    @Before
    public void setUp() {
        // default (unordered, case‑sensitive)
        defaultSet = new CompactSet<>();

        // sorted natural order
        sortedSet = CompactSet.<Integer>builder()
                .sortedOrder()
                .build();

        // reverse sorted order
        reverseSet = CompactSet.<Integer>builder()
                .reverseOrder()
                .build();

        // insertion order
        insertionSet = CompactSet.<String>builder()
                .insertionOrder()
                .build();

        // case‑insensitive string set
        caseInsensitiveSet = CompactSet.<String>builder()
                .caseSensitive(false)
                .build();
    }

    /* ------------------------------- Constructors ------------------------------- */

    @Test
    public void testDefaultConstructor() {
        assertTrue(defaultSet.isEmpty());
        assertEquals(0, defaultSet.size());
    }

    @Test
    public void testCollectionConstructor() {
        List<String> list = Arrays.asList("one", "two", "three");
        CompactSet<String> set = new CompactSet<>(list);
        assertEquals(3, set.size());
        assertTrue(set.containsAll(list));
    }

    @Test(expected = IllegalStateException.class)
    public void testCompactSizeTooSmallThrows() {
        // compactSize of 1 is illegal (must be >=2)
        CompactSet.builder().compactSize(1).build();
    }

    /* ------------------------------- Basic Operations ------------------------------- */

    @Test
    public void testAddAndContains() {
        assertTrue(defaultSet.add("alpha"));
        assertTrue(defaultSet.contains("alpha"));
        assertFalse(defaultSet.add("alpha")); // duplicate, should not modify
        assertEquals(1, defaultSet.size());
    }

    @Test
    public void testAddNull() {
        assertTrue(defaultSet.add(null));
        assertTrue(defaultSet.contains(null));
        assertFalse(defaultSet.add(null)); // duplicate null
        assertEquals(1, defaultSet.size());
    }

    @Test
    public void testRemove() {
        defaultSet.add("beta");
        assertTrue(defaultSet.remove("beta"));
        assertFalse(defaultSet.contains("beta"));
        assertFalse(defaultSet.remove("beta")); // already removed
        assertTrue(defaultSet.isEmpty());
    }

    @Test
    public void testClear() {
        defaultSet.addAll(Arrays.asList("a", "b", "c"));
        assertFalse(defaultSet.isEmpty());
        defaultSet.clear();
        assertTrue(defaultSet.isEmpty());
    }

    /* ------------------------------- Bulk Operations ------------------------------- */

    @Test
    public void testAddAll() {
        List<String> items = Arrays.asList("x", "y", "z");
        assertTrue(defaultSet.addAll(items));
        assertEquals(3, defaultSet.size());
        // adding same collection again should not modify
        assertFalse(defaultSet.addAll(items));
        assertEquals(3, defaultSet.size());
    }

    @Test
    public void testContainsAll() {
        defaultSet.addAll(Arrays.asList("a", "b", "c"));
        assertTrue(defaultSet.containsAll(Arrays.asList("a", "c")));
        assertFalse(defaultSet.containsAll(Arrays.asList("a", "d")));
    }

    @Test
    public void testRemoveAll() {
        defaultSet.addAll(Arrays.asList("a", "b", "c", "d"));
        assertTrue(defaultSet.removeAll(Arrays.asList("b", "d", "x")));
        assertEquals(2, defaultSet.size());
        assertFalse(defaultSet.contains("b"));
        assertFalse(defaultSet.contains("d"));
        // removing non‑existing elements should not change the set
        assertFalse(defaultSet.removeAll(Collections.singleton("z")));
    }

    @Test
    public void testRetainAll() {
        defaultSet.addAll(Arrays.asList("a", "b", "c"));
        assertTrue(defaultSet.retainAll(Arrays.asList("b", "c", "d")));
        assertEquals(2, defaultSet.size());
        assertTrue(defaultSet.contains("b"));
        assertTrue(defaultSet.contains("c"));
        assertFalse(defaultSet.contains("a"));
    }

    /* ------------------------------- Ordering Tests ------------------------------- */

    @Test
    public void testSortedOrderIteration() {
        sortedSet.addAll(Arrays.asList(5, 3, 9, 1));
        Iterator<Integer> it = sortedSet.iterator();
        assertEquals(Integer.valueOf(1), it.next());
        assertEquals(Integer.valueOf(3), it.next());
        assertEquals(Integer.valueOf(5), it.next());
        assertEquals(Integer.valueOf(9), it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void testReverseOrderIteration() {
        reverseSet.addAll(Arrays.asList(2, 4, 1, 3));
        Iterator<Integer> it = reverseSet.iterator();
        assertEquals(Integer.valueOf(4), it.next());
        assertEquals(Integer.valueOf(3), it.next());
        assertEquals(Integer.valueOf(2), it.next());
        assertEquals(Integer.valueOf(1), it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void testInsertionOrderIteration() {
        insertionSet.addAll(Arrays.asList("first", "second", "third"));
        Iterator<String> it = insertionSet.iterator();
        assertEquals("first", it.next());
        assertEquals("second", it.next());
        assertEquals("third", it.next());
        assertFalse(it.hasNext());
    }

    /* ------------------------------- Case Sensitivity ------------------------------- */

    @Test
    public void testCaseInsensitiveBehavior() {
        assertTrue(caseInsensitiveSet.add("Hello"));
        // Adding different case should not increase size
        assertFalse(caseInsensitiveSet.add("hello"));
        assertEquals(1, caseInsensitiveSet.size());
        assertTrue(caseInsensitiveSet.contains("HELLO"));
        assertTrue(caseInsensitiveSet.remove("HeLLo"));
        assertTrue(caseInsensitiveSet.isEmpty());
    }

    /* ------------------------------- Equality, HashCode, ToString ------------------------------- */

    @Test
    public void testEqualsAndHashCode() {
        Set<String> reference = new HashSet<>(Arrays.asList("a", "b", "c"));
        CompactSet<String> testSet = new CompactSet<>(reference);
        assertEquals(reference, testSet);
        assertEquals(reference.hashCode(), testSet.hashCode());
        // modify testSet and verify inequality
        testSet.add("d");
        assertNotEquals(reference, testSet);
    }

    @Test
    public void testToString() {
        CompactSet<Integer> set = new CompactSet<>();
        set.addAll(Arrays.asList(10, 20, 30));
        String s = set.toString();
        // The string should start with '[' and end with ']'
        assertTrue(s.startsWith("["));
        assertTrue(s.endsWith("]"));
        // Contains all elements (order not important for default set)
        assertTrue(s.contains("10"));
        assertTrue(s.contains("20"));
        assertTrue(s.contains("30"));
    }

    /* ------------------------------- toArray Tests ------------------------------- */

    @Test
    public void testToArray() {
        defaultSet.addAll(Arrays.asList("x", "y"));
        Object[] arr = defaultSet.toArray();
        List<Object> list = Arrays.asList(arr);
        assertTrue(list.containsAll(Arrays.asList("x", "y")));
        assertEquals(2, arr.length);
    }

    @Test
    public void testToArrayWithTypedArray() {
        defaultSet.addAll(Arrays.asList("alpha", "beta"));
        String[] target = new String[5];
        String[] result = defaultSet.toArray(target);
        // result should be the same array instance as target
        assertSame(target, result);
        assertEquals("alpha", result[0]);
        assertEquals("beta", result[1]);
        // Remaining positions should be null (per Java spec)
        assertNull(result[2]);
        assertNull(result[3]);
        assertNull(result[4]);
    }

    /* ------------------------------- Iterator Remove ------------------------------- */

    @Test
    public void testIteratorRemove() {
        defaultSet.addAll(Arrays.asList("a", "b", "c"));
        Iterator<String> it = defaultSet.iterator();
        while (it.hasNext()) {
            String val = it.next();
            if ("b".equals(val)) {
                it.remove();
            }
        }
        assertEquals(2, defaultSet.size());
        assertFalse(defaultSet.contains("b"));
    }

    /* ------------------------------- Additional Edge Cases ------------------------------- */

    @Test
    public void testIsEmptyAfterOperations() {
        assertTrue(defaultSet.isEmpty());
        defaultSet.add("elem");
        assertFalse(defaultSet.isEmpty());
        defaultSet.remove("elem");
        assertTrue(defaultSet.isEmpty());
    }

    @Test
    public void testContainsAllWithEmptyCollection() {
        assertTrue(defaultSet.containsAll(Collections.emptyList()));
    }

    @Test
    public void testRetainAllWithEmptySet() {
        defaultSet.addAll(Arrays.asList("a", "b"));
        assertTrue(defaultSet.retainAll(Collections.emptyList()));
        assertTrue(defaultSet.isEmpty());
    }
}
