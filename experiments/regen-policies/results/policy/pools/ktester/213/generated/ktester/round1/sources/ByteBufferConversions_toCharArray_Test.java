package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.CharBuffer;
import java.util.Objects;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ByteBufferConversions_toCharArray_Test {

    /**
     * Shared {@code Converter} instance used by all tests.
     */
    private static Converter sharedConverter;

    /**
     * Simple holder for a single test case.
     */
    private static final class TestCase {

        final String description;

        final Object from;

        // may be null for the “null converter” case
        final Converter converter;

        // null when an exception is expected
        final char[] expectedArray;

        // null when success is expected
        final Class<? extends Throwable> expectedException;

        TestCase(String description, Object from, Converter converter, char[] expectedArray, Class<? extends Throwable> expectedException) {
            this.description = description;
            this.from = from;
            this.converter = converter;
            this.expectedArray = expectedArray;
            this.expectedException = expectedException;
        }
    }

    /**
     * A {@link Converter} implementation that throws a {@link RuntimeException}
     * whenever any conversion method is invoked.  It is used to verify that
     * {@code ByteBufferConversions.toCharArray} propagates runtime exceptions from
     * the supplied converter.
     */
    private static final class ThrowingConverter extends Converter {

        ThrowingConverter() {
            super(new ConverterOptions());
        }

        @Override
        public <T> T convert(Object from, Class<T> toClass) {
            throw new RuntimeException("Intentional exception from ThrowingConverter");
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // The real Converter does not need any special configuration for these delegation tests.
        sharedConverter = new Converter(new ConverterOptions());
    }

    /**
     * Supplies all test cases defined in the JSON document.
     */
    private static Stream<Arguments> provideTestCases() {
        // Helper to create a char[] from a string literal (e.g. "hello" -> {'h','e','l','l','o'})
        java.util.function.Function<String, char[]> toCharArray = s -> s.toCharArray();
        return Stream.of(// ----- valid conversions -------------------------------------------------
        Arguments.of(new TestCase("Valid conversions - CharBuffer input", CharBuffer.wrap("hello"), sharedConverter, toCharArray.apply("hello"), null)), Arguments.of(new TestCase("Valid conversions - String input", "hello", sharedConverter, toCharArray.apply("hello"), null)), Arguments.of(new TestCase("Valid conversions - char[] input", new char[] { 'a', 'b', 'c' }, sharedConverter, new char[] { 'a', 'b', 'c' }, null)), Arguments.of(new TestCase("Valid conversions - byte[] input (UTF-8)", // "abc"
        new byte[] { 97, 98, 99 }, sharedConverter, toCharArray.apply("abc"), null)), Arguments.of(new TestCase("Valid conversions - ByteBuffer input", // "de"
        ByteBuffer.wrap(new byte[] { 100, 101 }), sharedConverter, toCharArray.apply("de"), null)), Arguments.of(new TestCase("Valid conversions - empty String input", "", sharedConverter, new char[0], null)), // ----- invalid conversions ------------------------------------------------
        Arguments.of(new TestCase("Invalid conversions - null source", null, sharedConverter, null, NullPointerException.class)), Arguments.of(new TestCase("Invalid conversions - unsupported type", 12345, sharedConverter, null, IllegalArgumentException.class)), Arguments.of(new TestCase("Invalid conversions - null converter", "test", null, null, NullPointerException.class)), Arguments.of(new TestCase("Invalid conversions - unsupported CharSequence type", new StringBuilder("abc"), sharedConverter, null, IllegalArgumentException.class)), Arguments.of(new TestCase("Invalid conversions - converter throws exception", "trigger", new ThrowingConverter(), null, RuntimeException.class)), Arguments.of(new TestCase("Invalid conversions - malformed UTF-8 byte array", new byte[] { (byte) 0xC3, (byte) 0x28 }, sharedConverter, null, IllegalArgumentException.class)));
    }

    @Test
    void toCharArray_delegatesCorrectly_withNonNullSource() {
        // GIVEN
        Object source = "dummy source";
        CharBuffer fakeCharBuffer = CharBuffer.wrap("hello");
        char[] expected = new char[] { 'h', 'e', 'l', 'l', 'o' };
        // Mock the static methods of the two utility classes.
        try (MockedStatic<ByteBufferConversions> mockedByteBuffer = Mockito.mockStatic(ByteBufferConversions.class);
            MockedStatic<CharBufferConversions> mockedCharBuffer = Mockito.mockStatic(CharBufferConversions.class)) {
            // ByteBufferConversions.toCharBuffer should return our fake CharBuffer.
            mockedByteBuffer.when(() -> ByteBufferConversions.toCharBuffer(eq(source), eq(sharedConverter))).thenReturn(fakeCharBuffer);
            // CharBufferConversions.toCharArray should return the expected char[].
            mockedCharBuffer.when(() -> CharBufferConversions.toCharArray(eq(fakeCharBuffer), eq(sharedConverter))).thenReturn(expected);
            // WHEN
            char[] actual = ByteBufferConversions.toCharArray(source, sharedConverter);
            // THEN
            assertArrayEquals(expected, actual, "The returned char[] must be exactly the one from CharBufferConversions");
            // Verify that the static methods were called exactly once with the expected arguments.
            mockedByteBuffer.verify(() -> ByteBufferConversions.toCharBuffer(eq(source), eq(sharedConverter)), times(1));
            mockedCharBuffer.verify(() -> CharBufferConversions.toCharArray(eq(fakeCharBuffer), eq(sharedConverter)), times(1));
        }
    }

    @Test
    void toCharArray_delegatesCorrectly_withNullSource() {
        // GIVEN
        Object source = null;
        // toCharBuffer is expected to return null for a null source
        CharBuffer fakeCharBuffer = null;
        // CharBufferConversions may decide to return an empty array for null
        char[] expected = new char[0];
        try (MockedStatic<ByteBufferConversions> mockedByteBuffer = Mockito.mockStatic(ByteBufferConversions.class);
            MockedStatic<CharBufferConversions> mockedCharBuffer = Mockito.mockStatic(CharBufferConversions.class)) {
            mockedByteBuffer.when(() -> ByteBufferConversions.toCharBuffer(isNull(), eq(sharedConverter))).thenReturn(fakeCharBuffer);
            mockedCharBuffer.when(() -> CharBufferConversions.toCharArray(isNull(), eq(sharedConverter))).thenReturn(expected);
            // WHEN
            char[] actual = ByteBufferConversions.toCharArray(source, sharedConverter);
            // THEN
            assertArrayEquals(expected, actual, "Null source should be handled and result from CharBufferConversions returned");
            mockedByteBuffer.verify(() -> ByteBufferConversions.toCharBuffer(isNull(), eq(sharedConverter)), times(1));
            mockedCharBuffer.verify(() -> CharBufferConversions.toCharArray(isNull(), eq(sharedConverter)), times(1));
        }
    }

    /**
     * Parameterised test that verifies {@link ByteBufferConversions#toCharArray(Object, Converter)}
     * for every scenario described in the JSON test‑case list.
     *
     * <p>We mock the two static collaborators:
     * <ul>
     *   <li>{@code ByteBufferConversions.toCharBuffer}</li>
     *   <li>{@code CharBufferConversions.toCharArray}</li>
     * </ul>
     * The mock behaviour is configured per‑case so that the method under test either returns the
     * expected {@code char[]} or propagates the expected exception.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("provideTestCases")
    void toCharArray_behaviourBasedOnInput(TestCase tc) {
        // ----------------------------------------------------------------------
        // Arrange – set up static mocks according to the test case definition.
        // ----------------------------------------------------------------------
        try (MockedStatic<ByteBufferConversions> mockedByteBuffer = Mockito.mockStatic(ByteBufferConversions.class);
            MockedStatic<CharBufferConversions> mockedCharBuffer = Mockito.mockStatic(CharBufferConversions.class)) {
            // ------------------------------------------------------------------
            // Successful conversion path
            // ------------------------------------------------------------------
            if (tc.expectedException == null) {
                // The delegate toCharBuffer should return a CharBuffer that contains the same characters
                // as the expected result.  We simply wrap the expected char[] into a CharBuffer.
                CharBuffer bufferFromDelegate = CharBuffer.wrap(tc.expectedArray);
                mockedByteBuffer.when(() -> ByteBufferConversions.toCharBuffer(eq(tc.from), eq(tc.converter))).thenReturn(bufferFromDelegate);
                // The CharBufferConversions mock returns the expected char[].
                mockedCharBuffer.when(() -> CharBufferConversions.toCharArray(eq(bufferFromDelegate), eq(tc.converter))).thenReturn(tc.expectedArray);
            } else {
                // ------------------------------------------------------------------
                // Failure path – we make the first delegate (toCharBuffer) throw the expected exception.
                // This mirrors the real implementation where the exception would be raised during
                // the conversion of the source object.
                // ------------------------------------------------------------------
                mockedByteBuffer.when(() -> ByteBufferConversions.toCharBuffer(eq(tc.from), eq(tc.converter))).thenThrow(tc.expectedException);
            }
            // ------------------------------------------------------------------
            // Act & Assert
            // ------------------------------------------------------------------
            if (tc.expectedException == null) {
                char[] actual = ByteBufferConversions.toCharArray(tc.from, tc.converter);
                assertArrayEquals(tc.expectedArray, actual, () -> "Expected char[] from successful conversion: " + tc.description);
                // Verify the static collaborators were called exactly once.
                mockedByteBuffer.verify(() -> ByteBufferConversions.toCharBuffer(eq(tc.from), eq(tc.converter)), times(1));
                mockedCharBuffer.verify(() -> CharBufferConversions.toCharArray(any(CharBuffer.class), eq(tc.converter)), times(1));
            } else {
                // Expect the exception to be propagated unchanged.
                Throwable thrown = assertThrows(tc.expectedException, () -> ByteBufferConversions.toCharArray(tc.from, tc.converter), () -> "Expected exception for case: " + tc.description);
                // Optional: verify that the exception originates from the mocked delegate.
                assertTrue(thrown.getClass().isAssignableFrom(tc.expectedException), "Thrown exception type should match expected");
                mockedByteBuffer.verify(() -> ByteBufferConversions.toCharBuffer(eq(tc.from), eq(tc.converter)), times(1));
                // CharBufferConversions should never be invoked when the first step already failed.
                mockedCharBuffer.verifyNoInteractions();
            }
        }
    }
}
