package com.cedarsoftware.util;

import org.junit.*;
import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Predicate;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ReflectionUtils}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public API methods</li>
 *   <li>Typical usage scenarios</li>
 *   <li>Edge‑cases, null handling and exception wrapping</li>
 *   <li>Cache behaviour (custom cache injection)</li>
 *   <li>Annotation lookup in class hierarchies</li>
 *   <li>Field and method selection logic</li>
 *   <li>Byte‑code class name extraction (valid & invalid data)</li>
 * </ul>
 */
public class ReflectionUtilsTest {

    /* ---------------------------------------------------------------------- */
    /* Helper types used by many tests                                        */
    /* ---------------------------------------------------------------------- */

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
    private @interface TestAnno { }

    @TestAnno
    private static class BaseAnnotated { }

    private static class SubAnnotated extends BaseAnnotated { }

    private interface InterfaceWithAnno {
        @TestAnno
        void annotatedMethod();
        void nonAnnotatedMethod();
    }

    private static class ImplementsInterface implements InterfaceWithAnno {
        @Override public void annotatedMethod() {}
        @Override public void nonAnnotatedMethod() {}
    }

    private static class FieldHolder {
        private String privateField = "secret";
        public int publicField = 42;
        static long staticField = 999L; // should be ignored by the default filter
        @TestAnno private int annotatedField = 7;
    }

    private static class MethodHolder {
        private void privateVoid() {}
        public String publicZeroArgs() { return "zero"; }
        public String overloaded(String s) { return "one:" + s; }
        public String overloaded(String s, int i) { return "two:" + s + i; }
        public String bridgeMethod() { return "bridge"; } // not a real bridge, just to have multiple candidates
        public void throwsChecked() throws IOException { throw new IOException("boom"); }
    }

    private static class ConstructorHolder {
        private ConstructorHolder() {}
        public ConstructorHolder(String s) {}
    }

    private static class CollisionParent {
        private int dup = 1;
    }

    private static class CollisionChild extends CollisionParent {
        private int dup = 2;
    }

    /* ---------------------------------------------------------------------- */
    /* Cache injection helpers                                                 */
    /* ---------------------------------------------------------------------- */

    private static class SimpleMap<K,V> extends HashMap<K,V> implements Map<K,V> { }

