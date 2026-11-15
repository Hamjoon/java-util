package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.stream.Collectors;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveMap_copy_Test {

    private TestableCaseInsensitiveMap<String, String> mapUnderTest;

    private TestableCaseInsensitiveMap<String, Integer> mapUnderTestStringInt;

    private TestableCaseInsensitiveMap<String, String> mapUnderTestStringStr;

    private TestableCaseInsensitiveMap<Integer, String> mapUnderTestIntStr;

    private TestableCaseInsensitiveMap<Object, String> mapUnderTestObjStr;

    /**
     * Simple concrete subclass that exposes the protected {@code copy} method.
     */
    private static class TestableCaseInsensitiveMap<K, V> extends CaseInsensitiveMap<K, V> {

        @Override
        public Map<K, V> copy(Map<K, V> source, Map<K, V> dest) {
            return super.copy(source, dest);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        mapUnderTestStringInt = new TestableCaseInsensitiveMap<>();
        mapUnderTestStringStr = new TestableCaseInsensitiveMap<>();
        mapUnderTestIntStr = new TestableCaseInsensitiveMap<>();
        mapUnderTestObjStr = new TestableCaseInsensitiveMap<>();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Reset static cache configuration to avoid side‑effects on other tests.
        CaseInsensitiveMap.setMaxCacheLengthString(100);
    }

    /**
     * Verify that copying a map with plain {@code String} keys converts those keys
     * to the internal {@code CaseInsensitiveString} representation.
     */
    @Test
    void copyConvertsStringKeysToCaseInsensitiveString() throws Exception {
        // source map with plain String keys
        Map<String, String> source = new HashMap<>();
        source.put("One", "1");
        source.put("Two", "2");
        // destination map (empty)
        Map<String, String> dest = new HashMap<>();
        // invoke the method under test
        Map<String, String> result = mapUnderTest.copy(source, dest);
        // the returned map must be the same instance we passed in
        assertSame(dest, result, "copy should return the destination map instance");
        // each key in the destination map must be a CaseInsensitiveString
        Class<?> ciStringClass = Class.forName("com.cedarsoftware.util.CaseInsensitiveMap$CaseInsensitiveString");
        for (String key : dest.keySet()) {
            assertTrue(ciStringClass.isInstance(key), "Key should be an instance of CaseInsensitiveString, but was: " + key.getClass());
        }
        // values must be copied unchanged
        assertEquals("1", dest.get(dest.keySet().iterator().next()));
        assertEquals(2, dest.size());
    }

    /**
     * Verify that when the source map contains a {@code CaseInsensitiveEntry},
     * the original internal key (a {@code CaseInsensitiveString}) is used
     * unchanged in the destination map.
     */
    @Test
    void copyPreservesOriginalKeyWhenSourceEntryIsCaseInsensitiveEntry() throws Exception {
        // Build a source CaseInsensitiveMap that will produce CaseInsensitiveEntry objects
        CaseInsensitiveMap<String, String> sourceMap = new CaseInsensitiveMap<>();
        sourceMap.put("Key", "value");
        // Destination map – we use a plain HashMap to see the exact key object stored
        Map<String, String> dest = new HashMap<>();
        // Perform the copy using the exposed method
        Map<String, String> result = mapUnderTest.copy(sourceMap, dest);
        assertSame(dest, result, "Returned map should be the destination map");
        // The destination should contain exactly one entry
        assertEquals(1, dest.size(), "Destination map should contain a single entry");
        // Retrieve the stored key and verify it is the internal CaseInsensitiveString
        Object storedKey = dest.keySet().iterator().next();
        Class<?> ciStringClass = Class.forName("com.cedarsoftware.util.CaseInsensitiveMap$CaseInsensitiveString");
        assertTrue(ciStringClass.isInstance(storedKey), "Stored key must be the internal CaseInsensitiveString instance");
        // The value must be copied correctly
        assertEquals("value", dest.get(storedKey));
    }

    /**
     * Verify that non‑String keys are copied verbatim without any conversion.
     */
    @Test
    void copyLeavesNonStringKeysUnchanged() {
        Map<Object, String> source = new HashMap<>();
        source.put(42, "answer");
        source.put(true, "yes");
        Map<Object, String> dest = new HashMap<>();
        // Use raw types to avoid generic warnings – the method works for any K,V.
        @SuppressWarnings("unchecked")
        Map<Object, String> result = ((TestableCaseInsensitiveMap) mapUnderTest).copy(source, dest);
        assertSame(dest, result, "Returned map should be the destination map");
        assertEquals(2, dest.size());
        assertEquals("answer", dest.get(42));
        assertEquals("yes", dest.get(true));
    }

    /**
     * Verify that copying an empty source map leaves the destination unchanged.
     */
    @Test
    void copyEmptySourceLeavesDestinationEmpty() {
        Map<String, Integer> source = new CaseInsensitiveMap<>();
        Map<String, Integer> dest = new CaseInsensitiveMap<>();
        Map<String, Integer> result = mapUnderTestStringInt.copy(source, dest);
        assertSame(dest, result, "Returned map must be the destination instance");
        assertTrue(dest.isEmpty(), "Destination map should stay empty");
    }

    /**
     * Verify that plain {@code String} keys are wrapped as {@code CaseInsensitiveString}
     * inside the destination map and that look‑ups are case‑insensitive while iteration
     * returns the original case.
     */
    @Test
    void copySimpleStringKeys() {
        CaseInsensitiveMap<String, Integer> source = new CaseInsensitiveMap<>();
        source.put("Apple", 1);
        source.put("Banana", 2);
        CaseInsensitiveMap<String, Integer> dest = new CaseInsensitiveMap<>();
        mapUnderTestStringInt.copy(source, dest);
        // case‑insensitive look‑ups
        assertEquals(1, dest.get("apple"));
        assertEquals(2, dest.get("BANANA"));
        // iteration returns original case
        Set<String> iteratedKeys = dest.entrySet().stream().map(Map.Entry::getKey).collect(Collectors.toSet());
        assertTrue(iteratedKeys.containsAll(Arrays.asList("Apple", "Banana")), "Iteration must expose the original key strings");
    }

    /**
     * Verify that a map containing heterogeneous key types is copied correctly:
     * String keys become case‑insensitive, other keys are unchanged.
     */
    @Test
    void copyMixedKeyTypes() {
        CaseInsensitiveMap<Object, String> source = new CaseInsensitiveMap<>();
        source.put("Key", "string");
        source.put(5, "int");
        Date date = new Date(0);
        source.put(date, "date");
        CaseInsensitiveMap<Object, String> dest = new CaseInsensitiveMap<>();
        mapUnderTestObjStr.copy(source, dest);
        // String key – case‑insensitive lookup
        assertEquals("string", dest.get("key"));
        assertEquals("string", dest.get("KEY"));
        // Non‑String keys – exact object identity
        assertEquals("int", dest.get(5));
        assertEquals("date", dest.get(date));
        // Ensure the original case of the String key is preserved when iterating
        Object storedKey = dest.keySet().stream().filter(k -> k instanceof String).findFirst().orElseThrow(() -> new AssertionError("String key missing"));
        assertEquals("Key", storedKey);
    }

    /**
     * Verify that entries from the source replace existing entries in the destination
     * when the keys compare equal case‑insensitively, and that new keys are added.
     */
    @Test
    void copyOverwritesExistingEntries() {
        CaseInsensitiveMap<String, Integer> dest = new CaseInsensitiveMap<>();
        // lower‑case key already present
        dest.put("key", 1);
        // unrelated entry
        dest.put("old", 99);
        CaseInsensitiveMap<String, Integer> source = new CaseInsensitiveMap<>();
        // same logical key, different case
        source.put("Key", 100);
        // brand‑new key
        source.put("NEW", 200);
        mapUnderTestStringInt.copy(source, dest);
        assertEquals(3, dest.size(), "Destination should contain three entries");
        // overwritten
        assertEquals(100, dest.get("key"));
        // unchanged
        assertEquals(99, dest.get("old"));
        // newly added
        assertEquals(200, dest.get("new"));
    }

    /**
     * Verify that {@code null} values are copied unchanged.
     */
    @Test
    void copyPreservesNullValues() {
        CaseInsensitiveMap<String, String> source = new CaseInsensitiveMap<>();
        source.put("A", null);
        source.put("B", "notNull");
        CaseInsensitiveMap<String, String> dest = new CaseInsensitiveMap<>();
        mapUnderTestStringStr.copy(source, dest);
        assertTrue(dest.containsKey("A"));
        assertNull(dest.get("A"));
        assertEquals("notNull", dest.get("b"));
    }

    /**
     * Verify that passing a {@code null} source map results in a {@code NullPointerException}.
     */
    @Test
    void copyNullSourceThrowsNPE() {
        CaseInsensitiveMap<String, Integer> dest = new CaseInsensitiveMap<>();
        assertThrows(NullPointerException.class, () -> mapUnderTestStringInt.copy(null, dest), "Copying from a null source should throw NPE");
    }

    /**
     * Verify that a {@code null} String key causes a {@code NullPointerException}
     * when the destination map attempts to wrap the key as a {@code CaseInsensitiveString}.
     */
    @Test
    void copyNullStringKeyThrowsNPE() {
        CaseInsensitiveMap<String, Integer> source = new CaseInsensitiveMap<>();
        // null key of type String
        source.put(null, 5);
        CaseInsensitiveMap<String, Integer> dest = new CaseInsensitiveMap<>();
        assertThrows(NullPointerException.class, () -> mapUnderTestStringInt.copy(source, dest), "Copying a null String key should throw NPE inside the destination's put");
    }
}
