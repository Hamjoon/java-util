package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.CharBuffer;
import java.util.stream.Stream;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mockito;

@ExtendWith(MockitoExtension.class)
class CharBufferConversions_toCharArray_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

    /**
     * A real Converter used for the majority of test cases (identity behaviour).
     */
    private static Converter realConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Use the default options – the concrete behaviour of the converter
        // is not under test here; we only need a functional instance.
        converter = new Converter(new ConverterOptions());
        realConverter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the shared converter reference.
        converter = null;
    }

    /**
     * Provides a stream of {@link TestCase}s built from the JSON specification.
     */
    private static Stream<Arguments> testCasesProvider() {
        // ---- identity converter (real instance) ---------------------------------
        Converter identity = realConverter;
        // ---- int‑to‑String converter (mock) ---------------------------------------
        Converter intToString = Mockito.mock(Converter.class);
        Mockito.when(intToString.convert(Mockito.eq(123), Mockito.eq(String.class))).thenReturn("123");
        // ---- null‑returning converter (mock) ---------------------------------------
        Converter nullReturning = Mockito.mock(Converter.class);
        Mockito.when(nullReturning.convert(Mockito.any(), Mockito.any())).thenReturn(null);
        // ---- exception‑throwing converter (mock) ---------------------------------
        Converter exceptionThrowing = Mockito.mock(Converter.class);
        Mockito.when(exceptionThrowing.convert(Mockito.any(), Mockito.any())).thenThrow(new RuntimeException("forced exception"));
        // ---- test cases -----------------------------------------------------------
        return Stream.of(/* Empty buffer scenarios */
        Arguments.of(new TestCase(// from
        "", // converter
        identity, // expected
        new char[0], null, "Empty String should yield an empty char array")), Arguments.of(new TestCase(CharBuffer.wrap(new char[] {}), identity, new char[0], null, "CharBuffer with no characters should yield an empty char array")), Arguments.of(new TestCase(// position set to limit → no remaining characters
        CharBuffer.wrap(new char[] { 'a' }).position(1), identity, new char[0], null, "CharBuffer whose position equals limit must produce an empty array")), /* Non‑empty buffer scenarios */
        Arguments.of(new TestCase("hello", identity, new char[] { 'h', 'e', 'l', 'l', 'o' }, null, "String \"hello\" should be converted to its characters")), Arguments.of(new TestCase(CharBuffer.wrap(new char[] { 'p', 'q' }), identity, new char[] { 'p', 'q' }, null, "Direct CharBuffer should be read unchanged")), /* Converter usage */
        Arguments.of(new TestCase(123, intToString, new char[] { '1', '2', '3' }, null, "Custom converter turning Integer 123 into \"123\"")), /* Null input scenarios */
        Arguments.of(new TestCase(null, identity, new char[0], null, "Null source should be treated as empty input")), Arguments.of(new TestCase("test", nullReturning, new char[0], null, "Converter returning null should result in an empty array")), /* Partial CharBuffer scenarios */
        Arguments.of(new TestCase(CharBuffer.wrap(new char[] { 'a', 'b', 'c', 'd' }).position(2), identity, new char[] { 'c', 'd' }, null, "CharBuffer with position 2 leaves two remaining characters")), Arguments.of(new TestCase(CharBuffer.wrap(new char[] { 'x', 'y', 'z' }).limit(2), identity, new char[] { 'x', 'y' }, null, "CharBuffer with limit smaller than capacity should respect the limit")), /* Byte array conversion */
        Arguments.of(new TestCase(// ASCII for A,B,C
        new byte[] { 65, 66, 67 }, identity, new char[] { 'A', 'B', 'C' }, null, "byte[] with ASCII values should become corresponding chars")), Arguments.of(new TestCase(new byte[] {}, identity, new char[0], null, "Empty byte[] must result in an empty char array")), /* Char array conversion */
        Arguments.of(new TestCase(new char[] { 'm', 'n', 'o' }, identity, new char[] { 'm', 'n', 'o' }, null, "Raw char[] should be wrapped and returned unchanged")), Arguments.of(new TestCase(new char[] {}, identity, new char[0], null, "Empty char[] should produce an empty char array")), /* StringBuilder / StringBuffer conversion */
        Arguments.of(new TestCase(new StringBuilder("pqrs"), identity, new char[] { 'p', 'q', 'r', 's' }, null, "StringBuilder should be converted to its characters")), Arguments.of(new TestCase(new StringBuffer(""), identity, new char[0], null, "Empty StringBuffer should yield an empty array")), /* Converter throwing exception */
        Arguments.of(new TestCase("trigger", exceptionThrowing, null, RuntimeException.class, "Exception from converter must be propagated")), /* Null converter exception */
        Arguments.of(new TestCase("sample", null, null, NullPointerException.class, "Passing a null Converter should cause NPE")));
    }

    @Test
    void toCharArray_WithNonEmptyCharBuffer_ReturnsCorrectArray() {
        // GIVEN
        CharBuffer buffer = CharBuffer.wrap(new char[] { 'a', 'b', 'c' });
        // WHEN
        char[] result = CharBufferConversions.toCharArray(buffer, converter);
        // THEN
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result, "The char array should contain the same characters as the source CharBuffer");
    }

    @Test
    void toCharArray_WithEmptyCharBuffer_ReturnsEmptyArray() {
        // GIVEN
        // no remaining characters
        CharBuffer emptyBuffer = CharBuffer.allocate(0);
        // WHEN
        char[] result = CharBufferConversions.toCharArray(emptyBuffer, converter);
        // THEN
        assertNotNull(result, "Result should never be null");
        assertEquals(0, result.length, "Resulting array should be empty for an empty CharBuffer");
    }

    @Test
    void toCharArray_WithNullSource_ReturnsEmptyArray() {
        // GIVEN
        Object nullSource = null;
        // WHEN
        char[] result = CharBufferConversions.toCharArray(nullSource, converter);
        // THEN
        assertNotNull(result, "Result should never be null even when source is null");
        assertEquals(0, result.length, "Resulting array should be empty when source is null");
    }

    @ParameterizedTest(name = "{index}: {0.description}")
    @MethodSource("testCasesProvider")
    void toCharArray_VariousScenarios(TestCase tc) {
        // Act & Assert
        if (tc.expectedException() != null) {
            // Expect an exception to be thrown
            assertThrows(tc.expectedException(), () -> CharBufferConversions.toCharArray(tc.from(), tc.converter()), tc.description());
        } else {
            // Normal execution – result must match expected array
            char[] result = CharBufferConversions.toCharArray(tc.from(), tc.converter());
            assertNotNull(result, "Result must never be null");
            assertArrayEquals(tc.expected(), result, "Unexpected char array for case: " + tc.description());
        }
    }
}
