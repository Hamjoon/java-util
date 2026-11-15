package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.util.stream.Stream;
// import org.junit.*;
// import static org.junit.Assert.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

class CompactSet_compactSize_Test {

    /**
     * Subclass that overrides {@code compactSize()} with a valid value (>= 2).
     */
    private static class ValidOverrideCompactSet<E> extends CompactSet<E> {

        private final int overriddenSize;

        ValidOverrideCompactSet(int overriddenSize) {
            this.overriddenSize = overriddenSize;
        }

        @Override
        protected int compactSize() {
            return overriddenSize;
        }
    }

    /**
     * Subclass that overrides {@code compactSize()} with an illegal value (< 2).
     */
    private static class InvalidOverrideCompactSet<E> extends CompactSet<E> {

        @Override
        protected int compactSize() {
            // illegal – must be >= 2
            return 1;
        }
    }

    /**
     * Helper subclass that exposes the protected {@code compactSize()} method.
     */
    private static class ExposedCompactSet<E> extends CompactSet<E> {

        public int getCompactSize() {
            return super.compactSize();
        }
    }

    // ---------- helper to obtain the internal map via reflection ----------
//     private static Map<?, ?> getInternalMap(CompactSet<?> set) throws Exception {
//         Field mapField = CompactSet.class.getDeclaredField("map");
//         mapField.setAccessible(true);
//         return (Map<?, ?>) mapField.get(set);
//     }

    // ---------- test data providers ----------
    private static Stream<Arguments> nullConfigProvider() {
        return Stream.of(Arguments.of(null, IllegalArgumentException.class));
    }

//     private static Stream<Arguments> emptyConfigProvider() {
//         return Stream.of(Arguments.of(Collections.emptyMap()), Arguments.of(new HashMap<>()));
//     }

//     private static Stream<Arguments> caseInsensitiveConfigProvider() {
//         Map<String, Object> cfg = new HashMap<>();
//         cfg.put("caseInsensitive", true);
//         return Stream.of(Arguments.of(cfg));
//     }

//     private static Stream<Arguments> naturalOrderingConfigProvider() {
//         Map<String, Object> cfg = new HashMap<>();
//         cfg.put("ordering", "natural");
//         return Stream.of(Arguments.of(cfg));
//     }

//     private static Stream<Arguments> invalidOrderingConfigProvider() {
//         Map<String, Object> cfg = new HashMap<>();
//         cfg.put("ordering", "unknown");
//         return Stream.of(Arguments.of(cfg));
//     }

//     private static Stream<Arguments> sizeBelowThresholdProvider() {
//         Map<String, Object> cfg = new HashMap<>();
//         cfg.put("size", 10);
//         return Stream.of(Arguments.of(cfg));
//     }

//     private static Stream<Arguments> sizeAboveThresholdProvider() {
//         Map<String, Object> cfg = new HashMap<>();
//         cfg.put("size", 100);
//         return Stream.of(Arguments.of(cfg));
//     }

    // -----------------------------------------------------------------
    @Test
    void defaultCompactSize_isLibraryDefault() {
        ExposedCompactSet<String> set = new ExposedCompactSet<>();
        int expected = CompactMap.DEFAULT_COMPACT_SIZE;
        assertEquals(expected, set.getCompactSize(), "compactSize() should return CompactMap.DEFAULT_COMPACT_SIZE by default");
    }

    @Test
    void overriddenCompactSize_validDoesNotThrow() {
        ValidOverrideCompactSet<String> set = new ValidOverrideCompactSet<>(10);
        assertTrue(set.isEmpty(), "New set should be empty");
        assertTrue(set.add("alpha"));
        assertTrue(set.add("beta"));
        assertEquals(2, set.size(), "Set should contain the two added elements");
    }

    @Test
    void overriddenCompactSize_invalidThrowsException() {
        assertThrows(IllegalStateException.class, InvalidOverrideCompactSet::new);
    }

