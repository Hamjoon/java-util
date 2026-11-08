package com.cedarsoftware.util;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Comprehensive JUnit‑4 test suite for {@link ReflectionUtils}.
 *
 * The tests cover:
 * <ul>
 *   <li>Annotation lookup on classes and methods (including inheritance)</li>
 *   <li>Field lookup (including inheritance, static filtering and name collisions)</li>
 *   <li>Declared‑field retrieval with default and custom predicates</li>
 *   <li>Method lookup (by name/types, by argument count) and selection strategy</li>
 *   <li>Constructor lookup and ordered constructor list</li>
 *   <li>Utility methods {@code call}, {@code getClassName}, {@code getClassNameFromByteCode},
 *       {@code isJavaCompilerAvailable} and cache‑swapping</li>
 *   <li>Behaviour of deprecated helpers</li>
 * </ul>
 *
 * No external mocking frameworks are used; all objects are created via real constructors.
 */
public class ReflectionUtilsTest {

    /* -------------------------------------------------------------
     *  Test fixtures – simple classes and annotations
     * ------------------------------------------------------------- */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    private @interface DummyAnno {}

    @DummyAnno
    private static class SuperClass {
        private String superField = "super";

        @DummyAnno
        public void annotatedMethod() {}

        private void privateMethod() {}
    }

    private static class SubClass extends SuperClass {
        private String subField = "sub";

        // overloads for getMethod(Object, name, argCount) tests
        public void overloaded(int i) {}
        protected void overloaded(String s) {}
        private void overloaded() {}
    }

    private static class CollisionParent {
        private int conflict = 1;
    }

    private static class CollisionChild extends CollisionParent {
        private int conflict = 2;
        private int unique = 3;
    }

    private static class FieldSample {
        public static int STATIC_VAL = 10;
        public int publicVal = 20;
        private int privateVal = 30;
        // enum‑like internal fields that the default filter should drop
        public static final String ENUM$VALUES = "enum";
        public static final String internal = "internal";
    }

    private static class NoArgMethodClass {
        @DummyAnno
        public void onlyOne() {}
    }

    private static class ConstructorSample {
        public ConstructorSample() {}
        protected ConstructorSample(String s) {}
        private ConstructorSample(int i, String s) {}
    }

    /* -------------------------------------------------------------
     *  Helper to read a class file as a byte array (used for the
     *  getClassNameFromByteCode test)
     * ------------------------------------------------------------- */
    private static byte[] readClassBytes(Class<?> clazz) throws IOException {
        String resource = clazz.getSimpleName() + ".class";
        try (InputStream is = clazz.getResourceAsStream(resource)) {
            assertNotNull("Unable to locate class file for " + clazz, is);
            return is.readAllBytes();
        }
    }

    /* -------------------------------------------------------------
     *  Preserve and restore the custom cache state between tests
     * ------------------------------------------------------------- */
    private Map<Object, Method> originalMethodCache;
    private Map<Object, Collection<Field>> originalFieldsCache;

    @Before
    public void saveOriginalCaches() {
        // Save the default caches so we can restore them after each test
        originalMethodCache = new HashMap<>(ReflectionUtils.METHOD_CACHE.get());
        originalFieldsCache = new HashMap<>(ReflectionUtils.FIELDS_CACHE.get());
    }

    @After
    public void restoreOriginalCaches() {
        // Restore the original caches – they are thread‑safe maps already
        ReflectionUtils.setMethodCache(originalMethodCache);
        ReflectionUtils.setClassFieldsCache(originalFieldsCache);
    }

    /* -------------------------------------------------------------
     *  Class‑annotation lookup tests
     * ------------------------------------------------------------- */
    @Test
    public void testGetClassAnnotation_Inherited() {
        DummyAnno anno = ReflectionUtils.getClassAnnotation(SubClass.class, DummyAnno.class);
        assertNotNull("Annotation should be inherited from SuperClass", anno);
    }

    @Test
    public void testGetClassAnnotation_Missing() {
        DummyAnno anno = ReflectionUtils.getClassAnnotation(String.class, DummyAnno.class);
        assertNull("String has no DummyAnno", anno);
    }

    @Test
    public void testGetClassAnnotation_NullClass() {
        DummyAnno anno = ReflectionUtils.getClassAnnotation(null, DummyAnno.class);
        assertNull("Null class should yield null result", anno);
    }

    /* -------------------------------------------------------------
     *  Method‑annotation lookup tests
     * ------------------------------------------------------------- */
    @Test
    public void testGetMethodAnnotation_Direct() throws Exception {
        Method m = SuperClass.class.getDeclaredMethod("annotatedMethod");
        DummyAnno anno = ReflectionUtils.getMethodAnnotation(m, DummyAnno.class);
        assertNotNull("Method annotation should be found directly", anno);
    }

