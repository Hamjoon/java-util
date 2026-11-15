package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_remove_2_Test {

    /**
     * Mocked delegate map used by the concrete test implementation.
     */
    @Mock
    private ConcurrentMap<Object, Object> internalMapMock;

    /**
     * Instance of the concrete test subclass.
     */
    private AbstractConcurrentNullSafeMap<String, String> mapUnderTest;

    /**
     * Simple concrete implementation that does not mask nulls.
     * The generic types are {@code <String, Object>} so that values can be any object
     * (e.g., Integer) while still enforcing a {@code String} key type for the
     * ClassCastException test.
     */
    private static class TestMap extends AbstractConcurrentNullSafeMap<String, Object> {

        TestMap(ConcurrentMap<Object, Object> internalMap) {
            super(internalMap);
        }

        @Override
        protected Object maskNullKey(String key) {
            // Identity behaviour – no sentinel handling for the test.
            return key;
        }

        @Override
        protected Object maskNullValue(Object value) {
            // Identity behaviour – no sentinel handling for the test.
            return value;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh concrete map for each test, using the mocked internal map.
        mapUnderTest = new TestMap(internalMapMock);
    }

    @AfterEach
    void teardownAfterEach() {
        // No explicit resources to clean up; Mockito handles mock reset automatically.
        // Resetting the mock makes each test independent.
        reset(internalMapMock);
    }

    private static Stream<Arguments> matchingEntryProvider() {
        return Stream.of(// Both key and value exist in the map with the exact pair.
        Arguments.of("a", 1), // Key associated with a null value; null handling is identity here.
        Arguments.of("c", null), // Both key and value are null.
        Arguments.of(null, null));
    }

    private static Stream<Arguments> nonMatchingEntryProvider() {
        return Stream.of(// Key exists but with a different value.
        Arguments.of("a", 2), // Key is absent from the map.
        Arguments.of("nonexistent", 1), // Null key present only with null value; non‑null value should fail.
        Arguments.of(null, 5));
    }

    @Test
    void remove_ReturnsTrue_WhenInternalMapRemovesEntry() {
        // GIVEN
        String key = "myKey";
        String value = "myValue";
        when(internalMapMock.remove(key, value)).thenReturn(true);
        // WHEN
        boolean result = mapUnderTest.remove(key, value);
        // THEN
        assertTrue(result, "remove should return true when the internal map reports removal");
        verify(internalMapMock, times(1)).remove(key, value);
    }

    @Test
    void remove_ReturnsFalse_WhenInternalMapDoesNotRemoveEntry() {
        // GIVEN
        String key = "anotherKey";
        String value = "anotherValue";
        when(internalMapMock.remove(key, value)).thenReturn(false);
        // WHEN
        boolean result = mapUnderTest.remove(key, value);
        // THEN
        assertFalse(result, "remove should return false when the internal map reports no removal");
        verify(internalMapMock, times(1)).remove(key, value);
    }

    @Test
    void remove_ForwardsNullKeyAndValue_Unmasked() {
        // GIVEN
        // The concrete test subclass does not mask nulls, so we can pass null directly.
        when(internalMapMock.remove(null, null)).thenReturn(true);
        // WHEN
        boolean result = mapUnderTest.remove(null, null);
        // THEN
        assertTrue(result, "remove should correctly forward null key/value to internal map");
        verify(internalMapMock, times(1)).remove(null, null);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] remove matching entry: {0}")
    @MethodSource("matchingEntryProvider")
    void remove_ReturnsTrue_WhenEntryMatches(Object key, Object value) {
        // GIVEN
        when(internalMapMock.remove(key, value)).thenReturn(true);
        // WHEN
        boolean result = mapUnderTest.remove(key, value);
        // THEN
        assertTrue(result, "remove should return true for a matching entry");
        verify(internalMapMock, times(1)).remove(key, value);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] remove non‑matching entry: {0}")
    @MethodSource("nonMatchingEntryProvider")
    void remove_ReturnsFalse_WhenEntryDoesNotMatch(Object key, Object value) {
        // GIVEN
        when(internalMapMock.remove(key, value)).thenReturn(false);
        // WHEN
        boolean result = mapUnderTest.remove(key, value);
        // THEN
        assertFalse(result, "remove should return false when the entry does not match");
        verify(internalMapMock, times(1)).remove(key, value);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void remove_ThrowsClassCastException_WhenKeyTypeMismatches() {
        // GIVEN
        // Integer instead of expected String.
        Object wrongKeyType = 123;
        Object value = 1;
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> mapUnderTest.remove(wrongKeyType, value), "remove should throw ClassCastException when key type is incompatible");
        // Verify that the internal map was never touched because the cast fails first.
        verifyNoInteractions(internalMapMock);
    }
}
