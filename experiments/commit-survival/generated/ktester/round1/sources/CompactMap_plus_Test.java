package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CompactMap_plus_Test {

    private CompactMap<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance before each test
        map = new CompactMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference for GC (not strictly required)
        map = null;
    }

    /**
     * Helper that supplies all argument variations described in the JSON test cases.
     *
     * @return a stream of {@link Arguments} where the first element is the input value
     *         and the second element is a human‑readable description (used only for
     *         test display purposes).
     */
    private static Stream<Arguments> plusInvalidInputsProvider() {
        // Empty HashMap
        Map<String, Integer> emptyHashMap = new HashMap<>();
        // Non‑empty HashMap
        Map<String, Integer> populatedHashMap = new HashMap<>();
        populatedHashMap.put("a", 1);
        populatedHashMap.put("b", 2);
        // TreeMap with a single entry
        Map<String, String> treeMap = new TreeMap<>();
        treeMap.put("x", "y");
        // Unmodifiable Map wrapper
        Map<String, Integer> unmodifiableMap = Collections.unmodifiableMap(new HashMap<>());
        // Custom anonymous object
        Object customObject = new Object() {

            @Override
            public String toString() {
                return "custom";
            }
        };
        return Stream.of(Arguments.of(null, "null argument"), Arguments.of(emptyHashMap, "empty HashMap"), Arguments.of("someString", "plain String"), Arguments.of(populatedHashMap, "non‑empty HashMap"), Arguments.of(treeMap, "TreeMap instance"), Arguments.of(new CompactMap<>(), "another CompactMap"), Arguments.of(Collections.emptyList(), "empty List"), Arguments.of(new int[] { 1, 2, 3 }, "int array"), Arguments.of(Integer.valueOf(42), "Integer wrapper"), Arguments.of(customObject, "custom anonymous object"), Arguments.of(unmodifiableMap, "unmodifiable Map"));
    }

    @Test
    void plus_ShouldThrowUnsupportedOperationException() {
        // Given: a CompactMap instance (already created in @BeforeEach)
        // When / Then: invoking plus(Object) must throw the expected exception
        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () -> map.plus(new CompactMap<>()));
        assertEquals("Unsupported operation [plus] or [+] between Maps.  Use putAll() instead.", exception.getMessage(), "Exception message should match the implementation");
    }

    /**
     * Verifies that {@code CompactMap.plus(Object)} always throws
     * {@link UnsupportedOperationException} with the exact message defined in the
     * implementation, irrespective of the supplied argument.
     *
     * @param right the argument passed to {@code plus}
     * @param description a short description used by JUnit for reporting
     */
    @ParameterizedTest(name = "plus({1}) → UnsupportedOperationException")
    @MethodSource("plusInvalidInputsProvider")
    void plus_ShouldThrowUnsupportedOperationException_ForAllInputs(Object right, String description) {
        // When / Then: invoking plus(Object) must throw the expected exception
        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () -> map.plus(right), () -> "Expected UnsupportedOperationException for input: " + description);
        // The implementation uses a constant message – verify it exactly.
        assertEquals("Unsupported operation [plus] or [+] between Maps.  Use putAll() instead.", exception.getMessage(), "Exception message should match the implementation");
    }
}
