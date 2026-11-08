package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link CompactMap}.
 *
 * The tests are organized in the following sections:
 *
 * 1.  Public API extraction – a quick reference of all public constructors and methods.
 * 2.  Basic functionality tests – one simple test per method that verifies the normal behaviour.
 * 3.  Edge‑case & exception tests – covering empty maps, single‑entry special handling,
 *     transition from array‑storage to Map‑storage, null handling, iterator fail‑fast,
 *     and the unsupported {@code minus} / {@code plus} operations.
 * 4.  Helper subclass – a small concrete subclass that overrides {@code compactSize()}
 *     (so we can trigger the array‑to‑Map switch quickly) and optionally {@code isCaseInsensitive()}
 *     for case‑insensitive key tests.
 *
 * All tests are self‑contained and use only the public API of {@link CompactMap}
 * and the nested classes that are declared as public in the source file.
 */
public class CompactMapTest {

    /* ----------------------------------------------------------------------
     * 1. Public API reference (for readability – not used by the compiler)
     * ----------------------------------------------------------------------
     *
     * Constructors
     *   CompactMap()
     *   CompactMap(Map<K,V> other)
     *
     * Map interface methods
     *   int size()
     *   boolean isEmpty()
     *   boolean containsKey(Object key)
     *   boolean containsValue(Object value)
     *   V get(Object key)
     *   V put(K key, V value)
     *   V remove(Object key)
     *   void putAll(Map<? extends K,? extends V> m)
     *   void clear()
     *   Set<K> keySet()
     *   Collection<V> values()
     *   Set<Map.Entry<K,V>> entrySet()
     *
     * Object overrides
     *   int hashCode()
     *   boolean equals(Object o)
     *   String toString()
     *
     * Unsupported operations
     *   Map<K,V> minus(Object o)   // throws UnsupportedOperationException
     *   Map<K,V> plus(Object o)    // throws UnsupportedOperationException
     *
     * ---------------------------------------------------------------------- */

    /**
     * Helper subclass that makes the {@code compactSize()} very small (3) so we can
     * verify the internal transition from the Object[] representation to a regular Map.
     */
    private static class SmallCompactMap<K, V> extends CompactMap<K, V> {
        @Override
        protected int compactSize() {
            return 3;   // tiny threshold for testing
        }

        // keep the default case‑sensitivity (false) – can be overridden in a test if needed
    }

    private CompactMap<String, String> map;          // default map (compactSize = 80)
    private SmallCompactMap<Integer, Integer> smallMap; // map with tiny compactSize

    @Before
    public void setUp() {
        map = new CompactMap<>();
        smallMap = new SmallCompactMap<>();
    }

    /* ----------------------------------------------------------------------
     * 2. BASIC FUNCTIONALITY TESTS (one per public method)
     * ---------------------------------------------------------------------- */

