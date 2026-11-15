package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableMap_put_Test {

    private SealableMap<String, Integer> map;

    private volatile boolean sealedState;

    private Supplier<Boolean> sealedSupplier;

    @BeforeEach
    void setupBeforeEach() {
        // start each test with an *unsealed* map and a few baseline entries
        sealedState = false;
        sealedSupplier = () -> sealedState;
        map = new SealableMap<>(sealedSupplier);
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        map.put(null, null);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
        map = null;
    }

    private static Stream<Arguments> unsealedPutProvider() {
        return Stream.of(// new key – expect null previous value
        Arguments.of("alpha", 123, null, "new key"), // existing key – expect previous value (1 for key "one")
        Arguments.of("one", 456, 1, "replace existing"), // null key – underlying map permits null, expect previous null
        Arguments.of(null, 99, null, "null key"), // existing key with null value – previous non‑null returned
        Arguments.of("two", null, 2, "null value replacement"));
    }

    /**
     * Helper that swaps the private {@code map} field inside {@link SealableMap}
     * with a supplied delegate.
     */
    private void injectDelegateMap(Map<?, ?> delegate) throws Exception {
        Field f = SealableMap.class.getDeclaredField("map");
        f.setAccessible(true);
        f.set(map, delegate);
    }

    @Test
    void putAddsNewEntryWhenUnsealed() {
        assertNull(map.get("four"), "Pre‑condition: key \"four\" must not exist");
        Integer previous = map.put("four", 4);
        assertNull(previous, "put should return null when the key is new");
        assertEquals(4, map.get("four"));
    }

    @Test
    void putReplacesExistingValueAndReturnsPrevious() {
        assertEquals(1, map.get("one"));
        Integer previous = map.put("one", 10);
        assertEquals(1, previous, "put must return the previous value for an existing key");
        assertEquals(10, map.get("one"));
    }

    @Test
    void putAllowsNullKeyAndNullValueWhenUnsealed() {
        // replace the existing null key
        Integer previous = map.put(null, 99);
        assertNull(previous, "previous value for null key should be null");
        assertEquals(99, map.get(null));
        // add a new null value
        map.put("nullValue", null);
        assertTrue(map.containsKey("nullValue"));
        assertNull(map.get("nullValue"));
    }

    @Test
    void putThrowsWhenMapIsSealed() {
        // make the map read‑only
        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> map.put("newKey", 123), "put must fail when the map is sealed");
    }

    @Test
    void putReturnsPreviousWhenSealedDoesNotExecute() {
        // Ensure the method never reaches the delegate map when sealed.
        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> map.put("one", 999));
        // The original value must stay untouched.
        assertEquals(1, map.get("one"));
    }

    @ParameterizedTest(name = "put({0}, {1}) → expected previous: {2} ({3})")
    @MethodSource("unsealedPutProvider")
    void putUnsealedBehaviour(String key, Integer value, Integer expectedPrevious, String _desc) {
        Integer previous = map.put(key, value);
        assertEquals(expectedPrevious, previous, () -> "Previous value for key '" + key + "' should be " + expectedPrevious);
        assertEquals(value, map.get(key), () -> "After put, map must contain the new value for key '" + key + "'");
    }

    @Test
    void putDoesNotMutateWhenSealed() {
        sealedState = true;
        assertThrows(IllegalStateException.class, () -> map.put("one", 999));
        // original entry must stay unchanged
        assertEquals(1, map.get("one"));
    }

    @Test
    void putWithTreeMapNullKeyThrowsNPE() throws Exception {
        // TreeMap does not allow null keys – we expect a NullPointerException
        TreeMap<String, Integer> tree = new TreeMap<>();
        injectDelegateMap(tree);
        assertThrows(NullPointerException.class, () -> map.put(null, 42), "TreeMap should reject a null key with NPE");
    }

    @Test
    void putWithTreeMapWrongKeyTypeThrowsClassCastException() throws Exception {
        // TreeMap<Integer, String> expects comparable Integer keys.
        TreeMap<Integer, String> tree = new TreeMap<>();
        //noinspection unchecked
        injectDelegateMap(tree);
        assertThrows(ClassCastException.class, () -> map.put("stringKey", 123), "Providing a non‑comparable key to TreeMap must cause CCE");
    }

    @Test
    void putOnUnmodifiableUnderlyingMapPropagatesException() throws Exception {
        Map<String, Integer> base = new HashMap<>();
        base.put("existing", 1);
        Map<String, Integer> unmodifiable = Collections.unmodifiableMap(base);
        injectDelegateMap(unmodifiable);
        assertThrows(UnsupportedOperationException.class, () -> map.put("newKey", 2), "Unmodifiable delegate must propagate UnsupportedOperationException");
    }

    @Test
    void putWhenUnderlyingMapIsNullThrowsNPE() throws Exception {
        // Simulate a mis‑constructed SealableMap where the delegate is null.
        injectDelegateMap(null);
        assertThrows(NullPointerException.class, () -> map.put("k", 1), "Calling put on a null delegate should result in NPE");
    }
}
