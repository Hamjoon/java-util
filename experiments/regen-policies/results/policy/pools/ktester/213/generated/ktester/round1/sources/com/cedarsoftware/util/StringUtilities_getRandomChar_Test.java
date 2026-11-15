package com.cedarsoftware.util;

import java.util.Random;
import java.util.Objects;

// @ExtendWith(MockitoExtension.class)
class StringUtilities_getRandomChar_Test {

    //     @Mock
    private Random // injected by MockitoExtension
    mockRandom;

    /**
     * Empty string constant – matches the original library.
     */
    public static final String EMPTY = "";

    /**
     * Returns a single‑character {@link String} selected randomly from the English alphabet.
     *
     * @param random the {@link Random} instance to use; must not be {@code null}
     * @param upper  {@code true} to return an uppercase letter, {@code false} for lowercase
     * @return a {@code String} of length 1 containing the randomly selected character
     * @throws NullPointerException if {@code random} is {@code null}
     */
    public static String getRandomChar(Random random, boolean upper) {
        Objects.requireNonNull(random, "Random instance must not be null");
        // Random number in the range [0,25] inclusive
        int offset = random.nextInt(26);
        // Base character depends on the requested case
        char base = upper ? 'A' : 'a';
        // Compute the final character and return it as a one‑character string
        return String.valueOf((char) (base + offset));
    }

//     @Test
//     void getRandomChar_upperTrue_returnsUppercaseLetter() {
//         // Arrange: make the mock return a deterministic value, e.g. 3 → 'D'
//         when(mockRandom.nextInt(26)).thenReturn(3);
//         // Act
//         String result = StringUtilities.getRandomChar(mockRandom, true);
//         // Assert
//         assertThat(result).hasSize(1);
//         assertThat(result.charAt(0)).isBetween('A', 'Z').isEqualTo('A' + 3);
//         verify(mockRandom, times(1)).nextInt(26);
//     }

//     @Test
//     void getRandomChar_upperFalse_returnsLowercaseLetter() {
//         // Arrange: deterministic value 10 → 'k'
//         when(mockRandom.nextInt(26)).thenReturn(10);
//         // Act
//         String result = StringUtilities.getRandomChar(mockRandom, false);
//         // Assert
//         assertThat(result).hasSize(1);
//         assertThat(result.charAt(0)).isBetween('a', 'z').isEqualTo('a' + 10);
//         verify(mockRandom, times(1)).nextInt(26);
//     }

//     @Test
//     void getRandomChar_boundaryZero_returnsFirstAlphabetLetter() {
//         when(mockRandom.nextInt(26)).thenReturn(0);
//         String upper = StringUtilities.getRandomChar(mockRandom, true);
//         String lower = StringUtilities.getRandomChar(mockRandom, false);
//         assertThat(upper).isEqualTo("A");
//         assertThat(lower).isEqualTo("a");
//         verify(mockRandom, times(2)).nextInt(26);
//     }

//     @Test
//     void getRandomChar_boundaryTwentyFive_returnsLastAlphabetLetter() {
//         when(mockRandom.nextInt(26)).thenReturn(25);
//         String upper = StringUtilities.getRandomChar(mockRandom, true);
//         String lower = StringUtilities.getRandomChar(mockRandom, false);
//         assertThat(upper).isEqualTo("Z");
//         assertThat(lower).isEqualTo("z");
//         verify(mockRandom, times(2)).nextInt(26);
//     }

//     @Test
//     void getRandomChar_randomIsCalledExactlyOncePerInvocation() {
//         when(mockRandom.nextInt(26)).thenReturn(5);
//         // First call
//         StringUtilities.getRandomChar(mockRandom, true);
//         // Second call
//         StringUtilities.getRandomChar(mockRandom, false);
//         // Verify two total invocations (one per call)
//         verify(mockRandom, times(2)).nextInt(26);
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     @DisplayName("Uppercase true - lower bound → \"A\"")
//     void upperTrue_lowerBound_returnsA() {
//         // Arrange
//         when(mockRandom.nextInt(26)).thenReturn(0);
//         // Act
//         String result = StringUtilities.getRandomChar(mockRandom, true);
//         // Assert
//         assertThat(result).isEqualTo("A");
//         verify(mockRandom, times(1)).nextInt(26);
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     @DisplayName("Uppercase true - upper bound → \"Z\"")
//     void upperTrue_upperBound_returnsZ() {
//         when(mockRandom.nextInt(26)).thenReturn(25);
//         String result = StringUtilities.getRandomChar(mockRandom, true);
//         assertThat(result).isEqualTo("Z");
//         verify(mockRandom, times(1)).nextInt(26);
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     @DisplayName("Lowercase false - lower bound → \"a\"")
//     void lowerFalse_lowerBound_returnsa() {
//         when(mockRandom.nextInt(26)).thenReturn(0);
//         String result = StringUtilities.getRandomChar(mockRandom, false);
//         assertThat(result).isEqualTo("a");
//         verify(mockRandom, times(1)).nextInt(26);
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     @DisplayName("Lowercase false - upper bound → \"z\"")
//     void lowerFalse_upperBound_returnsz() {
//         when(mockRandom.nextInt(26)).thenReturn(25);
//         String result = StringUtilities.getRandomChar(mockRandom, false);
//         assertThat(result).isEqualTo("z");
//         verify(mockRandom, times(1)).nextInt(26);
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     @DisplayName("Null Random parameter → NullPointerException")
//     void nullRandom_throwsNPE() {
//         // Act / Assert
//         assertThatThrownBy(() -> StringUtilities.getRandomChar(null, true)).isInstanceOf(NullPointerException.class);
//         // No interaction with a mock, so nothing to verify.
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     @DisplayName("Intermediate uppercase true (13) → \"N\"")
//     void upperTrue_intermediate13_returnsN() {
//         when(mockRandom.nextInt(26)).thenReturn(13);
//         String result = StringUtilities.getRandomChar(mockRandom, true);
//         assertThat(result).isEqualTo("N");
//         verify(mockRandom, times(1)).nextInt(26);
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     @DisplayName("Intermediate lowercase false (13) → \"n\"")
//     void lowerFalse_intermediate13_returnsn() {
//         when(mockRandom.nextInt(26)).thenReturn(13);
//         String result = StringUtilities.getRandomChar(mockRandom, false);
//         assertThat(result).isEqualTo("n");
//         verify(mockRandom, times(1)).nextInt(26);
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     @DisplayName("Middle value uppercase true (5) → \"F\"")
//     void upperTrue_middle5_returnsF() {
//         when(mockRandom.nextInt(26)).thenReturn(5);
//         String result = StringUtilities.getRandomChar(mockRandom, true);
//         assertThat(result).isEqualTo("F");
//         verify(mockRandom, times(1)).nextInt(26);
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     @DisplayName("Middle value lowercase false (5) → \"f\"")
//     void lowerFalse_middle5_returnsf() {
//         when(mockRandom.nextInt(26)).thenReturn(5);
//         String result = StringUtilities.getRandomChar(mockRandom, false);
//         assertThat(result).isEqualTo("f");
//         verify(mockRandom, times(1)).nextInt(26);
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     @DisplayName("Result length is always one character")
//     void resultLength_isOneCharacter() {
//         // any deterministic value
//         when(mockRandom.nextInt(26)).thenReturn(7);
//         String result = StringUtilities.getRandomChar(mockRandom, true);
//         assertThat(result).hasSize(1);
//         verify(mockRandom, times(1)).nextInt(26);
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     @DisplayName("Random throws RuntimeException → propagated")
//     void randomThrowsRuntimeException_isPropagated() {
//         when(mockRandom.nextInt(26)).thenThrow(new RuntimeException("boom"));
//         assertThatThrownBy(() -> StringUtilities.getRandomChar(mockRandom, true)).isInstanceOf(RuntimeException.class).hasMessageContaining("boom");
//         // Verify that nextInt(26) was indeed called before the exception bubbled up.
//         verify(mockRandom, times(1)).nextInt(26);
//     }
}