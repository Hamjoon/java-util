package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import java.text.SimpleDateFormat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SafeSimpleDateFormat_set2DigitYearStart_Test {

    private static Stream<Date> validDateProvider() {
        return Stream.of(// normal date
        Date.from(Instant.parse("2000-01-01T00:00:00Z")), // future beyond 2099
        Date.from(Instant.parse("2100-01-01T00:00:00Z")), // epoch start
        Date.from(Instant.parse("1970-01-01T00:00:00Z")), // leap day
        Date.from(Instant.parse("2004-02-29T00:00:00Z")));
    }

    @Test
    void set2DigitYearStart_delegatesToUnderlyingSimpleDateFormat() {
        // GIVEN – a mock SimpleDateFormat that will be returned by the static factory method
        SimpleDateFormat mockSdf = mock(SimpleDateFormat.class);
        try (MockedStatic<SafeSimpleDateFormat> mockedStatic = mockStatic(SafeSimpleDateFormat.class)) {
            // Stub the static getDateFormat(String) method to return our mock
            mockedStatic.when(() -> SafeSimpleDateFormat.getDateFormat(anyString())).thenReturn(mockSdf);
            // AND – an instance of the class under test
            SafeSimpleDateFormat safeSdf = new SafeSimpleDateFormat("yyyy-MM-dd");
            // AND – a specific date to use as the 2‑digit year start
            Date startDate = new Date();
            // WHEN – invoking the method under test
            safeSdf.set2DigitYearStart(startDate);
            // THEN – the underlying SimpleDateFormat should receive the same call
            verify(mockSdf, times(1)).set2DigitYearStart(startDate);
        }
    }

    @ParameterizedTest(name = "set2DigitYearStart should delegate for date = {0}")
    @MethodSource("validDateProvider")
    void set2DigitYearStart_delegatesForVariousValidDates(Date date) {
        SimpleDateFormat mockSdf = mock(SimpleDateFormat.class);
        try (MockedStatic<SafeSimpleDateFormat> mockedStatic = mockStatic(SafeSimpleDateFormat.class)) {
            mockedStatic.when(() -> SafeSimpleDateFormat.getDateFormat(anyString())).thenReturn(mockSdf);
            SafeSimpleDateFormat safeSdf = new SafeSimpleDateFormat("yyyy-MM-dd");
            safeSdf.set2DigitYearStart(date);
            verify(mockSdf, times(1)).set2DigitYearStart(date);
        }
    }

    @Test
    void set2DigitYearStart_nullDate_throwsIllegalArgumentException() {
        SimpleDateFormat mockSdf = mock(SimpleDateFormat.class);
        // Configure the mock to throw the same exception SimpleDateFormat would throw
        doThrow(new IllegalArgumentException("date is null")).when(mockSdf).set2DigitYearStart(null);
        try (MockedStatic<SafeSimpleDateFormat> mockedStatic = mockStatic(SafeSimpleDateFormat.class)) {
            mockedStatic.when(() -> SafeSimpleDateFormat.getDateFormat(anyString())).thenReturn(mockSdf);
            SafeSimpleDateFormat safeSdf = new SafeSimpleDateFormat("yyyy-MM-dd");
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> safeSdf.set2DigitYearStart(null));
            assertEquals("date is null", ex.getMessage());
        }
    }

    @Test
    void set2DigitYearStart_afterFormatInitialization_parsesTwoDigitYearAccordingToNewStart() throws ParseException {
        // Use a real format that parses two‑digit years
        SafeSimpleDateFormat safeSdf = new SafeSimpleDateFormat("yy");
        // Trigger the internal SimpleDateFormat creation by formatting a date
        safeSdf.format(new Date(), new StringBuffer(), new java.text.FieldPosition(0));
        // Set the 2‑digit year start to 1995‑06‑15
        Date startDate = Date.from(Instant.parse("1995-06-15T00:00:00Z"));
        safeSdf.set2DigitYearStart(startDate);
        // Parse a two‑digit year string "99" – should be interpreted as 1999 (within 1995‑2094 range)
        Date parsed = safeSdf.parse("99");
        Calendar cal = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault());
        cal.setTime(parsed);
        int year = cal.get(Calendar.YEAR);
        assertEquals(1999, year, "Parsed year should be 1999 based on the 1995 start date");
    }

    @Test
    void set2DigitYearStart_concurrentCalls_doNotCorruptInternalState() throws InterruptedException {
        final int THREAD_COUNT = 20;
        final Date sharedDate = Date.from(Instant.parse("2001-01-01T00:00:00Z"));
        SimpleDateFormat mockSdf = mock(SimpleDateFormat.class);
        try (MockedStatic<SafeSimpleDateFormat> mockedStatic = mockStatic(SafeSimpleDateFormat.class)) {
            mockedStatic.when(() -> SafeSimpleDateFormat.getDateFormat(anyString())).thenReturn(mockSdf);
            SafeSimpleDateFormat safeSdf = new SafeSimpleDateFormat("yyyy-MM-dd");
            ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
            for (int i = 0; i < THREAD_COUNT; i++) {
                executor.submit(() -> {
                    try {
                        // ensure all threads start together
                        startLatch.await();
                        safeSdf.set2DigitYearStart(sharedDate);
                    } catch (InterruptedException ignored) {
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }
            // Release all threads
            startLatch.countDown();
            // Wait for completion
            assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "All threads should finish within timeout");
            executor.shutdownNow();
            // Verify that the underlying SimpleDateFormat received the call exactly THREAD_COUNT times
            verify(mockSdf, times(THREAD_COUNT)).set2DigitYearStart(sharedDate);
        }
    }

    @Test
    void set2DigitYearStart_nullInternalFormat_throwsNullPointerException() {
        // Construct with a null pattern – internal call to getDateFormat(null) should NPE
        SafeSimpleDateFormat safeSdf = new SafeSimpleDateFormat(null);
        Date someDate = Date.from(Instant.parse("2000-01-01T00:00:00Z"));
        // We are not mocking the static method here to let the real implementation throw NPE
        assertThrows(NullPointerException.class, () -> safeSdf.set2DigitYearStart(someDate));
    }
}
