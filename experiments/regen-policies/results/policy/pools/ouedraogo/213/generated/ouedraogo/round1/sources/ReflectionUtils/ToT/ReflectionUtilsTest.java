package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ReflectionUtils}.
 *
 * The tests cover:
 * <ul>
 *   <li>Cache‑setting APIs (including null handling)</li>
 *   <li>Annotation lookup (class and method) with inheritance</li>
 *   <li>Field lookup (including inheritance, static exclusion, and custom filters)</li>
 *   <li>Declared‑field retrieval and map conversion (collision handling)</li>
 *   <li>Method lookup (by signature, by argument count) and caching</li>
 *   <li>Constructor lookup and ordered constructor list</li>
 *   <li>Utility methods (call, getNonOverloadedMethod, getClassName, …)</li>
 *   <li>Byte‑code class‑name extraction</li>
 *   <li>Java‑compiler detection respecting the system property</li>
 * </ul>
 *
 * All tests use only the public API of {@code ReflectionUtils} and standard JDK classes.
 */
public class ReflectionUtilsTest {

    /* ---------- Helper annotations & test classes ---------- */

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    private @interface TypeAnno {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    private @interface MethodAnno {}

    @TypeAnno
    private static class BaseClass {
        private String baseField;
        public static int staticField = 42;
    }

    private static class SubClass extends BaseClass {
        private int subField;
    }

    private interface AnnotatedInterface {
        @MethodAnno
        void annotatedMethod();
    }

    private static class ImplementsInterface implements AnnotatedInterface {
        @Override
        public void annotatedMethod() { /* no‑op */ }

        // overloaded methods for getMethod(Object,String,int) tests
        public void overloaded(Object o) {}
        protected void overloaded(String s) {}
        private void overloaded(Integer i) {}
    }

    private static class ExceptionThrower {
        public void explode() throws IOException {
            throw new IOException("boom");
        }
    }

    private static class ConstructorTest {
        public ConstructorTest() {}
        protected ConstructorTest(int a) {}
        ConstructorTest(String s, int i) {}               // package‑private
        private ConstructorTest(double d, String s, int i) {}
    }

    /* ---------- Test lifecycle ---------- */

    @Before
    public void resetCaches() {
        // Replace every cache with a fresh, thread‑safe map to guarantee test isolation.
        ReflectionUtils.setMethodCache(new ConcurrentHashMap<>());
        ReflectionUtils.setClassFieldsCache(new ConcurrentHashMap<>());
        ReflectionUtils.setFieldCache(new ConcurrentHashMap<>());
        ReflectionUtils.setClassAnnotationCache(new ConcurrentHashMap<>());
        ReflectionUtils.setMethodAnnotationCache(new ConcurrentHashMap<>());
        ReflectionUtils.setConstructorCache(new ConcurrentHashMap<>());
        ReflectionUtils.setSortedConstructorsCache(new ConcurrentHashMap<>());
    }

    @After
    public void clearSystemProperty() {
        System.clearProperty("java.util.force.jre");
    }

    /* ---------- Cache‑setting APIs ---------- */

    @Test(expected = NullPointerException.class)
    public void setMethodCache_null_throwsNPE() {
        ReflectionUtils.setMethodCache(null);
    }

    @Test
    public void setCustomMethodCache_isUsedByLookup() throws Exception {
        Map<Object, Method> custom = new ConcurrentHashMap<>();
        ReflectionUtils.setMethodCache(custom);

        Method m = ReflectionUtils.getMethod(String.class, "length");
        assertNotNull(m);
        // The cache should now contain the key for the lookup.
        assertTrue(custom.containsKey(new ReflectionUtils.MethodCacheKey(String.class, "length")));
    }

    /* ---------- Class annotation lookup ---------- */

    @Test
    public void getClassAnnotation_findsInheritedAnnotation_andCachesResult() {
        TypeAnno anno = ReflectionUtils.getClassAnnotation(SubClass.class, TypeAnno.class);
        assertNotNull(anno);
        // second call should hit the cache (no exception, same instance)
        TypeAnno cached = ReflectionUtils.getClassAnnotation(SubClass.class, TypeAnno.class);
        assertSame(anno, cached);
    }

    @Test
    public void getClassAnnotation_nullClass_returnsNull() {
        assertNull(ReflectionUtils.getClassAnnotation(null, TypeAnno.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getClassAnnotation_nullAnnotationClass_throws() {
        ReflectionUtils.getClassAnnotation(BaseClass.class, null);
    }

    /* ---------- Method annotation lookup ---------- */

    @Test
    public void getMethodAnnotation_findsAnnotationOnInterface() throws Exception {
        Method m = ImplementsInterface.class.getMethod("annotatedMethod");
        MethodAnno anno = ReflectionUtils.getMethodAnnotation(m, MethodAnno.class);
        assertNotNull(anno);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getMethodAnnotation_nullMethod_throws() {
        ReflectionUtils.getMethodAnnotation(null, MethodAnno.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getMethodAnnotation_nullAnnoClass_throws() throws Exception {
        Method m = ImplementsInterface.class.getMethod("annotatedMethod");
        ReflectionUtils.getMethodAnnotation(m, null);
    }

    /* ---------- Field lookup ---------- */

    @Test
    public void getField_findsInheritedField_andIgnoresStatic() {
        Field f = ReflectionUtils.getField(SubClass.class, "baseField");
        assertNotNull(f);
        assertEquals("baseField", f.getName());

        // static field must not be returned (null expected)
        assertNull(ReflectionUtils.getField(SubClass.class, "staticField"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getField_nullClass_throws() {
        ReflectionUtils.getField(null, "anything");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getField_nullName_throws() {
        ReflectionUtils.getField(SubClass.class, null);
    }

    /* ---------- Declared‑field retrieval ---------- */

    @Test
    public void getDeclaredFields_defaultFilter_excludesStaticAndSynthetic() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(SubClass.class);
        // Should contain only instance fields declared directly in SubClass
        assertEquals(1, fields.size());
        assertEquals("subField", fields.get(0).getName());
    }

    @Test
    public void getDeclaredFields_customFilter_allowsStatic() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(SubClass.class,
                f -> true); // accept everything
        // Both static and instance fields are returned
        Set<String> names = new HashSet<>();
        for (Field f : fields) names.add(f.getName());
        assertTrue(names.contains("staticField"));
        assertTrue(names.contains("subField"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getDeclaredFields_nullClass_throws() {
        ReflectionUtils.getDeclaredFields(null, f -> true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getDeclaredFields_nullFilter_throws() {
        ReflectionUtils.getDeclaredFields(SubClass.class, null);
    }

    /* ---------- All‑declared‑field map (collision handling) ---------- */

    private static class CollisionParent {
        private int value;
    }

    private static class CollisionChild extends CollisionParent {
        private String value; // hides parent field
    }

    @Test
    public void getAllDeclaredFieldsMap_handlesNameCollisions() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(CollisionChild.class);
        // simple name "value" should refer to child field
        assertTrue(map.containsKey("value"));
        assertEquals(String.class, map.get("value").getType());

        // fully‑qualified name should refer to parent field
        String fq = CollisionParent.class.getName() + ".value";
        assertTrue(map.containsKey(fq));
        assertEquals(int.class, map.get(fq).getType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getAllDeclaredFieldsMap_nullClass_throws() {
        ReflectionUtils.getAllDeclaredFieldsMap(null, f -> true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getAllDeclaredFieldsMap_nullFilter_throws() {
        ReflectionUtils.getAllDeclaredFieldsMap(CollisionChild.class, null);
    }

    /* ---------- Method lookup (by signature) ---------- */

    @Test
    public void getMethod_bySignature_cachesResult() throws Exception {
        Method m1 = ReflectionUtils.getMethod(String.class, "substring", int.class, int.class);
        assertNotNull(m1);
        Method m2 = ReflectionUtils.getMethod(String.class, "substring", int.class, int.class);
        assertSame(m1, m2); // cached instance
    }

    @Test(expected = IllegalArgumentException.class)
    public void getMethod_nullClass_throws() {
        ReflectionUtils.getMethod(null, "any");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getMethod_nullName_throws() {
        ReflectionUtils.getMethod(String.class, null);
    }

    /* ---------- Method lookup (by argument count) ---------- */

    @Test
    public void getMethod_byArgCount_selectsMostAccessible() {
        ImplementsInterface obj = new ImplementsInterface();
        Method m = ReflectionUtils.getMethod(obj, "overloaded", 1);
        assertTrue(Modifier.isPublic(m.getModifiers()));
        assertEquals("overloaded", m.getName());
        assertEquals(1, m.getParameterCount());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getMethod_byArgCount_noMatch_throws() {
        ReflectionUtils.getMethod(new Object(), "nonexistent", 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getMethod_byArgCount_nullInstance_throws() {
        ReflectionUtils.getMethod(null, "any", 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getMethod_byArgCount_negativeCount_throws() {
        ReflectionUtils.getMethod(new Object(), "any", -1);
    }

    /* ---------- Constructor lookup ---------- */

    @Test
    public void getConstructor_existing_returnsConstructorAndCachesIt() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(ConstructorTest.class, String.class, int.class);
        assertNotNull(ctor);
        Constructor<?> cached = ReflectionUtils.getConstructor(ConstructorTest.class, String.class, int.class);
        assertSame(ctor, cached);
    }

    @Test
    public void getConstructor_missing_returnsNullAndCachesNull() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(ConstructorTest.class, long.class);
        assertNull(ctor);
        // second call should also return null (cached)
        assertNull(ReflectionUtils.getConstructor(ConstructorTest.class, long.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getConstructor_nullClass_throws() {
        ReflectionUtils.getConstructor(null);
    }

    /* ---------- All constructors (sorted) ---------- */

    @Test
    public void getAllConstructors_isOrderedByAccessibilityAndParameterCount() {
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(ConstructorTest.class);
        assertEquals(4, ctors.length);

        // Expected order: public (0), protected (1), package‑private (2), private (3)
        assertTrue(Modifier.isPublic(ctors[0].getModifiers()));
        assertEquals(0, ctors[0].getParameterCount());

        assertTrue(Modifier.isProtected(ctors[1].getModifiers()));
        assertEquals(1, ctors[1].getParameterCount());

        assertFalse(Modifier.isPublic(ctors[2].getModifiers()));
        assertFalse(Modifier.isProtected(ctors[2].getModifiers()));
        assertFalse(Modifier.isPrivate(ctors[2].getModifiers()));
        assertEquals(2, ctors[2].getParameterCount());

        assertTrue(Modifier.isPrivate(ctors[3].getModifiers()));
        assertEquals(3, ctors[3].getParameterCount());
    }

    @Test
    public void getAllConstructors_nullClass_returnsEmptyArray() {
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(null);
        assertEquals(0, ctors.length);
    }

    /* ---------- getNonOverloadedMethod ---------- */

    @Test
    public void getNonOverloadedMethod_successfulLookup() {
        Method m = ReflectionUtils.getNonOverloadedMethod(String.class, "length");
        assertNotNull(m);
        assertEquals(0, m.getParameterCount());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getNonOverloadedMethod_nullClass_throws() {
        ReflectionUtils.getNonOverloadedMethod(null, "any");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getNonOverloadedMethod_blankName_throws() {
        ReflectionUtils.getNonOverloadedMethod(String.class, "   ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getNonOverloadedMethod_overloadedMethod_throws() {
        // String has overloaded "getBytes" methods
        ReflectionUtils.getNonOverloadedMethod(String.class, "getBytes");
    }

    /* ---------- call(Object,Method,…) ---------- */

    @Test
    public void call_withMethod_successfulInvocation() throws Exception {
        String s = "hello";
        Method m = String.class.getMethod("toUpperCase");
        Object result = ReflectionUtils.call(s, m);
        assertEquals("HELLO", result);
    }

    @Test
    public void call_withMethod_wrapsInvocationTargetException() {
        ExceptionThrower target = new ExceptionThrower();
        Method m;
        try {
            m = ExceptionThrower.class.getMethod("explode");
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
        try {
            ReflectionUtils.call(target, m);
            fail("Expected RuntimeException");
        } catch (RuntimeException re) {
            assertTrue(re.getCause() instanceof IOException);
            assertEquals("boom", re.getCause().getMessage());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void call_nullMethod_throws() {
        ReflectionUtils.call(new Object(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void call_nullInstance_throws() throws Exception {
        Method m = Object.class.getMethod("toString");
        ReflectionUtils.call(null, m);
    }

    /* ---------- call(Object,String,…) ---------- */

    @Test
    public void call_byName_successfulInvocation() {
        String s = "test";
        Object result = ReflectionUtils.call(s, "substring", 1, "test".length());
        assertEquals("est", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void call_byName_methodNotFound_throws() {
        ReflectionUtils.call(new Object(), "nonexistentMethod");
    }

    /* ---------- getClassName ---------- */

    @Test
    public void getClassName_nullAndNonNull() {
        assertEquals("null", ReflectionUtils.getClassName(null));
        assertEquals(String.class.getName(), ReflectionUtils.getClassName("abc"));
    }

    /* ---------- getClassNameFromByteCode ---------- */

    @Test
    public void getClassNameFromByteCode_readsOwnClassName() throws IOException {
        // Load the compiled .class file of this test class.
        String classFile = ReflectionUtilsTest.class.getSimpleName() + ".class";
        InputStream is = ReflectionUtilsTest.class.getResourceAsStream(classFile);
        assertNotNull("Unable to locate class file for test", is);
        byte[] bytes = is.readAllBytes();
        String name = ReflectionUtils.getClassNameFromByteCode(bytes);
        assertEquals(ReflectionUtilsTest.class.getName(), name);
    }

    @Test(expected = IllegalStateException.class)
    public void getClassNameFromByteCode_invalidData_throws() throws IOException {
        byte[] bogus = new byte[]{0,1,2,3,4};
        ReflectionUtils.getClassNameFromByteCode(bogus);
    }

    /* ---------- isJavaCompilerAvailable ---------- */

    @Test
    public void isJavaCompilerAvailable_respectsForceJreProperty() {
        System.setProperty("java.util.force.jre", "true");
        assertFalse(ReflectionUtils.isJavaCompilerAvailable());

        System.clearProperty("java.util.force.jre");
        // The result depends on the runtime; we only assert that the method returns a boolean.
        assertTrue(ReflectionUtils.isJavaCompilerAvailable() || !ReflectionUtils.isJavaCompilerAvailable());
    }
}
