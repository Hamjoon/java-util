package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_unmaskNullValue_Test {

    /**
     * Instance of the abstract class created with a real constructor and real method calls.
     */
    private AbstractConcurrentNullSafeMap<String, String> map;

    /**
     * Cached reflection handle for the protected method under test.
     */
    private Method unmaskNullValueMethod;

    /**
     * Simple holder to pass both the test value and a readable name to the parameterised test.
     */
    private static class ArgumentsHolder {

        final Object input;

        final String description;

        ArgumentsHolder(Object input, String description) {
            this.input = input;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    /**
     * A tiny user‑defined class used to verify reference equality.
     */
    private static final class CustomValue {

        private final String name;

        CustomValue(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "CustomValue{" + "name='" + name + '\'' + '}';
        }
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // The concrete internal map that the abstract class will delegate to.
        ConcurrentMap<Object, Object> internal = new ConcurrentHashMap<>();
        /*
         * Create a mock of the abstract class that:
         *   • Uses the real constructor (so the internalMap field is correctly initialised)
         *   • Calls real methods by default (CALLS_REAL_METHODS)
         *
         * This gives us an instance on which we can invoke the protected method via reflection
         * without having to implement the whole ConcurrentMap contract.
         */
        map = mock(AbstractConcurrentNullSafeMap.class, withSettings().useConstructor(internal).defaultAnswer(Mockito.CALLS_REAL_METHODS));
        // Obtain the protected method once; make it accessible for the test cases.
        unmaskNullValueMethod = AbstractConcurrentNullSafeMap.class.getDeclaredMethod("unmaskNullValue", Object.class);
        unmaskNullValueMethod.setAccessible(true);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the pattern consistent.
        map = null;
        unmaskNullValueMethod = null;
    }

    /**
     * Provides a stream of arguments: each entry contains the input value and a description.
     * The expected behaviour is inferred inside the test method:
     *   – If the input is the NULL_VALUE sentinel → {@code null}
     *   – Otherwise → the exact same instance (identity equality)
     */
    private static Stream<ArgumentsHolder> provideValues() {
        return Stream.of(// Null reference – should be returned as null (no conversion)
        new ArgumentsHolder(null, "null reference"), // Ordinary object – identity must be preserved
        new ArgumentsHolder(new Object(), "plain Object instance"), // Primitive wrapper – identity must be preserved
        new ArgumentsHolder(Boolean.TRUE, "Boolean.TRUE wrapper"), // Integer – identity must be preserved
        new ArgumentsHolder(12345, "Integer value"), // Array – identity must be preserved
        new ArgumentsHolder(new int[] { 1, 2, 3 }, "int array"), // Custom user‑defined class
        new ArgumentsHolder(new CustomValue("alpha"), "CustomValue instance"), // Different enum constant (not NullSentinel)
        new ArgumentsHolder(OtherEnum.SOME_VALUE, "OtherEnum constant"));
    }

    /**
     * When the sentinel {@code NullSentinel.NULL_VALUE} is supplied, the method must return {@code null}.
     */
    @Test
    void unmaskNullValue_returnsNullForSentinel() throws Exception {
        // GIVEN
        Object sentinel = AbstractConcurrentNullSafeMap.NullSentinel.NULL_VALUE;
        // WHEN
        @SuppressWarnings("unchecked")
        String result = (String) unmaskNullValueMethod.invoke(map, sentinel);
        // THEN
        assertNull(result, "unmaskNullValue should convert NULL_VALUE sentinel to null");
    }

    /**
     * When a regular (non‑sentinel) value is supplied, the method must return the value unchanged
     * (after the appropriate cast).
     */
    @Test
    void unmaskNullValue_returnsValueWhenNotSentinel() throws Exception {
        // GIVEN
        String payload = "payload";
        // WHEN
        @SuppressWarnings("unchecked")
        String result = (String) unmaskNullValueMethod.invoke(map, payload);
        // THEN
        assertEquals(payload, result, "unmaskNullValue should return the original value when it is not the sentinel");
    }

    /**
     * Verify that the method does not mistakenly treat other enum constants as the null sentinel.
     */
    @Test
    void unmaskNullValue_ignoresOtherEnumConstants() throws Exception {
        // GIVEN
        // different enum constant
        Object otherSentinel = AbstractConcurrentNullSafeMap.NullSentinel.NULL_KEY;
        // WHEN
        @SuppressWarnings("unchecked")
        Object result = unmaskNullValueMethod.invoke(map, otherSentinel);
        // THEN
        assertSame(otherSentinel, result, "Only NULL_VALUE should be unmasked to null; other enum constants must be returned unchanged");
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("provideValues")
    void unmaskNullValue_returnsInputUnchangedOrNull(ArgumentsHolder holder) throws Exception {
        // GIVEN
        Object input = holder.input;
        // WHEN
        Object result = unmaskNullValueMethod.invoke(map, input);
        // THEN
        if (input == null) {
            assertNull(result, "null input must result in null");
        } else {
            assertSame(input, result, "Method should return the exact same instance for non‑sentinel values");
        }
    }
}
