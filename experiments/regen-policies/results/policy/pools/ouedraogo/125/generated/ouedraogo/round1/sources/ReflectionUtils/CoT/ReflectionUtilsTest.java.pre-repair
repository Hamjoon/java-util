package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
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
 * <p>
 * The tests cover:
 * <ul>
 *   <li>All public API methods (basic functionality)</li>
 *   <li>Edge‑case and exception handling (null arguments, overload ambiguities, etc.)</li>
 *   <li>Caching behaviour (same object returned on repeated look‑ups)</li>
 *   <li>Byte‑code parsing via {@code getClassNameFromByteCode}</li>
 * </ul>
 */
public class ReflectionUtilsTest {

    /* --------------------------------------------------------------------- *
     *  Helper annotation and test classes used by many test cases          *
     * --------------------------------------------------------------------- */

    @Retention(RetentionPolicy.RUNTIME)
    private @interface TestAnno { }

    @TestAnno
    private static class AnnotatedBase {
        @TestAnno
        public void baseMethod() { }

        private int baseField;
    }

    @TestAnno
    private static class AnnotatedSub extends AnnotatedBase {
        @TestAnno
        public void subMethod() { }

        private String subField;

        // overloads for getNonOverloadedMethod tests
        public void overloaded() { }
        public void overloaded(String arg) { }
    }

    private static class SimpleClass {
        public String echo(String s) { return s; }

        private int hidden = 42;

        public void throwsChecked() throws Exception {
            throw new Exception("checked");
        }

        public void throwsRuntime() {
            throw new IllegalStateException("runtime");
        }
    }

    /* --------------------------------------------------------------------- *
     *  Reset caches before each test – using the public setters               *
     * --------------------------------------------------------------------- */

    @Before
    public void setUp() {
        ReflectionUtils.setMethodCache(new HashMap<>());
        ReflectionUtils.setClassFieldsCache(new HashMap<>());
        ReflectionUtils.setFieldCache(new HashMap<>());
        ReflectionUtils.setClassAnnotationCache(new HashMap<>());
        ReflectionUtils.setMethodAnnotationCache(new HashMap<>());
        ReflectionUtils.setConstructorCache(new HashMap<>());
    }

    /* --------------------------------------------------------------------- *
     *  1. set*Cache – basic sanity check                                      *
     * --------------------------------------------------------------------- */

    @Test
    public void testSetMethodCache() {
        Map<Object, Method> custom = new HashMap<>();
        ReflectionUtils.setMethodCache(custom);
        // internal state is not visible, but a subsequent lookup must not throw NPE
        Method m = ReflectionUtils.getMethod(SimpleClass.class, "echo", String.class);
        assertNotNull(m);
        assertEquals("echo", m.getName());
    }

    /* --------------------------------------------------------------------- *
     *  2. getClassAnnotation – normal and edge cases                         *
     * --------------------------------------------------------------------- */

    @Test
    public void testGetClassAnnotationFound() {
        TestAnno anno = ReflectionUtils.getClassAnnotation(AnnotatedSub.class, TestAnno.class);
        assertNotNull(anno);
    }

