package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import java.util.TimeZone;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.SimpleTimeZone;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TimeZoneConversions_toString_Test {

    /**
     * A real {@link Converter} instance – the method under test does not use it, but it must be non‑null.
     */
    private static Converter realConverter;

    /**
     * A mock {@link Converter} to verify that the method does not interact with it.
     */
    @Mock
    private Converter mockConverter;

    /**
     * A custom {@link TimeZone} that throws an {@link IllegalStateException}
     * from {@link #getID()}.  All other abstract methods are given trivial
     * implementations because they are not exercised by the method under test.
     */
    private static final class ThrowingTimeZone extends TimeZone {

        @Override
        public String getID() {
            throw new IllegalStateException("Intentional exception from getID()");
        }

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
        public int getDSTSavings() {
            return 0;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter requires a ConverterOptions instance; using the default constructor is sufficient.
        realConverter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Verify that the mock converter was not used during the test.
        verifyNoInteractions(mockConverter);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Clean up static resources if any (none needed here).
        realConverter = null;
    }

    /**
     * Provides a stream of arguments for the basic successful conversions.
     * The {@code converter} argument is deliberately {@code null} to reflect the JSON test data.
     */
    private static Stream<Arguments> basicStringCases() {
        return Stream.of(Arguments.of(TimeZone.getTimeZone("UTC"), "UTC"), Arguments.of(TimeZone.getTimeZone("America/New_York"), "America/New_York"), Arguments.of(new SimpleTimeZone(0, "Custom/Zone"), "Custom/Zone"));
    }

    @Test
    void toString_returnsTimeZoneId_forStandardZone() {
        // given
        TimeZone tz = TimeZone.getTimeZone("UTC");
        // when
        String result = TimeZoneConversions.toString(tz, realConverter);
        // then
        assertEquals("UTC", result);
    }

    @Test
    void toString_returnsTimeZoneId_forRegionalZone() {
        // given
        TimeZone tz = TimeZone.getTimeZone("America/New_York");
        // when
        String result = TimeZoneConversions.toString(tz, realConverter);
        // then
        assertEquals("America/New_York", result);
    }

    @Test
    void toString_throwsNullPointerException_whenSourceIsNull() {
        // given
        Object nullSource = null;
        // when / then
        assertThrows(NullPointerException.class, () -> TimeZoneConversions.toString(nullSource, realConverter));
    }

    @Test
    void toString_doesNotInteractWithConverter() {
        // given
        TimeZone tz = TimeZone.getTimeZone("GMT");
        // when
        String result = TimeZoneConversions.toString(tz, mockConverter);
        // then
        assertEquals("GMT", result);
        // Interaction verification is performed in @AfterEach via verifyNoInteractions(mockConverter);
    }

    @ParameterizedTest(name = "toString returns \"{1}\" for {0}")
    @MethodSource("basicStringCases")
    void toString_basicFunctionality(Object from, String expectedId) {
        // The method does not use the converter, so we can safely pass null.
        String result = TimeZoneConversions.toString(from, null);
        assertEquals(expectedId, result);
    }

    @Test
    void toString_throwsClassCastException_whenSourceIsNotTimeZone() {
        // given a plain String instead of a TimeZone
        Object notATimeZone = "not a TimeZone";
        // when / then
        assertThrows(ClassCastException.class, () -> TimeZoneConversions.toString(notATimeZone, null));
    }

    @Test
    void toString_returnsGMT_whenZoneIdIsUnknown() {
        // TimeZone.getTimeZone falls back to GMT for unknown IDs
        TimeZone unknown = TimeZone.getTimeZone("Invalid/Zone");
        String result = TimeZoneConversions.toString(unknown, null);
        assertEquals("GMT", result);
    }

    @Test
    void toString_propagatesExceptionFromCustomTimeZone() {
        // given a TimeZone whose getID() throws IllegalStateException
        TimeZone throwingTZ = new ThrowingTimeZone();
        // when / then
        assertThrows(IllegalStateException.class, () -> TimeZoneConversions.toString(throwingTZ, null));
    }
}
