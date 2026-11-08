package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ReflectionUtils}.
 *
 * The tests are organised in three logical sections that mirror the
 * collaborative steps described in the task:
 *
 * 1. **Public API extraction** – a list of all public methods with
 *    signatures is provided in the Javadoc of this class.
 *
 * 2. **Basic functionality tests** – one simple test per public method
 *    that checks the “happy path”.
 *
 * 3. **Edge‑case / exception tests** – additional tests that exercise
 *    null arguments, illegal arguments, overloaded‑method ambiguities,
 *    caching behaviour and the byte‑code parser.
 *
 * All tests use only the APIs that are present in the supplied source
 * file; no external mocking frameworks are used.
 */
public class ReflectionUtilsTest {

    /* --------------------------------------------------------------------
     * 1.  Public API extraction (for documentation / verification only)
     * -------------------------------------------------------------------- */
    /**
     * List of public static methods in {@link ReflectionUtils} (extracted manually):
     *
     * <pre>
     *  &lt;T extends Annotation&gt; T getClassAnnotation(Class<?> classToCheck, Class<T> annoClass)
     *  &lt;T extends Annotation&gt; T getMethodAnnotation(Method method, Class<T> annoClass)
     *  Method getMethod(Class<?> c, String methodName, Class<?>... types)
     *  List<Field> getDeclaredFields(Class<?> c)
     *  Collection<Field> getDeepDeclaredFields(Class<?> c)
     *  void getDeclaredFields(Class<?> c, Collection<Field> fields)
     *  Map<String, Field> getDeepDeclaredFieldMap(Class<?> c)
     *  Object call(Object bean, Method method, Object... args)
     *  Object call(Object bean, String methodName, Object... args)
     *  Method getMethod(Object bean, String methodName, int argCount)
     *  Method getNonOverloadedMethod(Class<?> clazz, String methodName)
     *  Constructor<?> getConstructor(Class<?> clazz, Class<?>... parameterTypes)
     *  String getClassName(Object o)
     *  String getClassNameFromByteCode(byte[] byteCode) throws Exception
     * </pre>
     */

    /* --------------------------------------------------------------------
     * 2.  Basic functionality tests (one per method)
     * -------------------------------------------------------------------- */

    // --------------------------------------------------------------------
    // Helper types used by many tests
    // --------------------------------------------------------------------
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    private @interface SampleAnnotation {
        String value() default "sample";
    }

    @SampleAnnotation("onInterface")
    private interface AnnotatedInterface {}

    @SampleAnnotation("onSuper")
    private static class SuperClass {}

    @SampleAnnotation("onSub")
    private static class SubClass extends SuperClass implements AnnotatedInterface {}

    private static class SimpleBean {
        private String hidden = "secret";

        public String echo(String s) {
            return s;
        }

        public void explode() {
            throw new IllegalStateException("boom");
        }
    }

    // --------------------------------------------------------------------
    // Test getClassAnnotation
    // --------------------------------------------------------------------
    @Test
    public void testGetClassAnnotation_HappyPath() {
        SampleAnnotation a = ReflectionUtils.getClassAnnotation(SubClass.class, SampleAnnotation.class);
        // The first annotation found in the hierarchy is the one on SubClass itself.
        assertNotNull(a);
        assertEquals("onSub", a.value());
    }

    @Test
    public void testGetClassAnnotation_FindsOnSuperclass() {
        // Remove annotation from SubClass, rely on superclass
        class NoAnno extends SuperClass {}
        SampleAnnotation a = ReflectionUtils.getClassAnnotation(NoAnno.class, SampleAnnotation.class);
        assertNotNull(a);
        assertEquals("onSuper", a.value());
    }

    @Test
    public void testGetClassAnnotation_FindsOnInterface() {
        class ImplementsOnly implements AnnotatedInterface {}
        SampleAnnotation a = ReflectionUtils.getClassAnnotation(ImplementsOnly.class, SampleAnnotation.class);
        assertNotNull(a);
        assertEquals("onInterface", a.value());
    }