    // ---------- actual tests ----------
//     @ParameterizedTest(name = "[{index}] withConfig(null) throws {1}")
//     @MethodSource("nullConfigProvider")
//     void withConfig_nullMap_throwsException(Map<String, Object> config, Class<? extends Exception> expected) {
//         CompactSet<String> set = new CompactSet<>();
//         assertThrows(expected, () -> set.withConfig(config));
//     }

//     @ParameterizedTest(name = "[{index}] with empty config returns same instance")
//     @MethodSource("emptyConfigProvider")
//     void withConfig_emptyMap_returnsSameInstance(Map<String, Object> config) {
//         CompactSet<String> set = new CompactSet<>();
//         CompactSet<String> result = set.withConfig(config);
//         assertSame(set, result, "Empty configuration should return the same CompactSet instance");
//     }

//     @ParameterizedTest(name = "[{index}] caseInsensitive config makes set case‑insensitive")
//     @MethodSource("caseInsensitiveConfigProvider")
//     void withConfig_caseInsensitive_true(Map<String, Object> config) throws Exception {
//         CompactSet<String> set = new CompactSet<>();
//         set.withConfig(config);
//         assertTrue(set.isCaseInsensitive(), "Config with caseInsensitive=true must make the set case‑insensitive");
//     }

//     @ParameterizedTest(name = "[{index}] ordering=natural creates TreeMap")
//     @MethodSource("naturalOrderingConfigProvider")
//     void withConfig_orderingNatural_createsTreeMap(Map<String, Object> config) throws Exception {
//         CompactSet<String> set = new CompactSet<>();
//         set.withConfig(config);
//         Map<?, ?> internal = getInternalMap(set);
//         assertTrue(internal instanceof TreeMap, "ordering='natural' should result in a TreeMap implementation");
//         // Verify natural ordering
//         set.add("beta");
//         set.add("alpha");
//         Iterator<String> it = set.iterator();
//         assertEquals("alpha", it.next());
//         assertEquals("beta", it.next());
//     }

//     @ParameterizedTest(name = "[{index}] invalid ordering throws IllegalArgumentException")
//     @MethodSource("invalidOrderingConfigProvider")
//     void withConfig_invalidOrdering_throwsException(Map<String, Object> config) {
//         CompactSet<String> set = new CompactSet<>();
//         assertThrows(IllegalArgumentException.class, () -> set.withConfig(config));
//     }

//     @ParameterizedTest(name = "[{index}] size below threshold keeps HashMap")
//     @MethodSource("sizeBelowThresholdProvider")
//     void withConfig_sizeBelowThreshold_keepsHashMap(Map<String, Object> config) throws Exception {
//         CompactSet<String> set = new CompactSet<>();
//         set.withConfig(config);
//         Map<?, ?> internal = getInternalMap(set);
//         assertTrue(internal instanceof HashMap, "When size < compact threshold, internal map should be a HashMap");
//         // also verify compactSize() is still the library default (>= config size)
//         ExposedCompactSet<String> exposed = new ExposedCompactSet<>();
//         assertTrue(exposed.getCompactSize() > (int) config.get("size"), "Default compact size must be greater than config size");
//     }

//     @ParameterizedTest(name = "[{index}] size above threshold uses LinkedHashMap")
//     @MethodSource("sizeAboveThresholdProvider")
//     void withConfig_sizeAboveThreshold_usesLinkedHashMap(Map<String, Object> config) throws Exception {
//         CompactSet<String> set = new CompactSet<>();
//         set.withConfig(config);
//         Map<?, ?> internal = getInternalMap(set);
//         assertTrue(internal instanceof LinkedHashMap, "When size >= compact threshold, internal map should be a LinkedHashMap preserving insertion order");
//         // Verify insertion order
//         set.add("first");
//         set.add("second");
//         Iterator<String> it = set.iterator();
//         assertEquals("first", it.next());
//         assertEquals("second", it.next());
//     }
}