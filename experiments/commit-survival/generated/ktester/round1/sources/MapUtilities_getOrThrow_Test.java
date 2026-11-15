package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class MapUtilities_getOrThrow_Test {

    /**
     * A {@link Map} that throws {@link IllegalStateException} from {@code containsKey}.
     */
    private static class FaultyContainsMap extends HashMap<Object, Object> {

        @Override
        public boolean containsKey(Object key) {
            throw new IllegalStateException("containsKey failure");
        }
    }

    /**
     * A {@link Map} that reports the key as present but throws {@link IllegalArgumentException}
     * from {@code get}.
     */
    private static class FaultyGetMap extends HashMap<Object, Object> {

        @Override
        public boolean containsKey(Object key) {
            // pretend the key exists
            return true;
        }

        @Override
        public Object get(Object key) {
            throw new IllegalArgumentException("get failure");
        }
    }

    /**
     * Simple key class with proper {@code equals} and {@code hashCode}.
     */
    private static final class Key {

        private final int id;

        Key(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof Key) && ((Key) o).id == this.id;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(id);
        }
    }

    @Test
    void getOrThrow_returnsValueWhenKeyExists() throws Exception {
        // GIVEN
        Map<String, Integer> map = new HashMap<>();
        map.put("answer", 42);
        IllegalStateException throwable = new IllegalStateException("should not be thrown");
        // WHEN
        Object result = MapUtilities.getOrThrow(map, "answer", throwable);
        // THEN
        assertEquals(42, result);
    }

    @Test
    void getOrThrow_throwsProvidedThrowableWhenKeyMissing() {
        // GIVEN
        Map<String, String> map = new HashMap<>();
        map.put("present", "value");
        RuntimeException expected = new RuntimeException("key not found");
        // WHEN / THEN
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> MapUtilities.getOrThrow(map, "absent", expected));
        assertSame(expected, thrown);
    }

    @Test
    void getOrThrow_throwsNullPointerExceptionWhenMapIsNull() {
        // GIVEN
        Map<?, ?> nullMap = null;
        IllegalArgumentException throwable = new IllegalArgumentException("dummy");
        // WHEN / THEN
        NullPointerException npe = assertThrows(NullPointerException.class, () -> MapUtilities.getOrThrow(nullMap, "anyKey", throwable));
        assertEquals("Map parameter cannot be null", npe.getMessage());
    }

    @Test
    void getOrThrow_throwsNullPointerExceptionWhenThrowableIsNull() {
        // GIVEN
        Map<String, String> map = new HashMap<>();
        map.put("k", "v");
        Throwable nullThrowable = null;
        // WHEN / THEN
        NullPointerException npe = assertThrows(NullPointerException.class, () -> MapUtilities.getOrThrow(map, "k", nullThrowable));
        assertEquals("Throwable object cannot be null", npe.getMessage());
    }

    @Test
    void getOrThrow_allowsNullKeyIfMapContainsIt() throws Exception {
        // GIVEN
        Map<Object, String> map = new HashMap<>();
        map.put(null, "nullKeyValue");
        IllegalStateException throwable = new IllegalStateException("should not be thrown");
        // WHEN
        Object result = MapUtilities.getOrThrow(map, null, throwable);
        // THEN
        assertEquals("nullKeyValue", result);
    }

    @Test
    void getOrThrow_throwsProvidedThrowableWhenKeyIsNullAndNotPresent() {
        // GIVEN
        Map<String, String> map = new HashMap<>();
        map.put("a", "b");
        IllegalArgumentException expected = new IllegalArgumentException("null key missing");
        // WHEN / THEN
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> MapUtilities.getOrThrow(map, null, expected));
        assertSame(expected, thrown);
    }

    @Test
    void getOrThrow_throwsNullPointerExceptionWhenMapIsNull_json() {
        // GIVEN
        Map<?, ?> map = null;
        IllegalArgumentException throwable = new IllegalArgumentException("missing");
        // WHEN / THEN
        NullPointerException npe = assertThrows(NullPointerException.class, () -> MapUtilities.getOrThrow(map, "anyKey", throwable));
        assertEquals("Map parameter cannot be null", npe.getMessage());
    }

    @Test
    void getOrThrow_throwsNullPointerExceptionWhenThrowableIsNull_json() {
        // GIVEN
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        IllegalArgumentException throwable = null;
        // WHEN / THEN
        NullPointerException npe = assertThrows(NullPointerException.class, () -> MapUtilities.getOrThrow(map, "anyKey", throwable));
        assertEquals("Throwable object cannot be null", npe.getMessage());
    }

    @Test
    void getOrThrow_returnsValueWhenKeyPresent_json() throws Exception {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put("foo", "bar");
        map.put("baz", 42);
        IllegalStateException throwable = new IllegalStateException("not found");
        // WHEN
        Object result = MapUtilities.getOrThrow(map, "baz", throwable);
        // THEN
        assertEquals(42, result);
    }

    @Test
    void getOrThrow_throwsProvidedThrowableWhenKeyAbsent_json() {
        // GIVEN
        Map<String, String> map = new HashMap<>();
        map.put("foo", "bar");
        IllegalArgumentException throwable = new IllegalArgumentException("key not present");
        // WHEN / THEN
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> MapUtilities.getOrThrow(map, "missingKey", throwable));
        assertSame(throwable, thrown);
    }

    @Test
    void getOrThrow_returnsNullWhenValueIsNull_json() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put("nullValueKey", null);
        RuntimeException throwable = new RuntimeException("should not be thrown");
        // WHEN
        Object result = MapUtilities.getOrThrow(map, "nullValueKey", throwable);
        // THEN
        assertNull(result);
    }

    @Test
    void getOrThrow_throwsProvidedThrowableWhenKeyTypeMismatch_json() {
        // GIVEN
        Map<Integer, String> map = new HashMap<>();
        map.put(1, "one");
        IllegalArgumentException throwable = new IllegalArgumentException("key not found");
        // WHEN / THEN
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> MapUtilities.getOrThrow(map, 1L, throwable));
        assertSame(throwable, thrown);
    }

    @Test
    void getOrThrow_returnsValueForEqualCustomKey_json() throws Exception {
        // GIVEN
        Map<Key, String> map = new HashMap<>();
        map.put(new Key(5), "five");
        RuntimeException throwable = new RuntimeException("should not be thrown");
        // WHEN
        Object result = MapUtilities.getOrThrow(map, new Key(5), throwable);
        // THEN
        assertEquals("five", result);
    }

    @Test
    void getOrThrow_returnsValueFromUnmodifiableMap_json() throws Exception {
        // GIVEN
        Map<String, Integer> mutable = new HashMap<>();
        mutable.put("a", 10);
        Map<String, Integer> unmodifiable = Collections.unmodifiableMap(mutable);
        IllegalArgumentException throwable = new IllegalArgumentException("missing");
        // WHEN
        Object result = MapUtilities.getOrThrow(unmodifiable, "a", throwable);
        // THEN
        assertEquals(10, result);
    }

    @Test
    void getOrThrow_throwsProvidedThrowableWhenKeyAbsentInEmptyMap_json() {
        // GIVEN
        Map<String, Integer> empty = Collections.emptyMap();
        IllegalStateException throwable = new IllegalStateException("empty map");
        // WHEN / THEN
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> MapUtilities.getOrThrow(empty, "anyKey", throwable));
        assertSame(throwable, thrown);
    }

    @Test
    void getOrThrow_propagatesExceptionFromContainsKey() {
        // GIVEN
        Map<Object, Object> map = new FaultyContainsMap();
        RuntimeException throwable = new RuntimeException("should not be thrown");
        // WHEN / THEN
        IllegalStateException ise = assertThrows(IllegalStateException.class, () -> MapUtilities.getOrThrow(map, "anyKey", throwable));
        assertEquals("containsKey failure", ise.getMessage());
    }

    @Test
    void getOrThrow_propagatesExceptionFromGet() {
        // GIVEN
        Map<Object, Object> map = new FaultyGetMap();
        RuntimeException throwable = new RuntimeException("should not be thrown");
        // WHEN / THEN
        IllegalArgumentException iae = assertThrows(IllegalArgumentException.class, () -> MapUtilities.getOrThrow(map, "present", throwable));
        assertEquals("get failure", iae.getMessage());
    }

    @Test
    void getOrThrow_propagatesNullPointerExceptionFromTreeMapNullKey() {
        // GIVEN
        TreeMap<Integer, String> treeMap = new TreeMap<>();
        treeMap.put(1, "one");
        IllegalArgumentException throwable = new IllegalArgumentException("missing");
        // WHEN / THEN
        NullPointerException npe = assertThrows(NullPointerException.class, () -> MapUtilities.getOrThrow(treeMap, null, throwable));
        // The exact message depends on the JDK implementation; we only assert the type.
        assertNotNull(npe.getMessage());
    }

    @Test
    void getOrThrow_propagatesClassCastExceptionFromTreeMapKeyTypeMismatch() {
        // GIVEN
        TreeMap<Integer, String> treeMap = new TreeMap<>();
        treeMap.put(1, "one");
        IllegalArgumentException throwable = new IllegalArgumentException("missing");
        // WHEN / THEN
        ClassCastException cce = assertThrows(ClassCastException.class, () -> MapUtilities.getOrThrow(treeMap, "notAnInteger", throwable));
        assertNotNull(cce.getMessage());
    }

    @Test
    void getOrThrow_throwsErrorInstanceWhenKeyAbsent() {
        // GIVEN
        Map<String, Integer> empty = Collections.emptyMap();
        AssertionError error = new AssertionError("assert error");
        // WHEN / THEN
        AssertionError thrown = assertThrows(AssertionError.class, () -> MapUtilities.getOrThrow(empty, "anyKey", error));
        assertSame(error, thrown);
    }
}
