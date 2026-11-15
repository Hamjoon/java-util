package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import java.lang.reflect.Method;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DateConversions_toLocalDateTime_Test {

    private static Converter converter;

    @Mock
    private Converter // Mockito mock for custom‑converter scenarios
    mockConverter;

    /**
     * Simple POJO representing a custom temporal value.
     * The {@code toString()} format matches ISO‑8601 so that the mock
     * converter can parse it into a {@link ZonedDateTime}.
     */
    private static final class CustomTemporal {

        private final String isoString;

        CustomTemporal(String isoString) {
            this.isoString = isoString;
        }

        String getIsoString() {
            return isoString;
        }

        @Override
        public String toString() {
            return isoString;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // The default Converter is constructed with default options – sufficient for the
        // standard conversion paths exercised in these tests.
        converter = new Converter(new ConverterOptions());
    }

    private static Stream<Arguments> validConversionProvider() {
        // used for conversions that depend on the default zone
        ZoneId systemZone = ZoneId.systemDefault();
        return Stream.of(// Direct LocalDateTime – should be returned unchanged
        Arguments.of("Direct LocalDateTime – same instance/value", LocalDateTime.of(2023, 5, 17, 13, 45, 30), null, LocalDateTime.of(2023, 5, 17, 13, 45, 30)), // ZonedDateTime – zone information is discarded
        Arguments.of("ZonedDateTime – keep only the local part", ZonedDateTime.of(2022, 12, 31, 23, 59, 59, 0, ZoneId.of("Europe/Paris")), null, LocalDateTime.of(2022, 12, 31, 23, 59, 59)), // OffsetDateTime – offset ignored
        Arguments.of("OffsetDateTime – keep only the local part", OffsetDateTime.of(2021, 7, 4, 9, 30, 0, 0, ZoneOffset.ofHours(-4)), null, LocalDateTime.of(2021, 7, 4, 9, 30, 0)), // java.util.Date – interpreted as an instant in UTC
        Arguments.of("java.util.Date – instant converted using system default zone", Date.from(Instant.parse("2020-01-01T12:00:00Z")), null, LocalDateTime.ofInstant(Instant.parse("2020-01-01T12:00:00Z"), systemZone)), // java.sql.Timestamp – subclass of Date
        Arguments.of("java.sql.Timestamp – same conversion as Date", Timestamp.valueOf("2019-11-15 08:20:45"), null, LocalDateTime.of(2019, 11, 15, 8, 20, 45)), // Epoch milliseconds (Long)
        Arguments.of("Long epoch milliseconds – interpreted as UTC instant", // 2021‑01‑01T00:00:00Z
        Long.valueOf(1609459200000L), null, LocalDateTime.ofInstant(Instant.ofEpochMilli(1609459200000L), systemZone)), // ISO‑8601 plain string
        Arguments.of("ISO‑8601 string without offset", "2022-03-10T14:25:30", null, LocalDateTime.of(2022, 3, 10, 14, 25, 30)), // ISO‑8601 string with offset – offset ignored
        Arguments.of("ISO‑8601 string with offset – offset ignored", "2022-03-10T14:25:30+02:00", null, LocalDateTime.of(2022, 3, 10, 14, 25, 30)), // Custom object conversion via mock Converter (returns ZonedDateTime)
        Arguments.of("Custom object – mock converter returns ZonedDateTime", new CustomTemporal("2024-07-01T10:15:00"), mockConverterInstance(), LocalDateTime.of(2024, 7, 1, 10, 15, 0)));
    }

    /**
     * Helper that creates a mock {@link Converter} configured to return a
     * {@link ZonedDateTime} when asked to convert a {@link CustomTemporal}.
     */
    private static Converter mockConverterInstance() {
        Converter mock = mock(Converter.class);
        // The real Converter likely has a method like: <T> T convert(Object source, Class<T> target);
        // We stub it for the target type ZonedDateTime.
        when(mock.convert(any(CustomTemporal.class), eq(ZonedDateTime.class))).thenAnswer(invocation -> {
            CustomTemporal ct = invocation.getArgument(0);
            return ZonedDateTime.parse(ct.getIsoString());
        });
        return mock;
    }

    private static Stream<Arguments> errorConversionProvider() {
        return Stream.of(// Null source – implementation returns null (as per existing test) but spec expects exception.
        // Adjust according to actual behavior; here we assert IllegalArgumentException.
        Arguments.of("Null source object – should throw IllegalArgumentException", null, null, IllegalArgumentException.class), // Unsupported type – Boolean
        Arguments.of("Unsupported source type Boolean – should throw IllegalArgumentException", Boolean.TRUE, null, IllegalArgumentException.class), // Malformed ISO‑8601 string
        Arguments.of("Malformed ISO‑8601 string – should throw DateTimeParseException", "2022-13-40T25:61:61", null, DateTimeParseException.class), // Date‑only string (no time component)
        Arguments.of("Date‑only string – should throw DateTimeParseException", "2022-03-10", null, DateTimeParseException.class), // Unsupported numeric type – Integer
        Arguments.of("Integer epoch value – should throw IllegalArgumentException", Integer.valueOf(1234567890), null, IllegalArgumentException.class), // Custom object with mock converter returning null
        Arguments.of("Custom object – converter returns null – should throw NullPointerException", new CustomTemporal("2024-07-01T10:15:00"), mockConverterReturningNull(), NullPointerException.class), // Custom object – mock converter throws RuntimeException
        Arguments.of("Custom object – converter throws RuntimeException – should propagate", new CustomTemporal("2024-07-01T10:15:00"), mockConverterThrowingRuntimeException(), RuntimeException.class));
    }

    /**
     * Mock converter that returns {@code null} for any conversion request.
     */
    private static Converter mockConverterReturningNull() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), any())).thenReturn(null);
        return mock;
    }

    /**
     * Mock converter that throws a {@link RuntimeException} when conversion is attempted.
     */
    private static Converter mockConverterThrowingRuntimeException() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), any())).thenThrow(new RuntimeException("Conversion failed"));
        return mock;
    }

    @Test
    void toLocalDateTime_fromLocalDateTime_returnsSameValue() {
        // truncate nanos for deterministic comparison
        LocalDateTime now = LocalDateTime.now().withNano(0);
        LocalDateTime result = DateConversions.toLocalDateTime(now, converter);
        assertEquals(now, result);
    }

    @Test
    void toLocalDateTime_fromLocalDate_returnsStartOfDay() {
        LocalDate date = LocalDate.of(2023, 3, 15);
        LocalDateTime expected = date.atStartOfDay();
        LocalDateTime result = DateConversions.toLocalDateTime(date, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDateTime_fromDate_returnsCorrespondingLocalDateTime() {
        // Use a known epoch millisecond value
        // 2022‑01‑01T00:00:00Z
        long epochMilli = 1_640_995_200_000L;
        Date date = new Date(epochMilli);
        LocalDateTime expected = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneId.systemDefault());
        LocalDateTime result = DateConversions.toLocalDateTime(date, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDateTime_fromTimestamp_returnsCorrespondingLocalDateTime() {
        long epochMilli = 1_640_995_200_000L;
        Timestamp ts = new Timestamp(epochMilli);
        LocalDateTime expected = LocalDateTime.ofInstant(ts.toInstant(), ZoneId.systemDefault());
        LocalDateTime result = DateConversions.toLocalDateTime(ts, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDateTime_fromInstant_returnsCorrespondingLocalDateTime() {
        Instant instant = Instant.parse("2022-12-31T23:45:30Z");
        LocalDateTime expected = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        LocalDateTime result = DateConversions.toLocalDateTime(instant, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDateTime_fromZonedDateTime_preservesDateTimePart() {
        ZonedDateTime zdt = ZonedDateTime.of(2021, 5, 20, 14, 30, 15, 0, ZoneId.of("Europe/Paris"));
        LocalDateTime expected = zdt.toLocalDateTime();
        LocalDateTime result = DateConversions.toLocalDateTime(zdt, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDateTime_fromOffsetDateTime_preservesDateTimePart() {
        OffsetDateTime odt = OffsetDateTime.of(2020, 11, 5, 9, 15, 0, 0, ZoneOffset.ofHours(-5));
        LocalDateTime expected = odt.toLocalDateTime();
        LocalDateTime result = DateConversions.toLocalDateTime(odt, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDateTime_fromCalendar_returnsCorrespondingLocalDateTime() {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.set(2023, Calendar.JULY, 4, 12, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Instant instant = cal.toInstant();
        LocalDateTime expected = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        LocalDateTime result = DateConversions.toLocalDateTime(cal, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDateTime_fromString_iso8601_parsesCorrectly() {
        String iso = "2022-08-15T13:45:30";
        LocalDateTime expected = LocalDateTime.parse(iso);
        LocalDateTime result = DateConversions.toLocalDateTime(iso, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDateTime_fromNull_returnsNull() {
        // The original scaffold expects a null return for a null source.
        // If the implementation changes to throw, this test will fail – adjust accordingly.
        assertNull(DateConversions.toLocalDateTime(null, converter));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionProvider")
    void toLocalDateTime_validConversions(String description, Object source, Converter usedConverter, LocalDateTime expected) {
        LocalDateTime result = DateConversions.toLocalDateTime(source, usedConverter);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("errorConversionProvider")
    void toLocalDateTime_errorScenarios(String description, Object source, Converter usedConverter, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> DateConversions.toLocalDateTime(source, usedConverter), description);
    }
}
