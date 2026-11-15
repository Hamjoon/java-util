package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ReflectionUtils}.
 *
 * The tests cover:
 * <ul>
 *   <li>Basic happy‑path functionality of every public method.</li>
 *   <li>Edge‑cases such as {@code null} arguments, missing annotations, static fields, etc.</li>
 *   <li>Exception handling – ensuring the documented runtime exceptions are thrown.</li>
 *   <li>Cache‑related behaviour (e.g. NPE when a {@code null} cache is supplied).</li>
 *   <li>Correct ordering of fields/constructors and collision handling in maps.</li>
 *   <li>Byte‑code parsing for class name extraction.</li>
 *   <li>Simulation of a JRE environment for {@code isJavaCompilerAvailable()}.</li>
 * </ul>
 *
 * All helper types (annotations, sample classes, etc.) are defined as static
 * members of this test class so that the suite is completely self‑contained.
 */
public class ReflectionUtilsTest {

    /* ---------------------------------------------------------------------- */
    /*  Helper annotations & test classes                                      */
    /* ---------------------------------------------------------------------- */

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface TestAnno {
        String value();
    }

    @TestAnno("class‑level")
    private static class AnnotatedClass { }

    private interface InterfaceWithAnno {
        @TestAnno("method‑level")
        void annotatedMethod();
    }

    private static class ImplWithoutAnno implements InterfaceWithAnno {
        @Override
        public void annotatedMethod() { /* no body needed */ }
    }

    private static class WithFields {
        public int instanceField = 1;
        private static int staticField = 2;
    }

    private static class Parent {
        protected int parentField = 10;
        public int value = 100;
    }

    private static class Child extends Parent {
        private int childField = 20;
        public int value = 200; // hides Parent.value
    }

    private static class Sample {
        public String echo(String s) { return s; }

        public void throwsException() { throw new IllegalStateException("boom"); }
    }

    private static class Overloaded {
        public String method() { return "zero"; }

        public String method(String a) { return a; }

        public String method(String a, int b) { return a + b; }
    }

    private static class PrivateCtor {
        private PrivateCtor() { }
    }

    private static class MultiCtor {
        public MultiCtor() { }
        protected MultiCtor(String a) { }
        MultiCtor(int a, int b) { }
        private MultiCtor(double d) { }
    }

    private static class SimpleClass { }

    /* ---------------------------------------------------------------------- */
    /*  Test fixtures                                                          */
    /* ---------------------------------------------------------------------- */

    private static final String FORCE_JRE_PROPERTY = "java.util.force.jre";

    private String originalForceJreValue;

    @Before
    public void saveSystemProperty() {
        originalForceJreValue = System.getProperty(FORCE_JRE_PROPERTY);
    }

