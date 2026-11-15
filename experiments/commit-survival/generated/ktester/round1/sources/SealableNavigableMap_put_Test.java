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
class SealableNavigableMap_put_Test {

    /**
     * Backing map used to construct the {@link SealableNavigableMap}.
     */
    private NavigableMap<String, Integer> backingMap;

    /**
     * Instance under test.
     */
    private SealableNavigableMap<String, Integer> sealableMap;

    /**
     * Supplier that drives the sealed / unsealed state.
     */
    private Supplier<Boolean> sealedSupplier;

    /**
     * Mutable flag used by {@code sealedSupplier}.
     */
    private boolean sealed;

    @BeforeEach
    void setupBeforeEach() {
        // start each test with the map unsealed
        sealed = false;
        sealedSupplier = () -> sealed;
        // a simple mutable NavigableMap implementation used as the delegate
        backingMap = new ConcurrentNavigableMapNullSafe<>();
        backingMap.put("one", 1);
        backingMap.put("two", 2);
        backingMap.put("three", 3);
        // construct the sealable wrapper
        sealableMap = new SealableNavigableMap<>(backingMap, sealedSupplier);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC; not strictly required.
        backingMap = null;
        sealableMap = null;
        sealedSupplier = null;
    }

    private static Stream<Arguments> normalOperationProvider() {
        return Stream.of(// new key → previous null
        Arguments.of("alpha", 123, null), // null value allowed (TreeMap permits null values)
        Arguments.of("beta", null, null), // replace existing key
        Arguments.of("alpha", 456, 123));
    }

    private static Stream<Arguments> sealedPutProvider() {
        return Stream.of(Arguments.of("gamma", 789), Arguments.of("alpha", 999));
    }

    @Test
    void putAddsNewEntryWhenUnsealed() {
        assertFalse(sealedSupplier.get(), "Map should start unsealed.");
        assertEquals(3, sealableMap.size(), "Initial size must match backing map.");
        // add a new entry
        Integer previous = sealableMap.put("four", 4);
        // put should return null because the key was not present before
        assertNull(previous, "put should return null for a new key.");
        // verify the entry is now present
        assertEquals(4, sealableMap.size(), "Size must increase after a successful put.");
        assertEquals(Integer.valueOf(4), sealableMap.get("four"));
        assertTrue(sealableMap.containsKey("four"));
    }

    @Test
    void putReplacesExistingValueWhenUnsealed() {
        assertEquals(Integer.valueOf(2), sealableMap.get("two"));
        // replace the value for an existing key
        Integer previous = sealableMap.put("two", 22);
        // put should return the old value
        assertEquals(Integer.valueOf(2), previous, "put must return the previous value for an existing key.");
        // new value must be stored
        assertEquals(Integer.valueOf(22), sealableMap.get("two"));
        assertEquals(3, sealableMap.size(), "Size must stay the same when replacing an existing entry.");
    }

    @Test
    void putThrowsWhenSealed() {
        // switch the map to sealed mode
        sealed = true;
        assertTrue(sealedSupplier.get(), "Map must be sealed for this test.");
        // any mutating operation must throw UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, () -> sealableMap.put("newKey", 99), "put should be prohibited when the map is sealed.");
        // ensure the map state has not changed
        assertEquals(3, sealableMap.size(), "Size must remain unchanged after a failed put.");
        assertFalse(sealableMap.containsKey("newKey"));
    }

    @Test
    void putAllowsNullKeyAndValueWhenUnsealed() {
        // The underlying map implementation permits null keys/values; the sealable wrapper should forward them.
        assertDoesNotThrow(() -> sealableMap.put(null, null));
        assertTrue(sealableMap.containsKey(null));
        assertNull(sealableMap.get(null));
    }

    @Test
    void putThrowsWhenSealedEvenForNullKeyOrValue() {
        sealed = true;
        assertThrows(UnsupportedOperationException.class, () -> sealableMap.put(null, null), "Even null entries must be blocked when the map is sealed.");
    }

    @ParameterizedTest(name = "{index}: put({0}, {1}) → expected return {2}")
    @MethodSource("normalOperationProvider")
    void putNormalOperation(String key, Integer value, Integer expectedReturn) {
        // act
        Integer returned = sealableMap.put(key, value);
        // assert return value
        assertEquals(expectedReturn, returned, () -> "put should return previous value (or null) for key " + key);
        // assert map state
        assertEquals(value, sealableMap.get(key), () -> "Map must contain the newly put value for key " + key);
    }

    @ParameterizedTest(name = "{index}: sealed put({0}, {1}) → throws IllegalStateException")
    @MethodSource("sealedPutProvider")
    void putWhenSealedThrows(String key, Integer value) {
        // make the map sealed
        sealed = true;
        assertTrue(sealedSupplier.get(), "Map must be sealed for this test");
        // the operation must fail before delegating to the backing map
        assertThrows(IllegalStateException.class, () -> sealableMap.put(key, value), "put must throw IllegalStateException when the map is sealed");
        // ensure no mutation happened
        assertFalse(sealableMap.containsKey(key), "Sealed put must not add the key to the map");
    }

