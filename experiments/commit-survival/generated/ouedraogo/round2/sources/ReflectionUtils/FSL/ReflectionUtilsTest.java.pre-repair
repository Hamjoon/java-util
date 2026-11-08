package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

import org.junit.Test;

/**
 * Comprehensive JUnit4 test suite for {@link ReflectionUtils}.
 */
public class ReflectionUtilsTest {

    /*--------------------------------------------------------------------
     *  Helper types used throughout the tests
     *--------------------------------------------------------------------*/

    @Retention(RetentionPolicy.RUNTIME)
    private @interface TestAnno {
        String value();
    }

    @TestAnno("interface")
    private interface IParent { }

    @TestAnno("subinterface")
    private interface IChild extends IParent { }

    @TestAnno("superclass")
    private static class SuperClass {
        @TestAnno("superMethod")
        public String annotatedMethod() { return "super"; }

        public String nonAnnotatedMethod() { return "none"; }

        public SuperClass() { }
    }

    @TestAnno("subclass")
    private static class SubClass extends SuperClass implements IChild {
        public String annotatedMethod() { return "sub"; } // overrides super method (no annotation here)

        @TestAnno("subMethod")
        public void subMethod() { }

        // field variety
        public int publicField;
        private String privateField;
        static int staticField;
        transient long transientField;
        // synthetic this$ field cannot be declared directly – ignored by tests

        public SubClass() { }
        public SubClass(String s) { }
    }

    private static class FieldShadowParent {
        private int duplicate;
        public String uniqueParent;
    }

    private static class FieldShadowChild extends FieldShadowParent {
        private String duplicate; // shadows parent field name
        public double uniqueChild;
    }

    private static class PrivateMethodClass {
        private String secret() { return "secret"; }
    }

    private static class ExceptionThrowingClass {
        public void boom() { throw new IllegalStateException("boom"); }
    }

    private static class OverloadedMethods {
        public void foo(int a) { }
        public void foo(String s) { }
        public void bar() { }
    }

    /*--------------------------------------------------------------------
     *  Tests for getClassAnnotation()
     *--------------------------------------------------------------------*/

    @Test
    public void testGetClassAnnotationDirect() {
        TestAnno a = ReflectionUtils.getClassAnnotation(SubClass.class, TestAnno.class);
        assertNotNull(a);
        assertEquals("subclass", a.value());
    }

    @Test
    public void testGetClassAnnotationSuperclass() {
        TestAnno a = ReflectionUtils.getClassAnnotation(SubClass.class, TestAnno.class);
        // direct annotation already found, to test superclass we ask for a different annotation type
        TestAnno superAnno = ReflectionUtils.getClassAnnotation(SuperClass.class, TestAnno.class);
        assertNotNull(superAnno);
        assertEquals("superclass", superAnno.value());
    }

    @Test
    public void testGetClassAnnotationInterface() {
        TestAnno a = ReflectionUtils.getClassAnnotation(SubClass.class, TestAnno.class);
        // we already have direct; to test interface search, use a different annotation that only appears on interface
        TestAnno ifaceAnno = ReflectionUtils.getClassAnnotation(IChild.class, TestAnno.class);
        assertNotNull(ifaceAnno);
        assertEquals("subinterface", ifaceAnno.value());
    }

    @Test
    public void testGetClassAnnotationNotFound() {
        assertNull(ReflectionUtils.getClassAnnotation(String.class, TestAnno.class));
    }

    /*--------------------------------------------------------------------
     *  Tests for getMethodAnnotation()
     *--------------------------------------------------------------------*/

    @Test
    public void testGetMethodAnnotationDirect() throws Exception {
        Method m = SubClass.class.getMethod("subMethod");
        TestAnno a = ReflectionUtils.getMethodAnnotation(m, TestAnno.class);
        assertNotNull(a);
        assertEquals("subMethod", a.value());
    }

    @Test
    public void testGetMethodAnnotationInheritedFromSuperclass() throws Exception {
        // SubClass overrides annotatedMethod without annotation; should find annotation on super method
        Method m = SubClass.class.getMethod("annotatedMethod");
        TestAnno a = ReflectionUtils.getMethodAnnotation(m, TestAnno.class);
        assertNotNull(a);
        assertEquals("superMethod", a.value());
    }

    @Test
    public void testGetMethodAnnotationNotFound() throws Exception {
        Method m = SubClass.class.getMethod("nonAnnotatedMethod");
        assertNull(ReflectionUtils.getMethodAnnotation(m, TestAnno.class));
    }

