package com.cedarsoftware.util;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
// import org.junit.Test;
// import static org.junit.Assert.*;
// @ExtendWith(MockitoExtension.class)
import static org.junit.jupiter.api.Assertions.*;

class CompactSet_builder_Test {

    /* --------------------------------------------------------------------- */
    @Test
    void defaultBuilderCreatesCaseSensitiveSet() {
        CompactSet<String> set = CompactSet.<String>builder().build();
        assertNotNull(set, "Builder should produce a non‑null CompactSet");
        assertTrue(set.isEmpty(), "New set must be empty");
        assertTrue(set.add("foo"), "First add should succeed");
        assertFalse(set.add("FOO"), "Case‑sensitive set should treat different case as distinct");
        assertEquals(2, set.size(), "Set should contain two distinct elements");
        assertTrue(set.contains("foo"));
        assertTrue(set.contains("FOO"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void caseInsensitiveBuilder() {
        CompactSet<String> set = CompactSet.<String>builder().caseSensitive(false).build();
        assertTrue(set.add("apple"));
        assertFalse(set.add("Apple"), "Case‑insensitive set must reject duplicate differing only by case");
        assertEquals(1, set.size(), "Only one logical element should be stored");
        assertTrue(set.contains("APPLE"));
        assertTrue(set.contains("apple"));
    }

    /* --------------------------------------------------------------------- */
//     @Test
//     void sortedOrderBuilder() {
//         CompactSet<String> set = CompactSet.<String>builder().sortedOrder().build();
//         set.add("c");
//         set.add("a");
//         set.add("b");
//         Iterator<String> it = set.iterator();
//         assertEquals("a", it.next());
//         assertEquals("b", it.next());
//         assertEquals("c", it.next());
//         assertFalse(it.hasNext(), "No more elements expected");
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void insertionOrderBuilder() {
//         CompactSet<String> set = CompactSet.<String>builder().insertionOrder().build();
//         set.add("first");
//         set.add("second");
//         set.add("third");
//         Iterator<String> it = set.iterator();
//         assertEquals("first", it.next());
//         assertEquals("second", it.next());
//         assertEquals("third", it.next());
//         assertFalse(it.hasNext());
//     }

    /* --------------------------------------------------------------------- */
    @Test
    void compactSizeBuilder() {
        final int targetSize = 80;
        CompactSet<Integer> set = CompactSet.<Integer>builder().compactSize(targetSize).build();
        for (int i = 0; i < targetSize + 10; i++) {
            assertTrue(set.add(i), "All inserts should be unique");
        }
        assertEquals(targetSize + 10, set.size(), "Set size must match number of distinct inserts");
        assertTrue(set.contains(0));
        assertTrue(set.contains(targetSize + 5));
    }

    /* --------------------------------------------------------------------- */
//     @Test
//     void builderChaining() {
//         CompactSet<String> set = CompactSet.<String>builder().caseSensitive(false).sortedOrder().compactSize(50).build();
//         assertTrue(set.add("a"));
//         assertFalse(set.add("A"));
//         Iterator<String> it = set.iterator();
//         assertEquals("a", it.next());
//         assertFalse(it.hasNext());
//     }

    /* --------------------------------------------------------------------- */
    @Test
    void builderReturnsNonNullAndDistinctInstances() {
        CompactSet.Builder<String> b1 = CompactSet.<String>builder();
        CompactSet.Builder<String> b2 = CompactSet.<String>builder();
        assertNotNull(b1, "First builder call must not return null");
        assertNotNull(b2, "Second builder call must not return null");
        assertNotSame(b1, b2, "Each call to builder() should produce a distinct Builder instance");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void builderCreatesEmptySet() throws Exception {
        CompactSet.Builder<String> builder = CompactSet.<String>builder();
        Method buildMethod = builder.getClass().getMethod("build");
        @SuppressWarnings("unchecked")
        CompactSet<String> set = (CompactSet<String>) buildMethod.invoke(builder);
        assertNotNull(set, "Result of build() must not be null");
        assertTrue(set.isEmpty(), "Newly built CompactSet should be empty");
        assertEquals(0, set.size(), "Size of a newly built set must be zero");
    }

    /* --------------------------------------------------------------------- */
//     @Test
//     void builderConfigPropagation() throws Exception {
//         CompactSet.Builder<String> builder = CompactSet.<String>builder().caseSensitive(false).sortedOrder();
//         Method buildMethod = builder.getClass().getMethod("build");
//         @SuppressWarnings("unchecked")
//         CompactSet<String> set = (CompactSet<String>) buildMethod.invoke(builder);
//         Map<String, Object> resultCfg = set.getConfig();
//         assertNotNull(resultCfg, "getConfig() must never return null");
//         Map<String, Object> expected = new HashMap<>();
//         expected.put("caseSensitive", Boolean.FALSE);
//         expected.put("ordering", "natural");
//         for (Map.Entry<String, Object> e : expected.entrySet()) {
//             assertTrue(resultCfg.containsKey(e.getKey()), "Result config should contain key: " + e.getKey());
//             assertEquals(e.getValue(), resultCfg.get(e.getKey()), "Value for key '" + e.getKey() + "' should match");
//         }
//     }

//     @Test
//     void builderEmptyConfigPropagation() throws Exception {
//         CompactSet.Builder<String> builder = CompactSet.<String>builder();
//         Method buildMethod = builder.getClass().getMethod("build");
//         @SuppressWarnings("unchecked")
//         CompactSet<String> set = (CompactSet<String>) buildMethod.invoke(builder);
//         Map<String, Object> resultCfg = set.getConfig();
//         assertNotNull(resultCfg, "Configuration map should never be null");
//         assertTrue(resultCfg.isEmpty() || resultCfg.keySet().stream().allMatch(k -> java.util.Arrays.asList("caseSensitive", "ordering", "compactSize").contains(k)), "Result config should contain only known default keys");
//     }

    /* --------------------------------------------------------------------- */
    @Test
    void distinctCompactSetInstances() {
        CompactSet<String> set1 = CompactSet.<String>builder().build();
        CompactSet<String> set2 = CompactSet.<String>builder().build();
        assertNotSame(set1, set2, "Each build() call must produce a distinct CompactSet instance");
        assertTrue(set1.isEmpty());
        assertTrue(set2.isEmpty());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void genericTypePreservationString() {
        CompactSet<String> set = CompactSet.<String>builder().build();
        assertTrue(set.add("testString"));
        assertTrue(set.contains("testString"));
    }

    @Test
    void genericTypePreservationInteger() {
        CompactSet<Integer> set = CompactSet.<Integer>builder().build();
        assertTrue(set.add(123));
        assertTrue(set.contains(123));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void builderImmutabilityAfterBuild() throws Exception {
        CompactSet.Builder<String> builder = CompactSet.<String>builder();
        // default: case‑sensitive
        CompactSet<String> setA = builder.build();
        // mutate builder
        builder.caseSensitive(false);
        // now case‑insensitive
        CompactSet<String> setB = builder.build();
        // setA remains case‑sensitive
        assertTrue(setA.add("Apple"));
        assertFalse(setA.add("apple"));
        // setB is case‑insensitive
        assertTrue(setB.add("Apple"));
        assertFalse(setB.add("apple"));
        assertTrue(setB.contains("APPLE"));
    }
}