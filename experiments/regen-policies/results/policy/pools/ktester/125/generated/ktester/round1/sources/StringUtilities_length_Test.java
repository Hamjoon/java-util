package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.nio.CharBuffer;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringUtilities_length_Test {

    static Stream<Arguments> additionalCharSequenceProvider() {
        // StringBuffer
        // length 8
        StringBuffer sbuf = new StringBuffer("buffered");
        // CharBuffer wrapping "abcd"
        // length 4
        CharBuffer cbuf = CharBuffer.wrap(new char[] { 'a', 'b', 'c', 'd' });
        // Custom CharSequence that reports zero length but toString() is non‑empty
        CharSequence customZero = new CharSequence() {

            @Override
            public int length() {
                return 0;
            }

            @Override
            public char charAt(int index) {
                throw new IndexOutOfBoundsException();
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                return "";
            }

            @Override
            public String toString() {
                return "nonEmpty";
            }
        };
        // Custom CharSequence that returns a negative length
        CharSequence customNegative = new CharSequence() {

            @Override
            public int length() {
                return -5;
            }

            @Override
            public char charAt(int index) {
                throw new IndexOutOfBoundsException();
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                return "";
            }

            @Override
            public String toString() {
                return "neg";
            }
        };
        // Large string of exactly 10 000 characters
        String large = "x".repeat(10_000);
        // String implements CharSequence
        CharSequence largeSeq = large;
        return Stream.of(Arguments.of("StringBuffer containing \"buffered\"", sbuf, 8), Arguments.of("CharBuffer wrapping \"abcd\"", cbuf, 4), Arguments.of("Custom CharSequence with zero length (toString non‑empty)", customZero, 0), Arguments.of("Custom CharSequence with negative length", customNegative, -5), Arguments.of("Large String of length 10 000", largeSeq, 10_000));
    }

    static Stream<Arguments> exceptionCharSequenceProvider() {
        // CharSequence that throws NullPointerException from length()
        CharSequence throwsNPE = new CharSequence() {

            @Override
            public int length() {
                throw new NullPointerException("forced NPE");
            }

            @Override
            public char charAt(int index) {
                return 'a';
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                return "";
            }
        };
        // CharSequence that throws IllegalStateException from length()
        CharSequence throwsISE = new CharSequence() {

            @Override
            public int length() {
                throw new IllegalStateException("forced ISE");
            }

            @Override
            public char charAt(int index) {
                return 'a';
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                return "";
            }
        };
        // CharSequence that throws generic RuntimeException from length()
        CharSequence throwsRE = new CharSequence() {

            @Override
            public int length() {
                throw new RuntimeException("forced RE");
            }

            @Override
            public char charAt(int index) {
                return 'a';
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                return "";
            }
        };
        // CharSequence that throws UnsupportedOperationException from length()
        CharSequence throwsUOE = new CharSequence() {

            @Override
            public int length() {
                throw new UnsupportedOperationException("forced UOE");
            }

            @Override
            public char charAt(int index) {
                return 'a';
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                return "";
            }
        };
        // CharSequence that returns negative length on first call, then throws on subsequent calls.
        // The utility method should call length() only once, thus returning the negative value.
        CharSequence negativeThenThrow = new CharSequence() {

            private int calls = 0;

            @Override
            public int length() {
                calls++;
                if (calls == 1) {
                    return -1;
                }
                throw new RuntimeException("second call");
            }

            @Override
            public char charAt(int index) {
                return 'a';
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                return "";
            }
        };
        // For the last case we expect the negative value, not an exception.
        // It will be covered in a separate test below.
        return Stream.of(Arguments.of("CharSequence whose length() throws NullPointerException", throwsNPE, NullPointerException.class), Arguments.of("CharSequence whose length() throws IllegalStateException", throwsISE, IllegalStateException.class), Arguments.of("CharSequence whose length() throws RuntimeException", throwsRE, RuntimeException.class), Arguments.of("CharSequence whose length() throws UnsupportedOperationException", throwsUOE, UnsupportedOperationException.class));
    }

    @Test
    void length_whenNull_returnsZero() {
        assertEquals(0, StringUtilities.length(null));
    }

    @Test
    void length_whenEmptyString_returnsZero() {
        assertEquals(0, StringUtilities.length(""));
    }

    @Test
    void length_whenNonEmptyString_returnsCorrectLength() {
        String s = "Hello, world!";
        assertEquals(s.length(), StringUtilities.length(s));
    }

    @Test
    void length_whenStringBuilder_returnsCorrectLength() {
        StringBuilder sb = new StringBuilder("StringBuilder");
        assertEquals(sb.length(), StringUtilities.length(sb));
    }

    @Test
    void length_whenCustomCharSequence_returnsCorrectLength() {
        CharSequence custom = new CharSequence() {

            private final String data = "CustomSeq";

            @Override
            public int length() {
                return data.length();
            }

            @Override
            public char charAt(int index) {
                return data.charAt(index);
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                return data.subSequence(start, end);
            }

            @Override
            public String toString() {
                return data;
            }
        };
        assertEquals(custom.length(), StringUtilities.length(custom));
    }

    @Test
    void length_whenCharSequenceWithZeroLength_returnsZero() {
        CharSequence zeroLength = new CharSequence() {

            @Override
            public int length() {
                return 0;
            }

            @Override
            public char charAt(int index) {
                throw new IndexOutOfBoundsException();
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                return "";
            }
        };
        assertEquals(0, StringUtilities.length(zeroLength));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("additionalCharSequenceProvider")
    void length_additionalCharSequences(String description, CharSequence cs, int expected) {
        assertEquals(expected, StringUtilities.length(cs), description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exceptionCharSequenceProvider")
    void length_whenCharSequenceThrowsException(String description, CharSequence cs, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> StringUtilities.length(cs), description);
    }

    @Test
    void length_whenCharSequenceReturnsNegativeAndThrowsOnSecondCall_returnsNegativeOnly() {
        // This CharSequence returns -1 on the first call and would throw on any subsequent call.
        // StringUtilities.length should invoke length() exactly once.
        CharSequence cs = new CharSequence() {

            private int calls = 0;

            @Override
            public int length() {
                calls++;
                if (calls == 1) {
                    return -1;
                }
                throw new RuntimeException("Should not be called a second time");
            }

            @Override
            public char charAt(int index) {
                return 'a';
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                return "";
            }
        };
        assertEquals(-1, StringUtilities.length(cs));
    }
}
