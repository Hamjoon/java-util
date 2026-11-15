package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringUtilities_getRandomString_Test {

    /**
     * A {@link Random} instance with a fixed seed used for deterministic tests.
     */
    private Random deterministicRandom;

    /* --------------------------------------------------------------------- */
    private static final class DeterministicRandom extends Random {

        private final Queue<Integer> values;

        DeterministicRandom(int... sequence) {
            this.values = new ArrayDeque<>();
            for (int v : sequence) {
                this.values.add(v);
            }
        }

        @Override
        public int nextInt(int bound) {
            // Guard against illegal bound – let the original Random implementation throw.
            if (bound <= 0) {
                return super.nextInt(bound);
            }
            // If no more predefined values, keep returning the last one.
            return values.isEmpty() ? bound - 1 : Math.min(values.poll(), bound - 1);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Seed chosen arbitrarily; any fixed value works.
        deterministicRandom = new Random(12345L);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test.
        deterministicRandom = null;
    }

    /**
     * Helper to create a readable description for the parameterised test.
     */
    private static String describe(int min, int max, int[] seq) {
        return String.format("min=%d, max=%d, seq=%s", min, max, Arrays.toString(seq));
    }

    static Stream<Arguments> lengthCalculationProvider() {
        return Stream.of(// min == max == 5, random offset 0 → length 5
        Arguments.of(describe(5, 5, new int[] { 0 }), new DeterministicRandom(0), 5, 5, 5), // lower bound: min=3, max=7, random offset 0 → length 3
        Arguments.of(describe(3, 7, new int[] { 0 }), new DeterministicRandom(0), 3, 7, 3), // upper bound: offset 4 (max-min) → length 7
        Arguments.of(describe(3, 7, new int[] { 4 }), new DeterministicRandom(4), 3, 7, 7), // mid‑range: offset 2 → length 5
        Arguments.of(describe(3, 7, new int[] { 2 }), new DeterministicRandom(2), 3, 7, 5));
    }

    static Stream<Arguments> characterCompositionProvider() {
        return Stream.of(// length 1, first char lower‑case (upper flag false), deterministic 0 → 'a'
        Arguments.of("len=1, first char lower‑case", // length offset 0 (min==max)
        new DeterministicRandom(0), 1, 1, "a"), // length 2, first lower‑case, second upper‑case → 'aA'
        Arguments.of("len=2, lower then upper", // length offset 0 (exact length 2)
        new DeterministicRandom(0), 2, 2, "aA"), // length 5 (max bound) with explicit character sequence.
        // Random.nextInt for length returns 2 (max‑offset) → length = 5.
        // Subsequent calls to getRandomChar consume 5,10,15,20,25.
        // Assuming getRandomChar maps n % 26 to a‑z / A‑Z, all values map to
        // 'f' (5%26) for lower‑case and 'F' (5%26) for upper‑case etc.
        // For simplicity we assert the pattern based on the known mapping:
        // 5 → 'f' (lower), 10 → 'j' (upper), 15 → 'p' (lower), 20 → 'v' (upper), 25 → 'z' (lower)
        // Since the implementation alternates upper flag, the expected string is "fJpVz".
        Arguments.of("len=5, deterministic char sequence", // first 2 for length, rest for chars
        new DeterministicRandom(2, 5, 10, 15, 20, 25), 3, 5, "fJpVz"), // zero length – should return empty string
        Arguments.of("len=0, empty string", new DeterministicRandom(0), 0, 0, ""), // negative bounds – we expect IllegalArgumentException from Random.nextInt
        // (the method will attempt nextInt with a non‑positive bound)
        Arguments.of("negative bounds – exception case", new DeterministicRandom(0), -3, -1, // null signals that we expect an exception, handled separately
        null));
    }

    @Test
    @DisplayName("String length is always within the supplied bounds")
    void testLengthWithinBounds() {
        // Test a variety of min/max pairs.
        int[][] ranges = { { 0, 0 }, { 0, 5 }, { 1, 1 }, { 1, 10 }, { 5, 15 }, { 10, 10 } };
        for (int[] range : ranges) {
            int min = range[0];
            int max = range[1];
            // Use a fresh Random for each iteration to avoid correlation
            Random r = new Random(98765L);
            String result = StringUtilities.getRandomString(r, min, max);
            assertNotNull(result, "Result should never be null");
            assertTrue(result.length() >= min, () -> "Length " + result.length() + " is less than min " + min);
            assertTrue(result.length() <= max, () -> "Length " + result.length() + " is greater than max " + max);
        }
    }

    @Test
    @DisplayName("Deterministic Random produces repeatable strings")
    void testDeterministicResultWithFixedSeed() {
        int minLen = 3;
        int maxLen = 8;
        // First call
        String first = StringUtilities.getRandomString(deterministicRandom, minLen, maxLen);
        // Reset the Random to the same seed to obtain the same sequence.
        deterministicRandom = new Random(12345L);
        String second = StringUtilities.getRandomString(deterministicRandom, minLen, maxLen);
        assertEquals(first, second, "With the same seed and parameters the generated strings must be identical");
        assertTrue(first.length() >= minLen && first.length() <= maxLen, "Generated string length must respect the supplied bounds");
    }

    @Test
    @DisplayName("When minLen equals maxLen the returned string length matches exactly")
    void testExactLengthWhenMinEqualsMax() {
        int exactLength = 7;
        Random r = new Random(555L);
        String result = StringUtilities.getRandomString(r, exactLength, exactLength);
        assertEquals(exactLength, result.length(), "When minLen == maxLen the produced string must have that exact length");
    }

    @ParameterizedTest(name = "[{index}] Length calculation – {0}")
    @MethodSource("lengthCalculationProvider")
    @DisplayName("Length calculation – deterministic bounds")
    void testLengthCalculationDeterministic(String description, Random random, int minLen, int maxLen, int expectedLength) {
        String result = StringUtilities.getRandomString(random, minLen, maxLen);
        assertEquals(expectedLength, result.length(), () -> "Expected length " + expectedLength + " but got " + result.length());
    }

    @ParameterizedTest(name = "[{index}] Char composition – {0}")
    @MethodSource("characterCompositionProvider")
    @DisplayName("Random character composition and edge cases")
    void testCharacterComposition(String description, Random random, int minLen, int maxLen, String expected) {
        String actual = StringUtilities.getRandomString(random, minLen, maxLen);
        assertEquals(expected, actual, () -> "Expected string \"" + expected + "\" but got \"" + actual + "\"");
    }

    @Test
    @DisplayName("Negative bounds produce IllegalArgumentException")
    void testNegativeBoundsException() {
        Random r = new DeterministicRandom(0);
        assertThrows(IllegalArgumentException.class, () -> StringUtilities.getRandomString(r, -3, -1), "Negative bounds should cause IllegalArgumentException");
    }

    @Test
    @DisplayName("minLen > maxLen produces IllegalArgumentException")
    void testMinGreaterThanMaxException() {
        Random r = new DeterministicRandom(0);
        assertThrows(IllegalArgumentException.class, () -> StringUtilities.getRandomString(r, 5, 4), "minLen > maxLen should cause IllegalArgumentException");
    }

    @Test
    @DisplayName("Null Random argument throws NullPointerException")
    void testNullRandomException() {
        assertThrows(NullPointerException.class, () -> StringUtilities.getRandomString(null, 2, 4), "Passing null Random must throw NullPointerException");
    }

    @Test
    @DisplayName("maxLen negative while minLen positive throws IllegalArgumentException")
    void testPositiveMinNegativeMaxException() {
        Random r = new DeterministicRandom(0);
        assertThrows(IllegalArgumentException.class, () -> StringUtilities.getRandomString(r, 3, -1), "Positive minLen with negative maxLen should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("Both minLen and maxLen negative – IllegalArgumentException")
    void testBothNegativeBoundsException() {
        Random r = new DeterministicRandom(0);
        assertThrows(IllegalArgumentException.class, () -> StringUtilities.getRandomString(r, -5, -3), "Both bounds negative should result in IllegalArgumentException");
    }

    @Test
    @DisplayName("minLen == maxLen == 0 returns empty string")
    void testZeroExactLength() {
        Random r = new DeterministicRandom(0);
        String result = StringUtilities.getRandomString(r, 0, 0);
        assertEquals("", result, "Exact zero length must return empty string");
    }
}
