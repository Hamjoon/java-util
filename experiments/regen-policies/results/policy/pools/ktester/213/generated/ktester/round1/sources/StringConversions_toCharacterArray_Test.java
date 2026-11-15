package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mockito;
import java.io.CharArrayReader;
import java.nio.CharBuffer;

@ExtendWith(MockitoExtension.class)
class StringConversions_toCharacterArray_Test {

    /**
     * The Converter instance used for the tests.
     */
    private static Converter converter;

    /**
     * A {@link CharSequence} whose {@code length()} method always throws
     * {@link IllegalStateException}.  Used to verify that {@code toCharacterArray}
     * does not swallow such exceptions.
     */
    private static final class LengthThrowingCharSequence implements CharSequence {

        @Override
        public int length() {
            throw new IllegalStateException("length() failure");
        }

        @Override
        public char charAt(int index) {
            return 'x';
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return "";
        }

        @Override
        public String toString() {
            return "";
        }
    }

    /**
     * A {@link CharSequence} whose {@code charAt(int)} method always throws
     * {@link IndexOutOfBoundsException}.  The {@code length()} method returns a
     * positive value so that the loop in {@code toCharacterArray} attempts to call
     * {@code charAt}.
     */
    private static final class CharAtThrowingCharSequence implements CharSequence {

        private final int len;

        CharAtThrowingCharSequence(String dummy) {
            this.len = dummy.length();
        }

        @Override
        public int length() {
            return len;
        }

