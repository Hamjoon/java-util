package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class CompactCILinkedMapTest {

    /*** 1. Public constructors ***/

    @Test
    public void testDefaultConstructorCreatesEmptyMap() {
        CompactCILinkedMap<String, String> map = new CompactCILinkedMap<>();
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
    }

    @Test
    public void testConstructorWithExistingMapCopiesEntries() {
        Map<String, String> source = new HashMap<>();
        source.put("One", "1");
        source.put("Two", "2");

        CompactCILinkedMap<String, String> map = new CompactCILinkedMap<>(source);
        assertEquals(2, map.size());
        assertEquals("1", map.get("One"));
        assertEquals("2", map.get("Two"));
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullMapThrowsException() {
        // CompactMap's constructor is expected to NPE on a null argument
        new CompactCILinkedMap<>(null);
    }

    /*** 2. Basic Map operations ***/

    @Test
    public void testPutAndGetCaseInsensitive() {
        CompactCILinkedMap<String, String> map = new CompactCILinkedMap<>();
        map.put("Key", "value1");

        // Different case variations must retrieve the same value
        assertEquals("value1", map.get("key"));
        assertEquals("value1", map.get("KEY"));
        assertTrue(map.containsKey("kEy"));
        // The original case is retained in keySet()
        assertTrue(map.keySet().contains("Key"));
        assertFalse(map.keySet().contains("key"));
    }

    @Test
    public void testPutOverwriteWithDifferentCase() {
        CompactCILinkedMap<String, String> map = new CompactCILinkedMap<>();
        map.put("Key", "first");
        map.put("key", "second"); // should replace the previous entry

        assertEquals(1, map.size());
        assertEquals("second", map.get("KEY"));
    }

    @Test
    public void testContainsKeyCaseInsensitive() {
        CompactCILinkedMap<String, Integer> map = new CompactCILinkedMap<>();
        map.put("AbC", 123);
        assertTrue(map.containsKey("abc"));
        assertTrue(map.containsKey("ABC"));
        assertFalse(map.containsKey("def"));
    }

    @Test
    public void testRemoveCaseInsensitive() {
        CompactCILinkedMap<String, String> map = new CompactCILinkedMap<>();
        map.put("RemoveMe", "value");
        assertEquals("value", map.remove("removeme"));
        assertFalse(map.containsKey("RemoveMe"));
        assertEquals(0, map.size());
    }

    @Test
    public void testPutNonStringKeyIsHandledNormally() {
        CompactCILinkedMap<Object, String> map = new CompactCILinkedMap<>();
        Integer intKey = 42;
        map.put(intKey, "answer");
        assertEquals("answer", map.get(42));
        assertTrue(map.containsKey(42));
    }

    @Test
    public void testPutNullKeyAndNullValue() {
        CompactCILinkedMap<String, String> map = new CompactCILinkedMap<>();
        map.put(null, "nullKey");
        map.put("nullValue", null);

        assertEquals("nullKey", map.get(null));
        assertTrue(map.containsKey(null));
        assertTrue(map.containsKey("nullValue"));
        assertNull(map.get("nullValue"));
    }

    @Test
    public void testPutAllCopiesAllEntries() {
        CompactCILinkedMap<String, String> map = new CompactCILinkedMap<>();
        Map<String, String> other = new HashMap<>();
        other.put("Alpha", "A");
        other.put("beta", "B");

        map.putAll(other);
        assertEquals(2, map.size());
        assertEquals("A", map.get("ALPHA"));
        assertEquals("B", map.get("BETA"));
    }

    @Test
    public void testClearEmptiesMap() {
        CompactCILinkedMap<String, String> map = new CompactCILinkedMap<>();
        map.put("One", "1");
        map.put("Two", "2");
        assertFalse(map.isEmpty());

        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    /*** 3. Collection views ***/

    @Test
    public void testKeySetOrderPreservedAfterManyInsertions() {
        CompactCILinkedMap<String, Integer> map = new CompactCILinkedMap<>();

        // Insert more entries than the typical compactSize (default is 8 in CompactMap)
        List<String> insertionOrder = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            String key = "k" + i;
            insertionOrder.add(key);
            map.put(key, i);
        }

        List<String> keySetOrder = new ArrayList<>(map.keySet());
        assertEquals(insertionOrder, keySetOrder);
    }

    @Test
    public void testValuesCollectionMatchesInsertionOrder() {
        CompactCILinkedMap<String, Integer> map = new CompactCILinkedMap<>();
        List<Integer> expected = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            String key = "v" + i;
            map.put(key, i);
            expected.add(i);
        }

        List<Integer> actual = new ArrayList<>(map.values());
        assertEquals(expected, actual);
    }

    @Test
    public void testEntrySetIterationOrder() {
        CompactCILinkedMap<String, String> map = new CompactCILinkedMap<>();
        LinkedHashMap<String, String> reference = new LinkedHashMap<>();

        for (char c = 'a'; c <= 'e'; c++) {
            String key = String.valueOf(c);
            String val = "val" + c;
            map.put(key, val);
            reference.put(key, val);
        }

        Iterator<Map.Entry<String, String>> mapIter = map.entrySet().iterator();
        Iterator<Map.Entry<String, String>> refIter = reference.entrySet().iterator();

        while (mapIter.hasNext() && refIter.hasNext()) {
            Map.Entry<String, String> m = mapIter.next();
            Map.Entry<String, String> r = refIter.next();
            assertEquals(r.getKey(), m.getKey());
            assertEquals(r.getValue(), m.getValue());
        }
        assertFalse(mapIter.hasNext());
        assertFalse(refIter.hasNext());
    }

    /*** 4. Minus / Plus operations ***/

    @Test
    public void testMinusCreatesNewMapWithoutSpecifiedKey() {
        CompactCILinkedMap<String, String> original = new CompactCILinkedMap<>();
        original.put("keep", "yes");
        original.put("remove", "no");

        Map<String, String> result = original.minus("REMOVE"); // case‑insensitive removal
        assertEquals(1, result.size());
        assertTrue(result.containsKey("keep"));
        assertFalse(result.containsKey("remove"));

        // original map must stay unchanged
        assertEquals(2, original.size());
        assertTrue(original.containsKey("remove"));
    }

    @Test
    public void testPlusCreatesNewMapWithAdditionalEntry() {
        CompactCILinkedMap<String, Integer> original = new CompactCILinkedMap<>();
        original.put("a", 1);
        original.put("b", 2);

        Map<String, Integer> result = original.plus(Collections.singletonMap("c", 3));
        assertEquals(3, result.size());
        assertTrue(result.containsKey("c"));
        assertEquals(Integer.valueOf(3), result.get("c"));

        // original map unchanged
        assertEquals(2, original.size());
        assertFalse(original.containsKey("c"));
    }

    /*** 5. Equality, hashCode and toString ***/

    @Test
    public void testEqualsAndHashCodeWithIdenticalContent() {
        CompactCILinkedMap<String, String> map1 = new CompactCILinkedMap<>();
        CompactCILinkedMap<String, String> map2 = new CompactCILinkedMap<>();

        map1.put("One", "1");
        map1.put("Two", "2");

        map2.put("ONE", "1"); // different case, same logical key
        map2.put("two", "2");

        assertTrue(map1.equals(map2));
        assertTrue(map2.equals(map1));
        assertEquals(map1.hashCode(), map2.hashCode());
    }

    @Test
    public void testEqualsWithStandardMapImplementation() {
        CompactCILinkedMap<String, String> ciMap = new CompactCILinkedMap<>();
        ciMap.put("X", "x");
        ciMap.put("Y", "y");

        Map<String, String> hashMap = new HashMap<>();
        hashMap.put("X", "x");
        hashMap.put("Y", "y");

        assertTrue(ciMap.equals(hashMap));
        assertTrue(hashMap.equals(ciMap));
    }

    @Test
    public void testToStringContainsAllEntries() {
        CompactCILinkedMap<String, Integer> map = new CompactCILinkedMap<>();
        map.put("first", 1);
        map.put("second", 2);

        String s = map.toString();
        assertTrue(s.contains("first=1"));
        assertTrue(s.contains("second=2"));
        // The string representation of a Map usually starts with '{' and ends with '}'
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
    }

    /*** 6. Edge case: very large number of entries (exceeds compactSize) ***/

    @Test
    public void testMapBehavesCorrectlyWhenSizeExceedsCompactThreshold() {
        CompactCILinkedMap<String, Integer> map = new CompactCILinkedMap<>();

        // Insert a large number of elements to force the internal delegate to be a LinkedHashMap
        for (int i = 0; i < 100; i++) {
            map.put("k" + i, i);
        }

        assertEquals(100, map.size());

        // Verify a few random look‑ups (case‑insensitive)
        assertEquals(Integer.valueOf(0), map.get("K0"));
        assertEquals(Integer.valueOf(50), map.get("k50"));
        assertEquals(Integer.valueOf(99), map.get("K99"));

        // Verify iteration order is still insertion order
        List<String> expectedOrder = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            expectedOrder.add("k" + i);
        }
        List<String> actualOrder = new ArrayList<>(map.keySet());
        assertEquals(expectedOrder, actualOrder);
    }
}
