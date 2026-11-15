package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import com.cedarsoftware.util.convert.Convert;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

@ExtendWith(MockitoExtension.class)
class Converter_allSupportedConversions_2_Test {

    /**
     * Verify that {@link Converter#allSupportedConversions()} returns a non‑null,
     * non‑empty map and that it contains a few expected source‑to‑target conversion entries.
     */
    @Test
    void shouldReturnAllSupportedConversionsContainingKnownMappings() {
        // GIVEN – nothing to arrange; the method is static and self‑contained.
        // WHEN
        Map<Class<?>, Set<Class<?>>> allConversions = Converter.allSupportedConversions();
        // THEN
        assertNotNull(allConversions, "The conversion map must not be null");
        assertFalse(allConversions.isEmpty(), "The conversion map must contain at least one entry");
        // A few well‑known conversions that the library supports.
        // 1. String can be converted to many target types (e.g., Integer, Long, Boolean, etc.).
        assertTrue(allConversions.containsKey(String.class), "The map should contain a key for String.class");
        Set<Class<?>> stringTargets = allConversions.get(String.class);
        assertNotNull(stringTargets, "Target set for String.class must not be null");
        assertTrue(stringTargets.contains(Integer.class), "String should be convertible to Integer");
        assertTrue(stringTargets.contains(Long.class), "String should be convertible to Long");
        assertTrue(stringTargets.contains(Boolean.class), "String should be convertible to Boolean");
        // 2. Integer can be converted to other numeric types.
        assertTrue(allConversions.containsKey(Integer.class), "The map should contain a key for Integer.class");
        Set<Class<?>> integerTargets = allConversions.get(Integer.class);
        assertNotNull(integerTargets, "Target set for Integer.class must not be null");
        assertTrue(integerTargets.contains(Long.class), "Integer should be convertible to Long");
        assertTrue(integerTargets.contains(Double.class), "Integer should be convertible to Double");
        // 3. Date conversions – Date should be convertible to java.time types.
        assertTrue(allConversions.containsKey(Date.class), "The map should contain a key for Date.class");
        Set<Class<?>> dateTargets = allConversions.get(Date.class);
        assertNotNull(dateTargets, "Target set for Date.class must not be null");
        assertTrue(dateTargets.contains(java.time.LocalDate.class), "Date should be convertible to LocalDate");
        assertTrue(dateTargets.contains(java.time.LocalDateTime.class), "Date should be convertible to LocalDateTime");
    }

    /* --------------------------------------------------------------
     *  Basic map validation
     * -------------------------------------------------------------- */
    @Test
    void shouldReturnNonNullAndNonEmptyMap() {
        Map<Class<?>, Set<Class<?>>> map = Converter.allSupportedConversions();
        assertNotNull(map, "The conversion map must not be null");
        assertFalse(map.isEmpty(), "The conversion map must contain at least one entry");
    }

    /* --------------------------------------------------------------
     *  Known conversion entries
     * -------------------------------------------------------------- */
    @Test
    void shouldContainCoreConversionEntries() {
        Map<Class<?>, Set<Class<?>>> map = Converter.allSupportedConversions();
        // String → Integer
        assertTrue(map.containsKey(String.class), "Map must contain String as a source");
        assertTrue(map.get(String.class).contains(Integer.class), "String should be convertible to Integer");
        // Integer → String
        assertTrue(map.containsKey(Integer.class), "Map must contain Integer as a source");
        assertTrue(map.get(Integer.class).contains(String.class), "Integer should be convertible to String");
        // Date → LocalDateTime
        assertTrue(map.containsKey(Date.class), "Map must contain Date as a source");
        assertTrue(map.get(Date.class).contains(java.time.LocalDateTime.class), "Date should be convertible to LocalDateTime");
        // LocalDate → Date
        assertTrue(map.containsKey(java.time.LocalDate.class), "Map must contain LocalDate as a source");
        assertTrue(map.get(java.time.LocalDate.class).contains(Date.class), "LocalDate should be convertible to Date");
        // UUID → String
        assertTrue(map.containsKey(UUID.class), "Map must contain UUID as a source");
        assertTrue(map.get(UUID.class).contains(String.class), "UUID should be convertible to String");
    }

