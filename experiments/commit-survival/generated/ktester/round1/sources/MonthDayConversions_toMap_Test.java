package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.ConverterOptions;
import org.junit.jupiter.api.*;
import java.time.MonthDay;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import com.cedarsoftware.util.CompactLinkedMap;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MonthDayConversions_toMap_Test {

    /**
     * Shared Converter instance used by the tests that need a real object.
     */
    private static Converter sharedConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a Converter with default options – the toMap implementation does not use it,
        // but providing a real instance mirrors typical usage and avoids null‑pointer surprises.
        ConverterOptions options = new ConverterOptions();
        sharedConverter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the shared Converter reference.
        sharedConverter = null;
    }

    /**
     * Supplies the successful conversion scenarios extracted from the JSON test cases.
     */
    static Stream<Arguments> provideValidMonthDayCases() {
        return Stream.of(// basic functionality group
        Arguments.of(MonthDay.of(1, 15), null, 15, 1, "Convert a normal MonthDay (January 15) – null converter"), Arguments.of(MonthDay.of(12, 31), null, 31, 12, "Convert the last day of the year (December 31) – null converter"), Arguments.of(MonthDay.of(2, 29), null, 29, 2, "Convert a leap‑year day (February 29) – null converter"), // additional cases group
        Arguments.of(MonthDay.of(1, 1), null, 1, 1, "Convert the earliest valid MonthDay (January 1) – null converter"), Arguments.of(MonthDay.of(4, 30), null, 30, 4, "Convert a MonthDay with a 30‑day month (April 30) – null converter"), Arguments.of(MonthDay.of(2, 28), null, 28, 2, "Convert a non‑leap‑year February date (February 28) – null converter"), Arguments.of(MonthDay.of(12, 30), null, 30, 12, "Convert a near‑maximum day in December (December 30) – null converter"), // case with a non‑null Converter instance
        Arguments.of(MonthDay.of(5, 15), new Converter(new ConverterOptions()), 15, 5, "Pass a non‑null Converter instance; method should ignore it"));
    }

    /**
     * Supplies the exception scenarios extracted from the JSON test cases.
     */
    static Stream<Arguments> provideExceptionCases() {
        return Stream.of(// Null 'from' argument – the cast to MonthDay triggers NPE
        Arguments.of(null, NullPointerException.class, "Pass a null 'from' argument; expect NullPointerException"), // Wrong type (String) – casting to MonthDay triggers ClassCastException
        Arguments.of("2021-03-10", ClassCastException.class, "Pass a String instead of MonthDay; expect ClassCastException"));
    }

    @Test
    void toMapProducesCorrectEntries() {
        // given
        // December 25th
        MonthDay monthDay = MonthDay.of(12, 25);
        // when
        Map<String, Object> result = MonthDayConversions.toMap(monthDay, sharedConverter);
        // then
        assertNotNull(result, "Resulting map should not be null");
        assertEquals(2, result.size(), "Map should contain exactly two entries");
        assertEquals(25, result.get("day"), "Day entry should match the MonthDay's day");
        assertEquals(12, result.get("month"), "Month entry should match the MonthDay's month");
    }

    @Test
    void toMapWorksWhenConverterIsNull() {
        // given
        // January 1st
        MonthDay monthDay = MonthDay.of(1, 1);
        // when
        Map<String, Object> result = MonthDayConversions.toMap(monthDay, null);
        // then
        assertNotNull(result);
        assertEquals(1, result.get("day"));
        assertEquals(1, result.get("month"));
    }

    @Test
    void toMapThrowsClassCastExceptionForInvalidInput() {
        // given
        Object notAMonthDay = "2023-01-01";
        // when / then
        assertThrows(ClassCastException.class, () -> MonthDayConversions.toMap(notAMonthDay, sharedConverter), "A non‑MonthDay argument should cause a ClassCastException");
    }

    /**
     * Parameterised test that verifies the method throws the expected exception
     * when supplied with invalid input.
     */
    @ParameterizedTest(name = "{2}")
    @MethodSource("provideExceptionCases")
    void toMapThrowsExpectedException(Object from, Class<? extends Throwable> expectedException, String description) {
        // when / then
        assertThrows(expectedException, () -> MonthDayConversions.toMap(from, null), description);
    }
}
