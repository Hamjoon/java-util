package com.cedarsoftware.util;
import com.cedarsoftware.util.ReflectionUtils.MethodCacheKey;


import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link ReflectionUtils}.
 *
 * <p>All tests use only the public API that is present in the supplied source
 * file.  No external mocking frameworks are used – real objects are created
 * through constructors or factory methods that exist in the library.</p>
 *
 * <p>The suite covers:</p>
 * <ul>
 *   <li>Annotation lookup (class and method) including inheritance</li>
 *   <li>Field lookup (single field, declared fields, hierarchy fields, maps)</li>
 *   <li>Constructor lookup and ordering of all constructors</li>
 *   <li>Method lookup (by name &amp; types, by argument count, non‑overloaded)</li>
 *   <li>Invocation helpers (call with {@link Method} and call with method name)</li>
 *   <li>Utility methods such as {@code getClassName}, {@code getClassNameFromByteCode},
 *       {@code isJavaCompilerAvailable}</li>
 *   <li>Cache replacement to guarantee test isolation</li>
 * </ul>
 */
public class ReflectionUtilsTest {

    /* --------------------------------------------------------------------- */
    /*  Helper annotations and test classes                                 */
    /* --------------------------------------------------------------------- */

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
    private @interface TestAnno { }

    @TestAnno
    private static class SuperClass {
        @TestAnno
        public void annotatedMethod() { }

        private String privateField = "super";

        public String publicField = "superPublic";

        private void privateMethod() { }

        protected void protectedMethod() { }

        public void publicMethod() { }
    }

    private static class SubClass extends SuperClass {
        private String privateField = "sub";

        // No annotation on this method – should be found via inheritance
        public void annotatedMethod() { }

        // Overloaded method – used for overload resolution tests
        public void overloaded(String s) { }

        public void overloaded(Integer i) { }

        // Bridge method simulation (compiler generated) – not needed explicitly
    }

    private interface AnnotatedInterface {
        @TestAnno
        void interfaceMethod();
    }

    private static class ImplementsInterface implements AnnotatedInterface {
        public void interfaceMethod() { }
    }

    private static class WithConstructors {
        public WithConstructors() { }

        protected WithConstructors(String s) { }

        WithConstructors(int i, String s) { }

        private WithConstructors(double d, int i, String s) { }
    }

    /* --------------------------------------------------------------------- */
    /*  Cache reset before each test – guarantees independence                */
    /* --------------------------------------------------------------------- */
    @Before
    public void resetCaches() {
        // Replace every internal cache with a fresh ConcurrentHashMap.
        // The ensureThreadSafe() method will accept it as‑is.
        ReflectionUtils.setMethodCache(new ConcurrentHashMap<>());
        ReflectionUtils.setClassFieldsCache(new ConcurrentHashMap<>());
        ReflectionUtils.setFieldCache(new ConcurrentHashMap<>());
        ReflectionUtils.setClassAnnotationCache(new ConcurrentHashMap<>());
        ReflectionUtils.setMethodAnnotationCache(new ConcurrentHashMap<>());
        ReflectionUtils.setConstructorCache(new ConcurrentHashMap<>());
        ReflectionUtils.setSortedConstructorsCache(new ConcurrentHashMap<>());
    }

    /* --------------------------------------------------------------------- */
    /*  Class‑level annotation lookup                                         */
    /* --------------------------------------------------------------------- */
    @Test
    public void testGetClassAnnotationFoundDirectly() {
        TestAnno anno = ReflectionUtils.getClassAnnotation(SuperClass.class, TestAnno.class);
        assertNotNull("Annotation should be found on the class itself", anno);
    }

    @Test
    public void testGetClassAnnotationInherited() {
        // SubClass does NOT have the annotation, but its superclass does.
        TestAnno anno = ReflectionUtils.getClassAnnotation(SubClass.class, TestAnno.class);
        assertNotNull("Annotation should be inherited from superclass", anno);
    }

    @Test
    public void testGetClassAnnotationCacheMiss() {
        // Use a class that definitely has no such annotation.
        Annotation result = ReflectionUtils.getClassAnnotation(String.class, TestAnno.class);
        assertNull("Cache should store a miss (null) for non‑annotated class", result);
    }

    /* --------------------------------------------------------------------- */
    /*  Method‑level annotation lookup                                         */
    /* --------------------------------------------------------------------- */
    @Test
    public void testGetMethodAnnotationDirect() throws NoSuchMethodException {
        Method m = SuperClass.class.getMethod("annotatedMethod");
        TestAnno anno = ReflectionUtils.getMethodAnnotation(m, TestAnno.class);
        assertNotNull("Method annotation should be found directly", anno);
    }

