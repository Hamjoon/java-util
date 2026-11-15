package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Year;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;
import java.util.Date;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class YearConversions_toInt_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

    /**
     * Shared {@link Converter} instance used for the “regular” cases.
     */
    private static Converter regularConverter;

    /* -------------------------------------------------------------
     *  Helper stub classes (if needed by external code). They are not
     *  used directly in the tests above because we rely on Mockito for
     *  the faulty converter, but they illustrate how a custom converter
     *  could be created.
     * ------------------------------------------------------------- */
    @SuppressWarnings("unused")
    private static class DummyConverter extends Converter {

        DummyConverter() {
            super(new ConverterOptions());
        }
    }

    @SuppressWarnings("unused")
    private static class FaultyConverter extends Converter {

        FaultyConverter() {
            super(new ConverterOptions());
        }

        @Override
        public Object convert(Object from, Class<?> toClass) {
            throw new RuntimeException("FaultyConverter always fails");
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // The converter does not affect the implementation, but we need a
        // non‑null instance to satisfy the method signature for most cases.
        ConverterOptions options = new ConverterOptions();
        regularConverter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference.
        converter = null;
    }

    private static Stream<Arguments> provideSuccessCases() {
        // A dummy converter that throws on any method call – it will never be used.
        Converter faultyConverter = Mockito.mock(Converter.class);
        Mockito.doThrow(new RuntimeException("Should not be called")).when(faultyConverter).convert(Mockito.any(), Mockito.any());
        return Stream.of(// Standard positive year
        arguments(Year.of(2023), null, 2023), // Zero year
        arguments(Year.of(0), null, 0), // Negative year (BC)
        arguments(Year.of(-1), null, -1), // Very large year (within int range)
        arguments(Year.of(999_999_999), null, 999_999_999), // Minimum allowed year (still fits in int)
        arguments(Year.of(-999_999_999), null, -999_999_999), // Small positive year
        arguments(Year.of(1), null, 1), // Using a regular non‑null converter – should be ignored
        arguments(Year.of(2023), regularConverter, 2023), // Using a faulty converter – still ignored
        arguments(Year.of(0), faultyConverter, 0));
    }

    private static Stream<Arguments> provideExceptionCases() {
        return Stream.of(// Null source object
        arguments(null, null, NullPointerException.class), // Non‑Year object: String
        arguments("2023", null, ClassCastException.class), // Non‑Year object: java.util.Date
        arguments(new Date(), null, ClassCastException.class));
    }

    @Test
    void toInt_givenYear_returnsCorrectInt() {
        // given
        Year year = Year.of(2023);
        // when
        int result = YearConversions.toInt(year, converter);
        // then
        assertEquals(2023, result, "YearConversions.toInt should return the Year value");
    }

    @Test
    void toInt_givenNonYearObject_throwsClassCastException() {
        // given
        Object notAYear = "2023";
        // when / then
        assertThrows(ClassCastException.class, () -> YearConversions.toInt(notAYear, converter), "Passing a non‑Year object should result in ClassCastException");
    }

    @Test
    void toInt_givenNull_throwsNullPointerException() {
        // given
        Object nullObj = null;
        // when / then
        assertThrows(NullPointerException.class, () -> YearConversions.toInt(nullObj, converter), "Passing null should result in NullPointerException");
    }

    /* -------------------------------------------------------------
     *  Success scenarios – the method should return the Year value.
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0}, converter={1} → expected={2}")
    @MethodSource("provideSuccessCases")
    void toInt_successfulConversion(Object from, Converter converter, int expected) {
        // when
        int result = YearConversions.toInt(from, converter);
        // then
        assertEquals(expected, result, () -> "Expected Year value " + expected + " but got " + result);
    }

    /* -------------------------------------------------------------
     *  Failure scenarios – the method should throw the expected exception.
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0} → throws {1}")
    @MethodSource("provideExceptionCases")
    void toInt_exceptionalCases(Object from, Converter converter, Class<? extends Throwable> expectedException) {
        // when / then
        assertThrows(expectedException, () -> YearConversions.toInt(from, converter), () -> "Expected " + expectedException.getSimpleName() + " when converting " + from);
    }
}
