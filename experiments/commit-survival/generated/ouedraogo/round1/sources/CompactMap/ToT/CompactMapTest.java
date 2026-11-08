package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.ConcurrentModificationException;

/**
 * Comprehensive JUnit‑4 test suite for {@link CompactMap}.
 *
 * The tests use a small {@code compactSize} (3) so that the internal
 * representation (EMPTY → SINGLE → ARRAY → MAP) can be exercised quickly.
 * A protected subclass {@code TestCompactMap} is used to expose internal
 * helpers such as {@code getLogicalValueType()} and to force the use of the
 * copy‑iterator path.
 */
public class CompactMapTest
{
    /** Subclass that makes the internal thresholds easy to test. */
    private static class TestCompactMap<K, V> extends CompactMap<K, V>
    {
        private final int compactSize;

        TestCompactMap(int compactSize)
        {
            this.compactSize = compactSize;
        }

        @Override
        protected int compactSize()
        {
            return compactSize;
        }

        /** Expose the protected method for assertions. */
        LogicalValueType logicalValueType()
        {
            return super.getLogicalValueType();
        }

        /** Force copy‑iterator usage (simulates a SortedMap underlying map). */
        @Override
        protected boolean useCopyIterator()
        {
            // Return true only when the underlying new map would be a SortedMap.
            // We can cheat by returning true directly for the test.
            return true;
        }
    }

    private TestCompactMap<String, Integer> map;

    @Before
    public void setUp()
    {
        // compactSize = 3 → triggers ARRAY at size 2‑3 and MAP at size 4
        map = new TestCompactMap<>(3);
    }

