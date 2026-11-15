package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.concurrent.ConcurrentNavigableMap;
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
class ConcurrentNavigableMapNullSafe_lastEntry_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    class ThrowingWrapMap extends ConcurrentNavigableMapNullSafe<String, Integer> {

        @Override
        protected Entry<String, Integer> wrapEntry(Entry<Object, Object> internalEntry) {
            throw new IllegalStateException("wrapEntry failure");
        }
    }

    class ThrowingWrapMap extends ConcurrentNavigableMapNullSafe<String, Integer> {

        @Override
        protected Entry<String, Integer> wrapEntry(Entry<Object, Object> internalEntry) {
            throw new IllegalStateException("wrapEntry failure");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh map for each test
        map = new ConcurrentNavigableMapNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to help GC (not strictly necessary)
        map.clear();
    }

    /* -------------------------------------------------------------
     *  Parameterized tests generated from the JSON description
     * ------------------------------------------------------------- */
    private static Stream<Arguments> provideLastEntryScenarios() {
        return Stream.of(// empty map → null
        Arguments.of(Collections.emptyMap(), null, "When the underlying ConcurrentSkipListMap is empty, lastEntry() should return null because there is no last element."), // single entry
        Arguments.of(Map.of("apple", 1), Map.entry("apple", 1), "When the map contains a single entry (key='apple', value=1), lastEntry() should return that entry."), // multiple entries – natural ordering
        Arguments.of(Map.of("apple", 1, "monkey", 2, "zebra", 3), Map.entry("zebra", 3), "When the map contains multiple entries with natural ordering of keys, lastEntry() should return the entry with the greatest key ('zebra' > 'monkey' > 'apple')."), // null key considered highest
        Arguments.of(Map.of(null, 99, "a", 1, "b", 2), Map.entry(null, 99), "When the map contains a null key (masked internally) and other non‑null keys, lastEntry() should treat the masked null as the highest possible key and return the entry wrapping the null key with its value."));
    }

    @Test
    void lastEntry_returnsHighestKeyEntry() {
        // given
        map.put("a", 1);
        map.put("c", 3);
        map.put("b", 2);
        // when
        Map.Entry<String, Integer> last = map.lastEntry();
        // then
        assertNotNull(last, "lastEntry should not be null when map is non‑empty");
        assertEquals("c", last.getKey(), "The highest key should be 'c'");
        assertEquals(3, last.getValue());
    }

    @Test
    void lastEntry_considersNullKeyAsLast() {
        // given
        map.put("a", 1);
        map.put(null, 99);
        map.put("b", 2);
        // when
        Map.Entry<String, Integer> last = map.lastEntry();
        // then
        assertNotNull(last);
        assertNull(last.getKey(), "Null key is considered the last entry");
        assertEquals(99, last.getValue());
    }

    @Test
    void lastEntry_returnsNullWhenMapIsEmpty() {
        // given an empty map
        // when
        Map.Entry<String, Integer> last = map.lastEntry();
        // then
        assertNull(last, "lastEntry should return null for an empty map");
    }

    @Test
    void lastEntry_afterRemovingHighestKeyUpdatesCorrectly() {
        // given
        map.put("x", 10);
        map.put("y", 20);
        map.put("z", 30);
        // null is highest
        map.put(null, 40);
        // when
        // remove the null key which is the highest
        map.remove(null);
        Map.Entry<String, Integer> last = map.lastEntry();
        // then
        assertNotNull(last);
        assertEquals("z", last.getKey(), "After removing null, 'z' becomes the highest key");
        assertEquals(30, last.getValue());
    }

    @Test
    void lastEntry_isConsistentWithPollLastEntry() {
        // given
        map.put("one", 1);
        map.put("two", 2);
        map.put(null, 99);
        // when
        Map.Entry<String, Integer> lastViaLastEntry = map.lastEntry();
        Map.Entry<String, Integer> lastViaPoll = map.pollLastEntry();
        // then
        assertNotNull(lastViaLastEntry);
        assertNotNull(lastViaPoll);
        assertEquals(lastViaLastEntry.getKey(), lastViaPoll.getKey());
        assertEquals(lastViaLastEntry.getValue(), lastViaPoll.getValue());
        // map should now be missing the previously last entry
        assertNull(map.lastEntry(), "Map should be empty after polling the only entry");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("provideLastEntryScenarios")
    void lastEntry_parameterizedTests(Map<String, Integer> initialData, Map.Entry<String, Integer> expected, String description) {
        // populate the map according to the scenario
        initialData.forEach(map::put);
        Map.Entry<String, Integer> actual = map.lastEntry();
        if (expected == null) {
            assertNull(actual, description);
        } else {
            assertNotNull(actual, description);
            assertEquals(expected.getKey(), actual.getKey(), description);
            assertEquals(expected.getValue(), actual.getValue(), description);
        }
    }

    @Test
    void lastEntry_propagatesConcurrentModificationException() throws Exception {
        // Simulate a concurrent modification by mocking the internal ConcurrentSkipListMap
        ConcurrentSkipListMap<Object, Object> mockSkipList = (ConcurrentSkipListMap<Object, Object>) java.lang.reflect.Proxy.newProxyInstance(ConcurrentSkipListMap.class.getClassLoader(), new Class[] { ConcurrentSkipListMap.class }, (proxy, method, args) -> {
            if ("lastEntry".equals(method.getName())) {
                // The real map would throw CME if modified concurrently; we simulate it directly
                throw new ConcurrentModificationException("simulated");
            }
            // delegate other calls to a real map to avoid NPEs
            return method.invoke(new ConcurrentSkipListMap<>(), args);
        });
        // Inject the mock into the protected field 'internalMap' of the superclass
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(map, mockSkipList);
        // Verify that the exception bubbles up
        assertThrows(ConcurrentModificationException.class, map::lastEntry, "ConcurrentModificationException should be propagated from the underlying map");
    }

    @Test
    void lastEntry_throwsNullPointerExceptionWhenInternalMapIsNull() throws Exception {
        // Force internalMap to null via reflection
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(map, null);
        // The cast inside lastEntry will cause NPE
        assertThrows(NullPointerException.class, map::lastEntry, "When internalMap is null, lastEntry() should throw NullPointerException");
    }

    @Test
    void lastEntry_propagatesExceptionFromComparator() {
        // Comparator that always throws IllegalArgumentException
        Comparator<String> badComparator = (o1, o2) -> {
            throw new IllegalArgumentException("Comparator failure");
        };
        // Construct a map with the bad comparator
        ConcurrentNavigableMapNullSafe<String, Integer> badMap = new ConcurrentNavigableMapNullSafe<>();
        // Replace internal map with a skip list built with the bad comparator
        ConcurrentSkipListMap<Object, Object> badSkipList = new ConcurrentSkipListMap<>(badComparator);
        try {
            Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
            internalMapField.setAccessible(true);
            internalMapField.set(badMap, badSkipList);
        } catch (Exception e) {
            fail("Reflection setup failed: " + e);
        }
        // Adding entries will trigger the comparator during insertion
        assertThrows(IllegalArgumentException.class, () -> badMap.put("a", 1), "Putting a key should already fail due to comparator, but we still test lastEntry propagation.");
        // Even if the map somehow contains entries, calling lastEntry must propagate the exception
        assertThrows(IllegalArgumentException.class, badMap::lastEntry, "IllegalArgumentException from comparator should be propagated by lastEntry()");
    }

    @Test
    void lastEntry_propagatesExceptionFromWrapEntry() throws Exception {
        // Subclass that overrides wrapEntry to always throw
        class ThrowingWrapMap extends ConcurrentNavigableMapNullSafe<String, Integer> {

            @Override
            protected Entry<String, Integer> wrapEntry(Entry<Object, Object> internalEntry) {
                throw new IllegalStateException("wrapEntry failure");
            }
        }
        ThrowingWrapMap throwingMap = new ThrowingWrapMap();
        throwingMap.put("x", 10);
        // ensure there is a non‑null entry for wrapEntry to be called
        throwingMap.put(null, 99);
        // The call to lastEntry should hit the overridden wrapEntry and throw
        IllegalStateException ex = assertThrows(IllegalStateException.class, throwingMap::lastEntry, "wrapEntry exception must be propagated");
        assertEquals("wrapEntry failure", ex.getMessage());
    }

    @Test
    void lastEntry_nullKeyOnly_returnsEntryWithNullKey() {
        map.put(null, 42);
        Map.Entry<String, Integer> last = map.lastEntry();
        assertNotNull(last, "Entry should not be null when map contains only a null key");
        assertNull(last.getKey(), "Key must be null");
        assertEquals(42, last.getValue(), "Value must match the stored value");
    }
}
