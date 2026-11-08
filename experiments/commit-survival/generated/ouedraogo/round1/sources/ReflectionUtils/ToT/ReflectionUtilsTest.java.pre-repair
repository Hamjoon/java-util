package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import static org.junit.Assert.*;

public class ReflectionUtilsTest {

    /* -------------------------------------------------------------
     * Helper annotations and classes used throughout the tests
     * ------------------------------------------------------------- */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    private @interface TestAnno {
        String value();
    }

    @TestAnno("Base")
    private static class BaseClass {
        @TestAnno("baseMethod")
        public void annotatedMethod() { }
    }

    @TestAnno("Derived")
    private static class DerivedClass extends BaseClass implements InterfaceA, InterfaceB {
        private static final int STATIC_FIELD = 42;
        private transient String transientField;
        private String normalField;
        private final String finalField = "final";

        public DerivedClass() { }

        public DerivedClass(String s) { }

        public void methodNoArgs() { }

        public void overloaded(int a) { }

        public void overloaded(String a) { }

        @Override
        public void ifaceMethod() { }

        public void methodWithException() throws Exception {
            throw new Exception("boom");
        }
    }

    private interface InterfaceA {
        @TestAnno("ifaceMethod")
        void ifaceMethod();
    }

    private interface InterfaceB { }

    private static class SampleClass {
        public int add(int a, int b) { return a + b; }
    }

    /* -------------------------------------------------------------
     * Test fixtures
     * ------------------------------------------------------------- */
    private DerivedClass derivedInstance;
    private Method methodNoArgs;
    private Method overloadedInt;
    private Method overloadedString;

    @Before
    public void setUp() throws Exception {
        derivedInstance = new DerivedClass();
        methodNoArgs = DerivedClass.class.getMethod("methodNoArgs");
        overloadedInt = DerivedClass.class.getMethod("overloaded", int.class);
        overloadedString = DerivedClass.class.getMethod("overloaded", String.class);
    }

    /* -------------------------------------------------------------
     * getClassAnnotation()
     * ------------------------------------------------------------- */
    @Test
    public void testGetClassAnnotation_InheritanceAndInterfaces() {
        // Direct annotation on class
        TestAnno anno = ReflectionUtils.getClassAnnotation(DerivedClass.class, TestAnno.class);
        assertNotNull(anno);
        assertEquals("Derived", anno.value());

        // Annotation on superclass
        anno = ReflectionUtils.getClassAnnotation(BaseClass.class, TestAnno.class);
        assertNotNull(anno);
        assertEquals("Base", anno.value());

        // Annotation on interface method (should not be found for class level)
        anno = ReflectionUtils.getClassAnnotation(InterfaceA.class, TestAnno.class);
        assertNotNull(anno);
        assertEquals("ifaceMethod", anno.value());
    }

    /* -------------------------------------------------------------
     * getMethodAnnotation()
     * ------------------------------------------------------------- */
    @Test
    public void testGetMethodAnnotation_SuperclassMethod() throws Exception {
        Method m = DerivedClass.class.getMethod("annotatedMethod");
        TestAnno anno = ReflectionUtils.getMethodAnnotation(m, TestAnno.class);
        assertNotNull(anno);
        assertEquals("baseMethod", anno.value());
    }

    /* -------------------------------------------------------------
     * getMethod(Class, name, types) – caching and null handling
     * ------------------------------------------------------------- */
    @Test
    public void testGetMethod_CachingAndNull() {
        Method first = ReflectionUtils.getMethod(DerivedClass.class, "methodNoArgs");
        Method second = ReflectionUtils.getMethod(DerivedClass.class, "methodNoArgs");
        assertSame(first, second);

        // non‑existent method returns null
        assertNull(ReflectionUtils.getMethod(DerivedClass.class, "doesNotExist"));
    }

