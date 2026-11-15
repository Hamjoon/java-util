package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

class TrackingMap_toString_Test {

    private Map<String, Integer> backingMap;

    private TrackingMap<String, Integer> trackingMap;

    private static class ArgumentsHolder {

        final Map<String, Integer> map;

        final String expected;

        final String description;

        ArgumentsHolder(Map<String, Integer> map, String expected, String description) {
            this.map = map;
            this.expected = expected;
            this.description = description;
        }
    }

    class KeyObj {

        @Override
        public String toString() {
            return "KeyObj";
        }
    }

    class ValueObj {

        @Override
        public String toString() {
            return "ValueObj";
        }
    }

    class BadMap<K, V> extends HashMap<K, V> {

        @Override
        public String toString() {
            throw new IllegalStateException("bad");
        }
    }

    class KeyObj {

        @Override
        public String toString() {
            return "KeyObj";
        }
    }

    class ValueObj {

        @Override
        public String toString() {
            return "ValueObj";
        }
    }

    class BadMap<K, V> extends HashMap<K, V> {

        @Override
        public String toString() {
            throw new IllegalStateException("bad");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh backing map for each test.
        backingMap = new HashMap<>();
        trackingMap = new TrackingMap<>(backingMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC and avoid cross‑test contamination.
        backingMap.clear();
        backingMap = null;
        trackingMap = null;
    }

    private static Stream<ArgumentsHolder> mapContentProvider() {
        return Stream.of(new ArgumentsHolder(Collections.emptyMap(), "{}", "Empty internal map"), new ArgumentsHolder(Collections.singletonMap("key1", 1), "{key1=1}", "Single entry"), new ArgumentsHolder(new LinkedHashMap<String, Integer>() {

            {
                put("first", 1);
                put("second", 2);
                put("third", 3);
            }
        }, "{first=1, second=2, third=3}", "Order preservation with LinkedHashMap"));
    }

    @Test
    void toStringReturnsWrappedMapString_whenMapHasEntries() {
        // given
        backingMap.put("one", 1);
        backingMap.put("two", 2);
        // when
        String result = trackingMap.toString();
        // then
        assertEquals(backingMap.toString(), result, "TrackingMap.toString() should delegate to the internal map's toString()");
    }

    @Test
    void toStringReturnsWrappedMapString_whenMapIsEmpty() {
        // given – backingMap is already empty
        // when
        String result = trackingMap.toString();
        // then
        assertEquals(backingMap.toString(), result, "TrackingMap.toString() should correctly handle an empty internal map");
    }

    @Test
    void constructorThrowsIllegalArgumentException_whenMapIsNull() {
        // given
        Map<String, Integer> nullMap = null;
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new TrackingMap<>(nullMap));
        assertEquals("Cannot construct a TrackingMap() with null", ex.getMessage(), "Constructor should reject a null backing map with a clear message");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("mapContentProvider")
    void toStringMatchesUnderlyingMap(ArgumentsHolder holder) {
        // replace backing map with the one from the holder
        trackingMap = new TrackingMap<>(holder.map);
        assertEquals(holder.expected, trackingMap.toString(), "toString() should exactly match the underlying map's representation");
    }

    @Test
    void toStringHandlesNullKeyAndNullValue() {
        Map<Object, Object> map = new HashMap<>();
        map.put(null, null);
        TrackingMap<Object, Object> tm = new TrackingMap<>(map);
        assertEquals("{null=null}", tm.toString(), "Map containing null key and null value should render as {null=null}");
    }

    @Test
    void toStringReflectsDynamicModificationAfterConstruction() {
        // start empty
        assertEquals("{}", trackingMap.toString());
        // modify via TrackingMap.put
        trackingMap.put("x", 10);
        assertEquals("{x=10}", trackingMap.toString(), "Modification after construction must be reflected");
    }

    @Test
    void toStringReflectsRemoval() {
        backingMap.put("a", 1);
        backingMap.put("b", 2);
        trackingMap.remove("a");
        assertEquals("{b=2}", trackingMap.toString(), "Removal should be reflected in toString()");
    }

    @Test
    void toStringReflectsClear() {
        backingMap.put("a", 1);
        backingMap.put("b", 2);
        trackingMap.clear();
        assertEquals("{}", trackingMap.toString(), "clear() should result in empty representation");
    }

    @Test
    void toStringReflectsPutAll() {
        Map<String, Integer> source = new HashMap<>();
        source.put("x", 10);
        source.put("y", 20);
        source.put("z", 30);
        trackingMap.putAll(source);
        String result = trackingMap.toString();
        // order is undefined – verify each fragment is present and braces are correct
        assertTrue(result.startsWith("{") && result.endsWith("}"), "Result must be wrapped in braces");
        for (String entry : List.of("x=10", "y=20", "z=30")) {
            assertTrue(result.contains(entry), "Result should contain entry " + entry);
        }
    }

    @Test
    void toStringUsesCustomKeyAndValueToString() {
        class KeyObj {

            @Override
            public String toString() {
                return "KeyObj";
            }
        }
        class ValueObj {

            @Override
            public String toString() {
                return "ValueObj";
            }
        }
        Map<Object, Object> map = new HashMap<>();
        map.put(new KeyObj(), new ValueObj());
        TrackingMap<Object, Object> tm = new TrackingMap<>(map);
        assertEquals("{KeyObj=ValueObj}", tm.toString(), "Custom toString implementations must be used");
    }

    @Test
    void toStringHandlesSelfReference() {
        // HashMap prints "(this Map)" for a self‑reference
        Map<String, Object> map = new HashMap<>();
        TrackingMap<String, Object> tm = new TrackingMap<>(map);
        map.put("self", tm);
        String result = tm.toString();
        assertTrue(result.contains("(this Map)"), "Self‑reference should be rendered as (this Map)");
    }

    @Test
    void toStringThrowsNullPointerException_whenInternalMapIsNull() throws Exception {
        // Construct a normal instance then force internalMap to null via reflection
        TrackingMap<String, Integer> tm = new TrackingMap<>(new HashMap<>());
        Field f = TrackingMap.class.getDeclaredField("internalMap");
        f.setAccessible(true);
        f.set(tm, null);
        assertThrows(NullPointerException.class, tm::toString, "Calling toString() with a null internal map should throw NPE");
    }

    @Test
    void toStringPropagatesUncheckedExceptionFromInternalMap() {
        class BadMap<K, V> extends HashMap<K, V> {

            @Override
            public String toString() {
                throw new IllegalStateException("bad");
            }
        }
        BadMap<String, Integer> bad = new BadMap<>();
        bad.put("a", 1);
        TrackingMap<String, Integer> tm = new TrackingMap<>(bad);
        IllegalStateException ex = assertThrows(IllegalStateException.class, tm::toString);
        assertEquals("bad", ex.getMessage(), "Exception from internal map's toString must propagate");
    }

    @Test
    void toStringThrowsConcurrentModificationException_whenMapModifiesDuringToString() {
        // Anonymous subclass that modifies itself while building the string
        Map<String, Integer> tricky = new HashMap<String, Integer>() {

            @Override
            public String toString() {
                // trigger iteration that will be interfered with
                Iterator<Entry<String, Integer>> it = entrySet().iterator();
                while (it.hasNext()) {
                    Entry<String, Integer> e = it.next();
                    // remove the entry during iteration -> CME on next call
                    remove(e.getKey());
                }
                // delegate to super for final representation (won't be reached)
                return super.toString();
            }
        };
        tricky.put("a", 1);
        tricky.put("b", 2);
        TrackingMap<String, Integer> tm = new TrackingMap<>(tricky);
        assertThrows(ConcurrentModificationException.class, tm::toString, "Modification during toString should result in CME");
    }
}
