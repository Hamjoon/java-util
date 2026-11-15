package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.*;
import java.util.stream.Stream;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MapConversions_toLocale_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; use the builder to obtain a concrete instance.
        ConverterOptions options = ConverterOptions.builder().build();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static converter reference
        converter = null;
    }

    private static Stream<Arguments> provideMapBasedSuccessCases() {
        return Stream.of(// primary key with Locale instance
        Arguments.of("locale", Locale.CANADA_FRENCH, Locale.CANADA_FRENCH), // secondary key with String
        Arguments.of("value", "fr_FR", Locale.FRANCE), // fallback key with String
        Arguments.of("_v", "de_DE", Locale.GERMANY), // primary key with dash‑separated string
        Arguments.of("locale", "en-GB", Locale.UK), // primary key with Locale instance (direct)
        Arguments.of("locale", Locale.CANADA, Locale.CANADA), // secondary key with Locale instance
        Arguments.of("value", Locale.JAPAN, Locale.JAPAN), // fallback key with Locale instance
        Arguments.of("_v", Locale.ITALY, Locale.ITALY));
    }

    private static Stream<Arguments> provideStringBasedSuccessCases() {
        return Stream.of(Arguments.of("en_US", Locale.US), Arguments.of("en-GB", Locale.UK), Arguments.of("ja", Locale.JAPAN), // whitespace trimmed
        Arguments.of("  en_us  ", Locale.US), // case normalisation
        Arguments.of("EN-us", Locale.US), // CharSequence
        Arguments.of(new StringBuilder("fr_FR"), Locale.FRANCE), Arguments.of("en_US_POSIX", new Locale("en", "US", "POSIX")));
    }

    private static Stream<Arguments> provideMapBasedExceptionCases() {
        return Stream.of(// unsupported value type
        Arguments.of(mapOf("value", 12345), IllegalArgumentException.class), // map without any recognised key
        Arguments.of(mapOf("unrelated", "en_US"), IllegalArgumentException.class), // primary key with whitespace‑only string
        Arguments.of(mapOf("locale", "   "), IllegalArgumentException.class), // secondary key with empty string
        Arguments.of(mapOf("value", ""), IllegalArgumentException.class), // fallback key with malformed string (trailing underscore)
        Arguments.of(mapOf("_v", "en_"), IllegalArgumentException.class), // primary key with too many components
        Arguments.of(mapOf("locale", "en_US_POSIX_EXTRA"), IllegalArgumentException.class), // secondary key with illegal characters
        Arguments.of(mapOf("locale", "en@US"), IllegalArgumentException.class));
    }

    private static Stream<Arguments> provideDirectUnsupportedCases() {
        return Stream.of(Arguments.of(42, IllegalArgumentException.class), Arguments.of(Arrays.asList(1, 2, 3), IllegalArgumentException.class), Arguments.of(true, IllegalArgumentException.class), Arguments.of(mapOf("locale", true), IllegalArgumentException.class));
    }

    private static Stream<Arguments> provideMalformedStringCases() {
        return Stream.of(Arguments.of("en-XX", IllegalArgumentException.class), Arguments.of("invalid_string", IllegalArgumentException.class), Arguments.of("", IllegalArgumentException.class), Arguments.of("   ", IllegalArgumentException.class), Arguments.of("en_", IllegalArgumentException.class), Arguments.of("_US", IllegalArgumentException.class), Arguments.of("en_US_POSIX_EXTRA", IllegalArgumentException.class), Arguments.of("en-US-POSIX-EXTRA", IllegalArgumentException.class), Arguments.of("en@US", IllegalArgumentException.class));
    }

    private static Stream<Arguments> provideNullConverterCases() {
        return Stream.of(Arguments.of("en_US"), Arguments.of(mapOf("locale", "en_US")));
    }

    /**
     * Simple var‑args map builder for readability in the test sources.
     */
    private static Map<String, Object> mapOf(Object... keyValues) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return map;
    }

    /**
     * Verify that a map containing a "locale" key is converted correctly.
     */
    @Test
    void toLocale_WithLocaleKey_ShouldReturnLocale() {
        Map<String, Object> map = new HashMap<>();
        map.put("locale", "fr_FR");
        Locale result = MapConversions.toLocale(map, converter);
        assertNotNull(result);
        assertEquals(Locale.FRANCE, result);
    }

    /**
     * Verify that a map containing a "value" key is used when "locale" is absent.
     */
    @Test
    void toLocale_WithValueKey_ShouldReturnLocale() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", "de_DE");
        Locale result = MapConversions.toLocale(map, converter);
        assertNotNull(result);
        assertEquals(Locale.GERMANY, result);
    }

    /**
     * Verify that a map containing the short key "_v" is also accepted.
     */
    @Test
    void toLocale_WithShortKey_ShouldReturnLocale() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", "ja_JP");
        Locale result = MapConversions.toLocale(map, converter);
        assertNotNull(result);
        assertEquals(Locale.JAPAN, result);
    }

    /**
     * Verify that a plain String (not a map) is converted directly to a Locale.
     */
    @Test
    void toLocale_WithString_ShouldReturnLocale() {
        String localeStr = "en_US";
        Locale result = MapConversions.toLocale(localeStr, converter);
        assertNotNull(result);
        assertEquals(Locale.US, result);
    }

    /**
     * Verify that null input returns null.
     */
    @Test
    void toLocale_WithNull_ShouldReturnNull() {
        Locale result = MapConversions.toLocale(null, converter);
        assertNull(result);
    }

    /**
     * Verify that an unsupported type (e.g., Integer) results in a ClassCastException
     * from the underlying dispatch logic.
     */
    @Test
    void toLocale_WithUnsupportedType_ShouldThrowException() {
        Integer unsupported = 42;
        assertThrows(ClassCastException.class, () -> MapConversions.toLocale(unsupported, converter));
    }

    @ParameterizedTest(name = "[{index}] map key ''{0}'' with value ''{1}'' => {2}")
    @MethodSource("provideMapBasedSuccessCases")
    void toLocale_MapBasedSuccess(String key, Object value, Locale expected) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        Locale result = MapConversions.toLocale(map, converter);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] string ''{0}'' => {1}")
    @MethodSource("provideStringBasedSuccessCases")
    void toLocale_StringBasedSuccess(Object input, Locale expected) {
        Locale result = MapConversions.toLocale(input, converter);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] map {0} => {1}")
    @MethodSource("provideMapBasedExceptionCases")
    void toLocale_MapBasedExceptions(Map<String, Object> map, Class<? extends Throwable> expected) {
        assertThrows(expected, () -> MapConversions.toLocale(map, converter));
    }

    @Test
    void toLocale_MapPrimaryKeyNullValue_ShouldReturnNull() {
        Map<String, Object> map = new HashMap<>();
        map.put("locale", null);
        assertNull(MapConversions.toLocale(map, converter));
    }

    @ParameterizedTest(name = "[{index}] input {0} ({1}) => {2}")
    @MethodSource("provideDirectUnsupportedCases")
    void toLocale_DirectUnsupported(Object input, Class<? extends Throwable> expected) {
        assertThrows(expected, () -> MapConversions.toLocale(input, converter));
    }

    @ParameterizedTest(name = "[{index}] malformed string ''{0}'' => {1}")
    @MethodSource("provideMalformedStringCases")
    void toLocale_MalformedStrings(String input, Class<? extends Throwable> expected) {
        assertThrows(expected, () -> MapConversions.toLocale(input, converter));
    }

    @ParameterizedTest(name = "[{index}] null converter with input {0}")
    @MethodSource("provideNullConverterCases")
    void toLocale_NullConverter(Object input) {
        assertThrows(NullPointerException.class, () -> MapConversions.toLocale(input, null));
    }
}
