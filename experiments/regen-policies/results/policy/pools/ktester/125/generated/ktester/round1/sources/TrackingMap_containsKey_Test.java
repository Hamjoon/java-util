package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class TrackingMap_containsKey_Test {

    private TrackingMap<String, String> trackingMap;

    private Map<String, String> backingMap;

    @BeforeEach
    void setupBeforeEach() {
        // Use a simple HashMap as the backing map for deterministic behaviour
        backingMap = new HashMap<>();
        trackingMap = new TrackingMap<>(backingMap);
        // Populate with a few entries
        trackingMap.put("alpha", "A");
        trackingMap.put("beta", "B");
        trackingMap.put("gamma", null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC; not strictly required
        trackingMap = null;
        backingMap = null;
    }

    private static Stream<Object> typeMismatchKeys() {
        return Stream.of(123, new Object());
    }

    @Test
    void containsKeyExistingKeyIsCounted() {
        // given
        assertFalse(trackingMap.keysUsed().contains("alpha"));
        // when
        boolean result = trackingMap.containsKey("alpha");
        // then
        assertTrue(result, "containsKey should return true for existing key");
        assertTrue(trackingMap.keysUsed().contains("alpha"), "keysUsed should contain the accessed key");
        assertEquals(1, trackingMap.keysUsed().size(), "Only the accessed key should be recorded");
    }

    @Test
    void containsKeyNonExistingKeyIsStillRecorded() {
        // given
        assertFalse(trackingMap.keysUsed().contains("delta"));
        // when
        boolean result = trackingMap.containsKey("delta");
        // then
        assertFalse(result, "containsKey should return false for absent key");
        assertTrue(trackingMap.keysUsed().contains("delta"), "keysUsed must record the key even if it does not exist");
        assertEquals(1, trackingMap.keysUsed().size(), "Only the non‑existent accessed key should be recorded");
    }

    @Test
    void containsKeyMultipleCallsRecordAllAccesses() {
        // when
        trackingMap.containsKey("alpha");
        trackingMap.containsKey("beta");
        // not present
        trackingMap.containsKey("delta");
        // then
        assertTrue(trackingMap.keysUsed().containsAll(java.util.Set.of("alpha", "beta", "delta")), "All accessed keys must be present in keysUsed");
        assertEquals(3, trackingMap.keysUsed().size(), "keysUsed size must reflect each distinct access");
    }

    @Test
    void containsKeyWithNullKeyIsHandledGracefully() {
        // given
        trackingMap.put(null, "nullValue");
        // when
        boolean result = trackingMap.containsKey(null);
        // then
        assertTrue(result, "containsKey should return true for a stored null key");
        assertTrue(trackingMap.keysUsed().contains(null), "null key must be recorded in keysUsed");
    }

    @Test
    void containsKeyDoesNotAffectMapSizeUntilExpunge() {
        // given
        assertEquals(3, trackingMap.size());
        // when
        // access existing
        trackingMap.containsKey("alpha");
        // access non‑existing
        trackingMap.containsKey("delta");
        // then
        // size should remain unchanged because expungeUnused has not been called
        assertEquals(3, trackingMap.size(), "Map size must stay the same before expungeUnused");
        // after expunge, only accessed keys remain
        trackingMap.expungeUnused();
        assertEquals(1, trackingMap.size(), "Only the accessed existing entry should remain after expunge");
        assertTrue(trackingMap.containsKey("alpha"));
    }

    /**
     * Verify that passing a key of an incompatible type (when the generic type
     * parameter is {@code String}) results in a {@link ClassCastException}
     * because the method casts the key before adding it to {@code readKeys}.
     */
    @ParameterizedTest
    @MethodSource("typeMismatchKeys")
    void containsKeyTypeMismatchThrowsClassCastException(Object mismatchedKey) {
        assertThrows(ClassCastException.class, () -> trackingMap.containsKey(mismatchedKey), "A ClassCastException must be thrown for key of type " + mismatchedKey.getClass().getSimpleName());
    }

    /**
     * When the backing {@code Map} throws a {@link RuntimeException} from its
     * {@code containsKey} implementation, {@code TrackingMap.containsKey}
     * must propagate the exception unchanged.
     */
    @Test
    void containsKeyInternalMapThrowsRuntimeException() {
        // Arrange: a mock map that always throws
        @SuppressWarnings("unchecked")
        Map<String, String> throwingMap = Mockito.mock(Map.class);
        Mockito.when(throwingMap.containsKey(any())).thenThrow(new RuntimeException("boom"));
        TrackingMap<String, String> mapWithThrowingBacking = new TrackingMap<>(throwingMap);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> mapWithThrowingBacking.containsKey("anyKey"), "RuntimeException from internal map must be propagated");
        assertEquals("boom", ex.getMessage());
    }

    /**
     * The {@code readKeys} set must not contain duplicate entries. Calling
     * {@code containsKey} twice with the same key should not increase the size
     * of {@code keysUsed()}.
     */
    @Test
    void containsKeyReadKeysIdempotency_DuplicateCalls() {
        // First call – records the key
        assertTrue(trackingMap.containsKey("duplicateKey"));
        assertEquals(1, trackingMap.keysUsed().size(), "After first call the size should be 1");
        // Second call – should not add another entry
        assertTrue(trackingMap.containsKey("duplicateKey"));
        assertEquals(1, trackingMap.keysUsed().size(), "Second call must not increase the size of keysUsed");
    }

    /**
     * Pre‑populate {@code keysUsed()} by calling {@code containsKey} once,
     * then call it again and verify that the set size stays unchanged.
     */
    @Test
    void containsKeyReadKeysIdempotency_Prepopulated() {
        // Pre‑populate
        assertTrue(trackingMap.containsKey("alpha"));
        int sizeAfterFirst = trackingMap.keysUsed().size();
        assertEquals(1, sizeAfterFirst, "Initial access should record exactly one key");
        // Second access – same key
        assertTrue(trackingMap.containsKey("alpha"));
        assertEquals(sizeAfterFirst, trackingMap.keysUsed().size(), "Repeated access must not increase keysUsed size");
    }

    /**
     * A key that exists in the internal map but maps to {@code null} must still
     * be reported as present by {@code containsKey} and recorded in
     * {@code keysUsed()}.
     */
    @Test
    void containsKeyKeyPresentWithNullValue() {
        // Arrange: put a key with a null value (already done in @BeforeEach for "gamma")
        assertTrue(trackingMap.containsKey("gamma"), "containsKey must return true for a key that maps to null");
        assertTrue(trackingMap.keysUsed().contains("gamma"), "The accessed key with null value must be recorded");
    }

    /**
     * If a {@code TrackingMap} instance is constructed with a {@code null}
     * internal map (simulated via reflection), calling {@code containsKey}
     * should result in a {@link NullPointerException}.
     */
    @Test
    void containsKeyInternalMapNullReference() throws Exception {
        // Create a normal instance first
        TrackingMap<String, String> map = new TrackingMap<>(new HashMap<>());
        // Use reflection to set the private final field 'internalMap' to null
        Field internalMapField = TrackingMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(map, null);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> map.containsKey("anyKey"), "Calling containsKey on a TrackingMap with a null internalMap must throw NPE");
    }
}
