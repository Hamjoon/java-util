package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import com.cedarsoftware.util.convert.Convert;
import com.cedarsoftware.util.convert.DefaultConverterOptions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.time.*;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class Converter_getSupportedConversions_2_Test {

    /**
     * Resets the private static final {@code instance} field of {@link Converter}
     * to a fresh {@code Converter} built with default options.  This is required
     * because {@code addConversion} mutates the singleton's internal state.
     */
    private static void resetConverterSingleton() {
        try {
            Field instanceField = Converter.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            // remove final modifier (works on most JVMs)
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(instanceField, instanceField.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
            // create a new Converter using the package‑private constructor
            Converter fresh = new Converter(new DefaultConverterOptions());
            instanceField.set(null, fresh);
        } catch (Exception e) {
            throw new RuntimeException("Failed to reset Converter singleton", e);
        }
    }

    /**
     * Verifies that {@link Converter#getSupportedConversions()} returns a non‑null,
     * non‑empty map and that a well‑known conversion (String → Integer) is present.
     */
    @Test
    void testGetSupportedConversionsReturnsNonEmptyMapContainingStringToInteger() {
        // when
        Map<String, Set<String>> supported = Converter.getSupportedConversions();
        // then
        assertNotNull(supported, "The map of supported conversions must not be null");
        assertFalse(supported.isEmpty(), "The map of supported conversions must not be empty");
        // Verify that a conversion from String to Integer is advertised
        Set<String> targets = supported.get(String.class.getName());
        assertNotNull(targets, "There should be a target‑set entry for the source type java.lang.String");
        // The target set should contain the fully‑qualified name of Integer
        assertTrue(targets.contains(Integer.class.getName()), "String should be convertible to Integer according to the supported conversions map");
    }

    @Test
    void testSupportedConversionsMapIsImmutable() {
        Map<String, Set<String>> map = Converter.getSupportedConversions();
        // The map itself must be unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> map.put("foo", Set.of("bar")));
        // Each inner set must also be unmodifiable
        map.values().forEach(set -> assertThrows(UnsupportedOperationException.class, () -> set.add("newTarget")));
    }

    @Test
    void testAddCustomConversionAppearsInSupportedMap() {
        try {
            Convert<LocalDateTime> conv = new Convert<LocalDateTime>() {

                @Override
                public LocalDateTime convert(Object o) {
                    return ((LocalDate) o).atStartOfDay();
                }
            };
            Converter.addConversion(LocalDate.class, LocalDateTime.class, conv);
            Map<String, Set<String>> map = Converter.getSupportedConversions();
            Set<String> targets = map.get(LocalDate.class.getName());
            assertNotNull(targets, "LocalDate entry must exist after adding conversion");
            assertTrue(targets.contains(LocalDateTime.class.getName()), "Custom conversion LocalDate -> LocalDateTime must be present");
        } finally {
            resetConverterSingleton();
        }
    }

    @Test
    void testAddConversionNullFunctionThrows() {
        Convert<?> dummy = (Convert<Object>) o -> o;
        assertThrows(IllegalArgumentException.class, () -> Converter.addConversion(LocalDate.class, LocalDateTime.class, null));
    }

    @Test
    void testAddConversionNullSourceThrows() {
        Convert<?> dummy = (Convert<Object>) o -> o;
        assertThrows(IllegalArgumentException.class, () -> Converter.addConversion(null, String.class, dummy));
    }

    @Test
    void testAddConversionNullTargetThrows() {
        Convert<?> dummy = (Convert<Object>) o -> o;
        assertThrows(IllegalArgumentException.class, () -> Converter.addConversion(Integer.class, null, dummy));
    }

    @Test
    void testMultipleCustomConversionsAccumulateTargets() {
        try {
            Convert<LocalDateTime> conv1 = o -> ((LocalDate) o).atStartOfDay();
            Convert<ZonedDateTime> conv2 = o -> ((LocalDate) o).atStartOfDay(ZoneId.systemDefault());
            Converter.addConversion(LocalDate.class, LocalDateTime.class, conv1);
            Converter.addConversion(LocalDate.class, ZonedDateTime.class, conv2);
            Map<String, Set<String>> map = Converter.getSupportedConversions();
            Set<String> targets = map.get(LocalDate.class.getName());
            assertNotNull(targets);
            assertTrue(targets.contains(LocalDateTime.class.getName()));
            assertTrue(targets.contains(ZonedDateTime.class.getName()));
        } finally {
            resetConverterSingleton();
        }
    }

    @Test
    void testCustomConversionAugmentsExistingEntry() {
        try {
            Convert<Long> conv = o -> Long.valueOf((String) o);
            Converter.addConversion(String.class, Long.class, conv);
            Map<String, Set<String>> map = Converter.getSupportedConversions();
            Set<String> targets = map.get(String.class.getName());
            assertNotNull(targets);
            assertTrue(targets.contains(Integer.class.getName()), "Built‑in conversion must remain");
            assertTrue(targets.contains(Long.class.getName()), "Custom conversion must be added");
        } finally {
            resetConverterSingleton();
        }
    }

    @Test
    void testImmutabilityAfterCustomAddition() {
        try {
            Convert<String> conv = o -> o.toString();
            Converter.addConversion(UUID.class, String.class, conv);
            Map<String, Set<String>> map = Converter.getSupportedConversions();
            // map immutability
            assertThrows(UnsupportedOperationException.class, () -> map.put("foo", Set.of("bar")));
            // inner set immutability
            map.values().forEach(set -> assertThrows(UnsupportedOperationException.class, () -> set.add("new")));
        } finally {
            resetConverterSingleton();
        }
    }

    @Test
    void testInnerSetImmutabilityExplicit() {
        Map<String, Set<String>> map = Converter.getSupportedConversions();
        for (Map.Entry<String, Set<String>> entry : map.entrySet()) {
            Set<String> targets = entry.getValue();
            assertThrows(UnsupportedOperationException.class, () -> targets.add("shouldFail"), "Set for source " + entry.getKey() + " must be immutable");
        }
    }

    @Test
    void testPrimitiveWrapperBidirectionalConversions() {
        Map<String, Set<String>> map = Converter.getSupportedConversions();
        // primitive int -> Integer
        Set<String> intTargets = map.get("int");
        assertNotNull(intTargets, "Map must contain entry for primitive int");
        assertTrue(intTargets.contains(Integer.class.getName()));
        // wrapper Integer -> int
        Set<String> integerTargets = map.get(Integer.class.getName());
        assertNotNull(integerTargets, "Map must contain entry for java.lang.Integer");
        assertTrue(integerTargets.contains("int"));
    }

    @Test
    void testCollectionConversionPresence() {
        Map<String, Set<String>> map = Converter.getSupportedConversions();
        Set<String> listTargets = map.get(List.class.getName());
        assertNotNull(listTargets, "List source entry must exist");
        assertTrue(listTargets.contains(Set.class.getName()), "List should be convertible to Set");
        assertTrue(listTargets.contains(Collection.class.getName()), "List should be convertible to Collection");
    }

    @Test
    void testMapReferenceStability() {
        Map<String, Set<String>> first = Converter.getSupportedConversions();
        Map<String, Set<String>> second = Converter.getSupportedConversions();
        // Content equality
        assertEquals(first, second, "Two successive calls must return equal content");
        // Distinct objects (fresh view)
        assertNotSame(first, second, "Two calls should return distinct map instances");
        // Modifying first should not affect second (modification throws, but we can test that
        // the second map still contains the expected entry)
        assertTrue(second.containsKey(String.class.getName()));
    }

    @Test
    void testNullInstanceMapThrowsNPE() throws Exception {
        // Save original instance to restore later
        Field instanceField = Converter.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        Object originalInstance = instanceField.get(null);
        try {
            // Set the singleton to null
            instanceField.set(null, null);
            assertThrows(NullPointerException.class, Converter::getSupportedConversions, "Calling getSupportedConversions with a null internal instance must throw NPE");
        } finally {
            // Restore original instance to avoid side‑effects for other tests
            instanceField.set(null, originalInstance);
        }
    }
}
