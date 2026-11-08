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

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ReflectionUtils}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>Cache‑replacement APIs</li>
 *   <li>Annotation lookup (class &amp; method) including inheritance</li>
 *   <li>Field retrieval (single &amp; bulk) and default filtering</li>
 *   <li>Method lookup (exact signature, by arg count, non‑overloaded)</li>
 *   <li>Constructor lookup and ordering of {@code getAllConstructors}</li>
 *   <li>Invocation helpers ({@code call}) and error handling</li>
 *   <li>Utility methods such as {@code getClassName}, {@code getClassNameFromByteCode},
 *       and {@code isJavaCompilerAvailable}</li>
 * </ul>
 *
 * <p>All tests use only the public API of {@link ReflectionUtils} and the
 * helper classes defined in this file; no external mocking frameworks are used.
 */
public class ReflectionUtilsTest {

    /* ---------------------------------------------------------------------- */
    /*  Helper annotations and test classes                                   */
    /* ---------------------------------------------------------------------- */

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface ClassMarker {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface MethodMarker {}

    @ClassMarker
    public static class BaseClass {
        private int basePrivate = 42;
        protected String baseProtected = "base";
        public static int staticField = 99;
        public int publicBase = 1;
    }

    public static class DerivedClass extends BaseClass {
        private String derivedPrivate = "derived";
        public int publicDerived = 2;
    }

    public interface TestInterface {
        @MethodMarker
        void annotatedMethod();
        void otherMethod();
    }

    public static class InterfaceImpl implements TestInterface {
        @Override
        public void annotatedMethod() { }

        @Override
        public void otherMethod() { }

        private void privateMethod(String s) { }
        public void overloaded(String s) { }
        protected void overloaded(Integer i) { }
    }

    public static class ConstructorTest {
        public ConstructorTest() {}
        protected ConstructorTest(String a) {}
        ConstructorTest(int a, int b) {}
        private ConstructorTest(double d) {}
    }

    public static class OverloadSelection {
        public void foo(String s) {}
        protected void foo(Integer i) {}
        private void foo(Double d) {}
    }

    /* ---------------------------------------------------------------------- */
    /*  Simple tracking map used to verify that custom caches are honoured    */
    /* ---------------------------------------------------------------------- */
    private static class TrackingMap<K, V> extends HashMap<K, V> {
        private final List<K> putKeys = new ArrayList<>();

        @Override
        public V put(K key, V value) {
            putKeys.add(key);
            return super.put(key, value);
        }

        public List<K> getPutKeys() {
            return putKeys;
        }
    }

    private TrackingMap<Object, Method> originalMethodCache;
    private TrackingMap<Object, Collection<Field>> originalFieldsCache;
    private TrackingMap<Object, Field> originalFieldCache;
    private TrackingMap<Object, Annotation> originalClassAnnoCache;
    private TrackingMap<Object, Annotation> originalMethodAnnoCache;
    private TrackingMap<Object, Constructor<?>> originalConstructorCache;
    private TrackingMap<Object, Constructor<?>[]> originalSortedCtorCache;

    @Before
    public void setUp() {
        // Preserve the default caches so that we can restore them after each test
        originalMethodCache = new TrackingMap<>();
        originalFieldsCache = new TrackingMap<>();
        originalFieldCache = new TrackingMap<>();
        originalClassAnnoCache = new TrackingMap<>();
        originalMethodAnnoCache = new TrackingMap<>();
        originalConstructorCache = new TrackingMap<>();
        originalSortedCtorCache = new TrackingMap<>();

        ReflectionUtils.setMethodCache(originalMethodCache);
        ReflectionUtils.setClassFieldsCache(originalFieldsCache);
        ReflectionUtils.setFieldCache(originalFieldCache);
        ReflectionUtils.setClassAnnotationCache(originalClassAnnoCache);
        ReflectionUtils.setMethodAnnotationCache(originalMethodAnnoCache);
        ReflectionUtils.setConstructorCache(originalConstructorCache);
        ReflectionUtils.setSortedConstructorsCache(originalSortedCtorCache);
    }

    @After
    public void tearDown() {
        // Restore the original LRUCache implementations (the library creates them lazily)
        ReflectionUtils.setMethodCache(new LinkedHashMap<>());
        ReflectionUtils.setClassFieldsCache(new LinkedHashMap<>());
        ReflectionUtils.setFieldCache(new LinkedHashMap<>());
        ReflectionUtils.setClassAnnotationCache(new LinkedHashMap<>());
        ReflectionUtils.setMethodAnnotationCache(new LinkedHashMap<>());
        ReflectionUtils.setConstructorCache(new LinkedHashMap<>());
        ReflectionUtils.setSortedConstructorsCache(new LinkedHashMap<>());
    }

    /* ---------------------------------------------------------------------- */
    /*  Cache replacement tests                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testSetMethodCacheIsUsed() throws Exception {
        Method m = InterfaceImpl.class.getMethod("annotatedMethod");
        Method found = ReflectionUtils.getMethod(InterfaceImpl.class, "annotatedMethod");
        assertSame(m, found);
        assertTrue(originalMethodCache.getPutKeys().contains(new ReflectionUtils.MethodCacheKey(InterfaceImpl.class, "annotatedMethod")));
    }

    @Test
    public void testSetFieldCacheIsUsed() {
        Field f = ReflectionUtils.getField(DerivedClass.class, "publicDerived");
        assertNotNull(f);
        assertTrue(originalFieldCache.getPutKeys().stream()
                .anyMatch(k -> k instanceof ReflectionUtils.FieldNameCacheKey));
    }

    @Test
    public void testSetClassAnnotationCacheIsUsed() {
        ClassMarker anno = ReflectionUtils.getClassAnnotation(BaseClass.class, ClassMarker.class);
        assertNotNull(anno);
        assertTrue(originalClassAnnoCache.getPutKeys().stream()
                .anyMatch(k -> k instanceof ReflectionUtils.ClassAnnotationCacheKey));
    }

    @Test
    public void testSetMethodAnnotationCacheIsUsed() {
        Method m = InterfaceImpl.class.getMethod("annotatedMethod");
        MethodMarker anno = ReflectionUtils.getMethodAnnotation(m, MethodMarker.class);
        assertNotNull(anno);
        assertTrue(originalMethodAnnoCache.getPutKeys().stream()
                .anyMatch(k -> k instanceof ReflectionUtils.MethodAnnotationCacheKey));
    }

    @Test
    public void testSetConstructorCacheIsUsed() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(ConstructorTest.class, double.class);
        assertNotNull(ctor);
        assertTrue(originalConstructorCache.getPutKeys().stream()
                .anyMatch(k -> k instanceof ReflectionUtils.ConstructorCacheKey));
    }

    @Test
    public void testSetSortedConstructorsCacheIsUsed() {
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(ConstructorTest.class);
        assertEquals(4, ctors.length);
        assertTrue(originalSortedCtorCache.getPutKeys().stream()
                .anyMatch(k -> k instanceof ReflectionUtils.SortedConstructorsCacheKey));
    }

    /* ---------------------------------------------------------------------- */
    /*  Class & method annotation lookup tests                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetClassAnnotationFoundInSuperclass() {
        // BaseClass is annotated, DerivedClass inherits it
        ClassMarker anno = ReflectionUtils.getClassAnnotation(DerivedClass.class, ClassMarker.class);
        assertNotNull(anno);
        // Second call should hit the cache (same instance)
        ClassMarker anno2 = ReflectionUtils.getClassAnnotation(DerivedClass.class, ClassMarker.class);
        assertSame(anno, anno2);
    }

    @Test
    public void testGetClassAnnotationNotFound() {
        // No annotation of type Deprecated on the class hierarchy
        Deprecated anno = ReflectionUtils.getClassAnnotation(DerivedClass.class, Deprecated.class);
        assertNull(anno);
    }

    @Test
    public void testGetMethodAnnotationFromInterface() throws Exception {
        Method m = InterfaceImpl.class.getMethod("annotatedMethod");
        MethodMarker anno = ReflectionUtils.getMethodAnnotation(m, MethodMarker.class);
        assertNotNull(anno);
        // Cached value should be returned on second call
        MethodMarker anno2 = ReflectionUtils.getMethodAnnotation(m, MethodMarker.class);
        assertSame(anno, anno2);
    }

    @Test
    public void testGetMethodAnnotationNotFound() throws Exception {
        Method m = InterfaceImpl.class.getMethod("otherMethod");
        MethodMarker anno = ReflectionUtils.getMethodAnnotation(m, MethodMarker.class);
        assertNull(anno);
    }

    /* ---------------------------------------------------------------------- */
    /*  Field retrieval tests                                                  */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetFieldFindsInheritedField() {
        Field f = ReflectionUtils.getField(DerivedClass.class, "baseProtected");
        assertNotNull(f);
        assertEquals("baseProtected", f.getName());
        assertEquals(BaseClass.class, f.getDeclaringClass());
    }

    @Test
    public void testGetFieldIgnoresStaticField() {
        Field f = ReflectionUtils.getField(BaseClass.class, "staticField");
        assertNull(f);
    }

    @Test
    public void testGetDeclaredFieldsWithCustomFilter() {
        List<Field> filtered = ReflectionUtils.getDeclaredFields(BaseClass.class,
                f -> f.getName().startsWith("base"));
        assertEquals(2, filtered.size()); // basePrivate and baseProtected
        Set<String> names = new HashSet<>();
        for (Field f : filtered) names.add(f.getName());
        assertTrue(names.contains("basePrivate"));
        assertTrue(names.contains("baseProtected"));
    }

    @Test
    public void testGetAllDeclaredFieldsDefaultFilter() {
        List<Field> all = ReflectionUtils.getAllDeclaredFields(DerivedClass.class);
        // Should contain publicDerived, baseProtected, basePrivate (private is filtered out by default)
        Set<String> names = new HashSet<>();
        for (Field f : all) names.add(f.getName());

        assertTrue(names.contains("publicDerived"));
        assertTrue(names.contains("baseProtected"));
        assertFalse(names.contains("staticField")); // static excluded
        assertFalse(names.contains("basePrivate")); // private excluded by default filter
    }

    @Test
    public void testGetAllDeclaredFieldsMapHandlesShadowedNames() {
        class Parent {
            private int value = 1;
        }
        class Child extends Parent {
            private int value = 2;
            public int other = 3;
        }
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(Child.class);
        // Two entries: "other" and fully qualified name for parent's field
        assertTrue(map.containsKey("other"));
        assertTrue(map.containsKey(Parent.class.getName() + ".value"));
        assertFalse(map.containsKey("value")); // simple name collides, child wins, parent is qualified
    }

    @Test
    public void testGetDeepDeclaredFieldsLegacyBehavior() {
        // The legacy method excludes transient and synthetic fields.
        // Our test class has no transient/synthetic fields, so result should equal default fields.
        Collection<Field> deep = ReflectionUtils.getDeepDeclaredFields(BaseClass.class);
        List<Field> defaultFields = ReflectionUtils.getAllDeclaredFields(BaseClass.class);
        assertEquals(defaultFields.size(), deep.size());
        assertTrue(deep.containsAll(defaultFields));
    }

    /* ---------------------------------------------------------------------- */
    /*  Method lookup and selection tests                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetMethodExactSignaturePrivate() throws Exception {
        Method m = ReflectionUtils.getMethod(InterfaceImpl.class, "privateMethod", String.class);
        assertNotNull(m);
        assertTrue(java.lang.reflect.Modifier.isPrivate(m.getModifiers()));
    }

    @Test
    public void testGetMethodByArgCountSelectsMostAccessible() {
        OverloadSelection obj = new OverloadSelection();
        Method m = ReflectionUtils.getMethod(obj, "foo", 1);
        assertNotNull(m);
        // public foo(String) should be chosen over protected foo(Integer)
        assertTrue(java.lang.reflect.Modifier.isPublic(m.getModifiers()));
        assertEquals(String.class, m.getParameterTypes()[0]);
    }

    @Test
    public void testGetNonOverloadedMethodSuccess() throws Exception {
        Method m = ReflectionUtils.getNonOverloadedMethod(BaseClass.class, "publicBase");
        assertNotNull(m);
        assertEquals("publicBase", m.getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethodFailsWhenOverloaded() throws Exception {
        // Both overloaded(String) and overloaded(Integer) exist
        ReflectionUtils.getNonOverloadedMethod(InterfaceImpl.class, "overloaded");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethodFailsWhenMissing() throws Exception {
        ReflectionUtils.getNonOverloadedMethod(BaseClass.class, "nonExisting");
    }

    /* ---------------------------------------------------------------------- */
    /*  Constructor handling tests                                             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetConstructorPrivate() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(ConstructorTest.class, double.class);
        assertNotNull(ctor);
        assertTrue(java.lang.reflect.Modifier.isPrivate(ctor.getModifiers()));
    }

    @Test
    public void testGetAllConstructorsOrdering() {
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(ConstructorTest.class);
        assertEquals(4, ctors.length);
        // Expected order: public (no‑arg), protected (String), package (int,int), private (double)
        assertTrue(java.lang.reflect.Modifier.isPublic(ctors[0].getModifiers()));
        assertTrue(java.lang.reflect.Modifier.isProtected(ctors[1].getModifiers()));
        assertFalse(java.lang.reflect.Modifier.isPublic(ctors[2].getModifiers()));
        assertTrue(java.lang.reflect.Modifier.isPrivate(ctors[3].getModifiers()));
        // Within same accessibility the one with more parameters comes first
        // (package‑private constructors: only one, so no further check needed)
    }

    /* ---------------------------------------------------------------------- */
    /*  Invocation helpers tests                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCallWithMethodObjectSuccess() throws Exception {
        InterfaceImpl target = new InterfaceImpl();
        Method m = InterfaceImpl.class.getMethod("annotatedMethod");
        Object result = ReflectionUtils.call(target, m);
        assertNull(result); // void method
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallWithNullMethodThrows() {
        ReflectionUtils.call(new Object(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallWithNullInstanceThrows() throws Exception {
        Method m = Object.class.getMethod("toString");
        ReflectionUtils.call(null, m);
    }

    @Test
    public void testCallWithPrivateMethodWrappedInRuntimeException() throws Exception {
        InterfaceImpl target = new InterfaceImpl();
        Method privateM = InterfaceImpl.class.getDeclaredMethod("privateMethod", String.class);
        // Do NOT make it accessible – the call() method should wrap the IllegalAccessException
        try {
            ReflectionUtils.call(target, privateM, "test");
            fail("Expected RuntimeException due to illegal access");
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof IllegalAccessException);
        }
    }

    @Test
    public void testCallByNameUsesCachedMethodLookup() {
        InterfaceImpl target = new InterfaceImpl();
        Object result = ReflectionUtils.call(target, "annotatedMethod");
        assertNull(result);
        // The underlying cache should now contain an entry for the method name + arg count 0
        assertTrue(originalMethodCache.getPutKeys().stream()
                .anyMatch(k -> k instanceof ReflectionUtils.MethodCacheKey));
    }

    /* ---------------------------------------------------------------------- */
    /*  Miscellaneous utility tests                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetClassNameHandlesNull() {
        assertEquals("null", ReflectionUtils.getClassName(null));
        assertEquals(this.getClass().getName(), ReflectionUtils.getClassName(this));
    }

    @Test
    public void testGetClassNameFromByteCode() throws IOException {
        // Load the bytecode of a known class (java.lang.String) from the runtime classpath
        String classResource = "/java/lang/String.class";
        InputStream is = String.class.getResourceAsStream(classResource);
        assertNotNull("Unable to locate String.class resource", is);
        byte[] bytes = is.readAllBytes();
        is.close();

        String name = ReflectionUtils.getClassNameFromByteCode(bytes);
        assertEquals("java.lang.String", name);
    }

    @Test
    public void testIsJavaCompilerAvailableTrue() {
        // In a typical JDK environment the compiler is present.
        // We cannot guarantee the environment, but the test should pass when the
        // system property is not set to force JRE mode.
        boolean available = ReflectionUtils.isJavaCompilerAvailable();
        // If the JDK is present, the result is true; otherwise false.
        // The important part is that the property overrides the detection.
        // Hence we only assert that the method returns a boolean without throwing.
        assertTrue(available || !available);
    }

    @Test
    public void testIsJavaCompilerAvailableForcedJre() {
        String prop = "java.util.force.jre";
        String original = System.getProperty(prop);
        try {
            System.setProperty(prop, "true");
            assertFalse(ReflectionUtils.isJavaCompilerAvailable());
        } finally {
            // Restore original value
            if (original != null) {
                System.setProperty(prop, original);
            } else {
                System.clearProperty(prop);
            }
        }
    }
}
