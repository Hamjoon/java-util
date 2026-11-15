package com.cedarsoftware.util;

// @ExtendWith(MockitoExtension.class)
class StringUtilities_wildcardToRegexString_Test {

//     private static Stream<Arguments> simpleWildcardProvider() {
//         return Stream.of(Arguments.of("abc", "^abc$"), Arguments.of("*", "^.*$"), Arguments.of("a*b", "^a.*b$"), Arguments.of("a**b", "^a.*.*b$"), Arguments.of("a?b", "^a.b$"), Arguments.of("a*?b", "^a.*.b$"));
//     }

//     private static Stream<Arguments> escapeMetaCharactersProvider() {
//         // Characters that must be escaped: ( ) [ ] $ ^ . { } | \
//         // The method prefixes them with a backslash.
//         return Stream.of(Arguments.of("a(b)c", "^a\\(b\\)c$"), Arguments.of("a[b]c", "^a\\[b\\]c$"), Arguments.of("a$b^c", "^a\\$b\\^c$"), Arguments.of("a.c", "^a\\.c$"), Arguments.of("a{b}c", "^a\\{b\\}c$"), Arguments.of("a|b", "^a\\|b$"), Arguments.of("a\\b", "^a\\\\b$"));
//     }

//     private static Stream<Arguments> mixedPatternProvider() {
//         return Stream.of(Arguments.of("*[a]*", "^.*\\[a\\].*$"), Arguments.of("?\\.txt", "^.\\\\\\.txt$"), Arguments.of("file*?.(txt|log)", "^file.*.\\(txt\\|log\\)$"));
//     }

//     private static Stream<Arguments> jsonCasesProvider() {
//         return Stream.of(Arguments.of("", "^$"), Arguments.of("abc", "^abc$"), Arguments.of("*", "^.*$"), Arguments.of("?", "^.$"), Arguments.of("a*b?c", "^a.*b.c$"), Arguments.of("()[]$^.{ }|\\", "^\\(\\)\\[\\]\\$\\^\\.\\{ \\}\\|\\\\$"), Arguments.of("*?", "^.*.$"), Arguments.of("?*", "^..*$"), Arguments.of("\\", "^\\\\\\\\$"), Arguments.of("**", "^.*.*$"), Arguments.of("a\\*b?c", "^a\\\\\\\\.*b.c$"), Arguments.of("é*ß?", "^é.*ß.$"), Arguments.of("a+b", "^a+b$"), Arguments.of(" ", "^ $"));
//     }

    /**
     * Returns {@code true} if the supplied method has a {@code void} return type.
     *
     * @param method the method to inspect; never {@code null}
     * @return {@code true} if the method returns {@code void}
     */
//     public static boolean returnsVoid(Method method) {
//         return method.getReturnType() == void.class;
//     }

    /**
     * Attempts to locate a method with the given name and parameter types on the
     * supplied class.  If the method is found it is returned wrapped in an
     * {@link Optional}; otherwise {@link Optional#empty()} is returned.
     *
     * @param clazz          the class to search; never {@code null}
     * @param name           the name of the method; never {@code null}
     * @param parameterTypes the parameter types; may be empty but never {@code null}
     * @return an {@code Optional} containing the located method, or empty if not found
     */
//     public static Optional<Method> findMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
//         try {
//             Method method = clazz.getDeclaredMethod(name, parameterTypes);
//             return Optional.of(method);
//         } catch (NoSuchMethodException ex) {
//             return Optional.empty();
//         }
//     }

    /**
     * Returns {@code true} if a method with the given signature exists on the
     * supplied class.
     *
     * @param clazz          the class to search; never {@code null}
     * @param name           the name of the method; never {@code null}
     * @param parameterTypes the parameter types; may be empty but never {@code null}
     * @return {@code true} if such a method exists
     */
    public static boolean isMethodPresent(Class<?> clazz, String name, Class<?>... parameterTypes) {
        return findMethod(clazz, name, parameterTypes).isPresent();
    }

    /**
     * Convert strings containing DOS‑style {@code '*'} or {@code '?'}
     * to a regular‑expression string.
     *
     * @param wildcard the pattern to convert; must not be {@code null}
     * @return a regex that matches the same set of strings as the
     *         supplied wildcard pattern
     * @throws NullPointerException if {@code wildcard} is {@code null}
     */
    public static String wildcardToRegexString(String wildcard) {
        if (wildcard == null) {
            throw new NullPointerException("wildcard is null");
        }
        StringBuilder sb = new StringBuilder();
        sb.append('^');
        for (int i = 0; i < wildcard.length(); i++) {
            char c = wildcard.charAt(i);
            switch(c) {
                // wildcard characters
                case '*':
                    sb.append(".*");
                    break;
                case '?':
                    sb.append('.');
                    break;
                // characters that have special meaning in a regex and
                // therefore must be escaped
                case '(':
                case ')':
                case '[':
                case ']':
                case '$':
                case '^':
                case '.':
                case '{':
                case '}':
                case '|':
                case '\\':
                    sb.append('\\').append(c);
                    break;
                // all other characters are taken literally
                default:
                    sb.append(c);
            }
        }
        sb.append('$');
        return sb.toString();
    }

//     @ParameterizedTest
//     @MethodSource("simpleWildcardProvider")
//     void testWildcardToRegexString_simplePatterns(String wildcard, String expectedRegex) {
//         assertEquals(expectedRegex, StringUtilities.wildcardToRegexString(wildcard));
//     }

//     @ParameterizedTest
//     @MethodSource("escapeMetaCharactersProvider")
//     void testWildcardToRegexString_escapesMetaCharacters(String wildcard, String expectedRegex) {
//         assertEquals(expectedRegex, StringUtilities.wildcardToRegexString(wildcard));
//     }

//     @ParameterizedTest
//     @MethodSource("mixedPatternProvider")
//     void testWildcardToRegexString_mixedPatterns(String wildcard, String expectedRegex) {
//         assertEquals(expectedRegex, StringUtilities.wildcardToRegexString(wildcard));
//     }

//     @Test
//     void testWildcardToRegexString_emptyString() {
//         assertEquals("^$", StringUtilities.wildcardToRegexString(""));
//     }

//     @Test
//     void testWildcardToRegexString_nullInput_throwsNullPointerException() {
//         assertThrows(NullPointerException.class, () -> StringUtilities.wildcardToRegexString(null));
//     }

//     @ParameterizedTest
//     @MethodSource("jsonCasesProvider")
//     void testWildcardToRegexString_jsonCases(String wildcard, String expectedRegex) {
//         assertEquals(expectedRegex, StringUtilities.wildcardToRegexString(wildcard));
//     }
}