        @Override
        public char charAt(int index) {
            throw new IndexOutOfBoundsException("charAt() failure at " + index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return "";
        }

        @Override
        public String toString() {
            return "";
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance – use the default constructor.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – helps the GC when many test classes are run.
        converter = null;
    }

    @Test
    void toCharacterArray_convertsStringCorrectly() {
        // given
        String source = "Hello";
        // when
        Character[] result = StringConversions.toCharacterArray(source, converter);
        // then
        assertNotNull(result, "Result should never be null for a non‑null source");
        assertEquals(source.length(), result.length, "Result length must match source length");
        for (int i = 0; i < source.length(); i++) {
            assertEquals(source.charAt(i), result[i].charValue(), "Character at index " + i + " is not as expected");
        }
    }

    @Test
    void toCharacterArray_emptyStringReturnsEmptyArray() {
        // given
        String source = "";
        // when
        Character[] result = StringConversions.toCharacterArray(source, converter);
        // then
        assertNotNull(result, "Result should not be null for an empty string");
        assertEquals(0, result.length, "Result array must be empty");
    }

    @Test
    void toCharacterArray_nullSourceThrowsClassCastException() {
        // The method casts the input to CharSequence without a null‑check.
        // Passing null therefore results in a NullPointerException (or ClassCastException).
        // We assert that a NullPointerException is thrown, which is the actual behaviour.
        assertThrows(NullPointerException.class, () -> StringConversions.toCharacterArray(null, converter), "Calling toCharacterArray with null should throw NPE");
    }

    @Test
    void toCharacterArray_charSequenceImplementationIsHandled() {
        // given
        CharSequence source = new StringBuilder("ABC");
        // when
        Character[] result = StringConversions.toCharacterArray(source, converter);
        // then
        assertArrayEquals(new Character[] { 'A', 'B', 'C' }, result, "Result should correctly convert any CharSequence implementation");
    }

    @Test
    void toCharacterArray_stringBuilderIsHandled() {
        // given
        StringBuilder source = new StringBuilder("AB");
        // when
        Character[] result = StringConversions.toCharacterArray(source, converter);
        // then
        assertArrayEquals(new Character[] { 'A', 'B' }, result, "StringBuilder should be treated like any CharSequence");
    }

    @Test
    void toCharacterArray_stringBufferIsHandled() {
        // given
        StringBuffer source = new StringBuffer("XYZ");
        // when
        Character[] result = StringConversions.toCharacterArray(source, converter);
        // then
        assertArrayEquals(new Character[] { 'X', 'Y', 'Z' }, result, "StringBuffer should be treated like any CharSequence");
    }

    @Test
    void toCharacterArray_charBufferIsHandled() {
        // given
        CharBuffer source = CharBuffer.wrap("abc");
        // when
        Character[] result = StringConversions.toCharacterArray(source, converter);
        // then
        assertArrayEquals(new Character[] { 'a', 'b', 'c' }, result, "CharBuffer implements CharSequence and must be converted correctly");
    }

    @Test
    void toCharacterArray_emptyStringBuilderReturnsEmptyArray() {
        // given
        StringBuilder source = new StringBuilder();
        // when
        Character[] result = StringConversions.toCharacterArray(source, converter);
        // then
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    void toCharacterArray_stringWithSpacesAndNullChar() {
        // given – contains a space, a NUL character and another space
        String source = " a\u0000b ";
        // when
        Character[] result = StringConversions.toCharacterArray(source, converter);
        // then
        assertArrayEquals(new Character[] { ' ', 'a', '\u0000', 'b', ' ' }, result, "All characters, including the NUL char, must be preserved");
    }

    @Test
    void toCharacterArray_unicodeSurrogatePair() {
        // given – 😀 is represented by the surrogate pair \uD83D\uDE00
        String source = "\uD83D\uDE00";
        // when
        Character[] result = StringConversions.toCharacterArray(source, converter);
        // then – each surrogate becomes a separate Character element
        assertEquals(2, result.length, "Surrogate pair should result in two Character entries");
        assertEquals('\uD83D', result[0].charValue());
        assertEquals('\uDE00', result[1].charValue());
    }

    @Test
    void toCharacterArray_nonNullConverterIsIgnored() {
        // given – a Mockito mock to prove the implementation does not call any method on it
        Converter mockConverter = Mockito.mock(Converter.class);
        String source = "Test";
        // when
        Character[] result = StringConversions.toCharacterArray(source, mockConverter);
        // then – conversion result must be identical to the normal case
        assertArrayEquals(new Character[] { 'T', 'e', 's', 't' }, result);
        // Verify that no interaction with the mock occurred
        Mockito.verifyNoInteractions(mockConverter);
    }

    @Test
    void toCharacterArray_nullSourceThrowsNullPointerException() {
        // The method casts the input to CharSequence without a null‑check.
        assertThrows(NullPointerException.class, () -> StringConversions.toCharacterArray(null, converter), "Calling toCharacterArray with null should throw NPE");
    }

    @Test
    void toCharacterArray_invalidCharArrayReaderThrowsClassCastException() {
        // CharArrayReader does NOT implement CharSequence
        CharArrayReader source = new CharArrayReader(new char[] { 'a', 'b', 'c' });
        assertThrows(ClassCastException.class, () -> StringConversions.toCharacterArray(source, converter), "CharArrayReader should cause a ClassCastException");
    }

    @Test
    void toCharacterArray_integerSourceThrowsClassCastException() {
        Integer source = Integer.valueOf(123);
        assertThrows(ClassCastException.class, () -> StringConversions.toCharacterArray(source, converter), "Non‑CharSequence input (Integer) must cause ClassCastException");
    }

    @Test
    void toCharacterArray_lengthThrowingCharSequencePropagatesException() {
        CharSequence source = new LengthThrowingCharSequence();
        assertThrows(IllegalStateException.class, () -> StringConversions.toCharacterArray(source, converter), "Exception from length() should be propagated");
    }

    @Test
    void toCharacterArray_charAtThrowingCharSequencePropagatesException() {
        CharSequence source = new CharAtThrowingCharSequence("abc");
        assertThrows(IndexOutOfBoundsException.class, () -> StringConversions.toCharacterArray(source, converter), "Exception from charAt() should be propagated");
    }
}
