package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CharBufferConversions_toStringBuffer_Test {

    /**
     * Shared Converter instance used by all test methods.
     */
    private static Converter converter;

    /**
     * Shared real Converter used by the simple “happy‑path” tests.
     */
    private static Converter realConverter;

    /**
     * Simple POJO used in the “custom object” test case.
     */
    private static final class CustomObject {

        private final String value;

        CustomObject(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "CustomObject{" + value + '}';
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // The library’s Converter needs a ConverterOptions instance – the default works for us.
        realConverter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly necessary but keeps the pattern tidy.
        converter = null;
    }

    /**
     * Supplies arguments for {@link #shouldConvertVariousSources}.
     */
    private static Stream<Arguments> toStringBufferProvider() {
        // 1️⃣ CharBuffer → unchanged
        CharBuffer charBuffer = CharBuffer.wrap("hello");
        Converter noOpConverter1 = mock(Converter.class);
        // No conversion needed – the static method will recognise CharBuffer and return it directly.
        // Still, we stub the generic convert call just in case.
        when(noOpConverter1.convert(eq(charBuffer), eq(CharBuffer.class))).thenReturn(charBuffer);
        Arguments arg1 = Arguments.of("Pass a CharBuffer directly; should result in identical content", charBuffer, noOpConverter1, "hello");
        // 2️⃣ Plain String
        String stringSource = "world";
        Converter noOpConverter2 = mock(Converter.class);
        // The conversion path is: String → CharBuffer (via CharSequence handling)
        CharBuffer expectedFromString = CharBuffer.wrap(stringSource);
        when(noOpConverter2.convert(eq(stringSource), eq(CharBuffer.class))).thenReturn(expectedFromString);
        Arguments arg2 = Arguments.of("Pass a plain String; should be wrapped as CharBuffer", stringSource, noOpConverter2, "world");
        // 3️⃣ char[] → CharBuffer
        char[] charArray = new char[] { 'a', 'b', 'c' };
        Converter noOpConverter3 = mock(Converter.class);
        CharBuffer expectedFromCharArray = CharBuffer.wrap(CharBuffer.wrap(new String(charArray)));
        // For simplicity we let the mock return a CharBuffer built from the array.
        when(noOpConverter3.convert(eq(charArray), eq(CharBuffer.class))).thenReturn(CharBuffer.wrap(new String(charArray)));
        Arguments arg3 = Arguments.of("Pass a char array; should become a StringBuffer with same characters", charArray, noOpConverter3, "abc");
        // 4️⃣ byte[] → CharBuffer using a custom converter
        // ASCII "ABC"
        byte[] byteArray = new byte[] { 65, 66, 67 };
        Converter byteArrayConverter = mock(Converter.class);
        when(byteArrayConverter.convert(eq(byteArray), eq(CharBuffer.class))).thenReturn(CharBuffer.wrap("ABC"));
        Arguments arg4 = Arguments.of("Pass a byte array (ASCII); custom converter should decode to \"ABC\"", byteArray, byteArrayConverter, "ABC");
        // 5️⃣ null input – expected literal "null"
        Converter noOpConverter5 = mock(Converter.class);
        // The mock should return a CharBuffer containing the literal "null".
        when(noOpConverter5.convert(isNull(), eq(CharBuffer.class))).thenReturn(CharBuffer.wrap("null"));
        Arguments arg5 = Arguments.of("Null source should become literal \"null\"", null, noOpConverter5, "null");
        // 6️⃣ Custom object → CharBuffer via a dedicated converter
        CustomObject custom = new CustomObject("xyz");
        Converter customObjectConverter = mock(Converter.class);
        when(customObjectConverter.convert(eq(custom), eq(CharBuffer.class))).thenReturn(CharBuffer.wrap("xyz"));
        Arguments arg6 = Arguments.of("CustomObject converted by its own converter to \"xyz\"", custom, customObjectConverter, "xyz");
        // 7️⃣ Map → CharBuffer using default toString()
        Map<String, String> map = Map.of("key", "value");
        Converter noOpConverter7 = mock(Converter.class);
        // The expected CharBuffer contains the map's toString representation.
        when(noOpConverter7.convert(eq(map), eq(CharBuffer.class))).thenReturn(CharBuffer.wrap(map.toString()));
        Arguments arg7 = Arguments.of("Map should fall back to its toString() representation", map, noOpConverter7, map.toString());
        return Stream.of(arg1, arg2, arg3, arg4, arg5, arg6, arg7);
    }

    @Test
    void shouldConvertStringToStringBuffer() {
        String source = "Hello, World!";
        StringBuffer result = CharBufferConversions.toStringBuffer(source, realConverter);
        assertNotNull(result, "Result must not be null");
        assertEquals(source, result.toString(), "StringBuffer content should match the source string");
    }

    @Test
    void shouldConvertCharBufferToStringBuffer() {
        CharBuffer source = CharBuffer.wrap("CharBufferContent");
        StringBuffer result = CharBufferConversions.toStringBuffer(source, realConverter);
        assertNotNull(result);
        assertEquals(source.toString(), result.toString(), "StringBuffer content should equal the CharBuffer's string representation");
    }

    @Test
    void shouldConvertCharArrayToStringBuffer() {
        char[] source = new char[] { 'J', 'a', 'v', 'a' };
        StringBuffer result = CharBufferConversions.toStringBuffer(source, realConverter);
        assertNotNull(result);
        assertEquals(new String(source), result.toString(), "StringBuffer content should match the char array converted to a string");
    }

    @Test
    void shouldConvertByteBufferToStringBuffer() {
        // ByteBuffer containing UTF‑8 bytes for "Test"
        ByteBuffer source = ByteBuffer.wrap("Test".getBytes());
        StringBuffer result = CharBufferConversions.toStringBuffer(source, converter);
        assertNotNull(result);
        assertEquals("Test", result.toString(), "StringBuffer should contain the characters decoded from the ByteBuffer");
    }

    @Test
    void shouldHandleNullInputGracefully() {
        // According to Converter contract, a null source yields null for object types.
        // CharBufferConversions should therefore return an empty StringBuffer.
        StringBuffer result = CharBufferConversions.toStringBuffer(null, converter);
        assertNotNull(result, "Result should never be null even when source is null");
        assertEquals(0, result.length(), "StringBuffer should be empty for a null source");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("toStringBufferProvider")
    void shouldConvertVariousSources(String description, Object source, Converter converter, String expectedContent) {
        StringBuffer result = CharBufferConversions.toStringBuffer(source, converter);
        assertNotNull(result, "Result must never be null");
        assertEquals(expectedContent, result.toString(), "Unexpected StringBuffer content for case: " + description);
    }
}
