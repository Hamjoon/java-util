package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Predicate;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;

// @ExtendWith(MockitoExtension.class)
class ReflectionUtils_getAllDeclaredFields_2_Test {

    /**
     * Default filter applied to every field returned by the utility methods.
     */
    public static final Predicate<Field> DEFAULT_FIELD_FILTER = field -> {
        int mods = field.getModifiers();
        // static fields are never part of the result
        if (Modifier.isStatic(mods)) {
            return false;
        }
        // synthetic fields (compiler‑generated) are ignored
        if (field.isSynthetic()) {
            return false;
        }
        String name = field.getName();
        // enum‑specific synthetic fields
        if (field.getDeclaringClass().isEnum()) {
            // "$VALUES" and "ENUM$VALUES" are the array that holds the enum constants
            // "ordinal" and "hash" are fields from java.lang.Enum
            // "internal" is a Groovy‑specific field that can appear on enums
            if ("$VALUES".equals(name) || "ENUM$VALUES".equals(name) || "ordinal".equals(name) || "hash".equals(name) || "internal".equals(name)) {
                return false;
            }
        }
        // Groovy adds a metaClass field to every class; it should be ignored
        if ("metaClass".equals(name) && field.getDeclaringClass().getName().contains("groovy")) {
            return false;
        }
        return true;
    };

    /* --------------------------------------------------------------------- */
    static class SimpleClass {

        private int fieldA;

        private String fieldB;
    }

    static class SuperClass {

        protected long superField;
    }

    static class SubClass extends SuperClass {

        private double subField;
    }

    static class WithStatic {

        private int instanceField;

        public static String staticField = "IGNORE_ME";
    }

    /**
     * Retrieves all fields from the supplied class and its super‑classes, applying the
     * default field filter.
     *
     * @param c the class whose fields are to be collected; must not be {@code null}
     * @return an unmodifiable list of fields that pass the default filter
     * @throws IllegalArgumentException if {@code c} is {@code null}
     */
    public static List<Field> getAllDeclaredFields(Class<?> c) {
        // accept everything – default filter still applies
        return getAllDeclaredFields(c, field -> true);
    }

    /**
     * Retrieves all fields from the supplied class and its super‑classes, applying both
     * the default filter and a caller‑provided custom filter.
     *
     * @param c      the class whose fields are to be collected; must not be {@code null}
     * @param filter a custom predicate that further restricts the fields; must not be {@code null}
     * @return an unmodifiable list of fields that satisfy both filters
     * @throws IllegalArgumentException if {@code c} or {@code filter} is {@code null}
     */
    public static List<Field> getAllDeclaredFields(Class<?> c, Predicate<Field> filter) {
        if (c == null) {
            throw new IllegalArgumentException("Class must not be null");
        }
        if (filter == null) {
            throw new IllegalArgumentException("Filter must not be null");
        }
        List<Field> result = new ArrayList<>();
        // Walk the inheritance hierarchy, starting with the concrete class and moving up.
        for (Class<?> current = c; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field f : current.getDeclaredFields()) {
                // Apply the default filter first, then the custom filter supplied by the caller.
                if (DEFAULT_FIELD_FILTER.test(f) && filter.test(f)) {
                    result.add(f);
                }
            }
        }
        // Return an immutable view to guarantee callers cannot modify the internal list.
        return Collections.unmodifiableList(result);
    }

//     @Test
//     void shouldReturnAllInstanceFieldsOfSimpleClass() {
//         List<Field> fields = ReflectionUtils.getAllDeclaredFields(SimpleClass.class);
//         Set<String> names = fields.stream().map(Field::getName).collect(Collectors.toSet());
//         assertEquals(2, fields.size(), "SimpleClass should expose exactly two fields");
//         assertTrue(names.contains("fieldA"));
//         assertTrue(names.contains("fieldB"));
//     }

//     @Test
//     void shouldIncludeInheritedFields() {
//         List<Field> fields = ReflectionUtils.getAllDeclaredFields(SubClass.class);
//         Set<String> names = fields.stream().map(Field::getName).collect(Collectors.toSet());
//         // SubClass declares subField, SuperClass declares superField
//         assertEquals(2, fields.size(), "SubClass should expose its own field and the inherited one");
//         assertTrue(names.contains("subField"));
//         assertTrue(names.contains("superField"));
//     }

//     @Test
//     void shouldFilterOutStaticFields() {
//         List<Field> fields = ReflectionUtils.getAllDeclaredFields(WithStatic.class);
//         Set<String> names = fields.stream().map(Field::getName).collect(Collectors.toSet());
//         assertEquals(1, fields.size(), "Static fields must be filtered out");
//         assertTrue(names.contains("instanceField"));
//         assertFalse(names.contains("staticField"));
//     }

//     @Test
//     void shouldRespectDefaultFieldFilterWhenCustomFilterIsUsed() {
//         // Custom filter that accepts everything – the default filter should still apply.
//         Predicate<Field> acceptAll = f -> true;
//         List<Field> fields = ReflectionUtils.getAllDeclaredFields(WithStatic.class, acceptAll);
//         // The static field must still be removed because the implementation always applies
//         // the DEFAULT_FIELD_FILTER before the custom one.
//         assertEquals(1, fields.size());
//         assertEquals("instanceField", fields.get(0).getName());
//     }
}