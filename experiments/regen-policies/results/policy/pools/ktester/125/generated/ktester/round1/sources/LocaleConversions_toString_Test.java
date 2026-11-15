package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Locale;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocaleConversions_toString_Test {

    @Mock
    private Converter // not used by the method but required by signature
    converter;

    /**
     * Provides a stream of valid Locale instances together with the expected
     * language tag and a flag indicating whether the {@code Converter}
     * argument should be {@code null}.
     */
    private static Stream<Arguments> validLocaleProvider() {
        return Stream.of(// Locale with only language
        Arguments.of(Locale.ENGLISH, "en", true), // Locale with language and country
        Arguments.of(Locale.US, "en-US", true), // Constructed with language and region
        Arguments.of(new Locale("zh", "CN"), "zh-CN", true), // Language, script and region (e.g., Serbian Latin in Serbia)
        Arguments.of(new Locale.Builder().setLanguage("sr").setScript("Latn").setRegion("RS").build(), "sr-Latn-RS", true), // Language, region and variant
        Arguments.of(new Locale.Builder().setLanguage("sl").setRegion("IT").setVariant("rozaj").build(), "sl-IT-rozaj", true), // Language, region and numeric variant
        Arguments.of(new Locale.Builder().setLanguage("de").setRegion("DE").setVariant("1996").build(), "de-DE-1996", true), // Same as above but with a non‑null Converter (the mock)
        Arguments.of(Locale.CANADA_FRENCH, "fr-CA", false));
    }

    /**
     * Provides a stream of invalid inputs together with the expected exception type.
     */
    private static Stream<Arguments> invalidInputProvider() {
        return Stream.of(// Null source object – should throw NullPointerException
        Arguments.of(null, NullPointerException.class), // A plain String – should throw ClassCastException
        Arguments.of("not a locale", ClassCastException.class), // An arbitrary Object – should throw ClassCastException
        Arguments.of(new Object(), ClassCastException.class));
    }

    @Test
    void toString_returnsLanguageTag_forValidLocale() {
        // given
        Locale locale = Locale.forLanguageTag("en-US");
        // when
        String result = LocaleConversions.toString(locale, converter);
        // then
        assertEquals("en-US", result);
    }

    @Test
    void toString_throwsNullPointerException_whenFromIsNull() {
        // given
        Object nullLocale = null;
        // then
        assertThrows(NullPointerException.class, () -> LocaleConversions.toString(nullLocale, converter));
    }

    @Test
    void toString_throwsClassCastException_whenFromIsNotLocale() {
        // given
        Object notALocale = "just a string";
        // then
        assertThrows(ClassCastException.class, () -> LocaleConversions.toString(notALocale, converter));
    }

    @ParameterizedTest(name = "[{index}] Locale {0} → \"{1}\" (converter null: {2})")
    @MethodSource("validLocaleProvider")
    void toString_returnsExpectedLanguageTag(Locale locale, String expectedTag, boolean converterIsNull) {
        // given
        Object from = locale;
        // use mock when not null
        Converter conv = converterIsNull ? null : converter;
        // when
        String result = LocaleConversions.toString(from, conv);
        // then
        assertEquals(expectedTag, result, () -> "Expected language tag '" + expectedTag + "' but got '" + result + "'");
    }

    @ParameterizedTest(name = "[{index}] from={0} → throws {1}")
    @MethodSource("invalidInputProvider")
    void toString_throwsExpectedException_forInvalidInput(Object from, Class<? extends Throwable> expectedException) {
        // when / then
        assertThrows(expectedException, () -> LocaleConversions.toString(from, null), () -> "Expected " + expectedException.getSimpleName() + " when passing " + from);
    }
}
