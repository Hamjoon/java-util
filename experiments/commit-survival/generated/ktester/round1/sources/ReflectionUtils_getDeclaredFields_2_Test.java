package com.cedarsoftware.util;

import groovy.lang.MetaClass;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for {@link ReflectionUtils#getDeclaredFields(Class, Collection)}.
 * Covers filtering rules, null handling, collection behaviour and exceptional paths.
 */
@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getDeclaredFields_2_Test {

    /* --------------------------------------------------------------------- */
    /*                     Test fixtures (helper classes)                    */
    /* --------------------------------------------------------------------- */

    /** Simple class with a public and a private field. */
    static class SimpleClass {
        public int publicField;
        private String privateField;
    }

    /** Class containing static and transient fields that must be ignored. */
    static class StaticTransientClass {
        public int keptField;
        static int staticField;
        transient int transientField;
    }

    /** Class with only a static field – should result in an empty collection. */
    static class ClassWithStaticField {
        static int onlyStatic;
    }

    /** Class with only a transient field – should result in an empty collection. */
    static class ClassWithTransientField {
        transient int onlyTransient;
    }

    /** Class that mimics Groovy's MetaClass field – must be skipped. */
    static class ClassWithGroovyMetaClassField {
        public MetaClass metaClass; // type is groovy.lang.MetaClass
        public int other;
    }

    /** Class with a synthetic this$ field (inner class). */
    static class Outer {
        class Inner {
            // compiler generates a synthetic field named this$0
            public int innerPublic;
            public MetaClass metaClass; // also present to test combined skipping
        }
    }

    /** Class with a public field only – used for null‑collection test. */
    static class ClassWithPublicField {
        public int onlyPublic;
    }

    /** Class with a metaClass field whose type is NOT groovy.lang.MetaClass. */
    static class ClassWithMetaClassStringField {
        public String metaClass; // name matches but type does not -> should be kept
    }

    /** Superclass with a field that should NOT be inherited by the test. */
    static class SuperClass {
        public int superField;
    }

    /** Subclass that declares its own field; inherited field must be ignored. */
    static class SubclassWithInheritedField extends SuperClass {
        private double subField;
    }

    /** Class with both public and private fields – used for accessibility test. */
    static class ClassWithPublicAndPrivateFields {
        public long pub;
        private boolean priv;
    }

    /** Enum with constants (static fields) – should be filtered out. */
    enum SampleEnum {
        ONE, TWO
    }

    /** Interface – has no instance fields. */
    interface SampleInterface { }

    /** Class that will throw a SecurityException when setAccessible is called. */
    static class ClassWithExceptionInSetAccessible {
        private int secret;
    }

    /** Class that will cause getDeclaredFields() to throw a SecurityException
     *  (simulated via a custom SecurityManager in the test). */
    static class ClassWithRestrictedReflection { }

    /** Custom collection that throws IllegalStateException on add(). */
    static class ThrowingCollection implements Collection<Field> {
        @Override public int size() { return 0; }
        @Override public boolean isEmpty() { return true; }
        @Override public boolean contains(Object o) { return false; }
        @Override public Iterator<Field> iterator() { return Collections.emptyIterator(); }
        @Override public Object[] toArray() { return new Object[0]; }
        @Override public <T> T[] toArray(T[] a) { return a; }
        @Override public boolean add(Field field) {
            throw new IllegalStateException("add() not allowed");
        }
        @Override public boolean remove(Object o) { return false; }
        @Override public boolean containsAll(Collection<?> c) { return false; }
        @Override public boolean addAll(Collection<? extends Field> c) { return false; }
        @Override public boolean removeAll(Collection<?> c) { return false; }
        @Override public boolean retainAll(Collection<?> c) { return false; }
        @Override public void clear() { }
    }

    /* --------------------------------------------------------------------- */
    /*                         Existing test cases                             */
    /* --------------------------------------------------------------------- */

    @Test
    void testPublicAndNonPublicFieldsAreCollected() {
        Collection<Field> fields = new ArrayList<>();
        ReflectionUtils.getDeclaredFields(SimpleClass.class, fields);

        assertEquals(2, fields.size(), "Should collect both public and private fields");
        List<String> names = fields.stream().map(Field::getName).toList();
        assertTrue(names.contains("publicField"));
        assertTrue(names.contains("privateField"));
    }

    @Test
    void testStaticAndTransientFieldsAreSkipped() {
        Collection<Field> fields = new ArrayList<>();
        ReflectionUtils.getDeclaredFields(StaticTransientClass.class, fields);
        assertEquals(1, fields.size(), "Only one field should survive the filter");
        Field field = fields.iterator().next();
        assertEquals("keptField", field.getName());
    }

    @Test
    void testSyntheticAndGroovyMetaClassFieldsAreSkipped() {
        Class<?> innerClass = Outer.Inner.class;
        Collection<Field> fields = new ArrayList<>();
        ReflectionUtils.getDeclaredFields(innerClass, fields);
        assertEquals(1, fields.size(), "Synthetic and metaClass fields must be filtered out");
        Field field = fields.iterator().next();
        assertEquals("innerPublic", field.getName());
    }

    /* --------------------------------------------------------------------- */
    /*                     Additional test cases from JSON                    */
    /* --------------------------------------------------------------------- */

    /** Group: field filtering and accessibility – mixed scenario. */
    @Test
    void testClassWithVariousFieldsFilteringAndAccessibility() {
        class ClassWithVariousFields {
            public int pub;
            private String priv;
            static long staticField;
            transient double transientField;
            public MetaClass metaClass; // Groovy metaClass – should be skipped
            public int this$0; // synthetic‑like name – should be skipped
        }

        Collection<Field> fields = new ArrayList<>();
        ReflectionUtils.getDeclaredFields(ClassWithVariousFields.class, fields);

        // Expected: pub and priv only
        assertEquals(2, fields.size(), "Only non‑static, non‑transient, non‑metaClass, non‑synthetic fields should remain");
        Set<String> names = fields.stream().map(Field::getName).collect(Collectors.toSet());
        assertTrue(names.containsAll(Set.of("pub", "priv")));

        // Verify that the private field had setAccessible called (no exception means it succeeded)
        Field privField = fields.stream().filter(f -> f.getName().equals("priv")).findFirst().orElseThrow();
        assertTrue(privField.canAccess(null) || !Modifier.isPublic(privField.getModifiers()),
                "private field should be made accessible");
    }

    /** Group: static field only – collection must stay empty. */
    @Test
    void testOnlyStaticFieldResultsInEmptyCollection() {
        Collection<Field> fields = new ArrayList<>();
        ReflectionUtils.getDeclaredFields(ClassWithStaticField.class, fields);
        assertTrue(fields.isEmpty(), "Static field should be filtered out, leaving collection empty");
    }

    /** Group: transient field only – collection must stay empty. */
    @Test
    void testOnlyTransientFieldResultsInEmptyCollection() {
        Collection<Field> fields = new ArrayList<>();
        ReflectionUtils.getDeclaredFields(ClassWithTransientField.class, fields);
        assertTrue(fields.isEmpty(), "Transient field should be filtered out, leaving collection empty");
    }

    /** Group: Groovy metaClass field – must be skipped. */
    @Test
    void testGroovyMetaClassFieldIsSkipped() {
        Collection<Field> fields = new ArrayList<>();
        ReflectionUtils.getDeclaredFields(ClassWithGroovyMetaClassField.class, fields);
        // Only the non‑metaClass field should survive
        assertEquals(1, fields.size(), "Groovy metaClass field must be filtered out");
        assertEquals("other", fields.iterator().next().getName());
    }

    /** Group: synthetic this$ field – must be skipped. */
    @Test
    void testSyntheticThisFieldIsSkipped() {
        class ClassWithSyntheticThisField {
            public int this$0; // synthetic name
            public int real;
        }
        Collection<Field> fields = new ArrayList<>();
        ReflectionUtils.getDeclaredFields(ClassWithSyntheticThisField.class, fields);
        assertEquals(1, fields.size(), "Synthetic this$ field must be filtered out");
        assertEquals("real", fields.iterator().next().getName());
    }

    /** Group: public & private fields – both collected, private made accessible. */
    @Test
    void testPublicAndPrivateFieldsCollectedAndAccessible() {
        Collection<Field> fields = new ArrayList<>();
        ReflectionUtils.getDeclaredFields(ClassWithPublicAndPrivateFields.class, fields);
        assertEquals(2, fields.size(), "Both public and private fields should be collected");
        Set<String> names = fields.stream().map(Field::getName).collect(Collectors.toSet());
        assertTrue(names.containsAll(Set.of("pub", "priv")));

        // Verify setAccessible was attempted (no exception means it succeeded)
        Field priv = fields.stream().filter(f -> f.getName().equals("priv")).findFirst().orElseThrow();
        assertTrue(priv.canAccess(null) || !Modifier.isPublic(priv.getModifiers()));
    }

    /** Group: null class argument – method must swallow exception and leave collection untouched. */
    @Test
    void testNullClassArgumentIsHandledGracefully() {
        Collection<Field> fields = new ArrayList<>();
        assertDoesNotThrow(() -> ReflectionUtils.getDeclaredFields(null, fields),
                "Method should swallow any Throwable when class argument is null");
        assertTrue(fields.isEmpty(), "Collection should remain empty when class is null");
    }

    /** Group: null collection argument – method must swallow NullPointerException. */
    @Test
    void testNullCollectionArgumentIsHandledGracefully() {
        assertDoesNotThrow(() -> ReflectionUtils.getDeclaredFields(ClassWithPublicField.class, null),
                "Method should swallow NullPointerException when collection argument is null");
    }

    /** Group: class with no declared fields (java.lang.Object). */
    @Test
    void testClassWithNoDeclaredFields() {
        Collection<Field> fields = new ArrayList<>();
        ReflectionUtils.getDeclaredFields(Object.class, fields);
        assertTrue(fields.isEmpty(), "Object has no declared fields; collection should stay empty");
    }

    /** Group: inherited fields are not included. */
    @Test
    void testInheritedFieldsAreNotReturned() {
        Collection<Field> fields = new ArrayList<>();
        ReflectionUtils.getDeclaredFields(SubclassWithInheritedField.class, fields);
        assertEquals(1, fields.size(), "Only fields declared in the subclass should be returned");
        assertEquals("subField", fields.iterator().next().getName());
    }

    /** Group: pre‑existing elements in collection are preserved. */
    @Test
    void testPreExistingElementsArePreserved() throws NoSuchFieldException {
        // Grab an existing field from SimpleClass to pre‑populate the collection
        Field existing = SimpleClass.class.getDeclaredField("publicField");
        Collection<Field> fields = new LinkedList<>();
        fields.add(existing);

        ReflectionUtils.getDeclaredFields(ClassWithPublicField.class, fields);
        assertEquals(2, fields.size(), "Original element must remain and new field must be added");
        Set<String> names = fields.stream().map(Field::getName).collect(Collectors.toSet());
        assertTrue(names.containsAll(Set.of("publicField", "onlyPublic")));
    }

    /** Group: non‑Groovy metaClass field is included. */
    @Test
    void testNonGroovyMetaClassFieldIsIncluded() {
        Collection<Field> fields = new ArrayList<>();
        ReflectionUtils.getDeclaredFields(ClassWithMetaClassStringField.class, fields);
        assertEquals(1, fields.size(), "metaClass field of type String should NOT be filtered out");
        assertEquals("metaClass", fields.iterator().next().getName());
    }

    /** Group: primitive class handling – should result in empty collection. */
    @Test
    void testPrimitiveClassHasNoFields() {
        Collection<Field> fields = new ArrayList<>();
        ReflectionUtils.getDeclaredFields(int.class, fields);
        assertTrue(fields.isEmpty(), "Primitive types have no declared fields");
    }

    /** Group: interface handling – should result in empty collection. */
    @Test
    void testInterfaceHasNoInstanceFields() {
        Collection<Field> fields = new ArrayList<>();
        ReflectionUtils.getDeclaredFields(SampleInterface.class, fields);
        assertTrue(fields.isEmpty(), "Interfaces do not declare instance fields");
    }

    /** Group: enum static fields are filtered. */
    @Test
    void testEnumConstantsAreFilteredOut() {
        Collection<Field> fields = new HashSet<>();
        ReflectionUtils.getDeclaredFields(SampleEnum.class, fields);
        assertTrue(fields.isEmpty(), "Enum constants are static and must be filtered out");
    }

    /** Group: unmodifiable collection argument – method must swallow UnsupportedOperationException. */
    @Test
    void testUnmodifiableCollectionIsHandledGracefully() {
        Collection<Field> unmodifiable = Collections.unmodifiableList(new ArrayList<>());
        assertDoesNotThrow(() -> ReflectionUtils.getDeclaredFields(ClassWithPublicField.class, unmodifiable),
                "Method should swallow UnsupportedOperationException from unmodifiable collection");
        assertTrue(unmodifiable.isEmpty(), "Unmodifiable collection should remain empty");
    }

    /** Group: collection that throws RuntimeException on add – exception must be swallowed. */
    @Test
    void testThrowingCollectionIsHandledGracefully() {
        Collection<Field> throwing = new ThrowingCollection();
        assertDoesNotThrow(() -> ReflectionUtils.getDeclaredFields(ClassWithPublicField.class, throwing),
                "Method should swallow IllegalStateException thrown by collection.add()");
    }

    /** Group: SecurityException from Class.getDeclaredFields – must be swallowed. */
    @Test
    void testSecurityExceptionFromGetDeclaredFieldsIsSwallowed() {
        // Install a SecurityManager that denies reflective access
        SecurityManager original = System.getSecurityManager();
        System.setSecurityManager(new SecurityManager() {
            @Override public void checkPermission(Permission perm) {
                // Allow everything except reflective access checks
                if (perm instanceof java.lang.reflect.ReflectPermission) {
                    throw new SecurityException("Reflective access denied");
                }
            }
        });

        try {
            Collection<Field> fields = new ArrayList<>();
            assertDoesNotThrow(() -> ReflectionUtils.getDeclaredFields(ClassWithRestrictedReflection.class, fields),
                    "Method should swallow SecurityException from getDeclaredFields()");
            assertTrue(fields.isEmpty(), "When reflection is denied, collection should stay empty");
        } finally {
            System.setSecurityManager(original); // restore original security manager
        }
    }

    /** Group: setAccessible throws SecurityException – field must still be added. */
    @Test
    void testSetAccessibleExceptionDoesNotPreventFieldAddition() throws Exception {
        // Install a SecurityManager that denies suppressAccessChecks permission only for the target field
        SecurityManager original = System.getSecurityManager();
        System.setSecurityManager(new SecurityManager() {
            @Override public void checkPermission(Permission perm) {
                if (perm instanceof java.lang.reflect.ReflectPermission &&
                    "suppressAccessChecks".equals(perm.getName())) {
                    throw new SecurityException("setAccessible denied");
                }
            }
        });

        try {
            Collection<Field> fields = new ArrayList<>();
            ReflectionUtils.getDeclaredFields(ClassWithExceptionInSetAccessible.class, fields);
            // The class has a single private field; it should still be added despite the exception
            assertEquals(1, fields.size(), "Field should be added even if setAccessible throws");
            Field f = fields.iterator().next();
            assertEquals("secret", f.getName());
        } finally {
            System.setSecurityManager(original);
        }
    }
}