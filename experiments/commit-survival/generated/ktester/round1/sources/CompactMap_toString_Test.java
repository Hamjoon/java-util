package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.util.*;
import java.util.concurrent.ConcurrentModificationException;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompactMap_toString_Test {

    class ModifyingCompactMap<K, V> extends CompactMap<K, V> {

        private boolean modified = false;

        @Override
        public Set<Entry<K, V>> entrySet() {
            Set<Entry<K, V>> original = super.entrySet();
            // Wrap the original iterator to inject a modification
            return new AbstractSet<Entry<K, V>>() {

                @Override
                public Iterator<Entry<K, V>> iterator() {
                    Iterator<Entry<K, V>> it = original.iterator();
                    return new Iterator<Entry<K, V>>() {

                        @Override
                        public boolean hasNext() {
                            return it.hasNext();
                        }

                        @Override
                        public Entry<K, V> next() {
                            if (!modified) {
                                // First element: modify the map (structural change)
                                put((K) "newKey", (V) "newValue");
                                modified = true;
                            }
                            return it.next();
                        }
                    };
                }

                @Override
                public int size() {
                    return original.size();
                }
            };
        }
    }

    class BadKey {

        @Override
        public String toString() {
            throw new RuntimeException("Key failure");
        }
    }

    class BadValue {

        @Override
        public String toString() {
            throw new RuntimeException("Value failure");
        }
    }

    class NullEntrySetMap<K, V> extends CompactMap<K, V> {

        @Override
        public Set<Entry<K, V>> entrySet() {
            // deliberately break the contract
            return null;
        }
    }

    class MockedCompactMap extends CompactMap<String, Integer> {

        @Override
        public Set<Entry<String, Integer>> entrySet() {
            return mockSet;
        }
    }

    /**
     * Simple custom object with a deterministic toString() implementation.
     */
    private static class CustomObject {

        @Override
        public String toString() {
            return "CustomObject";
        }
    }

    class ModifyingCompactMap<K, V> extends CompactMap<K, V> {

        private boolean modified = false;

        @Override
        public Set<Entry<K, V>> entrySet() {
            Set<Entry<K, V>> original = super.entrySet();
            // Wrap the original iterator to inject a modification
            return new AbstractSet<Entry<K, V>>() {

                @Override
                public Iterator<Entry<K, V>> iterator() {
                    Iterator<Entry<K, V>> it = original.iterator();
                    return new Iterator<Entry<K, V>>() {

                        @Override
                        public boolean hasNext() {
                            return it.hasNext();
                        }

                        @Override
                        public Entry<K, V> next() {
                            if (!modified) {
                                // First element: modify the map (structural change)
                                put((K) "newKey", (V) "newValue");
                                modified = true;
                            }
                            return it.next();
                        }
                    };
                }

                @Override
                public int size() {
                    return original.size();
                }
            };
        }
    }

    class BadKey {

        @Override
        public String toString() {
            throw new RuntimeException("Key failure");
        }
    }

    class BadValue {

        @Override
        public String toString() {
            throw new RuntimeException("Value failure");
        }
    }

    class NullEntrySetMap<K, V> extends CompactMap<K, V> {

        @Override
        public Set<Entry<K, V>> entrySet() {
            // deliberately break the contract
            return null;
        }
    }

    class MockedCompactMap extends CompactMap<String, Integer> {

        @Override
        public Set<Entry<String, Integer>> entrySet() {
            return mockSet;
        }
    }

    /**
     * Helper to build a CompactMap from a list of key/value pairs.
     */
    private static <K, V> CompactMap<K, V> buildMap(List<Map.Entry<K, V>> entries) {
        CompactMap<K, V> map = new CompactMap<>();
        for (Map.Entry<K, V> e : entries) {
            map.put(e.getKey(), e.getValue());
        }
        return map;
    }

    /**
     * Data for null‑key / null‑value test.
     */
    static Stream<Arguments> nullKeyValueProvider() {
        return Stream.of(Arguments.of(List.of(new AbstractMap.SimpleEntry<>(null, "nullValue"), new AbstractMap.SimpleEntry<>("nullKey", null), new AbstractMap.SimpleEntry<>(null, null)), "{null=nullValue, nullKey=null, null=null}"));
    }

    /**
     * Data for mixed‑type and custom toString test.
     */
    static Stream<Arguments> mixedTypesProvider() {
        return Stream.of(Arguments.of(List.of(new AbstractMap.SimpleEntry<>("obj", new CustomObject()), new AbstractMap.SimpleEntry<>("list", Arrays.asList(1, 2, 3))), "{obj=CustomObject, list=[1, 2, 3]}"));
    }

    /**
     * Data for self‑reference both as key and as value.
     */
    static Stream<Arguments> selfReferenceBothProvider() {
        return Stream.of(Arguments.of(// map will contain a single entry where both key and value are the map itself
        // placeholder, will be replaced inside test
        (CompactMap<String, String>) null, "{(this Map)=(this Map)}"));
    }

    @Test
    void emptyMapReturnsBraces() {
        CompactMap<String, Integer> map = new CompactMap<>();
        assertEquals("{}", map.toString(), "Empty CompactMap should be represented as {}");
    }

    @Test
    void singleEntryIsFormattedCorrectly() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("a", 1);
        assertEquals("{a=1}", map.toString(), "Single entry map should be formatted as {key=value}");
    }

    @Test
    void multipleEntriesContainAllPairs() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("a", 1);
        map.put("b", 2);
        String result = map.toString();
        // Basic structural checks
        assertTrue(result.startsWith("{") && result.endsWith("}"), "Result should start with '{' and end with '}'");
        // Both pairs must be present, order is not guaranteed
        assertTrue(result.contains("a=1"), "Result should contain the pair a=1");
        assertTrue(result.contains("b=2"), "Result should contain the pair b=2");
        // If both pairs are present they must be separated by ", "
        assertTrue(result.contains(", "), "Pairs should be separated by \", \"");
    }

    @Test
    void selfReferenceAsKeyIsPrintedAsThisMap() {
        CompactMap<Object, String> map = new CompactMap<>();
        map.put(map, "value");
        assertEquals("{(this Map)=value}", map.toString(), "Self‑reference as key should be rendered as (this Map)");
    }

    @Test
    void selfReferenceAsValueIsPrintedAsThisMap() {
        CompactMap<String, Object> map = new CompactMap<>();
        map.put("key", map);
        assertEquals("{key=(this Map)}", map.toString(), "Self‑reference as value should be rendered as (this Map)");
    }

    @ParameterizedTest(name = "null key/value handling")
    @MethodSource("nullKeyValueProvider")
    void nullKeyAndValueAreRenderedAsLiteralNull(List<Map.Entry<Object, Object>> entries, String expected) {
        CompactMap<Object, Object> map = buildMap(entries);
        assertEquals(expected, map.toString(), "null keys and values must be rendered as the literal string \"null\"");
    }

    @ParameterizedTest(name = "mixed types with custom toString")
    @MethodSource("mixedTypesProvider")
    void mixedTypesAndCustomToStringAreRenderedCorrectly(List<Map.Entry<String, Object>> entries, String expected) {
        CompactMap<String, Object> map = buildMap(entries);
        assertEquals(expected, map.toString(), "Objects with their own toString() should be included verbatim; collections use their standard representation");
    }

    @ParameterizedTest(name = "self‑reference as both key and value")
    @MethodSource("selfReferenceBothProvider")
    void selfReferenceBothKeyAndValue(CompactMap<String, String> placeholder, String expected) {
        CompactMap<String, String> map = new CompactMap<>();
        // put the map as both key and value via raw types to bypass generic checks
        @SuppressWarnings("unchecked")
        CompactMap raw = map;
        raw.put(map, map);
        assertEquals(expected, map.toString(), "When the map appears as both key and value it should be rendered as (this Map) for each side");
    }

    /**
     * Test that a ConcurrentModificationException propagates when the map is modified during iteration.
     */
    @Test
    void toStringThrowsConcurrentModificationExceptionWhenModifiedDuringIteration() {
        // Use a subclass that modifies itself on the first call to iterator().next()
        class ModifyingCompactMap<K, V> extends CompactMap<K, V> {

            private boolean modified = false;

            @Override
            public Set<Entry<K, V>> entrySet() {
                Set<Entry<K, V>> original = super.entrySet();
                // Wrap the original iterator to inject a modification
                return new AbstractSet<Entry<K, V>>() {

                    @Override
                    public Iterator<Entry<K, V>> iterator() {
                        Iterator<Entry<K, V>> it = original.iterator();
                        return new Iterator<Entry<K, V>>() {

                            @Override
                            public boolean hasNext() {
                                return it.hasNext();
                            }

                            @Override
                            public Entry<K, V> next() {
                                if (!modified) {
                                    // First element: modify the map (structural change)
                                    put((K) "newKey", (V) "newValue");
                                    modified = true;
                                }
                                return it.next();
                            }
                        };
                    }

                    @Override
                    public int size() {
                        return original.size();
                    }
                };
            }
        }
        ModifyingCompactMap<String, Integer> map = new ModifyingCompactMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertThrows(ConcurrentModificationException.class, map::toString, "Modifying the map while toString() iterates must cause CME");
    }

    /**
     * Test that a RuntimeException thrown from a key's toString() propagates.
     */
    @Test
    void keyToStringThrowsRuntimeExceptionIsPropagated() {
        class BadKey {

            @Override
            public String toString() {
                throw new RuntimeException("Key failure");
            }
        }
        CompactMap<Object, String> map = new CompactMap<>();
        map.put(new BadKey(), "value");
        RuntimeException ex = assertThrows(RuntimeException.class, map::toString, "RuntimeException from key.toString() should propagate");
        assertEquals("Key failure", ex.getMessage());
    }

    /**
     * Test that a RuntimeException thrown from a value's toString() propagates.
     */
    @Test
    void valueToStringThrowsRuntimeExceptionIsPropagated() {
        class BadValue {

            @Override
            public String toString() {
                throw new RuntimeException("Value failure");
            }
        }
        CompactMap<String, Object> map = new CompactMap<>();
        map.put("key", new BadValue());
        RuntimeException ex = assertThrows(RuntimeException.class, map::toString, "RuntimeException from value.toString() should propagate");
        assertEquals("Value failure", ex.getMessage());
    }

    /**
     * Test that a NullPointerException is thrown when entrySet() returns null (simulated via subclass).
     */
    @Test
    void entrySetReturnsNullLeadsToNullPointerException() {
        class NullEntrySetMap<K, V> extends CompactMap<K, V> {

            @Override
            public Set<Entry<K, V>> entrySet() {
                // deliberately break the contract
                return null;
            }
        }
        CompactMap<String, Integer> map = new NullEntrySetMap<>();
        // put works because underlying storage is still functional
        map.put("a", 1);
        assertThrows(NullPointerException.class, map::toString, "Calling toString() when entrySet() returns null must throw NPE");
    }

    /**
     * Test that an iterator that directly throws CME also propagates.
     */
    @Test
    void iteratorThrowsConcurrentModificationExceptionIsPropagated(@Mock Set<Map.Entry<String, Integer>> mockSet, @Mock Iterator<Map.Entry<String, Integer>> mockIter) {
        // Configure mock iterator to throw CME on first call to hasNext()
        when(mockSet.iterator()).thenReturn(mockIter);
        when(mockIter.hasNext()).thenThrow(new ConcurrentModificationException("Iterator CME"));
        // Subclass CompactMap to return the mocked set
        class MockedCompactMap extends CompactMap<String, Integer> {

            @Override
            public Set<Entry<String, Integer>> entrySet() {
                return mockSet;
            }
        }
        CompactMap<String, Integer> map = new MockedCompactMap();
        // ensure map is non‑empty (not strictly required for this test)
        map.put("x", 0);
        assertThrows(ConcurrentModificationException.class, map::toString, "Iterator throwing CME should be propagated by toString()");
    }
}
