package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.time.DayOfWeek;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_enumToString_Test {

    /**
     * Mocked Converter – not used by the method under test but required by the signature.
     */
    @Mock
    private Converter converter;

    /**
     * Outer class containing a static inner enum.
     */
    private static class OuterClass {

        enum InnerEnum {

            INNER, OUTER
        }
    }

    /**
     * Supplies all successful conversion scenarios.
     */
    private static Stream<Arguments> successfulEnumProvider() {
        // The test class defines a static field `converter` that will be injected by Mockito.
        // However, static method sources cannot access instance fields, so we pass `null`
        // for most cases and rely on the test method to use the injected mock when needed.
        return Stream.of(// JDK enums
        Arguments.of(DayOfWeek.MONDAY, "MONDAY", "Standard enum conversion: DayOfWeek.MONDAY", null), Arguments.of(Month.DECEMBER, "DECEMBER", "Enum conversion for a different enum type (Month.DECEMBER)", null), Arguments.of(TimeUnit.SECONDS, "SECONDS", "Enum from java.util.concurrent package", null), // Additional JDK enums
        Arguments.of(ChronoUnit.NANOS, "NANOS", "Enum from java.time.temporal package", null), Arguments.of(Month.APRIL, "APRIL", "Another constant from java.time.Month", null), Arguments.of(DayOfWeek.SUNDAY, "SUNDAY", "Another constant from DayOfWeek", null), // Custom enum defined in this test class
        Arguments.of(TestEnumWithOverride.VALUE, "VALUE", "Custom enum that overrides toString() – should return enum name", null), // Enum with a non‑null Converter (the mock will be supplied by the test instance)
        Arguments.of(TestEnumWithConverter.TWO, "TWO", "Pass a non‑null Converter instance to confirm it is ignored", null), // Nested static inner enum
        Arguments.of(OuterClass.InnerEnum.INNER, "INNER", "Enum defined as a static inner class should be handled correctly", null));
    }

    /**
     * Supplies all scenarios that should result in an exception.
     */
    private static Stream<Arguments> exceptionEnumProvider() {
        return Stream.of(Arguments.of(null, NullPointerException.class, "Passing null as the source object should cause NullPointerException"), Arguments.of("NotAnEnum", ClassCastException.class, "Providing a non‑enum object (String) should trigger ClassCastException"), Arguments.of(new Object(), ClassCastException.class, "Supplying an arbitrary object that does not implement Enum should result in ClassCastException"), Arguments.of(Integer.valueOf(5), ClassCastException.class, "Passing a boxed primitive (Integer) should also cause ClassCastException"));
    }

    @Test
    void enumToStringReturnsEnumName() {
        // GIVEN
        TestEnum value = TestEnum.ONE;
        // WHEN
        String result = StringConversions.enumToString(value, converter);
        // THEN
        assertEquals("ONE", result, "enumToString should return the enum constant name");
    }

    @Test
    void enumToStringHandlesDifferentEnumConstant() {
        // GIVEN
        TestEnum value = TestEnum.TWO;
        // WHEN
        String result = StringConversions.enumToString(value, converter);
        // THEN
        assertEquals("TWO", result);
    }

    @Test
    void enumToStringThrowsClassCastExceptionWhenNotEnum() {
        // GIVEN
        Object notAnEnum = "I am not an enum";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> StringConversions.enumToString(notAnEnum, converter), "Passing a non‑enum should result in ClassCastException");
    }

    @Test
    void enumToStringThrowsNullPointerExceptionWhenNullArgument() {
        // GIVEN
        Object nullValue = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> StringConversions.enumToString(nullValue, converter), "Passing null should result in NullPointerException");
    }

    /**
     * Verifies that {@code enumToString} returns the exact {@code name()} of a wide
     * range of enum constants, regardless of the enum type or its package.
     *
     * The {@code converter} argument is deliberately varied (null or a mock) to
     * confirm that the method completely ignores it.
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("successfulEnumProvider")
    void enumToStringReturnsCorrectName(Object from, String expected, String description, Converter suppliedConverter) {
        // WHEN
        String result = StringConversions.enumToString(from, suppliedConverter);
        // THEN
        assertEquals(expected, result, description);
    }

    /**
     * Confirms that {@code enumToString} throws the appropriate exception
     * when the supplied {@code from} argument is invalid.
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("exceptionEnumProvider")
    void enumToStringThrowsExpectedException(Object from, Class<? extends Throwable> expectedException, String description) {
        // WHEN / THEN
        assertThrows(expectedException, () -> StringConversions.enumToString(from, null), description);
    }
}
