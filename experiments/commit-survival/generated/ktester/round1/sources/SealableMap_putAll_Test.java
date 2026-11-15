package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SealableMap_putAll_Test {

    private SealableMap<String, Integer> map;

    private volatile boolean sealedState;

    private Supplier<Boolean> sealedSupplier;

    @BeforeEach
    void setupBeforeEach() {
        sealedState = false;
        sealedSupplier = () -> sealedState;
        map = new SealableMap<>(sealedSupplier);
        // initial content – used by several tests
        map.put("one", 1);
        map.put("two", 2);
        map.put(null, null);
    }

    private static Stream<Arguments> normalOperationProvider() {
        // case 1 – typical map with two entries
        Map<String, Integer> src1 = new HashMap<>();
        src1.put("a", 1);
        src1.put("b", 2);
        Map<String, Integer> exp1 = new HashMap<>(src1);
        // case 2 – empty map
        Map<String, Integer> src2 = Collections.emptyMap();
        Map<String, Integer> exp2 = new HashMap<>();
        // case 3 – null argument (handled separately, not part of this provider)
        return Stream.of(Arguments.of(src1, exp1, "copies all entries from a non‑empty map"), Arguments.of(src2, exp2, "leaves the map unchanged when source is empty"));
    }

    @Test
    void testPutAllWhenUnsealedAddsAndOverridesEntries() {
        // GIVEN a source map with new and overlapping entries
        Map<String, Integer> source = new HashMap<>();
        // new entry
        source.put("three", 3);
        // overrides existing "two"
        source.put("two", 22);
        // overrides null key
        source.put(null, 0);
        // WHEN putAll is invoked while the map is unsealed
        map.putAll(source);
        // THEN the map contains all expected entries
        assertEquals(1, map.get("one"));
        // overridden value
        assertEquals(22, map.get("two"));
        // newly added
        assertEquals(3, map.get("three"));
        // overridden null key
        assertEquals(0, map.get(null));
        // total number of entries
        assertEquals(4, map.size());
    }

    @Test
    void testPutAllWhenSealedThrowsException() {
        // GIVEN the map is sealed
        sealedState = true;
        Map<String, Integer> source = new HashMap<>();
        source.put("four", 4);
        // WHEN putAll is called, THEN an UnsupportedOperationException is thrown
        assertThrows(UnsupportedOperationException.class, () -> map.putAll(source));
    }

    @Test
    void testPutAllWithNullArgumentThrowsNPE() {
        // GIVEN a null argument
        Map<String, Integer> source = null;
        // WHEN putAll is called, THEN a NullPointerException is thrown (consistent with Map contract)
        assertThrows(NullPointerException.class, () -> map.putAll(source));
    }

    @Test
    void testPutAllPreservesExistingEntriesWhenSourceIsEmpty() {
        // GIVEN an empty source map
        Map<String, Integer> source = new HashMap<>();
        // WHEN putAll is invoked
        map.putAll(source);
        // THEN the original map is unchanged
        assertEquals(1, map.get("one"));
        assertEquals(2, map.get("two"));
        assertNull(map.get("three"));
        assertNull(map.get(null));
        assertEquals(3, map.size());
    }

    @Test
    void testPutAllWithNullKeyAndNullValue() {
        // GIVEN a source map that contains a null key and a null value
        Map<String, Integer> source = new HashMap<>();
        // null key, non‑null value
        source.put(null, 99);
        // non‑null key, null value
        source.put("five", null);
        // WHEN putAll is executed while unsealed
        map.putAll(source);
        // THEN the map reflects those entries correctly
        assertEquals(99, map.get(null));
        assertTrue(map.containsKey("five"));
        assertNull(map.get("five"));
        assertEquals(5, map.size());
    }

    @ParameterizedTest(name = "{index}: putAll with source {0}")
    @MethodSource("normalOperationProvider")
    void testPutAllNormalOperation(Map<String, Integer> source, Map<String, Integer> expected, String description) {
        // GIVEN a source map (may be empty)
        // WHEN putAll is invoked while the map is unsealed
        map.putAll(source);
        // THEN the underlying map matches the expected state
        expected.forEach((k, v) -> assertEquals(v, map.get(k), "Key: " + k));
        assertEquals(expected.size(), map.size(), "Map size mismatch");
    }

    @Test
    void testPutAllWhenSealedThrowsIllegalStateException() {
        // map is now sealed
        sealedState = true;
        Map<String, Integer> source = new HashMap<>();
        source.put("x", 10);
        // The implementation may throw IllegalStateException or UnsupportedOperationException.
        // We assert that *some* RuntimeException is thrown and that the underlying map stays unchanged.
        RuntimeException ex = assertThrows(RuntimeException.class, () -> map.putAll(source));
        assertTrue(ex instanceof IllegalStateException || ex instanceof UnsupportedOperationException, "Expected IllegalStateException or UnsupportedOperationException but got " + ex);
        // verify no change
        assertEquals(1, map.get("one"));
        assertEquals(2, map.get("two"));
        assertNull(map.get("x"));
    }

    @Test
    void testPutAllOverridesExistingEntries() {
        // GIVEN a map that already contains "key1" and "key2"
        map.put("key1", 100);
        map.put("key2", 200);
        // source contains a new value for "key1" and a brand‑new key "key3"
        Map<String, Integer> source = new HashMap<>();
        // should replace 100
        source.put("key1", 999);
        // new entry
        source.put("key3", 300);
        // WHEN putAll is called
        map.putAll(source);
        // THEN values are as expected
        assertEquals(999, map.get("key1"));
        assertEquals(200, map.get("key2"));
        assertEquals(300, map.get("key3"));
        // original 3 + 2 new/overridden entries
        assertEquals(5, map.size());
    }

    @Test
    void testPutAllWithSelfReferenceIsNoOp() {
        // Capture current state
        Map<String, Integer> before = new HashMap<>(map);
        // putAll with itself – should not alter the map nor throw
        map.putAll(map);
        // Verify map unchanged
        assertEquals(before, new HashMap<>(map));
    }

    @Test
    void testPutAllLargeMapBulkInsertion() {
        // Build a source map with 1,000 distinct entries
        Map<String, Integer> source = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            source.put("k" + i, i);
        }
        // WHEN putAll is invoked
        map.putAll(source);
        // THEN size increased by 1000 and all entries are present
        assertEquals(3 + 1000, map.size());
        for (int i = 0; i < 1000; i++) {
            assertEquals(i, map.get("k" + i));
        }
    }

    @Test
    void testPutAllWhenUnderlyingMapIsUnmodifiable() throws Exception {
        // Create a mock underlying map that throws UnsupportedOperationException on putAll
        @SuppressWarnings("unchecked")
        Map<String, Integer> mockMap = mock(Map.class);
        doThrow(new UnsupportedOperationException("unmodifiable")).when(mockMap).putAll(anyMap());
        // Inject the mock into the SealableMap instance via reflection
        Field field = SealableMap.class.getDeclaredField("map");
        field.setAccessible(true);
        field.set(map, mockMap);
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        // WHEN putAll is called, THEN the exception from the delegate is propagated
        assertThrows(UnsupportedOperationException.class, () -> map.putAll(source));
        // Verify that the mock's putAll was indeed invoked
        verify(mockMap, times(1)).putAll(source);
    }
}
