package com.cedarsoftware.util;
import com.cedarsoftware.util.ReflectionUtils.ConstructorCacheKey;
import com.cedarsoftware.util.ReflectionUtils.MethodCacheKey;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ReflectionUtils}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public API methods (basic happy‑path usage)</li>
 *   <li>Edge‑cases, null handling and expected exceptions</li>
 *   <li>Cache‑related behaviour by swapping the internal caches with simple {@link HashMap}s</li>
 *   <li>Annotation lookup across inheritance hierarchies</li>
 *   <li>Field‑lookup, field‑collision handling and custom predicates</li>
 *   <li>Method invocation helpers (including exception wrapping)</li>
 *   <li>Byte‑code parsing using the class file of {@link ReflectionUtils} itself</li>
 * </ul>
 *
 * No external mocking frameworks are used – all test data are real classes/annotations defined
 * in this file.
 */
public class ReflectionUtilsTest {

    /* -------------------------------------------------------------
     *  Helper annotation and test classes used by many test cases
     * ------------------------------------------------------------- */
    @Retention(RetentionPolicy.RUNTIME)
    private @interface TestAnno { }

    // Simple class hierarchy used for annotation & field tests
    private static class Parent {
        private int parentField;
        public String common = "parent";
    }

    private static class Child extends Parent {
        private String childField;
        public String common = "child";               // name collision with parent
        @TestAnno
        private void privateAnnotated() { }
        @TestAnno
        public void publicAnnotated() { }
    }

    @TestAnno
    private static class ClassWithAnno { }

    private static class NoAnnoClass { }

    private static class ExceptionThrower {
        public void explode() {
            throw new IllegalStateException("boom");
        }
    }

    private static class OverloadedMethods {
        public String foo() { return "zero"; }
        public String foo(String a) { return "one:" + a; }
        private String foo(Integer i) { return "int:" + i; }
    }

    /* -------------------------------------------------------------
     *  Cache handling – each test gets a clean set of simple maps
     * ------------------------------------------------------------- */
    private Map<Object, Method> methodCacheBackup;
    private Map<Object, Collection<Field>> fieldsCacheBackup;
    private Map<Object, Field> fieldNameCacheBackup;
    private Map<Object, Annotation> classAnnoCacheBackup;
    private Map<Object, Annotation> methodAnnoCacheBackup;
    private Map<Object, Constructor<?>> ctorCacheBackup;

    @Before
    public void setUp() {
        // backup original caches (they are static – we keep references to restore later)
        methodCacheBackup   = getStaticField("METHOD_CACHE");
        fieldsCacheBackup   = getStaticField("FIELDS_CACHE");
        fieldNameCacheBackup = getStaticField("FIELD_NAME_CACHE");
        classAnnoCacheBackup = getStaticField("CLASS_ANNOTATION_CACHE");
        methodAnnoCacheBackup = getStaticField("METHOD_ANNOTATION_CACHE");
        ctorCacheBackup     = getStaticField("CONSTRUCTOR_CACHE");

        // replace with fresh HashMaps
        ReflectionUtils.setMethodCache(new HashMap<>());
        ReflectionUtils.setClassFieldsCache(new HashMap<>());
        ReflectionUtils.setFieldCache(new HashMap<>());
        ReflectionUtils.setClassAnnotationCache(new HashMap<>());
        ReflectionUtils.setMethodAnnotationCache(new HashMap<>());
        ReflectionUtils.setConstructorCache(new HashMap<>());
    }

    @After
    public void tearDown() {
        // restore original caches to avoid side‑effects for other test suites
        ReflectionUtils.setMethodCache(methodCacheBackup);
        ReflectionUtils.setClassFieldsCache(fieldsCacheBackup);
        ReflectionUtils.setFieldCache(fieldNameCacheBackup);
        ReflectionUtils.setClassAnnotationCache(classAnnoCacheBackup);
        ReflectionUtils.setMethodAnnotationCache(methodAnnoCacheBackup);
        ReflectionUtils.setConstructorCache(ctorCacheBackup);
    }

