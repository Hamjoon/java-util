package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.Assert.*;

public class ReflectionUtilsTest {

    /* ---------------------------------------------------------------------- */
    /*  Helper annotations and classes used by the tests                       */
    /* ---------------------------------------------------------------------- */

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    private @interface TestAnno {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    private @interface MethodAnno {}

    @TestAnno
    private static class AnnotatedClass {}

    private static class MethodAnnotatedClass {
        @MethodAnno
        public void annotatedMethod() {}

        public void notAnnotated() {}
    }

    private static class SimpleFieldClass {
        public int visible;
        private static int staticField;
        private int hidden;
    }

    private static class Parent {
        private int value;
    }

    private static class Child extends Parent {
        private int value;          // shadows Parent.value
    }

    private static class Overloaded {
        public void foo() {}
        public void foo(String s) {}
        protected void foo(Integer i) {}
        private void foo(Object o, Object o2) {}
    }

    private static class MultiCtor {
        public MultiCtor() {}
        protected MultiCtor(String s) {}
        MultiCtor(int i, String s) {}                 // package‑private
        private MultiCtor(int i, String s, double d) {}
    }

    private static class ExceptionThrower {
        public void explode() {
            throw new IllegalArgumentException("boom");
        }
    }

    @TestAnno
    private static class ByteCodeTestClass {}

    /* ---------------------------------------------------------------------- */
    /*  Reset caches after each test to avoid cross‑test contamination          */
    /* ---------------------------------------------------------------------- */
    @Before
    public void setUp() {
        // Use fresh, simple maps for every cache before each test
        ReflectionUtils.setMethodCache(new HashMap<>());
        ReflectionUtils.setClassFieldsCache(new HashMap<>());
        ReflectionUtils.setFieldCache(new HashMap<>());
        ReflectionUtils.setClassAnnotationCache(new HashMap<>());
        ReflectionUtils.setMethodAnnotationCache(new HashMap<>());
        ReflectionUtils.setConstructorCache(new HashMap<>());
        ReflectionUtils.setSortedConstructorsCache(new HashMap<>());
    }

    @After
    public void tearDown() {
        // Restore the default (new empty) caches – the same as setUp()
        setUp();
    }

    /* ---------------------------------------------------------------------- */
    /*  1. Basic public‑method tests                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetClassAnnotationFound() {
        TestAnno anno = ReflectionUtils.getClassAnnotation(AnnotatedClass.class, TestAnno.class);
        assertNotNull("Annotation should be found", anno);
    }

    @Test
    public void testGetClassAnnotationNullClassReturnsNull() {
        TestAnno anno = ReflectionUtils.getClassAnnotation(null, TestAnno.class);
        assertNull("Null class should return null", anno);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetClassAnnotationNullAnnoClassThrows() {
        ReflectionUtils.getClassAnnotation(AnnotatedClass.class, null);
    }

    @Test
    public void testGetMethodAnnotationFound() throws NoSuchMethodException {
        Method m = MethodAnnotatedClass.class.getMethod("annotatedMethod");
        MethodAnno anno = ReflectionUtils.getMethodAnnotation(m, MethodAnno.class);
        assertNotNull("Method annotation should be found", anno);
    }

    @Test
    public void testGetMethodAnnotationNotFound() throws NoSuchMethodException {
        Method m = MethodAnnotatedClass.class.getMethod("notAnnotated");
        MethodAnno anno = ReflectionUtils.getMethodAnnotation(m, MethodAnno.class);
        assertNull("Method annotation not present should return null", anno);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodAnnotationNullMethodThrows() {
        ReflectionUtils.getMethodAnnotation(null, MethodAnno.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodAnnotationNullAnnoClassThrows() throws NoSuchMethodException {
        Method m = MethodAnnotatedClass.class.getMethod("annotatedMethod");
        ReflectionUtils.getMethodAnnotation(m, null);
    }

    @Test
    public void testGetFieldFound() {
        Field f = ReflectionUtils.getField(Child.class, "value");
        assertNotNull("Field 'value' should be found in Child hierarchy", f);
        assertEquals("Field name must match", "value", f.getName());
    }

    @Test
    public void testGetFieldNotFound() {
        Field f = ReflectionUtils.getField(Child.class, "nonExisting");
        assertNull("Non‑existing field should return null", f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetFieldNullClassThrows() {
        ReflectionUtils.getField(null, "any");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetFieldNullNameThrows() {
        ReflectionUtils.getField(Child.class, null);
    }

    @Test
    public void testGetDeclaredFieldsDefaultFilterExcludesStatic() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(SimpleFieldClass.class);
        assertFalse("Static field must be filtered out",
                fields.stream().anyMatch(f -> Modifier.isStatic(f.getModifiers())));
        assertTrue("Public instance field must be present",
                fields.stream().anyMatch(f -> f.getName().equals("visible")));
    }

    @Test
    public void testGetAllDeclaredFieldsMapCollision() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(Child.class);
        // Two fields named "value": one from Child, one from Parent (qualified)
        assertTrue("Map must contain simple name for Child's field", map.containsKey("value"));
        assertTrue("Map must contain qualified name for Parent's field", map.containsKey(Parent.class.getName() + ".value"));
    }

    @Test
    public void testCallMethodSuccessful() throws Exception {
        ExceptionThrower et = new ExceptionThrower();
        Method m = ExceptionThrower.class.getMethod("explode");
        try {
            ReflectionUtils.call(et, m);
            fail("RuntimeException expected because method throws");
        } catch (RuntimeException re) {
            assertTrue("Cause should be IllegalArgumentException",
                    re.getCause() instanceof IllegalArgumentException);
            assertEquals("boom", re.getCause().getMessage());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallNullMethodThrows() {
        ReflectionUtils.call(new Object(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallNullInstanceThrows() throws NoSuchMethodException {
        Method m = Object.class.getMethod("toString");
        ReflectionUtils.call(null, m);
    }

    @Test
    public void testCallByNameSuccess() {
        String s = "abc";
        Object result = ReflectionUtils.call(s, "toUpperCase");
        assertEquals("ABC", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallByNameMethodNotFound() {
        ReflectionUtils.call("test", "nonExistingMethod");
    }

    @Test
    public void testGetMethodBySignature() {
        Method m = ReflectionUtils.getMethod(String.class, "substring", int.class, int.class);
        assertNotNull("Should retrieve String.substring(int,int)", m);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodNullClassThrows() {
        ReflectionUtils.getMethod(null, "any");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodNullNameThrows() {
        ReflectionUtils.getMethod(Object.class, null);
    }

    @Test
    public void testGetMethodByArgCountSelectsBestMatch() {
        Overloaded o = new Overloaded();
        Method m = ReflectionUtils.getMethod(o, "foo", 2);
        assertNotNull(m);
        assertEquals(2, m.getParameterCount());
        // The selected method should be the private one (most specific)
        assertTrue(Modifier.isPrivate(m.getModifiers()));
    }

    @Test
    public void testGetConstructorFound() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(MultiCtor.class, String.class);
        assertNotNull("Protected constructor with String should be found", ctor);
        assertTrue(Modifier.isProtected(ctor.getModifiers()));
    }

    @Test
    public void testGetConstructorNotFoundReturnsNull() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(MultiCtor.class, double.class);
        assertNull("Non‑existent constructor should return null", ctor);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetConstructorNullClassThrows() {
        ReflectionUtils.getConstructor(null);
    }

    @Test
    public void testGetAllConstructorsOrdering() {
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(MultiCtor.class);
        assertEquals(4, ctors.length);
        // Expected order: public (no‑arg), protected, package‑private, private
        assertTrue(Modifier.isPublic(ctors[0].getModifiers()));
        assertTrue(Modifier.isProtected(ctors[1].getModifiers()));
        assertFalse(Modifier.isPublic(ctors[2].getModifiers()) ||
                    Modifier.isProtected(ctors[2].getModifiers()) ||
                    Modifier.isPrivate(ctors[2].getModifiers()));
        assertTrue(Modifier.isPrivate(ctors[3].getModifiers()));
    }

    @Test
    public void testGetNonOverloadedMethodSuccess() {
        Method m = ReflectionUtils.getNonOverloadedMethod(String.class, "length");
        assertNotNull(m);
        assertEquals("length", m.getName());
        assertEquals(0, m.getParameterCount());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethodWithOverloadThrows() {
        ReflectionUtils.getNonOverloadedMethod(Overloaded.class, "foo");
    }

    @Test
    public void testGetClassName() {
        assertEquals("null", ReflectionUtils.getClassName(null));
        assertEquals(String.class.getName(), ReflectionUtils.getClassName("test"));
    }

    @Test
    public void testGetClassNameFromByteCodeValid() throws IOException {
        // Load the .class bytes of a simple inner test class
        String resource = ByteCodeTestClass.class.getSimpleName() + ".class";
        InputStream is = ByteCodeTestClass.class.getResourceAsStream(resource);
        assertNotNull("Bytecode resource must be found", is);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int read;
        while ((read = is.read(buf)) != -1) {
            baos.write(buf, 0, read);
        }
        String className = ReflectionUtils.getClassNameFromByteCode(baos.toByteArray());
        assertEquals(ByteCodeTestClass.class.getName(), className);
    }

    @Test(expected = IllegalStateException.class)
    public void testGetClassNameFromByteCodeInvalid() throws IOException {
        ReflectionUtils.getClassNameFromByteCode(new byte[]{0, 1, 2, 3});
    }

    @Test
    public void testIsJavaCompilerAvailableSystemProperty() {
        // Force the method to behave as if running on a JRE
        System.setProperty("java.util.force.jre", "true");
        try {
            assertFalse("Compiler should be reported unavailable when forced", ReflectionUtils.isJavaCompilerAvailable());
        } finally {
            System.clearProperty("java.util.force.jre");
        }
    }

    @Test
    public void testCustomMethodCacheIsUsed() throws NoSuchMethodException {
        Map<Object, Method> customMap = new HashMap<>();
        ReflectionUtils.setMethodCache(customMap);
        // Trigger a cache entry
        Method m = ReflectionUtils.getMethod(String.class, "substring", int.class);
        assertNotNull(m);
        assertFalse("Custom cache should contain at least one entry", customMap.isEmpty());
    }

    @Test
    public void testCustomFieldCacheIsUsed() {
        Map<Object, Field> customMap = new HashMap<>();
        ReflectionUtils.setFieldCache(customMap);
        Field f = ReflectionUtils.getField(Child.class, "value");
        assertNotNull(f);
        assertFalse("Custom field cache should contain at least one entry", customMap.isEmpty());
    }

    @Test
    public void testCustomClassAnnotationCacheIsUsed() {
        Map<Object, Annotation> customMap = new HashMap<>();
        ReflectionUtils.setClassAnnotationCache(customMap);
        TestAnno anno = ReflectionUtils.getClassAnnotation(AnnotatedClass.class, TestAnno.class);
        assertNotNull(anno);
        assertFalse("Custom class annotation cache should contain an entry", customMap.isEmpty());
    }

    @Test
    public void testCustomMethodAnnotationCacheIsUsed() throws NoSuchMethodException {
        Map<Object, Annotation> customMap = new HashMap<>();
        ReflectionUtils.setMethodAnnotationCache(customMap);
        Method m = MethodAnnotatedClass.class.getMethod("annotatedMethod");
        MethodAnno anno = ReflectionUtils.getMethodAnnotation(m, MethodAnno.class);
        assertNotNull(anno);
        assertFalse("Custom method annotation cache should contain an entry", customMap.isEmpty());
    }

    @Test
    public void testCustomConstructorCacheIsUsed() {
        Map<Object, Constructor<?>> customMap = new HashMap<>();
        ReflectionUtils.setConstructorCache(customMap);
        Constructor<?> ctor = ReflectionUtils.getConstructor(MultiCtor.class, String.class);
        assertNotNull(ctor);
        assertFalse("Custom constructor cache should contain an entry", customMap.isEmpty());
    }

    @Test
    public void testCustomSortedConstructorsCacheIsUsed() {
        Map<Object, Constructor<?>[]> customMap = new HashMap<>();
        ReflectionUtils.setSortedConstructorsCache(customMap);
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(MultiCtor.class);
        assertNotNull(ctors);
        assertFalse("Custom sorted constructors cache should contain an entry", customMap.isEmpty());
    }
}
