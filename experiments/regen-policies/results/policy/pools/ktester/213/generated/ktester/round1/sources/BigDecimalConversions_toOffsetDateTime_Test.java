package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.Date;

/**
 * Tests for {@link BigDecimalConversions#toOffsetDateTime(Object, Converter)}.
 *
 * The tests focus on the public contract of the method:
 * <ul>
 *   <li>Conversion of various temporal types supported by {@link Converter}.</li>
 *   <li>Handling of {@code null} input.</li>
 * </ul>
 *
 * The {@link Converter} is created with default {@link ConverterOptions} and is reused for each test.
 */
@ExtendWith(MockitoExtension.class)
class BigDecimalConversions_toOffsetDateTime_Test{

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a fresh Converter for every test to avoid cross‑test contamination.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the static USER_DB map inside Converter (if any) to keep tests isolated.
        // Converter does not expose a clear method, but creating a new instance per test is sufficient.
        converter = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up.
    }

    @Test
    void shouldReturnNullWhenSourceIsNull() {
        // GIVEN
        Object source = null;

        // WHEN
        OffsetDateTime result = BigDecimalConversions.toOffsetDateTime(source, converter);

        // THEN
        assertNull(result, "Converting a null source should return null");
    }

    @Test
    void shouldConvertOffsetDateTimeDirectly() {
        // GIVEN
        OffsetDateTime source = OffsetDateTime.now();

        // WHEN
        OffsetDateTime result = BigDecimalConversions.toOffsetDateTime(source, converter);

        // THEN
        assertEquals(source, result, "OffsetDateTime should be returned unchanged");
    }

    @Test
    void shouldConvertZonedDateTimeToOffsetDateTime() {
        // GIVEN
        ZonedDateTime source = ZonedDateTime.now();

        // Expected value using the public Converter API (the same logic that BigDecimalConversions delegates to)
        OffsetDateTime expected = converter.convert(source, OffsetDateTime.class);

        // WHEN
        OffsetDateTime actual = BigDecimalConversions.toOffsetDateTime(source, converter);

        // THEN
        assertEquals(expected, actual, "ZonedDateTime should be converted to an equivalent OffsetDateTime");
    }

    @Test
    void shouldConvertInstantToOffsetDateTime() {
        // GIVEN
        Instant source = Instant.now();

        OffsetDateTime expected = converter.convert(source, OffsetDateTime.class);
        OffsetDateTime actual = BigDecimalConversions.toOffsetDateTime(source, converter);

        assertEquals(expected, actual, "Instant should be converted to an equivalent OffsetDateTime");
    }

    @Test
    void shouldConvertDateToOffsetDateTime() {
        // GIVEN
        Date source = new Date(); // current moment

        OffsetDateTime expected = converter.convert(source, OffsetDateTime.class);
        OffsetDateTime actual = BigDecimalConversions.toOffsetDateTime(source, converter);

        assertEquals(expected, actual, "java.util.Date should be converted to an equivalent OffsetDateTime");
    }

    @Test
    void shouldConvertLocalDateTimeToOffsetDateTimeUsingSystemDefaultZone() {
        // GIVEN
        LocalDateTime source = LocalDateTime.now();

        OffsetDateTime expected = converter.convert(source, OffsetDateTime.class);
        OffsetDateTime actual = BigDecimalConversions.toOffsetDateTime(source, converter);

        assertEquals(expected, actual, "LocalDateTime should be converted using the system default zone");
    }

    @Test
    void shouldConvertLocalDateToOffsetDateTimeAtStartOfDay() {
        // GIVEN
        LocalDate source = LocalDate.now();

        OffsetDateTime expected = converter.convert(source, OffsetDateTime.class);
        OffsetDateTime actual = BigDecimalConversions.toOffsetDateTime(source, converter);

        assertEquals(expected, actual, "LocalDate should be converted to the start of day OffsetDateTime");
    }
}