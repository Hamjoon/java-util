package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Locale;
import java.util.Map;
import static com.cedarsoftware.util.convert.MapConversions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocaleConversions_toMap_Test {

    @Mock
    private Converter // Converter is not used by the method, a mock is sufficient
    mockConverter;

    @BeforeEach
    void setupBeforeEach() {
        // Mockito @Mock fields are initialised automatically
        // Ensure the mock is reset before each test for safety
        reset(mockConverter);
    }

    /**
     * Provides a stream of arguments: each argument consists of a {@link Locale}
     * instance and the expected map entries (key → value).  The map is represented
     * as a simple {@code java.util.Map<String, String>} for readability.
     */
    private static Stream<Arguments> localeProvider() {
        return Stream.of(// basic language only
        Arguments.of(new Locale("en"), Map.of(LANGUAGE, "en")), // language and country
        Arguments.of(new Locale("en", "US"), Map.of(LANGUAGE, "en", COUNTRY, "US")), // language, country and variant
        Arguments.of(new Locale("en", "US", "POSIX"), Map.of(LANGUAGE, "en", COUNTRY, "US", VARIANT, "POSIX")), // language and script (no country)
        Arguments.of(new Locale.Builder().setLanguage("sr").setScript("Latn").build(), Map.of(LANGUAGE, "sr", SCRIPT, "Latn")), // full locale (language, script, country, variant)
        Arguments.of(new Locale.Builder().setLanguage("zh").setScript("Hans").setRegion("CN").setVariant("POSIX").build(), Map.of(LANGUAGE, "zh", SCRIPT, "Hans", COUNTRY, "CN", VARIANT, "POSIX")), // empty language with country
        Arguments.of(new Locale("", "FR"), Map.of(LANGUAGE, "", COUNTRY, "FR")), // all empty strings (language only, empty)
        Arguments.of(new Locale("", "", ""), Map.of(LANGUAGE, "")));
    }

    @Test
    void toMap_withOnlyLanguage() {
        // language only
        Locale locale = new Locale("en");
        Map<?, ?> result = LocaleConversions.toMap(locale, mockConverter);
        assertEquals(1, result.size(), "Map should contain only the language entry");
        assertEquals("en", result.get(LANGUAGE), "Language value should be 'en'");
        assertFalse(result.containsKey(COUNTRY), "Country should not be present");
        assertFalse(result.containsKey(SCRIPT), "Script should not be present");
        assertFalse(result.containsKey(VARIANT), "Variant should not be present");
    }

    @Test
    void toMap_withLanguageAndCountry() {
        // language + country
        Locale locale = new Locale("en", "US");
        Map<?, ?> result = LocaleConversions.toMap(locale, mockConverter);
        assertEquals(2, result.size(), "Map should contain language and country");
        assertEquals("en", result.get(LANGUAGE));
        assertEquals("US", result.get(COUNTRY));
        assertFalse(result.containsKey(SCRIPT));
        assertFalse(result.containsKey(VARIANT));
    }

    @Test
    void toMap_withLanguageCountryAndVariant() {
        // language + variant
        Locale locale = new Locale("en", "", "POSIX");
        Map<?, ?> result = LocaleConversions.toMap(locale, mockConverter);
        assertEquals(2, result.size(), "Map should contain language and variant");
        assertEquals("en", result.get(LANGUAGE));
        assertEquals("POSIX", result.get(VARIANT));
        assertFalse(result.containsKey(COUNTRY));
        assertFalse(result.containsKey(SCRIPT));
    }

    @Test
    void toMap_withAllFields() {
        Locale locale = new Locale.Builder().setLanguage("sr").setRegion("RS").setScript("Latn").setVariant("123").// language, country, script, variant
        build();
        Map<?, ?> result = LocaleConversions.toMap(locale, mockConverter);
        assertEquals(4, result.size(), "Map should contain language, country, script, and variant");
        assertEquals("sr", result.get(LANGUAGE));
        assertEquals("RS", result.get(COUNTRY));
        assertEquals("Latn", result.get(SCRIPT));
        assertEquals("123", result.get(VARIANT));
    }

    @Test
    void toMap_withEmptyCountryScriptVariant() {
        // Locale where country, script and variant are empty strings
        Locale locale = new Locale.Builder().setLanguage("ja").build();
        Map<?, ?> result = LocaleConversions.toMap(locale, mockConverter);
        assertEquals(1, result.size(), "Only language should be present when other fields are empty");
        assertEquals("ja", result.get(LANGUAGE));
        assertFalse(result.containsKey(COUNTRY));
        assertFalse(result.containsKey(SCRIPT));
        assertFalse(result.containsKey(VARIANT));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("localeProvider")
    void toMap_parameterized(Locale locale, Map<String, String> expected) {
        Map<?, ?> result = LocaleConversions.toMap(locale, mockConverter);
        // The result size must match the number of expected entries
        assertEquals(expected.size(), result.size(), () -> "Result map size mismatch for locale: " + locale);
        // Verify each expected key/value pair is present
        expected.forEach((key, value) -> assertEquals(value, result.get(key), () -> "Unexpected value for key '" + key + "' in locale: " + locale));
    }

    @Test
    void toMap_nullFromArgument_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> LocaleConversions.toMap(null, mockConverter), "Passing null as the source should throw NullPointerException");
    }

    @Test
    void toMap_nonLocaleSource_throwsClassCastException() {
        Object notALocale = "not a locale";
        assertThrows(ClassCastException.class, () -> LocaleConversions.toMap(notALocale, mockConverter), "Providing a non‑Locale source should throw ClassCastException");
    }
}
