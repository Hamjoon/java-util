package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.UUID;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_maskNullKey_Test {

    /**
     * Instance of the class under test.
     */
    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    /**
     * The sentinel value used internally to represent a {@code null} key.
     */
    private static String NULL_KEY_SENTINEL;

    /**
     * Simple custom key class used in one of the test cases.
     */
    static class KeyObj {

        final int id;

        KeyObj(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof KeyObj))
                return false;
            KeyObj keyObj = (KeyObj) o;
            return id == keyObj.id;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(id);
        }

        @Override
        public String toString() {
            return "KeyObj{id=" + id + '}';
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Retrieve the sentinel via reflection – it is a private static final field.
        try {
            var field = ConcurrentNavigableMapNullSafe.class.getDeclaredField("NULL_KEY_SENTINEL");
            field.setAccessible(true);
            NULL_KEY_SENTINEL = (String) field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Unable to access NULL_KEY_SENTINEL", e);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use the no‑arg constructor which creates an internal ConcurrentSkipListMap.
        map = new ConcurrentNavigableMapNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference for GC and to avoid accidental reuse between tests.
        map = null;
    }

    /**
     * Supplies a stream of non‑null keys of different types, matching the JSON
     * test cases.
     */
    static Stream<Object> nonNullKeyProvider() {
        return Stream.of(// String
        "hello", // Integer
        123, // Double
        3.14159, // Boolean
        true, // StringBuilder containing sentinel prefix
        new StringBuilder("null_12345"), // Custom object
        new KeyObj(42));
    }

    @Test
    void maskNullKey_returnsSentinelForNullKey() {
        // Act
        Object masked = map.maskNullKey(null);
        // Assert
        assertNotNull(masked, "Masked value for null key should not be null");
        assertTrue(masked instanceof String, "Masked value should be a String sentinel");
        assertEquals(NULL_KEY_SENTINEL, masked, "Sentinel should match the internal constant");
    }

    @Test
    void maskNullKey_returnsSameObjectForNonNullKey() {
        // Arrange
        String key = "testKey";
        // Act
        Object masked = map.maskNullKey(key);
        // Assert
        assertSame(key, masked, "Non‑null keys must be returned unchanged (same instance)");
    }

    @Test
    void maskNullKey_isConsistentAcrossMultipleCalls() {
        // Act / Assert for null key
        Object firstNullMask = map.maskNullKey(null);
        Object secondNullMask = map.maskNullKey(null);
        assertEquals(firstNullMask, secondNullMask, "Masking null repeatedly should yield the same sentinel");
        assertSame(firstNullMask, secondNullMask, "Sentinel instance should be identical across calls");
        // Act / Assert for a non‑null key
        String key = "anotherKey";
        Object firstMask = map.maskNullKey(key);
        Object secondMask = map.maskNullKey(key);
        assertSame(firstMask, secondMask, "Masking a non‑null key should always return the original instance");
    }

    /**
     * Verify that the sentinel string always starts with the literal prefix
     * {@code "null_"}.
     */
    @Test
    void sentinelString_hasCorrectPrefix() {
        Object masked = map.maskNullKey(null);
        assertTrue(masked instanceof String, "Sentinel must be a String");
        assertTrue(((String) masked).startsWith("null_"), "Sentinel string should start with the prefix \"null_\"");
    }

    /**
     * Ensure that two separate map instances share the *same* sentinel constant.
     */
    @Test
    void sentinel_isSharedAcrossInstances() {
        ConcurrentNavigableMapNullSafe<String, Integer> map2 = new ConcurrentNavigableMapNullSafe<>();
        Object sentinel1 = map.maskNullKey(null);
        Object sentinel2 = map2.maskNullKey(null);
        assertEquals(sentinel1, sentinel2, "Sentinel values must be equal across map instances");
        assertSame(sentinel1, sentinel2, "Sentinel must be the same static object");
    }

    /**
     * Verify that a non‑null key which *looks* like the sentinel (e.g. "null_12345")
     * is returned unchanged.
     */
    @Test
    void nonNullKey_lookingLikeSentinel_isReturnedUnchanged() {
        String looksLikeSentinel = "null_" + UUID.randomUUID().toString();
        Object masked = map.maskNullKey(looksLikeSentinel);
        assertSame(looksLikeSentinel, masked, "A non‑null key that resembles the sentinel must be returned unchanged");
    }

    /**
     * Parameterized test for a variety of non‑null key types. The method must
     * return the exact same instance that was supplied.
     */
    @ParameterizedTest(name = "{index}: key={0}")
    @MethodSource("nonNullKeyProvider")
    void maskNullKey_returnsOriginalInstanceForNonNullKeys(Object key) {
        // Act
        Object masked = map.maskNullKey(key);
        // Assert
        assertSame(key, masked, "Non‑null key of type " + key.getClass().getSimpleName() + " must be returned unchanged");
    }

    /**
     * Verify that a map instantiated with a different generic key type still
     * returns the shared sentinel for a {@code null} key.
     */
    @Test
    void maskNullKey_nullKeyOnDifferentGenericType_returnsSharedSentinel() {
        // Map with Integer keys (generic type does not affect sentinel)
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        Object masked = intMap.maskNullKey(null);
        assertEquals(NULL_KEY_SENTINEL, masked, "Sentinel must be the same static constant regardless of generic type");
        assertSame(NULL_KEY_SENTINEL, masked, "Sentinel instance should be identical across generic variations");
    }
}
