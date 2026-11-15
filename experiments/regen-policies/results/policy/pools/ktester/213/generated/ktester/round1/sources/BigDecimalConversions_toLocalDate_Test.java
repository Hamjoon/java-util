package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.Date;

/**
 * Tests for {@link BigDecimalConversions#toLocalDate(Object, Converter)}.
 */
@ExtendWith(MockitoExtension.class)
class BigDecimalConversions_toLocalDate_Test{

    /** Shared {@link Converter} instance used by all tests. */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // ConverterOptions has a public no‑arg constructor; use default options.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialization required at the moment.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required at the moment.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference.
        converter = null;
    }

    /**
     * Verify that a {@link java.util.Date} is correctly converted to a {@link LocalDate}.
     * The {@code Date} represents 2023‑01‑15T10:20:30Z. The expected {@code LocalDate}
     * is 2023‑01‑15 (the date component only).
     */
    @Test
    void toLocalDate_convertsDateToLocalDate() {
        // given
        Instant instant = Instant.parse("2023-01-15T10:20:30Z");
        Date date = Date.from(instant);

        // when
        LocalDate result = BigDecimalConversions.toLocalDate(date, converter);

        // then
        assertNotNull(result, "Resulting LocalDate should not be null");
        assertEquals(LocalDate.of(2023, 1, 15), result,
                "The LocalDate should match the date component of the supplied Date");
    }

    /**
     * Verify that a {@link java.time.ZonedDateTime} is correctly converted to a {@link LocalDate}.
     * The ZonedDateTime is created with a non‑UTC offset to ensure the conversion respects the
     * zone information.
     */
    @Test
    void toLocalDate_convertsZonedDateTimeToLocalDate() {
        // given
        ZonedDateTime zoned = ZonedDateTime.of(
                2022, 12, 31, 23, 45, 0, 0,
                ZoneId.of("America/New_York")); // UTC‑05:00 (standard time)

        // when
        LocalDate result = BigDecimalConversions.toLocalDate(zoned, converter);

        // then
        assertNotNull(result);
        // The date component should be 2022‑12‑31 regardless of the zone offset.
        assertEquals(LocalDate.of(2022, 12, 31), result);
    }

    /**
     * Verify that passing {@code null} as the source returns {@code null}.
     * This matches the behaviour of the underlying {@link Converter} which returns
     * {@code null} for object target types when the source is {@code null}.
     */
    @Test
    void toLocalDate_nullInputReturnsNull() {
        // given
        Object source = null;

        // when
        LocalDate result = BigDecimalConversions.toLocalDate(source, converter);

        // then
        assertNull(result, "Converting a null source should produce a null LocalDate");
    }
}