    /* --------------------------------------------------------------------- *
     *  Basic empty‑map behavior
     * --------------------------------------------------------------------- */
    @Test
    public void testEmptyMap()
    {
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
        assertFalse(map.containsKey("any"));
        assertFalse(map.containsValue(42));
        assertNull(map.get("any"));
        assertEquals("{}", map.toString());
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.logicalValueType());
    }

    /* --------------------------------------------------------------------- *
     *  Single entry where key equals the logical single‑value key ("key")
     * --------------------------------------------------------------------- */
    @Test
    public void testSingleEntryWithDefaultKey()
    {
        assertNull(map.put("key", 100));               // first insertion
        assertEquals(1, map.size());
        assertFalse(map.isEmpty());
        assertTrue(map.containsKey("key"));
        assertTrue(map.containsValue(100));
        assertEquals(Integer.valueOf(100), map.get("key"));
        assertEquals(CompactMap.LogicalValueType.OBJECT, map.logicalValueType());

        // Overwrite same key – should return previous value
        Integer prev = map.put("key", 200);
        assertEquals(Integer.valueOf(100), prev);
        assertEquals(Integer.valueOf(200), map.get("key"));
        assertEquals(1, map.size());
    }

    /* --------------------------------------------------------------------- *
     *  Single entry with a different key – stored as CompactMapEntry
     * --------------------------------------------------------------------- */
    @Test
    public void testSingleEntryWithCustomKey()
    {
        assertNull(map.put("alpha", 1));
        assertEquals(1, map.size());
        assertEquals(CompactMap.LogicalValueType.ENTRY, map.logicalValueType());

        assertTrue(map.containsKey("alpha"));
        assertFalse(map.containsKey("key"));
        assertTrue(map.containsValue(1));
        assertEquals(Integer.valueOf(1), map.get("alpha"));
    }

    /* --------------------------------------------------------------------- *
     *  Transition from SINGLE → ARRAY (size 2) and ARRAY → MAP (size 4)
     * --------------------------------------------------------------------- */
    @Test
    public void testArrayAndMapTransitions()
    {
        // Fill to size 2 → ARRAY representation
        map.put("a", 10);
        map.put("b", 20);
        assertEquals(2, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.logicalValueType());

        // Verify iteration order (insertion order is preserved in ARRAY)
        Iterator<String> keyIter = map.keySet().iterator();
        assertTrue(keyIter.hasNext());
        assertEquals("a", keyIter.next());
        assertTrue(keyIter.hasNext());
        assertEquals("b", keyIter.next());
        assertFalse(keyIter.hasNext());

        // Add third entry – still ARRAY (compactSize = 3)
        map.put("c", 30);
        assertEquals(3, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.logicalValueType());

        // Add fourth entry – should transition to MAP
        map.put("d", 40);
        assertEquals(4, map.size());
        assertEquals(CompactMap.LogicalValueType.MAP, map.logicalValueType());

        // Verify that all entries are present
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsKey("d"));
        assertEquals(Integer.valueOf(40), map.get("d"));
    }

    /* --------------------------------------------------------------------- *
     *  putAll – small map stays ARRAY, large map switches directly to MAP
     * --------------------------------------------------------------------- */
    @Test
    public void testPutAllTransitions()
    {
        // Small map (size 2) → ARRAY
        Map<String,Integer> small = new HashMap<>();
        small.put("x",1);
        small.put("y",2);
        map.putAll(small);
        assertEquals(2, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.logicalValueType());

        // Large map (size 5) → MAP directly (size > compactSize)
        Map<String,Integer> large = new LinkedHashMap<>();
        large.put("p",10);
        large.put("q",20);
        large.put("r",30);
        large.put("s",40);
        large.put("t",50);
        map.clear();
        map.putAll(large);
        assertEquals(5, map.size());
        assertEquals(CompactMap.LogicalValueType.MAP, map.logicalValueType());
        assertTrue(map.containsKey("r"));
        assertEquals(Integer.valueOf(30), map.get("r"));
    }

    /* --------------------------------------------------------------------- *
     *  Remove – shrinking ARRAY back to SINGLE and EMPTY
     * --------------------------------------------------------------------- */
    @Test
    public void testRemoveShrinksArray()
    {
        map.put("k1", 1);
        map.put("k2", 2); // now ARRAY (size 2)
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.logicalValueType());

        // Remove one key – should shrink to SINGLE (CompactMapEntry)
        Integer removed = map.remove("k1");
        assertEquals(Integer.valueOf(1), removed);
        assertEquals(1, map.size());
        assertEquals(CompactMap.LogicalValueType.ENTRY, map.logicalValueType());

        // Remove last key – back to EMPTY
        removed = map.remove("k2");
        assertEquals(Integer.valueOf(2), removed);
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.logicalValueType());
    }

    /* --------------------------------------------------------------------- *
     *  Remove from MAP representation and shrink back to ARRAY
     * --------------------------------------------------------------------- */
    @Test
    public void testRemoveFromMapShrinks()
    {
        // Build a MAP (size 4)
        map.put("a",1);
        map.put("b",2);
        map.put("c",3);
        map.put("d",4); // triggers MAP
        assertEquals(CompactMap.LogicalValueType.MAP, map.logicalValueType());

        // Remove one entry – size becomes 3 (compactSize) → should convert back to ARRAY
        map.remove("d");
        assertEquals(3, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.logicalValueType());

        // Verify remaining entries
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsKey("c"));
        assertFalse(map.containsKey("d"));
    }

    /* --------------------------------------------------------------------- *
     *  equals() and hashCode() consistency
     * --------------------------------------------------------------------- */
    @Test
    public void testEqualsAndHashCode()
    {
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3); // ARRAY representation

        Map<String,Integer> other = new HashMap<>();
        other.put("one", 1);
        other.put("two", 2);
        other.put("three", 3);

        assertTrue(map.equals(other));
        assertEquals(map.hashCode(), other.hashCode());

        // Change a value – equality should break
        map.put("two", 99);
        assertFalse(map.equals(other));
        assertNotEquals(map.hashCode(), other.hashCode());
    }

    /* --------------------------------------------------------------------- *
     *  toString format
     * --------------------------------------------------------------------- */
    @Test
    public void testToString()
    {
        assertEquals("{}", map.toString());

        map.put("x", 10);
        assertEquals("{x=10}", map.toString());

        map.put("y", 20);
        // Order is insertion order for ARRAY representation
        assertEquals("{x=10, y=20}", map.toString());

        // After conversion to MAP (size 4) order is undefined – check contains
        map.put("z", 30);
        map.put("w", 40);
        String s = map.toString();
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        assertTrue(s.contains("x=10"));
        assertTrue(s.contains("y=20"));
        assertTrue(s.contains("z=30"));
        assertTrue(s.contains("w=40"));
    }

    /* --------------------------------------------------------------------- *
     *  keySet, values, entrySet basic operations
     * --------------------------------------------------------------------- */
    @Test
    public void testCollectionViews()
    {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3); // ARRAY

        // keySet
        Set<String> keys = map.keySet();
        assertEquals(3, keys.size());
        assertTrue(keys.containsAll(Arrays.asList("a","b","c")));

        // values
        Collection<Integer> vals = map.values();
        assertEquals(3, vals.size());
        assertTrue(vals.containsAll(Arrays.asList(1,2,3)));

        // entrySet
        Set<Map.Entry<String,Integer>> entries = map.entrySet();
        assertEquals(3, entries.size());
        for (Map.Entry<String,Integer> e : entries)
        {
            assertEquals(Integer.valueOf(e.getKey().charAt(0) - 'a' + 1), e.getValue());
        }
    }

    /* --------------------------------------------------------------------- *
     *  entrySet.remove and retainAll
     * --------------------------------------------------------------------- */
    @Test
    public void testEntrySetRemoveAndRetain()
    {
        map.put("k1", 10);
        map.put("k2", 20);
        map.put("k3", 30); // ARRAY

        // Remove via entrySet
        Map.Entry<String,Integer> toRemove = new AbstractMap.SimpleEntry<>("k2", 20);
        assertTrue(map.entrySet().remove(toRemove));
        assertFalse(map.containsKey("k2"));
        assertEquals(2, map.size());

        // retainAll – keep only k3
        Set<Map.Entry<String,Integer>> retain = new HashSet<>();
        retain.add(new AbstractMap.SimpleEntry<>("k3", 30));
        retain.add(new AbstractMap.SimpleEntry<>("nonexistent", 0));
        assertTrue(map.entrySet().retainAll(retain));
        assertEquals(1, map.size());
        assertTrue(map.containsKey("k3"));
    }

    /* --------------------------------------------------------------------- *
     *  Iterator remove and fail‑fast behavior (CompactIterator)
     * --------------------------------------------------------------------- */
    @Test(expected = ConcurrentModificationException.class)
    public void testIteratorFailFast()
    {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3); // ARRAY

        Iterator<String> it = map.keySet().iterator();
        assertTrue(it.hasNext());
        it.next(); // advance to first element

        // Structural modification outside iterator should trigger CME on next()
        map.put("d", 4); // forces MAP conversion
        it.next(); // should throw ConcurrentModificationException
    }

    @Test
    public void testIteratorRemove()
    {
        map.put("x", 100);
        map.put("y", 200);
        map.put("z", 300); // ARRAY

        Iterator<String> it = map.keySet().iterator();
        while (it.hasNext())
        {
            String k = it.next();
            if ("y".equals(k))
            {
                it.remove(); // remove via iterator
            }
        }
        assertEquals(2, map.size());
        assertFalse(map.containsKey("y"));
        assertTrue(map.containsKey("x"));
        assertTrue(map.containsKey("z"));
    }

    /* --------------------------------------------------------------------- *
     *  Copy iterators (useCopyIterator forced true)
     * --------------------------------------------------------------------- */
    @Test
    public void testCopyIterators()
    {
        // Populate map (will stay ARRAY because we forced copy iterator)
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);

        // Key iterator (copy)
        Iterator<String> keyIter = map.keySet().iterator();
        List<String> collectedKeys = new ArrayList<>();
        while (keyIter.hasNext())
        {
            collectedKeys.add(keyIter.next());
        }
        assertEquals(Arrays.asList("one","two","three"), collectedKeys);

        // Value iterator (copy)
        Iterator<Integer> valIter = map.values().iterator();
        List<Integer> collectedVals = new ArrayList<>();
        while (valIter.hasNext())
        {
            collectedVals.add(valIter.next());
        }
        assertEquals(Arrays.asList(1,2,3), collectedVals);

        // Entry iterator (copy) and test remove via iterator
        Iterator<Map.Entry<String,Integer>> entryIter = map.entrySet().iterator();
        while (entryIter.hasNext())
        {
            Map.Entry<String,Integer> e = entryIter.next();
            if (e.getKey().equals("two"))
            {
                entryIter.remove();
            }
        }
        assertEquals(2, map.size());
        assertFalse(map.containsKey("two"));
    }

    /* --------------------------------------------------------------------- *
     *  CompactMapEntry.setValue writes through to the parent map
     * --------------------------------------------------------------------- */
    @Test
    public void testCompactMapEntrySetValue()
    {
        map.put("alpha", 5); // SINGLE as CompactMapEntry
        assertEquals(CompactMap.LogicalValueType.ENTRY, map.logicalValueType());

        // Retrieve the entry via entrySet().iterator().next()
        Map.Entry<String,Integer> entry = map.entrySet().iterator().next();
        assertTrue(entry instanceof CompactMap.CompactMapEntry);
        CompactMap.CompactMapEntry cmEntry = (CompactMap.CompactMapEntry) entry;

        // Change value through entry
        Integer old = cmEntry.setValue(99);
        assertEquals(Integer.valueOf(5), old);
        assertEquals(Integer.valueOf(99), map.get("alpha"));
    }

    /* --------------------------------------------------------------------- *
     *  Verify computeKeyHashCode / computeValueHashCode via hashCode()
     * --------------------------------------------------------------------- */
    @Test
    public void testHashCodeWithSpecialValues()
    {
        // Null key/value handling
        map.put(null, null);
        map.put("key", null);
        map.put(null, 123); // overwrite null key's value

        // Build a reference HashMap with same entries
        Map<String,Integer> ref = new HashMap<>();
        ref.put(null, 123);
        ref.put("key", null);

        assertEquals(ref.hashCode(), map.hashCode());
        assertTrue(map.equals(ref));
    }

    /* --------------------------------------------------------------------- *
     *  Verify that clear() resets the internal state correctly
     * --------------------------------------------------------------------- */
    @Test
    public void testClearResetsState()
    {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3); // ARRAY
        map.clear();

        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.logicalValueType());
        assertEquals("{}", map.toString());
    }
}
