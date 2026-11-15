package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Date;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BigDecimalConversions_toDate_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter requires a ConverterOptions object; using the default constructor.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // No external resources to clean up, but nulling the reference helps GC in long test suites.
        converter = null;
    }

    private static Stream<Arguments> numericConversionProvider() {
        return Stream.of(// BigDecimal representing whole seconds (epoch seconds)
        Arguments.of(BigDecimal.valueOf(1609459200L), 1609459200000L), // BigDecimal with fractional seconds – only milliseconds are kept
        Arguments.of(new BigDecimal("1609459200.123456789"), 1609459200123L), // BigInteger interpreted as epoch milliseconds directly
        Arguments.of(BigInteger.valueOf(1609459200000L), 1609459200000L));
    }

    @Test
    void toDate_givenDate_returnsSameInstant() {
        // GIVEN
        // current moment
        Date original = new Date();
        // WHEN
        Date result = BigDecimalConversions.toDate(original, converter);
        // THEN
        assertNotNull(result, "Resulting Date should not be null");
        assertEquals(original.getTime(), result.getTime(), "The returned Date should represent the same instant as the source Date");
    }

    @Test
    void toDate_givenInstant_returnsEquivalentDate() {
        // GIVEN
        Instant instant = Instant.now();
        // WHEN
        Date result = BigDecimalConversions.toDate(instant, converter);
        // THEN
        assertNotNull(result, "Resulting Date should not be null");
        assertEquals(instant.toEpochMilli(), result.getTime(), "The returned Date should represent the same instant as the source Instant");
    }

    @Test
    void toDate_givenNull_returnsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        Date result = BigDecimalConversions.toDate(source, converter);
        // THEN
        assertNull(result, "Converting a null source should yield null");
    }

    /**
     * Parameterized test that verifies conversion from various numeric
     * representations (BigDecimal, BigInteger) to {@link Date}.
     *
     * The expected epoch‑millis are calculated according to the contract
     * described in the JSON test cases.
     */
    @ParameterizedTest(name = "{index} => from={0}, expectedMillis={1}")
    @MethodSource("numericConversionProvider")
    void toDate_givenNumeric_returnsExpectedDate(Object from, long expectedMillis) {
        // WHEN
        Date result = BigDecimalConversions.toDate(from, converter);
        // THEN
        assertNotNull(result, "Result should not be null for numeric input");
        assertEquals(expectedMillis, result.getTime(), "Converted Date does not match expected epoch‑millis");
    }

    @Test
    void toDate_givenString_throwsIllegalArgumentException() {
        // GIVEN
        String source = "some random string";
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> BigDecimalConversions.toDate(source, converter), "A non‑parseable String should cause IllegalArgumentException");
    }

    @Test
    void toDate_givenArbitraryObject_throwsIllegalArgumentException() {
        // GIVEN
        Object source = new Object();
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> BigDecimalConversions.toDate(source, converter), "An unsupported Object should cause IllegalArgumentException");
    }

    @Test
    void toDate_givenNullSource_throwsNullPointerException() {
        // GIVEN
        Object source = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> BigDecimalConversions.toDate(source, converter), "A null source should cause NullPointerException when toInstant dereferences it");
    }

    @Test
    void toDate_givenLocalDate_throwsDateTimeException() {
        // GIVEN
        LocalDate source = LocalDate.of(2021, 1, 1);
        // WHEN / THEN
        assertThrows(DateTimeException.class, () -> BigDecimalConversions.toDate(source, converter), "Converting a LocalDate without time information should raise DateTimeException");
    }

    @Test
    void toDate_givenLocalTime_throwsDateTimeException() {
        // GIVEN
        LocalTime source = LocalTime.of(12, 34, 56);
        // WHEN / THEN
        assertThrows(DateTimeException.class, () -> BigDecimalConversions.toDate(source, converter), "Converting a LocalTime without date information should raise DateTimeException");
    }

    @Test
    void toDate_givenInstantFromJsonExample_returnsExactDate() {
        // GIVEN
        // 2021‑01‑01T00:00:00Z
        Instant instant = Instant.ofEpochSecond(1609459200L);
        // WHEN
        Date result = BigDecimalConversions.toDate(instant, converter);
        // THEN
        assertNotNull(result);
        assertEquals(1609459200000L, result.getTime(), "The Date should match the epoch‑millis defined in the JSON example");
    }
}
