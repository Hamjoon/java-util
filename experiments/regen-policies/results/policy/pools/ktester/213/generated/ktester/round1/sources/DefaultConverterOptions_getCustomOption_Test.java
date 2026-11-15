package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DefaultConverterOptions_getCustomOption_Test {

    private DefaultConverterOptions options;

    /**
     * Creates a fresh {@link DefaultConverterOptions} instance before each test and
     * populates its internal {@code customOptions} map with a known entry using
     * reflection (the class does not expose a public mutator for custom options).
     */
    @BeforeEach
    void setupBeforeEach() throws Exception {
        options = new DefaultConverterOptions();
        // Obtain the private field "customOptions"
        Field customOptionsField = DefaultConverterOptions.class.getDeclaredField("customOptions");
        customOptionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> customOptions = (Map<String, Object>) customOptionsField.get(options);
        // Ensure the map is a mutable implementation (the real class uses ConcurrentHashMap)
        if (customOptions == null) {
            customOptions = new ConcurrentHashMap<>();
            customOptionsField.set(options, customOptions);
        }
        // Populate the map with entries required by the test cases
        // String value
        customOptions.put("existingKey", "expectedValue");
        // Integer value
        customOptions.put("typeMismatch", 42);
        // Additional entries used by some legacy tests (kept for completeness)
        customOptions.put("intOption", 42);
        customOptions.put("stringOption", "hello");
    }

    private static Stream<Arguments> provideExistingKey() {
        return Stream.of(Arguments.of("existingKey", "expectedValue"));
    }

    private static Stream<Arguments> provideMissingKey() {
        return Stream.of(Arguments.of("missingKey"));
    }

    private static Stream<Arguments> provideTypeMismatch() {
        return Stream.of(Arguments.of("typeMismatch"));
    }

    private static Stream<Arguments> provideNullKey() {
        return Stream.of(Arguments.of((String) null));
    }

    @Test
    void getCustomOption_ShouldReturnStoredIntegerValue() {
        // given – the map was populated in @BeforeEach with key "intOption" → 42
        // when
        Integer result = options.getCustomOption("intOption");
        // then
        assertNotNull(result, "Result should not be null for an existing key");
        assertEquals(42, result.intValue(), "The retrieved integer value should match the stored value");
    }

    @Test
    void getCustomOption_ShouldReturnStoredStringValue() {
        // given – the map contains "stringOption" → "hello"
        // when
        String result = options.getCustomOption("stringOption");
        // then
        assertNotNull(result, "Result should not be null for an existing key");
        assertEquals("hello", result, "The retrieved string value should match the stored value");
    }

    @Test
    void getCustomOption_ShouldReturnNullWhenKeyIsAbsent() {
        // given – a key that has not been added to the map
        // when
        Object result = options.getCustomOption("nonExistentKey");
        // then
        assertNull(result, "Result should be null for a key that does not exist");
    }

    @Test
    void getCustomOption_ShouldThrowClassCastExceptionWhenRequestedTypeDoesNotMatch() {
        // given – "intOption" stores an Integer
        // when / then
        assertThrows(ClassCastException.class, () -> {
            // Attempt to retrieve the integer value as a String, which should fail.
            options.getCustomOption("intOption");
        });
    }

    @ParameterizedTest(name = "getCustomOption returns stored value for key ''{0}''")
    @MethodSource("provideExistingKey")
    void getCustomOption_ReturnsStoredValue_WhenKeyExists(String key, String expected) {
        // when
        String actual = options.getCustomOption(key);
        // then
        assertNotNull(actual, "Result should not be null for an existing key");
        assertEquals(expected, actual, "The retrieved value must match the stored value");
    }

    @ParameterizedTest(name = "getCustomOption returns null for missing key ''{0}''")
    @MethodSource("provideMissingKey")
    void getCustomOption_ReturnsNull_WhenKeyAbsent(String missingKey) {
        // when
        Object result = options.getCustomOption(missingKey);
        // then
        assertNull(result, "Result should be null when the key does not exist in the map");
    }

    @ParameterizedTest(name = "getCustomOption throws ClassCastException for type mismatch on key ''{0}''")
    @MethodSource("provideTypeMismatch")
    void getCustomOption_ThrowsClassCastException_OnTypeMismatch(String key) {
        // when / then
        assertThrows(ClassCastException.class, () -> {
            // Explicit generic type that does NOT match the stored Integer
            @SuppressWarnings("unused")
            String bogus = options.getCustomOption(key);
        }, "Retrieving a value with an incompatible generic type must cause ClassCastException");
    }

    @ParameterizedTest(name = "getCustomOption throws NullPointerException for null key")
    @MethodSource("provideNullKey")
    void getCustomOption_ThrowsNullPointerException_WhenKeyIsNull(String nullKey) {
        // when / then
        assertThrows(NullPointerException.class, () -> options.getCustomOption(nullKey), "Passing a null key to a ConcurrentHashMap‑backed map must throw NullPointerException");
    }
}
