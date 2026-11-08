package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.ReflectPermission;
import java.security.Permission;
import java.util.function.Predicate;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getAllDeclaredFieldsMap_2_Test {

    // Superclass with a field
    static class SuperClass {

        protected long superField;
    }

    // Subclass inherits from SuperClass and adds its own field
    static class SubClass extends SuperClass {

        private boolean subField;
    }

    // Simple class with instance and static fields – used in the original test suite
    static class Simple {

        private int instanceInt;

        public String instanceString;

        // should be filtered out
        static double staticDouble;
    }

    // Normal class with two instance fields (no static, no enum)
    static class NormalClass {

        int instanceField;

        String anotherField;
    }

    // Class that declares only a static field
    static class StaticClass {

        static final String STATIC_CONSTANT = "CONST";
    }

    // Groovy‑style class that contains a metaClass field of type groovy.lang.MetaClass
    static class GroovyClass {

        // The type name is enough – we do not have Groovy on the classpath, but the filter checks the name.
        @SuppressWarnings("unused")
        private Object // pretend it's groovy.lang.MetaClass
        metaClass;
    }

    // Base class for field‑shadowing test
    static class ShadowBaseClass {

        String value = "base";
    }

    // Subclass that redeclares a field with the same name
    static class ShadowSubClass extends ShadowBaseClass {

        String value = "sub";
    }

    // Empty class – no declared fields at all
    static class EmptyClass {
    }

    @Test
    void simpleClassShouldReturnOnlyInstanceFields() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(Simple.class);
        // Expect exactly the two instance fields
        assertEquals(2, map.size(), "Simple class should expose only instance fields");
        assertTrue(map.containsKey("instanceInt"));
        assertTrue(map.containsKey("instanceString"));
        assertFalse(map.containsKey("staticDouble"), "Static fields must be filtered out");
    }

    @Test
    void subclassShouldIncludeInheritedFields() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(SubClass.class);
        // Fields from both SuperClass and SubClass should be present
        assertEquals(2, map.size(), "SubClass should expose its own field and the inherited one");
        assertTrue(map.containsKey("superField"));
        assertTrue(map.containsKey("subField"));
    }

    @Test
    void enumShouldFilterOutSyntheticAndInternalFields() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(SampleEnum.class);
        // The enum defines a single user field 'value'; synthetic/internal fields must be removed
        assertEquals(1, map.size(), "Enum should expose only user‑declared fields");
        assertTrue(map.containsKey("value"));
        assertFalse(map.containsKey("ordinal"), "Ordinal field must be filtered out");
        assertFalse(map.containsKey("hash"), "Hash field must be filtered out");
        assertFalse(map.containsKey("$VALUES"), "Synthetic ENUM$VALUES field must be filtered out");
    }

    @Test
    void normalClassShouldContainAllInstanceFields() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(NormalClass.class);
        assertEquals(2, map.size(), "NormalClass should expose both instance fields");
        assertTrue(map.containsKey("instanceField"));
        assertTrue(map.containsKey("anotherField"));
    }

    @Test
    void staticClassShouldYieldEmptyMap() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(StaticClass.class);
        assertTrue(map.isEmpty(), "StaticClass only declares static fields, which must be filtered out");
    }

    @Test
    void groovyMetaClassShouldBeFilteredOut() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(GroovyClass.class);
        assertTrue(map.isEmpty(), "GroovyClass' metaClass field must be filtered out by the default predicate");
    }

    @Test
    void shadowingShouldPreferSubclassField() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(ShadowSubClass.class);
        assertEquals(1, map.size(), "Only one entry for the shadowed field name should exist");
        Field f = map.get("value");
        assertNotNull(f, "Field 'value' must be present");
        assertEquals(ShadowSubClass.class, f.getDeclaringClass(), "The field should be the one declared in the subclass, not the superclass");
    }

    @Test
    void emptyClassReturnsEmptyMap() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(EmptyClass.class);
        assertTrue(map.isEmpty(), "EmptyClass declares no fields, so the result must be empty");
    }

    @Test
    void nullClassArgumentThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getAllDeclaredFieldsMap(null), "Passing null as the class argument must raise IllegalArgumentException");
    }

    @Test
    void primitiveClassArgumentThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getAllDeclaredFieldsMap(int.class), "Primitive types have no declared fields and must cause IllegalArgumentException");
    }

    @Test
    void arrayClassArgumentReturnsEmptyMap() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(String[].class);
        assertTrue(map.isEmpty(), "Array classes have no declared fields, result should be empty");
    }

    @Test
    void securityManagerDeniesReflectiveAccess() {
        // Install a SecurityManager that disallows suppressAccessChecks.
        SecurityManager original = System.getSecurityManager();
        try {
            System.setSecurityManager(new SecurityManager() {

                @Override
                public void checkPermission(Permission perm) {
                    // Allow everything except ReflectPermission "suppressAccessChecks"
                    if (perm instanceof ReflectPermission && "suppressAccessChecks".equals(perm.getName())) {
                        throw new SecurityException("Reflective access denied by test SecurityManager");
                    }
                }
            });
            // The call to getDeclaredFields may trigger a SecurityException depending on the JDK.
            // We assert that the exception propagates.
            assertThrows(SecurityException.class, () -> ReflectionUtils.getAllDeclaredFieldsMap(NormalClass.class), "When a SecurityManager blocks reflective access, the method must propagate SecurityException");
        } finally {
            // Restore original SecurityManager to avoid side‑effects on other tests
            System.setSecurityManager(original);
        }
    }

    @Test
    void customFilterAllowsStaticFields() {
        // accept everything, even static
        Predicate<Field> acceptAll = field -> true;
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(StaticClass.class, acceptAll);
        assertEquals(1, map.size(), "Custom filter that accepts all fields should expose the static field");
        assertTrue(map.containsKey("STATIC_CONSTANT"));
    }

    @Test
    void customFilterRejectsAllFields() {
        Predicate<Field> rejectAll = field -> false;
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(NormalClass.class, rejectAll);
        assertTrue(map.isEmpty(), "A predicate that always returns false must filter out every field");
    }

    @Test
    void nullPredicateThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getAllDeclaredFieldsMap(NormalClass.class, null), "Providing a null predicate to the overload must result in NPE");
    }
}
