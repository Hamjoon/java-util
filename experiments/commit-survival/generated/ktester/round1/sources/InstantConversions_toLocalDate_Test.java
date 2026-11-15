package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.time.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Calendar;
import java.util.Date;

@ExtendWith(MockitoExtension.class)
class InstantConversions_toLocalDate_Test {

    /**
     * The mock Converter that will be used for all test cases.
     */
    private Converter mockConverter;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh mock before each test to avoid cross‑test interference.
        mockConverter = mock(Converter.class);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the mock to ensure no leftover stubbings affect other tests.
        reset(mockConverter);
    }

    /**
     * Supplies a stream of arguments: the source object, the ZonedDateTime the mock
     * converter should return, and the expected LocalDate extracted from that
     * ZonedDateTime.
     */
    private static Stream<Arguments> provideSourceObjects() {
        ZonedDateTime zdt1 = ZonedDateTime.of(2023, 5, 17, 10, 30, 0, 0, ZoneOffset.UTC);
        ZonedDateTime zdt2 = ZonedDateTime.of(1999, 12, 31, 23, 59, 59, 0, ZoneId.of("America/New_York"));
        ZonedDateTime zdt3 = ZonedDateTime.of(2000, 2, 29, 0, 0, 0, 0, ZoneOffset.ofHours(5));
        return Stream.of(// source as java.util.Date
        Arguments.of(Date.from(zdt1.toInstant()), zdt1, zdt1.toLocalDate()), // source as java.time.Instant
        Arguments.of(zdt2.toInstant(), zdt2, zdt2.toLocalDate()), // source as java.sql.Timestamp
        Arguments.of(Timestamp.from(zdt3.toInstant()), zdt3, zdt3.toLocalDate()), // source as java.time.LocalDateTime (treated as system default zone)
        Arguments.of(zdt1.toLocalDateTime(), zdt1, zdt1.toLocalDate()), // source as java.time.OffsetDateTime
        Arguments.of(zdt2.toOffsetDateTime(), zdt2, zdt2.toLocalDate()), // source as java.util.Calendar
        Arguments.of(Calendar.from(zdt3), zdt3, zdt3.toLocalDate()), // source already a LocalDate (conversion should be a no‑op)
        Arguments.of(zdt1.toLocalDate(), zdt1, zdt1.toLocalDate()));
    }

    /**
     * Verifies that {@code InstantConversions.toLocalDate(Object, Converter)} returns the
     * {@link LocalDate} component of the {@link ZonedDateTime} produced by the supplied
     * {@link Converter}.
     *
     * The test is parameterised over a variety of source objects. For each source we
     * stub the {@code Converter.convert(..., ZonedDateTime.class)} call to return a
     * predetermined {@code ZonedDateTime}. The expected {@code LocalDate} is then
     * compared with the actual result.
     */
    @ParameterizedTest
    @MethodSource("provideSourceObjects")
    void toLocalDate_returnsExpectedLocalDate(Object source, ZonedDateTime zonedDateTime, LocalDate expected) {
        // Arrange – stub the converter to return the supplied ZonedDateTime
        when(mockConverter.convert(eq(source), eq(ZonedDateTime.class))).thenReturn(zonedDateTime);
        // Act
        LocalDate actual = InstantConversions.toLocalDate(source, mockConverter);
        // Assert
        assertEquals(expected, actual, () -> "Unexpected LocalDate for source [" + source + "]");
        // Verify that the converter was asked exactly once for this conversion.
        verify(mockConverter, times(1)).convert(eq(source), eq(ZonedDateTime.class));
    }

    /**
     * When the source object is {@code null}, the contract of {@code InstantConversions}
     * (mirroring the behaviour of {@link Converter}) is to return {@code null} rather than
     * throwing an exception. This test confirms that expectation.
     */
    @Test
    void toLocalDate_nullSource_returnsNull() {
        // Arrange – the converter should not be called at all for a null source.
        // (If it were, the mock would return null, which would cause a NPE in the
        //  implementation; the test ensures the method guards against that.)
        // No stubbing required.
        // Act
        LocalDate result = InstantConversions.toLocalDate(null, mockConverter);
        // Assert
        assertNull(result, "Expected null result when source is null");
        verifyNoInteractions(mockConverter);
    }

    /**
     * Both arguments are {@code null}. The implementation delegates to
     * {@code toZonedDateTime}, which should throw a {@link NullPointerException}
     * when it attempts to dereference either argument.
     */
    @Test
    void toLocalDate_bothArgumentsNull_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> InstantConversions.toLocalDate(null, null), "Expected NullPointerException when both arguments are null");
    }

    /**
     * The source is non‑null but the {@link Converter} is {@code null}.
     * A {@link NullPointerException} is expected because the method attempts to
     * call {@code converter.convert(...)}.
     */
    @Test
    void toLocalDate_converterNull_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> InstantConversions.toLocalDate("invalid", null), "Expected NullPointerException when converter is null");
    }

    /**
     * An unsupported source type should cause {@code toZonedDateTime} to throw
     * {@link IllegalArgumentException}. We simulate this by configuring the mock
     * converter to throw the exception for the given input.
     */
    @Test
    void toLocalDate_unsupportedSource_throwsIllegalArgumentException() {
        Object unsupported = new Object();
        // Stub the converter to throw IllegalArgumentException for this input.
        when(mockConverter.convert(eq(unsupported), eq(ZonedDateTime.class))).thenThrow(new IllegalArgumentException("Unsupported type"));
        assertThrows(IllegalArgumentException.class, () -> InstantConversions.toLocalDate(unsupported, mockConverter), "Expected IllegalArgumentException for unsupported source type");
        verify(mockConverter, times(1)).convert(eq(unsupported), eq(ZonedDateTime.class));
    }

    /**
     * An {@link Instant} that is out of the range representable by {@link LocalDate}
     * should cause a {@link DateTimeException} when the {@link ZonedDateTime}
     * is converted to a {@link LocalDate}.
     *
     * <p>We stub the converter to return a {@code ZonedDateTime} based on the
     * extreme {@code Instant}. The subsequent call to {@code toLocalDate()} then
     * triggers the exception.</p>
     */
    @Test
    void toLocalDate_instantOutOfRange_throwsDateTimeException() {
        Instant outOfRange = Instant.ofEpochMilli(Long.MAX_VALUE);
        ZonedDateTime zdt = ZonedDateTime.ofInstant(outOfRange, ZoneOffset.UTC);
        // Stub the converter to return the out‑of‑range ZonedDateTime.
        when(mockConverter.convert(eq(outOfRange), eq(ZonedDateTime.class))).thenReturn(zdt);
        assertThrows(DateTimeException.class, () -> InstantConversions.toLocalDate(outOfRange, mockConverter), "Expected DateTimeException for Instant out of LocalDate range");
        verify(mockConverter, times(1)).convert(eq(outOfRange), eq(ZonedDateTime.class));
    }
}
