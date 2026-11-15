package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.CharBuffer;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Random;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CharBufferConversions_toString_Test {

    /**
     * Real converter used for integration‑style tests.
     */
    private static Converter realConverter;

    /**
     * Mocked converter used for interaction verification.
     */
    @Mock
    private Converter mockConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a real Converter with default options.
        realConverter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mock interactions after each test to avoid cross‑test contamination.
        reset(mockConverter);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up for the static realConverter.
        realConverter = null;
    }

    private static Stream<Arguments> nullAndExceptionHandlingProvider() {
        // ThrowingConverter – throws RuntimeException when asked to convert
        Converter throwingConverter = new Converter() {

            @Override
            public <T> T convert(Object from, Class<T> toClass) {
                throw new RuntimeException("forced");
            }
        };
        return Stream.of(Arguments.of("Null source should propagate NullPointerException", null, realConverter, NullPointerException.class), Arguments.of("Converter that throws RuntimeException should propagate it", new Object(), throwingConverter, RuntimeException.class));
    }

    private static Stream<Arguments> directCharSequenceProvider() {
        return Stream.of(Arguments.of("String input is unchanged", "hello world", "hello world"), Arguments.of("CharBuffer input returns its content", CharBuffer.wrap("buffered"), "buffered"), Arguments.of("StringBuilder input returns its content", new StringBuilder("builder"), "builder"), Arguments.of("StringBuffer input returns its content", new StringBuffer("buffer"), "buffer"));
    }

    private static Stream<Arguments> arrayTypesProvider() {
        return Stream.of(Arguments.of("char[] conversion", new char[] { 'J', 'a', 'v', 'a' }, "Java"), Arguments.of("byte[] conversion (UTF‑8)", new byte[] { 'J', 'a', 'v', 'a' }, "Java"));
    }

    private static Stream<Arguments> emptyInputsProvider() {
        return Stream.of(Arguments.of("Empty String", "", ""), Arguments.of("Empty char[]", new char[0], ""), Arguments.of("Zero‑length ByteBuffer", ByteBuffer.allocate(0), ""), Arguments.of("Empty CharBuffer", CharBuffer.allocate(0), ""));
    }

    private static Stream<Arguments> unicodeProvider() {
        return Stream.of(Arguments.of("Emoji in String", "😀", "😀"), Arguments.of("Surrogate pair char[]", new char[] { '\uD83D', '\uDE00' }, "😀"), Arguments.of("UTF‑8 bytes for emoji", ByteBuffer.wrap(new byte[] { (byte) 0xF0, (byte) 0x9F, (byte) 0x98, (byte) 0x80 }), "😀"));
    }

    @Test
    void toString_givenCharBuffer_returnsSameContent() {
        // GIVEN
        CharBuffer source = CharBuffer.wrap("Hello CharBuffer");
        // WHEN
        String result = CharBufferConversions.toString(source, realConverter);
        // THEN
        assertEquals("Hello CharBuffer", result);
    }

    @Test
    void toString_givenString_usesConverterAndReturnsString() {
        // GIVEN
        String source = "Hello String";
        // WHEN
        String result = CharBufferConversions.toString(source, realConverter);
        // THEN
        // The default Converter knows how to turn a String into a CharBuffer,
        // therefore the final String should be identical to the source.
        assertEquals("Hello String", result);
    }

    @Test
    void toString_givenNull_returnsEmptyString() {
        // GIVEN
        Object source = null;
        // WHEN
        String result = CharBufferConversions.toString(source, realConverter);
        // THEN
        // An empty CharBuffer yields an empty string (not the literal "null").
        assertEquals("", result);
    }

    @Test
    void toString_withMockedConverter_returnsConvertedValue() {
        // GIVEN
        // the actual value is irrelevant for the mock
        Object source = new Object();
        CharBuffer mockedBuffer = CharBuffer.wrap("mocked conversion");
        when(mockConverter.convert(source, CharBuffer.class)).thenReturn(mockedBuffer);
        // WHEN
        String result = CharBufferConversions.toString(source, mockConverter);
        // THEN
        assertEquals("mocked conversion", result);
        // Verify that the converter was asked to convert the source to a CharBuffer exactly once.
        verify(mockConverter, times(1)).convert(source, CharBuffer.class);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("nullAndExceptionHandlingProvider")
    void toString_nullAndExceptionHandling(String description, Object from, Converter converter, Class<? extends Throwable> expected) {
        if (expected == null) {
            // No exception expected – normal flow
            String result = CharBufferConversions.toString(from, converter);
            assertNotNull(result);
        } else {
            assertThrows(expected, () -> CharBufferConversions.toString(from, converter), description);
        }
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("directCharSequenceProvider")
    void toString_directCharSequence(String description, Object from, String expected) {
        String result = CharBufferConversions.toString(from, realConverter);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("arrayTypesProvider")
    void toString_arrayTypes(String description, Object from, String expected) {
        String result = CharBufferConversions.toString(from, realConverter);
        assertEquals(expected, result, description);
    }

    @Test
    void toString_byteBufferInput() {
        ByteBuffer bb = ByteBuffer.wrap(new byte[] { 't', 'e', 's', 't' });
        String result = CharBufferConversions.toString(bb, realConverter);
        assertEquals("test", result);
    }

    @Test
    void toString_numberToStringConverter() {
        // Converter that turns any Number into its decimal string representation
        Converter numberConverter = new Converter() {

            @Override
            public <T> T convert(Object from, Class<T> toClass) {
                if (from instanceof Number && toClass == CharBuffer.class) {
                    return toClass.cast(CharBuffer.wrap(String.valueOf(from)));
                }
                throw new IllegalArgumentException("Unsupported");
            }
        };
        String result = CharBufferConversions.toString(12345, numberConverter);
        assertEquals("12345", result);
    }

    @Test
    void toString_dateToIsoStringConverter() {
        // Converter that formats java.util.Date to ISO‑8601 UTC string
        Converter dateConverter = new Converter() {

            @Override
            public <T> T convert(Object from, Class<T> toClass) {
                if (from instanceof Date && toClass == CharBuffer.class) {
                    Instant instant = ((Date) from).toInstant();
                    String iso = DateTimeFormatter.ISO_INSTANT.format(instant);
                    return toClass.cast(CharBuffer.wrap(iso));
                }
                throw new IllegalArgumentException("Unsupported");
            }
        };
        String result = CharBufferConversions.toString(new Date(0L), dateConverter);
        assertEquals("1970-01-01T00:00:00Z", result);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("emptyInputsProvider")
    void toString_emptyInputs(String description, Object from, String expected) {
        String result = CharBufferConversions.toString(from, realConverter);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("unicodeProvider")
    void toString_unicode(String description, Object from, String expected) {
        String result = CharBufferConversions.toString(from, realConverter);
        assertEquals(expected, result, description);
    }

    @Test
    void toString_charBufferPositionAndLimit() {
        CharBuffer cb = CharBuffer.wrap("abcdef");
        cb.position(2);
        // characters at indices 2,3,4 -> "cde"
        cb.limit(5);
        String result = CharBufferConversions.toString(cb, realConverter);
        assertEquals("cde", result);
    }

    @Test
    void toString_nullConverterThrowsNPE() {
        assertThrows(NullPointerException.class, () -> CharBufferConversions.toString("test", null), "Passing a null Converter should cause NullPointerException");
    }

    @Test
    void toString_converterReturnsWrongType() {
        Converter badConverter = new Converter() {

            @Override
            public <T> T convert(Object from, Class<T> toClass) {
                // Return an Integer instead of CharBuffer
                return toClass.cast(Integer.valueOf(42));
            }
        };
        assertThrows(ClassCastException.class, () -> CharBufferConversions.toString("irrelevant", badConverter), "Returning non‑CharBuffer should cause ClassCastException");
    }

    @Test
    void toString_converterReturnsNull() {
        Converter nullReturning = new Converter() {

            @Override
            public <T> T convert(Object from, Class<T> toClass) {
                // deliberately return null
                return null;
            }
        };
        assertThrows(NullPointerException.class, () -> CharBufferConversions.toString("any", nullReturning), "Null return from converter should propagate NullPointerException");
    }

    @Test
    void toString_unsupportedSourceType() {
        assertThrows(IllegalArgumentException.class, () -> CharBufferConversions.toString(new Random(), realConverter), "Unsupported source type should raise IllegalArgumentException");
    }

    @Test
    void toString_invalidCharBufferState() {
        CharBuffer cb = CharBuffer.allocate(5);
        cb.position(4);
        // deliberately set limit lower than position – illegal state
        cb.limit(2);
        assertThrows(IllegalArgumentException.class, () -> CharBufferConversions.toString(cb, realConverter), "Illegal CharBuffer state should cause IllegalArgumentException");
    }

    @Test
    void toString_invalidByteBufferState() {
        ByteBuffer bb = ByteBuffer.allocate(5);
        bb.position(4);
        // illegal state
        bb.limit(2);
        assertThrows(IllegalArgumentException.class, () -> CharBufferConversions.toString(bb, realConverter), "Illegal ByteBuffer state should cause IllegalArgumentException");
    }

    @Test
    void toString_converterThrowsErrorPropagation() {
        Converter errorThrowing = new Converter() {

            @Override
            public <T> T convert(Object from, Class<T> toClass) {
                throw new AssertionError("assertion failure");
            }
        };
        AssertionError error = assertThrows(AssertionError.class, () -> CharBufferConversions.toString("trigger", errorThrowing));
        assertEquals("assertion failure", error.getMessage());
    }

    @Test
    void toString_converterReturnsCharBufferWithNullArray() {
        // CharBuffer.wrap(null) throws NPE immediately, so we simulate by returning a
        // CharBuffer that will throw NPE when toString() is invoked.
        Converter nullArrayConverter = new Converter() {

            @Override
            public <T> T convert(Object from, Class<T> toClass) {
                // Create a CharBuffer via reflection with a null backing array.
                CharBuffer cb = CharBuffer.allocate(0);
                // Force the internal array reference to null via reflection.
                try {
                    java.lang.reflect.Field hb = CharBuffer.class.getDeclaredField("hb");
                    hb.setAccessible(true);
                    hb.set(cb, null);
                } catch (Exception e) {
                    // If reflection fails, fall back to a direct NPE.
                    throw new NullPointerException("Unable to set null array");
                }
                return toClass.cast(cb);
            }
        };
        assertThrows(NullPointerException.class, () -> CharBufferConversions.toString("any", nullArrayConverter), "Null backing array inside CharBuffer should cause NPE");
    }
}
