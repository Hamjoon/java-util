package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapConversions_toFloat_Test {

    /**
     * Mocked {@link Converter} that will be supplied to the method under test.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Real converter used for the majority of non‑map tests.
     */
    private DefaultConverter defaultConverter;

    @BeforeEach
    void init() {
        defaultConverter = new DefaultConverter();
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the mock after each test to avoid cross‑test interference.
        reset(mockConverter);
        reset(mockConverter);
    }

    /**
     * Supplies all non‑Map test cases extracted from the JSON specification.
     */
    private static Stream<Arguments> provideNonMapCases() {
        return Stream.of(// ----- simple null handling -------------------------------------------------
        Arguments.of(null, null, null), // ----- direct Float instance -------------------------------------------------
        Arguments.of(Float.valueOf(12.34f), 12.34f, null), // ----- String parsing --------------------------------------------------------
        Arguments.of("56.78", 56.78f, null), Arguments.of("  78.9  ", 78.9f, null), Arguments.of("+12.34", 12.34f, null), Arguments.of("1e3", 1000.0f, null), // ----- malformed strings -----------------------------------------------------
        Arguments.of("not a number", null, NumberFormatException.class), Arguments.of("", null, NumberFormatException.class), Arguments.of("12,34", null, NumberFormatException.class), Arguments.of("12ab34", null, NumberFormatException.class), Arguments.of("12 34", null, NumberFormatException.class), Arguments.of("1.2.3", null, NumberFormatException.class), Arguments.of("0x1A", null, NumberFormatException.class), // ----- numeric wrappers ------------------------------------------------------
        Arguments.of(Double.valueOf(9.01), 9.01f, null), Arguments.of(Integer.valueOf(42), 42.0f, null), Arguments.of(Long.valueOf(123456789L), 123456789.0f, null), Arguments.of(Short.valueOf((short) 5), 5.0f, null), Arguments.of(Byte.valueOf((byte) 2), 2.0f, null), // ----- BigDecimal / BigInteger -----------------------------------------------
        Arguments.of(new BigDecimal("3.14159"), 3.14159f, null), Arguments.of(new BigDecimal("1e40"), Float.POSITIVE_INFINITY, null), Arguments.of(new BigInteger("987654321"), 987654321.0f, null), Arguments.of(new BigInteger("12345678901234567890"), Float.POSITIVE_INFINITY, null), // ----- atomic types -----------------------------------------------------------
        Arguments.of(new AtomicInteger(7), 7.0f, null), Arguments.of(new AtomicLong(123L), 123.0f, null), Arguments.of(new AtomicBoolean(true), 1.0f, null), Arguments.of(new AtomicBoolean(false), 0.0f, null), // ----- date / time -----------------------------------------------------------
        Arguments.of(new Date(0L), 0.0f, null), Arguments.of(new Timestamp(123456789L), 123456789.0f, null), Arguments.of(new Calendar.Builder().setInstant(55555L).build(), 55555.0f, null), // ----- unsupported direct types ------------------------------------------------
        Arguments.of(new Object(), null, ClassCastException.class), Arguments.of(Boolean.TRUE, null, ClassCastException.class), Arguments.of(UUID.randomUUID(), null, ClassCastException.class), Arguments.of(LocalDate.now(), null, ClassCastException.class), Arguments.of(Instant.now(), null, ClassCastException.class), Arguments.of(URI.create("http://example.com"), null, ClassCastException.class), Arguments.of(new URL("http://example.com"), null, ClassCastException.class), Arguments.of(ZonedDateTime.now(), null, ClassCastException.class));
    }

    /**
     * Verify that a map containing a {@code String} value is converted to a {@code Float}
     * using the supplied {@link Converter}.
     */
    @Test
    void toFloat_usesConverterWhenMapContainsStringValue() {
        // GIVEN
        Map<String, Object> source = new HashMap<>();
        source.put("value", "123.45");
        // The Converter is expected to receive the raw map value ("123.45") and the target type Float.class.
        when(mockConverter.convert(eq("123.45"), eq(Float.class))).thenReturn(123.45f);
        // WHEN
        Float result = MapConversions.toFloat(source, mockConverter);
        // THEN
        assertNotNull(result, "Result should not be null");
        assertEquals(123.45f, result);
        // Verify that the converter was called exactly once with the expected arguments.
        verify(mockConverter, times(1)).convert(eq("123.45"), eq(Float.class));
    }

    /**
     * Verify that a map containing a {@code Number} value is converted to a {@code Float}
     * using the supplied {@link Converter}.
     */
    @Test
    void toFloat_usesConverterWhenMapContainsNumberValue() {
        // GIVEN
        Map<String, Object> source = new HashMap<>();
        // Integer value
        source.put("value", 99);
        when(mockConverter.convert(eq(99), eq(Float.class))).thenReturn(99.0f);
        // WHEN
        Float result = MapConversions.toFloat(source, mockConverter);
        // THEN
        assertNotNull(result);
        assertEquals(99.0f, result);
        verify(mockConverter, times(1)).convert(eq(99), eq(Float.class));
    }

    /**
     * Verify that passing {@code null} as the source returns {@code null}
     * and does not interact with the {@link Converter}.
     */
    @Test
    void toFloat_nullSourceReturnsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        Float result = MapConversions.toFloat(source, mockConverter);
        // THEN
        assertNull(result, "Result should be null when source is null");
        // Ensure the converter was never called.
        verifyNoInteractions(mockConverter);
    }

    /**
     * Verify that a map without a {@code "value"} key returns {@code null}
     * (the default behaviour of {@code fromMap} for missing keys).
     */
    @Test
    void toFloat_mapWithoutValueKeyReturnsNull() {
        // GIVEN
        // empty map
        Map<String, Object> source = new HashMap<>();
        // WHEN
        Float result = MapConversions.toFloat(source, mockConverter);
        // THEN
        assertNull(result);
        // No conversion should be attempted because there is no value to convert.
        verifyNoInteractions(mockConverter);
    }

    @ParameterizedTest(name = "[{index}] toFloat({0}) => {1}")
    @MethodSource("provideNonMapCases")
    void testToFloat_nonMap(Object from, Object expectedResult, Class<? extends Throwable> expectedException) {
        if (expectedException != null) {
            // Expect an exception – the result is irrelevant.
            assertThrows(expectedException, () -> MapConversions.toFloat(from, defaultConverter), () -> "Expected " + expectedException.getSimpleName() + " for input: " + from);
        } else {
            Float result = MapConversions.toFloat(from, defaultConverter);
            if (expectedResult == null) {
                assertNull(result, "Result should be null for input: " + from);
            } else if (expectedResult instanceof Float && Float.isInfinite((Float) expectedResult)) {
                assertNotNull(result);
                assertTrue(Float.isInfinite(result) && result > 0, "Result should be positive infinity for input: " + from);
            } else {
                assertNotNull(result, "Result should not be null for input: " + from);
                assertEquals((Float) expectedResult, result, () -> "Incorrect conversion for input: " + from);
            }
        }
    }

    @Test
    void toFloat_mapWithNullValueReturnsNull() {
        // GIVEN
        Map<String, Object> source = new HashMap<>();
        source.put("value", null);
        // WHEN
        Float result = MapConversions.toFloat(source, mockConverter);
        // THEN
        assertNull(result);
        // The implementation should short‑circuit before delegating to the converter.
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toFloat_mapWithUnsupportedValueThrowsClassCastException() {
        // GIVEN
        Map<String, Object> source = new HashMap<>();
        Object unsupported = new Object();
        source.put("value", unsupported);
        when(mockConverter.convert(eq(unsupported), eq(Float.class))).thenThrow(new ClassCastException("Unsupported type"));
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> MapConversions.toFloat(source, mockConverter), "Expected ClassCastException for unsupported map value");
        verify(mockConverter, times(1)).convert(eq(unsupported), eq(Float.class));
    }

    @Test
    void toFloat_nullConverterThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> MapConversions.toFloat("123.45", null), "Passing a null Converter should raise NPE");
    }
}
