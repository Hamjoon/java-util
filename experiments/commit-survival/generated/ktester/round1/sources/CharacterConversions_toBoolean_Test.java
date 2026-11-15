package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link CharacterConversions#toBoolean(Object, Converter)}.
 *
 * The method simply casts the {@code from} argument to {@code char} and checks
 * whether the character represents a truthy value ( {@code 1}, {@code 't'},
 * {@code 'T'}, {@code '1'}, {@code 'y'}, {@code 'Y'} ).
 *
 * The {@link Converter} argument is not used by the implementation, but a mock
 * is supplied to satisfy the method signature and to illustrate typical usage.
 */
@ExtendWith(MockitoExtension.class)
class CharacterConversions_toBoolean_Test{

    @Mock
    private Converter mockConverter;   // not used by the method under test

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Mockito will initialise @Mock fields automatically.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /* -------------------------------------------------------------
       Positive (truthy) cases
       ------------------------------------------------------------- */

    @Test
    void toBoolean_returnsTrue_forCharOne() {
        // given
        Character from = (char) 1;   // literal char with value 1

        // when
        boolean result = CharacterConversions.toBoolean(from, mockConverter);

        // then
        assertTrue(result, "Character with value 1 should be true");
    }

    @Test
    void toBoolean_returnsTrue_forLowerCaseT() {
        // given
        Character from = 't';

        // when
        boolean result = CharacterConversions.toBoolean(from, mockConverter);

        // then
        assertTrue(result);
    }

    @Test
    void toBoolean_returnsTrue_forUpperCaseT() {
        // given
        Character from = 'T';

        // when
        boolean result = CharacterConversions.toBoolean(from, mockConverter);

        // then
        assertTrue(result);
    }

    @Test
    void toBoolean_returnsTrue_forDigitOne() {
        // given
        Character from = '1';

        // when
        boolean result = CharacterConversions.toBoolean(from, mockConverter);

        // then
        assertTrue(result);
    }

    @Test
    void toBoolean_returnsTrue_forLowerCaseY() {
        // given
        Character from = 'y';

        // when
        boolean result = CharacterConversions.toBoolean(from, mockConverter);

        // then
        assertTrue(result);
    }

    @Test
    void toBoolean_returnsTrue_forUpperCaseY() {
        // given
        Character from = 'Y';

        // when
        boolean result = CharacterConversions.toBoolean(from, mockConverter);

        // then
        assertTrue(result);
    }

    /* -------------------------------------------------------------
       Negative (falsy) cases
       ------------------------------------------------------------- */

    @Test
    void toBoolean_returnsFalse_forOtherCharacters() {
        // given a set of characters that are not considered truthy
        char[] falsy = {'0', '2', 'a', 'b', 'Z', ' ', '\n', 'f', 'F'};
        for (char c : falsy) {
            // when
            boolean result = CharacterConversions.toBoolean(c, mockConverter);
            // then
            assertFalse(result, () -> "Character '" + c + "' should be false");
        }
    }

    @Test
    void toBoolean_throwsClassCastException_whenNotACharacter() {
        // The implementation casts the object to char directly, so a non‑character
        // argument will cause a ClassCastException.
        Object notAChar = "string";

        assertThrows(ClassCastException.class,
                () -> CharacterConversions.toBoolean(notAChar, mockConverter));
    }
}