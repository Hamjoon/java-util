package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import java.util.Set;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.*;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class Converter_getSupportedConversions_2_Test {

    /**
     * Original singleton that must be restored after each test.
     */
    private static Converter originalInstance;

    class CustomConverter extends Converter {

        @Override
        public Map<String, Set<String>> getSupportedConversions() {
            return Map.of("java.time.LocalDate", Set.of("java.sql.Date"));
        }
    }

    class CustomConverter extends Converter {

        @Override
        public Map<String, Set<String>> getSupportedConversions() {
            return Map.of("java.time.LocalDate", Set.of("java.sql.Date"));
        }
    }

    /**
     * Save the real instance once before any test runs.
     */
    @BeforeAll
    static void captureOriginalInstance() throws Exception {
        Field f = Converter.class.getDeclaredField("instance");
        f.setAccessible(true);
        originalInstance = (Converter) f.get(null);
    }

    /**
     * Restore the original singleton after each test to avoid side‑effects.
     */
    @AfterEach
    void restoreOriginalInstance() throws Exception {
        setInstance(originalInstance);
    }

    /**
     * Helper to replace the static {@code instance} field.
     */
    private static void setInstance(Converter newInstance) throws Exception {
        Field f = Converter.class.getDeclaredField("instance");
        f.setAccessible(true);
        f.set(null, newInstance);
    }

    @Test
    void getSupportedConversions_returnsNonNullMapWithEntries() {
        // GIVEN – the static method is invoked (no additional setup needed)
        // WHEN
        Map<String, Set<String>> conversions = Converter.getSupportedConversions();
        // THEN
        assertNotNull(conversions, "The returned map must not be null");
        assertFalse(conversions.isEmpty(), "The returned map should contain at least one source‑to‑target conversion");
        // Spot‑check a few well‑known conversion groups to ensure the map is populated
        // (the exact keys are implementation‑dependent, so we only assert their presence
        //  if they exist; the test will still pass if the library adds or removes entries.)
        if (conversions.containsKey("java.lang.String")) {
            Set<String> targets = conversions.get("java.lang.String");
            assertNotNull(targets, "Target set for String must not be null");
            assertFalse(targets.isEmpty(), "String should be convertible to at least one target type");
        }
        if (conversions.containsKey("java.lang.Integer")) {
            Set<String> targets = conversions.get("java.lang.Integer");
            assertNotNull(targets, "Target set for Integer must not be null");
            assertFalse(targets.isEmpty(), "Integer should be convertible to at least one target type");
        }
    }

    @Test
    @DisplayName("Delegates to current instance and returns the exact Map reference")
    void staticDelegatesAndReturnsExactReference() throws Exception {
        // GIVEN – a mock Converter that returns a specific map instance
        Converter mock = mock(Converter.class);
        Map<String, Set<String>> expected = new HashMap<>();
        expected.put("java.lang.String", Set.of("java.lang.Integer"));
        when(mock.getSupportedConversions()).thenReturn(expected);
        setInstance(mock);
        // WHEN
        Map<String, Set<String>> actual = Converter.getSupportedConversions();
        // THEN – same reference, not a copy
        assertSame(expected, actual, "Static method must return the exact map supplied by the instance");
        verify(mock, times(1)).getSupportedConversions();
    }

    @Test
    @DisplayName("Returns an empty map when the underlying instance reports none")
    void returnsEmptyMapWhenInstanceReturnsEmpty() throws Exception {
        Converter mock = mock(Converter.class);
        when(mock.getSupportedConversions()).thenReturn(Collections.emptyMap());
        setInstance(mock);
        Map<String, Set<String>> result = Converter.getSupportedConversions();
        assertNotNull(result);
        assertTrue(result.isEmpty(), "Result should be empty when the instance returns an empty map");
    }

    @Test
    @DisplayName("Propagates a non‑trivial map unchanged")
    void propagatesNonTrivialMap() throws Exception {
        Converter mock = mock(Converter.class);
        Map<String, Set<String>> supplied = new HashMap<>();
        supplied.put("java.lang.String", Set.of("java.lang.Integer", "java.lang.Boolean"));
        supplied.put("java.lang.Double", Set.of("java.lang.String"));
        when(mock.getSupportedConversions()).thenReturn(supplied);
        setInstance(mock);
        Map<String, Set<String>> result = Converter.getSupportedConversions();
        assertEquals(supplied, result, "Returned map must equal the supplied map");
        assertSame(supplied, result, "Returned map must be the same instance (no defensive copy)");
    }

    @Test
    @DisplayName("Mutating the returned map influences subsequent calls (no defensive copy)")
    void mutationIsReflectedInSubsequentCalls() throws Exception {
        Converter mock = mock(Converter.class);
        Map<String, Set<String>> mutable = new HashMap<>();
        mutable.put("java.lang.String", new HashSet<>(Set.of("java.lang.Integer")));
        when(mock.getSupportedConversions()).thenReturn(mutable);
        setInstance(mock);
        // First call – obtain reference
        Map<String, Set<String>> first = Converter.getSupportedConversions();
        // Mutate the map
        first.put("java.lang.Boolean", Set.of("java.lang.String"));
        // Second call – should see the mutation
        Map<String, Set<String>> second = Converter.getSupportedConversions();
        assertSame(first, second, "Both calls must return the same map instance");
        assertTrue(second.containsKey("java.lang.Boolean"), "Mutation should be visible on subsequent calls");
    }

    @Test
    @DisplayName("Throws NullPointerException when static instance field is null")
    void throwsNPEWhenInstanceIsNull() throws Exception {
        // deliberately break the singleton
        setInstance(null);
        assertThrows(NullPointerException.class, Converter::getSupportedConversions, "Calling the static method with a null instance should throw NPE");
    }

    @Test
    @DisplayName("Returned map is unmodifiable when underlying map is unmodifiable")
    void unmodifiableMapPropagation() throws Exception {
        Converter mock = mock(Converter.class);
        Map<String, Set<String>> unmodifiable = Collections.unmodifiableMap(Map.of("java.lang.String", Set.of("java.lang.Integer")));
        when(mock.getSupportedConversions()).thenReturn(unmodifiable);
        setInstance(mock);
        Map<String, Set<String>> result = Converter.getSupportedConversions();
        assertThrows(UnsupportedOperationException.class, () -> result.put("java.lang.Boolean", Set.of("java.lang.String")), "Attempting to modify the unmodifiable map must throw");
    }

    @Test
    @DisplayName("Returned Set values are unmodifiable when underlying sets are unmodifiable")
    void unmodifiableSetPropagation() throws Exception {
        Converter mock = mock(Converter.class);
        Set<String> unmodSet = Collections.unmodifiableSet(Set.of("java.lang.Integer"));
        Map<String, Set<String>> map = Map.of("java.lang.String", unmodSet);
        when(mock.getSupportedConversions()).thenReturn(map);
        setInstance(mock);
        Map<String, Set<String>> result = Converter.getSupportedConversions();
        Set<String> retrievedSet = result.get("java.lang.String");
        assertThrows(UnsupportedOperationException.class, () -> retrievedSet.add("java.lang.Boolean"), "Attempting to modify the unmodifiable set must throw");
    }

    @Test
    @DisplayName("Handles a source key with an empty target Set")
    void emptyTargetSetIsReturned() throws Exception {
        Converter mock = mock(Converter.class);
        Map<String, Set<String>> map = new HashMap<>();
        map.put("java.lang.String", Collections.emptySet());
        when(mock.getSupportedConversions()).thenReturn(map);
        setInstance(mock);
        Map<String, Set<String>> result = Converter.getSupportedConversions();
        assertTrue(result.containsKey("java.lang.String"), "Map must contain the source key");
        assertNotNull(result.get("java.lang.String"));
        assertTrue(result.get("java.lang.String").isEmpty(), "Target set must be empty");
    }

    @Test
    @DisplayName("Throws NPE when map contains a null key and the map is accessed")
    void nullKeyCausesNPEOnAccess() throws Exception {
        Converter mock = mock(Converter.class);
        Map<String, Set<String>> map = new HashMap<>();
        map.put(null, Set.of("java.lang.Integer"));
        when(mock.getSupportedConversions()).thenReturn(map);
        setInstance(mock);
        Map<String, Set<String>> result = Converter.getSupportedConversions();
        // Accessing the key set and invoking an operation on the null key should NPE
        assertThrows(NullPointerException.class, () -> result.keySet().forEach(k -> k.length()), "Iterating over a null key should trigger NPE");
    }

    @Test
    @DisplayName("Throws NPE when map contains a null value and the value is accessed")
    void nullValueCausesNPEOnAccess() throws Exception {
        Converter mock = mock(Converter.class);
        Map<String, Set<String>> map = new HashMap<>();
        map.put("java.lang.String", null);
        when(mock.getSupportedConversions()).thenReturn(map);
        setInstance(mock);
        Map<String, Set<String>> result = Converter.getSupportedConversions();
        // Attempt to read the null value (e.g., size()) must throw NPE
        assertThrows(NullPointerException.class, () -> result.get("java.lang.String").size(), "Accessing a null value set should throw NPE");
    }

    @Test
    @DisplayName("All concurrent threads receive the identical map instance")
    void concurrentCallsReturnSameReference() throws Exception {
        Converter mock = mock(Converter.class);
        Map<String, Set<String>> shared = new HashMap<>();
        shared.put("java.lang.String", new HashSet<>(Set.of("java.lang.Integer")));
        when(mock.getSupportedConversions()).thenReturn(shared);
        setInstance(mock);
        int threadCount = 20;
        ExecutorService exec = Executors.newFixedThreadPool(threadCount);
        try {
            Callable<Map<String, Set<String>>> task = Converter::getSupportedConversions;
            Set<Map<String, Set<String>>> results = ConcurrentHashMap.newKeySet();
            for (int i = 0; i < threadCount; i++) {
                exec.submit(() -> results.add(task.call()));
            }
            exec.shutdown();
            assertTrue(exec.awaitTermination(5, TimeUnit.SECONDS), "Threads must finish");
            assertEquals(1, results.size(), "All threads should receive the same map reference");
        } finally {
            exec.shutdownNow();
        }
    }

    @Test
    @DisplayName("Static method delegates to a custom Converter subclass")
    void customInstanceReturnsItsOwnMap() throws Exception {
        // Create a concrete subclass that overrides getSupportedConversions()
        class CustomConverter extends Converter {

            @Override
            public Map<String, Set<String>> getSupportedConversions() {
                return Map.of("java.time.LocalDate", Set.of("java.sql.Date"));
            }
        }
        Converter custom = new CustomConverter();
        setInstance(custom);
        Map<String, Set<String>> result = Converter.getSupportedConversions();
        Map<String, Set<String>> expected = Map.of("java.time.LocalDate", Set.of("java.sql.Date"));
        assertEquals(expected, result, "Static call must return the map supplied by the custom instance");
    }
}
