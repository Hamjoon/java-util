package com.cedarsoftware.util;

import java.util.*;
// import org.junit.Test;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// import static org.junit.Assert.*;
// @ExtendWith(MockitoExtension.class)
class CompactSet_withConfig_Test {

    /**
     * Creates a {@link CompactSet} with the library defaults (case‑insensitive,
     * unordered, compactSize = CompactMap.DEFAULT_COMPACT_SIZE) and populates it
     * with the supplied elements.
     */
    private static CompactSet<String> createDefaultSet(String... elements) {
        CompactSet<String> set = CompactSet.<String>builder().build();
        set.addAll(Arrays.asList(elements));
        return set;
    }

    /**
     * Retrieves a value from the configuration map safely, casting to the expected type.
     */
    @SuppressWarnings("unchecked")
    private static <T> T configValue(CompactSet<?> set, String key) {
        return (T) set.getConfig().get(key);
    }

    /**
     * Verify that {@code withConfig} creates a new set that respects the supplied
     * configuration (compact size, case sensitivity and ordering) while preserving
     * the original elements.
     */
    @Test
    void testWithConfigAllOptionsApplied() {
        // Build an original set with explicit configuration
        CompactSet<String> original = CompactSet.<String>builder().caseSensitive(// case‑sensitive
        true).// unordered (hash‑like)
        noOrder().compactSize(// small compact size
        5).build();
        // Populate the set
        original.addAll(Arrays.asList("b", "a", "c"));
        // Prepare a new configuration map that overrides everything
        Map<String, Object> config = new HashMap<>();
        // case‑insensitive
        config.put(CompactMap.CASE_SENSITIVE, Boolean.FALSE);
        // larger compact size
        config.put(CompactMap.COMPACT_SIZE, Integer.valueOf(10));
        // sorted order
        config.put(CompactMap.ORDERING, CompactMap.SORTED);
        // Apply the configuration
        CompactSet<String> newSet = original.withConfig(config);
        // --- Verify that the configuration has been applied ---
        Map<String, Object> newConfig = newSet.getConfig();
        assertEquals(Boolean.FALSE, newConfig.get(CompactMap.CASE_SENSITIVE));
        assertEquals(10, newConfig.get(CompactMap.COMPACT_SIZE));
        assertEquals(CompactMap.SORTED, newConfig.get(CompactMap.ORDERING));
        // --- Verify that the original elements are present ---
        assertTrue(newSet.contains("a"));
        assertTrue(newSet.contains("b"));
        assertTrue(newSet.contains("c"));
        // Case‑insensitivity: a different case of an existing element must be found
        assertTrue(newSet.contains("A"));
        assertTrue(newSet.contains("B"));
        assertTrue(newSet.contains("C"));
        // --- Verify that the ordering is now sorted ---
        List<String> iterationOrder = new ArrayList<>();
        for (String s : newSet) {
            iterationOrder.add(s);
        }
        assertEquals(Arrays.asList("a", "b", "c"), iterationOrder);
    }

    /**
     * Verify that missing keys in the configuration map cause {@code withConfig}
     * to fall back to the current configuration of the original set.
     */
    @Test
    void testWithConfigPartialOverrides() {
        // Original set: case‑sensitive, insertion order, compact size 7
        CompactSet<String> original = CompactSet.<String>builder().caseSensitive(true).insertionOrder().compactSize(7).build();
        original.addAll(Arrays.asList("x", "y"));
        // Only change case sensitivity
        Map<String, Object> config = new HashMap<>();
        config.put(CompactMap.CASE_SENSITIVE, Boolean.FALSE);
        CompactSet<String> newSet = original.withConfig(config);
        // Configuration that was not supplied must stay the same
        Map<String, Object> newConfig = newSet.getConfig();
        assertEquals(Boolean.FALSE, newConfig.get(CompactMap.CASE_SENSITIVE));
        assertEquals(7, newConfig.get(CompactMap.COMPACT_SIZE));
        assertEquals(CompactMap.INSERTION, newConfig.get(CompactMap.ORDERING));
        // Verify case‑insensitivity works
        assertTrue(newSet.contains("X"));
        assertTrue(newSet.contains("Y"));
    }

