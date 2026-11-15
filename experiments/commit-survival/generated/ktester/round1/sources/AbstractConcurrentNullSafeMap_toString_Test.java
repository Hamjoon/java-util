package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_toString_Test {

    /**
     * Simple concrete implementation used for most normal tests.
     */
    private static class TestMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        TestMap(ConcurrentMap<Object, Object> internal) {
            super(internal);
        }
    }

    /**
     * Map that returns a pre‑constructed entry set – useful for ordering tests.
     */
    private static class FixedEntrySetMap extends AbstractConcurrentNullSafeMap<Object, Object> {

        private final Set<Entry<Object, Object>> fixedSet;

        FixedEntrySetMap(Set<Entry<Object, Object>> fixedSet) {
            super(new ConcurrentHashMap<>());
            this.fixedSet = fixedSet;
        }

        @Override
        public Set<Entry<Object, Object>> entrySet() {
            return fixedSet;
        }
    }

    /**
     * Map whose entrySet() deliberately returns {@code null}.
     */
    private static class NullEntrySetMap extends AbstractConcurrentNullSafeMap<Object, Object> {

        NullEntrySetMap() {
            super(new ConcurrentHashMap<>());
        }

        @Override
        public Set<Entry<Object, Object>> entrySet() {
            return null;
        }
    }

    /**
     * Map whose entrySet returns a Set whose iterator() returns {@code null}.
     */
    private static class NullIteratorMap extends AbstractConcurrentNullSafeMap<Object, Object> {

        NullIteratorMap() {
            super(new ConcurrentHashMap<>());
        }

        @Override
        public Set<Entry<Object, Object>> entrySet() {
            return new AbstractSet<>() {

                @Override
                public Iterator<Entry<Object, Object>> iterator() {
                    // deliberately broken
                    return null;
                }

                @Override
                public int size() {
                    return 0;
                }
            };
        }
    }

    /**
     * Map whose iterator returns a {@code null} entry.
     */
    private static class NullEntryMap extends AbstractConcurrentNullSafeMap<Object, Object> {

        NullEntryMap() {
            super(new ConcurrentHashMap<>());
        }

        @Override
        public Set<Entry<Object, Object>> entrySet() {
            return new AbstractSet<>() {

                @Override
                public Iterator<Entry<Object, Object>> iterator() {
                    return new Iterator<>() {

                        private boolean done = false;

                        @Override
                        public boolean hasNext() {
                            return !done;
                        }

                        @Override
                        public Entry<Object, Object> next() {
                            if (done)
                                throw new NoSuchElementException();
                            done = true;
                            // null entry
                            return null;
                        }
                    };
                }

                @Override
                public int size() {
                    return 1;
                }
            };
        }
    }

    /**
     * Map whose iterator throws {@link ConcurrentModificationException}.
     */
    private static class CmeIteratorMap extends AbstractConcurrentNullSafeMap<Object, Object> {

        CmeIteratorMap() {
            super(new ConcurrentHashMap<>());
        }

        @Override
        public Set<Entry<Object, Object>> entrySet() {
            return new AbstractSet<>() {

                @Override
                public Iterator<Entry<Object, Object>> iterator() {
                    return new Iterator<>() {

                        @Override
                        public boolean hasNext() {
                            throw new ConcurrentModificationException("simulated");
                        }

                        @Override
                        public Entry<Object, Object> next() {
                            throw new ConcurrentModificationException("simulated");
                        }
                    };
                }

                @Override
                public int size() {
                    return 0;
                }
            };
        }
    }

    @Test
    void toStringEmptyMap() {
        TestMap<String, String> map = new TestMap<>(new ConcurrentHashMap<>());
        assertEquals("{}", map.toString(), "Empty map should be represented as {}");
    }

    @Test
    void toStringSingleEntry() {
        TestMap<String, String> map = new TestMap<>(new ConcurrentHashMap<>());
        map.put("key", "value");
        assertEquals("{key=value}", map.toString(), "Map with a single entry should be formatted as {key=value}");
    }

    @Test
    void toStringMultipleEntriesOrderIndependent() {
        TestMap<String, String> map = new TestMap<>(new ConcurrentHashMap<>());
        map.put("a", "1");
        map.put("b", "2");
        map.put("c", "3");
        String result = map.toString();
        // The exact order is not guaranteed for a ConcurrentHashMap, so we verify the
        // presence of each expected fragment and the overall structure.
        assertTrue(result.startsWith("{") && result.endsWith("}"), "Result should start with '{' and end with '}'");
        assertTrue(result.contains("a=1"));
        assertTrue(result.contains("b=2"));
        assertTrue(result.contains("c=3"));
        // There must be exactly two commas separating the three entries.
        assertEquals(2, result.chars().filter(ch -> ch == ',').count(), "Exactly two commas should separate three entries");
    }

    @Test
    void toStringHandlesNullKeyAndValue() {
        TestMap<String, String> map = new TestMap<>(new ConcurrentHashMap<>());
        map.put(null, null);
        assertEquals("{null=null}", map.toString(), "Null keys and values should be rendered as the string \"null\"");
    }

    @Test
    void toStringSelfReferenceKeyAndValue() {
        // Use Object/Object types so we can insert the map itself as a key/value.
        TestMap<Object, Object> map = new TestMap<>(new ConcurrentHashMap<>());
        map.put(map, "val");
        map.put("key", map);
        // also verify that null handling still works together
        map.put(null, null);
        String result = map.toString();
        // Expected fragments
        String thisMap = "(this Map)";
        assertTrue(result.contains(thisMap + "=val"), "Self‑reference as key should be shown as (this Map)=val");
        assertTrue(result.contains("key=" + thisMap), "Self‑reference as value should be shown as key=(this Map)");
        assertTrue(result.contains("null=null"), "Null entry should still appear correctly");
        // Ensure the string is wrapped correctly.
        assertTrue(result.startsWith("{") && result.endsWith("}"));
    }

    @Test
    void toStringNullKeyWithNormalValue() {
        TestMap<String, String> map = new TestMap<>(new ConcurrentHashMap<>());
        map.put(null, "value1");
        assertEquals("{null=value1}", map.toString(), "A null key paired with a non‑null value must be rendered as \"{null=value}\"");
    }

    @Test
    void toStringNormalKeyWithNullValue() {
        TestMap<String, String> map = new TestMap<>(new ConcurrentHashMap<>());
        map.put("key1", null);
        assertEquals("{key1=null}", map.toString(), "A non‑null key paired with a null value must be rendered as \"{key=null}\"");
    }

    @Test
    void toStringMultipleEntriesIncludingNullsAndSelfReferences() {
        // Preserve insertion order using FixedEntrySetMap
        TestMap<Object, Object> map = new TestMap<>(new ConcurrentHashMap<>());
        // we will not use map.put because order is not guaranteed; instead we build a fixed set
        Set<Entry<Object, Object>> ordered = new LinkedHashSet<>();
        // null key
        ordered.add(new AbstractMap.SimpleEntry<>(null, "value1"));
        // null value
        ordered.add(new AbstractMap.SimpleEntry<>("key2", null));
        // self‑reference key
        ordered.add(new AbstractMap.SimpleEntry<>(map, "value3"));
        // self‑reference value
        ordered.add(new AbstractMap.SimpleEntry<>("key4", map));
        FixedEntrySetMap fixedMap = new FixedEntrySetMap(ordered);
        String result = fixedMap.toString();
        // Expected exact order
        String expected = "{null=value1, key2=null, (this Map)=value3, key4=(this Map)}";
        assertEquals(expected, result, "The string must preserve insertion order and replace self‑references correctly");
    }

    @Test
    void toStringMixedSelfAndNormalEntriesPreserveOrder() {
        // Build the exact order required by the JSON case:
        // {(this Map)=value1, key2=(this Map), key3=value3}
        Set<Entry<Object, Object>> ordered = new LinkedHashSet<>();
        // placeholder map reference will be set after construction
        FixedEntrySetMap placeholder = new FixedEntrySetMap(ordered);
        // self‑reference key
        ordered.add(new AbstractMap.SimpleEntry<>(placeholder, "value1"));
        // self‑reference value
        ordered.add(new AbstractMap.SimpleEntry<>("key2", placeholder));
        // normal entry
        ordered.add(new AbstractMap.SimpleEntry<>("key3", "value3"));
        String result = placeholder.toString();
        String expected = "{(this Map)=value1, key2=(this Map), key3=value3}";
        assertEquals(expected, result, "Mixed self‑reference and normal entries must be rendered in the supplied order");
    }

    @Test
    void toStringIdenticalToStringRepresentationsButNotSelf() {
        // Two distinct objects that both return "same" from toString()
        Object key = new Object() {

            @Override
            public String toString() {
                return "same";
            }
        };
        Object value = new Object() {

            @Override
            public String toString() {
                return "same";
            }
        };
        TestMap<Object, Object> map = new TestMap<>(new ConcurrentHashMap<>());
        map.put(key, value);
        assertEquals("{same=same}", map.toString(), "Objects with identical toString output must be printed normally, not as self‑reference");
    }

    @Test
    void toStringEntrySetReturnsNullThrowsNPE() {
        NullEntrySetMap map = new NullEntrySetMap();
        assertThrows(NullPointerException.class, map::toString, "If entrySet() returns null, toString() should throw NullPointerException");
    }

    @Test
    void toStringIteratorIsNullThrowsNPE() {
        NullIteratorMap map = new NullIteratorMap();
        assertThrows(NullPointerException.class, map::toString, "If iterator() returns null, toString() should throw NullPointerException");
    }

    @Test
    void toStringIteratorReturnsNullEntryThrowsNPE() {
        NullEntryMap map = new NullEntryMap();
        assertThrows(NullPointerException.class, map::toString, "If iterator.next() returns null, toString() should throw NullPointerException");
    }

    @Test
    void toStringKeyToStringThrowsRuntimeException() {
        Object badKey = new Object() {

            @Override
            public String toString() {
                throw new RuntimeException("bad key");
            }
        };
        TestMap<Object, String> map = new TestMap<>(new ConcurrentHashMap<>());
        map.put(badKey, "value");
        RuntimeException ex = assertThrows(RuntimeException.class, map::toString, "Exception from key.toString() must propagate");
        assertEquals("bad key", ex.getMessage());
    }

    @Test
    void toStringValueToStringThrowsRuntimeException() {
        Object badValue = new Object() {

            @Override
            public String toString() {
                throw new RuntimeException("bad value");
            }
        };
        TestMap<String, Object> map = new TestMap<>(new ConcurrentHashMap<>());
        map.put("key", badValue);
        RuntimeException ex = assertThrows(RuntimeException.class, map::toString, "Exception from value.toString() must propagate");
        assertEquals("bad value", ex.getMessage());
    }

    @Test
    void toStringIteratorThrowsConcurrentModificationException() {
        CmeIteratorMap map = new CmeIteratorMap();
        assertThrows(ConcurrentModificationException.class, map::toString, "Iterator throwing CME should let the exception propagate");
    }
}