    @Test
    public void testGetMethodAnnotation_Inherited() throws Exception {
        Method m = SubClass.class.getDeclaredMethod("annotatedMethod");
        DummyAnno anno = ReflectionUtils.getMethodAnnotation(m, DummyAnno.class);
        assertNotNull("Method annotation should be found on super class method", anno);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodAnnotation_NullMethod() {
        ReflectionUtils.getMethodAnnotation(null, DummyAnno.class);
    }

    /* -------------------------------------------------------------
     *  Field lookup & cache tests
     * ------------------------------------------------------------- */
    @Test
    public void testGetField_DirectAndInherited() {
        Field f1 = ReflectionUtils.getField(FieldSample.class, "publicVal");
        assertNotNull(f1);
        assertEquals("publicVal", f1.getName());

        // inherited field from super class
        Field f2 = ReflectionUtils.getField(SubClass.class, "superField");
        assertNotNull(f2);
        assertEquals("superField", f2.getName());

        // static fields are ignored by the default filter but are still searchable by name
        Field staticFld = ReflectionUtils.getField(FieldSample.class, "STATIC_VAL");
        assertNotNull(staticFld);
    }

    @Test
    public void testGetDeclaredFields_DefaultFilter() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(FieldSample.class);
        // STATIC_VAL, ENUM$VALUES and internal should be filtered out
        Set<String> names = new HashSet<>();
        for (Field f : fields) names.add(f.getName());

        assertTrue(names.contains("publicVal"));
        assertTrue(names.contains("privateVal"));
        assertFalse("static field must be filtered", names.contains("STATIC_VAL"));
        assertFalse("enum field must be filtered", names.contains("ENUM$VALUES"));
        assertFalse("internal field must be filtered", names.contains("internal"));
    }

    @Test
    public void testGetDeclaredFields_CustomFilter() {
        Predicate<Field> onlyPublic = f -> Modifier.isPublic(f.getModifiers());
        List<Field> publicOnly = ReflectionUtils.getDeclaredFields(FieldSample.class, onlyPublic);
        assertEquals(1, publicOnly.size());
        assertEquals("publicVal", publicOnly.get(0).getName());
    }

    @Test
    public void testGetAllDeclaredFields_Hierarchy() {
        List<Field> all = ReflectionUtils.getAllDeclaredFields(CollisionChild.class);
        // Should contain both 'conflict' fields (child and parent) and 'unique'
        Set<String> names = new HashSet<>();
        for (Field f : all) names.add(f.getName());

        assertTrue(names.contains("conflict"));
        assertTrue(names.contains("unique"));
        // Ensure ordering – child fields first
        assertEquals("conflict", all.get(0).getName()); // child version
        assertEquals("unique", all.get(1).getName());
    }

    @Test
    public void testGetAllDeclaredFieldsMap_NameCollision() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(CollisionChild.class);
        // Simple name 'conflict' should map to child field
        assertTrue(map.containsKey("conflict"));
        assertEquals(CollisionChild.class, map.get("conflict").getDeclaringClass());

