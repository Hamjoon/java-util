package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.ZoneId;
import java.util.Date;
import java.util.stream.Stream;
import java.util.function.Supplier;

@ExtendWith(MockitoExtension.class)
class TimeZoneConversions_toString_Test {

    /**
     * Converter instance – the method under test does not use it, but it must be supplied.
     */
    private Converter converter;

    /* --------------------------------------------------------------
     *  Helper holder class for parameterised tests
     * -------------------------------------------------------------- */
    private static final class ArgumentsHolder {

        final Object from;

        // may be null
        final Converter converter;

        // either String result or Class<? extends Throwable>
        final Object expected;

        final String description;

        ArgumentsHolder(Object from, Converter converter, Object expected, String description) {
            this.from = from;
            this.converter = converter;
            this.expected = expected;
            this.description = description;
        }
    }

    /**
     * A {@link TimeZone} implementation whose {@link #getID()} method deliberately
     * throws a supplied unchecked exception.  All other abstract methods are given
     * trivial implementations because they are never called by {@code toString}.
     */
    private static final class FaultyTimeZone extends TimeZone {

        private final Supplier<? extends RuntimeException> exceptionSupplier;

        FaultyTimeZone(Supplier<? extends RuntimeException> exceptionSupplier) {
            this.exceptionSupplier = exceptionSupplier;
        }

        @Override
        public String getID() {
            throw exceptionSupplier.get();
        }

        // The following methods are required concrete implementations but are not used
        // by the method under test.  Simple stub behaviour is sufficient.
        @Override
        public int getRawOffset() {
            return 0;
        }

        @Override
        public void setRawOffset(int offsetMillis) {
            // no‑op
        }

        @Override
        public boolean useDaylightTime() {
            return false;
        }

        @Override
        public boolean inDaylightTime(java.util.Date date) {
            return false;
        }

        @Override
        public java.util.TimeZone getDefault() {
            return TimeZone.getDefault();
        }

        @Override
        public java.util.TimeZone clone() {
            return (TimeZone) super.clone();
        }

        @Override
        public String getDisplayName(boolean daylight, int style, java.util.Locale locale) {
            return "FaultyTimeZone";
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // The real Converter can be used; its behaviour is irrelevant for this method.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test.
        converter = null;
    }

    private static Stream<ArgumentsHolder> normalBehaviorProvider() {
        return Stream.of(new ArgumentsHolder(TimeZone.getTimeZone("GMT"), null, "GMT", "When a TimeZone instance representing GMT is passed, toString should return its ID \"GMT\"."), new ArgumentsHolder(TimeZone.getTimeZone("America/New_York"), null, "America/New_York", "When a TimeZone instance for America/New_York is passed, toString should return the exact zone ID."));
    }

    private static Stream<ArgumentsHolder> customIdProvider() {
        return Stream.of(new ArgumentsHolder(TimeZone.getTimeZone("GMT+02:00"), null, "GMT+02:00", "TimeZone created with a GMT+02:00 offset should return that exact ID."), new ArgumentsHolder(TimeZone.getTimeZone("GMT-05:30"), null, "GMT-05:30", "Negative GMT offset strings are also valid IDs and should be returned unchanged."));
    }

    private static Stream<ArgumentsHolder> invalidTypeProvider() {
        return Stream.of(new ArgumentsHolder("not a timezone", null, ClassCastException.class, "If 'from' is a String, the cast fails and a ClassCastException should be thrown."), new ArgumentsHolder(new Date(), null, ClassCastException.class, "Providing an object of an unrelated type (Date) should also result in a ClassCastException."));
    }

    private static Stream<ArgumentsHolder> faultyTimeZoneProvider() {
        return Stream.of(new ArgumentsHolder(new FaultyTimeZone(() -> new IllegalStateException("boom")), null, IllegalStateException.class, "A TimeZone subclass whose getID() throws IllegalStateException should propagate it."), new ArgumentsHolder(new FaultyTimeZone(() -> new NullPointerException("null!")), null, NullPointerException.class, "A TimeZone subclass whose getID() throws NullPointerException should propagate it."));
    }

    @Test
    void toString_returnsTimeZoneId_forValidTimeZone() {
        // GIVEN
        TimeZone tz = TimeZone.getTimeZone("UTC");
        // WHEN
        String result = TimeZoneConversions.toString(tz, converter);
        // THEN
        assertEquals(tz.getID(), result);
    }

    @Test
    void toString_returnsCustomTimeZoneId() {
        // GIVEN
        // A non‑UTC zone – e.g., GMT+02:00
        TimeZone tz = TimeZone.getTimeZone("GMT+02:00");
        // WHEN
        String result = TimeZoneConversions.toString(tz, converter);
        // THEN
        assertEquals(tz.getID(), result);
    }

    @Test
    void toString_throwsClassCastException_whenObjectIsNotTimeZone() {
        // GIVEN
        Object notATimeZone = "I am not a TimeZone";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> TimeZoneConversions.toString(notATimeZone, converter));
    }

    @Test
    void toString_throwsNullPointerException_whenFromIsNull() {
        // GIVEN
        Object nullObj = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> TimeZoneConversions.toString(nullObj, converter));
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("normalBehaviorProvider")
    void toString_returnsExpectedId_forValidTimeZone(ArgumentsHolder holder) {
        // GIVEN
        Object from = holder.from;
        // may be null per test data
        Converter conv = holder.converter;
        // WHEN
        String result = TimeZoneConversions.toString(from, conv);
        // THEN
        assertEquals(holder.expected, result);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("customIdProvider")
    void toString_returnsExactOffsetId_forGmtOffsetTimeZone(ArgumentsHolder holder) {
        // GIVEN
        Object from = holder.from;
        Converter conv = holder.converter;
        // WHEN
        String result = TimeZoneConversions.toString(from, conv);
        // THEN
        assertEquals(holder.expected, result);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("invalidTypeProvider")
    void toString_throwsClassCastException_forInvalidFromType(ArgumentsHolder holder) {
        // GIVEN
        Object from = holder.from;
        Converter conv = holder.converter;
        // WHEN / THEN
        assertThrows((Class<? extends Throwable>) holder.expected, () -> TimeZoneConversions.toString(from, conv));
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("faultyTimeZoneProvider")
    void toString_propagatesException_fromFaultyTimeZone(ArgumentsHolder holder) {
        // GIVEN
        Object from = holder.from;
        Converter conv = holder.converter;
        // WHEN / THEN
        assertThrows((Class<? extends Throwable>) holder.expected, () -> TimeZoneConversions.toString(from, conv));
    }
}
