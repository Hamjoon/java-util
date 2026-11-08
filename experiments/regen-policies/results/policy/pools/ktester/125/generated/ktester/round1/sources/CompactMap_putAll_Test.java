package com.cedarsoftware.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class CompactMap_putAll_Test {

    /**
     * The map under test – a CompactMap with a very small compactSize so we can
     *  easily trigger the internal state changes without relying on the generated
     *  implementation classes.
     */
    private CompactMap<String, Integer> map;

    /**
     * Simple source map used by several test cases.
     */
    private Map<String, Integer> source;

    @BeforeEach
    void setupBeforeEach() {
        // Create a CompactMap that uses a LinkedHashMap as its backing map once the
        // compact size is exceeded. The anonymous subclass overrides the configuration
        // methods that would otherwise be generated at runtime.
        map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                // tiny size to keep the test deterministic
                return 3;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new LinkedHashMap<>();
            }

            @Override
            protected String getSingleValueKey() {
                return "single";
            }
        };
        source = new LinkedHashMap<>();
        source.put("a", 1);
        source.put("b", 2);
        source.put("c", 3);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure each test starts with a clean state.
        map.clear();
        source.clear();
    }

    /**
     * Provides a stream of arguments for the {@code nullOrEmptySourceMap} group.
     * Each argument is a {@link Map} instance (or {@code null}) together with a
     * description used only for readability in the test name.
     */
    private static Stream<Arguments> nullOrEmptySourceMapProvider() {
        return Stream.of(Arguments.of(null, "null map"), Arguments.of(Collections.emptyMap(), "empty map"));
    }

    /**
     * Simple non‑empty map used by several parameterised tests.
     */
    private static Map<String, Integer> simpleMap() {
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("a", 1);
        m.put("b", 2);
        m.put("c", 3);
        return m;
    }

    private static Stream<Arguments> differentMapImplementationsProvider() {
        Map<String, Integer> treeMap = new TreeMap<>() {

            {
                put("first", 1);
                put("second", 2);
                put("third", 3);
            }
        };
        Map<String, Integer> linkedMap = new LinkedHashMap<>() {

            {
                put("one", 1);
                put("two", 2);
                put("three", 3);
            }
        };
        return Stream.of(Arguments.of(treeMap, "TreeMap"), Arguments.of(linkedMap, "LinkedHashMap"));
    }

    /**
     * Returns {@code true} if the supplied method has a {@code void} return type.
     */
    public static boolean returnsVoid(Method method) {
        return method.getReturnType() == void.class;
    }

    /**
     * Finds a method with the given name and parameter types on the supplied class
     * (including its super‑classes).  Returns {@code null} if no such method exists.
     */
    public static Method findMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        try {
            return clazz.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            // Search declared methods (including private/protected) and super‑classes
            for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
                try {
                    return c.getDeclaredMethod(name, parameterTypes);
                } catch (NoSuchMethodException ignored) {
                }
            }
            return null;
        }
    }

    /**
     * Returns {@code true} if a method with the given signature is present on the class.
     */
    public static boolean isMethodPresent(Class<?> clazz, String name, Class<?>... parameterTypes) {
        return findMethod(clazz, name, parameterTypes) != null;
    }

    @Test
    void putAll_NullMap_DoesNothing() {
        map.put("x", 10);
        assertEquals(1, map.size(), "pre‑condition: map should contain one entry");
        // According to the concrete implementation a null argument is a no‑op
        map.putAll(null);
        assertEquals(1, map.size(), "size must stay unchanged when null is passed");
        assertEquals(10, map.get("x"));
    }

    @Test
    void putAll_EmptyMap_DoesNothing() {
        map.put("y", 20);
        assertEquals(1, map.size());
        // no‑op
        map.putAll(Collections.emptyMap());
        assertEquals(1, map.size(), "empty map should not affect the target map");
        assertEquals(20, map.get("y"));
    }

    @Test
    void putAll_NormalMap_AddsAllEntries() {
        assertTrue(map.isEmpty(), "map starts empty");
        map.putAll(source);
        assertEquals(source.size(), map.size(), "all entries must be added");
        for (Map.Entry<String, Integer> e : source.entrySet()) {
            assertTrue(map.containsKey(e.getKey()));
            assertEquals(e.getValue(), map.get(e.getKey()));
        }
    }

    @Test
    void putAll_OverridesExistingKeys() {
        // existing entry that will be overwritten
        map.put("a", 100);
        // entry that should stay untouched
        map.put("z", 999);
        // source contains a->1, b->2, c->3
        map.putAll(source);
        assertEquals(4, map.size(), "size = original unique keys + new ones");
        assertEquals(1, map.get("a"), "value for key 'a' must be overridden");
        assertEquals(2, map.get("b"));
        assertEquals(3, map.get("c"));
        assertEquals(999, map.get("z"));
    }

    @Test
    void putAll_DoesNotMutateSourceMap() {
        Map<String, Integer> copy = new LinkedHashMap<>(source);
        map.putAll(source);
        assertEquals(copy, source, "source map must remain unchanged after putAll");
    }

    @Test
    void putAll_TransitionsThroughInternalStates() {
        // Fill the map up to just before the compact size – it will be stored in the
        // compact array representation.
        map.put("k1", 1);
        map.put("k2", 2);
        assertEquals(2, map.size());
        // Adding a third entry pushes the size to the configured compactSize (3)
        // – still compact array.
        map.put("k3", 3);
        assertEquals(3, map.size());
        // Adding one more forces a transition to the backing LinkedHashMap.
        map.put("k4", 4);
        assertEquals(4, map.size());
        // Now putAll a collection that will cause another transition (e.g., clear + add)
        Map<String, Integer> extra = new HashMap<>();
        extra.put("x", 10);
        extra.put("y", 20);
        extra.put("z", 30);
        map.putAll(extra);
        assertEquals(7, map.size());
        assertTrue(map.keySet().containsAll(extra.keySet()));
    }

    @ParameterizedTest(name = "putAll with {1} should leave map unchanged")
    @MethodSource("nullOrEmptySourceMapProvider")
    void putAll_NullOrEmptySourceMap_NoChange(Map<String, Integer> src, String description) {
        // Pre‑populate the map to verify that the size does not change.
        map.put("pre", 42);
        int beforeSize = map.size();
        // The implementation treats a null argument as a no‑op (no NPE).
        map.putAll(src);
        assertEquals(beforeSize, map.size(), "size must stay unchanged when " + description + " is supplied");
        assertEquals(42, map.get("pre"));
    }

    @Test
    void putAll_AddsEntriesFromNonEmptyMap() {
        Map<String, Integer> src = simpleMap();
        map.putAll(src);
        assertEquals(3, map.size(), "size must become 3 after adding three entries");
        assertTrue(map.entrySet().containsAll(src.entrySet()), "all expected entries must be present");
    }

    @Test
    void putAll_OverwritesExistingEntries() {
        // Populate the map with different values for the same keys.
        map.put("x", 0);
        map.put("y", 0);
        Map<String, Integer> src = Map.of("x", 100, "y", 200);
        map.putAll(src);
        assertEquals(2, map.size(), "size stays 2 because keys are the same");
        assertEquals(100, map.get("x"));
        assertEquals(200, map.get("y"));
    }

    @Test
    void putAll_HandlesNullKeyAndNullValue() {
        Map<String, Integer> src = new HashMap<>() {

            {
                put(null, 999);
                put("nullValue", null);
            }
        };
        // CompactMap permits null keys/values (the implementation of put does not forbid them)
        map.putAll(src);
        assertEquals(2, map.size(), "size should be 2 after inserting null key/value");
        assertTrue(map.containsKey(null));
        assertEquals(999, map.get(null));
        assertTrue(map.containsKey("nullValue"));
        assertNull(map.get("nullValue"));
    }

    @ParameterizedTest(name = "putAll works with source {1}")
    @MethodSource("differentMapImplementationsProvider")
    void putAll_WorksWithDifferentMapImplementations(Map<String, Integer> src, String description) {
        map.putAll(src);
        assertEquals(src.size(), map.size(), "size must match source size for " + description);
        for (Map.Entry<String, Integer> e : src.entrySet()) {
            assertTrue(map.containsKey(e.getKey()));
            assertEquals(e.getValue(), map.get(e.getKey()));
        }
    }

    @Test
    void putAll_LargeMap_SizeVerification() {
        // Build a map with 1,000 entries using streams.
        Map<String, Integer> large = IntStream.range(0, 1000).boxed().collect(Collectors.toMap(i -> "k" + i, i -> i));
        map.putAll(large);
        assertEquals(1000, map.size(), "size must be 1000 after inserting the large map");
        // Spot‑check a few entries to avoid iterating over the whole map in the assertion.
        assertEquals(0, map.get("k0"));
        assertEquals(500, map.get("k500"));
        assertEquals(999, map.get("k999"));
    }
}
