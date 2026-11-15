package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_firstKey_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh map for each test to avoid cross‑test contamination
        map = new ConcurrentNavigableMapNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map (not strictly necessary because a new instance is created each time)
        map.clear();
    }

    private static Stream<Arguments> basicFirstKeyProvider() {
        return Stream.of(// only a null key – null is considered a valid (masked) entry and should be returned
        Arguments.of(Collections.singletonMap(null, 42), null, "Only a null key present – firstKey() should return null"), // single non‑null entry
        Arguments.of(Collections.singletonMap("a", 1), "a", "Single entry with key \"a\" – firstKey() should return \"a\""), // multiple keys – natural ordering should pick the smallest
        Arguments.of(new LinkedHashMap<String, Integer>() {

            {
                put("b", 2);
                put("a", 1);
                put("c", 3);
            }
        }, "a", "Multiple keys {\"b\",\"a\",\"c\"} – firstKey() should return the smallest key \"a\""));
    }

    /* -------------------------------------------------------------
     *  Helper method – uses reflection to set the protected internalMap field
     * ------------------------------------------------------------- */
    private static void setInternalMap(ConcurrentNavigableMapNullSafe<?, ?> target, Object newMap) throws Exception {
        Field f = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        f.setAccessible(true);
        f.set(target, newMap);
    }

    @Test
    void firstKeyReturnsLowestNonNullKey() {
        // Arrange – insert keys in non‑sorted order, including a null key
        map.put("b", 2);
        map.put("a", 1);
        map.put(null, 99);
        map.put("c", 3);
        // Act
        String first = map.firstKey();
        // Assert – the smallest natural‑order key ("a") is returned, null is ignored
        assertEquals("a", first);
    }

    @Test
    void firstKeyWhenOnlyNullKeyPresentReturnsNull() {
        // Arrange – map contains only a null key
        map.put(null, 42);
        // Act
        String first = map.firstKey();
        // Assert – null is considered the only element, so it is returned
        assertNull(first);
    }

    @Test
    void firstKeyOnEmptyMapThrowsNoSuchElementException() {
        // Act & Assert – an empty map should behave like ConcurrentSkipListMap and throw
        assertThrows(NoSuchElementException.class, () -> map.firstKey(), "Empty map must throw NoSuchElementException");
    }

    /* -------------------------------------------------------------
     *  Basic functionality tests (null handling, natural ordering)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("basicFirstKeyProvider")
    void firstKeyBasicFunctionality(Map<String, Integer> input, String expected, String description) {
        // Arrange
        input.forEach(map::put);
        // Act
        String actual = map.firstKey();
        // Assert
        assertEquals(expected, actual, description);
    }

    /* -------------------------------------------------------------
     *  Custom comparator ordering (reverse order)
     * ------------------------------------------------------------- */
    @Test
    void firstKeyWithCustomComparatorReturnsAccordingToComparator() throws Exception {
        // Create a ConcurrentSkipListMap with a reverse‑order comparator and inject it
        ConcurrentSkipListMap<Object, Object> reverseMap = new ConcurrentSkipListMap<>(Comparator.reverseOrder());
        setInternalMap(map, reverseMap);
        // Insert keys in natural order; due to reverse comparator the “smallest” is actually "c"
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // Act
        String first = map.firstKey();
        // Assert – with reverse ordering the first key is the greatest natural key
        assertEquals("c", first, "Reverse comparator should make \"c\" the first key");
    }

    /* -------------------------------------------------------------
     *  internalMap is null – should throw NullPointerException
     * ------------------------------------------------------------- */
    @Test
    void firstKeyWhenInternalMapIsNullThrowsNullPointerException() throws Exception {
        // Nullify the internal map via reflection
        setInternalMap(map, null);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> map.firstKey(), "Null internalMap must cause NullPointerException");
    }

    /* -------------------------------------------------------------
     *  internalMap of wrong type – should throw ClassCastException
     * ------------------------------------------------------------- */
    @Test
    void firstKeyWhenInternalMapWrongTypeThrowsClassCastException() throws Exception {
        // Inject a plain HashMap (not a ConcurrentSkipListMap) into the internalMap field
        setInternalMap(map, new HashMap<>());
        // Act & Assert
        assertThrows(ClassCastException.class, () -> map.firstKey(), "Wrong internalMap type must cause ClassCastException");
    }

    /* -------------------------------------------------------------
     *  Non‑comparable key without a comparator – should throw ClassCastException
     * ------------------------------------------------------------- */
    @Test
    void firstKeyWithNonComparableKeyThrowsClassCastException() {
        // Use raw types to bypass generic compile‑time checks
        @SuppressWarnings("unchecked")
        ConcurrentNavigableMapNullSafe<Object, Integer> rawMap = new ConcurrentNavigableMapNullSafe<>();
        // Insert a key that does not implement Comparable
        Object nonComparableKey = new Object();
        rawMap.put(nonComparableKey, 1);
        // Act & Assert – the underlying ConcurrentSkipListMap cannot order the key
        assertThrows(ClassCastException.class, rawMap::firstKey, "Non‑comparable key without comparator must cause ClassCastException");
    }
}
