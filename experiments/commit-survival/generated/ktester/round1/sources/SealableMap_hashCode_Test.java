package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableMap_hashCode_Test {

    /**
     * The map under test
     */
    private SealableMap<String, Integer> map;

    /**
     * Mutable sealed state used by the supplier
     */
    private volatile boolean sealedState = false;

    /**
     * Supplier that reflects the current sealedState
     */
    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    /**
     * Simple key whose {@code hashCode()} returns a predefined int.
     */
    private static final class CustomKey {

        private final int hash;

        CustomKey(int hash) {
            this.hash = hash;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof CustomKey && ((CustomKey) o).hash == hash;
        }
    }

    /**
     * Simple value whose {@code hashCode()} returns a predefined int.
     */
    private static final class CustomValue {

        private final int hash;

        CustomValue(int hash) {
            this.hash = hash;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof CustomValue && ((CustomValue) o).hash == hash;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh SealableMap for each test (unsealed by default)
        map = new SealableMap<>(sealedSupplier);
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        map.put(null, null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset state to avoid cross‑test interference
        map = null;
        sealedState = false;
    }

    /**
     * Supplies all test scenarios extracted from the JSON document.
     */
    private static Stream<Arguments> provideHashCodeScenarios() {
        // Helper lambdas for readability
        Supplier<Map<?, ?>> emptyMap = HashMap::new;
        Supplier<Map<?, ?>> mapA1 = () -> Map.of("a", 1);
        Supplier<Map<?, ?>> mapAB = () -> Map.of("a", 1, "b", 2);
        Supplier<Map<?, ?>> mapNullKey = () -> Map.of((Object) null, "val");
        Supplier<Map<?, ?>> mapNullValue = () -> Map.of("key", (Object) null);
        Supplier<Map<?, ?>> mapCustomObjects = () -> {
            Map<CustomKey, CustomValue> m = new HashMap<>();
            m.put(new CustomKey(5), new CustomValue(7));
            return m;
        };
        Supplier<Map<?, ?>> mapUnmodifiable = () -> Collections.unmodifiableMap(Map.of("k", 3));
        Supplier<Map<?, ?>> mapIntPairs = () -> Map.of(1, 2, 3, 4);
        Supplier<Map<?, ?>> mapSameHash = () -> Map.of(5, 5);
        Supplier<Map<?, ?>> mapZeroKey = () -> Map.of(0, Integer.MAX_VALUE);
        Supplier<Map<?, ?>> mapMinBoth = () -> Map.of(Integer.MIN_VALUE, Integer.MIN_VALUE);
        Supplier<Map<?, ?>> mapLargeKeys = () -> {
            Map<Integer, Integer> m = new HashMap<>();
            m.put(Integer.MAX_VALUE, 0);
            m.put(Integer.MAX_VALUE - 1, 0);
            return m;
        };
        Supplier<Map<?, ?>> treeMap = TreeMap::new;
        Supplier<Map<?, ?>> customHashMap = () -> new HashMap<String, Integer>() {

            @Override
            public int hashCode() {
                return 42;
            }
        };
        // -----------------------------------------------------------------
        // Group: SealableMap.hashCode() behavior
        // -----------------------------------------------------------------
        return Stream.of(// 1. empty map → hash 0
        Arguments.of(emptyMap, null, 0, "Empty map should have hashCode 0"), // 2. single entry ("a" → 1) → 96
        Arguments.of(mapA1, null, 96, "Single entry map should produce hashCode 96"), // 3. two entries ("a"→1, "b"→2) → 192
        Arguments.of(mapAB, null, 192, "Two‑entry map should produce hashCode 192"), // 4. null key, non‑null value → 116513
        Arguments.of(mapNullKey, null, 116513, "Map with null key and non‑null value should produce hashCode 116513"), // 5. non‑null key, null value → 1073741824
        Arguments.of(mapNullValue, null, 1073741824, "Map with non‑null key and null value should produce hashCode 1073741824"), // 6. custom objects with hashCodes 5 and 7 → expected 2 (5 ^ 7)
        Arguments.of(mapCustomObjects, null, 2, "Custom key/value objects should contribute their hashCodes (5 ^ 7 = 2)"), // 7. start empty, put("x",10) → 120
        Arguments.of(emptyMap, (Consumer<SealableMap<?, ?>>) m -> m.put("x", 10), 120, "After putting (\"x\",10) into empty map, hashCode should be 120"), // 8. start empty, put two entries → 192
        Arguments.of(emptyMap, (Consumer<SealableMap<?, ?>>) m -> {
            m.put("a", 1);
            m.put("b", 2);
        }, 192, "After adding two entries to empty map, hashCode should be 192"), // 9. unmodifiable map with one entry → 106
        Arguments.of(mapUnmodifiable, null, 106, "Unmodifiable wrapped map should still delegate hashCode correctly (\"k\"^3 = 106)"), // -----------------------------------------------------------------
        // Group: SealableMap.hashCode() additional scenarios
        // -----------------------------------------------------------------
        // 10. map with integer pairs (1→2,3→4) → 10
        Arguments.of(mapIntPairs, null, 10, "Map.of(1,2,3,4) should have hashCode 10"), // 11. map with identical key/value hash (5→5) → 0
        Arguments.of(mapSameHash, null, 0, "Map where key and value have identical hashCodes should result in 0"), // 12. map with key 0 and value Integer.MAX_VALUE → Integer.MAX_VALUE
        Arguments.of(mapZeroKey, null, Integer.MAX_VALUE, "Map with key hash 0 and value hash Integer.MAX_VALUE should yield Integer.MAX_VALUE"), // 13. map with both key/value hash = Integer.MIN_VALUE → 0
        Arguments.of(mapMinBoth, null, 0, "Map where both key and value hash to Integer.MIN_VALUE should yield 0"), // 14. empty map, put(null,null) → 0
        Arguments.of(emptyMap, (Consumer<SealableMap<?, ?>>) m -> m.put(null, null), 0, "Map containing only a null‑key/null‑value entry should have hashCode 0"), // 15. large positive keys causing overflow → -3
        Arguments.of(mapLargeKeys, null, -3, "Two large positive keys summed should overflow to -3"), // 16. TreeMap with out‑of‑order insertion → 192
        Arguments.of(treeMap, (Consumer<SealableMap<?, ?>>) m -> {
            m.put("b", 2);
            m.put("a", 1);
        }, 192, "TreeMap insertion order should not affect hashCode (still 192)"), // 17. custom map overriding hashCode → 42
        Arguments.of(customHashMap, null, 42, "Custom underlying map that overrides hashCode should return 42"));
    }

    @Test
    void hashCodeDelegatesToUnderlyingMap() {
        // Build an explicit underlying map with known contents
        Map<String, Integer> underlying = new HashMap<>();
        underlying.put("alpha", 10);
        underlying.put("beta", 20);
        underlying.put(null, null);
        // Create a SealableMap that wraps the explicit underlying map
        SealableMap<String, Integer> sealedMap = new SealableMap<>(underlying, sealedSupplier);
        // The hashCode of SealableMap must equal the hashCode of the wrapped map
        assertEquals(underlying.hashCode(), sealedMap.hashCode(), "SealableMap.hashCode() should delegate to the wrapped map");
    }

    @Test
    void hashCodeReflectsMapModifications() {
        // Capture hashCode after an initial set of entries
        int hashBefore = map.hashCode();
        // Mutate the map
        map.put("four", 4);
        int hashAfterPut = map.hashCode();
        // The hash should change because the underlying map changed
        assertNotEquals(hashBefore, hashAfterPut, "hashCode should change after adding a new entry");
        // Remove an entry and verify hash changes again
        map.remove("one");
        int hashAfterRemove = map.hashCode();
        assertNotEquals(hashAfterPut, hashAfterRemove, "hashCode should change after removing an entry");
    }

    @Test
    void hashCodeIsIndependentOfSealedState() {
        // Compute expected hashCode based on the current visible entries
        Map<String, Integer> snapshot = new HashMap<>();
        map.entrySet().forEach(e -> snapshot.put(e.getKey(), e.getValue()));
        int expectedHash = snapshot.hashCode();
        // Verify hashCode while unsealed
        assertEquals(expectedHash, map.hashCode(), "hashCode should match underlying map when unsealed");
        // Switch to sealed state and verify hashCode remains the same
        sealedState = true;
        assertEquals(expectedHash, map.hashCode(), "hashCode should remain unchanged when the map is sealed");
    }

    @Test
    void hashCodeHandlesNullKeyAndNullValue() {
        // The map already contains a null key with a null value from setUp()
        int hashWithNulls = map.hashCode();
        // Add another entry with a non‑null key/value to ensure null handling does not break hashing
        map.put("nonNull", 42);
        int hashAfterAdd = map.hashCode();
        // Both hash codes should be valid and different (since the map changed)
        assertNotEquals(hashWithNulls, hashAfterAdd, "hashCode should reflect the addition of a non‑null entry even when null entries exist");
    }

    /**
     * Parameterised test that covers all scenarios described in the supplied JSON.
     *
     * @param initialMapSupplier supplies the underlying map that will be wrapped by {@link SealableMap}
     * @param operation          optional mutating operation performed after construction (may be {@code null})
     * @param expectedHash       the hash code we expect {@link SealableMap#hashCode()} to return
     * @param description        human‑readable description of the scenario (used as assertion message)
     */
    @ParameterizedTest(name = "{3}")
    @MethodSource("provideHashCodeScenarios")
    void hashCodeParameterized(Supplier<Map<?, ?>> initialMapSupplier, Consumer<SealableMap<?, ?>> operation, int expectedHash, String description) {
        // Build the underlying map and wrap it with SealableMap
        Map<?, ?> underlying = initialMapSupplier.get();
        SealableMap<?, ?> sealedMap = new SealableMap<>(underlying, sealedSupplier);
        // Apply any extra operations (e.g., additional puts) if the test case requires it
        if (operation != null) {
            operation.accept(sealedMap);
        }
        // Verify the hash code matches the contract / expected value
        assertEquals(expectedHash, sealedMap.hashCode(), description);
    }
}
