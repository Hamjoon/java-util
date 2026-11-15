package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.*;
import java.util.*;

public class ReflectionUtilsTest {

    /*-------------------------------------------------------
     *  Helper annotations and test classes
     *-------------------------------------------------------*/

    @Retention(RetentionPolicy.RUNTIME)
    private @interface TestAnno { }

    @TestAnno
    private static class AnnotatedClass { }

    private interface AnnotatedInterface {
        @TestAnno
        default void annotatedMethod() { }
    }

    private static class ClassWithAnnotatedMethod implements AnnotatedInterface { }

    private static class BaseClass {
        public int publicBase = 1;
        private String privateBase = "base";
        static int staticBase = 2;
        transient int transientBase = 3;
        public BaseClass() { }
    }

    private static class SubClass extends BaseClass {
        public int publicBase = 10; // hides field in BaseClass
        private String privateSub = "sub";
        public SubClass() { }
    }

    private static class MethodExceptionClass {
        public void throwsException() {
            throw new IllegalStateException("boom");
        }
    }

    private static class OverloadedMethods {
        public void foo(int a) { }
        public void foo(String s) { }
    }

    private static class NoArgMethodClass {
        public String hello() { return "world"; }
    }

    private static class ConstructorClass {
        public ConstructorClass() { }
        public ConstructorClass(String s) { }
    }

    private static class DuplicateFieldParent {
        private String dup = "parent";
    }

    private static class DuplicateFieldChild extends DuplicateFieldParent {
        private String dup = "child";
    }

    /*-------------------------------------------------------
     *  List of public methods extracted from ReflectionUtils
     *-------------------------------------------------------*/
    // 1. <T extends Annotation> T getClassAnnotation(Class<?>, Class<T>)
    // 2. <T extends Annotation> T getMethodAnnotation(Method, Class<T>)
    // 3. Method getMethod(Class<?>, String, Class<?>...)
    // 4. List<Field> getDeclaredFields(Class<?>)
    // 5. Collection<Field> getDeepDeclaredFields(Class<?>)
    // 6. Map<String, Field> getDeepDeclaredFieldMap(Class<?>)
    // 7. Object call(Object, Method, Object...)
    // 8. Object call(Object, String, Object...)
    // 9. Method getMethod(Object, String, int)
    // 10. Constructor<?> getConstructor(Class<?>, Class<?>...)
    // 11. String getClassName(Object)
    // 12. String getClassNameFromByteCode(byte[])
    // 13. Method getNonOverloadedMethod(Class<?>, String)

    /*-------------------------------------------------------
     *  Basic functionality tests
     *-------------------------------------------------------*/

    @Test
    public void testGetClassAnnotation_FoundOnClass() {
        TestAnno anno = ReflectionUtils.getClassAnnotation(AnnotatedClass.class, TestAnno.class);
        assertNotNull("Annotation should be found on class", anno);
    }

    @Test
    public void testGetClassAnnotation_FoundOnInterface() {
        TestAnno anno = ReflectionUtils.getClassAnnotation(ClassWithAnnotatedMethod.class, TestAnno.class);
        assertNotNull("Annotation should be found on implemented interface method", anno);
    }

    @Test
    public void testGetClassAnnotation_NotFound() {
        TestAnno anno = ReflectionUtils.getClassAnnotation(BaseClass.class, TestAnno.class);
        assertNull("Annotation should not be found", anno);
    }

    @Test
    public void testGetMethodAnnotation_Found() throws Exception {
        Method m = AnnotatedInterface.class.getMethod("annotatedMethod");
        TestAnno anno = ReflectionUtils.getMethodAnnotation(m, TestAnno.class);
        assertNotNull("Method annotation should be found via inheritance", anno);
    }

    @Test
    public void testGetMethodAnnotation_NotFound() throws Exception {
        Method m = BaseClass.class.getMethod("toString"); // no TestAnno
        TestAnno anno = ReflectionUtils.getMethodAnnotation(m, TestAnno.class);
        assertNull("Method annotation should be null when not present", anno);
    }

    @Test
    public void testGetMethod_Success() {
        Method m = ReflectionUtils.getMethod(BaseClass.class, "toString");
        assertNotNull("Should retrieve public method toString()", m);
        assertEquals("Method name should match", "toString", m.getName());
    }

    @Test
    public void testGetMethod_NotFound() {
        Method m = ReflectionUtils.getMethod(BaseClass.class, "nonExistentMethod");
        assertNull("Should return null for missing method", m);
    }

