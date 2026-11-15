package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateConversions_toOffsetDateTime_Test {

    /**
     * Mocked {@link Converter} that will be passed to the method under test.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Mocked {@link ConverterOptions} returned by {@code mockConverter.getOptions()}.
     */
    @Mock
    private ConverterOptions mockOptions;

    /**
     * Auto‑closeable for Mockito annotations.
     */
    private AutoCloseable mocks;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise @Mock fields.
        mocks = MockitoAnnotations.openMocks(this);
        // Default stubbing: the converter returns the mocked options.
        when(mockConverter.getOptions()).thenReturn(mockOptions);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Release Mockito resources.
        mocks.close();
    }

    /**
     * Supplies test data for {@link #testToOffsetDateTime_ValidConversions}.
     */
    private static Stream<Arguments> provideValidConversions() {
        return Stream.of(// Case: GMT+02:00 offset
        Arguments.of(LocalDate.of(2023, 5, 15), TimeZone.getTimeZone("GMT+02:00"), OffsetDateTime.of(LocalDate.of(2023, 5, 15), LocalTime.MIDNIGHT, ZoneOffset.ofHours(2))), // Case: Leap day with UTC
        Arguments.of(LocalDate.of(2020, 2, 29), TimeZone.getTimeZone("UTC"), OffsetDateTime.of(LocalDate.of(2020, 2, 29), LocalTime.MIDNIGHT, ZoneOffset.UTC)));
    }

    /**
     * Verifies that {@code toOffsetDateTime} creates an {@link OffsetDateTime}
     * at midnight of the supplied {@link LocalDate} using the {@link TimeZone}
     * configured in the {@link ConverterOptions}.
     *
     * The test uses the UTC time‑zone (offset 0) to make the expected result
     * deterministic regardless of the machine's default zone or DST rules.
     */
    @Test
    void testToOffsetDateTime_UtcTimeZone() {
        // GIVEN
        LocalDate inputDate = LocalDate.of(2023, Month.MARCH, 15);
        TimeZone utc = TimeZone.getTimeZone("UTC");
        when(mockOptions.getTimeZone()).thenReturn(utc);
        // WHEN
        OffsetDateTime result = LocalDateConversions.toOffsetDateTime(inputDate, mockConverter);
        // THEN
        OffsetDateTime expected = OffsetDateTime.of(inputDate, LocalTime.MIDNIGHT, ZoneOffset.UTC);
        assertEquals(expected, result, "The OffsetDateTime should represent midnight UTC of the given LocalDate");
    }

    /**
     * Verifies that a non‑UTC time‑zone is honoured.  The test uses a fixed
     * point in time (January 1, 2022 12:00 UTC) so that the offset returned
     * by the {@link TimeZone} is predictable.
     */
    @Test
    void testToOffsetDateTime_NonUtcTimeZone() {
        // GIVEN
        LocalDate inputDate = LocalDate.of(2022, Month.JANUARY, 1);
        // Choose a zone with a constant offset (e.g., GMT+02:00)
        TimeZone plusTwo = TimeZone.getTimeZone("GMT+02:00");
        when(mockOptions.getTimeZone()).thenReturn(plusTwo);
        // WHEN
        OffsetDateTime result = LocalDateConversions.toOffsetDateTime(inputDate, mockConverter);
        // THEN
        ZoneOffset expectedOffset = ZoneOffset.ofHours(2);
        OffsetDateTime expected = OffsetDateTime.of(inputDate, LocalTime.MIDNIGHT, expectedOffset);
        assertEquals(expected, result, "The OffsetDateTime should use the offset supplied by the Converter's TimeZone");
    }

    /**
     * Parameterized test that validates conversion for a set of explicit
     * {@link LocalDate} / {@link TimeZone} pairs taken from the JSON test
     * description. It checks that the resulting {@link OffsetDateTime}
     * has midnight as the time component and the correct zone offset.
     */
    @ParameterizedTest(name = "[{index}] date={0}, zone={1}")
    @MethodSource("provideValidConversions")
    void testToOffsetDateTime_ValidConversions(LocalDate date, TimeZone zone, OffsetDateTime expected) {
        // GIVEN
        when(mockOptions.getTimeZone()).thenReturn(zone);
        // WHEN
        OffsetDateTime result = LocalDateConversions.toOffsetDateTime(date, mockConverter);
        // THEN
        assertEquals(expected, result, () -> "Expected OffsetDateTime " + expected + " but got " + result);
    }

    /**
     * Ensures that passing {@code null} as the source {@link LocalDate}
     * results in a {@link NullPointerException}. The method under test
     * casts the argument to {@code LocalDate} without a null‑check.
     */
    @Test
    void testToOffsetDateTime_NullSourceObject() {
        // GIVEN
        when(mockOptions.getTimeZone()).thenReturn(TimeZone.getTimeZone("UTC"));
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toOffsetDateTime(null, mockConverter), "A null source object should trigger NullPointerException");
    }
}
