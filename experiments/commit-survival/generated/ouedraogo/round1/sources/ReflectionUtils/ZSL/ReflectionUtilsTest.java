package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.AfterClass;

import java.io.InputStream;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import static org.junit.Assert.*;

public class ReflectionUtilsTest {

    /* ---------- Helper Annotations & Classes ---------- */

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface TestAnno {
        String value() default "";
    }

    @TestAnno("interface")
    public interface AnnotatedInterface {
    }

    @TestAnno("super")
    public static class SuperClass implements AnnotatedInterface {
        @TestAnno("superMethod")
        public String superMethod() {
            return "super";
        }

        public void methodToThrow() {
            throw new IllegalStateException("boom");
        }

        private String privateField = "p";
        public static final String STATIC_FIELD = "static";
        public transient String transientField = "transient";
    }

    public static class SubClass extends SuperClass {
        public String childField;
        private String duplicate; // same name as in SuperClass (private)
    }

    public static class DuplicateFieldParent {
        private String dup = "parent";
    }

    public static class DuplicateFieldChild extends DuplicateFieldParent {
        private String dup = "child";
    }

    public static class NoDefaultConstructor {
        private final String value;

        public NoDefaultConstructor(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public static class OverloadedMethods {
        public String foo(int a) { return "int"; }
        public String foo(String s) { return "string"; }
        public String bar() { return "bar"; }
    }

    /* ---------- Tests ---------- */

    @Test
    public void testGetClassAnnotation_SuperclassAndInterface() {
        // should find annotation on super class
        TestAnno a1 = ReflectionUtils.getClassAnnotation(SubClass.class, TestAnno.class);
        assertNotNull(a1);
        assertEquals("super", a1.value());

        // should find annotation on interface when not on class hierarchy
        TestAnno a2 = ReflectionUtils.getClassAnnotation(AnnotatedInterface.class, TestAnno.class);
        assertNotNull(a2);
        assertEquals("interface", a2.value());

        // class without annotation returns null
        assertNull(ReflectionUtils.getClassAnnotation(Object.class, TestAnno.class));
    }

    @Test
    public void testGetMethodAnnotation_InheritedMethod() throws Exception {
        Method superMethod = SuperClass.class.getMethod("superMethod");
        TestAnno anno = ReflectionUtils.getMethodAnnotation(superMethod, TestAnno.class);
        assertNotNull(anno);
        assertEquals("superMethod", anno.value());

        // method in subclass should inherit annotation from super
        Method subMethod = SubClass.class.getMethod("superMethod");
        TestAnno anno2 = ReflectionUtils.getMethodAnnotation(subMethod, TestAnno.class);
        assertNotNull(anno2);
        assertEquals("superMethod", anno2.value());

        // method without annotation yields null
        Method childMethod = SubClass.class.getMethod("foo", int.class);
        assertNull(ReflectionUtils.getMethodAnnotation(childMethod, TestAnno.class));
    }

    @Test
    public void testGetMethod_CachingAndNullHandling() {
        Method m1 = ReflectionUtils.getMethod(SubClass.class, "superMethod");
        assertNotNull(m1);
        Method m2 = ReflectionUtils.getMethod(SubClass.class, "superMethod");
        // should be the same cached instance
        assertSame(m1, m2);

        // non‑existent method returns null
        assertNull(ReflectionUtils.getMethod(SubClass.class, "doesNotExist"));
    }

    @Test
    public void testGetDeclaredFieldsAndDeepDeclaredFields() {
        List<Field> declared = ReflectionUtils.getDeclaredFields(SubClass.class);
        // Should contain all declared fields of SubClass (including private)
        Set<String> names = new HashSet<>();
        for (Field f : declared) names.add(f.getName());
        assertTrue(names.contains("childField"));
        assertTrue(names.contains("duplicate"));

        Collection<Field> deep = ReflectionUtils.getDeepDeclaredFields(SubClass.class);
        // Should contain fields from SubClass and SuperClass (excluding static/transient)
        Set<String> deepNames = new HashSet<>();
        for (Field f : deep) deepNames.add(f.getName());

        assertTrue(deepNames.contains("childField"));
        assertTrue(deepNames.contains("privateField"));
        assertTrue(deepNames.contains("duplicate"));
        assertFalse(deepNames.contains("STATIC_FIELD"));
        assertFalse(deepNames.contains("transientField"));
    }

    @Test
    public void testGetDeepDeclaredFieldMap_DuplicateNames() {
        Map<String, Field> map = ReflectionUtils.getDeepDeclaredFieldMap(DuplicateFieldChild.class);
        // Two fields named "dup" exist; one should be qualified
        assertTrue(map.containsKey("dup")); // the child field (declared in child) wins
        assertTrue(map.containsKey(DuplicateFieldParent.class.getName() + ".dup"));
        assertEquals("child", map.get("dup").getDeclaringClass().getSimpleName());
        assertEquals("parent", map.get(DuplicateFieldParent.class.getName() + ".dup").getDeclaringClass().getSimpleName());
    }

    @Test
    public void testCall_WithMethodObject_SuccessAndExceptionWrapping() throws Exception {
        SubClass obj = new SubClass();

        // successful call
        Method m = ReflectionUtils.getMethod(SubClass.class, "superMethod");
        Object result = ReflectionUtils.call(obj, m);
        assertEquals("super", result);

        // method that throws IllegalStateException should be wrapped in RuntimeException
        Method thrower = ReflectionUtils.getMethod(SuperClass.class, "methodToThrow");
        try {
            ReflectionUtils.call(obj, thrower);
            fail("Expected RuntimeException");
        } catch (RuntimeException re) {
            assertTrue(re.getCause() instanceof IllegalStateException);
            assertEquals("boom", re.getCause().getMessage());
        }

        // null method argument
        try {
            ReflectionUtils.call(obj, (Method) null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
            assertTrue(iae.getMessage().contains("null Method"));
        }

        // null bean argument
        try {
            ReflectionUtils.call(null, m);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
            assertTrue(iae.getMessage().contains("null object"));
        }
    }

    @Test
    public void testCall_WithMethodNameAndArgs() {
        OverloadedMethods bean = new OverloadedMethods();

        // call method by name with correct arg count (1)
        Object r1 = ReflectionUtils.call(bean, "foo", 5);
        assertEquals("int", r1);

        // call method by name with zero args
        Object r2 = ReflectionUtils.call(bean, "bar");
        assertEquals("bar", r2);
    }

    @Test
    public void testGetMethod_ObjectAndArgCount() {
        OverloadedMethods bean = new OverloadedMethods();

        // argCount 1 should resolve to foo(int) or foo(String) – both have count 1, but getMethodWithArgs picks first match.
        Method m = ReflectionUtils.getMethod(bean, "foo", 1);
        assertNotNull(m);
        assertEquals("foo", m.getName());

        // zero args resolves to bar()
        Method bar = ReflectionUtils.getMethod(bean, "bar", 0);
        assertNotNull(bar);
        assertEquals("bar", bar.getName());

        // invalid bean
        try {
            ReflectionUtils.getMethod(null, "foo", 1);
            fail("Expected IllegalArgumentException for null bean");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("null instance"));
        }

        // null method name
        try {
            ReflectionUtils.getMethod(bean, null, 0);
            fail("Expected IllegalArgumentException for null method name");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("null method name"));
        }

        // method not found should throw
        try {
            ReflectionUtils.getMethod(bean, "nonexistent", 0);
            fail("Expected IllegalArgumentException for missing method");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Method: nonexistent() is not found"));
        }
    }