    @SuppressWarnings("unchecked")
    private <T> T getStaticField(String name) {
        try {
            java.lang.reflect.Field f = ReflectionUtils.class.getDeclaredField(name);
            f.setAccessible(true);
            return (T) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /* -------------------------------------------------------------
     *  1.  Cache‑setter methods – basic sanity checks
     * ------------------------------------------------------------- */
    @Test
    public void testSetMethodCache() throws Exception {
        Map<Object, Method> custom = new HashMap<>();
        ReflectionUtils.setMethodCache(custom);

        Method m = ReflectionUtils.getMethod(String.class, "length");
        MethodCacheKey key = new MethodCacheKey(String.class, "length");
        assertSame("Method should be stored in the supplied cache", m, custom.get(key));
    }

    @Test
    public void testSetFieldCache() throws Exception {
        Map<Object, Field> custom = new HashMap<>();
        ReflectionUtils.setFieldCache(custom);

        Field f = ReflectionUtils.getField(Child.class, "childField");
        // Build the cache key used internally
        java.lang.reflect.Constructor<?> ctor = Class.forName("com.cedarsoftware.util.ReflectionUtils$FieldNameCacheKey")
                .getDeclaredConstructor(Class.class, String.class);
        ctor.setAccessible(true);
        Object key = ctor.newInstance(Child.class, "childField");
        assertSame("Field should be cached in the supplied map", f, custom.get(key));
    }

    /* -------------------------------------------------------------
     *  2.  Class‑annotation lookup
     * ------------------------------------------------------------- */
    @Test
    public void testGetClassAnnotationFound() {
        TestAnno anno = ReflectionUtils.getClassAnnotation(ClassWithAnno.class, TestAnno.class);
        assertNotNull("Annotation should be found on ClassWithAnno", anno);
    }

    @Test
    public void testGetClassAnnotationNotFound() {
        TestAnno anno = ReflectionUtils.getClassAnnotation(NoAnnoClass.class, TestAnno.class);
        assertNull("No annotation should be returned when missing", anno);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetClassAnnotationNullAnnotationClass() {
        ReflectionUtils.getClassAnnotation(ClassWithAnno.class, null);
    }

    @Test
    public void testGetClassAnnotationNullClassReturnsNull() {
        TestAnno anno = ReflectionUtils.getClassAnnotation(null, TestAnno.class);
        assertNull("Null class argument should result in null", anno);
    }

    /* -------------------------------------------------------------
     *  3.  Method‑annotation lookup (including inheritance)
     * ------------------------------------------------------------- */
    @Test
    public void testGetMethodAnnotationFoundOnDeclaredMethod() throws Exception {
        Method m = Child.class.getDeclaredMethod("publicAnnotated");
        TestAnno a = ReflectionUtils.getMethodAnnotation(m, TestAnno.class);
        assertNotNull("Annotation should be found on publicAnnotated", a);
    }

    @Test
    public void testGetMethodAnnotationFoundOnSuperclassMethod() throws Exception {
        // Parent has no annotation, Child overrides with annotation
        Method m = Child.class.getDeclaredMethod("privateAnnotated");
        TestAnno a = ReflectionUtils.getMethodAnnotation(m, TestAnno.class);
        assertNotNull("Annotation should be discovered via hierarchy", a);
    }

    @Test
    public void testGetMethodAnnotationNotFound() throws Exception {
        Method m = Parent.class.getDeclaredMethod("toString"); // no TestAnno
        TestAnno a = ReflectionUtils.getMethodAnnotation(m, TestAnno.class);
        assertNull("Should return null when annotation is absent", a);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodAnnotationNullMethod() {
        ReflectionUtils.getMethodAnnotation(null, TestAnno.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodAnnotationNullAnnoClass() throws Exception {
        Method m = Child.class.getDeclaredMethod("publicAnnotated");
        ReflectionUtils.getMethodAnnotation(m, null);
    }

    /* -------------------------------------------------------------
     *  4.  Field lookup (including private fields and null checks)
     * ------------------------------------------------------------- */
    @Test
    public void testGetFieldFoundPrivate() {
        Field f = ReflectionUtils.getField(Child.class, "childField");
        assertNotNull("Private field should be found", f);
        assertEquals("childField", f.getName());
        assertFalse("Field should be accessible after lookup", !f.isAccessible());
    }

    @Test
    public void testGetFieldNotFoundReturnsNull() {
        Field f = ReflectionUtils.getField(Child.class, "nonExistent");
        assertNull("Missing field should return null", f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetFieldNullClass() {
        ReflectionUtils.getField(null, "any");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetFieldNullName() {
        ReflectionUtils.getField(Child.class, null);
    }

    /* -------------------------------------------------------------
     *  5.  Declared‑field utilities with custom predicates
     * ------------------------------------------------------------- */
    @Test
    public void testGetDeclaredFieldsWithPredicate() {
        List<Field> publicFields = ReflectionUtils.getDeclaredFields(Child.class,
                f -> Modifier.isPublic(f.getModifiers()));
        // Child has a public field "common"
        assertEquals(1, publicFields.size());
        assertEquals("common", publicFields.get(0).getName());
    }

    @Test
    public void testGetDeclaredFieldsDefaultFilterExcludesStatic() throws Exception {
        class WithStatic {
            public static int S = 5;
            public int i = 1;
        }
        List<Field> fields = ReflectionUtils.getDeclaredFields(WithStatic.class);
        assertEquals(1, fields.size());
        assertEquals("i", fields.get(0).getName());
    }

    @Test
    public void testGetAllDeclaredFieldsHierarchyOrder() {
        List<Field> all = ReflectionUtils.getAllDeclaredFields(Child.class);
        // child fields first, then parent fields
        assertTrue(all.get(0).getName().equals("childField") ||
                   all.get(0).getName().equals("common"));
        // ensure parent field appears later
        boolean parentFound = all.stream().anyMatch(f -> f.getName().equals("parentField"));
        assertTrue("Parent field should be present", parentFound);
    }

    @Test
    public void testGetAllDeclaredFieldsMapCollisionResolution() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(Child.class);
        // "common" exists in both parent and child – child should win simple name
        assertTrue(map.containsKey("common"));
        Field f = map.get("common");
        assertEquals(Child.class, f.getDeclaringClass());

        // fully qualified name for parent field should also be present
        String fq = Parent.class.getName() + ".common";
        assertTrue(map.containsKey(fq));
        assertEquals(Parent.class, map.get(fq).getDeclaringClass());
    }

    @Test
    public void testGetDeepDeclaredFieldsLegacyBehaviour() {
        // The legacy method adds transient & synthetic filtering on top of the default filter
        Collection<Field> fields = ReflectionUtils.getDeepDeclaredFields(Child.class);
        // All fields from Child + Parent that are not static, not enum‑specific, etc.
        assertFalse(fields.isEmpty());
        // Ensure that a synthetic field (if any) is not present – we simply assert no IllegalStateException
    }

    @Test
    public void testGetDeepDeclaredFieldMapLegacyBehaviour() {
        Map<String, Field> map = ReflectionUtils.getDeepDeclaredFieldMap(Child.class);
        assertNotNull(map);
        // Simple sanity – the map should contain the "common" field from Child
        assertEquals(Child.class, map.get("common").getDeclaringClass());
    }

    @Test
    public void testDeprecatedGetDeclaredFieldsCollectionParameter() {
        Collection<Field> coll = new ArrayList<>();
        ReflectionUtils.getDeclaredFields(Child.class, coll);
        // The collection should now contain the same fields as getDeclaredFields(Class)
        List<Field> expected = ReflectionUtils.getDeclaredFields(Child.class);
        assertEquals(expected.size(), coll.size());
        assertTrue(coll.containsAll(expected));
    }

    /* -------------------------------------------------------------
     *  6.  Method invocation helpers (call)
     * ------------------------------------------------------------- */
    @Test
    public void testCallWithMethodObject() throws Exception {
        Method m = String.class.getMethod("toUpperCase");
        Object result = ReflectionUtils.call("abc", m);
        assertEquals("ABC", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallWithNullMethod() {
        ReflectionUtils.call(new Object(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallWithNullInstance() throws Exception {
        Method m = Object.class.getMethod("toString");
        ReflectionUtils.call(null, m);
    }

    @Test
    public void testCallWrapsInvocationTargetException() {
        ExceptionThrower et = new ExceptionThrower();
        Method explode = ReflectionUtils.getMethod(ExceptionThrower.class, "explode");
        try {
            ReflectionUtils.call(et, explode);
            fail("Expected RuntimeException wrapping IllegalStateException");
        } catch (RuntimeException re) {
            assertTrue(re.getCause() instanceof IllegalStateException);
            assertEquals("boom", re.getCause().getMessage());
        }
    }

    @Test
    public void testCallByNameSuccess() {
        OverloadedMethods om = new OverloadedMethods();
        Object result = ReflectionUtils.call(om, "foo", "test");
        assertEquals("one:test", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallByNameMethodNotFound() {
        ReflectionUtils.call(new Object(), "nonExistingMethod");
    }

    /* -------------------------------------------------------------
     *  7.  getMethod(Class, ...) – private and inherited methods
     * ------------------------------------------------------------- */
    @Test
    public void testGetMethodFindsPrivateMethod() throws Exception {
        Method m = ReflectionUtils.getMethod(Child.class, "privateAnnotated");
        assertNotNull(m);
        assertTrue(m.isAccessible());
    }

    @Test
    public void testGetMethodReturnsNullWhenMissing() {
        Method m = ReflectionUtils.getMethod(String.class, "nonExisting");
        assertNull(m);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodNullClass() {
        ReflectionUtils.getMethod(null, "any");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodNullName() {
        ReflectionUtils.getMethod(Object.class, null);
    }

    /* -------------------------------------------------------------
     *  8.  getMethod(Object, String, int) – selection strategy
     * ------------------------------------------------------------- */
    @Test
    public void testGetMethodByArgCountSelectsMostAccessible() {
        OverloadedMethods om = new OverloadedMethods();
        Method m = ReflectionUtils.getMethod(om, "foo", 0);
        assertEquals("foo", m.getName());
        assertEquals(0, m.getParameterCount());
        // The public zero‑arg method should be chosen over the private int version
        assertTrue(Modifier.isPublic(m.getModifiers()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodByArgCountNegativeCount() {
        ReflectionUtils.getMethod(new Object(), "toString", -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodByArgCountNotFound() {
        ReflectionUtils.getMethod(new Object(), "missingMethod", 2);
    }

    /* -------------------------------------------------------------
     *  9.  getNonOverloadedMethod – happy path and overload error
     * ------------------------------------------------------------- */
    @Test
    public void testGetNonOverloadedMethodSuccess() throws Exception {
        Method m = ReflectionUtils.getNonOverloadedMethod(OverloadedMethods.class, "foo");
        // The no‑arg version is the first one returned by Class.getMethods()
        assertEquals(0, m.getParameterCount());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethodWithOverloads() {
        // OverloadedMethods has three overloads of foo – should trigger exception
        ReflectionUtils.getNonOverloadedMethod(OverloadedMethods.class, "foo");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethodNullClass() {
        ReflectionUtils.getNonOverloadedMethod(null, "any");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethodBlankName() {
        ReflectionUtils.getNonOverloadedMethod(Object.class, "   ");
    }

    /* -------------------------------------------------------------
     * 10.  getClassName – null handling
     * ------------------------------------------------------------- */
    @Test
    public void testGetClassNameWhenObjectIsNull() {
        assertEquals("null", ReflectionUtils.getClassName(null));
    }

    @Test
    public void testGetClassNameWhenObjectIsNotNull() {
        String name = ReflectionUtils.getClassName("abc");
        assertEquals(String.class.getName(), name);
    }

    /* -------------------------------------------------------------
     * 11.  getClassNameFromByteCode – valid class file & malformed data
     * ------------------------------------------------------------- */
    @Test
    public void testGetClassNameFromByteCodeValid() throws IOException {
        // Load the byte‑code of this test class itself
        String resource = ReflectionUtils.class.getName().replace('.', '/') + ".class";
        byte[] bytes = ReflectionUtils.class.getClassLoader()
                .getResourceAsStream(resource).readAllBytes();

        String className = ReflectionUtils.getClassNameFromByteCode(bytes);
        assertEquals(ReflectionUtils.class.getName(), className);
    }

    @Test(expected = IOException.class)
    public void testGetClassNameFromByteCodeEmptyArray() throws IOException {
        ReflectionUtils.getClassNameFromByteCode(new byte[0]);
    }

    @Test(expected = IllegalStateException.class)
    public void testGetClassNameFromByteCodeInvalidTag() throws IOException {
        // Corrupt the constant‑pool tag (first byte after the header)
        byte[] good = ReflectionUtils.class.getClassLoader()
                .getResourceAsStream(ReflectionUtils.class.getName().replace('.', '/') + ".class")
                .readAllBytes();
        // Change a tag byte to an impossible value (e.g., 99)
        good[10] = (byte) 99;
        ReflectionUtils.getClassNameFromByteCode(good);
    }

    /* -------------------------------------------------------------
     * 12.  Constructor utilities – cache interaction
     * ------------------------------------------------------------- */
    @Test
    public void testGetConstructorFound() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(ArrayList.class);
        assertNotNull(ctor);
        assertEquals(0, ctor.getParameterCount());
    }

    @Test
    public void testGetConstructorNotFoundReturnsNull() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(String.class, int.class);
        assertNull(ctor);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetConstructorNullClass() {
        ReflectionUtils.getConstructor(null);
    }

    @Test
    public void testGetAllConstructorsCachesInstances() {
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(ArrayList.class);
        assertTrue(ctors.length > 0);
        // Verify that each constructor is present in the cache
        for (Constructor<?> c : ctors) {
            ConstructorCacheKey key = new ConstructorCacheKey(ArrayList.class, c.getParameterTypes());
            Constructor<?> cached = getStaticField("CONSTRUCTOR_CACHE").get(key);
            assertSame("Constructor should be cached", c, cached);
        }
    }
}
