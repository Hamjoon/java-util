package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DoubleConversions_toOffsetDateTime_Test {

    private Converter mockConverter;

    private ConverterOptions mockOptions;

    @BeforeEach
    void setupBeforeEach() {
        mockConverter = mock(Converter.class);
        mockOptions = mock(ConverterOptions.class);
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        mockConverter = mock(Converter.class);
        mockOptions = mock(ConverterOptions.class);
        when(mockConverter.getOptions()).thenReturn(mockOptions);
    }

    private static Stream<Arguments> provideSuccessfulConversions() {
        return Stream.of(// Instant input – UTC
        Arguments.of("Instant input (UTC)", Instant.parse("2023-03-25T12:34:56Z"), ZoneId.of("UTC"), OffsetDateTime.parse("2023-03-25T12:34:56Z")), // java.util.Date input – UTC
        Arguments.of("Date input (UTC)", Date.from(Instant.parse("2023-03-25T12:34:56Z")), ZoneId.of("UTC"), OffsetDateTime.parse("2023-03-25T12:34:56Z")), // Long epoch millis – UTC
        Arguments.of("Long epoch millis input (UTC)", 1679744096000L, ZoneId.of("UTC"), OffsetDateTime.parse("2023-03-25T12:34:56Z")), // ISO‑8601 String – UTC
        Arguments.of("String ISO‑8601 input (UTC)", "2023-03-25T12:34:56Z", ZoneId.of("UTC"), OffsetDateTime.parse("2023-03-25T12:34:56Z")), // LocalDateTime with America/New_York zone
        Arguments.of("LocalDateTime input with America/New_York zone", LocalDateTime.of(2023, 3, 25, 12, 34, 56), ZoneId.of("America/New_York"), OffsetDateTime.parse("2023-03-25T12:34:56-04:00")), // ZonedDateTime source zone (Europe/Paris) → target UTC
        Arguments.of("ZonedDateTime input with different source zone", ZonedDateTime.of(2023, 3, 25, 12, 34, 56, 0, ZoneId.of("Europe/Paris")), ZoneId.of("UTC"), OffsetDateTime.parse("2023-03-25T11:34:56Z")), // OffsetDateTime source (offset +02:00) → target UTC
        Arguments.of("OffsetDateTime input (convert to UTC)", OffsetDateTime.parse("2023-03-25T12:34:56+02:00"), ZoneId.of("UTC"), OffsetDateTime.parse("2023-03-25T10:34:56Z")), // Extreme epoch – Long.MIN_VALUE, UTC
        Arguments.of("Extreme epoch value – Long.MIN_VALUE (UTC)", Long.MIN_VALUE, ZoneId.of("UTC"), OffsetDateTime.ofInstant(Instant.ofEpochMilli(Long.MIN_VALUE), ZoneOffset.UTC)), // Extreme epoch – Long.MAX_VALUE, UTC
        Arguments.of("Extreme epoch value – Long.MAX_VALUE (UTC)", Long.MAX_VALUE, ZoneId.of("UTC"), OffsetDateTime.ofInstant(Instant.ofEpochMilli(Long.MAX_VALUE), ZoneOffset.UTC)));
    }

    private static Stream<Arguments> provideInvalidStringInputs() {
        return Stream.of(Arguments.of("Non‑ISO‑8601 string", "not-a-date-time"), Arguments.of("Impossible date (Feb 30)", "2023-02-30T12:34:56Z"));
    }

    /**
     * Verifies that {@code toOffsetDateTime} returns an {@link OffsetDateTime}
     * that represents the same instant as the stubbed {@link Instant}
     * and uses the {@link ZoneId} supplied by the converter options.
     */
    @Test
    void toOffsetDateTime_usesInstantAndZoneId() {
        // GIVEN
        // 2021‑05‑05T23:20:00Z
        Instant stubInstant = Instant.ofEpochSecond(1_620_000_000L);
        // explicit zone for deterministic offset
        ZoneId zoneId = ZoneId.of("UTC");
        when(mockOptions.getZoneId()).thenReturn(zoneId);
        // Stub the static method DoubleConversions.toInstant(...)
        try (MockedStatic<DoubleConversions> mockedStatic = Mockito.mockStatic(DoubleConversions.class)) {
            mockedStatic.when(() -> DoubleConversions.toInstant(any(), eq(mockConverter))).thenReturn(stubInstant);
            // WHEN
            OffsetDateTime result = DoubleConversions.toOffsetDateTime("any input", mockConverter);
            // THEN
            OffsetDateTime expected = stubInstant.atZone(zoneId).toOffsetDateTime();
            assertEquals(expected, result, "The OffsetDateTime should be derived from the stubbed Instant and the ZoneId from options");
        }
    }

    /**
     * Same as the previous test but with a non‑UTC zone to ensure the offset is applied correctly.
     */
    @Test
    void toOffsetDateTime_withNonUtcZone() {
        // GIVEN
        Instant stubInstant = Instant.parse("2022-01-01T12:00:00Z");
        // Eastern Time (may be -05:00 or -04:00 depending on DST)
        ZoneId zoneId = ZoneId.of("America/New_York");
        when(mockOptions.getZoneId()).thenReturn(zoneId);
        try (MockedStatic<DoubleConversions> mockedStatic = Mockito.mockStatic(DoubleConversions.class)) {
            mockedStatic.when(() -> DoubleConversions.toInstant(any(), eq(mockConverter))).thenReturn(stubInstant);
            // WHEN
            OffsetDateTime result = DoubleConversions.toOffsetDateTime(new Object(), mockConverter);
            // THEN
            OffsetDateTime expected = stubInstant.atZone(zoneId).toOffsetDateTime();
            assertEquals(expected, result, "OffsetDateTime should reflect the supplied non‑UTC ZoneId");
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideSuccessfulConversions")
    void toOffsetDateTime_successful(String description, Object from, ZoneId zoneId, OffsetDateTime expected) {
        // Arrange
        when(mockOptions.getZoneId()).thenReturn(zoneId);
        // Act
        OffsetDateTime result = DoubleConversions.toOffsetDateTime(from, mockConverter);
        // Assert
        assertEquals(expected, result, description);
    }

    @Test
    void toOffsetDateTime_nullFrom_throwsNPE() {
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
        assertThrows(NullPointerException.class, () -> DoubleConversions.toOffsetDateTime(null, mockConverter), "Passing a null source object should result in NullPointerException");
    }

    @Test
    void toOffsetDateTime_nullConverter_throwsNPE() {
        assertThrows(NullPointerException.class, () -> DoubleConversions.toOffsetDateTime(Instant.now(), null), "A null Converter argument should cause NullPointerException");
    }

    @Test
    void toOffsetDateTime_nullOptions_throwsNPE() {
        // Converter returns null options
        when(mockConverter.getOptions()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> DoubleConversions.toOffsetDateTime(Instant.parse("2023-03-25T12:34:56Z"), mockConverter), "Converter returning null Options should cause NullPointerException");
    }

    @Test
    void toOffsetDateTime_nullZoneId_throwsNPE() {
        // Options return null ZoneId
        when(mockOptions.getZoneId()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> DoubleConversions.toOffsetDateTime(Instant.parse("2023-03-25T12:34:56Z"), mockConverter), "Null ZoneId in options should cause NullPointerException");
    }

    @Test
    void toOffsetDateTime_unsupportedObject_throwsIllegalArgumentException() {
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
        assertThrows(IllegalArgumentException.class, () -> DoubleConversions.toOffsetDateTime(new Object(), mockConverter), "Unsupported source type should throw IllegalArgumentException");
    }

    @ParameterizedTest(name = "[{index}] Invalid string: {0}")
    @MethodSource("provideInvalidStringInputs")
    void toOffsetDateTime_invalidString_throwsDateTimeParseException(String description, String input) {
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
        assertThrows(DateTimeParseException.class, () -> DoubleConversions.toOffsetDateTime(input, mockConverter), description);
    }

    @Test
    void toOffsetDateTime_unsupportedNumericType_throwsIllegalArgumentException() {
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
        assertThrows(IllegalArgumentException.class, () -> DoubleConversions.toOffsetDateTime(Float.valueOf(12345.67f), mockConverter), "Unsupported numeric type (Float) should throw IllegalArgumentException");
    }
}
