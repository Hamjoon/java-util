package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.function.Predicate;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.security.Permission;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getAllDeclaredFields_Test {

    /* ---------------------------------------------------------------------- */
    static class SuperClass {

        public int publicSuper;

        private String privateSuper;

        protected long protectedSuper;
    }

    // 1️⃣  Simple class without inheritance (for basic retrieval)
    static class SimpleClass {

        int a;

        String b;
    }

    // 2️⃣  Base class used for inheritance order tests
    static class BaseClass {

        public int baseField;

        protected String protectedField;

        public static final String CONSTANT = "CONST";
    }

    // 3️⃣  Subclass extending BaseClass – fields are declared in a specific order
    static class SubClass extends BaseClass {

        public double subField;

        // static field to be filtered out
        private static final int STATIC_SUB = 42;
    }

    // 4️⃣  Empty class – no declared fields
    static class EmptyClass {
    }

    // 5️⃣  Class hierarchy for multiple‑level inheritance test
    static class GrandParent {

        int g;
    }

    static class Parent extends GrandParent {

        int p;
    }

    static class ChildClass extends Parent {

        int c;
    }

    // 6️⃣  Field hiding scenario – same name in super‑ and sub‑class
    static class BaseHide {

        int x;
    }

    static class HideSubClass extends BaseHide {

        // hides BaseHide.x
        int x;
    }

    // 7️⃣  Class for SecurityManager test – any reflective access must be denied
    static class SecuredClass {

        int secret;
    }

    /**
     * SecurityManager that throws on any reflective member access of {@link SecuredClass}.
     */
    static class DenySecuredClassSecurityManager extends SecurityManager {

        @Override
        public void checkMemberAccess(Class<?> clazz, int which) {
            if (clazz == SecuredClass.class) {
                throw new SecurityException("Access denied");
            }
        }

        // Allow everything else
        @Override
        public void checkPermission(Permission perm) {
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
        }
    }

    /* ---------------------------------------------------------------------- */
    @BeforeEach
    void clearCache() throws Exception {
        // The cache is a private static volatile field; replace it with a fresh map.
        java.lang.reflect.Field cacheField = ReflectionUtils.class.getDeclaredField("FIELDS_CACHE");
        cacheField.setAccessible(true);
        cacheField.set(null, new ConcurrentHashMap<>());
    }

    /**
     * Extracts field names preserving order.
     */
    private static List<String> fieldNames(List<Field> fields) {
        List<String> names = new ArrayList<>(fields.size());
        for (Field f : fields) {
            names.add(f.getName());
        }
        return names;
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void getAllDeclaredFields_nullClass_throwsException() {
        Predicate<Field> any = f -> true;
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getAllDeclaredFields(null, any));
        assertTrue(ex.getMessage().contains("class cannot be null"));
    }

    @Test
    void getAllDeclaredFields_nullPredicate_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getAllDeclaredFields(SimpleClass.class, null));
        assertTrue(ex.getMessage().contains("fieldFilter cannot be null"));
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void getAllDeclaredFields_includesSuperClassFields() {
        List<Field> fields = ReflectionUtils.getAllDeclaredFields(SubClass.class, f -> true);
        // Expected fields: publicSuper, privateSuper, protectedSuper, publicSub, privateSub
        assertEquals(5, fields.size(), "All declared fields from SubClass and its super‑class should be returned");
        // Verify that a known field from the super‑class is present
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("publicSuper")));
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("privateSub")));
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void getAllDeclaredFields_respectsPredicate() {
        // Keep only public fields
        Predicate<Field> publicOnly = f -> Modifier.isPublic(f.getModifiers());
        List<Field> publicFields = ReflectionUtils.getAllDeclaredFields(SubClass.class, publicOnly);
        // Expected public fields: publicSuper, publicSub
        assertEquals(2, publicFields.size(), "Only the two public fields should be returned");
        assertTrue(publicFields.stream().anyMatch(f -> f.getName().equals("publicSuper")));
        assertTrue(publicFields.stream().anyMatch(f -> f.getName().equals("publicSub")));
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void getAllDeclaredFields_returnsUnmodifiableList() {
        List<Field> fields = ReflectionUtils.getAllDeclaredFields(SubClass.class, f -> true);
        assertThrows(UnsupportedOperationException.class, () -> fields.add(null));
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void getAllDeclaredFields_isCached() {
        List<Field> firstCall = ReflectionUtils.getAllDeclaredFields(SubClass.class, f -> true);
        List<Field> secondCall = ReflectionUtils.getAllDeclaredFields(SubClass.class, f -> true);
        // The cache stores the exact list instance, so reference equality is expected
        assertSame(firstCall, secondCall, "Subsequent invocations with identical arguments should return the cached list");
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void getAllDeclaredFields_basicRetrieval_noInheritance() {
        List<Field> fields = ReflectionUtils.getAllDeclaredFields(SimpleClass.class, f -> true);
        List<String> names = fieldNames(fields);
        assertEquals(Set.of("a", "b"), new HashSet<>(names), "SimpleClass should expose exactly fields a and b");
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void getAllDeclaredFields_inheritanceAndOrder() {
        List<Field> fields = ReflectionUtils.getAllDeclaredFields(SubClass.class, f -> true);
        List<String> names = fieldNames(fields);
        // Expected order: SubClass fields, then BaseClass fields (as defined in source)
        List<String> expected = Arrays.asList("subField", "STATIC_SUB", "baseField", "protectedField", "CONSTANT");
        assertEquals(expected, names, "Fields must be collected subclass‑first, preserving declaration order");
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void getAllDeclaredFields_predicateFiltersStaticFields() {
        Predicate<Field> nonStatic = f -> !Modifier.isStatic(f.getModifiers());
        List<Field> fields = ReflectionUtils.getAllDeclaredFields(SubClass.class, nonStatic);
        List<String> names = fieldNames(fields);
        assertFalse(names.contains("STATIC_SUB"));
        assertFalse(names.contains("CONSTANT"));
        assertTrue(names.containsAll(Arrays.asList("subField", "baseField", "protectedField")));
    }

    @Test
    void getAllDeclaredFields_predicateSelectsByName() {
        Predicate<Field> onlyBase = f -> f.getName().equals("baseField");
        List<Field> fields = ReflectionUtils.getAllDeclaredFields(SubClass.class, onlyBase);
        assertEquals(1, fields.size());
        assertEquals("baseField", fields.get(0).getName());
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void getAllDeclaredFields_emptyClass_returnsEmptyList() {
        List<Field> fields = ReflectionUtils.getAllDeclaredFields(EmptyClass.class, f -> true);
        assertTrue(fields.isEmpty(), "EmptyClass has no declared fields");
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void getAllDeclaredFields_primitiveType_returnsEmpty() {
        List<Field> fields = ReflectionUtils.getAllDeclaredFields(int.class, f -> true);
        assertTrue(fields.isEmpty());
    }

    @Test
    void getAllDeclaredFields_arrayType_returnsEmpty() {
        List<Field> fields = ReflectionUtils.getAllDeclaredFields(String[].class, f -> true);
        assertTrue(fields.isEmpty());
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void getAllDeclaredFields_fieldHiding_returnsBoth() {
        List<Field> fields = ReflectionUtils.getAllDeclaredFields(HideSubClass.class, f -> true);
        List<String> names = fieldNames(fields);
        assertEquals(Arrays.asList("x", "x"), names, "Both subclass and superclass fields named 'x' should be present, subclass first");
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void getAllDeclaredFields_multipleInheritanceLevels() {
        List<Field> fields = ReflectionUtils.getAllDeclaredFields(ChildClass.class, f -> true);
        List<String> names = fieldNames(fields);
        assertEquals(Arrays.asList("c", "p", "g"), names);
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void getAllDeclaredFields_predicateSideEffect() {
        List<String> visited = new ArrayList<>();
        Predicate<Field> recording = f -> {
            visited.add(f.getName());
            return true;
        };
        List<Field> fields = ReflectionUtils.getAllDeclaredFields(ChildClass.class, recording);
        List<String> names = fieldNames(fields);
        assertEquals(Arrays.asList("c", "p", "g"), names);
        assertEquals(names, visited, "Predicate must be invoked in the same order as the returned list");
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void getAllDeclaredFields_predicateThrows_propagatesException() {
        Predicate<Field> bad = f -> {
            if (f.getName().equals("a")) {
                throw new IllegalStateException("bad field");
            }
            return true;
        };
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> ReflectionUtils.getAllDeclaredFields(SimpleClass.class, bad));
        assertEquals("bad field", ex.getMessage());
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void getAllDeclaredFields_caching_samePredicateInstance() {
        Predicate<Field> shared = f -> true;
        List<Field> first = ReflectionUtils.getAllDeclaredFields(SubClass.class, shared);
        List<Field> second = ReflectionUtils.getAllDeclaredFields(SubClass.class, shared);
        assertSame(first, second, "Identical predicate instance must hit the cache");
    }

    @Test
    void getAllDeclaredFields_caching_differentPredicateInstances() {
        Predicate<Field> p1 = f -> true;
        // distinct instance, same logic
        Predicate<Field> p2 = f -> true;
        List<Field> first = ReflectionUtils.getAllDeclaredFields(SubClass.class, p1);
        List<Field> second = ReflectionUtils.getAllDeclaredFields(SubClass.class, p2);
        assertNotSame(first, second, "Different predicate instances create distinct cache entries");
        // Content must still be identical
        assertEquals(fieldNames(first), fieldNames(second));
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void getAllDeclaredFields_securityManagerDeniesAccess() {
        SecurityManager original = System.getSecurityManager();
        System.setSecurityManager(new DenySecuredClassSecurityManager());
        try {
            SecurityException ex = assertThrows(SecurityException.class, () -> ReflectionUtils.getAllDeclaredFields(SecuredClass.class, f -> true));
            assertTrue(ex.getMessage().contains("Access denied"));
        } finally {
            // Restore original security manager to avoid side‑effects on other tests
            System.setSecurityManager(original);
        }
    }
}
