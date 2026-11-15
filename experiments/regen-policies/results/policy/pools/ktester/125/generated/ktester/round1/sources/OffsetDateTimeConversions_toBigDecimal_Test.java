package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
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

    /**
     * Mock for the static methods of {@link InstantConversions}.
     */
    private MockedStatic<InstantConversions> instantConversionsMock;

    /**
     * A real {@link Converter} instance – the class under test only forwards the call,
     *  it does not depend on any internal state of the converter for this method.
     */
    private Converter converter;

    /**
     * A regular {@link Converter} instance – the method under test does not inspect it.
     */
    private Converter defaultConverter;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise the static mock before each test.
        instantConversionsMock = mockStatic(InstantConversions.class);
        // Create a default Converter (uses default options).
        converter = new Converter(new ConverterOptions());
        instantConversionsMock = mockStatic(InstantConversions.class);
        defaultConverter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Release the static mock to avoid interference with other tests.
        instantConversionsMock.close();
        instantConversionsMock.close();
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// epoch zero
        Arguments.of("Valid conversion – epoch zero", OffsetDateTime.parse("1970-01-01T00:00:00Z"), BigDecimal.ZERO), // non‑zero instant with fractional seconds
        Arguments.of("Valid conversion – non‑zero instant", OffsetDateTime.parse("2023-03-15T12:34:56.789+02:00"), new BigDecimal("1678882496.789")), // zero nanos (integer seconds)
        Arguments.of("Valid conversion – zero nanos (integer seconds)", OffsetDateTime.parse("2022-01-01T00:00:00+00:00"), new BigDecimal("1640995200")));
    }

    @Test
    void testToBigDecimalDelegatesToInstantConversions() {
        // GIVEN – a deterministic OffsetDateTime and the expected BigDecimal result.
        Instant instant = Instant.parse("2023-01-01T12:34:56Z");
        OffsetDateTime offsetDateTime = OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
        BigDecimal expected = new BigDecimal("12345.6789");
        // WHEN – the static method is called, return the expected value.
        instantConversionsMock.when(() -> InstantConversions.toBigDecimal(instant, converter)).thenReturn(expected);
        // ACT – invoke the method under test.
        BigDecimal actual = OffsetDateTimeConversions.toBigDecimal(offsetDateTime, converter);
        // THEN – verify that the result matches the mocked value and the static method was invoked once.
        assertEquals(expected, actual, "The returned BigDecimal should be the one provided by InstantConversions");
        instantConversionsMock.verify(() -> InstantConversions.toBigDecimal(instant, converter), times(1));
    }

    /* --------------------------------------------------------------
     *  Valid conversions – parameterized tests
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} ⇒ {0}")
    @MethodSource("validConversionProvider")
    void testValidConversions(String description, OffsetDateTime input, BigDecimal expectedResult) {
        // Arrange – mock the static delegation to return the expected value.
        Instant instant = input.toInstant();
        instantConversionsMock.when(() -> InstantConversions.toBigDecimal(instant, defaultConverter)).thenReturn(expectedResult);
        // Act
        BigDecimal actual = OffsetDateTimeConversions.toBigDecimal(input, defaultConverter);
        // Assert
        assertEquals(expectedResult, actual, "The returned BigDecimal must match the value supplied by the mocked InstantConversions");
        instantConversionsMock.verify(() -> InstantConversions.toBigDecimal(instant, defaultConverter), times(1));
    }

    @Test
    void testNullSourceThrowsNullPointerException() {
        // The cast to OffsetDateTime triggers NPE before any static call.
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toBigDecimal(null, defaultConverter), "Passing a null 'from' argument must result in NullPointerException");
        // No interaction with InstantConversions should have occurred.
        instantConversionsMock.verifyNoInteractions();
    }

    @Test
    void testWrongTypeThrowsClassCastException() {
        // Passing a String instead of OffsetDateTime leads to ClassCastException.
        assertThrows(ClassCastException.class, () -> OffsetDateTimeConversions.toBigDecimal("2023-01-01T00:00:00Z", defaultConverter), "A non‑OffsetDateTime source must cause ClassCastException");
        instantConversionsMock.verifyNoInteractions();
    }

    @Test
    void testNullConverterPropagatesNullPointerExceptionFromInstantConversions() {
        OffsetDateTime odt = OffsetDateTime.now();
        Instant instant = odt.toInstant();
        // Mock the static method to throw NPE when the converter argument is null.
        instantConversionsMock.when(() -> InstantConversions.toBigDecimal(instant, null)).thenThrow(new NullPointerException("converter is null"));
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toBigDecimal(odt, null), "A null converter should cause NullPointerException from the delegated call");
        instantConversionsMock.verify(() -> InstantConversions.toBigDecimal(instant, null), times(1));
    }

    @Test
    void testOffsetDateTimeOutOfInstantRangeThrowsDateTimeException() {
        // This date is far beyond the epoch range supported by Instant.
        OffsetDateTime outOfRange = OffsetDateTime.of(1_000_000_000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        assertThrows(DateTimeException.class, () -> OffsetDateTimeConversions.toBigDecimal(outOfRange, defaultConverter), "OffsetDateTime values outside Instant range must propagate DateTimeException");
        // No call to InstantConversions should happen because the exception occurs earlier.
        instantConversionsMock.verifyNoInteractions();
    }

    @Test
    void testConverterThrowsExceptionPropagatesFromInstantConversions() {
        OffsetDateTime odt = OffsetDateTime.now();
        Instant instant = odt.toInstant();
        // Create a dummy converter that we will pass through; its behaviour is irrelevant
        // because we mock the static method to throw.
        Converter throwingConverter = mock(Converter.class);
        // Mock the static method to throw IllegalArgumentException when invoked with this converter.
        instantConversionsMock.when(() -> InstantConversions.toBigDecimal(instant, throwingConverter)).thenThrow(new IllegalArgumentException("converter failure"));
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toBigDecimal(odt, throwingConverter), "Exception thrown by the converter (via InstantConversions) must propagate");
        instantConversionsMock.verify(() -> InstantConversions.toBigDecimal(instant, throwingConverter), times(1));
    }
}
