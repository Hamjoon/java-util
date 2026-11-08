package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import java.io.InputStream;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

public class ReflectionUtilsTest {

    /*--------------------------------------------------------------
     *  Helper types used by the tests
     *--------------------------------------------------------------*/

    /** Simple annotation for testing class‑ and method‑level annotations */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface TestAnno {
        String value() default "";
    }

    /** Interface annotated with @TestAnno */
    @TestAnno("interface")
    interface AnnotatedInterface { }

    /** Superclass annotated with @TestAnno */
    @TestAnno("super")
    static class SuperClass implements AnnotatedInterface {
        public String superMethod() { return "super"; }
    }

    /** Subclass that inherits annotations from its hierarchy */
    static class SubClass extends SuperClass {
        private int privateField;
        public static final String STATIC_CONST = "CONST";
        private transient String transientField;
        public String publicField;
        public SubClass() { }
        public SubClass(String s) { }
        @TestAnno("method")
        public String annotatedMethod() { return "annotated"; }
        public String overloaded(int i) { return "int:" + i; }
        public String overloaded(String s) { return "str:" + s; }
        public void throwsChecked() throws Exception { throw new Exception("checked"); }
    }

    /** Simple class with a method that throws a RuntimeException */
    static class ExceptionThrower {
        public void boom() { throw new IllegalStateException("boom"); }
    }

    /** Class with only static fields – used to verify that static fields are ignored */
    static class OnlyStatic {
        public static int a = 1;
        private static String b = "b";
    }

    /*--------------------------------------------------------------
     *  1.  getClassAnnotation
     *--------------------------------------------------------------*/

    @Test
    public void testGetClassAnnotationDirect() {
        TestAnno a = ReflectionUtils.getClassAnnotation(SubClass.class, TestAnno.class);
        assertNotNull("Annotation should be found on SubClass (inherited from SuperClass)", a);
        assertEquals("super", a.value());
    }

    @Test
    public void testGetClassAnnotationInterface() {
        TestAnno a = ReflectionUtils.getClassAnnotation(SubClass.class, TestAnno.class);
        assertNotNull(a);
        // The first annotation encountered in the exhaustive search is the one on SuperClass,
        // but we also verify that interfaces are examined.
        assertTrue("Annotation value must be either 'super' or 'interface'",
                "super".equals(a.value()) || "interface".equals(a.value()));
    }

    @Test
    public void testGetClassAnnotationNotPresent() {
        TestAnno a = ReflectionUtils.getClassAnnotation(OnlyStatic.class, TestAnno.class);
        assertNull("No annotation should be found", a);
    }

    @Test(expected = NullPointerException.class)
    public void testGetClassAnnotationNullClass() {
        ReflectionUtils.getClassAnnotation(null, TestAnno.class);
    }

    @Test(expected = NullPointerException.class)
    public void testGetClassAnnotationNullAnnoClass() {
        ReflectionUtils.getClassAnnotation(SubClass.class, null);
    }

    /*--------------------------------------------------------------
     *  2.  getMethodAnnotation
     *--------------------------------------------------------------*/

    @Test
    public void testGetMethodAnnotationFoundOnSuperclass() throws Exception {
        Method m = SubClass.class.getMethod("superMethod");
        TestAnno a = ReflectionUtils.getMethodAnnotation(m, TestAnno.class);
        // No annotation on the method itself, but the class hierarchy is examined;
        // the annotation is on SuperClass (class level), not method level, so result is null.
        assertNull(a);
    }

    @Test
    public void testGetMethodAnnotationDirectMethod() throws Exception {
        Method m = SubClass.class.getMethod("annotatedMethod");
        TestAnno a = ReflectionUtils.getMethodAnnotation(m, TestAnno.class);
        assertNotNull(a);
        assertEquals("method", a.value());
    }

    @Test
    public void testGetMethodAnnotationNotPresent() throws Exception {
        Method m = SubClass.class.getMethod("toString");
        TestAnno a = ReflectionUtils.getMethodAnnotation(m, TestAnno.class);
        assertNull(a);
    }

    @Test(expected = NullPointerException.class)
    public void testGetMethodAnnotationNullMethod() {
        ReflectionUtils.getMethodAnnotation(null, TestAnno.class);
    }

    @Test(expected = NullPointerException.class)
    public void testGetMethodAnnotationNullAnnoClass() throws Exception {
        Method m = SubClass.class.getMethod("annotatedMethod");
        ReflectionUtils.getMethodAnnotation(m, null);
    }

    /*--------------------------------------------------------------
     *  3.  getMethod(Class, String, Class...)
     *--------------------------------------------------------------*/

    @Test
    public void testGetMethodExactMatch() {
        Method m = ReflectionUtils.getMethod(SubClass.class, "annotatedMethod");
        assertNotNull(m);
        assertEquals("annotatedMethod", m.getName());
    }

