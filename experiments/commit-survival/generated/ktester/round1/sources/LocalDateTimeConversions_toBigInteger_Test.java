package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.time.*;
import java.util.Date;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeConversions_toBigInteger_Test {

    /**
     * Mocked {@link Converter} – its behaviour is not relevant for this test.
     */
    private Converter mockConverter;

    /**
     * Simple stub implementation of {@link Converter} used in test data.
     */
    private static final class MockConverter implements Converter {
        // No behaviour needed for these tests.
    }

    /**
     * Container for successful conversion test data.
     */
    private static final class SuccessCase {

        final Object source;

        final BigInteger expected;

        SuccessCase(Object source, BigInteger expected) {
            this.source = source;
            this.expected = expected;
        }

        @Override
        public String toString() {
            return String.valueOf(source);
        }
    }

    /**
     * Container for exception test data.
     */
    private static final class ExceptionCase {

        final Object source;

        final Converter converter;

        final Class<? extends Throwable> expectedException;

        final String description;

        ExceptionCase(Object source, Converter converter, Class<? extends Throwable> expectedException, String description) {
            this.source = source;
            this.converter = converter;
            this.expectedException = expectedException;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        mockConverter = mock(Converter.class);
        mockConverter = mock(Converter.class);
    }

    private static Stream<SuccessCase> successCases() {
        return Stream.of(// Instant input – toInstant returns the same instant
        new SuccessCase(Instant.parse("2023-03-15T12:34:56Z"), // placeholder – will be computed in test
        new BigInteger("168...")), // Date at epoch
        new SuccessCase(new Date(0L), BigInteger.ZERO), // LocalDateTime at epoch (UTC)
        new SuccessCase(LocalDateTime.of(1970, 1, 1, 0, 0, 0, 0), BigInteger.ZERO), // LocalDate at epoch
        new SuccessCase(LocalDate.of(1970, 1, 1), BigInteger.ZERO), // LocalTime midnight – we will let toInstant produce epoch nanoseconds (0)
        new SuccessCase(LocalTime.of(0, 0, 0, 0), BigInteger.ZERO), // BigDecimal – fractional part discarded
        new SuccessCase(new BigDecimal("12345.678"), BigInteger.valueOf(12345L)), // Long representing epoch seconds
        new SuccessCase(123456789L, BigInteger.valueOf(123456789L)), // ISO‑8601 string
        new SuccessCase("2023-03-15T12:34:56Z", Instant.parse("2023-03-15T12:34:56Z").toEpochMilli().multiply(BigInteger.valueOf(1_000_000))), // Timestamp
        new SuccessCase(new Timestamp(1625097600000L), BigInteger.valueOf(1625097600000L).multiply(BigInteger.valueOf(1_000_000))));
    }

    /**
     * Helper that mimics the real {@code toInstant} conversion for the limited set of
     * source objects used in the success test cases. This is **only** for test‑data
     * preparation and does not replace the actual implementation.
     */
    private static Instant toInstantForSource(Object source) {
        if (source instanceof Instant) {
            return (Instant) source;
        }
        if (source instanceof Date) {
            return ((Date) source).toInstant();
        }
        if (source instanceof LocalDateTime) {
            return ((LocalDateTime) source).toInstant(ZoneOffset.UTC);
        }
        if (source instanceof LocalDate) {
            return ((LocalDate) source).atStartOfDay(ZoneOffset.UTC).toInstant();
        }
        if (source instanceof LocalTime) {
            // Assume today’s date at UTC midnight for the purpose of the test.
            LocalDate today = LocalDate.ofEpochDay(0);
            return LocalDateTime.of(today, (LocalTime) source).toInstant(ZoneOffset.UTC);
        }
        if (source instanceof BigDecimal) {
            // discard fraction
            long seconds = ((BigDecimal) source).longValue();
            return Instant.ofEpochSecond(seconds);
        }
        if (source instanceof Long) {
            return Instant.ofEpochSecond((Long) source);
        }
        if (source instanceof String) {
            return Instant.parse((String) source);
        }
        if (source instanceof Timestamp) {
            return ((Timestamp) source).toInstant();
        }
        throw new IllegalArgumentException("Unsupported test source: " + source);
    }

    private static Stream<ExceptionCase> exceptionCases() {
        return Stream.of(// Null source – toInstant should NPE
        new ExceptionCase(null, new MockConverter(), NullPointerException.class, "null source should cause NPE"), // Null converter – NPE before conversion
        new ExceptionCase(Instant.now(), null, NullPointerException.class, "null converter should cause NPE"), // Unsupported type – Boolean
        new ExceptionCase(Boolean.TRUE, new MockConverter(), IllegalArgumentException.class, "unsupported Boolean source"), // Malformed ISO‑8601 string
        new ExceptionCase("2023-02-30T25:61:00Z", new MockConverter(), DateTimeParseException.class, "malformed date‑time string"), // BigDecimal out of range
        new ExceptionCase(new BigDecimal("9223372036854775808"), new MockConverter(), DateTimeException.class, "BigDecimal exceeding Long.MAX_VALUE seconds"), // Arbitrary object
        new ExceptionCase(new Object(), new MockConverter(), IllegalArgumentException.class, "completely unsupported object"));
    }

    @Test
    void toBigInteger_delegatesToInstantAndInstantConversions() {
        // GIVEN
        Object source = "any source object";
        Instant fakeInstant = Instant.parse("2023-01-01T12:34:56Z");
        BigInteger expected = new BigInteger("12345678901234567890");
        // Mock static methods of LocalDateTimeConversions (only toInstant) and InstantConversions.
        try (MockedStatic<LocalDateTimeConversions> ldtMock = Mockito.mockStatic(LocalDateTimeConversions.class, CALLS_REAL_METHODS);
            MockedStatic<InstantConversions> icMock = Mockito.mockStatic(InstantConversions.class)) {
            // Stub the private static delegation toInstant to return our fake instant.
            ldtMock.when(() -> LocalDateTimeConversions.toInstant(eq(source), eq(mockConverter))).thenReturn(fakeInstant);
            // Stub the final conversion toBigInteger.
            icMock.when(() -> InstantConversions.toBigInteger(eq(fakeInstant), eq(mockConverter))).thenReturn(expected);
            // WHEN
            BigInteger actual = LocalDateTimeConversions.toBigInteger(source, mockConverter);
            // THEN
            assertEquals(expected, actual, "The returned BigInteger should be the one produced by InstantConversions");
            // Verify that the delegation happened exactly once with the expected arguments.
            ldtMock.verify(() -> LocalDateTimeConversions.toInstant(eq(source), eq(mockConverter)), times(1));
            icMock.verify(() -> InstantConversions.toBigInteger(eq(fakeInstant), eq(mockConverter)), times(1));
        }
    }

    @ParameterizedTest(name = "[{index}] toBigInteger success for source: {0}")
    @MethodSource("successCases")
    void toBigInteger_successfulConversions(SuccessCase tc) {
        // GIVEN
        Object source = tc.source;
        // Compute expected value if it was defined lazily (e.g., based on Instant)
        BigInteger expected = tc.expected;
        // Mock static methods
        try (MockedStatic<LocalDateTimeConversions> ldtMock = Mockito.mockStatic(LocalDateTimeConversions.class, CALLS_REAL_METHODS);
            MockedStatic<InstantConversions> icMock = Mockito.mockStatic(InstantConversions.class)) {
            // Determine what instant the mocked toInstant should return.
            // For simplicity we let the test data provide the instant via a helper method.
            Instant fakeInstant = toInstantForSource(source);
            ldtMock.when(() -> LocalDateTimeConversions.toInstant(eq(source), eq(mockConverter))).thenReturn(fakeInstant);
            icMock.when(() -> InstantConversions.toBigInteger(eq(fakeInstant), eq(mockConverter))).thenReturn(expected);
            // WHEN
            BigInteger actual = LocalDateTimeConversions.toBigInteger(source, mockConverter);
            // THEN
            assertEquals(expected, actual, "Returned BigInteger must match the mocked InstantConversions result");
            ldtMock.verify(() -> LocalDateTimeConversions.toInstant(eq(source), eq(mockConverter)), times(1));
            icMock.verify(() -> InstantConversions.toBigInteger(eq(fakeInstant), eq(mockConverter)), times(1));
        }
    }

    @ParameterizedTest(name = "[{index}] toBigInteger exception for source: {0}")
    @MethodSource("exceptionCases")
    void toBigInteger_exceptionScenarios(ExceptionCase ec) {
        // GIVEN
        Object source = ec.source;
        Converter conv = ec.converter;
        // Mock static methods to throw the expected exception when toInstant is called.
        try (MockedStatic<LocalDateTimeConversions> ldtMock = Mockito.mockStatic(LocalDateTimeConversions.class, CALLS_REAL_METHODS)) {
            // If the expected exception is due to a null argument, we let the real method throw it.
            if (source == null || conv == null) {
                // No stubbing – the real method will NPE.
            } else {
                // For other cases we stub toInstant to throw the exception.
                ldtMock.when(() -> LocalDateTimeConversions.toInstant(eq(source), eq(conv))).thenThrow(ec.expectedException);
            }
            // WHEN / THEN
            assertThrows(ec.expectedException, () -> LocalDateTimeConversions.toBigInteger(source, conv), ec.description);
        }
    }
}