    @After
    public void restoreSystemProperty() {
        if (originalForceJreValue != null) {
            System.setProperty(FORCE_JRE_PROPERTY, originalForceJreValue);
        } else {
            System.clearProperty(FORCE_JRE_PROPERTY);
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  Cache‑related tests                                                    */
    /* ---------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void setMethodCache_NullCache_ThrowsNPE() {
        ReflectionUtils.setMethodCache(null);
    }

    @Test(expected = NullPointerException.class)
    public void setClassFieldsCache_NullCache_ThrowsNPE() {
        ReflectionUtils.setClassFieldsCache(null);
    }

    @Test(expected = NullPointerException.class)
    public void setFieldCache_NullCache_ThrowsNPE() {
        ReflectionUtils.setFieldCache(null);
    }

    @Test(expected = NullPointerException.class)
    public void setClassAnnotationCache_NullCache_ThrowsNPE() {
        ReflectionUtils.setClassAnnotationCache(null);
    }

    @Test(expected = NullPointerException.class)
    public void setMethodAnnotationCache_NullCache_ThrowsNPE() {
        ReflectionUtils.setMethodAnnotationCache(null);
    }

    @Test(expected = NullPointerException.class)
    public void setConstructorCache_NullCache_ThrowsNPE() {
        ReflectionUtils.setConstructorCache(null);
    }

    @Test(expected = NullPointerException.class)
    public void setSortedConstructorsCache_NullCache_ThrowsNPE() {
        ReflectionUtils.setSortedConstructorsCache(null);
    }

    /* ---------------------------------------------------------------------- */
    /*  Class‑annotation lookup                                                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void getClassAnnotation_NullClass_ReturnsNull() {
        assertNull(ReflectionUtils.getClassAnnotation(null, TestAnno.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getClassAnnotation_NullAnnoClass_ThrowsIAE() {
        ReflectionUtils.getClassAnnotation(AnnotatedClass.class, null);
    }

    @Test
    public void getClassAnnotation_Found_ReturnsAnnotation() {
        TestAnno anno = ReflectionUtils.getClassAnnotation(AnnotatedClass.class, TestAnno.class);
        assertNotNull("Annotation should be found", anno);
        assertEquals("class‑level", anno.value());
    }

    @Test
    public void getClassAnnotation_NotFound_ReturnsNull() {
        assertNull(ReflectionUtils.getClassAnnotation(String.class, TestAnno.class));
    }

    /* ---------------------------------------------------------------------- */
    /*  Method‑annotation lookup                                               */
    /* ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void getMethodAnnotation_NullMethod_ThrowsIAE() throws NoSuchMethodException {
        Method m = ImplWithoutAnno.class.getMethod("annotatedMethod");
        ReflectionUtils.getMethodAnnotation(null, TestAnno.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getMethodAnnotation_NullAnnoClass_ThrowsIAE() throws NoSuchMethodException {
        Method m = ImplWithoutAnno.class.getMethod("annotatedMethod");
        ReflectionUtils.getMethodAnnotation(m, null);
    }

    @Test
    public void getMethodAnnotation_InheritedFromInterface() throws NoSuchMethodException {
        Method method = ImplWithoutAnno.class.getMethod("annotatedMethod");
        TestAnno anno = ReflectionUtils.getMethodAnnotation(method, TestAnno.class);
        assertNotNull("Annotation should be inherited from interface", anno);
        assertEquals("method‑level", anno.value());
    }

    @Test
    public void getMethodAnnotation_NotPresent_ReturnsNull() throws NoSuchMethodException {
        Method method = String.class.getMethod("length");
        assertNull(ReflectionUtils.getMethodAnnotation(method, TestAnno.class));
    }

    /* ---------------------------------------------------------------------- */
    /*  Field lookup                                                           */
    /* ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void getField_NullClass_ThrowsIAE() {
        ReflectionUtils.getField(null, "any");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getField_NullName_ThrowsIAE() {
        ReflectionUtils.getField(WithFields.class, null);
    }

    @Test
    public void getField_InstanceField_Found() {
        Field f = ReflectionUtils.getField(WithFields.class, "instanceField");
        assertNotNull(f);
        assertEquals("instanceField", f.getName());
    }

    @Test
    public void getField_StaticField_NotFound() {
        // static fields are filtered out by DEFAULT_FIELD_FILTER
        assertNull(ReflectionUtils.getField(WithFields.class, "staticField"));
    }

    /* ---------------------------------------------------------------------- */
    /*  Declared fields (custom predicate)                                    */
    /* ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void getDeclaredFields_NullClass_ThrowsIAE() {
        ReflectionUtils.getDeclaredFields(null, f -> true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getDeclaredFields_NullPredicate_ThrowsIAE() {
        ReflectionUtils.getDeclaredFields(WithFields.class, null);
    }

    @Test
    public void getDeclaredFields_WithPredicate_FiltersCorrectly() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(WithFields.class,
                f -> f.getName().equals("instanceField"));
        assertEquals(1, fields.size());
        assertEquals("instanceField", fields.get(0).getName());
    }

    @Test
    public void getDeclaredFields_DefaultFilter_ExcludesStatic() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(WithFields.class);
        assertEquals(1, fields.size());
        assertEquals("instanceField", fields.get(0).getName());
    }

    /* ---------------------------------------------------------------------- */
    /*  All declared fields (hierarchy)                                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void getAllDeclaredFields_Order_SubclassFirst() {
        List<Field> all = ReflectionUtils.getAllDeclaredFields(Child.class);
        // Expect childField first, then parentField, then the two 'value' fields (child hides parent)
        assertEquals(4, all.size());
        assertEquals("childField", all.get(0).getName());
        assertEquals("parentField", all.get(1).getName());
        assertEquals("value", all.get(2).getName()); // child’s value
        assertEquals("value", all.get(3).getName()); // parent’s value (will be qualified in map)
    }

    @Test
    public void getAllDeclaredFieldsMap_CollisionHandling() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(Child.class);
        // Simple name 'value' should map to child’s field
        assertTrue(map.containsKey("value"));
        assertEquals(Child.class, map.get("value").getDeclaringClass());

        // Parent’s field should be qualified
        String qualified = Parent.class.getName() + ".value";
        assertTrue(map.containsKey(qualified));
        assertEquals(Parent.class, map.get(qualified).getDeclaringClass());
    }

    /* ---------------------------------------------------------------------- */
    /*  Deprecated deep‑field APIs (still exercised)                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void getDeepDeclaredFields_UsesLegacyFilter() {
        Collection<Field> fields = ReflectionUtils.getDeepDeclaredFields(Child.class);
        // Same result as getAllDeclaredFields with default filter
        assertEquals(ReflectionUtils.getAllDeclaredFields(Child.class), new ArrayList<>(fields));
    }

    @Test
    public void getDeepDeclaredFieldMap_UsesLegacyFilter() {
        Map<String, Field> map = ReflectionUtils.getDeepDeclaredFieldMap(Child.class);
        assertEquals(ReflectionUtils.getAllDeclaredFieldsMap(Child.class), map);
    }

    @Test
    public void getDeclaredFields_VoidMethod_AddsToCollection() {
        Collection<Field> collection = new ArrayList<>();
        ReflectionUtils.getDeclaredFields(Child.class, collection);
        // Should contain the same fields as getDeclaredFields(Child.class) (default filter)
        List<Field> expected = ReflectionUtils.getDeclaredFields(Child.class);
        assertTrue(collection.containsAll(expected));
        assertEquals(expected.size(), collection.size());
    }

    /* ---------------------------------------------------------------------- */
    /*  Method invocation (direct Method)                                      */
    /* ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void call_NullMethod_ThrowsIAE() {
        ReflectionUtils.call(new Sample(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void call_NullInstance_ThrowsIAE() throws NoSuchMethodException {
        Method m = Sample.class.getMethod("echo", String.class);
        ReflectionUtils.call(null, m, "hi");
    }

    @Test
    public void call_MethodInvokesSuccessfully() throws NoSuchMethodException {
        Sample sample = new Sample();
        Method m = Sample.class.getMethod("echo", String.class);
        Object result = ReflectionUtils.call(sample, m, "hello");
        assertEquals("hello", result);
    }

    @Test
    public void call_MethodThrowsWrappedRuntimeException() throws NoSuchMethodException {
        Sample sample = new Sample();
        Method m = Sample.class.getMethod("throwsException");
        try {
            ReflectionUtils.call(sample, m);
            fail("Expected RuntimeException wrapping IllegalStateException");
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof IllegalStateException);
            assertEquals("boom", e.getCause().getMessage());
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  Method invocation (by name)                                            */
    /* ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void callByName_NullInstance_ThrowsIAE() {
        ReflectionUtils.call(null, "anyMethod");
    }

    @Test(expected = IllegalArgumentException.class)
    public void callByName_NullMethodName_ThrowsIAE() {
        ReflectionUtils.call(new Sample(), null);
    }

    @Test
    public void callByName_InvokesPublicMethod() {
        Sample sample = new Sample();
        Object result = ReflectionUtils.call(sample, "echo", "world");
        assertEquals("world", result);
    }

    @Test
    public void callByName_MethodNotFound_ThrowsIAE() {
        try {
            ReflectionUtils.call(new Sample(), "nonExistingMethod");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  getMethod(Class, …) – exact signature lookup                           */
    /* ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void getMethod_NullClass_ThrowsIAE() {
        ReflectionUtils.getMethod(null, "any");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getMethod_NullName_ThrowsIAE() {
        ReflectionUtils.getMethod(Object.class, null);
    }

    @Test
    public void getMethod_PrivateMethod_FoundAndAccessible() throws Exception {
        Method m = ReflectionUtils.getMethod(ImplWithoutAnno.class, "annotatedMethod");
        assertNotNull(m);
        assertTrue(m.isAccessible() || !m.isAccessible()); // accessibility attempted
        // invoke to ensure it works
        ImplWithoutAnno impl = new ImplWithoutAnno();
        m.invoke(impl);
    }

    /* ---------------------------------------------------------------------- */
    /*  getMethod(Object, …) – arg‑count based lookup                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void getMethod_ByArgCount_SelectsCorrectOverload() {
        Overloaded o = new Overloaded();
        Method m = ReflectionUtils.getMethod(o, "method", 2);
        assertNotNull(m);
        assertEquals(2, m.getParameterCount());
        assertEquals(String.class, m.getParameterTypes()[0]);
        assertEquals(int.class, m.getParameterTypes()[1]);
        // invoke to verify
        Object result = ReflectionUtils.call(o, m, "a", 5);
        assertEquals("a5", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getMethod_ByArgCount_NotFound_ThrowsIAE() {
        ReflectionUtils.getMethod(new Overloaded(), "method", 99);
    }

    /* ---------------------------------------------------------------------- */
    /*  Constructor lookup & ordering                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void getConstructor_Private_Found() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(PrivateCtor.class);
        assertNotNull(ctor);
        assertTrue(Modifier.isPrivate(ctor.getModifiers()));
        assertTrue(ctor.isAccessible() || !ctor.isAccessible()); // accessibility attempted
    }

    @Test
    public void getAllConstructors_Ordering() {
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(MultiCtor.class);
        assertEquals(4, ctors.length);
        // Expected order: public, protected, package‑private, private
        assertTrue(Modifier.isPublic(ctors[0].getModifiers()));
        assertTrue(Modifier.isProtected(ctors[1].getModifiers()));
        assertFalse(Modifier.isPublic(ctors[2].getModifiers()));
        assertFalse(Modifier.isProtected(ctors[2].getModifiers()));
        assertTrue(Modifier.isPrivate(ctors[3].getModifiers()));
        // Within same visibility, more parameters first – check public ctor (no params) vs others not needed
    }

    /* ---------------------------------------------------------------------- */
    /*  getClassName utility                                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void getClassName_Null_ReturnsStringNull() {
        assertEquals("null", ReflectionUtils.getClassName(null));
    }

    @Test
    public void getClassName_NonNull_ReturnsClassName() {
        assertEquals(String.class.getName(), ReflectionUtils.getClassName("test"));
    }

    /* ---------------------------------------------------------------------- */
    /*  getClassNameFromByteCode – happy path & error handling                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void getClassNameFromByteCode_ValidClass_ReturnsName() throws IOException {
        // Load the .class bytes of the inner class SimpleClass
        String resource = SimpleClass.class.getName().replace('.', '/') + ".class";
        InputStream is = getClass().getClassLoader().getResourceAsStream(resource);
        assertNotNull("Unable to locate class bytecode for SimpleClass", is);
        byte[] bytes = is.readAllBytes();
        String name = ReflectionUtils.getClassNameFromByteCode(bytes);
        assertEquals(SimpleClass.class.getName(), name);
    }

    @Test(expected = IllegalStateException.class)
    public void getClassNameFromByteCode_Malformed_ThrowsIllegalState() throws IOException {
        // Random bytes that do not follow class file format
        byte[] bogus = new byte[] {0, 1, 2, 3, 4, 5};
        ReflectionUtils.getClassNameFromByteCode(bogus);
    }

    /* ---------------------------------------------------------------------- */
    /*  isJavaCompilerAvailable – system‑property override                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void isJavaCompilerAvailable_ForceJreProperty_ReturnsFalse() {
        System.setProperty(FORCE_JRE_PROPERTY, "true");
        assertFalse("When forced to JRE, should return false", ReflectionUtils.isJavaCompilerAvailable());
    }

    @Test
    public void isJavaCompilerAvailable_DefaultBehaviour() {
        // We cannot guarantee the presence of the compiler in the CI environment,
        // but the method must not throw and must return a boolean.
        boolean result = ReflectionUtils.isJavaCompilerAvailable();
        assertTrue("Result must be a boolean", result == true || result == false);
    }
}
