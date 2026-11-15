package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.Locale;
import java.util.Map;

/**
 * Test class for {@link LocaleConversions}.
 *
 * The class under test contains two package‑private static conversion methods:
 * <ul>
 *   <li>{@code static String toString(Object from, Converter converter)}</li>
 *   <li>{@code static Map<?,?> toMap(Object from, Converter converter)}</li>
 * </ul>
 *
 * The tests are written in the same package so that the package‑private methods are
 * directly accessible without using reflection.
 *
 * The {@link Converter} argument is not used by the implementation, therefore a
 * {@code null} reference (or a minimal real instance) can be supplied.
 */
public class LocaleConversionsTest {

    /** The {@link Converter} instance used for all tests – it is never consulted by the code. */
    private Converter dummyConverter;

    @Before
    public void setUp() {
        // The Converter constructor requires a ConverterOptions instance.
        // As the implementation does not touch the Converter, we can safely pass null.
        dummyConverter = new Converter(null);
    }

    // -------------------------------------------------------------------------
    //  1.  List of public (exposed) methods
    // -------------------------------------------------------------------------
    // The class itself is {@code final} and has a private constructor, therefore the
    // only callable members are the two static methods below (package‑private):
    //
    //   static String toString(Object from, Converter converter)
    //   static Map<?,?> toMap(Object from, Converter converter)
    //
    // Both accept an {@link Object} that must be a {@link Locale} instance; otherwise
    // a {@code ClassCastException} will be thrown at runtime.

    // -------------------------------------------------------------------------
    //  2.  Basic functionality tests
    // -------------------------------------------------------------------------

    @Test
    public void testToString_WithSimpleLocale() {
        Locale locale = Locale.US; // language=en, country=US
        String result = LocaleConversions.toString(locale, dummyConverter);
        assertEquals("en-US", result);
    }

    @Test
    public void testToString_WithLanguageOnlyLocale() {
        Locale locale = new Locale("fr"); // French, no country
        String result = LocaleConversions.toString(locale, dummyConverter);
        assertEquals("fr", result);
    }

    @Test
    public void testToString_WithLocaleHavingVariant() {
        Locale locale = new Locale("de", "DE", "POSIX");
        String result = LocaleConversions.toString(locale, dummyConverter);
        // Language tag format: language-country-variant (variant is appended after a dash)
        assertEquals("de-DE-POSIX", result);
    }

    @Test
    public void testToMap_BasicLocale() {
        Locale locale = Locale.CANADA_FRENCH; // fr-CA
        Map<?, ?> map = LocaleConversions.toMap(locale, dummyConverter);
        assertNotNull(map);
        assertTrue(map.containsKey(MapConversions.LOCALE));
        assertEquals("fr-CA", map.get(MapConversions.LOCALE));
    }

    @Test
    public void testToMap_WithRootLocale() {
        Locale locale = Locale.ROOT; // empty language tag
        Map<?, ?> map = LocaleConversions.toMap(locale, dummyConverter);
        assertNotNull(map);
        assertTrue(map.containsKey(MapConversions.LOCALE));
        assertEquals("", map.get(MapConversions.LOCALE));
    }

    // -------------------------------------------------------------------------
    //  3.  Edge‑case / exception tests
    // -------------------------------------------------------------------------

    @Test(expected = ClassCastException.class)
    public void testToString_WithNonLocaleObject() {
        // Passing an object that is not a Locale should cause a ClassCastException
        LocaleConversions.toString("not a locale", dummyConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToMap_WithNonLocaleObject() {
        // Same expectation for the map conversion method
        LocaleConversions.toMap(12345, dummyConverter);
    }

    @Test
    public void testToString_WithNullLocale() {
        try {
            LocaleConversions.toString(null, dummyConverter);
            fail("Expected NullPointerException when source object is null");
        } catch (NullPointerException | ClassCastException e) {
            // The implementation does a cast before any null check, so a
            // NullPointerException (from the cast) or ClassCastException is acceptable.
            // We accept either to keep the test robust against JDK variations.
        }
    }

    @Test
    public void testToMap_WithNullLocale() {
        try {
            LocaleConversions.toMap(null, dummyConverter);
            fail("Expected NullPointerException when source object is null");
        } catch (NullPointerException | ClassCastException e) {
            // Same reasoning as above.
        }
    }

    @Test
    public void testToString_IgnoresConverterArgument() {
        Locale locale = Locale.ITALY; // it-IT
        // Pass a non‑null Converter; the method should still return the same tag.
        Converter anotherConverter = new Converter(null);
        String result = LocaleConversions.toString(locale, anotherConverter);
        assertEquals("it-IT", result);
    }

    @Test
    public void testToMap_IgnoresConverterArgument() {
        Locale locale = new Locale("es", "MX"); // es-MX
        Converter anotherConverter = new Converter(null);
        Map<?, ?> map = LocaleConversions.toMap(locale, anotherConverter);
        assertEquals("es-MX", map.get(MapConversions.LOCALE));
    }

    // -------------------------------------------------------------------------
    //  4.  Additional sanity checks
    // -------------------------------------------------------------------------

    @Test
    public void testToMap_ReturnsLinkedHashMapPreservingInsertionOrder() {
        Locale locale = Locale.JAPAN; // ja-JP
        Map<?, ?> map = LocaleConversions.toMap(locale, dummyConverter);
        // Since the implementation uses LinkedHashMap and only inserts one entry,
        // the iteration order must contain the LOCALE key first.
        assertTrue(map.keySet().iterator().next().equals(MapConversions.LOCALE));
    }
}
