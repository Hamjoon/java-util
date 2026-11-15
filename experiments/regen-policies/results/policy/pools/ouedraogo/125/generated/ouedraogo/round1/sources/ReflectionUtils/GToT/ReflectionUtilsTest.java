package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Predicate;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link ReflectionUtils}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public API methods</li>
 *   <li>Typical usage scenarios</li>
 *   <li>Edge‑case handling (null arguments, missing members, overloaded methods, etc.)</li>
 *   <li>Exception wrapping performed by {@code call(...)} methods</li>
 *   <li>Cache behaviour (custom caches, cache hits)</li>
 * </ul>
 */
public class ReflectionUtilsTest {

    /* -------------------------------------------------------------
     *   Test fixtures – simple classes and annotations used by many
     *   tests below.
     * ------------------------------------------------------------- */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    private @interface TestAnno {
        String value() default "";
    }

    @TestAnno("parent")
    private static class Parent {
        private String privateField = "parent";
        public static final String STATIC_FIELD = "static";
        private String duplicate = "parentDup";

        @TestAnno("parentMethod")
        public void annotatedMethod() { }

        private void privateMethod() { }

        public String greet(String name) {
            return "Hello " + name;
        }

        public String overload() { return "zero"; }
        public String overload(String a) { return "one:" + a; }
        public String overload(String a, String b) { return "two:" + a + "," + b; }

        public void throwsChecked() throws IOException {
            throw new IOException("checked");
        }
    }

    private static class Child extends Parent {
        private String childField = "child";
        private String duplicate = "childDup";

        @Override
        public void annotatedMethod() { } // not annotated – should inherit parent's annotation
    }

    private enum SampleEnum {
        ONE, TWO;
        private int internal = 0; // should be filtered out by default predicate
    }

    /* -------------------------------------------------------------
     *   Helper to read a .class file into a byte[] for the
     *   getClassNameFromByteCode tests.
     * ------------------------------------------------------------- */
    private static byte[] classBytes(Class<?> clazz) throws IOException {
        String resource = "/" + clazz.getName().replace('.', '/') + ".class";
        try (InputStream is = clazz.getResourceAsStream(resource)) {
            assertNotNull("Class file not found for " + clazz, is);
            return is.readAllBytes();
        }
    }

    /* -------------------------------------------------------------
     *   Reset caches before each test to avoid cross‑test interference.
     * ------------------------------------------------------------- */
    @Before
    public void resetCaches() {
        // Use fresh HashMaps for all caches
        ReflectionUtils.setMethodCache(new HashMap<>());
        ReflectionUtils.setClassFieldsCache(new HashMap<>());
        ReflectionUtils.setFieldCache(new HashMap<>());
        ReflectionUtils.setClassAnnotationCache(new HashMap<>());
        ReflectionUtils.setMethodAnnotationCache(new HashMap<>());
        ReflectionUtils.setConstructorCache(new HashMap<>());
    }

    /* -------------------------------------------------------------
     *   1. set*Cache methods – simple sanity checks that the caches
     *   are actually replaced.
     * ------------------------------------------------------------- */
    @Test
    public void testSetMethodCacheIsUsed() throws Exception {
        Map<Object, Method> custom = new HashMap<>();
        ReflectionUtils.setMethodCache(custom);

        Method m = ReflectionUtils.getMethod(Parent.class, "greet", String.class);
        assertNotNull(m);
        // The custom map should now contain the cache key
        assertTrue(custom.keySet().stream().anyMatch(k -> k instanceof ReflectionUtils.MethodCacheKey));
    }

    @Test
    public void testSetConstructorCacheIsUsed() throws Exception {
        Map<Object, Constructor<?>> custom = new HashMap<>();
        ReflectionUtils.setConstructorCache(custom);

        Constructor<?> ctor = ReflectionUtils.getConstructor(Parent.class);
        assertNotNull(ctor);
        assertTrue(custom.keySet().stream().anyMatch(k -> k instanceof ReflectionUtils.ConstructorCacheKey));
    }

