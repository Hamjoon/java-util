package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentMap;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.Date;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_maskNullValue_Test {

    /**
     * Mock backing map – not used directly by the tests but required by the constructor.
     */
    @Mock
    private ConcurrentMap<Object, Object> mockInternalMap;

    /**
     * Instance of the concrete test subclass.
     */
    private TestMap<String, String> testMap;

    /**
     * A simple concrete implementation used only for testing the protected helpers.
     */
    private static class TestMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        TestMap(ConcurrentMap<Object, Object> internalMap) {
            super(internalMap);
        }

        /**
         * Exposes the protected {@code maskNullValue} for verification.
         */
        Object exposeMaskNullValue(V value) {
            return maskNullValue(value);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise the concrete test map with the mocked internal map.
        testMap = new TestMap<>(mockInternalMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Verify that no unexpected interactions occurred with the mock.
        verifyNoMoreInteractions(mockInternalMap);
    }

    /**
     * Provides a wide range of inputs (including primitives, objects, arrays,
     * sentinel enum constants, etc.) together with the expected output from
     * {@code maskNullValue}.
     */
    private static Stream<Arguments> maskNullValueProvider() {
        // Sentinel constants
        AbstractConcurrentNullSafeMap.NullSentinel nullValueSentinel = AbstractConcurrentNullSafeMap.NullSentinel.NULL_VALUE;
        AbstractConcurrentNullSafeMap.NullSentinel nullKeySentinel = AbstractConcurrentNullSafeMap.NullSentinel.NULL_KEY;
        // Miscellaneous objects
        Object genericObject = new Object();
        int[] intArray = new int[] { 1, 2, 3 };
        Double nanDouble = Double.NaN;
        // 2023-01-01T00:00:00Z
        Date date = new Date(1672531200000L);
        return Stream.of(// null input – already covered above but kept for completeness
        Arguments.of(null, nullValueSentinel), // Simple non‑null values
        Arguments.of("sampleString", "sampleString"), Arguments.of(Integer.valueOf(42), Integer.valueOf(42)), Arguments.of(Boolean.TRUE, Boolean.TRUE), // Enum sentinel values (non‑null, should be returned unchanged)
        Arguments.of(nullValueSentinel, nullValueSentinel), Arguments.of(nullKeySentinel, nullKeySentinel), // Arbitrary objects
        Arguments.of(genericObject, genericObject), Arguments.of(intArray, intArray), // Special floating‑point value
        Arguments.of(nanDouble, nanDouble), // Empty string
        Arguments.of("", ""), // Standard library object
        Arguments.of(date, date));
    }

    @Test
    void maskNullValue_returnsSentinelWhenValueIsNull() {
        // GIVEN
        String value = null;
        // WHEN
        Object result = testMap.exposeMaskNullValue(value);
        // THEN
        assertEquals(AbstractConcurrentNullSafeMap.NullSentinel.NULL_VALUE, result, "maskNullValue should return the NULL_VALUE sentinel for null input");
    }

    @Test
    void maskNullValue_returnsOriginalWhenValueIsNonNull() {
        // GIVEN
        String value = "nonNullValue";
        // WHEN
        Object result = testMap.exposeMaskNullValue(value);
        // THEN
        assertSame(value, result, "maskNullValue should return the original object when it is not null");
    }

    @ParameterizedTest(name = "maskNullValue({0}) ⇒ {1}")
    @MethodSource("maskNullValueProvider")
    void maskNullValue_behaviourAcrossVariousInputs(Object input, Object expected) {
        // WHEN
        Object result = testMap.exposeMaskNullValue(input);
        // THEN
        if (expected == null) {
            // This branch is never hit because we never expect a literal null result;
            // the method returns a sentinel for null input.
            fail("Expected a sentinel, not null");
        } else if (expected instanceof Double && Double.isNaN((Double) expected)) {
            // NaN cannot be compared with equals or ==; use Double.isNaN.
            assertTrue(result instanceof Double && Double.isNaN((Double) result), "maskNullValue should return the same NaN Double instance");
        } else {
            // For all other cases we expect the exact same reference to be returned.
            assertSame(expected, result, () -> "maskNullValue should return the original reference for input: " + input);
        }
    }
}
