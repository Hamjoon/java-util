package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.*;
import static com.cedarsoftware.util.convert.MapConversions.LOCALE;

/**
 * Unit tests for {@link LocaleConversions}.
 * <p>
 * The tests cover:
 * <ul>
 *   <li>Typical locales (language only, language+country, language+country+variant)</li>
 *   <li>Edge cases such as {@code null} input and wrong input type</li>
 *   <li>Correct map structure returned by {@code toMap}</li>
 * </ul>
 */
public class LocaleConversionsTest {

    private Converter dummyConverter;   // not used by the methods, but required by signature

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        // The Converter instance is not used inside LocaleConversions, so we can safely pass null.
        // However, to keep the API contract clear we create a Converter with a null options argument.
        dummyConverter = new Converter(null);
    }

    /* --------------------------------------------------------------------- */
    /* toString() tests                                                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void toString_ShouldReturnLanguageTag_ForSimpleLocale() {
        Locale locale = new Locale("en");
        String result = LocaleConversions.toString(locale, dummyConverter);
        assertEquals("en", result);
    }

    @Test
    public void toString_ShouldReturnLanguageTag_ForLocaleWithCountry() {
        Locale locale = new Locale("en", "US");
        String result = LocaleConversions.toString(locale, dummyConverter);
        assertEquals("en-US", result);
    }

    @Test
    public void toString_ShouldReturnLanguageTag_ForLocaleWithVariant() {
        Locale locale = new Locale("en", "US", "POSIX");
        String result = LocaleConversions.toString(locale, dummyConverter);
        assertEquals("en-US-POSIX", result);
    }

    @Test
    public void toString_ShouldThrow_NullPointerException_WhenLocaleIsNull() {
        thrown.expect(NullPointerException.class);
        LocaleConversions.toString(null, dummyConverter);
    }

    @Test
    public void toString_ShouldThrow_ClassCastException_WhenInputIsNotLocale() {
        thrown.expect(ClassCastException.class);
        LocaleConversions.toString("not a locale", dummyConverter);
    }

    /* --------------------------------------------------------------------- */
    /* toMap() tests                                                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void toMap_ShouldContainSingleEntry_WithCorrectKeyAndValue() {
        Locale locale = new Locale("fr", "CA");
        Map<?, ?> result = LocaleConversions.toMap(locale, dummyConverter);

        assertNotNull("Result map must not be null", result);
        assertEquals("Map must contain exactly one entry", 1, result.size());
        assertTrue("Map must contain the LOCALE key", result.containsKey(LOCALE));

        Object value = result.get(LOCALE);
        assertTrue("Value associated with LOCALE must be a String", value instanceof String);
        assertEquals("Value must be the language tag of the locale", locale.toLanguageTag(), value);
    }

    @Test
    public void toMap_ShouldWork_ForLocaleWithOnlyLanguage() {
        Locale locale = new Locale("de");
        Map<?, ?> result = LocaleConversions.toMap(locale, dummyConverter);
        assertEquals("de", result.get(LOCALE));
    }

    @Test
    public void toMap_ShouldThrow_NullPointerException_WhenLocaleIsNull() {
        thrown.expect(NullPointerException.class);
        LocaleConversions.toMap(null, dummyConverter);
    }

    @Test
    public void toMap_ShouldThrow_ClassCastException_WhenInputIsNotLocale() {
        thrown.expect(ClassCastException.class);
        LocaleConversions.toMap(12345, dummyConverter);
    }
}