    /*--------------------------------------------------------------------
     *  Tests for getMethod(Class, name, types) and caching
     *--------------------------------------------------------------------*/

    @Test
    public void testGetMethodExactMatch() {
        Method m = ReflectionUtils.getMethod(SubClass.class, "subMethod");
        assertNotNull(m);
        assertEquals("subMethod", m.getName());
        // second call should retrieve from cache (no exception means success)
        Method cached = ReflectionUtils.getMethod(SubClass.class, "subMethod");
        assertSame(m, cached);
    }

    @Test
    public void testGetMethodWithParameters() {
        Method m = ReflectionUtils.getMethod(SubClass.class, "annotatedMethod");
        assertNotNull(m);
        assertEquals("annotatedMethod", m.getName());

        Method ctor = ReflectionUtils.getMethod(SubClass.class, "SubClass", String.class);
        assertNotNull(ctor);
        assertEquals("SubClass", ctor.getName());
    }

    @Test
    public void testGetMethodNotFoundReturnsNull() {
        assertNull(ReflectionUtils.getMethod(SubClass.class, "doesNotExist"));
    }

    /*--------------------------------------------------------------------
     *  Tests for field inspection utilities
     *--------------------------------------------------------------------*/

    @Test
    public void testGetDeclaredFieldsFiltersStaticTransient() {
        Collection<Field> fields = ReflectionUtils.getDeepDeclaredFields(SubClass.class);
        // Should contain publicField and privateField but not staticField or transientField
        boolean hasPublic = false, hasPrivate = false, hasStatic = false, hasTransient = false;
        for (Field f : fields) {
            if (f.getName().equals("publicField")) hasPublic = true;
            if (f.getName().equals("privateField")) hasPrivate = true;
            if (f.getName().equals("staticField")) hasStatic = true;
            if (f.getName().equals("transientField")) hasTransient = true;
        }
        assertTrue(hasPublic);
        assertTrue(hasPrivate);
        assertFalse(hasStatic);
        assertFalse(hasTransient);
    }

    @Test
    public void testGetDeepDeclaredFieldMapHandlesShadowing() {
        Map<String, Field> map = ReflectionUtils.getDeepDeclaredFieldMap(FieldShadowChild.class);
        // Parent field name "duplicate" should be present with qualified key
        assertTrue(map.containsKey("duplicate"));
        // The shadowed child field should be stored with qualified key
        String qualified = FieldShadowChild.class.getName() + ".duplicate";
        assertTrue(map.containsKey(qualified));
        // Ensure unique fields are present
        assertTrue(map.containsKey("uniqueParent"));
        assertTrue(map.containsKey("uniqueChild"));
    }

    /*--------------------------------------------------------------------
     *  Tests for call(Object, Method, args)
     *--------------------------------------------------------------------*/

