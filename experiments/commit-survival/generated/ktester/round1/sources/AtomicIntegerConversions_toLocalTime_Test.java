package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.DateTimeException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AtomicIntegerConversions_toLocalTime_Test {

    /**
     * Mocked static holder for the LongConversions class.
     */
    private MockedStatic<LongConversions> longConversionsMock;

    /**
     * A dummy Converter instance – the real implementation is not needed for this test.
     */
    private Converter dummyConverter;

    /* --------------------------------------------------------------
     *  Helper holder for parameterized arguments
     * -------------------------------------------------------------- */
    private static class ArgumentsHolder {

        final AtomicInteger atomicInteger;

        // may be null – LongConversions receives null as well
        final Converter converter;

        final LocalTime expected;

        final String description;

        ArgumentsHolder(AtomicInteger atomicInteger, Converter converter, LocalTime expected, String description) {
            this.atomicInteger = atomicInteger;
            this.converter = converter;
            this.expected = expected;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise the static mock for each test so that interactions are isolated.
        longConversionsMock = Mockito.mockStatic(LongConversions.class);
        // Create a simple Converter – the constructor requires ConverterOptions.
        // Using a real instance with default options is fine; if ConverterOptions is not
        // available on the classpath, a mock can be used instead.
        dummyConverter = mock(Converter.class);
    }

    @AfterEach
    void teardownAfterEach() {
        // Release the static mock to avoid interference with other tests.
        if (longConversionsMock != null) {
            longConversionsMock.close();
        }
    }

    /* --------------------------------------------------------------
     *  Valid conversion scenarios – parameterized test
     * -------------------------------------------------------------- */
    private static Stream<ArgumentsHolder> validConversionProvider() {
        return Stream.of(new ArgumentsHolder(new AtomicInteger(0), null, LocalTime.of(0, 0), "AtomicInteger value 0 should be interpreted as midnight (00:00)."), new ArgumentsHolder(new AtomicInteger(3_600_000), null, LocalTime.of(1, 0), "One hour in milliseconds should become 01:00."), new ArgumentsHolder(new AtomicInteger(86_399_999), null, LocalTime.of(23, 59, 59, 999_000_000), "Just before next day should become 23:59:59.999."));
    }

    @Test
    void toLocalTime_delegatesToLongConversions_andReturnsResult() {
        // GIVEN
        int atomicValue = 12_345;
        AtomicInteger atomicInteger = new AtomicInteger(atomicValue);
        LocalTime expected = LocalTime.of(12, 34, 56);
        // Configure the static mock to return the expected LocalTime when called with the
        // long representation of the atomic integer and the supplied converter.
        longConversionsMock.when(() -> LongConversions.toLocalTime(eq((long) atomicValue), same(dummyConverter))).thenReturn(expected);
        // WHEN
        LocalTime actual = AtomicIntegerConversions.toLocalTime(atomicInteger, dummyConverter);
        // THEN
        assertEquals(expected, actual, "The method should return the value supplied by LongConversions");
        // Verify that the static method was invoked exactly once with the correct arguments.
        longConversionsMock.verify(() -> LongConversions.toLocalTime(eq((long) atomicValue), same(dummyConverter)), times(1));
    }

    @Test
    void toLocalTime_throwsClassCastException_whenInputIsNotAtomicInteger() {
        // GIVEN
        Object notAtomic = "not an atomic integer";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> AtomicIntegerConversions.toLocalTime(notAtomic, dummyConverter));
        // No interaction with LongConversions should happen in this error path.
        longConversionsMock.verifyNoInteractions();
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("validConversionProvider")
    void toLocalTime_validConversions(ArgumentsHolder holder) {
        // GIVEN
        long longValue = holder.atomicInteger.get();
        longConversionsMock.when(() -> LongConversions.toLocalTime(eq(longValue), any())).thenReturn(holder.expected);
        // WHEN
        LocalTime actual = AtomicIntegerConversions.toLocalTime(holder.atomicInteger, dummyConverter);
        // THEN
        assertEquals(holder.expected, actual, "The returned LocalTime must match the value supplied by LongConversions.");
        longConversionsMock.verify(() -> LongConversions.toLocalTime(eq(longValue), same(dummyConverter)), times(1));
    }

    /* --------------------------------------------------------------
     *  Null source object handling
     * -------------------------------------------------------------- */
    @Test
    void toLocalTime_nullFrom_throwsNullPointerException() {
        // GIVEN
        Object nullFrom = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> AtomicIntegerConversions.toLocalTime(nullFrom, dummyConverter), "Casting a null 'from' argument to AtomicInteger should throw NPE.");
        // No call to LongConversions should happen.
        longConversionsMock.verifyNoInteractions();
    }

    /* --------------------------------------------------------------
     *  Invalid source type handling
     * -------------------------------------------------------------- */
    @Test
    void toLocalTime_invalidFrom_throwsClassCastException() {
        // GIVEN
        Object notAtomic = "not an atomic integer";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> AtomicIntegerConversions.toLocalTime(notAtomic, dummyConverter), "A non‑AtomicInteger source must cause a ClassCastException.");
        longConversionsMock.verifyNoInteractions();
    }

    /* --------------------------------------------------------------
     *  Converter exception propagation
     * -------------------------------------------------------------- */
    @Test
    void toLocalTime_converterThrowsException_propagatesIt() {
        // GIVEN
        AtomicInteger atomic = new AtomicInteger(1_000);
        Converter badConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                throw new IllegalArgumentException("converter failure");
            }
        };
        // The static delegate will be called, but we let it behave normally – the
        // exception originates from the converter itself, not from LongConversions.
        longConversionsMock.when(() -> LongConversions.toLocalTime(anyLong(), same(badConverter))).thenAnswer(invocation -> {
            // Simulate the real delegate trying to use the converter – it will throw.
            badConverter.convert(invocation.getArgument(0), LocalTime.class);
            // unreachable
            return null;
        });
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> AtomicIntegerConversions.toLocalTime(atomic, badConverter), "Exception thrown by the supplied Converter must propagate.");
        assertEquals("converter failure", ex.getMessage());
        longConversionsMock.verify(() -> LongConversions.toLocalTime(eq((long) atomic.get()), same(badConverter)), times(1));
    }

    /* --------------------------------------------------------------
     *  Negative atomic integer value handling – DateTimeException
     * -------------------------------------------------------------- */
    @Test
    void toLocalTime_negativeValue_propagatesDateTimeException() {
        // GIVEN
        AtomicInteger negative = new AtomicInteger(-1);
        // -1
        long negativeLong = negative.get();
        // Mock LongConversions to throw DateTimeException for negative input.
        longConversionsMock.when(() -> LongConversions.toLocalTime(eq(negativeLong), any())).thenThrow(new DateTimeException("Negative time not allowed"));
        // WHEN / THEN
        DateTimeException ex = assertThrows(DateTimeException.class, () -> AtomicIntegerConversions.toLocalTime(negative, dummyConverter), "Negative millisecond values should result in DateTimeException.");
        assertTrue(ex.getMessage().contains("Negative time"));
        longConversionsMock.verify(() -> LongConversions.toLocalTime(eq(negativeLong), same(dummyConverter)), times(1));
    }
}
