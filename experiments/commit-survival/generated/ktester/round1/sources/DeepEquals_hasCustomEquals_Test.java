package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DeepEquals_hasCustomEquals_Test {

    /**
     * A simple class DeepEquals_hasCustomEquals_Test{@code equals(Object)}.
     */
    private static class WithCustomEquals {

        private final int id;

        WithCustomEquals(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof WithCustomEquals))
                return false;
            return id == ((WithCustomEquals) o).id;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(id);
        }
    }

    /**
     * Subclass of {@link WithCustomEquals} that does **not** declare its own
     * {@code equals} method – it should inherit the custom implementation.
     */
    private static class SubWithoutOwnEquals extends WithCustomEquals {

        SubWithoutOwnEquals(int id) {
            super(id);
        }
    }

    /**
     * A class that does not override {@code equals(Object)}.
     */
    private static class WithoutCustomEquals {

        private final String value;

        WithoutCustomEquals(String value) {
            this.value = value;
        }
    }

    /**
     * Package‑private holder for the test classes referenced by fully‑qualified names.
     */
    static final class DeepEqualsTestClasses {

        /**
         * Directly overrides {@code equals(Object)}.
         */
        public static class CustomEqualsDirect {

            private final int id;

            public CustomEqualsDirect(int id) {
                this.id = id;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o)
                    return true;
                if (!(o instanceof CustomEqualsDirect))
                    return false;
                return id == ((CustomEqualsDirect) o).id;
            }

            @Override
            public int hashCode() {
                return Integer.hashCode(id);
            }
        }

        /**
         * Superclass that overrides {@code equals(Object)}.
         */
        public static class CustomEqualsBase {

            private final String name;

            public CustomEqualsBase(String name) {
                this.name = name;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o)
                    return true;
                if (!(o instanceof CustomEqualsBase))
                    return false;
                return name.equals(((CustomEqualsBase) o).name);
            }

            @Override
            public int hashCode() {
                return name.hashCode();
            }
        }

        /**
         * Does **not** declare its own {@code equals}, inherits from {@link CustomEqualsBase}.
         */
        public static class CustomEqualsInherited extends CustomEqualsBase {

            public CustomEqualsInherited(String name) {
                super(name);
            }
        }

        /**
         * No custom {@code equals} implementation – relies on {@code Object.equals}.
         */
        public static class NoCustomEquals {

            private final String payload;

            public NoCustomEquals(String payload) {
                this.payload = payload;
            }
        }

        /**
         * Abstract superclass that overrides {@code equals}.
         */
        public static abstract class AbstractCustomEquals {

            private final int value;

            protected AbstractCustomEquals(int value) {
                this.value = value;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o)
                    return true;
                if (o == null || getClass() != o.getClass())
                    return false;
                AbstractCustomEquals that = (AbstractCustomEquals) o;
                return value == that.value;
            }

            @Override
            public int hashCode() {
                return Integer.hashCode(value);
            }
        }

        /**
         * Concrete subclass of the abstract class – does not override {@code equals}.
         */
        public static class AbstractCustomEqualsSubclass extends AbstractCustomEquals {

            public AbstractCustomEqualsSubclass(int value) {
                super(value);
            }
        }
    }

    /**
     * Directly overrides {@code equals(Object)}.
     */
    public static class CustomEqualsDirect {

        private final int id;

        public CustomEqualsDirect(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof CustomEqualsDirect))
                return false;
            return id == ((CustomEqualsDirect) o).id;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(id);
        }
    }

    /**
     * Superclass that overrides {@code equals(Object)}.
     */
    public static class CustomEqualsBase {

        private final String name;

        public CustomEqualsBase(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof CustomEqualsBase))
                return false;
            return name.equals(((CustomEqualsBase) o).name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }

    /**
     * Does **not** declare its own {@code equals}, inherits from {@link CustomEqualsBase}.
     */
    public static class CustomEqualsInherited extends CustomEqualsBase {

        public CustomEqualsInherited(String name) {
            super(name);
        }
    }

    /**
     * No custom {@code equals} implementation – relies on {@code Object.equals}.
     */
    public static class NoCustomEquals {

        private final String payload;

        public NoCustomEquals(String payload) {
            this.payload = payload;
        }
    }

    /**
     * Abstract superclass that overrides {@code equals}.
     */
    public static abstract class AbstractCustomEquals {

        private final int value;

        protected AbstractCustomEquals(int value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            AbstractCustomEquals that = (AbstractCustomEquals) o;
            return value == that.value;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(value);
        }
    }

    /**
     * Concrete subclass of the abstract class – does not override {@code equals}.
     */
    public static class AbstractCustomEqualsSubclass extends AbstractCustomEquals {

        public AbstractCustomEqualsSubclass(int value) {
            super(value);
        }
    }

    /**
     * Directly overrides {@code equals(Object)}.
     */
    public static class CustomEqualsDirect {

        private final int id;

        public CustomEqualsDirect(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof CustomEqualsDirect))
                return false;
            return id == ((CustomEqualsDirect) o).id;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(id);
        }
    }

    /**
     * Superclass that overrides {@code equals(Object)}.
     */
    public static class CustomEqualsBase {

        private final String name;

        public CustomEqualsBase(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof CustomEqualsBase))
                return false;
            return name.equals(((CustomEqualsBase) o).name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }

    /**
     * Does **not** declare its own {@code equals}, inherits from {@link CustomEqualsBase}.
     */
    public static class CustomEqualsInherited extends CustomEqualsBase {

        public CustomEqualsInherited(String name) {
            super(name);
        }
    }

    /**
     * No custom {@code equals} implementation – relies on {@code Object.equals}.
     */
    public static class NoCustomEquals {

        private final String payload;

        public NoCustomEquals(String payload) {
            this.payload = payload;
        }
    }

    /**
     * Abstract superclass that overrides {@code equals}.
     */
    public static abstract class AbstractCustomEquals {

        private final int value;

        protected AbstractCustomEquals(int value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            AbstractCustomEquals that = (AbstractCustomEquals) o;
            return value == that.value;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(value);
        }
    }

    /**
     * Concrete subclass of the abstract class – does not override {@code equals}.
     */
    public static class AbstractCustomEqualsSubclass extends AbstractCustomEquals {

        public AbstractCustomEqualsSubclass(int value) {
            super(value);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Clear the static cache so each test runs in a clean state.
        Field cacheField = DeepEquals.class.getDeclaredField("_customEquals");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Boolean> cache = (Map<String, Boolean>) cacheField.get(null);
        cache.clear();
    }

    private static Stream<Arguments> provideHasCustomEqualsCases() {
        return Stream.of(// Direct override – first call (cache miss)
        Arguments.of("com.cedarsoftware.util.DeepEqualsTestClasses.CustomEqualsDirect", true, false), // Direct override – second call (cache hit)
        Arguments.of("com.cedarsoftware.util.DeepEqualsTestClasses.CustomEqualsDirect", true, true), // Inherited override – first call
        Arguments.of("com.cedarsoftware.util.DeepEqualsTestClasses.CustomEqualsInherited", true, false), // Inherited override – second call (cache hit)
        Arguments.of("com.cedarsoftware.util.DeepEqualsTestClasses.CustomEqualsInherited", true, true), // No custom equals – first call
        Arguments.of("com.cedarsoftware.util.DeepEqualsTestClasses.NoCustomEquals", false, false), // No custom equals – second call (cache hit)
        Arguments.of("com.cedarsoftware.util.DeepEqualsTestClasses.NoCustomEquals", false, true), // Abstract superclass override – single call
        Arguments.of("com.cedarsoftware.util.DeepEqualsTestClasses.AbstractCustomEqualsSubclass", true, false), // Primitive wrapper – Integer (no custom equals)
        Arguments.of("java.lang.Integer", false, false), // Array class – String[]
        Arguments.of("[Ljava.lang.String;", false, false), // Null argument – expect NPE
        Arguments.of(null, NullPointerException.class, false), // Primitive class – int
        Arguments.of("int", NullPointerException.class, false), // Interface class – java.lang.Runnable
        Arguments.of("java.lang.Runnable", NullPointerException.class, false));
    }

    @Test
    @DisplayName("hasCustomEquals returns true for a class DeepEquals_hasCustomEquals_Testequals")
    void testHasCustomEqualsWhenEqualsOverridden() {
        // GIVEN
        Class<?> clazz = WithCustomEquals.class;
        // WHEN
        boolean result = DeepEquals.hasCustomEquals(clazz);
        // THEN
        assertTrue(result, "Class DeepEquals_hasCustomEquals_Testequals() should be reported as having a custom equals");
    }

    @Test
    @DisplayName("hasCustomEquals returns true for a subclass inheriting a custom equals")
    void testHasCustomEqualsInheritedFromSuperclass() {
        // GIVEN
        Class<?> subclass = SubWithoutOwnEquals.class;
        // WHEN
        boolean result = DeepEquals.hasCustomEquals(subclass);
        // THEN
        assertTrue(result, "Subclass that does not declare equals() but inherits it from a superclass should be reported as having a custom equals");
    }

    @Test
    @DisplayName("hasCustomEquals returns false for a class without a custom equals")
    void testHasCustomEqualsWhenNoCustomEquals() {
        // GIVEN
        Class<?> clazz = WithoutCustomEquals.class;
        // WHEN
        boolean result = DeepEquals.hasCustomEquals(clazz);
        // THEN
        assertFalse(result, "Class without an overridden equals() should not be reported as having a custom equals");
    }

    @Test
    @DisplayName("hasCustomEquals throws NullPointerException when supplied null")
    void testHasCustomEqualsWithNullArgument() {
        // GIVEN
        Class<?> nullClass = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> DeepEquals.hasCustomEquals(nullClass), "Calling hasCustomEquals(null) should throw NullPointerException");
    }

    /**
     * Core parameterised test that executes {@link DeepEquals#hasCustomEquals(Class)} for
     * each scenario described in the JSON input.
     *
     * @param className   Fully‑qualified class name (or special descriptors for arrays/primitive).
     * @param expected    Either a {@code Boolean} (expected result) or an {@code Exception} class.
     * @param secondCall  {@code true} if the method should be invoked a second time to verify caching.
     */
    @ParameterizedTest(name = "[{index}] class={0}, expected={1}, secondCall={2}")
    @MethodSource("provideHasCustomEqualsCases")
    void testHasCustomEquals(String className, Object expected, boolean secondCall) throws Exception {
        Class<?> clazz = null;
        // Resolve the class name unless the test is deliberately passing null.
        if (className != null) {
            // Handles array descriptors like "[Ljava.lang.String;"
            clazz = Class.forName(className);
        }
        // ------------------------------------------------------------------
        // First invocation – either expect a boolean result or an exception.
        // ------------------------------------------------------------------
        if (expected instanceof Class && Throwable.class.isAssignableFrom((Class<?>) expected)) {
            // Expected exception path
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            assertThrows(exClass, () -> DeepEquals.hasCustomEquals(clazz), () -> "Expected " + exClass.getSimpleName() + " for class " + className);
        } else {
            // Expected boolean result
            boolean expectedBool = (Boolean) expected;
            boolean result = DeepEquals.hasCustomEquals(clazz);
            assertEquals(expectedBool, result, () -> "hasCustomEquals returned unexpected result for class " + className);
        }
        // ------------------------------------------------------------------
        // Optional second call to verify that the cached value is used.
        // ------------------------------------------------------------------
        if (secondCall) {
            if (expected instanceof Class && Throwable.class.isAssignableFrom((Class<?>) expected)) {
                Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
                assertThrows(exClass, () -> DeepEquals.hasCustomEquals(clazz), () -> "Expected " + exClass.getSimpleName() + " on cached call for class " + className);
            } else {
                boolean expectedBool = (Boolean) expected;
                boolean cachedResult = DeepEquals.hasCustomEquals(clazz);
                assertEquals(expectedBool, cachedResult, () -> "Cached result differs for class " + className);
            }
        }
    }

    @Test
    @DisplayName("hasCustomEquals returns true for a class that directly overrides equals")
    void directOverride() {
        assertTrue(DeepEquals.hasCustomEquals(DeepEqualsTestClasses.CustomEqualsDirect.class));
    }

    @Test
    @DisplayName("hasCustomEquals returns true for a subclass inheriting a custom equals")
    void inheritedOverride() {
        assertTrue(DeepEquals.hasCustomEquals(DeepEqualsTestClasses.CustomEqualsInherited.class));
    }

    @Test
    @DisplayName("hasCustomEquals returns false for a class without a custom equals")
    void noCustomEquals() {
        assertFalse(DeepEquals.hasCustomEquals(DeepEqualsTestClasses.NoCustomEquals.class));
    }

    @Test
    @DisplayName("hasCustomEquals throws NullPointerException when supplied null")
    void nullArgument() {
        assertThrows(NullPointerException.class, () -> DeepEquals.hasCustomEquals(null));
    }
}