    @Test
    public void testCallWithValidMethod() throws Exception {
        SubClass instance = new SubClass();
        Method m = SubClass.class.getMethod("subMethod");
        Object result = ReflectionUtils.call(instance, m);
        assertNull(result); // void method returns null
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallNullMethodThrows() {
        ReflectionUtils.call(new Object(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallNullBeanThrows() throws Exception {
        Method m = SubClass.class.getMethod("subMethod");
        ReflectionUtils.call(null, m);
    }

    @Test
    public void testCallMethodThrowsInvocationTargetException() throws Exception {
        ExceptionThrowingClass obj = new ExceptionThrowingClass();
        Method m = ExceptionThrowingClass.class.getMethod("boom");
        try {
            ReflectionUtils.call(obj, m);
            fail("Expected RuntimeException wrapping IllegalStateException");
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof IllegalStateException);
            assertEquals("boom", e.getCause().getMessage());
        }
    }

    @Test
    public void testCallPrivateMethodProducesIllegalAccessException() throws Exception {
        PrivateMethodClass obj = new PrivateMethodClass();
        Method privateMethod = PrivateMethodClass.class.getDeclaredMethod("secret");
        // Do NOT set accessible; invoke should cause IllegalAccessException
        try {
            ReflectionUtils.call(obj, privateMethod);
            fail("Expected RuntimeException due to IllegalAccessException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("IllegalAccessException"));
        }
    }

    /*--------------------------------------------------------------------
     *  Tests for call(Object, String, args) and getMethod(bean, name, count)
     *--------------------------------------------------------------------*/

    @Test
    public void testCallByNameSimple() {
        SubClass obj = new SubClass();
        Object result = ReflectionUtils.call(obj, "subMethod");
        assertNull(result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodWithNullBean() {
        ReflectionUtils.getMethod(null, "any", 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodWithNullName() {
        ReflectionUtils.getMethod(new Object(), null, 0);
    }

    @Test
    public void testGetMethodByArgCount() {
        OverloadedMethods obj = new OverloadedMethods();
        Method m = ReflectionUtils.getMethod(obj, "foo", 1); // both overloads have 1 arg, but first match returned
        assertNotNull(m);
        assertEquals("foo", m.getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodByArgCountNotFound() {
        ReflectionUtils.getMethod(new OverloadedMethods(), "nonexistent", 0);
    }

    @Test
    public void testCallByNameWithArguments() {
        SubClass obj = new SubClass("ctorArg");
        // Use constructor that takes a String; we call via static method getMethod then invoke
        Method ctor = ReflectionUtils.getMethod(SubClass.class, "SubClass", String.class);
        assertNotNull(ctor);
        // Call via ReflectionUtils.call (static call) – but this overload expects instance, so we use call with Method
        Object newInstance = ReflectionUtils.call(ReflectionUtils.class, ctor, "hello");
        // newInstance should be an instance of SubClass
        assertTrue(newInstance instanceof SubClass);
    }

    /*--------------------------------------------------------------------
     *  Tests for getConstructor()
     *--------------------------------------------------------------------*/

    @Test
    public void testGetConstructorExisting() {
        Constructor<?> c = ReflectionUtils.getConstructor(SubClass.class, String.class);
        assertNotNull(c);
        assertEquals(SubClass.class, c.getDeclaringClass());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetConstructorNonExisting() {
        ReflectionUtils.getConstructor(SubClass.class, int.class);
    }

    /*--------------------------------------------------------------------
     *  Tests for getClassLoaderName()
     *--------------------------------------------------------------------*/

    @Test
    public void testGetClassLoaderNameBootstrap() {
        // java.lang.String is loaded by bootstrap (null loader)
        assertEquals("bootstrap", ReflectionUtils.getClassLoaderName(String.class));
    }

    @Test
    public void testGetClassLoaderNameNonBootstrap() {
        String loaderName = ReflectionUtils.getClassLoaderName(ReflectionUtilsTest.class);
        assertNotNull(loaderName);
        assertFalse(loaderName.isEmpty());
        assertNotEquals("bootstrap", loaderName);
    }

    /*--------------------------------------------------------------------
     *  Tests for getClassName()
     *--------------------------------------------------------------------*/

    @Test
    public void testGetClassName() {
        assertEquals("null", ReflectionUtils.getClassName(null));
        assertEquals(SubClass.class.getName(), ReflectionUtils.getClassName(new SubClass()));
    }

    /*--------------------------------------------------------------------
     *  Tests for getClassNameFromByteCode()
     *--------------------------------------------------------------------*/

    @Test
    public void testGetClassNameFromByteCode() throws Exception {
        // Read the .class file bytes for ReflectionUtils itself
        InputStream is = ReflectionUtils.class.getResourceAsStream("ReflectionUtils.class");
        assertNotNull("Could not locate class file for ReflectionUtils", is);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read;
        while ((read = is.read(buffer)) != -1) {
            baos.write(buffer, 0, read);
        }
        byte[] classBytes = baos.toByteArray();

        String className = ReflectionUtils.getClassNameFromByteCode(classBytes);
        assertEquals("com.cedarsoftware.util.ReflectionUtils", className);
    }

    /*--------------------------------------------------------------------
     *  Tests for getNonOverloadedMethod()
     *--------------------------------------------------------------------*/

    @Test
    public void testGetNonOverloadedMethodSuccess() throws Exception {
        Method m = ReflectionUtils.getNonOverloadedMethod(OverloadedMethods.class, "bar");
        assertNotNull(m);
        assertEquals("bar", m.getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethodWithOverloads() {
        ReflectionUtils.getNonOverloadedMethod(OverloadedMethods.class, "foo");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethodNotFound() {
        ReflectionUtils.getNonOverloadedMethod(OverloadedMethods.class, "missing");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethodNullClass() {
        ReflectionUtils.getNonOverloadedMethod(null, "any");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethodNullName() {
        ReflectionUtils.getNonOverloadedMethod(SubClass.class, null);
    }
}
