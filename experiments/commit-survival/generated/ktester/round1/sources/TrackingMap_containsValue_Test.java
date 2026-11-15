package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TrackingMap_containsValue_Test {

    // -----------------------------------------------------------------------
    private TrackingMap<String, Integer> trackingMap;

    @Mock
    private Map<String, Integer> // injected mock by Mockito
    internalMap;

    @BeforeEach
    void setupBeforeEach() {
        // The mock `internalMap` is already created by the @Mock annotation.
        // Construct the TrackingMap with the mocked delegate.
        trackingMap = new TrackingMap<>(internalMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mock interactions to keep each test isolated.
        reset(internalMap);
    }

    private static Stream<Arguments> basicDelegationProvider() {
        // Prepare a distinct object for the "new Object()" case so the same reference is used for stubbing & call
        Object freshObject = new Object();
        return Stream.of(Arguments.of(null, false, "When the wrapped internalMap does not contain null, containsValue should return false."), Arguments.of("test", true, "When the wrapped internalMap contains the string \"test\", containsValue should return true."), Arguments.of(42, false, "When the wrapped internalMap does not contain the integer 42, containsValue should return false."), Arguments.of(freshObject, false, "When the wrapped internalMap does not contain a newly created object (different reference), containsValue should return false."));
    }

    private static Stream<Arguments> edgeAndSpecialCasesProvider() {
        // Objects needed for reference‑equality tests
        // will be used for the "true" case
        Object sameRefObject = new Object();
        // will be used for the "false" case
        Object differentRefObject = new Object();
        // Distinct String objects with equal content
        String distinctString = new String("test");
        // Distinct Integer objects (autoboxing equality)
        Integer distinctInteger = Integer.valueOf(5);
        // List instances
        List<Integer> equalList = Arrays.asList(1, 2, 3);
        List<Integer> emptyList = Collections.emptyList();
        return Stream.of(// null present in delegate → true
        Arguments.of(null, true, "When the wrapped internalMap contains a null value, containsValue(null) should return true."), // empty string present → true
        Arguments.of("", true, "When the wrapped internalMap contains an empty string, containsValue(\"\") should return true."), // distinct String object equal to "test" → true (value equality)
        Arguments.of(distinctString, true, "When the wrapped internalMap contains a distinct String object equal to \"test\", containsValue should return true (value equality, not reference)."), // distinct Integer object with value 5 → true (autoboxing equality)
        Arguments.of(distinctInteger, true, "When the wrapped internalMap contains an Integer with value 5, containsValue(new Integer(5)) should return true (autoboxing equality)."), // same reference object present → true
        Arguments.of(sameRefObject, true, "When the wrapped internalMap contains the exact same object reference passed to containsValue, it should return true."), // different reference object (same class but not same instance) → false
        Arguments.of(differentRefObject, false, "When the wrapped internalMap contains a different object reference (same class but not the same instance), containsValue should return false."), // empty list not present → false
        Arguments.of(emptyList, false, "When the wrapped internalMap does not contain an empty list, containsValue(emptyList) should return false."), // list equal to [1,2,3] present → true
        Arguments.of(equalList, true, "When the wrapped internalMap contains a list equal to [1,2,3], containsValue should return true regardless of list implementation."));
    }

    // -----------------------------------------------------------------------
    @Test
    void containsValue_ReturnsTrue_WhenDelegateContainsValue() {
        // GIVEN
        Integer searchedValue = 42;
        when(internalMap.containsValue(searchedValue)).thenReturn(true);
        // WHEN
        boolean result = trackingMap.containsValue(searchedValue);
        // THEN
        assertTrue(result, "containsValue should return true when the wrapped map reports true");
        verify(internalMap, times(1)).containsValue(searchedValue);
    }

    @Test
    void containsValue_ReturnsFalse_WhenDelegateDoesNotContainValue() {
        // GIVEN
        Integer searchedValue = 99;
        when(internalMap.containsValue(searchedValue)).thenReturn(false);
        // WHEN
        boolean result = trackingMap.containsValue(searchedValue);
        // THEN
        assertFalse(result, "containsValue should return false when the wrapped map reports false");
        verify(internalMap, times(1)).containsValue(searchedValue);
    }

    @Test
    void containsValue_HandlesNullValueGracefully() {
        // GIVEN
        when(internalMap.containsValue(null)).thenReturn(false);
        // WHEN
        boolean result = trackingMap.containsValue(null);
        // THEN
        assertFalse(result, "containsValue should correctly handle a null argument");
        verify(internalMap, times(1)).containsValue(null);
    }

    /**
     * Covers the "containsValue - basic delegation" group.
     *
     * @param value       the argument passed to {@code containsValue}
     * @param expected    the boolean result we expect from the delegate
     * @param description a short description for readability
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("basicDelegationProvider")
    void containsValue_BasicDelegation(Object value, boolean expected, String description) {
        // GIVEN – stub the delegate to return the expected value for the supplied argument
        when(internalMap.containsValue(value)).thenReturn(expected);
        // WHEN
        boolean result = trackingMap.containsValue(value);
        // THEN – result matches expectation and delegate was called once with the same argument
        assertEquals(expected, result, () -> "containsValue should return " + expected + " for argument: " + value);
        verify(internalMap, times(1)).containsValue(value);
    }

    /**
     * Covers the "containsValue - edge and special cases" group.
     *
     * @param value       the argument passed to {@code containsValue}
     * @param expected    the boolean result we expect from the delegate
     * @param description a short description for readability
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("edgeAndSpecialCasesProvider")
    void containsValue_EdgeAndSpecialCases(Object value, boolean expected, String description) {
        // GIVEN – stub the delegate for the exact argument we will pass
        when(internalMap.containsValue(value)).thenReturn(expected);
        // WHEN
        boolean result = trackingMap.containsValue(value);
        // THEN – verify result and interaction
        assertEquals(expected, result, () -> "containsValue should return " + expected + " for argument: " + value);
        verify(internalMap, times(1)).containsValue(value);
    }
}