    @Test
    void putNullKeyThrowsNPEWhenUnderlyingMapDisallowsIt() {
        // TreeMap does not allow null keys → NPE from delegate
        assertThrows(NullPointerException.class, () -> sealableMap.put(null, 111), "Underlying TreeMap should throw NPE for null key");
    }

    @Test
    void putNullKeyThrowsIllegalStateWhenSealed() {
        sealed = true;
        assertThrows(IllegalStateException.class, () -> sealableMap.put(null, 222), "Sealed map must throw IllegalStateException before delegate sees null key");
    }

    @Test
    void putNullKeyAllowedByComparator() {
        // Build a TreeMap with a comparator that accepts nulls
        Comparator<String> nullsFirst = Comparator.nullsFirst(String::compareTo);
        NavigableMap<String, Integer> mapWithComparator = new TreeMap<>(nullsFirst);
        SealableNavigableMap<String, Integer> map = new SealableNavigableMap<>(mapWithComparator, sealedSupplier);
        // null key is allowed, previous value is null, method returns null
        Integer returned = map.put(null, 111);
        assertNull(returned, "Returning null for previously absent null key");
        assertEquals(111, map.get(null), "Value for null key must be stored");
    }

    @Test
    void putNonComparableKeyThrowsClassCastException() {
        // TreeMap without comparator expects Comparable keys
        assertThrows(ClassCastException.class, () -> sealableMap.put((String) (Object) new Object(), 42), "Putting a non‑Comparable key must propagate ClassCastException");
    }

    @Test
    void replaceExistingKeyWithNullValue() {
        // pre‑populate the map with the entry ("delta" → 10)
        sealableMap.put("delta", 10);
        // replace with null
        Integer previous = sealableMap.put("delta", null);
        assertEquals(10, previous, "Previous value must be returned");
        assertNull(sealableMap.get("delta"), "New value must be null");
        assertEquals(4, sealableMap.size(), "Size unchanged after replacement");
    }

    @Test
    void replaceWithIdenticalValueDoesNotChangeSize() {
        sealableMap.put("epsilon", 20);
        Integer previous = sealableMap.put("epsilon", 20);
        assertEquals(20, previous, "Should return the previous identical value");
        assertEquals(20, sealableMap.get("epsilon"));
        assertEquals(4, sealableMap.size(), "Size stays the same");
    }

    @Test
    void putNullValueIntoConcurrentSkipListMapThrowsNPE() {
        // ConcurrentSkipListMap rejects null values
        NavigableMap<String, Integer> skipList = new ConcurrentSkipListMap<>();
        SealableNavigableMap<String, Integer> map = new SealableNavigableMap<>(skipList, sealedSupplier);
        assertThrows(NullPointerException.class, () -> map.put("zeta", null), "ConcurrentSkipListMap should throw NPE for null value");
    }

    @Test
    void putIntoUnmodifiableMapThrowsUnsupportedOperationException() {
        NavigableMap<String, Integer> unmodifiable = Collections.unmodifiableNavigableMap(new TreeMap<>());
        SealableNavigableMap<String, Integer> map = new SealableNavigableMap<>(unmodifiable, sealedSupplier);
        assertThrows(UnsupportedOperationException.class, () -> map.put("theta", 1), "Delegate should throw UnsupportedOperationException");
    }

    @Test
    void putKeyCausingComparatorExceptionPropagates() {
        Comparator<String> badComparator = (a, b) -> {
            if ("bad".equals(a) || "bad".equals(b)) {
                throw new IllegalArgumentException("bad key");
            }
            return a.compareTo(b);
        };
        NavigableMap<String, Integer> mapWithBadComp = new TreeMap<>(badComparator);
        SealableNavigableMap<String, Integer> map = new SealableNavigableMap<>(mapWithBadComp, sealedSupplier);
        assertThrows(IllegalArgumentException.class, () -> map.put("bad", 5), "Exception from comparator must propagate");
    }

    @Test
    void putWrongValueTypeThrowsClassCastException() {
        // A TreeMap that expects Integer values; we will force a ClassCastException
        // by using a raw map and inserting an Object via put (the generic type is erased at runtime).
        @SuppressWarnings("unchecked")
        NavigableMap rawMap = new TreeMap<>();
        SealableNavigableMap<String, Integer> map = new SealableNavigableMap<>(rawMap, sealedSupplier);
        assertThrows(ClassCastException.class, () -> map.put("key1", (Integer) (Object) new Object()), "Incompatible value type must cause ClassCastException");
    }
}
