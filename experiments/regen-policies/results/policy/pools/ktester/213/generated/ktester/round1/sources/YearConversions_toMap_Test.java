package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Year;
import java.util.Map;
import static com.cedarsoftware.util.convert.MapConversions.YEAR;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.LinkedHashMap;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class YearConversions_toMap_Test {

    /**
     * Converter instance used for the tests.
     */
    private Converter converter;

    /* -------------------------------------------------------------
     *  Helper class – a minimal stub of Converter used for the
     *  “non‑null converter ignored” test.
     * ------------------------------------------------------------- */
    private static class DummyConverter extends Converter {

        DummyConverter() {
            super(new ConverterOptions());
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a real Converter with default options – the method under test does not use it,
        // but providing a non‑null instance mirrors normal usage.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Release reference – helps GC and makes the test class tidy.
        converter = null;
    }

    private static Stream<Arguments> provideSuccessfulConversions() {
        return Stream.of(// normal case
        arguments(Year.of(2023), 2023), // minimum supported ISO year
        arguments(Year.of(-999_999_999), -999_999_999), // year zero
        arguments(Year.of(0), 0), // maximum supported ISO year
        arguments(Year.of(999_999_999), 999_999_999), // non‑null converter ignored (same as null)
        arguments(Year.of(2025), 2025));
    }

    @Test
    void toMap_returnsMapContainingYearKeyAndCorrectValue() {
        // given
        Year year = Year.of(2023);
        // when
        Map<?, ?> result = YearConversions.toMap(year, converter);
        // then
        assertNotNull(result, "Resulting map should not be null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertTrue(result.containsKey(YEAR), "Map should contain the YEAR key");
        assertEquals(year.getValue(), result.get(YEAR), "Map value should match Year#getValue()");
    }

    @Test
    void toMap_withNonYearObject_throwsClassCastException() {
        // given
        Object notAYear = "2023";
        // when / then
        assertThrows(ClassCastException.class, () -> YearConversions.toMap(notAYear, converter), "Passing a non‑Year object should result in ClassCastException");
    }

    @Test
    void toMap_withNullFrom_throwsNullPointerException() {
        // given
        Object nullObj = null;
        // when / then
        assertThrows(NullPointerException.class, () -> YearConversions.toMap(nullObj, converter), "Passing null as the source should result in NullPointerException");
    }

    /* -------------------------------------------------------------
     *  Parameterised tests for successful conversions
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => year={0}, expectedValue={1}")
    @MethodSource("provideSuccessfulConversions")
    void toMap_successfulConversion(Year inputYear, int expectedValue) {
        // when
        Map<?, ?> result = YearConversions.toMap(inputYear, converter);
        // then
        assertNotNull(result, "Resulting map should not be null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertTrue(result.containsKey(YEAR), "Map should contain the YEAR key");
        assertEquals(expectedValue, result.get(YEAR), "Map value should match Year#getValue()");
    }

    @Test
    void toMap_withInvalidSourceType_throwsClassCastException() {
        // given
        Object notAYear = "2023";
        // when / then
        assertThrows(ClassCastException.class, () -> YearConversions.toMap(notAYear, converter), "Supplying a non‑Year object should trigger ClassCastException");
    }

    /* -------------------------------------------------------------
     *  Test that the supplied Converter argument is ignored
     * ------------------------------------------------------------- */
    @Test
    void toMap_converterArgumentIsIgnored() {
        // given
        Year year = Year.of(2025);
        // a non‑null instance
        Converter dummy = new DummyConverter();
        // when
        Map<?, ?> result = YearConversions.toMap(year, dummy);
        // then
        assertNotNull(result);
        assertEquals(year.getValue(), result.get(YEAR));
    }

    /* -------------------------------------------------------------
     *  Verify returned map characteristics
     * ------------------------------------------------------------- */
    @Test
    void toMap_returnsMutableLinkedHashMapWithSingleEntry() {
        // given
        Year year = Year.of(1999);
        // when
        Map<?, ?> result = YearConversions.toMap(year, converter);
        // then
        assertTrue(result instanceof LinkedHashMap, "Result should be an instance of LinkedHashMap (preserves insertion order)");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertTrue(result.containsKey(YEAR), "Map should contain the YEAR key");
        assertEquals(year.getValue(), result.get(YEAR));
        // Verify mutability – we should be able to put a new entry without exception.
        assertDoesNotThrow(() -> ((LinkedHashMap<?, ?>) result).put("extra", 1), "Returned LinkedHashMap should be mutable");
    }
}
