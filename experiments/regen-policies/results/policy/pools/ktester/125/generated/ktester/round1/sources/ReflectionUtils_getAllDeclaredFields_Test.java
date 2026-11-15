package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Predicate;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getAllDeclaredFields_Test {

    /**
     * --------------------------------------------------------------------
     *  Test fixtures – simple class hierarchy with a few fields that we can
     *  use to verify that the method walks the inheritance chain and respects
     *  the supplied predicate.
     *  -------------------------------------------------------------------
     */
    static class SuperClass {

        public int superPublic;

        private String superPrivate;
    }

    static class SubClass extends SuperClass {

        protected double subProtected;

        // default (package‑private) visibility
        String subPackage;
    }

    class Simple {

        int a;

        String b;
    }

    /* ----------------------------------------------------------------------
     *  Helper classes for the various test scenarios.
     * ---------------------------------------------------------------------- */
    static class SimpleClass {

        int a;

        String b;
    }

    static class ClassWithStatic {

        static int staticField;

        int instanceField;
    }

    static class SubClassWithShadow extends SimpleClass {

        // Shadow field with same name as in SimpleClass
        // hides SimpleClass.a
        int a;

        int subField;

        int baseField;
    }

    interface TestInterface {

        // implicitly public static final
        int INTERFACE_CONST = 42;
    }

    static class ClassImplementsInterface implements TestInterface {

        int instanceField;
    }

    static class EmptyClass {
        // No fields declared
    }

    static class AnotherClass {

        long x;
    }

    /**
     * --------------------------------------------------------------------
     *  Helper to clear the internal static cache between tests. The cache is a
     *  private static field named “FIELDS_CACHE” in ReflectionUtils.
     *  -------------------------------------------------------------------
     */
    @BeforeEach
    void clearCache() throws Exception {
        var cacheField = ReflectionUtils.class.getDeclaredField("FIELDS_CACHE");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<?, ?> cache = (Map<?, ?>) cacheField.get(null);
        cache.clear();
    }

    static Stream<Arguments> basicFieldCollectionProvider() {
        Predicate<Field> acceptAll = f -> true;
        Predicate<Field> nonStatic = f -> !Modifier.isStatic(f.getModifiers());
        return Stream.of(Arguments.of("Collect all declared fields of SimpleClass", SimpleClass.class, acceptAll, Set.of("a", "b")), Arguments.of("Filter out static fields from ClassWithStatic", ClassWithStatic.class, nonStatic, Set.of("instanceField")));
    }

    /**
     * --------------------------------------------------------------------
     *  1️⃣  Verify that a null class argument causes the expected exception.
     *  -------------------------------------------------------------------
     */
    @Test
    void getAllDeclaredFields_nullClass_throws() {
        Predicate<Field> any = f -> true;
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getAllDeclaredFields(null, any));
        assertEquals("class cannot be null", ex.getMessage());
    }

    /**
     * --------------------------------------------------------------------
     *  2️⃣  Verify that a null predicate argument causes the expected exception.
     *  -------------------------------------------------------------------
     */
    @Test
    void getAllDeclaredFields_nullPredicate_throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getAllDeclaredFields(SubClass.class, null));
        assertEquals("fieldFilter cannot be null", ex.getMessage());
    }

    /**
     * --------------------------------------------------------------------
     *  3️⃣  Verify that the method returns **all** declared fields from the
     *      class hierarchy and that the supplied predicate is honoured.
     *  -------------------------------------------------------------------
     */
    @Test
    void getAllDeclaredFields_collectsAndFiltersCorrectly() {
        // Predicate that keeps only fields whose name contains “Public” or
        // “Protected”. This should drop the private and package‑private fields.
        Predicate<Field> filter = f -> f.getName().contains("Public") || f.getName().contains("Protected");
        List<Field> result = ReflectionUtils.getAllDeclaredFields(SubClass.class, filter);
        // Expected fields: SuperClass.superPublic, SubClass.subProtected
        Set<String> expectedNames = Set.of("superPublic", "subProtected");
        Set<String> actualNames = new HashSet<>();
        for (Field f : result) {
            actualNames.add(f.getName());
        }
        assertEquals(expectedNames, actualNames, "The returned field set should contain only the filtered fields from the whole hierarchy");
    }

    /**
     * --------------------------------------------------------------------
     *  4️⃣  Verify caching behaviour:
     *      • The same List instance is returned on subsequent calls.
     *      • The returned list is unmodifiable.
     *  -------------------------------------------------------------------
     */
    @Test
    void getAllDeclaredFields_cachesResultAndIsUnmodifiable() {
        Predicate<Field> acceptAll = f -> true;
        List<Field> firstCall = ReflectionUtils.getAllDeclaredFields(SubClass.class, acceptAll);
        List<Field> secondCall = ReflectionUtils.getAllDeclaredFields(SubClass.class, acceptAll);
        // The cache stores the exact List instance, so the two references must be identical.
        assertSame(firstCall, secondCall, "Subsequent calls should return the same cached List instance");
        // The list is supposed to be unmodifiable – attempting to modify it must fail.
        assertThrows(UnsupportedOperationException.class, () -> firstCall.add(SubClass.class.getDeclaredField("subProtected")), "The cached list must be unmodifiable");
    }

    /**
     * --------------------------------------------------------------------
     *  5️⃣  Verify that fields declared **only** in the target class are also
     *      returned (i.e. the method does not require a superclass).
     *  -------------------------------------------------------------------
     */
    @Test
    void getAllDeclaredFields_classWithoutSuperclass() {
        class Simple {

            int a;

            String b;
        }
        List<Field> fields = ReflectionUtils.getAllDeclaredFields(Simple.class, f -> true);
        Set<String> names = new HashSet<>();
        for (Field f : fields) {
            names.add(f.getName());
        }
        assertEquals(Set.of("a", "b"), names);
    }

    /* ----------------------------------------------------------------------
     *  2️⃣  Basic field collection without inheritance
     * ---------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("basicFieldCollectionProvider")
    void getAllDeclaredFields_basicCollection(String description, Class<?> target, Predicate<Field> filter, Set<String> expectedNames) {
        List<Field> result = ReflectionUtils.getAllDeclaredFields(target, filter);
        Set<String> actual = new HashSet<>();
        for (Field f : result) {
            actual.add(f.getName());
        }
        assertEquals(expectedNames, actual, description);
    }

    /* ----------------------------------------------------------------------
     *  3️⃣  Inheritance handling (including shadowed fields)
     * ---------------------------------------------------------------------- */
    @Test
    void getAllDeclaredFields_inheritanceWalksHierarchy() {
        Predicate<Field> acceptAll = f -> true;
        List<Field> fields = ReflectionUtils.getAllDeclaredFields(SubClass.class, acceptAll);
        // Expected order: subclass fields first, then superclass fields
        List<String> expectedOrder = List.of("subProtected", "subPackage", "superPublic", "superPrivate");
        List<String> actualOrder = new ArrayList<>();
        for (Field f : fields) {
            actualOrder.add(f.getName());
        }
        assertEquals(expectedOrder, actualOrder);
    }

    @Test
    void getAllDeclaredFields_inheritanceWithShadowedField() {
        Predicate<Field> acceptAll = f -> true;
        List<Field> fields = ReflectionUtils.getAllDeclaredFields(SubClassWithShadow.class, acceptAll);
        // Both the shadowing field (declared in subclass) and the original field (declared in superclass) must appear.
        Set<String> expected = Set.of("a", "subField", "baseField");
        Set<String> actual = new HashSet<>();
        for (Field f : fields) {
            actual.add(f.getName());
        }
        assertTrue(actual.containsAll(expected), "Both shadowed and original fields should be present");
    }

    /* ----------------------------------------------------------------------
     *  4️⃣  Interface fields handling
     * ---------------------------------------------------------------------- */
    @Test
    void getAllDeclaredFields_collectsInterfaceConstants() {
        Predicate<Field> acceptAll = f -> true;
        List<Field> fields = ReflectionUtils.getAllDeclaredFields(ClassImplementsInterface.class, acceptAll);
        Set<String> names = new HashSet<>();
        for (Field f : fields) {
            names.add(f.getName());
        }
        assertEquals(Set.of("instanceField", "INTERFACE_CONST"), names);
    }

    @Test
    void getAllDeclaredFields_filtersOutStaticInterfaceConstants() {
        Predicate<Field> nonStatic = f -> !Modifier.isStatic(f.getModifiers());
        List<Field> fields = ReflectionUtils.getAllDeclaredFields(ClassImplementsInterface.class, nonStatic);
        Set<String> names = new HashSet<>();
        for (Field f : fields) {
            names.add(f.getName());
        }
        assertEquals(Set.of("instanceField"), names);
    }

    /* ----------------------------------------------------------------------
     *  5️⃣  Empty class handling
     * ---------------------------------------------------------------------- */
    @Test
    void getAllDeclaredFields_emptyClassReturnsEmptyList() {
        Predicate<Field> acceptAll = f -> true;
        List<Field> fields = ReflectionUtils.getAllDeclaredFields(EmptyClass.class, acceptAll);
        assertTrue(fields.isEmpty(), "EmptyClass should yield an empty list");
    }

    @Test
    void getAllDeclaredFields_differentPredicatesProduceDistinctCacheEntries() {
        Predicate<Field> acceptAll = f -> true;
        Predicate<Field> rejectAll = f -> false;
        List<Field> listAll = ReflectionUtils.getAllDeclaredFields(SimpleClass.class, acceptAll);
        List<Field> listNone = ReflectionUtils.getAllDeclaredFields(SimpleClass.class, rejectAll);
        // Different predicate ⇒ different list instances
        assertNotSame(listAll, listNone);
        assertEquals(Set.of("a", "b"), new HashSet<>(listAll.stream().map(Field::getName).toList()));
        assertTrue(listNone.isEmpty(), "Predicate that rejects all fields must yield an empty list");
    }

    @Test
    void getAllDeclaredFields_cacheIsolationAcrossDifferentClasses() {
        Predicate<Field> acceptAll = f -> true;
        List<Field> simpleList = ReflectionUtils.getAllDeclaredFields(SimpleClass.class, acceptAll);
        List<Field> anotherList = ReflectionUtils.getAllDeclaredFields(AnotherClass.class, acceptAll);
        assertNotSame(simpleList, anotherList, "Cache entries must be isolated per class");
    }

    @Test
    void getAllDeclaredFields_statefulPredicateCreatesSeparateCacheEntry() {
        // Stateful predicate that always returns false but is a distinct instance
        AtomicInteger counter = new AtomicInteger(0);
        // always false
        Predicate<Field> statefulFalse = f -> counter.get() == 1;
        List<Field> result = ReflectionUtils.getAllDeclaredFields(SimpleClass.class, statefulFalse);
        assertTrue(result.isEmpty(), "Stateful predicate that always returns false should yield empty list");
    }

    /* ----------------------------------------------------------------------
     *  7️⃣  Predicate edge cases
     * ---------------------------------------------------------------------- */
    @Test
    void getAllDeclaredFields_predicateNeverMatches() {
        // impossible
        Predicate<Field> neverMatches = f -> f.getName().length() == 0;
        List<Field> result = ReflectionUtils.getAllDeclaredFields(SimpleClass.class, neverMatches);
        assertTrue(result.isEmpty(), "Predicate that never matches must return an empty list");
    }

    @Test
    void getAllDeclaredFields_predicateSelectsSingleField() {
        Predicate<Field> selectA = f -> "a".equals(f.getName());
        List<Field> result = ReflectionUtils.getAllDeclaredFields(SimpleClass.class, selectA);
        assertEquals(1, result.size());
        assertEquals("a", result.get(0).getName());
    }
}
