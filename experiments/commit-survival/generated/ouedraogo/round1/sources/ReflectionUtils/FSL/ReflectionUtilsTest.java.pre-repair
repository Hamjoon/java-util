package com.cedarsoftware.util;

import static org.junit.Assert.*;

import org.junit.Test;

import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.*;
import java.util.*;

public class ReflectionUtilsTest {

    // ---------- Helper Annotations ----------
    @Retention(RetentionPolicy.RUNTIME)
    private @interface TestAnno {
        String value() default "";
    }

    // ---------- Helper Classes for Annotation Tests ----------
    @TestAnno("interface")
    private interface AnnotatedInterface { }

    @TestAnno("super")
    private static class SuperClass { }

    private static class SubClass extends SuperClass implements AnnotatedInterface { }

    private static class MethodAnnoBase {
        @TestAnno("method")
        public void annotatedMethod() { }
    }

    private static class MethodAnnoDerived extends MethodAnnoBase {
        // No annotation here, should inherit from base
        @Override
        public void annotatedMethod() { }
    }

    // ---------- Helper Classes for Field Tests ----------
    private static class FieldBase {
        public int publicBase;
        private String privateBase;
        static int staticBase;
        transient int transientBase;
        public final int finalBase = 42;
    }

    private static class FieldSub extends FieldBase {
        public double publicSub;
        private String privateSub;
        static long staticSub;
        transient long transientSub;
        // Same name as in super class
        private int publicBase;
    }

    // ---------- Helper Classes for Method Call Tests ----------
    private static class CallTarget {
        public String echo(String s) { return s; }

        private String privateEcho(String s) { return "private:" + s; }

        public void throwsException() { throw new IllegalStateException("boom"); }
    }

    // ---------- Helper Classes for Constructor Tests ----------
    private static class SimpleConstructor {
        public SimpleConstructor() { }
        public SimpleConstructor(String s, int i) { }
    }

    // ---------- Helper Classes for getNonOverloadedMethod ----------
    private static class SingleMethod {
        public void foo() { }
    }

    private static class OverloadedMethods {
        public void bar() { }
        public void bar(String s) { }
    }

    // ---------- Tests for getClassAnnotation ----------
    @Test
    public void testGetClassAnnotationDirect() {
        TestAnno anno = ReflectionUtils.getClassAnnotation(SuperClass.class, TestAnno.class);
        assertNotNull(anno);
        assertEquals("super", anno.value());
    }

    @Test
    public void testGetClassAnnotationFromInterface() {
        TestAnno anno = ReflectionUtils.getClassAnnotation(SubClass.class, TestAnno.class);
        assertNotNull(anno);
        assertEquals("interface", anno.value());
    }

    @Test
    public void testGetClassAnnotationNotFound() {
        TestAnno anno = ReflectionUtils.getClassAnnotation(String.class, TestAnno.class);
        assertNull(anno);
    }

    // ---------- Tests for getMethodAnnotation ----------
    @Test
    public void testGetMethodAnnotationInherited() throws Exception {
        Method method = MethodAnnoDerived.class.getMethod("annotatedMethod");
        TestAnno anno = ReflectionUtils.getMethodAnnotation(method, TestAnno.class);
        assertNotNull(anno);
        assertEquals("method", anno.value());
    }

    @Test
    public void testGetMethodAnnotationNotFound() throws Exception {
        Method method = SubClass.class.getMethod("toString");
        TestAnno anno = ReflectionUtils.getMethodAnnotation(method, TestAnno.class);
        assertNull(anno);
    }

    // ---------- Tests for getMethod (by name & types) ----------
    @Test
    public void testGetMethodSuccessAndCache() {
        Method m1 = ReflectionUtils.getMethod(CallTarget.class, "echo", String.class);
        Method m2 = ReflectionUtils.getMethod(CallTarget.class, "echo", String.class);
        assertNotNull(m1);
        assertEquals(m1, m2); // cached instance should be identical
    }

