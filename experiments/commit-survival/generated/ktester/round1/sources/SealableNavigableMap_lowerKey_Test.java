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
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableNavigableMap_lowerKey_Test {

    private NavigableMap<String, Integer> backingMap;

    private SealableNavigableMap<String, Integer> sealableMap;

    private Supplier<Boolean> sealedSupplier;

    private boolean sealed;

    @BeforeEach
    void setupBeforeEach() {
        // start un‑sealed
        sealed = false;
        sealedSupplier = () -> sealed;
        // a simple backing map – the concrete implementation used by the library
        backingMap = new ConcurrentNavigableMapNullSafe<>();
        backingMap.put("one", 1);
        backingMap.put("two", 2);
        backingMap.put("three", 3);
        // null key is allowed by the implementation but is not relevant for lowerKey tests
        backingMap.put(null, null);
        sealableMap = new SealableNavigableMap<>(backingMap, sealedSupplier);
    }

    private static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(Arguments.of("c", "b"), Arguments.of("b", "a"), Arguments.of("e", "d"), Arguments.of("0", null));
    }

    private static Stream<Arguments> edgeCasesProvider() {
        return Stream.of(// single entry, key greater than existing
        Arguments.of("b", "a", "Single entry, key greater than existing"), // single entry, key equal to the only entry
        Arguments.of("a", null, "Single entry, key equal to existing"), // empty map
        Arguments.of("any", null, "Empty map"));
    }

    @Test
    void testLowerKeyReturnsCorrectKeyWhenKeyExists() {
        // "two" -> lower key should be "one"
        assertEquals("one", sealableMap.lowerKey("two"));
        // "three" -> lower key should be "two"
        assertEquals("two", sealableMap.lowerKey("three"));
    }

    @Test
    void testLowerKeyReturnsNullWhenNoLowerKeyExists() {
        // smallest key is "one"; lowerKey should be null
        assertNull(sealableMap.lowerKey("one"));
        // key smaller than any existing key
        assertNull(sealableMap.lowerKey("0"));
    }

    @Test
    void testLowerKeyWorksForNonExistingKeyWithinRange() {
        // "two-and-a-half" is between "two" and "three"
        assertEquals("two", sealableMap.lowerKey("two-and-a-half"));
    }

    @Test
    void testLowerKeyIsUnaffectedBySealedState() {
        // map is now sealed (read‑only)
        sealed = true;
        // lowerKey does not modify the map, so it should still work
        assertEquals("one", sealableMap.lowerKey("two"));
        assertNull(sealableMap.lowerKey("one"));
    }

    @Test
    void testLowerKeyWithNullKeyThrowsNPE() {
        // According to NavigableMap contract, null key is not allowed unless a custom comparator permits it.
        // The underlying ConcurrentNavigableMapNullSafe throws NPE for null keys in navigation methods.
        assertThrows(NullPointerException.class, () -> sealableMap.lowerKey(null));
    }

    // ---------- Basic functionality ----------
    @ParameterizedTest(name = "[{index}] lowerKey({0}) → {1}")
    @MethodSource("basicFunctionalityProvider")
    void testBasicFunctionality(String key, String expected) {
        // Build a map with keys a, b, c, d
        NavigableMap<String, Integer> map = new ConcurrentSkipListMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);
        SealableNavigableMap<String, Integer> sut = new SealableNavigableMap<>(map, sealedSupplier);
        assertEquals(expected, sut.lowerKey(key));
    }

    // ---------- Edge cases ----------
    @ParameterizedTest(name = "[{index}] edge case lowerKey({0}) → {1}")
    @MethodSource("edgeCasesProvider")
    void testEdgeCases(String key, String expected, String description) {
        // Single‑element map for the first two cases, empty map for the third.
        NavigableMap<String, Integer> map;
        if ("Empty map".equals(description)) {
            map = new ConcurrentSkipListMap<>();
        } else {
            map = new ConcurrentSkipListMap<>();
            map.put("a", 1);
        }
        SealableNavigableMap<String, Integer> sut = new SealableNavigableMap<>(map, sealedSupplier);
        assertEquals(expected, sut.lowerKey(key));
    }

    // ---------- Exception handling ----------
    @Test
    void testLowerKeyNullKeyThrowsNPE() {
        // Map is keyed by String; null is not permitted by the underlying TreeMap.
        assertThrows(NullPointerException.class, () -> sealableMap.lowerKey(null));
    }

    @Test
    void testLowerKeyWrongTypeThrowsClassCastException() {
        // The map expects String keys; passing an Integer should cause ClassCastException.
        assertThrows(ClassCastException.class, () -> sealableMap.lowerKey((String) (Object) 123));
    }

    // ---------- Underlying map null ----------
    @Test
    void testLowerKeyWhenInternalMapIsNullThrowsNPE() {
        // Construct a SealableNavigableMap with a null backing map.
        SealableNavigableMap<String, Integer> sut = new SealableNavigableMap<>(null, sealedSupplier);
        assertThrows(NullPointerException.class, () -> sut.lowerKey("a"));
    }
}
