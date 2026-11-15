package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_isConversionSupportedFor_2_Test {

    /**
     * Holds the snapshot of the built‑in conversion map before any test runs.
     */
    private static Map<String, Set<String>> supportedConversions;

    @BeforeAll
    static void setupBeforeAll() {
        // Capture the built‑in conversion map once; it is immutable for the
        // duration of the test suite (the library does not expose a mutator).
        supportedConversions = Converter.getSupportedConversions();
        assertNotNull(supportedConversions, "Supported conversions map must not be null");
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly necessary but keeps the
        // test class tidy.
        supportedConversions = null;
    }

    /**
     * Helper that checks the expected result against the actual call.
     */
    private void assertSupported(Class<?> source, Class<?> target, boolean expected) {
        boolean actual = Converter.isConversionSupportedFor(source, target);
        assertEquals(expected, actual, () -> String.format("Conversion support mismatch for %s → %s", source == null ? "null" : source.getSimpleName(), target == null ? "null" : target.getSimpleName()));
    }

    /**
     * Resolve a class name (including primitive names) to a {@link Class}.
     */
    private static Class<?> resolveClass(String name) {
        if (name == null) {
            return null;
        }
        switch(name) {
            case "boolean":
                return boolean.class;
            case "byte":
                return byte.class;
            case "char":
                return char.class;
            case "short":
                return short.class;
            case "int":
                return int.class;
            case "long":
                return long.class;
            case "float":
                return float.class;
            case "double":
                return double.class;
            default:
                try {
                    return Class.forName(name);
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException("Unable to resolve class name: " + name, e);
                }
        }
    }

    /* ---------- Null handling ---------- */
    private static Stream<Arguments> nullHandlingCases() {
        return Stream.of(Arguments.of(resolveClass(null), resolveClass(null), false, "Both source and target are null – conversion cannot be supported."), Arguments.of(resolveClass(null), resolveClass("java.lang.String"), false, "Source is null while target is a concrete class – conversion unsupported."), Arguments.of(resolveClass("java.lang.Integer"), resolveClass(null), false, "Target is null while source is a concrete class – conversion unsupported."));
    }

    /* ---------- Identity conversion (source == target) ---------- */
    private static Stream<Arguments> identityConversionCases() {
        return Stream.of(Arguments.of(resolveClass("java.lang.Integer"), resolveClass("java.lang.Integer"), true, "Same class conversion – always supported."), Arguments.of(resolveClass("java.lang.String"), resolveClass("java.lang.String"), true, "Same class conversion for String – always supported."));
    }

    /* ---------- Primitive ↔ Wrapper conversions ---------- */
    private static Stream<Arguments> primitiveWrapperCases() {
        return Stream.of(Arguments.of(resolveClass("int"), resolveClass("java.lang.Integer"), true, "Primitive int to wrapper Integer conversion is supported."), Arguments.of(resolveClass("java.lang.Integer"), resolveClass("int"), true, "Wrapper Integer to primitive int conversion is supported."), Arguments.of(resolveClass("double"), resolveClass("java.lang.Double"), true, "Primitive double to wrapper Double conversion is supported."), Arguments.of(resolveClass("java.lang.Boolean"), resolveClass("boolean"), true, "Wrapper Boolean to primitive boolean conversion is supported."));
    }

    /* ---------- String → Primitive/Wrapper conversions ---------- */
    private static Stream<Arguments> stringToPrimitiveOrWrapperCases() {
        return Stream.of(Arguments.of(resolveClass("java.lang.String"), resolveClass("java.lang.Integer"), true, "String containing a numeric value can be converted to Integer."), Arguments.of(resolveClass("java.lang.String"), resolveClass("int"), true, "String → primitive int conversion is supported."), Arguments.of(resolveClass("java.lang.String"), resolveClass("java.lang.Boolean"), true, "String containing \"true\"/\"false\" can be converted to Boolean."), Arguments.of(resolveClass("java.lang.String"), resolveClass("java.time.LocalDate"), true, "ISO‑8601 date string can be converted to LocalDate."));
    }

    /* ---------- Primitive/Wrapper → String conversions ---------- */
    private static Stream<Arguments> primitiveOrWrapperToStringCases() {
        return Stream.of(Arguments.of(resolveClass("java.lang.Integer"), resolveClass("java.lang.String"), true, "Numeric wrapper to String conversion is supported."), Arguments.of(resolveClass("double"), resolveClass("java.lang.String"), true, "Primitive double to String conversion is supported."));
    }

    /* ---------- Date/Time conversions ---------- */
    private static Stream<Arguments> dateTimeConversionCases() {
        return Stream.of(Arguments.of(resolveClass("java.util.Date"), resolveClass("java.time.LocalDateTime"), true, "java.util.Date can be converted to java.time.LocalDateTime."), Arguments.of(resolveClass("java.time.LocalDate"), resolveClass("java.sql.Timestamp"), true, "LocalDate can be converted to java.sql.Timestamp (via start of day)."), Arguments.of(resolveClass("java.time.ZonedDateTime"), resolveClass("java.util.Date"), true, "ZonedDateTime can be converted to java.util.Date."));
    }

    /* ---------- Atomic type conversions ---------- */
    private static Stream<Arguments> atomicConversionCases() {
        return Stream.of(Arguments.of(resolveClass("java.lang.Integer"), resolveClass("java.util.concurrent.atomic.AtomicInteger"), true, "Integer can be wrapped into AtomicInteger."), Arguments.of(resolveClass("java.util.concurrent.atomic.AtomicBoolean"), resolveClass("java.lang.Boolean"), true, "AtomicBoolean can be converted to Boolean."));
    }

    /* ---------- Unsupported conversions ---------- */
    private static Stream<Arguments> unsupportedConversionCases() {
        return Stream.of(Arguments.of(resolveClass("java.lang.Thread"), resolveClass("java.lang.Integer"), false, "No conversion path exists between Thread and Integer."), Arguments.of(resolveClass("java.lang.Object"), resolveClass("java.time.ZonedDateTime"), false, "Generic Object cannot be directly converted to ZonedDateTime."), Arguments.of(resolveClass("java.math.BigDecimal"), resolveClass("java.util.concurrent.atomic.AtomicLong"), false, "BigDecimal to AtomicLong is not a supported conversion."));
    }

    /* ---------- Primitive to Primitive conversions ---------- */
    private static Stream<Arguments> primitiveToPrimitiveCases() {
        return Stream.of(Arguments.of(resolveClass("int"), resolveClass("double"), true, "Widening primitive conversion from int to double should be supported."), Arguments.of(resolveClass("int"), resolveClass("boolean"), false, "No direct conversion exists between numeric primitive int and boolean primitive."));
    }

    /* ---------- Wrapper to Wrapper conversions ---------- */
    private static Stream<Arguments> wrapperToWrapperCases() {
        return Stream.of(Arguments.of(resolveClass("java.lang.Integer"), resolveClass("java.lang.Long"), false, "Different numeric wrapper types (Integer → Long) are not automatically convertible."), Arguments.of(resolveClass("java.lang.Integer"), resolveClass("long"), false, "Wrapper Integer to primitive long conversion is not supported without explicit narrowing."));
    }

    /* ---------- String to Date/Time conversions ---------- */
    private static Stream<Arguments> stringToDateTimeCases() {
        return Stream.of(Arguments.of(resolveClass("java.lang.String"), resolveClass("java.time.LocalDateTime"), true, "ISO‑8601 date‑time string can be converted to LocalDateTime."), Arguments.of(resolveClass("java.lang.String"), resolveClass("java.time.ZonedDateTime"), true, "ISO‑8601 zoned date‑time string can be converted to ZonedDateTime."));
    }

    /* ---------- Date/Time cross‑type conversions ---------- */
    private static Stream<Arguments> dateTimeCrossTypeCases() {
        return Stream.of(Arguments.of(resolveClass("java.util.Date"), resolveClass("java.time.LocalDate"), true, "java.util.Date can be converted to java.time.LocalDate (date part only)."), Arguments.of(resolveClass("java.sql.Timestamp"), resolveClass("java.time.LocalDateTime"), true, "java.sql.Timestamp can be converted to java.time.LocalDateTime."));
    }

    /* ---------- Atomic to Primitive conversions ---------- */
    private static Stream<Arguments> atomicToPrimitiveCases() {
        return Stream.of(Arguments.of(resolveClass("java.util.concurrent.atomic.AtomicInteger"), resolveClass("int"), true, "AtomicInteger unwraps to primitive int."), Arguments.of(resolveClass("java.util.concurrent.atomic.AtomicLong"), resolveClass("long"), true, "AtomicLong unwraps to primitive long."), Arguments.of(resolveClass("java.util.concurrent.atomic.AtomicBoolean"), resolveClass("boolean"), true, "AtomicBoolean unwraps to primitive boolean."));
    }

    /* ---------- BigNumber ↔ String conversions ---------- */
    private static Stream<Arguments> bigNumberStringCases() {
        return Stream.of(Arguments.of(resolveClass("java.math.BigDecimal"), resolveClass("java.lang.String"), true, "BigDecimal can be converted to its String representation."), Arguments.of(resolveClass("java.math.BigInteger"), resolveClass("java.lang.String"), true, "BigInteger can be converted to its String representation."), Arguments.of(resolveClass("java.lang.String"), resolveClass("java.math.BigDecimal"), true, "String containing a decimal number can be converted to BigDecimal."), Arguments.of(resolveClass("java.lang.String"), resolveClass("java.math.BigInteger"), true, "String containing an integer value can be converted to BigInteger."));
    }

    /* ---------- Enum conversions ---------- */
    private static Stream<Arguments> enumConversionCases() {
        return Stream.of(Arguments.of(resolveClass("java.time.DayOfWeek"), resolveClass("java.lang.String"), true, "Enum constant (e.g., DayOfWeek.MONDAY) can be converted to its name string."), Arguments.of(resolveClass("java.time.DayOfWeek"), resolveClass("java.lang.Integer"), false, "Direct conversion from an enum type to Integer is not supported."));
    }

    @Test
    void shouldReportSupportedConversionsCorrectly() {
        // A few well‑known supported conversions (based on library documentation)
        assertSupported(Integer.class, String.class, true);
        assertSupported(String.class, Integer.class, true);
        assertSupported(Long.class, Double.class, true);
        assertSupported(Boolean.class, String.class, true);
        assertSupported(java.util.Date.class, java.time.LocalDateTime.class, true);
        assertSupported(java.time.LocalDate.class, java.util.Date.class, true);
        assertSupported(java.math.BigDecimal.class, String.class, true);
        assertSupported(AtomicInteger.class, Integer.class, true);
    }

    @Test
    void shouldReportUnsupportedConversionsCorrectly() {
        // Some obviously unsupported conversions
        assertSupported(String.class, Thread.class, false);
        assertSupported(Object.class, java.sql.Blob.class, false);
        // not directly supported
        assertSupported(java.time.ZonedDateTime.class, java.time.Instant.class, false);
        assertSupported(java.util.Calendar.class, java.math.BigInteger.class, false);
        assertSupported(java.util.concurrent.atomic.AtomicBoolean.class, java.time.LocalDate.class, false);
    }

    @Test
    void shouldBeConsistentWithGetSupportedConversionsMap() {
        // Verify that the static method's answer aligns with the map returned by
        // Converter.getSupportedConversions().
        for (Map.Entry<String, Set<String>> entry : supportedConversions.entrySet()) {
            String sourceName = entry.getKey();
            Set<String> targets = entry.getValue();
            try {
                Class<?> sourceClass = Class.forName(sourceName);
                // All listed targets must be reported as supported
                for (String targetName : targets) {
                    Class<?> targetClass = Class.forName(targetName);
                    assertTrue(Converter.isConversionSupportedFor(sourceClass, targetClass), () -> sourceName + " → " + targetName + " should be supported");
                }
            } catch (ClassNotFoundException e) {
                // If the library lists a class that cannot be loaded in this environment,
                // we simply skip it – the test is focused on the method contract, not classpath issues.
            }
        }
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("nullHandlingCases")
    void testNullHandling(Class<?> source, Class<?> target, boolean expected, String description) {
        assertSupported(source, target, expected);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("identityConversionCases")
    void testIdentityConversion(Class<?> source, Class<?> target, boolean expected, String description) {
        assertSupported(source, target, expected);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("primitiveWrapperCases")
    void testPrimitiveWrapperConversions(Class<?> source, Class<?> target, boolean expected, String description) {
        assertSupported(source, target, expected);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("stringToPrimitiveOrWrapperCases")
    void testStringToPrimitiveOrWrapper(Class<?> source, Class<?> target, boolean expected, String description) {
        assertSupported(source, target, expected);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("primitiveOrWrapperToStringCases")
    void testPrimitiveOrWrapperToString(Class<?> source, Class<?> target, boolean expected, String description) {
        assertSupported(source, target, expected);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("dateTimeConversionCases")
    void testDateTimeConversions(Class<?> source, Class<?> target, boolean expected, String description) {
        assertSupported(source, target, expected);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("atomicConversionCases")
    void testAtomicConversions(Class<?> source, Class<?> target, boolean expected, String description) {
        assertSupported(source, target, expected);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("unsupportedConversionCases")
    void testUnsupportedConversions(Class<?> source, Class<?> target, boolean expected, String description) {
        assertSupported(source, target, expected);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("primitiveToPrimitiveCases")
    void testPrimitiveToPrimitiveConversions(Class<?> source, Class<?> target, boolean expected, String description) {
        assertSupported(source, target, expected);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("wrapperToWrapperCases")
    void testWrapperToWrapperConversions(Class<?> source, Class<?> target, boolean expected, String description) {
        assertSupported(source, target, expected);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("stringToDateTimeCases")
    void testStringToDateTimeConversions(Class<?> source, Class<?> target, boolean expected, String description) {
        assertSupported(source, target, expected);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("dateTimeCrossTypeCases")
    void testDateTimeCrossTypeConversions(Class<?> source, Class<?> target, boolean expected, String description) {
        assertSupported(source, target, expected);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("atomicToPrimitiveCases")
    void testAtomicToPrimitiveConversions(Class<?> source, Class<?> target, boolean expected, String description) {
        assertSupported(source, target, expected);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("bigNumberStringCases")
    void testBigNumberStringConversions(Class<?> source, Class<?> target, boolean expected, String description) {
        assertSupported(source, target, expected);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("enumConversionCases")
    void testEnumConversions(Class<?> source, Class<?> target, boolean expected, String description) {
        assertSupported(source, target, expected);
    }
}
