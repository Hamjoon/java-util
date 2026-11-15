package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class Converter_convert_Test {

    private Converter converter;

    class CustomClass {
    }

    class CustomClass {
    }

    @BeforeEach
    void setupBeforeEach() {
        // Each test gets a fresh Converter instance to avoid cross‑test contamination.
        converter = new Converter(defaultOptions());
    }

    /**
     * -----------------------------------------------------------------------
     *  Helper to create a minimal {@link ConverterOptions} implementation.
     *  The options expose no custom overrides, use UTF‑8 charset and the
     *  system default zone.  All other custom‑option methods return {@code null}
     *  because they are not needed for the tests below.
     *  ---------------------------------------------------------------------
     */
    private static ConverterOptions defaultOptions() {
        return new ConverterOptions() {

            @Override
            public Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> getConverterOverrides() {
                return Collections.emptyMap();
            }

            @Override
            public java.nio.charset.Charset getCharset() {
                return java.nio.charset.StandardCharsets.UTF_8;
            }

            @Override
            public ZoneId getZoneId() {
                return ZoneId.systemDefault();
            }

            @Override
            public Character trueChar() {
                return null;
            }

            @Override
            public Character falseChar() {
                return null;
            }

            @Override
            public <T> T getCustomOption(String name) {
                return null;
            }
        };
    }

    /**
     * -----------------------------------------------------------------------
     *  1️⃣  Verify that a {@code null} target type is rejected.
     *  ---------------------------------------------------------------------
     */
    @Test
    void convert_nullTargetType_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> converter.convert("anything", null));
        assertEquals("toType cannot be null", ex.getMessage());
    }

    /**
     * -----------------------------------------------------------------------
     *  2️⃣  Converting a {@code null} source to a reference type should yield {@code null}.
     *  ---------------------------------------------------------------------
     */
    @Test
    void convert_nullSourceToReference_returnsNull() {
        String result = converter.convert(null, String.class);
        assertNull(result);
    }

    /**
     * -----------------------------------------------------------------------
     *  3️⃣  Converting a {@code null} source to a primitive type should return the
     *      default primitive value (wrapped as its wrapper class).
     *  ---------------------------------------------------------------------
     */
    @Test
    void convert_nullSourceToPrimitive_returnsDefaultValue() {
        Integer intResult = converter.convert(null, int.class);
        assertEquals(0, intResult);
        Boolean boolResult = converter.convert(null, boolean.class);
        assertFalse(boolResult);
    }

    /**
     * -----------------------------------------------------------------------
     *  4️⃣  User‑added conversion overrides must be honoured and must take
     *      precedence over built‑in conversions.
     *  ---------------------------------------------------------------------
     */
    @Test
    void convert_userAddedOverride_isUsed() {
        // Add a custom conversion: String → Integer (always returns 42)
        converter.addConversion(String.class, Integer.class, (from, conv) -> 42);
        Integer result = converter.convert("ignored", Integer.class);
        assertEquals(42, result);
    }

    /**
     * -----------------------------------------------------------------------
     *  5️⃣  Verify that inheritance‑based conversion works.  Converting an
     *      {@code Integer} to its superclass {@code Number} should succeed
     *      via the inherited‑converter path.
     *  ---------------------------------------------------------------------
     */
    @Test
    void convert_inheritedConversion_isSupported() {
        Number result = converter.convert(123, Number.class);
        assertEquals(123, result);
        // The conversion should be reported as supported.
        assertTrue(converter.isConversionSupportedFor(Integer.class, Number.class));
    }

    /**
     * -----------------------------------------------------------------------
     *  6️⃣  An unsupported conversion must raise an {@link IllegalArgumentException}
     *      with a helpful message.
     *  ---------------------------------------------------------------------
     */
    @Test
    void convert_unsupportedConversion_throwsIllegalArgumentException() {
        Object src = new Object();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> converter.convert(src, Integer.class));
        String msg = ex.getMessage();
        assertTrue(msg.contains("Unsupported conversion"));
        assertTrue(msg.contains("source type [java.lang.Object]"));
        assertTrue(msg.contains("target type 'Integer'"));
    }

    /* -----------------------------------------------------------------------
     *  Group: UnsupportedConversionException
     * --------------------------------------------------------------------- */
    @Test
    void convert_booleanToDate_unsupportedConversion() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> converter.convert(false, java.util.Date.class));
        String msg = ex.getMessage();
        assertTrue(msg.contains("Unsupported conversion"));
        assertTrue(msg.contains("source type [java.lang.Boolean]"));
        assertTrue(msg.contains("target type 'Date'"));
    }

    /* -----------------------------------------------------------------------
     *  Group: NullFromUnsupportedConversion
     * --------------------------------------------------------------------- */
    @Test
    void convert_nullSourceToDate_unsupportedConversion() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> converter.convert(null, java.util.Date.class));
        String msg = ex.getMessage();
        assertTrue(msg.contains("Unsupported conversion"));
        assertTrue(msg.contains("source type [java.lang.Void]"));
        assertTrue(msg.contains("target type 'Date'"));
    }

    /* -----------------------------------------------------------------------
     *  Group: UserAddedConversionSuccess
     * --------------------------------------------------------------------- */
    @Test
    void convert_userAddedStringToBoolean_success() {
        converter.addConversion(String.class, Boolean.class, (from, conv) -> "yes".equalsIgnoreCase((String) from));
        Boolean result = converter.convert("yes", Boolean.class);
        assertTrue(result);
    }

    /* -----------------------------------------------------------------------
     *  Group: UserAddedConversionOverridesFactory
     * --------------------------------------------------------------------- */
    @Test
    void convert_userOverrideOverridesFactory() {
        // Factory conversion Integer → String would normally give "123"
        converter.addConversion(Integer.class, String.class, (from, conv) -> "overridden");
        String result = converter.convert(123, String.class);
        assertEquals("overridden", result);
    }

    /* -----------------------------------------------------------------------
     *  Group: UserAddedUnsupportedConversionFallsThrough
     * --------------------------------------------------------------------- */
    @Test
    void convert_userUnsupportedSentinel_isIgnored() throws Exception {
        // Obtain the internal UNSUPPORTED sentinel via reflection.
        Field f = Converter.class.getDeclaredField("UNSUPPORTED");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        Convert<?> unsupported = (Convert<?>) f.get(null);
        // Register a user conversion that is the sentinel.
        converter.addConversion(String.class, java.util.Date.class, unsupported);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> converter.convert("2020-01-01", java.util.Date.class));
        String msg = ex.getMessage();
        assertTrue(msg.contains("Unsupported conversion"));
        assertTrue(msg.contains("source type [java.lang.String]"));
        assertTrue(msg.contains("target type 'Date'"));
    }

    /* -----------------------------------------------------------------------
     *  Group: PrimitiveTargetConversion
     * --------------------------------------------------------------------- */
    @Test
    void convert_integerToPrimitiveInt_identity() {
        int result = converter.convert(42, int.class);
        assertEquals(42, result);
    }

    /* -----------------------------------------------------------------------
     *  Group: InheritedConversionViaSuperclass
     * --------------------------------------------------------------------- */
    @Test
    void convert_doubleToString_viaInheritedNumberConversion() {
        String result = converter.convert(3.1415, String.class);
        assertEquals("3.1415", result);
        assertTrue(converter.isConversionSupportedFor(Double.class, String.class));
    }

    /* -----------------------------------------------------------------------
     *  Group: InheritedUnsupportedSentinel
     * --------------------------------------------------------------------- */
    @Test
    void convert_doubleToString_userSentinelIgnored() throws Exception {
        // Register a sentinel for Number → String.
        Field f = Converter.class.getDeclaredField("UNSUPPORTED");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        Convert<?> unsupported = (Convert<?>) f.get(null);
        converter.addConversion(Number.class, String.class, unsupported);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> converter.convert(3.14, String.class));
        String msg = ex.getMessage();
        assertTrue(msg.contains("Unsupported conversion"));
        assertTrue(msg.contains("source type [java.lang.Double]"));
        assertTrue(msg.contains("target type 'String'"));
    }

    /* -----------------------------------------------------------------------
     *  Group: UserConversionThrowsException
     * --------------------------------------------------------------------- */
    @Test
    void convert_userConversionThrowsNumberFormatException() {
        converter.addConversion(String.class, Integer.class, (from, conv) -> Integer.parseInt((String) from));
        assertThrows(NumberFormatException.class, () -> converter.convert("notANumber", Integer.class));
    }

    /* -----------------------------------------------------------------------
     *  Group: PrimitiveTargetNullSource
     * --------------------------------------------------------------------- */
    @Test
    void convert_nullSourceToPrimitiveInt_unsupportedConversion() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> converter.convert(null, int.class));
        String msg = ex.getMessage();
        assertTrue(msg.contains("Unsupported conversion"));
        assertTrue(msg.contains("source type [java.lang.Void]"));
        assertTrue(msg.contains("target type 'int'"));
    }

    /* -----------------------------------------------------------------------
     *  Group: SupertypeUnsupportedConversion
     * --------------------------------------------------------------------- */
    @Test
    void convert_stringToObject_unsupportedConversion() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> converter.convert("sample", Object.class));
        String msg = ex.getMessage();
        assertTrue(msg.contains("Unsupported conversion"));
        assertTrue(msg.contains("source type [java.lang.String]"));
        assertTrue(msg.contains("target type 'Object'"));
    }

    /* -----------------------------------------------------------------------
     *  Group: IdentityConversionMissing
     * --------------------------------------------------------------------- */
    @Test
    void convert_customClassIdentityMissing_unsupportedConversion() {
        // Dummy custom class defined locally for the test.
        class CustomClass {
        }
        CustomClass src = new CustomClass();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> converter.convert(src, CustomClass.class));
        String msg = ex.getMessage();
        assertTrue(msg.contains("Unsupported conversion"));
        assertTrue(msg.contains("source type ["));
        assertTrue(msg.contains("CustomClass"));
        assertTrue(msg.contains("target type 'CustomClass'"));
    }
}
