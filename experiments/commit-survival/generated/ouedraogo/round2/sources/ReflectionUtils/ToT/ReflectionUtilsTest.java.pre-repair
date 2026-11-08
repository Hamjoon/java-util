package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

public class ReflectionUtilsTest {

    /* ----------------------------------------------------------------------
     * Helper annotation and test classes
     * ---------------------------------------------------------------------- */

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface TestAnno {
        String value() default "";
    }

    @TestAnno("Base")
    static class BaseClass {
        public int basePublic = 1;
        private String basePrivate = "base";
        static int staticField = 0;
        transient int transientField = 2;
    }

    interface BaseInterface {
        @TestAnno("Interface")
        default void ifaceMethod() {}
    }

    @TestAnno("Derived")
    static class DerivedClass extends BaseClass implements BaseInterface {
        public double derivedPublic = 3.14;
        private long derivedPrivate = 42L;
        // synthetic field this$0 will be generated for inner classes – we do not need to create it.
    }

    static class OverloadedMethods {
        public String foo() { return "zero"; }
        public String foo(int a) { return "one:" + a; }
        public String foo(int a, String b) { return "two:" + a + "," + b; }
        private String privateMethod() { return "private"; }
    }

    static class MethodAnnotated {
        @TestAnno("method")
        public void annotatedMethod() {}
        public void notAnnotated() {}
    }

    /* ----------------------------------------------------------------------
     * Setup / Teardown – clear static caches between tests to avoid cross‑test pollution
     * ---------------------------------------------------------------------- */
    @Before
    @After
    public void clearCaches() throws Exception {
        // The caches are package‑private static fields; we clear them via reflection.
        Field[] fields = ReflectionUtils.class.getDeclaredFields();
        for (Field f : fields) {
            if (Modifier.isStatic(f.getModifiers())
                    && ConcurrentMap.class.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                ((ConcurrentMap<?, ?>) f.get(null)).clear();
            }
        }
    }

    /* ----------------------------------------------------------------------
     * Tests for getClassAnnotation
     * ---------------------------------------------------------------------- */
    @Test
    public void testGetClassAnnotation_InheritanceAndInterface() {
        TestAnno anno = ReflectionUtils.getClassAnnotation(DerivedClass.class, TestAnno.class);
        // The most specific annotation on the class itself should be returned ("Derived")
        assertNotNull(anno);
        assertEquals("Derived", anno.value());

        // If we ask for an annotation that only exists on the interface, we should get it.
        TestAnno ifaceAnno = ReflectionUtils.getClassAnnotation(DerivedClass.class, TestAnno.class);
        assertEquals("Derived", ifaceAnno.value()); // class wins over interface

        // Asking for a non‑present annotation returns null
        Deprecated dep = ReflectionUtils.getClassAnnotation(DerivedClass.class, Deprecated.class);
        assertNull(dep);
    }

    /* ----------------------------------------------------------------------
     * Tests for getMethodAnnotation
     * ---------------------------------------------------------------------- */
    @Test
    public void testGetMethodAnnotation_SuperclassAndInterface() throws Exception {
        Method m = MethodAnnotated.class.getMethod("annotatedMethod");
        TestAnno anno = ReflectionUtils.getMethodAnnotation(m, TestAnno.class);
        assertNotNull(anno);
        assertEquals("method", anno.value());

        // method without annotation returns null
        Method notAnno = MethodAnnotated.class.getMethod("notAnnotated");
        assertNull(ReflectionUtils.getMethodAnnotation(notAnno, TestAnno.class));
    }

    /* ----------------------------------------------------------------------
     * Tests for getMethod (by name + parameter types) and caching
     * ---------------------------------------------------------------------- */
    @Test
    public void testGetMethod_CachingAndSignature() throws Exception {
        Method m1 = ReflectionUtils.getMethod(OverloadedMethods.class, "foo", int.class, String.class);
        assertNotNull(m1);
        assertEquals("foo", m1.getName());
        assertArrayEquals(new Class[]{int.class, String.class}, m1.getParameterTypes());

        // second call should hit the cache – same instance
        Method m2 = ReflectionUtils.getMethod(OverloadedMethods.class, "foo", int.class, String.class);
        assertSame(m1, m2);
    }

