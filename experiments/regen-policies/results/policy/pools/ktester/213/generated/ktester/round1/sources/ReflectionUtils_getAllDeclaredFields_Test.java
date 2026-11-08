package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.function.Predicate;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getAllDeclaredFields_Test {

    /**
     * A predicate that accepts every field (used to test that the cache works).
     */
    private static final Predicate<Field> ACCEPT_ALL = f -> true;

    /**
     * Subclass that adds its own field.
     */
    static class SubClass extends SuperClass {

        private String subField;

        // should be filtered out
        static String staticSubField;
    }

    /**
     * Simple superclass with a non‑static field.
     */
    static class SuperClass {

        private int superField;

        // should be filtered out by default
        static int staticSuperField;
    }

    /**
     * Subclass that also declares static fields (used for a predicate that accepts everything).
     */
    static class SubClassWithStatic extends SuperClass {

        private String subField;

        static String STATIC_SUB;

        static int STATIC_BASE;
    }

    /**
     * Class with a single non‑static field – used for basic functionality tests.
     */
    static class SimpleClass {

        private int simpleField;
    }

    /**
     * Class that declares no fields at all.
     */
    static class ClassWithNoFields {
        // deliberately empty
    }

    @BeforeEach
    void setupBeforeEach() {
        // Clear the internal field cache to make each test independent.
        // The cache is a private static AtomicReference<Map<FieldsCacheKey, Collection<Field>>>.
        java.lang.reflect.Field cacheField = ReflectionUtils.class.getDeclaredField("FIELDS_CACHE");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        AtomicReference<Map<?, ?>> ref = (AtomicReference<Map<?, ?>>) cacheField.get(null);
        // replace with a fresh, empty map
        ref.set(new HashMap<>());
    }

    /**
     * Extracts the simple names of a list of {@link Field}s preserving order.
     */
    private static List<String> extractNames(List<Field> fields) {
        List<String> names = new ArrayList<>(fields.size());
        for (Field f : fields) {
            names.add(f.getName());
        }
        return names;
    }

    @Test
    void getAllDeclaredFields_nullClass_throws() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> ReflectionUtils.getAllDeclaredFields(null, ACCEPT_ALL));
        assertTrue(ex.getMessage().contains("class cannot be null"));
    }

    @Test
    void getAllDeclaredFields_nullPredicate_throws() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> ReflectionUtils.getAllDeclaredFields(SubClass.class, null));
        assertTrue(ex.getMessage().contains("fieldFilter cannot be null"));
    }

    @Test
    void getAllDeclaredFields_collectsFromClassHierarchy() {
        List<Field> fields = ReflectionUtils.getAllDeclaredFields(SubClass.class, ACCEPT_ALL);
        // Expect exactly the two instance fields (superField, subField). Static fields are filtered out.
        assertEquals(2, fields.size(), "Should contain only non‑static fields from the hierarchy");
        boolean foundSuper = false;
        boolean foundSub = false;
        for (Field f : fields) {
            if (f.getName().equals("superField")) {
                foundSuper = true;
            }
            if (f.getName().equals("subField")) {
                foundSub = true;
            }
        }
        assertTrue(foundSuper, "superField should be present");
        assertTrue(foundSub, "subField should be present");
    }

    @Test
    void getAllDeclaredFields_defaultFilterExcludesStaticAndEnumInternals() {
        // Use the default filter supplied by ReflectionUtils
        List<Field> fields = ReflectionUtils.getAllDeclaredFields(TestEnum.class, ReflectionUtils.DEFAULT_FIELD_FILTER);
        // The only fields we expect are the enum constants themselves (ONE, TWO) – they are not
        // represented as java.lang.reflect.Field objects returned by getDeclaredFields, so the list should be empty.
        // The internal synthetic fields must be filtered out.
        assertEquals(0, fields.size(), "Default filter must exclude static, enum internal, and Groovy metaClass fields");
    }

    @Test
    void getAllDeclaredFields_returnsUnmodifiableList() {
        List<Field> fields = ReflectionUtils.getAllDeclaredFields(SubClass.class, ACCEPT_ALL);
        assertThrows(UnsupportedOperationException.class, () -> fields.add(null));
    }

    @Test
    void getAllDeclaredFields_cachesResult() {
        List<Field> firstCall = ReflectionUtils.getAllDeclaredFields(SubClass.class, ACCEPT_ALL);
        List<Field> secondCall = ReflectionUtils.getAllDeclaredFields(SubClass.class, ACCEPT_ALL);
        // The implementation caches an unmodifiable list, therefore both calls should return the same instance.
        assertSame(firstCall, secondCall, "Result should be cached and identical across calls");
    }

    @Test
    void getAllDeclaredFields_simpleClass_singleField() {
        List<Field> fields = ReflectionUtils.getAllDeclaredFields(SimpleClass.class, f -> true);
        assertEquals(1, fields.size(), "SimpleClass should expose exactly one field");
        assertEquals("simpleField", fields.get(0).getName());
    }

    @Test
    void getAllDeclaredFields_classWithNoFields_returnsEmpty() {
        List<Field> fields = ReflectionUtils.getAllDeclaredFields(ClassWithNoFields.class, f -> true);
        assertTrue(fields.isEmpty(), "ClassWithNoFields should yield an empty list");
    }

    @Test
    void getAllDeclaredFields_inheritanceOrder_subclassFirst() {
        List<Field> fields = ReflectionUtils.getAllDeclaredFields(SubClass.class, f -> true);
        // Expected order: fields declared in SubClass, then those in SuperClass
        List<String> names = extractNames(fields);
        assertEquals(Arrays.asList("subField", "superField"), names, "Fields should be collected subclass‑first preserving declaration order");
    }

    @Test
    void getAllDeclaredFields_inheritanceIncludingStatic_whenPredicateAcceptsAll() {
        List<Field> fields = ReflectionUtils.getAllDeclaredFields(SubClassWithStatic.class, f -> true);
        List<String> names = extractNames(fields);
        // static fields are also present because the predicate does not filter them out
        assertEquals(Arrays.asList("subField", "STATIC_SUB", "STATIC_BASE", "superField", "staticSuperField"), names, "When predicate accepts all, static fields from both subclass and superclass are included");
    }

    @Test
    void getAllDeclaredFields_predicateKeepsOnlyNamesStartingWithS() {
        Predicate<Field> startsWithS = f -> f.getName().startsWith("s");
        List<Field> fields = ReflectionUtils.getAllDeclaredFields(SubClass.class, startsWithS);
        List<String> names = extractNames(fields);
        assertEquals(Collections.singletonList("subField"), names, "Only the field whose name starts with 's' should survive");
    }

    @Test
    void getAllDeclaredFields_predicateFiltersStaticFieldsExplicitly() {
        Predicate<Field> nonStatic = f -> !Modifier.isStatic(f.getModifiers());
        List<Field> fields = ReflectionUtils.getAllDeclaredFields(SubClassWithStatic.class, nonStatic);
        List<String> names = extractNames(fields);
        assertEquals(Arrays.asList("subField", "superField"), names, "Custom predicate that removes static fields should behave like the default filter");
    }

    @Test
    void getAllDeclaredFields_enumWithDefaultFilter_returnsEmpty() {
        List<Field> fields = ReflectionUtils.getAllDeclaredFields(MyEnum.class, ReflectionUtils.DEFAULT_FIELD_FILTER);
        assertTrue(fields.isEmpty(), "Default filter must exclude synthetic enum fields and any user fields");
    }

    @Test
    void getAllDeclaredFields_enumBypassingDefaultFilter_returnsUserField() {
        List<Field> fields = ReflectionUtils.getAllDeclaredFields(MyEnum.class, f -> true);
        List<String> names = extractNames(fields);
        assertEquals(Collections.singletonList("customValue"), names, "When the default filter is bypassed, the user‑declared enum field should be returned");
    }

    @Test
    void getAllDeclaredFields_samePredicate_returnsSameInstance() {
        List<Field> first = ReflectionUtils.getAllDeclaredFields(SubClass.class, f -> true);
        List<Field> second = ReflectionUtils.getAllDeclaredFields(SubClass.class, f -> true);
        assertSame(first, second, "Identical class+predicate should hit the cache and return the same list instance");
    }

    @Test
    void getAllDeclaredFields_differentPredicate_returnsDifferentInstance() {
        List<Field> first = ReflectionUtils.getAllDeclaredFields(SubClass.class, f -> true);
        List<Field> second = ReflectionUtils.getAllDeclaredFields(SubClass.class, f -> f.getName().startsWith("b"));
        assertNotSame(first, second, "Changing the predicate must produce a distinct cache key and thus a new list");
    }

    @Test
    void getAllDeclaredFields_predicateThrowsException_propagates() {
        Predicate<Field> exploding = f -> {
            throw new IllegalStateException("predicate failure");
        };
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> ReflectionUtils.getAllDeclaredFields(SimpleClass.class, exploding));
        assertEquals("predicate failure", ex.getMessage());
    }
}
