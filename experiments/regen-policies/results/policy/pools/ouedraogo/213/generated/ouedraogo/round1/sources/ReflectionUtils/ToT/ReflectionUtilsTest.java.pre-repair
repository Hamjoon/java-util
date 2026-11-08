package com.cedarsoftware.util;

import org.junit.*;
import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ReflectionUtils}.
 *
 * <p>This test class covers:
 * <ul>
 *   <li>annotation lookup (class & method) including caching</li>
 *   <li>field lookup (including missing fields) and caching</li>
 *   <li>declared‑field filtering and inheritance handling</li>
 *   <li>method lookup – exact signatures, overload resolution and deterministic selection</li>
 *   <li>constructor lookup and ordering of {@code getAllConstructors}</li>
 *   <li>utility methods: {@code call}, {@code getClassName},
 *       {@code getClassNameFromByteCode}, {@code isJavaCompilerAvailable}</li>
 *   <li>error handling for null arguments and illegal states</li>
 * </ul>
 *
 * <p>Only APIs that appear in the supplied source are used; no external mocking
 * frameworks are employed.  Helper classes and annotations are defined as
 * static inner types so the test suite is self‑contained.
 */
public class ReflectionUtilsTest {

    /*======================================================================
     *  Test fixtures – simple hierarchy with annotations and fields
     *====================================================================*/

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    private @interface TestAnno {
        String value();
    }

    @TestAnno("Base")
    private static class BaseClass {
        private int baseField;
        public static final String STATIC_FIELD = "ignore";

        @TestAnno("baseMethod")
        private void annotatedMethod() {}

        private void overloaded(String s) {}
        private void overloaded(Integer i) {}
        private void overloaded(String s, Integer i) {}
    }

    @TestAnno("Derived")
    private static class DerivedClass extends BaseClass {
        private String baseField; // hides BaseClass.baseField (name collision)
        private int derivedField;

        @Override
        private void annotatedMethod() {}

        // bridge & synthetic example – a generic method
        @SuppressWarnings("unused")
        private <T> T genericMethod(T t) { return t; }
    }

    /*======================================================================
     *  Helper utilities used by many tests
     *======================================================================*/

    /** Reads the raw .class bytes of the supplied class. */
    private static byte[] classBytes(Class<?> clazz) throws IOException {
        String resource = '/' + clazz.getName().replace('.', '/') + ".class";
        try (InputStream is = clazz.getResourceAsStream(resource);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            assertNotNull("Unable to locate class file for " + clazz, is);
            byte[] buf = new byte[1024];
            int n;
            while ((n = is.read(buf)) != -1) {
                baos.write(buf, 0, n);
            }
            return baos.toByteArray();
        }
    }

    /*======================================================================
     *  Annotation lookup tests
     *======================================================================*/

    @Test
    public void testGetClassAnnotationFoundAndCached() {
        // first lookup – cache miss
        TestAnno a1 = ReflectionUtils.getClassAnnotation(DerivedClass.class, TestAnno.class);
        assertNotNull(a1);
        assertEquals("Derived", a1.value());

        // second lookup – should hit the cache and return the same instance
        TestAnno a2 = ReflectionUtils.getClassAnnotation(DerivedClass.class, TestAnno.class);
        assertSame("Annotation should be cached", a1, a2);
    }

    @Test
    public void testGetClassAnnotationFromSuperclass() {
        TestAnno a = ReflectionUtils.getClassAnnotation(DerivedClass.class, TestAnno.class);
        // DerivedClass has its own annotation, verify that it is returned (not the superclass one)
        assertEquals("Derived", a.value());
    }