    @Test
    public void testConstructorEmpty() {
        assertTrue("New map must be empty", map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testConstructorFromMap() {
        Map<String, String> source = new HashMap<>();
        source.put("a", "1");
        source.put("b", "2");
        CompactMap<String, String> fromMap = new CompactMap<>(source);
        assertEquals(2, fromMap.size());
        assertTrue(fromMap.containsKey("a"));
        assertTrue(fromMap.containsValue("2"));
    }

    @Test
    public void testSizeAndIsEmpty() {
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());

        map.put("x", "y");
        assertEquals(1, map.size());
        assertFalse(map.isEmpty());
    }

    @Test
    public void testContainsKeyAndContainsValue() {
        map.put("k1", "v1");
        map.put("k2", null);
        assertTrue(map.containsKey("k1"));
        assertTrue(map.containsKey("k2"));
        assertFalse(map.containsKey("missing"));

        assertTrue(map.containsValue("v1"));
        assertTrue(map.containsValue(null));
        assertFalse(map.containsValue("nope"));
    }

    @Test
    public void testGet() {
        map.put("alpha", "A");
        map.put("beta", "B");
        assertEquals("A", map.get("alpha"));
        assertNull(map.get("gamma")); // not present
        map.put("nullKey", null);
        assertNull(map.get("nullKey"));
    }

    @Test
    public void testPutOverwriteAndReturnValue() {
        map.put("k", "first");
        String old = map.put("k", "second");
        assertEquals("first", old);
        assertEquals("second", map.get("k"));
    }

    @Test
    public void testRemove() {
        map.put("one", "1");
        map.put("two", "2");
        String removed = map.remove("one");
        assertEquals("1", removed);
        assertFalse(map.containsKey("one"));
        assertNull(map.remove("nonexistent"));
    }

    @Test
    public void testPutAllAndClear() {
        Map<String, String> src = new HashMap<>();
        src.put("a", "A");
        src.put("b", "B");
        map.putAll(src);
        assertEquals(2, map.size());
        map.clear();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testHashCodeAndEquals() {
        map.put("x", "1");
        map.put("y", "2");

        CompactMap<String, String> other = new CompactMap<>();
        other.put("x", "1");
        other.put("y", "2");

        assertEquals(map, other);
        assertEquals(map.hashCode(), other.hashCode());

        other.put("z", "3");
        assertNotEquals(map, other);
    }

    @Test
    public void testToString() {
        assertEquals("{}", map.toString());
        map.put("k", "v");
        assertEquals("{k=v}", map.toString());
        map.put("a", "b");
        // Order is not guaranteed for HashMap – just verify format
        String s = map.toString();
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        assertTrue(s.contains("k=v"));
        assertTrue(s.contains("a=b"));
    }

    @Test
    public void testKeySet() {
        map.put("one", "1");
        map.put("two", "2");
        Set<String> ks = map.keySet();
        assertTrue(ks.contains("one"));
        assertTrue(ks.contains("two"));
        assertEquals(2, ks.size());

        // remove via keySet
        ks.remove("one");
        assertFalse(map.containsKey("one"));
        assertEquals(1, map.size());
    }

    @Test
    public void testValuesCollection() {
        map.put("a", "A");
        map.put("b", "B");
        Collection<String> vals = map.values();
        assertTrue(vals.contains("A"));
        assertTrue(vals.contains("B"));
        assertEquals(2, vals.size());

        // clear via values collection
        vals.clear();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testEntrySetBasicOperations() {
        map.put("k1", "v1");
        map.put("k2", "v2");
        Set<Map.Entry<String, String>> entries = map.entrySet();

        assertEquals(2, entries.size());
        for (Map.Entry<String, String> e : entries) {
            assertTrue(map.containsKey(e.getKey()));
            assertEquals(map.get(e.getKey()), e.getValue());
        }

        // remove via entrySet
        Iterator<Map.Entry<String, String>> it = entries.iterator();
        Map.Entry<String, String> first = it.next();
        it.remove(); // remove first entry
        assertEquals(1, map.size());
        assertFalse(map.containsKey(first.getKey()));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testMinusUnsupported() {
        map.minus("any");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPlusUnsupported() {
        map.plus("any");
    }

    /* ----------------------------------------------------------------------
     * 3. EDGE‑CASE & EXCEPTION TESTS
     * ---------------------------------------------------------------------- */

    @Test
    public void testSingleEntrySpecialKey() {
        // The default getSingleValueKey() returns the literal "key"
        CompactMap<String, String> m = new CompactMap<>();
        m.put("key", "onlyValue"); // should store the value directly (no CompactMapEntry)
        assertEquals(1, m.size());
        assertEquals("onlyValue", m.get("key"));
        // internal representation is OBJECT, not ENTRY – we verify via equals on entrySet
        Set<Map.Entry<String, String>> es = m.entrySet();
        assertEquals(1, es.size());
        Map.Entry<String, String> e = es.iterator().next();
        assertEquals("key", e.getKey());
        assertEquals("onlyValue", e.getValue());
    }

    @Test
    public void testSingleEntryDifferentKeyCreatesEntry() {
        CompactMap<String, String> m = new CompactMap<>();
        m.put("other", "val");
        assertEquals(1, m.size());
        assertTrue(m.entrySet().iterator().next() instanceof CompactMap.CompactMapEntry);
    }

    @Test
    public void testNullKeyAndNullValue() {
        CompactMap<String, String> m = new CompactMap<>();
        m.put(null, "nullKey");
        m.put("nullVal", null);
        assertEquals("nullKey", m.get(null));
        assertTrue(m.containsKey(null));
        assertTrue(m.containsValue(null));
        assertNull(m.get("nullVal"));
    }

    @Test
    public void testTransitionArrayToMap() {
        // smallMap has compactSize == 3, so after 4 puts we must switch to a real Map
        smallMap.put(1, 10);
        smallMap.put(2, 20);
        smallMap.put(3, 30);
        assertEquals(3, smallMap.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, smallMap.getLogicalValueType());

        // this 4th put triggers the transition
        smallMap.put(4, 40);
        assertEquals(4, smallMap.size());
        assertEquals(CompactMap.LogicalValueType.MAP, smallMap.getLogicalValueType());

        // verify all entries are present
        assertEquals(Integer.valueOf(10), smallMap.get(1));
        assertEquals(Integer.valueOf(20), smallMap.get(2));
        assertEquals(Integer.valueOf(30), smallMap.get(3));
        assertEquals(Integer.valueOf(40), smallMap.get(4));
    }

    @Test
    public void testRemoveShrinksFromArrayToEntry() {
        // Build a map with exactly 2 entries (still stored as Object[])
        smallMap.put(1, 100);
        smallMap.put(2, 200);
        assertEquals(2, smallMap.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, smallMap.getLogicalValueType());

        // Remove one entry – should shrink to single entry representation
        smallMap.remove(1);
        assertEquals(1, smallMap.size());
        assertEquals(CompactMap.LogicalValueType.OBJECT, smallMap.getLogicalValueType());
        assertEquals(Integer.valueOf(200), smallMap.get(2));
    }

    @Test
    public void testIteratorFailFastOnModification() {
        map.put("a", "A");
        map.put("b", "B");
        Iterator<String> it = map.keySet().iterator();
        assertTrue(it.hasNext());
        map.put("c", "C"); // structural modification after iterator creation
        try {
            it.next();
            fail("Expected ConcurrentModificationException");
        } catch (ConcurrentModificationException e) {
            // expected
        }
    }

    @Test
    public void testIteratorRemoveBehaviour() {
        map.put("x", "X");
        map.put("y", "Y");
        Iterator<String> it = map.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            if ("x".equals(key)) {
                it.remove(); // remove via iterator
            }
        }
        assertFalse(map.containsKey("x"));
        assertEquals(1, map.size());
    }

    @Test(expected = NoSuchElementException.class)
    public void testIteratorNoSuchElementException() {
        map.put("only", "value");
        Iterator<String> it = map.keySet().iterator();
        it.next(); // first element
        it.next(); // should throw
    }

    @Test(expected = IllegalStateException.class)
    public void testIteratorRemoveWithoutNext() {
        map.put("p", "P");
        Iterator<String> it = map.keySet().iterator();
        it.remove(); // illegal – next() not called yet
    }

    @Test
    public void testEntrySetContainsAndRemove() {
        map.put("k1", "v1");
        map.put("k2", "v2");
        Map.Entry<String, String> e = new AbstractMap.SimpleEntry<>("k1", "v1");
        assertTrue(map.entrySet().contains(e));

        // remove via entrySet.remove()
        boolean removed = map.entrySet().remove(e);
        assertTrue(removed);
        assertFalse(map.containsKey("k1"));
        assertEquals(1, map.size());
    }

    @Test
    public void testEntrySetRetainAll() {
        map.put("a", "1");
        map.put("b", "2");
        map.put("c", "3");

        Set<Map.Entry<String, String>> retain = new HashSet<>();
        retain.add(new AbstractMap.SimpleEntry<>("a", "1"));
        retain.add(new AbstractMap.SimpleEntry<>("c", "3"));

        boolean changed = map.entrySet().retainAll(retain);
        assertTrue(changed);
        assertEquals(2, map.size());
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsKey("c"));
        assertFalse(map.containsKey("b"));
    }

    @Test
    public void testKeySetRetainAllWithMixedTypes() {
        map.put("x", "X");
        map.put("y", "Y");
        map.put("z", "Z");

        List<String> toKeep = Arrays.asList("x", "z");
        boolean changed = map.keySet().retainAll(toKeep);
        assertTrue(changed);
        assertEquals(2, map.size());
        assertTrue(map.containsKey("x"));
        assertTrue(map.containsKey("z"));
        assertFalse(map.containsKey("y"));
    }

    @Test
    public void testValuesIteratorRemove() {
        map.put("k1", "v1");
        map.put("k2", "v2");
        Iterator<String> vit = map.values().iterator();
        while (vit.hasNext()) {
            String v = vit.next();
            if ("v1".equals(v)) {
                vit.remove();
            }
        }
        assertFalse(map.containsValue("v1"));
        assertEquals(1, map.size());
    }

    @Test
    public void testCopyIteratorsProduceSnapshot() {
        // Populate map, then obtain a copy iterator; modify map afterwards.
        map.put("a", "A");
        map.put("b", "B");
        Iterator<String> copyKeyIt = map.keySet().iterator(); // copy iterator because default map is HashMap (SortedMap? no)
        // Force copy iterator by using a SortedMap via subclass (optional). Here we rely on default behaviour:
        // Since useCopyIterator() returns false for HashMap, we test the snapshot behaviour of CopyIterator
        // via entrySet().iterator() after a structural change.
        Iterator<Map.Entry<String, String>> copyEntryIt = map.entrySet().iterator();

        // modify original map
        map.put("c", "C");
        map.remove("a");

        // The copy iterator should still iterate over the original snapshot (size 2)
        Set<String> seenKeys = new HashSet<>();
        while (copyEntryIt.hasNext()) {
            seenKeys.add(copyEntryIt.next().getKey());
        }
        assertEquals(new HashSet<>(Arrays.asList("a", "b")), seenKeys);
    }

    /* ----------------------------------------------------------------------
     * 4. Helper methods (if any)
     * ---------------------------------------------------------------------- */
}