    @Test
    public void testGetClassAnnotation_NoAnnotationReturnsNull() {
        class Plain {}
        assertNull(ReflectionUtils.getClassAnnotation(Plain.class, SampleAnnotation.class));
    }

    @Test
    public void testGetClassAnnotation_NullClassReturnsNull() {
        assertNull(ReflectionUtils.getClassAnnotation(null, SampleAnnotation.class));
    }

    // --------------------------------------------------------------------
    // Test getMethodAnnotation
    // --------------------------------------------------------------------
    @SampleAnnotation("methodAnno")
    private static class MethodAnnoClass {
        @SampleAnnotation("onMethod")
        public void annotatedMethod() {}
        public void plainMethod() {}
    }

    @Test
    public void testGetMethodAnnotation_FoundOnMethod() throws Exception {
        Method m = MethodAnnoClass.class.getMethod("annotatedMethod");
        SampleAnnotation a = ReflectionUtils.getMethodAnnotation(m, SampleAnnotation.class);
        assertNotNull(a);
        assertEquals("onMethod", a.value());
    }

    @Test
    public void testGetMethodAnnotation_NotFoundReturnsNull() throws Exception {
        Method m = MethodAnnoClass.class.getMethod("plainMethod");
        assertNull(ReflectionUtils.getMethodAnnotation(m, SampleAnnotation.class));
    }

    // --------------------------------------------------------------------
    // Test getMethod(Class, String, Class...)
    // --------------------------------------------------------------------
    @Test
    public void testGetMethod_HappyPath() throws Exception {
        Method m = ReflectionUtils.getMethod(String.class, "substring", int.class, int.class);
        assertNotNull(m);
        assertEquals("substring", m.getName());
    }

    @Test
    public void testGetMethod_NotFoundReturnsNull() {
        Method m = ReflectionUtils.getMethod(String.class, "nonExistingMethod", int.class);
        assertNull(m);
    }

    @Test
    public void testGetMethod_WithNullTypesArray() {
        // No‑arg method – pass null for the varargs parameter
        Method m = ReflectionUtils.getMethod(Object.class, "toString", (Class<?>[]) null);
        assertNotNull(m);
        assertEquals("toString", m.getName());
    }

    // --------------------------------------------------------------------
    // Test getDeclaredFields(Class)
    // --------------------------------------------------------------------
    private static class FieldTestClass {
        public int pub;
        private String prv;
        static long staticField;
        transient boolean trans;
    }

    @Test
    public void testGetDeclaredFields_CachesResult() {
        List<Field> first = ReflectionUtils.getDeclaredFields(FieldTestClass.class);
        List<Field> second = ReflectionUtils.getDeclaredFields(FieldTestClass.class);
        // The same List instance should be returned because of computeIfAbsent cache
        assertSame(first, second);
        // Verify that both public and private (non‑static, non‑transient) fields are present
        Set<String> names = new HashSet<>();
        for (Field f : first) names.add(f.getName());
        assertTrue(names.contains("pub"));
        assertTrue(names.contains("prv"));
        assertFalse(names.contains("staticField"));
        assertFalse(names.contains("trans"));
    }

    // --------------------------------------------------------------------
    // Test getDeepDeclaredFields(Class)
    // --------------------------------------------------------------------
    private static class BaseA {
        private int a;
        public String s;
    }

    private static class DerivedA extends BaseA {
        private double d;
        static int ignoredStatic;
    }

    @Test
    public void testGetDeepDeclaredFields_IncludesSuperAndFilters() {
        Collection<Field> fields = ReflectionUtils.getDeepDeclaredFields(DerivedA.class);
        Set<String> names = new HashSet<>();
        for (Field f : fields) names.add(f.getName());
        assertTrue(names.containsAll(Arrays.asList("a", "s", "d")));
        assertFalse(names.contains("ignoredStatic"));
    }

