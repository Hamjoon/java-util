package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ByteBufferConversions_toStringBuffer_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

    /**
     * Shared {@link Converter} instance used by the majority of tests.
     */
    private static Converter defaultConverter;

    /**
     * Simple holder for a single test scenario.
     */
    private static final class TestScenario {

        final String description;

        final Object from;

        final Converter converter;

        // null when an exception is expected
        final String expectedString;

        final Class<? extends Throwable> expectedException;

        final String detailedDescription;

        TestScenario(String description, Object from, Converter converter, String expectedString, Class<? extends Throwable> expectedException, String detailedDescription) {
            this.description = description;
            this.from = from;
            this.converter = converter;
            this.expectedString = expectedString;
            this.expectedException = expectedException;
            this.detailedDescription = detailedDescription;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    /**
     * Dummy converter that simply delegates to the real {@link Converter}
     * implementation.  It is used to keep the test code explicit about which
     * converter instance is being supplied.
     */
    private static class DummyConverter extends Converter {

        DummyConverter() {
            super(new ConverterOptions());
        }
    }

    /**
     * Converter that pretends to perform a conversion but always returns {@code null}
     * when asked to produce a {@link CharBuffer}.  This triggers a {@link NullPointerException}
     * inside {@code new StringBuffer(CharSequence)}.
     */
    private static class DummyNullReturningConverter extends Converter {

        DummyNullReturningConverter() {
            super(new ConverterOptions());
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T convert(Object source, Class<T> target) {
            // The ByteBufferConversions implementation ultimately asks for a CharBuffer.
            if (CharBuffer.class.isAssignableFrom(target)) {
                // Simulate a badly‑behaving converter.
                return null;
            }
            return super.convert(source, target);
        }
    }

    /**
     * Converter that throws a {@link RuntimeException} for any conversion request.
     */
    private static class DummyThrowingConverter extends Converter {

        DummyThrowingConverter() {
            super(new ConverterOptions());
        }

        @Override
        public <T> T convert(Object source, Class<T> target) {
            throw new RuntimeException("Intentional exception from DummyThrowingConverter");
        }
    }

    /**
     * A key object whose {@code toString()} deliberately throws an exception.
     * Used to verify that map‑to‑string conversion propagates such runtime failures.
     */
    private static final class BadToStringKey {

        @Override
        public String toString() {
            throw new RuntimeException("BadToStringKey toString failure");
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter has a public constructor that requires ConverterOptions.
        // Using the default options is sufficient for the built‑in conversions.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – helps the GC in long test suites.
        converter = null;
    }

    /**
     * Supplies all test scenarios.
     */
    private static Stream<TestScenario> scenarioProvider() {
        // Helper instances for the special converters
        Converter dummyConverter = new DummyConverter();
        Converter dummyNullReturning = new DummyNullReturningConverter();
        Converter dummyThrowing = new DummyThrowingConverter();
        List<TestScenario> list = new ArrayList<>();
        // -----------------------------------------------------------------
        // Null input
        // -----------------------------------------------------------------
        list.add(new TestScenario("Null input → empty StringBuffer", null, dummyConverter, "", null, "When the source object is null the method should return an empty StringBuffer."));
        // -----------------------------------------------------------------
        // CharBuffer input
        // -----------------------------------------------------------------
        list.add(new TestScenario("CharBuffer input → same content", CharBuffer.wrap("abc"), dummyConverter, "abc", null, "If the source is already a CharBuffer the method should wrap it directly."));
        // -----------------------------------------------------------------
        // ByteBuffer input (ASCII)
        // -----------------------------------------------------------------
        list.add(new TestScenario("ByteBuffer ASCII → \"ABC\"", ByteBuffer.wrap(new byte[] { 65, 66, 67 }), dummyConverter, "ABC", null, "ByteBuffer containing ASCII bytes should be decoded using UTF‑8."));
        // -----------------------------------------------------------------
        // byte[] input (ASCII)
        // -----------------------------------------------------------------
        list.add(new TestScenario("byte[] ASCII → \"DEF\"", new byte[] { 68, 69, 70 }, dummyConverter, "DEF", null, "Raw byte[] should be interpreted as UTF‑8 encoded characters."));
        // -----------------------------------------------------------------
        // char[] input
        // -----------------------------------------------------------------
        list.add(new TestScenario("char[] → \"xyz\"", new char[] { 'x', 'y', 'z' }, dummyConverter, "xyz", null, "char[] should be wrapped in a CharBuffer and then placed into a StringBuffer."));
        // -----------------------------------------------------------------
        // String input
        // -----------------------------------------------------------------
        list.add(new TestScenario("String input → same content", "hello world", dummyConverter, "hello world", null, "Plain String should be converted to a CharBuffer and then to a StringBuffer."));
        // -----------------------------------------------------------------
        // Map input
        // -----------------------------------------------------------------
        list.add(new TestScenario("Map input → toString representation", Map.of("key", "value"), dummyConverter, "{key=value}", null, "Map conversion should fall back to its toString representation."));
        // -----------------------------------------------------------------
        // Unsupported type (Integer)
        // -----------------------------------------------------------------
        list.add(new TestScenario("Unsupported Integer → IllegalArgumentException", 123, dummyConverter, null, IllegalArgumentException.class, "Unsupported source type must trigger IllegalArgumentException."));
        // -----------------------------------------------------------------
        // Empty inputs
        // -----------------------------------------------------------------
        list.add(new TestScenario("Empty CharBuffer → empty StringBuffer", CharBuffer.allocate(0), dummyConverter, "", null, "Empty CharBuffer should produce an empty StringBuffer."));
        list.add(new TestScenario("Empty ByteBuffer → empty StringBuffer", ByteBuffer.allocate(0), dummyConverter, "", null, "Empty ByteBuffer should produce an empty StringBuffer."));
        list.add(new TestScenario("Empty byte[] → empty StringBuffer", new byte[0], dummyConverter, "", null, "Empty byte array should be interpreted as an empty UTF‑8 string."));
        list.add(new TestScenario("Empty char[] → empty StringBuffer", new char[0], dummyConverter, "", null, "Empty char array should result in an empty StringBuffer."));
        list.add(new TestScenario("Empty String → empty StringBuffer", "", dummyConverter, "", null, "Empty String should be converted to an empty StringBuffer."));
        list.add(new TestScenario("Empty Map → \"{}\"", Map.of(), dummyConverter, "{}", null, "Empty Map should render using its toString representation, i.e., \"{}\"."));
        // -----------------------------------------------------------------
        // Unicode handling – ByteBuffer
        // -----------------------------------------------------------------
        list.add(new TestScenario("ByteBuffer UTF‑8 é → \"é\"", ByteBuffer.wrap(new byte[] { (byte) 0xC3, (byte) 0xA9 }), dummyConverter, "é", null, "ByteBuffer contains UTF‑8 bytes for the character 'é'."));
        list.add(new TestScenario("ByteBuffer UTF‑8 😀 → \"😀\"", ByteBuffer.wrap(new byte[] { (byte) 0xF0, (byte) 0x9F, (byte) 0x98, (byte) 0x80 }), dummyConverter, "😀", null, "ByteBuffer contains UTF‑8 bytes for the emoji 😀."));
        // -----------------------------------------------------------------
        // Unicode handling – byte[]
        // -----------------------------------------------------------------
        list.add(new TestScenario("byte[] UTF‑8 é → \"é\"", new byte[] { (byte) 0xC3, (byte) 0xA9 }, dummyConverter, "é", null, "Raw byte array with UTF‑8 representation of 'é' should be converted correctly."));
        list.add(new TestScenario("byte[] UTF‑8 😀 → \"😀\"", new byte[] { (byte) 0xF0, (byte) 0x9F, (byte) 0x98, (byte) 0x80 }, dummyConverter, "😀", null, "Raw byte array containing 4‑byte UTF‑8 emoji should be decoded properly."));
        // -----------------------------------------------------------------
        // Unicode handling – CharBuffer
        // -----------------------------------------------------------------
        list.add(new TestScenario("CharBuffer é → \"é\"", CharBuffer.wrap("é"), dummyConverter, "é", null, "CharBuffer already holds a Unicode character; it should be transferred unchanged."));
        list.add(new TestScenario("CharBuffer 😀 → \"😀\"", CharBuffer.wrap("😀"), dummyConverter, "😀", null, "CharBuffer with an emoji should be wrapped without loss."));
        // -----------------------------------------------------------------
        // StringBuilder input
        // -----------------------------------------------------------------
        list.add(new TestScenario("StringBuilder input → same content", new StringBuilder("builder"), dummyConverter, "builder", null, "StringBuilder implements CharSequence; conversion should treat it like a String."));
        // -----------------------------------------------------------------
        // Map with null value
        // -----------------------------------------------------------------
        list.add(new TestScenario("Map with null value → \"{key=null}\"", Map.of("key", null), dummyConverter, "{key=null}", null, "Map entries whose value is null should be rendered using the standard Map.toString format."));
        // -----------------------------------------------------------------
        // Unsupported type (Boolean)
        // -----------------------------------------------------------------
        list.add(new TestScenario("Unsupported Boolean → IllegalArgumentException", true, dummyConverter, null, IllegalArgumentException.class, "A Boolean is not a supported source type; the method must throw IllegalArgumentException."));
        // -----------------------------------------------------------------
        // Null converter
        // -----------------------------------------------------------------
        list.add(new TestScenario("Null converter → NullPointerException", "some string", null, null, NullPointerException.class, "When the Converter argument is null the method should throw NPE."));
        // -----------------------------------------------------------------
        // Converter returns null CharBuffer
        // -----------------------------------------------------------------
        list.add(new TestScenario("Converter returns null CharBuffer → NullPointerException", "any", dummyNullReturning, null, NullPointerException.class, "If the supplied Converter returns null, the StringBuffer constructor receives null and throws NPE."));
        // -----------------------------------------------------------------
        // Converter throws RuntimeException
        // -----------------------------------------------------------------
        list.add(new TestScenario("Converter throws RuntimeException → RuntimeException", "trigger", dummyThrowing, null, RuntimeException.class, "When the Converter throws a RuntimeException during conversion, the exception should propagate."));
        // -----------------------------------------------------------------
        // Invalid UTF‑8 byte array
        // -----------------------------------------------------------------
        list.add(new TestScenario("Invalid UTF‑8 byte[] → IllegalArgumentException", new byte[] { (byte) 0xC3, (byte) 0x28 }, dummyConverter, null, IllegalArgumentException.class, "Malformed UTF‑8 sequence should cause IllegalArgumentException."));
        // -----------------------------------------------------------------
        // Invalid UTF‑8 ByteBuffer
        // -----------------------------------------------------------------
        list.add(new TestScenario("Invalid UTF‑8 ByteBuffer → IllegalArgumentException", ByteBuffer.wrap(new byte[] { (byte) 0xE2, (byte) 0x28, (byte) 0xA1 }), dummyConverter, null, IllegalArgumentException.class, "Illegal UTF‑8 byte pattern in ByteBuffer should trigger IllegalArgumentException."));
        // -----------------------------------------------------------------
        // Map key toString throws
        // -----------------------------------------------------------------
        list.add(new TestScenario("Map key toString throws → RuntimeException", Map.of(new BadToStringKey(), "value"), dummyConverter, null, RuntimeException.class, "If a Map entry's key throws from toString(), the conversion should propagate that exception."));
        return list.stream();
    }

    @Test
    void toStringBuffer_fromString_returnsMatchingStringBuffer() {
        // GIVEN
        String source = "hello world";
        // WHEN
        StringBuffer result = ByteBufferConversions.toStringBuffer(source, converter);
        // THEN
        assertNotNull(result, "Resulting StringBuffer should not be null");
        assertEquals(source, result.toString(), "StringBuffer content must match the original String");
    }

    @Test
    void toStringBuffer_fromByteArray_returnsMatchingStringBuffer() {
        // GIVEN
        byte[] source = "byte array".getBytes(StandardCharsets.UTF_8);
        // WHEN
        StringBuffer result = ByteBufferConversions.toStringBuffer(source, converter);
        // THEN
        assertNotNull(result);
        assertEquals("byte array", result.toString());
    }

    @Test
    void toStringBuffer_fromByteBuffer_returnsMatchingStringBuffer() {
        // GIVEN
        ByteBuffer source = ByteBuffer.wrap("ByteBuffer".getBytes(StandardCharsets.UTF_8));
        // WHEN
        StringBuffer result = ByteBufferConversions.toStringBuffer(source, converter);
        // THEN
        assertNotNull(result);
        assertEquals("ByteBuffer", result.toString());
    }

    @Test
    void toStringBuffer_fromCharArray_returnsMatchingStringBuffer() {
        // GIVEN
        char[] source = new char[] { 'c', 'h', 'a', 'r', 's' };
        // WHEN
        StringBuffer result = ByteBufferConversions.toStringBuffer(source, converter);
        // THEN
        assertNotNull(result);
        assertEquals("chars", result.toString());
    }

    @Test
    void toStringBuffer_fromCharBuffer_returnsMatchingStringBuffer() {
        // GIVEN
        CharBuffer source = CharBuffer.wrap("CharBufferContent");
        // WHEN
        StringBuffer result = ByteBufferConversions.toStringBuffer(source, converter);
        // THEN
        assertNotNull(result);
        assertEquals("CharBufferContent", result.toString());
    }

    @Test
    void toStringBuffer_fromNull_returnsStringBufferWithNullString() {
        // GIVEN
        Object source = null;
        // WHEN
        StringBuffer result = ByteBufferConversions.toStringBuffer(source, converter);
        // THEN
        assertNotNull(result);
        // The conversion contract of Converter returns null for object types,
        // and ByteBufferConversions delegates to toCharBuffer which, for a null
        // source, yields the string "null". This is the behaviour of the original
        // library, and we assert against it.
        assertEquals("null", result.toString());
    }

    /**
     * Parameterised test that runs through the exhaustive list of scenarios described
     * in the JSON document.  Each {@link TestScenario} contains the input value,
     * the {@link Converter} to use, the expected result (either a StringBuffer content
     * or an exception type) and a short description.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("scenarioProvider")
    void toStringBuffer_variousScenarios(TestScenario scenario) {
        // Arrange
        Object source = scenario.from;
        Converter conv = scenario.converter;
        // Act & Assert
        if (scenario.expectedException != null) {
            Throwable thrown = assertThrows(scenario.expectedException, () -> ByteBufferConversions.toStringBuffer(source, conv), scenario.description);
            // For IllegalArgumentException we also want to ensure the message is not empty,
            // which gives us confidence that the exception originates from the conversion code.
            if (thrown instanceof IllegalArgumentException) {
                assertTrue(thrown.getMessage() != null && !thrown.getMessage().isEmpty(), "IllegalArgumentException should contain a descriptive message");
            }
        } else {
            StringBuffer result = ByteBufferConversions.toStringBuffer(source, conv);
            assertNotNull(result, "Resulting StringBuffer must not be null");
            assertEquals(scenario.expectedString, result.toString(), scenario.description);
        }
    }
}
