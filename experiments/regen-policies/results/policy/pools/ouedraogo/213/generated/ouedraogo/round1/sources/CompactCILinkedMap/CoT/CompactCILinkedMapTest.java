package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class CompactCILinkedMapTest {

    private CompactCILinkedMap<String, Integer> map;

    @Before
    public void setUp() {
        map = new CompactCILinkedMap<>();
    }

    /* -------------------------------------------------
     *  Public constructors
     * ------------------------------------------------- */

    @Test
    public void testDefaultConstructorCreatesEmptyMap() {
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testConstructorWithMapCopiesEntriesCaseInsensitive() {
        Map<String, Integer> source = new LinkedHashMap<>();
        source.put("One", 1);
        source.put("Two", 2);
        CompactCILinkedMap<String, Integer> copy = new CompactCILinkedMap<>(source);

        // original size
        assertEquals(2, copy.size());

        // case‑insensitive lookup
        assertEquals(Integer.valueOf(1), copy.get("ONE"));
        assertEquals(Integer.valueOf(2), copy.get("two"));
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullMapThrowsNPE() {
        new CompactCILinkedMap<>(null);
    }

    /* -------------------------------------------------
     *  Basic map operations
     * ------------------------------------------------- */

    @Test
    public void testPutAndGetCaseInsensitive() {
        map.put("KeyOne", 10);
        assertEquals(Integer.valueOf(10), map.get("keyone"));
        assertEquals(Integer.valueOf(10), map.get("KEYONE"));
    }

    @Test
    public void testPutNullKeyAndValue() {
        map.put(null, 123);
        assertTrue(map.containsKey(null));
        assertEquals(Integer.valueOf(123), map.get(null));

        map.put("nullValue", null);
        assertTrue(map.containsKey("nullValue"));
        assertNull(map.get("nullvalue"));
    }

    @Test
    public void testContainsKeyCaseInsensitive() {
        map.put("AbC", 5);
        assertTrue(map.containsKey("abc"));
        assertTrue(map.containsKey("ABC"));
    }

    @Test
    public void testContainsValue() {
        map.put("x", 42);
        assertTrue(map.containsValue(42));
        assertFalse(map.containsValue(99));
    }

    @Test
    public void testRemoveKeyCaseInsensitive() {
        map.put("ToRemove", 77);
        Integer removed = map.remove("toremove");
        assertEquals(Integer.valueOf(77), removed);
        assertFalse(map.containsKey("ToRemove"));
        assertTrue(map.isEmpty());
    }

    @Test
    public void testRemoveNonExistingKeyReturnsNull() {
        assertNull(map.remove("doesNotExist"));
    }

    @Test
    public void testPutAllPreservesInsertionOrderAndCaseInsensitivity() {
        Map<String, Integer> other = new LinkedHashMap<>();
        other.put("First", 1);
        other.put("Second", 2);
        other.put("THIRD", 3);

        map.putAll(other);

        // order must be First, Second, THIRD
        List<String> keysInOrder = new ArrayList<>(map.keySet());
        assertEquals(Arrays.asList("First", "Second", "THIRD"), keysInOrder);

        // case‑insensitive lookups
        assertEquals(Integer.valueOf(1), map.get("first"));
        assertEquals(Integer.valueOf(2), map.get("SECOND"));
        assertEquals(Integer.valueOf(3), map.get("third"));
    }

    @Test
    public void testClearEmptiesMap() {
        map.put("A", 1);
        map.put("B", 2);
        assertFalse(map.isEmpty());

        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testKeySetValuesAndEntrySet() {
        map.put("Alpha", 10);
        map.put("Beta", 20);

        Set<String> keySet = map.keySet();
        Collection<Integer> values = map.values();
        Set<Map.Entry<String, Integer>> entries = map.entrySet();

        assertEquals(new HashSet<>(Arrays.asList("Alpha", "Beta")), keySet);
        assertEquals(new HashSet<>(Arrays.asList(10, 20)), new HashSet<>(values));

        Map<String, Integer> rebuilt = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : entries) {
            rebuilt.put(e.getKey(), e.getValue());
        }
        assertEquals(2, rebuilt.size());
        assertEquals(Integer.valueOf(10), rebuilt.get("Alpha"));
        assertEquals(Integer.valueOf(20), rebuilt.get("Beta"));
    }

    /* -------------------------------------------------
     *  minus / plus (immutable‑style helpers)
     * ------------------------------------------------- */

    @Test
    public void testMinusRemovesKey() {
        map.put("keep", 1);
        map.put("drop", 2);
        Map<String, Integer> result = map.minus("DROP"); // case‑insensitive

        assertFalse(result.containsKey("drop"));
        assertTrue(result.containsKey("keep"));
        assertEquals(Integer.valueOf(1), result.get("keep"));
    }

    @Test
    public void testPlusAddsKey() {
        map.put("existing", 5);
        Map<String, Integer> result = map.plus(new AbstractMap.SimpleEntry<>("NEWKEY", 9));

        assertTrue(result.containsKey("existing"));
        assertTrue(result.containsKey("newkey"));
        assertEquals(Integer.valueOf(9), result.get("newkey"));
    }

    /* -------------------------------------------------
     *  Equality, hashCode and toString
     * ------------------------------------------------- */

    @Test
    public void testEqualsAndHashCode() {
        CompactCILinkedMap<String, Integer> other = new CompactCILinkedMap<>();
        map.put("x", 1);
        other.put("X", 1); // different case but logically equal

        assertEquals(map, other);
        assertEquals(map.hashCode(), other.hashCode());

        other.put("y", 2);
        assertNotEquals(map, other);
    }

    @Test
    public void testToStringContainsEntries() {
        map.put("one", 1);
        map.put("two", 2);
        String s = map.toString();
        assertTrue(s.contains("one=1"));
        assertTrue(s.contains("two=2"));
    }

    /* -------------------------------------------------
     *  Protected behaviour (exposed via a test subclass)
     * ------------------------------------------------- */

    private static class TestableCompactCILinkedMap extends CompactCILinkedMap<String, Integer> {
        public Map<String, Integer> exposeGetNewMap() {
            return super.getNewMap();
        }

        public boolean exposeIsCaseInsensitive() {
            return super.isCaseInsensitive();
        }

        public boolean exposeUseCopyIterator() {
            return super.useCopyIterator();
        }

        public int exposeCompactSize() {
            return super.compactSize();
        }
    }

    @Test
    public void testProtectedMethodsViaSubclass() {
        TestableCompactCILinkedMap testMap = new TestableCompactCILinkedMap();

        assertTrue(testMap.exposeIsCaseInsensitive());
        assertFalse(testMap.exposeUseCopyIterator());

        // compactSize() should be the default defined in CompactMap (>=0)
        assertTrue(testMap.exposeCompactSize() >= 0);

        // The map returned by getNewMap must be a CaseInsensitiveMap backed by a LinkedHashMap
        Map<String, Integer> newMap = testMap.exposeGetNewMap();
        assertNotNull(newMap);
        // insertion order guarantee – we insert two entries and verify order via iteration
        newMap.put("a", 1);
        newMap.put("b", 2);
        Iterator<String> it = newMap.keySet().iterator();
        assertEquals("a", it.next());
        assertEquals("b", it.next());
    }

    /* -------------------------------------------------
     *  Config / builder related methods (basic sanity)
     * ------------------------------------------------- */

    @Test
    public void testWithConfigReturnsSameType() {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(CompactMap.CASE_SENSITIVE, Boolean.FALSE);
        CompactMap<String, Integer> configured = map.withConfig(cfg);
        assertNotNull(configured);
        // the returned object should still behave like a map
        configured.put("cfgKey", 100);
        assertEquals(Integer.valueOf(100), configured.get("CFGKEY"));
    }

    @Test
    public void testBuilderCreatesNonNullMap() {
        CompactMap.Builder<String, Integer> builder = CompactMap.builder();
        CompactMap<String, Integer> built = builder.build();
        assertNotNull(built);
        assertTrue(built.isEmpty());
    }
}
