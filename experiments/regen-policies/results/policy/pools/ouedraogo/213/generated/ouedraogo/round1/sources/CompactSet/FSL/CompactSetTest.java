package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.util.*;

import static org.junit.Assert.*;

public class CompactSetTest {

    private CompactSet<String> defaultSet;

    @Before
    public void setUp() {
        defaultSet = new CompactSet<>();
    }

    /*--------------------------------------------------------------
     * Basic Set contract tests (size, isEmpty, add, remove, clear)
     *--------------------------------------------------------------*/
    @Test
    public void testAddAndSize() {
        assertTrue(defaultSet.isEmpty());
        assertEquals(0, defaultSet.size());

        assertTrue(defaultSet.add("one"));
        assertTrue(defaultSet.add("two"));
        assertFalse(defaultSet.add("one")); // duplicate, should return false

        assertEquals(2, defaultSet.size());
        assertFalse(defaultSet.isEmpty());
    }

    @Test
    public void testRemove() {
        defaultSet.add("a");
        defaultSet.add("b");

        assertTrue(defaultSet.remove("a"));
        assertFalse(defaultSet.remove("a")); // already removed
        assertFalse(defaultSet.remove("c")); // never present

        assertEquals(1, defaultSet.size());
        assertTrue(defaultSet.contains("b"));
    }

    @Test
    public void testClear() {
        defaultSet.addAll(Arrays.asList("x", "y", "z"));
        assertFalse(defaultSet.isEmpty());

        defaultSet.clear();
        assertTrue(defaultSet.isEmpty());
        assertEquals(0, defaultSet.size());
    }

    /*--------------------------------------------------------------
     * contains / containsAll
     *--------------------------------------------------------------*/
    @Test
    public void testContainsAndContainsAll() {
        defaultSet.addAll(Arrays.asList("alpha", "beta"));
        assertTrue(defaultSet.contains("alpha"));
        assertFalse(defaultSet.contains("gamma"));

        Collection<String> subset = Arrays.asList("alpha");
        Collection<String> superset = Arrays.asList("alpha", "beta", "gamma");

        assertTrue(defaultSet.containsAll(subset));
        assertFalse(defaultSet.containsAll(superset));
    }

    /*--------------------------------------------------------------
     * addAll, retainAll, removeAll
     *--------------------------------------------------------------*/
    @Test
    public void testAddAllRetainAllRemoveAll() {
        List<String> list = Arrays.asList("a", "b", "c", "d");
        assertTrue(defaultSet.addAll(list));
        assertEquals(4, defaultSet.size());

        // retain only a and c
        assertTrue(defaultSet.retainAll(Arrays.asList("a", "c", "x")));
        assertEquals(2, defaultSet.size());
        assertTrue(defaultSet.containsAll(Arrays.asList("a", "c")));

        // remove a
        assertTrue(defaultSet.removeAll(Collections.singleton("a")));
        assertEquals(1, defaultSet.size());
        assertFalse(defaultSet.contains("a"));
        assertTrue(defaultSet.contains("c"));

        // removeAll with no matching elements returns false
        assertFalse(defaultSet.removeAll(Collections.singleton("nonexistent")));
    }

    /*--------------------------------------------------------------
     * Iterator and ordering (sorted, insertion, reverse, unordered)
     *--------------------------------------------------------------*/
    @Test
    public void testIteratorSortedOrder() {
        CompactSet<String> sorted = CompactSet.<String>builder()
                .sortedOrder()
                .build();

        sorted.addAll(Arrays.asList("banana", "apple", "cherry"));
        Iterator<String> it = sorted.iterator();

        List<String> iterated = new ArrayList<>();
        while (it.hasNext()) {
            iterated.add(it.next());
        }

        assertEquals(Arrays.asList("apple", "banana", "cherry"), iterated);
    }

    @Test
    public void testIteratorInsertionOrder() {
        CompactSet<String> insertion = CompactSet.<String>builder()
                .insertionOrder()
                .build();

        insertion.addAll(Arrays.asList("first", "second", "third"));
        assertEquals(Arrays.asList("first", "second", "third"),
                new ArrayList<>(insertion));
    }

    @Test
    public void testIteratorReverseOrder() {
        CompactSet<String> reverse = CompactSet.<String>builder()
                .reverseOrder()
                .build();

        reverse.addAll(Arrays.asList("a", "b", "c"));
        assertEquals(Arrays.asList("c", "b", "a"),
                new ArrayList<>(reverse));
    }

    @Test
    public void testIteratorUnordered() {
        CompactSet<String> unordered = CompactSet.<String>builder()
                .noOrder()
                .build();

        unordered.addAll(Arrays.asList("x", "y", "z"));
        // Unordered – we only verify that all elements are present, order not asserted
        Set<String> expected = new HashSet<>(Arrays.asList("x", "y", "z"));
        assertEquals(expected, new HashSet<>(unordered));
    }

