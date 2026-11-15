package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map.Entry;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_lowerEntry_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    // used for the majority of cases
    private ConcurrentNavigableMapNullSafe<Integer, String> intMap;

    // used for the original string‑based tests
    private ConcurrentNavigableMapNullSafe<String, Integer> strMap;

    /* -------------------------------------------------------------
     *  Helper class to make expected‑entry assertions concise
     * ------------------------------------------------------------- */
    private static final class ExpectedEntry<K, V> {

        final K key;

        final V value;

        ExpectedEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        intMap = new ConcurrentNavigableMapNullSafe<>();
        strMap = new ConcurrentNavigableMapNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required
        map = null;
    }

    /**
     * Supplies the test data for the "basic functionality" and "additional edge cases" groups.
     *
     * <p>Each {@code Arguments} instance contains:
     * <ul>
     *   <li>the key to query</li>
     *   <li>the expected entry (or {@code null})</li>
     *   <li>the expected exception class (or {@code null} if no exception is expected)</li>
     * </ul>
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> lowerEntryBasicAndEdgeCases() {
        return Stream.of(// ---- basic functionality -------------------------------------------------
        org.junit.jupiter.params.provider.Arguments.of(5, new ExpectedEntry<>(1, "a"), null), org.junit.jupiter.params.provider.Arguments.of(1, null, null), org.junit.jupiter.params.provider.Arguments.of(7, new ExpectedEntry<>(5, "b"), null), org.junit.jupiter.params.provider.Arguments.of(null, null, null), // ---- additional edge cases -----------------------------------------------
        org.junit.jupiter.params.provider.Arguments.of(0, null, null), org.junit.jupiter.params.provider.Arguments.of(10, new ExpectedEntry<>(5, "b"), null), org.junit.jupiter.params.provider.Arguments.of(Integer.MIN_VALUE, null, null), org.junit.jupiter.params.provider.Arguments.of(Integer.MAX_VALUE, new ExpectedEntry<>(10, "c"), null), // mismatched key types – expect ClassCastException -------------------------
        org.junit.jupiter.params.provider.Arguments.of("", null, ClassCastException.class), org.junit.jupiter.params.provider.Arguments.of(5.0, null, ClassCastException.class), org.junit.jupiter.params.provider.Arguments.of(true, null, ClassCastException.class), org.junit.jupiter.params.provider.Arguments.of("5", null, ClassCastException.class), // duplicate edge case (smallest key) --------------------------------------
        org.junit.jupiter.params.provider.Arguments.of(1, null, null));
    }

    @Test
    void lowerEntryReturnsEntryWithGreatestKeyLessThanGivenKey() {
        // given
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // when
        Entry<String, Integer> entry = map.lowerEntry("b");
        // then
        assertNotNull(entry, "lowerEntry should return a non‑null entry when a lower key exists");
        assertEquals("a", entry.getKey(), "The key of the returned entry should be the greatest key less than the supplied key");
        assertEquals(1, entry.getValue());
    }

    @Test
    void lowerEntryReturnsNullWhenNoLowerKeyExists() {
        // given
        map.put("a", 10);
        map.put("b", 20);
        // when
        Entry<String, Integer> entry = map.lowerEntry("a");
        // then
        assertNull(entry, "lowerEntry should return null when there is no key less than the supplied key");
    }

    @Test
    void lowerEntryHandlesNullKeyAccordingToNullIsConsideredLast() {
        // given
        map.put("x", 100);
        map.put("y", 200);
        // null is treated as the greatest key
        map.put(null, 999);
        // when
        Entry<String, Integer> entry = map.lowerEntry(null);
        // then
        assertNotNull(entry, "lowerEntry(null) should return the greatest non‑null entry");
        assertEquals("y", entry.getKey(), "The entry returned for lowerEntry(null) must be the highest non‑null key");
        assertEquals(200, entry.getValue());
    }

    @Test
    void lowerEntryReturnsNullWhenMapIsEmpty() {
        // when
        Entry<String, Integer> entry = map.lowerEntry("anyKey");
        // then
        assertNull(entry, "lowerEntry on an empty map must return null");
    }

    @Test
    void lowerEntryWorksWithMixedNullAndNonNullValues() {
        // given
        map.put("alpha", null);
        map.put("beta", 2);
        map.put(null, 999);
        // when
        Entry<String, Integer> entry = map.lowerEntry("beta");
        // then
        assertNotNull(entry);
        assertEquals("alpha", entry.getKey());
        assertNull(entry.getValue(), "The value associated with the lower key may be null");
    }

    @ParameterizedTest(name = "[{index}] lowerEntry({0})")
    @MethodSource("lowerEntryBasicAndEdgeCases")
    void lowerEntry_BasicAndEdgeCases(Object key, ExpectedEntry<Integer, String> expected, Class<? extends Throwable> expectedException) {
        // ----------------------------------------------------------------------
        // Arrange – populate the map with the three canonical entries
        // ----------------------------------------------------------------------
        intMap.put(1, "a");
        intMap.put(5, "b");
        intMap.put(10, "c");
        // ----------------------------------------------------------------------
        // Act & Assert
        // ----------------------------------------------------------------------
        if (expectedException != null) {
            // The underlying ConcurrentSkipListMap should throw the exception directly.
            assertThrows(expectedException, () -> intMap.lowerEntry((Integer) key));
            return;
        }
        // Normal (non‑exceptional) path
        @SuppressWarnings("unchecked")
        Entry<Integer, String> result = intMap.lowerEntry((Integer) key);
        if (expected == null) {
            assertNull(result, "lowerEntry should return null for key: " + key);
        } else {
            assertNotNull(result, "lowerEntry must not be null for key: " + key);
            assertEquals(expected.key, result.getKey(), "Returned entry key mismatch for input key: " + key);
            assertEquals(expected.value, result.getValue(), "Returned entry value mismatch for input key: " + key);
        }
    }

    @Test
    void lowerEntryReturnsEntryWithGreatestKeyLessThanGivenKey_StringMap() {
        // given
        strMap.put("a", 1);
        strMap.put("b", 2);
        strMap.put("c", 3);
        // when
        Entry<String, Integer> entry = strMap.lowerEntry("b");
        // then
        assertNotNull(entry, "lowerEntry should return a non‑null entry when a lower key exists");
        assertEquals("a", entry.getKey(), "The key of the returned entry should be the greatest key less than the supplied key");
        assertEquals(1, entry.getValue());
    }

    @Test
    void lowerEntryReturnsNullWhenNoLowerKeyExists_StringMap() {
        // given
        strMap.put("a", 10);
        strMap.put("b", 20);
        // when
        Entry<String, Integer> entry = strMap.lowerEntry("a");
        // then
        assertNull(entry, "lowerEntry should return null when there is no key less than the supplied key");
    }

    @Test
    void lowerEntryHandlesNullKeyAccordingToNullIsConsideredLast_StringMap() {
        // given
        strMap.put("x", 100);
        strMap.put("y", 200);
        // null is treated as the greatest key
        strMap.put(null, 999);
        // when
        Entry<String, Integer> entry = strMap.lowerEntry(null);
        // then
        assertNotNull(entry, "lowerEntry(null) should return the greatest non‑null entry");
        assertEquals("y", entry.getKey(), "The entry returned for lowerEntry(null) must be the highest non‑null key");
        assertEquals(200, entry.getValue());
    }

    @Test
    void lowerEntryReturnsNullWhenMapIsEmpty_StringMap() {
        // when
        Entry<String, Integer> entry = strMap.lowerEntry("anyKey");
        // then
        assertNull(entry, "lowerEntry on an empty map must return null");
    }

    @Test
    void lowerEntryWorksWithMixedNullAndNonNullValues_StringMap() {
        // given
        strMap.put("alpha", null);
        strMap.put("beta", 2);
        strMap.put(null, 999);
        // when
        Entry<String, Integer> entry = strMap.lowerEntry("beta");
        // then
        assertNotNull(entry);
        assertEquals("alpha", entry.getKey());
        assertNull(entry.getValue(), "The value associated with the lower key may be null");
    }

    @Test
    void lowerEntry_throws_NullPointerException_whenInternalMapIsNull() throws Exception {
        // Arrange – force the protected internalMap field to be null via reflection
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(intMap, null);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> intMap.lowerEntry(5));
    }

    @Test
    void lowerEntry_propagates_IllegalArgumentException_fromComparator() throws Exception {
        // Comparator that throws IllegalArgumentException when the key is 42
        Comparator<Integer> throwingComparator = (a, b) -> {
            if (a == 42 || b == 42) {
                throw new IllegalArgumentException("Disallowed key 42");
            }
            return Integer.compare(a, b);
        };
        // Replace the internal map with one that uses the throwing comparator
        ConcurrentSkipListMap<Integer, String> customMap = new ConcurrentSkipListMap<>(throwingComparator);
        customMap.put(10, "ten");
        customMap.put(20, "twenty");
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(intMap, customMap);
        // Act & Assert – the call should propagate the IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> intMap.lowerEntry(42));
    }

    @Test
    void lowerEntry_propagates_RuntimeException_fromComparatorForNegativeKey() throws Exception {
        // Comparator that throws RuntimeException for any negative key
        Comparator<Integer> negativeKeyThrowingComparator = (a, b) -> {
            if (a < 0 || b < 0) {
                throw new RuntimeException("Negative keys not allowed");
            }
            return Integer.compare(a, b);
        };
        // Build a map that uses the above comparator
        ConcurrentSkipListMap<Integer, String> customMap = new ConcurrentSkipListMap<>(negativeKeyThrowingComparator);
        customMap.put(1, "one");
        customMap.put(5, "five");
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(intMap, customMap);
        // Act & Assert – the RuntimeException must be propagated
        assertThrows(RuntimeException.class, () -> intMap.lowerEntry(-1));
    }
}
