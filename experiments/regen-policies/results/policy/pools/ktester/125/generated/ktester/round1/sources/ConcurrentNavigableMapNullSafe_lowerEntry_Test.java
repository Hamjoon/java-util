package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentSkipListMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_lowerEntry_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    /**
     * Creates a fresh map for each test and populates it with a deterministic
     * set of entries.  The ordering is natural (String implements Comparable).
     * A {@code null} key is also added to verify the special “null‑last” handling.
     */
    @BeforeEach
    void setupBeforeEach() {
        map = new ConcurrentNavigableMapNullSafe<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // null key is considered last
        map.put(null, 100);
    }

    @AfterEach
    void teardownAfterEach() {
        // The map will be garbage‑collected; explicit cleanup not required.
        map.clear();
    }

    private static Stream<Arguments> basicCases() {
        return Stream.of(// lowerEntry("b") → entry "a"
        Arguments.of("b", "a", 1), // lowerEntry("a") → null
        Arguments.of("a", null, null), // lowerEntry("d") → entry "c"
        Arguments.of("d", "c", 3), // lowerEntry("") (lexicographically before "a") → null
        Arguments.of("", null, null));
    }

    @Test
    void lowerEntry_WithExistingKey_ReturnsGreatestKeyLessThanGiven() {
        // “b” → should return entry for “a”
        Map.Entry<String, Integer> entry = map.lowerEntry("b");
        assertNotNull(entry, "lowerEntry(\"b\") should not be null");
        assertEquals("a", entry.getKey());
        assertEquals(1, entry.getValue());
        // “c” → should return entry for “b”
        entry = map.lowerEntry("c");
        assertNotNull(entry);
        assertEquals("b", entry.getKey());
        assertEquals(2, entry.getValue());
        // “d” (greater than any existing non‑null key) → should return entry for “c”
        entry = map.lowerEntry("d");
        assertNotNull(entry);
        assertEquals("c", entry.getKey());
        assertEquals(3, entry.getValue());
    }

    @Test
    void lowerEntry_WithFirstKey_ReturnsNull() {
        // “a” is the smallest non‑null key; there is nothing lower.
        Map.Entry<String, Integer> entry = map.lowerEntry("a");
        assertNull(entry, "lowerEntry(\"a\") should be null because no lower key exists");
    }

    @Test
    void lowerEntry_WithNullKey_ReturnsEntryForGreatestNonNullKey() {
        // According to the class contract, null keys are considered last.
        // Therefore lowerEntry(null) should give the greatest non‑null key.
        Map.Entry<String, Integer> entry = map.lowerEntry(null);
        assertNotNull(entry);
        assertEquals("c", entry.getKey());
        assertEquals(3, entry.getValue());
    }

    @Test
    void lowerEntry_WithKeyNotPresentButWithinRange_ReturnsCorrectEntry() {
        // Key “bb” lies between “b” and “c”.
        Map.Entry<String, Integer> entry = map.lowerEntry("bb");
        assertNotNull(entry);
        assertEquals("b", entry.getKey());
        assertEquals(2, entry.getValue());
    }

    @Test
    void lowerEntry_WithKeyLessThanAllNonNullKeys_ReturnsNull() {
        // Any key that sorts before “a” (e.g., empty string) should yield null.
        Map.Entry<String, Integer> entry = map.lowerEntry("");
        assertNull(entry);
    }

    @ParameterizedTest(name = "lowerEntry({0}) → {1}")
    @MethodSource("basicCases")
    void lowerEntry_BasicFunctionality(String key, String expectedKey, Integer expectedValue) {
        Map.Entry<String, Integer> entry = map.lowerEntry(key);
        if (expectedKey == null) {
            assertNull(entry, "lowerEntry(\"" + key + "\") should be null");
        } else {
            assertNotNull(entry, "lowerEntry(\"" + key + "\") should not be null");
            assertEquals(expectedKey, entry.getKey(), "Unexpected key returned for lowerEntry(\"" + key + "\")");
            assertEquals(expectedValue, entry.getValue(), "Unexpected value returned for lowerEntry(\"" + key + "\")");
        }
    }

    @Test
    void lowerEntry_WithNullKey_ReturnsGreatestNonNullKey() {
        // According to the class contract, null keys are considered last.
        // Therefore lowerEntry(null) should give the greatest non‑null key.
        Map.Entry<String, Integer> entry = map.lowerEntry(null);
        assertNotNull(entry, "lowerEntry(null) should not be null when map contains non‑null keys");
        assertEquals("c", entry.getKey());
        assertEquals(3, entry.getValue());
    }

    @Test
    void lowerEntry_WithExistingNullEntry_AndKeyGreaterThanNull_ReturnsGreatestNonNull() {
        // The map already contains a null key (value 100). For any non‑null key,
        // lowerEntry should ignore the null key because it is considered the
        // highest key in natural order.
        Map.Entry<String, Integer> entry = map.lowerEntry("b");
        assertNotNull(entry);
        assertEquals("a", entry.getKey());
        assertEquals(1, entry.getValue());
    }

    @Test
    void lowerEntry_WithReverseComparator_OrdersAccordingly() {
        Comparator<String> reverse = Comparator.reverseOrder();
        ConcurrentNavigableMapNullSafe<String, Integer> revMap = new ConcurrentNavigableMapNullSafe<>(reverse);
        revMap.put("a", 1);
        revMap.put("b", 2);
        revMap.put("c", 3);
        // In reverse order: "c" < "b" < "a". lowerEntry("b") should give "c".
        Map.Entry<String, Integer> entry = revMap.lowerEntry("b");
        assertNotNull(entry);
        assertEquals("c", entry.getKey());
        assertEquals(3, entry.getValue());
        // "c" is the smallest key in reverse order → no lower entry.
        assertNull(revMap.lowerEntry("c"));
    }

    @Test
    void lowerEntry_OnEmptyMap_ReturnsNull() {
        ConcurrentNavigableMapNullSafe<String, Integer> empty = new ConcurrentNavigableMapNullSafe<>();
        assertNull(empty.lowerEntry("any"));
    }

    @Test
    void lowerEntry_ReturnedEntryIsLive_MutationPropagates() {
        // should be entry for "a"
        Map.Entry<String, Integer> entry = map.lowerEntry("b");
        assertNotNull(entry);
        assertEquals("a", entry.getKey());
        assertEquals(1, entry.getValue());
        // Mutate via setValue and verify the map sees the change.
        entry.setValue(999);
        assertEquals(999, map.get("a"));
    }

    @Test
    void lowerEntry_WithIncompatibleKeyType_ThrowsClassCastException() {
        // The map is typed to String keys; passing an Integer should trigger
        // ConcurrentSkipListMap's runtime type check.
        assertThrows(ClassCastException.class, () -> map.lowerEntry((String) (Object) 123));
    }

    @Test
    void lowerEntry_WithNullEntry_VerifyContractBehaviour() {
        // The map already contains a null entry (value 100). According to the
        // documented contract (null considered last), lowerEntry("b") must
        // return the entry for "a", not the null entry.
        Map.Entry<String, Integer> entry = map.lowerEntry("b");
        assertNotNull(entry);
        assertEquals("a", entry.getKey());
        assertEquals(1, entry.getValue());
    }
}