    @Before
    public void resetCaches() {
        // Restore default LRUCache implementations (the production code uses LRUCache,
        // but for tests a plain HashMap works fine – we replace them with fresh maps).
        ReflectionUtils.setMethodCache(new SimpleMap<>());
        ReflectionUtils.setClassFieldsCache(new SimpleMap<>());
        ReflectionUtils.setFieldCache(new SimpleMap<>());
        ReflectionUtils.setClassAnnotationCache(new SimpleMap<>());
        ReflectionUtils.setMethodAnnotationCache(new SimpleMap<>());
        ReflectionUtils.setConstructorCache(new SimpleMap<>());
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for cache‑setter methods                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testSetMethodCacheIsUsed() throws Exception {
        SimpleMap<Object, Method> custom = new SimpleMap<>();
        ReflectionUtils.setMethodCache(custom);

        Method m = ReflectionUtils.getMethod(MethodHolder.class, "publicZeroArgs");
        assertNotNull(m);
        // The custom map should now contain the cache key for the lookup
        assertTrue(custom.keySet().stream().anyMatch(k -> k instanceof ReflectionUtils.MethodCacheKey));
    }

    @Test
    public void testSetFieldCacheIsUsed() throws Exception {
        SimpleMap<Object, Field> custom = new SimpleMap<>();
        ReflectionUtils.setFieldCache(custom);

        Field f = ReflectionUtils.getField(FieldHolder.class, "privateField");
        assertNotNull(f);
        assertTrue(custom.keySet().stream().anyMatch(k -> k instanceof ReflectionUtils.FieldNameCacheKey));
    }

    /* ---------------------------------------------------------------------- */
    /* getClassAnnotation – normal and edge cases                              */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetClassAnnotationFoundInSuperclass() {
        TestAnno anno = ReflectionUtils.getClassAnnotation(SubAnnotated.class, TestAnno.class);
        assertNotNull("Annotation should be inherited from BaseAnnotated", anno);
    }

    @Test
    public void testGetClassAnnotationReturnsNullWhenMissing() {
        TestAnno anno = ReflectionUtils.getClassAnnotation(String.class, TestAnno.class);
        assertNull("No TestAnno on String", anno);
    }

    @Test
    public void testGetClassAnnotationNullClassReturnsNull() {
        TestAnno anno = ReflectionUtils.getClassAnnotation(null, TestAnno.class);
        assertNull(anno);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetClassAnnotationNullAnnotationClassThrows() {
        ReflectionUtils.getClassAnnotation(String.class, null);
    }

    /* ---------------------------------------------------------------------- */
    /* getMethodAnnotation – normal and edge cases                             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetMethodAnnotationFoundInInterface() throws Exception {
        Method m = InterfaceWithAnno.class.getMethod("annotatedMethod");
        TestAnno anno = ReflectionUtils.getMethodAnnotation(m, TestAnno.class);
        assertNotNull(anno);
    }

    @Test
    public void testGetMethodAnnotationNotFound() throws Exception {
        Method m = InterfaceWithAnno.class.getMethod("nonAnnotatedMethod");
        TestAnno anno = ReflectionUtils.getMethodAnnotation(m, TestAnno.class);
        assertNull(anno);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodAnnotationNullMethodThrows() {
        ReflectionUtils.getMethodAnnotation(null, TestAnno.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodAnnotationNullAnnoClassThrows() throws Exception {
        Method m = InterfaceWithAnno.class.getMethod("annotatedMethod");
        ReflectionUtils.getMethodAnnotation(m, null);
    }

    /* ---------------------------------------------------------------------- */
    /* getField – normal and edge cases                                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetFieldFindsPrivateField() {
        Field f = ReflectionUtils.getField(FieldHolder.class, "privateField");
        assertNotNull(f);
        assertEquals("privateField", f.getName());
        assertFalse(Modifier.isPublic(f.getModifiers()));
    }

    @Test
    public void testGetFieldIgnoresStaticField() {
        Field f = ReflectionUtils.getField(FieldHolder.class, "staticField");
        assertNull("Static fields are filtered out by default", f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetFieldNullClassThrows() {
        ReflectionUtils.getField(null, "any");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetFieldNullNameThrows() {
        ReflectionUtils.getField(FieldHolder.class, null);
    }

    /* ---------------------------------------------------------------------- */
    /* getDeclaredFields – custom predicate                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetDeclaredFieldsWithPredicate() {
        Predicate<Field> onlyAnnotated = f -> f.isAnnotationPresent(TestAnno.class);
        List<Field> list = ReflectionUtils.getDeclaredFields(FieldHolder.class, onlyAnnotated);
        assertEquals(1, list.size());
        assertEquals("annotatedField", list.get(0).getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetDeclaredFieldsNullClassThrows() {
        ReflectionUtils.getDeclaredFields(null, f -> true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetDeclaredFieldsNullPredicateThrows() {
        ReflectionUtils.getDeclaredFields(FieldHolder.class, null);
    }

    /* ---------------------------------------------------------------------- */
    /* getDeclaredFields – default filter                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetDeclaredFieldsDefaultFiltersStaticAndSynthetic() {
        List<Field> list = ReflectionUtils.getDeclaredFields(FieldHolder.class);
        // staticField should be filtered out, annotatedField and privateField stay
        Set<String> names = new HashSet<>();
        for (Field f : list) names.add(f.getName());
        assertTrue(names.contains("privateField"));
        assertTrue(names.contains("publicField"));
        assertTrue(names.contains("annotatedField"));
        assertFalse(names.contains("staticField"));
    }

    /* ---------------------------------------------------------------------- */
    /* getAllDeclaredFields – hierarchy traversal                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetAllDeclaredFieldsIncludesSuperclass() {
        List<Field> list = ReflectionUtils.getAllDeclaredFields(CollisionChild.class);
        Set<String> names = new HashSet<>();
        for (Field f : list) names.add(f.getName());
        // Both 'dup' fields should be present (different declaring classes)
        assertTrue(names.contains("dup"));
        // Ensure the child field appears before the parent (order test)
        assertTrue(list.indexOf(
                getFieldByName(CollisionChild.class, "dup")) <
                list.indexOf(getFieldByName(CollisionParent.class, "dup")));
    }

    private Field getFieldByName(Class<?> c, String name) {
        for (Field f : c.getDeclaredFields()) {
            if (f.getName().equals(name)) return f;
        }
        return null;
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetAllDeclaredFieldsNullClassThrows() {
        ReflectionUtils.getAllDeclaredFields(null);
    }

    /* ---------------------------------------------------------------------- */
    /* getAllDeclaredFieldsMap – name collision handling                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetAllDeclaredFieldsMapHandlesCollisions() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(CollisionChild.class);
        // Simple name 'dup' should map to the child field
        assertTrue(map.containsKey("dup"));
        assertEquals(CollisionChild.class, map.get("dup").getDeclaringClass());

        // Parent field should be stored under fully‑qualified name
        String fq = CollisionParent.class.getName() + ".dup";
        assertTrue(map.containsKey(fq));
        assertEquals(CollisionParent.class, map.get(fq).getDeclaringClass());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetAllDeclaredFieldsMapNullClassThrows() {
        ReflectionUtils.getAllDeclaredFieldsMap(null, f -> true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetAllDeclaredFieldsMapNullPredicateThrows() {
        ReflectionUtils.getAllDeclaredFieldsMap(FieldHolder.class, null);
    }

    /* ---------------------------------------------------------------------- */
    /* Deprecated APIs – still functional                                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDeprecatedGetDeepDeclaredFields() {
        Collection<Field> coll = ReflectionUtils.getDeepDeclaredFields(FieldHolder.class);
        // Should be same as default getAllDeclaredFields (minus transient/synthetic – none here)
        assertEquals(ReflectionUtils.getAllDeclaredFields(FieldHolder.class).size(), coll.size());
    }

    @Test
    public void testDeprecatedGetDeepDeclaredFieldMap() {
        Map<String, Field> map = ReflectionUtils.getDeepDeclaredFieldMap(FieldHolder.class);
        assertEquals(ReflectionUtils.getAllDeclaredFieldsMap(FieldHolder.class).size(), map.size());
    }

    @Test
    public void testDeprecatedGetDeclaredFieldsIntoCollection() {
        List<Field> list = new ArrayList<>();
        ReflectionUtils.getDeclaredFields(FieldHolder.class, list);
        assertFalse(list.isEmpty());
        // Should contain the same fields as getAllDeclaredFields (default filter)
        assertEquals(ReflectionUtils.getAllDeclaredFields(FieldHolder.class).size(), list.size());
    }

    /* ---------------------------------------------------------------------- */
    /* call(Object, Method) – normal and exception wrapping                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCallWithMethodReturnsResult() throws Exception {
        Method m = MethodHolder.class.getMethod("publicZeroArgs");
        Object result = ReflectionUtils.call(new MethodHolder(), m);
        assertEquals("zero", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallNullMethodThrows() {
        ReflectionUtils.call(new MethodHolder(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallNullInstanceThrows() throws Exception {
        Method m = MethodHolder.class.getMethod("publicZeroArgs");
        ReflectionUtils.call(null, m);
    }

    @Test
    public void testCallWrapsCheckedException() throws Exception {
        Method m = MethodHolder.class.getMethod("throwsChecked");
        try {
            ReflectionUtils.call(new MethodHolder(), m);
            fail("Expected RuntimeException wrapping IOException");
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof IOException);
            assertEquals("boom", e.getCause().getMessage());
        }
    }

    /* ---------------------------------------------------------------------- */
    /* call(Object, String) – method lookup by name & arg count                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCallByNameSimpleMethod() {
        Object result = ReflectionUtils.call(new MethodHolder(), "publicZeroArgs");
        assertEquals("zero", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallByNameMethodNotFoundThrows() {
        ReflectionUtils.call(new MethodHolder(), "nonexistentMethod");
    }

    /* ---------------------------------------------------------------------- */
    /* getMethod(Class, name, types) – private method access                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetMethodFindsPrivateMethodAndMakesAccessible() throws Exception {
        Method m = ReflectionUtils.getMethod(MethodHolder.class, "privateVoid");
        assertNotNull(m);
        assertTrue(m.isAccessible() || !Modifier.isPublic(m.getModifiers()));
        // Invoke to ensure accessibility works
        ReflectionUtils.call(new MethodHolder(), m);
    }

    @Test
    public void testGetMethodReturnsNullWhenNotFound() {
        Method m = ReflectionUtils.getMethod(MethodHolder.class, "doesNotExist");
        assertNull(m);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodNullClassThrows() {
        ReflectionUtils.getMethod(null, "any");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodNullNameThrows() {
        ReflectionUtils.getMethod(MethodHolder.class, null);
    }

    /* ---------------------------------------------------------------------- */
    /* getMethod(Object, name, argCount) – selection strategy                  */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetMethodByArgCountSelectsBestCandidate() {
        Method m = ReflectionUtils.getMethod(new MethodHolder(), "overloaded", 2);
        assertNotNull(m);
        assertEquals(2, m.getParameterCount());
        assertEquals("overloaded", m.getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodByArgCountNotFoundThrows() {
        ReflectionUtils.getMethod(new MethodHolder(), "overloaded", 3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodByArgCountNullInstanceThrows() {
        ReflectionUtils.getMethod(null, "any", 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodByArgCountNullNameThrows() {
        ReflectionUtils.getMethod(new MethodHolder(), null, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodByArgCountNegativeArgCountThrows() {
        ReflectionUtils.getMethod(new MethodHolder(), "any", -1);
    }

    /* ---------------------------------------------------------------------- */
    /* getConstructor – private and public constructors                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetConstructorPrivate() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(ConstructorHolder.class);
        assertNotNull(ctor);
        assertTrue(ctor.isAccessible() || !Modifier.isPublic(ctor.getModifiers()));
    }

    @Test
    public void testGetConstructorWithParameters() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(ConstructorHolder.class, String.class);
        assertNotNull(ctor);
        assertEquals(1, ctor.getParameterTypes().length);
    }

    @Test
    public void testGetConstructorReturnsNullWhenMissing() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(String.class, int.class);
        assertNull(ctor);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetConstructorNullClassThrows() {
        ReflectionUtils.getConstructor(null);
    }

    /* ---------------------------------------------------------------------- */
    /* getAllConstructors – array content matches declared constructors          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetAllConstructorsReturnsAllDeclared() {
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(ConstructorHolder.class);
        // Two constructors declared in the class
        assertEquals(2, ctors.length);
        Set<Integer> paramCounts = new HashSet<>();
        for (Constructor<?> c : ctors) paramCounts.add(c.getParameterTypes().length);
        assertTrue(paramCounts.contains(0));
        assertTrue(paramCounts.contains(1));
    }

    @Test
    public void testGetAllConstructorsWithNullClassReturnsEmptyArray() {
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(null);
        assertNotNull(ctors);
        assertEquals(0, ctors.length);
    }

    /* ---------------------------------------------------------------------- */
    /* getNonOverloadedMethod – success and overloaded error                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetNonOverloadedMethodSuccess() throws Exception {
        Method m = ReflectionUtils.getNonOverloadedMethod(MethodHolder.class, "publicZeroArgs");
        assertNotNull(m);
        assertEquals("publicZeroArgs", m.getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethodOverloadedThrows() {
        // MethodHolder has two overloads named "overloaded"
        ReflectionUtils.getNonOverloadedMethod(MethodHolder.class, "overloaded");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethodNullClassThrows() {
        ReflectionUtils.getNonOverloadedMethod(null, "any");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethodBlankNameThrows() {
        ReflectionUtils.getNonOverloadedMethod(MethodHolder.class, "  ");
    }

    /* ---------------------------------------------------------------------- */
    /* getClassName – null handling                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetClassNameHandlesNull() {
        assertEquals("null", ReflectionUtils.getClassName(null));
    }

    @Test
    public void testGetClassNameReturnsActualName() {
        assertEquals(String.class.getName(), ReflectionUtils.getClassName("test"));
    }

    /* ---------------------------------------------------------------------- */
    /* getClassNameFromByteCode – valid class and error scenarios               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetClassNameFromByteCodeValid() throws Exception {
        // Use the byte‑code of this test class itself (guaranteed to be on the classpath)
        String className = ReflectionUtils.getClassNameFromByteCode(
                readAllBytes(ReflectionUtilsTest.class.getResourceAsStream(
                        "/" + ReflectionUtilsTest.class.getName().replace('.', '/') + ".class")));
        assertEquals(ReflectionUtilsTest.class.getName(), className);
    }

    @Test(expected = IllegalStateException.class)
    public void testGetClassNameFromByteCodeInvalidTagThrows() throws Exception {
        // Corrupt the constant‑pool tag: first byte after the header will be 0 (invalid)
        byte[] corrupted = new byte[10];
        corrupted[0] = (byte)0xCA; // magic number high byte (just to pass the header)
        corrupted[1] = (byte)0xFE;
        corrupted[2] = (byte)0xBA;
        corrupted[3] = (byte)0xBE;
        // The next byte is the constant‑pool tag; set to an illegal value
        corrupted[8] = 0;
        ReflectionUtils.getClassNameFromByteCode(corrupted);
    }

    @Test(expected = IOException.class)
    public void testGetClassNameFromByteCodeTruncatedInputThrows() throws Exception {
        // Empty array – DataInputStream will hit EOF
        ReflectionUtils.getClassNameFromByteCode(new byte[0]);
    }

    private static byte[] readAllBytes(InputStream is) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) != -1) {
                baos.write(buf, 0, n);
            }
            return baos.toByteArray();
        }
    }
}