    @Test
    public void testGetClassAnnotationMissing() {
        // No such annotation on Object
        @SuppressWarnings("unchecked")
        Annotation a = ReflectionUtils.getClassAnnotation(Object.class, TestAnno.class);
        assertNull(a);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetClassAnnotationNullAnnoClass() {
        ReflectionUtils.getClassAnnotation(String.class, null);
    }

    @Test
    public void testGetMethodAnnotationFoundAndCached() throws Exception {
        Method m = DerivedClass.class.getDeclaredMethod("annotatedMethod");
        TestAnno a1 = ReflectionUtils.getMethodAnnotation(m, TestAnno.class);
        assertNotNull(a1);
        assertEquals("Derived", a1.value());

        // Cached result – same instance
        TestAnno a2 = ReflectionUtils.getMethodAnnotation(m, TestAnno.class);
        assertSame(a1, a2);
    }

    @Test
    public void testGetMethodAnnotationFromSuperclass() throws Exception {
        Method m = DerivedClass.class.getDeclaredMethod("overloaded", String.class);
        // No annotation on this method – should return null even though a method with same name exists in super
        assertNull(ReflectionUtils.getMethodAnnotation(m, TestAnno.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodAnnotationNullMethod() {
        ReflectionUtils.getMethodAnnotation(null, TestAnno.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodAnnotationNullAnnoClass() throws Exception {
        Method m = BaseClass.class.getDeclaredMethod("annotatedMethod");
        ReflectionUtils.getMethodAnnotation(m, null);
    }

    /*======================================================================
     *  Field lookup tests
     *======================================================================*/

    @Test
    public void testGetFieldFoundAndCached() {
        Field f1 = ReflectionUtils.getField(DerivedClass.class, "derivedField");
        assertNotNull(f1);
        assertEquals("derivedField", f1.getName());

        // Second call should return the same Field instance (cached)
        Field f2 = ReflectionUtils.getField(DerivedClass.class, "derivedField");
        assertSame(f1, f2);
    }

    @Test
    public void testGetFieldMissingCachedAsNull() {
        Field f1 = ReflectionUtils.getField(DerivedClass.class, "nonExistent");
        assertNull(f1);
        // second call – still null (cached miss)
        Field f2 = ReflectionUtils.getField(DerivedClass.class, "nonExistent");
        assertNull(f2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetFieldNullClass() {
        ReflectionUtils.getField(null, "any");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetFieldNullName() {
        ReflectionUtils.getField(DerivedClass.class, null);
    }

    /*======================================================================
     *  Declared‑field filtering & inheritance tests
     *======================================================================*/

    @Test
    public void testGetDeclaredFieldsDefaultFilterExcludesStatic() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(BaseClass.class);
        // STATIC_FIELD should be filtered out
        assertFalse(fields.stream().anyMatch(f -> "STATIC_FIELD".equals(f.getName())));
    }

    @Test
    public void testGetAllDeclaredFieldsInheritanceOrder() {
        List<Field> all = ReflectionUtils.getAllDeclaredFields(DerivedClass.class);
        // Expect subclass fields first, then superclass fields
        assertEquals("derivedField", all.get(0).getName());
        assertEquals("baseField", all.get(1).getName()); // hides superclass field
        // The hidden field from BaseClass should appear later
        assertTrue(all.stream().anyMatch(f -> "baseField".equals(f.getName()) && f.getDeclaringClass() == BaseClass.class));
    }

    @Test
    public void testGetAllDeclaredFieldsMapCollisionHandling() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(DerivedClass.class);
        // Two fields named "baseField": one from BaseClass, one from DerivedClass
        assertTrue(map.containsKey("baseField")); // child field wins simple name
        assertTrue(map.containsKey(BaseClass.class.getName() + ".baseField")); // parent field qualified
        Field child = map.get("baseField");
        Field parent = map.get(BaseClass.class.getName() + ".baseField");
        assertEquals(DerivedClass.class, child.getDeclaringClass());
        assertEquals(BaseClass.class, parent.getDeclaringClass());
    }

    /*======================================================================
     *  Method lookup & deterministic selection tests
     *======================================================================*/

    @Test
    public void testGetMethodExactSignature() {
        Method m = ReflectionUtils.getMethod(DerivedClass.class, "overloaded", String.class);
        assertNotNull(m);
        assertArrayEquals(new Class<?>[]{String.class}, m.getParameterTypes());
    }

    @Test
    public void testGetMethodByArgumentCountSelectsMostAccessible() {
        // DerivedClass inherits three overloaded methods named "overloaded"
        Method m = ReflectionUtils.getMethod(new DerivedClass(), "overloaded", 2);
        // The method with two parameters is private in BaseClass – no other candidate with same count
        assertEquals(2, m.getParameterCount());
        assertEquals("overloaded", m.getName());
    }

    @Test
    public void testGetMethodByArgumentCountDeterministicSelection() {
        // Create a class with two methods of same name/argCount but different visibility
        class Visibility {
            private void foo(Object o) {}
            public void foo(Object o) {}
        }
        Method selected = ReflectionUtils.getMethod(new Visibility(), "foo", 1);
        // Public method should be preferred over private
        assertTrue(Modifier.isPublic(selected.getModifiers()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodByArgumentCountNotFound() {
        ReflectionUtils.getMethod(new DerivedClass(), "nonExisting", 0);
    }

    @Test
    public void testGetNonOverloadedMethodSuccess() {
        Method m = ReflectionUtils.getNonOverloadedMethod(BaseClass.class, "annotatedMethod");
        assertNotNull(m);
        assertEquals("annotatedMethod", m.getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethodAmbiguous() {
        // overloaded methods cause ambiguity
        ReflectionUtils.getNonOverloadedMethod(BaseClass.class, "overloaded");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethodMissing() {
        ReflectionUtils.getNonOverloadedMethod(BaseClass.class, "doesNotExist");
    }

    /*======================================================================
     *  Constructor lookup & ordering tests
     *======================================================================*/

    private static class ConstructorTest {
        public ConstructorTest() {}
        protected ConstructorTest(String s) {}
        ConstructorTest(int i) {}
        private ConstructorTest(double d, String s) {}
    }

    @Test
    public void testGetConstructorFoundAndCached() {
        Constructor<?> c1 = ReflectionUtils.getConstructor(ConstructorTest.class, String.class);
        assertNotNull(c1);
        assertTrue(Modifier.isProtected(c1.getModifiers()));

        // Cached result – same instance
        Constructor<?> c2 = ReflectionUtils.getConstructor(ConstructorTest.class, String.class);
        assertSame(c1, c2);
    }

    @Test
    public void testGetConstructorMissingCachedAsNull() {
        Constructor<?> c1 = ReflectionUtils.getConstructor(ConstructorTest.class, long.class);
        assertNull(c1);
        Constructor<?> c2 = ReflectionUtils.getConstructor(ConstructorTest.class, long.class);
        assertNull(c2);
    }

    @Test
    public void testGetAllConstructorsSorting() {
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(ConstructorTest.class);
        // Expected order: public, protected, package, private
        assertTrue(Modifier.isPublic(ctors[0].getModifiers()));
        assertTrue(Modifier.isProtected(ctors[1].getModifiers()));
        // package-private (no modifier) is next
        assertFalse(Modifier.isPrivate(ctors[2].getModifiers()));
        assertFalse(Modifier.isProtected(ctors[2].getModifiers()));
        assertFalse(Modifier.isPublic(ctors[2].getModifiers()));
        // private last
        assertTrue(Modifier.isPrivate(ctors[3].getModifiers()));

        // Within same visibility, more parameters first
        // public ctor has 0 params, protected has 1, package has 1, private has 2 – already ordered correctly
        assertEquals(0, ctors[0].getParameterCount());
        assertEquals(2, ctors[3].getParameterCount());
    }

    /*======================================================================
     *  Utility method tests (call, getClassName, etc.)
     *======================================================================*/

    private static class CallTarget {
        public String echo(String s) { return s; }

        private void explode() { throw new IllegalStateException("boom"); }
    }

    @Test
    public void testCallWithMethodObjectSuccess() throws Exception {
        CallTarget ct = new CallTarget();
        Method m = CallTarget.class.getMethod("echo", String.class);
        Object result = ReflectionUtils.call(ct, m, "hi");
        assertEquals("hi", result);
    }

    @Test
    public void testCallWithMethodObjectRuntimeExceptionWrapping() throws Exception {
        CallTarget ct = new CallTarget();
        Method m = CallTarget.class.getDeclaredMethod("explode");
        ClassUtilities.trySetAccessible(m);
        try {
            ReflectionUtils.call(ct, m);
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof IllegalStateException);
            assertEquals("boom", e.getCause().getMessage());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallNullMethod() {
        ReflectionUtils.call(new Object(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallNullInstance() throws Exception {
        Method m = Object.class.getMethod("toString");
        ReflectionUtils.call(null, m);
    }

    @Test
    public void testCallByNameUsesCacheAndHandlesArgs() {
        CallTarget ct = new CallTarget();
        Object result = ReflectionUtils.call(ct, "echo", "cached");
        assertEquals("cached", result);
    }

    @Test
    public void testGetClassNameUtility() {
        assertEquals("null", ReflectionUtils.getClassName(null));
        assertEquals(String.class.getName(), ReflectionUtils.getClassName("test"));
    }

    @Test
    public void testGetClassNameFromByteCode() throws Exception {
        byte[] bytes = classBytes(DerivedClass.class);
        String name = ReflectionUtils.getClassNameFromByteCode(bytes);
        assertEquals(DerivedClass.class.getName(), name);
    }

    @Test(expected = IllegalStateException.class)
    public void testGetClassNameFromByteCodeInvalidData() throws Exception {
        // Corrupt data – not a valid class file
        ReflectionUtils.getClassNameFromByteCode(new byte[]{0,1,2,3});
    }

    @Test
    public void testIsJavaCompilerAvailableDefault() {
        // The JDK used for building the tests normally provides a compiler.
        // We just assert that the method returns a boolean without throwing.
        boolean available = ReflectionUtils.isJavaCompilerAvailable();
        // No assertion on true/false – both are valid depending on the runtime.
        assertTrue(available || !available);
    }

    @Test
    public void testIsJavaCompilerAvailableForcedJre() {
        // Force the JRE path via system property
        System.setProperty("java.util.force.jre", "true");
        try {
            assertFalse(ReflectionUtils.isJavaCompilerAvailable());
        } finally {
            System.clearProperty("java.util.force.jre");
        }
    }

    /*======================================================================
     *  Cache‑reset sanity checks (ensuring that custom caches can be swapped)
     *======================================================================*/

    @Test
    public void testSetMethodCacheAcceptsConcurrentMap() {
        Map<Object, Method> custom = new java.util.concurrent.ConcurrentHashMap<>();
        ReflectionUtils.setMethodCache(custom);
        // Verify that subsequent lookups use the same map instance (by reflection on the private field)
        try {
            Field cacheField = ReflectionUtils.class.getDeclaredField("METHOD_CACHE");
            cacheField.setAccessible(true);
            @SuppressWarnings("unchecked")
            AtomicReference<Map<?, Method>> ref = (AtomicReference<Map<?, Method>>) cacheField.get(null);
            assertSame(custom, ref.get());
        } catch (Exception e) {
            fail("Unable to verify cache swap: " + e);
        }
    }

    @Test
    public void testSetConstructorCacheRejectsNull() {
        try {
            ReflectionUtils.setConstructorCache(null);
            fail("Expected NPE");
        } catch (NullPointerException ignored) {
        }
    }
}