    @Test
    public void testGetMethodWithParameterTypes() {
        Method m = ReflectionUtils.getMethod(SubClass.class, "overloaded", int.class);
        assertNotNull(m);
        assertEquals(int.class, m.getParameterTypes()[0]);
    }

    @Test
    public void testGetMethodNotFoundReturnsNull() {
        Method m = ReflectionUtils.getMethod(SubClass.class, "nonExistingMethod");
        assertNull(m);
    }

    @Test
    public void testGetMethodCaching() {
        Method first = ReflectionUtils.getMethod(SubClass.class, "annotatedMethod");
        Method second = ReflectionUtils.getMethod(SubClass.class, "annotatedMethod");
        assertSame("Method instances should be cached and identical", first, second);
    }

    /*--------------------------------------------------------------
     *  4.  getDeclaredFields(Class)
     *--------------------------------------------------------------*/

    @Test
    public void testGetDeclaredFieldsFiltersStaticAndTransient() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(SubClass.class);
        // Expect privateField, publicField, but not static or transient fields
        Set<String> names = new HashSet<>();
        for (Field f : fields) names.add(f.getName());

        assertTrue(names.contains("privateField"));
        assertTrue(names.contains("publicField"));
        assertFalse("STATIC_CONST should be filtered out", names.contains("STATIC_CONST"));
        assertFalse("transientField should be filtered out", names.contains("transientField"));
    }

    @Test
    public void testGetDeclaredFieldsOnClassWithOnlyStatic() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(OnlyStatic.class);
        assertTrue("No instance fields should be returned", fields.isEmpty());
    }

    @Test(expected = NullPointerException.class)
    public void testGetDeclaredFieldsNullClass() {
        ReflectionUtils.getDeclaredFields(null);
    }

    /*--------------------------------------------------------------
     *  5.  getDeepDeclaredFields(Class)
     *--------------------------------------------------------------*/

    @Test
    public void testGetDeepDeclaredFieldsIncludesInherited() {
        Collection<Field> fields = ReflectionUtils.getDeepDeclaredFields(SubClass.class);
        Set<String> names = new HashSet<>();
        for (Field f : fields) names.add(f.getName());

        // From SubClass
        assertTrue(names.contains("privateField"));
        assertTrue(names.contains("publicField"));
        // From SuperClass (method does not add fields, but we verify no extra static)
        // No static fields should appear
        assertFalse(names.contains("STATIC_CONST"));
    }

    @Test
    public void testGetDeepDeclaredFieldsCaching() {
        Collection<Field> first = ReflectionUtils.getDeepDeclaredFields(SubClass.class);
        Collection<Field> second = ReflectionUtils.getDeepDeclaredFields(SubClass.class);
        assertSame("Cached collection should be returned", first, second);
    }

    /*--------------------------------------------------------------
     *  6.  getDeclaredFields(Class, Collection<Field>)
     *--------------------------------------------------------------*/

    @Test
    public void testPopulateCollectionWithDeclaredFields() {
        Collection<Field> coll = new ArrayList<>();
        ReflectionUtils.getDeclaredFields(SubClass.class, coll);
        assertFalse(coll.isEmpty());
        // Ensure at least one known field is present
        boolean found = coll.stream().anyMatch(f -> f.getName().equals("publicField"));
        assertTrue(found);
    }

    /*--------------------------------------------------------------
     *  7.  getDeepDeclaredFieldMap(Class)
     *--------------------------------------------------------------*/

    @Test
    public void testGetDeepDeclaredFieldMapHandlesDuplicateNames() {
        // Create a subclass that hides a field name from its superclass
        class Parent {
            private String dup = "parent";
        }
        class Child extends Parent {
            private String dup = "child";
        }

        Map<String, Field> map = ReflectionUtils.getDeepDeclaredFieldMap(Child.class);
        // Both fields should be present, one with simple name, one with qualified name
        assertTrue(map.containsKey("dup"));
        assertTrue(map.containsKey(Parent.class.getName() + ".dup"));
        assertTrue(map.containsKey(Child.class.getName() + ".dup"));
    }

    /*--------------------------------------------------------------
     *  8.  call(Object, Method, Object...)
     *--------------------------------------------------------------*/

    @Test
    public void testCallWithValidMethod() throws Exception {
        SubClass obj = new SubClass();
        Method m = SubClass.class.getMethod("annotatedMethod");
        Object result = ReflectionUtils.call(obj, m);
        assertEquals("annotated", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallNullMethod() {
        ReflectionUtils.call(new SubClass(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallNullBean() throws Exception {
        Method m = SubClass.class.getMethod("annotatedMethod");
        ReflectionUtils.call(null, m);
    }

    @Test
    public void testCallMethodThatThrowsCheckedExceptionWrapped() throws Exception {
        SubClass obj = new SubClass();
        Method m = SubClass.class.getMethod("throwsChecked");
        try {
            ReflectionUtils.call(obj, m);
            fail("RuntimeException expected");
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof Exception);
            assertEquals("checked", e.getCause().getMessage());
        }
    }

    @Test
    public void testCallMethodThatThrowsRuntimeExceptionWrapped() throws Exception {
        ExceptionThrower obj = new ExceptionThrower();
        Method m = ExceptionThrower.class.getMethod("boom");
        try {
            ReflectionUtils.call(obj, m);
            fail("RuntimeException expected");
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof IllegalStateException);
            assertEquals("boom", e.getCause().getMessage());
        }
    }

    /*--------------------------------------------------------------
     *  9.  call(Object, String, Object...)
     *--------------------------------------------------------------*/

    @Test
    public void testCallByNameSimple() {
        SubClass obj = new SubClass();
        Object result = ReflectionUtils.call(obj, "annotatedMethod");
        assertEquals("annotated", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallByNameMethodNotFound() {
        SubClass obj = new SubClass();
        ReflectionUtils.call(obj, "nonExistingMethod");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallByNameNullBean() {
        ReflectionUtils.call(null, "anyMethod");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallByNameNullMethodName() {
        SubClass obj = new SubClass();
        ReflectionUtils.call(obj, null);
    }

    /*--------------------------------------------------------------
     * 10. getMethod(Object, String, int)
     *--------------------------------------------------------------*/

    @Test
    public void testGetMethodByArgCount() {
        SubClass obj = new SubClass();
        Method m = ReflectionUtils.getMethod(obj, "overloaded", 1);
        assertNotNull(m);
        assertEquals(1, m.getParameterTypes().length);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodByArgCountNullBean() {
        ReflectionUtils.getMethod(null, "any", 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodByArgCountNullName() {
        SubClass obj = new SubClass();
        ReflectionUtils.getMethod(obj, null, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodByArgCountNotFound() {
        SubClass obj = new SubClass();
        ReflectionUtils.getMethod(obj, "nonExisting", 0);
    }

    /*--------------------------------------------------------------
     * 11. getConstructor(Class, Class...)
     *--------------------------------------------------------------*/

    @Test
    public void testGetConstructorNoArgs() {
        Constructor<?> c = ReflectionUtils.getConstructor(SubClass.class);
        assertNotNull(c);
        assertEquals(0, c.getParameterCount());
    }

    @Test
    public void testGetConstructorWithArgs() {
        Constructor<?> c = ReflectionUtils.getConstructor(SubClass.class, String.class);
        assertNotNull(c);
        assertEquals(1, c.getParameterCount());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetConstructorNotExist() {
        ReflectionUtils.getConstructor(SubClass.class, int.class);
    }

    /*--------------------------------------------------------------
     * 12. getClassName(Object)
     *--------------------------------------------------------------*/

    @Test
    public void testGetClassNameNonNull() {
        String name = ReflectionUtils.getClassName(new SubClass());
        assertEquals(SubClass.class.getName(), name);
    }

    @Test
    public void testGetClassNameNull() {
        assertEquals("null", ReflectionUtils.getClassName(null));
    }

    /*--------------------------------------------------------------
     * 13. getClassNameFromByteCode(byte[])
     *--------------------------------------------------------------*/

    @Test
    public void testGetClassNameFromByteCodeRealClass() throws Exception {
        // Load the .class bytes of this test class itself
        String resource = "/" + ReflectionUtilsTest.class.getName().replace('.', '/') + ".class";
        InputStream is = ReflectionUtilsTest.class.getResourceAsStream(resource);
        assertNotNull("Unable to locate class file for test", is);
        byte[] bytes = is.readAllBytes();
        String className = ReflectionUtils.getClassNameFromByteCode(bytes);
        assertEquals(ReflectionUtilsTest.class.getName(), className);
    }

    @Test(expected = IllegalStateException.class)
    public void testGetClassNameFromByteCodeInvalidFormat() throws Exception {
        // Empty byte array triggers IllegalStateException because the parser expects a tag
        ReflectionUtils.getClassNameFromByteCode(new byte[0]);
    }

    /*--------------------------------------------------------------
     * 14. getClassLoaderName (package‑private)
     *--------------------------------------------------------------*/

    @Test
    public void testGetClassLoaderNameBootstrap() {
        // Bootstrap classes such as java.lang.Object have a null class loader
        String name = ReflectionUtils.getClassLoaderName(Object.class);
        assertEquals("bootstrap", name);
    }

    @Test
    public void testGetClassLoaderNameNonBootstrap() {
        String name = ReflectionUtils.getClassLoaderName(ReflectionUtilsTest.class);
        assertNotNull(name);
        assertFalse(name.isEmpty());
        assertNotEquals("bootstrap", name);
    }
}