    @Test
    public void testGetDeepDeclaredFields_CacheWorks() {
        Collection<Field> first = ReflectionUtils.getDeepDeclaredFields(DerivedA.class);
        Collection<Field> second = ReflectionUtils.getDeepDeclaredFields(DerivedA.class);
        // The same collection instance is cached
        assertSame(first, second);
    }

    // --------------------------------------------------------------------
    // Test getDeclaredFields(Class, Collection)
    // --------------------------------------------------------------------
    @Test
    public void testGetDeclaredFields_CollectionPopulation() {
        List<Field> list = new ArrayList<>();
        ReflectionUtils.getDeclaredFields(FieldTestClass.class, list);
        Set<String> names = new HashSet<>();
        for (Field f : list) names.add(f.getName());
        assertTrue(names.containsAll(Arrays.asList("pub", "prv")));
        assertFalse(names.contains("staticField"));
        assertFalse(names.contains("trans"));
    }

    // --------------------------------------------------------------------
    // Test getDeepDeclaredFieldMap(Class)
    // --------------------------------------------------------------------
    private static class DuplicateNameParent {
        private int dup;
    }

    private static class DuplicateNameChild extends DuplicateNameParent {
        private String dup; // same simple name, different type
    }

    @Test
    public void testGetDeepDeclaredFieldMap_DuplicateNamesHandled() {
        Map<String, Field> map = ReflectionUtils.getDeepDeclaredFieldMap(DuplicateNameChild.class);
        // The child field should mask the parent field under the simple name,
        // and the parent field should be accessible via the qualified key.
        assertTrue(map.containsKey("dup"));
        assertTrue(map.containsKey(DuplicateNameParent.class.getName() + ".dup"));
        assertEquals(String.class, map.get("dup").getType());
        assertEquals(int.class, map.get(DuplicateNameParent.class.getName() + ".dup").getType());
    }

