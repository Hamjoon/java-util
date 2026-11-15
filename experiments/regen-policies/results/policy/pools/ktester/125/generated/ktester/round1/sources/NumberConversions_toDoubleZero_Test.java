package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toDoubleZero_Test {

    @Mock
    private Converter // a dummy converter – should not be touched
    mockConverter;

    @AfterEach
    void teardownAfterEach() {
        // Verify that the mock converter was never interacted with.
        verifyNoInteractions(mockConverter);
    }

    /**
     * Supplies all combinations of {@code from} values and a flag that
     * indicates whether a non‑null {@link Converter} should be passed.
     */
    private static Stream<Arguments> toDoubleZeroArguments() {
        // Custom object whose toString throws – used to ensure the method does not call it.
        Object throwingToString = new Object() {

            @Override
            public String toString() {
                throw new RuntimeException("toString should not be invoked");
            }
        };
        return Stream.of(// -----------------------------------------------------------------
        // Cases where the converter is null (the majority of scenarios)
        // -----------------------------------------------------------------
        Arguments.of(null, false), // Integer
        Arguments.of(123, false), // String
        Arguments.of("42.5", false), // Double
        Arguments.of(3.14, false), // Object with dangerous toString
        Arguments.of(throwingToString, false), // Long
        Arguments.of(Long.valueOf(9876543210L), false), // Float
        Arguments.of(Float.valueOf(3.5f), false), // BigDecimal
        Arguments.of(new BigDecimal("12345.6789"), false), // BigInteger
        Arguments.of(new BigInteger("98765432109876543210"), false), // AtomicInteger
        Arguments.of(new AtomicInteger(42), false), // AtomicLong
        Arguments.of(new AtomicLong(123456789L), false), // Boolean
        Arguments.of(Boolean.TRUE, false), // Character
        Arguments.of('A', false), // java.util.Date
        Arguments.of(new Date(), false), // Calendar
        Arguments.of(Calendar.getInstance(), false), // LocalDate
        Arguments.of(LocalDate.now(), false), // LocalDateTime
        Arguments.of(LocalDateTime.now(), false), // ZonedDateTime
        Arguments.of(ZonedDateTime.now(), false), // OffsetDateTime
        Arguments.of(OffsetDateTime.now(), false), // Instant
        Arguments.of(Instant.now(), false), // Duration
        Arguments.of(Duration.ofSeconds(10), false), // Year
        Arguments.of(Year.of(2024), false), // Timestamp
        Arguments.of(new Timestamp(System.currentTimeMillis()), false), // int array
        Arguments.of(new int[] { 1, 2, 3 }, false), // List<Integer>
        Arguments.of(Arrays.asList(1, 2, 3), false), // Map<String,Integer>
        Arguments.of(Collections.singletonMap("key", 1), false), // Enum
        Arguments.of(DayOfWeek.MONDAY, false), // --------------------------------------------------------------
        // Cases where a non‑null Converter is supplied (must still be ignored)
        // --------------------------------------------------------------
        // non‑null from, non‑null converter
        Arguments.of(123, true), // null from, non‑null converter (the converter would throw if used)
        Arguments.of(null, true));
    }

    @Test
    void toDoubleZeroReturnsZeroWhenSourceIsNull() {
        Double result = NumberConversions.toDoubleZero(null, mockConverter);
        assertNotNull(result, "Result must not be null");
        assertEquals(0.0, result, "Result should be Double zero");
    }

    @Test
    void toDoubleZeroReturnsZeroForVariousNonNullSources() {
        Object[] sources = { // Integer
        123, // Double
        45.67, // String
        "99.99", new java.math.BigDecimal("10.5"), new java.util.Date(), new java.util.concurrent.atomic.AtomicLong(5L) };
        for (Object src : sources) {
            Double result = NumberConversions.toDoubleZero(src, mockConverter);
            assertNotNull(result, "Result must not be null for source: " + src);
            assertEquals(0.0, result, "Result should be Double zero for source: " + src);
        }
    }

    /**
     * Parameterised test that covers every input variant described in the
     * JSON test‑case list.  The second argument indicates whether a non‑null
     * {@link Converter} should be supplied.  When a non‑null converter is
     * required and the source value is {@code null}, a special mock that
     * throws on any invocation is created to prove that the method truly
     * ignores the converter.
     */
    @ParameterizedTest(name = "[{index}] from={0}, useConverter={1}")
    @MethodSource("toDoubleZeroArguments")
    void toDoubleZeroAlwaysReturnsZero(Object from, boolean useConverter) {
        // Choose the appropriate Converter instance.
        Converter converter;
        if (useConverter) {
            if (from == null) {
                // A converter that would throw if any method were called.
                converter = mock(Converter.class, invocation -> {
                    throw new RuntimeException("Converter should not be used");
                });
            } else {
                // Use the injected mock – it must stay untouched.
                converter = mockConverter;
            }
        } else {
            converter = null;
        }
        // Invoke the method under test.
        Double result = NumberConversions.toDoubleZero(from, converter);
        // Verify the contract: always the shared constant zero.
        assertNotNull(result, "Result must not be null");
        assertEquals(0.0, result, "Result should be Double zero");
    }
}
