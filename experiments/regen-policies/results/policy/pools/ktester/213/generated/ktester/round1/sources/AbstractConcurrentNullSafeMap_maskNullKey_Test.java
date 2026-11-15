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
import java.lang.reflect.Method;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_maskNullKey_Test {

    private TestMap mapUnderTest;

    /**
     * Simple concrete implementation used only for testing the protected behaviour.
     */
    private static class TestMap extends AbstractConcurrentNullSafeMap<String, String> {

        TestMap(ConcurrentMap<Object, Object> internal) {
            super(internal);
        }

        /**
         * Public wrapper that delegates to the protected {@code maskNullKey}.
         */
        Object exposeMaskNullKey(String key) {
            return maskNullKey(key);
        }
    }

    /**
     * A tiny user‑defined key class used in one of the parameterised test cases.
     */
    private static final class CustomKey {

        final int id;

        CustomKey(int id) {
            this.id = id;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a real ConcurrentHashMap as the backing map – no mocking needed.
        ConcurrentMap<Object, Object> backingMap = new ConcurrentHashMap<>();
        mapUnderTest = new TestMap(backingMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to ensure test isolation.
        mapUnderTest.clear();
    }

    /**
     * Provides a stream of arguments for the {@link #maskNullKey_ShouldReturnOriginalKey_ForVariousNonNullKeys(Object, Object, String)}
     * test. Each argument consists of:
     * <ul>
     *   <li>the key to pass to {@code maskNullKey}</li>
     *   <li>the expected result (always the same instance as the key)</li>
     *   <li>a short description used as the test name</li>
     * </ul>
     */
    private static Stream<Arguments> nonNullKeyProvider() {
        return Stream.of(Arguments.of(42, 42, "Integer key should be returned unchanged"), Arguments.of(AbstractConcurrentNullSafeMap.NullSentinel.NULL_KEY, AbstractConcurrentNullSafeMap.NullSentinel.NULL_KEY, "Enum sentinel key should be returned unchanged"), Arguments.of(new Object(), null, "Fresh Object key should be returned unchanged (identity)"), Arguments.of("", "", "Empty string should be returned unchanged"), Arguments.of(Boolean.FALSE, Boolean.FALSE, "Boolean false should be returned unchanged"), Arguments.of(new CustomKey(1), null, "CustomKey instance should be returned unchanged (identity)"));
    }

    /**
     * Helper that invokes the protected {@code maskNullKey} method via reflection.
     *
     * @param key the key to mask (may be any Object)
     * @return the result of {@code maskNullKey}
     */
    private Object invokeMaskNullKeyReflectively(Object key) {
        try {
            Method method = AbstractConcurrentNullSafeMap.class.getDeclaredMethod("maskNullKey", Object.class);
            method.setAccessible(true);
            return method.invoke(mapUnderTest, key);
        } catch (Exception e) {
            throw new RuntimeException("Reflection call to maskNullKey failed", e);
        }
    }

    @Test
    @DisplayName("maskNullKey returns NULL_KEY sentinel when key is null")
    void maskNullKey_ShouldReturnNullKeySentinel_WhenKeyIsNull() {
        // GIVEN
        String key = null;
        // WHEN
        Object result = mapUnderTest.exposeMaskNullKey(key);
        // THEN
        assertSame(AbstractConcurrentNullSafeMap.NullSentinel.NULL_KEY, result, "When the key is null the method must return the NULL_KEY sentinel");
    }

    @Test
    @DisplayName("maskNullKey returns the original key when it is non‑null")
    void maskNullKey_ShouldReturnOriginalKey_WhenKeyIsNonNull() {
        // GIVEN
        String key = "actualKey";
        // WHEN
        Object result = mapUnderTest.exposeMaskNullKey(key);
        // THEN
        assertSame(key, result, "When the key is non‑null the method must return the original key instance");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("nonNullKeyProvider")
    @DisplayName("maskNullKey returns the original object for various non‑null keys")
    void maskNullKey_ShouldReturnOriginalKey_ForVariousNonNullKeys(Object key, Object expected, String description) {
        // GIVEN
        // (key is supplied by the method source)
        // WHEN
        Object result = invokeMaskNullKeyReflectively(key);
        // THEN
        // For the cases where we cannot know the exact instance at compile‑time
        // (e.g., new Object() or CustomKey) the expected argument is null.
        // In those situations we assert identity equality with the original key.
        if (expected == null) {
            assertSame(key, result, "The method must return the exact same instance that was supplied");
        } else {
            // For immutable boxed values and enum constants we can also use identity.
            assertSame(expected, result, "The method must return the exact same instance that was supplied");
        }
    }
}
