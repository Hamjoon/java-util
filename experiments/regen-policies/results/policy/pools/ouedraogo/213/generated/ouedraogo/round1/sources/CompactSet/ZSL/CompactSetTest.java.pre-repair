package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import java.util.*;

import static org.junit.Assert.*;

public class CompactSetTest {

    private CompactSet<String> defaultSet;

    @Before
    public void setUp() {
        // The plain no‑arg constructor should give us the “default” configuration
        defaultSet = new CompactSet<>();
    }

    /* -------------------------------------------------------------
     *  Basic Set contract (add, remove, contains, size, isEmpty)
     * ------------------------------------------------------------- */
    @Test
    public void testAddRemoveContainsSizeIsEmpty() {
        CompactSet<Integer> set = new CompactSet<>();

        assertTrue(set.isEmpty());
        assertEquals(0, set.size());

        // add first element – should return true
        assertTrue(set.add(10));
        assertTrue(set.contains(10));
        assertEquals(1, set.size());
        assertFalse(set.isEmpty());

        // add duplicate – should return false and not increase size
        assertFalse(set.add(10));
        assertEquals(1, set.size());

        // remove existing element – should return true
        assertTrue(set.remove(10));
        assertFalse(set.contains(10));
        assertEquals(0, set.size());

        // remove non‑existing element – should return false
        assertFalse(set.remove(99));
    }

    @Test
    public void testAddAllContainsAllAndClear() {
        List<String> list = Arrays.asList("a", "b", "c");
        CompactSet<String> set = new CompactSet<>(list);

        assertEquals(3, set.size());
        assertTrue(set.containsAll(list));

        // addAll with some duplicates
        List<String> extra = Arrays.asList("c", "d");
        assertTrue(set.addAll(extra));   // 'd' is new, so modification should be true
        assertEquals(4, set.size());
        assertTrue(set.contains("d"));

        // clear the set
        set.clear();
        assertTrue(set.isEmpty());
    }

    @Test
    public void testRetainAllAndRemoveAll() {
        CompactSet<String> set = new CompactSet<>(Arrays.asList("x", "y", "z"));
        Collection<String> retain = Arrays.asList("x", "y");

        assertTrue(set.retainAll(retain));
        assertEquals(2, set.size());
        assertFalse(set.contains("z"));

        // removeAll with an element that is present and one that is not
        assertTrue(set.removeAll(Arrays.asList("x", "notThere")));
        assertEquals(1, set.size());
        assertFalse(set.contains("x"));
        assertTrue(set.contains("y"));
    }