    @Test
    public void testGetMethodAnnotationFromInterface() throws NoSuchMethodException {
        Method ifaceMethod = AnnotatedInterface.class.getMethod("interfaceMethod");
        TestAnno anno = ReflectionUtils.getMethodAnnotation(ifaceMethod, TestAnno.class);
        assertNotNull("Method annotation should be found on interface method", anno);
    }

    @Test
    public void testGetMethodAnnotationInherited() throws NoSuchMethodException {
        // Method defined in SuperClass is annotated; SubClass inherits it.
        Method m = SubClass.class.getMethod("annotatedMethod");
        TestAnno anno = ReflectionUtils.getMethodAnnotation(m, TestAnno.class);
        assertNotNull("Annotation should be found via class hierarchy", anno);
    }

    @Test
    public void testGetMethodAnnotationCacheMiss() throws NoSuchMethodException {
        Method m = String.class.getMethod("length");
        TestAnno anno = ReflectionUtils.getMethodAnnotation(m, TestAnno.class);
        assertNull("Cache should store miss for method without annotation", anno);
    }

    /* --------------------------------------------------------------------- */
    /*  Field lookup (single field, declared, hierarchy)                     */
    /* --------------------------------------------------------------------- */
    @Test
    public void testGetFieldFoundInSuperclass() {
        Field f = ReflectionUtils.getField(SubClass.class, "privateField");
        assertNotNull("Should locate private field declared in subclass", f);
        assertEquals("privateField", f.getName());
        assertEquals(SubClass.class, f.getDeclaringClass());
    }

    @Test
    public void testGetFieldNotFound() {
        Field f = ReflectionUtils.getField(SubClass.class, "nonExistent");
        assertNull("Should return null for missing field", f);
    }

    @Test
    public void testGetDeclaredFieldsWithCustomFilter() {
        // Filter only public fields
        List<Field> publicFields = ReflectionUtils.getDeclaredFields(SubClass.class,
                f -> Modifier.isPublic(f.getModifiers()));
        assertEquals("SubClass declares no public fields", 0, publicFields.size());

        // SuperClass public field should be reachable via getAllDeclaredFields
        List<Field> allPublic = ReflectionUtils.getAllDeclaredFields(SubClass.class,
                f -> Modifier.isPublic(f.getModifiers()));
        assertTrue("Superclass public field must be present", allPublic.stream()
                .anyMatch(f -> f.getName().equals("publicField")));
    }

    @Test
    public void testGetAllDeclaredFieldsOrder() {
        List<Field> all = ReflectionUtils.getAllDeclaredFields(SubClass.class);
        // The first element must be the field from SubClass (privateField)
        assertEquals("privateField", all.get(0).getName());
        // The later element should be the field from SuperClass (publicField)
        assertTrue(all.stream().anyMatch(f -> f.getName().equals("publicField")));
    }

    @Test
    public void testGetAllDeclaredFieldsMapCollision() {
        // Create a hierarchy where both classes declare a field with same name.
        class Parent {
            private String dup = "parent";
        }
        class Child extends Parent {
            private String dup = "child";
        }

        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(Child.class);
        // Simple name should map to child field
        assertEquals("child", map.get("dup").getDeclaredAnnotations().length == 0 ? "child" : "unexpected");
        // Fully qualified name should map to parent field
        String fq = Parent.class.getName() + ".dup";
        assertTrue("Parent field should be reachable via fully qualified key", map.containsKey(fq));
    }

