package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class Converter_getSupportedConversions_2_Test {

    private static Stream<Arguments> positiveConversionProvider() {
        return Stream.of(arguments(Integer.class.getName(), String.class.getName(), "Integer → String conversion is built‑in"), arguments(java.time.LocalDate.class.getName(), java.time.LocalDateTime.class.getName(), "LocalDate → LocalDateTime conversion is built‑in"), arguments(java.util.Date.class.getName(), java.time.Instant.class.getName(), "Date → Instant conversion is built‑in"), arguments(Boolean.class.getName(), String.class.getName(), "Boolean → String conversion is built‑in"));
    }

    /**
     * Verify that the static {@code getSupportedConversions()} method returns a non‑null,
     * non‑empty map. The map keys are source class names and the values are the set of
     * target class names that can be converted to.
     */
    @Test
    void testGetSupportedConversionsNotNullAndNotEmpty() {
        Map<String, Set<String>> conversions = Converter.getSupportedConversions();
        assertNotNull(conversions, "The returned map must not be null");
        assertFalse(conversions.isEmpty(), "The map should contain at least one conversion entry");
        // Spot‑check a few well‑known conversions to increase confidence.
        // These class names are part of the documented conversion set.
        assertTrue(conversions.containsKey(String.class.getName()), "Conversions should include String as a source type");
        assertTrue(conversions.containsKey(Integer.class.getName()), "Conversions should include Integer as a source type");
        // Ensure that a typical target (e.g., Long) appears for at least one source.
        boolean containsLongTarget = conversions.values().stream().anyMatch(set -> set.contains(Long.class.getName()));
        assertTrue(containsLongTarget, "At least one source should be convertible to Long");
    }

    /**
     * Verify that repeated invocations of the method return maps that are equal
     * (i.e., the conversion definitions are stable) and that the returned map
     * is unmodifiable – attempts to modify it should throw an {@link UnsupportedOperationException}.
     */
    @Test
    void testGetSupportedConversionsConsistentAcrossCalls() {
        Map<String, Set<String>> firstCall = Converter.getSupportedConversions();
        Map<String, Set<String>> secondCall = Converter.getSupportedConversions();
        assertEquals(firstCall, secondCall, "Two successive calls should return equal maps");
        // The map returned by the library is expected to be immutable.
        // Attempting to modify it should result in an exception.
        assertThrows(UnsupportedOperationException.class, () -> firstCall.put("dummy", Set.of()));
    }

    /**
     * DefaultSupportedConversions – case 1:
     * Verify that the built‑in conversion table contains the mapping
     * {@code java.lang.String → java.lang.Integer}.
     */
    @Test
    void testDefaultSupportedConversions_containsStringToInteger() {
        Map<String, Set<String>> map = Converter.getSupportedConversions();
        String source = String.class.getName();
        String target = Integer.class.getName();
        assertTrue(map.containsKey(source), "Default map must contain the source type String");
        assertNotNull(map.get(source), "Target set for String must not be null");
        assertTrue(map.get(source).contains(target), "String → Integer conversion should be present in the default map");
    }

    /**
     * DefaultSupportedConversions – case 2:
     * Ensure that every key and every value in the map are non‑null,
     * non‑empty fully‑qualified class names and that no target set is empty.
     */
    @Test
    void testDefaultSupportedConversions_structureValidity() {
        Map<String, Set<String>> map = Converter.getSupportedConversions();
        assertAll("Map structure checks", () -> assertNotNull(map, "Map itself must not be null"), () -> assertFalse(map.isEmpty(), "Map must contain at least one entry"), () -> map.forEach((key, valueSet) -> {
            assertNotNull(key, "Source class name must not be null");
            assertFalse(key.isBlank(), "Source class name must not be blank");
            // simple sanity check that it looks like a fully‑qualified name
            assertTrue(key.contains("."), "Source class name should be fully qualified: " + key);
            assertNotNull(valueSet, "Target set for source " + key + " must not be null");
            assertFalse(valueSet.isEmpty(), "Target set for source " + key + " must contain at least one entry");
            valueSet.forEach(target -> {
                assertNotNull(target, "Target class name must not be null (source: " + key + ")");
                assertFalse(target.isBlank(), "Target class name must not be blank (source: " + key + ")");
                assertTrue(target.contains("."), "Target class name should be fully qualified: " + target);
            });
        }));
    }

    /**
     * SupportedConversionsContent – positive cases.
     * Each argument is a triple: source class name, expected target class name,
     * and a description used for the test name.
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("positiveConversionProvider")
    void testSupportedConversionsContent_positive(String sourceClass, String targetClass, String description) {
        Map<String, Set<String>> map = Converter.getSupportedConversions();
        assertTrue(map.containsKey(sourceClass), "Map should contain source type " + sourceClass);
        Set<String> targets = map.get(sourceClass);
        assertNotNull(targets, "Target set for " + sourceClass + " must not be null");
        assertTrue(targets.contains(targetClass), description + " – expected conversion not found");
    }

    /**
     * SupportedConversionsContent – negative case.
     * Verify that an unsupported conversion (String → Void) is **not** present.
     */
    @Test
    void testSupportedConversionsContent_negativeStringToVoidNotPresent() {
        Map<String, Set<String>> map = Converter.getSupportedConversions();
        String source = String.class.getName();
        String unwantedTarget = Void.class.getName();
        // If the source key is missing altogether, the test still passes because the conversion is absent.
        Set<String> targets = map.getOrDefault(source, Set.of());
        assertFalse(targets.contains(unwantedTarget), "String → Void conversion must NOT be listed in the supported conversions");
    }
}
