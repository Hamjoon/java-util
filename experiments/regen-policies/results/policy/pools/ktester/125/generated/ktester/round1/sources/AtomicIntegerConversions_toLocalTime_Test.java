package com.cedarsoftware.util.convert;

import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class AtomicIntegerConversions_toLocalTime_Test {

    /**
     * Mocked static scope for {@link LongConversions}.
     */
    private MockedStatic<LongConversions> longConversionsMock;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh mock for the static class before each test.
        longConversionsMock = Mockito.mockStatic(LongConversions.class);
    }

    @AfterEach
    void teardownAfterEach() {
        // Release the static mock to avoid interference with other tests.
        longConversionsMock.close();
    }

    /**
     * Supplies the normal conversion scenarios described in the JSON spec.
     */
    private static Stream<Arguments> normalCaseProvider() {
        // Helper converter that simply returns the source object (used for the custom case)
        Converter customConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                return src;
            }
        };
        return Stream.of(// Zero value → midnight
        Arguments.of(new AtomicInteger(0), null, LocalTime.MIDNIGHT), // Large value that exceeds a day (1_000_000_000 seconds)
        // Expected time = secondsOfDay = 1_000_000_000 % 86_400
        Arguments.of(new AtomicInteger(1_000_000_000), null, LocalTime.ofSecondOfDay(1_000_000_000L % 86_400L)), // Negative value – wraps to previous day’s last second
        Arguments.of(new AtomicInteger(-1), null, LocalTime.ofSecondOfDay((86_400L - 1) % 86_400L)), // Integer.MAX_VALUE
        Arguments.of(new AtomicInteger(Integer.MAX_VALUE), null, LocalTime.ofSecondOfDay(((long) Integer.MAX_VALUE) % 86_400L)), // Integer.MIN_VALUE (negative)
        Arguments.of(new AtomicInteger(Integer.MIN_VALUE), null, LocalTime.ofSecondOfDay(((long) Integer.MIN_VALUE) % 86_400L)), // Custom non‑null converter
        Arguments.of(new AtomicInteger(12_345_678), customConverter, LocalTime.ofSecondOfDay(12_345_678L % 86_400L)));
    }

    @Test
    void toLocalTime_delegatesToLongConversions_andReturnsResult() {
        // ----- given ---------------------------------------------------------
        // An AtomicInteger with a known value.
        AtomicInteger atomicInt = new AtomicInteger(42);
        // A mock Converter – its behaviour is irrelevant for this test,
        // we only need to verify that the same instance is passed through.
        Converter converter = mock(Converter.class);
        // The LocalTime we expect LongConversions to return.
        LocalTime expectedTime = LocalTime.of(12, 34, 56);
        // Stub the static method to return the expected value when called
        // with the long representation of the atomic integer's value.
        long expectedLong = 42L;
        longConversionsMock.when(() -> LongConversions.toLocalTime(expectedLong, converter)).thenReturn(expectedTime);
        // ----- when ----------------------------------------------------------
        LocalTime actual = AtomicIntegerConversions.toLocalTime(atomicInt, converter);
        // ----- then ----------------------------------------------------------
        assertEquals(expectedTime, actual, "The method should return the LocalTime produced by LongConversions");
        // Verify that the static method was invoked exactly once with the correct arguments.
        longConversionsMock.verify(() -> LongConversions.toLocalTime(expectedLong, converter), times(1));
    }

    @Test
    void toLocalTime_withNegativeAtomicIntegerValue() {
        // ----- given ---------------------------------------------------------
        AtomicInteger atomicInt = new AtomicInteger(-7);
        Converter converter = mock(Converter.class);
        LocalTime expectedTime = LocalTime.of(23, 59, 59);
        long expectedLong = -7L;
        longConversionsMock.when(() -> LongConversions.toLocalTime(expectedLong, converter)).thenReturn(expectedTime);
        // ----- when ----------------------------------------------------------
        LocalTime actual = AtomicIntegerConversions.toLocalTime(atomicInt, converter);
        // ----- then ----------------------------------------------------------
        assertEquals(expectedTime, actual);
        longConversionsMock.verify(() -> LongConversions.toLocalTime(expectedLong, converter), times(1));
    }

    /**
     * Verifies that the method forwards the correct long value and the
     * supplied {@link Converter} (which may be {@code null}) to
     * {@link LongConversions#toLocalTime(long, Converter)} and returns
     * the result unchanged.
     */
    @ParameterizedTest(name = "{index}: from={0}, converter={1}")
    @MethodSource("normalCaseProvider")
    void toLocalTime_normalCases(AtomicInteger from, Converter converter, LocalTime expected) {
        // ----- given ---------------------------------------------------------
        long expectedLong = (long) from.get();
        // stub the static delegate to return the expected LocalTime
        longConversionsMock.when(() -> LongConversions.toLocalTime(expectedLong, converter)).thenReturn(expected);
        // ----- when ----------------------------------------------------------
        LocalTime actual = AtomicIntegerConversions.toLocalTime(from, converter);
        // ----- then ----------------------------------------------------------
        assertEquals(expected, actual, "Returned LocalTime must be exactly the one provided by LongConversions");
        longConversionsMock.verify(() -> LongConversions.toLocalTime(expectedLong, converter), times(1));
    }

    @Test
    void toLocalTime_nullSource_throwsNullPointerException() {
        // ----- given ---------------------------------------------------------
        Object from = null;
        Converter converter = null;
        // ----- when & then ---------------------------------------------------
        assertThrows(NullPointerException.class, () -> AtomicIntegerConversions.toLocalTime(from, converter), "A null source should cause NullPointerException when cast to AtomicInteger");
    }

    @Test
    void toLocalTime_nullSourceWithNonNullConverter_throwsNullPointerException() {
        // ----- given ---------------------------------------------------------
        Object from = null;
        Converter converter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                return src;
            }
        };
        // ----- when & then ---------------------------------------------------
        assertThrows(NullPointerException.class, () -> AtomicIntegerConversions.toLocalTime(from, converter), "Even with a non‑null Converter, a null source must trigger NullPointerException");
    }

    @Test
    void toLocalTime_wrongSourceType_Integer_throwsClassCastException() {
        // ----- given ---------------------------------------------------------
        // not an AtomicInteger
        Object from = Integer.valueOf(42);
        Converter converter = null;
        // ----- when & then ---------------------------------------------------
        assertThrows(ClassCastException.class, () -> AtomicIntegerConversions.toLocalTime(from, converter), "Passing a non‑AtomicInteger source should result in ClassCastException");
    }

    @Test
    void toLocalTime_wrongSourceType_String_throwsClassCastException() {
        // ----- given ---------------------------------------------------------
        Object from = "not an atomic integer";
        Converter converter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                return src;
            }
        };
        // ----- when & then ---------------------------------------------------
        assertThrows(ClassCastException.class, () -> AtomicIntegerConversions.toLocalTime(from, converter), "A String source should cause ClassCastException");
    }
}
