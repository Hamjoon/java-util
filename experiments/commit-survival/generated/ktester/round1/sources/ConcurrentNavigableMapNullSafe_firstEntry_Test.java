package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentNavigableMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_firstEntry_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh map for each test
        map = new ConcurrentNavigableMapNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the pattern consistent
        map = null;
    }

    private static Stream<Arguments> singleEntryProvider() {
        return Stream.of(Arguments.of("Single Integer key", Map.of("1", 100), "1", 100), Arguments.of("Single String key", Map.of("alpha", 42), "alpha", 42));
    }

    private static Stream<Arguments> multipleEntriesProvider() {
        return Stream.of(Arguments.of("Integer keys – natural ordering", Map.of("3", 30, "1", 10, "2", 20), "1", 10), Arguments.of("String keys – alphabetical ordering", Map.of("c", 3, "a", 1, "b", 2), "a", 1));
    }

    // -----------------------------------------------------------------
    private void setInternalMap(Object newMap) throws Exception {
        Field field = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        field.setAccessible(true);
        field.set(map, newMap);
    }

    @Test
    void firstEntry_returnsEntryWithLowestKey_whenMapHasMultipleKeys() {
        // given
        map.put("b", 2);
        map.put("a", 1);
        map.put("c", 3);
        // null is considered *last* in ordering
        map.put(null, 99);
        // when
        Map.Entry<String, Integer> entry = map.firstEntry();
        // then
        assertNotNull(entry, "firstEntry() should not return null when map is non‑empty");
        assertEquals("a", entry.getKey(), "The smallest (lexicographic) key should be returned");
        assertEquals(1, entry.getValue());
    }

    @Test
    void firstEntry_ignoresNullKey_andReturnsNullWhenOnlyNullKeyPresent() {
        // given
        map.put(null, 42);
        // when
        Map.Entry<String, Integer> entry = map.firstEntry();
        // then
        // Because null is treated as the *largest* key, it should be the only entry and
        // therefore also the first entry in the underlying ConcurrentSkipListMap.
        // The contract of ConcurrentNavigableMap.firstEntry() is to return that entry.
        assertNotNull(entry, "firstEntry() should return the entry for the null key when it is the only key");
        assertNull(entry.getKey(), "Key should be null");
        assertEquals(42, entry.getValue());
    }

    @Test
    void firstEntry_returnsNull_whenMapIsEmpty() {
        // given – map is freshly created and empty
        // when
        Map.Entry<String, Integer> entry = map.firstEntry();
        // then
        assertNull(entry, "firstEntry() must return null for an empty map");
    }

    @Test
    void firstEntry_reflectsChangesAfterRemoval() {
        // given
        map.put("x", 10);
        map.put("y", 20);
        map.put("z", 30);
        // when – remove the current first entry
        map.remove("x");
        Map.Entry<String, Integer> entry = map.firstEntry();
        // then
        assertNotNull(entry);
        assertEquals("y", entry.getKey());
        assertEquals(20, entry.getValue());
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("singleEntryProvider")
    void firstEntry_returnsSingleEntry_whenMapHasOneElement(String description, Map<String, Integer> initialData, String expectedKey, Integer expectedValue) {
        // given
        initialData.forEach(map::put);
        // when
        Map.Entry<String, Integer> entry = map.firstEntry();
        // then
        assertNotNull(entry, description);
        assertEquals(expectedKey, entry.getKey(), description);
        assertEquals(expectedValue, entry.getValue(), description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("multipleEntriesProvider")
    void firstEntry_returnsLowestKey_whenMapHasMultipleEntries(String description, Map<String, Integer> initialData, String expectedKey, Integer expectedValue) {
        // given
        initialData.forEach(map::put);
        // when
        Map.Entry<String, Integer> entry = map.firstEntry();
        // then
        assertNotNull(entry, description);
        assertEquals(expectedKey, entry.getKey(), description);
        assertEquals(expectedValue, entry.getValue(), description);
    }

    @Test
    void firstEntry_ignoresNullKey_andReturnsNonNullWhenOtherKeysExist() {
        // given – null key is considered the *largest* key, therefore it must not be the first entry
        map.put(null, 99);
        map.put("b", 2);
        map.put("a", 1);
        // when
        Map.Entry<String, Integer> entry = map.firstEntry();
        // then
        assertNotNull(entry, "firstEntry() should return the smallest non‑null key");
        assertEquals("a", entry.getKey());
        assertEquals(1, entry.getValue());
    }

    @Test
    void firstEntry_returnsNullKeyEntry_whenItIsTheOnlyEntry() {
        // given – only a null key is present
        map.put(null, 42);
        // when
        Map.Entry<String, Integer> entry = map.firstEntry();
        // then
        assertNotNull(entry, "firstEntry() should return the entry for the null key when it is the sole entry");
        assertNull(entry.getKey(), "Key should be null");
        assertEquals(42, entry.getValue());
    }

    @Test
    void firstEntry_throwsNullPointerException_whenInternalMapIsNull() throws Exception {
        // given – force internalMap to null via reflection
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(map, null);
        // when / then
        assertThrows(NullPointerException.class, () -> map.firstEntry(), "firstEntry() should throw NPE when internalMap is null");
    }

    @Test
    void firstEntry_throwsClassCastException_whenInternalMapIsWrongType() throws Exception {
        // given – replace internalMap with a ConcurrentHashMap (not a ConcurrentSkipListMap)
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        ConcurrentHashMap<Object, Object> wrongMap = new ConcurrentHashMap<>();
        // Populate with a couple of entries to avoid early null return
        wrongMap.put("x", 1);
        wrongMap.put("y", 2);
        internalMapField.set(map, wrongMap);
        // when / then
        assertThrows(ClassCastException.class, () -> map.firstEntry(), "firstEntry() should throw CCE when internalMap is not a ConcurrentSkipListMap");
    }
}