    /* -------------------------------------------------------------
     * getDeclaredFields(Class) – cache building
     * ------------------------------------------------------------- */
    @Test
    public void testGetDeclaredFields_Cache() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(DerivedClass.class);
        // Verify that static and transient fields are filtered out
        boolean hasStatic = fields.stream().anyMatch(f -> Modifier.isStatic(f.getModifiers()));
        boolean hasTransient = fields.stream().anyMatch(f -> Modifier.isTransient(f.getModifiers()));
        assertFalse(hasStatic);
        assertFalse(hasTransient);
        // Normal field should be present
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("normalField")));
    }

    /* -------------------------------------------------------------
     * getDeepDeclaredFields() – inheritance handling
     * ------------------------------------------------------------- */
    @Test
    public void testGetDeepDeclaredFields_IncludesSuperclassFields() {
        Collection<Field> fields = ReflectionUtils.getDeepDeclaredFields(DerivedClass.class);
        // field from BaseClass (inherited) should be present
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("annotatedMethod")));
        // own field
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("normalField")));
    }

    /* -------------------------------------------------------------
     * getDeepDeclaredFieldMap() – duplicate name handling
     * ------------------------------------------------------------- */
    @Test
    public void testGetDeepDeclaredFieldMap_DuplicateNames() throws Exception {
        class Parent {
            private String dup;
        }
        class Child extends Parent {
            private String dup;
        }
        Map<String, Field> map = ReflectionUtils.getDeepDeclaredFieldMap(Child.class);
        // Both fields should be present, one with qualified key
        assertTrue(map.containsKey("dup"));
        assertTrue(map.containsKey(Parent.class.getName() + ".dup"));
        assertEquals(2, map.size());
    }

    /* -------------------------------------------------------------
     * call(Object, Method, ...) – normal, null method, null bean
     * ------------------------------------------------------------- */
    @Test
    public void testCall_WithMethod_Success() throws Exception {
        Object result = ReflectionUtils.call(derivedInstance, methodNoArgs);
        assertNull(result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCall_WithNullMethod() {
        ReflectionUtils.call(derivedInstance, (Method) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCall_WithNullBean() {
        ReflectionUtils.call(null, methodNoArgs);
    }

    @Test
    public void testCall_MethodThrowsExceptionWrapped() {
        try {
            Method m = DerivedClass.class.getMethod("methodWithException");
            ReflectionUtils.call(derivedInstance, m);
            fail("Expected RuntimeException");
        } catch (RuntimeException re) {
            assertTrue(re.getCause() instanceof Exception);
            assertEquals("boom", re.getCause().getMessage());
        }
    }

    /* -------------------------------------------------------------
     * call(Object, String, ...) – cached lookup and error handling
     * ------------------------------------------------------------- */
    @Test
    public void testCall_ByName_Success() {
        Object result = ReflectionUtils.call(new SampleClass(), "add", 2, 3);
        assertEquals(5, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCall_ByName_NullBean() {
        ReflectionUtils.call(null, "add", 1, 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCall_ByName_NullMethodName() {
        ReflectionUtils.call(new SampleClass(), null, 1, 2);
    }

    /* -------------------------------------------------------------
     * getMethod(Object, String, int) – argument count based lookup
     * ------------------------------------------------------------- */
    @Test
    public void testGetMethod_ByArgCount() {
        Method m = ReflectionUtils.getMethod(derivedInstance, "overloaded", 1);
        // Both overloaded methods have argCount 1; the first matching public method is returned
        assertNotNull(m);
        assertTrue(m.getName().equals("overloaded"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethod_ByArgCount_NullBean() {
        ReflectionUtils.getMethod(null, "any", 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethod_ByArgCount_NullName() {
        ReflectionUtils.getMethod(derivedInstance, null, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethod_ByArgCount_NoMatch() {
        ReflectionUtils.getMethod(derivedInstance, "nonExisting", 0);
    }

    /* -------------------------------------------------------------
     * getConstructor() – success and failure
     * ------------------------------------------------------------- */
    @Test
    public void testGetConstructor_Success() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(DerivedClass.class);
        assertNotNull(ctor);
        assertEquals(0, ctor.getParameterCount());
    }

    @Test
    public void testGetConstructor_WithParameters() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(DerivedClass.class, String.class);
        assertNotNull(ctor);
        assertEquals(1, ctor.getParameterCount());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetConstructor_NoSuchConstructor() {
        ReflectionUtils.getConstructor(DerivedClass.class, int.class);
    }

    /* -------------------------------------------------------------
     * makeParamKey – private utility method
     * ------------------------------------------------------------- */
    @Test
    public void testMakeParamKey_VariousInputs() throws Exception {
        Method mk = ReflectionUtils.class.getDeclaredMethod("makeParamKey", Class[].class);
        mk.setAccessible(true);

        // No parameters
        String key = (String) mk.invoke(null, (Object) new Class<?>[0]);
        assertEquals("", key);

        // Single parameter
        key = (String) mk.invoke(null, (Object) new Class<?>[]{String.class});
        assertEquals(":java.lang.String", key);

        // Multiple parameters
        key = (String) mk.invoke(null, (Object) new Class<?>[]{int.class, List.class});
        assertEquals(":int|java.util.List", key);
    }

    /* -------------------------------------------------------------
     * getNonOverloadedMethod – normal and error cases
     * ------------------------------------------------------------- */
    @Test
    public void testGetNonOverloadedMethod_Success() {
        Method m = ReflectionUtils.getNonOverloadedMethod(SampleClass.class, "add");
        assertNotNull(m);
        assertEquals(2, m.getParameterCount());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethod_NullClass() {
        ReflectionUtils.getNonOverloadedMethod(null, "any");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethod_NullName() {
        ReflectionUtils.getNonOverloadedMethod(SampleClass.class, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethod_Overloaded() {
        // DerivedClass has two overloads named "overloaded"
        ReflectionUtils.getNonOverloadedMethod(DerivedClass.class, "overloaded");
    }

    /* -------------------------------------------------------------
     * getClassName(Object) – null handling
     * ------------------------------------------------------------- */
    @Test
    public void testGetClassName_NullAndNonNull() {
        assertEquals("null", ReflectionUtils.getClassName(null));
        assertEquals(String.class.getName(), ReflectionUtils.getClassName("test"));
    }

    /* -------------------------------------------------------------
     * getClassLoaderName – bootstrap vs. application loader
     * ------------------------------------------------------------- */
    @Test
    public void testGetClassLoaderName_Bootstrap() {
        // java.lang.String is loaded by bootstrap classloader
        String name = ReflectionUtils.getClassLoaderName(String.class);
        assertEquals("bootstrap", name);
    }

    @Test
    public void testGetClassLoaderName_Application() {
        String name = ReflectionUtils.getClassLoaderName(this.getClass());
        assertTrue(name.startsWith("sun.misc.Launcher") || name.startsWith("jdk.internal.loader"));
    }

    /* -------------------------------------------------------------
     * getClassNameFromByteCode – read own inner class bytecode
     * ------------------------------------------------------------- */
    @Test
    public void testGetClassNameFromByteCode() throws Exception {
        // Load bytecode of the inner SampleClass defined above
        String classFileName = SampleClass.class.getSimpleName() + ".class";
        InputStream is = SampleClass.class.getResourceAsStream(classFileName);
        assertNotNull("Could not locate class file for SampleClass", is);
        byte[] bytes = is.readAllBytes();
        is.close();

        String className = ReflectionUtils.getClassNameFromByteCode(bytes);
        assertEquals(SampleClass.class.getName(), className);
    }

    /* -------------------------------------------------------------
     * getDeclaredFields (void version) – ensure private fields become accessible
     * ------------------------------------------------------------- */
    @Test
    public void testGetDeclaredFields_VoidVersion() throws Exception {
        Collection<Field> coll = new ArrayList<>();
        ReflectionUtils.getDeclaredFields(DerivedClass.class, coll);
        // Should contain the private field 'normalField' and make it accessible
        Optional<Field> opt = coll.stream().filter(f -> f.getName().equals("normalField")).findFirst();
        assertTrue(opt.isPresent());
        assertTrue(opt.get().canAccess(derivedInstance));
    }

    /* -------------------------------------------------------------
     * getMethodWithArgs – exercised indirectly via getMethod(Object,…)
     * ------------------------------------------------------------- */
    @Test
    public void testGetMethodWithArgs_MatchingByArgCount() {
        Method m = ReflectionUtils.getMethod(derivedInstance, "methodNoArgs", 0);
        assertNotNull(m);
        assertEquals("methodNoArgs", m.getName());
    }
}
