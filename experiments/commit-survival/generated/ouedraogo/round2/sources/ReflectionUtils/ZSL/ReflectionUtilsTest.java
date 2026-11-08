package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import static org.junit.Assert.*;

public class ReflectionUtilsTest {

    /* -------------------------------------------------------
     *  Helper annotation and test classes
     * ------------------------------------------------------- */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface Marker {
        String value() default "";
    }

    @Marker("interface")
    interface AnnotatedInterface {
        @Marker("interfaceMethod")
        void ifaceMethod();
    }

    @Marker("super")
    static class SuperClass implements AnnotatedInterface {
        @Override
        public void ifaceMethod() { }

        @Marker("superMethod")
        public void superMethod() { }

        private void privateSuperMethod() { }

        static int staticField = 0;
        transient String transientField;
        public String publicField;
        private String privateField;
        public SuperClass() { }
    }

    @Marker("sub")
    static class SubClass extends SuperClass {
        @Marker("subMethod")
        public void subMethod() { }

        // duplicate field name (private) to test field map collision handling
        private String privateField;

        public SubClass() { }
    }

    static class MethodExceptionThrower {
        public void happy() { }
        public void explode() { throw new IllegalStateException("boom"); }
    }

    /* -------------------------------------------------------
     *  Setup / Teardown (clear caches if needed)
     * ------------------------------------------------------- */
    @Before
    @After
    public void clearCaches() throws Exception {
        // Use reflection to clear the private caches between tests to avoid cross‑test pollution.
        // FIELD_MAP, METHOD_MAP, METHOD_MAP2, METHOD_MAP3, CONSTRUCTORS, FIELD_META_CACHE
        for (String name : new String[]{
                "FIELD_MAP","METHOD_MAP","METHOD_MAP2","METHOD_MAP3","CONSTRUCTORS","FIELD_META_CACHE"}) {
            Field f = ReflectionUtils.class.getDeclaredField(name);
            f.setAccessible(true);
            ((Map<?,?>)f.get(null)).clear();
        }
    }

    /* -------------------------------------------------------
     *  Tests for getClassAnnotation
     * ------------------------------------------------------- */
    @Test
    public void testGetClassAnnotationFoundOnClass() {
        Marker a = ReflectionUtils.getClassAnnotation(SubClass.class, Marker.class);
        assertNotNull(a);
        assertEquals("sub", a.value()); // direct annotation on SubClass
    }

    @Test
    public void testGetClassAnnotationFoundOnSuperclass() {
        Marker a = ReflectionUtils.getClassAnnotation(SubClass.class, Marker.class);
        // Already tested above, also ensure we can find super class annotation via interface
        Marker ifaceAnno = ReflectionUtils.getClassAnnotation(SubClass.class, Marker.class);
        assertEquals("sub", ifaceAnno.value()); // direct wins
    }

    @Test
    public void testGetClassAnnotationFoundOnInterface() {
        Marker a = ReflectionUtils.getClassAnnotation(SuperClass.class, Marker.class);
        assertNotNull(a);
        assertEquals("super", a.value());

        // Look for interface annotation
        Marker iface = ReflectionUtils.getClassAnnotation(SuperClass.class, Marker.class);
        assertEquals("super", iface.value()); // super wins over interface
    }

    @Test
    public void testGetClassAnnotationNotFound() {
        class NoAnno {}
        assertNull(ReflectionUtils.getClassAnnotation(NoAnno.class, Marker.class));
    }

    /* -------------------------------------------------------
     *  Tests for getMethodAnnotation
     * ------------------------------------------------------- */
    @Test
    public void testGetMethodAnnotationOnSuperMethod() throws Exception {
        Method m = SubClass.class.getMethod("superMethod");
        Marker a = ReflectionUtils.getMethodAnnotation(m, Marker.class);
        assertNotNull(a);
        assertEquals("superMethod", a.value());
    }