    /* --------------------------------------------------------------
     *  Unsupported conversion absence
     * -------------------------------------------------------------- */
    @Test
    void shouldNotContainUnsupportedConversionEntries() {
        Map<Class<?>, Set<Class<?>>> map = Converter.allSupportedConversions();
        // UUID → Integer must NOT be present
        assertFalse(map.getOrDefault(UUID.class, Collections.emptySet()).contains(Integer.class), "UUID should NOT be convertible to Integer");
        // Boolean → ZonedDateTime must NOT be present
        assertFalse(map.getOrDefault(Boolean.class, Collections.emptySet()).contains(java.time.ZonedDateTime.class), "Boolean should NOT be convertible to ZonedDateTime");
    }

    /* --------------------------------------------------------------
     *  Immutability of the returned map
     * -------------------------------------------------------------- */
    @Test
    void mapShouldBeUnmodifiable() {
        Map<Class<?>, Set<Class<?>>> map = Converter.allSupportedConversions();
        // put
        assertThrows(UnsupportedOperationException.class, () -> map.put(Object.class, Collections.emptySet()), "map.put should throw UnsupportedOperationException");
        // remove
        assertThrows(UnsupportedOperationException.class, () -> map.remove(String.class), "map.remove should throw UnsupportedOperationException");
        // clear
        assertThrows(UnsupportedOperationException.class, map::clear, "map.clear should throw UnsupportedOperationException");
        // putAll
        assertThrows(UnsupportedOperationException.class, () -> map.putAll(Collections.singletonMap(Object.class, Set.of(String.class))), "map.putAll should throw UnsupportedOperationException");
    }

    @Test
    void keySetShouldBeUnmodifiable() {
        Map<Class<?>, Set<Class<?>>> map = Converter.allSupportedConversions();
        Set<Class<?>> keySet = map.keySet();
        assertThrows(UnsupportedOperationException.class, () -> keySet.remove(String.class), "keySet.remove should throw UnsupportedOperationException");
    }

    @Test
    void entrySetIteratorShouldBeUnmodifiable() {
        Map<Class<?>, Set<Class<?>>> map = Converter.allSupportedConversions();
        Iterator<Map.Entry<Class<?>, Set<Class<?>>>> it = map.entrySet().iterator();
        // Move to first element (if any) and try to remove
        if (it.hasNext()) {
            it.next();
            assertThrows(UnsupportedOperationException.class, it::remove, "entrySet iterator.remove should throw UnsupportedOperationException");
        }
    }

    @Test
    void valuesCollectionIteratorShouldBeUnmodifiable() {
        Map<Class<?>, Set<Class<?>>> map = Converter.allSupportedConversions();
        Iterator<Set<Class<?>>> it = map.values().iterator();
        if (it.hasNext()) {
            it.next();
            assertThrows(UnsupportedOperationException.class, it::remove, "values iterator.remove should throw UnsupportedOperationException");
        }
    }

    /* --------------------------------------------------------------
     *  Immutability of the target sets
     * -------------------------------------------------------------- */
    @Test
    void targetSetShouldBeUnmodifiable() {
        Map<Class<?>, Set<Class<?>>> map = Converter.allSupportedConversions();
        // Choose an arbitrary existing source key
        Class<?> source = map.keySet().iterator().next();
        Set<Class<?>> targetSet = map.get(source);
        // add
        assertThrows(UnsupportedOperationException.class, () -> targetSet.add(Object.class), "targetSet.add should throw UnsupportedOperationException");
        // remove
        assertThrows(UnsupportedOperationException.class, () -> targetSet.remove(Object.class), "targetSet.remove should throw UnsupportedOperationException");
        // clear
        assertThrows(UnsupportedOperationException.class, targetSet::clear, "targetSet.clear should throw UnsupportedOperationException");
        // bulk operations
        assertThrows(UnsupportedOperationException.class, () -> targetSet.addAll(Set.of(Object.class)), "targetSet.addAll should throw UnsupportedOperationException");
        assertThrows(UnsupportedOperationException.class, () -> targetSet.removeAll(Set.of(Object.class)), "targetSet.removeAll should throw UnsupportedOperationException");
        assertThrows(UnsupportedOperationException.class, () -> targetSet.retainAll(Set.of(Object.class)), "targetSet.retainAll should throw UnsupportedOperationException");
        // removeIf
        assertThrows(UnsupportedOperationException.class, () -> targetSet.removeIf(cls -> true), "targetSet.removeIf should throw UnsupportedOperationException");
    }

