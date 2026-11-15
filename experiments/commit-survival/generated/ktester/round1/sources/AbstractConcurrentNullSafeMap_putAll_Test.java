package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_putAll_Test {

    @Mock
    private ConcurrentMap<Object, Object> mockInternalMap;

    private TestMap testMap;

    /**
     * A concrete subclass that lets us control the masking behaviour.
     */
    private static class TestMap extends AbstractConcurrentNullSafeMap<String, String> {

        TestMap(ConcurrentMap<Object, Object> internal) {
            super(internal);
        }

        // Simple sentinel values that are easy to assert against
        @Override
        protected Object maskNullKey(String key) {
            return key == null ? "NULL_KEY_SENTINEL" : key;
        }

        @Override
        protected Object maskNullValue(String value) {
            return value == null ? "NULL_VALUE_SENTINEL" : value;
        }

        // The unmask methods are not needed for this test, but we provide trivial implementations
        @Override
        protected String unmaskNullKey(Object key) {
            return "NULL_KEY_SENTINEL".equals(key) ? null : (String) key;
        }

        @Override
        protected String unmaskNullValue(Object value) {
            return "NULL_VALUE_SENTINEL".equals(value) ? null : (String) value;
        }
    }

    /**
     * Subclass that throws on a specific key during masking – used for exception tests.
     */
    private static class ExceptionMaskingMap extends TestMap {

        ExceptionMaskingMap(ConcurrentMap<Object, Object> internal) {
            super(internal);
        }

        @Override
        protected Object maskNullKey(String key) {
            if ("badKey".equals(key)) {
                throw new RuntimeException("maskNullKey failure");
            }
            return super.maskNullKey(key);
        }

        @Override
        protected Object maskNullValue(String value) {
            if ("badValue".equals(value)) {
                throw new RuntimeException("maskNullValue failure");
            }
            return super.maskNullValue(value);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise the concrete map with the mocked internal map before each test
        testMap = new TestMap(mockInternalMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mock interactions to avoid cross‑test contamination
        reset(mockInternalMap);
    }

    @Test
    void putAll_ShouldDelegateToInternalMap_WithProperMasking() {
        // GIVEN
        Map<String, String> source = new HashMap<>();
        source.put("a", "1");
        source.put(null, "nullKey");
        source.put("nullValue", null);
        // WHEN
        testMap.putAll(source);
        // THEN
        // Verify that internalMap.put was called exactly three times with the expected masked arguments
        InOrder inOrder = inOrder(mockInternalMap);
        inOrder.verify(mockInternalMap).put("a", "1");
        inOrder.verify(mockInternalMap).put("NULL_KEY_SENTINEL", "nullKey");
        inOrder.verify(mockInternalMap).put("nullValue", "NULL_VALUE_SENTINEL");
        verifyNoMoreInteractions(mockInternalMap);
    }

    @Test
    void putAll_WithEmptyMap_ShouldNotInteractWithInternalMap() {
        // GIVEN
        Map<String, String> empty = Collections.emptyMap();
        // WHEN
        testMap.putAll(empty);
        // THEN
        verifyNoInteractions(mockInternalMap);
    }

    @Test
    void putAll_NullMapArgument_ShouldThrowNullPointerException() {
        // GIVEN
        Map<String, String> nullMap = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> testMap.putAll(nullMap));
    }

    @Test
    void putAll_MapWithNullKey_ShouldMaskKey() {
        Map<String, String> map = new HashMap<>();
        map.put(null, "value");
        testMap.putAll(map);
        verify(mockInternalMap).put("NULL_KEY_SENTINEL", "value");
        verifyNoMoreInteractions(mockInternalMap);
    }

    @Test
    void putAll_MapWithNullValue_ShouldMaskValue() {
        Map<String, String> map = new HashMap<>();
        map.put("key", null);
        testMap.putAll(map);
        verify(mockInternalMap).put("key", "NULL_VALUE_SENTINEL");
        verifyNoMoreInteractions(mockInternalMap);
    }

    @Test
    void putAll_MapWithNullKeyAndNullValue_ShouldMaskBoth() {
        Map<String, String> map = new HashMap<>();
        map.put(null, null);
        testMap.putAll(map);
        verify(mockInternalMap).put("NULL_KEY_SENTINEL", "NULL_VALUE_SENTINEL");
        verifyNoMoreInteractions(mockInternalMap);
    }

    @Test
    void putAll_MapWithDuplicateKeys_ShouldKeepLastValue() {
        // A custom Map that returns two entries with the same key.
        Map<String, String> dupKeyMap = new AbstractMap<>() {

            @Override
            public Set<Entry<String, String>> entrySet() {
                Entry<String, String> first = new SimpleEntry<>("dup", "first");
                Entry<String, String> second = new SimpleEntry<>("dup", "second");
                return new LinkedHashSet<>(Arrays.asList(first, second));
            }
        };
        testMap.putAll(dupKeyMap);
        // The second entry overwrites the first – we only verify the final put call.
        InOrder inOrder = inOrder(mockInternalMap);
        inOrder.verify(mockInternalMap).put("dup", "first");
        inOrder.verify(mockInternalMap).put("dup", "second");
        verifyNoMoreInteractions(mockInternalMap);
    }

    @Test
    void putAll_MapWithOverlapAndNulls_ShouldOverwriteExistingEntries() {
        // Use a real internal map to inspect final state.
        ConcurrentMap<Object, Object> realInternal = new ConcurrentHashMap<>();
        // pre‑populate with a non‑null and a null key (masked)
        realInternal.put("a", "1");
        // represents null key
        realInternal.put("NULL_KEY_SENTINEL", "2");
        TestMap realMap = new TestMap(realInternal);
        Map<String, String> incoming = new HashMap<>();
        // overwrite existing non‑null key
        incoming.put("a", "10");
        // overwrite existing null key
        incoming.put(null, "20");
        // new entry
        incoming.put("b", "30");
        realMap.putAll(incoming);
        assertEquals(3, realInternal.size());
        assertEquals("10", realInternal.get("a"));
        assertEquals("20", realInternal.get("NULL_KEY_SENTINEL"));
        assertEquals("30", realInternal.get("b"));
    }

    @Test
    void putAll_MixedNullMap_ShouldMaskAppropriately() {
        Map<String, String> mixed = new HashMap<>();
        // null value
        mixed.put("x", null);
        // null key
        mixed.put(null, "y");
        // normal entry
        mixed.put("z", "zVal");
        testMap.putAll(mixed);
        InOrder inOrder = inOrder(mockInternalMap);
        inOrder.verify(mockInternalMap).put("x", "NULL_VALUE_SENTINEL");
        inOrder.verify(mockInternalMap).put("NULL_KEY_SENTINEL", "y");
        inOrder.verify(mockInternalMap).put("z", "zVal");
        verifyNoMoreInteractions(mockInternalMap);
    }

    @Test
    void putAll_LargeMap1000_ShouldProcessAllEntries() {
        Map<Integer, Integer> large = new LinkedHashMap<>();
        for (int i = 0; i < 1000; i++) {
            large.put(i, i);
        }
        // Use a real internal map to avoid overwhelming the mock verification.
        ConcurrentMap<Object, Object> realInternal = new ConcurrentHashMap<>();
        TestMap realMap = new TestMap(realInternal);
        // The generic types of TestMap are <String,String>, but putAll accepts any <? extends K, ? extends V>.
        // We therefore need a cast – the method does not depend on the runtime type of keys/values.
        @SuppressWarnings("unchecked")
        Map rawMap = large;
        realMap.putAll(rawMap);
        assertEquals(1000, realInternal.size());
        for (int i = 0; i < 1000; i++) {
            assertEquals(i, realInternal.get(i));
        }
    }

    @Test
    void putAll_ExceptionThrowingMap_IteratorThrowsAfterFirstEntry() {
        Map<String, String> badMap = new AbstractMap<>() {

            @Override
            public Set<Entry<String, String>> entrySet() {
                return new AbstractSet<>() {

                    @Override
                    public Iterator<Entry<String, String>> iterator() {
                        return new Iterator<>() {

                            private int count = 0;

                            @Override
                            public boolean hasNext() {
                                return true;
                            }

                            @Override
                            public Entry<String, String> next() {
                                if (count++ == 0) {
                                    return new SimpleEntry<>("first", "1");
                                }
                                throw new RuntimeException("Iterator failure");
                            }
                        };
                    }

                    @Override
                    public int size() {
                        // not used
                        return Integer.MAX_VALUE;
                    }
                };
            }
        };
        // First entry should be put, then the RuntimeException propagates.
        RuntimeException ex = assertThrows(RuntimeException.class, () -> testMap.putAll(badMap));
        assertEquals("Iterator failure", ex.getMessage());
        verify(mockInternalMap).put("first", "1");
        // No further interactions because the iterator failed.
        verifyNoMoreInteractions(mockInternalMap);
    }

    @Test
    void putAll_NullEntrySetMap_ShouldThrowNullPointerException() {
        Map<String, String> nullEntrySetMap = new AbstractMap<>() {

            @Override
            public Set<Entry<String, String>> entrySet() {
                // deliberately broken
                return null;
            }
        };
        assertThrows(NullPointerException.class, () -> testMap.putAll(nullEntrySetMap));
        verifyNoInteractions(mockInternalMap);
    }

    @Test
    void putAll_MapWithNullEntry_ShouldThrowNullPointerException() {
        Map<String, String> mapWithNullEntry = new AbstractMap<>() {

            @Override
            public Set<Entry<String, String>> entrySet() {
                Set<Entry<String, String>> set = new LinkedHashSet<>();
                // null element in the set
                set.add(null);
                return set;
            }
        };
        assertThrows(NullPointerException.class, () -> testMap.putAll(mapWithNullEntry));
        verifyNoInteractions(mockInternalMap);
    }

    @Test
    void putAll_MaskNullKeyThrowsException_ShouldPropagate() {
        ConcurrentMap<Object, Object> realInternal = new ConcurrentHashMap<>();
        ExceptionMaskingMap map = new ExceptionMaskingMap(realInternal);
        Map<String, String> source = new HashMap<>();
        source.put("goodKey", "value");
        // this key triggers exception
        source.put("badKey", "value2");
        // The first entry should be added before the exception occurs.
        RuntimeException ex = assertThrows(RuntimeException.class, () -> map.putAll(source));
        assertEquals("maskNullKey failure", ex.getMessage());
        assertEquals("value", realInternal.get("goodKey"));
        assertFalse(realInternal.containsKey("badKey"));
    }

    @Test
    void putAll_MaskNullValueThrowsException_ShouldPropagate() {
        ConcurrentMap<Object, Object> realInternal = new ConcurrentHashMap<>();
        ExceptionMaskingMap map = new ExceptionMaskingMap(realInternal);
        Map<String, String> source = new HashMap<>();
        source.put("k1", "goodValue");
        // this value triggers exception
        source.put("k2", "badValue");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> map.putAll(source));
        assertEquals("maskNullValue failure", ex.getMessage());
        // Only the first entry should be present.
        assertEquals("goodValue", realInternal.get("k1"));
        assertFalse(realInternal.containsKey("k2"));
    }

    @Test
    void putAll_InternalMapIsNull_ShouldThrowNullPointerException() {
        // Construct a subclass that deliberately passes null to the super constructor.
        AbstractConcurrentNullSafeMap<String, String> mapWithNullInternal = new AbstractConcurrentNullSafeMap<>(null) {

            @Override
            protected Object maskNullKey(String key) {
                return key;
            }

            @Override
            protected Object maskNullValue(String value) {
                return value;
            }

            @Override
            protected String unmaskNullKey(Object key) {
                return (String) key;
            }

            @Override
            protected String unmaskNullValue(Object value) {
                return (String) value;
            }

            @Override
            public Set<Entry<String, String>> entrySet() {
                return Collections.emptySet();
            }
        };
        Map<String, String> source = Map.of("a", "1");
        assertThrows(NullPointerException.class, () -> mapWithNullInternal.putAll(source));
    }

    @Test
    void putAll_InternalMapPutThrowsException_ShouldPropagate() {
        // Configure mock to throw when a specific masked key is used.
        doThrow(new IllegalStateException("boom")).when(mockInternalMap).put(eq("badKey"), any());
        Map<String, String> source = new LinkedHashMap<>();
        source.put("goodKey", "v1");
        // second put will throw
        source.put("badKey", "v2");
        // First entry should be added, then exception propagates.
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> testMap.putAll(source));
        assertEquals("boom", ex.getMessage());
        InOrder inOrder = inOrder(mockInternalMap);
        inOrder.verify(mockInternalMap).put("goodKey", "v1");
        inOrder.verify(mockInternalMap).put("badKey", "v2");
        // No further interactions after the exception.
    }

    @Test
    void putAll_EntrySetIteratorThrowsConcurrentModificationException_ShouldPropagate() {
        Map<String, String> concurrentMap = new AbstractMap<>() {

            @Override
            public Set<Entry<String, String>> entrySet() {
                return new AbstractSet<>() {

                    @Override
                    public Iterator<Entry<String, String>> iterator() {
                        return new Iterator<>() {

                            private int count = 0;

                            @Override
                            public boolean hasNext() {
                                return count < 2;
                            }

                            @Override
                            public Entry<String, String> next() {
                                if (count++ == 0) {
                                    return new SimpleEntry<>("first", "1");
                                }
                                throw new ConcurrentModificationException("CMOD");
                            }
                        };
                    }

                    @Override
                    public int size() {
                        return 2;
                    }
                };
            }
        };
        ConcurrentModificationException ex = assertThrows(ConcurrentModificationException.class, () -> testMap.putAll(concurrentMap));
        assertEquals("CMOD", ex.getMessage());
        // Only the first entry should have been forwarded.
        verify(mockInternalMap).put("first", "1");
        verifyNoMoreInteractions(mockInternalMap);
    }
}
