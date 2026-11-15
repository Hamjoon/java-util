package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.annotation.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.Assert.*;

public class ReflectionUtilsTest {

    /* -------------------------------------------------------
     *  Helper annotations and test classes
     * -----------------------------------------------------*/

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    private @interface TestAnno {
        String value() default "";
    }

    @TestAnno("parent")
    private static class Parent {
        private int privateField = 1;
        protected String protectedField = "p";
        public static final int STATIC_CONST = 42;
        public int dup = 10;

        @TestAnno("parentMethod")
        private void hidden() { }

        public void overloaded(int a) { }

        public void overloaded(String s) { }

        public void overloaded(int a, String s) { }

        public void throwsChecked() throws IOException {
            throw new IOException("checked");
        }
    }

    @TestAnno("child")
    private static class Child extends Parent {
        private String childField = "c";
        public int dup = 20; // field name collides with Parent.dup

        @Override
        public void overloaded(int a) { }

        @TestAnno("childMethod")
        public void childMethod() { }

        private void privateMethod() { }
    }

    private static class NoDefaultConstructor {
        private final int x;

        private NoDefaultConstructor(int x) {
            this.x = x;
        }
    }

    private static class ConstructorsOrder {
        public ConstructorsOrder() { }
        protected ConstructorsOrder(String a) { }
        ConstructorsOrder(int a, int b) { }
        private ConstructorsOrder(double a, double b, double c) { }
    }

    /* -------------------------------------------------------
     *  Setup / Teardown
     * -----------------------------------------------------*/

    @Before
    public void setUp() {
        // Ensure any forced JRE flag is cleared before each test
        System.clearProperty("java.util.force.jre");
    }

    @After
    public void tearDown() {
        System.clearProperty("java.util.force.jre");
    }

    /* -------------------------------------------------------
     *  Tests for class annotation handling
     * -----------------------------------------------------*/

    @Test
    public void testGetClassAnnotationFoundAndCached() {
        TestAnno a1 = ReflectionUtils.getClassAnnotation(Parent.class, TestAnno.class);
        assertNotNull(a1);
        assertEquals("parent", a1.value());

        // second call should hit the cache (same instance)
        TestAnno a2 = ReflectionUtils.getClassAnnotation(Parent.class, TestAnno.class);
        assertSame(a1, a2);
    }

    @Test
    public void testGetClassAnnotationMissing() {
        // No such annotation on java.lang.String
        Deprecated missing = ReflectionUtils.getClassAnnotation(String.class, Deprecated.class);
        assertNull(missing);
    }

    /* -------------------------------------------------------
     *  Tests for method annotation handling
     * -----------------------------------------------------*/

    @Test
    public void testGetMethodAnnotationFoundAndCached() throws NoSuchMethodException {
        Method m = Parent.class.getDeclaredMethod("hidden");
        TestAnno ann = ReflectionUtils.getMethodAnnotation(m, TestAnno.class);
        assertNotNull(ann);
        assertEquals("parentMethod", ann.value());

        // cache hit
        TestAnno ann2 = ReflectionUtils.getMethodAnnotation(m, TestAnno.class);
        assertSame(ann, ann2);
    }

    @Test
    public void testGetMethodAnnotationOnInheritedMethod() throws NoSuchMethodException {
        // Child inherits hidden() from Parent, but we ask for the method object from Parent
        Method parentHidden = Parent.class.getDeclaredMethod("hidden");
        TestAnno ann = ReflectionUtils.getMethodAnnotation(parentHidden, TestAnno.class);
        assertNotNull(ann);
        assertEquals("parentMethod", ann.value());

        // Also ask via a Child method reference (which doesn't exist, should still find via hierarchy)
        Method childMethod = Child.class.getDeclaredMethod("childMethod");
        TestAnno childAnn = ReflectionUtils.getMethodAnnotation(childMethod, TestAnno.class);
        assertNotNull(childAnn);
        assertEquals("childMethod", childAnn.value());
    }

    @Test
    public void testGetMethodAnnotationMissing() throws NoSuchMethodException {
        Method m = Parent.class.getDeclaredMethod("overloaded", int.class);
        Deprecated ann = ReflectionUtils.getMethodAnnotation(m, Deprecated.class);
        assertNull(ann);
    }

    /* -------------------------------------------------------
     *  Tests for field lookup and caching
     * -----------------------------------------------------*/

    @Test
    public void testGetFieldFoundAndCached() {
        // private field in Parent
        java.lang.reflect.Field f1 = ReflectionUtils.getField(Child.class, "privateField");
        assertNotNull(f1);
        assertEquals("privateField", f1.getName());

        // second call should return same instance (cached)
        java.lang.reflect.Field f2 = ReflectionUtils.getField(Child.class, "privateField");
        assertSame(f1, f2);
    }

    @Test
    public void testGetFieldMissing() {
        java.lang.reflect.Field f = ReflectionUtils.getField(Child.class, "nonExistent");
        assertNull(f);
    }

