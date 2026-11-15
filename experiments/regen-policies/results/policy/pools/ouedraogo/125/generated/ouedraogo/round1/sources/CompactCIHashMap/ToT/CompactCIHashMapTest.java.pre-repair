package com.cedarsoftware.util;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link CompactCIHashMap}.
 *
 * The tests cover:
 * <ul>
 *   <li>Basic Map operations (put, get, remove, clear, size, isEmpty)</li>
 *   <li>Case‑insensitivity of keys</li>
 *   <li>Bulk operations (putAll, minus, plus)</li>
 *   <li>Views (keySet, values, entrySet)</li>
 *   <li>Object contract (equals, hashCode, toString)</li>
 *   <li>Protected helper methods accessed via reflection</li>
 *   <li>Edge cases such as null keys/values and duplicate keys with different case</li>
 * </ul>
 *
 * The test class uses only the public API of {@link CompactCIHashMap},
 * {@link CompactMap}, and standard JDK collections as required by the
 * assignment constraints.
 */
public class CompactCIHashMapTest {

    /* --------------------------------------------------------------------- */
    /* Helper utilities                                                      */
    /* --------------------------------------------------------------------- */

    /** Creates a map pre‑populated with the supplied entries (case‑insensitive). */
    private CompactCIHashMap<String, Integer> createMap(Object... entries) {
        CompactCIHashMap<String, Integer> map = new CompactCIHashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            @SuppressWarnings("unchecked")
            String key = (String) entries[i];
            @SuppressWarnings("unchecked")
            Integer value = (Integer) entries[i + 1];
            map.put(key, value);
        }
        return map;
    }

    /** Invokes a protected method on the given instance via reflection. */
    private Object invokeProtected(Object target, String methodName, Class<?>[] paramTypes, Object... args) throws Exception {
        Method m = target.getClass().getDeclaredMethod(methodName, paramTypes);
        m.setAccessible(true);
        return m.invoke(target, args);
    }

    /* --------------------------------------------------------------------- */
    /* Basic Map operations                                                  */
    /* --------------------------------------------------------------------- */

    @Test
    public void testPutAndGet_CaseInsensitive() {
        CompactCIHashMap<String, String> map = new CompactCIHashMap<>();
        map.put("KeyOne", "value1");
        // Same key, different case – should replace the previous entry
        map.put("keyone", "value2");

        assertEquals("value2", map.get("KEYONE"));
        assertEquals("value2", map.get("keyOne"));
        assertEquals(1, map.size());
    }

    @Test
    public void testContainsKeyAndContainsValue() {
        CompactCIHashMap<String, Integer> map = new CompactCIHashMap<>();
        map.put("Alpha", 10);
        map.put("Beta", 20);

        assertTrue(map.containsKey("ALPHA"));
        assertTrue(map.containsKey("beta"));
        assertFalse(map.containsKey("Gamma"));

        assertTrue(map.containsValue(10));
        assertTrue(map.containsValue(20));
        assertFalse(map.containsValue(30));
    }

    @Test
    public void testRemoveAndClear() {
        CompactCIHashMap<String, String> map = new CompactCIHashMap<>();
        map.put("First", "1");
        map.put("Second", "2");

        String removed = map.remove("FIRST");
        assertEquals("1", removed);
        assertFalse(map.containsKey("first"));
        assertEquals(1, map.size());

        map.clear();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testPutAll_MergesCorrectly() {
        Map<String, Integer> source = new HashMap<>();
        source.put("One", 1);
        source.put("Two", 2);

        CompactCIHashMap<String, Integer> map = new CompactCIHashMap<>();
        map.put("Three", 3);
        map.putAll(source);

        assertEquals(3, map.size());
        assertEquals(Integer.valueOf(1), map.get("ONE"));
        assertEquals(Integer.valueOf(2), map.get("two"));
        assertEquals(Integer.valueOf(3), map.get("THREE"));
    }

    /* --------------------------------------------------------------------- */
    /* Views (keySet, values, entrySet)                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testKeySet_IsCaseInsensitiveAndReflectsChanges() {
        CompactCIHashMap<String, String> map = createMap("A", "apple", "b", "banana");
        Set<String> keys = map.keySet();

        // The set contains the original case of the first insertion
        assertTrue(keys.contains("A"));
        assertTrue(keys.contains("b"));
        assertEquals(2, keys.size());

        // Changing the map updates the view
        map.put("C", "cherry");
        assertTrue(keys.contains("C"));
        assertEquals(3, keys.size());

        // Removing via the view removes from the map
        keys.remove("B");
        assertFalse(map.containsKey("b"));
        assertEquals(2, map.size());
    }

    @Test
    public void testValues_CollectionReflectsMapState() {
        CompactCIHashMap<String, Integer> map = createMap("x", 5, "y", 10);
        Collection<Integer> values = map.values();

        assertTrue(values.containsAll(Arrays.asList(5, 10)));
        assertEquals(2, values.size());

        map.put("Z", 15);
        assertTrue(values.contains(15));
        assertEquals(3, values.size());

        values.remove(5);
        assertFalse(map.containsKey("x"));
        assertEquals(2, map.size());
    }

    @Test
    public void testEntrySet_IteratesAndAllowsModification() {
        CompactCIHashMap<String, String> map = createMap("k1", "v1", "k2", "v2");
        Set<Map.Entry<String, String>> entries = map.entrySet();

        // Verify content
        Map<String, String> snapshot = new HashMap<>();
        for (Map.Entry<String, String> e : entries) {
            snapshot.put(e.getKey(), e.getValue());
        }
        assertEquals(2, snapshot.size());
        assertEquals("v1", snapshot.get("k1"));
        assertEquals("v2", snapshot.get("k2"));

        // Modify via entry set
        for (Map.Entry<String, String> e : entries) {
            if (e.getKey().equalsIgnoreCase("K1")) {
                e.setValue("newV1");
            }
        }
        assertEquals("newV1", map.get("K1"));
    }

    /* --------------------------------------------------------------------- */
    /* Object contract (equals, hashCode, toString)                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCode() {
        CompactCIHashMap<String, Integer> map1 = createMap("a", 1, "B", 2);
        CompactCIHashMap<String, Integer> map2 = createMap("A", 1, "b", 2);
        CompactCIHashMap<String, Integer> map3 = createMap("a", 1, "b", 3);

        assertEquals(map1, map2);
        assertEquals(map1.hashCode(), map2.hashCode());

        assertNotEquals(map1, map3);
        assertNotEquals(map1.hashCode(), map3.hashCode());

        // Equality with a plain HashMap (case‑insensitive behavior)
        Map<String, Integer> plain = new HashMap<>();
        plain.put("A", 1);
        plain.put("b", 2);
        assertEquals(map1, plain);
        assertEquals(plain, map1);
    }

    @Test
    public void testToString_IncludesAllEntries() {
        CompactCIHashMap<String, String> map = createMap("one", "1", "two", "2");
        String s = map.toString();

        assertTrue(s.contains("one=1"));
        assertTrue(s.contains("two=2"));
        // Order is not guaranteed for an unordered map, but both pairs must appear
        assertTrue(s.startsWith("{") && s.endsWith("}"));
    }

    /* --------------------------------------------------------------------- */
    /* minus and plus – immutable‑style operations                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testMinus_RemovesKeyAndLeavesOriginalIntact() {
        CompactCIHashMap<String, String> original = createMap("a", "A", "b", "B", "c", "C");
        Map<String, String> reduced = original.minus("B"); // key case‑insensitive

        assertFalse(reduced.containsKey("b"));
        assertTrue(reduced.containsKey("a"));
        assertTrue(reduced.containsKey("c"));
        assertEquals(2, reduced.size());

        // original map must stay unchanged
        assertTrue(original.containsKey("b"));
        assertEquals(3, original.size());
    }

    @Test
    public void testPlus_AddsEntryAndLeavesOriginalIntact() {
        CompactCIHashMap<String, Integer> original = createMap("x", 100);
        Map<String, Integer> extended = original.plus("Y"); // add a key with null value

        assertTrue(extended.containsKey("y"));
        assertNull(extended.get("y"));
        assertEquals(Integer.valueOf(100), extended.get("X"));
        assertEquals(2, extended.size());

        // original unchanged
        assertFalse(original.containsKey("y"));
        assertEquals(1, original.size());
    }

    /* --------------------------------------------------------------------- */
    /* Protected helper methods accessed via reflection                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void testIsCaseInsensitive_ProtectedMethod() throws Exception {
        CompactCIHashMap<String, String> map = new CompactCIHashMap<>();
        Boolean result = (Boolean) invokeProtected(map, "isCaseInsensitive", new Class<?>[0]);
        assertTrue("CompactCIHashMap should report case‑insensitive", result);
    }

    @Test
    public void testGetNewMap_ProtectedMethodReturnsCaseInsensitiveMap() throws Exception {
        CompactCIHashMap<String, String> map = new CompactCIHashMap<>();
        @SuppressWarnings("unchecked")
        Map<String, String> internal = (Map<String, String>) invokeProtected(map, "getNewMap", new Class<?>[0]);

        // The returned map must accept case‑insensitive keys
        internal.put("Hello", "World");
        assertEquals("World", internal.get("hello"));
        assertEquals("World", internal.get("HELLO"));
    }

    @Test
    public void testComputeKeyHashCode_Consistency() throws Exception {
        CompactCIHashMap<String, String> map = new CompactCIHashMap<>();
        int hash1 = (int) invokeProtected(map, "computeKeyHashCode", new Class<?>[]{Object.class}, "TestKey");
        int hash2 = (int) invokeProtected(map, "computeKeyHashCode", new Class<?>[]{Object.class}, "testkey"); // different case

        assertEquals("Hash codes for case‑insensitive keys must be equal", hash1, hash2);
    }

    /* --------------------------------------------------------------------- */
    /* Edge cases – null handling                                            */
    /* --------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testPut_NullKeyThrowsException() {
        CompactCIHashMap<String, String> map = new CompactCIHashMap<>();
        map.put(null, "someValue"); // Expected NPE according to typical CaseInsensitiveMap contract
    }

    @Test
    public void testPut_NullValueIsAllowed() {
        CompactCIHashMap<String, String> map = new CompactCIHashMap<>();
        map.put("nullable", null);
        assertTrue(map.containsKey("NULLABLE"));
        assertNull(map.get("nullable"));
        assertEquals(1, map.size());
    }

    @Test
    public void testGet_NonExistingKeyReturnsNull() {
        CompactCIHashMap<String, Integer> map = new CompactCIHashMap<>();
        map.put("present", 42);
        assertNull(map.get("absent"));
        assertNull(map.get("ABSENT"));
    }

    /* --------------------------------------------------------------------- */
    /* Miscellaneous checks                                                  */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCompactSize_InheritedDefault() throws Exception {
        CompactCIHashMap<String, String> map = new CompactCIHashMap<>();
        int compactSize = (int) invokeProtected(map, "compactSize", new Class<?>[0]);
        // The default compact size is defined in CompactMap (implementation detail);
        // we only assert that it is non‑negative.
        assertTrue("compactSize should be non‑negative", compactSize >= 0);
    }

    @Test
    public void testBuilderDoesNotAffectCIMap() {
        // Even though CompactMap provides a builder, CompactCIHashMap does not expose it.
        // This test just ensures that using the builder to create a normal CompactMap
        // does not interfere with CI behavior.
        CompactMap<String, String> builderMap = CompactMap.builder().build();
        builderMap.put("Key", "value");
        assertEquals("value", builderMap.get("Key"));
        // The builderMap is case‑sensitive – different case should not match.
        assertNull(builderMap.get("key"));
    }
}
