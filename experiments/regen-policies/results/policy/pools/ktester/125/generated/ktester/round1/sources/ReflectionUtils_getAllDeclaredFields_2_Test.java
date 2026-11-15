package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.security.Permission;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getAllDeclaredFields_2_Test {

    /**
     * Simple class with instance and static fields.
     */
    static class SimpleClass {

        int instanceInt;

        String instanceString;

        static int staticInt;
    }

    /**
     * Base class for inheritance test.
     */
    static class BaseClass {

        long baseField;
    }

    /**
     * Derived class that inherits from BaseClass.
     */
    static class DerivedClass extends BaseClass {

        int derivedField;
    }

    /**
     * Class with one static and one instance field.
     */
    static class ClassWithStaticField {

        static int STATIC_FIELD;

        int instanceField;
    }

    /**
     * Class that mimics a Groovy class with a metaClass field of the exact Groovy type.
     */
    static class ClassWithGroovyMetaClassField {

        // should be filtered out
        groovy.lang.MetaClass metaClass;
    }

    /**
     * Plain POJO with three normal instance fields.
     */
    static class PlainPojo {

        int a;

        String b;

        double c;
    }

    /**
     * Super class for inheritance test.
     */
    static class SuperClass {

        long superField;
    }

    /**
     * Sub class extending {@link SuperClass}.
     */
    static class SubClass extends SuperClass {

        int subField;
    }

    /**
     * Class that defines a field named {@code metaClass} but of type {@code String}.
     */
    static class ClassWithMetaClassStringField {

        // should NOT be filtered out
        String metaClass;
    }

    /**
     * Normal class that declares a field named {@code hash}.
     */
    static class ClassWithHashField {

        // should be included because the class is not an Enum
        int hash;
    }

    /**
     * Abstract class with a protected instance field.
     */
    abstract static class AbstractBase {

        protected String protectedField;
    }

    /**
     * Class with a static final constant and a regular instance field.
     */
    static class ClassWithStaticFinalAndInstance {

        static final int CONST = 42;

        int instanceField;
    }

    /**
     * Class that would get a synthetic JaCoCo field (static).
     */
    static class ClassWithJacocoSyntheticField {

        // In a real JaCoCo‑instrumented class a static field named "$jacocoData" would appear.
        // We simulate it explicitly to test the filter (static fields are always removed).
        static Object $jacocoData;
    }

    /**
     * Interface containing constant fields (implicitly public static final).
     */
    interface InterfaceWithConstants {

        int CONST_ONE = 1;

        String CONST_STR = "test";
    }

    /**
     * Class with a public field used for the SecurityManager test.
     */
    static class ClassWithPublicField {

        public int publicField;
    }

    /**
     * Provides a stream of arguments: the class under test, the expected list of
     * field names (empty list if none), and an optional expected exception type.
     */
    private static Stream<Arguments> fieldProvider() {
        return Stream.of(// ----- default filter branches -------------------------------------------------
        Arguments.of(ClassWithStaticField.class, List.of("instanceField"), null), Arguments.of(EnumWithInternalFields.class, Collections.emptyList(), null), Arguments.of(ClassWithGroovyMetaClassField.class, Collections.emptyList(), null), Arguments.of(EnumWithHashOrdinalFields.class, Collections.emptyList(), null), Arguments.of(PlainPojo.class, List.of("a", "b", "c"), null), // ----- null argument -----------------------------------------------------------
        Arguments.of(null, null, NullPointerException.class), // ----- inheritance handling (already covered by explicit test) --------------------
        // (kept for completeness – expects both inherited and declared fields)
        Arguments.of(SubClass.class, List.of("superField", "subField"), null), // ----- enum with custom instance field -----------------------------------------
        Arguments.of(EnumWithCustomField.class, List.of("custom"), null), // ----- metaClass field with non‑Groovy type ------------------------------------
        Arguments.of(ClassWithMetaClassStringField.class, List.of("metaClass"), null), // ----- field named hash in non‑enum class --------------------------------------
        Arguments.of(ClassWithHashField.class, List.of("hash"), null), // ----- primitive class argument ------------------------------------------------
        Arguments.of(int.class, Collections.emptyList(), null), // ----- array class argument ----------------------------------------------------
        Arguments.of(String[].class, Collections.emptyList(), null), // ----- interface with constant fields -------------------------------------------
        Arguments.of(InterfaceWithConstants.class, Collections.emptyList(), null), // ----- abstract class with protected field --------------------------------------
        Arguments.of(AbstractBase.class, List.of("protectedField"), null), // ----- class with static final and instance field -------------------------------
        Arguments.of(ClassWithStaticFinalAndInstance.class, List.of("instanceField"), null), // ----- class with synthetic JaCoCo static field ----------------------------------
        Arguments.of(ClassWithJacocoSyntheticField.class, Collections.emptyList(), null), // ----- security manager restriction --------------------------------------------
        Arguments.of(ClassWithPublicField.class, null, SecurityException.class));
    }

    @Test
    void shouldReturnOnlyInstanceFieldsAndExcludeStaticFields() {
        List<Field> fields = ReflectionUtils.getAllDeclaredFields(SimpleClass.class);
        Set<String> fieldNames = fields.stream().map(Field::getName).collect(Collectors.toSet());
        // Expect exactly the two instance fields.
        assertEquals(2, fields.size(), "Static fields must be filtered out");
        assertTrue(fieldNames.contains("instanceInt"));
        assertTrue(fieldNames.contains("instanceString"));
        assertFalse(fieldNames.contains("staticInt"));
    }

    @Test
    void shouldIncludeInheritedInstanceFields() {
        List<Field> fields = ReflectionUtils.getAllDeclaredFields(DerivedClass.class);
        Set<String> fieldNames = fields.stream().map(Field::getName).collect(Collectors.toSet());
        // Both the field from the base class and the derived class should appear.
        assertEquals(2, fields.size(), "Inherited fields must be included");
        assertTrue(fieldNames.contains("baseField"));
        assertTrue(fieldNames.contains("derivedField"));
    }

    @Test
    void shouldFilterOutSyntheticEnumFieldsAndReturnOnlyUserDefinedFields() {
        List<Field> fields = ReflectionUtils.getAllDeclaredFields(SampleEnum.class);
        Set<String> fieldNames = fields.stream().map(Field::getName).collect(Collectors.toSet());
        // The enum defines only one user‑declared field: "value".
        assertEquals(1, fields.size(), "Synthetic enum fields must be filtered out");
        assertTrue(fieldNames.contains("value"));
        // Ensure that the typical synthetic field "$VALUES" (or "ENUM$VALUES") is not present.
        assertFalse(fieldNames.stream().anyMatch(name -> name.equals("ENUM$VALUES") || name.equals("$VALUES")));
    }

    @ParameterizedTest(name = "[{index}] Class: {0}")
    @MethodSource("fieldProvider")
    void testGetAllDeclaredFields(Class<?> clazz, List<String> expectedFieldNames, Class<? extends Throwable> expectedException) {
        // SecurityManager test requires special handling because we need to install a manager
        // that throws a SecurityException when reflective access is attempted.
        SecurityManager originalSecurityManager = System.getSecurityManager();
        try {
            if (clazz == ClassWithPublicField.class) {
                // Install a restrictive SecurityManager that denies reflective access.
                System.setSecurityManager(new SecurityManager() {

                    @Override
                    public void checkPermission(Permission perm) {
                        // Allow everything except reflective access.
                        if (perm instanceof ReflectPermission) {
                            throw new SecurityException("Reflective access denied by test SecurityManager");
                        }
                    }

                    @Override
                    public void checkPermission(Permission perm, Object context) {
                        checkPermission(perm);
                    }
                });
            }
            if (expectedException != null) {
                // Expect an exception (either NullPointerException or SecurityException)
                assertThrows(expectedException, () -> ReflectionUtils.getAllDeclaredFields(clazz));
            } else {
                // Normal execution path – collect field names and compare with expectation.
                List<Field> fields = ReflectionUtils.getAllDeclaredFields(clazz);
                Set<String> actualNames = fields.stream().map(Field::getName).collect(Collectors.toSet());
                Set<String> expectedNames = new HashSet<>(expectedFieldNames);
                assertEquals(expectedNames, actualNames, () -> "Field names mismatch for class " + clazz);
            }
        } finally {
            // Restore the original SecurityManager to avoid side effects on other tests.
            System.setSecurityManager(originalSecurityManager);
        }
    }
}
