package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CharacterConversions_toDouble_Test {

    @Mock
    private Converter // not used by the method under test
    mockConverter;

    /**
     * A mock that throws on any method call – used to prove the method truly ignores the converter.
     */
    private static final Converter throwingConverter = mock(Converter.class, invocation -> {
        throw new RuntimeException("Converter method should not be called");
    });

    private static Stream<Arguments> provideSuccessfulConversions() {
        return Stream.of(// basic cases
        arguments(Character.valueOf('A'), null, 65.0, "Valid Character instance 'A'"), arguments(Character.valueOf('\u0000'), null, 0.0, "Character with value zero"), arguments(Character.valueOf('z'), null, 122.0, "Valid Character instance 'z' with null converter"), arguments(Character.valueOf('z'), mock(Converter.class), 122.0, "Valid Character instance 'z' with a non‑null (mock) converter"), // additional and boundary cases
        arguments(Character.valueOf('\uFFFF'), null, 65535.0, "Maximum char value (U+FFFF)"), arguments('b', null, 98.0, "Primitive char literal 'b' (autoboxed)"), arguments(Character.valueOf('\uD800'), null, 55296.0, "High‑surrogate char (U+D800)"), arguments(Character.valueOf('\uDC00'), null, 56320.0, "Low‑surrogate char (U+DC00)"), // case with a converter that would throw if used
        arguments(Character.valueOf('C'), throwingConverter, 67.0, "Valid Character with a throwing Converter – should be ignored"));
    }

    private static Stream<Arguments> provideExceptionConversions() {
        return Stream.of(arguments(null, null, NullPointerException.class, "Null source object should cause NPE"), arguments(Integer.valueOf(65), null, ClassCastException.class, "Integer cannot be cast to char"), arguments("A", null, ClassCastException.class, "String cannot be cast to char"), arguments(Byte.valueOf((byte) 65), null, ClassCastException.class, "Byte wrapper cannot be cast to char"), arguments(Short.valueOf((short) 65), null, ClassCastException.class, "Short wrapper cannot be cast to char"), arguments(Long.valueOf(65L), null, ClassCastException.class, "Long wrapper cannot be cast to char"), arguments(Double.valueOf(65.0), null, ClassCastException.class, "Double wrapper cannot be cast to char"));
    }

    @Test
    void toDouble_WithCharacter_ReturnsCorrectDouble() {
        // given
        // Unicode value 65
        Character input = 'A';
        double expected = (double) 'A';
        // when
        double result = CharacterConversions.toDouble(input, mockConverter);
        // then
        assertEquals(expected, result, "The double value should match the char's numeric value");
        // Verify that the converter was never touched.
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toDouble_WithNull_ThrowsNullPointerException() {
        // given
        Object input = null;
        // when / then
        assertThrows(NullPointerException.class, () -> CharacterConversions.toDouble(input, mockConverter), "Casting null to char should throw NPE");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toDouble_WithInvalidType_ThrowsClassCastException() {
        // given
        // Not a Character, cannot be cast to char
        Integer input = 123;
        // when / then
        assertThrows(ClassCastException.class, () -> CharacterConversions.toDouble(input, mockConverter), "Casting a non‑Character to char should throw CCE");
        verifyNoInteractions(mockConverter);
    }

    /* -------------------------------------------------
     *  Successful conversion test cases (expected double)
     * ------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("provideSuccessfulConversions")
    void toDouble_ReturnsExpectedDouble(Object from, Converter converter, double expected, String description) {
        double result = CharacterConversions.toDouble(from, converter);
        assertEquals(expected, result, 0.0, description);
        // Verify that the converter (if it is a Mockito mock) was never touched.
        if (converter != null && mockingDetails(converter).isMock()) {
            verifyNoInteractions(converter);
        }
    }

    /* -------------------------------------------------
     *  Exception test cases (expected to throw)
     * ------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("provideExceptionConversions")
    void toDouble_ThrowsExpectedException(Object from, Converter converter, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> CharacterConversions.toDouble(from, converter), description);
        // Verify that the converter (if it is a Mockito mock) was never touched.
        if (converter != null && mockingDetails(converter).isMock()) {
            verifyNoInteractions(converter);
        }
    }
}
