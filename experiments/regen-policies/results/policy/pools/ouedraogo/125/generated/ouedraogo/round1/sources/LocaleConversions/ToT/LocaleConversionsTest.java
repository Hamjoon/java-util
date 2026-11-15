package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactMap;
import com.cedarsoftware.util.StringUtilities;
import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.Locale;
import java.util.Map;

import static com.cedarsoftware.util.convert.MapConversions.COUNTRY;
import static com.cedarsoftware.util.convert.MapConversions.LANGUAGE;
import static com.cedarsoftware.util.convert.MapConversions.SCRIPT;
import static com.cedarsoftware.util.convert.MapConversions.VARIANT;
import static org.junit.Assert.*;

public class LocaleConversionsTest {

    // the Converter argument is not used by LocaleConversions, so we can safely use null.
    private Converter dummyConverter;

    @Before
    public void setUp() {
        // a minimal Converter can be created with a null options object – the methods under test never touch it.
        dummyConverter = new Converter(null);
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /* -------------------------------------------------
     *  Tests for LocaleConversions.toString(Object, Converter)
     * ------------------------------------------------- */

    @Test
    public void toString_ShouldReturnLanguageTag_ForStandardLocale() {
        Locale locale = Locale.US;               // language = "en", country = "US"
        String result = LocaleConversions.toString(locale, dummyConverter);
        assertEquals("en-US", result);
    }

    @Test
    public void toString_ShouldReturnLanguageTag_WithScriptAndVariant() {
        // Build a locale with language, script, country and variant
        Locale locale = new Locale.Builder()
                .setLanguage("sr")
                .setScript("Latn")
                .setRegion("RS")
                .setVariant("POSIX")
                .build();
        String result = LocaleConversions.toString(locale, dummyConverter);
        // The IETF BCP 47 language tag format: language-script-region-variant
        assertEquals("sr-Latn-RS-POSIX", result);
    }

    @Test
    public void toString_ShouldThrowNPE_WhenLocaleIsNull() {
        thrown.expect(NullPointerException.class);
        LocaleConversions.toString(null, dummyConverter);
    }

    /* -------------------------------------------------
     *  Tests for LocaleConversions.toMap(Object, Converter)
     * ------------------------------------------------- */

    @Test
    public void toMap_ShouldContainAllComponents_WhenAllPresent() {
        // Locale with language, country, script and variant
        Locale locale = new Locale.Builder()
                .setLanguage("sr")
                .setScript("Latn")
                .setRegion("RS")
                .setVariant("POSIX")
                .build();

        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) LocaleConversions.toMap(locale, dummyConverter);

        // Verify map is a CompactMap (insertion‑order map) – size should be 4
        assertTrue(map instanceof CompactMap);
        assertEquals(4, map.size());

        assertEquals("sr", map.get(LANGUAGE));
        assertEquals("RS", map.get(COUNTRY));
        assertEquals("Latn", map.get(SCRIPT));
        assertEquals("POSIX", map.get(VARIANT));
    }

    @Test
    public void toMap_ShouldContainOnlyLanguage_WhenNoOtherComponents() {
        Locale locale = Locale.forLanguageTag("fr"); // only language, no country/script/variant

        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) LocaleConversions.toMap(locale, dummyConverter);

        assertEquals(1, map.size());
        assertEquals("fr", map.get(LANGUAGE));
        assertFalse(map.containsKey(COUNTRY));
        assertFalse(map.containsKey(SCRIPT));
        assertFalse(map.containsKey(VARIANT));
    }

    @Test
    public void toMap_ShouldOmitEmptyCountry_ScriptAndVariant() {
        // Locale with language only; country, script and variant are empty strings
        Locale locale = new Locale("de", "", "");

        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) LocaleConversions.toMap(locale, dummyConverter);

        assertEquals(1, map.size());
        assertEquals("de", map.get(LANGUAGE));
        assertFalse(map.containsKey(COUNTRY));
        assertFalse(map.containsKey(SCRIPT));
        assertFalse(map.containsKey(VARIANT));
    }

    @Test
    public void toMap_ShouldOmitCountry_WhenStringUtilitiesConsidersItEmpty() {
        // Create a locale where country is an empty string (StringUtilities.hasContent returns false)
        Locale locale = new Locale("es", "", ""); // country = ""

        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) LocaleConversions.toMap(locale, dummyConverter);

        assertEquals(1, map.size());
        assertEquals("es", map.get(LANGUAGE));
        assertFalse(map.containsKey(COUNTRY));
    }

    @Test
    public void toMap_ShouldThrowNPE_WhenLocaleIsNull() {
        thrown.expect(NullPointerException.class);
        LocaleConversions.toMap(null, dummyConverter);
    }

    /* -------------------------------------------------
     *  Additional sanity checks
     * ------------------------------------------------- */

    @Test
    public void toMap_ShouldReturnInsertionOrderedMap() {
        Locale locale = new Locale.Builder()
                .setLanguage("en")
                .setRegion("GB")
                .setScript("Latn")
                .setVariant("POSIX")
                .build();

        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) LocaleConversions.toMap(locale, dummyConverter);

        // The CompactMap builder was asked for insertion order, so the key iterator order should match insertion order.
        String[] expectedOrder = {LANGUAGE, COUNTRY, SCRIPT, VARIANT};
        int idx = 0;
        for (String key : map.keySet()) {
            assertEquals("Key order mismatch at position " + idx, expectedOrder[idx], key);
            idx++;
        }
        assertEquals("All expected keys should be present", expectedOrder.length, idx);
    }
}
