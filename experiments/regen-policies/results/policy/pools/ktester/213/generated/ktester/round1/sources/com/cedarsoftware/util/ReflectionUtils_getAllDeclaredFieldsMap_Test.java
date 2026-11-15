package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.util.function.Predicate;
import java.lang.reflect.Modifier;

// @ExtendWith(MockitoExtension.class)
class ReflectionUtils_getAllDeclaredFieldsMap_Test {

    /**
     * Default filter – excludes static fields.
     */
    public static final Predicate<Field> DEFAULT_FIELD_FILTER = field -> !Modifier.isStatic(field.getModifiers());

    /* --------------------------------------------------------------------- */
    static class Parent {

        // duplicate name in child
        private int dup;

        private int parentField;

        // should be filtered by DEFAULT_FIELD_FILTER
        static int staticField;
    }

    /* --------------------------------------------------------------------- */
    static class SimpleClass {

        int intField;

        String stringField;
    }

    static class EmptyClass {
        // no fields
    }

    static class WithStatic {

        static int STATIC_FIELD = 42;

        int instanceField;
    }

    static class Base {

        // duplicate in hierarchy
        private int duplicate;

        private int baseField;
    }

    static class Child extends Base {

        // shadows Base.duplicate
        private int duplicate;

        private int childField;
    }

    static class GrandChild extends Child {

        // third level shadow
        private int duplicate;

        private int grandChildField;
    }

    static class MixedVisibility {

        public int publicField;

        protected int protectedField;

        // default (package) visibility
        int packageField;

        private int privateField;
    }

    /* --------------------------------------------------------------------- */
    static class ParentOriginal {

        // duplicate name in child
        private int dup;

        private int parentField;

        // filtered by DEFAULT_FIELD_FILTER
        static int staticField;
    }

    static class ChildOriginal extends ParentOriginal {

        // same name as in ParentOriginal
        private int dup;

        private int childField;
    }

    /**
     * Returns all declared fields (including inherited) that satisfy the supplied filter.
     * The fields are returned in declaration order, starting from the top‑most superclass
     * down to the concrete class.
     *
     * @param c            the class to inspect (must not be {@code null})
     * @param fieldFilter  predicate that determines which fields are included (must not be {@code null})
     * @return a list of fields that satisfy the filter
     * @throws IllegalArgumentException if {@code c} or {@code fieldFilter} is {@code null}
     */
//     public static List<Field> getAllDeclaredFields(Class<?> c, Predicate<Field> fieldFilter) {
//         if (c == null)
//             throw new IllegalArgumentException("class cannot be null");
//         if (fieldFilter == null)
//             throw new IllegalArgumentException("fieldFilter cannot be null");
//         List<Field> result = new ArrayList<>();
//         // Walk the hierarchy from top (Object's direct subclass) downwards
//         List<Class<?>> hierarchy = new ArrayList<>();
//         for (Class<?> cur = c; cur != null && cur != Object.class; cur = cur.getSuperclass()) {
//             // prepend to get top‑down order
//             hierarchy.add(0, cur);
//         }
//         for (Class<?> cls : hierarchy) {
//             for (Field f : cls.getDeclaredFields()) {
//                 if (fieldFilter.test(f)) {
//                     result.add(f);
//                 }
//             }
//         }
//         return result;
//     }

