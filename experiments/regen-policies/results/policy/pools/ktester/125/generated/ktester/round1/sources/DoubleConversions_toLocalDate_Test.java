package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.Date;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DoubleConversions_toLocalDate_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // ConverterOptions has a public no‑arg constructor; use default options.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up for the static converter.
        converter = null;
    }

    /**
     * Parameter provider for {@link #testToLocalDateExceptions(Object, Converter, Class)}.
     *
     * <p>Each {@link Arguments} entry corresponds to a test case from the JSON
     * description. The fourth element (a {@code String}) is only used for a
     * readable display name in the test report.</p>
     */
    private static Stream<Arguments> exceptionCases() {
        // Helper values used across several cases
        Date sampleDate = Date.from(Instant.parse("2023-01-01T00:00:00Z"));
        // not a supported type for toZonedDateTime
        String unsupportedString = "2023-01-01T00:00:00Z";
        LocalDate localDate = LocalDate.of(2023, 1, 1);
        Object unrelatedObject = new Object();
        return Stream.of(// 1. null 'from' argument
        Arguments.of(null, converter, NullPointerException.class, "null 'from' should trigger NullPointerException"), // 2. null 'converter' argument
        Arguments.of(sampleDate, null, NullPointerException.class, "null 'converter' should trigger NullPointerException"), // 3. Unsupported String value
        Arguments.of(unsupportedString, converter, IllegalArgumentException.class, "unsupported String should trigger IllegalArgumentException"), // 4. LocalDate (no zone/offset) – should be illegal for ZonedDateTime conversion
        Arguments.of(localDate, converter, IllegalArgumentException.class, "LocalDate should trigger IllegalArgumentException"), // 5. Completely unrelated object type
        Arguments.of(unrelatedObject, converter, ClassCastException.class, "unrelated Object should trigger ClassCastException"));
    }

    @Test
    void toLocalDate_fromLocalDate_returnsSameInstance() {
        LocalDate source = LocalDate.of(2023, 5, 17);
        LocalDate result = DoubleConversions.toLocalDate(source, converter);
        assertEquals(source, result);
    }

    @Test
    void toLocalDate_fromLocalDateTime_returnsDatePart() {
        LocalDateTime source = LocalDateTime.of(2023, 5, 17, 13, 45, 30);
        LocalDate expected = source.toLocalDate();
        LocalDate result = DoubleConversions.toLocalDate(source, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDate_fromZonedDateTime_returnsDatePart() {
        ZonedDateTime source = ZonedDateTime.of(2023, 5, 17, 13, 45, 30, 0, ZoneId.of("UTC"));
        LocalDate expected = source.toLocalDate();
        LocalDate result = DoubleConversions.toLocalDate(source, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDate_fromInstant_returnsDateInSystemDefaultZone() {
        Instant instant = Instant.parse("2023-05-17T13:45:30Z");
        ZoneId systemZone = ZoneId.systemDefault();
        LocalDate expected = instant.atZone(systemZone).toLocalDate();
        LocalDate result = DoubleConversions.toLocalDate(instant, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDate_fromDate_returnsDateInSystemDefaultZone() {
        Instant instant = Instant.parse("2023-05-17T13:45:30Z");
        Date date = Date.from(instant);
        ZoneId systemZone = ZoneId.systemDefault();
        LocalDate expected = instant.atZone(systemZone).toLocalDate();
        LocalDate result = DoubleConversions.toLocalDate(date, converter);
        assertEquals(expected, result);
    }

    /**
     * Executes the exception‑handling scenarios defined in {@link #exceptionCases()}.
     *
     * @param from          the source object passed to {@code toLocalDate}
     * @param conv          the {@link Converter} instance (may be {@code null})
     * @param expectedEx    the type of exception that should be thrown
     */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("exceptionCases")
    void testToLocalDateExceptions(Object from, Converter conv, Class<? extends Throwable> expectedEx, String _displayName) {
        // The method under test is expected to throw the specified exception.
        assertThrows(expectedEx, () -> DoubleConversions.toLocalDate(from, conv));
    }
}
