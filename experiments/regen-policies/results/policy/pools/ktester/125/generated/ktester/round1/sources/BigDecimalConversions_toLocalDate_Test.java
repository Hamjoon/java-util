package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.DateTimeException;
import java.time.format.DateTimeParseException;

@ExtendWith(MockitoExtension.class)
class BigDecimalConversions_toLocalDate_Test {

    /**
     * Converter instance used for all tests.
     */
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // The Converter has a public constructor that requires ConverterOptions.
        // Using the default options is sufficient for the built‑in conversions.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test.
        converter = null;
    }

    @Test
    void toLocalDate_fromBigDecimalEpochSecondZero_returns1970_01_01() {
        // GIVEN
        // 0 seconds since the epoch
        BigDecimal epochSeconds = BigDecimal.ZERO;
        // WHEN
        LocalDate result = BigDecimalConversions.toLocalDate(epochSeconds, converter);
        // THEN
        assertNotNull(result, "Resulting LocalDate should not be null");
        assertEquals(LocalDate.of(1970, 1, 1), result, "Epoch second 0 should map to 1970‑01‑01");
    }

    @Test
    void toLocalDate_fromBigDecimalPositiveValue_returnsCorrectDate() {
        // GIVEN
        // 86400 seconds = 1 day after epoch → 1970‑01‑02
        BigDecimal oneDayInSeconds = BigDecimal.valueOf(86_400);
        // WHEN
        LocalDate result = BigDecimalConversions.toLocalDate(oneDayInSeconds, converter);
        // THEN
        assertEquals(LocalDate.of(1970, 1, 2), result);
    }

    @Test
    void toLocalDate_fromNull_returnsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        LocalDate result = BigDecimalConversions.toLocalDate(source, converter);
        // THEN
        assertNull(result, "Converting a null source should return null");
    }

    @Test
    void toLocalDate_fromStringIsoDate_isConvertedViaConverter() {
        // GIVEN
        // The Converter can turn an ISO‑8601 date string into a LocalDate.
        String isoDate = "2023-08-15";
        // WHEN
        LocalDate result = BigDecimalConversions.toLocalDate(isoDate, converter);
        // THEN
        assertEquals(LocalDate.of(2023, 8, 15), result);
    }

    @Test
    @DisplayName("Calling toLocalDate with a null 'from' argument should throw NullPointerException")
    void testToLocalDate_NullFrom_throwsNullPointerException() {
        // GIVEN
        Object from = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> BigDecimalConversions.toLocalDate(from, converter), "Null source should cause NullPointerException");
    }

    @Test
    @DisplayName("Calling toLocalDate with a null Converter should throw NullPointerException")
    void testToLocalDate_NullConverter_throwsNullPointerException() {
        // GIVEN
        String from = "2023-01-01";
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> BigDecimalConversions.toLocalDate(from, null), "Null Converter should cause NullPointerException");
    }

    @Test
    @DisplayName("Invalid date string should result in DateTimeParseException")
    void testToLocalDate_InvalidString_throwsDateTimeParseException() {
        // GIVEN
        String invalidDate = "not-a-date";
        // WHEN / THEN
        assertThrows(DateTimeParseException.class, () -> BigDecimalConversions.toLocalDate(invalidDate, converter), "Unparseable string should trigger DateTimeParseException");
    }

    @Test
    @DisplayName("Unsupported source type should trigger IllegalArgumentException")
    void testToLocalDate_UnsupportedType_throwsIllegalArgumentException() {
        // GIVEN
        Object unsupported = new Object();
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> BigDecimalConversions.toLocalDate(unsupported, converter), "Unsupported source type should cause IllegalArgumentException");
    }

    @Test
    @DisplayName("Numeric value out of LocalDate range should cause DateTimeException")
    void testToLocalDate_NumberOutOfRange_throwsDateTimeException() {
        // GIVEN
        // Long.MAX_VALUE represents a far‑future epoch second far beyond LocalDate's limits.
        Long outOfRange = Long.MAX_VALUE;
        // WHEN / THEN
        assertThrows(DateTimeException.class, () -> BigDecimalConversions.toLocalDate(outOfRange, converter), "Out‑of‑range epoch seconds should trigger DateTimeException");
    }
}