    /* --------------------------------------------------------------
     *  Consistency across multiple calls
     * -------------------------------------------------------------- */
    @Test
    void consecutiveCallsShouldReturnSameMapInstanceAndEqualContent() {
        Map<Class<?>, Set<Class<?>>> first = Converter.allSupportedConversions();
        Map<Class<?>, Set<Class<?>>> second = Converter.allSupportedConversions();
        // Reference equality
        assertSame(first, second, "Two consecutive calls should return the same map instance");
        // Content equality (defensive)
        assertEquals(first, second, "Two consecutive calls should have equal content");
    }

    /* --------------------------------------------------------------
     *  Data integrity checks
     * -------------------------------------------------------------- */
    @Test
    void mapShouldContainNoNullKeysOrValues() {
        Map<Class<?>, Set<Class<?>>> map = Converter.allSupportedConversions();
        for (Map.Entry<Class<?>, Set<Class<?>>> entry : map.entrySet()) {
            assertNotNull(entry.getKey(), "Map must not contain null keys");
            Set<Class<?>> targets = entry.getValue();
            assertNotNull(targets, "Target set must not be null for key: " + entry.getKey());
            for (Class<?> target : targets) {
                assertNotNull(target, "Target set must not contain null elements for key: " + entry.getKey());
            }
        }
    }

    @Test
    void eachTargetSetShouldContainUniqueClasses() {
        Map<Class<?>, Set<Class<?>>> map = Converter.allSupportedConversions();
        for (Map.Entry<Class<?>, Set<Class<?>>> entry : map.entrySet()) {
            Set<Class<?>> targets = entry.getValue();
            // Set semantics guarantee uniqueness; we verify by comparing size with distinct count
            long distinctCount = targets.stream().distinct().count();
            assertEquals(targets.size(), distinctCount, "Target set for " + entry.getKey() + " should contain unique classes");
        }
    }

    /* --------------------------------------------------------------
     *  Dynamic conversion registration
     * -------------------------------------------------------------- */
    @Test
    void addingCustomConversionShouldBeReflectedInAllSupportedConversions() {
        // Register a custom conversion: Boolean -> Integer
        Convert<Boolean, Integer> boolToInt = (from, conv) -> from ? 1 : 0;
        Converter.addConversion(Boolean.class, Integer.class, boolToInt);
        // Retrieve the map after registration
        Map<Class<?>, Set<Class<?>>> map = Converter.allSupportedConversions();
        // Verify the new mapping exists
        Set<Class<?>> boolTargets = map.get(Boolean.class);
        assertNotNull(boolTargets, "Boolean source entry must exist after adding conversion");
        assertTrue(boolTargets.contains(Integer.class), "Custom conversion Boolean → Integer should be present in the map");
        // Ensure the map is still unmodifiable after registration
        assertThrows(UnsupportedOperationException.class, () -> map.put(Object.class, Set.of(String.class)), "Map must remain unmodifiable after adding a custom conversion");
    }

    /* --------------------------------------------------------------
     *  Additional immutability checks after dynamic registration
     * -------------------------------------------------------------- */
    @Test
    void mapAndSetRemainUnmodifiableAfterCustomConversion() {
        // Ensure a custom conversion is present (Boolean → Integer)
        Convert<Boolean, Integer> boolToInt = (from, conv) -> from ? 1 : 0;
        Converter.addConversion(Boolean.class, Integer.class, boolToInt);
        Map<Class<?>, Set<Class<?>>> map = Converter.allSupportedConversions();
        // Map immutability
        assertThrows(UnsupportedOperationException.class, () -> map.remove(Boolean.class), "Map.remove should still throw after custom conversion");
        // Set immutability for the newly added entry
        Set<Class<?>> boolTargets = map.get(Boolean.class);
        assertNotNull(boolTargets, "Target set for Boolean must exist");
        assertThrows(UnsupportedOperationException.class, () -> boolTargets.add(Long.class), "Target set should remain unmodifiable after custom conversion");
    }
}
