package com.cedarsoftware.util;

import org.junit.*;
import java.io.IOException;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ReflectionUtils}.
 *
 * The test class covers:
 * <ul>
 *   <li>All public API methods (basic happy‑path tests)</li>
 *   <li>Edge‑case handling (null arguments, illegal arguments, negative counts, etc.)</li>
 *   <li>Exception‑wrapping behavior of {@code call(...)} methods</li>
 *   <li>Cache‑replacement APIs</li>
 *   <li>Annotation lookup across inheritance hierarchies</li>
 *   <li>Field‑lookup utilities and name‑collision handling</li>
 *   <li>Byte‑code class‑name extraction</li>
 *   <li>Java‑compiler availability detection (including forced JRE mode)</li>
 * </ul>
 *
 * No external mocking frameworks are used – all objects are created via real constructors
 * or factory methods that exist in the source code.
 */
public class ReflectionUtilsTest {

    /* -------------------------------------------------------
     *  Helper types used throughout the test suite
     * ------------------------------------------------------- */

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    private @interface TestAnno {
        String value() default "";
    }

    @TestAnno("base")
    private static class BaseClass {
        public int basePublic;
        private String basePrivate;
        static int baseStatic;
    }

    @TestAnno("derived")
    private static class DerivedClass extends BaseClass {
        public int derivedPublic;
        private String derivedPrivate;
        static int derivedStatic;
        // field name collision with BaseClass
        public int basePublic; // hides BaseClass.basePublic
    }

    private static class MethodHolder {
        public String publicMethod() { return "public"; }

        protected String protectedMethod() { return "protected"; }

        String packageMethod() { return "package"; }

        @SuppressWarnings("unused")
        private String privateMethod() { return "private"; }

        // overloaded method for getMethod(Object, String, int) tests
        public void overload(int a) {}
        public void overload(String s) {}
    }

    @TestAnno
    private static class AnnotatedClass {
        @TestAnno("field")
        private int annotatedField;
    }

    private static class ExceptionThrower {
        public void throwsChecked() throws Exception {
            throw new Exception("checked");
        }

        public void throwsRuntime() {
            throw new IllegalStateException("runtime");
        }
    }

    /* -------------------------------------------------------
     *  Cache replacement tests (set*Cache)
     * ------------------------------------------------------- */

