package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ZonedDateTimeConversions_toLocalDate_Test {

    /**
     * Mocked Converter that will be used for all test cases.
     */
    @Mock
    Converter mockConverter;

    /**
     * The ZonedDateTime that the mock will return when a conversion to ZonedDateTime is requested.
     */
    private ZonedDateTime stubZonedDateTime;

    @BeforeEach
    void setupBeforeEach() {
        // Prepare a deterministic ZonedDateTime that will be returned by the mock.
        stubZonedDateTime = ZonedDateTime.of(// year, month, day
        2023, // year, month, day
        5, // year, month, day
        10, // hour, minute, second, nano
        15, // hour, minute, second, nano
        30, // hour, minute, second, nano
        45, // hour, minute, second, nano
        0, // zone
        ZoneId.of("UTC"));
        // Whenever the Converter is asked to convert *any* object to ZonedDateTime,
        // return the stubbed instance.
        when(mockConverter.convert(any(), eq(ZonedDateTime.class))).thenReturn(stubZonedDateTime);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mock interactions to keep each test isolated.
        reset(mockConverter);
    }

    /**
     * Supplies the combinations of arguments that should trigger a {@link NullPointerException}.
     */
    private static Stream<Arguments> nullHandlingProvider() {
        return Stream.of(// case 1: from == null, converter is a mocked instance (non‑null)
        Arguments.of(null, mock(Converter.class)), // case 2: both from and converter are null
        Arguments.of(null, null));
    }

    /**
     * Verifies that {@code ZonedDateTimeConversions.toLocalDate(Object, Converter)}
     * correctly extracts the {@link java.time.LocalDate} part from the ZonedDateTime
     * produced by the supplied {@link Converter}.
     */
    @Test
    void toLocalDate_returnsLocalDateFromConvertedZonedDateTime() {
        // GIVEN – an arbitrary input object (its actual value is irrelevant because the mock
        // Converter will ignore it and always return {@code stubZonedDateTime}).
        Object arbitraryInput = "2023-05-10T15:30:45Z";
        // WHEN – the method under test is invoked.
        LocalDate result = ZonedDateTimeConversions.toLocalDate(arbitraryInput, mockConverter);
        // THEN – the result must be the LocalDate component of the stubbed ZonedDateTime.
        assertEquals(stubZonedDateTime.toLocalDate(), result, "The LocalDate extracted from the converted ZonedDateTime is not as expected.");
    }

    /**
     * Additional sanity check: when the Converter returns a ZonedDateTime in a different zone,
     * the LocalDate should still match the date component of that ZonedDateTime.
     */
    @Test
    void toLocalDate_handlesDifferentTimeZone() {
        // GIVEN – a ZonedDateTime in a non‑UTC zone.
        ZonedDateTime differentZone = ZonedDateTime.of(2022, 12, 31, 23, 0, 0, 0, ZoneId.of("America/New_York"));
        when(mockConverter.convert(any(), eq(ZonedDateTime.class))).thenReturn(differentZone);
        // WHEN
        LocalDate result = ZonedDateTimeConversions.toLocalDate(new Object(), mockConverter);
        // THEN
        assertEquals(differentZone.toLocalDate(), result, "LocalDate should reflect the date part of the ZonedDateTime regardless of its zone.");
    }

    /**
     * Parameterized test that validates the method throws {@link NullPointerException}
     * when either the source object or the {@link Converter} argument is {@code null}.
     *
     * <p>The test data mirrors the JSON test cases supplied in the task description.
     */
    @ParameterizedTest(name = "{index}: from={0}, converter={1}")
    @MethodSource("nullHandlingProvider")
    void toLocalDate_nullHandling(Object from, Converter converter) {
        // WHEN / THEN – a NullPointerException must be thrown.
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toLocalDate(from, converter), "Expected NullPointerException when 'from' or 'converter' is null");
    }
}
