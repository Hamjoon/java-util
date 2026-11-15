package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_maskNullKey_Test {

    // Mocked internal map – required by the abstract class constructor.
    @Mock
    private ConcurrentMap<Object, Object> internalMapMock;

    // Concrete test implementation of the abstract class.
    private TestMap<String, Integer> testMap;

    /**
     * Simple concrete subclass used only for exercising the protected helper methods.
     */
    private static class TestMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        TestMap(ConcurrentMap<Object, Object> internalMap) {
            super(internalMap);
        }
        // The abstract class already implements all required ConcurrentMap methods,
        // but they delegate to the internal map. For this test we don't need any
        // additional behaviour, so the class is left empty.
    }

    /**
     * A tiny custom key class used in one of the parameterised test cases.
     */
    private static final class CustomKey {

        private final String id;

        CustomKey(String id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof CustomKey))
                return false;
            CustomKey that = (CustomKey) o;
            return id.equals(that.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        @Override
        public String toString() {
            return "CustomKey{id='" + id + "'}";
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise the concrete test map with the mocked internal map.
        testMap = new TestMap<>(internalMapMock);
    }

    @AfterEach
    void teardownAfterEach() {
        // No resources to clean up for this test class.
        testMap = null;
    }

    /**
     * Provides a stream of arguments: each element is an array where
     *   [0] = description (for readability)
     *   [1] = input key (may be null)
     *   [2] = expected result (either the sentinel or the same instance)
     */
    private static Stream<Arguments> maskNullKeyProvider() {
        return Stream.of(Arguments.of("null key", null, AbstractConcurrentNullSafeMap.NullSentinel.NULL_KEY), Arguments.of("String key", "testString", "testString"), Arguments.of("Integer key", 42, 42), Arguments.of("Custom object key", new CustomKey("id"), new CustomKey("id")));
    }

    @Test
    void maskNullKey_givenNullKey_returnsNullKeySentinel() {
        // GIVEN
        String key = null;
        // WHEN
        Object masked = testMap.maskNullKey(key);
        // THEN
        assertEquals(AbstractConcurrentNullSafeMap.NullSentinel.NULL_KEY, masked, "maskNullKey should return the NULL_KEY sentinel for a null key");
    }

    @Test
    void maskNullKey_givenNonNullKey_returnsSameKeyInstance() {
        // GIVEN
        String key = "nonNullKey";
        // WHEN
        Object masked = testMap.maskNullKey(key);
        // THEN
        assertSame(key, masked, "maskNullKey should return the original key instance when the key is not null");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("maskNullKeyProvider")
    void maskNullKey_parameterisedTests(String description, Object key, Object expected) {
        // WHEN
        Object masked = testMap.maskNullKey(key);
        // THEN
        if (key == null) {
            // For null we expect the sentinel enum constant.
            assertEquals(expected, masked, "When key is null, maskNullKey must return the NULL_KEY sentinel");
        } else {
            // For non‑null we expect reference equality.
            assertSame(key, masked, "When key is non‑null, maskNullKey must return the same instance");
        }
    }
}