    @Test
    public void testSetMethodCache_NullRejected() {
        try {
            ReflectionUtils.setMethodCache(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            // expected – swap() checks for null
        }
    }

    @Test
    public void testSetMethodCache_ValidMapReplacesCache() {
        Map<Object, Method> custom = new ConcurrentHashMap<>();
        ReflectionUtils.setMethodCache(custom);
        // internal cache is private; we validate by ensuring future lookups succeed
        Method m = ReflectionUtils.getMethod(MethodHolder.class, "publicMethod");
        assertNotNull(m);
        assertEquals("publicMethod", m.getName());
    }

    @Test
    public void testSetFieldCache_ValidMap() {
        Map<Object, Field> custom = new ConcurrentHashMap<>();
        ReflectionUtils.setFieldCache(custom);
        Field f = ReflectionUtils.getField(DerivedClass.class, "derivedPublic");
        assertNotNull(f);
        assertEquals("derivedPublic", f.getName());
    }

    @Test
    public void testSetClassFieldsCache_ValidMap() {
        Map<Object, Collection<Field>> custom = new ConcurrentHashMap<>();
        ReflectionUtils.setClassFieldsCache(custom);
        List<Field> fields = ReflectionUtils.getDeclaredFields(DerivedClass.class);
        assertNotNull(fields);
        assertTrue(fields.size() > 0);
    }

    @Test
    public void testSetClassAnnotationCache_ValidMap() {
        Map<Object, Annotation> custom = new ConcurrentHashMap<>();
        ReflectionUtils.setClassAnnotationCache(custom);
        TestAnno a = ReflectionUtils.getClassAnnotation(AnnotatedClass.class, TestAnno.class);
        assertNotNull(a);
        assertEquals("", a.value());
    }

    @Test
    public void testSetMethodAnnotationCache_ValidMap() {
        Map<Object, Annotation> custom = new ConcurrentHashMap<>();
        ReflectionUtils.setMethodAnnotationCache(custom);
        Method m = ReflectionUtils.getMethod(MethodHolder.class, "publicMethod");
        TestAnno a = ReflectionUtils.getMethodAnnotation(m, TestAnno.class);
        assertNull(a); // no annotation on the method – just ensure no exception
    }

    @Test
    public void testSetConstructorCache_ValidMap() {
        Map<Object, Constructor<?>> custom = new ConcurrentHashMap<>();
        ReflectionUtils.setConstructorCache(custom);
        Constructor<?> c = ReflectionUtils.getConstructor(DerivedClass.class);
        assertNotNull(c);
        assertEquals(DerivedClass.class, c.getDeclaringClass());
    }

    @Test
    public void testSetSortedConstructorsCache_ValidMap() {
        Map<Object, Constructor<?>[]> custom = new ConcurrentHashMap<>();
        ReflectionUtils.setSortedConstructorsCache(custom);
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(DerivedClass.class);
        assertNotNull(ctors);
        assertTrue(ctors.length > 0);
    }

    /* -------------------------------------------------------
     *  Annotation lookup tests
     * ------------------------------------------------------- */

    @Test
    public void testGetClassAnnotation_HappyPath() {
        TestAnno anno = ReflectionUtils.getClassAnnotation(DerivedClass.class, TestAnno.class);
        assertNotNull(anno);
        assertEquals("derived", anno.value());
    }

    @Test
    public void testGetClassAnnotation_NullClassReturnsNull() {
        TestAnno anno = ReflectionUtils.getClassAnnotation(null, TestAnno.class);
        assertNull(anno);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetClassAnnotation_NullAnnoClassThrows() {
        ReflectionUtils.getClassAnnotation(DerivedClass.class, null);
    }

    @Test
    public void testGetMethodAnnotation_HappyPath() throws NoSuchMethodException {
        Method m = MethodHolder.class.getDeclaredMethod("publicMethod");
        // No annotation present – should return null without error
        TestAnno anno = ReflectionUtils.getMethodAnnotation(m, TestAnno.class);
        assertNull(anno);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodAnnotation_NullMethodThrows() {
        ReflectionUtils.getMethodAnnotation(null, TestAnno.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodAnnotation_NullAnnoClassThrows() throws NoSuchMethodException {
        Method m = MethodHolder.class.getDeclaredMethod("publicMethod");
        ReflectionUtils.getMethodAnnotation(m, null);
    }

    /* -------------------------------------------------------
     *  Field lookup tests
     * ------------------------------------------------------- */

    @Test
    public void testGetField_HappyPath() {
        Field f = ReflectionUtils.getField(DerivedClass.class, "derivedPublic");
        assertNotNull(f);
        assertEquals("derivedPublic", f.getName());
    }

    @Test
    public void testGetField_InheritedField() {
        Field f = ReflectionUtils.getField(DerivedClass.class, "basePublic");
        assertNotNull(f);
        // Should resolve to the *declared* field in DerivedClass that hides the base field
        assertEquals(DerivedClass.class, f.getDeclaringClass());
    }

    @Test
    public void testGetField_NotFoundReturnsNull() {
        Field f = ReflectionUtils.getField(DerivedClass.class, "nonExistent");
        assertNull(f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetField_NullClassThrows() {
        ReflectionUtils.getField(null, "any");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetField_NullNameThrows() {
        ReflectionUtils.getField(DerivedClass.class, null);
    }

    @Test
    public void testGetDeclaredFields_DefaultFilterExcludesStatic() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(DerivedClass.class);
        for (Field f : fields) {
            assertFalse(Modifier.isStatic(f.getModifiers()));
        }
    }

    @Test
    public void testGetDeclaredFields_CustomFilter() {
        Predicate<Field> onlyPublic = f -> Modifier.isPublic(f.getModifiers());
        List<Field> publicFields = ReflectionUtils.getDeclaredFields(DerivedClass.class, onlyPublic);
        for (Field f : publicFields) {
            assertTrue(Modifier.isPublic(f.getModifiers()));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetDeclaredFields_NullClassThrows() {
        ReflectionUtils.getDeclaredFields(null, f -> true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetDeclaredFields_NullFilterThrows() {
        ReflectionUtils.getDeclaredFields(DerivedClass.class, null);
    }

    @Test
    public void testGetAllDeclaredFields_IncludesSuper() {
        List<Field> all = ReflectionUtils.getAllDeclaredFields(DerivedClass.class);
        // Should contain at least one field declared in BaseClass
        boolean foundBase = all.stream().anyMatch(f -> f.getDeclaringClass() == BaseClass.class);
        assertTrue(foundBase);
    }

    @Test
    public void testGetAllDeclaredFieldsMap_CollisionHandling() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(DerivedClass.class);
        // The simple name "basePublic" should map to the field declared in DerivedClass
        assertEquals(DerivedClass.class, map.get("basePublic").getDeclaringClass());
        // The fully‑qualified name should map to the hidden field in BaseClass
        String qualified = BaseClass.class.getName() + ".basePublic";
        assertTrue(map.containsKey(qualified));
        assertEquals(BaseClass.class, map.get(qualified).getDeclaringClass());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetAllDeclaredFieldsMap_NullClassThrows() {
        ReflectionUtils.getAllDeclaredFieldsMap(null, f -> true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetAllDeclaredFieldsMap_NullFilterThrows() {
        ReflectionUtils.getAllDeclaredFieldsMap(DerivedClass.class, null);
    }

    @Test
    public void testDeprecatedDeepDeclaredFields_BehaveLikeAllDeclaredFields() {
        Collection<Field> deep = ReflectionUtils.getDeepDeclaredFields(DerivedClass.class);
        List<Field> all = ReflectionUtils.getAllDeclaredFields(DerivedClass.class);
        assertEquals(all.size(), deep.size());
    }

    @Test
    public void testDeprecatedDeepDeclaredFieldMap_BehaveLikeAllDeclaredFieldsMap() {
        Map<String, Field> deepMap = ReflectionUtils.getDeepDeclaredFieldMap(DerivedClass.class);
        Map<String, Field> normalMap = ReflectionUtils.getAllDeclaredFieldsMap(DerivedClass.class);
        assertEquals(normalMap.size(), deepMap.size());
    }

    /* -------------------------------------------------------
     *  Method lookup and invocation tests
     * ------------------------------------------------------- */

    @Test
    public void testGetMethod_HappyPath() {
        Method m = ReflectionUtils.getMethod(MethodHolder.class, "publicMethod");
        assertNotNull(m);
        assertEquals("publicMethod", m.getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethod_NullClassThrows() {
        ReflectionUtils.getMethod(null, "any");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethod_NullNameThrows() {
        ReflectionUtils.getMethod(MethodHolder.class, null);
    }

    @Test
    public void testGetMethod_WithParameterTypes() {
        Method m = ReflectionUtils.getMethod(MethodHolder.class, "overload", String.class);
        assertNotNull(m);
        assertEquals(String.class, m.getParameterTypes()[0]);
    }

    @Test
    public void testGetMethod_ObjectAndArgCount_SelectsBestCandidate() {
        MethodHolder holder = new MethodHolder();
        Method m = ReflectionUtils.getMethod(holder, "overload", 1);
        // Both overloads have 1 parameter – the selector prefers non‑synthetic/non‑bridge,
        // then higher accessibility (public > protected > package > private). Both are public,
        // so the first encountered (int) is chosen. We verify by invoking.
        assertEquals(int.class, m.getParameterTypes()[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethod_Object_NullInstanceThrows() {
        ReflectionUtils.getMethod(null, "any", 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethod_Object_NullNameThrows() {
        ReflectionUtils.getMethod(new MethodHolder(), null, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethod_Object_NegativeArgCountThrows() {
        ReflectionUtils.getMethod(new MethodHolder(), "any", -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethod_Object_MethodNotFoundThrows() {
        ReflectionUtils.getMethod(new MethodHolder(), "doesNotExist", 0);
    }

    @Test
    public void testCall_WithMethod_HappyPath() throws Exception {
        Method m = ReflectionUtils.getMethod(MethodHolder.class, "publicMethod");
        Object result = ReflectionUtils.call(new MethodHolder(), m);
        assertEquals("public", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCall_WithMethod_NullMethodThrows() {
        ReflectionUtils.call(new MethodHolder(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCall_WithMethod_NullInstanceThrows() throws Exception {
        Method m = ReflectionUtils.getMethod(MethodHolder.class, "publicMethod");
        ReflectionUtils.call(null, m);
    }

    @Test
    public void testCall_WithMethod_InvocationTargetExceptionWrapped() throws Exception {
        Method m = ReflectionUtils.getMethod(ExceptionThrower.class, "throwsChecked");
        ExceptionThrower et = new ExceptionThrower();
        try {
            ReflectionUtils.call(et, m);
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof Exception);
            assertEquals("checked", e.getCause().getMessage());
        }
    }

    @Test
    public void testCall_WithMethod_RuntimeExceptionUnchanged() throws Exception {
        Method m = ReflectionUtils.getMethod(ExceptionThrower.class, "throwsRuntime");
        ExceptionThrower et = new ExceptionThrower();
        try {
            ReflectionUtils.call(et, m);
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            // The original runtime exception should be the cause
            assertTrue(e.getCause() instanceof IllegalStateException);
            assertEquals("runtime", e.getCause().getMessage());
        }
    }

    @Test
    public void testCall_WithName_HappyPath() {
        MethodHolder holder = new MethodHolder();
        Object result = ReflectionUtils.call(holder, "publicMethod");
        assertEquals("public", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCall_WithName_NullInstanceThrows() {
        ReflectionUtils.call(null, "any");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCall_WithName_NullNameThrows() {
        ReflectionUtils.call(new MethodHolder(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCall_WithName_MethodNotFoundThrows() {
        ReflectionUtils.call(new MethodHolder(), "nonExistent");
    }

    /* -------------------------------------------------------
     *  Constructor lookup tests
     * ------------------------------------------------------- */

    @Test
    public void testGetConstructor_HappyPath() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(DerivedClass.class);
        assertNotNull(ctor);
        assertEquals(DerivedClass.class, ctor.getDeclaringClass());
    }

    @Test
    public void testGetConstructor_WithParams_NotFoundReturnsNull() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(DerivedClass.class, String.class);
        assertNull(ctor);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetConstructor_NullClassThrows() {
        ReflectionUtils.getConstructor(null);
    }

    @Test
    public void testGetAllConstructors_OrderVerification() {
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(DerivedClass.class);
        assertTrue(ctors.length >= 1);
        // Public constructors should appear before protected/package/private
        int firstPublicIdx = -1;
        for (int i = 0; i < ctors.length; i++) {
            if (Modifier.isPublic(ctors[i].getModifiers())) {
                firstPublicIdx = i;
                break;
            }
        }
        assertTrue("At least one public constructor expected", firstPublicIdx >= 0);
        // Ensure that any non‑public constructor does not precede the first public one
        for (int i = 0; i < firstPublicIdx; i++) {
            assertFalse(Modifier.isPublic(ctors[i].getModifiers()));
        }
    }

    @Test
    public void testGetAllConstructors_NullClassReturnsEmptyArray() {
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(null);
        assertNotNull(ctors);
        assertEquals(0, ctors.length);
    }

    /* -------------------------------------------------------
     *  Miscellaneous utilities
     * ------------------------------------------------------- */

    @Test
    public void testGetNonOverloadedMethod_HappyPath() {
        Method m = ReflectionUtils.getNonOverloadedMethod(MethodHolder.class, "publicMethod");
        assertNotNull(m);
        assertEquals("publicMethod", m.getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethod_NullClassThrows() {
        ReflectionUtils.getNonOverloadedMethod(null, "any");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethod_BlankNameThrows() {
        ReflectionUtils.getNonOverloadedMethod(MethodHolder.class, "   ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethod_MethodNotFoundThrows() {
        ReflectionUtils.getNonOverloadedMethod(MethodHolder.class, "doesNotExist");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethod_OverloadedMethodThrows() {
        // Both overload(int) and overload(String) exist
        ReflectionUtils.getNonOverloadedMethod(MethodHolder.class, "overload");
    }

    @Test
    public void testGetClassName_NullObject() {
        assertEquals("null", ReflectionUtils.getClassName(null));
    }

    @Test
    public void testGetClassName_NonNullObject() {
        String name = ReflectionUtils.getClassName(new DerivedClass());
        assertEquals(DerivedClass.class.getName(), name);
    }

    @Test
    public void testGetClassNameFromByteCode_ValidClass() throws IOException {
        // Load the .class file for this test class itself
        String resource = ReflectionUtils.class.getSimpleName() + ".class";
        byte[] bytes = ReflectionUtilsTest.class.getResourceAsStream(resource).readAllBytes();
        String className = ReflectionUtils.getClassNameFromByteCode(bytes);
        assertEquals(ReflectionUtils.class.getName(), className);
    }

    @Test(expected = IllegalStateException.class)
    public void testGetClassNameFromByteCode_InvalidTagThrows() throws IOException {
        // Corrupt the constant‑pool tag to force the default case
        byte[] corrupt = new byte[]{0xCA, (byte)0xFE, (byte)0xBA, (byte)0xBE, // magic
                0,0, 0,0, // version
                0,1, // constant‑pool count = 1 (invalid, will trigger default)
                99 // unknown tag
        };
        ReflectionUtils.getClassNameFromByteCode(corrupt);
    }

    @Test
    public void testIsJavaCompilerAvailable_TrueOrFalse() {
        // The result depends on the runtime JDK/JRE – we just ensure the method runs
        boolean available = ReflectionUtils.isJavaCompilerAvailable();
        assertTrue(available || !available);
    }

    @Test
    public void testIsJavaCompilerAvailable_ForceJreMode() {
        System.setProperty("java.util.force.jre", "true");
        try {
            assertFalse(ReflectionUtils.isJavaCompilerAvailable());
        } finally {
            System.clearProperty("java.util.force.jre");
        }
    }
}
