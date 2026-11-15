package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
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

    @Mock
    private ConcurrentMap<Object, Object> mockInternalMap;

    private TestMap mapUnderTest;

    /**
     * Concrete subclass used only for testing the abstract class.
     */
    private static final class TestMap extends AbstractConcurrentNullSafeMap<String, String> {

        TestMap(ConcurrentMap<Object, Object> internal) {
            super(internal);
        }
    }

    /**
     * Subclass that deliberately passes a {@code null} internal map to trigger NPEs.
     */
    private static final class NullInternalMapTest extends AbstractConcurrentNullSafeMap<String, String> {

        NullInternalMapTest() {
            super(null);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise mocks and the map instance before each test.
        MockitoAnnotations.openMocks(this);
        mapUnderTest = new TestMap(mockInternalMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mock interactions to avoid cross‑test contamination.
        reset(mockInternalMap);
    }

    /**
     * Provides arguments for the {@code containsValue_additional} group.
     *
     * Each {@link Arguments} consists of:
     * <ul>
     *   <li>input value (Object)</li>
     *   <li>whether the internal map should report {@code true}</li>
     *   <li>expected result of {@code containsValue}</li>
     *   <li>description (used only for readability)</li>
     * </ul>
     */
    private static Stream<Arguments> additionalValuesProvider() {
        Object nullSentinel = AbstractConcurrentNullSafeMap.NullSentinel.NULL_VALUE;
        Object nullKeySentinel = AbstractConcurrentNullSafeMap.NullSentinel.NULL_KEY;
        return Stream.of(// Argument is the sentinel NULL_VALUE itself
        Arguments.of(nullSentinel, true, true, "Argument is NullSentinel.NULL_VALUE and map contains it → true"), Arguments.of(nullSentinel, false, false, "Argument is NullSentinel.NULL_VALUE and map does NOT contain it → false"), // Argument is the sentinel used for keys – should behave like any other value
        Arguments.of(nullKeySentinel, false, false, "Argument is NullSentinel.NULL_KEY; internal map does not contain it → false"), // Integer values
        Arguments.of(123, true, true, "Integer 123 present in internal map → true"), Arguments.of(456, false, false, "Integer 456 absent from internal map → false"), // Distinct String instance equal to stored value
        Arguments.of(new String("duplicateString"), true, true, "Distinct String equal to stored value → true"), // Any non‑null value when the map is empty
        Arguments.of("anyValue", false, false, "Empty internal map, any value → false"));
    }

    @Test
    void containsValue_withNonNullValue_delegatesToInternalMap() {
        // GIVEN
        String value = "someValue";
        when(mockInternalMap.containsValue(value)).thenReturn(true);
        // WHEN
        boolean result = mapUnderTest.containsValue(value);
        // THEN
        assertTrue(result, "containsValue should return true when internal map reports true");
        verify(mockInternalMap, times(1)).containsValue(value);
        verifyNoMoreInteractions(mockInternalMap);
    }

    @Test
    void containsValue_withNonNullValue_returnsFalseWhenNotPresent() {
        // GIVEN
        String value = "absentValue";
        when(mockInternalMap.containsValue(value)).thenReturn(false);
        // WHEN
        boolean result = mapUnderTest.containsValue(value);
        // THEN
        assertFalse(result, "containsValue should return false when internal map reports false");
        verify(mockInternalMap, times(1)).containsValue(value);
        verifyNoMoreInteractions(mockInternalMap);
    }

    @Test
    void containsValue_withNullValue_usesNullSentinel() {
        // GIVEN
        // The sentinel used for null values is the enum constant NullSentinel.NULL_VALUE.
        // We cannot reference the enum directly (it is package‑private), but we can
        // obtain the exact object instance via reflection on the enum type.
        Object nullSentinel = AbstractConcurrentNullSafeMap.NullSentinel.NULL_VALUE;
        when(mockInternalMap.containsValue(nullSentinel)).thenReturn(true);
        // WHEN
        boolean result = mapUnderTest.containsValue(null);
        // THEN
        assertTrue(result, "containsValue(null) should delegate to internalMap with NullSentinel.NULL_VALUE");
        verify(mockInternalMap, times(1)).containsValue(nullSentinel);
        verifyNoMoreInteractions(mockInternalMap);
    }

    @Test
    void containsValue_withNullValue_returnsFalseWhenSentinelAbsent() {
        // GIVEN
        Object nullSentinel = AbstractConcurrentNullSafeMap.NullSentinel.NULL_VALUE;
        when(mockInternalMap.containsValue(nullSentinel)).thenReturn(false);
        // WHEN
        boolean result = mapUnderTest.containsValue(null);
        // THEN
        assertFalse(result, "containsValue(null) should return false when sentinel is not present");
        verify(mockInternalMap, times(1)).containsValue(nullSentinel);
        verifyNoMoreInteractions(mockInternalMap);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("additionalValuesProvider")
    void containsValue_additional_cases(Object inputValue, boolean internalMapReturns, boolean expectedResult, String description) {
        // GIVEN – configure mock according to the scenario
        when(mockInternalMap.containsValue(inputValue)).thenReturn(internalMapReturns);
        // WHEN
        boolean result = mapUnderTest.containsValue(inputValue);
        // THEN
        assertEquals(expectedResult, result, () -> "Unexpected result for input [" + inputValue + "]: " + description);
        verify(mockInternalMap, times(1)).containsValue(inputValue);
        verifyNoMoreInteractions(mockInternalMap);
    }

    @Test
    void containsValue_nullInternalMap_throwsNPE_forNullArgument() {
        // GIVEN a map whose internalMap reference is null
        NullInternalMapTest mapWithNullInternal = new NullInternalMapTest();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> mapWithNullInternal.containsValue(null), "Calling containsValue(null) with a null internalMap should throw NPE");
    }

    @Test
    void containsValue_nullInternalMap_throwsNPE_forNonNullArgument() {
        // GIVEN a map whose internalMap reference is null
        NullInternalMapTest mapWithNullInternal = new NullInternalMapTest();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> mapWithNullInternal.containsValue("any"), "Calling containsValue(non‑null) with a null internalMap should throw NPE");
    }

    @Test
    void containsValue_internalMapThrowsRuntimeException_forNullArgument() {
        // GIVEN a mock that throws RuntimeException when containsValue is called with the sentinel
        ConcurrentMap<Object, Object> faultyMap = mock(ConcurrentMap.class);
        Object nullSentinel = AbstractConcurrentNullSafeMap.NullSentinel.NULL_VALUE;
        when(faultyMap.containsValue(nullSentinel)).thenThrow(new RuntimeException("faulty map"));
        TestMap faultyTestMap = new TestMap(faultyMap);
        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class, () -> faultyTestMap.containsValue(null), "RuntimeException from internalMap should propagate");
        assertEquals("faulty map", ex.getMessage());
    }

    @Test
    void containsValue_internalMapThrowsRuntimeException_forNonNullArgument() {
        // GIVEN a mock that throws RuntimeException for any value
        ConcurrentMap<Object, Object> faultyMap = mock(ConcurrentMap.class);
        when(faultyMap.containsValue(any())).thenThrow(new RuntimeException("generic failure"));
        TestMap faultyTestMap = new TestMap(faultyMap);
        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class, () -> faultyTestMap.containsValue(42), "RuntimeException from internalMap should propagate for non‑null values");
        assertEquals("generic failure", ex.getMessage());
    }
}
