package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.concurrent.ConcurrentHashMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_remove_2_Test {

    private TestMap map;

    /**
     * Concrete implementation used for normal (non‑mocked) tests.
     */
    private static class TestMap extends AbstractConcurrentNullSafeMap<String, Integer> {

        TestMap() {
            super(new ConcurrentHashMap<>());
        }

        /**
         * Constructor used to inject a custom internal map (e.g., a Mockito mock).
         */
        TestMap(ConcurrentMap<Object, Object> internal) {
            super(internal);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        map = new TestMap();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to avoid cross‑test contamination.
        map.clear();
    }

    /* -------------------------------------------------------------
     *  Parameterized tests – successful removals
     * ------------------------------------------------------------- */
    private static Stream<Arguments> successfulRemovalProvider() {
        return Stream.of(// key, value, description
        Arguments.of("alpha", 1, "Exact non‑null key/value pair should be removed"), Arguments.of(null, 2, "Null key with non‑null value should be removed"), Arguments.of("beta", null, "Non‑null key with null value should be removed"), Arguments.of(null, null, "Both key and value null should be removed"));
    }

    /* -------------------------------------------------------------
     *  Parameterized tests – unsuccessful removals
     * ------------------------------------------------------------- */
    private static Stream<Arguments> unsuccessfulRemovalProvider() {
        return Stream.of(Arguments.of("alpha", 2, "Existing key with different value → false"), Arguments.of("gamma", 3, "Non‑existent key → false"), Arguments.of(null, 5, "Null key with non‑matching value → false"), Arguments.of("beta", 7, "Key with null stored value, supplied non‑null value → false"), Arguments.of(null, null, "Map empty, null key/value removal → false"), Arguments.of("delta", null, "Missing key, null value argument → false"));
    }

    @Test
    void removeExistingEntryWithNonNullKeyAndValue_returnsTrue() {
        // given
        map.put("key1", "value1");
        // when
        boolean removed = map.remove("key1", "value1");
        // then
        assertTrue(removed, "remove should return true when the entry exists");
        assertFalse(map.containsKey("key1"), "key should no longer be present after removal");
    }

    @Test
    void removeNonExistingEntry_returnsFalse() {
        // given – map is empty
        // when
        boolean removed = map.remove("missingKey", "missingValue");
        // then
        assertFalse(removed, "remove should return false when the entry does not exist");
    }

    @Test
    void removeEntryWithNullKey_returnsTrue() {
        // given
        map.put(null, "nullKeyValue");
        // when
        boolean removed = map.remove(null, "nullKeyValue");
        // then
        assertTrue(removed, "remove should handle a null key and return true when the entry exists");
        assertFalse(map.containsKey(null), "null key should be removed");
    }

    @Test
    void removeEntryWithNullValue_returnsTrue() {
        // given
        map.put("someKey", null);
        // when
        boolean removed = map.remove("someKey", null);
        // then
        assertTrue(removed, "remove should handle a null value and return true when the entry exists");
        assertFalse(map.containsKey("someKey"), "key whose value was null should be removed");
    }

    @Test
    void removeEntryWithNullKeyAndNullValue_returnsTrue() {
        // given
        map.put(null, null);
        // when
        boolean removed = map.remove(null, null);
        // then
        assertTrue(removed, "remove should handle both null key and null value");
        assertFalse(map.containsKey(null), "null key should be removed even when its value is null");
    }

    @ParameterizedTest(name = "[{index}] remove({0}, {1}) → true – {2}")
    @MethodSource("successfulRemovalProvider")
    void removeSuccessful(Object key, Integer value, String description) {
        // Arrange – put the entry that we expect to remove
        map.put((String) key, value);
        // Act
        boolean result = map.remove(key, value);
        // Assert
        assertTrue(result, "remove should return true for a matching entry");
        assertFalse(map.containsKey(key), "Entry should no longer be present after removal");
    }

    @ParameterizedTest(name = "[{index}] remove({0}, {1}) → false – {2}")
    @MethodSource("unsuccessfulRemovalProvider")
    void removeUnsuccessful(Object key, Integer value, String description) {
        // Arrange – populate map only with entries required for the test cases
        // (skip the case where map should stay empty)
        if (key != null && !"gamma".equals(key) && !"delta".equals(key)) {
            // For the cases that need an existing entry:
            // "alpha" → 1, null → 2, "beta" → null
            if ("alpha".equals(key)) {
                map.put("alpha", 1);
            } else if (key == null) {
                map.put(null, 2);
            } else if ("beta".equals(key)) {
                map.put("beta", null);
            }
        }
        // Act
        boolean result = map.remove(key, value);
        // Assert
        assertFalse(result, "remove should return false when entry does not match");
    }

    /* -------------------------------------------------------------
     *  Type‑mismatch handling – expect ClassCastException
     * ------------------------------------------------------------- */
    @Test
    void removeWithWrongKeyType_throwsClassCastException() {
        // Map is typed <String, Integer>
        map.put("valid", 1);
        // Passing an Integer as key should trigger a ClassCastException inside the cast
        assertThrows(ClassCastException.class, () -> map.remove(123, 1));
    }

    @Test
    void removeWithWrongValueType_throwsClassCastException() {
        map.put("epsilon", 10);
        // Passing a String as value should trigger a ClassCastException during ((V) value)
        assertThrows(ClassCastException.class, () -> map.remove("epsilon", "stringValue"));
    }

    /* -------------------------------------------------------------
     *  Internal map exception propagation
     * ------------------------------------------------------------- */
    @Test
    void removeWhenInternalMapIsNull_throwsNullPointerException() {
        // Construct a map instance with a null internalMap (via subclass constructor)
        TestMap mapWithNullInternal = new TestMap(null);
        // No need to put anything – the NPE occurs before any lookup
        assertThrows(NullPointerException.class, () -> mapWithNullInternal.remove("anyKey", 42));
    }

    @Test
    void removeWhenInternalMapThrowsUnsupportedOperationException_propagatesException(@Mock ConcurrentMap<Object, Object> mockedInternal) {
        // Configure the mock to throw UnsupportedOperationException on remove(Object,Object)
        when(mockedInternal.remove(any(), any())).thenThrow(new UnsupportedOperationException("remove not supported"));
        TestMap mapWithMock = new TestMap(mockedInternal);
        // Act & Assert
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> mapWithMock.remove("testKey", 99));
        assertEquals("remove not supported", ex.getMessage());
        // Verify that the underlying map's remove method was indeed invoked
        verify(mockedInternal, times(1)).remove(any(), any());
    }
}
