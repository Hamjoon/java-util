package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link CaseInsensitiveMap}.
 *
 * The suite is organized as follows:
 *
 * <ul>
 *   <li>Extraction of all public methods (including signatures) – listed in the comment block.</li>
 *   <li>One basic test per method that verifies the normal (happy‑path) behaviour.</li>
 *   <li>Additional tests that cover edge‑cases, null handling, case‑insensitivity nuances,
 *       and the two deprecated operations that must throw {@link UnsupportedOperationException}.</li>
 *   <li>Tests for the inner classes {@link CaseInsensitiveMap.CaseInsensitiveEntry}
 *       and {@link CaseInsensitiveMap.CaseInsensitiveString}.</li>
 * </ul>
 *
 * No external mocking frameworks are used – the tests instantiate real objects
 * from the library and from the JDK only.
 */
public class CaseInsensitiveMapTest {

    /* --------------------------------------------------------------
     *  Public methods of CaseInsensitiveMap (extracted from the source)
     * --------------------------------------------------------------
     *
     * Constructors
     *   CaseInsensitiveMap()
     *   CaseInsensitiveMap(int initialCapacity)
     *   CaseInsensitiveMap(int initialCapacity, float loadFactor)
     *   CaseInsensitiveMap(Map<K,V> source, Map<K,V> mapInstance)
     *   CaseInsensitiveMap(Map<K,V> m)
     *
     * Core Map API
     *   V get(Object key)
     *   boolean containsKey(Object key)
     *   V put(K key, V value)
     *   Object putObject(Object key, Object value)
     *   void putAll(Map<? extends K,? extends V> m)
     *   V remove(Object key)
     *   int size()
     *   boolean isEmpty()
     *   boolean equals(Object other)
     *   int hashCode()
     *   String toString()
     *   void clear()
     *   boolean containsValue(Object value)
     *   Collection<V> values()
     *   Map minus(Object removeMe)               // deprecated – throws
     *   Map plus(Object right)                  // deprecated – throws
     *   Map<K,V> getWrappedMap()
     *   Set<K> keySet()
     *   Set<Map.Entry<K,V>> entrySet()
     *
     * Inner classes
     *   class CaseInsensitiveEntry extends AbstractMap.SimpleEntry<K,V>
     *        CaseInsensitiveEntry(Entry<K,V> entry)
     *        K getKey()
     *        K getOriginalKey()
     *        V setValue(V value)
     *
     *   static final class CaseInsensitiveString implements Comparable
     *        CaseInsensitiveString(String string)
     *        String toString()
     *        int hashCode()
     *        boolean equals(Object other)
     *        int compareTo(Object o)
     *
     * -------------------------------------------------------------- */

    private CaseInsensitiveMap<String, Integer> map;

    @Before
    public void setUp() {
        map = new CaseInsensitiveMap<>();
    }

    /* ==================== BASIC FUNCTIONALITY TESTS ==================== */

    @Test
    public void testPutAndGet_StringKey_CaseInsensitive() {
        map.put("Foo", 1);
        assertEquals(Integer.valueOf(1), map.get("foo"));
        assertEquals(Integer.valueOf(1), map.get("FOO"));
    }

    @Test
    public void testPutAndGet_NonStringKey() {
        Integer key = 42;               // non‑String key
        map.put(key.toString(), 10);    // normal string key
        assertNull(map.get(key));       // different type – not found
        map.put((String) key, 20);      // store with string representation
        assertEquals(Integer.valueOf(20), map.get(key.toString()));
    }

    @Test
    public void testContainsKey_StringCaseInsensitive() {
        map.put("Bar", 2);
        assertTrue(map.containsKey("bar"));
        assertTrue(map.containsKey("BAR"));
        assertFalse(map.containsKey("baz"));
    }

    @Test
    public void testRemove_StringKey() {
        map.put("XyZ", 5);
        assertEquals(Integer.valueOf(5), map.remove("xyz"));
        assertTrue(map.isEmpty());
    }

    @Test
    public void testSizeAndIsEmpty() {
        assertTrue(map.isEmpty());
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(2, map.size());
        assertFalse(map.isEmpty());
    }

