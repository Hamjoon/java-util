package com.cedarsoftware.util.convert;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.math.BigDecimal;
// import org.junit.Test;
// import static org.junit.Assert.*;

class Converter_getSupportedConversions_Test {

    /**
     * Simple dummy class used for a custom conversion.
     */
    public static final class Dummy {
    }

    /* --------------------------------------------------------------------- */
    private static Map<?, ?> getConversionDb() throws Exception {
        Field f = Converter.class.getDeclaredField("CONVERSION_DB");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Object, Convert<?>> map = (Map<Object, Convert<?>>) f.get(null);
        return map;
    }

    private static Map<?, ?> getUserDb() throws Exception {
        Field f = Converter.class.getDeclaredField("USER_DB");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Object, Convert<?>> map = (Map<Object, Convert<?>>) f.get(null);
        return map;
    }

    /**
     * Helper to clear both internal maps.
     */
    private static void clearAllDbs() throws Exception {
        getConversionDb().clear();
        getUserDb().clear();
    }

    /**
     * Re‑populate the built‑in conversion map by invoking the private method.
     */
    private static void rebuildBuiltInDb() throws Exception {
        Method m = Converter.class.getDeclaredMethod("buildFactoryConversions");
        m.setAccessible(true);
        m.invoke(null);
    }

    /**
     * Obtain a ConversionPair instance via the public static factory method.
     */
    private static Object pair(Class<?> src, Class<?> tgt) {
        return Converter.pair(src, tgt);
    }

    /**
     * Create a dummy Convert implementation (lambda) that returns a constant.
     */
    private static Convert<?> dummyConvert(Object result) {
        return (from, conv) -> result;
    }

    /**
     * Verify that the method returns a non‑null, non‑empty map when no user
     * conversions have been registered.
     */
//     @Test
//     void shouldReturnBuiltInConversionsWhenNoUserConversions() {
//         Map<String, Set<String>> supported = Converter.getSupportedConversions();
//         assertNotNull(supported, "The returned map must not be null");
//         assertFalse(supported.isEmpty(), "The map should contain built‑in conversions");
//         // Spot‑check that a well‑known conversion is present.
//         // The library always supports conversion from String to Integer.
//         Set<String> targets = supported.get(String.class.getName());
//         assertNotNull(targets, "String source should be present");
//         assertTrue(targets.contains(Integer.class.getName()), "String → Integer conversion should be listed");
//     }

    /**
     * Verify that a user‑added conversion appears in the map returned by
     * {@code getSupportedConversions()}.
     */
//     @Test
//     void shouldIncludeUserAddedConversion() {
//         // Register a custom conversion: String → Dummy
//         Converter.addConversion(String.class, Dummy.class, (from, conv) -> new Dummy());
//         Map<String, Set<String>> supported = Converter.getSupportedConversions();
//         assertNotNull(supported, "The returned map must not be null");
//         Set<String> targets = supported.get(String.class.getName());
//         assertNotNull(targets, "String source should be present after adding a user conversion");
//         assertTrue(targets.contains(Dummy.class.getName()), "The custom conversion (String → Dummy) should be listed");
//     }

    /**
     * Both built‑in and user conversions with overlapping source keys.
     * Verify that the resulting set merges targets without duplication.
     */
//     @Test
//     void shouldMergeBuiltInAndUserConversionsForSameSource() throws Exception {
//         // Built‑in already provides String → Integer (checked in previous test).
//         // Add two user conversions for the same source (String) to Long and BigDecimal.
//         Converter.addConversion(String.class, Long.class, dummyConvert(0L));
//         Converter.addConversion(String.class, BigDecimal.class, dummyConvert(BigDecimal.ONE));
//         Map<String, Set<String>> map = Converter.getSupportedConversions();
//         Set<String> targets = map.get(String.class.getName());
//         assertNotNull(targets, "String source entry must exist");
//         // Expected three distinct target class names.
//         Set<String> expected = Set.of(Integer.class.getName(), Long.class.getName(), BigDecimal.class.getName());
//         assertEquals(expected, targets, "String → {Integer,Long,BigDecimal} should be present without duplicates");
//     }

    /**
     * When both internal maps are empty, the method should return an empty TreeMap.
     */
//     @Test
//     void shouldReturnEmptyMapWhenBothDbsAreEmpty() throws Exception {
//         // remove all built‑in and user conversions
//         clearAllDbs();
//         Map<String, Set<String>> map = Converter.getSupportedConversions();
//         assertNotNull(map, "Result must not be null");
//         assertTrue(map.isEmpty(), "Map should be empty when no conversions are registered");
//     }