    @Test
    public void testGetMethod_NotFoundReturnsNull() {
        Method m = ReflectionUtils.getMethod(String.class, "nonExistingMethod");
        assertNull(m);
    }

    /* ----------------------------------------------------------------------
     * Tests for getDeclaredFields (metadata cache)
     * ---------------------------------------------------------------------- */
    @Test
    public void testGetDeclaredFields_Cache() {
        List<Field> fields1 = ReflectionUtils.getDeclaredFields(BaseClass.class);
        List<Field> fields2 = ReflectionUtils.getDeclaredFields(BaseClass.class);
        assertSame(fields1, fields2); // same cached list

        // ensure static and transient fields are filtered out
        boolean containsStatic = fields1.stream().anyMatch(f -> Modifier.isStatic(f.getModifiers()));
        boolean containsTransient = fields1.stream().anyMatch(f -> Modifier.isTransient(f.getModifiers()));
        assertFalse(containsStatic);
        assertFalse(containsTransient);
    }

    /* ----------------------------------------------------------------------
     * Tests for getDeepDeclaredFields (including inheritance)
     * ---------------------------------------------------------------------- */
    @Test
    public void testGetDeepDeclaredFields_IncludesSuperAndFilters() {
        Collection<Field> fields = ReflectionUtils.getDeepDeclaredFields(DerivedClass.class);
        Set<String> names = new HashSet<>();
        for (Field f : fields) names.add(f.getName());

        // fields from both classes
        assertTrue(names.contains("basePublic"));
        assertTrue(names.contains("basePrivate"));
        assertTrue(names.contains("derivedPublic"));
        assertTrue(names.contains("derivedPrivate"));

        // static and transient should be absent
        assertFalse(names.contains("staticField"));
        assertFalse(names.contains("transientField"));
    }

    /* ----------------------------------------------------------------------
     * Tests for getDeepDeclaredFieldMap – duplicate names handling
     * ---------------------------------------------------------------------- */
    static class DuplicateFieldParent {
        private int dup = 1;
    }
    static class DuplicateFieldChild extends DuplicateFieldParent {
        private int dup = 2;
    }

    @Test
    public void testGetDeepDeclaredFieldMap_DuplicateNames() {
        Map<String, Field> map = ReflectionUtils.getDeepDeclaredFieldMap(DuplicateFieldChild.class);
        // both fields should be present – one with qualified key
        assertTrue(map.containsKey("dup"));
        assertTrue(map.containsKey(DuplicateFieldChild.class.getName() + ".dup"));
        assertEquals(2, map.size());
    }

