package com.cedarsoftware.util.convert;

import java.time.LocalTime;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.DateTimeException;
import java.util.stream.Stream;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class IntegerConversions_toLocalTime_Test {

    private Converter converter;

    private MockedStatic<LongConversions> longConversionsMock;

    /**
     * A regular, non‑null converter used for the happy‑path tests.
     */
    private Converter dummyConverter;

    @BeforeEach
    void setupBeforeEach() {
        // Create a real Converter with default options – it has no side‑effects for this test.
        converter = new Converter(new ConverterOptions());
        // Mock the static LongConversions class so we can verify the delegation.
        longConversionsMock = mockStatic(LongConversions.class);
    }

    @AfterEach
    void teardownAfterEach() {
        // Release the static mock.
        if (longConversionsMock != null) {
            longConversionsMock.close();
        }
    }

    /**
     * Supplies valid conversion scenarios.
     */
    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// 0 ms → midnight
        Arguments.of(0, LocalTime.MIDNIGHT), // 12 345 ms → 12 345 000 000 ns
        Arguments.of(12_345, LocalTime.ofNanoOfDay(12_345L * 1_000_000L)), // Small positive value 86 ms
        Arguments.of(86, LocalTime.ofNanoOfDay(86L * 1_000_000L)));
    }

    /**
     * Supplies values that are out of the LocalTime range.
     */
    private static Stream<Arguments> dateTimeExceptionProvider() {
        return Stream.of(// Negative millisecond
        Arguments.of(-1), // Just below 24 h (86 399 999 ms)
        Arguments.of(86_399_999), // Exactly 24 h (86 400 000 ms) – not representable
        Arguments.of(86_400_000), // Max int value
        Arguments.of(Integer.MAX_VALUE), // Min int value
        Arguments.of(Integer.MIN_VALUE));
    }

    @Test
    void toLocalTime_delegatesToLongConversions_andReturnsResult() {
        // GIVEN
        // milliseconds as Integer
        Integer source = 12_345;
        LocalTime expected = LocalTime.of(12, 34, 56);
        long expectedMs = source.longValue();
        // Stub the static call.
        longConversionsMock.when(() -> LongConversions.toLocalTime(expectedMs, converter)).thenReturn(expected);
        // WHEN
        LocalTime actual = IntegerConversions.toLocalTime(source, converter);
        // THEN
        assertEquals(expected, actual, "The LocalTime returned by IntegerConversions should match the stubbed value");
        // Verify that the static method was invoked exactly once with the expected arguments.
        longConversionsMock.verify(() -> LongConversions.toLocalTime(expectedMs, converter), times(1));
    }

    @Test
    void toLocalTime_nullSource_throwsNullPointerException() {
        // GIVEN
        Object nullSource = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> IntegerConversions.toLocalTime(nullSource, dummyConverter), "Passing null as the source must raise NullPointerException");
        // No interaction with LongConversions should happen.
        longConversionsMock.verifyNoInteractions();
    }

    @Test
    void toLocalTime_invalidSourceType_throwsClassCastException() {
        // GIVEN
        Object notAnInteger = "not-an-integer";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> IntegerConversions.toLocalTime(notAnInteger, converter), "Passing a non‑Integer source should result in ClassCastException");
    }

    /* -------------------------------------------------------------
     *  Parameterized tests for valid integer → LocalTime conversions
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0} → expected={1}")
    @MethodSource("validConversionProvider")
    void toLocalTime_validInputs_delegatesAndReturnsResult(Integer from, LocalTime expected) {
        // GIVEN
        long ms = from.longValue();
        // Stub the static delegation.
        longConversionsMock.when(() -> LongConversions.toLocalTime(ms, dummyConverter)).thenReturn(expected);
        // WHEN
        LocalTime actual = IntegerConversions.toLocalTime(from, dummyConverter);
        // THEN
        assertEquals(expected, actual, () -> "Expected LocalTime " + expected + " for input milliseconds " + from);
        // Verify that the static method was called exactly once with the expected arguments.
        longConversionsMock.verify(() -> LongConversions.toLocalTime(ms, dummyConverter), times(1));
    }

    /* -------------------------------------------------------------
     *  Parameterized tests for values that should cause DateTimeException
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0} → throws DateTimeException")
    @MethodSource("dateTimeExceptionProvider")
    void toLocalTime_invalidValues_propagatesDateTimeException(Integer from) {
        // GIVEN
        long ms = from.longValue();
        // Stub the static method to throw DateTimeException.
        longConversionsMock.when(() -> LongConversions.toLocalTime(ms, dummyConverter)).thenThrow(new DateTimeException("Invalid time"));
        // WHEN / THEN
        assertThrows(DateTimeException.class, () -> IntegerConversions.toLocalTime(from, dummyConverter), () -> "Expected DateTimeException for milliseconds = " + from);
        // Verify delegation occurred.
        longConversionsMock.verify(() -> LongConversions.toLocalTime(ms, dummyConverter), times(1));
    }

    /* -------------------------------------------------------------
     *  Invalid source type handling (non‑Integer)
     * ------------------------------------------------------------- */
    @Test
    void toLocalTime_wrongSourceType_throwsClassCastException() {
        // GIVEN
        Object notAnInteger = "not-an-integer";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> IntegerConversions.toLocalTime(notAnInteger, dummyConverter), "Casting a non‑Integer source should raise ClassCastException");
        longConversionsMock.verifyNoInteractions();
    }

    @Test
    void toLocalTime_longSource_throwsClassCastException() {
        // GIVEN
        // boxed long, not Integer
        Object longSource = Long.valueOf(12_345L);
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> IntegerConversions.toLocalTime(longSource, dummyConverter), "Casting a Long source to Integer must raise ClassCastException");
        longConversionsMock.verifyNoInteractions();
    }

    /* -------------------------------------------------------------
     *  Null converter handling – LongConversions is expected to NPE
     * ------------------------------------------------------------- */
    @Test
    void toLocalTime_nullConverter_propagatesNullPointerExceptionFromLongConversions() {
        // GIVEN
        Integer source = 1_000;
        long ms = source.longValue();
        // Stub the static call to throw NPE when converter is null.
        longConversionsMock.when(() -> LongConversions.toLocalTime(ms, null)).thenThrow(new NullPointerException("converter is null"));
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> IntegerConversions.toLocalTime(source, null), "A null converter should result in NullPointerException from LongConversions");
        longConversionsMock.verify(() -> LongConversions.toLocalTime(ms, null), times(1));
    }

    /* -------------------------------------------------------------
     *  Converter that throws IllegalStateException
     * ------------------------------------------------------------- */
    @Test
    void toLocalTime_converterThrowsIllegalStateException_propagatesIt() {
        // GIVEN
        Integer source = 1_000;
        long ms = source.longValue();
        Converter throwingConverter = mock(Converter.class);
        // Simulate LongConversions throwing the exception because of the converter.
        longConversionsMock.when(() -> LongConversions.toLocalTime(ms, throwingConverter)).thenThrow(new IllegalStateException("converter failure"));
        // WHEN / THEN
        assertThrows(IllegalStateException.class, () -> IntegerConversions.toLocalTime(source, throwingConverter), "Exception from Converter should propagate unchanged");
        longConversionsMock.verify(() -> LongConversions.toLocalTime(ms, throwingConverter), times(1));
    }

    /* -------------------------------------------------------------
     *  Converter that throws NullPointerException during processing
     * ------------------------------------------------------------- */
    @Test
    void toLocalTime_converterThrowsNullPointerException_propagatesIt() {
        // GIVEN
        Integer source = 1_000;
        long ms = source.longValue();
        Converter nullPointerThrowingConverter = mock(Converter.class);
        longConversionsMock.when(() -> LongConversions.toLocalTime(ms, nullPointerThrowingConverter)).thenThrow(new NullPointerException("converter NPE"));
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> IntegerConversions.toLocalTime(source, nullPointerThrowingConverter), "NullPointerException from Converter must propagate");
        longConversionsMock.verify(() -> LongConversions.toLocalTime(ms, nullPointerThrowingConverter), times(1));
    }
}