    /**
     * Single user‑defined conversion only (added via public API).
     */
//     @Test
//     void shouldReportSingleUserConversionOnly() throws Exception {
//         clearAllDbs();
//         // User conversion: Long → Date
//         Converter.addConversion(Long.class, Date.class, dummyConvert(new Date()));
//         Map<String, Set<String>> map = Converter.getSupportedConversions();
//         Set<String> targets = map.get(Long.class.getName());
//         assertNotNull(targets, "Long source entry must exist");
//         assertEquals(Set.of(Date.class.getName()), targets, "Only the user‑defined conversion should be present");
//     }

    /**
     * Verify that the returned map's keys are ordered alphabetically.
     */
//     @Test
//     void shouldReturnAlphabeticallyOrderedSourceKeys() throws Exception {
//         clearAllDbs();
//         // Add conversions with sources Boolean, Integer, String
//         Converter.addConversion(Boolean.class, Integer.class, dummyConvert(1));
//         Converter.addConversion(Integer.class, String.class, dummyConvert("int"));
//         Converter.addConversion(String.class, Boolean.class, dummyConvert(true));
//         Map<String, Set<String>> map = Converter.getSupportedConversions();
//         List<String> keys = new ArrayList<>(map.keySet());
//         List<String> expectedOrder = List.of(Boolean.class.getName(), Integer.class.getName(), String.class.getName());
//         assertEquals(expectedOrder, keys, "Source keys should be sorted alphabetically (TreeMap ordering)");
//     }

    /**
     * Verify that each Set of target class names is sorted alphabetically.
     */
//     @Test
//     void shouldReturnAlphabeticallyOrderedTargetSets() throws Exception {
//         clearAllDbs();
//         // All conversions target Number
//         Converter.addConversion(Integer.class, Number.class, dummyConvert(1));
//         Converter.addConversion(Long.class, Number.class, dummyConvert(2L));
//         Converter.addConversion(BigDecimal.class, Number.class, dummyConvert(BigDecimal.ONE));
//         Map<String, Set<String>> map = Converter.getSupportedConversions();
//         // The Set implementation used by the method is expected to be a SortedSet.
//         Set<String> numberTargets = map.get(Number.class.getName());
//         assertNull(numberTargets, "Number should be a target, not a source");
//         // Verify ordering for each source's target set (each set contains only "java.lang.Number")
//         for (String source : List.of(Integer.class.getName(), Long.class.getName(), BigDecimal.class.getName())) {
//             Set<String> targets = map.get(source);
//             assertNotNull(targets, "Targets for source " + source + " must exist");
//             assertTrue(targets instanceof SortedSet, "Target set should be a SortedSet for deterministic ordering");
//             assertEquals(List.of(Number.class.getName()), new ArrayList<>(targets), "Each source should map to Number only");
//         }
//     }

    /**
     * Verify that concurrent modification of USER_DB during processing does NOT
     * throw a ConcurrentModificationException (the map is a ConcurrentHashMap).
     */
//     @Test
//     void shouldHandleConcurrentModificationOfUserDbGracefully() throws Exception {
//         clearAllDbs();
//         // Add an initial conversion to ensure the method iterates over some entries.
//         Converter.addConversion(String.class, Integer.class, dummyConvert(0));
//         // Background thread that adds a new conversion while getSupportedConversions() runs.
//         ExecutorService executor = Executors.newSingleThreadExecutor();
//         Future<?> future = executor.submit(() -> {
//             try {
//                 // Small delay to increase likelihood of overlap.
//                 Thread.sleep(10);
//                 Converter.addConversion(Long.class, String.class, dummyConvert("long"));
//             } catch (InterruptedException e) {
//                 Thread.currentThread().interrupt();
//             }
//         });
//         // Invoke the method; it should complete without throwing.
//         Map<String, Set<String>> result = Converter.getSupportedConversions();
//         // Ensure the background addition succeeded.
//         future.get(1, TimeUnit.SECONDS);
//         executor.shutdownNow();
//         // Verify that both conversions are present (order not important).
//         assertTrue(result.get(String.class.getName()).contains(Integer.class.getName()), "Original conversion should still be present");
//         assertTrue(result.get(Long.class.getName()).contains(String.class.getName()), "Conversion added concurrently should be present");
//     }

    /**
     * Sanity check: both internal maps are non‑null but empty.
     * The method should return an empty TreeMap without throwing.
     */
//     @Test
//     void shouldReturnEmptyMapWhenInternalMapsAreEmptyButNonNull() throws Exception {
//         // ensures maps are empty but still instantiated
//         clearAllDbs();
//         Map<String, Set<String>> map = Converter.getSupportedConversions();
//         assertNotNull(map, "Result must not be null");
//         assertTrue(map.isEmpty(), "Map should be empty when internal DBs contain no entries");
//     }
}