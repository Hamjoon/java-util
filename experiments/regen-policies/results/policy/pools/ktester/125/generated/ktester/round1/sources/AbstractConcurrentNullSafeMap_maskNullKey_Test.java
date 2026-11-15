package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.DayOfWeek;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_maskNullKey_Test {

    private TestMap map;

    /**
     * Concrete subclass exposing the protected maskNullKey method for testing
     */
    private static class TestMap extends AbstractConcurrentNullSafeMap<String, String> {

        TestMap(ConcurrentMap<Object, Object> internalMap) {
            super(internalMap);
        }

        @Override
        public String put(String key, String value) {
            throw new UnsupportedOperationException("not needed for this test");
        }

        @Override
        public String get(Object key) {
            throw new UnsupportedOperationException("not needed for this test");
        }

        @Override
        public boolean containsKey(Object key) {
            throw new UnsupportedOperationException("not needed for this test");
        }

        /**
         * expose protected method for a {@code String} key (original test)
         */
        Object maskNullKeyPublic(String key) {
            return maskNullKey(key);
        }

        /**
         * expose protected method for any object – used by the parameterised tests
         */
        @SuppressWarnings("unchecked")
        Object maskNullKeyPublic(Object key) {
            return maskNullKey((K) key);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a simple ConcurrentHashMap as the backing map
        ConcurrentMap<Object, Object> backing = new ConcurrentHashMap<>();
        map = new TestMap(backing);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to avoid cross‑test contamination
        map.internalMap.clear();
    }

    /**
     * Supplies a diverse set of non‑null keys, including the sentinel itself.
     */
    private static Stream<Arguments> maskNullKeyProvider() {
        // Objects that will be reused as the expected reference
        Object genericObject = new Object();
        StringBuilder sb = new StringBuilder("test");
        return Stream.of(// Integer (value 0)
        Arguments.of(Integer.valueOf(0), Integer.valueOf(0)), // Boolean true
        Arguments.of(Boolean.TRUE, Boolean.TRUE), // Plain Object instance
        Arguments.of(genericObject, genericObject), // Mutable StringBuilder
        Arguments.of(sb, sb), // Enum constant from java.time.DayOfWeek
        Arguments.of(DayOfWeek.MONDAY, DayOfWeek.MONDAY), // The sentinel itself – should be returned unchanged
        Arguments.of(AbstractConcurrentNullSafeMap.NullSentinel.NULL_KEY, AbstractConcurrentNullSafeMap.NullSentinel.NULL_KEY));
    }

    @Test
    void maskNullKey_returnsNullKeySentinel_whenKeyIsNull() {
        // given
        String key = null;
        // when
        Object masked = map.maskNullKeyPublic(key);
        // then
        assertEquals(AbstractConcurrentNullSafeMap.NullSentinel.NULL_KEY, masked, "maskNullKey should return the NULL_KEY sentinel for a null key");
    }

    @Test
    void maskNullKey_returnsSameObject_whenKeyIsNonNull() {
        // given
        String key = "nonNullKey";
        // when
        Object masked = map.maskNullKeyPublic(key);
        // then
        assertSame(key, masked, "maskNullKey should return the original key object when it is not null");
    }

    /**
     * Parameterised test that verifies {@code maskNullKey} returns the *exact* object
     * it receives when the key is non‑null, regardless of the key's runtime type.
     *
     * <p>Each argument consists of:
     * <ul>
     *   <li>the key to pass to {@code maskNullKey}</li>
     *   <li>the expected result (the same instance that was supplied)</li>
     * </ul>
     * </p>
     */
    @ParameterizedTest(name = "[{index}] key={0} ({1})")
    @MethodSource("maskNullKeyProvider")
    void maskNullKey_returnsSameInstance_forVariousNonNullKeys(Object key, Object expected) {
        // when
        Object masked = map.maskNullKeyPublic(key);
        // then
        assertSame(expected, masked, () -> "maskNullKey should return the same instance for key: " + key);
    }
}
