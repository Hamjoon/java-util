package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.Iterator;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapUtilities_mapOfEntries_Test {

    /**
     * Simple {@link Map.Entry} implementation that throws a {@link NullPointerException}
     * when {@code getKey()} is invoked.
     */
    private static class ThrowingKeyEntry<K, V> implements Map.Entry<K, V> {

        private final K key;

        private final V value;

        ThrowingKeyEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey() {
            throw new NullPointerException("key NPE");
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Simple {@link Map.Entry} implementation that throws an {@link IllegalStateException}
     * when {@code getValue()} is invoked.
     */
    private static class ThrowingValueEntry<K, V> implements Map.Entry<K, V> {

        private final K key;

        private final V value;

        ThrowingValueEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            throw new IllegalStateException("value ISE");
        }

        @Override
        public V setValue(V value) {
            throw new UnsupportedOperationException();
        }
    }

    @Test
    void mapOfEntries_nullOrEmptyArray_returnsEmptyUnmodifiableMap() {
        // null array
        Map<Object, Object> nullResult = MapUtilities.mapOfEntries((Map.Entry<?, ?>[]) null);
        assertNotNull(nullResult);
        assertTrue(nullResult.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> nullResult.put("a", "b"));
        // empty array
        Map<Object, Object> emptyResult = MapUtilities.mapOfEntries();
        assertNotNull(emptyResult);
        assertTrue(emptyResult.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> emptyResult.put("a", "b"));
    }

    @Test
    void mapOfEntries_normalEntries_returnsCorrectMapAndPreservesOrder() {
        Map.Entry<String, Integer> e1 = new AbstractMap.SimpleEntry<>("first", 1);
        Map.Entry<String, Integer> e2 = new AbstractMap.SimpleEntry<>("second", 2);
        Map.Entry<String, Integer> e3 = new AbstractMap.SimpleEntry<>("third", 3);
        Map<String, Integer> result = MapUtilities.mapOfEntries(e1, e2, e3);
        // Verify contents
        assertEquals(3, result.size());
        assertEquals(1, result.get("first"));
        assertEquals(2, result.get("second"));
        assertEquals(3, result.get("third"));
        // Verify order preservation (LinkedHashMap semantics)
        Set<String> keys = result.keySet();
        assertIterableEquals(java.util.List.of("first", "second", "third"), keys);
        // Verify immutability
        assertThrows(UnsupportedOperationException.class, () -> result.put("fourth", 4));
        assertThrows(UnsupportedOperationException.class, () -> result.remove("first"));
    }

    @Test
    void mapOfEntries_nullEntryInArray_throwsNullPointerException() {
        Map.Entry<String, Integer> valid = new AbstractMap.SimpleEntry<>("ok", 1);
        Map.Entry<String, Integer> nullEntry = null;
        NullPointerException ex = assertThrows(NullPointerException.class, () -> MapUtilities.mapOfEntries(valid, nullEntry));
        assertEquals("Entries must not be null.", ex.getMessage());
    }

    @Test
    void mapOfEntries_duplicateKeys_lastOneWins() {
        Map.Entry<String, String> e1 = new AbstractMap.SimpleEntry<>("key", "first");
        Map.Entry<String, String> e2 = new AbstractMap.SimpleEntry<>("key", "second");
        Map<String, String> result = MapUtilities.mapOfEntries(e1, e2);
        assertEquals(1, result.size());
        assertEquals("second", result.get("key"));
    }

    /**
     * Single entry – verifies size, content and immutability.
     */
    @Test
    void mapOfEntries_singleEntry() {
        Map.Entry<String, String> entry = new AbstractMap.SimpleEntry<>("key", "value");
        Map<String, String> result = MapUtilities.mapOfEntries(entry);
        assertEquals(1, result.size());
        assertEquals("value", result.get("key"));
        assertTrue(result.containsKey("key"));
        assertThrows(UnsupportedOperationException.class, () -> result.put("new", "val"));
    }

    /**
     * Multiple distinct entries – checks order and immutability.
     */
    @Test
    void mapOfEntries_multipleEntries_preserveInsertionOrder() {
        Map.Entry<String, Integer> a = new AbstractMap.SimpleEntry<>("a", 1);
        Map.Entry<String, Integer> b = new AbstractMap.SimpleEntry<>("b", 2);
        Map.Entry<String, Integer> c = new AbstractMap.SimpleEntry<>("c", 3);
        Map<String, Integer> result = MapUtilities.mapOfEntries(a, b, c);
        assertEquals(3, result.size());
        assertIterableEquals(java.util.List.of("a", "b", "c"), result.keySet());
        assertEquals(1, result.get("a"));
        assertEquals(2, result.get("b"));
        assertEquals(3, result.get("c"));
        assertThrows(UnsupportedOperationException.class, () -> result.remove("a"));
    }

    /**
     * Null key – LinkedHashMap permits a null key.
     */
    @Test
    void mapOfEntries_nullKey_isAllowed() {
        Map.Entry<String, String> entry = new AbstractMap.SimpleEntry<>(null, "nullKey");
        Map<String, String> result = MapUtilities.mapOfEntries(entry);
        assertEquals(1, result.size());
        assertTrue(result.containsKey(null));
        assertEquals("nullKey", result.get(null));
    }

    /**
     * Null value – LinkedHashMap permits a null value.
     */
    @Test
    void mapOfEntries_nullValue_isAllowed() {
        Map.Entry<String, String> entry = new AbstractMap.SimpleEntry<>("nullVal", null);
        Map<String, String> result = MapUtilities.mapOfEntries(entry);
        assertEquals(1, result.size());
        assertTrue(result.containsKey("nullVal"));
        assertNull(result.get("nullVal"));
    }

    /**
     * Entry that throws NullPointerException when getKey() is called.
     */
    @Test
    void mapOfEntries_entryKeyThrowsException_propagates() {
        Map.Entry<String, String> throwingKeyEntry = new ThrowingKeyEntry<>("k", "v");
        NullPointerException npe = assertThrows(NullPointerException.class, () -> MapUtilities.mapOfEntries(throwingKeyEntry));
        assertEquals("key NPE", npe.getMessage());
    }

    /**
     * Entry that throws IllegalStateException when getValue() is called.
     */
    @Test
    void mapOfEntries_entryValueThrowsException_propagates() {
        Map.Entry<String, String> throwingValueEntry = new ThrowingValueEntry<>("k", "v");
        IllegalStateException ise = assertThrows(IllegalStateException.class, () -> MapUtilities.mapOfEntries(throwingValueEntry));
        assertEquals("value ISE", ise.getMessage());
    }
}