    /* -------------------------------------------------------------
     *  Iterator and array conversion
     * ------------------------------------------------------------- */
    @Test
    public void testIteratorOrderInsertion() {
        CompactSet<String> set = CompactSet.<String>builder()
                .insertionOrder()
                .build();

        set.addAll(Arrays.asList("first", "second", "third"));
        Iterator<String> it = set.iterator();

        assertEquals("first", it.next());
        assertEquals("second", it.next());
        assertEquals("third", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void testIteratorOrderSorted() {
        CompactSet<Integer> set = CompactSet.<Integer>builder()
                .sortedOrder()
                .build();

        set.addAll(Arrays.asList(30, 10, 20));
        Iterator<Integer> it = set.iterator();

        assertEquals(Integer.valueOf(10), it.next());
        assertEquals(Integer.valueOf(20), it.next());
        assertEquals(Integer.valueOf(30), it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void testIteratorOrderReverse() {
        CompactSet<Integer> set = CompactSet.<Integer>builder()
                .reverseOrder()
                .build();

        set.addAll(Arrays.asList(1, 2, 3));
        Iterator<Integer> it = set.iterator();

        assertEquals(Integer.valueOf(3), it.next());
        assertEquals(Integer.valueOf(2), it.next());
        assertEquals(Integer.valueOf(1), it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void testToArrayVariants() {
        CompactSet<String> set = new CompactSet<>(Arrays.asList("one", "two"));
        Object[] arr = set.toArray();
        assertArrayEquals(new Object[]{"one", "two"}, arr);

        String[] a = new String[0];
        String[] arr2 = set.toArray(a);
        assertArrayEquals(new String[]{"one", "two"}, arr2);

        // pre‑sized array larger than needed
        String[] big = new String[5];
        String[] arr3 = set.toArray(big);
        assertEquals("one", arr3[0]);
        assertEquals("two", arr3[1]);
        assertNull(arr3[2]); // remaining slots should be null per Set contract
    }

    /* -------------------------------------------------------------
     *  Equality, hashCode and toString
     * ------------------------------------------------------------- */
    @Test
    public void testEqualsHashCodeToString() {
        Set<String> hashSet = new HashSet<>(Arrays.asList("alpha", "beta"));
        CompactSet<String> compactSet = new CompactSet<>(hashSet);

        assertTrue(compactSet.equals(hashSet));
        assertTrue(hashSet.equals(compactSet));
        assertEquals(hashSet.hashCode(), compactSet.hashCode());

        String s = compactSet.toString();
        // The string representation should contain both elements (order not important)
        assertTrue(s.startsWith("["));
        assertTrue(s.contains("alpha"));
        assertTrue(s.contains("beta"));
        assertTrue(s.endsWith("]"));
    }

    /* -------------------------------------------------------------
     *  Case‑insensitive behaviour
     * ------------------------------------------------------------- */
    @Test
    public void testCaseInsensitive() {
        CompactSet<String> ciSet = CompactSet.<String>builder()
                .caseSensitive(false)
                .build();

        assertTrue(ciSet.add("Hello"));
        // duplicate with different case – should not be added
        assertFalse(ciSet.add("hello"));
        assertEquals(1, ciSet.size());

        // contains should be case‑insensitive
        assertTrue(ciSet.contains("HELLO"));
        assertTrue(ciSet.contains("HeLLo"));
    }

    /* -------------------------------------------------------------
     *  Configuration exposure (getConfig) and withConfig
     * ------------------------------------------------------------- */
    @Test
    public void testGetConfigMatchesBuilderSettings() {
        CompactSet<String> set = CompactSet.<String>builder()
                .caseSensitive(false)
                .compactSize(80)
                .sortedOrder()
                .build();

        Map<String, Object> cfg = set.getConfig();

        assertEquals(3, cfg.size());
        assertEquals(80, cfg.get(CompactMap.COMPACT_SIZE));
        assertEquals(Boolean.FALSE, cfg.get(CompactMap.CASE_SENSITIVE));
        assertEquals(CompactMap.SORTED, cfg.get(CompactMap.ORDERING));
    }

    @Test
    public void testWithConfigCreatesModifiedCopy() {
        // start with a default set containing three elements
        CompactSet<String> original = new CompactSet<>(Arrays.asList("a", "b", "c"));

        Map<String, Object> newCfg = new HashMap<>();
        newCfg.put(CompactMap.COMPACT_SIZE, 100);
        newCfg.put(CompactMap.CASE_SENSITIVE, Boolean.FALSE);
        newCfg.put(CompactMap.ORDERING, CompactMap.REVERSE);

        CompactSet<String> modified = original.withConfig(newCfg);
        Map<String, Object> cfg = modified.getConfig();

        assertEquals(100, cfg.get(CompactMap.COMPACT_SIZE));
        assertEquals(Boolean.FALSE, cfg.get(CompactMap.CASE_SENSITIVE));
        assertEquals(CompactMap.REVERSE, cfg.get(CompactMap.ORDERING));

        // Ensure all original elements were copied over
        assertTrue(modified.containsAll(Arrays.asList("a", "b", "c")));
        assertEquals(3, modified.size());

        // The original set must remain unchanged
        assertEquals(Boolean.TRUE, original.getConfig().get(CompactMap.CASE_SENSITIVE));
        assertEquals(CompactMap.UNORDERED, original.getConfig().get(CompactMap.ORDERING));
    }

    /* -------------------------------------------------------------
     *  isDefaultCompactSet validation
     * ------------------------------------------------------------- */
    @Test
    public void testIsDefaultCompactSetTrueForPlainConstructor() {
        // The no‑arg constructor should yield the default configuration
        CompactSet<Integer> set = new CompactSet<>();
        assertTrue(set.isDefaultCompactSet());
    }

    @Test
    public void testIsDefaultCompactSetFalseWhenChanged() {
        CompactSet<Integer> set = CompactSet.<Integer>builder()
                .compactSize(60)          // change compact size
                .caseSensitive(false)    // change case sensitivity
                .sortedOrder()           // change ordering
                .build();

        assertFalse(set.isDefaultCompactSet());
    }

    /* -------------------------------------------------------------
     *  Builder convenience – ensure the Builder builds a functional set
     * ------------------------------------------------------------- */
    @Test
    public void testBuilderCreatesFunctionalSet() {
        CompactSet<String> set = CompactSet.<String>builder()
                .caseSensitive(true)
                .compactSize(70)
                .insertionOrder()
                .build();

        assertTrue(set.add("x"));
        assertTrue(set.add("y"));
        assertFalse(set.add("x")); // duplicate

        // Verify ordering is insertion order
        Iterator<String> it = set.iterator();
        assertEquals("x", it.next());
        assertEquals("y", it.next());
        assertFalse(it.hasNext());

        // Verify configuration reflects builder choices
        Map<String, Object> cfg = set.getConfig();
        assertEquals(70, cfg.get(CompactMap.COMPACT_SIZE));
        assertEquals(Boolean.TRUE, cfg.get(CompactMap.CASE_SENSITIVE));
        assertEquals(CompactMap.INSERTION, cfg.get(CompactMap.ORDERING));
    }

    /* -------------------------------------------------------------
     *  Ensure that addAll returns correct modification flag
     * ------------------------------------------------------------- */
    @Test
    public void testAddAllModificationFlag() {
        CompactSet<Integer> set = new CompactSet<>(Arrays.asList(1, 2));
        // Adding a collection that only contains existing elements should return false
        assertFalse(set.addAll(Arrays.asList(1, 2)));

        // Adding at least one new element should return true
        assertTrue(set.addAll(Arrays.asList(2, 3, 4)));
        assertTrue(set.containsAll(Arrays.asList(1, 2, 3, 4)));
    }
}