    // --------------------------------------------------------------------
    // Test call(Object, Method, Object...)
    // --------------------------------------------------------------------
    @Test
    public void testCall_WithMethod_SuccessfulInvocation() throws Exception {
        SimpleBean bean = new SimpleBean();
        Method m = SimpleBean.class.getMethod("echo", String.class);
        Object result = ReflectionUtils.call(bean, m, "hello");
        assertEquals("hello", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCall_WithNullMethod_Throws() {
        ReflectionUtils.call(new SimpleBean(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCall_WithNullBean_Throws() throws Exception {
        Method m = SimpleBean.class.getMethod("echo", String.class);
        ReflectionUtils.call(null, m, "hi");
    }

    @Test
    public void testCall_MethodThrowsWrappedRuntimeException() throws Exception {
        SimpleBean bean = new SimpleBean();
        Method m = SimpleBean.class.getMethod("explode");
        try {
            ReflectionUtils.call(bean, m);
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof IllegalStateException);
            assertEquals("boom", e.getCause().getMessage());
        }
    }

    // --------------------------------------------------------------------
    // Test call(Object, String, Object...)
    // --------------------------------------------------------------------
    @Test
    public void testCall_ByName_SuccessfulInvocation() {
        SimpleBean bean = new SimpleBean();
        Object result = ReflectionUtils.call(bean, "echo", "world");
        assertEquals("world", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCall_ByName_NullBean() {
        ReflectionUtils.call(null, "echo", "x");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCall_ByName_NullMethodName() {
        SimpleBean bean = new SimpleBean();
        ReflectionUtils.call(bean, null, "x");
    }

    // --------------------------------------------------------------------
    // Test getMethod(Object, String, int)
    // --------------------------------------------------------------------
    @Test
    public void testGetMethod_Object_HappyPath() {
        SimpleBean bean = new SimpleBean();
        Method m = ReflectionUtils.getMethod(bean, "echo", 1);
        assertNotNull(m);
        assertEquals("echo", m.getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethod_Object_NullBean() {
        ReflectionUtils.getMethod(null, "any", 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethod_Object_NullMethodName() {
        ReflectionUtils.getMethod(new SimpleBean(), null, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethod_Object_MethodNotFound() {
        ReflectionUtils.getMethod(new SimpleBean(), "nonExisting", 0);
    }

    // --------------------------------------------------------------------
    // Test getNonOverloadedMethod
    // --------------------------------------------------------------------
    private static class OverloadClass {
        public void foo() {}
        public void foo(int x) {}
    }

    private static class SingleMethodClass {
        public String bar() { return "bar"; }
    }

    @Test
    public void testGetNonOverloadedMethod_HappyPath() {
        Method m = ReflectionUtils.getNonOverloadedMethod(SingleMethodClass.class, "bar");
        assertNotNull(m);
        assertEquals("bar", m.getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethod_NullClass() {
        ReflectionUtils.getNonOverloadedMethod(null, "any");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethod_NullMethodName() {
        ReflectionUtils.getNonOverloadedMethod(SingleMethodClass.class, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethod_OverloadedThrows() {
        ReflectionUtils.getNonOverloadedMethod(OverloadClass.class, "foo");
    }

    // --------------------------------------------------------------------
    // Test getConstructor
    // --------------------------------------------------------------------
    private static class ConstructorTest {
        public ConstructorTest() {}
        public ConstructorTest(String s, int i) {}
    }

    @Test
    public void testGetConstructor_HappyPath_NoArgs() throws Exception {
        Constructor<?> c = ReflectionUtils.getConstructor(ConstructorTest.class);
        assertNotNull(c);
        assertEquals(0, c.getParameterTypes().length);
    }

    @Test
    public void testGetConstructor_HappyPath_WithArgs() throws Exception {
        Constructor<?> c = ReflectionUtils.getConstructor(ConstructorTest.class, String.class, int.class);
        assertNotNull(c);
        assertArrayEquals(new Class<?>[]{String.class, int.class}, c.getParameterTypes());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetConstructor_NoSuchConstructorThrows() {
        ReflectionUtils.getConstructor(ConstructorTest.class, double.class);
    }

    // --------------------------------------------------------------------
    // Test getClassName
    // --------------------------------------------------------------------
    @Test
    public void testGetClassName_NullReturnsStringNull() {
        assertEquals("null", ReflectionUtils.getClassName(null));
    }

    @Test
    public void testGetClassName_NonNullReturnsClassName() {
        String name = ReflectionUtils.getClassName(this);
        assertEquals(this.getClass().getName(), name);
    }

    // --------------------------------------------------------------------
    // Test getClassNameFromByteCode
    // --------------------------------------------------------------------
    /**
     * Helper class whose compiled .class file we will read as a byte array.
     */
    private static class DummyBytecodeClass {}

    @Test
    public void testGetClassNameFromByteCode_ValidClass() throws Exception {
        // Load the .class bytes of the inner class defined above.
        String resourceName = DummyBytecodeClass.class.getName().replace('.', '/') + ".class";
        InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName);
        assertNotNull("Unable to locate class bytecode for DummyBytecodeClass", is);
        byte[] bytes = readAllBytes(is);
        String className = ReflectionUtils.getClassNameFromByteCode(bytes);
        assertEquals(DummyBytecodeClass.class.getName(), className);
    }

    @Test(expected = Exception.class)
    public void testGetClassNameFromByteCode_MalformedInputThrows() throws Exception {
        // An empty byte array does not represent a valid class file.
        ReflectionUtils.getClassNameFromByteCode(new byte[0]);
    }

    // --------------------------------------------------------------------
    // Utility method to read an InputStream fully (Java 8 compatible)
    // --------------------------------------------------------------------
    private static byte[] readAllBytes(InputStream is) throws Exception {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[4096];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }

}
