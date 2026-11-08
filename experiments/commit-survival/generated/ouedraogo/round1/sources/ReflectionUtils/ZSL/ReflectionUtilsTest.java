package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.lang.annotation.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.Assert.*;

public class ReflectionUtilsTest {

    /* ---------- Helper Annotations & Classes ---------- */

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    private @interface TestAnno {
        String value();
    }

    @TestAnno("class")
    private static class BaseClass {
        @TestAnno("method")
        public String annotatedMethod() {
            return "base";
        }

        public String overloaded() { return "no-args"; }
        public String overloaded(String s) { return "one-arg"; }
    }

    private static class SubClass extends BaseClass implements TestInterface {
        // inherits annotatedMethod()
        @Override
        public String interfaceMethod() {
            return "sub";
        }
    }

    @TestAnno("interface")
    private interface TestInterface {
        @TestAnno("interfaceMethod")
        default String interfaceMethod() { return "default"; }
    }

    private static class NoDefaultConstructor {
        private NoDefaultConstructor(int x) { }
    }

    /* ---------- Reset static caches before each test ---------- */
    @Before
    public void resetCaches() throws Exception {
        // Use reflection to clear the static concurrent maps so tests stay independent
        for (String fieldName : Arrays.asList(
                "FIELD_MAP", "METHOD_MAP", "METHOD_MAP2", "METHOD_MAP3", "CONSTRUCTORS", "FIELD_META_CACHE")) {
            java.lang.reflect.Field f = ReflectionUtils.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            java.util.concurrent.ConcurrentMap<?, ?> map = (java.util.concurrent.ConcurrentMap<?, ?>) f.get(null);
            map.clear();
        }
    }

    /* ---------- Tests for getClassAnnotation ---------- */
    @Test
    public void testGetClassAnnotationDirect() {
        TestAnno anno = ReflectionUtils.getClassAnnotation(BaseClass.class, TestAnno.class);
        assertNotNull(anno);
        assertEquals("class", anno.value());
    }

    @Test
    public void testGetClassAnnotationFromSuperclass() {
        TestAnno anno = ReflectionUtils.getClassAnnotation(SubClass.class, TestAnno.class);
        assertNotNull(anno);
        assertEquals("class", anno.value()); // inherited from BaseClass
    }

    @Test
    public void testGetClassAnnotationFromInterface() {
        TestAnno anno = ReflectionUtils.getClassAnnotation(SubClass.class, TestAnno.class);
        // The first found annotation is the class annotation; to test interface we need a class without class annotation
        class OnlyInterface implements TestInterface {}
        TestAnno ifaceAnno = ReflectionUtils.getClassAnnotation(OnlyInterface.class, TestAnno.class);
        assertNotNull(ifaceAnno);
        assertEquals("interface", ifaceAnno.value());
    }

    @Test
    public void testGetClassAnnotationNotFound() {
        class NoAnno {}
        assertNull(ReflectionUtils.getClassAnnotation(NoAnno.class, TestAnno.class));
    }

    /* ---------- Tests for getMethodAnnotation ---------- */
    @Test
    public void testGetMethodAnnotationDirect() throws Exception {
        Method m = BaseClass.class.getMethod("annotatedMethod");
        TestAnno anno = ReflectionUtils.getMethodAnnotation(m, TestAnno.class);
        assertNotNull(anno);
        assertEquals("method", anno.value());
    }

    @Test
    public void testGetMethodAnnotationFromInterface() throws Exception {
        Method m = SubClass.class.getMethod("interfaceMethod");
        TestAnno anno = ReflectionUtils.getMethodAnnotation(m, TestAnno.class);
        assertNotNull(anno);
        assertEquals("interfaceMethod", anno.value());
    }

    @Test
    public void testGetMethodAnnotationNotFound() throws Exception {
        Method m = BaseClass.class.getMethod("toString");
        assertNull(ReflectionUtils.getMethodAnnotation(m, TestAnno.class));
    }

    /* ---------- Tests for getMethod (by name & types) ---------- */
    @Test
    public void testGetMethodCaching() throws Exception {
        Method first = ReflectionUtils.getMethod(BaseClass.class, "annotatedMethod");
        Method second = ReflectionUtils.getMethod(BaseClass.class, "annotatedMethod");
        assertSame(first, second);
    }

    @Test
    public void testGetMethodWithParameters() throws Exception {
        Method m = ReflectionUtils.getMethod(BaseClass.class, "overloaded", String.class);
        assertNotNull(m);
        assertEquals(String.class, m.getParameterTypes()[0]);
    }

    @Test
    public void testGetMethodNotFoundReturnsNull() {
        Method m = ReflectionUtils.getMethod(BaseClass.class, "nonExistingMethod");
        assertNull(m);
    }

    /* ---------- Tests for getDeclaredFields & getDeepDeclaredFields ---------- */
    @Test
    public void testGetDeclaredFieldsCache() {
        List<Field> first = ReflectionUtils.getDeclaredFields(BaseClass.class);
        List<Field> second = ReflectionUtils.getDeclaredFields(BaseClass.class);
        assertSame(first, second);
        // Ensure static fields are filtered out (there are none in BaseClass)
        for (Field f : first) {
            assertFalse(Modifier.isStatic(f.getModifiers()));
        }
    }

    @Test
    public void testGetDeepDeclaredFieldsInheritance() {
        Collection<Field> fields = ReflectionUtils.getDeepDeclaredFields(SubClass.class);
        // Should contain fields from BaseClass and SubClass (including private ones)
        boolean foundBase = false, foundSub = false;
        for (Field f : fields) {
            if (f.getDeclaringClass() == BaseClass.class) foundBase = true;
            if (f.getDeclaringClass() == SubClass.class) foundSub = true;
        }
        assertTrue(foundBase);
        assertTrue(foundSub);
    }

