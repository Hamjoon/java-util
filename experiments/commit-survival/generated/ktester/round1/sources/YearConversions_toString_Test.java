package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Year;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.Date;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class YearConversions_toString_Test {

    @Mock
    private Converter // Converter is not used by the method but required for the signature
    converter;

    @Mock
    private Converter // not used by the method but required for the signature
    mockConverter;

    /**
     * Supplies every test case described in the JSON document.
     *
     * @return a stream of arguments for {@link #testToString(Object, Converter, String, Class, String)}
     */
    private static Stream<Arguments> provideToStringCases() {
        // Converter that throws if its {@code convert} method is called.
        Converter throwingConverter = new Converter() {

            @Override
            public <T> T convert(Object o, Class<T> c) {
                throw new RuntimeException("Converter should not be invoked");
            }
        };
        // Converter that does nothing (returns {@code null}) – also should never be used.
        Converter benignConverter = new Converter() {

            @Override
            public <T> T convert(Object o, Class<T> c) {
                return null;
            }
        };
        return Stream.of(// ---- basic and edge cases -------------------------------------------------
        arguments(Year.of(2023), null, "2023", null, "Standard case: a valid Year instance is provided; returns \"2023\"."), arguments(Year.of(0), null, "0", null, "Zero year: Year.of(0) should be converted to \"0\"."), arguments(Year.MAX_VALUE, null, "999999999", null, "Maximum supported year constant; returns \"999999999\"."), arguments(Year.MIN_VALUE, null, "-999999999", null, "Minimum supported year constant; returns \"-999999999\"."), arguments(null, null, null, NullPointerException.class, "Null source object; casting null to Year and calling toString() throws NPE."), arguments("2023", null, null, ClassCastException.class, "Invalid source type (String); cast fails with ClassCastException."), arguments(new Date(), null, null, ClassCastException.class, "Another non‑Year object (Date); cast fails with ClassCastException."), arguments(Year.of(2023), new Converter() {

            @Override
            public <T> T convert(Object o, Class<T> c) {
                // benign implementation
                return null;
            }
        }, "2023", null, "Valid Year with a non‑null Converter instance; converter is ignored."), // ---- additional cases ----------------------------------------------------
        arguments(Year.of(-1), null, "-1", null, "Negative year not at the extreme limit; returns \"-1\"."), arguments(Year.of(1), null, "1", null, "Smallest positive year; returns \"1\"."), arguments(Year.of(999_999_998), null, "999999998", null, "Just below the maximum Year constant; returns \"999999998\"."), arguments(Year.of(-999_999_998), null, "-999999998", null, "Just above the minimum Year constant; returns \"-999999998\"."), arguments(Year.of(2023), throwingConverter, "2023", null, "Valid Year with a Converter that would throw if invoked; confirms converter is ignored."), arguments(Year.of(2023), benignConverter, "2023", null, "Valid Year with a benign Converter; result remains \"2023\"."));
    }

    @Test
    void toStringReturnsYearString() {
        // given
        Year year = Year.of(2023);
        // when
        String result = YearConversions.toString(year, mockConverter);
        // then
        assertEquals("2023", result);
    }

    @Test
    void toStringWithNonYearThrowsClassCastException() {
        // given
        Object notAYear = "2023";
        // when / then
        assertThrows(ClassCastException.class, () -> YearConversions.toString(notAYear, mockConverter));
    }

    @ParameterizedTest(name = "{index}: {4}")
    @MethodSource("provideToStringCases")
    void testToString(Object from, Converter converter, String expectedResult, Class<? extends Throwable> expectedException, String description) {
        if (expectedException != null) {
            // Expect an exception – the result value is ignored
            assertThrows(expectedException, () -> YearConversions.toString(from, converter), description);
        } else {
            // Normal successful conversion
            String actual = YearConversions.toString(from, converter);
            assertEquals(expectedResult, actual, description);
        }
    }
}
