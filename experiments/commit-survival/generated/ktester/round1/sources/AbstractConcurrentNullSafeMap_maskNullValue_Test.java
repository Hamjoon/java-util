package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.DayOfWeek;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_maskNullValue_Test {

    /**
     * Mocked internal map that the abstract class delegates to.
     */
    @Mock
    private ConcurrentMap<Object, Object> internalMapMock;

    /**
     * Concrete test implementation that simply exposes the protected method.
     */
    private TestMap testMap;

    /**
     * Minimal concrete subclass that exposes {@code maskNullValue} for testing.
     * All abstract {@link ConcurrentMap} methods are implemented with
     * {@code UnsupportedOperationException} because they are irrelevant for
     * the {@code maskNullValue} tests.
     */
    private static class TestMap extends AbstractConcurrentNullSafeMap<String, String> {

        TestMap(ConcurrentMap<Object, Object> internal) {
            super(internal);
        }

        /**
         * Public wrapper to call the protected method under test.
         */
        Object maskNullValuePublic(String value) {
            return maskNullValue(value);
        }

        // -----------------------------------------------------------------
        // Boilerplate implementations of the required ConcurrentMap methods.
        // They are not used in the current tests and therefore simply throw.
        // -----------------------------------------------------------------
        @Override
        public int size() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isEmpty() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsKey(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsValue(Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String get(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String put(String key, String value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String remove(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void putAll(Map<? extends String, ? extends String> m) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<Entry<String, String>> entrySet() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<String> keySet() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Collection<String> values() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String putIfAbsent(String key, String value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object key, Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean replace(String key, String oldValue, String newValue) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String replace(String key, String value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String computeIfAbsent(String key, java.util.function.Function<? super String, ? extends String> mappingFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String computeIfPresent(String key, java.util.function.BiFunction<? super String, ? super String, ? extends String> remappingFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String compute(String key, java.util.function.BiFunction<? super String, ? super String, ? extends String> remappingFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String merge(String key, String value, java.util.function.BiFunction<? super String, ? super String, ? extends String> remappingFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getOrDefault(Object key, String defaultValue) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void forEach(java.util.function.BiConsumer<? super String, ? super String> action) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void replaceAll(java.util.function.BiFunction<? super String, ? super String, ? extends String> function) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Simple POJO used to verify that the exact object reference is preserved.
     */
    private static class CustomObject {

        private final boolean flag;

        CustomObject(boolean flag) {
            this.flag = flag;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof CustomObject))
                return false;
            CustomObject that = (CustomObject) o;
            return flag == that.flag;
        }

        @Override
        public int hashCode() {
            return Objects.hash(flag);
        }

        @Override
        public String toString() {
            return "CustomObject{flag=" + flag + '}';
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a concrete instance of the abstract class using the mocked internal map.
        testMap = new TestMap(internalMapMock);
    }

    private static Stream<Arguments> maskNullValueProvider() {
        // custom object to test reference preservation
        CustomObject custom = new CustomObject(true);
        return Stream.of(// null case – already covered by explicit test but kept for completeness
        Arguments.of(null, AbstractConcurrentNullSafeMap.NullSentinel.NULL_VALUE), // Integer
        Arguments.of(42, 42), // String
        Arguments.of("testString", "testString"), // Custom POJO (represented as a JSON map in the original description)
        Arguments.of(custom, custom));
    }

    private static Stream<Arguments> maskNullValueAdditionalProvider() {
        // Boolean
        Boolean bool = Boolean.TRUE;
        // Double
        Double dbl = 3.14159;
        // List (array representation)
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3));
        // Map (collection wrapped in a JSON object)
        Map<String, List<String>> map = new HashMap<>();
        map.put("list", Arrays.asList("a", "b", "c"));
        // Enum constant
        DayOfWeek enumConst = DayOfWeek.MONDAY;
        // Sentinel already present (non‑null)
        Object sentinel = AbstractConcurrentNullSafeMap.NullSentinel.NULL_VALUE;
        // Lambda / functional interface
        Function<Integer, Integer> lambda = x -> x;
        return Stream.of(Arguments.of(bool, bool), Arguments.of(dbl, dbl), Arguments.of(list, list), Arguments.of(map, map), Arguments.of(enumConst, enumConst), Arguments.of(sentinel, sentinel), Arguments.of(lambda, lambda));
    }

    @Test
    void maskNullValue_returnsSentinelWhenValueIsNull() {
        // GIVEN a null value
        String value = null;
        // WHEN the protected maskNullValue method is invoked via the public wrapper
        Object result = testMap.maskNullValuePublic(value);
        // THEN the sentinel representing a null value is returned
        assertEquals(AbstractConcurrentNullSafeMap.NullSentinel.NULL_VALUE, result, "maskNullValue should return NullSentinel.NULL_VALUE for null input");
    }

    @Test
    void maskNullValue_returnsOriginalWhenValueIsNonNull() {
        // GIVEN a non‑null value
        String value = "nonNull";
        // WHEN the protected maskNullValue method is invoked via the public wrapper
        Object result = testMap.maskNullValuePublic(value);
        // THEN the original value is returned unchanged
        assertEquals(value, result, "maskNullValue should return the original value when it is not null");
    }

    @ParameterizedTest(name = "[{index}] maskNullValue({0}) → {1}")
    @MethodSource("maskNullValueProvider")
    void maskNullValue_variousInputs(Object input, Object expected) {
        // WHEN
        Object actual = testMap.maskNullValuePublic(input);
        // THEN – use reference equality when we expect the exact same object,
        // otherwise fall back to equals comparison.
        if (expected == input) {
            assertSame(expected, actual, () -> "Expected the same instance for input: " + input);
        } else {
            assertEquals(expected, actual, () -> "Expected equal value for input: " + input);
        }
    }

    @ParameterizedTest(name = "[{index}] maskNullValue_additional({0}) → {1}")
    @MethodSource("maskNullValueAdditionalProvider")
    void maskNullValue_additionalVariedInputs(Object input, Object expected) {
        // WHEN
        Object actual = testMap.maskNullValuePublic(input);
        // THEN – same logic as above.
        if (expected == input) {
            assertSame(expected, actual, () -> "Expected the same instance for input: " + input);
        } else {
            assertEquals(expected, actual, () -> "Expected equal value for input: " + input);
        }
    }
}
