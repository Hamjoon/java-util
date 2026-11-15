package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_hashCode_Test {

    private TestMap map;

    /**
     * Concrete implementation used only for testing the abstract class.
     */
    private static class TestMap extends AbstractConcurrentNullSafeMap<String, String> {

        TestMap(ConcurrentHashMap<Object, Object> internal) {
            super(internal);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a real ConcurrentHashMap as the backing store – no mocking needed.
        map = new TestMap(new ConcurrentHashMap<>());
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to guarantee isolation between tests.
        map.clear();
    }

    /**
     * Provides single‑entry scenarios covering null / non‑null keys and values.
     *
     * Each argument consists of:
     *   - key (may be null)
     *   - value (may be null)
     *   - expected hash code for the map containing exactly that entry
     */
    private static Stream<Arguments> singleEntryProvider() {
        return Stream.of(// key = "a", value = "b" → 97 ^ 98 = 3
        Arguments.of("a", "b", "a".hashCode() ^ "b".hashCode()), // key = null, value = "b" → 0 ^ 98 = 98
        Arguments.of(null, "b", 0 ^ "b".hashCode()), // key = "a", value = null → 97 ^ 0 = 97
        Arguments.of("a", null, "a".hashCode() ^ 0), // key = null, value = null → 0 ^ 0 = 0
        Arguments.of(null, null, 0));
    }

    @Test
    void hashCode_emptyMap_returnsZero() {
        assertEquals(0, map.hashCode(), "Hash code of an empty map should be 0");
    }

    @Test
    void hashCode_singleEntry_computesXorOfKeyAndValueHash() {
        map.put("key", "value");
        int expected = "key".hashCode() ^ "value".hashCode();
        assertEquals(expected, map.hashCode(), "Hash code should be the xor of the key and value hash codes");
    }

    @Test
    void hashCode_nullKeyAndNullValue_countsAsZero() {
        map.put(null, null);
        // Both key and value are null → each contributes 0 to the hash.
        assertEquals(0, map.hashCode(), "Hash code with a null key and null value should be 0");
    }

    @Test
    void hashCode_multipleEntries_sumOfXors() {
        map.put("a", "1");
        map.put("b", null);
        map.put(null, "2");
        map.put("c", "3");
        int expected = 0;
        // a -> "a".hashCode() ^ "1".hashCode()
        expected += "a".hashCode() ^ "1".hashCode();
        // b -> "b".hashCode() ^ 0 (null value)
        expected += "b".hashCode() ^ 0;
        // null key -> 0 ^ "2".hashCode()
        expected += 0 ^ "2".hashCode();
        // c -> "c".hashCode() ^ "3".hashCode()
        expected += "c".hashCode() ^ "3".hashCode();
        assertEquals(expected, map.hashCode(), "Hash code should be the sum of xor‑s of each entry's key/value hashes");
    }

    @ParameterizedTest(name = "hashCode_singleEntry[key={0},value={1}] = {2}")
    @MethodSource("singleEntryProvider")
    void hashCode_singleEntry_variousScenarios(String key, String value, int expected) {
        map.put(key, value);
        assertEquals(expected, map.hashCode(), () -> String.format("Map with entry (%s,%s) should yield hash %d", String.valueOf(key), String.valueOf(value), expected));
    }

    /**
     * Test the multiple‑entry scenario described in the JSON test case:
     *   ("a","b"), (null,"c"), ("d",null)
     *
     * Expected hash = (97^98) + (0^99) + (100^0) = 3 + 99 + 100 = 202
     */
    @Test
    void hashCode_multipleEntries_jsonCase() {
        // 97 ^ 98 = 3
        map.put("a", "b");
        // 0 ^ 99 = 99
        map.put(null, "c");
        // 100 ^ 0 = 100
        map.put("d", null);
        int expected = ("a".hashCode() ^ "b".hashCode()) + (0 ^ "c".hashCode()) + ("d".hashCode() ^ 0);
        assertEquals(202, expected, "Pre‑calculated expected hash should be 202");
        assertEquals(expected, map.hashCode(), "Hash code should match the sum of xor‑s for each entry");
    }
}
