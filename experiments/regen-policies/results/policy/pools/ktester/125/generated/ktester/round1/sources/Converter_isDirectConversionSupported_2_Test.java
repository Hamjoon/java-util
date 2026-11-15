package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_isDirectConversionSupported_2_Test {

    private static Stream<Arguments> supportedDirectConversions() {
        return Stream.of(// Identical types
        Arguments.of(Integer.class, Integer.class), // Primitive ↔ Wrapper
        Arguments.of(int.class, Integer.class), Arguments.of(Integer.class, int.class), // BigDecimal → BigInteger
        Arguments.of(BigDecimal.class, BigInteger.class), // LocalDate → LocalDateTime
        Arguments.of(LocalDate.class, LocalDateTime.class), // Date → Timestamp
        Arguments.of(Date.class, java.sql.Timestamp.class), // String → Character (single‑char strings)
        Arguments.of(String.class, Character.class), // UUID → String
        Arguments.of(UUID.class, String.class));
    }

    private static Stream<Arguments> primitiveWrapperDirectConversions() {
        return Stream.of(// boolean ↔ Boolean
        Arguments.of(boolean.class, Boolean.class), Arguments.of(Boolean.class, boolean.class), // char ↔ Character
        Arguments.of(char.class, Character.class), Arguments.of(Character.class, char.class), // double ↔ Double
        Arguments.of(double.class, Double.class), Arguments.of(Double.class, double.class), // float ↔ Float
        Arguments.of(float.class, Float.class), Arguments.of(Float.class, float.class), // long ↔ Long
        Arguments.of(long.class, Long.class), Arguments.of(Long.class, long.class), // short ↔ Short
        Arguments.of(short.class, Short.class), Arguments.of(Short.class, short.class), // byte ↔ Byte
        Arguments.of(byte.class, Byte.class), Arguments.of(Byte.class, byte.class));
    }

    private static Stream<Arguments> unsupportedConversions() {
        return Stream.of(Arguments.of(Integer.class, Long.class), Arguments.of(int.class, Long.class), Arguments.of(Long.class, Integer.class), Arguments.of(Double.class, Integer.class), Arguments.of(String.class, Integer.class), Arguments.of(Date.class, LocalDateTime.class), Arguments.of(LocalDateTime.class, Date.class), Arguments.of(UUID.class, Date.class), Arguments.of(java.nio.CharBuffer.class, String.class), Arguments.of(java.nio.ByteBuffer.class, byte[].class), Arguments.of(java.util.concurrent.atomic.AtomicInteger.class, Integer.class));
    }

    private static Stream<Arguments> nullInputCases() {
        return Stream.of(Arguments.of(null, String.class), Arguments.of(String.class, null), Arguments.of(null, null));
    }

    private static Stream<Arguments> invalidClassCases() {
        return Stream.of(// void as source
        Arguments.of(void.class, String.class), // void as target
        Arguments.of(String.class, void.class), // primitive array as source
        Arguments.of(int[].class, Integer.class), // object array ↔ primitive array
        Arguments.of(Integer[].class, int[].class));
    }

    /**
     * Verify that every conversion reported by {@link Converter#getSupportedConversions()}
     * is recognised as a direct conversion by {@link Converter#isDirectConversionSupported(Class, Class)}.
     */
    @Test
    void testAllSupportedConversionsAreDirect() {
        Map<String, Set<String>> supported = Converter.getSupportedConversions();
        assertNotNull(supported, "Supported conversions map must not be null");
        assertFalse(supported.isEmpty(), "Supported conversions map must contain entries");
        for (Map.Entry<String, Set<String>> entry : supported.entrySet()) {
            String sourceName = entry.getKey();
            Set<String> targetNames = entry.getValue();
            assertNotNull(sourceName, "Source class name must not be null");
            assertNotNull(targetNames, "Target class set must not be null");
            assertFalse(targetNames.isEmpty(), "Target class set for " + sourceName + " must not be empty");
            Class<?> sourceClass;
            try {
                sourceClass = Class.forName(sourceName);
            } catch (ClassNotFoundException e) {
                fail("Source class not found: " + sourceName, e);
                // unreachable, but keeps compiler happy
                return;
            }
            for (String targetName : targetNames) {
                Class<?> targetClass;
                try {
                    targetClass = Class.forName(targetName);
                } catch (ClassNotFoundException e) {
                    fail("Target class not found: " + targetName, e);
                    continue;
                }
                boolean direct = Converter.isDirectConversionSupported(sourceClass, targetClass);
                assertTrue(direct, () -> String.format("Expected direct conversion support from %s to %s", sourceClass.getName(), targetClass.getName()));
            }
        }
    }

    /**
     * Verify that a conversion which is not listed in the supported conversions map
     * returns {@code false}.
     */
    @Test
    void testUnsupportedConversionReturnsFalse() {
        // Choose a pair that is highly unlikely to be supported
        Class<?> source = Object.class;
        Class<?> target = Thread.class;
        // Sanity check that the pair is indeed not present in the supported map
        Map<String, Set<String>> supported = Converter.getSupportedConversions();
        boolean listed = supported.getOrDefault(Object.class.getName(), Set.of()).contains(Thread.class.getName());
        assertFalse(listed, "The test pair should not be listed as a supported conversion");
        boolean result = Converter.isDirectConversionSupported(source, target);
        assertFalse(result, () -> String.format("Expected no direct conversion support from %s to %s", source.getName(), target.getName()));
    }

    /**
     * Simple sanity checks for a few well‑known conversions.
     */
    @Test
    void testKnownConversions() {
        assertTrue(Converter.isDirectConversionSupported(String.class, Integer.class), "String → Integer should be a direct conversion");
        assertTrue(Converter.isDirectConversionSupported(int.class, Integer.class), "int → Integer should be a direct conversion");
        assertTrue(Converter.isDirectConversionSupported(Integer.class, int.class), "Integer → int should be a direct conversion");
        assertTrue(Converter.isDirectConversionSupported(Date.class, java.time.Instant.class), "Date → Instant should be a direct conversion");
        assertTrue(Converter.isDirectConversionSupported(java.time.Instant.class, Date.class), "Instant → Date should be a direct conversion");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0} → {1} should be direct")
    @MethodSource("supportedDirectConversions")
    void testSupportedDirectConversions(Class<?> source, Class<?> target) {
        assertTrue(Converter.isDirectConversionSupported(source, target), () -> String.format("Expected direct conversion from %s to %s", source, target));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0} ↔ {1} primitive/wrapper direct")
    @MethodSource("primitiveWrapperDirectConversions")
    void testPrimitiveWrapperDirectConversions(Class<?> source, Class<?> target) {
        assertTrue(Converter.isDirectConversionSupported(source, target), () -> String.format("Expected direct conversion between %s and %s", source, target));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0} → {1} should NOT be direct")
    @MethodSource("unsupportedConversions")
    void testUnsupportedConversions(Class<?> source, Class<?> target) {
        assertFalse(Converter.isDirectConversionSupported(source, target), () -> String.format("Did not expect direct conversion from %s to %s", source, target));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] null handling: source={0}, target={1}")
    @MethodSource("nullInputCases")
    void testNullInputHandling(Class<?> source, Class<?> target) {
        assertFalse(Converter.isDirectConversionSupported(source, target), () -> String.format("Expected false when source=%s, target=%s", source, target));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] invalid types: source={0}, target={1}")
    @MethodSource("invalidClassCases")
    void testInvalidClassTypes(Class<?> source, Class<?> target) {
        assertThrows(IllegalArgumentException.class, () -> Converter.isDirectConversionSupported(source, target), () -> String.format("Expected IllegalArgumentException for source=%s, target=%s", source, target));
    }
}
