package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import java.util.Random;

@ExtendWith(MockitoExtension.class)
class StringUtilities_getRandomChar_Test {

    /**
     * Mocked {@link Random} used to control the output of {@code nextInt}.
     */
    @Mock
    Random mockRandom;

    /**
     * Helper {@link Random} that always returns a fixed integer from {@code nextInt(int)}.
     */
    private static final class FixedRandom extends Random {

        private final int fixedValue;

        FixedRandom(int fixedValue) {
            this.fixedValue = fixedValue;
        }

        @Override
        public int nextInt(int bound) {
            // Ignore the bound – return the pre‑configured value.
            return fixedValue;
        }
    }

    /**
     * Helper {@link Random} that throws a {@link RuntimeException} from {@code nextInt(int)}.
     */
    private static final class ExceptionRandom extends Random {

        @Override
        public int nextInt(int bound) {
            throw new RuntimeException("Intentional exception from Random");
        }
    }

    @Test
    void getRandomChar_returnsUpperCaseLetterWhenUpperIsTrue() {
        // GIVEN – the mock Random will return 0, 25 and a middle value.
        when(mockRandom.nextInt(26)).thenReturn(0, 25, 13);
        // WHEN – upper = true
        String first = StringUtilities.getRandomChar(mockRandom, true);
        String second = StringUtilities.getRandomChar(mockRandom, true);
        String third = StringUtilities.getRandomChar(mockRandom, true);
        // THEN – verify the correct characters are produced.
        assertEquals("A", first, "0 should map to 'A'");
        assertEquals("Z", second, "25 should map to 'Z'");
        assertEquals("N", third, "13 should map to 'N' (0‑based)");
        // All results must be a single‑character string.
        assertEquals(1, first.length());
        assertEquals(1, second.length());
        assertEquals(1, third.length());
    }

    @Test
    void getRandomChar_returnsLowerCaseLetterWhenUpperIsFalse() {
        // GIVEN – the mock Random will return 0, 25 and a middle value.
        when(mockRandom.nextInt(26)).thenReturn(0, 25, 13);
        // WHEN – upper = false
        String first = StringUtilities.getRandomChar(mockRandom, false);
        String second = StringUtilities.getRandomChar(mockRandom, false);
        String third = StringUtilities.getRandomChar(mockRandom, false);
        // THEN – verify the correct lowercase characters.
        assertEquals("a", first, "0 should map to 'a'");
        assertEquals("z", second, "25 should map to 'z'");
        assertEquals("n", third, "13 should map to 'n'");
        // All results must be a single‑character string.
        assertEquals(1, first.length());
        assertEquals(1, second.length());
        assertEquals(1, third.length());
    }

    @Test
    void getRandomChar_isDeterministicForGivenRandomSequence() {
        // GIVEN – a deterministic sequence of ints from the mock.
        when(mockRandom.nextInt(26)).thenReturn(5, 10, 5, 10);
        // WHEN – alternating upper flag.
        // 5 -> 'F'
        String a1 = StringUtilities.getRandomChar(mockRandom, true);
        // 10 -> 'k'
        String a2 = StringUtilities.getRandomChar(mockRandom, false);
        // 5 -> 'F' again
        String a3 = StringUtilities.getRandomChar(mockRandom, true);
        // 10 -> 'k' again
        String a4 = StringUtilities.getRandomChar(mockRandom, false);
        // THEN – the method should produce the same characters for the same int.
        assertEquals("F", a1);
        assertEquals("k", a2);
        assertEquals("F", a3);
        assertEquals("k", a4);
    }

    @Test
    void getRandomChar_nullRandom_throwsNullPointerException() {
        // GIVEN a null Random reference.
        Random nullRandom = null;
        // WHEN / THEN – a NullPointerException is expected because the method dereferences the argument.
        assertThrows(NullPointerException.class, () -> StringUtilities.getRandomChar(nullRandom, true), "Passing null Random should throw NullPointerException");
    }

    @Test
    void getRandomChar_outOfRangeHighValue_returnsBracketCharacter() {
        // GIVEN – a Random that incorrectly returns 26 (one beyond the valid range).
        Random r = new FixedRandom(26);
        // WHEN – upper case flag.
        String result = StringUtilities.getRandomChar(r, true);
        // THEN – ASCII 65 ('A') + 26 = 91 which is '['.
        assertEquals("[", result, "Value 26 should map to '['");
        assertEquals(1, result.length(), "Result must still be a single character");
    }

    @Test
    void getRandomChar_outOfRangeLowValue_returnsBacktickCharacter() {
        // GIVEN – a Random that incorrectly returns -1.
        Random r = new FixedRandom(-1);
        // WHEN – lower case flag.
        String result = StringUtilities.getRandomChar(r, false);
        // THEN – ASCII 97 ('a') - 1 = 96 which is '`'.
        assertEquals("`", result, "Value -1 should map to '`'");
        assertEquals(1, result.length(), "Result must still be a single character");
    }

    @Test
    void getRandomChar_randomThrowsRuntimeException_propagatesException() {
        // GIVEN – a Random implementation that throws RuntimeException.
        Random r = new ExceptionRandom();
        // WHEN / THEN – the exception should bubble up unchanged.
        RuntimeException ex = assertThrows(RuntimeException.class, () -> StringUtilities.getRandomChar(r, true), "Exception from Random.nextInt should propagate");
        assertEquals("Intentional exception from Random", ex.getMessage());
    }

    @Test
    void getRandomChar_returnsNewStringInstance_notSameAsEMPTY() {
        // GIVEN – a deterministic Random.
        Random r = new FixedRandom(0);
        // WHEN – request an upper‑case character.
        String result = StringUtilities.getRandomChar(r, true);
        // THEN – the returned instance must not be the same reference as StringUtilities.EMPTY.
        assertNotSame(StringUtilities.EMPTY, result, "Result should be a new String object, not the shared EMPTY constant");
        // Also verify the content is correct.
        assertEquals("A", result);
    }

    @Test
    void getRandomChar_resultLengthIsExactlyOne() {
        // GIVEN – any random value (5 chosen arbitrarily).
        Random r = new FixedRandom(5);
        // WHEN – request a lower‑case character.
        String result = StringUtilities.getRandomChar(r, false);
        // THEN – length must be exactly 1 irrespective of the random value.
        assertEquals(1, result.length(), "The method must always return a single‑character string");
        // Verify the actual character for completeness.
        assertEquals("f", result);
    }
}
