package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class TrackingMap_remove_Test {

    private TrackingMap<String, String> realTrackingMap;

    private Map<String, String> realBackingMap;

    @Mock
    private Map<String, String> mockBackingMap;

    @BeforeEach
    void setupBeforeEach() {
        // fresh real map for functional tests
        realBackingMap = new HashMap<>();
        realTrackingMap = new TrackingMap<>(realBackingMap);
    }

    @AfterEach
    void teardownAfterEach() {
        realTrackingMap = null;
        realBackingMap = null;
    }

    private static Stream<Arguments> basicRemovalProvider() {
        return Stream.of(// key exists in both internalMap and readKeys
        arguments("alpha", true, true, "A", false, false), // key exists only in internalMap (never accessed)
        arguments("beta", true, false, "B", false, false), // key exists only in readKeys (never stored)
        arguments("gamma", false, true, null, false, false), // key exists in neither collection
        arguments("delta", false, false, null, false, false));
    }

    private static Stream<Arguments> nullKeyProvider() {
        return Stream.of(// both internalMap and readKeys contain null
        arguments(true, true, "NULL_VAL", false, false), // only readKeys contains null
        arguments(false, true, null, false, false), // neither contains null
        arguments(false, false, null, false, false));
    }

    /**
     * Removing an existing key should return its value, delete the entry from the
     * underlying map and also ensure the key is no longer reported as “used”.
     */
    @Test
    void removeExistingKey() {
        // arrange
        realBackingMap.put("alpha", "A");
        realBackingMap.put("beta", "B");
        // mark "alpha" as accessed so it appears in keysUsed()
        assertEquals("A", realTrackingMap.get("alpha"));
        assertTrue(realTrackingMap.keysUsed().contains("alpha"));
        // act
        String removed = realTrackingMap.remove("alpha");
        // assert
        assertEquals("A", removed, "remove should return the previous value");
        assertFalse(realBackingMap.containsKey("alpha"), "underlying map must no longer contain the key");
        assertFalse(realTrackingMap.containsKey("alpha"), "TrackingMap must report key as absent");
        assertFalse(realTrackingMap.keysUsed().contains("alpha"), "key should be removed from the used‑keys set");
        assertEquals(1, realTrackingMap.size(), "size should reflect the remaining entry");
    }

    /**
     * Removing a key that was never accessed should not add it to the set of used keys.
     */
    @Test
    void removeNeverAccessedKeyDoesNotMarkAsUsed() {
        // arrange
        realBackingMap.put("x", "X");
        realBackingMap.put("y", "Y");
        // act
        String removed = realTrackingMap.remove("y");
        // assert
        assertEquals("Y", removed);
        assertTrue(realTrackingMap.keysUsed().isEmpty(), "keysUsed should stay empty because the key was never accessed before removal");
        assertFalse(realTrackingMap.containsKey("y"));
        assertEquals(1, realTrackingMap.size());
    }

    /**
     * Removing a non‑existent key should return null and must not affect the used‑keys set.
     */
    @Test
    void removeNonExistentKeyReturnsNullAndLeavesStateUntouched() {
        // arrange
        realBackingMap.put("present", "value");
        // access a different key to populate keysUsed()
        realTrackingMap.get("present");
        assertTrue(realTrackingMap.keysUsed().contains("present"));
        // act
        String removed = realTrackingMap.remove("absent");
        // assert
        assertNull(removed, "remove on missing key should return null");
        assertEquals(1, realTrackingMap.size(), "size must stay unchanged");
        assertTrue(realTrackingMap.keysUsed().contains("present"), "existing used keys must remain untouched");
        assertFalse(realTrackingMap.keysUsed().contains("absent"), "absent key must not be added to the used‑keys set");
    }

    /**
     * Verify that the underlying map’s {@code remove} method is invoked exactly once
     * when {@code TrackingMap.remove} is called. This uses a Mockito mock as the backing map.
     */
    @Test
    void removeDelegatesToUnderlyingMap() {
        // arrange – mock returns a specific value when remove is called
        when(mockBackingMap.remove("mockKey")).thenReturn("mockValue");
        TrackingMap<String, String> trackingMapWithMock = new TrackingMap<>(mockBackingMap);
        // act
        String result = trackingMapWithMock.remove("mockKey");
        // assert
        assertEquals("mockValue", result, "remove should forward the return value from the backing map");
        verify(mockBackingMap, times(1)).remove("mockKey");
        // The key should also be removed from the internal read‑keys set (which starts empty)
        assertFalse(trackingMapWithMock.keysUsed().contains("mockKey"), "removed key must not be present in keysUsed()");
    }

    /**
     * After removing a key that was previously accessed, the key must disappear from
     * {@code keysUsed()} even though the access happened before the removal.
     */
    @Test
    void removePreviouslyAccessedKeyClearsFromKeysUsed() {
        // arrange
        realBackingMap.put("k1", "v1");
        // registers access
        realTrackingMap.get("k1");
        assertTrue(realTrackingMap.keysUsed().contains("k1"));
        // act
        realTrackingMap.remove("k1");
        // assert
        assertFalse(realTrackingMap.keysUsed().contains("k1"), "keysUsed must no longer contain a key that has been removed");
        assertFalse(realTrackingMap.containsKey("k1"));
        assertEquals(0, realTrackingMap.size());
    }

    /**
     * Parameterized test covering the four basic removal scenarios:
     *   1. key present in both internalMap and readKeys
     *   2. key present only in internalMap
     *   3. key present only in readKeys
     *   4. key present in neither collection
     */
    @ParameterizedTest(name = "{index}: remove({0})")
    @MethodSource("basicRemovalProvider")
    void removeBasicScenarios(String key, boolean putInMap, boolean addToReadKeys, String expectedReturn, boolean expectInMapAfter, boolean expectInReadKeysAfter) throws Exception {
        // arrange
        if (putInMap) {
            realBackingMap.put(key, expectedReturn);
        }
        if (addToReadKeys) {
            // Access the key (or a non‑existent key) to register it in readKeys
            realTrackingMap.get(key);
        }
        // act
        String result = realTrackingMap.remove(key);
        // assert return value
        assertEquals(expectedReturn, result, () -> "remove should return " + (expectedReturn == null ? "null" : "\"" + expectedReturn + "\""));
        // assert internal map state
        assertEquals(expectInMapAfter, realBackingMap.containsKey(key), () -> "internalMap should " + (expectInMapAfter ? "" : "not ") + "contain the key after removal");
        // assert readKeys state
        assertEquals(expectInReadKeysAfter, realTrackingMap.keysUsed().contains(key), () -> "keysUsed should " + (expectInReadKeysAfter ? "" : "not ") + "contain the key after removal");
    }

    /**
     * Parameterized test for the three null‑key scenarios.
     */
    @ParameterizedTest(name = "{index}: remove(null) – mapContains={0}, readKeysContains={1}")
    @MethodSource("nullKeyProvider")
    void removeNullKeyScenarios(boolean putNullInMap, boolean addNullToReadKeys, String expectedReturn, boolean expectNullInMapAfter, boolean expectNullInReadKeysAfter) throws Exception {
        // arrange
        if (putNullInMap) {
            realBackingMap.put(null, "NULL_VAL");
        }
        if (addNullToReadKeys) {
            // get(null) will add null to readKeys regardless of map presence
            realTrackingMap.get(null);
        }
        // act
        String result = realTrackingMap.remove(null);
        // assert
        assertEquals(expectedReturn, result, () -> "remove(null) should return " + (expectedReturn == null ? "null" : "\"" + expectedReturn + "\""));
        assertEquals(expectNullInMapAfter, realBackingMap.containsKey(null), () -> "internalMap should " + (expectNullInMapAfter ? "" : "not ") + "contain null after removal");
        assertEquals(expectNullInReadKeysAfter, realTrackingMap.keysUsed().contains(null), () -> "keysUsed should " + (expectNullInReadKeysAfter ? "" : "not ") + "contain null after removal");
    }

    /**
     * Removing a key of an incompatible type should safely return null without
     * throwing a {@link ClassCastException}.
     */
    @Test
    void removeWithTypeMismatchKeyReturnsNullAndLeavesStateUntouched() {
        // arrange
        realBackingMap.put("stringKey", "value");
        // act
        // Integer key on a String‑keyed map
        String result = realTrackingMap.remove(42);
        // assert
        assertNull(result, "remove with mismatched key type should return null");
        assertEquals(1, realTrackingMap.size(), "map size must remain unchanged");
        assertTrue(realTrackingMap.containsKey("stringKey"));
        assertTrue(realTrackingMap.keysUsed().isEmpty(), "no keys should be marked as used");
    }

    /**
     * Removing a key whose mapping has an explicit {@code null} value.
     */
    @Test
    void removeKeyWithNullValue() {
        // arrange
        realBackingMap.put("epsilon", null);
        // register access so the key appears in readKeys
        realTrackingMap.get("epsilon");
        assertTrue(realTrackingMap.keysUsed().contains("epsilon"));
        // act
        String removed = realTrackingMap.remove("epsilon");
        // assert
        assertNull(removed, "remove should return null for a mapping whose value is null");
        assertFalse(realBackingMap.containsKey("epsilon"), "internalMap must no longer contain the key");
        assertFalse(realTrackingMap.keysUsed().contains("epsilon"), "keysUsed must no longer contain the key");
    }

    /**
     * Idempotent removal – first call removes the entry, second call is a no‑op.
     */
    @Test
    void removeIdempotentBehavior() {
        // arrange
        realBackingMap.put("zeta", "Z");
        // mark as used
        realTrackingMap.get("zeta");
        // first removal
        String first = realTrackingMap.remove("zeta");
        assertEquals("Z", first, "first removal should return the stored value");
        assertFalse(realTrackingMap.keysUsed().contains("zeta"));
        assertFalse(realTrackingMap.containsKey("zeta"));
        assertEquals(0, realTrackingMap.size());
        // second removal – should be a no‑op
        String second = realTrackingMap.remove("zeta");
        assertNull(second, "second removal should return null");
        assertEquals(0, realTrackingMap.size(), "size must stay at zero after second removal");
    }

    /**
     * When the wrapped map is unmodifiable, {@code remove} should first clear the
     * key from {@code readKeys} and then propagate the {@link UnsupportedOperationException}
     * from the underlying map.
     */
    @Test
    void removeWithUnmodifiableInternalMapThrowsAfterReadKeysUpdate() throws Exception {
        // arrange – mutable readKeys, unmodifiable internal map
        Map<String, String> mutableMap = new HashMap<>();
        mutableMap.put("alpha", "A");
        Map<String, String> unmodifiable = Collections.unmodifiableMap(mutableMap);
        TrackingMap<String, String> trackingMap = new TrackingMap<>(unmodifiable);
        // register access so readKeys contains the key
        trackingMap.get("alpha");
        assertTrue(trackingMap.keysUsed().contains("alpha"));
        // act & assert
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> trackingMap.remove("alpha"), "remove should propagate UnsupportedOperationException from internal map");
        // readKeys must have been cleaned despite the exception
        assertFalse(trackingMap.keysUsed().contains("alpha"), "readKeys must no longer contain the key after the exception");
    }

    /**
     * Same as above but with a {@code null} key.
     */
    @Test
    void removeNullWithUnmodifiableInternalMapThrowsAfterReadKeysUpdate() throws Exception {
        // arrange
        Map<String, String> mutableMap = new HashMap<>();
        mutableMap.put(null, "NULL_VAL");
        Map<String, String> unmodifiable = Collections.unmodifiableMap(mutableMap);
        TrackingMap<String, String> trackingMap = new TrackingMap<>(unmodifiable);
        // register null access
        trackingMap.get(null);
        assertTrue(trackingMap.keysUsed().contains(null));
        // act & assert
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> trackingMap.remove(null), "remove(null) should propagate UnsupportedOperationException from internal map");
        assertFalse(trackingMap.keysUsed().contains(null), "readKeys must no longer contain null after the exception");
    }

    /**
     * When {@code readKeys} is an unmodifiable {@link Set}, the first operation
     * in {@code remove} throws {@link UnsupportedOperationException} and the
     * underlying map remains untouched.
     */
    @Test
    void removeWithUnmodifiableReadKeysThrowsBeforeMapUpdate() throws Exception {
        // arrange – mutable internal map, unmodifiable readKeys containing "beta"
        realBackingMap.put("beta", "B");
        Set<String> mutableReadKeys = new HashSet<>();
        mutableReadKeys.add("beta");
        Set<String> unmodifiableReadKeys = Collections.unmodifiableSet(mutableReadKeys);
        // inject the unmodifiable set via reflection
        Field f = TrackingMap.class.getDeclaredField("readKeys");
        f.setAccessible(true);
        f.set(realTrackingMap, unmodifiableReadKeys);
        assertTrue(realTrackingMap.keysUsed().contains("beta"));
        // act & assert
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> realTrackingMap.remove("beta"), "remove should fail on readKeys.remove with UnsupportedOperationException");
        // internal map must stay unchanged
        assertTrue(realBackingMap.containsKey("beta"), "internal map must still contain the key");
        assertTrue(realTrackingMap.keysUsed().contains("beta"), "readKeys must still contain the key");
    }

    /**
     * Same as above but with a {@code null} key.
     */
    @Test
    void removeNullWithUnmodifiableReadKeysThrowsBeforeMapUpdate() throws Exception {
        // arrange – mutable internal map, unmodifiable readKeys containing null
        realBackingMap.put(null, "NULL_VAL");
        Set<String> mutableReadKeys = new HashSet<>();
        mutableReadKeys.add(null);
        Set<String> unmodifiableReadKeys = Collections.unmodifiableSet(mutableReadKeys);
        Field f = TrackingMap.class.getDeclaredField("readKeys");
        f.setAccessible(true);
        f.set(realTrackingMap, unmodifiableReadKeys);
        assertTrue(realTrackingMap.keysUsed().contains(null));
        // act & assert
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> realTrackingMap.remove(null), "remove(null) should fail on readKeys.remove with UnsupportedOperationException");
        // internal map unchanged
        assertTrue(realBackingMap.containsKey(null), "internal map must still contain null key");
        assertTrue(realTrackingMap.keysUsed().contains(null), "readKeys must still contain null");
    }

    /**
     * Both the internal map and {@code readKeys} are unmodifiable.
     * The method should fail on the {@code readKeys.remove} call before touching the map.
     */
    @Test
    void removeWhenBothCollectionsUnmodifiableThrowsFromReadKeys() throws Exception {
        // arrange – unmodifiable internal map (empty) and unmodifiable readKeys containing "gamma"
        Map<String, String> emptyMap = Collections.unmodifiableMap(new HashMap<>());
        TrackingMap<String, String> trackingMap = new TrackingMap<>(emptyMap);
        Set<String> mutableReadKeys = new HashSet<>();
        mutableReadKeys.add("gamma");
        Set<String> unmodifiableReadKeys = Collections.unmodifiableSet(mutableReadKeys);
        Field f = TrackingMap.class.getDeclaredField("readKeys");
        f.setAccessible(true);
        f.set(trackingMap, unmodifiableReadKeys);
        assertTrue(trackingMap.keysUsed().contains("gamma"));
        // act & assert
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> trackingMap.remove("gamma"), "remove should fail on readKeys.remove before attempting internalMap.remove");
        // internal map must stay unchanged (it was empty)
        assertFalse(trackingMap.getWrappedMap().containsKey("gamma"));
        assertTrue(trackingMap.keysUsed().contains("gamma"), "readKeys should still contain the key after exception");
    }
}
