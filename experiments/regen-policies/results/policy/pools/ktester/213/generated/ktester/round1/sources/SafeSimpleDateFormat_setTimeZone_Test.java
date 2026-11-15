package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.TimeZone;
import java.text.SimpleDateFormat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.Instant;
import java.util.Date;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SafeSimpleDateFormat_setTimeZone_Test {

    /**
     * The format string used for constructing the test instance.
     */
    private static final String TEST_FORMAT = "yyyy-MM-dd";

    /**
     * The instance under test.
     */
    private SafeSimpleDateFormat safeSdf;

    /**
     * Mock of the underlying SimpleDateFormat returned by the static factory.
     */
    private SimpleDateFormat mockUnderlyingSdf;

    /**
     * Mocked static context for {@link SafeSimpleDateFormat#getDateFormat(String)}.
     */
    private MockedStatic<SafeSimpleDateFormat> mockedStatic;

    @BeforeEach
    void setupBeforeEach() {
        // Create a mock SimpleDateFormat that will be returned by the static method.
        mockUnderlyingSdf = mock(SimpleDateFormat.class);
        // Mock the static factory method to return our mock instead of a real instance.
        mockedStatic = Mockito.mockStatic(SafeSimpleDateFormat.class);
        mockedStatic.when(() -> SafeSimpleDateFormat.getDateFormat(TEST_FORMAT)).thenReturn(mockUnderlyingSdf);
        // Now construct the SafeSimpleDateFormat – the constructor will invoke the
        // mocked static method, receiving the mock we prepared.
        safeSdf = new SafeSimpleDateFormat(TEST_FORMAT);
    }

    @AfterEach
    void teardownAfterEach() {
        // Close the static mock to avoid leaking it into other tests.
        if (mockedStatic != null) {
            mockedStatic.close();
        }
    }

    /**
     * Provider for the “Valid TimeZone handling” group (first three cases).
     */
    private static Stream<Arguments> validTimeZoneProvider() {
        return Stream.of(Arguments.of("UTC", "2023-01-01 00:00:00"), Arguments.of("America/Los_Angeles", "2022-12-31 16:00:00"), Arguments.of("Asia/Tokyo", "2023-01-01 09:00:00"));
    }

    /**
     * Provider for DST transition handling.
     */
    private static Stream<Arguments> dstProvider() {
        return Stream.of(Arguments.of("Europe/Paris", "2023-06-01T12:00:00Z", "2023-06-01 14:00:00"), Arguments.of("Europe/Paris", "2023-01-01T12:00:00Z", "2023-01-01 13:00:00"));
    }

    @Test
    void setTimeZone_delegatesToUnderlyingSimpleDateFormat() {
        // GIVEN
        TimeZone tz = TimeZone.getTimeZone("UTC");
        // WHEN
        safeSdf.setTimeZone(tz);
        // THEN
        // Verify that the static factory was called with the correct format string.
        mockedStatic.verify(() -> SafeSimpleDateFormat.getDateFormat(TEST_FORMAT), times(1));
        // Verify that the underlying SimpleDateFormat received the exact TimeZone instance.
        verify(mockUnderlyingSdf, times(1)).setTimeZone(tz);
    }

    @Test
    void setTimeZone_withNullTimeZone_throwsNullPointerException() {
        // GIVEN
        TimeZone nullTz = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> safeSdf.setTimeZone(nullTz));
        // Ensure that the underlying SimpleDateFormat was still invoked (the NPE originates
        // from the SimpleDateFormat implementation).
        verify(mockUnderlyingSdf, times(1)).setTimeZone(nullTz);
    }

    @ParameterizedTest(name = "[{index}] tz={0} → expected={1}")
    @MethodSource("validTimeZoneProvider")
    void setTimeZone_formatsAccordingToTimeZone(String tzId, String expected) {
        // Close the static mock so that the real SimpleDateFormat is used.
        mockedStatic.close();
        // Use a pattern that includes date and time (no zone token).
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // Apply the time‑zone under test.
        TimeZone tz = TimeZone.getTimeZone(tzId);
        sdf.setTimeZone(tz);
        // The instant to format: 2023‑01‑01T00:00:00Z.
        Date instant = Date.from(Instant.parse("2023-01-01T00:00:00Z"));
        // Verify formatted output.
        assertEquals(expected, sdf.format(instant));
    }

    @Test
    void setTimeZone_calledTwiceWithSameZone_producesSameResult() {
        mockedStatic.close();
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        TimeZone tz = TimeZone.getTimeZone("UTC");
        // First call.
        sdf.setTimeZone(tz);
        // Second call with the same instance.
        sdf.setTimeZone(tz);
        Date instant = Date.from(Instant.parse("2023-01-01T00:00:00Z"));
        assertEquals("2023-01-01 00:00:00", sdf.format(instant));
    }

    @Test
    void setTimeZone_differentInstancesSameId_behaveIdentically() {
        mockedStatic.close();
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // Two distinct TimeZone objects that both represent UTC.
        TimeZone tzFirst = TimeZone.getTimeZone("UTC");
        TimeZone tzSecond = TimeZone.getTimeZone("UTC");
        sdf.setTimeZone(tzFirst);
        // replace with second instance
        sdf.setTimeZone(tzSecond);
        Date instant = Date.from(Instant.parse("2023-01-01T00:00:00Z"));
        assertEquals("2023-01-01 00:00:00", sdf.format(instant));
    }

    @ParameterizedTest(name = "[{index}] tz={0}, instant={1} → expected={2}")
    @MethodSource("dstProvider")
    void setTimeZone_handlesDstTransitions(String tzId, String instantIso, String expected) {
        mockedStatic.close();
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone(tzId));
        Date instant = Date.from(Instant.parse(instantIso));
        assertEquals(expected, sdf.format(instant));
    }

    @Test
    void setTimeZone_handlesHalfHourOffset() {
        mockedStatic.close();
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+05:30"));
        Date instant = Date.from(Instant.parse("2023-06-01T12:00:00Z"));
        assertEquals("2023-06-01 17:30:00", sdf.format(instant));
    }

    @Test
    void setTimeZone_sequentialDifferentZones_lastOneWins() {
        mockedStatic.close();
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        // final zone
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
        Date instant = Date.from(Instant.parse("2023-01-01T00:00:00Z"));
        assertEquals("2023-01-01 09:00:00", sdf.format(instant));
    }

    @Test
    void setTimeZone_patternWithZoneOffsetToken_includesOffset() {
        mockedStatic.close();
        // Pattern contains the zone offset token 'Z'.
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
        sdf.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        Date instant = Date.from(Instant.parse("2023-07-15T16:00:00Z"));
        // In July New York is on EDT (UTC‑4).
        assertEquals("2023-07-15 12:00:00 -0400", sdf.format(instant));
    }

    @Test
    void setTimeZone_invalidPattern_propagatesIllegalArgumentException() {
        mockedStatic.close();
        // Malformed pattern (unclosed quote) – SimpleDateFormat will throw IAE when used.
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy-MM-dd['");
        // The exception is thrown when the underlying SimpleDateFormat is retrieved
        // inside setTimeZone.
        assertThrows(IllegalArgumentException.class, () -> sdf.setTimeZone(TimeZone.getTimeZone("UTC")));
    }

    @Test
    void setTimeZone_nullPattern_propagatesNullPointerException() {
        mockedStatic.close();
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(null);
        // getDateFormat(null) will cause a NPE inside the static method.
        assertThrows(NullPointerException.class, () -> sdf.setTimeZone(TimeZone.getTimeZone("UTC")));
    }
}
