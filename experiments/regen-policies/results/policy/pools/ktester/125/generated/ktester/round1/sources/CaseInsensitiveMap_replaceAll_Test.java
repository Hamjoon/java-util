package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.function.BiFunction;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveMap_replaceAll_Test {

    private CaseInsensitiveMap<String, String> stringMap;

    private CaseInsensitiveMap<Integer, String> intKeyMap;

    @BeforeEach
    void setupBeforeEach() {
        // Map with String keys (case‑insensitive)
        stringMap = new CaseInsensitiveMap<>();
        stringMap.put("One", "1");
        stringMap.put("Two", "2");
        stringMap.put("Three", "3");
        // Map with non‑String keys to verify they are passed through unchanged
        intKeyMap = new CaseInsensitiveMap<>();
        intKeyMap.put(10, "ten");
        intKeyMap.put(20, "twenty");
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset any static configuration that could affect other tests
        CaseInsensitiveMap.setMaxCacheLengthString(100);
    }

    /**
     * Verify that {@code replaceAll} updates the values correctly and that the
     * {@code BiFunction} receives the original (un‑wrapped) keys.
     */
    @Test
    void testReplaceAllUpdatesValuesAndReceivesOriginalKeys() {
        // Capture the keys that the BiFunction receives
        List<Object> receivedKeys = new ArrayList<>();
        BiFunction<String, String, String> updater = (k, v) -> {
            // record the key
            receivedKeys.add(k);
            // new value format
            return k + ":" + v;
        };
        stringMap.replaceAll(updater);
        // Expected values after replacement
        assertEquals("One:1", stringMap.get("ONE"));
        assertEquals("Two:2", stringMap.get("two"));
        assertEquals("Three:3", stringMap.get("ThReE"));
        // The function should have been invoked exactly once per entry
        assertEquals(3, receivedKeys.size());
        // Keys must be the original String instances (case‑preserved), not the internal wrapper
        assertTrue(receivedKeys.containsAll(Arrays.asList("One", "Two", "Three")));
        for (Object key : receivedKeys) {
            assertTrue(key instanceof String, "Key must be a String, not a wrapper");
        }
    }

    /**
     * Verify that {@code replaceAll} works with maps that contain non‑String keys.
     * The keys should be passed to the function unchanged.
     */
    @Test
    void testReplaceAllWithNonStringKeys() {
        List<Object> receivedKeys = new ArrayList<>();
        BiFunction<Integer, String, String> updater = (k, v) -> {
            receivedKeys.add(k);
            return k + "-" + v;
        };
        intKeyMap.replaceAll(updater);
        assertEquals("10-ten", intKeyMap.get(10));
        assertEquals("20-twenty", intKeyMap.get(20));
        assertEquals(2, receivedKeys.size());
        assertTrue(receivedKeys.containsAll(Arrays.asList(10, 20)));
    }

    /**
     * {@code replaceAll} should throw a {@link NullPointerException} when a null
     * function is supplied, mirroring the behaviour of {@link Map#replaceAll}.
     */
    @Test
    void testReplaceAllNullFunctionThrowsNPE() {
        assertThrows(NullPointerException.class, () -> stringMap.replaceAll(null));
    }

    /**
     * After {@code replaceAll}, the map must still honour case‑insensitive look‑ups.
     */
    @Test
    void testReplaceAllPreservesCaseInsensitiveLookup() {
        stringMap.replaceAll((k, v) -> v.toUpperCase());
        // Look‑ups with different casings should still succeed
        assertEquals("1", stringMap.get("ONE"));
        assertEquals("2", stringMap.get("two"));
        assertEquals("3", stringMap.get("ThReE"));
    }

    @Test
    void testReplaceAllCaseInsensitiveStringKeys_UpperAndLower() {
        // Prepare a map with mixed‑case string keys
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("KeyOne", "valueOne");
        map.put("KEYTWO", "valueTwo");
        BiFunction<String, String, String> func = (k, v) -> v.toUpperCase() + "-" + k.toLowerCase();
        map.replaceAll(func);
        // Verify that the original key case is preserved in the map
        assertEquals("VALUEONE-keyone", map.get("keyone"));
        assertEquals("VALUETWO-keytwo", map.get("keytwo"));
    }

    @Test
    void testReplaceAllCaseInsensitiveStringKeys_NullReturn() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("Alpha", "A");
        map.put("Beta", "B");
        BiFunction<String, String, String> func = (k, v) -> null;
        map.replaceAll(func);
        assertNull(map.get("alpha"));
        assertNull(map.get("BETA"));
    }

    @Test
    void testReplaceAllEmptyMap_NoAction() {
        CaseInsensitiveMap<String, Integer> empty = new CaseInsensitiveMap<>();
        BiFunction<String, Integer, Integer> identity = (k, v) -> v;
        // Should not throw and map stays empty
        empty.replaceAll(identity);
        assertTrue(empty.isEmpty());
    }

    @Test
    void testReplaceAllMixedKeyTypes() {
        CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>();
        map.put("Alpha", "ONE");
        map.put(5, "10");
        BiFunction<Object, String, String> func = (k, v) -> k.toString() + "-" + (v == null ? "null" : v);
        map.replaceAll(func);
        assertEquals("Alpha-ONE", map.get("alpha"));
        assertEquals("5-10", map.get(5));
    }

    @Test
    void testReplaceAllFunctionThrowsRuntimeException() {
        BiFunction<String, String, String> exploding = (k, v) -> {
            throw new IllegalStateException("boom");
        };
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> stringMap.replaceAll(exploding));
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void testReplaceAllNullValuesInitially() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("test", null);
        BiFunction<String, String, String> func = (k, v) -> (v == null) ? "filled" : v;
        map.replaceAll(func);
        assertEquals("filled", map.get("TEST"));
    }

    @Test
    void testReplaceAllNullKeySupportedByBackingMap() {
        // LinkedHashMap (default backing) permits null keys
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put(null, "val");
        BiFunction<String, String, String> func = (k, v) -> ((k == null) ? "nullKey" : k) + ":" + v;
        map.replaceAll(func);
        assertEquals("nullKey:val", map.get(null));
    }

    @Test
    void testReplaceAllCaseInsensitiveDuplicateKeys() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("Key", "first");
        // should overwrite first entry but keep original case of first insertion
        map.put("key", "second");
        BiFunction<String, String, String> func = (k, v) -> v + "_" + k.toUpperCase();
        map.replaceAll(func);
        // Only one entry should exist; original case is from first insertion ("Key")
        assertEquals(1, map.size());
        assertTrue(map.containsKey("KEY"));
        assertEquals("second_KEY", map.get("key"));
    }

    @Test
    void testReplaceAllIdentityFunction_NoChange() {
        // Clone the map to compare after replaceAll
        CaseInsensitiveMap<String, String> original = new CaseInsensitiveMap<>(stringMap);
        BiFunction<String, String, String> identity = (k, v) -> v;
        stringMap.replaceAll(identity);
        assertEquals(original, stringMap);
    }

    @Test
    void testReplaceAllOnUnmodifiableBackingMap_ThrowsUOE() throws Exception {
        // Create a normal map and then replace its backing map with an unmodifiable view via reflection
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("A", "a");
        map.put("B", "b");
        // Access the private 'map' field
        Field mapField = CaseInsensitiveMap.class.getDeclaredField("map");
        mapField.setAccessible(true);
        Map<String, String> unmodifiable = Collections.unmodifiableMap(new LinkedHashMap<>(map.getWrappedMap()));
        mapField.set(map, unmodifiable);
        BiFunction<String, String, String> identity = (k, v) -> v;
        assertThrows(UnsupportedOperationException.class, () -> map.replaceAll(identity));
    }

    @Test
    void testReplaceAllOnHashtable_NullValueNotAllowed() {
        // Build a CaseInsensitiveMap that wraps a Hashtable (which disallows null values)
        Hashtable<String, String> hashtable = new Hashtable<>();
        hashtable.put("A", "a");
        hashtable.put("B", "b");
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>(hashtable);
        BiFunction<String, String, String> returnsNull = (k, v) -> null;
        // Hashtable will throw NPE when trying to store null
        assertThrows(NullPointerException.class, () -> map.replaceAll(returnsNull));
    }

    @Test
    void testReplaceAllMismatchedGenericKeyType_ClassCastException() {
        // Create a raw map and insert a String key
        @SuppressWarnings("unchecked")
        CaseInsensitiveMap rawMap = new CaseInsensitiveMap();
        rawMap.put("Key", "value");
        // Cast to a map that *expects* Integer keys
        @SuppressWarnings("unchecked")
        CaseInsensitiveMap<Integer, String> mismatched = rawMap;
        BiFunction<Integer, String, String> func = (k, v) -> v;
        // The unwrap step will try to cast the original String key to Integer, causing CCE
        assertThrows(ClassCastException.class, () -> mismatched.replaceAll(func));
    }
}
