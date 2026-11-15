package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_unmaskNullKey_Test {

    private TestMap map;

    /**
     * Simple concrete subclass that makes the protected methods visible for testing.
     */
    private static class TestMap extends ConcurrentNavigableMapNullSafe<String, Integer> {

        /**
         * Exposes {@code unmaskNullKey} as a public method for the test suite.
         */
        public String callUnmaskNullKey(Object maskedKey) {
            return super.unmaskNullKey(maskedKey);
        }

        /**
         * Exposes {@code maskNullKey} so the sentinel can be obtained without reflection.
         */
        public Object callMaskNullKey(String key) {
            return super.maskNullKey(key);
        }
    }

    /**
     * Generic concrete subclass that exposes the protected methods for testing.
     */
    private static class GenericTestMap<K, V> extends ConcurrentNavigableMapNullSafe<K, V> {

        /**
         * Exposes {@code unmaskNullKey} as a public method.
         */
        public K callUnmaskNullKey(Object maskedKey) {
            return super.unmaskNullKey(maskedKey);
        }

        /**
         * Exposes {@code maskNullKey} so the sentinel can be obtained without reflection.
         */
        public Object callMaskNullKey(K key) {
            return super.maskNullKey(key);
        }
    }

    /**
     * Simple custom class used in a few test cases.
     */
    private static class CustomObject {

        final String name;

        CustomObject(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof CustomObject) && Objects.equals(name, ((CustomObject) o).name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        @Override
        public String toString() {
            return "CustomObject{" + name + '}';
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance before each test to avoid cross‑test contamination.
        map = new TestMap();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up after each test.
        map = null;
    }

    static Stream<Arguments> sentinelProvider() {
        GenericTestMap<String, Integer> map = new GenericTestMap<>();
        // the real sentinel
        Object sentinel = map.callMaskNullKey(null);
        // new String with same content
        Object sentinelCopy = new String(sentinel.toString());
        return Stream.of(// direct sentinel
        Arguments.of(sentinel), // new instance, same content
        Arguments.of(sentinelCopy));
    }

    static Stream<Arguments> regularKeyProvider() {
        return Stream.of(// String
        Arguments.of("testKey"), // Integer
        Arguments.of(42), // null (explicit cast for readability)
        Arguments.of((Object) null));
    }

    @Test
    void unmaskNullKey_returnsNull_whenSentinelProvided() {
        // Obtain the sentinel that represents a null key via the protected mask method.
        Object sentinel = map.callMaskNullKey(null);
        // The sentinel should not be null itself.
        assertNotNull(sentinel, "Sentinel for null key must not be null");
        // When the sentinel is passed to unmaskNullKey, the result must be null.
        String result = map.callUnmaskNullKey(sentinel);
        assertNull(result, "unmaskNullKey should return null for the sentinel value");
    }

    @Test
    void unmaskNullKey_returnsOriginalKey_whenNonSentinelProvided() {
        String originalKey = "sampleKey";
        // Directly pass a regular key (not the sentinel) to unmaskNullKey.
        String result = map.callUnmaskNullKey(originalKey);
        // The method should return the same key instance (or an equal value).
        assertEquals(originalKey, result, "unmaskNullKey should return the original key when it is not the sentinel");
    }

    @Test
    void unmaskNullKey_handlesArbitraryObjectTypesGracefully() {
        // Although the map is typed <String, Integer>, the protected method accepts Object.
        // Passing an Integer should result in a ClassCastException when the cast to K occurs.
        Object masked = 12345;
        // Verify that the cast exception is thrown, confirming the generic cast behaviour.
        assertThrows(ClassCastException.class, () -> map.callUnmaskNullKey(masked));
    }

    /**
     * Sentinel handling – also checks that a new String instance with the same content works.
     */
    @ParameterizedTest(name = "[{index}] sentinel = {0}")
    @MethodSource("sentinelProvider")
    void unmaskNullKey_sentinelHandling(Object sentinel) {
        GenericTestMap<String, Integer> map = new GenericTestMap<>();
        assertNull(map.callUnmaskNullKey(sentinel), "Sentinel (or equal content) must be unmasked to null");
    }

    /**
     * Regular (non‑sentinel) keys of various types – the map is typed with Object so no cast failures.
     */
    @ParameterizedTest(name = "[{index}] key = {0}")
    @MethodSource("regularKeyProvider")
    void unmaskNullKey_returnsKeyUnchanged(Object key) {
        GenericTestMap<Object, Integer> map = new GenericTestMap<>();
        assertEquals(key, map.callUnmaskNullKey(key), "Non‑sentinel key should be returned unchanged");
    }

    /**
     * A String that looks similar to the sentinel but has different content must not be treated as sentinel.
     */
    @Test
    void unmaskNullKey_similarStringNotSentinel() {
        GenericTestMap<String, Integer> map = new GenericTestMap<>();
        String similar = "null_differentValue";
        assertEquals(similar, map.callUnmaskNullKey(similar), "String with different content must be returned unchanged");
    }

    /**
     * Boolean key – map typed with Object to avoid cast issues.
     */
    @Test
    void unmaskNullKey_booleanKey() {
        GenericTestMap<Object, Integer> map = new GenericTestMap<>();
        Boolean value = Boolean.TRUE;
        assertEquals(value, map.callUnmaskNullKey(value));
    }

    /**
     * List key – verifies that collection instances are returned unchanged.
     */
    @Test
    void unmaskNullKey_listKey() {
        GenericTestMap<Object, Integer> map = new GenericTestMap<>();
        List<String> list = Arrays.asList("a", "b");
        assertEquals(list, map.callUnmaskNullKey(list));
    }

    /**
     * Custom object key – should be returned unchanged when generic type matches (Object).
     */
    @Test
    void unmaskNullKey_customObjectKey() {
        GenericTestMap<Object, Integer> map = new GenericTestMap<>();
        CustomObject custom = new CustomObject("demo");
        assertEquals(custom, map.callUnmaskNullKey(custom));
    }

    /**
     * Subclass key casting – map generic type is Number, masked key is Integer.
     */
    @Test
    void unmaskNullKey_subclassKeyCasting() {
        GenericTestMap<Number, String> map = new GenericTestMap<>();
        Integer intKey = 123;
        Number result = map.callUnmaskNullKey(intKey);
        assertEquals(intKey, result);
    }

    /**
     * Type mismatch – map generic type is Integer, masked key is String → ClassCastException.
     */
    @Test
    void unmaskNullKey_typeMismatchStringWhenKIsInteger() {
        GenericTestMap<Integer, String> map = new GenericTestMap<>();
        assertThrows(ClassCastException.class, () -> map.callUnmaskNullKey("not-an-integer"));
    }

    /**
     * Custom object type mismatch – map generic type is String, masked key is CustomObject.
     */
    @Test
    void unmaskNullKey_customObjectTypeMismatch() {
        GenericTestMap<String, Integer> map = new GenericTestMap<>();
        CustomObject custom = new CustomObject("mismatch");
        assertThrows(ClassCastException.class, () -> map.callUnmaskNullKey(custom));
    }

    /**
     * Array key type mismatch – map generic type is Integer, masked key is int[].
     */
    @Test
    void unmaskNullKey_arrayKeyTypeMismatch() {
        GenericTestMap<Integer, String> map = new GenericTestMap<>();
        int[] array = new int[] { 1, 2, 3 };
        assertThrows(ClassCastException.class, () -> map.callUnmaskNullKey(array));
    }
}
