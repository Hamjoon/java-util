package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TrackingMap_putAll_Test {

    /**
     * Used for the mock‑based test.
     */
    @Mock
    private Map<String, Integer> mockBackingMap;

    /**
     * Auto‑closeable for {@link MockitoAnnotations#openMocks(Object)}.
     */
    private AutoCloseable mocks;

    /**
     * Normal behaviour – various insertion scenarios.
     */
    @Nested
    class NormalBehavior {

        /**
         * Helper to create a TrackingMap pre‑populated with the given entries.
         */
        private TrackingMap<String, Integer> createTrackingMap(Map<String, Integer> initial) {
            Map<String, Integer> backing = new HashMap<>(initial);
            return new TrackingMap<>(backing);
        }

        @Test
        void putAllCopiesAllEntriesFromNonEmptyMap() {
            // GIVEN
            TrackingMap<String, Integer> tm = createTrackingMap(Map.of("apple", 1, "banana", 2));
            Map<String, Integer> source = Map.of("apple", 1, "banana", 2);
            // WHEN
            tm.putAll(source);
            // THEN
            assertEquals(2, tm.size());
            assertEquals(1, tm.get("apple"));
            assertEquals(2, tm.get("banana"));
        }

        @Test
        void putAllOverwritesExistingKeyAndAddsNewKey() {
            // GIVEN
            TrackingMap<String, Integer> tm = createTrackingMap(Map.of("apple", 10, "banana", 2));
            Map<String, Integer> source = Map.of("apple", 10, "cherry", 3);
            // WHEN
            tm.putAll(source);
            // THEN
            assertEquals(3, tm.size());
            assertEquals(10, tm.get("apple"));
            assertEquals(2, tm.get("banana"));
            assertEquals(3, tm.get("cherry"));
        }

        @Test
        void putAllWithEmptyMapLeavesTrackingMapUnchanged() {
            // GIVEN
            TrackingMap<String, Integer> tm = createTrackingMap(Map.of("apple", 10, "banana", 2, "cherry", 3));
            Map<String, Integer> empty = Collections.emptyMap();
            // WHEN
            tm.putAll(empty);
            // THEN
            assertEquals(3, tm.size());
            assertEquals(10, tm.get("apple"));
            assertEquals(2, tm.get("banana"));
            assertEquals(3, tm.get("cherry"));
        }

        @Test
        void putAllAcceptsNullKeyWhenBackingMapAllowsIt() {
            // GIVEN
            TrackingMap<String, Integer> tm = createTrackingMap(Map.of("apple", 1));
            Map<String, Integer> source = new HashMap<>();
            source.put(null, 99);
            // WHEN
            tm.putAll(source);
            // THEN
            assertEquals(2, tm.size());
            assertTrue(tm.containsKey(null));
            assertEquals(99, tm.get(null));
            assertEquals(1, tm.get("apple"));
        }

        @Test
        void putAllAcceptsNullValue() {
            // GIVEN
            TrackingMap<String, Integer> tm = createTrackingMap(Map.of("apple", 1));
            Map<String, Integer> source = new HashMap<>();
            source.put("date", null);
            // WHEN
            tm.putAll(source);
            // THEN
            assertEquals(2, tm.size());
            assertTrue(tm.containsKey("date"));
            assertNull(tm.get("date"));
            assertEquals(1, tm.get("apple"));
        }

        @Test
        void putAllWithNullKeyAndNullValue() {
            // GIVEN
            TrackingMap<String, Integer> tm = createTrackingMap(Map.of("apple", 1));
            Map<String, Integer> source = new HashMap<>();
            source.put(null, null);
            // WHEN
            tm.putAll(source);
            // THEN
            assertEquals(2, tm.size());
            assertTrue(tm.containsKey(null));
            assertNull(tm.get(null));
        }

        @Test
        void putAllBulkInsertionAddsManyEntries() {
            // GIVEN
            TrackingMap<String, Integer> tm = createTrackingMap(Map.of("apple", 1));
            Map<String, Integer> bulk = new HashMap<>();
            for (int i = 0; i <= 20; i++) {
                bulk.put("k" + i, i);
            }
            // WHEN
            tm.putAll(bulk);
            // THEN
            // 1 original + 21 new
            assertEquals(22, tm.size());
            assertEquals(1, tm.get("apple"));
            for (int i = 0; i <= 20; i++) {
                assertEquals(i, tm.get("k" + i));
            }
        }

        @Test
        void putAllOverwriteWithSameValueLeavesMapUnchanged() {
            // GIVEN
            TrackingMap<String, Integer> tm = createTrackingMap(Map.of("apple", 10, "banana", 2, "cherry", 3));
            Map<String, Integer> source = Map.of("banana", 2, "date", 4);
            // WHEN
            tm.putAll(source);
            // THEN
            assertEquals(4, tm.size());
            assertEquals(10, tm.get("apple"));
            assertEquals(2, tm.get("banana"));
            assertEquals(3, tm.get("cherry"));
            assertEquals(4, tm.get("date"));
        }
    }

    /**
     * Exception‑related test cases.
     */
    @Nested
    class ExceptionCases {

        @Test
        void putAllNullArgumentThrowsNPE() {
            TrackingMap<String, Integer> tm = new TrackingMap<>(new HashMap<>());
            assertThrows(NullPointerException.class, () -> tm.putAll(null));
        }

        @Test
        void putAllWithIncompatibleKeyTypeThrowsClassCastException() {
            // TrackingMap<String,Integer> but source map has Integer key.
            TrackingMap<String, Integer> tm = new TrackingMap<>(new HashMap<>());
            @SuppressWarnings("unchecked")
            Map rawMap = new HashMap<>();
            // key is Integer
            rawMap.put(123, 456);
            assertThrows(ClassCastException.class, () -> tm.putAll(rawMap));
        }

        @Test
        void putAllWithIncompatibleValueTypeThrowsClassCastException() {
            // TrackingMap<String,Integer> but source map has String value.
            TrackingMap<String, Integer> tm = new TrackingMap<>(new HashMap<>());
            @SuppressWarnings("unchecked")
            Map rawMap = new HashMap<>();
            // value is String
            rawMap.put("apple", "not-an-integer");
            assertThrows(ClassCastException.class, () -> tm.putAll(rawMap));
        }

        @Test
        void putAllOnUnmodifiableBackingMapThrowsUnsupportedOperationException() {
            Map<String, Integer> unmodifiable = Collections.unmodifiableMap(new HashMap<>());
            TrackingMap<String, Integer> tm = new TrackingMap<>(unmodifiable);
            Map<String, Integer> source = Map.of("newKey", 1);
            assertThrows(UnsupportedOperationException.class, () -> tm.putAll(source));
        }

        @Test
        void putAllWithNullKeyOnTreeMapThrowsNullPointerException() {
            // TreeMap does not allow null keys.
            TrackingMap<String, Integer> tm = new TrackingMap<>(new TreeMap<>());
            Map<String, Integer> source = new HashMap<>();
            source.put(null, 99);
            assertThrows(NullPointerException.class, () -> tm.putAll(source));
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise @Mock fields.
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Release mock resources.
        mocks.close();
    }

    /**
     * Verify that {@code TrackingMap.putAll(..)} forwards the call to the wrapped map.
     */
    @Test
    void putAllDelegatesToWrappedMap() {
        // GIVEN
        TrackingMap<String, Integer> trackingMap = new TrackingMap<>(mockBackingMap);
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);
        // WHEN
        trackingMap.putAll(source);
        // THEN
        verify(mockBackingMap, times(1)).putAll(source);
        // No other interactions should have happened.
        verifyNoMoreInteractions(mockBackingMap);
    }

    /**
     * When entries are added via {@code putAll}, they are *not* marked as accessed.
     * Consequently a subsequent call to {@code expungeUnused()} removes them.
     */
    @Test
    void putAllAddsEntriesButDoesNotMarkAsUsed() {
        // GIVEN
        Map<String, Integer> backing = new HashMap<>();
        TrackingMap<String, Integer> trackingMap = new TrackingMap<>(backing);
        Map<String, Integer> toAdd = new HashMap<>();
        toAdd.put("x", 10);
        toAdd.put("y", 20);
        // WHEN
        trackingMap.putAll(toAdd);
        // THEN – entries are present before expunge
        assertEquals(2, trackingMap.size());
        assertTrue(trackingMap.containsKey("x"));
        assertTrue(trackingMap.containsKey("y"));
        // WHEN – expunge unused entries
        trackingMap.expungeUnused();
        // THEN – map should be empty because putAll does not count as an access
        assertEquals(0, trackingMap.size());
        assertTrue(trackingMap.isEmpty());
        // The backing map should also be empty (the same instance is used internally)
        assertTrue(backing.isEmpty());
    }

    /**
     * If some keys are accessed after a {@code putAll}, only those accessed remain
     * after {@code expungeUnused()}.
     */
    @Test
    void putAllThenAccessSomeKeys() {
        // GIVEN
        TrackingMap<String, String> trackingMap = new TrackingMap<>(new HashMap<>());
        Map<String, String> source = Map.of("alpha", "A", "beta", "B", "gamma", "C");
        trackingMap.putAll(source);
        // Access "beta" – this should mark it as used.
        assertEquals("B", trackingMap.get("beta"));
        // WHEN
        trackingMap.expungeUnused();
        // THEN – only "beta" should remain.
        assertEquals(1, trackingMap.size());
        assertTrue(trackingMap.containsKey("beta"));
        assertFalse(trackingMap.containsKey("alpha"));
        assertFalse(trackingMap.containsKey("gamma"));
    }

    /**
     * Idempotent behaviour – putting a map that does not change any entry.
     */
    @Test
    void putAllIdempotentWhenMapContainsIdenticalEntries() {
        // GIVEN
        Map<String, Integer> initial = new HashMap<>();
        initial.put("apple", 10);
        TrackingMap<String, Integer> tm = new TrackingMap<>(initial);
        // identical entry
        Map<String, Integer> source = Map.of("apple", 10);
        // WHEN
        tm.putAll(source);
        // THEN – map unchanged
        assertEquals(1, tm.size());
        assertEquals(10, tm.get("apple"));
    }

    /**
     * Self‑putAll – the map should remain unchanged and no exception should be thrown.
     */
    @Test
    void putAllSelfIsNoOp() {
        // GIVEN
        TrackingMap<String, Integer> tm = new TrackingMap<>(new HashMap<>());
        tm.put("a", 1);
        tm.put("b", 2);
        int originalSize = tm.size();
        Map<String, Integer> snapshot = new HashMap<>(tm);
        // WHEN
        // self reference
        tm.putAll(tm);
        // THEN – size and contents unchanged
        assertEquals(originalSize, tm.size());
        assertEquals(snapshot, new HashMap<>(tm));
    }

    /**
     * Verify that after a successful putAll, the newly added keys are *not* marked as used.
     * This is a focused repeat of the earlier expunge test but with a different initial state.
     */
    @Test
    void putAllDoesNotMarkNewKeysAsUsed_AdditionalCheck() {
        // GIVEN
        TrackingMap<String, Integer> tm = new TrackingMap<>(new HashMap<>());
        // mark as used by virtue of being put directly
        tm.put("existing", 5);
        // access to mark as used
        tm.get("existing");
        Map<String, Integer> source = Map.of("newKey", 42);
        tm.putAll(source);
        // WHEN
        tm.expungeUnused();
        // THEN – only the originally accessed entry remains
        assertEquals(1, tm.size());
        assertTrue(tm.containsKey("existing"));
        assertFalse(tm.containsKey("newKey"));
    }
}
