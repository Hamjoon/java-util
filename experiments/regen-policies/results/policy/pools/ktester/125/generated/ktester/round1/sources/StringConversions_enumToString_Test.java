package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_enumToString_Test {

    /**
     * A mock Converter – the method under test does not use it, but the signature requires it.
     */
    @Mock
    private Converter converter;

    private static Stream<Arguments> normalOperationProvider() {
        return Stream.of(// Standard enum constant, null converter
        Arguments.of(SampleEnum.FIRST, false, "FIRST"), // Standard enum constant, non‑null converter (mock)
        Arguments.of(SampleEnum.SECOND, true, "SECOND"), // Inner enum constant, null converter
        Arguments.of(InnerEnum.VALUE, false, "VALUE"));
    }

    private static Stream<Arguments> errorHandlingProvider() {
        return Stream.of(// Null 'from' with null converter
        Arguments.of(null, false, NullPointerException.class), // Null 'from' with non‑null converter
        Arguments.of(null, true, NullPointerException.class), // Non‑enum object (String) with null converter
        Arguments.of("someString", false, ClassCastException.class), // Non‑enum object (String) with non‑null converter
        Arguments.of("anotherString", true, ClassCastException.class));
    }

    @Test
    void enumToString_returnsEnumName() {
        // GIVEN
        TestEnum value = TestEnum.FIRST;
        // WHEN
        String result = StringConversions.enumToString(value, converter);
        // THEN
        assertEquals("FIRST", result);
    }

    @Test
    void enumToString_nullFrom_throwsNullPointerException() {
        // GIVEN
        Object nullValue = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> StringConversions.enumToString(nullValue, converter));
    }

    @Test
    void enumToString_nonEnum_throwsClassCastException() {
        // GIVEN
        Object notAnEnum = "I am not an enum";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> StringConversions.enumToString(notAnEnum, converter));
    }

    /**
     * Verifies that the method returns the enum constant name for
     * various enum values and both {@code null} and non‑null {@link Converter}
     * arguments.
     */
    @ParameterizedTest(name = "[{index}] enum={0}, useConverter={1} → expected={2}")
    @MethodSource("normalOperationProvider")
    void enumToString_normalOperation(Enum<?> enumValue, boolean useConverter, String expected) {
        // GIVEN
        Converter conv = useConverter ? converter : null;
        // WHEN
        String result = StringConversions.enumToString(enumValue, conv);
        // THEN
        assertEquals(expected, result);
    }

    /**
     * Confirms that the method throws the correct exception for invalid inputs:
     * <ul>
     *   <li>{@code null} {@code from} argument → {@link NullPointerException}</li>
     *   <li>Non‑enum {@code from} argument → {@link ClassCastException}</li>
     * </ul>
     * The converter may be {@code null} or a mock; it must not affect the outcome.
     */
    @ParameterizedTest(name = "[{index}] from={0}, useConverter={1} → throws={2}")
    @MethodSource("errorHandlingProvider")
    void enumToString_errorHandling(Object from, boolean useConverter, Class<? extends Throwable> expectedException) {
        // GIVEN
        Converter conv = useConverter ? converter : null;
        // WHEN / THEN
        assertThrows(expectedException, () -> StringConversions.enumToString(from, conv));
    }

    @Test
    void enumToString_returnsEnumName_direct() {
        // GIVEN
        SampleEnum value = SampleEnum.FIRST;
        // WHEN
        String result = StringConversions.enumToString(value, converter);
        // THEN
        assertEquals("FIRST", result);
    }
}