    @Test
    public void testClear() {
        map.put("one", 1);
        map.put("two", 2);
        map.clear();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testContainsValue() {
        map.put("alpha", 100);
        assertTrue(map.containsValue(100));
        assertFalse(map.containsValue(200));
    }

    @Test
    public void testValuesCollectionReflectsUpdates() {
        map.put("k1", 1);
        map.put("k2", 2);
        Collection<Integer> values = map.values();
        assertTrue(values.containsAll(Arrays.asList(1, 2)));

        map.remove("k1");
        assertFalse(values.contains(1));
        assertTrue(values.contains(2));
    }

    @Test
    public void testEqualsAndHashCode_Simple() {
        CaseInsensitiveMap<String, Integer> other = new CaseInsensitiveMap<>();
        map.put("Key", 10);
        other.put("key", 10);   // different case, same value

        assertTrue(map.equals(other));
        assertEquals(map.hashCode(), other.hashCode());

        other.put("another", 5);
        assertFalse(map.equals(other));
    }

    @Test
    public void testToStringDelegatesToWrappedMap() {
        map.put("A", 1);
        map.put("B", 2);
        String s = map.toString();
        // The exact format is the same as LinkedHashMap's toString()
        assertTrue(s.contains("A=1"));
        assertTrue(s.contains("B=2"));
    }

    @Test
    public void testGetWrappedMap() {
        Map<String, Integer> wrapped = map.getWrappedMap();
        assertNotNull(wrapped);
        assertTrue(wrapped instanceof LinkedHashMap);
        map.put("test", 99);
        assertEquals(Integer.valueOf(99), wrapped.get(new CaseInsensitiveMap.CaseInsensitiveString("TEST")));
    }

    @Test
    public void testPutObject() {
        map.putObject("OBJ", 123);
        assertEquals(Integer.valueOf(123), map.get("obj"));
    }

    @Test
    public void testPutAll_WithMixedCaseKeys() {
        Map<String, Integer> source = new HashMap<>();
        source.put("One", 1);
        source.put("TWO", 2);
        map.putAll(source);
        assertEquals(Integer.valueOf(1), map.get("one"));
        assertEquals(Integer.valueOf(2), map.get("two"));
    }

    @Test
    public void testKeySet_CaseInsensitiveBehavior() {
        map.put("First", 10);
        map.put("second", 20);
        Set<String> keys = map.keySet();

        // contains works case‑insensitively
        assertTrue(keys.contains("FIRST"));
        assertTrue(keys.contains("Second"));
        assertFalse(keys.contains("third"));

        // remove works case‑insensitively
        assertTrue(keys.remove("FIRST"));
        assertFalse(map.containsKey("first"));
        assertEquals(1, keys.size());

        // iterator returns original case strings
        Iterator<String> it = keys.iterator();
        assertTrue(it.hasNext());
        assertEquals("second", it.next());
    }

    @Test
    public void testEntrySet_BasicOperations() {
        map.put("Alpha", 100);
        map.put("Beta", 200);
        Set<Map.Entry<String, Integer>> entries = map.entrySet();

        // contains works with case‑insensitive keys
        Map.Entry<String, Integer> probe = new AbstractMap.SimpleEntry<>("alpha", 100);
        assertTrue(entries.contains(probe));

        // remove works case‑insensitively
        assertTrue(entries.remove(new AbstractMap.SimpleEntry<>("BETA", 200)));
        assertFalse(map.containsKey("beta"));
        assertEquals(1, entries.size());

        // iterator returns CaseInsensitiveEntry that exposes original key string
        Iterator<Map.Entry<String, Integer>> it = entries.iterator();
        assertTrue(it.hasNext());
        Map.Entry<String, Integer> e = it.next();
        assertEquals("Alpha", e.getKey());   // original case
        assertEquals(Integer.valueOf(100), e.getValue());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testMinus_DeprecatedThrows() {
        map.minus("any");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPlus_DeprecatedThrows() {
        map.plus("any");
    }

    /* ==================== EDGE CASES & EXCEPTION SCENARIOS ==================== */

    @Test
    public void testNullKeyBehaviour() {
        // The implementation does not guard against null keys – they are stored as null.
        map.put(null, 0);
        assertTrue(map.containsKey(null));
        assertEquals(Integer.valueOf(0), map.get(null));
        assertEquals(Integer.valueOf(0), map.remove(null));
        assertFalse(map.containsKey(null));
    }

    @Test
    public void testNullValueBehaviour() {
        map.put("nullValue", null);
        assertTrue(map.containsKey("NULLVALUE"));
        assertTrue(map.containsValue(null));
        assertNull(map.get("nullvalue"));
    }

    @Test
    public void testCaseInsensitiveStringEqualsAndHashCode() {
        CaseInsensitiveMap.CaseInsensitiveString s1 = new CaseInsensitiveMap.CaseInsensitiveString("TeSt");
        CaseInsensitiveMap.CaseInsensitiveString s2 = new CaseInsensitiveMap.CaseInsensitiveString("test");
        CaseInsensitiveMap.CaseInsensitiveString s3 = new CaseInsensitiveMap.CaseInsensitiveString("TESTING");

        assertTrue(s1.equals(s2));
        assertTrue(s2.equals("TEST"));
        assertFalse(s1.equals(s3));
        assertEquals(s1.hashCode(), s2.hashCode());
    }

    @Test
    public void testCaseInsensitiveStringCompareTo() {
        CaseInsensitiveMap.CaseInsensitiveString s1 = new CaseInsensitiveMap.CaseInsensitiveString("abc");
        CaseInsensitiveMap.CaseInsensitiveString s2 = new CaseInsensitiveMap.CaseInsensitiveString("ABC");
        CaseInsensitiveMap.CaseInsensitiveString s3 = new CaseInsensitiveMap.CaseInsensitiveString("abd");

        assertEquals(0, s1.compareTo(s2));
        assertTrue(s1.compareTo(s3) < 0);
        assertTrue(s3.compareTo(s1) > 0);
        assertTrue(s1.compareTo("ab") > 0); // compare to plain String
        assertTrue(s1.compareTo(123) < 0);  // non‑String compared -> defined as -1
    }

    @Test
    public void testCaseInsensitiveEntrySetValueUpdate() {
        map.put("Key", 1);
        Set<Map.Entry<String, Integer>> entrySet = map.entrySet();
        Map.Entry<String, Integer> entry = entrySet.iterator().next();

        // setValue should update the underlying map
        entry.setValue(42);
        assertEquals(Integer.valueOf(42), map.get("key"));
    }

    @Test
    public void testKeySetRemoveAllAndRetainAll() {
        map.put("A", 1);
        map.put("B", 2);
        map.put("C", 3);
        Set<String> ks = map.keySet();

        // removeAll with mixed case
        ks.removeAll(Arrays.asList("a", "c"));
        assertFalse(map.containsKey("a"));
        assertFalse(map.containsKey("c"));
        assertTrue(map.containsKey("b"));

        // retainAll – keep only B (different case)
        ks.retainAll(Collections.singleton("B"));
        assertEquals(1, ks.size());
        assertTrue(ks.contains("b"));
    }

    @Test
    public void testEntrySetRemoveAllAndRetainAll() {
        map.put("X", 10);
        map.put("Y", 20);
        map.put("Z", 30);
        Set<Map.Entry<String, Integer>> es = map.entrySet();

        // removeAll with a single entry (different case)
        es.removeAll(Collections.singleton(new AbstractMap.SimpleEntry<>("x", 10)));
        assertFalse(map.containsKey("x"));
        assertEquals(2, es.size());

        // retainAll – keep only Y
        es.retainAll(Collections.singleton(new AbstractMap.SimpleEntry<>("Y", 20)));
        assertEquals(1, es.size());
        assertTrue(map.containsKey("y"));
        assertFalse(map.containsKey("z"));
    }

    @Test
    public void testConstructor_WithSourceAndMapInstance() {
        Map<String, Integer> source = new HashMap<>();
        source.put("One", 1);
        source.put("Two", 2);
        Map<String, Integer> backing = new TreeMap<>();
        CaseInsensitiveMap<String, Integer> custom = new CaseInsensitiveMap<>(source, backing);

        assertTrue(custom.containsKey("ONE"));
        assertEquals(Integer.valueOf(2), custom.get("two"));
        assertTrue(custom.getWrappedMap() instanceof TreeMap);
    }

    @Test
    public void testConstructor_WithVariousMapTypes() {
        // TreeMap
        CaseInsensitiveMap<String, Integer> tiTree = new CaseInsensitiveMap<>(new TreeMap<>());
        assertTrue(tiTree.getWrappedMap() instanceof TreeMap);

        // LinkedHashMap
        CaseInsensitiveMap<String, Integer> tiLinked = new CaseInsensitiveMap<>(new LinkedHashMap<>());
        assertTrue(tiLinked.getWrappedMap() instanceof LinkedHashMap);

        // ConcurrentHashMap
        CaseInsensitiveMap<String, Integer> tiConcurrent = new CaseInsensitiveMap<>(new ConcurrentHashMap<>());
        assertTrue(tiConcurrent.getWrappedMap() instanceof ConcurrentHashMap);
    }

    @Test
    public void testPutAll_WithCaseInsensitiveEntry() {
        // Build a map that already uses CaseInsensitiveEntry
        CaseInsensitiveMap<String, Integer> inner = new CaseInsensitiveMap<>();
        inner.put("inner", 5);
        Map<String, Integer> source = new HashMap<>();
        source.putAll(inner); // entries are normal

        // Now use putAll – it will treat normal entries correctly
        map.putAll(source);
        assertEquals(Integer.valueOf(5), map.get("INNER"));
    }

    @Test
    public void testHashCode_ConsistencyWithKeySet() {
        map.put("Aa", 1); // "Aa" and "aA" have same hashCodeIgnoreCase
        map.put("aA", 2);
        // Because keys are case‑insensitive, the second put overwrites the first
        assertEquals(1, map.size());
        // The hashCode of the map must match the hashCode of its keySet (as implemented)
        assertEquals(map.keySet().hashCode(), map.hashCode() ^ map.values().stream()
                .mapToInt(v -> v == null ? 0 : v.hashCode()).sum());
    }
}