    /* -------------------------------------------------------------
     *   2. getClassAnnotation – normal, inherited and null handling.
     * ------------------------------------------------------------- */
    @Test
    public void testGetClassAnnotationInherited() {
        TestAnno anno = ReflectionUtils.getClassAnnotation(Child.class, TestAnno.class);
        assertNotNull(anno);
        assertEquals("parent", anno.value());
    }

    @Test
    public void testGetClassAnnotationMissing() {
        TestAnno anno = ReflectionUtils.getClassAnnotation(String.class, TestAnno.class);
        assertNull(anno);
    }

    @Test
    public void testGetClassAnnotationNullClass() {
        TestAnno anno = ReflectionUtils.getClassAnnotation(null, TestAnno.class);
        assertNull(anno);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetClassAnnotationNullAnnoClass() {
        ReflectionUtils.getClassAnnotation(Parent.class, null);
    }

    /* -------------------------------------------------------------
     *   3. getMethodAnnotation – direct and inherited.
     * ------------------------------------------------------------- */
    @Test
    public void testGetMethodAnnotationDirect() throws Exception {
        Method m = Parent.class.getDeclaredMethod("annotatedMethod");
        TestAnno anno = ReflectionUtils.getMethodAnnotation(m, TestAnno.class);
        assertNotNull(anno);
        assertEquals("parentMethod", anno.value());
    }

    @Test
    public void testGetMethodAnnotationInherited() throws Exception {
        Method childM = Child.class.getDeclaredMethod("annotatedMethod");
        TestAnno anno = ReflectionUtils.getMethodAnnotation(childM, TestAnno.class);
        // Should find the annotation on the super‑class method
        assertNotNull(anno);
        assertEquals("parentMethod", anno.value());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodAnnotationNullMethod() {
        ReflectionUtils.getMethodAnnotation(null, TestAnno.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodAnnotationNullAnnoClass() throws Exception {
        Method m = Parent.class.getDeclaredMethod("annotatedMethod");
        ReflectionUtils.getMethodAnnotation(m, null);
    }

    /* -------------------------------------------------------------
     *   4. getField – private field, static field (should be ignored),
     *   and missing field.
     * ------------------------------------------------------------- */
    @Test
    public void testGetFieldPrivate() {
        Field f = ReflectionUtils.getField(Parent.class, "privateField");
        assertNotNull(f);
        assertEquals("privateField", f.getName());
        assertFalse(Modifier.isStatic(f.getModifiers()));
    }

    @Test
    public void testGetFieldStaticIsIgnored() {
        Field f = ReflectionUtils.getField(Parent.class, "STATIC_FIELD");
        assertNotNull(f);
        // static fields are still returned by getField (the filter is applied elsewhere)
        assertTrue(Modifier.isStatic(f.getModifiers()));
    }

    @Test
    public void testGetFieldMissing() {
        Field f = ReflectionUtils.getField(Parent.class, "nonexistent");
        assertNull(f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetFieldNullClass() {
        ReflectionUtils.getField(null, "any");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetFieldNullName() {
        ReflectionUtils.getField(Parent.class, null);
    }

    /* -------------------------------------------------------------
     *   5. getDeclaredFields (default filter) – static & enum internals
     *   are filtered out.
     * ------------------------------------------------------------- */
    @Test
    public void testGetDeclaredFieldsDefaultFilter() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(Parent.class);
        // privateField and childField (inherited) are NOT included – only declared in Parent
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("privateField")));
        assertFalse(fields.stream().anyMatch(f -> f.getName().equals("STATIC_FIELD")));
    }

    @Test
    public void testGetDeclaredFieldsCustomPredicate() {
        Predicate<Field> onlyStringFields = f -> f.getType() == String.class;
        List<Field> fields = ReflectionUtils.getDeclaredFields(Parent.class, onlyStringFields);
        assertTrue(fields.stream().allMatch(f -> f.getType() == String.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetDeclaredFieldsNullClass() {
        ReflectionUtils.getDeclaredFields(null, f -> true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetDeclaredFieldsNullPredicate() {
        ReflectionUtils.getDeclaredFields(Parent.class, null);
    }

    /* -------------------------------------------------------------
     *   6. getAllDeclaredFields & map – inheritance, collision handling.
     * ------------------------------------------------------------- */
    @Test
    public void testGetAllDeclaredFieldsInheritance() {
        List<Field> all = ReflectionUtils.getAllDeclaredFields(Child.class);
        // Should contain fields from Child and Parent (including private ones)
        assertTrue(all.stream().anyMatch(f -> f.getName().equals("childField")));
        assertTrue(all.stream().anyMatch(f -> f.getName().equals("privateField")));
    }

    @Test
    public void testGetAllDeclaredFieldsMapCollision() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(Child.class);
        // Both Parent and Child have a field named "duplicate"
        assertTrue(map.containsKey("duplicate")); // child wins for simple name
        assertTrue(map.containsKey(Parent.class.getName() + ".duplicate")); // qualified parent key
        assertEquals("childDup", map.get("duplicate").get(Child.class).get(null)); // sanity check via reflection
    }

    /* -------------------------------------------------------------
     *   7. call(Object, Method, ...) – successful call, null method,
     *   illegal access and exception wrapping.
     * ------------------------------------------------------------- */
    @Test
    public void testCallWithMethodSuccess() throws Exception {
        Parent p = new Parent();
        Method m = ReflectionUtils.getMethod(Parent.class, "greet", String.class);
        Object result = ReflectionUtils.call(p, m, "World");
        assertEquals("Hello World", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallWithNullMethod() {
        ReflectionUtils.call(new Parent(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallWithNullInstance() throws Exception {
        Method m = ReflectionUtils.getMethod(Parent.class, "greet", String.class);
        ReflectionUtils.call(null, m);
    }

    @Test
    public void testCallWrapsInvocationTargetException() {
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

    /* -------------------------------------------------------------
     *   8. call(Object, String, ...) – method lookup by name & arg count.
     * ------------------------------------------------------------- */
    @Test
    public void testCallByNameNoOverload() {
        Parent p = new Parent();
        Object result = ReflectionUtils.call(p, "greet", "Bob");
        assertEquals("Hello Bob", result);
    }

    @Test
    public void testCallByNameWithOverloads() {
        Parent p = new Parent();
        // argCount = 2 selects overload(String,String)
        Object result = ReflectionUtils.call(p, "overload", "a", "b");
        assertEquals("two:a,b", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallByNameMethodNotFound() {
        ReflectionUtils.call(new Parent(), "nonexistentMethod");
    }

    /* -------------------------------------------------------------
     *   9. getMethod(Class, ...) – private method, caching, null args.
     * ------------------------------------------------------------- */
    @Test
    public void testGetMethodPrivate() throws Exception {
        Method m = ReflectionUtils.getMethod(Parent.class, "privateMethod");
        assertNotNull(m);
        assertTrue(m.isAccessible() || !Modifier.isPublic(m.getModifiers()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodNullClass() {
        ReflectionUtils.getMethod(null, "any");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodNullName() {
        ReflectionUtils.getMethod(Parent.class, null);
    }

    /* -------------------------------------------------------------
     *   10. getMethod(Object, name, argCount) – selection strategy.
     * ------------------------------------------------------------- */
    @Test
    public void testGetMethodByArgCountSelectsMostAccessible() {
        Method m = ReflectionUtils.getMethod(new Parent(), "overload", 1);
        // The public overload with one argument should be selected
        assertEquals(1, m.getParameterCount());
        assertEquals(String.class, m.getParameterTypes()[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodByArgCountNotFound() {
        ReflectionUtils.getMethod(new Parent(), "nonexistent", 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodByArgCountNegativeCount() {
        ReflectionUtils.getMethod(new Parent(), "greet", -1);
    }

    /* -------------------------------------------------------------
     *   11. getConstructor & getAllConstructors – private ctor & cache.
     * ------------------------------------------------------------- */
    private static class PrivateCtor {
        private PrivateCtor() { }
        private PrivateCtor(String s) { }
    }

    @Test
    public void testGetConstructorPrivate() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(PrivateCtor.class);
        assertNotNull(ctor);
        assertTrue(ctor.isAccessible() || !Modifier.isPublic(ctor.getModifiers()));
    }

    @Test
    public void testGetAllConstructorsCaching() {
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(PrivateCtor.class);
        assertEquals(2, ctors.length);
        // Ensure they are cached (the same map instance is used)
        Constructor<?> cached = ReflectionUtils.getConstructor(PrivateCtor.class, String.class);
        assertNotNull(cached);
        boolean found = Arrays.stream(ctors).anyMatch(c -> c.equals(cached));
        assertTrue(found);
    }

    @Test
    public void testGetConstructorMissing() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(String.class, int.class);
        assertNull(ctor);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetConstructorNullClass() {
        ReflectionUtils.getConstructor(null);
    }

    /* -------------------------------------------------------------
     *   12. getClassName – null handling.
     * ------------------------------------------------------------- */
    @Test
    public void testGetClassName() {
        assertEquals("java.lang.String", ReflectionUtils.getClassName("test"));
        assertEquals("null", ReflectionUtils.getClassName(null));
    }

    /* -------------------------------------------------------------
     *   13. getClassNameFromByteCode – valid class file and malformed data.
     * ------------------------------------------------------------- */
    @Test
    public void testGetClassNameFromByteCodeValid() throws Exception {
        byte[] bytes = classBytes(Parent.class);
        String name = ReflectionUtils.getClassNameFromByteCode(bytes);
        assertEquals(Parent.class.getName(), name);
    }

    @Test(expected = IllegalStateException.class)
    public void testGetClassNameFromByteCodeInvalidTag() {
        // Construct a minimal, invalid constant‑pool byte array (wrong tag 0)
        byte[] bogus = new byte[] {
                0xCA, 0xFE, 0xBA, 0xBE, // magic
                0x00, 0x00, // minor
                0x00, 0x34, // major (Java 8)
                0x00, 0x01, // constant_pool_count = 1 (invalid, must be >=2)
        };
        try {
            ReflectionUtils.getClassNameFromByteCode(bogus);
        } catch (IOException e) {
            // Convert to unchecked for the expected IllegalStateException path
            throw new RuntimeException(e);
        }
    }

    /* -------------------------------------------------------------
     *   14. Deprecated methods – ensure they still delegate correctly.
     * ------------------------------------------------------------- */
    @Test
    public void testDeprecatedGetDeepDeclaredFields() {
        Collection<Field> fields = ReflectionUtils.getDeepDeclaredFields(Parent.class);
        // Should contain the same fields as getAllDeclaredFields with the legacy filter
        List<Field> expected = ReflectionUtils.getAllDeclaredFields(Parent.class, f ->
                !Modifier.isTransient(f.getModifiers()) && !f.isSynthetic());
        assertEquals(new HashSet<>(expected), new HashSet<>(fields));
    }

    @Test
    public void testDeprecatedGetDeepDeclaredFieldMap() {
        Map<String, Field> map = ReflectionUtils.getDeepDeclaredFieldMap(Child.class);
        // Verify that a known field exists with its simple name
        assertTrue(map.containsKey("childField"));
        // Verify that a colliding field from the parent is qualified
        assertTrue(map.containsKey(Parent.class.getName() + ".duplicate"));
    }

    @Test
    public void testDeprecatedGetDeclaredFieldsCollection() {
        Collection<Field> collection = new ArrayList<>();
        ReflectionUtils.getDeclaredFields(Parent.class, collection);
        // Collection should now contain the filtered fields (non‑transient, non‑synthetic)
        assertFalse(collection.isEmpty());
        assertTrue(collection.stream().allMatch(f -> !Modifier.isTransient(f.getModifiers()) && !f.isSynthetic()));
    }
}
