package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.ByteBuffer;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapConversions_toByteBuffer_Test {

    @Mock
    private Converter // not used by the method but required by signature
    mockConverter;

    /**
     * Helper to create a map with the given key/value pair.
     */
    private static Map<String, Object> mapOf(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            String k = (String) keyValues[i];
            Object v = keyValues[i + 1];
            map.put(k, v);
        }
        return map;
    }

    private static Stream<Arguments> successCases() {
        return Stream.of(// toByteBuffer_success_cases
        Arguments.of(mapOf("value", "AQID"), new byte[] { 1, 2, 3 }, "Map contains only the 'value' key with a valid Base64 string"), Arguments.of(mapOf("_v", "BAUG"), new byte[] { 4, 5, 6 }, "Map contains only the legacy '_v' key with a valid Base64 string"), // toByteBuffer_additional_success_cases
        Arguments.of(mapOf("value", "AQID", "_v", "BAUG"), new byte[] { 1, 2, 3 }, "Both 'value' and '_v' present – 'value' takes precedence"), Arguments.of(mapOf("value", ""), new byte[0], "Empty Base64 string should decode to an empty ByteBuffer"), Arguments.of(mapOf("value", "AQIDBA=="), new byte[] { 1, 2, 3, 4 }, "Valid Base64 with padding characters"), Arguments.of(mapOf("_v", "AAECAwQFBgcICQ==", "extraKey", "ignored"), new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }, "Legacy '_v' with extra unrelated keys"), Arguments.of(mapOf("value", "", "_v", "BAUG"), new byte[0], "Empty 'value' string takes precedence over a valid '_v'"));
    }

    private static Stream<Arguments> failureCases() {
        return Stream.of(// toByteBuffer_failure_cases
        Arguments.of(mapOf(), "Missing or null 'value' or '_v' field", "Map without 'value' or '_v' keys"), Arguments.of(mapOf("value", null), "Missing or null 'value' or '_v' field", "Map with 'value' key but null value"), Arguments.of(mapOf("value", 12345), "Value must be a Base64-encoded String", "Map with non‑String value"), Arguments.of(mapOf("value", "!!!not_base64!!!"), "Invalid Base64 encoding", "Map with an invalid Base64 string"), // toByteBuffer_additional_failure_cases
        Arguments.of(mapOf("value", null, "_v", "BAUG"), "Missing or null 'value' or '_v' field", "Null 'value' should cause failure even if '_v' is present"), Arguments.of(mapOf("value", "   "), "Invalid Base64 encoding", "Whitespace‑only string is not valid Base64"), // toByteBuffer_invalid_base64_whitespace_cases
        Arguments.of(mapOf("value", " AQID"), "Invalid Base64 encoding", "Leading space in Base64 string"), Arguments.of(mapOf("value", "AQID "), "Invalid Base64 encoding", "Trailing space in Base64 string"), Arguments.of(mapOf("value", "AQ ID"), "Invalid Base64 encoding", "Internal space in Base64 string"));
    }

    private static Stream<Arguments> typeMismatchCases() {
        return Stream.of(Arguments.of(Arrays.asList("not", "a", "map"), "List instead of Map"), Arguments.of("just a string", "String instead of Map"), Arguments.of(12345, "Integer instead of Map"));
    }

    @Test
    @DisplayName("toByteBuffer converts a valid Base64 string from 'value' key")
    void testValidBase64FromValueKey() {
        byte[] original = new byte[] { 1, 2, 3, 4, 5 };
        String base64 = Base64.getEncoder().encodeToString(original);
        Map<String, Object> source = mapOf("value", base64);
        ByteBuffer buffer = MapConversions.toByteBuffer(source, mockConverter);
        assertNotNull(buffer, "Resulting ByteBuffer should not be null");
        assertEquals(original.length, buffer.remaining(), "ByteBuffer length should match original");
        byte[] result = new byte[original.length];
        buffer.get(result);
        assertArrayEquals(original, result, "Decoded bytes must match original");
    }

    @Test
    @DisplayName("toByteBuffer converts a valid Base64 string from '_v' key when 'value' missing")
    void testValidBase64FromVKey() {
        byte[] original = new byte[] { 10, 20, 30 };
        String base64 = Base64.getEncoder().encodeToString(original);
        Map<String, Object> source = mapOf("_v", base64);
        ByteBuffer buffer = MapConversions.toByteBuffer(source, mockConverter);
        assertNotNull(buffer);
        assertEquals(original.length, buffer.remaining());
        byte[] result = new byte[original.length];
        buffer.get(result);
        assertArrayEquals(original, result);
    }

    @Test
    @DisplayName("toByteBuffer throws when neither 'value' nor '_v' keys are present")
    void testMissingKeys() {
        Map<String, Object> source = new HashMap<>();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapConversions.toByteBuffer(source, mockConverter));
        assertTrue(ex.getMessage().contains("Missing or null 'value' or '_v' field"));
    }

    @Test
    @DisplayName("toByteBuffer throws when the value is not a String")
    void testNonStringValue() {
        Map<String, Object> source = mapOf("value", 12345);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapConversions.toByteBuffer(source, mockConverter));
        assertTrue(ex.getMessage().contains("Value must be a Base64-encoded String"));
    }

    @Test
    @DisplayName("toByteBuffer throws when Base64 string is malformed")
    void testInvalidBase64() {
        Map<String, Object> source = mapOf("value", "!!! not base64 !!!");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapConversions.toByteBuffer(source, mockConverter));
        assertTrue(ex.getMessage().contains("Invalid Base64 encoding"));
        assertNotNull(ex.getCause(), "Cause should be the original IllegalArgumentException from Base64 decoder");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("successCases")
    @DisplayName("toByteBuffer – successful conversions")
    void testSuccessCases(Map<String, Object> source, byte[] expectedBytes, String description) {
        ByteBuffer buffer = MapConversions.toByteBuffer(source, mockConverter);
        assertNotNull(buffer, "Resulting ByteBuffer must not be null");
        assertEquals(expectedBytes.length, buffer.remaining(), "ByteBuffer length must match the expected byte array length");
        byte[] actual = new byte[expectedBytes.length];
        buffer.get(actual);
        assertArrayEquals(expectedBytes, actual, "Decoded bytes must equal the expected byte array");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("failureCases")
    @DisplayName("toByteBuffer – expected IllegalArgumentException")
    void testFailureCases(Map<String, Object> source, String expectedMessageFragment, String description) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapConversions.toByteBuffer(source, mockConverter), "Method should throw IllegalArgumentException");
        assertTrue(ex.getMessage().contains(expectedMessageFragment), () -> "Exception message should contain: " + expectedMessageFragment);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("typeMismatchCases")
    @DisplayName("toByteBuffer – ClassCastException for non‑Map inputs")
    void testTypeMismatchCases(Object nonMapInput, String description) {
        assertThrows(ClassCastException.class, () -> MapConversions.toByteBuffer(nonMapInput, mockConverter), "Passing a non‑Map should result in ClassCastException");
    }

    @Test
    @DisplayName("toByteBuffer – NullPointerException when map argument is null")
    void testNullMapArgument() {
        assertThrows(NullPointerException.class, () -> MapConversions.toByteBuffer(null, mockConverter), "Null map should cause NullPointerException");
    }

    @Test
    @DisplayName("toByteBuffer – Base64 string containing newline causes IllegalArgumentException")
    void testBase64WithNewline() {
        Map<String, Object> source = mapOf("value", "AQID\nBAUG");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapConversions.toByteBuffer(source, mockConverter));
        assertTrue(ex.getMessage().contains("Invalid Base64 encoding"), "Exception message must indicate invalid Base64");
        assertNotNull(ex.getCause(), "Cause should be the original IllegalArgumentException from decoder");
    }
}
