package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TrackingMap_hashCode_Test {

    @Mock
    private Map<String, Integer> // used for delegation verification
    mockMap;

    private TrackingMap<String, Integer> trackingMapWithMock;

    class ConstantHashMap<K, V> extends HashMap<K, V> {

        @Override
        public int hashCode() {
            return 42;
        }
    }

    class CollidingKey {

        private final int id;

        CollidingKey(int id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            // constant, causing collision
            return 100;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof CollidingKey))
                return false;
            return id == ((CollidingKey) o).id;
        }

        @Override
        public String toString() {
            return "CollidingKey{" + id + '}';
        }
    }

    class ConstantHashMap<K, V> extends HashMap<K, V> {

        @Override
        public int hashCode() {
            return 42;
        }
    }

    class CollidingKey {

        private final int id;

        CollidingKey(int id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            // constant, causing collision
            return 100;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof CollidingKey))
                return false;
            return id == ((CollidingKey) o).id;
        }

        @Override
        public String toString() {
            return "CollidingKey{" + id + '}';
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a TrackingMap that wraps the mocked map
        trackingMapWithMock = new TrackingMap<>(mockMap);
    }

    private static Stream<Arguments> hashCodeScenarios() {
        return Stream.of(// empty map
        Arguments.of("empty HashMap", (Supplier<Map<String, Integer>>) HashMap::new), // single entry {"a"=1}
        Arguments.of("single entry", (Supplier<Map<String, Integer>>) () -> {
            Map<String, Integer> m = new HashMap<>();
            m.put("a", 1);
            return m;
        }), // multiple entries {"a"=1, "b"=2, "c"=3}
        Arguments.of("multiple entries", (Supplier<Map<String, Integer>>) () -> {
            Map<String, Integer> m = new HashMap<>();
            m.put("a", 1);
            m.put("b", 2);
            m.put("c", 3);
            return m;
        }), // null key {null=0, "x"=5}
        Arguments.of("null key present", (Supplier<Map<String, Integer>>) () -> {
            Map<String, Integer> m = new HashMap<>();
            m.put(null, 0);
            m.put("x", 5);
            return m;
        }), // null value {"k"=null}
        Arguments.of("null value present", (Supplier<Map<String, Integer>>) () -> {
            Map<String, Integer> m = new HashMap<>();
            m.put("k", null);
            return m;
        }), // custom map whose hashCode() always returns 42
        Arguments.of("custom map with constant hashCode 42", (Supplier<Map<String, Integer>>) () -> {
            class ConstantHashMap<K, V> extends HashMap<K, V> {

                @Override
                public int hashCode() {
                    return 42;
                }
            }
            Map<String, Integer> m = new ConstantHashMap<>();
            m.put("any", 1);
            return m;
        }));
    }

    @Test
    void hashCode_delegatesToInternalMap_usingMock() {
        // GIVEN
        int expectedHash = 123456;
        when(mockMap.hashCode()).thenReturn(expectedHash);
        // WHEN
        int actualHash = trackingMapWithMock.hashCode();
        // THEN
        assertEquals(expectedHash, actualHash, "hashCode should delegate to the internal map");
        verify(mockMap, times(1)).hashCode();
    }

    @Test
    void hashCode_matchesUnderlyingMap_forRealMap() {
        // GIVEN
        Map<String, Integer> realMap = new HashMap<>();
        realMap.put("one", 1);
        realMap.put("two", 2);
        TrackingMap<String, Integer> trackingMap = new TrackingMap<>(realMap);
        // WHEN
        int hashFromTrackingMap = trackingMap.hashCode();
        int hashFromRealMap = realMap.hashCode();
        // THEN
        assertEquals(hashFromRealMap, hashFromTrackingMap, "hashCode should be identical to the wrapped map's hashCode");
    }

    @Test
    void hashCode_reflectsChangesInUnderlyingMap() {
        // GIVEN
        Map<String, Integer> realMap = new HashMap<>();
        TrackingMap<String, Integer> trackingMap = new TrackingMap<>(realMap);
        int hashEmpty = trackingMap.hashCode();
        // WHEN - add an entry
        realMap.put("key", 42);
        int hashAfterPut = trackingMap.hashCode();
        // THEN
        assertNotEquals(hashEmpty, hashAfterPut, "hashCode should change after the underlying map is modified");
        // WHEN - clear the map again
        realMap.clear();
        int hashAfterClear = trackingMap.hashCode();
        // THEN
        assertEquals(hashEmpty, hashAfterClear, "hashCode should revert to the original value after clearing the map");
    }

    @Test
    void constructor_throwsException_whenUnderlyingMapIsNull() {
        // GIVEN / WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> new TrackingMap<String, Integer>(null), "Constructor must reject a null underlying map");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("hashCodeScenarios")
    void hashCode_variousScenarios(String description, Supplier<Map<String, Integer>> mapSupplier) {
        // GIVEN
        Map<String, Integer> internal = mapSupplier.get();
        TrackingMap<String, Integer> tm = new TrackingMap<>(internal);
        // WHEN
        int actual = tm.hashCode();
        // THEN
        assertEquals(internal.hashCode(), actual, () -> "hashCode should match the underlying map's hashCode: " + description);
    }

    @Test
    void hashCodeAfterRemoval_reflectsCurrentState() {
        // GIVEN a map with two entries
        Map<String, Integer> internal = new HashMap<>();
        internal.put("a", 1);
        internal.put("b", 2);
        TrackingMap<String, Integer> tm = new TrackingMap<>(internal);
        int hashBeforeRemoval = internal.hashCode();
        // WHEN remove key "a" directly on the underlying map
        internal.remove("a");
        int hashAfterRemoval = tm.hashCode();
        // THEN hashCode must equal the new internal map's hashCode
        assertEquals(internal.hashCode(), hashAfterRemoval, "hashCode should reflect the map after removal");
        assertNotEquals(hashBeforeRemoval, hashAfterRemoval, "hashCode should change after an entry is removed");
    }

    @Test
    void hashCodeAfterClear_returnsEmptyMapHash() {
        // GIVEN a populated map
        Map<String, Integer> internal = new HashMap<>();
        internal.put("x", 10);
        internal.put("y", 20);
        internal.put("z", 30);
        TrackingMap<String, Integer> tm = new TrackingMap<>(internal);
        // WHEN the map is cleared
        internal.clear();
        int hashAfterClear = tm.hashCode();
        // THEN the hash should be equal to a fresh empty HashMap's hashCode (usually 0)
        assertEquals(new HashMap<>().hashCode(), hashAfterClear, "After clear, hashCode should equal that of an empty map");
    }

    @Test
    void hashCodeAfterPutAll_matchesUnderlyingMap() {
        // GIVEN an empty TrackingMap wrapping a real HashMap
        Map<String, Integer> internal = new HashMap<>();
        TrackingMap<String, Integer> tm = new TrackingMap<>(internal);
        // AND a source map with several entries
        Map<String, Integer> source = new HashMap<>();
        source.put("x", 10);
        source.put("y", 20);
        source.put("z", 30);
        // WHEN putAll is invoked
        tm.putAll(source);
        // THEN the hashCode must equal the internal map's hashCode (and thus source's)
        assertEquals(internal.hashCode(), tm.hashCode(), "hashCode after putAll should match the underlying map's hashCode");
        assertEquals(source.hashCode(), tm.hashCode(), "hashCode after putAll should also match the source map's hashCode");
    }

    @Test
    void hashCodeWithTreeMap_delegatesCorrectly() {
        // GIVEN a TreeMap with entries in non‑natural order of insertion
        TreeMap<String, Integer> treeMap = new TreeMap<>();
        treeMap.put("c", 3);
        treeMap.put("a", 1);
        treeMap.put("b", 2);
        TrackingMap<String, Integer> tm = new TrackingMap<>(treeMap);
        // WHEN
        int actual = tm.hashCode();
        // THEN
        assertEquals(treeMap.hashCode(), actual, "TrackingMap should delegate hashCode to a TreeMap correctly");
    }

    @Test
    void hashCodeWithCustomKeyObjects_collidingHashCodes() {
        // GIVEN a map whose keys have identical hashCode() but different equals()
        class CollidingKey {

            private final int id;

            CollidingKey(int id) {
                this.id = id;
            }

            @Override
            public int hashCode() {
                // constant, causing collision
                return 100;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o)
                    return true;
                if (!(o instanceof CollidingKey))
                    return false;
                return id == ((CollidingKey) o).id;
            }

            @Override
            public String toString() {
                return "CollidingKey{" + id + '}';
            }
        }
        Map<CollidingKey, Integer> internal = new HashMap<>();
        internal.put(new CollidingKey(1), 10);
        internal.put(new CollidingKey(2), 20);
        // TrackingMap works with generic types, so we need a raw type cast for simplicity
        @SuppressWarnings("unchecked")
        TrackingMap<CollidingKey, Integer> tm = new TrackingMap((Map) internal);
        // WHEN
        int actual = tm.hashCode();
        // THEN
        assertEquals(internal.hashCode(), actual, "hashCode must be delegated unchanged even when keys have colliding hash codes");
    }

    @Test
    void hashCodeWithUnmodifiableMap_delegatesCorrectly() {
        // GIVEN an unmodifiable map view
        Map<String, Integer> mutable = new HashMap<>();
        mutable.put("u", 100);
        mutable.put("v", 200);
        Map<String, Integer> unmodifiable = Collections.unmodifiableMap(mutable);
        TrackingMap<String, Integer> tm = new TrackingMap<>(unmodifiable);
        // WHEN
        int actual = tm.hashCode();
        // THEN
        assertEquals(unmodifiable.hashCode(), actual, "TrackingMap should correctly delegate hashCode to an unmodifiable map");
    }
}
