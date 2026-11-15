package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.convert.InstantConversions;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateConversions_toBigDecimal_Test {

    /**
     * A mock {@link Converter} used for the tests that need a converter.
     */
    @Mock
    Converter mockConverter;

    /**
     * The {@link Instant} that will be used as the expected intermediate value.
     */
    private static Instant sampleInstant;

    /**
     * The expected {@link BigDecimal} result for {@code sampleInstant}.
     */
    private static BigDecimal expectedBigDecimal;

    /**
     * A reusable {@link Instant} used for the “already an Instant” test case.
     */
    private static final Instant FIXED_INSTANT = Instant.parse("2023-08-15T12:34:56Z");

    @BeforeAll
    static void setupBeforeAll() {
        // Choose a fixed instant so the test is deterministic.
        sampleInstant = Instant.parse("2023-01-01T12:34:56.789Z");
        // Compute the expected BigDecimal using the real InstantConversions implementation.
        expectedBigDecimal = InstantConversions.toBigDecimal(sampleInstant, null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mock interactions to keep tests independent.
        reset(mockConverter);
    }

    /**
     * Helper that computes the expected {@link BigDecimal} for a given {@link Instant}
     * using the real {@link InstantConversions} implementation.
     */
    private static BigDecimal expectedFromInstant(Instant instant) {
        // The real implementation does not need a Converter, so we pass {@code null}.
        return InstantConversions.toBigDecimal(instant, null);
    }

    private static Stream<Arguments> successfulConversionProvider() {
        // Instant (already an Instant)
        Arguments instantCase = Arguments.of(FIXED_INSTANT, expectedFromInstant(FIXED_INSTANT), "Instant should be returned unchanged and converted to BigDecimal");
        // String ISO‑8601 instant
        Instant parsedFromString = Instant.parse("2023-08-15T12:34:56Z");
        Arguments stringCase = Arguments.of("2023-08-15T12:34:56Z", expectedFromInstant(parsedFromString), "ISO‑8601 string should be parsed to Instant and then to BigDecimal");
        // java.util.Date at epoch
        Date epochDate = new Date(0L);
        Arguments dateCase = Arguments.of(epochDate, expectedFromInstant(Instant.ofEpochMilli(0L)), "Date at epoch should map to Instant epoch → BigDecimal ZERO");
        // LocalDate at epoch (midnight UTC)
        LocalDate epochLocalDate = LocalDate.of(1970, 1, 1);
        Instant instantFromLocalDate = epochLocalDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Arguments localDateCase = Arguments.of(epochLocalDate, expectedFromInstant(instantFromLocalDate), "LocalDate at epoch should map to Instant epoch → BigDecimal ZERO");
        // LocalDateTime at epoch (interpreted as UTC)
        LocalDateTime epochLDT = LocalDateTime.of(1970, 1, 1, 0, 0, 0, 0);
        Instant instantFromLDT = epochLDT.toInstant(ZoneOffset.UTC);
        Arguments localDateTimeCase = Arguments.of(epochLDT, expectedFromInstant(instantFromLDT), "LocalDateTime at epoch interpreted as UTC → BigDecimal ZERO");
        // java.sql.Timestamp
        Timestamp ts = new Timestamp(123_456_789L);
        Arguments timestampCase = Arguments.of(ts, expectedFromInstant(Instant.ofEpochMilli(123_456_789L)), "Timestamp should be converted via its epoch milliseconds");
        // AtomicLong representing epoch milliseconds
        AtomicLong atomicLong = new AtomicLong(987_654_321L);
        Arguments atomicLongCase = Arguments.of(atomicLong, expectedFromInstant(Instant.ofEpochMilli(987_654_321L)), "AtomicLong (epoch ms) → Instant → BigDecimal");
        // BigInteger interpreted as epoch seconds
        BigInteger bigInt = new BigInteger("1000000000");
        Arguments bigIntegerCase = Arguments.of(bigInt, expectedFromInstant(Instant.ofEpochSecond(1_000_000_000L)), "BigInteger (epoch seconds) → Instant → BigDecimal");
        // Double representing seconds with fractional part
        Double doubleVal = 123.456d;
        // 123
        long secs = doubleVal.longValue();
        // 456_000_000
        long nanos = (long) ((doubleVal - secs) * 1_000_000_000L);
        Instant instantFromDouble = Instant.ofEpochSecond(secs, nanos);
        Arguments doubleCase = Arguments.of(doubleVal, expectedFromInstant(instantFromDouble), "Double seconds with fraction → Instant → BigDecimal");
        return Stream.of(instantCase, stringCase, dateCase, localDateCase, localDateTimeCase, timestampCase, atomicLongCase, bigIntegerCase, doubleCase);
    }

    private static Stream<Arguments> exceptionProvider() {
        // Null source – the contract of toInstant is to throw NPE.
        Arguments nullCase = Arguments.of(null, NullPointerException.class, "Null source should propagate NullPointerException from toInstant");
        // Unparsable string
        Arguments unparsableString = Arguments.of("not-a-date", DateTimeParseException.class, "Invalid ISO‑8601 string should cause DateTimeParseException");
        // Unsupported object type
        Arguments unsupportedObject = Arguments.of(new Object(), IllegalArgumentException.class, "Unsupported source type should result in IllegalArgumentException");
        // LocalTime without a date component
        Arguments localTimeCase = Arguments.of(LocalTime.of(12, 34, 56), DateTimeException.class, "LocalTime cannot be converted to an Instant – expect DateTimeException");
        return Stream.of(nullCase, unparsableString, unsupportedObject, localTimeCase);
    }

    @Test
    void toBigDecimal_givenInstant_returnsBigDecimalFromInstantConversions() {
        // GIVEN
        Object source = sampleInstant;
        // WHEN
        BigDecimal actual = LocalDateConversions.toBigDecimal(source, mockConverter);
        // THEN
        assertEquals(expectedBigDecimal, actual, "Conversion from Instant should delegate to InstantConversions and produce the same BigDecimal");
        // No interaction with the mock converter is expected because the source is already an Instant.
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toBigDecimal_givenDate_usesConverterToObtainInstant() {
        // GIVEN
        Date date = Date.from(sampleInstant);
        // Stub the converter so that a Date is converted to the predetermined Instant.
        when(mockConverter.convert(eq(date), eq(Instant.class))).thenReturn(sampleInstant);
        // WHEN
        BigDecimal actual = LocalDateConversions.toBigDecimal(date, mockConverter);
        // THEN
        assertEquals(expectedBigDecimal, actual, "Conversion from Date should use the Converter to obtain an Instant and then produce the correct BigDecimal");
        // Verify that the converter was called exactly once with the expected arguments.
        verify(mockConverter, times(1)).convert(eq(date), eq(Instant.class));
    }

    @Test
    void toBigDecimal_givenNull_returnsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        BigDecimal actual = LocalDateConversions.toBigDecimal(source, mockConverter);
        // THEN
        assertNull(actual, "Conversion of a null source should result in a null BigDecimal");
        // No interaction with the converter is required for a null source.
        verifyNoInteractions(mockConverter);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("successfulConversionProvider")
    void toBigDecimal_successfulConversions(Object source, BigDecimal expected, String description) {
        // WHEN
        BigDecimal actual = LocalDateConversions.toBigDecimal(source, mockConverter);
        // THEN
        assertEquals(expected, actual, "Unexpected BigDecimal for source: " + description);
        // No specific interactions with the mock are required for these cases.
        verifyNoInteractions(mockConverter);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("exceptionProvider")
    void toBigDecimal_exceptions(Object source, Class<? extends Throwable> expectedException, String description) {
        // WHEN / THEN
        assertThrows(expectedException, () -> LocalDateConversions.toBigDecimal(source, mockConverter), "Expected exception for: " + description);
        // No interaction with the mock converter is expected for these failure paths.
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toBigDecimal_givenInstant_doesNotUseConverter() {
        // GIVEN
        Instant source = FIXED_INSTANT;
        // WHEN
        BigDecimal result = LocalDateConversions.toBigDecimal(source, mockConverter);
        // THEN
        assertEquals(expectedFromInstant(FIXED_INSTANT), result, "Instant should be delegated directly to InstantConversions");
        verifyNoInteractions(mockConverter);
    }
}
