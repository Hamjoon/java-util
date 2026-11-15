package com.cedarsoftware.util.convert;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;
import java.time.DateTimeException;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeConversions_toZonedDateTime_Test {

    @Mock
    private Converter mockConverter;

    @Mock
    private ConverterOptions mockOptions;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations (handled by @ExtendWith) and stub common behaviour
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("America/New_York"));
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mock interactions to avoid cross‑test contamination
        reset(mockConverter, mockOptions);
    }

    private static Stream<Arguments> normalConversionProvider() {
        LocalDateTime ldt = LocalDateTime.of(2023, 5, 10, 12, 30, 45);
        ZoneId utc = ZoneId.of("UTC");
        ZonedDateTime expected = ZonedDateTime.of(ldt, utc);
        return Stream.of(Arguments.of("Standard case – UTC zone", ldt, utc, expected));
    }

    @Test
    void testToZonedDateTimeSuccess() {
        // given
        LocalDateTime ldt = LocalDateTime.of(2023, 5, 10, 15, 30, 45);
        // when
        ZonedDateTime result = LocalDateTimeConversions.toZonedDateTime(ldt, mockConverter);
        // then
        assertNotNull(result, "Result should not be null");
        assertEquals(ldt, result.toLocalDateTime(), "LocalDateTime component must be unchanged");
        assertEquals(ZoneId.of("America/New_York"), result.getZone(), "ZoneId must be the one supplied by ConverterOptions");
    }

    @Test
    void testToZonedDateTimeNullInput() {
        // given: a null source object
        // when / then
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toZonedDateTime(null, mockConverter), "Passing null should raise NullPointerException due to the cast");
    }

    @Test
    void testToZonedDateTimeWrongType() {
        // given: an object that is not a LocalDateTime
        Object notALocalDateTime = "2023-05-10T15:30:45";
        // when / then
        assertThrows(ClassCastException.class, () -> LocalDateTimeConversions.toZonedDateTime(notALocalDateTime, mockConverter), "Passing an object of the wrong type should raise ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("normalConversionProvider")
    @DisplayName("Normal conversion with valid LocalDateTime and ZoneId")
    void testNormalConversion(String description, LocalDateTime input, ZoneId zoneId, ZonedDateTime expected) {
        // Arrange – customise the zone returned by the mocked options
        when(mockOptions.getZoneId()).thenReturn(zoneId);
        // Act
        ZonedDateTime result = LocalDateTimeConversions.toZonedDateTime(input, mockConverter);
        // Assert
        assertNotNull(result, "Result must not be null");
        assertEquals(expected.toLocalDateTime(), result.toLocalDateTime(), "LocalDateTime component must be unchanged");
        assertEquals(expected.getZone(), result.getZone(), "ZoneId must match the one supplied by ConverterOptions");
    }

    @Test
    @DisplayName("Null `from` argument should raise NullPointerException")
    void testNullFromParameter() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toZonedDateTime(null, mockConverter), "Passing null should raise NullPointerException due to the cast");
    }

    @Test
    @DisplayName("Wrong type for `from` argument should raise ClassCastException")
    void testInvalidFromType() {
        // given
        Object notALocalDateTime = "2023-05-10T12:30:45";
        // Act & Assert
        assertThrows(ClassCastException.class, () -> LocalDateTimeConversions.toZonedDateTime(notALocalDateTime, mockConverter), "Passing an object of the wrong type should raise ClassCastException");
    }

    @Test
    @DisplayName("Null `converter` argument should raise NullPointerException")
    void testNullConverterArgument() {
        LocalDateTime ldt = LocalDateTime.of(2023, 5, 10, 12, 30, 45);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toZonedDateTime(ldt, null), "Calling with a null converter must throw NullPointerException");
    }

    @Test
    @DisplayName("Converter returning null options should raise NullPointerException")
    void testNullConverterOptions() {
        // Arrange – make getOptions() return null
        when(mockConverter.getOptions()).thenReturn(null);
        LocalDateTime ldt = LocalDateTime.of(2023, 5, 10, 12, 30, 45);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toZonedDateTime(ldt, mockConverter), "Null options should cause a NullPointerException when getZoneId() is invoked");
    }

    @Test
    @DisplayName("Null ZoneId in ConverterOptions should raise NullPointerException")
    void testNullZoneIdInOptions() {
        // Arrange – options exist but return null ZoneId
        when(mockOptions.getZoneId()).thenReturn(null);
        LocalDateTime ldt = LocalDateTime.of(2023, 5, 10, 12, 30, 45);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toZonedDateTime(ldt, mockConverter), "ZonedDateTime.of with a null ZoneId must throw NullPointerException");
    }

    @Test
    @DisplayName("Non‑existent local time (DST gap) should raise DateTimeException")
    void testDstGapDateTimeException() {
        // Arrange – a LocalDateTime that does not exist in America/New_York (spring forward)
        LocalDateTime gapLdt = LocalDateTime.of(2023, 3, 12, 2, 30, 0);
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("America/New_York"));
        // Act & Assert
        assertThrows(DateTimeException.class, () -> LocalDateTimeConversions.toZonedDateTime(gapLdt, mockConverter), "ZonedDateTime.of should throw DateTimeException for a gap local time");
    }
}
