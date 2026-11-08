package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DeepEquals_hasCustomHashCode_Test {

    /**
     * A subclass that does **not** override {@code hashCode()}, but inherits it from its parent.
     */
    static class SubOfWithCustomHash extends WithCustomHash {
        // no hashCode() here
    }

    /**
     * A class that does NOT override {@code hashCode()}.
     */
    static class WithoutCustomHash {
        // default Object.hashCode()
    }

    /**
     * A class that overrides {@code hashCode()}.
     */
    static class ClassWithCustomHash {

        @Override
        public int hashCode() {
            return 42;
        }
    }

    /**
     * Subclass that inherits {@code hashCode()} from its parent.
     */
    static class SubclassOfClassWithCustomHash extends ClassWithCustomHash {
        // no hashCode() here
    }

    /**
     * A class that does NOT override {@code hashCode()}.
     */
    static class ClassWithoutCustomHash {
        // default Object.hashCode()
    }

    /**
     * Subclass of a class without a custom hashCode.
     */
    static class SubclassOfClassWithoutCustomHash extends ClassWithoutCustomHash {
        // no hashCode() here
    }

    /**
     * Abstract class that overrides {@code hashCode()}.
     */
    abstract static class AbstractClassWithCustomHash {

        @Override
        public int hashCode() {
            return 123;
        }
    }

    /**
     * Class that overloads {@code hashCode(int)} but does NOT declare the no‑arg version.
     */
    static class ClassWithOverloadedHashCode {

        // overload, not an override
        public int hashCode(int dummy) {
            return dummy;
        }
    }

    /**
     * Outer class containing a private inner class that overrides {@code hashCode()}.
     */
    static class Outer {

        private static class PrivateInnerWithCustomHash {

            @Override
            public int hashCode() {
                return 777;
            }
        }
    }

    private static class PrivateInnerWithCustomHash {

        @Override
        public int hashCode() {
            return 777;
        }
    }

    private static class PrivateInnerWithCustomHash {

        @Override
        public int hashCode() {
            return 777;
        }
    }

    /* --------------------------------------------------------------------- */
    private static class ArgumentsHolder {

        final Class<?> clazz;

        final boolean expected;

        final String description;

        ArgumentsHolder(Class<?> clazz, boolean expected, String description) {
            this.clazz = clazz;
            this.expected = expected;
            this.description = description;
        }
    }

    /**
     * A class that overrides {@code hashCode()}.
     */
    static class WithCustomHash {

        @Override
        public int hashCode() {
            return 42;
        }
    }

    /**
     * Clears the internal cache after each test to avoid cross‑test pollution.
     */
    @AfterEach
    void teardownAfterEach() throws Exception {
        clearCustomHashCache();
    }

    /**
     * Helper that empties the private static cache used by {@code hasCustomHashCode}.
     */
    private static void clearCustomHashCache() throws Exception {
        Field field = DeepEquals.class.getDeclaredField("_customHash");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Boolean> map = (Map<String, Boolean>) field.get(null);
        map.clear();
    }

    /**
     * Data provider for the "Custom hash detection" group.
     */
    static Stream<ArgumentsHolder> customHashDetectionProvider() {
        return Stream.of(new ArgumentsHolder(ClassWithCustomHash.class, true, "Class directly declares its own hashCode() method, so hasCustomHashCode should return true."), new ArgumentsHolder(SubclassOfClassWithCustomHash.class, true, "Subclass does not declare hashCode() but its superclass does; the loop climbs to the superclass and returns true."), new ArgumentsHolder(ClassWithoutCustomHash.class, false, "Class (and its super‑classes up to Object) do not declare a hashCode() method, so the method should return false."), new ArgumentsHolder(SubclassOfClassWithoutCustomHash.class, false, "Subclass inherits no hashCode() declaration; the loop reaches Object and returns false."));
    }

    /**
     * Data provider for the "Edge cases - primitive, array, and interface types" group.
     */
    static Stream<ArgumentsHolder> edgeCasesProvider() {
        return Stream.of(new ArgumentsHolder(int.class, false, "Primitive type classes (e.g., int.class) do not declare a hashCode() method; the method should return false."), new ArgumentsHolder(String[].class, false, "Array classes inherit Object.hashCode but do not declare their own; the method should return false."), new ArgumentsHolder(java.util.List.class, false, "Interface types cannot override Object.hashCode; the method should return false."));
    }

    /**
     * Data provider for the "Enum and abstract class overrides" group.
     */
    static Stream<ArgumentsHolder> enumAndAbstractProvider() {
        return Stream.of(new ArgumentsHolder(EnumWithCustomHash.class, true, "Enum that overrides hashCode() declares its own method; hasCustomHashCode should return true."), new ArgumentsHolder(AbstractClassWithCustomHash.class, true, "Abstract class that overrides hashCode() still declares the method; the method should return true."));
    }

    /**
     * Data provider for the "Method overload vs. override detection" group.
     */
    static Stream<ArgumentsHolder> overloadDetectionProvider() {
        return Stream.of(new ArgumentsHolder(ClassWithOverloadedHashCode.class, false, "Class defines a method named hashCode(int) (overloaded) but does not declare the no‑arg hashCode(); the method should return false."));
    }

    /**
     * Data provider for the "Visibility and caching behavior" group.
     */
    static Stream<ArgumentsHolder> visibilityAndCachingProvider() {
        return Stream.of(new ArgumentsHolder(Outer.PrivateInnerWithCustomHash.class, true, "Private inner class declares its own hashCode(); visibility does not affect getDeclaredMethod, so the method should return true."), // The second case tests caching – we will call the method twice in the test body.
        new ArgumentsHolder(ClassWithCustomHash.class, true, "Calling hasCustomHashCode twice for the same class should return the cached result (true) on the second call."));
    }

    @Test
    void hasCustomHashCode_returnsTrue_forClassThatDeclaresHashCode() {
        // GIVEN
        Class<?> clazz = WithCustomHash.class;
        // WHEN
        boolean result = DeepEquals.hasCustomHashCode(clazz);
        // THEN
        assertTrue(result, "Class that declares its own hashCode() should return true");
    }

    @Test
    void hasCustomHashCode_returnsTrue_forSubclassInheritingHashCode() {
        // GIVEN
        Class<?> clazz = SubOfWithCustomHash.class;
        // WHEN
        boolean result = DeepEquals.hasCustomHashCode(clazz);
        // THEN
        assertTrue(result, "Subclass that does not declare hashCode() but inherits it from a superclass should return true");
    }

    @Test
    void hasCustomHashCode_returnsFalse_forClassWithoutCustomHashCode() {
        // GIVEN
        Class<?> clazz = WithoutCustomHash.class;
        // WHEN
        boolean result = DeepEquals.hasCustomHashCode(clazz);
        // THEN
        assertFalse(result, "Class that does not declare its own hashCode() (and none of its super‑classes except Object) should return false");
    }

    @Test
    void hasCustomHashCode_considersJavaLangString_asCustomHash() {
        // GIVEN
        // String overrides hashCode()
        Class<?> clazz = String.class;
        // WHEN
        boolean result = DeepEquals.hasCustomHashCode(clazz);
        // THEN
        assertTrue(result, "java.lang.String overrides hashCode() and should be reported as having a custom hashCode");
    }

    @Test
    void hasCustomHashCode_cachesResult_andReturnsSameValueOnSubsequentCalls() {
        // GIVEN
        Class<?> clazz = WithCustomHash.class;
        // WHEN - first call populates the cache
        boolean firstCall = DeepEquals.hasCustomHashCode(clazz);
        // WHEN - second call should hit the cache
        boolean secondCall = DeepEquals.hasCustomHashCode(clazz);
        // THEN
        assertTrue(firstCall, "First call should detect custom hashCode");
        assertTrue(secondCall, "Second call should return the cached result");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("customHashDetectionProvider")
    void testCustomHashDetection(ArgumentsHolder holder) {
        boolean result = DeepEquals.hasCustomHashCode(holder.clazz);
        if (holder.expected) {
            assertTrue(result, holder.description);
        } else {
            assertFalse(result, holder.description);
        }
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("edgeCasesProvider")
    void testEdgeCases(ArgumentsHolder holder) {
        boolean result = DeepEquals.hasCustomHashCode(holder.clazz);
        if (holder.expected) {
            assertTrue(result, holder.description);
        } else {
            assertFalse(result, holder.description);
        }
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("enumAndAbstractProvider")
    void testEnumAndAbstractOverrides(ArgumentsHolder holder) {
        boolean result = DeepEquals.hasCustomHashCode(holder.clazz);
        if (holder.expected) {
            assertTrue(result, holder.description);
        } else {
            assertFalse(result, holder.description);
        }
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("overloadDetectionProvider")
    void testOverloadDetection(ArgumentsHolder holder) {
        boolean result = DeepEquals.hasCustomHashCode(holder.clazz);
        if (holder.expected) {
            assertTrue(result, holder.description);
        } else {
            assertFalse(result, holder.description);
        }
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("visibilityAndCachingProvider")
    void testVisibilityAndCaching(ArgumentsHolder holder) {
        // first call
        boolean first = DeepEquals.hasCustomHashCode(holder.clazz);
        // second call – should hit the cache
        boolean second = DeepEquals.hasCustomHashCode(holder.clazz);
        if (holder.expected) {
            assertTrue(first, "First call: " + holder.description);
            assertTrue(second, "Second call (cached): " + holder.description);
        } else {
            assertFalse(first, "First call: " + holder.description);
            assertFalse(second, "Second call (cached): " + holder.description);
        }
    }

    /**
     * Test that passing {@code null} throws {@link NullPointerException}.
     */
    @Test
    void testNullArgumentThrowsNPE() {
        assertThrows(NullPointerException.class, () -> DeepEquals.hasCustomHashCode(null), "Passing null as the Class argument should cause a NullPointerException");
    }
}