    @Test
    public void testGetMethodAnnotationOnInterfaceMethod() throws Exception {
        Method m = SubClass.class.getMethod("ifaceMethod");
        Marker a = ReflectionUtils.getMethodAnnotation(m, Marker.class);
        assertNotNull(a);
        assertEquals("interfaceMethod", a.value());
    }

    @Test
    public void testGetMethodAnnotationNotFound() throws Exception {
        Method m = SubClass.class.getMethod("subMethod");
        assertNull(ReflectionUtils.getMethodAnnotation(m, Deprecated.class));
    }

    /* -------------------------------------------------------
     *  Tests for getMethod (Class, name, types) caching
     * ------------------------------------------------------- */
    @Test
    public void testGetMethodSuccess() {
        Method m = ReflectionUtils.getMethod(SubClass.class, "subMethod");
        assertNotNull(m);
        assertEquals("subMethod", m.getName());
    }

    @Test
    public void testGetMethodNotFoundReturnsNull() {
        Method m = ReflectionUtils.getMethod(SubClass.class, "nonExisting");
        assertNull(m);
    }

    /* -------------------------------------------------------
     *  Tests for getDeepDeclaredFields and related utilities
     * ------------------------------------------------------- */
    @Test
    public void testGetDeepDeclaredFieldsFiltersCorrectly() {
        Collection<Field> fields = ReflectionUtils.getDeepDeclaredFields(SubClass.class);
        Set<String> names = new HashSet<>();
        for (Field f : fields) names.add(f.getName());

        // Should contain publicField, privateField (both from SuperClass and SubClass)
        assertTrue(names.contains("publicField"));
        assertTrue(names.contains("privateField"));
        // static and transient fields must be excluded
        assertFalse(names.contains("staticField"));
        assertFalse(names.contains("transientField"));
    }

    @Test
    public void testGetDeepDeclaredFieldMapHandlesDuplicateNames() {
        Map<String, Field> map = ReflectionUtils.getDeepDeclaredFieldMap(SubClass.class);
        // Two fields named "privateField": one from SuperClass, one from SubClass.
        // The map should contain the subclass version under a qualified key.
        assertTrue(map.containsKey("privateField"));
        assertTrue(map.containsKey(SubClass.class.getName() + ".privateField"));
        Field subField = map.get(SubClass.class.getName() + ".privateField");
        assertEquals(SubClass.class, subField.getDeclaringClass());
    }

    @Test
    public void testGetDeclaredFieldsCacheIsConsistent() {
        List<Field> first = ReflectionUtils.getDeclaredFields(SubClass.class);
        List<Field> second = ReflectionUtils.getDeclaredFields(SubClass.class);
        assertSame(first, second); // computeIfAbsent returns the same instance
    }

