package com.cedarsoftware.util;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.mockito.junit.jupiter.MockitoExtension;
// <-- missing semicolon added
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.BeforeEach;

@ExtendWith(MockitoExtension.class)
class DeepEquals_deepEquals_2_Test {

    /**
     * Shared options map used by several tests – recreated before each test.
     */
    private Map<String, Object> options;

    /**
     * Simple POJO with a custom {@code equals} implementation.
     */
    private static final class Person {

        private final String name;

        private final int age;

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        /**
         * Custom equals that only checks the name – we will optionally ignore it.
         */
        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Person))
                return false;
            Person p = (Person) o;
            return Objects.equals(name, p.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }

    /**
     * A class whose {@code equals} always returns {@code false}.
     */
    private static final class AlwaysFalseEquals {

        private final String value;

        AlwaysFalseEquals(String value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            // deliberately ignore fields
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    /**
     * Simple node used for cyclic‑reference tests.
     */
    private static final class Node {

        Node next;
    }

    /**
     * Simple holder for arguments used by the helper streams.
     */
    private static final class Arguments {

        final String description;

        final Object a;

        final Object b;

        final boolean expected;

        // only for the string‑number tests
        final Boolean optionEnabled;

        private Arguments(String description, Object a, Object b, boolean expected) {
            this(description, a, b, null, expected);
        }

        private Arguments(String description, Object a, Object b, Boolean optionEnabled, boolean expected) {
            this.description = description;
            this.a = a;
            this.b = b;
            this.expected = expected;
            this.optionEnabled = optionEnabled;
        }

        static Arguments of(String description, Object a, Object b, boolean expected) {
            return new Arguments(description, a, b, expected);
        }

        static Arguments of(String description, Object a, Object b, boolean optionEnabled, boolean expected) {
            return new Arguments(description, a, b, optionEnabled, expected);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        options = new HashMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to avoid cross‑test contamination.
        options.clear();
    }

    static Stream<Arguments> nullAndIdentityProvider() {
        Object obj = new Object();
        return Stream.of(Arguments.of("Both null → true", null, null, true), Arguments.of("One null, one non‑null → false", obj, null, false), Arguments.of("Same instance → true", obj, obj, true));
    }

    static Stream<Arguments> primitiveWrapperProvider() {
        return Stream.of(Arguments.of("Integer vs Long with same value → true", Integer.valueOf(42), Long.valueOf(42L), true), Arguments.of("Double vs Float with same value → true", Double.valueOf(3.14), Float.valueOf(3.14f), true), Arguments.of("Boolean true vs false → false", Boolean.TRUE, Boolean.FALSE, false));
    }

    static Stream<Arguments> stringNumberProvider() {
        return Stream.of(Arguments.of("String vs Integer without option → false", "123", Integer.valueOf(123), false, false), Arguments.of("String vs Integer with option → true", "123", Integer.valueOf(123), true, true), Arguments.of("String vs Double with option → true", "12.5", Double.valueOf(12.5), true, true));
    }
}
