package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateConversions_toZonedDateTime_Test {

    @Mock
    private Converter // the Converter passed to the method
    mockConverter;

    @Mock
    private ConverterOptions // the options held inside the Converter
    mockOptions;

    // for closing the mock context
    private AutoCloseable mocks;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations and define default behaviour
        mocks = MockitoAnnotations.openMocks(this);
        // When the converter asks for its options, return the mocked options
        when(mockConverter.getOptions()).thenReturn(mockOptions);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Release the Mockito resources
        mocks.close();
    }

    /* --------------------------------------------------------------
     *  Normal & edge‑case conversions – parameterised
     * -------------------------------------------------------------- */
    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// epoch start
        Arguments.of(LocalDate.of(1970, 1, 1), "UTC"), // far future
        Arguments.of(LocalDate.of(9999, 12, 31), "America/New_York"), // typical European zone
        Arguments.of(LocalDate.of(2023, 2, 28), "Europe/Paris"), // leap‑year date
        Arguments.of(LocalDate.of(2024, 2, 29), "Asia/Tokyo"), // custom offset zone
        Arguments.of(LocalDate.of(2000, 1, 1), "UTC+05:30"));
    }

    /**
     * Verify that a valid {@link LocalDate} is converted to a {@link ZonedDateTime}
     * at midnight using the ZoneId supplied by the {@link ConverterOptions}.
     */
    @Test
    void toZonedDateTime_WithValidLocalDate_ReturnsMidnightZonedDateTime() {
        // given
        LocalDate inputDate = LocalDate.of(2023, Month.MAY, 17);
        ZoneId expectedZone = ZoneId.of("UTC");
        when(mockOptions.getZoneId()).thenReturn(expectedZone);
        // when
        ZonedDateTime result = LocalDateConversions.toZonedDateTime(inputDate, mockConverter);
        // then
        ZonedDateTime expected = ZonedDateTime.of(inputDate, LocalTime.MIDNIGHT, expectedZone);
        assertEquals(expected, result, "The ZonedDateTime should represent midnight of the supplied LocalDate in the converter's zone");
    }

    /**
     * Verify that a {@code null} source value results in a {@link NullPointerException}
     * because the method casts the argument to {@link LocalDate} and then dereferences it.
     */
    @Test
    void toZonedDateTime_WithNullSource_ThrowsNullPointerException() {
        // given
        when(mockOptions.getZoneId()).thenReturn(ZoneId.systemDefault());
        // when / then
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toZonedDateTime(null, mockConverter), "Passing null as the source should cause a NullPointerException");
    }

    /**
     * Verify that a source value of an unexpected type causes a {@link ClassCastException}.
     */
    @Test
    void toZonedDateTime_WithInvalidSourceType_ThrowsClassCastException() {
        // given
        // a String, not a LocalDate
        Object notALocalDate = "2023-05-17";
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("America/New_York"));
        // when / then
        assertThrows(ClassCastException.class, () -> LocalDateConversions.toZonedDateTime(notALocalDate, mockConverter), "Passing a non‑LocalDate source should cause a ClassCastException");
    }

    @Test
    void toZonedDateTime_WithNullConverter_ThrowsNullPointerException() {
        // given
        LocalDate date = LocalDate.of(2023, 5, 17);
        // when / then
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toZonedDateTime(date, null), "A null Converter should cause a NullPointerException when getOptions() is invoked");
    }

    @Test
    void toZonedDateTime_WithConverterReturningNullOptions_ThrowsNullPointerException() {
        // given
        LocalDate date = LocalDate.of(2023, 5, 17);
        // options are null
        when(mockConverter.getOptions()).thenReturn(null);
        // when / then
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toZonedDateTime(date, mockConverter), "Null options from the converter should cause a NullPointerException");
    }

    @Test
    void toZonedDateTime_WithOptionsReturningNullZoneId_ThrowsNullPointerException() {
        // given
        LocalDate date = LocalDate.of(2023, 5, 17);
        // zoneId is null
        when(mockOptions.getZoneId()).thenReturn(null);
        // when / then
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toZonedDateTime(date, mockConverter), "A null ZoneId should cause a NullPointerException inside ZonedDateTime.of");
    }

    /* --------------------------------------------------------------
     *  Converter internal failures – exception propagation
     * -------------------------------------------------------------- */
    @Test
    void toZonedDateTime_WhenConverterGetOptionsThrows_PropagatesException() {
        // given
        LocalDate date = LocalDate.of(2023, 5, 17);
        when(mockConverter.getOptions()).thenThrow(new IllegalStateException("Options failure"));
        // when / then
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> LocalDateConversions.toZonedDateTime(date, mockConverter), "Exception from getOptions() should propagate");
        assertEquals("Options failure", ex.getMessage());
    }

    @Test
    void toZonedDateTime_WhenOptionsGetZoneIdThrows_PropagatesException() {
        // given
        LocalDate date = LocalDate.of(2023, 5, 17);
        when(mockOptions.getZoneId()).thenThrow(new IllegalStateException("ZoneId failure"));
        // when / then
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> LocalDateConversions.toZonedDateTime(date, mockConverter), "Exception from getZoneId() should propagate");
        assertEquals("ZoneId failure", ex.getMessage());
    }

    /* --------------------------------------------------------------
     *  DST gap handling – DateTimeException
     * -------------------------------------------------------------- */
    @Test
    void toZonedDateTime_WithDstGap_ThrowsDateTimeException() {
        // given
        // 2011‑12‑30 does not exist at midnight in Pacific/Apia because of the DST jump
        LocalDate date = LocalDate.of(2011, 12, 30);
        ZoneId zone = ZoneId.of("Pacific/Apia");
        when(mockOptions.getZoneId()).thenReturn(zone);
        // when / then
        assertThrows(DateTimeException.class, () -> LocalDateConversions.toZonedDateTime(date, mockConverter), "Midnight on a DST‑gap day should result in DateTimeException");
    }
}
