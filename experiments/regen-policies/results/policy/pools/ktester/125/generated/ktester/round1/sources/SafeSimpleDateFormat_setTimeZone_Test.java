package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SafeSimpleDateFormat_setTimeZone_Test {

    // ---------- Group: Valid TimeZone ----------
    static Stream<Arguments> validTimeZoneProvider() {
        return Stream.of(Arguments.of(TimeZone.getTimeZone("UTC"), "UTC"), Arguments.of(TimeZone.getTimeZone("America/New_York"), "America/New_York"));
    }

    /**
     * Verifies that calling {@code setTimeZone} changes the formatting
     * output to reflect the supplied {@link TimeZone}.
     */
    @Test
    void setTimeZone_changesUnderlyingFormatTimezone() {
        // Arrange – a format that includes date and hour/minute.
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy-MM-dd HH:mm");
        // 1970‑01‑01T00:00:00Z
        Date epoch = new Date(0L);
        // Act & Assert – UTC
        TimeZone utc = TimeZone.getTimeZone("UTC");
        sdf.setTimeZone(utc);
        String utcResult = sdf.format(epoch);
        assertEquals("1970-01-01 00:00", utcResult, "Formatting in UTC should produce the epoch representation.");
        // Act & Assert – America/Los_Angeles (UTC‑8, no DST at epoch)
        TimeZone la = TimeZone.getTimeZone("America/Los_Angeles");
        sdf.setTimeZone(la);
        String laResult = sdf.format(epoch);
        assertEquals("1969-12-31 16:00", laResult, "Formatting in Los Angeles time should reflect the 8‑hour offset.");
    }

    @ParameterizedTest(name = "Valid zone ''{1}'' should be set on the underlying SimpleDateFormat")
    @MethodSource("validTimeZoneProvider")
    void validTimeZone_setsUnderlyingFormatter(TimeZone tz, String expectedId) {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy-MM-dd HH:mm");
        sdf.setTimeZone(tz);
        // Retrieve the cached SimpleDateFormat via the public static accessor.
        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd HH:mm");
        assertEquals(expectedId, underlying.getTimeZone().getID(), "Underlying SimpleDateFormat's time zone should be " + expectedId);
    }

    // ---------- Group: Null TimeZone ----------
    @Test
    void nullTimeZone_throwsNullPointerException() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy-MM-dd");
        assertThrows(NullPointerException.class, () -> sdf.setTimeZone(null), "Passing a null TimeZone should result in NullPointerException");
    }

    // ---------- Group: Cache / Re‑use Behavior ----------
    @Test
    void cacheReuse_sameFormatterInstanceIsUpdated() {
        String pattern = "yyyy-MM-dd";
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(pattern);
        SimpleDateFormat first = SafeSimpleDateFormat.getDateFormat(pattern);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        assertEquals("UTC", first.getTimeZone().getID());
        // Second call with a different zone – should affect the *same* instance.
        sdf.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
        SimpleDateFormat second = SafeSimpleDateFormat.getDateFormat(pattern);
        assertSame(first, second, "Cache should return the same SimpleDateFormat instance");
        assertEquals("America/Los_Angeles", second.getTimeZone().getID(), "The formatter's time zone must reflect the last set value");
    }

    // ---------- Group: Different Pattern Cache Isolation ----------
    @Test
    void differentPatternCache_isolatedInstances() {
        String pattern1 = "yyyy-MM-dd";
        String pattern2 = "dd/MM/yyyy";
        SafeSimpleDateFormat sdf1 = new SafeSimpleDateFormat(pattern1);
        SafeSimpleDateFormat sdf2 = new SafeSimpleDateFormat(pattern2);
        SimpleDateFormat fmt1 = SafeSimpleDateFormat.getDateFormat(pattern1);
        SimpleDateFormat fmt2 = SafeSimpleDateFormat.getDateFormat(pattern2);
        // Set UTC only on the first formatter.
        sdf1.setTimeZone(TimeZone.getTimeZone("UTC"));
        assertEquals("UTC", fmt1.getTimeZone().getID());
        // The second formatter should retain the JVM default zone (or whatever it started with).
        String defaultId = TimeZone.getDefault().getID();
        assertEquals(defaultId, fmt2.getTimeZone().getID(), "Formatter for a different pattern must not be affected by the first one's setTimeZone");
    }

    // ---------- Group: Custom Offset TimeZone ----------
    @Test
    void customOffsetTimeZone_isPropagatedCorrectly() {
        TimeZone custom = TimeZone.getTimeZone("GMT+05:30");
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy-MM-dd HH:mm");
        sdf.setTimeZone(custom);
        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd HH:mm");
        assertEquals("GMT+05:30", underlying.getTimeZone().getID(), "Custom offset ID should be preserved");
        // Verify formatting reflects the +5:30 offset.
        Date epoch = new Date(0L);
        String formatted = sdf.format(epoch);
        assertEquals("1970-01-01 05:30", formatted, "Formatting should reflect the custom offset");
    }

    // ---------- Group: Unrecognized TimeZone ID ----------
    @Test
    void unknownTimeZoneId_fallsBackToGMT() {
        // falls back to GMT
        TimeZone unknown = TimeZone.getTimeZone("Invalid/Zone");
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy-MM-dd HH:mm");
        sdf.setTimeZone(unknown);
        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd HH:mm");
        assertEquals("GMT", underlying.getTimeZone().getID(), "Unknown IDs should resolve to GMT");
        // Formatting epoch should be identical to UTC.
        Date epoch = new Date(0L);
        assertEquals("1970-01-01 00:00", sdf.format(epoch));
    }

    // ---------- Group: Repeated Same TimeZone ----------
    @Test
    void repeatedSameTimeZone_noExceptionAndNoChange() {
        TimeZone la = TimeZone.getTimeZone("America/Los_Angeles");
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy-MM-dd HH:mm");
        sdf.setTimeZone(la);
        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd HH:mm");
        String firstId = underlying.getTimeZone().getID();
        // Second call with the same instance – should be a no‑op but safe.
        assertDoesNotThrow(() -> sdf.setTimeZone(la));
        assertEquals(firstId, underlying.getTimeZone().getID(), "Time zone should remain unchanged after setting the same value again");
    }

    // ---------- Group: Concurrent SetTimeZone ----------
    @Test
    void concurrentSetTimeZone_isThreadSafe() throws InterruptedException, ExecutionException {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy-MM-dd HH:mm");
        List<TimeZone> zones = List.of(TimeZone.getTimeZone("UTC"), TimeZone.getTimeZone("Europe/Paris"), TimeZone.getTimeZone("Asia/Tokyo"));
        ExecutorService exec = Executors.newFixedThreadPool(zones.size());
        List<Callable<Void>> tasks = new ArrayList<>();
        for (TimeZone tz : zones) {
            tasks.add(() -> {
                sdf.setTimeZone(tz);
                // small sleep to increase interleaving chance
                Thread.sleep(10);
                return null;
            });
        }
        // Run all tasks and wait for completion.
        List<Future<Void>> futures = exec.invokeAll(tasks);
        for (Future<Void> f : futures) {
            // propagate any exception
            f.get();
        }
        exec.shutdownNow();
        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd HH:mm");
        String finalId = underlying.getTimeZone().getID();
        // The final ID must be one of the zones we set; no corruption should have occurred.
        assertTrue(zones.stream().anyMatch(tz -> tz.getID().equals(finalId)), "Final time zone should be one of the zones set concurrently");
    }

    // ---------- Group: Invalid Pattern ----------
    @Test
    void invalidPattern_throwsIllegalArgumentExceptionOnSetTimeZone() {
        // The pattern is syntactically broken; SimpleDateFormat will throw IAE when used.
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy-MM-dd['\"");
        // setTimeZone triggers getDateFormat which will attempt to create the formatter.
        assertThrows(IllegalArgumentException.class, () -> sdf.setTimeZone(TimeZone.getTimeZone("UTC")), "Invalid pattern should cause IllegalArgumentException when formatter is created");
    }

    // ---------- Group: Null Pattern ----------
    @Test
    void nullPattern_throwsNullPointerExceptionOnSetTimeZone() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(null);
        // The call to getDateFormat(null) will eventually cause NPE inside SimpleDateFormat.
        assertThrows(NullPointerException.class, () -> sdf.setTimeZone(TimeZone.getTimeZone("UTC")), "Null pattern should result in NullPointerException when formatter is created");
    }
}