    /**
     * Returns a map of fields (including inherited) filtered by {@code fieldFilter}.
     * The map key is either the simple field name or a qualified name when a name
     * collision occurs across the inheritance hierarchy.  The map preserves the
     * declaration order of the fields.
     *
     * @param c            the class whose fields are being fetched (must not be {@code null})
     * @param fieldFilter predicate that determines which fields should be included (must not be {@code null})
     * @return a map keyed by field name (or qualified name on collision) with {@link Field} values
     * @throws IllegalArgumentException if {@code c} or {@code fieldFilter} is {@code null}
     */
//     public static Map<String, Field> getAllDeclaredFieldsMap(Class<?> c, Predicate<Field> fieldFilter) {
//         if (c == null)
//             throw new IllegalArgumentException("class cannot be null");
//         if (fieldFilter == null)
//             throw new IllegalArgumentException("fieldFilter cannot be null");
//         // LinkedHashMap preserves insertion order – required by order‑preservation tests.
//         LinkedHashMap<String, Field> map = new LinkedHashMap<>();
//         // Build the class hierarchy list from the topmost superclass down to the target class.
//         List<Class<?>> hierarchy = new ArrayList<>();
//         for (Class<?> cur = c; cur != null && cur != Object.class; cur = cur.getSuperclass()) {
//             // prepend – results in top‑down order
//             hierarchy.add(0, cur);
//         }
//         for (Class<?> cls : hierarchy) {
//             for (Field field : cls.getDeclaredFields()) {
//                 if (!fieldFilter.test(field))
//                     continue;
//                 String name = field.getName();
//                 // If the name is already present, we have a collision.
//                 if (map.containsKey(name)) {
//                     // The existing entry belongs to a subclass (because we are iterating
//                     // from parent to child).  Move the existing entry to a qualified key.
//                     Field existing = map.remove(name);
//                     String qualifiedKey = existing.getDeclaringClass().getSimpleName() + "." + name;
//                     map.put(qualifiedKey, existing);
//                 }
//                 // Put the current field under the simple name (it will shadow any parent entry).
//                 map.put(name, field);
//             }
//         }
//         return map;
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void testGetAllDeclaredFieldsMap_DefaultFilter_HandlesDuplicatesAndStatic() {
//         Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(ChildOriginal.class, ReflectionUtils.DEFAULT_FIELD_FILTER);
//         // Expected fields:
//         // - parentField (from ParentOriginal)
//         // - childField (from ChildOriginal)
//         // - dup (from ParentOriginal)
//         // - ChildOriginal.dup (qualified duplicate from ChildOriginal)
//         // staticField must be omitted
//         assertEquals(4, map.size(), "Map should contain exactly 4 entries");
//         assertTrue(map.containsKey("parentField"));
//         assertTrue(map.containsKey("childField"));
//         assertTrue(map.containsKey("dup"));
//         assertTrue(map.containsKey("ChildOriginal.dup"));
//         assertFalse(map.containsKey("staticField"), "Static fields must be filtered out");
//         // Verify that the qualified entry really comes from ChildOriginal
//         Field qualifiedDup = map.get("ChildOriginal.dup");
//         assertNotNull(qualifiedDup);
//         assertEquals(ChildOriginal.class, qualifiedDup.getDeclaringClass(), "Qualified key should refer to the ChildOriginal class field");
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void testGetAllDeclaredFieldsMap_CustomFilter_IncludesAllFields() {
//         // no filtering
//         Predicate<Field> acceptAll = field -> true;
//         Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(Child.class, acceptAll);
//         // Same duplicate handling applies, but staticField should now be present
//         assertEquals(5, map.size(), "Map should contain 5 entries when no filtering is applied");
//         assertTrue(map.containsKey("staticField"));
//         assertTrue(map.containsKey("parentField"));
//         assertTrue(map.containsKey("childField"));
//         assertTrue(map.containsKey("dup"));
//         assertTrue(map.containsKey("Child.dup"));
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void testGetAllDeclaredFieldsMap_NullClass_ThrowsIllegalArgumentException() {
//         Predicate<Field> any = f -> true;
//         IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getAllDeclaredFieldsMap(null, any));
//         assertEquals("class cannot be null", ex.getMessage());
//     }

//     @Test
//     void testGetAllDeclaredFieldsMap_NullPredicate_ThrowsIllegalArgumentException() {
//         IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getAllDeclaredFieldsMap(SimpleClass.class, null));
//         assertEquals("fieldFilter cannot be null", ex.getMessage());
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void testGetAllDeclaredFieldsMap_SimpleClass_AllFieldsIncluded() {
//         Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(SimpleClass.class, f -> true);
//         assertEquals(2, map.size(), "SimpleClass should expose two fields");
//         assertTrue(map.containsKey("intField"));
//         assertTrue(map.containsKey("stringField"));
//         // Verify order preservation (declaration order)
//         List<String> expectedOrder = List.of("intField", "stringField");
//         assertEquals(expectedOrder, new ArrayList<>(map.keySet()));
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void testGetAllDeclaredFieldsMap_DuplicateNames_SimpleHierarchy() {
//         Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(Child.class, f -> true);
//         // Expected keys: duplicate (Child), Base.duplicate (parent), baseField, childField
//         assertEquals(4, map.size());
//         assertTrue(map.containsKey("duplicate"));
//         assertTrue(map.containsKey("Base.duplicate"));
//         assertTrue(map.containsKey("baseField"));
//         assertTrue(map.containsKey("childField"));
//         // The simple key must refer to the Child's field
//         assertEquals(Child.class, map.get("duplicate").getDeclaringClass());
//         // The qualified key must refer to the Base's field
//         assertEquals(Base.class, map.get("Base.duplicate").getDeclaringClass());
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void testGetAllDeclaredFieldsMap_DuplicateNames_ThreeLevels() {
//         Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(GrandChild.class, f -> true);
//         // Expected keys:
//         // - duplicate               (GrandChild)
//         // - Child.duplicate         (Child)
//         // - Base.duplicate          (Base)
//         // - baseField, childField, grandChildField
//         assertEquals(7, map.size());
//         assertTrue(map.containsKey("duplicate"));
//         assertTrue(map.containsKey("Child.duplicate"));
//         assertTrue(map.containsKey("Base.duplicate"));
//         assertTrue(map.containsKey("baseField"));
//         assertTrue(map.containsKey("childField"));
//         assertTrue(map.containsKey("grandChildField"));
//         // Verify the declaring classes
//         assertEquals(GrandChild.class, map.get("duplicate").getDeclaringClass());
//         assertEquals(Child.class, map.get("Child.duplicate").getDeclaringClass());
//         assertEquals(Base.class, map.get("Base.duplicate").getDeclaringClass());
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void testGetAllDeclaredFieldsMap_CustomFilter_ExcludesStatic() {
//         Predicate<Field> nonStatic = f -> !Modifier.isStatic(f.getModifiers());
//         Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(WithStatic.class, nonStatic);
//         assertEquals(1, map.size());
//         assertTrue(map.containsKey("instanceField"));
//         assertFalse(map.containsKey("STATIC_FIELD"));
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void testGetAllDeclaredFieldsMap_CustomFilter_SelectsIntFieldOnly() {
//         Predicate<Field> onlyInt = f -> "intField".equals(f.getName());
//         Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(SimpleClass.class, onlyInt);
//         assertEquals(1, map.size());
//         assertTrue(map.containsKey("intField"));
//         assertFalse(map.containsKey("stringField"));
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void testGetAllDeclaredFieldsMap_PredicateFiltersAll() {
//         Predicate<Field> none = f -> false;
//         Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(SimpleClass.class, none);
//         assertTrue(map.isEmpty(), "All fields should be filtered out");
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void testGetAllDeclaredFieldsMap_PredicateThrowsException() {
//         Predicate<Field> throwsException = f -> {
//             throw new RuntimeException("filter failure");
//         };
//         RuntimeException ex = assertThrows(RuntimeException.class, () -> ReflectionUtils.getAllDeclaredFieldsMap(SimpleClass.class, throwsException));
//         assertEquals("filter failure", ex.getMessage());
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void testGetAllDeclaredFieldsMap_EmptyClass() {
//         Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(EmptyClass.class, f -> true);
//         assertTrue(map.isEmpty(), "EmptyClass should result in an empty map");
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void testGetAllDeclaredFieldsMap_OrderPreservation_MixedVisibility() {
//         Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(MixedVisibility.class, f -> true);
//         List<String> expectedOrder = List.of("publicField", "protectedField", "packageField", "privateField");
//         assertEquals(expectedOrder, new ArrayList<>(map.keySet()));
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void testGetAllDeclaredFieldsMap_CustomFilter_IncludesStatic() {
//         Predicate<Field> acceptAll = f -> true;
//         Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(WithStatic.class, acceptAll);
//         assertEquals(2, map.size());
//         assertTrue(map.containsKey("STATIC_FIELD"));
//         assertTrue(map.containsKey("instanceField"));
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void testGetAllDeclaredFieldsMap_SecurityExceptionPropagates() {
//         try (MockedStatic<ReflectionUtils> mocked = mockStatic(ReflectionUtils.class)) {
//             mocked.when(() -> ReflectionUtils.getAllDeclaredFields(any(), any())).thenThrow(new SecurityException("access denied"));
//             SecurityException ex = assertThrows(SecurityException.class, () -> ReflectionUtils.getAllDeclaredFieldsMap(WithStatic.class, f -> true));
//             assertEquals("access denied", ex.getMessage());
//         }
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void testGetAllDeclaredFieldsMap_InaccessibleObjectExceptionPropagates() {
//         try (MockedStatic<ReflectionUtils> mocked = mockStatic(ReflectionUtils.class)) {
//             mocked.when(() -> ReflectionUtils.getAllDeclaredFields(any(), any())).thenThrow(new InaccessibleObjectException("module not open"));
//             InaccessibleObjectException ex = assertThrows(InaccessibleObjectException.class, () -> ReflectionUtils.getAllDeclaredFieldsMap(WithStatic.class, f -> true));
//             assertEquals("module not open", ex.getMessage());
//         }
//     }
}