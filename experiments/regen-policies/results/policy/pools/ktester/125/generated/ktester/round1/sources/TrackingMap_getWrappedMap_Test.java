package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ExtendWith(MockitoExtension.class)
class TrackingMap_getWrappedMap_Test {

    /**
     * A concrete map used for most tests.
     */
    private Map<String, Integer> backingMap;

    /**
     * The {@code TrackingMap} under test.
     */
    private TrackingMap<String, Integer> trackingMap;

    /**
     * A mock map used for the Mockito‑based test.
     */
    private Map<String, String> mockMap;

    /**
     * The {@code TrackingMap} that wraps the mock map.
     */
    private TrackingMap<String, String> trackingMapWithMock;

    @BeforeEach
    void setupBeforeEach() {
        // 1️⃣ Concrete backing map (HashMap) – used for functional verification.
        backingMap = new HashMap<>();
        backingMap.put("one", 1);
        backingMap.put("two", 2);
        trackingMap = new TrackingMap<>(backingMap);
        // 2️⃣ Mockito mock – used to verify that the exact same reference is returned.
        mockMap = mock(Map.class);
        trackingMapWithMock = new TrackingMap<>(mockMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help the garbage collector; not strictly required.
        backingMap = null;
        trackingMap = null;
        mockMap = null;
        trackingMapWithMock = null;
    }

    /**
     * Provides a stream of different {@link Map} implementations together with a
     * description used for the {@code DisplayName}.
     */
    private static Stream<Arguments> mapImplementations() {
        return Stream.of(Arguments.of(new LinkedHashMap<String, Integer>(), "LinkedHashMap"), Arguments.of(new TreeMap<String, Integer>(), "TreeMap"), Arguments.of(new ConcurrentHashMap<String, Integer>(), "ConcurrentHashMap"));
    }

    @Test
    @DisplayName("getWrappedMap returns the original HashMap instance")
    void returnsOriginalHashMapInstance() {
        Map<String, Integer> result = trackingMap.getWrappedMap();
        // The returned map must be the same object that was passed to the constructor.
        assertSame(backingMap, result, "getWrappedMap should return the exact backing HashMap instance");
        // Mutating the returned map must be reflected in the TrackingMap view.
        result.put("three", 3);
        assertEquals(3, trackingMap.size(), "Changes made through the wrapped map should affect the TrackingMap size");
        assertEquals(3, trackingMap.get("three"));
    }

    @Test
    @DisplayName("getWrappedMap works with a CaseInsensitiveMap")
    void worksWithCaseInsensitiveMap() {
        // CaseInsensitiveMap is part of the library; we use it directly.
        CaseInsensitiveMap<String, String> ciMap = new CaseInsensitiveMap<>();
        ciMap.put("Alpha", "A");
        ciMap.put("Beta", "B");
        TrackingMap<String, String> ciTrackingMap = new TrackingMap<>(ciMap);
        Map<String, String> result = ciTrackingMap.getWrappedMap();
        assertSame(ciMap, result, "The wrapped map should be the original CaseInsensitiveMap instance");
        // Verify that case‑insensitive behaviour is preserved.
        assertEquals("A", ciTrackingMap.get("ALPHA"));
        assertEquals("B", ciTrackingMap.get("beta"));
    }

    @Test
    @DisplayName("getWrappedMap returns the mock map instance")
    void returnsMockMapInstance() {
        Map<String, String> result = trackingMapWithMock.getWrappedMap();
        // The method must return the exact mock we supplied.
        assertSame(mockMap, result, "getWrappedMap should return the exact mock map supplied to the constructor");
        // Verify that interactions with the returned mock are recorded.
        when(mockMap.get("key")).thenReturn("value");
        assertEquals("value", trackingMapWithMock.get("key"));
        verify(mockMap).get("key");
    }

    @Test
    @DisplayName("getWrappedMap does not expose a mutable copy")
    void doesNotExposeCopy() {
        Map<String, Integer> result = trackingMap.getWrappedMap();
        // Attempt to clear the returned map.
        result.clear();
        // The underlying map must also be cleared because the reference is shared.
        assertTrue(backingMap.isEmpty(), "Clearing the map returned by getWrappedMap must clear the original backing map");
        assertTrue(trackingMap.isEmpty(), "TrackingMap must reflect the cleared state of its backing map");
    }

    @ParameterizedTest(name = "getWrappedMap returns the original {1} instance")
    @MethodSource("mapImplementations")
    @DisplayName("getWrappedMap returns the original map instance for various implementations")
    void returnsOriginalMapInstanceForVariousImplementations(Map<String, Integer> map, String description) {
        // Populate the map with a single entry to avoid it being empty (some implementations behave differently when empty).
        map.put("key", 42);
        TrackingMap<String, Integer> tm = new TrackingMap<>(map);
        Map<String, Integer> result = tm.getWrappedMap();
        assertSame(map, result, "Expected the same " + description + " instance to be returned");
    }

    @Test
    @DisplayName("getWrappedMap works with an empty LinkedHashMap")
    void emptyLinkedHashMapInstance() {
        Map<String, Integer> emptyLinked = new LinkedHashMap<>();
        TrackingMap<String, Integer> tm = new TrackingMap<>(emptyLinked);
        assertSame(emptyLinked, tm.getWrappedMap(), "Empty LinkedHashMap should be returned unchanged");
    }

    @Test
    @DisplayName("getWrappedMap returns unmodifiable map instance")
    void unmodifiableMapInstance() {
        Map<String, Integer> mutable = new HashMap<>();
        mutable.put("a", 1);
        Map<String, Integer> unmodifiable = Collections.unmodifiableMap(mutable);
        TrackingMap<String, Integer> tm = new TrackingMap<>(unmodifiable);
        assertSame(unmodifiable, tm.getWrappedMap(), "Unmodifiable map reference must be preserved");
    }

    @Test
    @DisplayName("External modifications to the original map are visible via getWrappedMap")
    void externalModificationsVisible() {
        Map<String, Integer> external = new HashMap<>();
        external.put("x", 10);
        TrackingMap<String, Integer> tm = new TrackingMap<>(external);
        // Modify the original map directly.
        external.put("y", 20);
        external.remove("x");
        Map<String, Integer> wrapped = tm.getWrappedMap();
        assertFalse(wrapped.containsKey("x"));
        assertTrue(wrapped.containsKey("y"));
        assertEquals(20, tm.get("y"));
    }

    @Test
    @DisplayName("Modifications via TrackingMap are reflected in the wrapped map")
    void modificationsViaTrackingMapReflected() {
        TrackingMap<String, Integer> tm = new TrackingMap<>(new HashMap<>());
        tm.put("alpha", 1);
        tm.put("beta", 2);
        tm.remove("alpha");
        Map<String, Integer> wrapped = tm.getWrappedMap();
        assertFalse(wrapped.containsKey("alpha"));
        assertEquals(2, wrapped.get("beta"));
    }

    @Test
    @DisplayName("getWrappedMap after clear() still returns the original map")
    void getWrappedMapAfterClear() {
        TrackingMap<String, Integer> tm = new TrackingMap<>(new HashMap<>(Map.of("a", 1, "b", 2)));
        Map<String, Integer> original = tm.getWrappedMap();
        tm.clear();
        assertSame(original, tm.getWrappedMap(), "Reference must stay the same after clear()");
        assertTrue(original.isEmpty(), "Underlying map should be empty after clear()");
    }

    @Test
    @DisplayName("getWrappedMap after putAll() still returns the original map")
    void getWrappedMapAfterPutAll() {
        Map<String, Integer> base = new HashMap<>(Map.of("a", 1));
        TrackingMap<String, Integer> tm = new TrackingMap<>(base);
        Map<String, Integer> original = tm.getWrappedMap();
        Map<String, Integer> other = Map.of("b", 2, "c", 3);
        tm.putAll(other);
        assertSame(original, tm.getWrappedMap(), "Reference must stay the same after putAll()");
        assertEquals(3, original.size());
        assertEquals(2, original.get("b"));
    }

    @Test
    @DisplayName("getWrappedMap after informAdditionalUsage(Collection) retains original map")
    void getWrappedMapAfterInformAdditionalUsageCollection() {
        TrackingMap<String, Integer> tm = new TrackingMap<>(new HashMap<>(Map.of("k1", 1)));
        Map<String, Integer> original = tm.getWrappedMap();
        tm.informAdditionalUsage(List.of("k1", "k2"));
        assertSame(original, tm.getWrappedMap(), "Reference must stay the same after informAdditionalUsage(Collection)");
    }

    @Test
    @DisplayName("getWrappedMap after informAdditionalUsage(TrackingMap) retains original map")
    void getWrappedMapAfterInformAdditionalUsageTrackingMap() {
        TrackingMap<String, Integer> tm1 = new TrackingMap<>(new HashMap<>(Map.of("a", 1)));
        TrackingMap<String, Integer> tm2 = new TrackingMap<>(new HashMap<>(Map.of("b", 2)));
        Map<String, Integer> original = tm1.getWrappedMap();
        tm1.informAdditionalUsage(tm2);
        assertSame(original, tm1.getWrappedMap(), "Reference must stay the same after informAdditionalUsage(TrackingMap)");
    }

    @Test
    @DisplayName("getWrappedMap after expungeUnused() still returns original map")
    void getWrappedMapAfterExpungeUnused() {
        Map<String, Integer> base = new HashMap<>(Map.of("used", 1, "unused", 2));
        TrackingMap<String, Integer> tm = new TrackingMap<>(base);
        // Access only one key.
        tm.get("used");
        tm.expungeUnused();
        assertSame(base, tm.getWrappedMap(), "Reference must stay the same after expungeUnused()");
        assertFalse(base.containsKey("unused"));
    }

    @Test
    @DisplayName("Successive calls to getWrappedMap return identical object")
    void successiveCallsReturnIdenticalObject() {
        TrackingMap<String, Integer> tm = new TrackingMap<>(new HashMap<>());
        Map<String, Integer> first = tm.getWrappedMap();
        Map<String, Integer> second = tm.getWrappedMap();
        Map<String, Integer> third = tm.getWrappedMap();
        assertSame(first, second);
        assertSame(second, third);
    }

    @Test
    @DisplayName("Direct modifications through returned map are visible via TrackingMap")
    void directModificationsThroughReturnedMapVisible() {
        TrackingMap<String, Integer> tm = new TrackingMap<>(new HashMap<>());
        Map<String, Integer> wrapped = tm.getWrappedMap();
        wrapped.put("direct", 99);
        assertTrue(tm.containsKey("direct"));
        assertEquals(99, tm.get("direct"));
    }

    @Test
    @DisplayName("getWrappedMap returns null when internal map is null (reflection hack)")
    void getWrappedMapReturnsNullWhenInternalMapIsNull() throws Exception {
        // Construct a normal TrackingMap first.
        TrackingMap<String, Integer> tm = new TrackingMap<>(new HashMap<>());
        // Use reflection to set the private final field to null.
        Field internalMapField = TrackingMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(tm, null);
        // Now the method should return null.
        assertNull(tm.getWrappedMap(), "When internalMap is null, getWrappedMap() should return null");
    }
}
