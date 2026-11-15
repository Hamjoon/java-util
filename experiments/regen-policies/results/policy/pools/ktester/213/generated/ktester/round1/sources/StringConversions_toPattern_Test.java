package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class StringConversions_toPattern_Test{

    /** The mock Converter is not used by {@code StringConversions.toPattern},
     *  but it is required by the method signature. */
    @Mock
    private Converter mockConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Mockito creates the mock for us; nothing else to initialise.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    @Test
    void toPattern_compilesSimpleRegex() {
        // GIVEN
        String regex = "a*b+";
        // WHEN
        Pattern result = StringConversions.toPattern(regex, mockConverter);
        // THEN
        assertNotNull(result, "Resulting pattern should not be null");
        assertEquals(regex, result.pattern(), "Pattern string should match the original regex");
    }

    @Test
    void toPattern_trimsInputBeforeCompiling() {
        // GIVEN
        String regexWithSpaces = "   \\d{3}-\\d{2}-\\d{4}   ";
        String expected = "\\d{3}-\\d{2}-\\d{4}";
        // WHEN
        Pattern result = StringConversions.toPattern(regexWithSpaces, mockConverter);
        // THEN
        assertEquals(expected, result.pattern(), "Pattern should be compiled from the trimmed string");
    }

    @Test
    void toPattern_handlesEmptyString() {
        // GIVEN
        String empty = "";
        // WHEN
        Pattern result = StringConversions.toPattern(empty, mockConverter);
        // THEN
        assertEquals("", result.pattern(), "Empty string should produce a pattern that matches only the empty input");
        assertTrue(result.matcher("").matches(), "Pattern should match an empty CharSequence");
    }

    @Test
    void toPattern_nullInputThrowsNPE() {
        // GIVEN
        Object nullObj = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class,
                () -> StringConversions.toPattern(nullObj, mockConverter),
                "Calling toPattern with a null source should throw NullPointerException");
    }
}