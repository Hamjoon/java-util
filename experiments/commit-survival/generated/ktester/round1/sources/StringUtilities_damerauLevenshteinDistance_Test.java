package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringUtilities_damerauLevenshteinDistance_Test {

    private static Stream<Arguments> nullAndEmptyProvider() {
        return Stream.of(Arguments.of(null, null, 0, "Both source and target are null – should return 0"), Arguments.of(null, "abc", 3, "Source null, target length 3 – distance equals target length"), Arguments.of("", null, 0, "Source empty, target null – both considered empty, distance 0"), Arguments.of("", "abc", 3, "Source empty, target non‑empty – distance equals target length"), Arguments.of("abc", "", 3, "Target empty, source non‑empty – distance equals source length"));
    }

    private static Stream<Arguments> basicLevenshteinProvider() {
        return Stream.of(Arguments.of("kitten", "sitting", 3, "Classic example – only insertions, deletions and substitutions, no transposition"), Arguments.of("a", "a", 0, "Identical single‑character strings"), Arguments.of("a", "b", 1, "Single‑character substitution"), Arguments.of("ab", "ac", 1, "One substitution at the second position"));
    }

    private static Stream<Arguments> transpositionDetectionProvider() {
        return Stream.of(Arguments.of("test", "tset", 1, "Characters 'e' and 's' are swapped – transposition should give distance 1"), Arguments.of("ab", "ba", 1, "Two‑character swap – transposition logic exercised when indices > 1"), Arguments.of("ca", "ac", 1, "Swap at the beginning – ensures algorithm handles transposition after guard"), Arguments.of("abcdef", "abcfde", 2, "Two transpositions (de ↔ fd) plus one insertion"));
    }

    private static Stream<Arguments> edgeCasesLengthOneProvider() {
        return Stream.of(Arguments.of("x", "", 1, "Source length 1, target empty – one deletion"), Arguments.of("", "y", 1, "Source empty, target length 1 – one insertion"));
    }

    private static Stream<Arguments> identicalLongerStringsProvider() {
        return Stream.of(Arguments.of("abcdefgh", "abcdefgh", 0, "Two identical longer strings – distance should be zero"));
    }

    private static Stream<Arguments> insertionDeletionProvider() {
        return Stream.of(Arguments.of("kitten", "kit", 3, "Target is a prefix of source – three deletions required"), Arguments.of("kit", "kitten", 3, "Source is a prefix of target – three insertions required"));
    }

    private static Stream<Arguments> mixedOperationsProvider() {
        return Stream.of(Arguments.of("a cat", "ac at", 1, "Space and 'c' are swapped – single transposition"), Arguments.of("abdc", "abcd", 1, "Characters 'd' and 'c' are transposed"));
    }

    private static Stream<Arguments> unicodeCharactersProvider() {
        return Stream.of(Arguments.of("café", "cafe", 1, "Unicode character 'é' vs 'e' – single substitution"), Arguments.of("😀a", "a😀", 1, "Emoji and letter transposed – distance 1"));
    }

    private static Stream<Arguments> nonStringCharSequenceProvider() {
        return Stream.of(Arguments.of(new StringBuilder("test"), "tset", 1, "Source is a StringBuilder; transposition of 'e' and 's' yields distance 1"));
    }

    private static Stream<Arguments> caseSensitivityProvider() {
        return Stream.of(Arguments.of("Test", "test", 1, "Only the first character differs by case – distance 1"), Arguments.of("ABC", "abc", 3, "All three characters differ only by case – distance equals number of differing characters"));
    }

    @Test
    void shouldReturnZeroWhenBothAreNullOrEmpty() {
        // both null
        assertEquals(0, StringUtilities.damerauLevenshteinDistance(null, null));
        // both empty
        assertEquals(0, StringUtilities.damerauLevenshteinDistance("", ""));
        // source null, target non‑empty
        assertEquals(3, StringUtilities.damerauLevenshteinDistance(null, "abc"));
        // source empty, target non‑empty
        assertEquals(4, StringUtilities.damerauLevenshteinDistance("", "test"));
        // target null, source non‑empty
        assertEquals(5, StringUtilities.damerauLevenshteinDistance("hello", null));
        // target empty, source non‑empty
        assertEquals(2, StringUtilities.damerauLevenshteinDistance("ab", ""));
    }

    @Test
    void shouldReturnZeroWhenStringsAreIdentical() {
        assertEquals(0, StringUtilities.damerauLevenshteinDistance("identical", "identical"));
        assertEquals(0, StringUtilities.damerauLevenshteinDistance("a", "a"));
    }

    @Test
    void shouldCalculateStandardLevenshteinWhenNoTranspositions() {
        // classic example: "kitten" → "sitting" has distance 3
        assertEquals(3, StringUtilities.damerauLevenshteinDistance("kitten", "sitting"));
    }

    @Test
    void shouldDetectSingleTransposition() {
        // "ca" → "ac" is a single transposition, distance = 1
        assertEquals(1, StringUtilities.damerauLevenshteinDistance("ca", "ac"));
        // "abcd" → "acbd" swaps 'b' and 'c'
        assertEquals(1, StringUtilities.damerauLevenshteinDistance("abcd", "acbd"));
    }

    @Test
    void shouldHandleMultipleOperationsIncludingTranspositions() {
        // "example" → "samples"
        // Expected Damerau‑Levenshtein distance is 3:
        // e x a m p l e
        // s a m p l e s
        // operations: replace 'e' with 's', delete 'x', insert 's' at end
        assertEquals(3, StringUtilities.damerauLevenshteinDistance("example", "samples"));
    }

    @Test
    void shouldWorkWithUnicodeAndCaseSensitivity() {
        // Unicode characters are treated like any other char
        assertEquals(2, StringUtilities.damerauLevenshteinDistance("café", "cafe"));
        // Case matters – 'A' vs 'a' is a substitution
        assertEquals(1, StringUtilities.damerauLevenshteinDistance("A", "a"));
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("nullAndEmptyProvider")
    void nullAndEmptyHandling(CharSequence source, CharSequence target, int expected, String description) {
        assertEquals(expected, StringUtilities.damerauLevenshteinDistance(source, target), description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("basicLevenshteinProvider")
    void basicLevenshteinNoTransposition(CharSequence source, CharSequence target, int expected, String description) {
        assertEquals(expected, StringUtilities.damerauLevenshteinDistance(source, target), description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("transpositionDetectionProvider")
    void transpositionDetection(CharSequence source, CharSequence target, int expected, String description) {
        assertEquals(expected, StringUtilities.damerauLevenshteinDistance(source, target), description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("edgeCasesLengthOneProvider")
    void edgeCasesLengthOne(CharSequence source, CharSequence target, int expected, String description) {
        assertEquals(expected, StringUtilities.damerauLevenshteinDistance(source, target), description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("identicalLongerStringsProvider")
    void identicalLongerStrings(CharSequence source, CharSequence target, int expected, String description) {
        assertEquals(expected, StringUtilities.damerauLevenshteinDistance(source, target), description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("insertionDeletionProvider")
    void insertionAndDeletion(CharSequence source, CharSequence target, int expected, String description) {
        assertEquals(expected, StringUtilities.damerauLevenshteinDistance(source, target), description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("mixedOperationsProvider")
    void mixedOperationsSubstitutionAndTransposition(CharSequence source, CharSequence target, int expected, String description) {
        assertEquals(expected, StringUtilities.damerauLevenshteinDistance(source, target), description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("unicodeCharactersProvider")
    void unicodeCharacters(CharSequence source, CharSequence target, int expected, String description) {
        assertEquals(expected, StringUtilities.damerauLevenshteinDistance(source, target), description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("nonStringCharSequenceProvider")
    void nonStringCharSequence(CharSequence source, CharSequence target, int expected, String description) {
        assertEquals(expected, StringUtilities.damerauLevenshteinDistance(source, target), description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("caseSensitivityProvider")
    void caseSensitivity(CharSequence source, CharSequence target, int expected, String description) {
        assertEquals(expected, StringUtilities.damerauLevenshteinDistance(source, target), description);
    }
}
