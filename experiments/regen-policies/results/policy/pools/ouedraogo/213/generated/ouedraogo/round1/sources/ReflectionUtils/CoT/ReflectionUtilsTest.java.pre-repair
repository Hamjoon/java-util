package com.cedarsoftware.util;

import org.junit.*;
import java.io.IOException;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.*;

public class ReflectionUtilsTest {

    /* -------------------------------------------------------------
     *  Helper annotations and classes used by the test suite
     * ------------------------------------------------------------- */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    private @interface TestAnno {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    private @interface MethodAnno {}

    @TestAnno
    private static class SuperClass {
        private int superPrivateField;
        public String superPublicField;

        @MethodAnno
        public void annotatedMethod() {}

        private void privateMethod() {}
        protected void protectedMethod() {}
        public void publicMethod() {}
        void packageMethod() {}
    }

    private static class SubClass extends SuperClass {
        private String subPrivateField;
        public int subPublicField;

        // field name collision with superclass
        private int superPublicField; // hides SuperClass.superPublicField

        @Override
        public void publicMethod() {} // overrides

        public void overloadedMethod(String s) {}
        public void overloadedMethod(Integer i) {}
    }

    private static class NoDefaultCtor {
        private NoDefaultCtor(String s) {}
    }

    private static class MultipleCtors {
        public MultipleCtors() {}
        protected MultipleCtors(int a) {}
        private MultipleCtors(String s, int i) {}
    }

    /* -------------------------------------------------------------
     *  Reset caches before each test to avoid cross‑test interference
     * ------------------------------------------------------------- */
    @Before
    public void resetCaches() {
        // Restore the default LRUCache implementation (the library provides it)
        // The easiest way is to replace each cache with a fresh ConcurrentHashMap.
        ReflectionUtils.setMethodCache(new ConcurrentHashMap<>());
        ReflectionUtils.setClassFieldsCache(new ConcurrentHashMap<>());
        ReflectionUtils.setFieldCache(new ConcurrentHashMap<>());
        ReflectionUtils.setClassAnnotationCache(new ConcurrentHashMap<>());
        ReflectionUtils.setMethodAnnotationCache(new ConcurrentHashMap<>());
        ReflectionUtils.setConstructorCache(new ConcurrentHashMap<>());
        ReflectionUtils.setSortedConstructorsCache(new ConcurrentHashMap<>());
    }

    /* -------------------------------------------------------------
     *  Tests for cache‑setter methods (basic sanity)
     * ------------------------------------------------------------- */
    @Test
    public void testSetMethodCache() {
        Map<Object, Method> custom = new ConcurrentHashMap<>();
        ReflectionUtils.setMethodCache(custom);
        // No exception means success – the internal AtomicReference now points at our map.
    }

    @Test
    public void testSetAllCaches() {
        ReflectionUtils.setClassFieldsCache(new ConcurrentHashMap<>());
        ReflectionUtils.setFieldCache(new ConcurrentHashMap<>());
        ReflectionUtils.setClassAnnotationCache(new ConcurrentHashMap<>());
        ReflectionUtils.setMethodAnnotationCache(new ConcurrentHashMap<>());
        ReflectionUtils.setConstructorCache(new ConcurrentHashMap<>());
        ReflectionUtils.setSortedConstructorsCache(new ConcurrentHashMap<>());
    }

    /* -------------------------------------------------------------
     *  getClassAnnotation – normal and edge cases
     * ------------------------------------------------------------- */
    @Test
    public void testGetClassAnnotationFoundOnClass() {
        TestAnno anno = ReflectionUtils.getClassAnnotation(SubClass.class, TestAnno.class);
        assertNotNull("Annotation should be found on superclass", anno);
    }

    @Test
    public void testGetClassAnnotationNotFound() {
        Deprecated anno = ReflectionUtils.getClassAnnotation(SubClass.class, Deprecated.class);
        assertNull("No @Deprecated on class hierarchy", anno);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetClassAnnotationNullAnnotationClass() {
        ReflectionUtils.getClassAnnotation(SubClass.class, null);
    }

    @Test
    public void testGetClassAnnotationNullClassReturnsNull() {
        assertNull(ReflectionUtils.getClassAnnotation(null, TestAnno.class));
    }

    /* -------------------------------------------------------------
     *  getMethodAnnotation – normal and edge cases
     * ------------------------------------------------------------- */
    @Test
    public void testGetMethodAnnotationFound() throws Exception {
        Method m = SuperClass.class.getMethod("annotatedMethod");
        MethodAnno anno = ReflectionUtils.getMethodAnnotation(m, MethodAnno.class);
        assertNotNull("Method annotation should be found", anno);
    }

    @Test
    public void testGetMethodAnnotationNotFound() throws Exception {
        Method m = SuperClass.class.getMethod("privateMethod");
        Deprecated anno = ReflectionUtils.getMethodAnnotation(m, Deprecated.class);
        assertNull("No @Deprecated on method", anno);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodAnnotationNullMethod() {
        ReflectionUtils.getMethodAnnotation(null, MethodAnno.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodAnnotationNullAnnotationClass() throws Exception {
        Method m = SuperClass.class.getMethod("annotatedMethod");
        ReflectionUtils.getMethodAnnotation(m, null);
    }

    /* -------------------------------------------------------------
     *  getField – normal and edge cases
     * ------------------------------------------------------------- */
    @Test
    public void testGetFieldExisting() {
        Field f = ReflectionUtils.getField(SubClass.class, "subPrivateField");
        assertNotNull(f);
        assertEquals("subPrivateField", f.getName());
    }

    @Test
    public void testGetFieldInherited() {
        Field f = ReflectionUtils.getField(SubClass.class, "superPrivateField");
        assertNotNull(f);
        assertEquals("superPrivateField", f.getName());
    }

    @Test
    public void testGetFieldNotExists() {
        assertNull(ReflectionUtils.getField(SubClass.class, "nonExisting"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetFieldNullClass() {
        ReflectionUtils.getField(null, "any");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetFieldNullName() {
        ReflectionUtils.getField(SubClass.class, null);
    }

    /* -------------------------------------------------------------
     *  getDeclaredFields (custom predicate) and default filter
     * ------------------------------------------------------------- */
    @Test
    public void testGetDeclaredFieldsWithPredicate() {
        Predicate<Field> stringFields = f -> f.getType().equals(String.class);
        List<Field> fields = ReflectionUtils.getDeclaredFields(SubClass.class, stringFields);
        assertEquals(1, fields.size());
        assertEquals("subPrivateField", fields.get(0).getName());
    }

    @Test
    public void testGetDeclaredFieldsDefaultFilterExcludesStatic() throws Exception {
        // Add a static field via reflection (simulated) – we cannot add at runtime,
        // so we rely on the default filter which excludes static fields.
        List<Field> fields = ReflectionUtils.getDeclaredFields(SuperClass.class);
        for (Field f : fields) {
            assertFalse("Static fields must be filtered out", Modifier.isStatic(f.getModifiers()));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetDeclaredFieldsNullClass() {
        ReflectionUtils.getDeclaredFields(null, f -> true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetDeclaredFieldsNullPredicate() {
        ReflectionUtils.getDeclaredFields(SubClass.class, null);
    }

    /* -------------------------------------------------------------
     *  getAllDeclaredFields (hierarchy) and map handling
     * ------------------------------------------------------------- */
    @Test
    public void testGetAllDeclaredFieldsIncludesSuperclass() {
        List<Field> all = ReflectionUtils.getAllDeclaredFields(SubClass.class);
        // Should contain at least the two fields declared in SubClass and the two from SuperClass
        Set<String> names = new HashSet<>();
        for (Field f : all) {
            names.add(f.getName());
        }
        assertTrue(names.contains("subPrivateField"));
        assertTrue(names.contains("superPrivateField"));
    }

    @Test
    public void testGetAllDeclaredFieldsMapHandlesCollision() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(SubClass.class);
        // The field "superPublicField" exists in both classes; map should contain
        // the subclass version under the qualified name.
        assertTrue(map.containsKey("superPublicField"));
        assertTrue(map.containsKey(SubClass.class.getName() + ".superPublicField"));
        Field subField = map.get(SubClass.class.getName() + ".superPublicField");
        assertEquals(SubClass.class, subField.getDeclaringClass());
    }

    @Test
    public void testGetAllDeclaredFieldsMapDefaultFilter() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(SubClass.class);
        // Ensure static fields are not present (none exist in our test classes, but
        // the default filter must still be applied).
        for (Field f : map.values()) {
            assertFalse(Modifier.isStatic(f.getModifiers()));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetAllDeclaredFieldsMapNullClass() {
        ReflectionUtils.getAllDeclaredFieldsMap(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetAllDeclaredFieldsMapNullPredicate() {
        ReflectionUtils.getAllDeclaredFieldsMap(SubClass.class, null);
    }

    /* -------------------------------------------------------------
     *  Deprecated deep‑field helpers – they delegate to the default
     *  implementation, so we only need a smoke test.
     * ------------------------------------------------------------- */
    @Test
    public void testGetDeepDeclaredFields() {
        Collection<Field> fields = ReflectionUtils.getDeepDeclaredFields(SubClass.class);
        assertNotNull(fields);
        assertFalse(fields.isEmpty());
    }

    @Test
    public void testGetDeepDeclaredFieldMap() {
        Map<String, Field> map = ReflectionUtils.getDeepDeclaredFieldMap(SubClass.class);
        assertNotNull(map);
        assertFalse(map.isEmpty());
    }

    @Test
    public void testGetDeclaredFieldsLegacyCollector() {
        Collection<Field> collector = new ArrayList<>();
        ReflectionUtils.getDeclaredFields(SubClass.class, collector);
        assertFalse(collector.isEmpty());
    }

    /* -------------------------------------------------------------
     *  call(Object, Method, ...) – normal execution and exception
     * ------------------------------------------------------------- */
    @Test
    public void testCallMethodSuccess() throws Exception {
        SubClass instance = new SubClass();
        Method m = SubClass.class.getDeclaredMethod("publicMethod");
        Object result = ReflectionUtils.call(instance, m);
        assertNull("void method should return null", result);
    }

    @Test
    public void testCallMethodExceptionWrapped() throws Exception {
        Method m = ExceptionThrower.class.getDeclaredMethod("throwsChecked");
        Exception ex = null;
        try {
            ReflectionUtils.call(new ExceptionThrower(), m);
        } catch (RuntimeException e) {
            ex = e;
            assertTrue(e.getCause() instanceof IOException);
        }
        assertNotNull("RuntimeException should wrap the original exception", ex);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallMethodNullMethod() {
        ReflectionUtils.call(new Object(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallMethodNullInstance() throws Exception {
        Method m = Object.class.getMethod("toString");
        ReflectionUtils.call(null, m);
    }

    /* -------------------------------------------------------------
     *  call(Object, String, ...) – method lookup by name
     * ------------------------------------------------------------- */
    @Test
    public void testCallByNameSuccess() {
        SubClass obj = new SubClass();
        Object result = ReflectionUtils.call(obj, "publicMethod");
        assertNull(result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallByNameMethodNotFound() {
        ReflectionUtils.call(new SubClass(), "nonExistingMethod");
    }

    /* -------------------------------------------------------------
     *  getMethod(Class, ...) – exact signature lookup
     * ------------------------------------------------------------- */
    @Test
    public void testGetMethodExactSignature() {
        Method m = ReflectionUtils.getMethod(SubClass.class, "overloadedMethod", String.class);
        assertNotNull(m);
        assertEquals(String.class, m.getParameterTypes()[0]);
    }

    @Test
    public void testGetMethodNonExistingReturnsNull() {
        Method m = ReflectionUtils.getMethod(SubClass.class, "bogus", int.class);
        assertNull(m);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodNullClass() {
        ReflectionUtils.getMethod(null, "any");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodNullName() {
        ReflectionUtils.getMethod(SubClass.class, null);
    }

    /* -------------------------------------------------------------
     *  getMethod(Object, String, int) – overload resolution
     * ------------------------------------------------------------- */
    @Test
    public void testGetMethodByArgCountSelectsBestMatch() {
        SubClass obj = new SubClass();
        Method m = ReflectionUtils.getMethod(obj, "overloadedMethod", 1);
        assertNotNull(m);
        // The selected method should be the one with a single parameter; both overloads have 1 param,
        // but the non‑synthetic, public one should be preferred (String version is declared first).
        assertEquals(String.class, m.getParameterTypes()[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodByArgCountNotFound() {
        ReflectionUtils.getMethod(new SubClass(), "nonExisting", 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodByArgCountNegativeCount() {
        ReflectionUtils.getMethod(new SubClass(), "publicMethod", -1);
    }

    /* -------------------------------------------------------------
     *  getConstructor – existing and missing constructors
     * ------------------------------------------------------------- */
    @Test
    public void testGetConstructorExists() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(MultipleCtors.class);
        assertNotNull(ctor);
        assertEquals(0, ctor.getParameterCount());
    }

    @Test
    public void testGetConstructorWithParams() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(MultipleCtors.class, int.class);
        assertNotNull(ctor);
        assertEquals(1, ctor.getParameterCount());
    }

    @Test
    public void testGetConstructorMissingReturnsNull() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(NoDefaultCtor.class);
        assertNull(ctor);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetConstructorNullClass() {
        ReflectionUtils.getConstructor(null);
    }

    /* -------------------------------------------------------------
     *  getAllConstructors – ordering verification
     * ------------------------------------------------------------- */
    @Test
    public void testGetAllConstructorsOrdering() {
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(MultipleCtors.class);
        assertEquals(3, ctors.length);
        // Expected order: public, protected, private
        assertTrue(Modifier.isPublic(ctors[0].getModifiers()));
        assertTrue(Modifier.isProtected(ctors[1].getModifiers()));
        assertTrue(Modifier.isPrivate(ctors[2].getModifiers()));
        // Within same visibility the one with more parameters comes first
        // (protected ctor has 1 param, private has 2)
        assertEquals(2, ctors[2].getParameterCount());
    }

    @Test
    public void testGetAllConstructorsNullClassReturnsEmptyArray() {
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(null);
        assertNotNull(ctors);
        assertEquals(0, ctors.length);
    }

    /* -------------------------------------------------------------
     *  getClassNameFromByteCode – error handling
     * ------------------------------------------------------------- */
    @Test(expected = IOException.class)
    public void testGetClassNameFromByteCodeInvalidData() throws Exception {
        byte[] bogus = new byte[10]; // too short to be a valid class file
        ReflectionUtils.getClassNameFromByteCode(bogus);
    }

    /* -------------------------------------------------------------
     *  isJavaCompilerAvailable – forced JRE simulation
     * ------------------------------------------------------------- */
    @Test
    public void testIsJavaCompilerAvailableForcedJre() {
        System.setProperty("java.util.force.jre", "true");
        try {
            assertFalse("When forced JRE flag is set, compiler must be reported unavailable",
                    ReflectionUtils.isJavaCompilerAvailable());
        } finally {
            System.clearProperty("java.util.force.jre");
        }
    }

    /* -------------------------------------------------------------
     *  getClassName – null handling
     * ------------------------------------------------------------- */
    @Test
    public void testGetClassNameHandlesNull() {
        assertEquals("null", ReflectionUtils.getClassName(null));
        assertEquals(String.class.getName(), ReflectionUtils.getClassName("test"));
    }

    /* -------------------------------------------------------------
     *  getNonOverloadedMethod – happy path and error cases
     * ------------------------------------------------------------- */
    @Test
    public void testGetNonOverloadedMethodSuccess() throws Exception {
        Method m = ReflectionUtils.getNonOverloadedMethod(SuperClass.class, "privateMethod");
        assertNotNull(m);
        assertEquals("privateMethod", m.getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethodAmbiguous() {
        // SubClass has two overloadedMethod(String) and overloadedMethod(Integer)
        ReflectionUtils.getNonOverloadedMethod(SubClass.class, "overloadedMethod");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethodNotFound() {
        ReflectionUtils.getNonOverloadedMethod(SubClass.class, "doesNotExist");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethodNullClass() {
        ReflectionUtils.getNonOverloadedMethod(null, "any");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethodBlankName() {
        ReflectionUtils.getNonOverloadedMethod(SubClass.class, "   ");
    }

    /* -------------------------------------------------------------
     *  Helper class used for the call(Object, Method, ...) exception test
     * ------------------------------------------------------------- */
    private static class ExceptionThrower {
        public void throwsChecked() throws IOException {
            throw new IOException("checked exception");
        }
    }
}
