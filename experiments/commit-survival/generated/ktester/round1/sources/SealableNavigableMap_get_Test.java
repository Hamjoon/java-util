package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class SealableNavigableMap_get_Test {

    private NavigableMap<String, Integer> underlyingMap;

    private SealableNavigableMap<String, Integer> sealableMap;

    private Supplier<Boolean> sealedSupplier;

    private boolean sealed;

    @BeforeEach
    void setupBeforeEach() {
        // start unsealed for each test
        sealed = false;
        sealedSupplier = () -> sealed;
        // Use a spy so we can verify delegation to the underlying map
        underlyingMap = spy(new ConcurrentNavigableMapNullSafe<>());
        // Populate the map with a few entries, including a null key/value
        underlyingMap.put("one", 1);
        underlyingMap.put("two", 2);
        underlyingMap.put(null, null);
        // Create the SealableNavigableMap that wraps the spy
        sealableMap = new SealableNavigableMap<>(underlyingMap, sealedSupplier);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to avoid cross‑test contamination
        underlyingMap.clear();
    }

    /**
     * Supplies the arguments for {@link #getBasicFunctionality(Object, Object)}.
     */
    private static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(// existing key
        arguments("apple", "red"), // null key (value also null)
        arguments(null, null), // missing key
        arguments("nonexistent", null));
    }

    @Test
    void getReturnsValueWhenKeyIsPresent() {
        Integer result = sealableMap.get("one");
        assertEquals(Integer.valueOf(1), result, "get should return the stored value for an existing key");
        // Verify that the call was delegated to the underlying map
        verify(underlyingMap, times(1)).get("one");
    }

    @Test
    void getReturnsNullWhenKeyIsAbsent() {
        Integer result = sealableMap.get("missing");
        assertNull(result, "get should return null for a key that is not present");
        verify(underlyingMap, times(1)).get("missing");
    }

    @Test
    void getHandlesNullKey() {
        Integer result = sealableMap.get(null);
        assertNull(result, "get should return the stored null value for a null key");
        verify(underlyingMap, times(1)).get(null);
    }

    @Test
    void getIsUnaffectedBySealedState() {
        // Verify normal behaviour when unsealed
        assertEquals(Integer.valueOf(2), sealableMap.get("two"));
        // Switch to sealed and repeat – get should still work
        sealed = true;
        assertEquals(Integer.valueOf(2), sealableMap.get("two"), "get must work regardless of the sealed state");
        // Verify delegation happened twice (once before sealing, once after)
        verify(underlyingMap, times(2)).get("two");
    }

    @Test
    void getDelegatesExactlyOncePerCall() {
        // Call get several times with different keys
        sealableMap.get("one");
        sealableMap.get("two");
        sealableMap.get("one");
        // Verify the underlying map received exactly one call per distinct invocation
        verify(underlyingMap, times(2)).get("one");
        verify(underlyingMap, times(1)).get("two");
    }

    /* -------------------------------------------------------------
     *  Parameterized test – basic functionality group
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: get({0}) => {1}")
    @MethodSource("basicFunctionalityProvider")
    void getBasicFunctionality(Object key, Object expected) {
        // A fresh underlying map for each parameter set
        NavigableMap<String, String> map = spy(new ConcurrentNavigableMapNullSafe<>());
        // Only add the entry that is required for the "existing key" case
        if ("apple".equals(key)) {
            map.put("apple", "red");
        }
        // For the explicit null‑key case we also add a null entry
        if (key == null) {
            map.put(null, null);
        }
        SealableNavigableMap<String, String> sealable = new SealableNavigableMap<>(map, () -> false);
        // Act
        Object result = sealable.get(key);
        // Assert
        assertEquals(expected, result, () -> "Expected get(" + key + ") to return " + expected + " but got " + result);
        verify(map, times(1)).get(key);
    }

    /* -------------------------------------------------------------
     *  Empty map returns null
     * ------------------------------------------------------------- */
    @Test
    void getReturnsNullWhenMapIsEmpty() {
        NavigableMap<String, Integer> emptyMap = spy(new TreeMap<>());
        SealableNavigableMap<String, Integer> sealable = new SealableNavigableMap<>(emptyMap, () -> false);
        assertNull(sealable.get("anyKey"), "Empty map should return null for any key");
        verify(emptyMap, times(1)).get("anyKey");
    }

    /* -------------------------------------------------------------
     *  Key mapped to an explicit null value
     * ------------------------------------------------------------- */
    @Test
    void getReturnsNullWhenValueIsExplicitlyNull() {
        NavigableMap<String, Integer> map = spy(new TreeMap<>());
        map.put("nullValueKey", null);
        SealableNavigableMap<String, Integer> sealable = new SealableNavigableMap<>(map, () -> false);
        assertNull(sealable.get("nullValueKey"), "Entry whose value is null must still be considered present and return null");
        verify(map, times(1)).get("nullValueKey");
    }

    /* -------------------------------------------------------------
     *  Null key on a plain TreeMap – should propagate NPE
     * ------------------------------------------------------------- */
    @Test
    void getPropagatesNullPointerExceptionForNullKeyInTreeMap() {
        NavigableMap<String, Integer> treeMap = new TreeMap<>();
        SealableNavigableMap<String, Integer> sealable = new SealableNavigableMap<>(treeMap, () -> false);
        assertThrows(NullPointerException.class, () -> sealable.get(null), "TreeMap does not allow null keys; NPE must be propagated");
    }

    /* -------------------------------------------------------------
     *  Incompatible key type – should propagate ClassCastException
     * ------------------------------------------------------------- */
    @Test
    void getPropagatesClassCastExceptionForInvalidKeyType() {
        NavigableMap<String, Integer> treeMap = new TreeMap<>();
        treeMap.put("a", 1);
        SealableNavigableMap<String, Integer> sealable = new SealableNavigableMap<>(treeMap, () -> false);
        assertThrows(ClassCastException.class, () -> sealable.get(123), "Passing a key of the wrong type must result in ClassCastException");
    }

    /* -------------------------------------------------------------
     *  Custom comparator that tolerates null keys
     * ------------------------------------------------------------- */
    @Test
    void getWorksWithComparatorThatAllowsNullKeys() {
        Comparator<String> nullFriendly = Comparator.nullsFirst(String::compareTo);
        NavigableMap<String, String> map = new TreeMap<>(nullFriendly);
        map.put(null, "nullKeyValue");
        SealableNavigableMap<String, String> sealable = new SealableNavigableMap<>(map, () -> false);
        assertEquals("nullKeyValue", sealable.get(null), "Comparator that permits null keys should allow retrieval of the null‑key entry");
    }
}
