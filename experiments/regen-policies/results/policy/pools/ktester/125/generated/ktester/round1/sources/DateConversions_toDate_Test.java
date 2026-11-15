package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DateConversions_toDate_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Use the real Converter with default options – this gives us the full conversion
        // capabilities that {@code DateConversions} relies on.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the pattern consistent.
        converter = null;
    }

    private static Stream<Arguments> basicNumericAndDateProvider() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        // 2021‑06‑01T12:00:00Z
        cal.setTimeInMillis(1622548800000L);
        return Stream.of(// Long value – should be interpreted as milliseconds since epoch
        Arguments.of(Long.valueOf(1622548800000L), 1622548800000L, "Convert a Long value directly to a Date; verifies that toLong extracts the long value unchanged."), // Integer value – treated as milliseconds (note the smaller magnitude)
        Arguments.of(Integer.valueOf(1622548800), 1622548800L, "Convert an Integer (treated as milliseconds) to a Date; shows that numeric types are cast to long."), // Existing java.util.Date – toLong should return its getTime()
        Arguments.of(new Date(1622548800000L), 1622548800000L, "Pass an existing java.util.Date; toLong should return its getTime() value."), // Calendar instance – toLong should use Calendar#getTimeInMillis()
        Arguments.of(cal, 1622548800000L, "Convert a Calendar instance; toLong should use Calendar#getTimeInMillis()."));
    }

    private static Stream<Arguments> stringRepresentationProvider() {
        // ISO‑8601 UTC string
        String iso = "2021-06-01T12:00:00Z";
        // Locale‑specific pattern (MM/dd/yyyy HH:mm:ss) – the conversion uses the default parser,
        // which interprets the string in the system default time‑zone.  For reproducibility we
        // convert it to epoch using the default zone.
        String locale = "06/01/2021 12:00:00";
        // Numeric string representing epoch millis
        String numeric = "1622548800000";
        // Compute expected epoch for the locale‑specific string using the default zone.
        // The test assumes the default zone is UTC; if not, the conversion may differ.
        // To keep the test deterministic we force UTC by creating a Calendar in UTC.
        Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCal.set(2021, Calendar.JUNE, 1, 12, 0, 0);
        utcCal.set(Calendar.MILLISECOND, 0);
        long localeExpected = utcCal.getTimeInMillis();
        return Stream.of(Arguments.of(iso, 1622548800000L, "ISO‑8601 UTC string should be parsed to an instant and converted to a Date with the same epoch milliseconds."), Arguments.of(locale, localeExpected, "Locale‑specific date‑time string (MM/dd/yyyy HH:mm:ss) should be parsed using the default formatter and converted to the correct epoch."), Arguments.of(numeric, 1622548800000L, "Numeric string representing milliseconds since epoch should be interpreted as a long value."));
    }

    @Test
    void toDate_NullInput_ReturnsEpochDate() {
        // given
        Object from = null;
        // when
        Date result = DateConversions.toDate(from, converter);
        // then
        assertNotNull(result, "Resulting Date should never be null");
        assertEquals(0L, result.getTime(), "Null input should be converted to epoch (0 ms)");
    }

    @Test
    void toDate_DateInput_ReturnsEqualInstantButNewInstance() {
        // given
        // 2022-01-01T00:00:00Z
        Date original = new Date(1_640_995_200_000L);
        // when
        Date result = DateConversions.toDate(original, converter);
        // then
        assertNotNull(result);
        assertEquals(original.getTime(), result.getTime(), "Converted Date should have the same epoch milliseconds as the source");
        assertNotSame(original, result, "Converted Date should be a new instance, not the original object");
    }

    @Test
    void toDate_LongInput_ReturnsCorrespondingDate() {
        // given
        // 2022-01-01T00:00:00Z
        Long millis = 1_640_995_200_000L;
        // when
        Date result = DateConversions.toDate(millis, converter);
        // then
        assertEquals(millis.longValue(), result.getTime(), "Long input should be interpreted as epoch milliseconds");
    }

    @Test
    void toDate_NumericStringInput_ReturnsCorrespondingDate() {
        // given
        // same instant as above
        String millisString = "1640995200000";
        // when
        Date result = DateConversions.toDate(millisString, converter);
        // then
        assertEquals(Long.parseLong(millisString), result.getTime(), "Numeric String should be parsed to long and converted to Date");
    }

    @Test
    void toDate_CalendarInput_ReturnsCorrespondingDate() {
        // given
        Calendar cal = new GregorianCalendar();
        // 2022-01-01T00:00:00Z
        cal.setTimeInMillis(1_640_995_200_000L);
        // when
        Date result = DateConversions.toDate(cal, converter);
        // then
        assertEquals(cal.getTimeInMillis(), result.getTime(), "Calendar input should be converted using its time-in-millis value");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("basicNumericAndDateProvider")
    void toDate_BasicNumericAndDateTypes(Object from, long expectedEpochMillis, String description) {
        // when
        Date result = DateConversions.toDate(from, converter);
        // then
        assertNotNull(result, description);
        assertEquals(expectedEpochMillis, result.getTime(), () -> "Expected epoch millis " + expectedEpochMillis + " for case: " + description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("stringRepresentationProvider")
    void toDate_StringRepresentations(Object from, long expectedEpochMillis, String description) {
        Date result = DateConversions.toDate(from, converter);
        assertEquals(expectedEpochMillis, result.getTime(), () -> "Failed on case: " + description);
    }

    @Test
    void toDate_NullSource_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> DateConversions.toDate(null, converter), "Passing a null `from` argument should cause a NullPointerException.");
    }

    @Test
    void toDate_UnsupportedType_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> DateConversions.toDate(Boolean.TRUE, converter), "A Boolean value is not a supported source type for date conversion; should throw IllegalArgumentException.");
    }

    @Test
    void toDate_MalformedIsoString_ThrowsDateTimeParseException() {
        String malformed = "2021-13-01T25:61:00Z";
        assertThrows(DateTimeParseException.class, () -> DateConversions.toDate(malformed, converter), "Malformed ISO‑8601 string should cause DateTimeParseException.");
    }

    @Test
    void toDate_NonNumericString_ThrowsNumberFormatException() {
        String notADate = "not-a-date-or-number";
        assertThrows(NumberFormatException.class, () -> DateConversions.toDate(notADate, converter), "Non‑numeric, non‑date string should result in NumberFormatException.");
    }

    @Test
    void toDate_NumericOverflowString_ThrowsNumberFormatException() {
        // Long.MAX_VALUE + 1
        String overflow = "9223372036854775808";
        assertThrows(NumberFormatException.class, () -> DateConversions.toDate(overflow, converter), "String representing a number larger than Long.MAX_VALUE should throw NumberFormatException.");
    }

    @Test
    void toDate_ConverterThrowsException_PropagatesRuntimeException() {
        // Mock a Converter that throws a RuntimeException for any conversion request.
        Converter badConverter = mock(Converter.class);
        when(badConverter.convert(any(), any())).thenThrow(new RuntimeException("converter failure"));
        String iso = "2021-06-01T12:00:00Z";
        RuntimeException ex = assertThrows(RuntimeException.class, () -> DateConversions.toDate(iso, badConverter), "Exception from custom Converter should propagate.");
        assertEquals("converter failure", ex.getMessage(), "Propagated exception should retain original message.");
    }
}
