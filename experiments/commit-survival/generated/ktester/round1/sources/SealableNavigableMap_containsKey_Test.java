package com.cedarsoftware.util;

import java.util.NavigableMap;
import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Comparator;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SealableNavigableMap_containsKey_Test {

    private NavigableMap<String, Integer> backingMap;

    private SealableNavigableMap<String, Integer> sealableMap;

    private Supplier<Boolean> sealedSupplier;

    private boolean sealed;

    @BeforeEach
    void setupBeforeEach() {
        // start each test with the map unsealed
        sealed = false;
        sealedSupplier = () -> sealed;
        // use the same concrete implementation that the production code expects
        backingMap = new ConcurrentNavigableMapNullSafe<>();
        backingMap.put("one", 1);
        backingMap.put("two", 2);
        // include a null key/value for edge‑case testing
        backingMap.put(null, null);
        sealableMap = new SealableNavigableMap<>(backingMap, sealedSupplier);
    }

    private static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> basicFunctionalityProvider() {
        return java.util.stream.Stream.of(org.junit.jupiter.params.provider.Arguments.of("existingKey", true), org.junit.jupiter.params.provider.Arguments.of("missingKey", false), org.junit.jupiter.params.provider.Arguments.of(new Object(), false), // map has no null entry
        org.junit.jupiter.params.provider.Arguments.of(null, false));
    }

    @Test
    void containsKey_returnsTrueForExistingKey_whenUnsealed() {
        assertFalse(sealedSupplier.get(), "Map should start unsealed");
        assertTrue(sealableMap.containsKey("one"));
        assertTrue(sealableMap.containsKey("two"));
    }

    @Test
    void containsKey_returnsFalseForMissingKey_whenUnsealed() {
        assertFalse(sealableMap.containsKey("three"));
        assertFalse(sealableMap.containsKey("non‑existent"));
    }

    @Test
    void containsKey_handlesNullKey_whenUnsealed() {
        // The backing map was populated with a null key
        assertTrue(sealableMap.containsKey(null));
    }

    @Test
    void containsKey_behaviourIsReadOnly_whenSealed() {
        // switch to sealed state
        sealed = true;
        assertTrue(sealedSupplier.get(), "Map should now be sealed");
        // reads must still work
        assertTrue(sealableMap.containsKey("one"));
        assertFalse(sealableMap.containsKey("missing"));
        assertTrue(sealableMap.containsKey(null));
    }

    @Test
    void containsKey_isUnaffectedBySubsequentMutations() {
        // verify initial state
        assertTrue(sealableMap.containsKey("one"));
        assertFalse(sealableMap.containsKey("four"));
        // mutate the map while it is unsealed
        sealableMap.put("four", 4);
        assertTrue(sealableMap.containsKey("four"));
        // seal the map and ensure the result stays consistent
        sealed = true;
        assertTrue(sealableMap.containsKey("four"));
        assertTrue(sealableMap.containsKey("one"));
    }

    @Test
    void containsKey_doesNotThrowWhenKeyIsNullAndMapIsSealed() {
        sealed = true;
        assertDoesNotThrow(() -> sealableMap.containsKey(null));
        assertTrue(sealableMap.containsKey(null));
    }

    /**
     * Basic functionality – parameterised test covering:
     *   • existing key → true
     *   • missing key  → false
     *   • key of a different type → false
     *   • null key when the underlying map does NOT contain null → false
     */
    @ParameterizedTest(name = "containsKey({0}) → {1}")
    @MethodSource("basicFunctionalityProvider")
    void containsKey_basicFunctionality(Object key, boolean expected) {
        // Use a fresh map that does NOT contain a null entry
        NavigableMap<String, Integer> map = new ConcurrentNavigableMapNullSafe<>();
        map.put("existingKey", 42);
        map.put("anotherKey", 7);
        SealableNavigableMap<String, Integer> sut = new SealableNavigableMap<>(map, sealedSupplier);
        assertEquals(expected, sut.containsKey(key));
    }

    /**
     * Empty map – any key lookup must return false.
     */
    @Test
    void containsKey_emptyMap_returnsFalse() {
        NavigableMap<String, Integer> empty = new TreeMap<>();
        SealableNavigableMap<String, Integer> sut = new SealableNavigableMap<>(empty, sealedSupplier);
        assertFalse(sut.containsKey("anyKey"));
    }

    /**
     * Null key on a standard {@link TreeMap} that does NOT allow nulls.
     * The underlying {@code TreeMap} throws {@link NullPointerException},
     * which must propagate through {@code SealableNavigableMap}.
     */
    @Test
    void containsKey_nullKey_throwsNullPointerException_onTreeMap() {
        NavigableMap<String, Integer> treeMap = new TreeMap<>();
        treeMap.put("a", 1);
        SealableNavigableMap<String, Integer> sut = new SealableNavigableMap<>(treeMap, sealedSupplier);
        assertThrows(NullPointerException.class, () -> sut.containsKey(null));
    }

    /**
     * Type‑mismatch: the underlying map expects {@code String} keys.
     * Supplying an {@code Integer} causes {@link TreeMap} to throw
     * {@link ClassCastException}, which must be propagated.
     */
    @Test
    void containsKey_typeMismatch_throwsClassCastException() {
        NavigableMap<String, Integer> treeMap = new TreeMap<>();
        treeMap.put("one", 1);
        SealableNavigableMap<String, Integer> sut = new SealableNavigableMap<>(treeMap, sealedSupplier);
        assertThrows(ClassCastException.class, () -> sut.containsKey(123));
    }

    /**
     * Null key present – a {@link TreeMap} with a comparator that
     * accepts null keys stores a null entry; {@code containsKey(null)}
     * must return {@code true}.
     */
    @Test
    void containsKey_nullKeyPresent_returnsTrue() {
        Comparator<String> nullFriendly = Comparator.nullsFirst(String::compareTo);
        NavigableMap<String, Integer> map = new TreeMap<>(nullFriendly);
        map.put(null, 0);
        map.put("x", 1);
        SealableNavigableMap<String, Integer> sut = new SealableNavigableMap<>(map, sealedSupplier);
        assertTrue(sut.containsKey(null));
    }

    /**
     * {@code Double.NaN} is a valid key in a {@link TreeMap}.
     * After inserting {@code Double.NaN}, {@code containsKey(Double.NaN)}
     * should return {@code true}.
     */
    @Test
    void containsKey_NaNKey_returnsTrue() {
        NavigableMap<Double, Integer> map = new TreeMap<>();
        map.put(Double.NaN, 99);
        SealableNavigableMap<Double, Integer> sut = new SealableNavigableMap<>(map, sealedSupplier);
        assertTrue(sut.containsKey(Double.NaN));
    }

    /**
     * Verify that any runtime exception thrown by the wrapped map
     * propagates unchanged.
     */
    @Test
    void containsKey_underlyingMapThrowsRuntimeException_propagates() {
        @SuppressWarnings("unchecked")
        NavigableMap<String, Integer> mockMap = mock(NavigableMap.class);
        when(mockMap.containsKey("trigger")).thenThrow(new RuntimeException("boom"));
        SealableNavigableMap<String, Integer> sut = new SealableNavigableMap<>(mockMap, sealedSupplier);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> sut.containsKey("trigger"));
        assertEquals("boom", ex.getMessage());
    }

    /**
     * Underlying map throws {@link UnsupportedOperationException};
     * the exception must propagate.
     */
    @Test
    void containsKey_underlyingMapThrowsUnsupportedOperationException_propagates() {
        @SuppressWarnings("unchecked")
        NavigableMap<String, Integer> mockMap = mock(NavigableMap.class);
        when(mockMap.containsKey(any())).thenThrow(new UnsupportedOperationException("unsupported"));
        SealableNavigableMap<String, Integer> sut = new SealableNavigableMap<>(mockMap, sealedSupplier);
        assertThrows(UnsupportedOperationException.class, () -> sut.containsKey("anyKey"));
    }

    /**
     * Underlying map throws {@link IllegalArgumentException};
     * the exception must propagate.
     */
    @Test
    void containsKey_underlyingMapThrowsIllegalArgumentException_propagates() {
        @SuppressWarnings("unchecked")
        NavigableMap<String, Integer> mockMap = mock(NavigableMap.class);
        when(mockMap.containsKey(any())).thenThrow(new IllegalArgumentException("illegal"));
        SealableNavigableMap<String, Integer> sut = new SealableNavigableMap<>(mockMap, sealedSupplier);
        assertThrows(IllegalArgumentException.class, () -> sut.containsKey("illegal"));
    }

    /**
     * Underlying map throws {@link IllegalStateException};
     * the exception must propagate.
     */
    @Test
    void containsKey_underlyingMapThrowsIllegalStateException_propagates() {
        @SuppressWarnings("unchecked")
        NavigableMap<String, Integer> mockMap = mock(NavigableMap.class);
        when(mockMap.containsKey(any())).thenThrow(new IllegalStateException("bad state"));
        SealableNavigableMap<String, Integer> sut = new SealableNavigableMap<>(mockMap, sealedSupplier);
        assertThrows(IllegalStateException.class, () -> sut.containsKey("anyKey"));
    }
}
