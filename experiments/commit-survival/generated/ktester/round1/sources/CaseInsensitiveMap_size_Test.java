package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveMap_size_Test {

    private CaseInsensitiveMap<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        // By default use the no‑arg constructor for each test.
        map = new CaseInsensitiveMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference to help GC; not strictly required.
        map = null;
    }

    /**
     * Replaces the private {@code map} field inside {@link CaseInsensitiveMap}
     * with the supplied {@code newMap}. This is used to simulate different
     * underlying implementations and error‑throwing maps without needing a
     * dedicated constructor.
     *
     * @param ciMap   the CaseInsensitiveMap instance under test
     * @param newMap  the map to inject (may be {@code null})
     * @throws Exception if reflection fails
     */
    private static void setUnderlyingMap(CaseInsensitiveMap<?, ?> ciMap, Map<?, ?> newMap) throws Exception {
        Field f = CaseInsensitiveMap.class.getDeclaredField("map");
        f.setAccessible(true);
        f.set(ciMap, newMap);
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> basicSizeScenarios() {
        return Stream.of(Arguments.of(List.of(), 0, "empty map"), Arguments.of(List.of("single"), 1, "single entry"), Arguments.of(List.of("a", "b", "c", "d", "e"), 5, "five distinct entries"));
    }

    @Test
    void size_EmptyMap_ReturnsZero() {
        // given: a freshly created map (already done in @BeforeEach)
        // when:
        int actualSize = map.size();
        // then:
        assertEquals(0, actualSize, "Size of a newly created map should be 0");
    }

    @Test
    void size_AfterAddingElements_ReturnsCorrectCount() {
        // given:
        map.put("One", 1);
        map.put("Two", 2);
        map.put("Three", 3);
        // when:
        int actualSize = map.size();
        // then:
        assertEquals(3, actualSize, "Size should reflect number of distinct keys");
    }

    @Test
    void size_CaseInsensitiveKeyInsertion_UpdatesExistingEntry() {
        // given: two keys that differ only by case
        map.put("Key", 10);
        // should replace the previous entry
        map.put("key", 20);
        // when:
        int actualSize = map.size();
        // then:
        assertEquals(1, actualSize, "Case‑insensitive keys must be treated as the same entry");
        assertEquals(20, map.get("KEY"), "Value should be the one from the last insertion");
    }

    @Test
    void size_ConstructorWithInitialCapacity_ReturnsZero() {
        // given:
        CaseInsensitiveMap<String, Integer> capMap = new CaseInsensitiveMap<>(16);
        // when:
        int actualSize = capMap.size();
        // then:
        assertEquals(0, actualSize, "Map created with initial capacity should start empty");
    }

    @Test
    void size_ConstructorWithInitialCapacityAndLoadFactor_ReturnsZero() {
        // given:
        CaseInsensitiveMap<String, Integer> capMap = new CaseInsensitiveMap<>(16, 0.75f);
        // when:
        int actualSize = capMap.size();
        // then:
        assertEquals(0, actualSize, "Map created with capacity and load factor should start empty");
    }

    @Test
    void size_ConstructorFromExistingMap_CopiesSizeCorrectly() {
        // given: a source map with several entries (including case‑variant keys)
        Map<String, Integer> source = new LinkedHashMap<>();
        source.put("Alpha", 1);
        source.put("Beta", 2);
        source.put("gamma", 3);
        // duplicate key in different case – will be merged by CI map
        source.put("GAMMA", 4);
        // when:
        CaseInsensitiveMap<String, Integer> ciMap = new CaseInsensitiveMap<>(source);
        // then:
        // source size is 4, but CI map should treat "gamma"/"GAMMA" as one key → size 3
        assertEquals(3, ciMap.size(), "Constructor should copy entries respecting case‑insensitive semantics");
    }

    @Test
    void size_ConstructorWithSourceAndCustomMapInstance_CopiesSizeCorrectly() {
        // given: a source map
        Map<String, Integer> source = new HashMap<>();
        source.put("One", 1);
        source.put("Two", 2);
        source.put("Three", 3);
        // when: we ask the CI map to use a TreeMap as its backing store
        CaseInsensitiveMap<String, Integer> ciMap = new CaseInsensitiveMap<>(source, new TreeMap<>());
        // then:
        assertEquals(3, ciMap.size(), "Size should equal the number of entries copied from the source map");
    }

    // -----------------------------------------------------------------
    @Test
    void size_AfterClear_ReturnsZero() {
        map.put("A", 1);
        map.put("B", 2);
        map.clear();
        assertEquals(0, map.size(), "Calling clear() should reset size to 0");
    }

    @Test
    void size_AfterRemovingTwoKeys_FromFiveEntries_ReturnsThree() {
        map.put("k1", 1);
        map.put("k2", 2);
        map.put("k3", 3);
        map.put("k4", 4);
        map.put("k5", 5);
        map.remove("k1");
        map.remove("k2");
        assertEquals(3, map.size(), "Size should be 3 after removing two of five entries");
    }

    @Test
    void size_AfterPutAll_WithFourEntries_ReturnsFour() {
        Map<String, Integer> src = new HashMap<>();
        src.put("a", 1);
        src.put("b", 2);
        src.put("c", 3);
        src.put("d", 4);
        map.putAll(src);
        assertEquals(4, map.size(), "putAll of four distinct entries should result in size 4");
    }

    // -----------------------------------------------------------------
    @Test
    void size_UnderlyingConcurrentHashMap_Empty_ReturnsZero() throws Exception {
        // Replace the delegate with a ConcurrentHashMap via reflection
        setUnderlyingMap(map, new ConcurrentHashMap<>());
        assertEquals(0, map.size(), "Empty ConcurrentHashMap should still report size 0");
    }

    @Test
    void size_UnderlyingTreeMap_TwoEntries_ReturnsTwo() throws Exception {
        setUnderlyingMap(map, new TreeMap<>());
        map.put("first", 1);
        map.put("second", 2);
        assertEquals(2, map.size(), "TreeMap with two entries must report size 2");
    }

    @Test
    void size_UnderlyingLinkedHashMap_ThreeEntries_ReturnsThree() throws Exception {
        setUnderlyingMap(map, new LinkedHashMap<>());
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        assertEquals(3, map.size(), "LinkedHashMap should preserve order but size must be 3");
    }

    @Test
    void size_UnderlyingWeakHashMap_TwoEntries_ReturnsTwo() throws Exception {
        setUnderlyingMap(map, new WeakHashMap<>());
        map.put(new String("k1"), 10);
        map.put(new String("k2"), 20);
        assertEquals(2, map.size(), "WeakHashMap size after insertion must be 2 (GC not forced)");
    }

    @Test
    void size_UnderlyingConcurrentSkipListMap_FourEntries_ReturnsFour() throws Exception {
        setUnderlyingMap(map, new ConcurrentSkipListMap<>());
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);
        assertEquals(4, map.size(), "ConcurrentSkipListMap should report correct size");
    }

    // -----------------------------------------------------------------
    @Test
    void size_PutNullKey_NonNullValue_ReturnsOne() {
        map.put(null, 42);
        assertEquals(1, map.size(), "Null key with non‑null value should count as an entry");
    }

    @Test
    void size_PutNullKey_And_NonNullKey_ReturnsTwo() {
        map.put(null, 1);
        map.put("notNull", 2);
        assertEquals(2, map.size(), "Distinct null and non‑null keys should both be counted");
    }

    @Test
    void size_PutNonNullKey_NullValue_ReturnsOne() {
        map.put("key", null);
        assertEquals(1, map.size(), "Entry with null value still counts toward size");
    }

    // -----------------------------------------------------------------
    @Test
    void size_RemoveAbsentKey_SizeUnchanged() {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.remove("nonexistent");
        assertEquals(3, map.size(), "Removing a non‑existent key must not change size");
    }

    @Test
    void size_RemoveNullKey_FromEmptyMap_ReturnsZero() {
        map.remove(null);
        assertEquals(0, map.size(), "Removing null from empty map leaves size 0");
    }

    // -----------------------------------------------------------------
    @Test
    void size_putAll_OverlappingKeys_ResultingSizeThree() {
        map.put("one", 1);
        map.put("two", 2);
        Map<String, Integer> src = new HashMap<>();
        // should replace existing "two"
        src.put("Two", 22);
        src.put("three", 3);
        map.putAll(src);
        assertEquals(3, map.size(), "Overlapping case‑insensitive key should replace, not add");
    }

    @Test
    void size_putAll_DuplicateCaseKeys_ThenAddAnother_ReturnsTwo() {
        Map<String, Integer> src1 = new HashMap<>();
        src1.put("A", 1);
        // overwrites previous
        src1.put("a", 2);
        map.putAll(src1);
        assertEquals(1, map.size(), "Duplicate case‑insensitive keys collapse to one entry");
        Map<String, Integer> src2 = new HashMap<>();
        src2.put("B", 3);
        map.putAll(src2);
        assertEquals(2, map.size(), "Adding a distinct key after collapse yields total size 2");
    }

    // -----------------------------------------------------------------
    @Test
    void size_UnderlyingMapIsNull_ThrowsNPE() throws Exception {
        setUnderlyingMap(map, null);
        assertThrows(NullPointerException.class, map::size, "Calling size() when delegate map is null must throw NPE");
    }

    @Test
    void size_UnderlyingMapThrowsUnsupportedOperationException_Propagates() throws Exception {
        Map<String, Integer> badMap = new AbstractMap<>() {

            @Override
            public Set<Entry<String, Integer>> entrySet() {
                return Collections.emptySet();
            }

            @Override
            public int size() {
                throw new UnsupportedOperationException("size not supported");
            }
        };
        setUnderlyingMap(map, badMap);
        assertThrows(UnsupportedOperationException.class, map::size, "Exception from delegate size() must be propagated");
    }

    @Test
    void size_UnderlyingMapThrowsIllegalStateException_Propagates() throws Exception {
        Map<String, Integer> badMap = new AbstractMap<>() {

            @Override
            public Set<Entry<String, Integer>> entrySet() {
                return Collections.emptySet();
            }

            @Override
            public int size() {
                throw new IllegalStateException("illegal state");
            }
        };
        setUnderlyingMap(map, badMap);
        assertThrows(IllegalStateException.class, map::size, "IllegalStateException from delegate must be propagated");
    }

    @ParameterizedTest(name = "size after adding {2}")
    @MethodSource("basicSizeScenarios")
    void size_BasicParameterizedTest(List<String> keys, int expectedSize, String description) {
        for (int i = 0; i < keys.size(); i++) {
            map.put(keys.get(i), i);
        }
        assertEquals(expectedSize, map.size(), "Size after inserting " + description + " should be " + expectedSize);
    }
}
