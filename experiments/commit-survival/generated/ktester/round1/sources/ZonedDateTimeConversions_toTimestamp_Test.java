package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ZonedDateTimeConversions_toTimestamp_Test {

    /**
     * A mock Converter – the implementation of {@code toTimestamp} does not use it,
     *  but it is required by the method signature.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Simple holder for a single valid test case.
     */
    private static final class ValidCase {

        final ZonedDateTime zonedDateTime;

        final Converter converter;

        final Timestamp expected;

        final String description;

        ValidCase(ZonedDateTime zonedDateTime, Converter converter, Timestamp expected) {
            this.zonedDateTime = zonedDateTime;
            this.converter = converter;
            this.expected = expected;
            this.description = String.format("ZonedDateTime %s with converter %s", zonedDateTime, (converter == null ? "null" : converter.getClass().getSimpleName()));
        }

        @Override
        public String toString() {
            return description;
        }
    }

    /**
     * Minimal dummy implementation of {@link Converter} used in one test case.
     *  The class is intentionally empty because the conversion method does not
     *  interact with it.
     */
    private static class DummyConverter implements Converter {
        // No methods need to be overridden for this test.
    }

    /**
     * Supplies test data for valid conversions.
     *
     * <ul>
     *   <li>UTC ZonedDateTime – the resulting Timestamp should represent the same instant.</li>
     *   <li>America/New_York ZonedDateTime – the resulting Timestamp must reflect the
     *       underlying UTC instant, not the local wall‑time.</li>
     * </ul>
     *
     * @return a stream of {@link ValidCase} objects.
     */
    private static Stream<ValidCase> validCasesProvider() {
        return Stream.of(// UTC case – converter is null (allowed, method does not use it)
        new ValidCase(ZonedDateTime.of(2023, 3, 15, 10, 30, 0, 0, ZoneId.of("UTC")), null, Timestamp.from(ZonedDateTime.of(2023, 3, 15, 10, 30, 0, 0, ZoneId.of("UTC")).toInstant())), // New York case – using a dummy converter instance
        new ValidCase(ZonedDateTime.of(2023, 3, 15, 15, 30, 0, 0, ZoneId.of("America/New_York")), new DummyConverter(), // 2023‑03‑15 15:30 in New York is 19:30 UTC (offset -04:00 on that date)
        Timestamp.from(ZonedDateTime.of(2023, 3, 15, 19, 30, 0, 0, ZoneId.of("UTC")).toInstant())));
    }

    @Test
    void toTimestamp_convertsUtcZonedDateTimeCorrectly() {
        // GIVEN
        ZonedDateTime zdt = ZonedDateTime.of(// year, month, day
        2023, // year, month, day
        5, // year, month, day
        10, // hour, minute, second, nano
        15, // hour, minute, second, nano
        30, // hour, minute, second, nano
        45, // hour, minute, second, nano
        123_000_000, // zone
        ZoneId.of("UTC"));
        Timestamp expected = Timestamp.from(zdt.toInstant());
        // WHEN
        Timestamp actual = ZonedDateTimeConversions.toTimestamp(zdt, mockConverter);
        // THEN
        assertEquals(expected, actual, "Timestamp produced from a UTC ZonedDateTime should match the instant-derived Timestamp");
    }

    @Test
    void toTimestamp_convertsZonedDateTimeWithDifferentZoneCorrectly() {
        // GIVEN
        ZonedDateTime zdt = ZonedDateTime.of(2023, 5, 10, 10, 30, 45, 0, // Eastern Time (UTC‑5/‑4)
        ZoneId.of("America/New_York"));
        Timestamp expected = Timestamp.from(zdt.toInstant());
        // WHEN
        Timestamp actual = ZonedDateTimeConversions.toTimestamp(zdt, mockConverter);
        // THEN
        assertEquals(expected, actual, "Timestamp should represent the same instant regardless of the original time‑zone");
    }

    @Test
    void toTimestamp_throwsClassCastExceptionWhenInputIsNotZonedDateTime() {
        // GIVEN
        Object notZdt = "I am not a ZonedDateTime";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> ZonedDateTimeConversions.toTimestamp(notZdt, mockConverter), "A non‑ZonedDateTime input must cause a ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validCasesProvider")
    void toTimestamp_validConversions(ValidCase tc) {
        // GIVEN – the case already provides the input ZonedDateTime and expected Timestamp
        Object from = tc.zonedDateTime;
        // may be null or a dummy instance
        Converter converter = tc.converter;
        // WHEN
        Timestamp actual = ZonedDateTimeConversions.toTimestamp(from, converter);
        // THEN
        assertEquals(tc.expected, actual, () -> "Timestamp mismatch for input " + from);
    }

    @Test
    void toTimestamp_nullFrom_throwsNullPointerException() {
        // GIVEN
        Object from = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toTimestamp(from, mockConverter), "Passing null as 'from' should result in NullPointerException");
    }

    @Test
    void toTimestamp_incompatibleType_throwsClassCastException() {
        // GIVEN
        // String, not a ZonedDateTime
        Object from = "2023-03-15T10:30:00Z";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> ZonedDateTimeConversions.toTimestamp(from, mockConverter), "Providing a non‑ZonedDateTime value must cause ClassCastException");
    }
}
