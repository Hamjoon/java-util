package com.cedarsoftware.util.convert;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class CharacterConversions_toAtomicInteger_Test {

    /**
     * A real {@code Converter} instance – not strictly required for the
     *  current implementation but useful for future‑proofing the test.
     */
    private static Converter realConverter;

    /**
     * Mocked {@code Converter} – demonstrates that the method does not
     *  interact with the converter at all.
     */
    @Mock
    private Converter mockConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // The real Converter needs a ConverterOptions instance.
        realConverter = new Converter(new ConverterOptions());
    }

    @BeforeEach
    void setupBeforeEach() {
        // Reset the mock before each test to avoid leftover interactions.
        Mockito.reset(mockConverter);
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Simple letters
        Arguments.of(Character.valueOf('A'), null, "Converts Character 'A' to its Unicode value 65", 65), // Null character
        Arguments.of(Character.valueOf('\u0000'), null, "Converts null character '\\u0000' to 0", 0), // Numeric character
        Arguments.of(Character.valueOf('9'), null, "Converts numeric character '9' to 57", 57), // Maximum char value
        Arguments.of(Character.valueOf('\uFFFF'), null, "Maximum char '\\uFFFF' should become 65535", 65535), // Line feed
        Arguments.of(Character.valueOf('\n'), null, "Line‑feed character '\\n' should become 10", 10), // Space
        Arguments.of(Character.valueOf(' '), null, "Space character should become 32", 32), // High‑surrogate
        Arguments.of(Character.valueOf('\uD800'), null, "High‑surrogate '\\uD800' should become 55296", 55296), // Low‑surrogate
        Arguments.of(Character.valueOf('\uDC00'), null, "Low‑surrogate '\\uDC00' should become 56320", 56320), // Another ordinary letter
        Arguments.of(Character.valueOf('Z'), new Converter(new ConverterOptions()), "Non‑null Converter is ignored; 'Z' becomes 90", 90));
    }

    private static Stream<Arguments> invalidConversionProvider() {
        return Stream.of(Arguments.of(Integer.valueOf(5), null, "Casting Integer to char should fail"), Arguments.of("B", null, "Casting String to char should fail"), Arguments.of(Byte.valueOf((byte) 1), null, "Casting Byte to char should fail"), Arguments.of(Short.valueOf((short) 2), null, "Casting Short to char should fail"), Arguments.of(Long.valueOf(3L), null, "Casting Long to char should fail"), Arguments.of(Float.valueOf(4.0f), null, "Casting Float to char should fail"), Arguments.of(Double.valueOf(5.0), null, "Casting Double to char should fail"), Arguments.of(Boolean.TRUE, null, "Casting Boolean to char should fail"));
    }

    @Test
    void toAtomicInteger_withCharacter_returnsCorrectAtomicInteger() {
        // given
        // Unicode 65
        Character source = 'A';
        // when
        AtomicInteger result = CharacterConversions.toAtomicInteger(source, realConverter);
        // then
        assertNotNull(result, "Result must not be null");
        assertEquals(65, result.get(), "AtomicInteger should contain the int value of the char");
    }

    @Test
    void toAtomicInteger_withPrimitiveCharWrappedInObject_returnsCorrectAtomicInteger() {
        // given
        // char value 0x1234 = 4660 decimal
        Object source = (char) 0x1234;
        // when
        AtomicInteger result = CharacterConversions.toAtomicInteger(source, mockConverter);
        // then
        assertEquals(0x1234, result.get());
        // Verify that the converter was never used.
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toAtomicInteger_withZeroChar_returnsZeroAtomicInteger() {
        // given
        Character source = '\u0000';
        // when
        AtomicInteger result = CharacterConversions.toAtomicInteger(source, realConverter);
        // then
        assertEquals(0, result.get());
    }

    @Test
    void toAtomicInteger_withNull_throwsNullPointerException() {
        // given
        Object source = null;
        // when / then
        assertThrows(NullPointerException.class, () -> CharacterConversions.toAtomicInteger(source, realConverter), "Casting null to char should throw NPE");
    }

    @Test
    void toAtomicInteger_withNonCharObject_throwsClassCastException() {
        // given
        Object source = "not a char";
        // when / then
        assertThrows(ClassCastException.class, () -> CharacterConversions.toAtomicInteger(source, mockConverter), "Casting a non‑character object to char should throw CCE");
    }

    /**
     * Valid character conversions.  The {@code converter} argument is supplied as {@code null}
     * to verify that the method truly ignores it.
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("validConversionProvider")
    void toAtomicInteger_validConversions(Object from, Converter converter, String description, int expected) {
        // when
        AtomicInteger result = CharacterConversions.toAtomicInteger(from, converter);
        // then
        assertNotNull(result, "Result must not be null");
        assertEquals(expected, result.get(), () -> "AtomicInteger should contain the Unicode code point of the supplied character");
    }

    /**
     * Invalid conversions where the source object cannot be cast to {@code char}.
     * The method is expected to throw {@link ClassCastException}.
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("invalidConversionProvider")
    void toAtomicInteger_invalidConversions(Object from, Converter converter, String description) {
        // when / then
        assertThrows(ClassCastException.class, () -> CharacterConversions.toAtomicInteger(from, converter), () -> "Expected ClassCastException for: " + description);
    }

    /**
     * Converting a {@code null} source should result in a {@link NullPointerException}
     * because the cast to {@code char} triggers unboxing of a {@code null} reference.
     */
    @Test
    void toAtomicInteger_nullSource_throwsNullPointerException() {
        // given
        Object source = null;
        // when / then
        assertThrows(NullPointerException.class, () -> CharacterConversions.toAtomicInteger(source, realConverter), "Casting null to char should throw NullPointerException");
    }

    @Test
    void toAtomicInteger_ignoresConverter_noInteraction() {
        // given
        Character source = 'X';
        // when
        AtomicInteger result = CharacterConversions.toAtomicInteger(source, mockConverter);
        // then
        assertEquals('X', result.get());
        // The method must not call any method on the converter.
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toAtomicInteger_twoCallsReturnDistinctInstances() {
        // given
        // Unicode 75
        Character source = 'K';
        // when
        AtomicInteger first = CharacterConversions.toAtomicInteger(source, realConverter);
        AtomicInteger second = CharacterConversions.toAtomicInteger(source, realConverter);
        // then
        assertAll(() -> assertEquals(75, first.get(), "First instance should contain 75"), () -> assertEquals(75, second.get(), "Second instance should contain 75"), () -> assertNotSame(first, second, "Each call must return a new AtomicInteger instance"));
    }
}
