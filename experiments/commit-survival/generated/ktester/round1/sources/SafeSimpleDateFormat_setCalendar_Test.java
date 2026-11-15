package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.TimeZone;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SafeSimpleDateFormat_setCalendar_Test {

    private static MockedStatic<SafeSimpleDateFormat> mockedStatic;

    private SafeSimpleDateFormat safeSimpleDateFormat;

    private SimpleDateFormat mockSimpleDateFormat;

    private Calendar testCalendar;

    @BeforeEach
    void setupBeforeEach() {
        // Mock the static factory method getDateFormat(String)
        mockedStatic = mockStatic(SafeSimpleDateFormat.class);
        mockSimpleDateFormat = mock(SimpleDateFormat.class);
        // Whenever getDateFormat is called with any format string, return the mock SimpleDateFormat
        mockedStatic.when(() -> SafeSimpleDateFormat.getDateFormat(anyString())).thenReturn(mockSimpleDateFormat);
        // Create the instance under test – the constructor will call the mocked static method
        safeSimpleDateFormat = new SafeSimpleDateFormat("yyyy/MM/dd");
        // Prepare a Calendar instance to be used in the tests
        testCalendar = Calendar.getInstance();
        // mimic the constructor's behaviour
        testCalendar.clear();
    }

    @AfterEach
    void teardownAfterEach() {
        // Release the static mock to avoid interference with other tests
        mockedStatic.close();
    }

    private static Stream<Arguments> validCalendarsProvider() {
        return Stream.of(Arguments.of(Calendar.getInstance(TimeZone.getTimeZone("UTC"))), Arguments.of(Calendar.getInstance(TimeZone.getTimeZone("America/New_York"))));
    }

    private static Stream<Arguments> differentCalendarImplementationsProvider() {
        return Stream.of(Arguments.of(new java.util.GregorianCalendar(TimeZone.getTimeZone("Asia/Tokyo"))), // JapaneseImperialCalendar exists in the JDK (subclass of Calendar)
        Arguments.of(new java.util.JapaneseImperialCalendar(TimeZone.getTimeZone("Asia/Tokyo"))));
    }

    @Test
    void setCalendar_delegatesToUnderlyingSimpleDateFormat() {
        // When
        safeSimpleDateFormat.setCalendar(testCalendar);
        // Then
        verify(mockSimpleDateFormat, times(1)).setCalendar(testCalendar);
    }

    @Test
    void setCalendar_withNullCalendar_delegatesCorrectly() {
        // When
        safeSimpleDateFormat.setCalendar(null);
        // Then
        verify(mockSimpleDateFormat, times(1)).setCalendar(null);
    }

    @ParameterizedTest(name = "[{index}] Calendar timezone = {0}")
    @MethodSource("validCalendarsProvider")
    void setCalendar_validCalendars_delegatesIdentity(Calendar calendar) {
        // When
        safeSimpleDateFormat.setCalendar(calendar);
        // Then – the exact same instance must be passed to the mock.
        verify(mockSimpleDateFormat, times(1)).setCalendar(calendar);
    }

    @Test
    void setCalendar_nullCalendar_throwsNullPointerException() {
        // Configure the mock to throw NPE when null is supplied (real SimpleDateFormat does this).
        doThrow(new NullPointerException()).when(mockSimpleDateFormat).setCalendar(null);
        // When / Then
        assertThrows(NullPointerException.class, () -> safeSimpleDateFormat.setCalendar(null), "Expected NullPointerException when passing null Calendar");
    }

    @ParameterizedTest(name = "[{index}] Calendar class = {0}")
    @MethodSource("differentCalendarImplementationsProvider")
    void setCalendar_differentImplementations_delegatesIdentity(Calendar calendar) {
        // When
        safeSimpleDateFormat.setCalendar(calendar);
        // Then
        verify(mockSimpleDateFormat, times(1)).setCalendar(calendar);
    }

    @Test
    void setCalendar_multipleInvocations_lastOneWins() {
        Calendar first = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        Calendar second = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"));
        // First call
        safeSimpleDateFormat.setCalendar(first);
        // Second call – should overwrite the first
        safeSimpleDateFormat.setCalendar(second);
        // Verify that the mock received both calls in order and the last call used 'second'
        InOrder inOrder = inOrder(mockSimpleDateFormat);
        inOrder.verify(mockSimpleDateFormat).setCalendar(first);
        inOrder.verify(mockSimpleDateFormat).setCalendar(second);
    }

    /**
     * Verify that the calendar set on the {@code SafeSimpleDateFormat}
     * influences the formatted output.  This test uses the real
     * {@code SimpleDateFormat} implementation (no static mocking).
     */
    @Test
    void setCalendar_effectOnFormatting_reflectsTimezone() {
        // Release the static mock so that the real cache is used.
        mockedStatic.close();
        // Create a real instance (the constructor will call the real static method)
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
        // Set a New‑York calendar (UTC‑5 in winter)
        Calendar nyCal = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
        sdf.setCalendar(nyCal);
        // Format the epoch (1970‑01‑01T00:00:00Z)
        Date epoch = new Date(0L);
        String result = sdf.format(epoch);
        // Expected string: 1969‑12‑31 19:00:00 -0500
        assertEquals("1969-12-31 19:00:00 -0500", result, "Formatting with a New‑York calendar should apply the -0500 offset");
    }

    /**
     * Verify that two {@code SafeSimpleDateFormat} objects created with the same
     * pattern share the same underlying {@code SimpleDateFormat} (static cache).
     * After setting the calendar on the first instance, the second instance
     * should see the same calendar instance.
     */
    @Test
    void setCalendar_sharedUnderlyingSimpleDateFormat_instancesShareCalendar() throws Exception {
        // Release the static mock – we need the real cache for this test.
        mockedStatic.close();
        // Two instances with the same pattern.
        SafeSimpleDateFormat sdfA = new SafeSimpleDateFormat("yyyy-MM-dd");
        SafeSimpleDateFormat sdfB = new SafeSimpleDateFormat("yyyy-MM-dd");
        Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        sdfA.setCalendar(utcCal);
        // Retrieve the underlying SimpleDateFormat from the static cache.
        SimpleDateFormat underlyingA = SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd");
        SimpleDateFormat underlyingB = SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd");
        // Both references must point to the same object (cache guarantee).
        assertSame(underlyingA, underlyingB, "Both SafeSimpleDateFormat instances should share the same SimpleDateFormat");
        // The calendar stored inside the shared SimpleDateFormat must be the one we set.
        Calendar calendarFromUnderlying = underlyingA.getCalendar();
        assertSame(utcCal, calendarFromUnderlying, "The calendar retrieved from the shared SimpleDateFormat must be the exact instance set on the first SafeSimpleDateFormat");
    }

    /**
     * Helper to access the private field {@code _format} via reflection.
     * Not used in the current test suite but kept for possible future extensions.
     */
    @SuppressWarnings("unused")
    private static String getFormatViaReflection(SafeSimpleDateFormat instance) throws Exception {
        Field f = SafeSimpleDateFormat.class.getDeclaredField("_format");
        f.setAccessible(true);
        return (String) f.get(instance);
    }
}
