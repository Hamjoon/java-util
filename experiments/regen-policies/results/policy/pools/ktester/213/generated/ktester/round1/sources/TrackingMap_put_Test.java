package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TrackingMap_put_Test {

    /**
     * ---------------  Fixtures used by several tests  ----------------- **
     */
    private TrackingMap<String, String> trackingMap;

    private Map<String, String> realBackingMap;

    /**
     * Custom key where equals is based on `id` but hashCode is intentionally different.
     */
    private static final class Key {

        private final int id;

        Key(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof Key) && ((Key) o).id == this.id;
        }

        @Override
        public int hashCode() {
            // Deliberately not based solely on `id` to create different hash codes for equal objects.
            return System.identityHashCode(this);
        }

        @Override
        public String toString() {
            return "Key{id=" + id + '}';
        }
    }

    /**
     * Map implementation that throws IllegalArgumentException when the key equals "forbidden".
     */
    private static final class ForbiddenKeyMap<K, V> extends HashMap<K, V> {

        @Override
        public V put(K key, V value) {
            if ("forbidden".equals(key)) {
                throw new IllegalArgumentException("Forbidden key used");
            }
            return super.put(key, value);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // initialise a fresh TrackingMap backed by a plain HashMap for the
        // functional tests that need real map behaviour.
        realBackingMap = new HashMap<>();
        trackingMap = new TrackingMap<>(realBackingMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references – not strictly required but keeps the test runner tidy.
        trackingMap = null;
        realBackingMap = null;
    }

    private static Stream<Arguments> providePutNewEntryIntoEmptyMap() {
        return Stream.of(Arguments.of("alpha", 100, null));
    }

    private static Stream<Arguments> providePutAdditionalNewEntries() {
        return Stream.of(Arguments.of("beta", 200, null));
    }

    private static Stream<Arguments> provideOverwriteExistingKey() {
        return Stream.of(Arguments.of("alpha", 150, 100));
    }

    private static Stream<Arguments> provideNullKeyHandling() {
        return Stream.of(Arguments.of(null, 300, null), Arguments.of(null, 350, 300));
    }

    private static Stream<Arguments> provideNullValueHandling() {
        return Stream.of(Arguments.of("gamma", null, null), // previous value was null
        Arguments.of("gamma", 400, null));
    }

    private static Stream<Arguments> provideKeyEqualityDifferentInstance() {
        return Stream.of(Arguments.of(new String("delta"), 500, null), Arguments.of("delta", 550, 500));
    }

    private static Stream<Arguments> providePutAfterClear() {
        return Stream.of(Arguments.of("epsilon", 600, null));
    }

    private static Stream<Arguments> provideSideEffectVerification() {
        return Stream.of(Arguments.of("zeta", 700));
    }

    private static Stream<Arguments> provideCustomKeyObjects() {
        return Stream.of(Arguments.of(new Key(1), 900, null), Arguments.of(new Key(1), 950, 900));
    }

    private static Stream<Arguments> provideCollectionAsKey() {
        List<String> list1 = Arrays.asList("a", "b");
        List<String> list2 = new ArrayList<>(list1);
        return Stream.of(Arguments.of(list1, 1000, null), Arguments.of(list2, 1050, 1000));
    }

    private static Stream<Arguments> provideCollectionAsValue() {
        List<Integer> list1 = Arrays.asList(1, 2, 3);
        List<Integer> list2 = Arrays.asList(4, 5);
        return Stream.of(Arguments.of("listKey", list1, null), Arguments.of("listKey", list2, list1));
    }

    @Test
    void putNewKeyIncreasesSize() {
        assertEquals(0, trackingMap.size(), "Map should start empty");
        trackingMap.put("alpha", "first");
        assertEquals(1, trackingMap.size(), "Size should be 1 after inserting a new key");
        assertEquals("first", trackingMap.get("alpha"));
    }

    @Test
    void putExistingKeyReturnsPreviousValueAndUpdatesEntry() {
        trackingMap.put("beta", "oldValue");
        String returned = trackingMap.put("beta", "newValue");
        assertEquals("oldValue", returned, "put should return the previous value associated with the key");
        assertEquals(1, trackingMap.size(), "Size must stay unchanged when overwriting an existing key");
        assertEquals("newValue", trackingMap.get("beta"), "The value associated with the key must be updated");
    }

    @Test
    void putNullKeyAndNullValueAreHandledByUnderlyingMap() {
        // HashMap (the backing map) permits a null key and a null value.
        trackingMap.put(null, null);
        assertEquals(1, trackingMap.size(), "Map should contain one entry for the null key");
        assertTrue(trackingMap.containsKey(null), "containsKey should recognise the null key");
        assertNull(trackingMap.get(null), "Value retrieved for null key should be null");
    }

    @Test
    void putDoesNotMarkKeyAsUsed() {
        trackingMap.put("gamma", "value");
        // No get/containsKey has been called yet, so keysUsed must be empty.
        assertTrue(trackingMap.keysUsed().isEmpty(), "keysUsed should remain empty after a plain put operation");
    }

    @Test
    void putDelegatesToUnderlyingMap(@Mock Map<String, String> mockedMap) {
        // Arrange – stub the mock to return a known previous value.
        when(mockedMap.put("delta", "v1")).thenReturn("previous");
        // Act – create a TrackingMap that wraps the mock and invoke put.
        TrackingMap<String, String> mapUnderTest = new TrackingMap<>(mockedMap);
        String result = mapUnderTest.put("delta", "v1");
        // Assert – the result must be the value the mock returned.
        assertEquals("previous", result, "TrackingMap.put should return the value from the wrapped map");
        // Verify that the underlying map's put method was called exactly once with the same arguments.
        verify(mockedMap, times(1)).put("delta", "v1");
        // No other interactions should have occurred.
        verifyNoMoreInteractions(mockedMap);
    }

    // ----------------------------------------------------------------------
    @ParameterizedTest(name = "{index} => key={0}, value={1}, expected={2}")
    @MethodSource("providePutNewEntryIntoEmptyMap")
    void putNewEntryIntoEmptyMap(Object key, Object value, Object expected) {
        Object result = trackingMap.put((String) key, value);
        assertEquals(expected, result);
    }

    // ----------------------------------------------------------------------
    @ParameterizedTest(name = "{index} => key={0}, value={1}, expected={2}")
    @MethodSource("providePutAdditionalNewEntries")
    void putAdditionalNewEntries(Object key, Object value, Object expected) {
        Object result = trackingMap.put((String) key, value);
        assertEquals(expected, result);
    }

    // ----------------------------------------------------------------------
    @ParameterizedTest(name = "{index} => key={0}, value={1}, expected={2}")
    @MethodSource("provideOverwriteExistingKey")
    void overwriteExistingKey(Object key, Object value, Object expected) {
        // pre‑populate with the previous value (100) for key "alpha"
        trackingMap.put("alpha", 100);
        Object result = trackingMap.put((String) key, value);
        assertEquals(expected, result);
    }

    // ----------------------------------------------------------------------
    @ParameterizedTest(name = "{index} => key={0}, value={1}, expected={2}")
    @MethodSource("provideNullKeyHandling")
    void nullKeyHandling(Object key, Object value, Object expected) {
        // If we expect a previous value, pre‑populate the map accordingly.
        if (expected != null) {
            trackingMap.put(null, expected);
        }
        Object result = trackingMap.put((String) key, value);
        assertEquals(expected, result);
    }

    // ----------------------------------------------------------------------
    @ParameterizedTest(name = "{index} => key={0}, value={1}, expected={2}")
    @MethodSource("provideNullValueHandling")
    void nullValueHandling(Object key, Object value, Object expected) {
        // Pre‑populate with a null value for the second case.
        if (expected != null && "null".equals(expected.toString())) {
            // no pre‑population needed – expected null means map had no entry or value null.
        } else if (expected == null && "gamma".equals(key)) {
            // second case expects previous null value, so we insert it first.
            trackingMap.put("gamma", null);
        }
        Object result = trackingMap.put((String) key, value);
        assertEquals(expected, result);
    }

    // ----------------------------------------------------------------------
    @ParameterizedTest(name = "{index} => key={0}, value={1}, expected={2}")
    @MethodSource("provideKeyEqualityDifferentInstance")
    void keyEqualityDifferentInstance(Object key, Object value, Object expected) {
        // First case: new String("delta") – no pre‑population.
        // Second case: literal "delta" – pre‑populate with the first key/value.
        if (expected != null && expected.equals(500)) {
            trackingMap.put(new String("delta"), 500);
        }
        Object result = trackingMap.put((String) key, value);
        assertEquals(expected, result);
    }

    // ----------------------------------------------------------------------
    @ParameterizedTest(name = "{index} => key={0}, value={1}, expected={2}")
    @MethodSource("providePutAfterClear")
    void putAfterClear(Object key, Object value, Object expected) {
        trackingMap.clear();
        Object result = trackingMap.put((String) key, value);
        assertEquals(expected, result);
    }

    // ----------------------------------------------------------------------
    @ParameterizedTest(name = "{index} => key={0}, value={1}")
    @MethodSource("provideSideEffectVerification")
    void sideEffectVerification(Object key, Object value) {
        Object result = trackingMap.put((String) key, value);
        assertNull(result);
        assertEquals(value, trackingMap.get((String) key), "After put, get should return the newly stored value");
    }

    // ----------------------------------------------------------------------
    @Test
    void overwriteWithSameValue() {
        trackingMap.put("alpha", 150);
        Object result = trackingMap.put("alpha", 150);
        assertEquals(150, result);
        assertEquals(150, trackingMap.get("alpha"));
    }

    // ----------------------------------------------------------------------
    @Test
    void overwriteNullValueWithNull() {
        trackingMap.put("gamma", null);
        Object result = trackingMap.put("gamma", null);
        assertNull(result);
        assertNull(trackingMap.get("gamma"));
    }

    // ----------------------------------------------------------------------
    @Test
    void overwriteNonNullWithNull() {
        trackingMap.put("beta", 200);
        Object result = trackingMap.put("beta", null);
        assertEquals(200, result);
        assertNull(trackingMap.get("beta"));
    }

    // ----------------------------------------------------------------------
    @Test
    void mutableKeyBehavior() {
        StringBuilder mutableKey = new StringBuilder("mutableKey");
        Object result = trackingMap.put(mutableKey.toString(), 800);
        assertNull(result);
        // mutate the original StringBuilder – the map key (a String) stays unchanged.
        mutableKey.append("_changed");
        assertEquals(800, trackingMap.get("mutableKey"));
        assertNull(trackingMap.get(mutableKey.toString()));
    }

    // ----------------------------------------------------------------------
    @ParameterizedTest(name = "{index} => key={0}, value={1}, expected={2}")
    @MethodSource("provideCustomKeyObjects")
    void customKeyObjects(Key key, Object value, Object expected) {
        // Pre‑populate for the second case.
        if (expected != null && expected.equals(900)) {
            trackingMap.put(new Key(1), 900);
        }
        Object result = trackingMap.put(key, value);
        assertEquals(expected, result);
    }

    // ----------------------------------------------------------------------
    @ParameterizedTest(name = "{index} => key={0}, value={1}, expected={2}")
    @MethodSource("provideCollectionAsKey")
    void collectionAsKey(List<String> key, Object value, Object expected) {
        // Pre‑populate for the second case.
        if (expected != null && expected.equals(1000)) {
            trackingMap.put(key, 1000);
        }
        Object result = trackingMap.put(key, value);
        assertEquals(expected, result);
    }

    // ----------------------------------------------------------------------
    @ParameterizedTest(name = "{index} => key={0}, value={1}, expected={2}")
    @MethodSource("provideCollectionAsValue")
    void collectionAsValue(String key, List<Integer> value, Object expected) {
        // Pre‑populate for the second case.
        if (expected != null) {
            trackingMap.put(key, expected);
        }
        Object result = trackingMap.put(key, value);
        assertEquals(expected, result);
    }

    // ----------------------------------------------------------------------
    @Test
    void sizeIncrementOnNewKeys() {
        assertEquals(0, trackingMap.size());
        trackingMap.put("k1", 10);
        assertEquals(1, trackingMap.size());
        trackingMap.put("k2", 20);
        assertEquals(2, trackingMap.size());
        trackingMap.put("k3", 30);
        assertEquals(3, trackingMap.size());
    }

    // ----------------------------------------------------------------------
    @Test
    void enumAsKey() {
        trackingMap.put(Day.MONDAY, 111);
        Object first = trackingMap.put(Day.MONDAY, 222);
        assertEquals(111, first);
        assertEquals(222, trackingMap.get(Day.MONDAY));
    }

    // ----------------------------------------------------------------------
    @Test
    void putWithNullInternalMapThrowsNPE() {
        trackingMap.setWrappedMap(null);
        assertThrows(NullPointerException.class, () -> trackingMap.put("anyKey", 123));
    }

    // ----------------------------------------------------------------------
    @Test
    void putOnUnmodifiableMapThrowsUOE() {
        Map<String, Object> unmodifiable = Collections.unmodifiableMap(new HashMap<>());
        trackingMap.setWrappedMap(unmodifiable);
        assertThrows(UnsupportedOperationException.class, () -> trackingMap.put("key1", 1));
    }

    // ----------------------------------------------------------------------
    @Test
    void putNullKeyOnHashtableThrowsNPE() {
        trackingMap.setWrappedMap(new Hashtable<>());
        assertThrows(NullPointerException.class, () -> trackingMap.put(null, 42));
    }

    // ----------------------------------------------------------------------
    @Test
    void putNullValueOnHashtableThrowsNPE() {
        trackingMap.setWrappedMap(new Hashtable<>());
        assertThrows(NullPointerException.class, () -> trackingMap.put("someKey", null));
    }

    // ----------------------------------------------------------------------
    @Test
    void putNonComparableKeyOnTreeMapThrowsCCE() {
        trackingMap.setWrappedMap(new TreeMap<>());
        assertThrows(ClassCastException.class, () -> trackingMap.put(new Object(), 99));
    }

    // ----------------------------------------------------------------------
    @Test
    void putMixedIncomparableKeysOnTreeMapThrowsCCE() {
        trackingMap.setWrappedMap(new TreeMap<>());
        // first insert a String key – works because TreeMap can compare Strings.
        trackingMap.put("stringKey", 10);
        // now insert an Integer – should fail.
        assertThrows(ClassCastException.class, () -> trackingMap.put(1, 20));
    }

    // ----------------------------------------------------------------------
    @Test
    void putKeyCausingCustomMapIllegalArgumentException() {
        trackingMap.setWrappedMap(new ForbiddenKeyMap<>());
        assertThrows(IllegalArgumentException.class, () -> trackingMap.put("forbidden", 777));
    }
}
