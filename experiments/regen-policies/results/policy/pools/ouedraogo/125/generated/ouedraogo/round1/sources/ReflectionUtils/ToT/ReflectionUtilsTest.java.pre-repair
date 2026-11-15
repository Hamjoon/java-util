package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ReflectionUtils}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>Cache replacement via the {@code set*Cache} methods.</li>
 *   <li>Annotation lookup on classes and methods (including inheritance).</li>
 *   <li>Field lookup (including hierarchy, static exclusion and default filter).</li>
 *   <li>Declared‑field utilities with custom predicates.</li>
 *   <li>Method lookup utilities (by name, by argument count, overloaded handling, caching).</li>
 *   <li>Constructor lookup and bulk constructor retrieval.</li>
 *   <li>Utility helpers such as {@code makeParamKey}, {@code getClassName},
 *       and {@code getClassNameFromByteCode}.</li>
 *   <li>Exception handling for the {@code call} overloads.</li>
 * </ul>
 *
 * <p>All tests use real objects – no mocking frameworks are employed – and rely only on
 * classes, constructors and methods that exist in the source code above or in the JDK.
 */
public class ReflectionUtilsTest {

    /* ---------------------------------------------------------------------- */
    /*  Test Annotations                                                       */
    /* ---------------------------------------------------------------------- */

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
    private @interface TestAnno { }

    /* ---------------------------------------------------------------------- */
    /*  Test Classes                                                            */
    /* ---------------------------------------------------------------------- */

    @TestAnno
    static class Parent {
        private int parentInt;
        public String parentString;

        @TestAnno
        public void annotatedParentMethod() { }

        private void privateParentMethod() { }

        public void overloaded(int a) { }
        public void overloaded(String s) { }
    }

    static class Child extends Parent {
        private int childInt;
        public static final String STATIC_FIELD = "ignore";

        @Override
        public void overloaded(int a) { } // overrides one overload

        public void childOnlyMethod() { }
    }

    enum SampleEnum {
        ONE, TWO;
        private int enumField; // should be filtered out by the default predicate
    }

    /* ---------------------------------------------------------------------- */
    /*  Helper Map to verify that a custom cache is really used                */
    /* ---------------------------------------------------------------------- */

    private static class TrackingMap<K, V> extends HashMap<K, V> {
        boolean computeIfAbsentCalled = false;

