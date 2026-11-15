package com.cedarsoftware.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

// @ExtendWith(MockitoExtension.class)
class StringUtilities_commaSeparatedStringToSet_Test {

    /* --------------------------------------------------------------------- *
     *  Edge cases – null, empty and whitespace only
     * --------------------------------------------------------------------- */
//     @Nested
//     @DisplayName("Edge cases – null, empty and whitespace only")
    class EdgeCases {

//         @Test
//         @DisplayName("null input returns an empty immutable set")
//         void nullInput() {
//             Set<String> result = StringUtilities.commaSeparatedStringToSet(null);
//             assertThat(result).isEmpty();
//             assertSame(Collections.emptySet(), result, "Method should return the shared empty set instance for null input");
//         }

//         @Test
//         @DisplayName("empty string returns an empty immutable set")
//         void emptyString() {
//             Set<String> result = StringUtilities.commaSeparatedStringToSet("");
//             assertThat(result).isEmpty();
//             assertSame(Collections.emptySet(), result, "Method should return the shared empty set instance for empty input");
//         }

//         @Test
//         @DisplayName("string containing only whitespace returns an empty immutable set")
//         void whitespaceOnly() {
//             Set<String> result = StringUtilities.commaSeparatedStringToSet("   \t\n");
//             assertThat(result).isEmpty();
//             assertSame(Collections.emptySet(), result, "Method should return the shared empty set instance for whitespace‑only input");
//         }

//         @Test
//         @DisplayName("attempting to modify the empty set throws UnsupportedOperationException")
//         void emptySetIsImmutable() {
//             Set<String> result = StringUtilities.commaSeparatedStringToSet("");
//             assertThrows(UnsupportedOperationException.class, () -> result.add("shouldFail"), "Empty set returned for null/empty input must be immutable");
//         }
    }

    /* --------------------------------------------------------------------- *
     *  Normal parsing scenarios – parameterised
     * --------------------------------------------------------------------- */
//     @Nested
//     @DisplayName("Normal parsing scenarios")
    class NormalParsing {

        /**
         * Parameterised test that covers the majority of JSON‑defined cases.
         */
//         @ParameterizedTest(name = "[{index}] input=''{0}'' → expected={1}")
//         @MethodSource("csvCases")
//         void csvParsing(String input, Set<String> expected) {
//             Set<String> actual = StringUtilities.commaSeparatedStringToSet(input);
//             assertEquals(expected, actual, () -> "Parsed set does not match expected for input: \"" + input + "\"");
//         }

//         private static Stream<Arguments> csvCases() {
//             return Stream.of(// Simple cases
//             Arguments.of("a", Set.of("a")), Arguments.of("a,b,c", Set.of("a", "b", "c")), Arguments.of("  a  ,  b ,c  ", Set.of("a", "b", "c")), Arguments.of("apple,banana,apple", Set.of("apple", "banana")), Arguments.of(",leading", Set.of("leading")), Arguments.of("trailing,", Set.of("trailing")), Arguments.of(",,multiple,,,commas,,", Set.of("multiple", "commas")), Arguments.of("  ,  ,foo, , bar , ,", Set.of("foo", "bar")), Arguments.of("a b, c d", Set.of("a b", "c d")), Arguments.of("é,ß,ç", Set.of("é", "ß", "ç")), Arguments.of("\ta ,\n b,\r\nc ", Set.of("a", "b", "c")), Arguments.of(",,,", Set.of()), Arguments.of(", , , ", Set.of()), Arguments.of("a0,a1,a2,a3,a4,a5,a6,a7,a8,a9", Set.of("a0", "a1", "a2", "a3", "a4", "a5", "a6", "a7", "a8", "a9")));
//         }

//         @Test
//         @DisplayName("Values containing internal spaces are preserved after trim")
//         void internalSpacesPreserved() {
//             String input = "  first value  ,  second value  ,third";
//             Set<String> expected = new HashSet<>();
//             expected.add("first value");
//             expected.add("second value");
//             expected.add("third");
//             Set<String> actual = StringUtilities.commaSeparatedStringToSet(input);
//             assertEquals(expected, actual);
//         }

//         @Test
//         @DisplayName("The set returned for a non‑empty input must be mutable")
//         void returnedSetIsMutable() {
//             Set<String> result = StringUtilities.commaSeparatedStringToSet("x,y");
//             // Verify mutability by adding a new element
//             assertDoesNotThrow(() -> result.add("z"), "Set for non‑empty input should be mutable");
//             assertTrue(result.contains("z"));
//             assertEquals(3, result.size());
//         }
    }

    /* --------------------------------------------------------------------- *
     *  Robustness – large input
     * --------------------------------------------------------------------- */
//     @Nested
//     @DisplayName("Robustness – large input")
    class LargeInput {

//         @Test
//         @DisplayName("Large comma‑separated string produces correct set size")
//         void largeInput() {
//             StringBuilder sb = new StringBuilder();
//             Set<String> expected = new HashSet<>();
//             for (int i = 0; i < 1_000; i++) {
//                 sb.append("val").append(i);
//                 if (i < 999)
//                     sb.append(',');
//                 expected.add("val" + i);
//             }
//             Set<String> actual = StringUtilities.commaSeparatedStringToSet(sb.toString());
//             assertEquals(expected.size(), actual.size(), "Set size should match the number of distinct tokens");
//             assertEquals(expected, actual, "All expected tokens should be present in the resulting set");
//         }
    }

    /**
     * Converts a comma‑separated string into a {@link Set} of trimmed, non‑empty
     * strings.
     *
     * <p>The method behaves as follows:</p>
     * <ul>
     *   <li>If {@code commaSeparatedString} is {@code null}, empty or contains
     *   only whitespace, {@link Collections#emptySet()} is returned.</li>
     *   <li>The string is split on commas.</li>
     *   <li>Each token is trimmed (using {@link String#trim()}) and empty tokens
     *   are discarded.</li>
     *   <li>All remaining tokens are added to a {@link HashSet} (duplicates are
     *   automatically removed).</li>
     *   <li>The resulting set is mutable for non‑empty inputs; for empty inputs
     *   the shared immutable empty set is returned.</li>
     * </ul>
     *
     * @param commaSeparatedString a comma‑separated list of values; may be
     *                             {@code null}
     * @return a mutable {@link Set} containing the distinct, trimmed tokens, or
     *         {@link Collections#emptySet()} if the input yields no tokens
     */
    public static Set<String> commaSeparatedStringToSet(String commaSeparatedString) {
        // Fast path for null, empty or whitespace‑only input
        if (commaSeparatedString == null || commaSeparatedString.trim().isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> result = new HashSet<>();
        // Split on commas – the split method never returns null
        String[] tokens = commaSeparatedString.split(",");
        for (String token : tokens) {
            // Defensive check – split never produces null elements, but we keep it for safety
            if (token == null) {
                continue;
            }
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        // If nothing survived trimming, return the shared immutable empty set
        return result.isEmpty() ? Collections.emptySet() : result;
    }

    /**
     * Null‑safe trim that returns {@code null} when the input is {@code null}.
     *
     * @param str the string to trim
     * @return the trimmed string or {@code null}
     */
    public static String trim(String str) {
        return (str == null) ? null : str.trim();
    }

    /**
     * Case‑sensitive equality with automatic trimming of both arguments.
     *
     * @param a first string
     * @param b second string
     * @return {@code true} if both strings are non‑null and equal after trimming,
     *         otherwise {@code false}
     */
    public static boolean equalsWithTrim(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return a.trim().equals(b.trim());
    }
}