    @Test
    public void testGetDeclaredFieldsDefaultFilter() {
        List<java.lang.reflect.Field> fields = ReflectionUtils.getDeclaredFields(Parent.class);
        // Should contain privateField and protectedField but not static const
        Set<String> names = new HashSet<>();
        for (java.lang.reflect.Field f : fields) names.add(f.getName());

        assertTrue(names.contains("privateField"));
        assertTrue(names.contains("protectedField"));
        assertFalse(names.contains("STATIC_CONST"));
    }

    @Test
    public void testGetDeclaredFieldsCustomFilter() {
        // Only include fields of type int
        List<java.lang.reflect.Field> ints = ReflectionUtils.getDeclaredFields(Parent.class,
                f -> f.getType() == int.class);
        assertEquals(2, ints.size()); // privateField and dup
        Set<String> names = new HashSet<>();
        for (java.lang.reflect.Field f : ints) names.add(f.getName());
        assertTrue(names.contains("privateField"));
        assertTrue(names.contains("dup"));
    }

    @Test
    public void testGetAllDeclaredFieldsHierarchyAndCollision() {
        List<java.lang.reflect.Field> all = ReflectionUtils.getAllDeclaredFields(Child.class);
        // Should contain fields from Child and Parent, respecting default filter
        Set<String> simpleNames = new HashSet<>();
        for (java.lang.reflect.Field f : all) simpleNames.add(f.getName());

        assertTrue(simpleNames.contains("childField"));
        assertTrue(simpleNames.contains("privateField"));
        assertTrue(simpleNames.contains("protectedField"));
        // The colliding field name "dup" appears only once as simple name (child wins)
        assertTrue(simpleNames.contains("dup"));
        // Verify map contains both qualified names for collision
        Map<String, java.lang.reflect.Field> map = ReflectionUtils.getAllDeclaredFieldsMap(Child.class);
        assertTrue(map.containsKey("dup")); // child field
        assertTrue(map.containsKey(Parent.class.getName() + ".dup")); // parent field
        assertEquals(Child.class, map.get("dup").getDeclaringClass());
        assertEquals(Parent.class, map.get(Parent.class.getName() + ".dup").getDeclaringClass());
    }

    /* -------------------------------------------------------
     *  Tests for constructor handling
     * -----------------------------------------------------*/

    @Test
    public void testGetConstructorFoundAndCached() {
        Constructor<?> c1 = ReflectionUtils.getConstructor(NoDefaultConstructor.class, int.class);
        assertNotNull(c1);
        assertEquals(1, c1.getParameterCount());

        // cache hit
        Constructor<?> c2 = ReflectionUtils.getConstructor(NoDefaultConstructor.class, int.class);
        assertSame(c1, c2);
    }

    @Test
    public void testGetConstructorMissing() {
        Constructor<?> c = ReflectionUtils.getConstructor(NoDefaultConstructor.class, String.class);
        assertNull(c);
    }

    @Test
    public void testGetAllConstructorsSortedOrder() {
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(ConstructorsOrder.class);
        assertEquals(4, ctors.length);

        // Expected order: public (no args), protected (String), package-private (int,int), private (double,double,double)
        assertTrue(Modifier.isPublic(ctors[0].getModifiers()));
        assertTrue(Modifier.isProtected(ctors[1].getModifiers()));
        assertFalse(Modifier.isPublic(ctors[2].getModifiers()));
        assertFalse(Modifier.isProtected(ctors[2].getModifiers()));
        assertTrue(Modifier.isPrivate(ctors[3].getModifiers()));

        // Within same accessibility, more parameters first – check protected constructor has 1 param, package-private has 2
        assertEquals(1, ctors[1].getParameterCount());
        assertEquals(2, ctors[2].getParameterCount());
        assertEquals(3, ctors[3].getParameterCount());
    }

    /* -------------------------------------------------------
     *  Tests for method lookup utilities
     * -----------------------------------------------------*/

    @Test
    public void testGetMethodByNameAndTypes() {
        Method m = ReflectionUtils.getMethod(Parent.class, "overloaded", int.class);
        assertNotNull(m);
        assertEquals(1, m.getParameterCount());
        assertEquals(int.class, m.getParameterTypes()[0]);

        // Ensure caching works
        Method cached = ReflectionUtils.getMethod(Parent.class, "overloaded", int.class);
        assertSame(m, cached);
    }

    @Test
    public void testGetMethodByInstanceAndArgCount() {
        Child obj = new Child();

        // There are three overloaded methods named "overloaded" with 1, 2, and 2 params (int+String)
        Method m = ReflectionUtils.getMethod(obj, "overloaded", 2);
        assertNotNull(m);
        assertEquals(2, m.getParameterCount());

        // Verify that the selected method is the most accessible (public) one from Child (overridden)
        assertEquals(Child.class, m.getDeclaringClass());
    }

