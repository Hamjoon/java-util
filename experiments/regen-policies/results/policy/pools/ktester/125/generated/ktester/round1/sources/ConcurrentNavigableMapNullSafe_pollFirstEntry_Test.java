package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.Map;
import java.util.concurrent.ConcurrentNavigableMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.AbstractMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;

class ConcurrentNavigableMapNullSafe_pollFirstEntry_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    private static class ArgumentsHolder {

        final Map<Integer, String> input;

        final Map.Entry<Integer, String> expected;

        final String description;

        ArgumentsHolder(Map<Integer, String> input, Map.Entry<Integer, String> expected, String description) {
            this.input = input;
            this.expected = expected;
            this.description = description;
        }
    }

    class ExplodingMap extends ConcurrentNavigableMapNullSafe<String, Integer> {

        @Override
        protected String unmaskNullKey(Object maskedKey) {
            throw new RuntimeException("forced failure inside wrapEntry");
        }
    }

    class ExplodingMap extends ConcurrentNavigableMapNullSafe<String, Integer> {

        @Override
        protected String unmaskNullKey(Object maskedKey) {
            throw new RuntimeException("forced failure inside wrapEntry");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh map for each test
        map = new ConcurrentNavigableMapNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to help GC; not strictly required
        map.clear();
    }

    private static Stream<ArgumentsHolder> nonEmptyMapProvider() {
        return Stream.of(// single entry case
        new ArgumentsHolder(Map.of(1, "one"), new AbstractMap.SimpleEntry<>(1, "one"), "Map contains a single entry (key=1, value=\"one\")."), // multiple entries case – natural ordering of Integer keys selects 3
        new ArgumentsHolder(Map.of(5, "five", 3, "three", 9, "nine"), new AbstractMap.SimpleEntry<>(3, "three"), "Map contains keys 5, 3, 9 – the smallest key is 3."));
    }

    @Test
    void pollFirstEntryReturnsLowestKeyEntry() {
        // given
        map.put("b", 2);
        map.put("a", 1);
        map.put("c", 3);
        // null is considered last, should not be returned
        map.put(null, 99);
        // when
        Map.Entry<String, Integer> first = map.pollFirstEntry();
        // then
        assertNotNull(first, "pollFirstEntry should return an entry when map is non‑empty");
        assertEquals("a", first.getKey(), "The smallest key should be returned");
        assertEquals(1, first.getValue());
        // the map should now have three entries left, without the polled one
        assertEquals(3, map.size());
        assertFalse(map.containsKey("a"));
        // null key must still be present
        assertTrue(map.containsKey(null));
    }

    @Test
    void pollFirstEntryReturnsNullWhenMapIsEmpty() {
        // given an empty map
        // when
        Map.Entry<String, Integer> first = map.pollFirstEntry();
        // then
        assertNull(first, "pollFirstEntry should return null for an empty map");
    }

    @Test
    void pollFirstEntryHandlesOnlyNullKey() {
        // given a map that contains only a null key
        map.put(null, 42);
        // when
        Map.Entry<String, Integer> first = map.pollFirstEntry();
        // then
        // According to the class contract, null keys are considered last in the ascending view,
        // therefore pollFirstEntry should treat the null key as the only entry and return it.
        assertNotNull(first, "pollFirstEntry should return the null‑key entry when it is the only entry");
        assertNull(first.getKey(), "Key of the returned entry should be null");
        assertEquals(42, first.getValue());
        // map should now be empty
        assertTrue(map.isEmpty());
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("nonEmptyMapProvider")
    void pollFirstEntryNonEmptyMap(ArgumentsHolder holder) {
        // populate the map using the supplied key/value pairs
        // convert to String/Integer for our map type
        holder.input.forEach((k, v) -> map.put(k.toString(), v.length()));
        // when
        Map.Entry<String, Integer> entry = map.pollFirstEntry();
        // then
        assertNotNull(entry, "Entry must not be null");
        // convert expected key/value back to the map's generic types
        assertEquals(holder.expected.getKey().toString(), entry.getKey(), "Returned key should be the smallest according to natural ordering");
        assertEquals(holder.expected.getValue().length(), entry.getValue(), "Returned value should correspond to the expected entry");
        // ensure the entry has been removed
        assertFalse(map.containsKey(entry.getKey()));
    }

    @Test
    void pollFirstEntryThrowsWhenInternalMapIsNull() throws Exception {
        // Force the protected field 'internalMap' to null via reflection
        Field f = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        f.setAccessible(true);
        f.set(map, null);
        // The method should attempt to cast null to ConcurrentSkipListMap and throw NPE
        assertThrows(NullPointerException.class, map::pollFirstEntry, "Calling pollFirstEntry with a null internalMap must throw NullPointerException");
    }

    @Test
    void pollFirstEntryThrowsWhenInternalMapIsWrongType() throws Exception {
        // Replace internalMap with a ConcurrentMap that is NOT a ConcurrentSkipListMap
        ConcurrentMap<Object, Object> wrongMap = mock(ConcurrentMap.class);
        Field f = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        f.setAccessible(true);
        f.set(map, wrongMap);
        // The cast inside pollFirstEntry should fail, resulting in ClassCastException
        assertThrows(ClassCastException.class, map::pollFirstEntry, "Calling pollFirstEntry with an internalMap of the wrong type must throw ClassCastException");
    }

    @Test
    void pollFirstEntryPropagatesExceptionFromWrapEntry() throws Exception {
        // Create a real ConcurrentSkipListMap with a single entry
        ConcurrentSkipListMap<Object, Object> realMap = new ConcurrentSkipListMap<>();
        realMap.put("key", 123);
        // Inject the real map into the instance
        Field f = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        f.setAccessible(true);
        f.set(map, realMap);
        // Subclass that forces wrapEntry (indirectly) to throw by overriding a protected helper used inside wrapEntry
        class ExplodingMap extends ConcurrentNavigableMapNullSafe<String, Integer> {

            @Override
            protected String unmaskNullKey(Object maskedKey) {
                throw new RuntimeException("forced failure inside wrapEntry");
            }
        }
        ExplodingMap explodingMap = new ExplodingMap();
        // Inject the same real internal map into the subclass instance
        f.set(explodingMap, realMap);
        // The call should propagate the RuntimeException we forced
        RuntimeException ex = assertThrows(RuntimeException.class, explodingMap::pollFirstEntry, "Exception thrown inside wrapEntry must propagate to the caller");
        assertEquals("forced failure inside wrapEntry", ex.getMessage());
    }
}