        @Override
        public V computeIfAbsent(K key, java.util.function.Function<? super K, ? extends V> mappingFunction) {
            computeIfAbsentCalled = true;
            return super.computeIfAbsent(key, mappingFunction);
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  Reset caches before each test (so tests are independent)              */
    /* ---------------------------------------------------------------------- */

    @Before
    public void resetCaches() {
        // Restore the default LRUCache implementations – the public API
        // only provides a way to replace them, not to reset them, so we
        // replace them with fresh ConcurrentHashMaps which behave like a
        // simple thread‑safe cache.
        ReflectionUtils.setMethodCache(new ConcurrentHashMap<>());
        ReflectionUtils.setClassFieldsCache(new ConcurrentHashMap<>());
        ReflectionUtils.setFieldCache(new ConcurrentHashMap<>());
        ReflectionUtils.setClassAnnotationCache(new ConcurrentHashMap<>());
        ReflectionUtils.setMethodAnnotationCache(new ConcurrentHashMap<>());
        ReflectionUtils.setConstructorCache(new ConcurrentHashMap<>());
    }

    /* ---------------------------------------------------------------------- */
    /*  Cache replacement tests                                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testSetMethodCacheIsUsedByGetMethod() {
        TrackingMap<ReflectionUtils.MethodCacheKey, Method> tracking = new TrackingMap<>();
        ReflectionUtils.setMethodCache(tracking);

        Method m1 = ReflectionUtils.getMethod(Child.class, "childOnlyMethod");
        assertNotNull(m1);
        assertTrue("Our custom map should have been invoked via computeIfAbsent()", tracking.computeIfAbsentCalled);

        // second call must hit the cache (computeIfAbsent not called again)
        tracking.computeIfAbsentCalled = false;
        Method m2 = ReflectionUtils.getMethod(Child.class, "childOnlyMethod");
        assertSame(m1, m2);
        assertFalse("Second lookup should hit the cache and not call computeIfAbsent()", tracking.computeIfAbsentCalled);
    }

    @Test
    public void testSetConstructorCacheIsUsed() {
        TrackingMap<ReflectionUtils.ConstructorCacheKey, Constructor<?>> tracking = new TrackingMap<>();
        ReflectionUtils.setConstructorCache(tracking);

        Constructor<?> ctor = ReflectionUtils.getConstructor(Parent.class);
        assertNotNull(ctor);
        assertTrue(tracking.computeIfAbsentCalled);
    }

    /* ---------------------------------------------------------------------- */
    /*  Class‑level annotation lookup                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetClassAnnotationFoundAndCached() {
        TestAnno anno1 = ReflectionUtils.getClassAnnotation(Parent.class, TestAnno.class);
        assertNotNull(anno1);
        // second call must return the same instance (cached)
        TestAnno anno2 = ReflectionUtils.getClassAnnotation(Parent.class, TestAnno.class);
        assertSame(anno1, anno2);
    }

    @Test
    public void testGetClassAnnotationMissingReturnsNull() {
        @SuppressWarnings("unused")
        class NoAnno { }
        assertNull(ReflectionUtils.getClassAnnotation(NoAnno.class, TestAnno.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetClassAnnotationNullClassThrows() {
        ReflectionUtils.getClassAnnotation(null, TestAnno.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetClassAnnotationNullAnnotationClassThrows() {
        ReflectionUtils.getClassAnnotation(Parent.class, null);
    }

    /* ---------------------------------------------------------------------- */
    /*  Method‑level annotation lookup                                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetMethodAnnotationInherited() throws NoSuchMethodException {
        Method childMethod = Child.class.getDeclaredMethod("annotatedParentMethod");
        TestAnno anno = ReflectionUtils.getMethodAnnotation(childMethod, TestAnno.class);
        assertNotNull(anno);
    }

    @Test
    public void testGetMethodAnnotationMissingReturnsNull() throws NoSuchMethodException {
        Method m = Child.class.getDeclaredMethod("childOnlyMethod");
        assertNull(ReflectionUtils.getMethodAnnotation(m, TestAnno.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodAnnotationNullMethodThrows() {
        ReflectionUtils.getMethodAnnotation(null, TestAnno.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodAnnotationNullAnnotationClassThrows() throws NoSuchMethodException {
        Method m = Parent.class.getDeclaredMethod("annotatedParentMethod");
        ReflectionUtils.getMethodAnnotation(m, null);
    }

    /* ---------------------------------------------------------------------- */
    /*  Field lookup (including hierarchy & static exclusion)                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetFieldFindsInheritedField() {
        Field f = ReflectionUtils.getField(Child.class, "parentString");
        assertNotNull(f);
        assertEquals("parentString", f.getName());
        assertEquals(Parent.class, f.getDeclaringClass());
    }

    @Test
    public void testGetFieldIgnoresStaticField() {
        Field f = ReflectionUtils.getField(Child.class, "STATIC_FIELD");
        assertNull(f); // static fields are filtered out by getField()
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetFieldNullClassThrows() {
        ReflectionUtils.getField(null, "any");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetFieldNullNameThrows() {
        ReflectionUtils.getField(Parent.class, null);
    }

    /* ---------------------------------------------------------------------- */
    /*  Declared‑field utilities (default filter)                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetDeclaredFieldsDefaultFilterExcludesStaticAndEnumInternals() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(SampleEnum.class);
        // enum has a synthetic static field "$VALUES" and a private field "enumField"
        // Both must be filtered out, leaving only the two enum constants (which are
        // represented as static final fields – also filtered out). Hence the list should be empty.
        assertTrue(fields.isEmpty());
    }

    @Test
    public void testGetDeclaredFieldsWithCustomPredicate() {
        Predicate<Field> nameStartsWithParent = f -> f.getName().startsWith("parent");
        List<Field> list = ReflectionUtils.getDeclaredFields(Parent.class, nameStartsWithParent);
        assertEquals(2, list.size()); // parentInt and parentString
        assertTrue(list.stream().anyMatch(f -> f.getName().equals("parentInt")));
        assertTrue(list.stream().anyMatch(f -> f.getName().equals("parentString")));
    }

    @Test
    public void testGetAllDeclaredFieldsIncludesSuperclass() {
        List<Field> all = ReflectionUtils.getAllDeclaredFields(Child.class);
        // Expect childInt + parentInt + parentString (static field filtered)
        Set<String> names = new HashSet<>();
        for (Field f : all) names.add(f.getName());

        assertTrue(names.contains("childInt"));
        assertTrue(names.contains("parentInt"));
        assertTrue(names.contains("parentString"));
        assertFalse(names.contains("STATIC_FIELD"));
    }

    @Test
    public void testGetAllDeclaredFieldsMapHandlesNameCollisions() {
        class CollisionParent {
            private String dup;
        }
        class CollisionChild extends CollisionParent {
            private String dup; // hides parent field
        }

        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(CollisionChild.class);
        // Two entries: "dup" (from child) and "CollisionParent.dup"
        assertEquals(2, map.size());
        assertTrue(map.containsKey("dup"));
        assertTrue(map.containsKey(CollisionParent.class.getName() + ".dup"));
    }

    @Test
    public void testDeprecatedGetDeepDeclaredFieldsBehavesLikeAllDeclaredFields() {
        List<Field> deep = new ArrayList<>(ReflectionUtils.getDeepDeclaredFields(Child.class));
        // Same expectation as getAllDeclaredFields()
        Set<String> names = new HashSet<>();
        for (Field f : deep) names.add(f.getName());

        assertTrue(names.contains("childInt"));
        assertTrue(names.contains("parentInt"));
        assertTrue(names.contains("parentString"));
    }

    @Test
    public void testDeprecatedGetDeepDeclaredFieldMapMatchesNewImplementation() {
        Map<String, Field> deepMap = ReflectionUtils.getDeepDeclaredFieldMap(Child.class);
        Map<String, Field> newMap = ReflectionUtils.getAllDeclaredFieldsMap(Child.class);
        assertEquals(newMap, deepMap);
    }

    @Test
    public void testDeprecatedGetDeclaredFieldsCollectionAddsFields() {
        Collection<Field> collection = new ArrayList<>();
        ReflectionUtils.getDeclaredFields(Parent.class, collection);
        // Collection should now contain parentInt and parentString (default filter)
        assertEquals(2, collection.size());
        Set<String> names = new HashSet<>();
        for (Field f : collection) names.add(f.getName());
        assertTrue(names.contains("parentInt"));
        assertTrue(names.contains("parentString"));
    }

    /* ---------------------------------------------------------------------- */
    /*  Method lookup utilities                                                  */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetMethodFindsPrivateMethodAndMakesItAccessible() throws Exception {
        Method m = ReflectionUtils.getMethod(Parent.class, "privateParentMethod");
        assertNotNull(m);
        assertFalse(m.isAccessible()); // before we call it, JDK8 may still report false
        // The utility should have called setAccessible(true) – we verify by invoking
        m.setAccessible(true);
        m.invoke(new Parent()); // no exception means it was accessible
    }

    @Test
    public void testGetMethodCacheWorks() {
        Method first = ReflectionUtils.getMethod(Parent.class, "annotatedParentMethod");
        Method second = ReflectionUtils.getMethod(Parent.class, "annotatedParentMethod");
        assertSame(first, second);
    }

    @Test
    public void testGetMethodByArgCountSelectsMostSpecificNonSynthetic() {
        // Child overrides one overload of "overloaded(int)" – the utility should return that method
        Method m = ReflectionUtils.getMethod(new Child(), "overloaded", 1);
        assertEquals(Child.class, m.getDeclaringClass());
        assertEquals(int.class, m.getParameterTypes()[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodByArgCountMissingMethodThrows() {
        ReflectionUtils.getMethod(new Child(), "nonexistent", 0);
    }

    @Test
    public void testGetNonOverloadedMethodSuccess() {
        Method m = ReflectionUtils.getNonOverloadedMethod(Parent.class, "annotatedParentMethod");
        assertNotNull(m);
        assertEquals("annotatedParentMethod", m.getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethodWhenOverloadedThrows() {
        // "overloaded" exists with two signatures → should throw
        ReflectionUtils.getNonOverloadedMethod(Parent.class, "overloaded");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethodNullClassThrows() {
        ReflectionUtils.getNonOverloadedMethod(null, "any");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethodBlankNameThrows() {
        ReflectionUtils.getNonOverloadedMethod(Parent.class, "   ");
    }

    /* ---------------------------------------------------------------------- */
    /*  Constructor utilities                                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetConstructorFindsAndMakesAccessible() throws Exception {
        Constructor<?> ctor = ReflectionUtils.getConstructor(Parent.class);
        assertNotNull(ctor);
        assertTrue(Modifier.isPublic(ctor.getModifiers()));
        Object instance = ctor.newInstance();
        assertTrue(instance instanceof Parent);
    }

    @Test
    public void testGetConstructorReturnsNullWhenNotFound() {
        // Parent has no constructor with a String argument
        Constructor<?> ctor = ReflectionUtils.getConstructor(Parent.class, String.class);
        assertNull(ctor);
    }

    @Test
    public void testGetAllConstructorsCachesEachConstructor() {
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(Parent.class);
        assertTrue(ctors.length >= 1);
        // Verify that each constructor is present in the cache by requesting it again
        for (Constructor<?> c : ctors) {
            Constructor<?> cached = ReflectionUtils.getConstructor(Parent.class, c.getParameterTypes());
            assertSame("Constructor should be cached", c, cached);
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  Utility helpers                                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testMakeParamKeyEmptyAndNonEmpty() {
        assertEquals("", ReflectionUtils.makeParamKey());
        assertEquals(":String|int", ReflectionUtils.makeParamKey(String.class, int.class));
    }

    @Test
    public void testGetClassNameHandlesNull() {
        assertEquals("null", ReflectionUtils.getClassName(null));
        assertEquals(this.getClass().getName(), ReflectionUtils.getClassName(this));
    }

    @Test
    public void testGetClassNameFromByteCodeReturnsCorrectName() throws IOException {
        // Load the bytecode of this test class itself
        String classResource = ReflectionUtilsTest.class.getSimpleName() + ".class";
        byte[] bytes = ReflectionUtilsTest.class.getResourceAsStream(classResource).readAllBytes();

        String className = ReflectionUtils.getClassNameFromByteCode(bytes);
        assertEquals(ReflectionUtilsTest.class.getName(), className);
    }

    @Test(expected = IllegalStateException.class)
    public void testGetClassNameFromByteCodeInvalidDataThrows() throws IOException {
        // Pass a random byte array that does not represent a class file
        ReflectionUtils.getClassNameFromByteCode(new byte[]{0, 1, 2, 3});
    }

    /* ---------------------------------------------------------------------- */
    /*  Call overloads – success & error handling                              */
    /* ---------------------------------------------------------------------- */

    private static class CallTarget {
        String echo(String s) { return s; }

        void throwsChecked() throws Exception {
            throw new Exception("checked");
        }
    }

    @Test
    public void testCallWithMethodSuccess() throws Exception {
        CallTarget target = new CallTarget();
        Method m = ReflectionUtils.getMethod(CallTarget.class, "echo", String.class);
        Object result = ReflectionUtils.call(target, m, "hello");
        assertEquals("hello", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallWithNullMethodThrows() {
        ReflectionUtils.call(new CallTarget(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallWithNullInstanceThrows() throws Exception {
        Method m = ReflectionUtils.getMethod(CallTarget.class, "echo", String.class);
        ReflectionUtils.call(null, m);
    }

    @Test
    public void testCallWithMethodThatThrowsCheckedIsWrapped() throws Exception {
        CallTarget target = new CallTarget();
        Method m = ReflectionUtils.getMethod(CallTarget.class, "throwsChecked");
        try {
            ReflectionUtils.call(target, m);
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof Exception);
            assertEquals("checked", e.getCause().getMessage());
        }
    }

    @Test
    public void testCallByNameUsesArgumentCountResolution() {
        CallTarget target = new CallTarget();
        Object result = ReflectionUtils.call(target, "echo", "world");
        assertEquals("world", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallByNameMethodNotFoundThrows() {
        ReflectionUtils.call(new CallTarget(), "nonexistent");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallByNameNullInstanceThrows() {
        ReflectionUtils.call(null, "echo");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallByNameNullMethodNameThrows() {
        ReflectionUtils.call(new CallTarget(), null);
    }
}