        // Parent's field must be stored under fully‑qualified key
        String fq = CollisionParent.class.getName() + ".conflict";
        assertTrue(map.containsKey(fq));
        assertEquals(CollisionParent.class, map.get(fq).getDeclaringClass());
    }

    @Test
    public void testGetDeepDeclaredFields_DeprecatedBehaviour() {
        Collection<Field> deep = ReflectionUtils.getDeepDeclaredFields(FieldSample.class);
        // The deprecated version excludes transient & synthetic fields – our sample has none
        // It should otherwise behave like getAllDeclaredFields with default filter
        Set<String> names = new HashSet<>();
        for (Field f : deep) names.add(f.getName());

        assertTrue(names.contains("publicVal"));
        assertTrue(names.contains("privateVal"));
        assertFalse(names.contains("STATIC_VAL"));
    }

    /* -------------------------------------------------------------
     *  Method lookup tests (including selection strategy)
     * ------------------------------------------------------------- */
    @Test
    public void testGetMethod_BySignature_Private() throws Exception {
        Method m = ReflectionUtils.getMethod(SubClass.class, "privateMethod");
        assertNotNull(m);
        assertTrue(Modifier.isPrivate(m.getModifiers()));
    }

    @Test
    public void testGetMethod_Object_ArgCount_Selection() {
        SubClass instance = new SubClass();
        Method m = ReflectionUtils.getMethod(instance, "overloaded", 1);
        assertNotNull(m);
        // Expect the public int version to be chosen (higher accessibility)
        assertArrayEquals(new Class<?>[]{int.class}, m.getParameterTypes());
        assertTrue(Modifier.isPublic(m.getModifiers()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethod_Object_ArgCount_NotFound() {
        ReflectionUtils.getMethod(new SubClass(), "nonexistent", 2);
    }

    @Test
    public void testGetNonOverloadedMethod_Success() {
        Method m = ReflectionUtils.getNonOverloadedMethod(NoArgMethodClass.class, "onlyOne");
        assertNotNull(m);
        assertEquals("onlyOne", m.getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethod_Overloaded() {
        // SubClass has three overloaded methods named "overloaded"
        ReflectionUtils.getNonOverloadedMethod(SubClass.class, "overloaded");
    }

    /* -------------------------------------------------------------
     *  Constructor lookup & ordering tests
     * ------------------------------------------------------------- */
    @Test
    public void testGetConstructor_VariousAccess() {
        Constructor<?> publicCtor = ReflectionUtils.getConstructor(ConstructorSample.class);
        assertNotNull(publicCtor);
        assertTrue(Modifier.isPublic(publicCtor.getModifiers()));

        Constructor<?> protectedCtor = ReflectionUtils.getConstructor(ConstructorSample.class, String.class);
        assertNotNull(protectedCtor);
        assertTrue(Modifier.isProtected(protectedCtor.getModifiers()));

        Constructor<?> privateCtor = ReflectionUtils.getConstructor(ConstructorSample.class, int.class, String.class);
        assertNotNull(privateCtor);
        assertTrue(Modifier.isPrivate(privateCtor.getModifiers()));
    }

    @Test
    public void testGetAllConstructors_Order() {
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(ConstructorSample.class);
        assertEquals(3, ctors.length);
        // Expected order: public, protected, private
        assertTrue(Modifier.isPublic(ctors[0].getModifiers()));
        assertTrue(Modifier.isProtected(ctors[1].getModifiers()));
        assertTrue(Modifier.isPrivate(ctors[2].getModifiers()));
    }

    /* -------------------------------------------------------------
     *  Utility method tests
     * ------------------------------------------------------------- */
    @Test
    public void testCall_WithMethodObject() throws Exception {
        class Target {
            String greet(String name) { return "Hi " + name; }
        }
        Target t = new Target();
        Method m = Target.class.getDeclaredMethod("greet", String.class);
        Object result = ReflectionUtils.call(t, m, "Bob");
        assertEquals("Hi Bob", result);
    }

    @Test
    public void testCall_WithMethodObject_ExceptionWrapping() {
        class Exploder {
            void boom() { throw new IllegalStateException("boom!"); }
        }
        Exploder e = new Exploder();
        Method m;
        try {
            m = Exploder.class.getDeclaredMethod("boom");
        } catch (NoSuchMethodException ex) {
            fail("Method not found");
            return;
        }

        try {
            ReflectionUtils.call(e, m);
            fail("Expected RuntimeException");
        } catch (RuntimeException re) {
            assertTrue(re.getCause() instanceof IllegalStateException);
            assertEquals("boom!", re.getCause().getMessage());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCall_NullMethod() {
        ReflectionUtils.call(new Object(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCall_NullInstance() throws Exception {
        Method m = Object.class.getMethod("toString");
        ReflectionUtils.call(null, m);
    }

    @Test
    public void testCall_ByName_Simple() {
        class Simple {
            String echo(String s) { return s; }
        }
        Simple s = new Simple();
        Object out = ReflectionUtils.call(s, "echo", "xyz");
        assertEquals("xyz", out);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCall_ByName_MethodNotFound() {
        ReflectionUtils.call(new Object(), "nonexistentMethod");
    }

    @Test
    public void testGetClassName() {
        assertEquals("null", ReflectionUtils.getClassName(null));
        assertEquals(String.class.getName(), ReflectionUtils.getClassName("hello"));
    }

    @Test
    public void testGetClassNameFromByteCode() throws IOException {
        byte[] bytes = readClassBytes(FieldSample.class);
        String name = ReflectionUtils.getClassNameFromByteCode(bytes);
        assertEquals(FieldSample.class.getName(), name);
    }

    @Test
    public void testIsJavaCompilerAvailable_ForceJre() {
        // Force the method to behave as if running on a JRE
        System.setProperty("java.util.force.jre", "true");
        try {
            assertFalse("Compiler should be reported unavailable when forced", ReflectionUtils.isJavaCompilerAvailable());
        } finally {
            System.clearProperty("java.util.force.jre");
        }
    }

    /* -------------------------------------------------------------
     *  Cache‑swap tests – verify that custom maps can be installed
     * ------------------------------------------------------------- */
    @Test
    public void testSetMethodCache_CustomMap() throws Exception {
        Map<Object, Method> custom = new ConcurrentHashMap<>();
        ReflectionUtils.setMethodCache(custom);

        // After swapping, a lookup should populate the custom map
        Method m = ReflectionUtils.getMethod(String.class, "length");
        assertNotNull(m);
        assertTrue(custom.containsKey(new ReflectionUtils.MethodCacheKey(String.class, "length")));
    }

    @Test
    public void testSetClassFieldsCache_CustomMap() {
        Map<Object, Collection<Field>> custom = new ConcurrentHashMap<>();
        ReflectionUtils.setClassFieldsCache(custom);

        List<Field> fields = ReflectionUtils.getDeclaredFields(FieldSample.class);
        assertNotNull(fields);
        // The cache key used internally contains the predicate identity; we can only
        // verify that the map now holds something (the exact key is opaque).
        assertFalse(custom.isEmpty());
    }
}
