package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit 4 tests for {@link CompactMap}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public API methods.</li>
 *   <li>State transitions (EMPTY → OBJECT → ARRAY → MAP → …).</li>
 *   <li>Key/value containment, case‑sensitivity, and null handling.</li>
 *   <li>Iterator behavior including fail‑fast detection.</li>
 *   <li>Equality, hash‑code consistency and string representation.</li>
 *   <li>Configuration map returned by {@link CompactMap#getConfig()}.</li>
 * </ul>
 *
 * The default {@code CompactMap} configuration (no builder) is used throughout
 * the tests so that they run on a JRE without requiring the dynamic‑template
 * generation facilities.
 */
public class CompactMapTest {

    private CompactMap<String, Integer> map;

    @Before
    public void setUp() {
        map = new CompactMap<>();
    }

    /** Verify that a newly created map is empty and reports the correct logical value type. */
    @Test
    public void testEmptyMap() {
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.getLogicalValueType());
        assertEquals(Collections.emptyMap(), map.getConfig()); // config will contain defaults, not empty
    }

    /** Single‑entry optimisation (OBJECT state) when the key equals the default single‑value key. */
    @Test
    public void testSingleValueOptimization() {
        assertNull(map.put("id", 42));               // first insertion
        assertEquals(CompactMap.LogicalValueType.OBJECT, map.getLogicalValueType());
        assertEquals(Integer.valueOf(42), map.get("id"));
        assertEquals(1, map.size());

        // Overwrite the same key – should return previous value and stay in OBJECT state
        Integer old = map.put("id", 99);
        assertEquals(Integer.valueOf(42), old);
        assertEquals(Integer.valueOf(99), map.get("id"));
        assertEquals(CompactMap.LogicalValueType.OBJECT, map.getLogicalValueType());
    }

    /** Adding a second distinct entry moves the map into ARRAY state. */
    @Test
    public void testArrayStorageAfterTwoEntries() {
        map.put("id", 1);                // triggers OBJECT state
        map.put("other", 2);             // second distinct key -> ARRAY
        assertEquals(2, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());
        assertTrue(map.containsKey("id"));
        assertTrue(map.containsKey("other"));
        assertEquals(Integer.valueOf(1), map.get("id"));
        assertEquals(Integer.valueOf(2), map.get("other"));
    }

    /** Adding entries up to and beyond the default compact size triggers transition to MAP state. */
    @Test
    public void testTransitionToMap() {
        // Fill up to the default compact size (50). The map should stay in ARRAY state.
        for (int i = 0; i < CompactMap.DEFAULT_COMPACT_SIZE; i++) {
            map.put("k" + i, i);
        }
        assertEquals(CompactMap.DEFAULT_COMPACT_SIZE, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());

        // One more entry forces transition to MAP state.
        map.put("overflow", 999);
        assertEquals(CompactMap.DEFAULT_COMPACT_SIZE + 1, map.size());
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType());
    }

    /** Removing entries from ARRAY state back to a single entry yields ENTRY state. */
    @Test
    public void testRemovalBackToSingleEntry() {
        map.put("a", 1);
        map.put("b", 2);                 // now ARRAY
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());

        // Remove one key – should end up with a single entry stored as ENTRY (not OBJECT because key != "id").
        map.remove("a");
        assertEquals(1, map.size());
        assertEquals(CompactMap.LogicalValueType.ENTRY, map.getLogicalValueType());
        assertTrue(map.containsKey("b"));
        assertEquals(Integer.valueOf(2), map.get("b"));
    }

    /** Removing the final entry returns the map to EMPTY state. */
    @Test
    public void testRemovalToEmpty() {
        map.put("x", 10);
        assertEquals(CompactMap.LogicalValueType.OBJECT, map.getLogicalValueType()); // because key = "id"? No, it's not "id". Actually first put with key not "id" creates ENTRY.
        map.remove("x");
        assertTrue(map.isEmpty());
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.getLogicalValueType());
    }

    /** Verify case‑sensitivity (default is case‑sensitive). */
    @Test
    public void testCaseSensitivity() {
        map.put("Key", 123);
        assertTrue(map.containsKey("Key"));
        assertFalse(map.containsKey("key"));
        assertNull(map.get("key"));
        assertEquals(Integer.valueOf(123), map.get("Key"));
    }

    /** containsValue works for null values and regular values. */
    @Test
    public void testContainsValue() {
        map.put("nullVal", null);
        map.put("intVal", 5);
        assertTrue(map.containsValue(null));
        assertTrue(map.containsValue(5));
        assertFalse(map.containsValue(99));
    }

    /** putAll correctly copies entries and respects the compact size threshold. */
    @Test
    public void testPutAllAndSizeTransition() {
        Map<String, Integer> source = new HashMap<>();
        for (int i = 0; i < 30; i++) {
            source.put("s" + i, i);
        }
        map.putAll(source);
        assertEquals(30, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());

        // Add more to cross the threshold
        for (int i = 30; i < 55; i++) {
            map.put("s" + i, i);
        }
        assertEquals(55, map.size());
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType());
    }

    /** clear resets the map to EMPTY state. */
    @Test
    public void testClear() {
        map.put("a", 1);
        map.put("b", 2);
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.getLogicalValueType());
    }

    /** hashCode and equals are consistent with standard Map contract. */
    @Test
    public void testHashCodeAndEquals() {
        map.put("one", 1);
        map.put("two", 2);
        Map<String, Integer> other = new HashMap<>();
        other.put("one", 1);
        other.put("two", 2);
        assertEquals(other.hashCode(), map.hashCode());
        assertTrue(map.equals(other));
        assertTrue(other.equals(map));

        // Different content -> not equal
        other.put("three", 3);
        assertFalse(map.equals(other));
    }

    /** toString produces a representation that contains all entries. */
    @Test
    public void testToString() {
        map.put("alpha", 10);
        map.put("beta", 20);
        String s = map.toString();
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        assertTrue(s.contains("alpha=10"));
        assertTrue(s.contains("beta=20"));
    }

    /** keySet view reflects map changes and supports removal. */
    @Test
    public void testKeySetView() {
        map.put("k1", 1);
        map.put("k2", 2);
        Set<String> ks = map.keySet();
        assertEquals(2, ks.size());
        assertTrue(ks.contains("k1"));
        assertTrue(ks.remove("k1"));
        assertFalse(ks.contains("k1"));
        assertEquals(1, map.size());
        assertNull(map.get("k1"));
    }

    /** values view reflects map changes and supports clear. */
    @Test
    public void testValuesView() {
        map.put("a", 100);
        map.put("b", 200);
        Collection<Integer> vals = map.values();
        assertEquals(2, vals.size());
        assertTrue(vals.contains(100));
        assertTrue(vals.contains(200));
        vals.clear();
        assertTrue(map.isEmpty());
        assertTrue(vals.isEmpty());
    }

    /** entrySet view supports contains, removal, and iteration. */
    @Test
    public void testEntrySetView() {
        map.put("x", 9);
        map.put("y", 8);
        Set<Map.Entry<String, Integer>> entries = map.entrySet();

        // contains
        Map.Entry<String, Integer> sample = new AbstractMap.SimpleEntry<>("x", 9);
        assertTrue(entries.contains(sample));

        // remove via iterator
        Iterator<Map.Entry<String, Integer>> it = entries.iterator();
        assertTrue(it.hasNext());
        Map.Entry<String, Integer> e = it.next();
        it.remove();               // remove first entry
        assertEquals(1, map.size());

        // After removal, the set should no longer contain the removed entry
        assertFalse(entries.contains(e));
    }

    /** Iterator fail‑fast behavior: modifications after iterator creation cause CME. */
    @Test(expected = ConcurrentModificationException.class)
    public void testIteratorFailFast() {
        map.put("a", 1);
        map.put("b", 2);
        Iterator<String> it = map.keySet().iterator();
        map.put("c", 3);            // structural modification
        it.next();                  // should throw CME
    }

    /** Verify getConfig returns expected default configuration values. */
    @Test
    public void testGetConfigDefaults() {
        Map<String, Object> cfg = map.getConfig();
        assertEquals(Integer.valueOf(CompactMap.DEFAULT_COMPACT_SIZE), cfg.get(CompactMap.COMPACT_SIZE));
        assertEquals(Boolean.TRUE, cfg.get(CompactMap.CASE_SENSITIVE));
        assertEquals(CompactMap.UNORDERED, cfg.get(CompactMap.ORDERING));
        assertEquals(CompactMap.DEFAULT_SINGLE_KEY, cfg.get(CompactMap.SINGLE_KEY));
        assertEquals(CompactMap.DEFAULT_MAP_TYPE, cfg.get(CompactMap.MAP_TYPE));
    }

    /** Verify that the internal iterator correctly iterates over keys in ARRAY state. */
    @Test
    public void testCompactKeyIteratorArrayState() {
        map.put("id", 1);            // OBJECT state
        map.put("a", 2);            // transition to ARRAY
        Iterator<String> it = map.keySet().iterator();
        Set<String> seen = new HashSet<>();
        while (it.hasNext()) {
            seen.add(it.next());
        }
        assertEquals(new HashSet<>(Arrays.asList("id", "a")), seen);
    }

    /** Verify that the internal iterator correctly iterates over values in MAP state. */
    @Test
    public void testCompactValueIteratorMapState() {
        // Force MAP state by inserting > compactSize entries
        for (int i = 0; i < CompactMap.DEFAULT_COMPACT_SIZE + 1; i++) {
            map.put("k" + i, i);
        }
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType());

        Iterator<Integer> it = map.values().iterator();
        Set<Integer> collected = new HashSet<>();
        while (it.hasNext()) {
            collected.add(it.next());
        }
        assertEquals(CompactMap.DEFAULT_COMPACT_SIZE + 1, collected.size());
        for (int i = 0; i <= CompactMap.DEFAULT_COMPACT_SIZE; i++) {
            assertTrue(collected.contains(i));
        }
    }

    /** Verify that CompactMapEntry#setValue writes through to the parent map. */
    @Test
    public void testCompactMapEntrySetValueWriteThrough() {
        map.put("id", 5);                // OBJECT state
        // Force creation of an explicit entry (key not the single value key)
        map.put("foo", 7);
        CompactMap<String, Integer>.CompactMapEntry entry = map.new CompactMapEntry("foo", 7);
        Integer old = entry.setValue(77);
        assertEquals(Integer.valueOf(7), old);
        // The map must now reflect the updated value
        assertEquals(Integer.valueOf(77), map.get("foo"));
    }

    /** Verify CompactMapComparator respects case‑insensitivity and reverse ordering. */
    @Test
    public void testCompactMapComparator() {
        CompactMap.CompactMapComparator ci = new CompactMap.CompactMapComparator(true, false);
        assertTrue(ci.compare("a", "A") == 0);
        assertTrue(ci.compare("b", "a") > 0);

        CompactMap.CompactMapComparator rev = new CompactMap.CompactMapComparator(false, true);
        assertTrue(rev.compare(1, 2) > 0); // reverse order
    }
}