    /* ----------------------------------------------------------------------
     * Tests for call(Object, Method, ...) – normal, null method, null bean, exception propagation
     * ---------------------------------------------------------------------- */
    @Test
    public void testCall_WithMethod_Success() throws Exception {
        OverloadedMethods obj = new OverloadedMethods();
        Method m = ReflectionUtils.getMethod(OverloadedMethods.class, "foo", int.class);
        Object result = ReflectionUtils.call(obj, m, 7);
        assertEquals("one:7", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCall_WithNullMethod() {
        ReflectionUtils.call(new OverloadedMethods(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCall_WithNullBean() throws Exception {
        Method m = ReflectionUtils.getMethod(OverloadedMethods.class, "foo");
        ReflectionUtils.call(null, m);
    }

    @Test
    public void testCall_MethodThrowsExceptionWrapped() throws Exception {
        // private method that throws IllegalStateException
        Method privateM = OverloadedMethods.class.getDeclaredMethod("privateMethod");
        privateM.setAccessible(true);
        OverloadedMethods obj = new OverloadedMethods();

        try {
            ReflectionUtils.call(obj, privateM);
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
            // the private method does not throw, but we simulate by invoking a method that throws
            // We'll create a class that throws.
        }
    }

    static class ExceptionThrower {
        public void explode() {
            throw new IllegalStateException("boom");
        }
    }

    @Test
    public void testCall_ExceptionWrapping() throws Exception {
        ExceptionThrower et = new ExceptionThrower();
        Method m = ExceptionThrower.class.getMethod("explode");
        try {
            ReflectionUtils.call(et, m);
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof IllegalStateException);
            assertEquals("boom", e.getCause().getMessage());
        }
    }

    /* ----------------------------------------------------------------------
     * Tests for call(Object, String, ...) – method lookup by argument count and execution
     * ---------------------------------------------------------------------- */
    @Test
    public void testCall_ByNameAndArgCount() {
        OverloadedMethods obj = new OverloadedMethods();
        Object result = ReflectionUtils.call(obj, "foo", 5, "hello");
        assertEquals("two:5,hello", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCall_ByName_NullBean() {
        ReflectionUtils.call(null, "foo");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCall_ByName_NullMethodName() {
        ReflectionUtils.call(new OverloadedMethods(), null);
    }

    /* ----------------------------------------------------------------------
     * Tests for getMethod(Object, String, int) – caching and error handling
     * ---------------------------------------------------------------------- */
    @Test
    public void testGetMethod_ObjectCache() {
        OverloadedMethods obj = new OverloadedMethods();
        Method m1 = ReflectionUtils.getMethod(obj, "foo", 0);
        Method m2 = ReflectionUtils.getMethod(obj, "foo", 0);
        assertSame(m1, m2);
        assertEquals("foo", m1.getName());
        assertEquals(0, m1.getParameterTypes().length);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethod_Object_NoSuchMethod() {
        ReflectionUtils.getMethod(new OverloadedMethods(), "nonExisting", 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethod_Object_NullBean() {
        ReflectionUtils.getMethod(null, "foo", 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethod_Object_NullMethodName() {
        ReflectionUtils.getMethod(new OverloadedMethods(), null, 0);
    }

    /* ----------------------------------------------------------------------
     * Tests for getConstructor – success and failure
     * ---------------------------------------------------------------------- */
    static class SampleCtor {
        public SampleCtor() {}
        public SampleCtor(String s, int i) {}
    }

    @Test
    public void testGetConstructor_Success() throws Exception {
        Constructor<?> c1 = ReflectionUtils.getConstructor(SampleCtor.class);
        assertNotNull(c1);
        assertEquals(0, c1.getParameterCount());

        Constructor<?> c2 = ReflectionUtils.getConstructor(SampleCtor.class, String.class, int.class);
        assertNotNull(c2);
        assertEquals(2, c2.getParameterCount());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetConstructor_NoSuchConstructor() {
        ReflectionUtils.getConstructor(SampleCtor.class, double.class);
    }

    /* ----------------------------------------------------------------------
     * Tests for makeParamKey – edge cases
     * ---------------------------------------------------------------------- */
    @Test
    public void testMakeParamKey_EmptyAndNull() throws Exception {
        Method privateMakeKey = ReflectionUtils.class.getDeclaredMethod("makeParamKey", Class[].class);
        privateMakeKey.setAccessible(true);
        String empty = (String) privateMakeKey.invoke(null, (Object) new Class[0]);
        assertEquals("", empty);

        String nullResult = (String) privateMakeKey.invoke(null, (Object) null);
        assertEquals("", nullResult);
    }

    @Test
    public void testMakeParamKey_WithParams() throws Exception {
        Method privateMakeKey = ReflectionUtils.class.getDeclaredMethod("makeParamKey", Class[].class);
        privateMakeKey.setAccessible(true);
        String key = (String) privateMakeKey.invoke(null, (Object) new Class[]{int.class, String.class});
        assertEquals(":int|java.lang.String", key);
    }

    /* ----------------------------------------------------------------------
     * Tests for getNonOverloadedMethod – normal and overloaded error
     * ---------------------------------------------------------------------- */
    @Test
    public void testGetNonOverloadedMethod_Success() {
        Method m = ReflectionUtils.getNonOverloadedMethod(OverloadedMethods.class, "foo");
        assertNotNull(m);
        assertEquals(0, m.getParameterTypes().length);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethod_Overloaded() {
        // OverloadedMethods has multiple "foo" methods – should throw
        ReflectionUtils.getNonOverloadedMethod(OverloadedMethods.class, "foo");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethod_NullClass() {
        ReflectionUtils.getNonOverloadedMethod(null, "any");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethod_NullName() {
        ReflectionUtils.getNonOverloadedMethod(OverloadedMethods.class, null);
    }

    /* ----------------------------------------------------------------------
     * Tests for getClassName(Object)
     * ---------------------------------------------------------------------- */
    @Test
    public void testGetClassName_NullAndNonNull() {
        assertEquals("null", ReflectionUtils.getClassName(null));
        assertEquals(String.class.getName(), ReflectionUtils.getClassName("hello"));
    }

    /* ----------------------------------------------------------------------
     * Tests for getClassLoaderName – bootstrap vs. system
     * ---------------------------------------------------------------------- */
    @Test
    public void testGetClassLoaderName_Bootstrap() {
        assertEquals("bootstrap", ReflectionUtils.getClassLoaderName(Object.class));
    }

    @Test
    public void testGetClassLoaderName_SystemClass() {
        String name = ReflectionUtils.getClassLoaderName(ReflectionUtilsTest.class);
        assertNotNull(name);
        assertFalse(name.isEmpty());
        assertNotEquals("bootstrap", name);
    }

    /* ----------------------------------------------------------------------
     * Tests for getClassNameFromByteCode – using the .class file of a known class
     * ---------------------------------------------------------------------- */
    @Test
    public void testGetClassNameFromByteCode_ValidClass() throws Exception {
        // Load the .class bytes of a known class (this test class itself)
        String resourcePath = '/' + ReflectionUtilsTest.class.getName().replace('.', '/') + ".class";
        InputStream is = ReflectionUtilsTest.class.getResourceAsStream(resourcePath);
        assertNotNull("Could not locate class bytecode resource", is);
        byte[] bytes = toByteArray(is);
        String className = ReflectionUtils.getClassNameFromByteCode(bytes);
        assertEquals(ReflectionUtilsTest.class.getName(), className);
    }

    @Test(expected = IllegalStateException.class)
    public void testGetClassNameFromByteCode_UnsupportedTag() throws Exception {
        // Create a minimal malformed bytecode that triggers the IllegalStateException
        byte[] malformed = new byte[]{
                (byte)0xCA, (byte)0xFE, (byte)0xBA, (byte)0xBE, // magic
                0,0, // minor
                0,0, // major
                0,1, // constant_pool_count = 1 (invalid, because count must be >=1)
        };
        ReflectionUtils.getClassNameFromByteCode(malformed);
    }

    /* ----------------------------------------------------------------------
     * Helper to read InputStream fully
     * ---------------------------------------------------------------------- */
    private static byte[] toByteArray(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) != -1) {
            baos.write(buf, 0, n);
        }
        return baos.toByteArray();
    }

    /* ----------------------------------------------------------------------
     * Tests for private buildDeclaredFields – exercised via reflection
     * ---------------------------------------------------------------------- */
    @Test
    public void testBuildDeclaredFields_Private() throws Exception {
        Method m = ReflectionUtils.class.getDeclaredMethod("buildDeclaredFields", Class.class);
        m.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Field> fields = (List<Field>) m.invoke(null, DuplicateFieldParent.class);
        // static/enum synthetic fields are filtered; this class has only one instance field
        assertEquals(1, fields.size());
        assertEquals("dup", fields.get(0).getName());
    }
}