    @Test
    public void testGetDeclaredFields_CacheWorks() {
        List<Field> fields1 = ReflectionUtils.getDeclaredFields(BaseClass.class);
        List<Field> fields2 = ReflectionUtils.getDeclaredFields(BaseClass.class);
        assertSame("Second call should return cached list instance", fields1, fields2);
        // Verify that private field is present
        boolean found = fields1.stream().anyMatch(f -> f.getName().equals("privateBase"));
        assertTrue("privateBase field should be present", found);
    }

    @Test
    public void testGetDeepDeclaredFields_IncludesInherited() {
        Collection<Field> fields = ReflectionUtils.getDeepDeclaredFields(SubClass.class);
        // should contain privateBase (inherited) and privateSub
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("privateBase")));
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("privateSub")));
        // static and transient fields should be filtered out
        assertFalse(fields.stream().anyMatch(f -> Modifier.isStatic(f.getModifiers())));
        assertFalse(fields.stream().anyMatch(f -> Modifier.isTransient(f.getModifiers())));
    }

    @Test
    public void testGetDeepDeclaredFieldMap_DuplicateNamesHandled() {
        Map<String, Field> map = ReflectionUtils.getDeepDeclaredFieldMap(DuplicateFieldChild.class);
        // Two fields named "dup" – one from parent, one from child.
        // The child field should be stored with qualified key.
        assertTrue(map.containsKey("dup"));
        assertTrue(map.containsKey(DuplicateFieldChild.class.getName() + ".dup"));
        assertEquals("child", map.get(DuplicateFieldChild.class.getName() + ".dup").get(null));
    }

    @Test
    public void testCall_WithMethodObject() throws Exception {
        NoArgMethodClass obj = new NoArgMethodClass();
        Method m = ReflectionUtils.getMethod(NoArgMethodClass.class, "hello");
        Object result = ReflectionUtils.call(obj, m);
        assertEquals("world", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCall_NullMethod() {
        ReflectionUtils.call(new Object(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCall_NullBean() throws Exception {
        Method m = NoArgMethodClass.class.getMethod("hello");
        ReflectionUtils.call(null, m);
    }

    @Test
    public void testCall_MethodThrowsExceptionWrapped() {
        MethodExceptionClass obj = new MethodExceptionClass();
        Method m = ReflectionUtils.getMethod(MethodExceptionClass.class, "throwsException");
        try {
            ReflectionUtils.call(obj, m);
            fail("Expected RuntimeException wrapping IllegalStateException");
        } catch (RuntimeException ex) {
            assertTrue(ex.getCause() instanceof IllegalStateException);
            assertEquals("boom", ex.getCause().getMessage());
        }
    }

    @Test
    public void testCall_ByName() {
        NoArgMethodClass obj = new NoArgMethodClass();
        Object result = ReflectionUtils.call(obj, "hello");
        assertEquals("world", result);
    }

    @Test
    public void testGetMethod_ByBeanAndArgCount() {
        Method m = ReflectionUtils.getMethod(new NoArgMethodClass(), "hello", 0);
        assertNotNull(m);
        assertEquals("hello", m.getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethod_ByBean_NullBean() {
        ReflectionUtils.getMethod(null, "any", 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethod_ByBean_NullMethodName() {
        ReflectionUtils.getMethod(new Object(), null, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethod_ByBean_MethodNotFound() {
        ReflectionUtils.getMethod(new NoArgMethodClass(), "nonexistent", 0);
    }

    @Test
    public void testGetConstructor_Success() {
        Constructor<?> c = ReflectionUtils.getConstructor(ConstructorClass.class);
        assertNotNull(c);
        assertEquals(0, c.getParameterTypes().length);
    }

    @Test
    public void testGetConstructor_WithParams() {
        Constructor<?> c = ReflectionUtils.getConstructor(ConstructorClass.class, String.class);
        assertNotNull(c);
        assertEquals(1, c.getParameterTypes().length);
        assertEquals(String.class, c.getParameterTypes()[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetConstructor_NoSuchConstructor() {
        ReflectionUtils.getConstructor(ConstructorClass.class, int.class);
    }

    @Test
    public void testGetClassName_Null() {
        assertEquals("null", ReflectionUtils.getClassName(null));
    }

    @Test
    public void testGetClassName_NonNull() {
        String name = ReflectionUtils.getClassName(this);
        assertEquals(this.getClass().getName(), name);
    }

    @Test
    public void testGetClassNameFromByteCode_ValidClass() throws Exception {
        // Load the .class file bytes of NoArgMethodClass using the class loader.
        String resource = NoArgMethodClass.class.getName().replace('.', '/') + ".class";
        try (InputStream is = NoArgMethodClass.class.getClassLoader().getResourceAsStream(resource)) {
            assertNotNull("Class file stream should be found", is);
            byte[] bytes = is.readAllBytes();
            String className = ReflectionUtils.getClassNameFromByteCode(bytes);
            assertEquals(NoArgMethodClass.class.getName(), className);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testGetClassNameFromByteCode_InvalidBytes() throws Exception {
        // Pass garbage bytes to trigger IllegalStateException
        byte[] garbage = new byte[] {0,1,2,3,4,5};
        ReflectionUtils.getClassNameFromByteCode(garbage);
    }

    @Test
    public void testGetNonOverloadedMethod_Success() {
        Method m = ReflectionUtils.getNonOverloadedMethod(NoArgMethodClass.class, "hello");
        assertNotNull(m);
        assertEquals("hello", m.getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethod_NullClass() {
        ReflectionUtils.getNonOverloadedMethod(null, "any");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethod_NullName() {
        ReflectionUtils.getNonOverloadedMethod(NoArgMethodClass.class, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethod_Overloaded() {
        ReflectionUtils.getNonOverloadedMethod(OverloadedMethods.class, "foo");
    }

    @Test
    public void testGetClassLoaderName_BootstrapOrAppClassLoader() {
        // For a class loaded by the application class loader the name should not be "null"
        String name = ReflectionUtils.getClassLoaderName(ReflectionUtilsTest.class);
        assertNotNull(name);
        // For bootstrap-loaded class (e.g., java.lang.Object) the method returns "bootstrap"
        String bootstrap = ReflectionUtils.getClassLoaderName(Object.class);
        assertEquals("bootstrap", bootstrap);
    }

    /*-------------------------------------------------------
     *  Edge‑case and exception handling tests (explicit)
     *-------------------------------------------------------*/

    @Test
    public void testGetMethod_CacheReturnsSameInstance() {
        Method m1 = ReflectionUtils.getMethod(BaseClass.class, "toString");
        Method m2 = ReflectionUtils.getMethod(BaseClass.class, "toString");
        assertSame("Cached method instances should be identical", m1, m2);
    }

    @Test
    public void testGetDeepDeclaredFields_CacheWorks() {
        Collection<Field> first = ReflectionUtils.getDeepDeclaredFields(BaseClass.class);
        Collection<Field> second = ReflectionUtils.getDeepDeclaredFields(BaseClass.class);
        assertSame("Cache should return same collection object", first, second);
    }

    @Test
    public void testGetDeepDeclaredFieldMap_ContainsAllFields() {
        Map<String, Field> map = ReflectionUtils.getDeepDeclaredFieldMap(SubClass.class);
        assertTrue(map.containsKey("publicBase")); // field from SubClass (hides parent)
        // Ensure parent field is still reachable via qualified key
        assertTrue(map.containsKey(BaseClass.class.getName() + ".publicBase"));
    }

    @Test
    public void testCall_WithNullArgumentsArray() throws Exception {
        NoArgMethodClass obj = new NoArgMethodClass();
        Method m = ReflectionUtils.getMethod(NoArgMethodClass.class, "hello");
        // args varargs can be omitted; passing null should be handled as empty array
        Object result = ReflectionUtils.call(obj, m, (Object[]) null);
        assertEquals("world", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethod_WithNullClass() {
        ReflectionUtils.getMethod(null, "anyMethod");
    }

    @Test
    public void testMakeParamKey_Empty() throws Exception {
        // Access private static method via reflection to verify behavior
        Method mk = ReflectionUtils.class.getDeclaredMethod("makeParamKey", Class[].class);
        mk.setAccessible(true);
        String key = (String) mk.invoke(null, (Object) new Class<?>[0]);
        assertEquals("", key);
    }

    @Test
    public void testMakeParamKey_WithParams() throws Exception {
        Method mk = ReflectionUtils.class.getDeclaredMethod("makeParamKey", Class[].class);
        mk.setAccessible(true);
        String key = (String) mk.invoke(null, (Object) new Class<?>[]{String.class, int.class});
        assertEquals(":java.lang.String|int", key);
    }
}
