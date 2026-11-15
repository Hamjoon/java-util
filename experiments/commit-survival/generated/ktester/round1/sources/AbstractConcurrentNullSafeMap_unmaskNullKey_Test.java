package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_unmaskNullKey_Test {

    private TestMap<String, Integer> mapUnderTest;

    private ConcurrentMap<Object, Object> internalMap;

    /**
     * Concrete implementation used only for testing the protected behaviour.
     */
    private static class TestMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        TestMap(ConcurrentMap<Object, Object> internalMap) {
            super(internalMap);
        }

        /**
         * Expose the protected method so the test can call it.
         */
        K callUnmaskNullKey(Object key) {
            return super.unmaskNullKey(key);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        internalMap = new ConcurrentHashMap<>();
        mapUnderTest = new TestMap<>(internalMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the internal map to avoid cross‑test contamination.
        internalMap.clear();
    }

    private static Stream<Object> nonStringKeysProvider() {
        return Stream.of(// Integer
        123, // Plain Object
        new Object());
    }

    @Test
    void unmaskNullKey_returnsNull_whenKeyIsNullSentinel() {
        // GIVEN
        Object sentinelKey = AbstractConcurrentNullSafeMap.NullSentinel.NULL_KEY;
        // WHEN
        String result = mapUnderTest.callUnmaskNullKey(sentinelKey);
        // THEN
        assertNull(result, "unmaskNullKey should return null for the NULL_KEY sentinel");
    }

    @Test
    void unmaskNullKey_returnsOriginalKey_whenKeyIsNotSentinel() {
        // GIVEN
        String originalKey = "myKey";
        // WHEN
        String result = mapUnderTest.callUnmaskNullKey(originalKey);
        // THEN
        assertEquals(originalKey, result, "unmaskNullKey should return the original key when it is not the NULL_KEY sentinel");
    }

    @Test
    void unmaskNullKey_handlesNonStringKeyTypes() {
        // GIVEN
        Integer originalKey = 42;
        // WHEN
        Integer result = mapUnderTest.callUnmaskNullKey(originalKey);
        // THEN
        assertEquals(originalKey, result, "unmaskNullKey should correctly cast and return non‑String keys");
    }

    @Test
    void unmaskNullKey_returnsNull_whenPassedNullDirectly() {
        // GIVEN
        Object nullKey = null;
        // WHEN
        String result = mapUnderTest.callUnmaskNullKey(nullKey);
        // THEN
        // The implementation does not treat a literal null specially; it will be cast.
        // Therefore the expected result is null (the casted null).
        assertNull(result, "unmaskNullKey should return null when the input key itself is null");
    }

    @Test
    void unmaskNullKey_throwsClassCastException_whenKeyIsNonString() {
        // GIVEN
        Integer nonStringKey = 42;
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> mapUnderTest.callUnmaskNullKey(nonStringKey), "When the map is typed with K=String, a non‑String key must cause a ClassCastException");
    }

    @Test
    void unmaskNullKey_returnsSentinelValue_whenKeyIsValueSentinel_andGenericIsObject() {
        // GIVEN: a map whose key type is Object (no casting restriction)
        TestMap<Object, Integer> objectKeyMap = new TestMap<>(new ConcurrentHashMap<>());
        Object sentinel = AbstractConcurrentNullSafeMap.NullSentinel.NULL_VALUE;
        // WHEN
        Object result = objectKeyMap.callUnmaskNullKey(sentinel);
        // THEN
        assertSame(sentinel, result, "With K=Object the NULL_VALUE sentinel should be returned unchanged");
    }

    @Test
    void unmaskNullKey_throwsClassCastException_whenValueSentinelUsedWithStringKey() {
        // GIVEN
        Object sentinel = AbstractConcurrentNullSafeMap.NullSentinel.NULL_VALUE;
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> mapUnderTest.callUnmaskNullKey(sentinel), "With K=String the NULL_VALUE sentinel is not assignable and must trigger a ClassCastException");
    }

    @ParameterizedTest(name = "unmaskNullKey should throw ClassCastException for key: {0}")
    @MethodSource("nonStringKeysProvider")
    void unmaskNullKey_throwsClassCastException_forInvalidKeyTypes(Object invalidKey) {
        assertThrows(ClassCastException.class, () -> mapUnderTest.callUnmaskNullKey(invalidKey), "Invalid key type should cause a ClassCastException");
    }
}
