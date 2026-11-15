package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Year;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class YearConversions_toInt_Test {

    /**
     * A mock {@link Converter} – the method under test does not use it, but it must be supplied.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Simple {@link Converter} implementation that does nothing.
     */
    private static class DummyConverter implements Converter {
        // No methods are needed for the test; the real Converter interface has many default methods.
    }

    /**
     * Converter whose methods throw if ever called – used to prove the method never touches it.
     */
    private static class ThrowingConverter implements Converter {

        @Override
        public Object convert(Object from, Class<?> toClass) {
            throw new UnsupportedOperationException("Should never be called");
        }
    }

    private static final class ArgumentsHolder {

        final Object from;

        final Converter converter;

        // int or Class<? extends Throwable>
        final Object expected;

        final String description;

        ArgumentsHolder(Object from, Converter converter, Object expected, String description) {
            this.from = from;
            this.converter = converter;
            this.expected = expected;
            this.description = description;
        }
    }

    /**
     * Test data for successful conversions, including boundary and zero values.
     */
    private static Stream<ArgumentsHolder> validConversions() {
        return Stream.of(new ArgumentsHolder(Year.of(2023), null, 2023, "Converts a standard Year instance to its integer value."), new ArgumentsHolder(Year.of(-5), null, -5, "Converts a negative year (BC) to integer."), new ArgumentsHolder(Year.of(Year.MAX_VALUE), null, 999_999_999, "Converts the maximum supported Year value."), new ArgumentsHolder(Year.of(Year.MIN_VALUE), null, -999_999_999, "Converts the minimum supported Year value."), new ArgumentsHolder(Year.of(0), null, 0, "Converts the ISO‑8601 year zero."));
    }

    /**
     * Test data for cases where the converter argument is non‑null.
     */
    private static Stream<ArgumentsHolder> converterIgnored() {
        return Stream.of(new ArgumentsHolder(Year.of(2023), new DummyConverter(), 2023, "Non‑null DummyConverter is ignored."), new ArgumentsHolder(Year.of(-42), new ThrowingConverter(), -42, "ThrowingConverter is ignored; conversion still succeeds."));
    }

    /**
     * Test data for error handling (null input or wrong type).
     */
    private static Stream<ArgumentsHolder> errorCases() {
        return Stream.of(new ArgumentsHolder(null, null, NullPointerException.class, "Null 'from' argument should cause NullPointerException."), new ArgumentsHolder(Integer.valueOf(2023), null, ClassCastException.class, "Non‑Year object should cause ClassCastException."));
    }

    @Test
    void toInt_givenYear_returnsCorrectInt() {
        // given
        Year year = Year.of(2023);
        // when
        int result = YearConversions.toInt(year, mockConverter);
        // then
        assertEquals(2023, result, "Year value should be returned as int");
    }

    @Test
    void toInt_givenNonYear_throwsClassCastException() {
        // given
        Object notAYear = "2023";
        // when / then
        assertThrows(ClassCastException.class, () -> YearConversions.toInt(notAYear, mockConverter), "Passing a non‑Year object should result in ClassCastException");
    }

    @Test
    void toInt_givenNull_throwsNullPointerException() {
        // given
        Object nullObj = null;
        // when / then
        assertThrows(NullPointerException.class, () -> YearConversions.toInt(nullObj, mockConverter), "Passing null should result in NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversions")
    void toInt_givenValidYear_returnsCorrectInt(ArgumentsHolder ah) {
        // given
        Object from = ah.from;
        // will be null for these cases
        Converter conv = ah.converter;
        // when
        int result = YearConversions.toInt(from, conv);
        // then
        assertEquals(ah.expected, result, ah.description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("converterIgnored")
    void toInt_converterArgumentIsIgnored(ArgumentsHolder ah) {
        // given
        Object from = ah.from;
        Converter conv = ah.converter;
        // when
        int result = YearConversions.toInt(from, conv);
        // then
        assertEquals(ah.expected, result, ah.description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("errorCases")
    void toInt_errorHandling(ArgumentsHolder ah) {
        // given
        Object from = ah.from;
        Converter conv = ah.converter;
        // when / then
        if (ah.expected instanceof Class) {
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) ah.expected;
            assertThrows(exClass, () -> YearConversions.toInt(from, conv), ah.description);
        } else {
            fail("Test data error: expected should be an exception class");
        }
    }
}
