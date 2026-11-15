package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class StringUtilities_hashCodeIgnoreCase_Test {

    /**
     * Holds the original value of the private static flag so we can restore it after each test.
     */
    private static boolean originalAsciiCompatibleLocale;

    /**
     * Reflective access to the private static field {@code isAsciiCompatibleLocale}.
     */
    private static Field asciiFlagField;

    @BeforeAll
    static void setupBeforeAll() throws Exception {
        asciiFlagField = StringUtilities.class.getDeclaredField("isAsciiCompatibleLocale");
        asciiFlagField.setAccessible(true);
        originalAsciiCompatibleLocale = (boolean) asciiFlagField.get(null);
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Ensure each test starts with the original flag value.
        asciiFlagField.set(null, originalAsciiCompatibleLocale);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Reset after each test to avoid cross‑test contamination.
        asciiFlagField.set(null, originalAsciiCompatibleLocale);
    }

    @AfterAll
    static void teardownAfterAll() throws Exception {
        // Restore the original flag value for any other tests that might run later.
        asciiFlagField.set(null, originalAsciiCompatibleLocale);
    }

    private static Stream<Arguments> fastPathCases() {
        return Stream.of(Arguments.of("AbC", 96354), Arguments.of("abc", 96354), Arguments.of("xyz", 119193), Arguments.of("Z", 122), Arguments.of("z", 122), Arguments.of("AaAaAa", 2466804), Arguments.of("12345", 48690), Arguments.of("!@# $%^&*()", 1241354555));
    }

    private static Stream<Arguments> slowPathCases() {
        return Stream.of(Arguments.of("Ω", 969), Arguments.of("ΩΩ", 31008), Arguments.of("AΩ", 3976), Arguments.of("ß", 2236), Arguments.of("İ", 3049), Arguments.of("ǅ", 3027), Arguments.of("𐍈𐍈", 288800), Arguments.of("AΩßİǅ𐍈", 1241354555));
    }

    private static Stream<Arguments> localeSwitchCases() {
        return Stream.of(Arguments.of("Test", 2627610), Arguments.of("Τest", 2627610));
    }

    /**
     * Returns {@code true} if the supplied method has a {@code void}
     * return type.
     *
     * @param method the method to inspect; never {@code null}
     * @return {@code true} if the method returns {@code void}
     */
    public static boolean returnsVoid(Method method) {
        return method.getReturnType() == void.class;
    }

    /**
     * Find a method with the given name and parameter types on the
     * supplied class (including declared methods of super‑classes).
     *
     * @param clazz      the class to search; never {@code null}
     * @param name       the method name; never {@code null}
     * @param paramTypes the parameter types; may be empty but never {@code null}
     * @return the {@link Method} if found, otherwise {@code null}
     */
    public static Method findMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        try {
            return clazz.getMethod(name, paramTypes);
        } catch (NoSuchMethodException e) {
            // try declared methods (including private/protected)
            try {
                return clazz.getDeclaredMethod(name, paramTypes);
            } catch (NoSuchMethodException ex) {
                // not found
                return null;
            }
        }
    }

    /**
     * Returns {@code true} if a method with the given signature is present
     * on the supplied class.
     *
     * @param clazz      the class to inspect; never {@code null}
     * @param name       the method name; never {@code null}
     * @param paramTypes the parameter types; may be empty but never {@code null}
     * @return {@code true} if such a method exists
     */
    public static boolean isMethodPresent(Class<?> clazz, String name, Class<?>... paramTypes) {
        return findMethod(clazz, name, paramTypes) != null;
    }

    @Test
    void testHashCodeIgnoreCase_nullInput_returnsZero() {
        assertEquals(0, StringUtilities.hashCodeIgnoreCase(null));
    }

    @Test
    void testHashCodeIgnoreCase_emptyString_returnsZero() {
        assertEquals(0, StringUtilities.hashCodeIgnoreCase(""));
    }

    @Test
    void testHashCodeIgnoreCase_caseInsensitiveEquality() {
        String s1 = "HeLLo WoRLd";
        String s2 = "hEllO wOrlD";
        int h1 = StringUtilities.hashCodeIgnoreCase(s1);
        int h2 = StringUtilities.hashCodeIgnoreCase(s2);
        assertEquals(h1, h2, "Hash codes must be equal for case‑insensitive strings");
    }

    @Test
    void testHashCodeIgnoreCase_asciiFastPathMatchesSlowPath_forAsciiOnly() throws Exception {
        // Force fast path
        asciiFlagField.set(null, true);
        String ascii = "AbCdEfG123!@#";
        int fast = StringUtilities.hashCodeIgnoreCase(ascii);
        // Force slow path
        asciiFlagField.set(null, false);
        int slow = StringUtilities.hashCodeIgnoreCase(ascii);
        assertEquals(slow, fast, "For pure ASCII strings the fast and slow paths must produce identical hash codes");
    }

    @Test
    void testHashCodeIgnoreCase_nonAsciiDifferenceBetweenPaths() throws Exception {
        // The character '\u0130' is LATIN CAPITAL LETTER I WITH DOT ABOVE.
        // Its Unicode lower‑case mapping is '\u0069' (plain 'i'), which is *different*
        // from the character itself when left unchanged.
        // "İABC"
        String withNonAscii = "\u0130ABC";
        // Fast path (ASCII only) – the non‑ASCII character is left unchanged.
        asciiFlagField.set(null, true);
        int fastHash = StringUtilities.hashCodeIgnoreCase(withNonAscii);
        // Slow path – uses Character.toLowerCase which will convert '\u0130' to 'i'.
        asciiFlagField.set(null, false);
        int slowHash = StringUtilities.hashCodeIgnoreCase(withNonAscii);
        assertNotEquals(fastHash, slowHash, "When a non‑ASCII uppercase character is present the two paths should yield different hashes");
    }

    /**
     * Fast‑path (ASCII compatible locale) test cases.
     * The internal flag {@code isAsciiCompatibleLocale} is forced to {@code true}
     * before each invocation.
     */
    @ParameterizedTest(name = "[{index}] fastPath ''{0}'' → {1}")
    @MethodSource("fastPathCases")
    void testHashCodeIgnoreCase_fastPath(String input, int expected) throws Exception {
        // force fast path
        asciiFlagField.set(null, true);
        assertEquals(expected, StringUtilities.hashCodeIgnoreCase(input), () -> "Fast‑path hash for \"" + input + "\" did not match expected");
    }

    /**
     * Slow‑path (non‑ASCII locale) test cases.
     * The internal flag {@code isAsciiCompatibleLocale} is forced to {@code false}
     * before each invocation.
     */
    @ParameterizedTest(name = "[{index}] slowPath ''{0}'' → {1}")
    @MethodSource("slowPathCases")
    void testHashCodeIgnoreCase_slowPath(String input, int expected) throws Exception {
        // force slow path
        asciiFlagField.set(null, false);
        assertEquals(expected, StringUtilities.hashCodeIgnoreCase(input), () -> "Slow‑path hash for \"" + input + "\" did not match expected");
    }

    /**
     * Verify that the hash code is identical regardless of the internal locale flag.
     * Both flag settings are exercised for each input.
     */
    @ParameterizedTest(name = "[{index}] localeSwitch ''{0}'' → {1}")
    @MethodSource("localeSwitchCases")
    void testHashCodeIgnoreCase_localeSwitch(String input, int expected) throws Exception {
        // Fast path
        asciiFlagField.set(null, true);
        int fast = StringUtilities.hashCodeIgnoreCase(input);
        // Slow path
        asciiFlagField.set(null, false);
        int slow = StringUtilities.hashCodeIgnoreCase(input);
        assertEquals(expected, fast, () -> "Fast‑path hash for \"" + input + "\" did not match expected");
        assertEquals(expected, slow, () -> "Slow‑path hash for \"" + input + "\" did not match expected");
        assertEquals(fast, slow, () -> "Fast and slow path hashes differ for \"" + input + "\" when they should be equal");
    }
}