    @Test
    public void testGetClassAnnotationNullClassReturnsNull() {
        TestAnno anno = ReflectionUtils.getClassAnnotation(null, TestAnno.class);
        assertNull(anno);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetClassAnnotationNullAnnoClassThrows() {
        ReflectionUtils.getClassAnnotation(AnnotatedSub.class, null);
    }

    @Test
    public void testGetClassAnnotationCaching() {
        // first call – cache miss
        TestAnno a1 = ReflectionUtils.getClassAnnotation(AnnotatedSub.class, TestAnno.class);
        // second call – must return the same instance (cached)
        TestAnno a2 = ReflectionUtils.getClassAnnotation(AnnotatedSub.class, TestAnno.class);
        assertSame(a1, a2);
    }

    /* --------------------------------------------------------------------- *
     *  3. getMethodAnnotation – normal and edge cases                         *
     * --------------------------------------------------------------------- */

    @Test
    public void testGetMethodAnnotationFound() throws NoSuchMethodException {
        Method m = AnnotatedSub.class.getMethod("subMethod");
        TestAnno anno = ReflectionUtils.getMethodAnnotation(m, TestAnno.class);
        assertNotNull(anno);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodAnnotationNullMethodThrows() {
        ReflectionUtils.getMethodAnnotation(null, TestAnno.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodAnnotationNullAnnoClassThrows() throws NoSuchMethodException {
        Method m = AnnotatedSub.class.getMethod("subMethod");
        ReflectionUtils.getMethodAnnotation(m, null);
    }

    @Test
    public void testGetMethodAnnotationCaching() throws NoSuchMethodException {
        Method m = AnnotatedSub.class.getMethod("subMethod");
        TestAnno a1 = ReflectionUtils.getMethodAnnotation(m, TestAnno.class);
        TestAnno a2 = ReflectionUtils.getMethodAnnotation(m, TestAnno.class);
        assertSame(a1, a2);
    }

    /* --------------------------------------------------------------------- *
     *  4. getField – normal, null checks and inheritance                     *
     * --------------------------------------------------------------------- */

    @Test
    public void testGetFieldInSuperclass() {
        Field f = ReflectionUtils.getField(AnnotatedSub.class, "baseField");
        assertNotNull(f);
        assertEquals("baseField", f.getName());
        assertEquals(AnnotatedBase.class, f.getDeclaringClass());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetFieldNullClassThrows() {
        ReflectionUtils.getField(null, "any");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetFieldNullNameThrows() {
        ReflectionUtils.getField(AnnotatedSub.class, null);
    }

    @Test
    public void testGetFieldCaching() {
        Field f1 = ReflectionUtils.getField(AnnotatedSub.class, "subField");
        Field f2 = ReflectionUtils.getField(AnnotatedSub.class, "subField");
        assertSame(f1, f2);
    }

    /* --------------------------------------------------------------------- *
     *  5. getDeclaredFields (with Predicate) – normal and edge cases         *
     * --------------------------------------------------------------------- */

    @Test
    public void testGetDeclaredFieldsWithPredicate() {
        List<Field> privateFields = ReflectionUtils.getDeclaredFields(AnnotatedSub.class,
                f -> Modifier.isPrivate(f.getModifiers()));
        // Should contain only 'subField' (private) and 'baseField' is not declared in Sub
        assertEquals(1, privateFields.size());
        assertEquals("subField", privateFields.get(0).getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetDeclaredFieldsNullClassThrows() {
        ReflectionUtils.getDeclaredFields(null, f -> true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetDeclaredFieldsNullPredicateThrows() {
        ReflectionUtils.getDeclaredFields(AnnotatedSub.class, null);
    }

    @Test
    public void testGetDeclaredFieldsDefaultFilter() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(AnnotatedSub.class);
        // default filter excludes static fields – there are none; includes private instance fields
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("subField")));
    }

    /* --------------------------------------------------------------------- *
     *  6. getAllDeclaredFields (with Predicate) – hierarchy traversal        *
     * --------------------------------------------------------------------- */

    @Test
    public void testGetAllDeclaredFieldsWithPredicate() {
        List<Field> all = ReflectionUtils.getAllDeclaredFields(AnnotatedSub.class,
                f -> f.getName().endsWith("Field"));
        // Should contain subField and baseField
        Set<String> names = new HashSet<>();
        for (Field f : all) names.add(f.getName());
        assertTrue(names.containsAll(Arrays.asList("subField", "baseField")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetAllDeclaredFieldsNullClassThrows() {
        ReflectionUtils.getAllDeclaredFields(null, f -> true);
    }

    @Test
    public void testGetAllDeclaredFieldsDefaultFilter() {
        List<Field> all = ReflectionUtils.getAllDeclaredFields(AnnotatedSub.class);
        // default filter excludes static fields – still both instance fields present
        Set<String> names = new HashSet<>();
        for (Field f : all) names.add(f.getName());
        assertTrue(names.containsAll(Arrays.asList("subField", "baseField")));
    }

    /* --------------------------------------------------------------------- *
     *  7. getAllDeclaredFieldsMap – default and custom predicates            *
     * --------------------------------------------------------------------- */

    @Test
    public void testGetAllDeclaredFieldsMapDefault() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(AnnotatedSub.class);
        // Both fields should be present, simple names because no collision
        assertTrue(map.containsKey("subField"));
        assertTrue(map.containsKey("baseField"));
    }

    @Test
    public void testGetAllDeclaredFieldsMapCollision() {
        // Create a subclass that shadows a field name
        class Shadow extends AnnotatedSub {
            private int baseField; // shadows field from AnnotatedBase
        }
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(Shadow.class);
        // Simple name should map to subclass field; parent field should be qualified
        assertTrue(map.containsKey("baseField"));
        assertTrue(map.containsKey(AnnotatedBase.class.getName() + ".baseField"));
        assertEquals(Shadow.class, map.get("baseField").getDeclaringClass());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetAllDeclaredFieldsMapNullClassThrows() {
        ReflectionUtils.getAllDeclaredFieldsMap(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetAllDeclaredFieldsMapNullPredicateThrows() {
        ReflectionUtils.getAllDeclaredFieldsMap(AnnotatedSub.class, null);
    }

    /* --------------------------------------------------------------------- *
     *  8. Deprecated helpers – they still delegate to the main methods        *
     * --------------------------------------------------------------------- */

    @Test
    public void testGetDeepDeclaredFields() {
        Collection<Field> fields = ReflectionUtils.getDeepDeclaredFields(AnnotatedSub.class);
        assertNotNull(fields);
        // Ensure it respects the legacy filter (non‑transient, non‑synthetic)
        for (Field f : fields) {
            assertFalse(Modifier.isTransient(f.getModifiers()));
            assertFalse(f.isSynthetic());
        }
    }

    @Test
    public void testGetDeepDeclaredFieldMap() {
        Map<String, Field> map = ReflectionUtils.getDeepDeclaredFieldMap(AnnotatedSub.class);
        assertNotNull(map);
        assertTrue(map.containsKey("subField"));
    }

    @Test
    public void testDeprecatedGetDeclaredFields() {
        List<Field> list = new ArrayList<>();
        ReflectionUtils.getDeclaredFields(AnnotatedSub.class, list);
        assertTrue(list.size() > 0);
    }

    /* --------------------------------------------------------------------- *
     *  9. call(Object, Method, ...) – happy path and error handling          *
     * --------------------------------------------------------------------- */

    @Test
    public void testCallWithMethodSuccess() throws Exception {
        SimpleClass obj = new SimpleClass();
        Method m = SimpleClass.class.getMethod("echo", String.class);
        Object result = ReflectionUtils.call(obj, m, "hello");
        assertEquals("hello", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallWithNullMethodThrows() {
        ReflectionUtils.call(new SimpleClass(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallWithNullInstanceThrows() throws Exception {
        Method m = SimpleClass.class.getMethod("echo", String.class);
        ReflectionUtils.call(null, m);
    }

    @Test
    public void testCallMethodThatThrowsRuntimeExceptionWrapped() throws Exception {
        SimpleClass obj = new SimpleClass();
        Method m = SimpleClass.class.getMethod("throwsRuntime");
        try {
            ReflectionUtils.call(obj, m);
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof IllegalStateException);
            assertEquals("runtime", e.getCause().getMessage());
        }
    }

    /* --------------------------------------------------------------------- *
     *  10. call(Object, String, ...) – name based lookup                     *
     * --------------------------------------------------------------------- */

    @Test
    public void testCallByNameSuccess() {
        SimpleClass obj = new SimpleClass();
        Object result = ReflectionUtils.call(obj, "echo", "world");
        assertEquals("world", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallByNameNullInstanceThrows() {
        ReflectionUtils.call(null, "echo", "x");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallByNameNullMethodNameThrows() {
        SimpleClass obj = new SimpleClass();
        ReflectionUtils.call(obj, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallByNameMethodNotFoundThrows() {
        SimpleClass obj = new SimpleClass();
        ReflectionUtils.call(obj, "nonExistingMethod");
    }

    /* --------------------------------------------------------------------- *
     *  11. getMethod(Class, ...) – normal, null checks, caching             *
     * --------------------------------------------------------------------- */

    @Test
    public void testGetMethodFoundAndCache() {
        Method m1 = ReflectionUtils.getMethod(SimpleClass.class, "echo", String.class);
        Method m2 = ReflectionUtils.getMethod(SimpleClass.class, "echo", String.class);
        assertNotNull(m1);
        assertSame(m1, m2); // cached instance
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodNullClassThrows() {
        ReflectionUtils.getMethod(null, "any");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodNullNameThrows() {
        ReflectionUtils.getMethod(SimpleClass.class, null);
    }

    @Test
    public void testGetMethodPrivateMakesAccessible() throws NoSuchMethodException {
        // private method in SimpleClass
        Method privateM = SimpleClass.class.getDeclaredMethod("hidden");
        // The utility should make it accessible when retrieved via reflection
        Method retrieved = ReflectionUtils.getMethod(SimpleClass.class, "hidden");
        assertNotNull(retrieved);
        assertTrue(retrieved.isAccessible());
    }

    /* --------------------------------------------------------------------- *
     *  12. getMethod(Object, String, int) – overload resolution & errors     *
     * --------------------------------------------------------------------- */

    @Test
    public void testGetMethodByArgCountSelectsCorrectMethod() {
        AnnotatedSub obj = new AnnotatedSub();
        Method m = ReflectionUtils.getMethod(obj, "overloaded", 0);
        assertEquals(0, m.getParameterCount());
        Method m2 = ReflectionUtils.getMethod(obj, "overloaded", 1);
        assertEquals(1, m2.getParameterCount());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodByArgCountNullInstanceThrows() {
        ReflectionUtils.getMethod(null, "any", 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodByArgCountNullNameThrows() {
        ReflectionUtils.getMethod(new Object(), null, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodByArgCountNegativeCountThrows() {
        ReflectionUtils.getMethod(new Object(), "toString", -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodByArgCountNotFoundThrows() {
        SimpleClass obj = new SimpleClass();
        ReflectionUtils.getMethod(obj, "nonExisting", 0);
    }

    /* --------------------------------------------------------------------- *
     *  13. getNonOverloadedMethod – normal, null/blank, overload error      *
     * --------------------------------------------------------------------- */

    @Test
    public void testGetNonOverloadedMethodSuccess() {
        Method m = ReflectionUtils.getNonOverloadedMethod(SimpleClass.class, "echo");
        assertNotNull(m);
        assertEquals("echo", m.getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethodNullClassThrows() {
        ReflectionUtils.getNonOverloadedMethod(null, "any");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethodBlankNameThrows() {
        ReflectionUtils.getNonOverloadedMethod(SimpleClass.class, "   ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethodOverloadedThrows() {
        ReflectionUtils.getNonOverloadedMethod(AnnotatedSub.class, "overloaded");
    }

    /* --------------------------------------------------------------------- *
     *  14. getClassName – null handling                                      *
     * --------------------------------------------------------------------- */

    @Test
    public void testGetClassName() {
        assertEquals("null", ReflectionUtils.getClassName(null));
        assertEquals(SimpleClass.class.getName(), ReflectionUtils.getClassName(new SimpleClass()));
    }

    /* --------------------------------------------------------------------- *
     *  15. getClassNameFromByteCode – happy path and malformed data          *
     * --------------------------------------------------------------------- */

    @Test
    public void testGetClassNameFromByteCode() throws IOException {
        // Load the .class file of AnnotatedSub from the classpath
        String resourceName = AnnotatedSub.class.getName().replace('.', '/') + ".class";
        InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName);
        assertNotNull("Could not locate class file for AnnotatedSub", is);
        byte[] bytes = is.readAllBytes();
        String className = ReflectionUtils.getClassNameFromByteCode(bytes);
        assertEquals(AnnotatedSub.class.getName(), className);
    }

    @Test(expected = IllegalStateException.class)
    public void testGetClassNameFromByteCodeInvalidDataThrows() throws IOException {
        // Random data that does not represent a valid class file
        byte[] garbage = new byte[] {0, 1, 2, 3, 4, 5};
        ReflectionUtils.getClassNameFromByteCode(garbage);
    }

    /* --------------------------------------------------------------------- *
     *  16. getConstructor & getAllConstructors – caching and accessibility   *
     * --------------------------------------------------------------------- */

    @Test
    public void testGetConstructorFound() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(AnnotatedSub.class);
        assertNotNull(ctor);
        assertEquals(0, ctor.getParameterCount());
    }

    @Test
    public void testGetConstructorCaching() {
        Constructor<?> c1 = ReflectionUtils.getConstructor(AnnotatedSub.class);
        Constructor<?> c2 = ReflectionUtils.getConstructor(AnnotatedSub.class);
        assertSame(c1, c2);
    }

    @Test
    public void testGetAllConstructorsReturnsArray() {
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(AnnotatedSub.class);
        assertTrue(ctors.length >= 1); // at least the default constructor
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetConstructorNullClassThrows() {
        ReflectionUtils.getConstructor(null);
    }
}
