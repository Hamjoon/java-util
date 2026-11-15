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
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentModificationException;
import java.util.function.Consumer;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TrackingMap_size_Test {

    /**
     * Mocked internal map used for delegation verification.
     */
    @Mock
    private Map<String, Integer> mockMap;

    /**
     * Instance under test that wraps {@code mockMap}.
     */
    private TrackingMap<String, Integer> trackingMapWithMock;

    /**
     * Auto‑closeable handle for Mockito annotations (used in JUnit 5 @BeforeAll/@AfterAll).
     */
    private static AutoCloseable mocks;

    /**
     * Helper map implementation that can be instructed to throw a
     * {@link ConcurrentModificationException} when {@code size()} is invoked.
     */
    private static class ConcurrentModMap<K, V> extends HashMap<K, V> {

        private volatile boolean failOnSize = false;

        void enableFailure() {
            this.failOnSize = true;
        }

        @Override
        public int size() {
            if (failOnSize) {
                throw new ConcurrentModificationException("Simulated concurrent modification");
            }
            return super.size();
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise Mockito's annotation processing for the whole test class.
        mocks = MockitoAnnotations.openMocks(TrackingMap_size_Test.class);
    }

    @BeforeEach
    void setupBeforeEach() {
        // The @Mock field is already instantiated by MockitoExtension,
        // we just need to create the TrackingMap that delegates to it.
        trackingMapWithMock = new TrackingMap<>(mockMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mock interactions so each test starts with a clean slate.
        reset(mockMap);
    }

    @AfterAll
    static void teardownAfterAll() throws Exception {
        // Release resources opened by MockitoAnnotations.
        if (mocks != null) {
            mocks.close();
        }
    }

    /**
     * Supplies the data for {@link #size_basicScenarios(String, Consumer, int)}.
     *
     * Each {@link Arguments} consists of:
     * <ul>
     *   <li>A human‑readable description (used as the test name).</li>
     *   <li>A {@link Consumer} that mutates a freshly created {@link TrackingMap}
     *       according to the scenario.</li>
     *   <li>The expected size after the mutations.</li>
     * </ul>
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> basicSizeScenarios() {
        return Stream.of(// 1. Empty map right after construction
        org.junit.jupiter.params.provider.Arguments.of("A newly created TrackingMap with no entries should report size 0.", (Consumer<TrackingMap<String, Integer>>) map -> {
        }, /* no mutation */
        0), // 2. After putting three distinct key/value pairs
        org.junit.jupiter.params.provider.Arguments.of("After putting three distinct key/value pairs, size() should return 3.", (Consumer<TrackingMap<String, Integer>>) map -> {
            map.put("a", 1);
            map.put("b", 2);
            map.put("c", 3);
        }, 3), // 3. After adding three entries then removing one
        org.junit.jupiter.params.provider.Arguments.of("After adding three entries then removing one key, size() should reflect the remaining 2 entries.", (Consumer<TrackingMap<String, Integer>>) map -> {
            map.put("x", 10);
            map.put("y", 20);
            map.put("z", 30);
            map.remove("y");
        }, 2), // 4. After adding entries and then calling clear()
        org.junit.jupiter.params.provider.Arguments.of("After adding entries and then calling clear(), size() must return 0.", (Consumer<TrackingMap<String, Integer>>) map -> {
            map.put("k1", 100);
            map.put("k2", 200);
            map.clear();
        }, 0), // 5. Using putAll() with a source map containing four entries
        org.junit.jupiter.params.provider.Arguments.of("Using putAll() with a source map containing four entries should make size() return 4.", (Consumer<TrackingMap<String, Integer>>) map -> {
            Map<String, Integer> source = new HashMap<>();
            source.put("a", 1);
            source.put("b", 2);
            source.put("c", 3);
            source.put("d", 4);
            map.putAll(source);
        }, 4), // 6. Putting a key with a null value still counts as an entry
        org.junit.jupiter.params.provider.Arguments.of("Putting a key with a null value still counts as an entry; size() should be 1.", (Consumer<TrackingMap<String, Integer>>) map -> map.put("nullVal", null), 1), // 7. Duplicate key does not increase entry count
        org.junit.jupiter.params.provider.Arguments.of("Putting the same key twice replaces the value without increasing the entry count; after two distinct puts and one duplicate, size() should be 2.", (Consumer<TrackingMap<String, Integer>>) map -> {
            map.put("dup", 1);
            map.put("unique", 2);
            map.put("dup", 3);
        }, // duplicate key
        2));
    }

    @Test
    void size_delegatesToInternalMap() {
        // GIVEN
        when(mockMap.size()).thenReturn(42);
        // WHEN
        int result = trackingMapWithMock.size();
        // THEN
        assertEquals(42, result, "size() should return the value supplied by the internal map");
        verify(mockMap, times(1)).size();
    }

    @Test
    void size_reflectsActualNumberOfEntries() {
        // GIVEN: a real backing map with known content
        Map<String, Integer> realMap = new HashMap<>();
        realMap.put("one", 1);
        realMap.put("two", 2);
        realMap.put("three", 3);
        TrackingMap<String, Integer> trackingMap = new TrackingMap<>(realMap);
        // WHEN
        int size = trackingMap.size();
        // THEN
        assertEquals(3, size, "size() should match the number of entries in the wrapped map");
        // Also verify that modifications are reflected
        trackingMap.put("four", 4);
        assertEquals(4, trackingMap.size(), "size() should update after adding a new entry");
        trackingMap.remove("two");
        assertEquals(3, trackingMap.size(), "size() should update after removing an entry");
    }

    @Test
    void size_onEmptyMapReturnsZero() {
        // GIVEN
        TrackingMap<String, Integer> emptyTrackingMap = new TrackingMap<>(new HashMap<>());
        // WHEN
        int size = emptyTrackingMap.size();
        // THEN
        assertEquals(0, size, "size() of an empty TrackingMap must be 0");
    }

    @Test
    void size_throwsWhenInternalMapIsNull() {
        // GIVEN / WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new TrackingMap<String, Integer>(null));
        assertEquals("Cannot construct a TrackingMap() with null", ex.getMessage(), "Constructor should reject a null backing map");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("basicSizeScenarios")
    void size_basicScenarios(String description, Consumer<TrackingMap<String, Integer>> setup, int expected) {
        // GIVEN a fresh TrackingMap backed by a real HashMap
        TrackingMap<String, Integer> tm = new TrackingMap<>(new HashMap<>());
        // WHEN the scenario-specific mutations are applied
        setup.accept(tm);
        // THEN size() must match the expectation
        assertEquals(expected, tm.size(), description);
    }

    @Test
    void size_throwsNullPointerExceptionWhenInternalMapIsNullViaReflection() throws Exception {
        // GIVEN a normally constructed TrackingMap
        TrackingMap<String, Integer> tm = new TrackingMap<>(new HashMap<>());
        // Tamper with the private field 'internalMap' using reflection
        Field internalMapField = TrackingMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        // force the internal map to null
        internalMapField.set(tm, null);
        // WHEN / THEN a NullPointerException must be thrown
        assertThrows(NullPointerException.class, tm::size, "Calling size() when internalMap is null should throw NullPointerException");
    }

    @Test
    void size_throwsConcurrentModificationExceptionWhenMapIsModifiedConcurrently() {
        // GIVEN a TrackingMap that wraps a custom map capable of throwing CME
        ConcurrentModMap<String, Integer> concurrentMap = new ConcurrentModMap<>();
        TrackingMap<String, Integer> tm = new TrackingMap<>(concurrentMap);
        // Populate the map with some data
        tm.put("a", 1);
        tm.put("b", 2);
        // Simulate a concurrent modification by toggling the flag *before* size() is called
        concurrentMap.enableFailure();
        // WHEN / THEN the size() call should propagate the ConcurrentModificationException
        assertThrows(ConcurrentModificationException.class, tm::size, "size() should propagate ConcurrentModificationException if the underlying map detects concurrent modification");
    }
}