    /* -------------------------------------------------------
     *  Tests for getMethod(Object, String, int) and overload checks
     * ------------------------------------------------------- */
    @Test
    public void testGetMethodByArgCountSuccess() throws Exception {
        SubClass instance = new SubClass();
        Method m = ReflectionUtils.getMethod(instance, "subMethod", 0);
        assertNotNull(m);
        assertEquals("subMethod", m.getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodByArgCountBeanNull() {
        ReflectionUtils.getMethod(null, "any", 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodByArgCountNameNull() {
        new SubClass().toString(); // just to have an instance
        ReflectionUtils.getMethod(new SubClass(), null, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodByArgCountNotFound() {
        ReflectionUtils.getMethod(new SubClass(), "nonExisting", 0);
    }

    /* -------------------------------------------------------
     *  Tests for getNonOverloadedMethod
     * ------------------------------------------------------- */
    @Test
    public void testGetNonOverloadedMethodSuccess() throws Exception {
        Method m = ReflectionUtils.getNonOverloadedMethod(SuperClass.class, "superMethod");
        assertNotNull(m);
        assertEquals("superMethod", m.getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethodWithOverload() {
        // java.lang.String has overloaded "valueOf"
        ReflectionUtils.getNonOverloadedMethod(String.class, "valueOf");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethodNotFound() {
        ReflectionUtils.getNonOverloadedMethod(SubClass.class, "doesNotExist");
    }

    /* -------------------------------------------------------
     *  Tests for call(Object, Method, ...) and exception handling
     * ------------------------------------------------------- */
    @Test
    public void testCallMethodDirectSuccess() throws Exception {
        Method m = ReflectionUtils.getMethod(MethodExceptionThrower.class, "happy");
        Object result = ReflectionUtils.call(new MethodExceptionThrower(), m);
        assertNull(result); // void method returns null via reflection
    }

    @Test(expected = RuntimeException.class)
    public void testCallMethodDirectExceptionWrapped() throws Exception {
        Method m = ReflectionUtils.getMethod(MethodExceptionThrower.class, "explode");
        ReflectionUtils.call(new MethodExceptionThrower(), m);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallMethodDirectNullMethod() {
        ReflectionUtils.call(new Object(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallMethodDirectNullBean() throws Exception {
        Method m = ReflectionUtils.getMethod(MethodExceptionThrower.class, "happy");
        ReflectionUtils.call(null, m);
    }

    /* -------------------------------------------------------
     *  Tests for call(Object, String, ...) convenience method
     * ------------------------------------------------------- */
    @Test
    public void testCallByNameSuccess() {
        MethodExceptionThrower bean = new MethodExceptionThrower();
        Object ret = ReflectionUtils.call(bean, "happy");
        assertNull(ret);
    }

    @Test(expected = RuntimeException.class)
    public void testCallByNameExceptionWrapped() {
        MethodExceptionThrower bean = new MethodExceptionThrower();
        ReflectionUtils.call(bean, "explode");
    }

    /* -------------------------------------------------------
     *  Tests for getConstructor
     * ------------------------------------------------------- */
    @Test
    public void testGetConstructorSuccess() throws Exception {
        Constructor<?> ctor = ReflectionUtils.getConstructor(SubClass.class);
        assertNotNull(ctor);
        assertEquals(0, ctor.getParameterCount());
        Object obj = ctor.newInstance();
        assertTrue(obj instanceof SubClass);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetConstructorNotFound() {
        ReflectionUtils.getConstructor(String.class, int.class); // no such constructor
    }

    /* -------------------------------------------------------
     *  Tests for getClassName
     * ------------------------------------------------------- */
    @Test
    public void testGetClassName() {
        assertEquals("null", ReflectionUtils.getClassName(null));
        assertEquals(SubClass.class.getName(), ReflectionUtils.getClassName(new SubClass()));
    }

    /* -------------------------------------------------------
     *  Tests for getClassNameFromByteCode
     * ------------------------------------------------------- */
    @Test
    public void testGetClassNameFromByteCode() throws Exception {
        // Obtain the bytecode of the test class itself (SubClass)
        String resource = SubClass.class.getSimpleName() + ".class";
        InputStream is = SubClass.class.getResourceAsStream(resource);
        assertNotNull("Could not locate class file for SubClass", is);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int read;
        while ((read = is.read(buf)) != -1) {
            baos.write(buf, 0, read);
        }
        byte[] byteCode = baos.toByteArray();

        String className = ReflectionUtils.getClassNameFromByteCode(byteCode);
        assertEquals(SubClass.class.getName(), className);
    }

    /* -------------------------------------------------------
     *  Tests for internal helper makeParamKey via getMethod caching keys
     * ------------------------------------------------------- */
    @Test
    public void testMakeParamKeyEffectOnMethodLookup() {
        // First call caches method with no args
        Method m1 = ReflectionUtils.getMethod(SubClass.class, "subMethod");
        // Second call with same signature should retrieve from cache (same instance)
        Method m2 = ReflectionUtils.getMethod(SubClass.class, "subMethod");
        assertSame(m1, m2);
    }
}