    /*--------------------------------------------------------------
     * toArray methods
     *--------------------------------------------------------------*/
    @Test
    public void testToArray() {
        defaultSet.addAll(Arrays.asList("one", "two"));
        Object[] arr = defaultSet.toArray();
        assertEquals(2, arr.length);
        assertTrue(Arrays.asList("one", "two").containsAll(Arrays.asList(arr)));

        String[] a = new String[0];
        String[] typed = defaultSet.toArray(a);
        assertEquals(2, typed.length);
        assertTrue(Arrays.asList("one", "two").containsAll(Arrays.asList(typed)));
    }

    /*--------------------------------------------------------------
     * equals, hashCode, toString
     *--------------------------------------------------------------*/
    @Test
    public void testEqualsAndHashCode() {
        CompactSet<String> set1 = CompactSet.<String>builder().build();
        CompactSet<String> set2 = CompactSet.<String>builder().build();

        set1.addAll(Arrays.asList("x", "y"));
        set2.addAll(Arrays.asList("y", "x"));

        assertEquals(set1, set2);
        assertEquals(set1.hashCode(), set2.hashCode());

        set2.add("z");
        assertNotEquals(set1, set2);
    }

    @Test
    public void testToString() {
        defaultSet.addAll(Arrays.asList("a", "b"));
        String s = defaultSet.toString();
        assertTrue(s.startsWith("["));
        assertTrue(s.endsWith("]"));
        assertTrue(s.contains("a"));
        assertTrue(s.contains("b"));
    }

    /*--------------------------------------------------------------
     * isDefaultCompactSet
     *--------------------------------------------------------------*/
    @Test
    public void testIsDefaultCompactSet() {
        CompactSet<String> set = new CompactSet<>();
        assertTrue(set.isDefaultCompactSet());
    }

    /*--------------------------------------------------------------
     * Builder configuration and getConfig()
     *--------------------------------------------------------------*/
    @Test
    public void testBuilderConfigurationAndGetConfig() {
        CompactSet<String> custom = CompactSet.<String>builder()
                .caseSensitive(false)
                .compactSize(20)
                .insertionOrder()
                .build();

        Map<String, Object> cfg = custom.getConfig();

        assertEquals(20, cfg.get(CompactMap.COMPACT_SIZE));
        assertEquals(Boolean.FALSE, cfg.get(CompactMap.CASE_SENSITIVE));
        assertEquals(CompactMap.INSERTION, cfg.get(CompactMap.ORDERING));
    }

    /*--------------------------------------------------------------
     * withConfig – merging configurations
     *--------------------------------------------------------------*/
    @Test
    public void testWithConfigOverridesAndPreservesElements() {
        CompactSet<String> base = CompactSet.<String>builder()
                .caseSensitive(true)
                .compactSize(10)
                .insertionOrder()
                .build();

        base.addAll(Arrays.asList("a", "B", "c"));

        Map<String, Object> newCfg = new HashMap<>();
        newCfg.put(CompactMap.CASE_SENSITIVE, Boolean.FALSE); // make case‑insensitive
        newCfg.put(CompactMap.ORDERING, CompactMap.SORTED);   // change ordering

        CompactSet<String> derived = base.withConfig(newCfg);

        // Elements must be preserved
        assertEquals(3, derived.size());
        assertTrue(derived.contains("a"));
        assertTrue(derived.contains("B"));
        assertTrue(derived.contains("c"));

        // Case‑insensitive check
        assertTrue(derived.contains("b")); // original had "B"

        // Ordering should now be sorted (natural order)
        List<String> expectedOrder = Arrays.asList("B", "a", "c"); // natural order ignoring case
        // Since case‑insensitive, "B" and "b" compare equal; we just verify sorted order of distinct values
        List<String> actual = new ArrayList<>(derived);
        Collections.sort(actual, String.CASE_INSENSITIVE_ORDER);
        assertEquals(expectedOrder, actual);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithConfigNullThrows() {
        CompactSet<String> set = new CompactSet<>();
        set.withConfig(null);
    }

    /*--------------------------------------------------------------
     * Constructor that accepts a Collection
     *--------------------------------------------------------------*/
    @Test
    public void testCollectionConstructor() {
        List<String> src = Arrays.asList("x", "y", "z");
        CompactSet<String> fromCollection = new CompactSet<>(src);
        assertEquals(3, fromCollection.size());
        assertTrue(fromCollection.containsAll(src));
    }

    /*--------------------------------------------------------------
     * Case‑sensitivity behavior
     *--------------------------------------------------------------*/
    @Test
    public void testCaseInsensitiveBehavior() {
        CompactSet<String> ciSet = CompactSet.<String>builder()
                .caseSensitive(false)
                .build();

        ciSet.add("Foo");
        assertTrue(ciSet.contains("foo"));
        assertTrue(ciSet.contains("FOO"));

        // Adding a duplicate in different case should not change size
        assertFalse(ciSet.add("FOO"));
        assertEquals(1, ciSet.size());
    }
}
