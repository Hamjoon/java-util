package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CharacterConversions_toAtomicLong_Test {

    /**
     * Mocked Converter – not used by the method but required for the signature.
     */
    @Mock
    private Converter mockConverter;

    @BeforeEach
    void setupBeforeEach() {
        // MockitoExtension already initialises @Mock fields.
        // Ensure the mock has no previous interactions.
        clearInvocations(mockConverter);
    }

    @AfterEach
    void teardownAfterEach() {
        // Verify that the method under test does not touch the Converter mock.
        verifyNoInteractions(mockConverter);
    }

    /**
     * Supplies successful conversion scenarios.
     */
    private static Stream<Arguments> successfulConversionProvider() {
        // Normal ASCII character
        Arguments a = Arguments.of(Character.valueOf('A'), null, 65L);
        // Greek capital omega (U+03A9)
        Arguments omega = Arguments.of(Character.valueOf('\u03A9'), null, 937L);
        // Whitespace
        Arguments space = Arguments.of(Character.valueOf(' '), null, 32L);
        // Lowercase z
        Arguments z = Arguments.of(Character.valueOf('z'), null, 122L);
        // Character.MIN_VALUE (U+0000)
        Arguments min = Arguments.of(Character.valueOf('\u0000'), null, 0L);
        // Character.MAX_VALUE (U+FFFF)
        Arguments max = Arguments.of(Character.valueOf('\uFFFF'), null, 65535L);
        // High‑surrogate (U+D800)
        Arguments highSurrogate = Arguments.of(Character.valueOf('\uD800'), null, 55296L);
        // Low‑surrogate (U+DC00)
        Arguments lowSurrogate = Arguments.of(Character.valueOf('\uDC00'), null, 56320L);
        // Numeric character '5'
        Arguments five = Arguments.of(Character.valueOf('5'), null, 53L);
        // Using a non‑null Converter that should be ignored
        Converter dummyConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> dest) {
                // Should never be called
                throw new IllegalStateException("Converter should not be invoked");
            }
        };
        Arguments withConverter = Arguments.of(Character.valueOf('M'), dummyConverter, 77L);
        // Another dummy converter (no‑op) – also ignored
        Converter noopConverter = new Converter() {
        };
        Arguments withNoop = Arguments.of(Character.valueOf('z'), noopConverter, 122L);
        return Stream.of(a, omega, space, z, min, max, highSurrogate, lowSurrogate, five, withConverter, withNoop);
    }

    /**
     * Verify that a {@link Character} is correctly converted to an {@link AtomicLong}
     * containing the character's Unicode code point.
     */
    @Test
    void toAtomicLong_givenCharacter_returnsAtomicLongWithCharValue() {
        // GIVEN
        // Unicode value 65
        Character input = 'A';
        // WHEN
        AtomicLong result = CharacterConversions.toAtomicLong(input, mockConverter);
        // THEN
        assertNotNull(result, "Result should not be null");
        assertEquals(65L, result.get(), "AtomicLong should contain the char's numeric value");
    }

    /**
     * Verify conversion for a different character, e.g., a whitespace.
     */
    @Test
    void toAtomicLong_givenWhitespaceCharacter_returnsCorrectValue() {
        // GIVEN
        // Unicode value 32
        Character input = ' ';
        // WHEN
        AtomicLong result = CharacterConversions.toAtomicLong(input, mockConverter);
        // THEN
        assertNotNull(result);
        assertEquals(32L, result.get());
    }

    /**
     * Verify that passing a {@link java.lang.Character} wrapper works,
     * and that the method does not attempt to use the Converter.
     */
    @Test
    void toAtomicLong_givenCharacterWrapper_isHandled() {
        // GIVEN
        // Unicode 122
        Object input = Character.valueOf('z');
        // WHEN
        AtomicLong result = CharacterConversions.toAtomicLong(input, mockConverter);
        // THEN
        assertEquals(122L, result.get());
    }

    /* -----------------------------------------------------------
     *  Successful conversion cases – parameterized
     * ----------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0} expected={2}")
    @MethodSource("successfulConversionProvider")
    void toAtomicLong_successfulConversions(Object from, Converter converter, long expected) {
        // WHEN
        AtomicLong result = CharacterConversions.toAtomicLong(from, converter);
        // THEN
        assertNotNull(result, "Result must not be null");
        assertEquals(expected, result.get(), () -> "AtomicLong should contain the numeric value of the character");
    }

    @Test
    void toAtomicLong_givenInteger_throwsClassCastException() {
        // GIVEN
        Object input = Integer.valueOf(123);
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> CharacterConversions.toAtomicLong(input, mockConverter), "Casting a non‑Character to char should throw ClassCastException");
    }

    @Test
    void toAtomicLong_givenString_throwsClassCastException() {
        // GIVEN
        // "A"
        Object input = String.valueOf('A');
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> CharacterConversions.toAtomicLong(input, mockConverter), "Casting a String to char should throw ClassCastException");
    }

    @Test
    void toAtomicLong_givenNull_throwsNullPointerException() {
        // GIVEN
        Object input = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> CharacterConversions.toAtomicLong(input, mockConverter), "Passing null should result in NullPointerException due to unboxing");
    }

    @Test
    void toAtomicLong_givenConverterThatThrows_isIgnored() {
        // GIVEN
        // 77
        Character input = 'M';
        Converter throwingConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> dest) {
                throw new RuntimeException("Should not be called");
            }
        };
        // WHEN
        AtomicLong result = CharacterConversions.toAtomicLong(input, throwingConverter);
        // THEN
        assertNotNull(result);
        assertEquals(77L, result.get(), "Method should ignore the supplied Converter even if it throws");
    }
}
