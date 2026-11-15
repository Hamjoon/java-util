package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.function.BiConsumer;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveMap_forEach_Test {

    private CaseInsensitiveMap<String, String> stringMap;

    private CaseInsensitiveMap<Object, String> mixedMap;

    @BeforeAll
    static void setupBeforeAll() {
        // Ensure the cache length is at its default for all tests
        CaseInsensitiveMap.setMaxCacheLengthString(100);
    }

    @BeforeEach
    void setupBeforeEach() {
        // Map used for String‑key tests
        stringMap = new CaseInsensitiveMap<>();
        stringMap.put("One", "Alpha");
        stringMap.put("TWO", "Beta");
        stringMap.put("ThReE", "Gamma");
        // Map used for non‑String key tests
        mixedMap = new CaseInsensitiveMap<>();
        mixedMap.put(1, "One");
        mixedMap.put("Key", "Value");
        mixedMap.put(null, "NullValue");
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear maps to avoid cross‑test contamination
        if (stringMap != null) {
            stringMap.clear();
        }
        if (mixedMap != null) {
            mixedMap.clear();
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // Reset any static configuration that might have been changed by other tests
        CaseInsensitiveMap.setMaxCacheLengthString(100);
    }

    /**
     * Helper that injects a raw {@link CaseInsensitiveMap.CaseInsensitiveString}
     * instance into the backing map. This simulates a map that already contains
     * the wrapper (e.g., after deserialization or manual manipulation).
     */
    private void injectCaseInsensitiveStringKey(CaseInsensitiveMap<String, String> map, String rawKey, String value) throws Exception {
        // Obtain the internal map via the public accessor
        Map<?, ?> backing = map.getWrappedMap();
        // Create the wrapper via reflection (constructor is public)
        CaseInsensitiveMap.CaseInsensitiveString cis = new CaseInsensitiveMap.CaseInsensitiveString(rawKey);
        // Use reflection to bypass generic type safety and put the wrapper directly
        @SuppressWarnings("unchecked")
        Map<Object, Object> rawMap = (Map<Object, Object>) backing;
        rawMap.put(cis, value);
    }

    @Test
    void testForEachPreservesOriginalStringKeyCase() {
        // Capture the keys and values seen by the BiConsumer
        List<String> observedKeys = new ArrayList<>();
        List<String> observedValues = new ArrayList<>();
        BiConsumer<? super String, ? super String> consumer = (k, v) -> {
            observedKeys.add(k);
            observedValues.add(v);
        };
        // Act
        assertDoesNotThrow(() -> stringMap.forEach(consumer));
        // Verify that each entry was visited exactly once
        assertEquals(3, observedKeys.size(), "forEach should visit three entries");
        assertEquals(3, observedValues.size());
        // The order of iteration for LinkedHashMap (the default backing map) is insertion order
        List<String> expectedKeys = Arrays.asList("One", "TWO", "ThReE");
        List<String> expectedValues = Arrays.asList("Alpha", "Beta", "Gamma");
        assertEquals(expectedKeys, observedKeys, "Keys should be the original case as inserted");
        assertEquals(expectedValues, observedValues, "Values should match the inserted values");
        // Additionally verify case‑insensitive access still works after forEach
        assertEquals("Alpha", stringMap.get("one"));
        assertEquals("Beta", stringMap.get("two"));
        assertEquals("Gamma", stringMap.get("three"));
    }

    @Test
    void testForEachWithNonStringKeys() {
        // Capture entries visited by forEach
        List<Object> observedKeys = new ArrayList<>();
        List<String> observedValues = new ArrayList<>();
        BiConsumer<? super Object, ? super String> consumer = (k, v) -> {
            observedKeys.add(k);
            observedValues.add(v);
        };
        // Act
        assertDoesNotThrow(() -> mixedMap.forEach(consumer));
        // Verify that all three entries were visited
        assertEquals(3, observedKeys.size());
        assertEquals(3, observedValues.size());
        // Because the default backing map is a LinkedHashMap, iteration order follows insertion order
        List<Object> expectedKeys = Arrays.asList(1, "Key", null);
        List<String> expectedValues = Arrays.asList("One", "Value", "NullValue");
        assertEquals(expectedKeys, observedKeys, "Non‑String keys should be emitted unchanged");
        assertEquals(expectedValues, observedValues);
    }

    @Test
    void testForEachDoesNotExposeCaseInsensitiveStringWrapper() {
        // The BiConsumer should receive the original key objects, not the internal wrapper.
        // We'll use a flag to ensure the key instance is a String (or the original non‑String type).
        BiConsumer<? super String, ? super String> consumer = (k, v) -> {
            // The key must be an instance of String, not of the internal wrapper class
            assertTrue(k instanceof String, "Key should be a plain String");
            assertFalse(k.getClass().getName().contains("CaseInsensitiveString"), "Key should not be the internal wrapper class");
        };
        assertDoesNotThrow(() -> stringMap.forEach(consumer));
    }

    @Test
    void testUnwrapCaseInsensitiveStringKey() throws Exception {
        // Create a fresh map with no entries
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        // Manually insert a wrapper key "TeSt"
        injectCaseInsensitiveStringKey(map, "TeSt", "value");
        List<String> recordedKeys = new ArrayList<>();
        BiConsumer<? super String, ? super String> consumer = (k, v) -> recordedKeys.add(k);
        map.forEach(consumer);
        assertEquals(Collections.singletonList("TeSt"), recordedKeys, "The consumer should receive the original String, not the wrapper");
    }

    @Test
    void testEmptyMapForEachInvokesActionZeroTimes() {
        CaseInsensitiveMap<String, String> empty = new CaseInsensitiveMap<>();
        List<Object> recordedKeys = new ArrayList<>();
        BiConsumer<? super String, ? super String> consumer = (k, v) -> recordedKeys.add(k);
        empty.forEach(consumer);
        assertTrue(recordedKeys.isEmpty(), "Empty map should result in no consumer invocations");
    }

    @Test
    void testNormalStringKeyIsForwardedUnchanged() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("Hello", "world");
        List<String> recordedPairs = new ArrayList<>();
        BiConsumer<? super String, ? super String> consumer = (k, v) -> recordedPairs.add(k + "=" + v);
        map.forEach(consumer);
        assertEquals(Collections.singletonList("Hello=world"), recordedPairs, "The key/value pair should be recorded exactly as inserted");
    }

    @Test
    void testMixedStringAndCaseInsensitiveStringKeys() throws Exception {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        // regular key
        map.put("Alpha", "A");
        // manually insert a wrapper key "Beta"
        injectCaseInsensitiveStringKey(map, "Beta", "B");
        List<String> recordedKeys = new ArrayList<>();
        BiConsumer<? super String, ? super String> consumer = (k, v) -> recordedKeys.add(k);
        map.forEach(consumer);
        // Insertion order is preserved (LinkedHashMap default)
        List<String> expected = Arrays.asList("Alpha", "Beta");
        assertEquals(expected, recordedKeys, "Both normal and unwrapped keys should appear in original case");
    }

    @Test
    void testNonStringKeyIsPassedThroughUnchanged() {
        CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>();
        map.put(42, "answer");
        List<Object> recordedKeys = new ArrayList<>();
        BiConsumer<? super Object, ? super String> consumer = (k, v) -> recordedKeys.add(k);
        map.forEach(consumer);
        assertEquals(Collections.singletonList(42), recordedKeys, "Integer key must be delivered unchanged");
    }

    @Test
    void testNullKeyIsHandledGracefully() {
        CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>();
        map.put(null, "nullValue");
        List<Object> recordedKeys = new ArrayList<>();
        BiConsumer<? super Object, ? super String> consumer = (k, v) -> recordedKeys.add(k);
        map.forEach(consumer);
        assertEquals(Collections.singletonList(null), recordedKeys, "Null key should be passed through without unwrapping");
    }

    @Test
    void testNullActionThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> stringMap.forEach(null), "Passing a null consumer must throw NPE as per Map.forEach contract");
    }

    @Test
    void testActionThrowsExceptionIsPropagated() {
        BiConsumer<? super String, ? super String> badConsumer = (k, v) -> {
            throw new IllegalStateException("boom");
        };
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> stringMap.forEach(badConsumer), "Exception thrown by the consumer must propagate");
        assertEquals("boom", ex.getMessage());
    }
}
