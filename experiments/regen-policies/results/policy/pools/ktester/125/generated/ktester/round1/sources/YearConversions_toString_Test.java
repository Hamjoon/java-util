package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Year;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class YearConversions_toString_Test {

    /**
     * Mocked Converter – the method under test does not use it, but we verify no interaction.
     */
    private Converter mockConverter;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh mock for each test to guarantee isolation.
        mockConverter = mock(Converter.class);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the mock was not used – the method should ignore the Converter.
        verifyNoInteractions(mockConverter);
    }

    /**
     * Provides successful conversion scenarios.
     *
     * <p>Each argument consists of:
     * <ul>
     *   <li>{@link Year} instance to convert</li>
     *   <li>Expected string result</li>
     *   <li>Whether to pass a mock {@link Converter} (true) or {@code null} (false)</li>
     *   <li>Human‑readable description used as the test name</li>
     * </ul>
     */
    private static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(// regular positive year
        Arguments.of(Year.of(2023), "2023", false, "Valid Year instance should be converted to its ISO‑8601 string representation."), // negative year (BC)
        Arguments.of(Year.of(-5), "-0005", false, "Year supports negative values (BC). The method should correctly return the string representation including the sign."), // year zero (proleptic ISO year 0)
        Arguments.of(Year.of(0), "0000", false, "Year zero (the ISO proleptic year 0) should be formatted as \"0000\"."), // maximum 4‑digit positive year
        Arguments.of(Year.of(9999), "9999", false, "Maximum 4‑digit positive year should be formatted without leading zeros."), // year with more than four digits
        Arguments.of(Year.of(10000), "10000", false, "Year with more than four digits should be converted to its full numeric string."), // large negative year
        Arguments.of(Year.of(-9999), "-9999", false, "Large negative (BC) year should retain the leading minus sign."), // Year.MAX_VALUE (9‑digit year)
        Arguments.of(Year.of(Year.MAX_VALUE), "999999999", false, "Year.MAX_VALUE (9‑digit year) should be converted correctly."), // Year.MIN_VALUE (large negative year)
        Arguments.of(Year.of(Year.MIN_VALUE), "-999999999", false, "Year.MIN_VALUE (large negative year) should be converted with a leading minus."), // non‑null Converter – method must ignore it
        Arguments.of(Year.of(2023), "2023", true, "A non‑null Converter instance is supplied; method ignores it and still returns the correct string."));
    }

    /**
     * Provides scenarios that should raise an exception.
     *
     * <p>Each argument consists of:
     * <ul>
     *   <li>Input object for the {@code from} parameter</li>
     *   <li>Expected exception class</li>
     *   <li>Human‑readable description used as the test name</li>
     * </ul>
     */
    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(// null input – cast to Year triggers NPE
        Arguments.of(null, NullPointerException.class, "Passing a null 'from' argument causes a NullPointerException when the method attempts to cast to Year."), // non‑Year object – cast fails with ClassCastException
        Arguments.of("2023", ClassCastException.class, "If 'from' is not a Year (e.g., a String), the cast fails and a ClassCastException is thrown."));
    }

    @Test
    void toString_returnsYearString_whenGivenYearInstance() {
        // GIVEN
        Year year = Year.of(2023);
        // WHEN
        String result = YearConversions.toString(year, mockConverter);
        // THEN
        assertEquals("2023", result, "Year.toString() should return the ISO‑8601 year representation");
    }

    @Test
    void toString_throwsNullPointerException_whenGivenNull() {
        // GIVEN
        Object nullValue = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> YearConversions.toString(nullValue, mockConverter), "Passing null should result in a NullPointerException due to the cast to Year");
    }

    @Test
    void toString_throwsClassCastException_whenGivenNonYearObject() {
        // GIVEN
        Object notAYear = "2023";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> YearConversions.toString(notAYear, mockConverter), "Passing an object that is not a Year should cause a ClassCastException");
    }

    /**
     * Successful conversions – the method should return the ISO‑8601 string
     * representation of the supplied {@link Year}.  The {@code Converter}
     * argument may be {@code null} or a mock; it must be ignored.
     */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("successfulConversionProvider")
    void toString_returnsCorrectString_whenGivenYear(Year year, String expected, boolean useMockConverter, String description) {
        // GIVEN
        Converter converter = useMockConverter ? mockConverter : null;
        // WHEN
        String result = YearConversions.toString(year, converter);
        // THEN
        assertEquals(expected, result, description);
    }

    /**
     * Cases that must result in an exception.  The method casts the input to
     * {@link Year}, so a {@code null} value leads to {@link NullPointerException}
     * and a non‑{@code Year} value leads to {@link ClassCastException}.
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("exceptionProvider")
    void toString_throwsExpectedException(Object from, Class<? extends Throwable> expectedException, String description) {
        // WHEN / THEN
        assertThrows(expectedException, () -> YearConversions.toString(from, mockConverter), description);
    }
}
