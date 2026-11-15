package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Locale;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class LocaleConversions_toString_Test {

    /**
     * Mocked {@link Converter} – not used by the method but required for the signature.
     */
    @Mock
    private Converter mockConverter;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise mocks (handled by MockitoExtension, but explicit init is harmless)
        MockitoAnnotations.openMocks(this);
    }

    private static Stream<Arguments> validLocaleProvider() {
        return Stream.of(// Locale with only language (ENGLISH) → "en"
        arguments(Locale.ENGLISH, Locale.ENGLISH.toLanguageTag(), "Locale with only language (ENGLISH) should produce language tag 'en'."), // Locale US (language + country) → "en-US"
        arguments(Locale.US, Locale.US.toLanguageTag(), "Locale with language and country (US) should produce language tag 'en-US'."), // new Locale("fr","CA") → "fr-CA"
        arguments(new Locale("fr", "CA"), new Locale("fr", "CA").toLanguageTag(), "Locale constructed with language 'fr' and country 'CA' should produce 'fr-CA'."), // Locale with language, script, region → "zh-Hans-CN"
        arguments(new Locale.Builder().setLanguage("zh").setScript("Hans").setRegion("CN").build(), new Locale.Builder().setLanguage("zh").setScript("Hans").setRegion("CN").build().toLanguageTag(), "Locale with language, script, and region should produce full language tag including script."), // Locale with language, country, variant → "de-DE-POSIX"
        arguments(new Locale("de", "DE", "POSIX"), new Locale("de", "DE", "POSIX").toLanguageTag(), "Locale with language, country, and variant should include variant in the language tag."), // Locale with script only → "en-Latn"
        arguments(new Locale.Builder().setLanguage("en").setScript("Latn").build(), new Locale.Builder().setLanguage("en").setScript("Latn").build().toLanguageTag(), "Locale built with language 'en' and script 'Latn' (no region) should produce the tag 'en-Latn'."), // Locale with language and variant only → "en-POSIX"
        arguments(new Locale("en", "", "POSIX"), new Locale("en", "", "POSIX").toLanguageTag(), "Locale with language 'en' and variant 'POSIX' but no country should generate the tag 'en-POSIX'."), // Locale with empty language → "und-US"
        arguments(new Locale("", "US"), new Locale("", "US").toLanguageTag(), "Locale with empty language and country 'US' should be represented as the undefined language tag 'und-US'."), // Locale with private‑use extension → "en-x-private"
        arguments(new Locale.Builder().setLanguage("en").setExtension('x', "private").build(), new Locale.Builder().setLanguage("en").setExtension('x', "private").build().toLanguageTag(), "Locale containing a private‑use extension should be reflected in the language tag as 'en-x-private'."));
    }

    private static Stream<Arguments> invalidInputProvider() {
        return Stream.of(arguments(null, NullPointerException.class, "Passing a null 'from' should result in a NullPointerException because the method casts the argument to Locale."), arguments("not a locale", ClassCastException.class, "Passing an object that is not a Locale should cause a ClassCastException when the cast is performed."));
    }

    @Test
    void toString_returnsCorrectLanguageTag_forSimpleLocale() {
        // GIVEN
        // language = "en", country = "US"
        Locale locale = Locale.US;
        // "en-US"
        String expectedTag = locale.toLanguageTag();
        // WHEN
        String result = LocaleConversions.toString(locale, mockConverter);
        // THEN
        assertEquals(expectedTag, result, "LocaleConversions.toString should return the locale's language tag");
    }

    @Test
    void toString_returnsCorrectLanguageTag_forLocaleWithScriptAndVariant() {
        // GIVEN
        Locale locale = new Locale.Builder().setLanguage("sr").setScript("Latn").setRegion("RS").setVariant("rozaj").// "sr-Latn-RS-rozaj"
        build();
        String expectedTag = locale.toLanguageTag();
        // WHEN
        String result = LocaleConversions.toString(locale, mockConverter);
        // THEN
        assertEquals(expectedTag, result, "LocaleConversions.toString should correctly handle script and variant components");
    }

    @Test
    void toString_throwsClassCastException_whenObjectIsNotLocale() {
        // GIVEN
        Object notALocale = "not a locale";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> LocaleConversions.toString(notALocale, mockConverter), "Passing a non‑Locale object should result in a ClassCastException");
    }

    @Test
    void toString_throwsNullPointerException_whenFromIsNull() {
        // GIVEN
        Object nullObj = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocaleConversions.toString(nullObj, mockConverter), "Passing null as the source object should result in a NullPointerException");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("validLocaleProvider")
    void toString_returnsCorrectLanguageTag_forValidLocales(Locale locale, String expectedTag, String description) {
        // WHEN
        String result = LocaleConversions.toString(locale, mockConverter);
        // THEN
        assertEquals(expectedTag, result, () -> "Unexpected language tag for case: " + description);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("invalidInputProvider")
    void toString_throwsExpectedException_forInvalidInputs(Object from, Class<? extends Throwable> expectedException, String description) {
        // WHEN / THEN
        assertThrows(expectedException, () -> LocaleConversions.toString(from, mockConverter), () -> "Expected " + expectedException.getSimpleName() + " for case: " + description);
    }

    @Test
    void toString_ignoresNonNullConverter_andReturnsCorrectTag() {
        // GIVEN
        // "de"
        Locale locale = Locale.GERMAN;
        String expectedTag = locale.toLanguageTag();
        // a concrete (non‑mock) Converter instance – the class is empty in the source,
        // so an anonymous subclass suffices.
        Converter realConverter = new Converter() {
        };
        // WHEN
        String result = LocaleConversions.toString(locale, realConverter);
        // THEN
        assertEquals(expectedTag, result, "Even when a non‑null Converter instance is supplied, the method should still return the correct language tag.");
    }
}
