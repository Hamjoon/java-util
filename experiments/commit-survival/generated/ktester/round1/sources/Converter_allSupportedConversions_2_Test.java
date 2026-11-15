package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import com.cedarsoftware.util.convert.Convert;
import org.mockito.Mock;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Converter_allSupportedConversions_2_Test {

    /* --------------------------------------------------------------------- */
    private static Converter originalInstance = null;

    private static boolean instanceWasReplaced = false;

    class Foo {
    }

    class Foo {
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the static singleton is restored after each test that mutates it.
        restoreOriginalInstanceIfNeeded();
    }

    /**
     * Replaces {@code Converter.instance} with a fresh instance for isolation.
     * The original instance is saved so it can be restored later.
     */
    private void replaceStaticInstance() throws Exception {
        Field f = Converter.class.getDeclaredField("instance");
        f.setAccessible(true);
        if (!instanceWasReplaced) {
            originalInstance = (Converter) f.get(null);
        }
        Converter fresh = new Converter(new DefaultConverterOptions());
        f.set(null, fresh);
        instanceWasReplaced = true;
    }

    /**
     * Restores the original {@code Converter.instance} if it was replaced.
     */
    private static void restoreOriginalInstanceIfNeeded() {
        if (instanceWasReplaced) {
            try {
                Field f = Converter.class.getDeclaredField("instance");
                f.setAccessible(true);
                f.set(null, originalInstance);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                instanceWasReplaced = false;
                originalInstance = null;
            }
        }
    }

    /**
     * Verifies that {@code Converter.allSupportedConversions()} returns a non‑null map
     * that contains at least one expected source‑target conversion pair.
     *
     * The library ships with a conversion from {@link Integer} to {@link String},
     * so we assert that this mapping is present.  The exact contents of the map are
     * implementation‑specific, but the presence of a known conversion guarantees that
     * the underlying data structure has been populated correctly.
     */
    @Test
    void testAllSupportedConversionsContainsKnownMapping() {
        // given
        // (nothing to arrange – the static singleton is already initialised)
        // when
        Map<Class<?>, Set<Class<?>>> allConversions = Converter.allSupportedConversions();
        // then
        assertNotNull(allConversions, "The map of all supported conversions must not be null");
        assertTrue(allConversions.containsKey(Integer.class), "The map should contain a key for Integer.class");
        Set<Class<?>> integerTargets = allConversions.get(Integer.class);
        assertNotNull(integerTargets, "Target set for Integer.class must not be null");
        assertTrue(integerTargets.contains(String.class), "Integer → String conversion should be supported");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void defaultSupportedConversions_mapIsPopulated() {
        Map<Class<?>, Set<Class<?>>> map = Converter.allSupportedConversions();
        assertNotNull(map, "Map must not be null");
        // Expect at least one source with a non‑empty target set, e.g. String → Integer
        assertTrue(map.containsKey(String.class), "Map should contain a key for String.class");
        Set<Class<?>> targets = map.get(String.class);
        assertNotNull(targets, "Target set for String.class must not be null");
        assertFalse(targets.isEmpty(), "Target set for String.class must contain at least one entry");
        // The exact target is not critical; we just assert non‑emptiness.
    }

    @Test
    void defaultSupportedConversions_noCustomEntriesBeforeRegistration() {
        Map<Class<?>, Set<Class<?>>> map = Converter.allSupportedConversions();
        // Use a made‑up class that certainly hasn't been registered.
        class Foo {
        }
        assertFalse(map.containsKey(Foo.class), "Map must not contain entries for unregistered custom classes");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void customConversionRegistration_stringToLocalDate_isReflected() throws Exception {
        // isolate modifications
        replaceStaticInstance();
        @SuppressWarnings("unchecked")
        Convert<LocalDate> mockConversion = mock(Convert.class);
        Converter.instance.addConversion(String.class, LocalDate.class, mockConversion);
        Map<Class<?>, Set<Class<?>>> map = Converter.allSupportedConversions();
        assertTrue(map.containsKey(String.class), "Map should contain String as a source after registration");
        Set<Class<?>> targets = map.get(String.class);
        assertNotNull(targets);
        assertTrue(targets.contains(LocalDate.class), "Custom conversion String → LocalDate must be present");
    }

    @Test
    void customConversionRegistration_integerToBoolean_isReflected() throws Exception {
        // isolate modifications
        replaceStaticInstance();
        @SuppressWarnings("unchecked")
        Convert<Boolean> mockConversion = mock(Convert.class);
        Converter.instance.addConversion(Integer.class, Boolean.class, mockConversion);
        Map<Class<?>, Set<Class<?>>> map = Converter.allSupportedConversions();
        assertTrue(map.containsKey(Integer.class), "Map should contain Integer as a source after registration");
        Set<Class<?>> targets = map.get(Integer.class);
        assertNotNull(targets);
        assertTrue(targets.contains(Boolean.class), "Custom conversion Integer → Boolean must be present");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void mapImmutability_putThrowsUnsupportedOperationException() {
        Map<Class<?>, Set<Class<?>>> map = Converter.allSupportedConversions();
        assertThrows(UnsupportedOperationException.class, () -> map.put(Double.class, Collections.emptySet()), "Attempting to modify the returned map should throw UnsupportedOperationException");
    }

    @Test
    void targetSetImmutability_addThrowsUnsupportedOperationException() {
        Map<Class<?>, Set<Class<?>>> map = Converter.allSupportedConversions();
        // Find any existing target set (e.g., for Integer.class)
        Set<Class<?>> targetSet = map.values().stream().filter(s -> !s.isEmpty()).findFirst().orElseThrow(() -> new AssertionError("No non‑empty target set found in the map"));
        assertThrows(UnsupportedOperationException.class, () -> targetSet.add(Float.class), "Attempting to modify a target set should throw UnsupportedOperationException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void duplicateConversionHandling_sameConversionAddedTwice_noDuplicateEntries() throws Exception {
        // isolate modifications
        replaceStaticInstance();
        @SuppressWarnings("unchecked")
        Convert<Integer> mockConversion = mock(Convert.class);
        // First registration
        Converter.instance.addConversion(String.class, Integer.class, mockConversion);
        // Second (duplicate) registration
        Converter.instance.addConversion(String.class, Integer.class, mockConversion);
        Map<Class<?>, Set<Class<?>>> map = Converter.allSupportedConversions();
        Set<Class<?>> targets = map.get(String.class);
        assertNotNull(targets, "Target set for String.class must exist");
        long count = targets.stream().filter(c -> c.equals(Integer.class)).count();
        assertEquals(1L, count, "Integer.class should appear exactly once in the target set");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void consistencyAcrossCalls_sameMapInstanceReturned() {
        Map<Class<?>, Set<Class<?>>> first = Converter.allSupportedConversions();
        Map<Class<?>, Set<Class<?>>> second = Converter.allSupportedConversions();
        assertSame(first, second, "Two successive calls should return the same map instance");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void specificDefaultConversionPresence_intToIntegerExists() {
        Map<Class<?>, Set<Class<?>>> map = Converter.allSupportedConversions();
        assertTrue(map.containsKey(int.class), "Map should contain primitive int as a source");
        Set<Class<?>> targets = map.get(int.class);
        assertNotNull(targets);
        assertTrue(targets.contains(Integer.class), "Default conversion int → Integer must be present");
    }

    @Test
    void specificDefaultConversionPresence_dateToTimestampExists() {
        Map<Class<?>, Set<Class<?>>> map = Converter.allSupportedConversions();
        assertTrue(map.containsKey(Date.class), "Map should contain java.util.Date as a source");
        Set<Class<?>> targets = map.get(Date.class);
        assertNotNull(targets);
        assertTrue(targets.contains(Timestamp.class), "Default conversion Date → Timestamp must be present");
    }

    @Test
    void specificDefaultConversionPresence_localDateToLocalDateTimeExists() {
        Map<Class<?>, Set<Class<?>>> map = Converter.allSupportedConversions();
        assertTrue(map.containsKey(LocalDate.class), "Map should contain LocalDate as a source");
        Set<Class<?>> targets = map.get(LocalDate.class);
        assertNotNull(targets);
        assertTrue(targets.contains(LocalDateTime.class), "Default conversion LocalDate → LocalDateTime must be present");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void nullInstanceHandling_throwsNullPointerException() throws Exception {
        // Save the original instance and replace it with null
        Field f = Converter.class.getDeclaredField("instance");
        f.setAccessible(true);
        Converter saved = (Converter) f.get(null);
        try {
            f.set(null, null);
            assertThrows(NullPointerException.class, Converter::allSupportedConversions, "Calling allSupportedConversions() when instance is null should throw NPE");
        } finally {
            // Restore original instance so other tests are not affected
            f.set(null, saved);
        }
    }
}
