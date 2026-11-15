package com.cedarsoftware.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
// import org.junit.*;
// import org.junit.runner.RunWith;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CompactMap_computeValueHashCode_Test {

    private static TestMap map;

    /**
     * Helper holder for a value / expected hash pair.
     */
    private static final class ValueHashPair {

        final Object value;

        final int expectedHash;

        ValueHashPair(Object value, int expectedHash) {
            this.value = value;
            this.expectedHash = expectedHash;
        }
    }

    /**
     * Helper holder for a value / expected exception class pair.
     */
    private static final class ExceptionPair {

        final Object value;

        final Class<? extends RuntimeException> exceptionClass;

        ExceptionPair(Object value, Class<? extends RuntimeException> exceptionClass) {
            this.value = value;
            this.exceptionClass = exceptionClass;
        }
    }

    /**
     * Concrete subclass that makes the protected method public for the tests.
     */
    private static class TestMap extends CompactMap<Object, Object> {

        @Override
        protected int compactSize() {
            // any value ≥ 2 is fine
            return 3;
        }

        @Override
        protected Map<Object, Object> getNewMap() {
            return new LinkedHashMap<>();
        }

        /**
         * Public wrapper around the protected method under test.
         */
        public int computeValueHashCodePublic(Object value) {
            return computeValueHashCode(value);
        }
    }

    /**
     * Dummy object with a deterministic hashCode based on its {@code id} field.
     */
    private static class Dummy {

        private final int id;

        Dummy(int id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            return id;
        }
    }

    /**
     * Object whose {@code hashCode()} returns a fixed value.
     */
    private static class FixedHash {

        private final int hash;

        FixedHash(int hash) {
            this.hash = hash;
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    /**
     * Object whose {@code hashCode()} throws a specific unchecked exception.
     */
    private static class ThrowingHash {

        private final RuntimeException toThrow;

        ThrowingHash(RuntimeException toThrow) {
            this.toThrow = toThrow;
        }

        @Override
        public int hashCode() {
            throw toThrow;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Create a single instance that will be reused across tests.
        map = new TestMap();
    }

    @AfterAll
    static void teardownAfterAll() {
        map = null;
    }

    /**
     * Provides a stream of (value, expectedHash) pairs for normal (non‑exception) scenarios.
     */
    private static Stream<Arguments> normalValueProvider() {
        return Stream.of(Arguments.of(5, 5), Arguments.of(new Dummy(42), 42), Arguments.of("testString", "testString".hashCode()), Arguments.of(new FixedHash(123456), 123456), Arguments.of(Boolean.TRUE, Boolean.TRUE.hashCode()), Arguments.of(123456789L, Long.valueOf(123456789L).hashCode()), Arguments.of(new FixedHash(0), 0), // empty map hash code is defined as 0
        Arguments.of(new TestMap(), 0));
    }

    /**
     * Provides a stream of (value, expectedExceptionClass) pairs for exception scenarios.
     */
    private static Stream<Arguments> exceptionValueProvider() {
        return Stream.of(Arguments.of(new ThrowingHash(new NullPointerException("hashCode NPE")), NullPointerException.class), Arguments.of(new ThrowingHash(new IllegalArgumentException("invalid argument")), IllegalArgumentException.class), Arguments.of(new ThrowingHash(new IllegalStateException("illegal state")), IllegalStateException.class));
    }

    @Test
    void computeValueHashCode_selfReference_returnsSpecialConstant() {
        int hash = map.computeValueHashCodePublic(map);
        assertEquals(17, hash, "When the value is the map instance itself the method must return 17");
    }

    @Test
    void computeValueHashCode_null_returnsZero() {
        int hash = map.computeValueHashCodePublic(null);
        assertEquals(0, hash, "When the value is null the method must return 0");
    }

    @Test
    void computeValueHashCode_regularObject_returnsObjectHashCode() {
        String value = "testString";
        int expected = value.hashCode();
        int hash = map.computeValueHashCodePublic(value);
        assertEquals(expected, hash, "For a non‑null, non‑self value the method must delegate to value.hashCode()");
    }

    @Test
    void computeValueHashCode_equalButNotSameObject_usesObjectHashCode() {
        // Two distinct Integer objects with the same value are equal but not the same reference.
        Integer a = Integer.valueOf(42);
        Integer b = Integer.valueOf(42);
        assertNotSame(a, b, "Integers must be distinct instances for this test");
        assertEquals(a, b, "Integers must be equal for this test");
        int hash = map.computeValueHashCodePublic(b);
        assertEquals(a.hashCode(), hash, "Equality does not affect the result – only reference identity to the map matters");
    }

    @ParameterizedTest(name = "[{index}] computeValueHashCode({0}) = {1}")
    @MethodSource("normalValueProvider")
    void computeValueHashCode_variousValues(Object value, int expectedHash) {
        int actual = map.computeValueHashCodePublic(value);
        assertEquals(expectedHash, actual, "Expected hash code " + expectedHash + " for value: " + value);
    }

    @ParameterizedTest(name = "[{index}] computeValueHashCode throws {1}")
    @MethodSource("exceptionValueProvider")
    void computeValueHashCode_propagatesExceptions(Object value, Class<? extends RuntimeException> exClass) {
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> map.computeValueHashCodePublic(value), "Expected exception of type " + exClass.getSimpleName());
        assertTrue(exClass.isInstance(thrown), "Expected exception of type " + exClass.getSimpleName() + " but got " + thrown.getClass().getSimpleName());
    }
}
