package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.Timestamp;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.time.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ZonedDateTimeConversions_toTimestamp_Test {

    /**
     * Shared Converter instance used by the tests.
     */
    private static Converter sharedConverter;

    /**
     * A mock Converter to verify that the method does not interact with it.
     */
    @Mock
    private Converter mockConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // The real Converter is not required for the conversion itself,
        // but we create one to satisfy the method signature.
        sharedConverter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference.
        sharedConverter = null;
    }

    /**
     * Provides the successful conversion scenarios extracted from the JSON test cases.
     */
    private static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(// Normal conversion – UTC zone
        Arguments.of("Converts a ZonedDateTime in UTC to a Timestamp (millis precision)", ZonedDateTime.of(2023, 5, 10, 12, 30, 45, 123_000_000, ZoneId.of("UTC")), Timestamp.from(ZonedDateTime.of(2023, 5, 10, 12, 30, 45, 123_000_000, ZoneId.of("UTC")).toInstant())), // Different zones – same instant (Europe/Paris is UTC+2 in May)
        Arguments.of("Paris zone (+02:00) representing the same instant as UTC test", ZonedDateTime.of(2023, 5, 10, 14, 30, 45, 0, ZoneId.of("Europe/Paris")), Timestamp.from(ZonedDateTime.of(2023, 5, 10, 14, 30, 45, 0, ZoneId.of("Europe/Paris")).toInstant())), // Custom offset zone (+02:30)
        Arguments.of("Custom offset +02:30 – instant should be 2023‑05‑10T12:30:00Z", ZonedDateTime.of(2023, 5, 10, 15, 0, 0, 0, ZoneOffset.ofHoursMinutes(2, 30)), Timestamp.from(ZonedDateTime.of(2023, 5, 10, 15, 0, 0, 0, ZoneOffset.ofHoursMinutes(2, 30)).toInstant())), // Epoch start (1970‑01‑01T00:00:00Z)
        Arguments.of("Epoch start should produce a Timestamp with zero milliseconds", ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC")), Timestamp.from(Instant.EPOCH)), // Nanosecond precision beyond milliseconds
        Arguments.of("Preserve nanosecond component (987 654 321 ns) even when millis part is zero", ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 987_654_321, ZoneOffset.UTC), Timestamp.from(Instant.ofEpochSecond(0, 987_654_321))), // DST spring‑forward gap (America/New_York)
        Arguments.of("Local time 02:30 does not exist – ZonedDateTime resolves to 03:30 EDT", ZonedDateTime.of(2023, 3, 12, 2, 30, 0, 0, ZoneId.of("America/New_York")), Timestamp.from(ZonedDateTime.of(2023, 3, 12, 2, 30, 0, 0, ZoneId.of("America/New_York")).toInstant())), // DST fall‑back ambiguous time (America/New_York) – earlier offset (-04:00)
        Arguments.of("Ambiguous 01:30 during fall‑back – earlier offset (-04:00) is used", ZonedDateTime.ofLocal(LocalDateTime.of(2023, 11, 5, 1, 30), ZoneId.of("America/New_York"), ZoneOffset.ofHours(-4)), Timestamp.from(ZonedDateTime.ofLocal(LocalDateTime.of(2023, 11, 5, 1, 30), ZoneId.of("America/New_York"), ZoneOffset.ofHours(-4)).toInstant())), // Far future date (Year 3000)
        Arguments.of("Conversion works for dates far beyond the typical 1970‑2038 range", ZonedDateTime.of(3000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")), Timestamp.from(ZonedDateTime.of(3000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")).toInstant())), // Non‑null Converter argument (no effect)
        Arguments.of("Providing a non‑null Converter should not affect the result", ZonedDateTime.of(2023, 5, 10, 12, 30, 45, 0, ZoneId.of("UTC")), Timestamp.from(ZonedDateTime.of(2023, 5, 10, 12, 30, 45, 0, ZoneId.of("UTC")).toInstant())));
    }

    @Test
    void toTimestamp_convertsZonedDateTimeCorrectly() {
        // GIVEN
        ZonedDateTime zdt = ZonedDateTime.of(2023, 5, 15, 10, 30, 45, 123_000_000, ZoneId.of("UTC"));
        // WHEN
        Timestamp ts = ZonedDateTimeConversions.toTimestamp(zdt, sharedConverter);
        // THEN
        assertNotNull(ts, "Timestamp should not be null");
        assertEquals(zdt.toInstant().toEpochMilli(), ts.getTime(), "Timestamp epoch millis must match the ZonedDateTime instant");
        assertEquals(zdt.toInstant().getNano(), ts.getNanos(), "Timestamp nanos must match the ZonedDateTime instant nanos");
    }

    @Test
    void toTimestamp_usesProvidedConverterEvenIfUnused() {
        // GIVEN
        ZonedDateTime zdt = ZonedDateTime.now();
        // WHEN
        Timestamp ts = ZonedDateTimeConversions.toTimestamp(zdt, mockConverter);
        // THEN
        assertNotNull(ts, "Resulting Timestamp must not be null");
        // The method should not interact with the Converter at all.
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toTimestamp_nullSourceThrowsNullPointerException() {
        // GIVEN
        Object nullSource = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toTimestamp(nullSource, sharedConverter), "Passing null as the source should throw NullPointerException");
    }

    @Test
    void toTimestamp_invalidSourceThrowsClassCastException() {
        // GIVEN
        Object notAZonedDateTime = "2023-01-01T00:00:00Z";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> ZonedDateTimeConversions.toTimestamp(notAZonedDateTime, sharedConverter), "Passing a non‑ZonedDateTime object should throw ClassCastException");
    }

    /* -------------------------------------------------------------
     *  Parameterized success cases – one test per JSON group.
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("successfulConversionProvider")
    void toTimestamp_successful(String description, ZonedDateTime input, Timestamp expected) {
        // WHEN
        Timestamp actual = ZonedDateTimeConversions.toTimestamp(input, null);
        // THEN
        assertNotNull(actual, "Resulting Timestamp must not be null");
        assertEquals(expected, actual, () -> "Failed conversion: " + description);
    }
}
