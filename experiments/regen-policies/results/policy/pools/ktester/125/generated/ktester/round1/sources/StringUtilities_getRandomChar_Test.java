package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.util.Random;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringUtilities_getRandomChar_Test {

    @Mock
    Random // injected by Mockito
    mockRandom;

    private static Stream<Arguments> uppercaseProvider() {
        return Stream.of(// first uppercase letter
        Arguments.of(0, 'A'), // last uppercase letter
        Arguments.of(25, 'Z'), // middle uppercase letter
        Arguments.of(13, 'N'), // additional values
        Arguments.of(1, 'B'), Arguments.of(24, 'Y'), Arguments.of(5, 'F'));
    }

    private static Stream<Arguments> lowercaseProvider() {
        return Stream.of(// first lowercase letter
        Arguments.of(0, 'a'), // last lowercase letter
        Arguments.of(25, 'z'), // middle lowercase letter
        Arguments.of(13, 'n'), // additional values
        Arguments.of(1, 'b'), Arguments.of(24, 'y'), Arguments.of(5, 'f'));
    }

    /**
     * Verify that when {@code upper} is {@code true} the returned character is an
     * uppercase letter between 'A' and 'Z' (inclusive) and that the underlying
     * {@link Random#nextInt(int)} is invoked with the expected bound (26).
     */
    @Test
    void getRandomChar_upperCase_returnsUppercaseLetter() {
        // Arrange: make the mock return a deterministic value, e.g. 0 → 'A'
        when(mockRandom.nextInt(26)).thenReturn(0);
        // Act
        String result = StringUtilities.getRandomChar(mockRandom, true);
        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.charAt(0)).isBetween('A', 'Z');
        // because we forced nextInt to 0
        assertThat(result).isEqualTo("A");
        verify(mockRandom, times(1)).nextInt(26);
    }

    /**
     * Verify that when {@code upper} is {@code false} the returned character is a
     * lowercase letter between 'a' and 'z' (inclusive).
     */
    @Test
    void getRandomChar_lowerCase_returnsLowercaseLetter() {
        // Arrange: force nextInt to return the maximum possible index (25) → 'z'
        when(mockRandom.nextInt(26)).thenReturn(25);
        // Act
        String result = StringUtilities.getRandomChar(mockRandom, false);
        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.charAt(0)).isBetween('a', 'z');
        // 25 offset from 'a' yields 'z'
        assertThat(result).isEqualTo("z");
        verify(mockRandom, times(1)).nextInt(26);
    }

    /**
     * Verify that the method works for a range of random values.
     * The test iterates through several possible return values from {@code Random}
     * and checks that the produced character matches the expected one.
     */
    @Test
    void getRandomChar_randomValues_matchExpectedCharacters() {
        for (int i = 0; i < 26; i++) {
            // Reset mock for each iteration
            reset(mockRandom);
            when(mockRandom.nextInt(26)).thenReturn(i);
            // Uppercase case
            String upper = StringUtilities.getRandomChar(mockRandom, true);
            char expectedUpper = (char) ('A' + i);
            assertThat(upper).isEqualTo(String.valueOf(expectedUpper));
            // Lowercase case
            String lower = StringUtilities.getRandomChar(mockRandom, false);
            char expectedLower = (char) ('a' + i);
            assertThat(lower).isEqualTo(String.valueOf(expectedLower));
            // called once per invocation
            verify(mockRandom, times(2)).nextInt(26);
        }
    }

    /**
     * Ensure that a {@link NullPointerException} is thrown when a {@code null}
     * {@link Random} instance is supplied. The method does not guard against a
     * null argument, so the NPE originates from the call to {@code random.nextInt}.
     */
    @Test
    void getRandomChar_nullRandom_throwsNullPointerException() {
        assertThatThrownBy(() -> StringUtilities.getRandomChar(null, true)).isInstanceOf(NullPointerException.class);
    }

    /* --------------------------------------------------------------
     *  Uppercase character generation
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "upper={0}, index={1} → expected=''{2}''")
    @MethodSource("uppercaseProvider")
    void getRandomChar_uppercaseGeneration(int index, char expected) {
        // Arrange
        when(mockRandom.nextInt(26)).thenReturn(index);
        // Act
        String result = StringUtilities.getRandomChar(mockRandom, true);
        // Assert
        assertThat(result).hasSize(1).isEqualTo(String.valueOf(expected));
        verify(mockRandom, times(1)).nextInt(26);
    }

    /* --------------------------------------------------------------
     *  Lowercase character generation
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "lower={0}, index={1} → expected=''{2}''")
    @MethodSource("lowercaseProvider")
    void getRandomChar_lowercaseGeneration(int index, char expected) {
        // Arrange
        when(mockRandom.nextInt(26)).thenReturn(index);
        // Act
        String result = StringUtilities.getRandomChar(mockRandom, false);
        // Assert
        assertThat(result).hasSize(1).isEqualTo(String.valueOf(expected));
        verify(mockRandom, times(1)).nextInt(26);
    }

    /* --------------------------------------------------------------
     *  Random bound verification
     * -------------------------------------------------------------- */
    @Test
    void getRandomChar_alwaysCallsNextIntWithBound26() {
        // Arrange – the mock will return any value; we only care about the bound
        when(mockRandom.nextInt(anyInt())).thenReturn(7);
        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
        // Act
        StringUtilities.getRandomChar(mockRandom, true);
        StringUtilities.getRandomChar(mockRandom, false);
        // Assert – capture both invocations and verify the bound is always 26
        verify(mockRandom, times(2)).nextInt(captor.capture());
        assertThat(captor.getAllValues()).containsExactly(26, 26);
    }

    /* --------------------------------------------------------------
     *  Exception propagation from Random – IllegalStateException
     * -------------------------------------------------------------- */
    @Test
    void getRandomChar_randomThrowsIllegalStateException_propagates() {
        // Arrange
        when(mockRandom.nextInt(26)).thenThrow(new IllegalStateException("random failure"));
        // Act / Assert
        assertThatThrownBy(() -> StringUtilities.getRandomChar(mockRandom, false)).isInstanceOf(IllegalStateException.class).hasMessageContaining("random failure");
    }

    /* --------------------------------------------------------------
     *  Exception propagation from Random – IllegalArgumentException
     * -------------------------------------------------------------- */
    @Test
    void getRandomChar_randomThrowsIllegalArgumentException_propagates() {
        // Arrange
        when(mockRandom.nextInt(26)).thenThrow(new IllegalArgumentException("bad bound"));
        // Act / Assert
        assertThatThrownBy(() -> StringUtilities.getRandomChar(mockRandom, true)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("bad bound");
    }

    /* --------------------------------------------------------------
     *  Result string immutability
     * -------------------------------------------------------------- */
    @Test
    void getRandomChar_twoCallsWithSameRandomValue_returnEqualButDistinctStrings() {
        // Arrange – always return 2 → 'C'/'c'
        when(mockRandom.nextInt(26)).thenReturn(2);
        // Act
        String first = StringUtilities.getRandomChar(mockRandom, true);
        String second = StringUtilities.getRandomChar(mockRandom, true);
        // Assert – values are equal but not the same object (no interning)
        assertThat(first).isEqualTo(second);
        assertThat(first).isNotSameAs(second);
        verify(mockRandom, times(2)).nextInt(26);
    }
}
