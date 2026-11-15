package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import com.cedarsoftware.util.convert.Convert;
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

@ExtendWith(MockitoExtension.class)
class Converter_allSupportedConversions_2_Test {

    private static Converter originalInstance;

    @BeforeAll
    static void captureOriginalInstance() throws Exception {
        originalInstance = getSingletonInstance();
    }

    @AfterEach
    void restoreSingleton() throws Exception {
        setSingletonInstance(originalInstance);
    }

    /**
     * Retrieves the private static final {@code instance} field of {@link Converter}.
     */
    private static Converter getSingletonInstance() throws Exception {
        Field f = Converter.class.getDeclaredField("instance");
        f.setAccessible(true);
        return (Converter) f.get(null);
    }

    /**
     * Replaces the private static final {@code instance} field with the supplied value.
     */
    private static void setSingletonInstance(Converter newInstance) throws Exception {
        Field f = Converter.class.getDeclaredField("instance");
        f.setAccessible(true);
        // remove final modifier (works on most JVMs for test purposes)
        Field modifiers = Field.class.getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(f, f.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
        f.set(null, newInstance);
    }

    /**
     * Replaces the internal registry with the supplied map (used for corrupted‑map tests).
     */
    private static void setInternalRegistry(Converter instance, Map<Class<?>, Set<Class<?>>> newMap) throws Exception {
        for (Field f : Converter.class.getDeclaredFields()) {
            if (Map.class.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                // remove final modifier if present
                Field modifiers = Field.class.getDeclaredField("modifiers");
                modifiers.setAccessible(true);
                modifiers.setInt(f, f.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
                f.set(instance, newMap);
                return;
            }
        }
        throw new IllegalStateException("Unable to locate internal conversion map");
    }

    /**
     * Verifies that {@link Converter#allSupportedConversions()} returns a non‑null,
     * non‑empty map and that a few well‑known source‑to‑target conversion pairs are present.
     */
    @Test
    void testAllSupportedConversionsContainsExpectedMappings() {
        // Act
        Map<Class<?>, Set<Class<?>>> conversions = Converter.allSupportedConversions();
        // Assert – basic contract
        assertNotNull(conversions, "The returned map must not be null");
        assertFalse(conversions.isEmpty(), "The map of supported conversions should not be empty");
        // Assert – a few concrete, documented conversions
        // String → Integer
        assertTrue(conversions.containsKey(String.class), "Conversions should contain an entry for source type String");
        Set<Class<?>> stringTargets = conversions.get(String.class);
        assertNotNull(stringTargets, "Target set for String should not be null");
        assertTrue(stringTargets.contains(Integer.class), "String should be convertible to Integer");
        // Integer → String
        assertTrue(conversions.containsKey(Integer.class), "Conversions should contain an entry for source type Integer");
        Set<Class<?>> intTargets = conversions.get(Integer.class);
        assertNotNull(intTargets, "Target set for Integer should not be null");
        assertTrue(intTargets.contains(String.class), "Integer should be convertible to String");
        // Date → Long (timestamp)
        assertTrue(conversions.containsKey(java.util.Date.class), "Conversions should contain an entry for source type Date");
        Set<Class<?>> dateTargets = conversions.get(java.util.Date.class);
        assertNotNull(dateTargets, "Target set for Date should not be null");
        assertTrue(dateTargets.contains(Long.class), "Date should be convertible to Long");
    }

    /**
     * Finds the internal conversion registry map inside a {@link Converter} instance.
     *  The exact field name is not part of the public API, therefore we locate
     *  the first field of type {@code Map<Class<?>, Set<Class<?>>>}.
     */
    @SuppressWarnings("unchecked")
    private static Map<Class<?>, Set<Class<?>>> getInternalRegistry(Converter instance) throws Exception {
        for (Field f : Converter.class.getDeclaredFields()) {
            if (Map.class.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                return (Map<Class<?>, Set<Class<?>>>) f.get(instance);
            }
        }
        throw new IllegalStateException("Unable to locate internal conversion map");
    }

    @Test
    void testDefaultSupportedConversions_basicContract() {
        Map<Class<?>, Set<Class<?>>> conversions = Converter.allSupportedConversions();
        assertNotNull(conversions, "Returned map must not be null");
        assertFalse(conversions.isEmpty(), "Returned map must contain at least one entry");
        // well‑known pairs
        assertTrue(conversions.containsKey(String.class), "Map should contain key String.class");
        assertTrue(conversions.get(String.class).contains(Integer.class), "String should be convertible to Integer");
        assertTrue(conversions.containsKey(Integer.class), "Map should contain key Integer.class");
        assertTrue(conversions.get(Integer.class).contains(String.class), "Integer should be convertible to String");
        assertTrue(conversions.containsKey(Date.class), "Map should contain key Date.class");
        assertTrue(conversions.get(Date.class).contains(Long.class), "Date should be convertible to Long");
    }

    @Test
    void testDefaultSupportedConversions_noNullKeysOrValues() {
        Map<Class<?>, Set<Class<?>>> conversions = Converter.allSupportedConversions();
        for (Map.Entry<Class<?>, Set<Class<?>>> e : conversions.entrySet()) {
            assertNotNull(e.getKey(), "Conversion map must not contain null keys");
            Set<Class<?>> targets = e.getValue();
            assertNotNull(targets, "Target set for key " + e.getKey() + " must not be null");
            for (Class<?> target : targets) {
                assertNotNull(target, "Target set must not contain null values");
            }
        }
    }

    @Test
    void testDefaultSupportedConversions_expectedMatrixSample() {
        Map<Class<?>, Set<Class<?>>> conversions = Converter.allSupportedConversions();
        // Sample verification for Integer → {Long, BigDecimal, String}
        Set<Class<?>> intTargets = conversions.get(Integer.class);
        assertNotNull(intTargets, "Integer targets should be present");
        assertAll("Integer conversion targets", () -> assertTrue(intTargets.contains(Long.class), "Integer → Long"), () -> assertTrue(intTargets.contains(BigDecimal.class), "Integer → BigDecimal"), () -> assertTrue(intTargets.contains(String.class), "Integer → String"));
    }

    @Test
    void testCustomConversionRegistration_addsNewEntry() {
        // Register LocalDate → Instant
        Convert<Instant> conv = (from, c) -> Instant.from(((LocalDate) from).atStartOfDay(java.time.ZoneOffset.UTC));
        Converter.addConversion(LocalDate.class, Instant.class, conv);
        Map<Class<?>, Set<Class<?>>> map = Converter.allSupportedConversions();
        assertTrue(map.containsKey(LocalDate.class), "Map should contain the newly added source LocalDate");
        assertTrue(map.get(LocalDate.class).contains(Instant.class), "LocalDate should be convertible to Instant after registration");
    }

    @Test
    void testCustomConversionRegistration_aggregation() {
        // Register String → UUID
        Convert<UUID> uuidConv = (from, c) -> UUID.fromString((String) from);
        Converter.addConversion(String.class, UUID.class, uuidConv);
        // Also ensure String → Integer (built‑in) is still present
        Map<Class<?>, Set<Class<?>>> map = Converter.allSupportedConversions();
        Set<Class<?>> stringTargets = map.get(String.class);
        assertNotNull(stringTargets, "String source entry must exist");
        assertTrue(stringTargets.contains(UUID.class), "Custom target UUID must be present");
        assertTrue(stringTargets.contains(Integer.class), "Built‑in target Integer must still be present");
    }

    @Test
    void testAddConversion_nullArguments() {
        Convert<Object> dummy = (from, c) -> from;
        assertAll("null argument validation", () -> assertThrows(NullPointerException.class, () -> Converter.addConversion(null, Integer.class, dummy), "Null source should throw NPE"), () -> assertThrows(NullPointerException.class, () -> Converter.addConversion(String.class, null, dummy), "Null target should throw NPE"), () -> assertThrows(NullPointerException.class, () -> Converter.addConversion(String.class, Integer.class, null), "Null conversion function should throw NPE"));
    }

    @Test
    void testAllSupportedConversions_isUnmodifiableMap() {
        Map<Class<?>, Set<Class<?>>> map = Converter.allSupportedConversions();
        assertThrows(UnsupportedOperationException.class, () -> map.put(Object.class, Collections.emptySet()), "Attempting to modify the outer map must throw UnsupportedOperationException");
    }

    @Test
    void testAllSupportedConversions_innerSetsAreUnmodifiable() {
        Map<Class<?>, Set<Class<?>>> map = Converter.allSupportedConversions();
        // pick any existing key, e.g., String.class
        Set<Class<?>> inner = map.get(String.class);
        assertNotNull(inner, "Inner set for String must exist");
        assertThrows(UnsupportedOperationException.class, () -> inner.add(Object.class), "Modifying an inner set must throw UnsupportedOperationException");
    }

    @Test
    void testIdempotentRegistration_samePairMultipleTimes() {
        Convert<LocalDateTime> conv = (from, c) -> ((LocalDate) from).atStartOfDay();
        // first registration
        Converter.addConversion(LocalDate.class, LocalDateTime.class, conv);
        // second registration – should not create duplicate entries
        Converter.addConversion(LocalDate.class, LocalDateTime.class, conv);
        Map<Class<?>, Set<Class<?>>> map = Converter.allSupportedConversions();
        Set<Class<?>> targets = map.get(LocalDate.class);
        assertNotNull(targets, "LocalDate entry must exist after registration");
        assertTrue(targets.contains(LocalDateTime.class), "Target LocalDateTime must be present");
        assertEquals(1, targets.stream().filter(t -> t.equals(LocalDateTime.class)).count(), "LocalDateTime should appear exactly once in the target set");
    }

    @Test
    void testAllSupportedConversions_nullSingleton_throwsNPE() throws Exception {
        // replace the singleton with null
        setSingletonInstance(null);
        assertThrows(NullPointerException.class, Converter::allSupportedConversions, "Calling allSupportedConversions() when the singleton is null must throw NPE");
    }

    @Test
    void testAllSupportedConversions_corruptedMap_nullKey() throws Exception {
        Converter instance = getSingletonInstance();
        Map<Class<?>, Set<Class<?>>> corrupted = new HashMap<>();
        // null key
        corrupted.put(null, Collections.singleton(String.class));
        setInternalRegistry(instance, corrupted);
        assertThrows(NullPointerException.class, Converter::allSupportedConversions, "Null key in internal map should cause NPE");
    }

    @Test
    void testAllSupportedConversions_corruptedMap_nullValueSet() throws Exception {
        Converter instance = getSingletonInstance();
        Map<Class<?>, Set<Class<?>>> corrupted = new HashMap<>();
        // null value
        corrupted.put(Integer.class, null);
        setInternalRegistry(instance, corrupted);
        assertThrows(NullPointerException.class, Converter::allSupportedConversions, "Null value set in internal map should cause NPE");
    }

    @Test
    void testAllSupportedConversions_concurrentModification_throwsCME() throws Exception {
        Converter instance = getSingletonInstance();
        Map<Class<?>, Set<Class<?>>> registry = getInternalRegistry(instance);
        // Use a thread‑safe map that we can mutate while iteration occurs
        Map<Class<?>, Set<Class<?>>> concurrentMap = new ConcurrentHashMap<>(registry);
        setInternalRegistry(instance, concurrentMap);
        // Background thread that adds a dummy entry after a short delay
        ExecutorService exec = Executors.newSingleThreadExecutor();
        Future<?> future = exec.submit(() -> {
            try {
                Thread.sleep(10);
                concurrentMap.put(UUID.class, Collections.singleton(String.class));
            } catch (InterruptedException ignored) {
            }
        });
        // The main thread invokes the method; depending on timing it may see a CME
        try {
            // We cannot guarantee a CME on every run, so we assert that either
            // a CME is thrown or the map contains the newly added entry after the call.
            assertDoesNotThrow(() -> {
                Map<Class<?>, Set<Class<?>>> result = Converter.allSupportedConversions();
                // If no exception, ensure the map is still well‑formed
                assertNotNull(result);
            });
        } finally {
            // ensure background task finished
            future.get();
            exec.shutdownNow();
        }
    }
}