    /**
     * Verify that passing a {@code null} configuration map results in an exception.
     */
    @Test
    void testWithConfigNullArgument() {
        CompactSet<String> set = CompactSet.<String>builder().build();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> set.withConfig(null));
        assertTrue(ex.getMessage().contains("config cannot be null"));
    }

    @Test
    void testOverrideCompactSizeOnly() {
        CompactSet<String> original = createDefaultSet("one", "two");
        Map<String, Object> config = new HashMap<>();
        config.put(CompactMap.COMPACT_SIZE, Integer.valueOf(5));
        CompactSet<String> newSet = original.withConfig(config);
        assertEquals(Integer.valueOf(5), configValue(newSet, CompactMap.COMPACT_SIZE));
        // defaults for the other options
        assertEquals(Boolean.FALSE, configValue(newSet, CompactMap.CASE_SENSITIVE));
        assertEquals(CompactMap.UNORDERED, configValue(newSet, CompactMap.ORDERING));
        // elements must be preserved
        assertTrue(newSet.containsAll(Arrays.asList("one", "two")));
    }

    @Test
    void testOverrideCaseSensitiveOnly() {
        CompactSet<String> original = createDefaultSet("Alpha", "Beta");
        Map<String, Object> config = new HashMap<>();
        config.put(CompactMap.CASE_SENSITIVE, Boolean.TRUE);
        CompactSet<String> newSet = original.withConfig(config);
        assertEquals(Boolean.TRUE, configValue(newSet, CompactMap.CASE_SENSITIVE));
        // defaults for the other options
        assertEquals(Integer.valueOf(CompactMap.DEFAULT_COMPACT_SIZE), configValue(newSet, CompactMap.COMPACT_SIZE));
        assertEquals(CompactMap.UNORDERED, configValue(newSet, CompactMap.ORDERING));
        // case‑sensitive check
        assertTrue(newSet.contains("Alpha"));
        assertFalse(newSet.contains("alpha"));
    }

    @Test
    void testOverrideOrderingSorted() {
        CompactSet<String> original = createDefaultSet("c", "a", "b");
        Map<String, Object> config = new HashMap<>();
        config.put(CompactMap.ORDERING, CompactMap.SORTED);
        CompactSet<String> newSet = original.withConfig(config);
        assertEquals(CompactMap.SORTED, configValue(newSet, CompactMap.ORDERING));
        // verify sorted iteration
        List<String> iter = new ArrayList<>();
        newSet.forEach(iter::add);
        assertEquals(Arrays.asList("a", "b", "c"), iter);
    }

    @Test
    void testOverrideOrderingReverse() {
        CompactSet<String> original = createDefaultSet("a", "b", "c");
        Map<String, Object> config = new HashMap<>();
        config.put(CompactMap.ORDERING, CompactMap.REVERSE);
        CompactSet<String> newSet = original.withConfig(config);
        assertEquals(CompactMap.REVERSE, configValue(newSet, CompactMap.ORDERING));
        // verify reverse iteration
        List<String> iter = new ArrayList<>();
        newSet.forEach(iter::add);
        assertEquals(Arrays.asList("c", "b", "a"), iter);
    }

    @Test
    void testOverrideOrderingInsertion() {
        // original unordered; we will request insertion ordering
        CompactSet<String> original = createDefaultSet("first", "second", "third");
        Map<String, Object> config = new HashMap<>();
        config.put(CompactMap.ORDERING, CompactMap.INSERTION);
        CompactSet<String> newSet = original.withConfig(config);
        assertEquals(CompactMap.INSERTION, configValue(newSet, CompactMap.ORDERING));
        // insertion order must match the order of addition to the *new* set
        List<String> iter = new ArrayList<>();
        newSet.forEach(iter::add);
        assertEquals(Arrays.asList("first", "second", "third"), iter);
    }

    @Test
    void testPartialConfigWithNullValues() {
        CompactSet<String> original = createDefaultSet("x", "y");
        Map<String, Object> config = new HashMap<>();
        config.put(CompactMap.COMPACT_SIZE, null);
        config.put(CompactMap.CASE_SENSITIVE, null);
        // only this overrides
        config.put(CompactMap.ORDERING, CompactMap.SORTED);
        CompactSet<String> newSet = original.withConfig(config);
        // null entries must fall back to original values
        assertEquals(Integer.valueOf(CompactMap.DEFAULT_COMPACT_SIZE), configValue(newSet, CompactMap.COMPACT_SIZE));
        assertEquals(Boolean.FALSE, configValue(newSet, CompactMap.CASE_SENSITIVE));
        assertEquals(CompactMap.SORTED, configValue(newSet, CompactMap.ORDERING));
        // element preservation
        assertTrue(newSet.containsAll(Arrays.asList("x", "y")));
    }

    @Test
    void testEmptyConfigMapCopiesCurrentSettings() {
        CompactSet<String> original = CompactSet.<String>builder().caseSensitive(false).noOrder().compactSize(10).build();
        original.addAll(Arrays.asList("p", "q"));
        Map<String, Object> emptyConfig = Collections.emptyMap();
        CompactSet<String> newSet = original.withConfig(emptyConfig);
        // new instance must be distinct
        assertNotSame(original, newSet);
        // configuration must be identical
        assertEquals(original.getConfig(), newSet.getConfig());
        // elements must be preserved
        assertTrue(newSet.containsAll(Arrays.asList("p", "q")));
    }

    @Test
    void testOriginalSetUnchangedAfterWithConfig() {
        CompactSet<String> original = CompactSet.<String>builder().caseSensitive(true).insertionOrder().compactSize(8).build();
        original.addAll(Arrays.asList("a", "b"));
        Map<String, Object> config = new HashMap<>();
        config.put(CompactMap.CASE_SENSITIVE, Boolean.FALSE);
        config.put(CompactMap.ORDERING, CompactMap.INSERTION);
        CompactSet<String> newSet = original.withConfig(config);
        // original config unchanged
        assertEquals(Boolean.TRUE, configValue(original, CompactMap.CASE_SENSITIVE));
        assertEquals(Integer.valueOf(8), configValue(original, CompactMap.COMPACT_SIZE));
        assertEquals(CompactMap.INSERTION, configValue(original, CompactMap.ORDERING));
        // original elements unchanged
        assertEquals(2, original.size());
        assertTrue(original.containsAll(Arrays.asList("a", "b")));
        // new set reflects overrides
        assertEquals(Boolean.FALSE, configValue(newSet, CompactMap.CASE_SENSITIVE));
        assertEquals(CompactMap.INSERTION, configValue(newSet, CompactMap.ORDERING));
    }

    @Test
    void testInvalidCompactSizeType() {
        CompactSet<String> original = createDefaultSet("one");
        Map<String, Object> config = new HashMap<>();
        // wrong type
        config.put(CompactMap.COMPACT_SIZE, "ten");
        assertThrows(ClassCastException.class, () -> original.withConfig(config));
    }

    @Test
    void testInvalidCaseSensitiveType() {
        CompactSet<String> original = createDefaultSet("one");
        Map<String, Object> config = new HashMap<>();
        // wrong type
        config.put(CompactMap.CASE_SENSITIVE, "true");
        assertThrows(ClassCastException.class, () -> original.withConfig(config));
    }

    @Test
    void testInvalidOrderingType() {
        CompactSet<String> original = createDefaultSet("one");
        Map<String, Object> config = new HashMap<>();
        // wrong type
        config.put(CompactMap.ORDERING, 123);
        assertThrows(ClassCastException.class, () -> original.withConfig(config));
    }

    @Test
    void testUnknownOrderingValue() {
        CompactSet<String> original = createDefaultSet("one");
        Map<String, Object> config = new HashMap<>();
        // unsupported value
        config.put(CompactMap.ORDERING, "magic");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> original.withConfig(config));
        assertTrue(ex.getMessage().toLowerCase().contains("unsupported ordering"));
    }

    @Test
    void testNegativeCompactSize() {
        CompactSet<String> original = createDefaultSet("one");
        Map<String, Object> config = new HashMap<>();
        config.put(CompactMap.COMPACT_SIZE, -5);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> original.withConfig(config));
        assertTrue(ex.getMessage().toLowerCase().contains("compactsize must be positive"));
    }

    @Test
    void testZeroCompactSize() {
        CompactSet<String> original = createDefaultSet("one");
        Map<String, Object> config = new HashMap<>();
        config.put(CompactMap.COMPACT_SIZE, 0);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> original.withConfig(config));
        assertTrue(ex.getMessage().toLowerCase().contains("compactsize must be positive"));
    }
}
