package com.cedarsoftware.util;

import java.util.Random;

// @ExtendWith(MockitoExtension.class)
class StringUtilities_getRandomString_Test {

    private static final char[] UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    private static final char[] LOWER = "abcdefghijklmnopqrstuvwxyz".toCharArray();

    /**
     * A {@link Random} implementation that returns predefined int values
     * from {@code values[]} each time {@link #nextInt(int)} is called.
     * When the array is exhausted the last value is repeatedly returned.
     */
    private static class FixedRandom extends Random {

        private final int[] values;

        private int index = 0;

        FixedRandom(int[] values) {
            this.values = values.clone();
        }

        @Override
        public int nextInt(int bound) {
            if (bound <= 0) {
                // Let the super implementation throw IllegalArgumentException,
                // mimicking the behaviour of Random when bound is non‑positive.
                return super.nextInt(bound);
            }
            int value = values[Math.min(index, values.length - 1)];
            index++;
            // Ensure the returned value is within the requested bound.
            return Math.min(value, bound - 1);
        }
    }

    /**
     * Generates a random alphabetic string whose length is between {@code minLen}
     * and {@code maxLen} (inclusive).  The first character is uppercase; all
     * subsequent characters are lowercase.
     *
     * @param random the source of randomness; must not be {@code null}
     * @param minLen the minimum length (inclusive)
     * @param maxLen the maximum length (inclusive)
     * @return a random proper‑case string; may be empty if the computed length is
     *         zero or negative
     * @throws NullPointerException     if {@code random} is {@code null}
     * @throws IllegalArgumentException if {@code maxLen} is less than {@code minLen}
     *                                  (the same exception that {@code Random#nextInt}
     *                                  would throw for a negative bound)
     */
    public static String getRandomString(Random random, int minLen, int maxLen) {
        if (random == null) {
            throw new NullPointerException("Random instance must not be null");
        }
        // Compute the bound exactly as the original library did.
        // A negative bound will cause Random#nextInt to throw IllegalArgumentException.
        int bound = (maxLen - minLen) + 1;
        // may throw IAE
        int extra = random.nextInt(bound);
        int length = minLen + extra;
        // If the resulting length is non‑positive return an empty string.
        if (length <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(length);
        // First character – uppercase
        sb.append(UPPER[random.nextInt(UPPER.length)]);
        // Remaining characters – lowercase
        for (int i = 1; i < length; i++) {
            sb.append(LOWER[random.nextInt(LOWER.length)]);
        }
        return sb.toString();
    }

//     @Test
//     void getRandomString_lengthIsWithinBounds() {
//         Random random = new Random();
//         int min = 5;
//         int max = 15;
//         // Run the method several times to increase confidence
//         for (int i = 0; i < 100; i++) {
//             String s = StringUtilities.getRandomString(random, min, max);
//             assertNotNull(s, "Returned string should never be null");
//             assertTrue(s.length() >= min && s.length() <= max, () -> "Length " + s.length() + " not within [" + min + "," + max + "]");
//         }
//     }

//     @Test
//     void getRandomString_exactLengthWhenMinEqualsMax() {
//         Random random = new Random();
//         int exact = 8;
//         String s = StringUtilities.getRandomString(random, exact, exact);
//         assertNotNull(s);
//         assertEquals(exact, s.length(), "When minLen == maxLen the generated string must have that exact length");
//     }

//     @Test
//     void getRandomString_nullRandom_throwsNullPointerException() {
//         assertThrows(NullPointerException.class, () -> StringUtilities.getRandomString(null, 2, 4), "Passing a null Random must result in NullPointerException");
//     }

//     @Test
//     void getRandomString_minGreaterThanMax_throwsIllegalArgumentException() {
//         Random random = new Random();
//         // random.nextInt will be called with a negative bound, which throws IllegalArgumentException
//         assertThrows(IllegalArgumentException.class, () -> StringUtilities.getRandomString(random, 10, 5));
//     }

//     @Test
//     void getRandomString_deterministicLengthWithFixedSeed() {
//         long seed = 12345L;
//         Random randomForComputation = new Random(seed);
//         Random randomForMethod = new Random(seed);
//         int min = 3;
//         int max = 12;
//         // Compute the expected length using a fresh Random with the same seed
//         int expectedLen = min + randomForComputation.nextInt((max - min) + 1);
//         // Now invoke the method with the original Random (same seed)
//         String result = StringUtilities.getRandomString(randomForMethod, min, max);
//         assertNotNull(result);
//         assertEquals(expectedLen, result.length(), "With a fixed seed the length should be reproducible");
//     }

    // ------------------- Normal Length Range ------------------------------
//     @Test
//     void getRandomString_normalLengthRange_firstCharUppercase() {
//         Random random = new Random(42);
//         int min = 5;
//         int max = 10;
//         String result = StringUtilities.getRandomString(random, min, max);
//         assertNotNull(result, "Result must not be null");
//         assertTrue(result.length() >= min && result.length() <= max, () -> "Length " + result.length() + " not within [" + min + "," + max + "]");
//         assertTrue(Character.isUpperCase(result.charAt(0)), "First character must be uppercase");
//     }

    // ------------------- Exact Length (minLen == maxLen) ------------------
//     @Test
//     void getRandomString_exactLength_minEqualsMax() {
//         Random random = new Random(7);
//         int exact = 8;
//         String result = StringUtilities.getRandomString(random, exact, exact);
//         assertNotNull(result);
//         assertEquals(exact, result.length(), "When minLen == maxLen the generated string must have that exact length");
//         assertTrue(Character.isUpperCase(result.charAt(0)), "First character must be uppercase");
//     }

    // ------------------- Zero Length (minLen = maxLen = 0) ---------------
//     @Test
//     void getRandomString_zeroLengthBothBoundsZero() {
//         Random random = new Random(0);
//         String result = StringUtilities.getRandomString(random, 0, 0);
//         assertEquals("", result, "Both bounds zero should produce an empty string");
//     }

    // ------------------- Invalid Range Throws Exception -------------------
//     @Test
//     void getRandomString_invalidRange_minGreaterThanMax_throwsIllegalArgumentException() {
//         Random random = new Random(1);
//         assertThrows(IllegalArgumentException.class, () -> StringUtilities.getRandomString(random, 10, 5), "When maxLen < minLen the bound for nextInt is negative, causing IllegalArgumentException");
//     }

    // ------------------- Negative Length Result Empty --------------------
//     @Test
//     void getRandomString_negativeMinLengthResultEmpty() {
//         // FixedRandom returns 0 for nextInt, making len = -2 + 0 = -2 (negative)
//         Random random = new FixedRandom(new int[] { 0 });
//         String result = StringUtilities.getRandomString(random, -2, 2);
//         assertEquals("", result, "Negative computed length should result in an empty string (loop never executes)");
//     }

    // ------------------- Zero Length Possible with minLen=0 ---------------
//     @Test
//     void getRandomString_zeroLengthPossibleWithMinZero() {
//         // FixedRandom always returns 0, forcing length = 0
//         Random random = new FixedRandom(new int[] { 0 });
//         String result = StringUtilities.getRandomString(random, 0, 5);
//         assertEquals("", result, "When nextInt returns 0 and minLen is 0 the method should return an empty string");
//     }

    // ------------------- Both Negative Bounds Return Empty ---------------
//     @Test
//     void getRandomString_bothNegativeBoundsReturnEmpty() {
//         Random random = new Random(12345);
//         String result = StringUtilities.getRandomString(random, -5, -3);
//         assertEquals("", result, "Both bounds negative should always produce a negative length → empty string");
//     }

    // ------------------- Maximum Length Boundary -------------------------
//     @Test
//     void getRandomString_maximumLengthBoundary() {
//         // FixedRandom returns the highest possible value (bound-1 = 2) -> length = 5
//         Random random = new FixedRandom(new int[] { 2 });
//         int min = 3;
//         int max = 5;
//         String result = StringUtilities.getRandomString(random, min, max);
//         assertEquals(5, result.length(), "With nextInt returning the max possible value, length should be maxLen");
//         assertTrue(Character.isUpperCase(result.charAt(0)), "First character must be uppercase");
//     }

    // ------------------- Large Fixed Length -------------------------------
//     @Test
//     void getRandomString_largeFixedLength() {
//         Random random = new Random(99);
//         int len = 100;
//         String result = StringUtilities.getRandomString(random, len, len);
//         assertEquals(len, result.length(), "Large fixed length should be honoured");
//         assertTrue(Character.isUpperCase(result.charAt(0)), "First character must be uppercase");
//     }

    // ------------------- Overflow Bound Throws IllegalArgumentException ----
//     @Test
//     void getRandomString_overflowBound_negativeMinLargeMax_throwsIllegalArgumentException() {
//         Random random = new Random(1);
//         // min = -100, max = Integer.MAX_VALUE -> bound overflows to negative
//         assertThrows(IllegalArgumentException.class, () -> StringUtilities.getRandomString(random, -100, Integer.MAX_VALUE), "Overflowing bound should cause IllegalArgumentException from Random.nextInt");
//     }

//     @Test
//     void getRandomString_overflowBound_extremeValues_throwsIllegalArgumentException() {
//         Random random = new Random(42);
//         // min = Integer.MIN_VALUE, max = Integer.MAX_VALUE -> bound wraps to 0
//         assertThrows(IllegalArgumentException.class, () -> StringUtilities.getRandomString(random, Integer.MIN_VALUE, Integer.MAX_VALUE), "Bound that wraps to zero must cause IllegalArgumentException");
//     }

//     @Test
//     void getRandomString_nullRandom_throwsNullPointerException_original() {
//         assertThrows(NullPointerException.class, () -> StringUtilities.getRandomString(null, 1, 5));
//     }

//     @Test
//     void getRandomString_minGreaterThanMax_throwsIllegalArgumentException_original() {
//         Random random = new Random();
//         // random.nextInt will be called with a negative bound, which throws IllegalArgumentException
//         assertThrows(IllegalArgumentException.class, () -> StringUtilities.getRandomString(random, 10, 5));
//     }
}