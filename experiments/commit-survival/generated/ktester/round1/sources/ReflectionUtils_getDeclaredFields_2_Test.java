package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Modifier;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getDeclaredFields_2_Test {

    // field named "this$0" that points to the outer instance.
    class InnerClass {

        public int innerPublic;
    }

    /**
     * Simple class used by several tests.
     */
    static class SimpleClass {

        public int publicField;

        protected String protectedField;

        private double privateField;

        static long staticField;

        transient int transientField;
    }

    /**
     * Class that declares only static and transient fields.
     */
    static class StaticTransientClass {

        public static int staticField;

        private transient String transientField;
    }

    /**
     * Class that has a field named {@code metaClass} of type {@code groovy.lang.MetaClass}.
     */
    static class GroovyMetaClassClass {

        // The type name must be exactly "groovy.lang.MetaClass".  We cannot import the real
        // Groovy class, so we will use a mocked Field in the test instead of a real class.
        // The field is declared only to make the source compile.
        private Object metaClass;
    }

    /**
     * Class with a public and a private field.
     */
    static class PublicPrivateFieldsClass {

        public String pubField;

        private int privField;
    }

    /**
     * Super‑class with a field – used to verify that inherited fields are ignored.
     */
    static class SuperClass {

        public long superField;
    }

    /**
     * Child class that inherits from {@link SuperClass} but declares no fields of its own.
     */
    static class ChildClass extends SuperClass {
        // no fields declared here
    }

    /**
     * Outer class containing a non‑static inner class – the compiler creates a synthetic {@code this$} field.
     */
    class OuterClass {

        class InnerClass {

            public int innerPublic;
        }
    }

    class InnerClass {

        public int innerPublic;
    }

    /**
     * Simple collection that throws a RuntimeException on every {@code add}.
     */
    static class ThrowingCollection<E> implements Collection<E> {

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean contains(Object o) {
            return false;
        }

        @Override
        public Iterator<E> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public Object[] toArray() {
            return new Object[0];
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return a;
        }

        @Override
        public boolean add(E e) {
            throw new RuntimeException("add() not allowed");
        }

        @Override
        public boolean remove(Object o) {
            return false;
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            throw new RuntimeException("addAll() not allowed");
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return false;
        }

        @Override
        public void clear() {
        }
    }

    @Test
    void getDeclaredFields_IncludesPublicProtectedAndPrivateNonStaticFields() {
        Collection<Field> fields = new ArrayList<>();
        ReflectionUtils.getDeclaredFields(SimpleClass.class, fields);
        // Expected field names (order is not guaranteed)
        List<String> expected = List.of("publicField", "protectedField", "privateField");
        assertEquals(expected.size(), fields.size(), "Only non‑static, non‑transient fields should be collected");
        for (Field f : fields) {
            assertTrue(expected.contains(f.getName()), "Unexpected field collected: " + f.getName());
        }
    }

    @Test
    void getDeclaredFields_SkipsStaticAndTransientFields() {
        Collection<Field> fields = new ArrayList<>();
        ReflectionUtils.getDeclaredFields(SimpleClass.class, fields);
        // Ensure static and transient fields are not present
        for (Field f : fields) {
            assertNotEquals("staticField", f.getName(), "Static field should be skipped");
            assertNotEquals("transientField", f.getName(), "Transient field should be skipped");
        }
    }

    @Test
    void getDeclaredFields_SkipsSyntheticThis$FieldInInnerClass() {
        // The inner class is non‑static, so the compiler adds a synthetic
        // field named something like "this$0". That field must be ignored.
        Collection<Field> fields = new ArrayList<>();
        ReflectionUtils.getDeclaredFields(InnerClass.class, fields);
        // The only declared field we defined is "innerPublic"
        assertEquals(1, fields.size(), "Only the explicit field should be collected");
        Field onlyField = fields.iterator().next();
        assertEquals("innerPublic", onlyField.getName(), "Collected field name does not match expected");
    }

    @Test
    void getDeclaredFields_DoesNotThrowWhenCollectionIsEmpty() {
        // Verify that the method works even when the collection is initially empty.
        Collection<Field> fields = new ArrayList<>();
        assertDoesNotThrow(() -> ReflectionUtils.getDeclaredFields(SimpleClass.class, fields));
        assertFalse(fields.isEmpty(), "Fields should have been added to the collection");
    }

    @Test
    void getDeclaredFields_HandlesNullsGracefully() {
        // The method catches Throwable internally, so passing a null class
        // should not propagate an exception (it will simply be ignored).
        Collection<Field> fields = new ArrayList<>();
        assertDoesNotThrow(() -> ReflectionUtils.getDeclaredFields(null, fields));
        // No fields can be added when the class argument is null.
        assertTrue(fields.isEmpty(), "No fields should be added when class argument is null");
    }

    @Test
    void getDeclaredFields_HandlesNullClassGracefully() {
        Collection<Field> fields = new ArrayList<>();
        assertDoesNotThrow(() -> ReflectionUtils.getDeclaredFields(null, fields));
        assertTrue(fields.isEmpty(), "No fields should be added when class argument is null");
    }

    @Test
    void getDeclaredFields_SkipStaticAndTransientOnlyFields() {
        Collection<Field> fields = new ArrayList<>();
        ReflectionUtils.getDeclaredFields(StaticTransientClass.class, fields);
        assertEquals(0, fields.size(), "Class defines only static/transient fields – collection must stay empty");
    }

    @Test
    void getDeclaredFields_SkipGroovyMetaClassField() {
        // Mock a Field that looks like a Groovy metaClass field.
        Field metaField = mock(Field.class);
        when(metaField.getModifiers()).thenReturn(Modifier.PRIVATE);
        when(metaField.getName()).thenReturn("metaClass");
        Class<?> metaType = mock(Class.class);
        when(metaType.getName()).thenReturn("groovy.lang.MetaClass");
        when(metaField.getType()).thenReturn(metaType);
        // Mock a normal field to ensure the method would add it if not filtered.
        Field normalField = mock(Field.class);
        when(normalField.getModifiers()).thenReturn(Modifier.PUBLIC);
        when(normalField.getName()).thenReturn("normal");
        when(normalField.getType()).thenReturn(String.class);
        Class<?> mockedClass = mock(Class.class);
        when(mockedClass.getDeclaredFields()).thenReturn(new Field[] { metaField, normalField });
        Collection<Field> fields = new ArrayList<>();
        ReflectionUtils.getDeclaredFields(mockedClass, fields);
        // Only the normal field should survive the filter.
        assertEquals(1, fields.size(), "Groovy metaClass field must be skipped");
        assertEquals("normal", fields.iterator().next().getName());
    }

    @Test
    void getDeclaredFields_IncludesPublicAndPrivateFields_MakesPrivateAccessible() throws Exception {
        Collection<Field> fields = new ArrayList<>();
        ReflectionUtils.getDeclaredFields(PublicPrivateFieldsClass.class, fields);
        assertEquals(2, fields.size(), "Both public and private fields must be collected");
        boolean foundPublic = false;
        boolean foundPrivate = false;
        for (Field f : fields) {
            if ("pubField".equals(f.getName())) {
                foundPublic = true;
                assertTrue(Modifier.isPublic(f.getModifiers()));
            } else if ("privField".equals(f.getName())) {
                foundPrivate = true;
                // The private field should have been made accessible.
                assertTrue(f.canAccess(null) || f.isAccessible(), "Private field should be accessible after processing");
            }
        }
        assertTrue(foundPublic && foundPrivate, "Both expected fields should be present");
    }

    @Test
    void getDeclaredFields_PreserveExistingCollectionContents() {
        // Pre‑populate the collection with a dummy element.
        List<Field> prePopulated = new ArrayList<>();
        // any field
        prePopulated.add(SimpleClass.class.getDeclaredFields()[0]);
        ReflectionUtils.getDeclaredFields(PublicPrivateFieldsClass.class, prePopulated);
        // The original element plus two new fields = 3 total.
        assertEquals(3, prePopulated.size(), "Existing collection elements must be retained");
        Set<String> names = new HashSet<>();
        for (Field f : prePopulated) {
            names.add(f.getName());
        }
        assertTrue(names.contains("pubField"));
        assertTrue(names.contains("privField"));
    }

    @Test
    void getDeclaredFields_DoesNotIncludeInheritedFields() {
        Collection<Field> fields = new ArrayList<>();
        ReflectionUtils.getDeclaredFields(ChildClass.class, fields);
        assertEquals(0, fields.size(), "Declared fields of a subclass with none should be empty (inherited fields are ignored)");
    }

    @Test
    void getDeclaredFields_NullCollectionArgumentIsHandledGracefully() {
        // The method catches any Throwable; passing a null collection must not cause NPE.
        assertDoesNotThrow(() -> ReflectionUtils.getDeclaredFields(PublicPrivateFieldsClass.class, null));
        // No way to inspect the collection – the contract is simply “don’t throw”.
    }

    @Test
    void getDeclaredFields_ImmutableCollectionIsIgnoredWithoutException() {
        Collection<Field> immutable = Collections.unmodifiableCollection(new ArrayList<>());
        assertDoesNotThrow(() -> ReflectionUtils.getDeclaredFields(PublicPrivateFieldsClass.class, immutable));
        // The immutable collection cannot be modified, so it stays empty.
        assertTrue(immutable.isEmpty(), "Unmodifiable collection should remain empty");
    }

    @Test
    void getDeclaredFields_CollectionThrowingRuntimeExceptionIsSwallowed() {
        Collection<Field> throwing = new ThrowingCollection<>();
        assertDoesNotThrow(() -> ReflectionUtils.getDeclaredFields(PublicPrivateFieldsClass.class, throwing));
        // Nothing to assert beyond the fact that no exception escaped.
    }

    @Test
    void getDeclaredFields_SetAccessibleThrowsInaccessibleObjectException() throws Exception {
        // Mock a private field that throws InaccessibleObjectException when setAccessible(true) is called.
        Field problematicField = mock(Field.class);
        when(problematicField.getModifiers()).thenReturn(Modifier.PRIVATE);
        when(problematicField.getName()).thenReturn("problem");
        when(problematicField.getType()).thenReturn(String.class);
        doThrow(new InaccessibleObjectException("cannot make accessible")).when(problematicField).setAccessible(true);
        // Mock a normal public field that should be added without issue.
        Field normalField = mock(Field.class);
        when(normalField.getModifiers()).thenReturn(Modifier.PUBLIC);
        when(normalField.getName()).thenReturn("normal");
        when(normalField.getType()).thenReturn(int.class);
        Class<?> mockedClass = mock(Class.class);
        when(mockedClass.getDeclaredFields()).thenReturn(new Field[] { problematicField, normalField });
        Collection<Field> fields = new ArrayList<>();
        assertDoesNotThrow(() -> ReflectionUtils.getDeclaredFields(mockedClass, fields));
        // The normal field must be present; the problematic one is ignored because
        // setAccessible threw an exception that was swallowed.
        assertEquals(1, fields.size());
        assertEquals("normal", fields.iterator().next().getName());
    }

    @Test
    void getDeclaredFields_ClassGetDeclaredFieldsThrowsSecurityException() {
        Class<?> mockedClass = mock(Class.class);
        when(mockedClass.getDeclaredFields()).thenThrow(new SecurityException("access denied"));
        Collection<Field> fields = new ArrayList<>();
        assertDoesNotThrow(() -> ReflectionUtils.getDeclaredFields(mockedClass, fields));
        // The collection should stay unchanged (empty) because the exception is swallowed.
        assertTrue(fields.isEmpty(), "Fields collection must remain empty when SecurityException occurs");
    }
}