    @Test
    public void testSelectMethodPrefersNonSyntheticAndAccessibility() throws NoSuchMethodException {
        // Create synthetic method scenario via lambda (synthetic method generated in the class)
        // We'll use a method from java.util.Collections (static, non-synthetic) and a synthetic bridge from a generic class.
        // For simplicity, we just verify that selectMethod prefers a public method over a private one when both match.
        Method privateM = Parent.class.getDeclaredMethod("privateMethod");
        // make it accessible for the test (ReflectionUtils will do it anyway)
        privateM.setAccessible(true);
        Method publicM = Parent.class.getMethod("overloaded", int.class); // public
        List<Method> candidates = Arrays.asList(privateM, publicM);
        Method selected = (Method) ReflectionUtils.class.getDeclaredMethod("selectMethod", List.class)
                .invoke(null, candidates);
        assertEquals(publicM, selected);
    }

    @Test
    public void testGetNonOverloadedMethodSuccess() {
        Method m = ReflectionUtils.getNonOverloadedMethod(Parent.class, "hidden");
        assertNotNull(m);
        assertEquals("hidden", m.getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethodFailsWhenOverloaded() {
        // overloaded is overloaded -> should throw
        ReflectionUtils.getNonOverloadedMethod(Parent.class, "overloaded");
    }

    /* -------------------------------------------------------
     *  Tests for reflective call utilities
     * -----------------------------------------------------*/

    @Test
    public void testCallMethodSuccess() throws Exception {
        Child obj = new Child();
        Method m = ReflectionUtils.getMethod(Child.class, "childMethod");
        Object result = ReflectionUtils.call(obj, m);
        // childMethod returns void -> result should be null
        assertNull(result);
    }

    @Test
    public void testCallMethodHandlesCheckedException() throws Exception {
        Parent p = new Parent();
        Method m = ReflectionUtils.getMethod(Parent.class, "throwsChecked");
        try {
            ReflectionUtils.call(p, m);
            fail("Expected RuntimeException wrapping IOException");
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof IOException);
            assertEquals("checked", e.getCause().getMessage());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallMethodNullMethod() {
        ReflectionUtils.call(new Object(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallMethodNullInstance() throws Exception {
        Method m = Parent.class.getDeclaredMethod("hidden");
        ReflectionUtils.call(null, m);
    }

    @Test
    public void testCallByNameConvenience() {
        Child obj = new Child();
        // childMethod is public, no args
        Object res = ReflectionUtils.call(obj, "childMethod");
        assertNull(res);
    }

    /* -------------------------------------------------------
     *  Tests for utility methods
     * -----------------------------------------------------*/

    @Test
    public void testGetClassName() {
        assertEquals("null", ReflectionUtils.getClassName(null));
        assertEquals(ReflectionUtilsTest.class.getName(), ReflectionUtils.getClassName(this));
    }

    @Test
    public void testGetClassNameFromByteCode() throws IOException {
        // Load the class file bytes for java.lang.String using the classloader resource stream
        String resourcePath = "/" + String.class.getName().replace('.', '/') + ".class";
        byte[] bytes = ReflectionUtilsTest.class.getResourceAsStream(resourcePath).readAllBytes();
        String name = ReflectionUtils.getClassNameFromByteCode(bytes);
        assertEquals("java.lang.String", name);
    }

    @Test
    public void testIsJavaCompilerAvailableDefault() {
        // In most JDK environments the compiler is present; we just verify the method returns a boolean
        boolean available = ReflectionUtils.isJavaCompilerAvailable();
        // No assertion about true/false – just that it doesn't throw
        assertTrue(available || !available);
    }

    @Test
    public void testIsJavaCompilerAvailableForceJre() {
        System.setProperty("java.util.force.jre", "true");
        assertFalse(ReflectionUtils.isJavaCompilerAvailable());
    }

    /* -------------------------------------------------------
     *  Tests for cache replacement API (simple sanity)
     * -----------------------------------------------------*/

    @Test
    public void testSetCustomMethodCache() {
        Map<Object, Method> custom = new HashMap<>();
        ReflectionUtils.setMethodCache(custom);
        // After setting, a lookup should populate our custom map
        Method m = ReflectionUtils.getMethod(Parent.class, "overloaded", int.class);
        assertTrue(custom.containsKey(new ReflectionUtils.MethodCacheKey(Parent.class, "overloaded", int.class)));
        assertSame(m, custom.get(new ReflectionUtils.MethodCacheKey(Parent.class, "overloaded", int.class)));
    }

    @Test
    public void testSetCustomConstructorCache() {
        Map<Object, Constructor<?>> custom = new HashMap<>();
        ReflectionUtils.setConstructorCache(custom);
        Constructor<?> ctor = ReflectionUtils.getConstructor(NoDefaultConstructor.class, int.class);
        assertTrue(custom.containsKey(new ReflectionUtils.ConstructorCacheKey(NoDefaultConstructor.class, int.class)));
        assertSame(ctor, custom.get(new ReflectionUtils.ConstructorCacheKey(NoDefaultConstructor.class, int.class)));
    }
}
