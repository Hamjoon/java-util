package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Converter_isConversionSupportedFor_Test {

    /**
     * The instance under test.
     */
    private Converter converter;

    /**
     * Mocked options passed to the {@link Converter} constructor.
     */
    @Mock
    private ConverterOptions mockOptions;

    /**
     * Reference to the private static conversion table.
     */
    private static Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> staticDb;

    /**
     * The sentinel value used by the library to denote an unsupported conversion.
     */
    private static Convert<?> UNSUPPORTED_SENTINEL;

    @BeforeAll
    static void initStaticFields() throws Exception {
        // Obtain the private static CONVERSION_DB map.
        Field dbField = Converter.class.getDeclaredField("CONVERSION_DB");
        dbField.setAccessible(true);
        //noinspection unchecked
        staticDb = (Map<Map.Entry<Class<?>, Class<?>>, Convert<?>>) dbField.get(null);
        // Obtain the UNSUPPORTED sentinel.
        Field unsupField = Converter.class.getDeclaredField("UNSUPPORTED");
        unsupField.setAccessible(true);
        //noinspection unchecked
        UNSUPPORTED_SENTINEL = (Convert<?>) unsupField.get(null);
    }

    @BeforeEach
    void setupBeforeEach() {
        // The Converter constructor copies any user‑provided overrides.
        // We return an empty map so the USER_DB starts empty.
        Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> emptyOverrides = Collections.emptyMap();
        // The ConverterOptions interface has many default methods; only the one used by the
        // constructor (getConverterOverrides) needs to be stubbed.
        org.mockito.Mockito.when(mockOptions.getConverterOverrides()).thenReturn(emptyOverrides);
        // Other option methods are not consulted by the code under test, but we provide harmless defaults.
        org.mockito.Mockito.when(mockOptions.getCharset()).thenReturn(null);
        org.mockito.Mockito.when(mockOptions.getZoneId()).thenReturn(null);
        org.mockito.Mockito.when(mockOptions.trueChar()).thenReturn(null);
        org.mockito.Mockito.when(mockOptions.falseChar()).thenReturn(null);
        org.mockito.Mockito.when(mockOptions.getCustomOption(Mockito.anyString())).thenReturn(null);
        converter = new Converter(mockOptions);
        // Stub the options required by the constructor.
        when(mockOptions.getConverterOverrides()).thenReturn(Collections.emptyMap());
        when(mockOptions.getCharset()).thenReturn(null);
        when(mockOptions.getZoneId()).thenReturn(null);
        when(mockOptions.trueChar()).thenReturn(null);
        when(mockOptions.falseChar()).thenReturn(null);
        when(mockOptions.getCustomOption(anyString())).thenReturn(null);
        converter = new Converter(mockOptions);
        //noinspection unchecked
        ((Map<?, ?>) getField(converter, "USER_DB")).clear();
    }

    /**
     * Helper to read a private field via reflection.
     */
    private static Object getField(Object target, String name) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Helper to create a map entry used as a key in the conversion tables.
     */
    private static Map.Entry<Class<?>, Class<?>> pair(Class<?> src, Class<?> tgt) {
        return new SimpleEntry<>(src, tgt);
    }

    /**
     * Data for the “User DB conversion present (supported)” group.
     */
    private static Stream<Arguments> userSupported() {
        return Stream.of(Arguments.of(String.class, Integer.class, true, "User‑added conversion should be recognised"));
    }

    /**
     * Data for the “User DB conversion present (unsupported)” group.
     */
    private static Stream<Arguments> userUnsupported() {
        return Stream.of(Arguments.of(Long.class, String.class, false, "UNSUPPORTED sentinel in USER_DB must be ignored"));
    }

    /**
     * Data for the “Static DB conversion present (supported)” group.
     */
    private static Stream<Arguments> staticSupported() {
        return Stream.of(Arguments.of(Boolean.class, String.class, true, "Built‑in conversion should be found in CONVERSION_DB"));
    }

    /**
     * Data for the “Static DB conversion present (unsupported)” group.
     */
    private static Stream<Arguments> staticUnsupported() {
        return Stream.of(Arguments.of(Float.class, Double.class, false, "UNSUPPORTED sentinel in CONVERSION_DB must be ignored"));
    }

    /**
     * Data for the “Inherited converter present” group.
     */
    private static Stream<Arguments> inheritedPresent() {
        return Stream.of(Arguments.of(Number.class, String.class, true, "Number → String is provided by inherited converter"));
    }

    /**
     * Data for the “No conversion supported anywhere” group.
     */
    private static Stream<Arguments> noConversion() {
        return Stream.of(Arguments.of(Date.class, UUID.class, false, "No entry anywhere, should return false"));
    }

    /**
     * Data for the “Primitive ↔ Wrapper handling” group.
     */
    private static Stream<Arguments> primitiveWrapper() {
        return Stream.of(Arguments.of(int.class, Integer.class, true, "int → Integer becomes Integer → Integer (identity)"), Arguments.of(boolean.class, String.class, true, "boolean → Boolean wrapper, built‑in Boolean→String exists"), Arguments.of(char.class, Long.class, false, "char → Character has no conversion to Long"));
    }

    /**
     * Data for the “User DB unsupported, static DB supported” group.
     */
    private static Stream<Arguments> userUnsupStaticSupported() {
        return Stream.of(Arguments.of(Double.class, String.class, true, "User entry UNSUPPORTED, static entry supported"));
    }

    /**
     * Data for the “Static DB unsupported, inherited converter supported” group.
     */
    private static Stream<Arguments> staticUnsupInheritedSupported() {
        return Stream.of(Arguments.of(Short.class, CharSequence.class, true, "Static UNSUPPORTED, inherited (toString) conversion works"));
    }

    /**
     * Data for the “Inherited conversion via assignability” group.
     */
    private static Stream<Arguments> assignableInherited() {
        return Stream.of(Arguments.of(Integer.class, Number.class, true, "Integer is assignable to Number – identity conversion"));
    }

    /**
     * Data for the “Primitive to different primitive via wrapper” group.
     */
    private static Stream<Arguments> primitiveToPrimitive() {
        return Stream.of(Arguments.of(int.class, long.class, true, "int → Integer, long → Long, built‑in Integer→Long exists"));
    }

    /**
     * Data for the “Identity conversion when no explicit entry” group.
     */
    private static Stream<Arguments> identityConversion() {
        return Stream.of(Arguments.of(BigDecimal.class, BigDecimal.class, true, "Identical source/target should be considered supported"));
    }

    /**
     * Verify that a built‑in conversion (registered in the static {@code CONVERSION_DB})
     * is reported as supported.  The library contains a conversion from {@code int}
     * (primitive) to {@code Integer} (wrapper) – after normalisation both become
     * {@code Integer.class}, so the method should return {@code true}.
     */
    @Test
    void builtInConversionIsSupported() {
        assertTrue(converter.isConversionSupportedFor(int.class, Integer.class), "Primitive int → Integer should be supported by the built‑in conversion table");
    }

    /**
     * Verify that a user‑added conversion is recognised.  We add a simple
     * conversion from {@code String} to {@code Integer} and then query the
     * support method.
     */
    @Test
    void customUserConversionIsSupported() {
        // Add a user conversion: String → Integer
        converter.addConversion(String.class, Integer.class, (Object src) -> Integer.valueOf((String) src));
        assertTrue(converter.isConversionSupportedFor(String.class, Integer.class), "User‑added conversion String → Integer should be reported as supported");
    }

    /**
     * Verify that a completely unsupported conversion pair returns {@code false}.
     * The library does not contain a conversion from {@code Thread} to {@code String},
     * nor can it be inherited, so the result must be {@code false}.
     */
    @Test
    void unsupportedConversionReturnsFalse() {
        assertFalse(converter.isConversionSupportedFor(Thread.class, String.class), "Conversion from Thread to String is not supported and should return false");
    }

    /**
     * Verify that the method correctly handles {@code null} arguments.
     * The implementation does not guard against nulls, so a {@code NullPointerException}
     * is expected.
     */
    @Test
    void nullArgumentsThrowException() {
        assertThrows(NullPointerException.class, () -> converter.isConversionSupportedFor(null, String.class));
        assertThrows(NullPointerException.class, () -> converter.isConversionSupportedFor(String.class, null));
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("userSupported")
    void testUserDbSupported(Class<?> src, Class<?> tgt, boolean expected, String description) {
        // Register a real conversion in USER_DB.
        converter.addConversion(src, tgt, o -> {
            // simple stub conversion
            return null;
        });
        assertEquals(expected, converter.isConversionSupportedFor(src, tgt), description);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("userUnsupported")
    void testUserDbUnsupportedSentinel(Class<?> src, Class<?> tgt, boolean expected, String description) {
        // Directly put the UNSUPPORTED sentinel into USER_DB via reflection.
        @SuppressWarnings("unchecked")
        Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> userDb = (Map<Map.Entry<Class<?>, Class<?>>, Convert<?>>) getField(converter, "USER_DB");
        userDb.put(pair(src, tgt), UNSUPPORTED_SENTINEL);
        // No static entry – expect false.
        assertEquals(expected, converter.isConversionSupportedFor(src, tgt), description);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("staticSupported")
    void testStaticDbSupported(Class<?> src, Class<?> tgt, boolean expected, String description) {
        // Ensure the static DB contains a supported conversion.
        // The library already ships Boolean→String, but we make sure it exists.
        staticDb.putIfAbsent(pair(src, tgt), (Convert<?>) o -> null);
        assertEquals(expected, converter.isConversionSupportedFor(src, tgt), description);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("staticUnsupported")
    void testStaticDbUnsupportedSentinel(Class<?> src, Class<?> tgt, boolean expected, String description) {
        // Insert the UNSUPPORTED sentinel into the static map.
        staticDb.put(pair(src, tgt), UNSUPPORTED_SENTINEL);
        assertEquals(expected, converter.isConversionSupportedFor(src, tgt), description);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("inheritedPresent")
    void testInheritedConverterPresent(Class<?> src, Class<?> tgt, boolean expected, String description) {
        // No entry in USER_DB or CONVERSION_DB – rely on inherited logic.
        assertEquals(expected, converter.isConversionSupportedFor(src, tgt), description);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("noConversion")
    void testNoConversionAnywhere(Class<?> src, Class<?> tgt, boolean expected, String description) {
        assertEquals(expected, converter.isConversionSupportedFor(src, tgt), description);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("primitiveWrapper")
    void testPrimitiveWrapperHandling(Class<?> src, Class<?> tgt, boolean expected, String description) {
        // For the supported cases we rely on the library’s built‑in entries.
        // For the unsupported case we make sure no accidental entry exists.
        if (!expected) {
            staticDb.remove(pair(ClassUtilities.toPrimitiveWrapperClass(src), ClassUtilities.toPrimitiveWrapperClass(tgt)));
        }
        assertEquals(expected, converter.isConversionSupportedFor(src, tgt), description);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("userUnsupStaticSupported")
    void testUserUnsupportedStaticSupported(Class<?> src, Class<?> tgt, boolean expected, String description) {
        // User DB gets the UNSUPPORTED sentinel.
        @SuppressWarnings("unchecked")
        Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> userDb = (Map<Map.Entry<Class<?>, Class<?>>, Convert<?>>) getField(converter, "USER_DB");
        userDb.put(pair(src, tgt), UNSUPPORTED_SENTINEL);
        // Static DB gets a real conversion.
        staticDb.put(pair(src, tgt), (Convert<?>) o -> null);
        assertEquals(expected, converter.isConversionSupportedFor(src, tgt), description);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("staticUnsupInheritedSupported")
    void testStaticUnsupportedInheritedSupported(Class<?> src, Class<?> tgt, boolean expected, String description) {
        // Static DB gets the UNSUPPORTED sentinel.
        staticDb.put(pair(src, tgt), UNSUPPORTED_SENTINEL);
        // No user entry – rely on inherited conversion (e.g., toString).
        assertEquals(expected, converter.isConversionSupportedFor(src, tgt), description);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("assignableInherited")
    void testInheritedViaAssignability(Class<?> src, Class<?> tgt, boolean expected, String description) {
        // No explicit mapping – the inherited logic should treat assignable types as supported.
        assertEquals(expected, converter.isConversionSupportedFor(src, tgt), description);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("primitiveToPrimitive")
    void testPrimitiveToDifferentPrimitive(Class<?> src, Class<?> tgt, boolean expected, String description) {
        // Ensure the static DB contains Integer→Long conversion.
        staticDb.putIfAbsent(pair(Integer.class, Long.class), (Convert<?>) o -> null);
        assertEquals(expected, converter.isConversionSupportedFor(src, tgt), description);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("identityConversion")
    void testIdentityConversionWhenNoExplicitEntry(Class<?> src, Class<?> tgt, boolean expected, String description) {
        // No entry is added; the method should treat identical classes as supported via inheritance.
        assertEquals(expected, converter.isConversionSupportedFor(src, tgt), description);
    }

    @Test
    void nullSourceThrowsNPE() {
        assertThrows(NullPointerException.class, () -> converter.isConversionSupportedFor(null, String.class));
    }

    @Test
    void nullTargetThrowsNPE() {
        assertThrows(NullPointerException.class, () -> converter.isConversionSupportedFor(String.class, null));
    }

    @Test
    void bothNullThrowsNPE() {
        assertThrows(NullPointerException.class, () -> converter.isConversionSupportedFor(null, null));
    }

    @Test
    void voidSourceThrowsIllegalArgument() {
        // ClassUtilities.toPrimitiveWrapperClass throws IllegalArgumentException for void.class
        assertThrows(IllegalArgumentException.class, () -> converter.isConversionSupportedFor(void.class, String.class));
    }

    @Test
    void voidTargetThrowsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> converter.isConversionSupportedFor(String.class, void.class));
    }
}
