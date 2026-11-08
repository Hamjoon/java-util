package com.cedarsoftware.util;

import static com.cedarsoftware.util.CompactMap.CASE_SENSITIVE;
import static com.cedarsoftware.util.CompactMap.COMPACT_SIZE;
import static com.cedarsoftware.util.CompactMap.INNER_MAP_TYPE;
import static com.cedarsoftware.util.CompactMap.MAP_TYPE;
import static com.cedarsoftware.util.CompactMap.ORDERING;
import static com.cedarsoftware.util.CompactMap.UNORDERED;
import static com.cedarsoftware.util.CompactMap.SORTED;
import static org.junit.jupiter.api.Assertions.*;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import static com.cedarsoftware.util.CompactMap.INSERTION;
import java.util.LinkedHashMap;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class CompactMap_validateAndFinalizeOptions_Test {

    private static Stream<Arguments> mapTypeAndCaseSensitivityProvider() {
        return Stream.of(// ordering = insertion, caseSensitive = false -> wrap LinkedHashMap
        Arguments.of(new HashMap<String, Object>() {

            {
                put(ORDERING, INSERTION);
                put(CASE_SENSITIVE, false);
            }
        }, CaseInsensitiveMap.class, LinkedHashMap.class, "Insertion ordering + caseInsensitive should wrap LinkedHashMap"), // ordering = sorted, caseSensitive = false -> keep TreeMap
        Arguments.of(new HashMap<String, Object>() {

            {
                put(ORDERING, SORTED);
                put(CASE_SENSITIVE, false);
            }
        }, TreeMap.class, null, "Sorted ordering + caseInsensitive should keep TreeMap"), // ordering = unordered, caseSensitive = false -> wrap HashMap
        Arguments.of(new HashMap<String, Object>() {

            {
                put(ORDERING, UNORDERED);
                put(CASE_SENSITIVE, false);
            }
        }, CaseInsensitiveMap.class, HashMap.class, "Unordered + caseInsensitive should wrap HashMap"), // ordering = unordered, caseSensitive = true -> keep HashMap
        Arguments.of(new HashMap<String, Object>() {

            {
                put(ORDERING, UNORDERED);
                put(CASE_SENSITIVE, true);
            }
        }, HashMap.class, null, "Unordered + caseSensitive true should keep HashMap"));
    }

    /**
     * When no options are supplied the method should populate the map with the
     * default values and not throw any exception.
     */
    @Test
    void defaultOptionsAreApplied() {
        Map<String, Object> options = new HashMap<>();
        CompactMap.validateAndFinalizeOptions(options);
        assertEquals(CompactMap.DEFAULT_COMPACT_SIZE, options.get(COMPACT_SIZE));
        assertEquals(CompactMap.DEFAULT_CASE_SENSITIVE, options.get(CASE_SENSITIVE));
        assertEquals(CompactMap.DEFAULT_MAP_TYPE, options.get(MAP_TYPE));
        // ordering defaults to UNORDERED
        assertEquals(UNORDERED, options.get(ORDERING));
    }

    /**
     * compactSize must be >= 2, otherwise an IllegalArgumentException is thrown.
     */
    @Test
    void compactSizeTooSmallThrows() {
        Map<String, Object> options = new HashMap<>();
        // invalid value
        options.put(COMPACT_SIZE, 1);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> CompactMap.validateAndFinalizeOptions(options));
        assertTrue(ex.getMessage().contains("compactSize must be >= 2"));
    }

    /**
     * When a source map is supplied, its ordering must be compatible with the
     * requested ordering.  The static helper methods in {@link MapUtilities}
     * are mocked to control the returned ordering.
     */
    @Test
    void orderingConflictWithSourceMapThrows() {
        Map<String, Object> options = new HashMap<>();
        options.put(ORDERING, UNORDERED);
        // any non‑null map
        options.put(CompactMap.SOURCE_MAP, new HashMap<>());
        try (MockedStatic<MapUtilities> utilities = Mockito.mockStatic(MapUtilities.class)) {
            utilities.when(() -> MapUtilities.detectMapOrdering(Mockito.anyMap())).thenReturn(// source map is sorted
            SORTED);
            utilities.when(() -> MapUtilities.getMapStructureString(Mockito.anyMap())).thenReturn("{sorted map}");
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> CompactMap.validateAndFinalizeOptions(options));
            assertTrue(ex.getMessage().contains("conflicts with source map's ordering"));
            assertTrue(ex.getMessage().contains(SORTED));
        }
    }

    /**
     * When caseSensitive is false and the ordering is neither SORTED nor REVERSE,
     * the method should replace the MAP_TYPE with {@link CaseInsensitiveMap}
     * and store the original map type under INNER_MAP_TYPE.
     */
    @Test
    void caseInsensitiveReplacesMapTypeWhenApplicable() {
        Map<String, Object> options = new HashMap<>();
        options.put(CASE_SENSITIVE, false);
        // not sorted / reverse
        options.put(ORDERING, INSERTION);
        // arbitrary non‑CI map
        options.put(MAP_TYPE, TreeMap.class);
        CompactMap.validateAndFinalizeOptions(options);
        assertEquals(CaseInsensitiveMap.class, options.get(MAP_TYPE));
        assertEquals(TreeMap.class, options.get(INNER_MAP_TYPE));
    }

    /**
     * When caseSensitive is false but the ordering is SORTED, the map type
     * must stay unchanged (the sorted map already handles ordering).
     */
    @Test
    void caseInsensitiveDoesNotReplaceWhenSortedOrdering() {
        Map<String, Object> options = new HashMap<>();
        options.put(CASE_SENSITIVE, false);
        options.put(ORDERING, SORTED);
        options.put(MAP_TYPE, TreeMap.class);
        CompactMap.validateAndFinalizeOptions(options);
        assertEquals(TreeMap.class, options.get(MAP_TYPE));
        assertNull(options.get(INNER_MAP_TYPE));
    }

    /**
     * Parameterized test for map type determination and case‑sensitivity handling.
     * Each argument supplies the initial options map and the expected resulting
     * MAP_TYPE and INNER_MAP_TYPE entries.
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("mapTypeAndCaseSensitivityProvider")
    void mapTypeAndCaseSensitivityHandling(Map<String, Object> inputOptions, Class<?> expectedMapType, Class<?> expectedInnerMapType, String description) {
        // Act
        CompactMap.validateAndFinalizeOptions(inputOptions);
        // Assert
        assertEquals(expectedMapType, inputOptions.get(MAP_TYPE), "MAP_TYPE mismatch");
        if (expectedInnerMapType == null) {
            assertNull(inputOptions.get(INNER_MAP_TYPE), "INNER_MAP_TYPE should be absent");
        } else {
            assertEquals(expectedInnerMapType, inputOptions.get(INNER_MAP_TYPE), "INNER_MAP_TYPE mismatch");
        }
    }

    /**
     * Source map ordering compatibility – successful path.
     * The source map is a TreeMap (detected as SORTED) and the requested ordering
     * matches, so no exception is thrown and MAP_TYPE is set to TreeMap.
     */
    @Test
    void sourceMapOrderingMatchesNoException() {
        Map<String, Object> source = new TreeMap<>();
        source.put("x", 10);
        Map<String, Object> options = new HashMap<>();
        options.put(ORDERING, SORTED);
        options.put(CompactMap.SOURCE_MAP, source);
        try (MockedStatic<MapUtilities> utilities = Mockito.mockStatic(MapUtilities.class)) {
            utilities.when(() -> MapUtilities.detectMapOrdering(source)).thenReturn(SORTED);
            // No need to mock getMapStructureString because no exception is thrown
            CompactMap.validateAndFinalizeOptions(options);
            assertEquals(TreeMap.class, options.get(MAP_TYPE));
            assertNull(options.get(INNER_MAP_TYPE));
        }
    }

    /**
     * Default resolution with {@code putIfAbsent}.
     * When COMPACT_SIZE and CASE_SENSITIVE are omitted the method must insert
     * the default values while preserving any already‑provided options.
     */
    @Test
    void defaultResolutionWithPutIfAbsent() {
        Map<String, Object> options = new HashMap<>();
        // ordering supplied, others omitted
        options.put(ORDERING, UNORDERED);
        CompactMap.validateAndFinalizeOptions(options);
        // Defaults must be added
        assertEquals(CompactMap.DEFAULT_COMPACT_SIZE, options.get(COMPACT_SIZE));
        assertEquals(CompactMap.DEFAULT_CASE_SENSITIVE, options.get(CASE_SENSITIVE));
        // MAP_TYPE is derived from ordering = unordered -> HashMap
        assertEquals(HashMap.class, options.get(MAP_TYPE));
    }

    /**
     * When defaults are already present (compactSize=20, caseSensitive=false) the
     * method must keep those values and still apply the case‑insensitivity
     * transformation.
     */
    @Test
    void defaultsPresentAndCaseInsensitiveTransformation() {
        Map<String, Object> options = new HashMap<>();
        options.put(COMPACT_SIZE, 20);
        options.put(CASE_SENSITIVE, false);
        options.put(ORDERING, UNORDERED);
        CompactMap.validateAndFinalizeOptions(options);
        assertEquals(20, options.get(COMPACT_SIZE));
        assertEquals(false, options.get(CASE_SENSITIVE));
        // unordered + caseInsensitive -> wrap HashMap
        assertEquals(CaseInsensitiveMap.class, options.get(MAP_TYPE));
        assertEquals(HashMap.class, options.get(INNER_MAP_TYPE));
    }

    /**
     * Passing a {@code null} options map should result in a {@link NullPointerException}
     * because the method immediately dereferences the map.
     */
    @Test
    void nullOptionsThrowsNPE() {
        assertThrows(NullPointerException.class, () -> CompactMap.validateAndFinalizeOptions(null));
    }

    /**
     * Invalid option types – ordering is an Integer (should cause ClassCastException).
     */
    @Test
    void invalidOrderingTypeThrowsClassCastException() {
        Map<String, Object> options = new HashMap<>();
        // wrong type
        options.put(ORDERING, 123);
        options.put(COMPACT_SIZE, 10);
        options.put(CASE_SENSITIVE, true);
        assertThrows(ClassCastException.class, () -> CompactMap.validateAndFinalizeOptions(options));
    }

    /**
     * Invalid option types – compactSize is a String (should cause ClassCastException).
     */
    @Test
    void invalidCompactSizeTypeThrowsClassCastException() {
        Map<String, Object> options = new HashMap<>();
        options.put(ORDERING, UNORDERED);
        // wrong type
        options.put(COMPACT_SIZE, "large");
        options.put(CASE_SENSITIVE, true);
        assertThrows(ClassCastException.class, () -> CompactMap.validateAndFinalizeOptions(options));
    }

    /**
     * Invalid option types – caseSensitive is a String (should cause ClassCastException).
     */
    @Test
    void invalidCaseSensitiveTypeThrowsClassCastException() {
        Map<String, Object> options = new HashMap<>();
        options.put(ORDERING, UNORDERED);
        options.put(COMPACT_SIZE, 10);
        // wrong type
        options.put(CASE_SENSITIVE, "yes");
        assertThrows(ClassCastException.class, () -> CompactMap.validateAndFinalizeOptions(options));
    }

    /**
     * Invalid ordering value – a string that is not recognized should cause
     * {@link IllegalArgumentException} from {@code determineMapType}.
     */
    @Test
    void unknownOrderingValueThrowsIllegalArgumentException() {
        Map<String, Object> options = new HashMap<>();
        options.put(ORDERING, "unknownOrdering");
        options.put(COMPACT_SIZE, 10);
        options.put(CASE_SENSITIVE, true);
        assertThrows(IllegalArgumentException.class, () -> CompactMap.validateAndFinalizeOptions(options));
    }

    /**
     * Source map option is not a {@link Map} – casting should fail with
     * {@link ClassCastException}.
     */
    @Test
    void sourceMapNotAMapThrowsClassCastException() {
        Map<String, Object> options = new HashMap<>();
        options.put(ORDERING, UNORDERED);
        // wrong type
        options.put(CompactMap.SOURCE_MAP, "I am not a map");
        assertThrows(ClassCastException.class, () -> CompactMap.validateAndFinalizeOptions(options));
    }

    /**
     * Null ordering value – the key exists but its value is {@code null},
     * leading to a {@link NullPointerException} when the method later calls
     * {@code ordering.equals(...)}.
     */
    @Test
    void nullOrderingValueThrowsNullPointerException() {
        Map<String, Object> options = new HashMap<>();
        // explicit null
        options.put(ORDERING, null);
        options.put(COMPACT_SIZE, 10);
        options.put(CASE_SENSITIVE, true);
        assertThrows(NullPointerException.class, () -> CompactMap.validateAndFinalizeOptions(options));
    }
}
