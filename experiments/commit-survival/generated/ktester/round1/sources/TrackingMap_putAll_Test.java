package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TrackingMap_putAll_Test {

    @Mock
    private Map<String, Integer> // mock used for delegation verification
    internalMapMock;

    private TrackingMap<String, Integer> trackingMapWithMock;

    @BeforeEach
    void setupBeforeEach() {
        // Create a TrackingMap that wraps the mocked internal map
        trackingMapWithMock = new TrackingMap<>(internalMapMock);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mock interactions after each test to keep tests independent
        reset(internalMapMock);
    }

    private static Stream<Arguments> providePutAllBasicScenarios() {
        Map<String, Integer> nonEmpty = new HashMap<>();
        nonEmpty.put("a", 1);
        nonEmpty.put("b", 2);
        Map<String, Integer> empty = new HashMap<>();
        return Stream.of(Arguments.of(nonEmpty, 2, "putAll – basic insertion (non‑empty map)"), Arguments.of(empty, 0, "putAll – basic insertion (empty map)"));
    }

    @Test
    void putAll_delegatesToInternalMap() {
        // GIVEN
        Map<String, Integer> source = new HashMap<>();
        source.put("one", 1);
        source.put("two", 2);
        // WHEN
        trackingMapWithMock.putAll(source);
        // THEN
        // Verify that the internal map's putAll was invoked exactly once with the same map instance
        verify(internalMapMock, times(1)).putAll(source);
        // No other interactions should have happened
        verifyNoMoreInteractions(internalMapMock);
    }

    @Test
    void putAll_withEmptyMap_delegatesCorrectly() {
        // GIVEN
        Map<String, Integer> empty = new HashMap<>();
        // WHEN
        trackingMapWithMock.putAll(empty);
        // THEN
        verify(internalMapMock, times(1)).putAll(empty);
        verifyNoMoreInteractions(internalMapMock);
    }

    @Test
    void putAll_effectivelyAddsEntries_whenUsingRealInternalMap() {
        // GIVEN
        Map<String, Integer> realInternal = new HashMap<>();
        TrackingMap<String, Integer> trackingMap = new TrackingMap<>(realInternal);
        Map<String, Integer> toAdd = new HashMap<>();
        toAdd.put("alpha", 10);
        toAdd.put("beta", 20);
        // Pre‑condition check
        assertTrue(trackingMap.isEmpty(), "TrackingMap should start empty");
        // WHEN
        trackingMap.putAll(toAdd);
        // THEN
        assertEquals(2, trackingMap.size(), "TrackingMap should contain two entries after putAll");
        assertEquals(10, trackingMap.get("alpha"));
        assertEquals(20, trackingMap.get("beta"));
        // Ensure the underlying map reflects the same state
        assertEquals(toAdd, realInternal);
    }

    @Test
    void constructor_nullMap_throwsIllegalArgumentException() {
        // GIVEN / WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new TrackingMap<String, Integer>(null));
        assertEquals("Cannot construct a TrackingMap() with null", ex.getMessage());
    }

    /**
     * Basic insertion scenarios using a real internal map.
     * Covers:
     *   – inserting a non‑empty map into an empty TrackingMap
     *   – inserting an empty map (no state change)
     */
    @ParameterizedTest(name = "{index} => description={2}")
    @MethodSource("providePutAllBasicScenarios")
    void putAll_basicInsertionScenarios(Map<String, Integer> source, int expectedSizeAfter, String description) {
        // GIVEN
        Map<String, Integer> realInternal = new HashMap<>();
        TrackingMap<String, Integer> trackingMap = new TrackingMap<>(realInternal);
        // WHEN
        trackingMap.putAll(source);
        // THEN
        assertEquals(expectedSizeAfter, trackingMap.size(), "Size after putAll should match expectation");
        // Verify each expected entry is present (if any)
        source.forEach((k, v) -> assertEquals(v, trackingMap.get(k), "Value for key '" + k + "' should be correctly stored"));
    }

    /**
     * Verify that passing a null reference to {@code putAll} results in a
     * {@link NullPointerException}. The mock internal map is configured to
     * throw the exception, mirroring the behaviour of typical Map implementations.
     */
    @Test
    void putAll_nullArgument_throwsNullPointerException() {
        // GIVEN
        doThrow(new NullPointerException()).when(internalMapMock).putAll(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> trackingMapWithMock.putAll(null), "putAll(null) should throw NullPointerException");
    }

    /**
     * Verify that a map containing a {@code null} key causes a
     * {@link NullPointerException} to be thrown. The mock internal map is
     * stubbed to throw when such a map is supplied.
     */
    @Test
    void putAll_mapWithNullKey_throwsNullPointerException() {
        // GIVEN
        Map<String, Integer> mapWithNullKey = new HashMap<>();
        mapWithNullKey.put(null, 1);
        // Configure the mock to throw NPE when a map containing a null key is passed.
        doThrow(new NullPointerException()).when(internalMapMock).putAll(argThat(m -> m.containsKey(null)));
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> trackingMapWithMock.putAll(mapWithNullKey), "putAll(mapWithNullKey) should throw NullPointerException");
    }

    /**
     * Ensure that using a real internal map, calling {@code putAll} with an
     * empty map does not alter the state of the {@link TrackingMap}.
     */
    @Test
    void putAll_emptyMap_noStateChange_realInternal() {
        // GIVEN
        Map<String, Integer> realInternal = new HashMap<>();
        realInternal.put("existing", 42);
        TrackingMap<String, Integer> trackingMap = new TrackingMap<>(realInternal);
        Map<String, Integer> empty = new HashMap<>();
        // WHEN
        trackingMap.putAll(empty);
        // THEN
        assertEquals(1, trackingMap.size(), "Size should remain unchanged after putting empty map");
        assertEquals(42, trackingMap.get("existing"), "Existing entry must stay intact");
        assertEquals(realInternal, trackingMap.getWrappedMap(), "Underlying map should be unchanged");
    }
}
