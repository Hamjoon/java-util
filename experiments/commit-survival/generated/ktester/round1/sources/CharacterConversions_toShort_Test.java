package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CharacterConversions_toShort_Test {

    @Mock
    private Converter // injected by MockitoExtension
    mockConverter;

    @BeforeEach
    void setupBeforeEach() {
        // Ensure the mock has a clean state before each test.
        Mockito.reset(mockConverter);
    }

    private static Stream<Arguments> validCharacterConversions() {
        return Stream.of(// Simple ASCII character 'A' (code point 65)
        Arguments.of(Character.valueOf('A'), (short) 65, "Convert a simple ASCII character 'A' (char value 65) to short; result should be 65."), // Character with code point 0x8000 (32768) -> overflow to -32768
        Arguments.of(Character.valueOf('\u8000'), (short) -32768, "Convert the character with code point 32768 (0x8000) to short; casting to short yields -32768."), // Maximum char value 0xFFFF (65535) -> -1
        Arguments.of(Character.valueOf('\uFFFF'), (short) -1, "Convert the maximum char value 65535 (0xFFFF) to short; result should be -1 after overflow."), // Character holding 12345 (within signed short range)
        Arguments.of(Character.valueOf((char) 12345), (short) 12345, "Pass a Character object holding 12345 (within signed short range); result stays 12345."), // Character holding 40000 (>32767) -> overflow
        Arguments.of(Character.valueOf((char) 40000), (short) -25536, "Pass a Character object with value 40000 (>32767); casting to short wraps to -25536."));
    }

    private static Stream<Arguments> boundaryCharacterValues() {
        return Stream.of(// Null character (code point 0)
        Arguments.of(Character.valueOf('\u0000'), (short) 0, "Convert the null character (char value 0) to short; result should be 0."), // Highest positive char value 0x7FFF (32767)
        Arguments.of(Character.valueOf('\u7FFF'), (short) 32767, "Convert the highest positive char value (0x7FFF = 32767) to short; result stays 32767."), // Char value 0x8001 (32769) -> -32767
        Arguments.of(Character.valueOf('\u8001'), (short) -32767, "Convert char value 0x8001 (32769) which overflows signed short, yielding -32767."), // Explicit Character instance with 0x8001
        Arguments.of(new Character('\u8001'), (short) -32767, "Pass a Character object holding 0x8001; casting to char then to short produces -32767."), // Explicit Character instance with null character
        Arguments.of(new Character('\u0000'), (short) 0, "Pass a Character object holding the null character; result should be 0."));
    }

    private static Stream<Arguments> exceptionScenarios() {
        return Stream.of(// Null source
        Arguments.of(null, NullPointerException.class, "Calling toShort with a null source should throw NullPointerException because the cast to char dereferences null."), // Integer cannot be cast to char
        Arguments.of(Integer.valueOf(5), ClassCastException.class, "An Integer cannot be cast to char; the method should throw ClassCastException."), // String cannot be cast to char
        Arguments.of("A", ClassCastException.class, "A String is not a char/Character; casting to char results in ClassCastException."));
    }

    private static Stream<Arguments> nonCharacterNumericTypes() {
        return Stream.of(Arguments.of(Byte.valueOf((byte) 10), ClassCastException.class, "A Byte cannot be cast to char; method should throw ClassCastException."), Arguments.of(Short.valueOf((short) 100), ClassCastException.class, "A Short cannot be cast to char; expect ClassCastException."), Arguments.of(Long.valueOf(100L), ClassCastException.class, "A Long cannot be cast to char; method should throw ClassCastException."), Arguments.of(Float.valueOf(1.5f), ClassCastException.class, "A Float cannot be cast to char; expect ClassCastException."), Arguments.of(Double.valueOf(2.0), ClassCastException.class, "A Double cannot be cast to char; method should throw ClassCastException."));
    }

    private static Stream<Arguments> additionalClassCastScenarios() {
        return Stream.of(Arguments.of(new Object(), ClassCastException.class, "Passing a plain Object instance cannot be cast to char, should throw ClassCastException."), Arguments.of(new int[] { 1, 2 }, ClassCastException.class, "An int array is not a Character; casting to char triggers ClassCastException."), Arguments.of(new Date(), ClassCastException.class, "A Date object cannot be cast to char; expect ClassCastException."), Arguments.of(Boolean.TRUE, ClassCastException.class, "A Boolean value is not compatible with char casting; should throw ClassCastException."), Arguments.of(new StringBuilder("A"), ClassCastException.class, "StringBuilder does not implement Character; casting to char results in ClassCastException."), Arguments.of(new AtomicInteger(5), ClassCastException.class, "AtomicInteger is not a Character; casting to char should throw ClassCastException."));
    }

    @Test
    void toShort_WithCharacter_ReturnsCorrectShort() {
        // GIVEN
        // Unicode code point 90
        Character input = 'Z';
        short expected = (short) 'Z';
        // WHEN
        short result = CharacterConversions.toShort(input, mockConverter);
        // THEN
        assertEquals(expected, result, "The short value should match the char code point.");
        // Verify that the converter was never used.
        Mockito.verifyNoInteractions(mockConverter);
    }

    @Test
    void toShort_WithNull_ThrowsNullPointerException() {
        // GIVEN
        Object input = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> CharacterConversions.toShort(input, mockConverter), "Unboxing a null Character should throw NPE.");
        Mockito.verifyNoInteractions(mockConverter);
    }

    @Test
    void toShort_WithInvalidType_ThrowsClassCastException() {
        // GIVEN
        // String cannot be unboxed to char
        Object input = "A";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> CharacterConversions.toShort(input, mockConverter), "Casting a non‑Character object to char should throw CCE.");
        Mockito.verifyNoInteractions(mockConverter);
    }

    @Test
    void toShort_WithNumber_ThrowsClassCastException() {
        // GIVEN
        // Number is not a Character
        Object input = Integer.valueOf(123);
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> CharacterConversions.toShort(input, mockConverter), "Casting a Number to char should throw CCE.");
        Mockito.verifyNoInteractions(mockConverter);
    }

    /* --------------------------------------------------------------
     *  Valid character conversions (including overflow cases)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("validCharacterConversions")
    void toShort_ValidCharacterConversions(Object input, short expected, String description) {
        short result = CharacterConversions.toShort(input, mockConverter);
        assertEquals(expected, result, description);
        verifyNoInteractions(mockConverter);
    }

    /* --------------------------------------------------------------
     *  Boundary character values
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("boundaryCharacterValues")
    void toShort_BoundaryValues(Object input, short expected, String description) {
        short result = CharacterConversions.toShort(input, mockConverter);
        assertEquals(expected, result, description);
        verifyNoInteractions(mockConverter);
    }

    /* --------------------------------------------------------------
     *  Exception scenarios (null, wrong type, etc.)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("exceptionScenarios")
    void toShort_ExceptionScenarios(Object input, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> CharacterConversions.toShort(input, mockConverter), description);
        verifyNoInteractions(mockConverter);
    }

    /* --------------------------------------------------------------
     *  Non‑character numeric types (byte, short, long, float, double)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("nonCharacterNumericTypes")
    void toShort_NonCharacterNumericTypes(Object input, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> CharacterConversions.toShort(input, mockConverter), description);
        verifyNoInteractions(mockConverter);
    }

    /* --------------------------------------------------------------
     *  Additional ClassCastException scenarios (objects, arrays, etc.)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("additionalClassCastScenarios")
    void toShort_AdditionalClassCastScenarios(Object input, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> CharacterConversions.toShort(input, mockConverter), description);
        verifyNoInteractions(mockConverter);
    }

    /* --------------------------------------------------------------
     *  Converter parameter effect – ensure it is never used
     * -------------------------------------------------------------- */
    @Test
    void toShort_ConverterParameterIsIgnored() {
        // A mock that would throw if any method were invoked.
        Converter throwingConverter = mock(Converter.class, invocation -> {
            throw new IllegalStateException("Converter should not be used");
        });
        // Input is a normal character; conversion must succeed.
        // code point 90
        Character input = 'Z';
        short result = CharacterConversions.toShort(input, throwingConverter);
        assertEquals((short) 90, result, "Conversion should succeed and ignore the converter.");
        // Verify that no interaction with the throwing mock occurred.
        verifyNoInteractions(throwingConverter);
    }
}
