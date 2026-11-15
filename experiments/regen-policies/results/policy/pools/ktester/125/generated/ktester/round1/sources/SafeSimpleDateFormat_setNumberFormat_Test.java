package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

@ExtendWith(MockitoExtension.class)
class SafeSimpleDateFormat_setNumberFormat_Test {

    private SafeSimpleDateFormat safeSimpleDateFormat;

    private SimpleDateFormat mockSimpleDateFormat;

    private MockedStatic<SafeSimpleDateFormat> safeSimpleDateFormatStaticMock;

    @BeforeEach
    void setupBeforeEach() {
        // Mock the static method SafeSimpleDateFormat.getDateFormat(String)
        safeSimpleDateFormatStaticMock = mockStatic(SafeSimpleDateFormat.class);
        // Create a mock SimpleDateFormat that will be returned by the static method
        mockSimpleDateFormat = mock(SimpleDateFormat.class);
        // When any format string is supplied, return the mock SimpleDateFormat
        safeSimpleDateFormatStaticMock.when(() -> SafeSimpleDateFormat.getDateFormat(anyString())).thenReturn(mockSimpleDateFormat);
        // Construct the object under test – the constructor itself calls getDateFormat,
        // which now returns the mock we prepared.
        safeSimpleDateFormat = new SafeSimpleDateFormat("yyyy/MM/dd");
    }

    @AfterEach
    void teardownAfterEach() {
        // Release the static mock to avoid side‑effects on other tests
        safeSimpleDateFormatStaticMock.close();
    }