    @Test
    public void testGetDeepDeclaredFieldMapDuplicateNames() {
        class Parent { private String dup; }
        class Child extends Parent { private String dup; }
        Map<String, Field> map = ReflectionUtils.getDeepDeclaredFieldMap(Child.class);
        // Two fields with same name, one should be renamed with declaring class prefix
        assertTrue(map.containsKey("dup"));
        assertTrue(map.containsKey(Child.class.getName() + ".dup"));
    }

    /* ---------- Tests for call(Object, Method, ...) ---------- */
    @Test
    public void testCallDirectSuccess() throws Exception {
        BaseClass obj = new BaseClass();
        Method m = BaseClass.class.getMethod("annotatedMethod");
        Object result = ReflectionUtils.call(obj, m);
        assertEquals("base", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallNullMethod() {
        ReflectionUtils.call(new BaseClass(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallNullBean() throws Exception {
        Method m = BaseClass.class.getMethod("annotatedMethod");
        ReflectionUtils.call(null, m);
    }

    @Test
    public void testCallMethodThrowsRuntimeException() throws Exception {
        class ThrowsException {
            public void explode() { throw new RuntimeException("boom"); }
        }
        ThrowsException obj = new ThrowsException();
        Method m = ThrowsException.class.getMethod("explode");
        try {
            ReflectionUtils.call(obj, m);
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Exception thrown inside reflectively called method"));
            assertEquals("boom", e.getCause().getMessage());
        }
    }

    /* ---------- Tests for call(Object, String, ...) ---------- */
    @Test
    public void testCallByNameSuccess() {
        BaseClass obj = new BaseClass();
        Object result = ReflectionUtils.call(obj, "annotatedMethod");
        assertEquals("base", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallByNameMethodNotFound() {
        BaseClass obj = new BaseClass();
        ReflectionUtils.call(obj, "nonExisting");
    }

    @Test
    public void testCallByNameExceptionWrapping() {
        class Bad {
            public void bad() { throw new IllegalStateException("bad"); }
        }
        Bad b = new Bad();
        try {
            ReflectionUtils.call(b, "bad");
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Exception thrown inside reflectively called method"));
            assertEquals("bad", e.getCause().getMessage());
        }
    }

    /* ---------- Tests for getMethod(Object, String, int) ---------- */
    @Test
    public void testGetMethodByArgCountCaching() {
        BaseClass obj = new BaseClass();
        Method m1 = ReflectionUtils.getMethod(obj, "overloaded", 0);
        Method m2 = ReflectionUtils.getMethod(obj, "overloaded", 0);
        assertSame(m1, m2);
        assertEquals(0, m1.getParameterTypes().length);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodByArgCountNoMatch() {
        BaseClass obj = new BaseClass();
        ReflectionUtils.getMethod(obj, "nonExisting", 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodByArgCountNullBean() {
        ReflectionUtils.getMethod(null, "any", 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodByArgCountNullName() {
        ReflectionUtils.getMethod(new BaseClass(), null, 0);
    }

    /* ---------- Tests for getConstructor ---------- */
    @Test
    public void testGetConstructorSuccess() throws Exception {
        Constructor<?> c = ReflectionUtils.getConstructor(String.class, byte[].class);
        assertNotNull(c);
        assertEquals(String.class, c.getDeclaringClass());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetConstructorNotFound() {
        ReflectionUtils.getConstructor(NoDefaultConstructor.class);
    }

    /* ---------- Tests for getNonOverloadedMethod ---------- */
    @Test
    public void testGetNonOverloadedMethodSuccess() {
        Method m = ReflectionUtils.getNonOverloadedMethod(BaseClass.class, "annotatedMethod");
        assertNotNull(m);
        assertEquals("annotatedMethod", m.getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethodNotFound() {
        ReflectionUtils.getNonOverloadedMethod(BaseClass.class, "nonExisting");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethodOverloaded() {
        // BaseClass has overloaded() with two signatures, should trigger ambiguity
        ReflectionUtils.getNonOverloadedMethod(BaseClass.class, "overloaded");
    }

    /* ---------- Tests for getClassName ---------- */
    @Test
    public void testGetClassName() {
        assertEquals("java.lang.String", ReflectionUtils.getClassName("hello"));
        assertEquals("null", ReflectionUtils.getClassName(null));
    }

    /* ---------- Tests for getClassNameFromByteCode ---------- */
    @Test
    public void testGetClassNameFromByteCode() throws Exception {
        // Load the .class bytes for java.lang.String from the runtime
        String className = "java.lang.String";
        String resourcePath = "/" + className.replace('.', '/') + ".class";
        InputStream is = ClassLoader.getSystemResourceAsStream(className.replace('.', '/') + ".class");
        assertNotNull("Unable to locate class file for java.lang.String", is);
        byte[] bytes = is.readAllBytes();
        is.close();

        String extracted = ReflectionUtils.getClassNameFromByteCode(bytes);
        assertEquals(className, extracted);
    }

    /* ---------- Tests for makeParamKey indirectly via getMethod and getConstructor ---------- */
    @Test
    public void testMakeParamKeyEffectOnCaching() throws Exception {
        // First call with no parameters
        Method m1 = ReflectionUtils.getMethod(BaseClass.class, "annotatedMethod");
        // Second call with same signature should hit cache (same instance)
        Method m2 = ReflectionUtils.getMethod(BaseClass.class, "annotatedMethod");
        assertSame(m1, m2);

        // Now a method with parameters
        Method p1 = ReflectionUtils.getMethod(BaseClass.class, "overloaded", String.class);
        Method p2 = ReflectionUtils.getMethod(BaseClass.class, "overloaded", String.class);
        assertSame(p1, p2);
    }
}
