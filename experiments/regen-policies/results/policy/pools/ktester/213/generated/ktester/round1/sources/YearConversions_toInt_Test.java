package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import java.time.Year;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class YearConversions_toInt_Test {

    /**
     * Mocked Converter – the method under test should not call any of its methods.
     */
    @Mock
    private Converter mockConverter;

    @BeforeEach
    void setupBeforeEach() {
        // Mockito will initialise the @Mock fields automatically.
        // Ensure the mock is clean before each test.
        clearInvocations(mockConverter);
    }

    @AfterEach
    void teardownAfterEach() {
        // Verify that the mock was not used; the conversion logic does not depend on it.
        verifyNoInteractions(mockConverter);
    }

    /**
     * Provides a stream of valid Year instances together with their expected int values.
     */
    static Stream<org.junit.jupiter.params.provider.Arguments> validYearProvider() {
        return Stream.of(org.junit.jupiter.params.provider.Arguments.of(Year.of(0), 0), org.junit.jupiter.params.provider.Arguments.of(Year.of(-42), -42), org.junit.jupiter.params.provider.Arguments.of(Year.of(Year.MIN_VALUE), Year.MIN_VALUE), org.junit.jupiter.params.provider.Arguments.of(Year.of(Year.MAX_VALUE), Year.MAX_VALUE));
    }

    /**
     * Positive case – a valid {@link Year} returns its numeric value.
     */
    @Test
    void toInt_givenYear_returnsCorrectInt() {
        // given
        Year year = Year.of(2023);
        // when
        int result = YearConversions.toInt(year, mockConverter);
        // then
        assertEquals(2023, result, "The int value should match the Year value");
    }

    /**
     * Edge case – the minimum supported year (Year.MIN_VALUE) is returned correctly.
     */
    @Test
    void toInt_givenMinYear_returnsMinValue() {
        // given
        Year minYear = Year.of(Year.MIN_VALUE);
        // when
        int result = YearConversions.toInt(minYear, mockConverter);
        // then
        assertEquals(Year.MIN_VALUE, result);
    }

    /**
     * Edge case – the maximum supported year (Year.MAX_VALUE) is returned correctly.
     */
    @Test
    void toInt_givenMaxYear_returnsMaxValue() {
        // given
        Year maxYear = Year.of(Year.MAX_VALUE);
        // when
        int result = YearConversions.toInt(maxYear, mockConverter);
        // then
        assertEquals(Year.MAX_VALUE, result);
    }

    /**
     * Null source should result in a {@link NullPointerException} because the method casts without a check.
     */
    @Test
    void toInt_givenNull_throwsNullPointerException() {
        // given
        Object nullObj = null;
        // when / then
        assertThrows(NullPointerException.class, () -> YearConversions.toInt(nullObj, mockConverter));
    }

    /**
     * Passing an object that is not a {@link Year} should cause a {@link ClassCastException}.
     */
    @Test
    void toInt_givenNonYearObject_throwsClassCastException() {
        // given
        Object notAYear = "2023";
        // when / then
        assertThrows(ClassCastException.class, () -> YearConversions.toInt(notAYear, mockConverter));
    }

    /**
     * The {@link Converter} argument is ignored – providing a mock should not affect the result.
     */
    @Test
    void toInt_givenNonNullConverter_unused() {
        // given
        Year year = Year.of(1999);
        // when
        int result = YearConversions.toInt(year, mockConverter);
        // then
        assertEquals(1999, result);
    }

    /**
     * Even if the supplied Converter throws when any method is called, it must be ignored.
     */
    @Test
    void toInt_givenThrowingConverter_ignoresConverter() {
        // a Converter that throws on any interaction
        Converter throwingConverter = mock(Converter.class, invocation -> {
            throw new RuntimeException("Should not be called");
        });
        Year year = Year.of(2021);
        int result = YearConversions.toInt(year, throwingConverter);
        assertEquals(2021, result);
        // No verification on throwingConverter – it must simply not be used.
    }

    /**
     * Parameterized test covering a range of valid Year values (zero, negative, min, max).
     */
    @ParameterizedTest(name = "toInt({0}) = {1}")
    @MethodSource("validYearProvider")
    void toInt_givenVariousYear_returnsCorrectInt(Year year, int expected) {
        int result = YearConversions.toInt(year, mockConverter);
        assertEquals(expected, result);
    }

    /**
     * Ensure that calling toInt does not mutate the original Year instance (Year is immutable).
     */
    @Test
    void toInt_givenYearInstanceRemainsUnchanged() {
        Year original = Year.of(1984);
        // reference equality check
        Year beforeCall = original;
        int result = YearConversions.toInt(original, mockConverter);
        assertEquals(1984, result);
        assertSame(beforeCall, original, "The Year instance should remain the same (immutable).");
    }
}