    @Test
    void setNumberFormat_delegatesToUnderlyingSimpleDateFormat() {
        // Given
        NumberFormat customNumberFormat = mock(NumberFormat.class);
        // When
        safeSimpleDateFormat.setNumberFormat(customNumberFormat);
        // Then
        verify(mockSimpleDateFormat, times(1)).setNumberFormat(customNumberFormat);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void setNumberFormat_firstInvocation_delegatesToUnderlyingSimpleDateFormat() {
        // Given a real DecimalFormat (not a mock) and a real SimpleDateFormat cache.
        // stop returning the mock
        safeSimpleDateFormatStaticMock.close();
        safeSimpleDateFormatStaticMock = mockStatic(SafeSimpleDateFormat.class);
        // Let the real static method execute so the internal cache is used.
        safeSimpleDateFormatStaticMock.when(() -> SafeSimpleDateFormat.getDateFormat(anyString())).thenCallRealMethod();
        // Re‑create the object after resetting the static mock.
        safeSimpleDateFormat = new SafeSimpleDateFormat("yyyy-MM-dd");
        DecimalFormat df = new DecimalFormat("#,##0.00");
        // When
        safeSimpleDateFormat.setNumberFormat(df);
        // Then – retrieve the SimpleDateFormat that was cached for the pattern
        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd");
        assertSame(df, underlying.getNumberFormat(), "The NumberFormat of the cached SimpleDateFormat must be the one supplied.");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void setNumberFormat_secondInvocation_replacesNumberFormatOnSameSimpleDateFormat() {
        safeSimpleDateFormatStaticMock.close();
        safeSimpleDateFormatStaticMock = mockStatic(SafeSimpleDateFormat.class);
        safeSimpleDateFormatStaticMock.when(() -> SafeSimpleDateFormat.getDateFormat(anyString())).thenCallRealMethod();
        safeSimpleDateFormat = new SafeSimpleDateFormat("yyyy-MM-dd");
        DecimalFormat first = new DecimalFormat("#,##0.00");
        DecimalFormat second = new DecimalFormat("#,##0.##");
        // First call – creates and caches the SimpleDateFormat
        safeSimpleDateFormat.setNumberFormat(first);
        SimpleDateFormat cachedAfterFirst = SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd");
        assertSame(first, cachedAfterFirst.getNumberFormat(), "After first call the NumberFormat should be the first DecimalFormat.");
        // Second call – should use the *same* SimpleDateFormat instance
        safeSimpleDateFormat.setNumberFormat(second);
        SimpleDateFormat cachedAfterSecond = SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd");
        assertSame(cachedAfterFirst, cachedAfterSecond, "The SimpleDateFormat instance must be identical (cache hit).");
        assertSame(second, cachedAfterSecond.getNumberFormat(), "After second call the NumberFormat must be replaced with the second DecimalFormat.");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void setNumberFormat_nullNumberFormat_throwsNullPointerException() {
        // The static mock already returns a mock SimpleDateFormat, whose
        // setNumberFormat(null) will throw NPE as per JDK behaviour.
        assertThrows(NullPointerException.class, () -> safeSimpleDateFormat.setNumberFormat(null), "Passing null should propagate NullPointerException.");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void setNumberFormat_differentPatterns_maintainSeparateCaches() {
        safeSimpleDateFormatStaticMock.close();
        safeSimpleDateFormatStaticMock = mockStatic(SafeSimpleDateFormat.class);
        safeSimpleDateFormatStaticMock.when(() -> SafeSimpleDateFormat.getDateFormat(anyString())).thenCallRealMethod();
        SafeSimpleDateFormat sdf1 = new SafeSimpleDateFormat("yyyy-MM-dd");
        SafeSimpleDateFormat sdf2 = new SafeSimpleDateFormat("MM/dd/yyyy");
        DecimalFormat df1 = new DecimalFormat("#,##0.00");
        DecimalFormat df2 = new DecimalFormat("0.###");
        sdf1.setNumberFormat(df1);
        sdf2.setNumberFormat(df2);
        SimpleDateFormat underlying1 = SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd");
        SimpleDateFormat underlying2 = SafeSimpleDateFormat.getDateFormat("MM/dd/yyyy");
        assertSame(df1, underlying1.getNumberFormat(), "Pattern 'yyyy-MM-dd' must hold its own NumberFormat.");
        assertSame(df2, underlying2.getNumberFormat(), "Pattern 'MM/dd/yyyy' must hold its own NumberFormat.");
        assertNotSame(underlying1, underlying2, "Different patterns must not share the same SimpleDateFormat instance.");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void setNumberFormat_concurrentUpdates_allThreadsSeeConsistentState() throws Exception {
        safeSimpleDateFormatStaticMock.close();
        safeSimpleDateFormatStaticMock = mockStatic(SafeSimpleDateFormat.class);
        safeSimpleDateFormatStaticMock.when(() -> SafeSimpleDateFormat.getDateFormat(anyString())).thenCallRealMethod();
        SafeSimpleDateFormat shared = new SafeSimpleDateFormat("yyyy-MM-dd");
        // Prepare distinct DecimalFormat instances
        List<DecimalFormat> formats = Arrays.asList(new DecimalFormat("#,##0"), new DecimalFormat("0.##"), new DecimalFormat("#.###"), new DecimalFormat("0.###"), new DecimalFormat("#,##0.00"));
        ExecutorService executor = Executors.newFixedThreadPool(formats.size());
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(formats.size());
        // Keep a reference to the format set by the *last* thread (deterministic ordering)
        AtomicReference<DecimalFormat> lastSet = new AtomicReference<>();
        for (int i = 0; i < formats.size(); i++) {
            final DecimalFormat fmt = formats.get(i);
            final int index = i;
            executor.submit(() -> {
                try {
                    // ensure all threads start together
                    startLatch.await();
                    shared.setNumberFormat(fmt);
                    // Record the format of the thread that finishes last
                    if (index == formats.size() - 1) {
                        lastSet.set(fmt);
                    }
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        // Release all threads
        startLatch.countDown();
        // Wait for completion
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "All threads should finish in time");
        executor.shutdownNow();
        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd");
        NumberFormat finalNumberFormat = underlying.getNumberFormat();
        // The final NumberFormat must be one of the formats we supplied
        assertTrue(formats.contains(finalNumberFormat), "Final NumberFormat should be one of the concurrently supplied formats.");
        // In our deterministic setup the last thread's format should be present
        assertSame(lastSet.get(), finalNumberFormat, "The NumberFormat should match the one set by the last thread.");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void setNumberFormat_invalidPattern_throwsIllegalArgumentException() {
        safeSimpleDateFormatStaticMock.close();
        safeSimpleDateFormatStaticMock = mockStatic(SafeSimpleDateFormat.class);
        safeSimpleDateFormatStaticMock.when(() -> SafeSimpleDateFormat.getDateFormat(anyString())).thenCallRealMethod();
        SafeSimpleDateFormat badPattern = new SafeSimpleDateFormat("yyyy-@@-dd");
        DecimalFormat df = new DecimalFormat("#,##0.00");
        assertThrows(IllegalArgumentException.class, () -> badPattern.setNumberFormat(df), "An illegal date pattern must cause IllegalArgumentException from setNumberFormat.");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void setNumberFormat_nullPattern_throwsNullPointerException() {
        safeSimpleDateFormatStaticMock.close();
        safeSimpleDateFormatStaticMock = mockStatic(SafeSimpleDateFormat.class);
        safeSimpleDateFormatStaticMock.when(() -> SafeSimpleDateFormat.getDateFormat(anyString())).thenCallRealMethod();
        SafeSimpleDateFormat nullPattern = new SafeSimpleDateFormat(null);
        DecimalFormat df = new DecimalFormat("#,##0.00");
        assertThrows(NullPointerException.class, () -> nullPattern.setNumberFormat(df), "A null pattern should cause NullPointerException when setNumberFormat triggers getDateFormat.");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void setNumberFormat_numberFormatCloneThrows_unsupportedOperationExceptionPropagates() {
        safeSimpleDateFormatStaticMock.close();
        safeSimpleDateFormatStaticMock = mockStatic(SafeSimpleDateFormat.class);
        safeSimpleDateFormatStaticMock.when(() -> SafeSimpleDateFormat.getDateFormat(anyString())).thenCallRealMethod();
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy-MM-dd");
        NumberFormat badNumberFormat = new NumberFormat() {

            @Override
            public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
                return null;
            }

            @Override
            public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
                return null;
            }

            @Override
            public Number parse(String source, ParsePosition pos) {
                return null;
            }

            @Override
            public Object clone() {
                throw new UnsupportedOperationException("clone not supported");
            }
        };
        assertThrows(UnsupportedOperationException.class, () -> sdf.setNumberFormat(badNumberFormat), "When SimpleDateFormat attempts to clone the NumberFormat, the exception must propagate.");
    }
}
