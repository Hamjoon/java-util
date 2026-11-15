package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.ZoneId;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.SimpleTimeZone;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TimeZoneConversions_toZoneId_Test {

    @Mock
    private Converter // Converter is not used by the method but required by signature
    mockConverter;

    /**
     * A dummy {@link Converter} implementation – the method under test ignores it.
     */
    private static class DummyConverter implements Converter {
        // empty – only needed to satisfy the method signature
    }

    /**
     * A {@link TimeZone} subclass whose {@code toZoneId()} deliberately throws.
     */
    private static class ThrowingTimeZone extends TimeZone {

        @Override
        public ZoneId toZoneId() {
            throw new UnsupportedOperationException("forced exception");
        }

        @Override
        public String getID() {
            return "Throwing";
        }

        @Override
        public int getRawOffset() {
            return 0;
        }

        @Override
        public void setRawOffset(int offsetMillis) {
        }

        @Override
        public boolean useDaylightTime() {
            return false;
        }

        @Override
        public boolean inDaylightTime(Date date) {
            return false;
        }

        @Override
        public int getDSTSavings() {
            return 0;
        }

        @Override
        public int getOffset(long date) {
            return 0;
        }

        @Override
        public int getOffset(int era, int year, int month, int day, int dayOfWeek, int milliseconds) {
            return 0;
        }
    }

    /**
     * A {@link TimeZone} subclass that returns {@code null} from {@code getID()}.
     */
    private static class NullIdTimeZone extends TimeZone {

        @Override
        public String getID() {
            return null;
        }

        @Override
        public int getRawOffset() {
            return 0;
        }

        @Override
        public void setRawOffset(int offsetMillis) {
        }

        @Override
        public boolean useDaylightTime() {
            return false;
        }

        @Override
        public boolean inDaylightTime(Date date) {
            return false;
        }

        @Override
        public int getDSTSavings() {
            return 0;
        }

        @Override
        public int getOffset(long date) {
            return 0;
        }

        @Override
        public int getOffset(int era, int year, int month, int day, int dayOfWeek, int milliseconds) {
            return 0;
        }
    }

    private static Stream<Arguments> normalConversionsProvider() {
        return Stream.of(Arguments.of("Convert a UTC TimeZone to its ZoneId (should be ZoneOffset.UTC)", TimeZone.getTimeZone("UTC"), ZoneOffset.UTC), Arguments.of("Convert a region‑specific TimeZone to its ZoneId (America/New_York)", TimeZone.getTimeZone("America/New_York"), ZoneId.of("America/New_York")));
    }

    private static Stream<Arguments> offsetBasedConversionsProvider() {
        return Stream.of(Arguments.of("Convert GMT+02:00 TimeZone to ZoneId", TimeZone.getTimeZone("GMT+02:00"), "GMT+02:00"), Arguments.of("Convert GMT-05:00 TimeZone to ZoneId", TimeZone.getTimeZone("GMT-05:00"), "GMT-05:00"));
    }

    private static Stream<Arguments> subclassHandlingProvider() {
        return Stream.of(Arguments.of("SimpleTimeZone subclass should produce its custom ID", new SimpleTimeZone(0, "Custom"), "Custom"));
    }

    private static Stream<Arguments> errorHandlingProvider() {
        return Stream.of(Arguments.of("Null input should result in NullPointerException", null, NullPointerException.class), Arguments.of("Non‑TimeZone input should result in ClassCastException", "not a timezone", ClassCastException.class));
    }

    @Test
    void toZoneId_WithValidTimeZone_ReturnsCorrespondingZoneId() {
        // given
        TimeZone timeZone = TimeZone.getTimeZone("America/New_York");
        ZoneId expected = timeZone.toZoneId();
        // when
        ZoneId actual = TimeZoneConversions.toZoneId(timeZone, mockConverter);
        // then
        assertEquals(expected, actual, "The ZoneId returned should match the TimeZone's ZoneId");
    }

    @Test
    void toZoneId_WithNullInput_ThrowsNullPointerException() {
        // given
        Object nullInput = null;
        // when / then
        assertThrows(NullPointerException.class, () -> TimeZoneConversions.toZoneId(nullInput, mockConverter), "Calling toZoneId with null should throw NullPointerException");
    }

    @Test
    void toZoneId_WithInvalidType_ThrowsClassCastException() {
        // given
        Object notATimeZone = "not-a-timezone";
        // when / then
        assertThrows(ClassCastException.class, () -> TimeZoneConversions.toZoneId(notATimeZone, mockConverter), "Calling toZoneId with a non‑TimeZone object should throw ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("normalConversionsProvider")
    void toZoneId_NormalConversions(String description, TimeZone from, ZoneId expected) {
        // when
        ZoneId actual = TimeZoneConversions.toZoneId(from, mockConverter);
        // then
        assertEquals(expected, actual, () -> "Expected ZoneId '" + expected + "' but got '" + actual + "'");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("offsetBasedConversionsProvider")
    void toZoneId_OffsetBasedConversions(String description, TimeZone from, String expectedId) {
        ZoneId actual = TimeZoneConversions.toZoneId(from, mockConverter);
        assertEquals(expectedId, actual.getId(), () -> "Expected ZoneId ID '" + expectedId + "' but got '" + actual.getId() + "'");
    }

    @Test
    void toZoneId_CustomConverterIgnored() {
        // given a non‑null converter that should be ignored
        Converter dummy = new DummyConverter();
        TimeZone tz = TimeZone.getTimeZone("Europe/Paris");
        ZoneId expected = ZoneId.of("Europe/Paris");
        // when
        ZoneId actual = TimeZoneConversions.toZoneId(tz, dummy);
        // then
        assertEquals(expected, actual, "Method should ignore the provided Converter and return correct ZoneId");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("subclassHandlingProvider")
    void toZoneId_SubclassHandling(String description, TimeZone from, String expectedId) {
        ZoneId actual = TimeZoneConversions.toZoneId(from, mockConverter);
        assertEquals(expectedId, actual.getId(), () -> "Expected ZoneId ID '" + expectedId + "' but got '" + actual.getId() + "'");
    }

    @Test
    void toZoneId_InvalidIdFallback() {
        // TimeZone.getTimeZone returns GMT when the ID is unknown
        TimeZone tz = TimeZone.getTimeZone("nonexistent/Zone");
        ZoneId expected = ZoneId.of("GMT");
        ZoneId actual = TimeZoneConversions.toZoneId(tz, mockConverter);
        assertEquals(expected, actual, "When an unknown ID is supplied, TimeZone defaults to GMT; method should return GMT ZoneId");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("errorHandlingProvider")
    void toZoneId_ErrorHandling(String description, Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> TimeZoneConversions.toZoneId(from, mockConverter), () -> "Expected exception " + expectedException.getSimpleName());
    }

    @Test
    void toZoneId_CustomTimeZoneToZoneIdException() {
        TimeZone tz = new ThrowingTimeZone();
        assertThrows(UnsupportedOperationException.class, () -> TimeZoneConversions.toZoneId(tz, mockConverter), "Method should propagate UnsupportedOperationException from TimeZone.toZoneId()");
    }

    @Test
    void toZoneId_CustomTimeZoneWithNullId() {
        TimeZone tz = new NullIdTimeZone();
        assertThrows(NullPointerException.class, () -> TimeZoneConversions.toZoneId(tz, mockConverter), "Calling toZoneId on a TimeZone with null ID should throw NullPointerException");
    }
}
