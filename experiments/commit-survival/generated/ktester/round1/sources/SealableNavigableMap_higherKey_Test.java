package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableNavigableMap_higherKey_Test {

    private NavigableMap<String, Integer> map;

    private SealableNavigableMap<String, Integer> sealableMap;

    private Supplier<Boolean> sealedSupplier;

    private boolean sealed;

    @BeforeEach
    void setupBeforeEach() {
        sealed = false;
        sealedSupplier = () -> sealed;
        // Populate a map with a known ordering
        map = new ConcurrentNavigableMapNullSafe<>();
        map.put("one", 1);
        map.put("three", 3);
        map.put("two", 2);
        // The map also allows a null entry (as seen in other tests), but higherKey(null) is expected to NPE
        map.put(null, null);
        sealableMap = new SealableNavigableMap<>(map, sealedSupplier);
    }

    @AfterEach
    void teardownAfterEach() {
        map = null;
        sealableMap = null;
        sealedSupplier = null;
    }

    private static Stream<Arguments> basicHigherKeyProvider() {
        return Stream.of(// key 2 → higher key 3
        Arguments.of(2.0, 3.0), // key 4 → no higher key
        Arguments.of(4.0, null), // key 2.5 (not present) → higher key 3
        Arguments.of(2.5, 3.0), // key 0 → first (lowest) key 1
        Arguments.of(0.0, 1.0));
    }

    private static Stream<Arguments> singleElementHigherKeyProvider() {
        return Stream.of(// key smaller than the sole entry
        Arguments.of(0, 5), // key equal to the sole entry
        Arguments.of(5, null), // key larger than the sole entry
        Arguments.of(10, null));
    }

    private static Stream<Arguments> reverseOrderHigherKeyProvider() {
        return Stream.of(// in reverse order, higherKey(3) → 2
        Arguments.of(3, 2), // higherKey(4) → 3
        Arguments.of(4, 3), // 1 is the lowest (greatest in reverse), no higher key
        Arguments.of(1, null), // 2.5 not present; using 2 → higherKey is 1
        Arguments.of(2, 1));
    }

    @Test
    void testHigherKeyBasic() {
        // Build a map with natural ordering of Double keys: 1.0, 2.0, 3.0, 4.0
        NavigableMap<Double, String> base = new ConcurrentNavigableMapNullSafe<>();
        base.put(1.0, "one");
        base.put(2.0, "two");
        base.put(3.0, "three");
        base.put(4.0, "four");
        SealableNavigableMap<Double, String> sm = new SealableNavigableMap<>(base, sealedSupplier);
        if (expected instanceof Class) {
            // Expected an exception type
            Class<? extends Throwable> ex = (Class<? extends Throwable>) expected;
            assertThrows(ex, () -> sm.higherKey((Double) key));
        } else {
            assertEquals(expected, sm.higherKey((Double) key));
        }
    }

    @Test
    void testHigherKeyWhenKeyNotPresent() {
        // Key that does not exist but falls between existing keys
        assertEquals("three", sealableMap.higherKey("one-and-a-half"));
        // Key greater than any existing key
        assertNull(sealableMap.higherKey("zzzz"));
    }

    @Test
    void testHigherKeyWhenSealed() {
        // make the map read‑only
        sealed = true;
        // higherKey is a read operation; it should still work
        assertEquals("three", sealableMap.higherKey("one"));
        assertNull(sealableMap.higherKey("two"));
    }

    @Test
    void testHigherKeyWithNullKey() {
        // TreeMap (and thus the underlying navMap) throws NPE for null argument
        assertThrows(NullPointerException.class, () -> sealableMap.higherKey(null));
    }

    @Test
    void testHigherKeyOnEmptyMap() {
        // Create an empty SealableNavigableMap
        NavigableMap<String, Integer> empty = new ConcurrentNavigableMapNullSafe<>();
        SealableNavigableMap<String, Integer> emptySealable = new SealableNavigableMap<>(empty, sealedSupplier);
        assertNull(emptySealable.higherKey("any"));
    }

    @ParameterizedTest(name = "{index}: singleElement higherKey({0}) → {1}")
    @MethodSource("singleElementHigherKeyProvider")
    void testHigherKeySingleElement(Object key, Object expected) {
        NavigableMap<Integer, String> single = new ConcurrentNavigableMapNullSafe<>();
        single.put(5, "five");
        SealableNavigableMap<Integer, String> sm = new SealableNavigableMap<>(single, sealedSupplier);
        if (expected instanceof Class) {
            Class<? extends Throwable> ex = (Class<? extends Throwable>) expected;
            assertThrows(ex, () -> sm.higherKey((Integer) key));
        } else {
            assertEquals(expected, sm.higherKey((Integer) key));
        }
    }

    @Test
    void testHigherKeyWithIncompatibleKeyType() {
        // Underlying map uses Integer keys
        NavigableMap<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "one");
        intMap.put(2, "two");
        SealableNavigableMap<Integer, String> sm = new SealableNavigableMap<>(intMap, sealedSupplier);
        // Supplying a String key must cause ClassCastException
        assertThrows(ClassCastException.class, () -> sm.higherKey((Integer) (Object) "stringKey"));
    }

    @ParameterizedTest(name = "{index}: reverseOrder higherKey({0}) → {1}")
    @MethodSource("reverseOrderHigherKeyProvider")
    void testHigherKeyReverseOrder(Integer key, Integer expected) {
        // TreeMap with reverse order comparator
        NavigableMap<Integer, String> revMap = new TreeMap<>(Collections.reverseOrder());
        revMap.put(1, "one");
        revMap.put(2, "two");
        revMap.put(3, "three");
        revMap.put(4, "four");
        SealableNavigableMap<Integer, String> sm = new SealableNavigableMap<>(revMap, sealedSupplier);
        assertEquals(expected, sm.higherKey(key));
    }

    @Test
    void testHigherKeyBasicStringMap() {
        // "one" -> next higher key should be "three"
        assertEquals("three", sealableMap.higherKey("one"));
        // "three" -> next higher key should be "two"
        assertEquals("two", sealableMap.higherKey("three"));
        // "two" is the greatest key, so higherKey should be null
        assertNull(sealableMap.higherKey("two"));
    }
}
