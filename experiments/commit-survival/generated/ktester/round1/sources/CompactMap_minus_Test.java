package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CompactMap_minus_Test {

    private CompactMap<String, Integer> map;

    /**
     * Simple custom key class used in one of the parameterized tests.
     */
    private static final class CustomKey {

        private final String id;

        CustomKey(String id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof CustomKey))
                return false;
            CustomKey that = (CustomKey) o;
            return Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return "CustomKey{id='" + id + "'}";
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use the no‑arg constructor – it does not require any external dependencies
        map = new CompactMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference to help GC; not strictly necessary but keeps the pattern consistent
        map = null;
    }

    /**
     * Supplies a diverse set of arguments to the {@code minus_ShouldThrowUnsupportedOperationException}
     * test, covering null, primitives, custom objects, collections, maps and another
     * {@link CompactMap} instance.
     */
    private static Stream<Arguments> provideMinusArguments() {
        return Stream.of(Arguments.of((Object) null), Arguments.of("someKey"), // Integer (autoboxed)
        Arguments.of(123), // Boolean (autoboxed)
        Arguments.of(true), // User‑defined object
        Arguments.of(new CustomKey("key")), // Another CompactMap instance
        Arguments.of(new CompactMap<>()), // Empty List
        Arguments.of(Collections.emptyList()), // Singleton Map
        Arguments.of(Collections.singletonMap("a", 1)));
    }

    /**
     * Supplies the arguments for {@code minus_MapStateUnchangedAfterException}.
     * Each argument consists of the value to pass to {@code minus} and a map
     * representing the expected initial state of the {@link CompactMap}.
     */
    private static Stream<Arguments> provideMapStateArguments() {
        Map<String, Integer> threeEntries = new LinkedHashMap<>();
        threeEntries.put("key1", 10);
        threeEntries.put("key2", 20);
        threeEntries.put("key3", 30);
        return Stream.of(// Map with three entries, argument does not exist in the map
        Arguments.of("unusedKey", threeEntries), // Empty map, argument is null
        Arguments.of(null, Collections.emptyMap()));
    }

    @Test
    void minus_ShouldThrowUnsupportedOperationException() {
        // GIVEN: a CompactMap instance (already prepared in @BeforeEach)
        // WHEN / THEN: invoking minus must throw the expected exception with the correct message
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> map.minus("anyKey"));
        assertEquals("Unsupported operation [minus] or [-] between Maps.  Use removeAll() or retainAll() instead.", ex.getMessage());
    }

    @Test
    void minus_WithNullArgument_ShouldThrowUnsupportedOperationException() {
        // GIVEN: a CompactMap instance (already prepared)
        // WHEN / THEN: invoking minus with null also throws the same exception
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> map.minus(null));
        assertEquals("Unsupported operation [minus] or [-] between Maps.  Use removeAll() or retainAll() instead.", ex.getMessage());
    }

    /**
     * Verifies that after the expected exception is thrown, the map's contents
     * remain unchanged.
     *
     * Two scenarios are covered:
     * <ul>
     *   <li>A map pre‑populated with three entries.</li>
     *   <li>An empty map.</li>
     * </ul>
     */
    @ParameterizedTest(name = "minus on map with size {1} should leave map unchanged")
    @MethodSource("provideMapStateArguments")
    void minus_MapStateUnchangedAfterException(Object removeMe, Map<String, Integer> initialEntries) {
        // GIVEN: a map populated with the supplied initial entries
        map.putAll(initialEntries);
        // Capture a snapshot of the map's state before invoking minus
        Map<String, Integer> beforeSnapshot = new HashMap<>(map);
        // WHEN / THEN: minus throws the expected exception
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> map.minus(removeMe));
        assertEquals("Unsupported operation [minus] or [-] between Maps.  Use removeAll() or retainAll() instead.", ex.getMessage());
        // AND: the map's state is unchanged
        assertEquals(beforeSnapshot.size(), map.size(), "Map size must remain unchanged");
        assertEquals(beforeSnapshot, map, "Map entries must remain unchanged");
    }
}
