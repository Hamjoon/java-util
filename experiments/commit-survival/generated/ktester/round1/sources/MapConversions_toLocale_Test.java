package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MapConversions_toLocale_Test {

    /**
     * Converter used for all tests.
     */
    private static Converter converter;

    /**
     * Real converter used for the majority of tests (identity conversion for Strings).
     */
    private static Converter realConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // The default Converter performs a simple identity conversion for String → String.
        realConverter = new Converter(new ConverterOptions());
    }

    /**
     * Helper to build a map that mimics the structure expected by MapConversions.toLocale.
     */
    private static Map<String, String> map(String language, String country, String script, String variant) {
        Map<String, String> m = new HashMap<>();
        if (language != null)
            m.put(MapConversions.LANGUAGE, language);
        if (country != null)
            m.put(MapConversions.COUNTRY, country);
        if (script != null)
            m.put(MapConversions.SCRIPT, script);
        if (variant != null)
            m.put(MapConversions.VARIANT, variant);
        return m;
    }

    private static Stream<Arguments> validLocaleProvider() {
        return Stream.of(Arguments.of("en", null, null, null, Locale.ENGLISH), Arguments.of("en", "GB", null, null, Locale.UK), Arguments.of("fr", "FR", null, null, Locale.FRANCE), Arguments.of("sr", null, "Latn", null, new Locale.Builder().setLanguage("sr").setScript("Latn").build()), Arguments.of("en", null, null, "POSIX", new Locale.Builder().setLanguage("en").setVariant("POSIX").build()), Arguments.of("en", "CA", "Latn", "POSIX", new Locale.Builder().setLanguage("en").setRegion("CA").setScript("Latn").setVariant("POSIX").build()));
    }

    @Test
    void toLocale_languageOnly() {
        Map<String, String> src = map("en", null, null, null);
        Locale result = MapConversions.toLocale(src, realConverter);
        assertEquals(Locale.ENGLISH, result);
    }

    @Test
    void toLocale_languageAndCountry() {
        Map<String, String> src = map("en", "US", null, null);
        Locale result = MapConversions.toLocale(src, realConverter);
        assertEquals(Locale.US, result);
    }

    @Test
    void toLocale_fullBuilder() {
        // language, country, script, variant
        Map<String, String> src = map("sr", "RS", "Latn", "POSIX");
        Locale result = MapConversions.toLocale(src, converter);
        Locale expected = new Locale.Builder().setLanguage("sr").setRegion("RS").setScript("Latn").setVariant("POSIX").build();
        assertEquals(expected, result);
    }

    @Test
    void toLocale_missingLanguageFallsBackToFromMap() {
        // When language key is empty, MapConversions should delegate to fromMap().
        // The fallback path creates a Locale from the generic map handling.
        // Provide a map that contains the "value" key with a locale string.
        Map<String, String> src = new HashMap<>();
        // French Canada
        src.put(MapConversions.VALUE, "fr-CA");
        Locale result = MapConversions.toLocale(src, converter);
        // The fallback ultimately uses Locale.forLanguageTag via fromMap,
        // so we expect the locale to be parsed accordingly.
        assertEquals(Locale.forLanguageTag("fr-CA"), result);
    }

    @Test
    void toLocale_invalidLanguageThrows() {
        Map<String, String> src = map("invalid-language-code-xyz", null, null, null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapConversions.toLocale(src, converter));
        assertTrue(ex.getMessage().contains("Locale language"));
    }

    @Test
    void toLocale_invalidCountryThrows() {
        // numeric country is illegal
        Map<String, String> src = map("en", "123", null, null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapConversions.toLocale(src, converter));
        assertTrue(ex.getMessage().contains("Locale region"));
    }

    @Test
    void toLocale_invalidScriptThrows() {
        Map<String, String> src = map("en", "US", "123", null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapConversions.toLocale(src, realConverter));
        assertTrue(ex.getMessage().contains("Locale script"));
    }

    @Test
    void toLocale_invalidVariantThrows() {
        // Variant can be any string, but an empty string is treated as absent;
        // we use a control character that Locale.Builder rejects.
        Map<String, String> src = map("en", "US", null, "\u0000");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapConversions.toLocale(src, converter));
        assertTrue(ex.getMessage().contains("Locale variant"));
    }

    @Test
    void toLocale_validLanguageAndScript() {
        Map<String, String> src = map("sr", null, "Latn", null);
        Locale result = MapConversions.toLocale(src, realConverter);
        Locale expected = new Locale.Builder().setLanguage("sr").setScript("Latn").build();
        assertEquals(expected, result);
    }

    @Test
    void toLocale_validLanguageAndVariant() {
        Map<String, String> src = map("en", null, null, "POSIX");
        Locale result = MapConversions.toLocale(src, realConverter);
        Locale expected = new Locale.Builder().setLanguage("en").setVariant("POSIX").build();
        assertEquals(expected, result);
    }

    @Test
    void toLocale_allFieldsValid() {
        Map<String, String> src = map("en", "CA", "Latn", "POSIX");
        Locale result = MapConversions.toLocale(src, realConverter);
        Locale expected = new Locale.Builder().setLanguage("en").setRegion("CA").setScript("Latn").setVariant("POSIX").build();
        assertEquals(expected, result);
    }

    @Test
    void toLocale_languageEmptyOptionalCountryIgnored() {
        Map<String, String> src = map("en", "", null, null);
        Locale result = MapConversions.toLocale(src, realConverter);
        assertEquals(Locale.ENGLISH, result);
    }

    @Test
    void toLocale_languageEmptyOptionalScriptIgnored() {
        Map<String, String> src = map("en", null, "", null);
        Locale result = MapConversions.toLocale(src, realConverter);
        assertEquals(Locale.ENGLISH, result);
    }

    @Test
    void toLocale_languageEmptyOptionalVariantIgnored() {
        Map<String, String> src = map("en", null, null, "");
        Locale result = MapConversions.toLocale(src, realConverter);
        assertEquals(Locale.ENGLISH, result);
    }

    @Test
    void toLocale_languageCountryScriptNoVariant() {
        Map<String, String> src = map("en", "US", "Latn", null);
        Locale result = MapConversions.toLocale(src, realConverter);
        Locale expected = new Locale.Builder().setLanguage("en").setRegion("US").setScript("Latn").build();
        assertEquals(expected, result);
    }

    @Test
    void toLocale_languageCountryVariantNoScript() {
        Map<String, String> src = map("en", "CA", null, "POSIX");
        Locale result = MapConversions.toLocale(src, realConverter);
        Locale expected = new Locale.Builder().setLanguage("en").setRegion("CA").setVariant("POSIX").build();
        assertEquals(expected, result);
    }

    @Test
    void toLocale_languageScriptVariantNoCountry() {
        Map<String, String> src = map("en", null, "Latn", "POSIX");
        Locale result = MapConversions.toLocale(src, realConverter);
        Locale expected = new Locale.Builder().setLanguage("en").setScript("Latn").setVariant("POSIX").build();
        assertEquals(expected, result);
    }

    @Test
    void toLocale_mapWithExtraKeysIgnored() {
        Map<String, String> src = new HashMap<>();
        src.put(MapConversions.LANGUAGE, "fr");
        src.put(MapConversions.COUNTRY, "FR");
        src.put("foo", "bar");
        src.put("baz", "qux");
        Locale result = MapConversions.toLocale(src, realConverter);
        assertEquals(Locale.FRANCE, result);
    }

    @Test
    void toLocale_languageEmptyFallsBackToFromMap() {
        // The language entry is empty, forcing a fallback to the private fromMap method.
        Map<String, String> src = new HashMap<>();
        // typical value key used by fromMap
        src.put(MapConversions.VALUE, "fr-CA");
        Locale result = MapConversions.toLocale(src, realConverter);
        // The exact Locale depends on fromMap implementation; we only assert non‑null and correct language tag.
        assertNotNull(result);
        assertEquals(Locale.forLanguageTag("fr-CA"), result);
    }

    @Test
    void toLocale_languageNullFallsBackToFromMap() {
        Map<String, String> src = new HashMap<>();
        // explicit null
        src.put(MapConversions.LANGUAGE, null);
        src.put(MapConversions.VALUE, "de-DE");
        Locale result = MapConversions.toLocale(src, realConverter);
        assertNotNull(result);
        assertEquals(Locale.forLanguageTag("de-DE"), result);
    }

    @Test
    void toLocale_nullMapArgumentThrows() {
        assertThrows(NullPointerException.class, () -> MapConversions.toLocale(null, realConverter));
    }

    @Test
    void toLocale_nonMapArgumentThrows() {
        List<Integer> notAMap = Arrays.asList(1, 2, 3);
        assertThrows(ClassCastException.class, () -> MapConversions.toLocale(notAMap, realConverter));
    }

    @Test
    void toLocale_nullConverterThrows() {
        Map<String, String> src = map("en", null, null, null);
        assertThrows(NullPointerException.class, () -> MapConversions.toLocale(src, null));
    }

    @Test
    void toLocale_converterThrowsDuringLanguageConversion() {
        // Mock converter that throws when converting the language entry.
        Converter mock = mock(Converter.class);
        when(mock.convert(eq("en"), eq(String.class))).thenThrow(new RuntimeException("language conversion failed"));
        // Any other conversion (e.g., country) just returns the argument.
        when(mock.convert(any(), eq(String.class))).thenAnswer(inv -> inv.getArgument(0));
        Map<String, String> src = map("en", "US", null, null);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> MapConversions.toLocale(src, mock));
        assertEquals("language conversion failed", ex.getMessage());
    }

    @Test
    void toLocale_converterThrowsDuringCountryConversion() {
        // Mock converter that throws only for the country conversion.
        Converter mock = mock(Converter.class);
        when(mock.convert(eq("en"), eq(String.class))).thenReturn("en");
        when(mock.convert(eq("US"), eq(String.class))).thenThrow(new RuntimeException("country conversion failed"));
        // Fallback for any other key.
        when(mock.convert(any(), eq(String.class))).thenAnswer(inv -> inv.getArgument(0));
        Map<String, String> src = map("en", "US", null, null);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> MapConversions.toLocale(src, mock));
        assertEquals("country conversion failed", ex.getMessage());
    }

    /**
     * Parameterized test that drives a collection of simple valid maps
     * (language, optional country/script/variant) and checks the resulting Locale.
     * This demonstrates a concise way to cover many permutations.
     */
    @ParameterizedTest
    @MethodSource("validLocaleProvider")
    void toLocale_parameterizedValidCases(String language, String country, String script, String variant, Locale expected) {
        Map<String, String> src = map(language, country, script, variant);
        Locale result = MapConversions.toLocale(src, realConverter);
        assertEquals(expected, result);
    }
}
