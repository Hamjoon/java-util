package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_unmaskNullKey_Test {

    private TestMap map;

    /**
     * Simple concrete implementation used only for testing the protected behaviour.
     */
    private static class TestMap extends AbstractConcurrentNullSafeMap<String, String> {

        TestMap(ConcurrentMap<Object, Object> internal) {
            super(internal);
        }

        /**
         * Expose the protected method so the test can call it.
         */
        @Override
        public String unmaskNullKey(Object key) {
            return super.unmaskNullKey(key);
        }
    }

    /**
     * Concrete implementation for an {@code Integer} key map.
     */
    private static class TestMapInt extends AbstractConcurrentNullSafeMap<Integer, String> {

        TestMapInt(ConcurrentMap<Object, Object> internal) {
            super(internal);
        }

        @Override
        public Integer unmaskNullKey(Object key) {
            return super.unmaskNullKey(key);
        }
    }

    /**
     * Concrete implementation for an {@code Object} key map.
     */
    private static class TestMapObject extends AbstractConcurrentNullSafeMap<Object, String> {

        TestMapObject(ConcurrentMap<Object, Object> internal) {
            super(internal);
        }

        @Override
        public Object unmaskNullKey(Object key) {
            return super.unmaskNullKey(key);
        }
    }

    /**
     * Concrete implementation for a {@code Boolean} key map.
     */
    private static class TestMapBoolean extends AbstractConcurrentNullSafeMap<Boolean, String> {

        TestMapBoolean(ConcurrentMap<Object, Object> internal) {
            super(internal);
        }

        @Override
        public Boolean unmaskNullKey(Object key) {
            return super.unmaskNullKey(key);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a fresh ConcurrentHashMap for each test to avoid cross‑test contamination.
        ConcurrentMap<Object, Object> internal = new ConcurrentHashMap<>();
        map = new TestMap(internal);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to aid garbage collection and to make sure no lingering state remains.
        map.clear();
    }

    /**
     * Provides a stream of arguments for normal behaviour verification.
     * Each argument consists of:
     *   - the key object to pass,
     *   - the expected result (either a value or null),
     *   - a description for readability.
     */
    private static Stream<Arguments> normalBehaviourProvider() {
        return Stream.of(Arguments.of(AbstractConcurrentNullSafeMap.NullSentinel.NULL_KEY, null, "Sentinel for null key should be unmasked to null"), Arguments.of("actualKey", "actualKey", "Regular non‑null key should be returned unchanged"), Arguments.of(null, null, "Genuine null value (not a sentinel) should be returned as null"), Arguments.of(AbstractConcurrentNullSafeMap.NullSentinel.NULL_VALUE, AbstractConcurrentNullSafeMap.NullSentinel.NULL_VALUE, "Sentinel for null value is *not* treated specially by unmaskNullKey"));
    }

    @Test
    void unmaskNullKey_returnsNullWhenSentinelProvided() {
        // GIVEN: the sentinel representing a null key
        Object sentinel = AbstractConcurrentNullSafeMap.NullSentinel.NULL_KEY;
        // WHEN: the sentinel is passed to unmaskNullKey
        String result = map.unmaskNullKey(sentinel);
        // THEN: the method should return null
        assertNull(result, "unmaskNullKey should convert NULL_KEY sentinel to null");
    }

    @Test
    void unmaskNullKey_returnsOriginalKeyWhenNotSentinel() {
        // GIVEN: a regular (non‑null) key
        String originalKey = "myKey";
        // WHEN: the key is passed to unmaskNullKey
        String result = map.unmaskNullKey(originalKey);
        // THEN: the same key instance should be returned
        assertSame(originalKey, result, "unmaskNullKey should return the original key when it is not the sentinel");
    }

    @Test
    void unmaskNullKey_throwsClassCastExceptionWhenKeyTypeIsIncompatible() {
        // GIVEN: an object of a different type than the map's key generic type
        Object wrongTypeKey = Integer.valueOf(42);
        // WHEN / THEN: a ClassCastException is expected because the method casts to K (String)
        assertThrows(ClassCastException.class, () -> map.unmaskNullKey(wrongTypeKey), "unmaskNullKey should throw ClassCastException when the key cannot be cast to the map's key type");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("normalBehaviourProvider")
    void unmaskNullKey_behaviourMatrix(Object inputKey, Object expected, String description) {
        // When the key is passed to unmaskNullKey
        String result = map.unmaskNullKey(inputKey);
        // Then the result matches the expectation
        assertEquals(expected, result, description);
    }

    @Test
    void unmaskNullKey_genericIntegerKey_returnsSameInteger() {
        TestMapInt intMap = new TestMapInt(new ConcurrentHashMap<>());
        Integer key = 123;
        Integer result = intMap.unmaskNullKey(key);
        assertSame(key, result, "When K=Integer, an Integer key should be returned unchanged");
    }

    @Test
    void unmaskNullKey_genericObjectKey_returnsSameString() {
        TestMapObject objMap = new TestMapObject(new ConcurrentHashMap<>());
        String key = "stringKey";
        Object result = objMap.unmaskNullKey(key);
        assertSame(key, result, "When K=Object, a String key should be returned unchanged");
    }

    @Test
    void unmaskNullKey_genericBooleanKey_returnsSameBoolean() {
        TestMapBoolean boolMap = new TestMapBoolean(new ConcurrentHashMap<>());
        Boolean key = Boolean.TRUE;
        Boolean result = boolMap.unmaskNullKey(key);
        assertSame(key, result, "When K=Boolean, a Boolean key should be returned unchanged");
    }

    @Test
    void unmaskNullKey_nullSentinelValue_throwsClassCastExceptionForStringMap() {
        // Map is typed with K=String; NULL_VALUE cannot be cast to String.
        assertThrows(ClassCastException.class, () -> map.unmaskNullKey(AbstractConcurrentNullSafeMap.NullSentinel.NULL_VALUE), "NULL_VALUE sentinel should cause ClassCastException for a String‑typed map");
    }

    @Test
    void unmaskNullKey_arbitraryObject_throwsClassCastExceptionForStringMap() {
        Object obj = new Object();
        assertThrows(ClassCastException.class, () -> map.unmaskNullKey(obj), "An arbitrary Object not assignable to String should cause ClassCastException");
    }

    @Test
    void unmaskNullKey_stringKeyOnIntegerMap_throwsClassCastException() {
        TestMapInt intMap = new TestMapInt(new ConcurrentHashMap<>());
        assertThrows(ClassCastException.class, () -> intMap.unmaskNullKey("stringKey"), "Passing a String key to a map typed with K=Integer should cause ClassCastException");
    }
}
