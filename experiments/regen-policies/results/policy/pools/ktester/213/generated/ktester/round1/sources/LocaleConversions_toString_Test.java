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
    Converter // not used by the method but required by signature
    mockConverter;

    /**
     * Supplies the arguments for {@link #toString_WithValidLocale_ReturnsExpectedTag}.
     *
     * <p>Each {@link Arguments} entry contains:
     * <ul>
     *   <li>the {@code from} object (a {@link Locale})</li>
     *   <li>the {@code converter} argument (null or the mock)</li>
     *   <li>the expected BCP‑47 language tag</li>
     * </ul>
     */
    private static Stream<Arguments> validLocaleProvider() {
        return Stream.of(// Standard locale with language and country
        Arguments.of(Locale.US, null, "en-US"), // Locale with only language
        Arguments.of(new Locale("fr"), null, "fr"), // Locale with language, country, and script encoded as a variant ("#Latn")
        Arguments.of(new Locale("sr", "RS", "#Latn"), null, "sr-Latn-RS"), // Locale created from a full language tag – round‑trip should preserve it
        Arguments.of(Locale.forLanguageTag("zh-Hant-TW"), null, "zh-Hant-TW"), // Root locale – no language, country or script
        Arguments.of(Locale.ROOT, null, ""), // Same as first case but with a non‑null Converter (the method ignores it)
        Arguments.of(Locale.US, mockConverterInstance(), "en-US"));
    }

    /**
     * Helper to obtain a {@link Converter} mock instance for the static method source.
     * The mock is created lazily to avoid the need for a test instance.
     */
    private static Converter mockConverterInstance() {
        // Mockito cannot be used in a static context without an injector,
        // so we create a simple stub implementation.
        return new Converter() {
        };
    }

    /**
     * Supplies the arguments for {@link #toString_WithInvalidInput_ThrowsException}.
     */
    private static Stream<Arguments> invalidInputProvider() {
        return Stream.of(// Null source object – casting succeeds (null) but dereferencing causes NPE
        Arguments.of(null, null, NullPointerException.class), // Object that is not a Locale – cast fails with ClassCastException
        Arguments.of("not a locale", null, ClassCastException.class));
    }

    @Test
    void toString_WithStandardLocale_ReturnsLanguageTag() {
        // given
        // en-US
        Locale locale = Locale.US;
        // when
        String result = LocaleConversions.toString(locale, mockConverter);
        // then
        assertEquals("en-US", result);
    }

    @Test
    void toString_WithRootLocale_ReturnsEmptyString() {
        // given
        // root locale
        Locale locale = Locale.ROOT;
        // when
        String result = LocaleConversions.toString(locale, mockConverter);
        // then
        assertEquals("", result);
    }

    @Test
    void toString_WithLanguageOnlyLocale_ReturnsLanguageTagWithoutRegion() {
        // given
        // French, no country
        Locale locale = new Locale("fr");
        // when
        String result = LocaleConversions.toString(locale, mockConverter);
        // then
        assertEquals("fr", result);
    }

    @Test
    void toString_WithNonLocaleObject_ThrowsClassCastException() {
        // given
        Object notALocale = "not a locale";
        // when / then
        assertThrows(ClassCastException.class, () -> LocaleConversions.toString(notALocale, mockConverter));
    }

    /**
     * Parameterised test that verifies the correct language tag is produced
     * for a variety of {@link Locale} instances.
     *
     * @param from      the source object passed to {@code toString}
     * @param converter the converter argument (may be {@code null})
     * @param expected  the expected language tag
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validLocaleProvider")
    void toString_WithValidLocale_ReturnsExpectedTag(Object from, Converter converter, String expected) {
        // when
        String result = LocaleConversions.toString(from, converter);
        // then
        assertEquals(expected, result);
    }

    /**
     * Parameterised test that verifies the method throws the correct exception
     * for various illegal inputs.
     *
     * @param from          the source object passed to {@code toString}
     * @param converter     the converter argument (null or mock)
     * @param expectedClass the expected exception class
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("invalidInputProvider")
    void toString_WithInvalidInput_ThrowsException(Object from, Converter converter, Class<? extends Throwable> expectedClass) {
        // when / then
        assertThrows(expectedClass, () -> LocaleConversions.toString(from, converter));
    }
}
