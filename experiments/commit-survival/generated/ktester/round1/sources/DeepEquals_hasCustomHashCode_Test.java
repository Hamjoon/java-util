package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import com.cedarsoftware.util.test.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.stream.Stream;

class DeepEquals_hasCustomHashCode_Test {

    /**
     * No explicit {@code hashCode()} – inherits {@code Object.hashCode()}.
     */
    static class NoHashOverride {
        // empty
    }

    /**
     * Declares its own {@code hashCode()} implementation.
     */
    static class WithHashOverride {

        @Override
        public int hashCode() {
            return 42;
        }
    }

    /**
     * Does not declare {@code hashCode()}, but inherits it from {@link WithHashOverride}.
     */
    static class SubclassInherit extends WithHashOverride {
        // empty
    }

    private static void clearCache() {
        try {
            Field f = DeepEquals.class.getDeclaredField("_customHash");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Boolean> map = (Map<String, Boolean>) f.get(null);
            map.clear();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Stream<Arguments> provideDirectCustomHash() {
        return Stream.of(Arguments.of(CustomHashClass.class, true));
    }

    private static Stream<Arguments> provideSuperclassCustomHash() {
        return Stream.of(Arguments.of(SubClassInheritingCustomHash.class, true));
    }

    private static Stream<Arguments> provideNoCustomHash() {
        return Stream.of(Arguments.of(NoCustomHashClass.class, false));
    }

    @Test
    void shouldDetectStandardLibraryClassWithCustomHashCode() {
        // java.lang.String overrides hashCode()
        assertTrue(DeepEquals.hasCustomHashCode(String.class), "String should be reported as having a custom hashCode()");
    }

    @Test
    void shouldReturnFalseForPlainObjectClass() {
        // java.lang.Object does NOT declare its own hashCode()
        assertFalse(DeepEquals.hasCustomHashCode(Object.class), "Object should not be reported as having a custom hashCode()");
    }

    @Test
    void shouldDetectUserClassThatOverridesHashCode() {
        assertTrue(DeepEquals.hasCustomHashCode(WithHashOverride.class), "Class that declares hashCode() should be reported as having a custom hashCode()");
    }

    @Test
    void shouldDetectSubclassInheritingCustomHashCodeFromSuperclass() {
        // The subclass does not declare hashCode(), but its superclass does.
        assertTrue(DeepEquals.hasCustomHashCode(SubclassInherit.class), "Subclass should be reported as having a custom hashCode() because its superclass declares one");
    }

    @Test
    void shouldCacheResultBetweenCalls() {
        // First call populates the internal cache.
        boolean first = DeepEquals.hasCustomHashCode(NoHashOverride.class);
        // Second call should retrieve the cached value (still false).
        boolean second = DeepEquals.hasCustomHashCode(NoHashOverride.class);
        assertEquals(first, second, "Subsequent calls must return the same cached result");
    }

    @Test
    void shouldCacheResultForClassWithCustomHashCode() {
        // First call (true) caches the result.
        boolean first = DeepEquals.hasCustomHashCode(WithHashOverride.class);
        // Second call (true) should be served from cache.
        boolean second = DeepEquals.hasCustomHashCode(WithHashOverride.class);
        assertEquals(first, second, "Cached result for a class with custom hashCode() must be consistent");
    }

    @Test
    void shouldDetectDirectCustomHashCode() {
        // ensure a clean cache for this scenario
        clearCache();
        assertTrue(DeepEquals.hasCustomHashCode(CustomHashClass.class), "CustomHashClass declares its own hashCode() and should be detected as true");
    }

    @Test
    void shouldDetectSuperclassCustomHashCode() {
        clearCache();
        assertTrue(DeepEquals.hasCustomHashCode(SubClassInheritingCustomHash.class), "Subclass inherits hashCode() from its superclass and should be reported as true");
    }

    @Test
    void shouldReturnFalseWhenNoCustomHashCode() {
        clearCache();
        assertFalse(DeepEquals.hasCustomHashCode(NoCustomHashClass.class), "NoCustomHashClass and its hierarchy do not declare hashCode(), expect false");
    }

    @Test
    void shouldCacheResultForTrue() {
        clearCache();
        boolean first = DeepEquals.hasCustomHashCode(CustomHashClass.class);
        boolean second = DeepEquals.hasCustomHashCode(CustomHashClass.class);
        assertTrue(first, "First call must return true");
        assertEquals(first, second, "Second call must hit the cache and return the same true value");
    }

    @Test
    void shouldCacheResultForFalse() {
        clearCache();
        boolean first = DeepEquals.hasCustomHashCode(NoCustomHashClass.class);
        boolean second = DeepEquals.hasCustomHashCode(NoCustomHashClass.class);
        assertFalse(first, "First call must return false");
        assertEquals(first, second, "Second call must hit the cache and return the same false value");
    }

    @Test
    void shouldThrowNPEWhenClassIsNull() {
        clearCache();
        assertThrows(NullPointerException.class, () -> DeepEquals.hasCustomHashCode(null), "Passing null should result in a NullPointerException");
    }

    @Test
    void shouldReturnFalseForPrimitiveClass() {
        clearCache();
        // Primitive types cannot declare methods; expected result is false.
        assertFalse(DeepEquals.hasCustomHashCode(int.class), "Primitive int.class should be reported as having no custom hashCode()");
    }

    @Test
    void shouldReturnFalseForArrayClass() {
        clearCache();
        assertFalse(DeepEquals.hasCustomHashCode(String[].class), "Array classes inherit Object.hashCode() and should be reported as false");
    }

    @Test
    void shouldReturnFalseForInterface() {
        clearCache();
        assertFalse(DeepEquals.hasCustomHashCode(java.util.List.class), "Interfaces cannot declare an instance hashCode method; expect false");
    }

    @Test
    void shouldDetectPrivateHashCodeMethod() {
        clearCache();
        assertTrue(DeepEquals.hasCustomHashCode(PrivateHashClass.class), "A private hashCode() method still counts as a custom implementation");
    }

    @Test
    void shouldDetectHashCodeWithNonIntReturnType() {
        clearCache();
        assertTrue(DeepEquals.hasCustomHashCode(LongHashClass.class), "hashCode() returning long should still be found via getDeclaredMethod");
    }

    @Test
    void shouldDetectStaticHashCodeMethod() {
        clearCache();
        assertTrue(DeepEquals.hasCustomHashCode(StaticHashClass.class), "A static hashCode() method is considered a custom implementation");
    }

    @Test
    void shouldReturnFalseForObjectClass() {
        clearCache();
        assertFalse(DeepEquals.hasCustomHashCode(Object.class), "Object does not declare its own hashCode beyond the default");
    }

    @Test
    void shouldMaintainIndependentCacheEntries() {
        clearCache();
        // Populate cache for a class with a custom hashCode
        assertTrue(DeepEquals.hasCustomHashCode(CustomHashClass.class), "CustomHashClass should be cached as true");
        // Populate cache for a class without a custom hashCode
        assertFalse(DeepEquals.hasCustomHashCode(NoCustomHashClass.class), "NoCustomHashClass should be cached as false");
        // Verify that both cached values are still correct
        assertTrue(DeepEquals.hasCustomHashCode(CustomHashClass.class), "Cached true value must remain true");
        assertFalse(DeepEquals.hasCustomHashCode(NoCustomHashClass.class), "Cached false value must remain false");
    }

    @ParameterizedTest(name = "{index} => class={0}, expected={1}")
    @MethodSource("provideDirectCustomHash")
    void paramDirectCustomHash(Class<?> clazz, boolean expected) {
        clearCache();
        assertEquals(expected, DeepEquals.hasCustomHashCode(clazz));
    }

    @ParameterizedTest(name = "{index} => class={0}, expected={1}")
    @MethodSource("provideSuperclassCustomHash")
    void paramSuperclassCustomHash(Class<?> clazz, boolean expected) {
        clearCache();
        assertEquals(expected, DeepEquals.hasCustomHashCode(clazz));
    }

    @ParameterizedTest(name = "{index} => class={0}, expected={1}")
    @MethodSource("provideNoCustomHash")
    void paramNoCustomHash(Class<?> clazz, boolean expected) {
        clearCache();
        assertEquals(expected, DeepEquals.hasCustomHashCode(clazz));
    }
}