    @Test
    public void testGetNonOverloadedMethod_SuccessAndAmbiguity() {
        // success case
        Method bar = ReflectionUtils.getNonOverloadedMethod(OverloadedMethods.class, "bar");
        assertNotNull(bar);
        assertEquals("bar", bar.getName());

        // ambiguous (overloaded) should throw
        try {
            ReflectionUtils.getNonOverloadedMethod(OverloadedMethods.class, "foo");
            fail("Expected IllegalArgumentException for overloaded method");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("overloaded methods"));
        }

        // null class
        try {
            ReflectionUtils.getNonOverloadedMethod(null, "foo");
            fail("Expected IllegalArgumentException for null class");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("null class"));
        }

        // null method name
        try {
            ReflectionUtils.getNonOverloadedMethod(OverloadedMethods.class, null);
            fail("Expected IllegalArgumentException for null method name");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("null method name"));
        }
    }

    @Test
    public void testGetConstructorAndInstantiation() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(NoDefaultConstructor.class, String.class);
        assertNotNull(ctor);
        try {
            Object instance = ctor.newInstance("hello");
            assertTrue(instance instanceof NoDefaultConstructor);
            assertEquals("hello", ((NoDefaultConstructor) instance).getValue());
        } catch (Exception e) {
            fail("Unexpected exception: " + e);
        }

        // request non‑existent constructor -> IllegalArgumentException
        try {
            ReflectionUtils.getConstructor(NoDefaultConstructor.class, int.class);
            fail("Expected IllegalArgumentException for missing constructor");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("did not exist"));
        }
    }

    @Test
    public void testGetClassName() {
        assertEquals("null", ReflectionUtils.getClassName(null));
        assertEquals(String.class.getName(), ReflectionUtils.getClassName("test"));
    }

    @Test
    public void testGetClassNameFromByteCode() throws Exception {
        // Load the .class bytes of a known class (this test class itself)
        String className = ReflectionUtilsTest.class.getName();
        String resource = "/" + className.replace('.', '/') + ".class";
        InputStream is = ReflectionUtilsTest.class.getResourceAsStream(resource);
        assertNotNull("Unable to locate class bytecode for " + className, is);
        byte[] bytes = is.readAllBytes();

        String extracted = ReflectionUtils.getClassNameFromByteCode(bytes);
        assertEquals(className, extracted);
    }

    /* ---------- Utility to clear caches (optional) ---------- */
    @AfterClass
    public static void clearCaches() throws Exception {
        // Using reflection to clear internal caches to avoid cross‑test interference.
        // Not required for correctness but keeps test isolation.
        Field f1 = ReflectionUtils.class.getDeclaredField("METHOD_MAP");
        f1.setAccessible(true);
        ((ConcurrentMap<?, ?>) f1.get(null)).clear();

        Field f2 = ReflectionUtils.class.getDeclaredField("METHOD_MAP2");
        f2.setAccessible(true);
        ((ConcurrentMap<?, ?>) f2.get(null)).clear();

        Field f3 = ReflectionUtils.class.getDeclaredField("METHOD_MAP3");
        f3.setAccessible(true);
        ((ConcurrentMap<?, ?>) f3.get(null)).clear();

        Field f4 = ReflectionUtils.class.getDeclaredField("FIELD_MAP");
        f4.setAccessible(true);
        ((ConcurrentMap<?, ?>) f4.get(null)).clear();

        Field f5 = ReflectionUtils.class.getDeclaredField("CONSTRUCTORS");
        f5.setAccessible(true);
        ((ConcurrentMap<?, ?>) f5.get(null)).clear();

        Field f6 = ReflectionUtils.class.getDeclaredField("FIELD_META_CACHE");
        f6.setAccessible(true);
        ((ConcurrentMap<?, ?>) f6.get(null)).clear();
    }
}
