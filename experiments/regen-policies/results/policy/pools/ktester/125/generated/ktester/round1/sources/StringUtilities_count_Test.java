package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link StringUtilities#count(String, char)}.
 * <p>
 * The test suite follows the provided test‑class framework and
 * covers:
 * <ul>
 *   <li>normal counting scenarios (including Unicode, whitespace,
 *       newline, carriage‑return, etc.)</li>
 *   <li>edge cases such as empty source strings and very long strings</li>
 *   <li>exceptional situations (null source string and static field mutation)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class StringUtilities_count_Test {

    /* --------------------------------------------------------------------- */
    /*  Lifecycle hooks – no special resources are required for these tests */
    /* --------------------------------------------------------------------- */
    @BeforeAll
    static void setupBeforeAll() {
        // No global initialization needed
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialization needed
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the mutable static field is restored after each test
        StringUtilities.EMPTY = "";
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup needed
    }

    /* --------------------------------------------------------------------- */
    /*  Normal counting – parameterized */
    /* --------------------------------------------------------------------- */

    /**
     * Verifies that the method returns the expected number of occurrences
     * for a wide range of inputs.
     */
    @ParameterizedTest(name = "[{index}] \"{0}\" contains ''{1}'' → {2}")
    @MethodSource("provideNormalCountingCases")
    void count_returnsExpectedNumberOfOccurrences(String input, char target, int expected) {
        int actual = StringUtilities.count(input, target);
        assertEquals(expected, actual,
                () -> String.format("Expected %d occurrences of '%c' in \"%s\" but got %d",
                        expected, target, input, actual));
    }

    /**
     * Supplies normal (non‑exceptional) test data.
     */
    private static Stream<Arguments> provideNormalCountingCases() {
        return Stream.of(
                // basic examples from the original framework
                Arguments.of("banana", 'a', 3),
                Arguments.of("banana", 'b', 1),
                Arguments.of("banana", 'n', 2),
                Arguments.of("banana", 'z', 0),
                Arguments.of("", 'a', 0),
                Arguments.of("aaaaa", 'a', 5),
                Arguments.of("AaAaA", 'a', 2), // case‑sensitive
                Arguments.of("😀😀a😀", '😀', 3), // Unicode surrogate pair as a char

                // additional edge cases from the JSON spec
                Arguments.of("\u0000\u0000\u0000", '\u0000', 3), // three null chars
                Arguments.of("abc", '\t', 0), // tab not present
                Arguments.of("\t\tabc\t", '\t', 3), // three tabs
                Arguments.of("😀😁😀", '\uD83D', 3), // high‑surrogate of each emoji
                Arguments.of("😀😁😀", '\uDE00', 2), // low‑surrogate of 😀
                Arguments.of("line1\nline2\nline3", '\n', 2), // newline
                Arguments.of("line1\rline2\r\nline3", '\r', 2), // carriage‑return (once alone, once in CRLF)
                Arguments.of("   ", ' ', 3), // three spaces
                Arguments.of("ababa", 'b', 2), // non‑consecutive occurrences
                Arguments.of("a", 'a', 1), // single‑character match
                Arguments.of("a", 'b', 0), // single‑character non‑match
                Arguments.of("a".repeat(10_000), 'a', 10_000) // very long string
        );
    }

    /* --------------------------------------------------------------------- */
    /*  Exceptional cases – null source string */
    /* --------------------------------------------------------------------- */

    @Test
    void count_withNullString_throwsNullPointerException() {
        // According to the JSON spec the method should throw NPE when the source is null.
        assertThrows(NullPointerException.class,
                () -> StringUtilities.count(null, 'x'));
    }

    /* --------------------------------------------------------------------- */
    /*  Exceptional case – static field mutation */
    /* --------------------------------------------------------------------- */

    @Test
    void count_whenEmptyFieldIsNull_throwsNullPointerException() {
        // Preserve original value and restore it in @AfterEach
        StringUtilities.EMPTY = null;
        // The token becomes (null + c) which yields a null reference inside
        // count(CharSequence, CharSequence) and should therefore trigger NPE.
        assertThrows(NullPointerException.class,
                () -> StringUtilities.count("any non‑null string", 'a'));
    }

    /* --------------------------------------------------------------------- */
    /*  Direct delegation sanity checks (not required by JSON but useful) */
    /* --------------------------------------------------------------------- */

    @Test
    void count_whenTokenIsEmptyString_returnsZero() {
        // This cannot happen via the public overload, but we verify the delegated method directly.
        int result = StringUtilities.count("any", "");
        assertEquals(0, result, "Counting an empty token should always return 0");
    }

    @Test
    void count_whenStringContainsOnlyTargetChar_returnsStringLength() {
        String s = "xxxx";
        int result = StringUtilities.count(s, 'x');
        assertEquals(s.length(), result);
    }

    @Test
    void count_whenStringContainsNoTargetChar_returnsZero() {
        String s = "hello world";
        int result = StringUtilities.count(s, 'z');
        assertEquals(0, result);
    }
}