    @Test
    public void testGetMethodNotFoundReturnsNull() {
        Method m = ReflectionUtils.getMethod(CallTarget.class, "nonExisting", String.class);
        assertNull(m);
    }

    // ---------- Tests for getDeclaredFields ----------
    @Test
    public void testGetDeclaredFieldsCaching() {
        List<Field> first = ReflectionUtils.getDeclaredFields(FieldBase.class);
        List<Field> second = ReflectionUtils.getDeclaredFields(FieldBase.class);
        assertSame(first, second); // cache should return same list instance
        // Ensure static and transient fields are present (they are not filtered here)
        assertTrue(first.stream().anyMatch(f -> f.getName().equals("staticBase")));
    }

    // ---------- Tests for getDeepDeclaredFields ----------
    @Test
    public void testGetDeepDeclaredFieldsFiltering() {
        Collection<Field> fields = ReflectionUtils.getDeepDeclaredFields(FieldSub.class);
        // Should contain publicBase from subclass (even though same name as super)
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("publicBase") && f.getDeclaringClass() == FieldSub.class));
        // Should contain publicBase from super as separate entry
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("publicBase") && f.getDeclaringClass() == FieldBase.class));
        // static fields should be excluded
        assertFalse(fields.stream().anyMatch(f -> Modifier.isStatic(f.getModifiers())));
        // transient fields should be excluded
        assertFalse(fields.stream().anyMatch(f -> Modifier.isTransient(f.getModifiers())));
    }

    // ---------- Tests for getDeepDeclaredFieldMap ----------
    @Test
    public void testGetDeepDeclaredFieldMapHandlesDuplicates() {
        Map<String, Field> map = ReflectionUtils.getDeepDeclaredFieldMap(FieldSub.class);
        // Simple name should map to subclass field (last one added)
        Field field = map.get("publicBase");
        assertNotNull(field);
        assertEquals(FieldSub.class, field.getDeclaringClass());

        // The super field should be accessible via qualified key
        Field superField = map.get(FieldBase.class.getName() + ".publicBase");
        assertNotNull(superField);
        assertEquals(FieldBase.class, superField.getDeclaringClass());
    }

    // ---------- Tests for call(Object, Method, ...) ----------
    @Test
    public void testCallWithPublicMethod() throws Exception {
        CallTarget target = new CallTarget();
        Method m = ReflectionUtils.getMethod(CallTarget.class, "echo", String.class);
        Object result = ReflectionUtils.call(target, m, "hi");
        assertEquals("hi", result);
    }

    @Test
    public void testCallWithPrivateMethodAccessible() throws Exception {
        CallTarget target = new CallTarget();
        Method privateMethod = CallTarget.class.getDeclaredMethod("privateEcho", String.class);
        privateMethod.setAccessible(true);
        Object result = ReflectionUtils.call(target, privateMethod, "test");
        assertEquals("private:test", result);
    }

    @Test(expected = RuntimeException.class)
    public void testCallMethodThrowsExceptionWrapped() throws Exception {
        CallTarget target = new CallTarget();
        Method m = ReflectionUtils.getMethod(CallTarget.class, "throwsException");
        ReflectionUtils.call(target, m);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallNullMethod() {
        ReflectionUtils.call(new Object(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallNullBean() throws Exception {
        Method m = ReflectionUtils.getMethod(CallTarget.class, "echo", String.class);
        ReflectionUtils.call(null, m, "x");
    }

    // ---------- Tests for call(Object, String, ...) ----------
    @Test
    public void testCallByNameSuccess() {
        CallTarget target = new CallTarget();
        Object result = ReflectionUtils.call(target, "echo", "hello");
        assertEquals("hello", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallByNameBeanNull() {
        ReflectionUtils.call(null, "echo", "x");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallByNameMethodNameNull() {
        CallTarget target = new CallTarget();
        ReflectionUtils.call(target, null, "x");
    }

    // ---------- Tests for getMethod(Object, String, int) ----------
    @Test
    public void testGetMethodByArgCountSuccess() {
        CallTarget target = new CallTarget();
        Method m = ReflectionUtils.getMethod(target, "echo", 1);
        assertNotNull(m);
        assertEquals("echo", m.getName());
        assertEquals(1, m.getParameterTypes().length);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodByArgCountBeanNull() {
        ReflectionUtils.getMethod(null, "any", 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodByArgCountNameNull() {
        ReflectionUtils.getMethod(new Object(), null, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodByArgCountNotFound() {
        CallTarget target = new CallTarget();
        ReflectionUtils.getMethod(target, "nonExisting", 2);
    }

    // ---------- Tests for getConstructor ----------
    @Test
    public void testGetConstructorSuccess() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(SimpleConstructor.class);
        assertNotNull(ctor);
        assertEquals(0, ctor.getParameterTypes().length);
    }

    @Test
    public void testGetConstructorWithParamsSuccess() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(SimpleConstructor.class, String.class, int.class);
        assertNotNull(ctor);
        assertArrayEquals(new Class<?>[]{String.class, int.class}, ctor.getParameterTypes());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetConstructorNotFoundThrows() {
        ReflectionUtils.getConstructor(SimpleConstructor.class, int.class);
    }

    // ---------- Tests for getClassLoaderName ----------
    @Test
    public void testGetClassLoaderNameBootstrap() {
        String name = ReflectionUtils.getClassLoaderName(String.class);
        assertEquals("bootstrap", name);
    }

    @Test
    public void testGetClassLoaderNameCustom() {
        String name = ReflectionUtils.getClassLoaderName(ReflectionUtilsTest.class);
        assertNotNull(name);
        assertFalse(name.isEmpty());
        assertNotEquals("bootstrap", name);
    }

    // ---------- Tests for getClassName ----------
    @Test
    public void testGetClassNameNull() {
        assertEquals("null", ReflectionUtils.getClassName(null));
    }

    @Test
    public void testGetClassNameNonNull() {
        String name = ReflectionUtils.getClassName(this);
        assertEquals(this.getClass().getName(), name);
    }

    // ---------- Tests for getClassNameFromByteCode ----------
    @Test
    public void testGetClassNameFromByteCode() throws Exception {
        // Load bytecode of this test class itself
        String resourcePath = ReflectionUtilsTest.class.getName().replace('.', '/') + ".class";
        InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
        assertNotNull("Could not load class bytecode", is);
        byte[] bytes = is.readAllBytes();
        String className = ReflectionUtils.getClassNameFromByteCode(bytes);
        assertEquals(ReflectionUtilsTest.class.getName(), className);
    }

    // ---------- Tests for getNonOverloadedMethod ----------
    @Test
    public void testGetNonOverloadedMethodSuccess() {
        Method m = ReflectionUtils.getNonOverloadedMethod(SingleMethod.class, "foo");
        assertNotNull(m);
        assertEquals("foo", m.getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethodWithOverload() {
        ReflectionUtils.getNonOverloadedMethod(OverloadedMethods.class, "bar");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethodClassNull() {
        ReflectionUtils.getNonOverloadedMethod(null, "foo");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethodNameNull() {
        ReflectionUtils.getNonOverloadedMethod(SingleMethod.class, null);
    }

    // ---------- Tests for getMethodNoArgs (indirect via getNonOverloadedMethod) ----------
    @Test
    public void testGetMethodNoArgsFindsMethod() {
        Method m = ReflectionUtils.getNonOverloadedMethod(SingleMethod.class, "foo");
        assertNotNull(m);
        assertEquals(0, m.getParameterTypes().length);
    }

    // ---------- Tests for makeParamKey indirectly ----------
    @Test
    public void testMakeParamKeyUsedInGetMethodCaching() {
        // First call caches method with no parameters
        Method m1 = ReflectionUtils.getMethod(CallTarget.class, "toString");
        // Second call should retrieve same instance from cache
        Method m2 = ReflectionUtils.getMethod(CallTarget.class, "toString");
        assertSame(m1, m2);
    }
}