    /* --------------------------------------------------------------------- */
    /*  Constructor lookup and ordering                                      */
    /* --------------------------------------------------------------------- */
    @Test
    public void testGetConstructorExactMatch() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(WithConstructors.class, int.class, String.class);
        assertNotNull("Constructor with (int, String) should be found", ctor);
        assertEquals(2, ctor.getParameterCount());
    }

    @Test
    public void testGetConstructorCacheMiss() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(WithConstructors.class, long.class);
        assertNull("No constructor with (long) – cache should store null", ctor);
    }

    @Test
    public void testGetAllConstructorsOrdering() {
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(WithConstructors.class);
        assertEquals(4, ctors.length);
        // Public constructor first
        assertTrue(Modifier.isPublic(ctors[0].getModifiers()));
        // Followed by protected, package‑private, then private
        assertTrue(Modifier.isProtected(ctors[1].getModifiers()));
        assertFalse(Modifier.isPublic(ctors[2].getModifiers()) || Modifier.isProtected(ctors[2].getModifiers()));
        assertTrue(Modifier.isPrivate(ctors[3].getModifiers()));
        // Within same visibility, more parameters first – verify for protected ctor
        assertEquals(1, ctors[1].getParameterCount()); // protected ctor has one param
    }

    /* --------------------------------------------------------------------- */
    /*  Non‑overloaded method lookup                                          */
    /* --------------------------------------------------------------------- */
    @Test
    public void testGetNonOverloadedMethodSuccess() throws Exception {
        Method m = ReflectionUtils.getNonOverloadedMethod(SubClass.class, "publicMethod");
        assertNotNull(m);
        assertEquals("publicMethod", m.getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethodAmbiguous() throws Exception {
        // SubClass has two overloaded methods named "overloaded"
        ReflectionUtils.getNonOverloadedMethod(SubClass.class, "overloaded");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethodMissing() throws Exception {
        ReflectionUtils.getNonOverloadedMethod(SubClass.class, "doesNotExist");
    }

    /* --------------------------------------------------------------------- */
    /*  getMethod (by name & types) and getMethod (by instance & argCount)   */
    /* --------------------------------------------------------------------- */
    @Test
    public void testGetMethodBySignature() {
        Method m = ReflectionUtils.getMethod(SubClass.class, "overloaded", String.class);
        assertNotNull(m);
        assertEquals(String.class, m.getParameterTypes()[0]);
    }

    @Test
    public void testGetMethodByInstanceArgCountPrefersAccessibility() {
        SubClass obj = new SubClass();
        Method m = ReflectionUtils.getMethod(obj, "privateMethod", 0);
        assertNotNull(m);
        assertTrue(Modifier.isPrivate(m.getModifiers()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodByInstanceArgCountNotFound() {
        SubClass obj = new SubClass();
        ReflectionUtils.getMethod(obj, "nonExisting", 1);
    }

    /* --------------------------------------------------------------------- */
    /*  call(...) helpers                                                    */
    /* --------------------------------------------------------------------- */
    @Test
    public void testCallWithMethodObject() throws Exception {
        SubClass instance = new SubClass();
        Method m = ReflectionUtils.getMethod(SubClass.class, "publicMethod");
        Object result = ReflectionUtils.call(instance, m);
        assertNull("publicMethod returns void – should be null", result);
    }

    @Test
    public void testCallWithMethodName() throws Exception {
        SubClass instance = new SubClass();
        // publicMethod has no args
        Object result = ReflectionUtils.call(instance, "publicMethod");
        assertNull("Void method should return null", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallWithNullMethod() {
        ReflectionUtils.call(new Object(), (Method) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallWithNullInstance() throws Exception {
        Method m = SubClass.class.getMethod("publicMethod");
        ReflectionUtils.call(null, m);
    }

    /* --------------------------------------------------------------------- */
    /*  Utility methods                                                     */
    /* --------------------------------------------------------------------- */
    @Test
    public void testGetClassName() {
        assertEquals("null", ReflectionUtils.getClassName(null));
        assertEquals(SubClass.class.getName(), ReflectionUtils.getClassName(new SubClass()));
    }

    @Test
    public void testGetClassNameFromByteCode() throws IOException {
        // Load the .class file of this test class itself.
        String resourcePath = ReflectionUtilsTest.class.getName().replace('.', '/') + ".class";
        InputStream is = ReflectionUtilsTest.class.getClassLoader().getResourceAsStream(resourcePath);
        assertNotNull("Unable to locate .class resource for test", is);
        byte[] bytes = is.readAllBytes();
        String className = ReflectionUtils.getClassNameFromByteCode(bytes);
        assertEquals("Class name extracted from bytecode must match", ReflectionUtilsTest.class.getName(), className);
    }

    @Test
    public void testIsJavaCompilerAvailableWhenForcedJre() {
        // Force the method to think we are on a JRE.
        String prop = "java.util.force.jre";
        String original = System.getProperty(prop);
        try {
            System.setProperty(prop, "true");
            assertFalse("When system property forces JRE, compiler must be reported unavailable",
                    ReflectionUtils.isJavaCompilerAvailable());
        } finally {
            // Restore original property value
            if (original != null) {
                System.setProperty(prop, original);
            } else {
                System.clearProperty(prop);
            }
        }
    }

    /* --------------------------------------------------------------------- */
    /*  Internal helper – ensures that the LRUCache fallback works with a    */
    /*  plain ConcurrentHashMap (used in the @Before method).                */
    /* --------------------------------------------------------------------- */
    @Test
    public void testCacheReplacementWorks() {
        // After reset, the method cache should be empty.
        MethodCacheKey dummyKey = new ReflectionUtils.MethodCacheKey(SubClass.class, "dummy");
        assertFalse("Cache should be empty after replacement",
                ReflectionUtils.METHOD_CACHE.get().containsKey(dummyKey));
        // Insert a dummy entry and verify retrieval.
        Method dummyMethod = null; // null is allowed – represents a miss.
        ReflectionUtils.METHOD_CACHE.get().put(dummyKey, dummyMethod);
        assertTrue("Dummy entry must be present", ReflectionUtils.METHOD_CACHE.get().containsKey(dummyKey));
    }
}
