package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.CharBuffer;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toCharBuffer_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // The Converter is required as a parameter, even though the method does not use it.
        // Use default options – they are sufficient for the conversion performed by toCharBuffer.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Clean up the instance reference.
        converter = null;
    }

    private static Stream<Arguments> basicNullAndEmptyProvider() {
        return Stream.of(// null source (converter also null as per JSON)
        Arguments.of(null, null, "null", "When the source object is null, asString should return the literal \"null\""), // empty string
        Arguments.of("", null, "", "An empty string should be wrapped unchanged; length must be 0"));
    }

    private static Stream<Arguments> variousObjectTypesProvider() {
        return Stream.of(Arguments.of(123, null, "123", "Integer should be converted to its decimal representation"), Arguments.of(true, null, "true", "Boolean true should become the literal \"true\""), Arguments.of('c', null, "c", "Character should be converted to its single‑character string"), Arguments.of(new char[] { 'a', 'b', 'c' }, null, "abc", "char[] should be concatenated into a string"), Arguments.of(CharBuffer.wrap("hello world"), null, "hello world", "Existing CharBuffer content unchanged"), Arguments.of(new StringBuilder("builder"), null, "builder", "StringBuilder should be converted via toString()"), Arguments.of(new StringBuffer("buffer"), null, "buffer", "StringBuffer should be converted via toString()"), Arguments.of(UUID.fromString("123e4567-e89b-12d3-a456-426655440000"), null, "123e4567-e89b-12d3-a456-426655440000", "UUID should be rendered as its standard textual representation"), Arguments.of(new Date(0L), null, new Date(0L).toString(), "Date should be converted using Date.toString()"), Arguments.of(new Object() {

            @Override
            public String toString() {
                return "customObject";
            }
        }, null, "customObject", "User‑defined object with overridden toString()"));
    }

    @Test
    void toCharBuffer_withString_returnsWrappedCharBuffer() {
        // given
        String source = "Hello, World!";
        // when
        CharBuffer result = StringConversions.toCharBuffer(source, converter);
        // then
        assertNotNull(result, "Resulting CharBuffer should not be null");
        assertEquals(source, result.toString(), "CharBuffer content should match the original string");
    }

    @Test
    void toCharBuffer_withInteger_returnsWrappedCharBuffer() {
        // given
        Integer source = 42;
        // when
        CharBuffer result = StringConversions.toCharBuffer(source, converter);
        // then
        assertNotNull(result);
        assertEquals(source.toString(), result.toString(), "CharBuffer should contain the string representation of the integer");
    }

    @Test
    void toCharBuffer_withNull_returnsWrappedCharBufferContainingNullString() {
        // given
        Object source = null;
        // when
        CharBuffer result = StringConversions.toCharBuffer(source, converter);
        // then
        assertNotNull(result);
        assertEquals("null", result.toString(), "CharBuffer should contain the literal string \"null\" when source is null");
    }

    @Test
    void toCharBuffer_withCustomObject_returnsWrappedCharBuffer() {
        // given
        Object source = new Object() {

            @Override
            public String toString() {
                return "CustomObjectString";
            }
        };
        // when
        CharBuffer result = StringConversions.toCharBuffer(source, converter);
        // then
        assertNotNull(result);
        assertEquals("CustomObjectString", result.toString(), "CharBuffer should contain the custom object's toString() result");
    }

    @ParameterizedTest(name = "[{index}] source={0}, expected=\"{2}\"")
    @MethodSource("basicNullAndEmptyProvider")
    void toCharBuffer_basicNullAndEmpty(Object source, Converter conv, String expected, String description) {
        // when
        CharBuffer result = StringConversions.toCharBuffer(source, conv);
        // then
        assertNotNull(result, description);
        assertEquals(expected, result.toString(), description);
        if (expected.isEmpty()) {
            assertEquals(0, result.length(), "Empty string should result in CharBuffer of length 0");
        }
    }

    @ParameterizedTest(name = "[{index}] source={0} => \"{2}\"")
    @MethodSource("variousObjectTypesProvider")
    void toCharBuffer_variousObjectTypes(Object source, Converter conv, String expected, String description) {
        // when
        CharBuffer result = StringConversions.toCharBuffer(source, conv);
        // then
        assertNotNull(result, description);
        assertEquals(expected, result.toString(), description);
    }

    @Test
    void toCharBuffer_propagatesExceptionFromToString() {
        // given an object whose toString() throws a RuntimeException
        Object source = new Object() {

            @Override
            public String toString() {
                throw new RuntimeException("boom");
            }
        };
        // when / then
        RuntimeException ex = assertThrows(RuntimeException.class, () -> StringConversions.toCharBuffer(source, null), "RuntimeException from toString() should propagate");
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void toCharBuffer_nullSourceDoesNotThrow() {
        // This repeats the earlier null test but explicitly passes a null converter as per JSON.
        CharBuffer result = StringConversions.toCharBuffer(null, null);
        assertNotNull(result);
        assertEquals("null", result.toString(), "Null source with null converter should produce CharBuffer containing \"null\"");
    }

    @Disabled("Creating a char array of size Integer.MAX_VALUE is not feasible in a unit‑test environment.")
    @Test
    void toCharBuffer_largeCharArray_throws() {
        // This test is disabled because allocating such a large array would cause OOM.
        // If the implementation were to check size before wrapping, an IllegalArgumentException
        // would be expected. The test documents the intended behaviour.
        assertThrows(RuntimeException.class, () -> {
            char[] huge = new char[Integer.MAX_VALUE];
            StringConversions.toCharBuffer(huge, null);
        });
    }

    @Disabled("Creating a String of size Integer.MAX_VALUE is not feasible in a unit‑test environment.")
    @Test
    void toCharBuffer_largeString_throws() {
        // Disabled for the same reason as the previous test.
        assertThrows(RuntimeException.class, () -> {
            String huge = new String(new char[Integer.MAX_VALUE]);
            StringConversions.toCharBuffer(huge, null);
        });
    }
}
