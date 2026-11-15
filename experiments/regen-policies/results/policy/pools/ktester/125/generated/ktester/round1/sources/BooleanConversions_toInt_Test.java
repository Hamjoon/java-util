package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BooleanConversions_toInt_Test {

    /**
     * Shared Converter instance used by all tests.
     */
    private static Converter sharedConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter requires a ConverterOptions instance; use the default options.
        sharedConverter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // No external resources to clean up, but clear the reference for completeness.
        sharedConverter = null;
    }

    /**
     * Supplies all combinations of Boolean source values and Converter arguments
     * that are expected to succeed.  The expected result is expressed as an
     * {@link Integer} (the canonical constant from {@link CommonValues}).
     */
    private static Stream<Arguments> provideSuccessfulCases() {
        // A regular mock – does nothing special.
        Converter regularMock = mock(Converter.class);
        // A mock that throws on any method call.
        Converter throwingMock = mock(Converter.class, invocation -> {
            throw new RuntimeException("Converter should not be called");
        });
        return Stream.of(// true with null converter
        Arguments.of(Boolean.TRUE, null, CommonValues.INTEGER_ONE, "true + null converter"), // true with shared (real) converter
        Arguments.of(Boolean.TRUE, sharedConverter, CommonValues.INTEGER_ONE, "true + real converter"), // true with regular mock
        Arguments.of(Boolean.TRUE, regularMock, CommonValues.INTEGER_ONE, "true + regular mock"), // true with throwing mock
        Arguments.of(Boolean.TRUE, throwingMock, CommonValues.INTEGER_ONE, "true + throwing mock"), // false with null converter
        Arguments.of(Boolean.FALSE, null, CommonValues.INTEGER_ZERO, "false + null converter"), // false with shared (real) converter
        Arguments.of(Boolean.FALSE, sharedConverter, CommonValues.INTEGER_ZERO, "false + real converter"), // false with regular mock
        Arguments.of(Boolean.FALSE, regularMock, CommonValues.INTEGER_ZERO, "false + regular mock"), // false with throwing mock
        Arguments.of(Boolean.FALSE, throwingMock, CommonValues.INTEGER_ZERO, "false + throwing mock"), // Boolean.valueOf(true) (cached Boolean)
        Arguments.of(Boolean.valueOf(true), null, CommonValues.INTEGER_ONE, "Boolean.valueOf(true)"), // new Boolean(false) (non‑cached instance)
        Arguments.of(new Boolean(false), null, CommonValues.INTEGER_ZERO, "new Boolean(false)"));
    }

    @Test
    void toInt_givenTrueBoolean_returnsOne() {
        // GIVEN
        Boolean source = Boolean.TRUE;
        // WHEN
        Integer result = BooleanConversions.toInt(source, sharedConverter);
        // THEN
        assertEquals(CommonValues.INTEGER_ONE, result, "Converting Boolean.TRUE should yield Integer 1");
    }

    @Test
    void toInt_givenFalseBoolean_returnsZero() {
        // GIVEN
        Boolean source = Boolean.FALSE;
        // WHEN
        Integer result = BooleanConversions.toInt(source, sharedConverter);
        // THEN
        assertEquals(CommonValues.INTEGER_ZERO, result, "Converting Boolean.FALSE should yield Integer 0");
    }

    @Test
    void toInt_givenNull_throwsNullPointerException() {
        // GIVEN
        Object source = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> BooleanConversions.toInt(source, sharedConverter), "Passing null should result in NullPointerException because of the cast to Boolean");
    }

    @Test
    void toInt_givenNonBoolean_throwsClassCastException() {
        // GIVEN
        Object source = "not a boolean";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> BooleanConversions.toInt(source, sharedConverter), "Passing a non‑Boolean object should result in ClassCastException");
    }

    @ParameterizedTest(name = "toInt({0}, {3}) → {2}")
    @MethodSource("provideSuccessfulCases")
    void toInt_successfulScenarios(Object source, Converter converter, Integer expected, String description) {
        // WHEN
        Integer result = BooleanConversions.toInt(source, converter);
        // THEN
        assertEquals(expected, result, () -> "Unexpected result for case: " + description);
        // Verify that the returned instance is the exact constant from CommonValues
        assertSame(expected, result, () -> "Result should be the canonical Integer instance for case: " + description);
    }

    @Test
    void toInt_givenNullSource_throwsNullPointerException() {
        // GIVEN
        Object source = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> BooleanConversions.toInt(source, sharedConverter), "Casting null to Boolean should trigger NullPointerException");
    }

    @Test
    void toInt_givenNonBooleanSource_throwsClassCastException() {
        // GIVEN
        Object source = "not a boolean";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> BooleanConversions.toInt(source, sharedConverter), "Casting a non‑Boolean object should trigger ClassCastException");
    }

    @Test
    void toInt_converterArgumentIsNeverUsed() {
        // GIVEN
        Converter throwingConverter = mock(Converter.class, invocation -> {
            throw new RuntimeException("Converter method should not be invoked");
        });
        Boolean source = Boolean.TRUE;
        // WHEN
        Integer result = BooleanConversions.toInt(source, throwingConverter);
        // THEN
        assertEquals(CommonValues.INTEGER_ONE, result, "Result must be 1 even when a throwing Converter is supplied");
        // No verification needed – the mock would have thrown if any method was called.
    }

    @Test
    void toInt_returnsCanonicalIntegerInstances() {
        // GIVEN
        Boolean trueSource = Boolean.TRUE;
        Boolean falseSource = Boolean.FALSE;
        // WHEN
        Integer trueResult = BooleanConversions.toInt(trueSource, null);
        Integer falseResult = BooleanConversions.toInt(falseSource, null);
        // THEN
        assertSame(CommonValues.INTEGER_ONE, trueResult, "True conversion should return the shared INTEGER_ONE constant");
        assertSame(CommonValues.INTEGER_ZERO, falseResult, "False conversion should return the shared INTEGER_ZERO constant");
    }
}
