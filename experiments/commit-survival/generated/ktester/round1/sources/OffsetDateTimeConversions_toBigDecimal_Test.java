package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toBigDecimal_Test {

    private Converter mockConverter;

    private MockedStatic<InstantConversions> instantConversionsMock;

    @BeforeEach
    void setupBeforeEach() {
        // Create a mock Converter – its internal behaviour is irrelevant for this test
        mockConverter = mock(Converter.class);
        // Mock the static InstantConversions class
        instantConversionsMock = Mockito.mockStatic(InstantConversions.class);
    }

    @AfterEach
    void teardownAfterEach() {
        // Release the static mock
        instantConversionsMock.close();
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Valid Conversion – UTC
        Arguments.of(OffsetDateTime.of(2023, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC), BigDecimal.valueOf(1672574400L), "A normal OffsetDateTime (UTC) should convert to epoch seconds"), // Offset Handling – -05:00
        Arguments.of(OffsetDateTime.of(2023, 1, 1, 12, 0, 0, 0, ZoneOffset.ofHours(-5)), BigDecimal.valueOf(1672592400L), "OffsetDateTime with -05:00 offset should be converted to correct UTC instant"), // Fractional Seconds
        Arguments.of(OffsetDateTime.of(2023, 1, 1, 12, 0, 0, 123_456_789, ZoneOffset.UTC), new BigDecimal("1672574400.123456789"), "OffsetDateTime with nanoseconds should retain fractional part"), // Extreme Future Date
        Arguments.of(OffsetDateTime.of(3000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), BigDecimal.valueOf(32503636800L), "A far‑future OffsetDateTime should be converted without overflow"));
    }

    @Test
    void testToBigDecimalDelegatesToInstantConversions() {
        // GIVEN
        OffsetDateTime offsetDateTime = OffsetDateTime.parse("2023-01-02T10:15:30+02:00");
        Instant expectedInstant = offsetDateTime.toInstant();
        BigDecimal expectedResult = new BigDecimal("123.45");
        // Mock the static call that the method under test delegates to
        instantConversionsMock.when(() -> InstantConversions.toBigDecimal(expectedInstant, mockConverter)).thenReturn(expectedResult);
        // WHEN
        BigDecimal actualResult = OffsetDateTimeConversions.toBigDecimal(offsetDateTime, mockConverter);
        // THEN
        assertEquals(expectedResult, actualResult, "The returned BigDecimal should be the one provided by InstantConversions");
        instantConversionsMock.verify(() -> InstantConversions.toBigDecimal(expectedInstant, mockConverter), times(1));
    }

    @Test
    void testToBigDecimalWithInvalidInputThrowsClassCastException() {
        // GIVEN
        Object notAnOffsetDateTime = "I am not an OffsetDateTime";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> OffsetDateTimeConversions.toBigDecimal(notAnOffsetDateTime, mockConverter), "Passing a non‑OffsetDateTime object should result in a ClassCastException");
    }

    @ParameterizedTest(name = "{index} => {2}")
    @MethodSource("validConversionProvider")
    void testValidConversions(OffsetDateTime input, BigDecimal expected, String description) {
        // GIVEN
        Instant instant = input.toInstant();
        instantConversionsMock.when(() -> InstantConversions.toBigDecimal(instant, mockConverter)).thenReturn(expected);
        // WHEN
        BigDecimal actual = OffsetDateTimeConversions.toBigDecimal(input, mockConverter);
        // THEN
        assertEquals(expected, actual, description);
        instantConversionsMock.verify(() -> InstantConversions.toBigDecimal(instant, mockConverter), times(1));
    }

    @Test
    void testNullInputThrowsNullPointerException() {
        // GIVEN
        Object nullInput = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toBigDecimal(nullInput, mockConverter), "Passing a null OffsetDateTime should result in NullPointerException");
    }

    @Test
    void testInvalidTypeThrowsClassCastException() {
        // GIVEN
        Object notOffsetDateTime = "2023-01-01T12:00Z";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> OffsetDateTimeConversions.toBigDecimal(notOffsetDateTime, mockConverter), "Providing a value that is not an OffsetDateTime should cause ClassCastException");
    }

    @Test
    void testOutOfInstantRangeThrowsDateTimeException() {
        // GIVEN
        OffsetDateTime outOfRange = OffsetDateTime.of(1_000_000_000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        // WHEN / THEN
        assertThrows(DateTimeException.class, () -> OffsetDateTimeConversions.toBigDecimal(outOfRange, mockConverter), "OffsetDateTime whose instant is outside Instant range should throw DateTimeException");
    }

    @Test
    void testCustomConverterOverrideReturnsConstant() {
        // GIVEN
        OffsetDateTime odt = OffsetDateTime.of(2023, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);
        Converter constantConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                // ignore the source and always return TEN
                return BigDecimal.TEN;
            }
        };
        Instant instant = odt.toInstant();
        // Mock the static call to return whatever the converter would have produced
        instantConversionsMock.when(() -> InstantConversions.toBigDecimal(instant, constantConverter)).thenReturn(BigDecimal.TEN);
        // WHEN
        BigDecimal result = OffsetDateTimeConversions.toBigDecimal(odt, constantConverter);
        // THEN
        assertEquals(BigDecimal.TEN, result, "Method should return the constant supplied by the custom converter");
        instantConversionsMock.verify(() -> InstantConversions.toBigDecimal(instant, constantConverter), times(1));
    }

    @Test
    void testNullConverterPropagatesNullPointerException() {
        // GIVEN
        OffsetDateTime odt = OffsetDateTime.of(2023, 5, 15, 10, 30, 0, 0, ZoneOffset.UTC);
        Converter nullConverter = null;
        Instant instant = odt.toInstant();
        // The static method is expected to throw NPE when the converter argument is null
        instantConversionsMock.when(() -> InstantConversions.toBigDecimal(instant, nullConverter)).thenThrow(new NullPointerException("converter is null"));
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toBigDecimal(odt, nullConverter), "A null Converter should cause a NullPointerException from InstantConversions");
        instantConversionsMock.verify(() -> InstantConversions.toBigDecimal(instant, nullConverter), times(1));
    }

    @Test
    void testConverterThrowsExceptionIsPropagated() {
        // GIVEN
        OffsetDateTime odt = OffsetDateTime.of(2023, 3, 10, 8, 45, 0, 0, ZoneOffset.UTC);
        Converter explodingConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                throw new IllegalArgumentException("converter error");
            }
        };
        Instant instant = odt.toInstant();
        // Mock static method to re‑throw the exception from the converter
        instantConversionsMock.when(() -> InstantConversions.toBigDecimal(instant, explodingConverter)).thenThrow(new IllegalArgumentException("converter error"));
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toBigDecimal(odt, explodingConverter), "Exception from the supplied Converter should propagate");
        assertEquals("converter error", ex.getMessage());
        instantConversionsMock.verify(() -> InstantConversions.toBigDecimal(instant, explodingConverter), times(1));
    }

    @Test
    void testConverterReturnsIncompatibleTypeCausesClassCastException() {
        // GIVEN
        OffsetDateTime odt = OffsetDateTime.of(2022, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC);
        Converter badConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                return "not a BigDecimal";
            }
        };
        Instant instant = odt.toInstant();
        // Force the static method to return a String cast to BigDecimal (unchecked)
        instantConversionsMock.when(() -> InstantConversions.toBigDecimal(instant, badConverter)).thenAnswer(invocation -> (BigDecimal) (Object) "not a BigDecimal");
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> OffsetDateTimeConversions.toBigDecimal(odt, badConverter), "Returning a non‑BigDecimal from InstantConversions should cause ClassCastException");
        instantConversionsMock.verify(() -> InstantConversions.toBigDecimal(instant, badConverter), times(1));
    }
}
