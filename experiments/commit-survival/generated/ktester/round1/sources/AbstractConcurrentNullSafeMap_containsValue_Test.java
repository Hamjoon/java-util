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
class AbstractConcurrentNullSafeMap_containsValue_Test {

    /**
     * The mock that represents the internal map used by the abstract class.
     */
    @Mock
    private ConcurrentMap<Object, Object> internalMap;

    /**
     * A concrete instance of the abstract class created via an anonymous subclass.
     */
    private AbstractConcurrentNullSafeMap<String, String> testMap;

    @BeforeEach
    void setupBeforeEach() {
        // Create a concrete instance that delegates to the mocked internal map.
        testMap = new AbstractConcurrentNullSafeMap<String, String>(internalMap) {
        };
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the mock after each test to avoid interaction bleed‑over.
        reset(internalMap);
    }

    /**
     * Provides arguments for the additional scenario tests.
     *
     * Each argument consists of:
     *   1. The value to pass to {@code containsValue}.
     *   2. The boolean that the mocked internal map should return for that value.
     *   3. The expected result of {@code containsValue}.
     *   4. A short description (used only for readability in the test name).
     */
    private static Stream<Arguments> additionalScenariosProvider() {
        // Helper objects used in several cases
        StringBuilder customObject = new StringBuilder("test");
        StringBuilder unstoredCustomObject = new StringBuilder("notStored");
        return Stream.of(// sentinel NULL_VALUE passed as a regular (non‑null) argument – present
        Arguments.of(AbstractConcurrentNullSafeMap.NullSentinel.NULL_VALUE, true, true, "sentinel NULL_VALUE present as value"), // sentinel NULL_VALUE passed – absent
        Arguments.of(AbstractConcurrentNullSafeMap.NullSentinel.NULL_VALUE, false, false, "sentinel NULL_VALUE absent"), // sentinel NULL_KEY passed – should be treated as ordinary value (absent)
        Arguments.of(AbstractConcurrentNullSafeMap.NullSentinel.NULL_KEY, false, false, "sentinel NULL_KEY treated as normal value, absent"), // custom user object stored – present
        Arguments.of(customObject, true, true, "custom user object present"), // custom user object not stored – absent
        Arguments.of(unstoredCustomObject, false, false, "custom user object absent"), // null argument with empty map – sentinel absent
        Arguments.of(null, false, false, "null argument, empty map (sentinel absent)"), // non‑null argument with empty map – absent
        Arguments.of("anyNonNull", false, false, "non‑null argument, empty map"), // verify no side‑effects: after call map size unchanged (mocked size not used,
        // we just ensure no mutating interactions occurred)
        Arguments.of("apple", true, true, "verify containsValue does not modify map"));
    }

    /**
     * Utility to produce a readable representation of the argument for assertion messages.
     */
    private static String stringify(Object obj) {
        if (obj == null)
            return "null";
        if (obj instanceof AbstractConcurrentNullSafeMap.NullSentinel) {
            return "NullSentinel." + ((AbstractConcurrentNullSafeMap.NullSentinel) obj).name();
        }
        return obj.toString();
    }

    @Test
    void containsValue_nullValue_usesNullValueSentinel() {
        // GIVEN
        when(internalMap.containsValue(AbstractConcurrentNullSafeMap.NullSentinel.NULL_VALUE)).thenReturn(true);
        // WHEN
        boolean result = testMap.containsValue(null);
        // THEN
        assertTrue(result, "containsValue(null) should return true when the sentinel is present");
        verify(internalMap, times(1)).containsValue(AbstractConcurrentNullSafeMap.NullSentinel.NULL_VALUE);
        verifyNoMoreInteractions(internalMap);
    }

    @Test
    void containsValue_nullValue_returnsFalseWhenSentinelAbsent() {
        // GIVEN
        when(internalMap.containsValue(AbstractConcurrentNullSafeMap.NullSentinel.NULL_VALUE)).thenReturn(false);
        // WHEN
        boolean result = testMap.containsValue(null);
        // THEN
        assertFalse(result, "containsValue(null) should return false when the sentinel is absent");
        verify(internalMap, times(1)).containsValue(AbstractConcurrentNullSafeMap.NullSentinel.NULL_VALUE);
        verifyNoMoreInteractions(internalMap);
    }

    @Test
    void containsValue_nonNullValue_delegatesDirectly() {
        // GIVEN
        String value = "someValue";
        when(internalMap.containsValue(value)).thenReturn(true);
        // WHEN
        boolean result = testMap.containsValue(value);
        // THEN
        assertTrue(result, "containsValue(non‑null) should delegate to the internal map");
        verify(internalMap, times(1)).containsValue(value);
        verifyNoMoreInteractions(internalMap);
    }

    @Test
    void containsValue_nonNullValue_returnsFalseWhenAbsent() {
        // GIVEN
        String value = "missingValue";
        when(internalMap.containsValue(value)).thenReturn(false);
        // WHEN
        boolean result = testMap.containsValue(value);
        // THEN
        assertFalse(result, "containsValue(non‑null) should return false when the value is absent");
        verify(internalMap, times(1)).containsValue(value);
        verifyNoMoreInteractions(internalMap);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("additionalScenariosProvider")
    void containsValue_additionalScenarios(Object inputValue, boolean internalMapResponse, boolean expectedResult, String description) {
        // Arrange: configure the mock to return the supplied response for the exact argument.
        when(internalMap.containsValue(inputValue)).thenReturn(internalMapResponse);
        // Act
        boolean result = testMap.containsValue(inputValue);
        // Assert
        assertEquals(expectedResult, result, () -> "containsValue(" + stringify(inputValue) + ") expected to be " + expectedResult);
        verify(internalMap, times(1)).containsValue(inputValue);
        // No other interactions (e.g., puts, removes) should have occurred.
        verifyNoMoreInteractions(internalMap);
    }
}
