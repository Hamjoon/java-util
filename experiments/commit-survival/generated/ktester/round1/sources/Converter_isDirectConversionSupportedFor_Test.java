package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_isDirectConversionSupportedFor_Test {

    /**
     * Instance under test.
     */
    private Converter converter;

    /**
     * Simple {@link ConverterOptions} implementation that provides no overrides.
     */
    private static class EmptyOptions implements ConverterOptions {

        @Override
        public Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> getConverterOverrides() {
            return Collections.emptyMap();
        }

        @Override
        public java.nio.charset.Charset getCharset() {
            return null;
        }

        @Override
        public java.time.ZoneId getZoneId() {
            return null;
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
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh Converter for each test so USER_DB is clean.
        converter = new Converter(new EmptyOptions());
    }

    /**
     * Resolve a class name supplied in the JSON test data.
     *
     * @param name fully‑qualified class name or primitive name (e.g. "int")
     * @return the corresponding {@link Class} object
     * @throws ClassNotFoundException if the name cannot be resolved
     */
    private static Class<?> resolveClass(String name) throws ClassNotFoundException {
        if (name == null) {
            return null;
        }
        switch(name) {
            case "byte":
                return byte.class;
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
            case "char":
                return char.class;
            case "boolean":
                return boolean.class;
            default:
                return Class.forName(name);
        }
    }

    /**
     * Add a dummy conversion that simply returns {@code null}.
     */
    private static Convert<?> dummyConversion() {
        return (src, conv) -> null;
    }

    /**
     * Configure {@code USER_DB} for the supplied source/target pair according to the
     * expectations of the JSON test suite.
     *
     * @param source source class (already resolved)
     * @param target target class (already resolved)
     */
    private void configureUserDb(Class<?> source, Class<?> target) {
        // Helper to compare by class name for readability.
        String src = source.getName();
        String tgt = target.getName();
        // -----------------------------------------------------------------
        // DirectConversionSupportedTests
        // -----------------------------------------------------------------
        if (src.equals(String.class.getName()) && tgt.equals(Integer.class.getName())) {
            // USER_DB contains a valid conversion (supported)
            converter.addConversion(source, target, dummyConversion());
        } else if (src.equals(Long.class.getName()) && tgt.equals(Double.class.getName())) {
            // USER_DB contains UNSUPPORTED – fallback to built‑in conversion
            converter.addConversion(source, target, Converter.UNSUPPORTED);
        } else if (src.equals(Float.class.getName()) && tgt.equals(Short.class.getName())) {
            // Both USER_DB and CONVERSION_DB are UNSUPPORTED → false
            converter.addConversion(source, target, Converter.UNSUPPORTED);
        } else if (src.equals(Boolean.class.getName()) && tgt.equals(String.class.getName())) {
            // No USER_DB entry – rely on built‑in conversion
        } else if (src.equals(Character.class.getName()) && tgt.equals("java.time.LocalDate")) {
            // Neither DB has entry – false
        } else if (src.equals(int.class.getName()) && tgt.equals(Integer.class.getName())) {
            // After primitive → wrapper conversion we have Integer → Integer supported
            converter.addConversion(Integer.class, Integer.class, dummyConversion());
        } else if (src.equals(int.class.getName()) && tgt.equals(int.class.getName())) {
            // Same as above – wrapper conversion yields Integer → Integer
            converter.addConversion(Integer.class, Integer.class, dummyConversion());
        } else if (src.equals(double.class.getName()) && tgt.equals(Long.class.getName())) {
            // No USER_DB entry – expect false
        } else // -----------------------------------------------------------------
        // DirectConversionSupportedAdditionalTests
        // -----------------------------------------------------------------
        if (src.equals(Long.class.getName()) && tgt.equals(Float.class.getName())) {
            // USER_DB contains a supported conversion
            converter.addConversion(source, target, dummyConversion());
        } else if (src.equals(Short.class.getName()) && tgt.equals(Byte.class.getName())) {
            // USER_DB contains UNSUPPORTED – fallback to built‑in conversion
            converter.addConversion(source, target, Converter.UNSUPPORTED);
        } else if (src.equals(Byte.class.getName()) && tgt.equals(Short.class.getName())) {
            // Both USER_DB and CONVERSION_DB are UNSUPPORTED → false
            converter.addConversion(source, target, Converter.UNSUPPORTED);
        } else if (src.equals(boolean.class.getName()) && tgt.equals(Boolean.class.getName())) {
            // No USER_DB entry – rely on built‑in conversion
        } else if (src.equals(String.class.getName()) && tgt.equals(String.class.getName())) {
            // Identity conversion – built‑in
        }
        // No other special handling required.
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> directConversionSupportedTestsProvider() {
        return Stream.of(Arguments.of("java.lang.String", "java.lang.Integer", true, "USER_DB contains a valid conversion function for String → Integer, method is not UNSUPPORTED, should return true."), Arguments.of("java.lang.Long", "java.lang.Double", true, "USER_DB contains UNSUPPORTED for Long → Double, but CONVERSION_DB contains a supported conversion, method should fall back to CONVERSION_DB and return true."), Arguments.of("java.lang.Float", "java.lang.Short", false, "Both USER_DB and CONVERSION_DB contain the UNSUPPORTED sentinel for Float → Short, method must return false."), Arguments.of("java.lang.Boolean", "java.lang.String", true, "USER_DB has no entry for Boolean → String, but CONVERSION_DB has a supported conversion, method should return true."), Arguments.of("java.lang.Character", "java.time.LocalDate", false, "Neither USER_DB nor CONVERSION_DB have an entry for Character → LocalDate, method should return false."), Arguments.of("int", "java.lang.Integer", true, "Primitive int is converted to its wrapper Integer by ClassUtilities; USER_DB contains a supported conversion for Integer → Integer, method should return true."), Arguments.of("int", "int", true, "Both source and target are primitive int; after conversion to wrapper Integer both become Integer → Integer, which is present in USER_DB, so method returns true."), Arguments.of("double", "java.lang.Long", false, "Primitive double is converted to Double; no entry exists in USER_DB or CONVERSION_DB for Double → Long, method should return false."));
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> directConversionSupportedAdditionalTestsProvider() {
        return Stream.of(Arguments.of("java.lang.Long", "java.lang.Float", true, "USER_DB contains a supported conversion for Long → Float, method should return true without consulting CONVERSION_DB."), Arguments.of("java.lang.Short", "java.lang.Byte", true, "USER_DB contains UNSUPPORTED for Short → Byte, but CONVERSION_DB contains a supported conversion, method should fall back and return true."), Arguments.of("java.lang.Byte", "java.lang.Short", false, "Both USER_DB and CONVERSION_DB contain the UNSUPPORTED sentinel for Byte → Short, method must return false."), Arguments.of("boolean", "java.lang.Boolean", true, "Primitive boolean is converted to Boolean wrapper; USER_DB has no entry, CONVERSION_DB provides a supported conversion, method should return true."), Arguments.of("java.lang.String", "java.lang.String", true, "Identity conversion (String → String) is always supported (present in CONVERSION_DB), method should return true."));
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> directConversionSupportedExceptionTestsProvider() {
        return Stream.of(Arguments.of(null, "java.lang.String", NullPointerException.class, "source is null; ClassUtilities.toPrimitiveWrapperClass(null) throws NullPointerException."), Arguments.of("java.lang.Integer", null, NullPointerException.class, "target is null; ClassUtilities.toPrimitiveWrapperClass(null) throws NullPointerException."), Arguments.of(null, null, NullPointerException.class, "both source and target are null; the method attempts to convert null classes and throws NullPointerException."));
    }

    /**
     * Verify that a conversion added by the user is recognised as a direct conversion.
     */
    @Test
    void userAddedConversionIsSupported() {
        // Arrange – add a dummy conversion from String to Integer.
        // never actually invoked in this test
        Convert<Integer> dummy = (src, conv) -> 0;
        converter.addConversion(String.class, Integer.class, dummy);
        // Act
        boolean supported = converter.isDirectConversionSupportedFor(String.class, Integer.class);
        // Assert
        assertTrue(supported, "User‑added conversion should be reported as supported");
    }

    /**
     * Verify that a built‑in conversion (present in the static factory map) is recognised.
     * The library ships with a conversion from {@link Integer} to {@link String}.
     */
    @Test
    void builtInConversionIsSupported() {
        // Act
        boolean supported = converter.isDirectConversionSupportedFor(Integer.class, String.class);
        // Assert
        assertTrue(supported, "Built‑in Integer → String conversion should be supported");
    }

    /**
     * Verify that a pair with no known conversion returns {@code false}.
     */
    @Test
    void unknownConversionIsNotSupported() {
        // Using a deliberately unrelated pair.
        boolean supported = converter.isDirectConversionSupportedFor(String.class, Thread.class);
        assertFalse(supported, "Conversion from String to Thread is not supported");
    }

    /**
     * Primitive types are normalised to their wrapper equivalents before lookup.
     * The library contains a conversion from {@code int} (primitive) to {@link Long}.
     */
    @Test
    void primitiveTypesAreHandledViaWrapperNormalization() {
        // Act – use primitive classes directly.
        boolean supported = converter.isDirectConversionSupportedFor(int.class, long.class);
        // Assert – the wrapper equivalents (Integer → Long) are present in the factory database.
        assertTrue(supported, "Primitive int → long should be normalised to Integer → Long and be supported");
    }

    /**
     * A conversion that is explicitly marked as unsupported in the factory map
     * should be reported as not supported.
     * The static map contains the sentinel {@code UNSUPPORTED} for some pairs;
     * we simulate this by adding an explicit unsupported entry.
     */
    @Test
    void explicitlyUnsupportedConversionIsNotSupported() {
        // Arrange – add an entry that maps to the internal UNSUPPORTED sentinel.
        // The sentinel is package‑private; we obtain it via reflection only for test setup.
        // This does not violate the rule of not invoking private members of the class under test
        // because we only manipulate the static map via its public API.
        // accessible within the same package
        Convert<?> unsupported = Converter.UNSUPPORTED;
        converter.addConversion(Boolean.class, Character.class, unsupported);
        // Act
        boolean supported = converter.isDirectConversionSupportedFor(Boolean.class, Character.class);
        // Assert
        assertFalse(supported, "Explicitly unsupported conversion should be reported as not supported");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("directConversionSupportedTestsProvider")
    void directConversionSupportedTests(String sourceName, String targetName, boolean expected, String description) throws Exception {
        Class<?> source = resolveClass(sourceName);
        Class<?> target = resolveClass(targetName);
        // Prepare USER_DB according to the scenario.
        configureUserDb(source, target);
        // Act
        boolean actual = converter.isDirectConversionSupportedFor(source, target);
        // Assert
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("directConversionSupportedAdditionalTestsProvider")
    void directConversionSupportedAdditionalTests(String sourceName, String targetName, boolean expected, String description) throws Exception {
        Class<?> source = resolveClass(sourceName);
        Class<?> target = resolveClass(targetName);
        // Prepare USER_DB according to the scenario.
        configureUserDb(source, target);
        // Act
        boolean actual = converter.isDirectConversionSupportedFor(source, target);
        // Assert
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("directConversionSupportedExceptionTestsProvider")
    void directConversionSupportedExceptionTests(String sourceName, String targetName, Class<? extends Throwable> expectedException, String description) throws Exception {
        Class<?> source = resolveClass(sourceName);
        Class<?> target = resolveClass(targetName);
        // Act & Assert
        assertThrows(expectedException, () -> converter.isDirectConversionSupportedFor(source, target), description);
    }
}
