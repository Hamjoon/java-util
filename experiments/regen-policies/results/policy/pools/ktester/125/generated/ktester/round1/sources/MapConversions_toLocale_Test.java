package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MapConversions_toLocale_Test {

    private static Converter converter;

    /**
     * Real converter that performs the default conversion (identity for Strings).
     */
    private static Converter realConverter;

    /**
     * Mocked converter used in a few specialised test cases.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Simple holder for the parameterized test data.
     */
    private static class InvalidCase {

        final String language;

        final String country;

        final String script;

        final String variant;

        final String expectedMessageFragment;

        InvalidCase(String language, String country, String script, String variant, String expectedMessageFragment) {
            this.language = language;
            this.country = country;
            this.script = script;
            this.variant = variant;
            this.expectedMessageFragment = expectedMessageFragment;
        }

        @Override
        public String toString() {
            return String.format("language=%s, country=%s, script=%s, variant=%s", language, country, script, variant);
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // The real Converter needs a non‑null options instance; use defaults.
        realConverter = new Converter(new ConverterOptions());
    }

    @BeforeEach
    void setupBeforeEach() {
        // Reset the mock before each test so that stubbing does not leak.
        reset(mockConverter);
    }

    /**
     * Helper to build the map that will be passed to {@code toLocale}.
     */
    private static Map<String, String> localeMap(String language, String country, String script, String variant) {
        Map<String, String> map = new HashMap<>();
        if (language != null)
            map.put(MapConversions.LANGUAGE, language);
        if (country != null)
            map.put(MapConversions.COUNTRY, country);
        if (script != null)
            map.put(MapConversions.SCRIPT, script);
        if (variant != null)
            map.put(MapConversions.VARIANT, variant);
        return map;
    }

    private static Stream<InvalidCase> invalidCasesProvider() {
        return Stream.of(new InvalidCase("e!", null, null, null, "Locale language 'e!' invalid."), new InvalidCase("en", "U1", null, null, "Locale region 'U1' invalid."), new InvalidCase("en", null, "123", null, "Locale script '123' invalid."), new InvalidCase("en", null, null, "@@", "Locale variant '@@' invalid."));
    }

    @Test
    void toLocale_withOnlyLanguage() {
        Map<String, String> map = localeMap("en", null, null, null);
        Locale result = MapConversions.toLocale(map, realConverter);
        assertEquals(Locale.ENGLISH, result);
    }

    @Test
    void toLocale_withLanguageAndCountry() {
        Map<String, String> map = localeMap("fr", "CA", null, null);
        Locale result = MapConversions.toLocale(map, realConverter);
        assertEquals(Locale.CANADA_FRENCH, result);
    }

    @Test
    void toLocale_withScript() {
        // Example: Serbian in Latin script
        Map<String, String> map = localeMap("sr", "RS", "Latn", null);
        Locale result = MapConversions.toLocale(map, realConverter);
        assertEquals("sr-Latn-RS", result.toLanguageTag());
    }

    @Test
    void toLocale_withVariant() {
        // Example: German with variant "POSIX"
        Map<String, String> map = localeMap("de", "DE", null, "POSIX");
        Locale result = MapConversions.toLocale(map, realConverter);
        assertEquals("de-DE-POSIX", result.toString());
    }

    @Test
    void toLocale_withAllComponents() {
        // Example: Chinese (Traditional) in Taiwan with variant "POSIX"
        Map<String, String> map = localeMap("zh", "TW", "Hant", "POSIX");
        Locale result = MapConversions.toLocale(map, converter);
        // Builder builds a locale that matches the language tag
        assertEquals("zh-Hant-TW-POSIX", result.toLanguageTag());
    }

    @Test
    void toLocale_invalidLanguageThrows() {
        Map<String, String> map = localeMap("invalid-lang", null, null, null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapConversions.toLocale(map, realConverter));
        assertTrue(ex.getMessage().contains("Locale language"));
    }

    @Test
    void toLocale_invalidCountryThrows() {
        Map<String, String> map = localeMap("en", "1234", null, null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapConversions.toLocale(map, realConverter));
        assertTrue(ex.getMessage().contains("Locale region"));
    }

    @Test
    void toLocale_invalidScriptThrows() {
        Map<String, String> map = localeMap("en", "US", "!!!", null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapConversions.toLocale(map, realConverter));
        assertTrue(ex.getMessage().contains("Locale script"));
    }

    @Test
    void toLocale_invalidVariantThrows() {
        // Variant may be any string, but an empty string is treated as absent;
        // we use a control character that Builder rejects.
        Map<String, String> map = localeMap("en", "US", null, "\u0000");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapConversions.toLocale(map, converter));
        assertTrue(ex.getMessage().contains("Locale variant"));
    }

    /**
     * Language empty – the method should delegate to the private {@code fromMap}.
     */
    @Test
    void toLocale_languageEmptyFallsBackToFromMap() {
        Map<String, String> map = localeMap("", "US", null, null);
        Locale result = MapConversions.toLocale(map, realConverter);
        assertNotNull(result, "Result must be a non‑null Locale when language is empty");
        // The exact Locale is implementation‑dependent; we only assert that we got something.
    }

    /**
     * Language key missing – also triggers the {@code fromMap} fallback.
     */
    @Test
    void toLocale_missingLanguageFallsBackToFromMap() {
        Map<String, String> map = new HashMap<>();
        map.put(MapConversions.COUNTRY, "GB");
        Locale result = MapConversions.toLocale(map, realConverter);
        assertNotNull(result, "Result must be a non‑null Locale when language key is absent");
    }

    /**
     * Null map entry for optional keys – they must be ignored.
     */
    @Test
    void toLocale_nullOptionalEntriesAreIgnored() {
        Map<String, String> map = localeMap("de", null, null, null);
        Locale result = MapConversions.toLocale(map, realConverter);
        assertEquals(new Locale("de"), result);
    }

    /**
     * Null {@code from} argument – should cause a {@code NullPointerException}.
     */
    @Test
    void toLocale_nullFromThrowsNPE() {
        assertThrows(NullPointerException.class, () -> MapConversions.toLocale(null, realConverter));
    }

    /**
     * Non‑Map {@code from} argument – should cause a {@code ClassCastException}.
     */
    @Test
    void toLocale_nonMapFromThrowsClassCastException() {
        assertThrows(ClassCastException.class, () -> MapConversions.toLocale("just a string, not a map", realConverter));
    }

    /**
     * Null {@code converter} argument – should cause a {@code NullPointerException}.
     */
    @Test
    void toLocale_nullConverterThrowsNPE() {
        Map<String, String> map = localeMap("en", null, null, null);
        assertThrows(NullPointerException.class, () -> MapConversions.toLocale(map, null));
    }

    /**
     * Converter that throws a {@code RuntimeException} – the exception must propagate.
     */
    @Test
    void toLocale_converterThrowsRuntimeExceptionIsPropagated() {
        // Stub the mock to throw on any conversion.
        when(mockConverter.convert(anyString(), eq(String.class))).thenThrow(new RuntimeException("conversion failure"));
        Map<String, String> map = localeMap("en", "US", null, null);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> MapConversions.toLocale(map, mockConverter));
        assertEquals("conversion failure", ex.getMessage());
    }

    /**
     * Converter that upper‑cases every value – the Builder must receive the transformed values.
     */
    @Test
    void toLocale_converterTransformsValuesBeforeBuilder() {
        // Upper‑case conversion stub.
        when(mockConverter.convert(anyString(), eq(String.class))).thenAnswer(invocation -> ((String) invocation.getArgument(0)).toUpperCase());
        Map<String, String> map = localeMap("En", "us", "latn", "posix");
        Locale result = MapConversions.toLocale(map, mockConverter);
        // Expected upper‑cased components.
        assertEquals("EN", result.getLanguage());
        assertEquals("US", result.getCountry());
        assertEquals("LATN", result.getScript());
        assertEquals("POSIX", result.getVariant());
    }

    /**
     * Whitespace language – treated as non‑empty, leading to an IllegalArgumentException.
     */
    @Test
    void toLocale_whitespaceLanguageThrowsIllegalArgumentException() {
        Map<String, String> map = localeMap("   ", null, null, null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapConversions.toLocale(map, realConverter));
        assertTrue(ex.getMessage().contains("Locale language"));
    }

    /**
     * Variant empty string – should be ignored (no exception, only language is set).
     */
    @Test
    void toLocale_emptyVariantIsIgnored() {
        Map<String, String> map = localeMap("en", "US", null, "");
        Locale result = MapConversions.toLocale(map, realConverter);
        // Variant should be empty; builder does not set it.
        assertEquals(new Locale.Builder().setLanguage("en").setRegion("US").build(), result);
    }

    @ParameterizedTest(name = "[{index}] language={0}, country={1}, script={2}, variant={3}")
    @MethodSource("invalidCasesProvider")
    void toLocale_invalidValuesThrowIllegalArgumentException(InvalidCase ic) {
        Map<String, String> map = localeMap(ic.language, ic.country, ic.script, ic.variant);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapConversions.toLocale(map, realConverter));
        assertTrue(ex.getMessage().contains(ic.expectedMessageFragment));
    }
}
