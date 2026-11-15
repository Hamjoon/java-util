package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_maskNullKey_Test {

    private TestMap<String, Integer> map;

    /**
     * Simple concrete subclass that makes {@code maskNullKey} public for the tests.
     */
    private static class TestMap<K, V> extends ConcurrentNavigableMapNullSafe<K, V> {

        /**
         * Exposes the protected {@code maskNullKey} method.
         */
        public Object callMaskNullKey(K key) {
            return super.maskNullKey(key);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        map = new TestMap<>();
    }

    /**
     * Retrieves the private static sentinel value via reflection.
     *
     * @return the sentinel string defined in {@link ConcurrentNavigableMapNullSafe}
     * @throws RuntimeException if reflection fails
     */
    private static String getSentinelViaReflection() {
        try {
            Field f = ConcurrentNavigableMapNullSafe.class.getDeclaredField("NULL_KEY_SENTINEL");
            f.setAccessible(true);
            return (String) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException("Unable to access NULL_KEY_SENTINEL", e);
        }
    }

    /**
     * Parameter provider for the {@link #maskNullKey_various(Object, Object)} test.
     *
     * <p>Each element is an {@code Object[]} where index 0 is the input key and index 1
     * is the expected result after calling {@code maskNullKey}.</p>
     */
    private static Stream<Object[]> maskNullKeyProvider() {
        String sentinel = getSentinelViaReflection();
        return Stream.of(// null key → sentinel
        new Object[] { null, sentinel }, // non‑null keys of various types
        new Object[] { "exampleKey", "exampleKey" }, new Object[] { 12345, 12345 }, new Object[] { "", "" }, new Object[] { Boolean.TRUE, Boolean.TRUE }, new Object[] { Map.of("custom", "object"), Map.of("custom", "object") });
    }

    @Test
    void maskNullKey_returnsSentinelForNullKey() {
        Object masked = map.callMaskNullKey(null);
        assertNotNull(masked, "Sentinel must not be null");
        assertTrue(masked instanceof String, "Sentinel should be a String");
        String sentinel = (String) masked;
        // The sentinel is generated as "null_" + UUID.randomUUID()
        assertTrue(sentinel.startsWith("null_"), "Sentinel should start with the prefix \"null_\" but was: " + sentinel);
        // Calling again should return the *same* sentinel instance (same value)
        Object maskedAgain = map.callMaskNullKey(null);
        assertEquals(sentinel, maskedAgain, "Multiple calls with null should return the same sentinel value");
    }

    @Test
    void maskNullKey_returnsSameObjectForNonNullKey() {
        String key = "myKey";
        Object masked = map.callMaskNullKey(key);
        assertSame(key, masked, "Non‑null key must be returned unchanged");
    }

    @Test
    void maskNullKey_doesNotExposeInternalSentinelDirectly() {
        // Ensure that the sentinel is not equal to an arbitrary string that merely
        // starts with the same prefix.
        Object masked = map.callMaskNullKey(null);
        assertNotEquals("null_12345", masked, "Sentinel should be unique; it must not equal an arbitrary string");
    }

    @Test
    void maskNullKey_integrationWithMapPutAndGet() {
        // Using the public API, which internally relies on maskNullKey,
        // we verify that a null key can be stored and retrieved.
        map.put(null, 42);
        assertEquals(42, map.get(null), "Value stored under a null key should be retrievable");
    }

    @ParameterizedTest(name = "maskNullKey({0}) ⇒ {1}")
    @MethodSource("maskNullKeyProvider")
    void maskNullKey_various(Object key, Object expected) {
        Object masked = map.callMaskNullKey(key);
        // For the null‑key case we also assert the sentinel format.
        if (key == null) {
            assertNotNull(masked, "Sentinel must not be null");
            assertTrue(masked instanceof String, "Sentinel should be a String");
            String sentinel = (String) masked;
            assertTrue(sentinel.startsWith("null_"), "Sentinel should start with the prefix \"null_\" but was: " + sentinel);
            // The sentinel obtained via reflection must be exactly the same value.
            assertEquals(getSentinelViaReflection(), sentinel, "Sentinel returned by maskNullKey must match the static field");
        }
        // General equality check (covers value equality for primitives and objects)
        assertEquals(expected, masked, () -> "maskNullKey should return the expected value for key: " + key);
        // For reference‑equality checks on objects that are not immutable primitives,
        // we verify that the same instance is returned.
        if (key != null && !(key instanceof Integer) && !(key instanceof Boolean)) {
            assertSame(key, masked, "Non‑null key objects must be returned unchanged (same instance)");
        }
    }

    @Test
    void maskNullKey_returnsSameSentinelInstanceForMultipleNullCalls() {
        Object first = map.callMaskNullKey(null);
        Object second = map.callMaskNullKey(null);
        assertSame(first, second, "Multiple calls with a null key should return the *same* sentinel instance");
    }
}
