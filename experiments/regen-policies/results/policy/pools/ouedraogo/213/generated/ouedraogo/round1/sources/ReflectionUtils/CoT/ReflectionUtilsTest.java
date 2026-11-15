package com.cedarsoftware.util;

import org.junit.*;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Comprehensive JUnit‑4 test suite for {@link ReflectionUtils}.
 * <p>
 * The tests are grouped by the public API method they exercise.
 * Each test verifies the normal behaviour and also the documented
 * edge‑cases (null arguments, illegal arguments, exception handling,
 * cache replacement etc.).
 * </p>
 */
public class ReflectionUtilsTest {

    /* --------------------------------------------------------------------- */
    /*  Helper types used by many test cases                                 */
    /* --------------------------------------------------------------------- */

    /** Simple runtime annotation used for class / method annotation tests. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    private @interface TestAnno {
        String value() default "";
    }

    /** Base class with a class‑level annotation. */
    @TestAnno("base")
    private static class BaseClass {
        private int privateField = 1;
        protected String protectedField = "base";

        @TestAnno("baseMethod")
        public void annotatedMethod() {}

        private void privateMethod() {}
    }

    /** Sub‑class inherits from {@link BaseClass} and adds its own members. */
    private static class SubClass extends BaseClass {
        public double publicField = 2.0;
        private static final String STATIC_FIELD = "ignore";

        @Override
        public void annotatedMethod() {} // overrides without annotation

        public void overloaded() {}
        public void overloaded(String s) {}
        public void overloaded(String s, int i) {}

        public void throwsChecked() throws Exception {
            throw new Exception("checked");
        }

        public void throwsRuntime() {
            throw new IllegalStateException("runtime");
        }
    }

    /** Interface with an annotated method – used for method‑annotation lookup. */
    private interface TestInterface {
        @TestAnno("interfaceMethod")
        void interfaceAnnotated();
    }

    /** Class that implements the interface – used for hierarchical method‑annotation lookup. */
    private static class InterfaceImpl implements TestInterface {
        public void interfaceAnnotated() {}
    }

    /** Enum used to verify the default field filter excludes internal enum fields. */
    private enum SampleEnum {
        ONE, TWO;
        private int hidden = 0;
    }

    /** Simple class with multiple constructors of varying visibility. */
    private static class MultiCtor {
        public MultiCtor() {}
        protected MultiCtor(String s) {}
        MultiCtor(int i, String s) {}
        private MultiCtor(double d) {}
    }

    /* --------------------------------------------------------------------- */
    /*  Test infrastructure – reset static caches after each test            */
    /* --------------------------------------------------------------------- */
    @After
    public void resetCaches() {
        // Replace every cache with a fresh, thread‑safe map so tests are independent.
        ReflectionUtils.setMethodCache(new ConcurrentHashMap<>());
        ReflectionUtils.setClassFieldsCache(new ConcurrentHashMap<>());
        ReflectionUtils.setFieldCache(new ConcurrentHashMap<>());
        ReflectionUtils.setClassAnnotationCache(new ConcurrentHashMap<>());
        ReflectionUtils.setMethodAnnotationCache(new ConcurrentHashMap<>());
        ReflectionUtils.setConstructorCache(new ConcurrentHashMap<>());
        ReflectionUtils.setSortedConstructorsCache(new ConcurrentHashMap<>());
    }

    /* --------------------------------------------------------------------- */
    /*  1. Cache‑setter methods – null argument handling                      */
    /* --------------------------------------------------------------------- */
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void setMethodCache_NullMap_ThrowsNPE() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("cache must not be null");
        ReflectionUtils.setMethodCache(null);
    }

    @Test
    public void setClassFieldsCache_NullMap_ThrowsNPE() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("cache must not be null");
        ReflectionUtils.setClassFieldsCache(null);
    }

    @Test
    public void setFieldCache_NullMap_ThrowsNPE() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("cache must not be null");
        ReflectionUtils.setFieldCache(null);
    }

    @Test
    public void setClassAnnotationCache_NullMap_ThrowsNPE() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("cache must not be null");
        ReflectionUtils.setClassAnnotationCache(null);
    }

    @Test
    public void setMethodAnnotationCache_NullMap_ThrowsNPE() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("cache must not be null");
        ReflectionUtils.setMethodAnnotationCache(null);
    }

    @Test
    public void setConstructorCache_NullMap_ThrowsNPE() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("cache must not be null");
        ReflectionUtils.setConstructorCache(null);
    }

    @Test
    public void setSortedConstructorsCache_NullMap_ThrowsNPE() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("cache must not be null");
        ReflectionUtils.setSortedConstructorsCache(null);
    }

    /* --------------------------------------------------------------------- */
    /*  2. getClassAnnotation – normal and edge cases                        */
    /* --------------------------------------------------------------------- */
    @Test
    public void getClassAnnotation_FindsAnnotationOnClass() {
        TestAnno anno = ReflectionUtils.getClassAnnotation(SubClass.class, TestAnno.class);
        Assert.assertNotNull("Annotation should be found on BaseClass", anno);
        Assert.assertEquals("base", anno.value());
    }

    @Test
    public void getClassAnnotation_ClassIsNull_ReturnsNull() {
        TestAnno anno = ReflectionUtils.getClassAnnotation(null, TestAnno.class);
        Assert.assertNull("Null class should yield null result", anno);
    }

    @Test
    public void getClassAnnotation_AnnoClassIsNull_ThrowsIAE() {
        thrown.expect(IllegalArgumentException.class);
        ReflectionUtils.getClassAnnotation(SubClass.class, null);
    }

    /* --------------------------------------------------------------------- */
    /*  3. getMethodAnnotation – normal and edge cases                        */
    /* --------------------------------------------------------------------- */
    @Test
    public void getMethodAnnotation_FindsAnnotationOnMethod() throws NoSuchMethodException {
        Method m = BaseClass.class.getMethod("annotatedMethod");
        TestAnno anno = ReflectionUtils.getMethodAnnotation(m, TestAnno.class);
        Assert.assertNotNull(anno);
        Assert.assertEquals("baseMethod", anno.value());
    }

    @Test
    public void getMethodAnnotation_InheritedFromInterface() throws NoSuchMethodException {
        Method m = TestInterface.class.getMethod("interfaceAnnotated");
        TestAnno anno = ReflectionUtils.getMethodAnnotation(m, TestAnno.class);
        Assert.assertNotNull(anno);
        Assert.assertEquals("interfaceMethod", anno.value());
    }

    @Test
    public void getMethodAnnotation_MethodIsNull_ThrowsIAE() {
        thrown.expect(IllegalArgumentException.class);
        ReflectionUtils.getMethodAnnotation(null, TestAnno.class);
    }

    @Test
    public void getMethodAnnotation_AnnoClassIsNull_ThrowsIAE() throws NoSuchMethodException {
        thrown.expect(IllegalArgumentException.class);
        Method m = BaseClass.class.getMethod("annotatedMethod");
        ReflectionUtils.getMethodAnnotation(m, null);
    }

    /* --------------------------------------------------------------------- */
    /*  4. getField – successful lookup, missing field, null arguments        */
    /* --------------------------------------------------------------------- */
    @Test
    public void getField_FindsInheritedPrivateField() {
        Field f = ReflectionUtils.getField(SubClass.class, "privateField");
        Assert.assertNotNull(f);
        Assert.assertEquals("privateField", f.getName());
    }

    @Test
    public void getField_FieldDoesNotExist_ReturnsNull() {
        Field f = ReflectionUtils.getField(SubClass.class, "nonExisting");
        Assert.assertNull(f);
    }

    @Test
    public void getField_NullClass_ThrowsIAE() {
        thrown.expect(IllegalArgumentException.class);
        ReflectionUtils.getField(null, "any");
    }

    @Test
    public void getField_NullFieldName_ThrowsIAE() {
        thrown.expect(IllegalArgumentException.class);
        ReflectionUtils.getField(SubClass.class, null);
    }

    /* --------------------------------------------------------------------- */
    /*  5. getDeclaredFields (custom filter)                                 */
    /* --------------------------------------------------------------------- */
    @Test
    public void getDeclaredFields_WithCustomFilter_ReturnsOnlyMatching() {
        Predicate<Field> onlyPublic = f -> Modifier.isPublic(f.getModifiers());
        List<Field> fields = ReflectionUtils.getDeclaredFields(SubClass.class, onlyPublic);
        Assert.assertEquals(1, fields.size());
        Assert.assertEquals("publicField", fields.get(0).getName());
    }

    @Test
    public void getDeclaredFields_NullClass_ThrowsIAE() {
        thrown.expect(IllegalArgumentException.class);
        ReflectionUtils.getDeclaredFields(null, f -> true);
    }

    @Test
    public void getDeclaredFields_NullFilter_ThrowsIAE() {
        thrown.expect(IllegalArgumentException.class);
        ReflectionUtils.getDeclaredFields(SubClass.class, null);
    }

    /* --------------------------------------------------------------------- */
    /*  6. getDeclaredFields (default filter) – enum special cases           */
    /* --------------------------------------------------------------------- */
    @Test
    public void getDeclaredFields_DefaultFilter_ExcludesEnumSyntheticFields() {
        List<Field> enumFields = ReflectionUtils.getDeclaredFields(SampleEnum.class);
        // The enum should expose only the user‑defined fields (none in this case)
        Assert.assertTrue(enumFields.isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /*  7. getAllDeclaredFields – hierarchy aggregation                       */
    /* --------------------------------------------------------------------- */
    @Test
    public void getAllDeclaredFields_IncludesSuperclassFields() {
        List<Field> all = ReflectionUtils.getAllDeclaredFields(SubClass.class);
        // Expect fields from SubClass and BaseClass (excluding static & synthetic)
        Set<String> names = new HashSet<>();
        for (Field f : all) names.add(f.getName());

        Assert.assertTrue(names.contains("publicField"));
        Assert.assertTrue(names.contains("privateField"));
        Assert.assertTrue(names.contains("protectedField"));
    }

    /* --------------------------------------------------------------------- */
    /*  8. getAllDeclaredFieldsMap – name collision handling                 */
    /* --------------------------------------------------------------------- */
    private static class ShadowFieldParent {
        private int value = 1;
    }
    private static class ShadowFieldChild extends ShadowFieldParent {
        private int value = 2;
    }

    @Test
    public void getAllDeclaredFieldsMap_HandlesShadowedFields() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(ShadowFieldChild.class);
        // Simple name should map to child's field
        Assert.assertTrue(map.containsKey("value"));
        Assert.assertEquals(ShadowFieldChild.class, map.get("value").getDeclaringClass());

        // Fully qualified name should map to parent's field
        String fq = ShadowFieldParent.class.getName() + ".value";
        Assert.assertTrue(map.containsKey(fq));
        Assert.assertEquals(ShadowFieldParent.class, map.get(fq).getDeclaringClass());
    }

    /* --------------------------------------------------------------------- */
    /*  9. call(Object, Method, ...) – success and failure scenarios        */
    /* --------------------------------------------------------------------- */
    @Test
    public void call_WithValidMethod_ReturnsResult() throws Exception {
        SubClass obj = new SubClass();
        Method m = SubClass.class.getMethod("toString");
        Object result = ReflectionUtils.call(obj, m);
        Assert.assertEquals(obj.toString(), result);
    }

    @Test
    public void call_MethodThrowsCheckedException_WrapsInRuntimeException() throws Exception {
        SubClass obj = new SubClass();
        Method m = SubClass.class.getMethod("throwsChecked");
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("Exception thrown inside reflectively called method");
        ReflectionUtils.call(obj, m);
    }

    @Test
    public void call_MethodThrowsRuntimeException_WrapsInRuntimeException() throws Exception {
        SubClass obj = new SubClass();
        Method m = SubClass.class.getMethod("throwsRuntime");
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("Exception thrown inside reflectively called method");
        ReflectionUtils.call(obj, m);
    }

    @Test
    public void call_NullMethod_ThrowsIAE() {
        thrown.expect(IllegalArgumentException.class);
        ReflectionUtils.call(new Object(), null);
    }

    @Test
    public void call_NullInstance_ThrowsIAE() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        Method m = Object.class.getMethod("toString");
        ReflectionUtils.call(null, m);
    }

    /* --------------------------------------------------------------------- */
    /* 10. call(Object, String, ...) – method lookup + invocation            */
    /* --------------------------------------------------------------------- */
    @Test
    public void call_ByName_InvokesMethod() {
        SubClass obj = new SubClass();
        Object result = ReflectionUtils.call(obj, "toString");
        Assert.assertEquals(obj.toString(), result);
    }

    @Test
    public void call_ByName_MethodNotFound_ThrowsIAE() {
        thrown.expect(IllegalArgumentException.class);
        ReflectionUtils.call(new SubClass(), "nonExistingMethod");
    }

    /* --------------------------------------------------------------------- */
    /* 11. getMethod(Class, String, Class...) – private/protected lookup      */
    /* --------------------------------------------------------------------- */
    @Test
    public void getMethod_FindsPrivateMethod() throws Exception {
        Method m = ReflectionUtils.getMethod(SubClass.class, "privateMethod");
        Assert.assertNotNull(m);
        Assert.assertTrue(Modifier.isPrivate(m.getModifiers()));
    }

    @Test
    public void getMethod_FindsMethodWithParameters() throws Exception {
        Method m = ReflectionUtils.getMethod(SubClass.class, "overloaded", String.class, int.class);
        Assert.assertNotNull(m);
        Assert.assertEquals(2, m.getParameterCount());
    }

    @Test
    public void getMethod_NullClass_ThrowsIAE() {
        thrown.expect(IllegalArgumentException.class);
        ReflectionUtils.getMethod(null, "any");
    }

    @Test
    public void getMethod_NullName_ThrowsIAE() {
        thrown.expect(IllegalArgumentException.class);
        ReflectionUtils.getMethod(SubClass.class, null);
    }

    /* --------------------------------------------------------------------- */
    /* 12. getMethod(Object, String, int) – overloaded selection logic        */
    /* --------------------------------------------------------------------- */
    @Test
    public void getMethod_ByArgCount_SelectsMostAccessible() {
        SubClass obj = new SubClass();
        Method m = ReflectionUtils.getMethod(obj, "overloaded", 2);
        Assert.assertEquals(2, m.getParameterCount());
        // The selected method should be the public one (not synthetic/bridge)
        Assert.assertTrue(Modifier.isPublic(m.getModifiers()));
    }

    @Test
    public void getMethod_ByArgCount_MethodNotFound_ThrowsIAE() {
        thrown.expect(IllegalArgumentException.class);
        ReflectionUtils.getMethod(new SubClass(), "overloaded", 5);
    }

    /* --------------------------------------------------------------------- */
    /* 13. getNonOverloadedMethod – success and overloaded‑method error      */
    /* --------------------------------------------------------------------- */
    @Test
    public void getNonOverloadedMethod_FindsUniqueMethod() throws Exception {
        Method m = ReflectionUtils.getNonOverloadedMethod(SubClass.class, "toString");
        Assert.assertNotNull(m);
        Assert.assertEquals("toString", m.getName());
    }

    @Test
    public void getNonOverloadedMethod_Overloaded_ThrowsIAE() {
        thrown.expect(IllegalArgumentException.class);
        ReflectionUtils.getNonOverloadedMethod(SubClass.class, "overloaded");
    }

    @Test
    public void getNonOverloadedMethod_NullClass_ThrowsIAE() {
        thrown.expect(IllegalArgumentException.class);
        ReflectionUtils.getNonOverloadedMethod(null, "any");
    }

    @Test
    public void getNonOverloadedMethod_EmptyName_ThrowsIAE() {
        thrown.expect(IllegalArgumentException.class);
        ReflectionUtils.getNonOverloadedMethod(SubClass.class, "");
    }

    /* --------------------------------------------------------------------- */
    /* 14. getConstructor – existing and missing constructors                 */
    /* --------------------------------------------------------------------- */
    @Test
    public void getConstructor_FindsPublicNoArgCtor() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(MultiCtor.class);
        Assert.assertNotNull(ctor);
        Assert.assertTrue(Modifier.isPublic(ctor.getModifiers()));
    }

    @Test
    public void getConstructor_FindsPrivateCtor() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(MultiCtor.class, double.class);
        Assert.assertNotNull(ctor);
        Assert.assertTrue(Modifier.isPrivate(ctor.getModifiers()));
    }

    @Test
    public void getConstructor_NoSuchCtor_ReturnsNull() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(MultiCtor.class, long.class);
        Assert.assertNull(ctor);
    }

    @Test
    public void getConstructor_NullClass_ThrowsIAE() {
        thrown.expect(IllegalArgumentException.class);
        ReflectionUtils.getConstructor(null);
    }

    /* --------------------------------------------------------------------- */
    /* 15. getAllConstructors – ordering verification                         */
    /* --------------------------------------------------------------------- */
    @Test
    public void getAllConstructors_ReturnsSortedByVisibilityAndParams() {
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(MultiCtor.class);
        Assert.assertEquals(4, ctors.length);
        // Expected order: public (0 params), protected (1 param), package‑private (2 params), private (1 param)
        Assert.assertTrue(Modifier.isPublic(ctors[0].getModifiers()));
        Assert.assertTrue(Modifier.isProtected(ctors[1].getModifiers()));
        Assert.assertFalse(Modifier.isPublic(ctors[2].getModifiers()) ||
                           Modifier.isProtected(ctors[2].getModifiers()) ||
                           Modifier.isPrivate(ctors[2].getModifiers())); // package‑private
        Assert.assertTrue(Modifier.isPrivate(ctors[3].getModifiers()));
    }

    @Test
    public void getAllConstructors_NullClass_ReturnsEmptyArray() {
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(null);
        Assert.assertEquals(0, ctors.length);
    }

    /* --------------------------------------------------------------------- */
    /* 16. getClassNameFromByteCode – malformed input handling               */
    /* --------------------------------------------------------------------- */
    @Test
    public void getClassNameFromByteCode_MalformedInput_ThrowsIllegalState() {
        thrown.expect(IllegalStateException.class);
        // Empty byte array will cause the switch‑case default path
        byte[] empty = new byte[0];
        try {
            ReflectionUtils.getClassNameFromByteCode(empty);
        } catch (IOException e) {
            // The method declares IOException, but malformed data triggers IllegalStateException
            throw new RuntimeException(e);
        }
    }

    /* --------------------------------------------------------------------- */
    /* 17. isJavaCompilerAvailable – system‑property override                 */
    /* --------------------------------------------------------------------- */
    @Test
    public void isJavaCompilerAvailable_ForceJre_ReturnsFalse() {
        System.setProperty("java.util.force.jre", "true");
        try {
            Assert.assertFalse(ReflectionUtils.isJavaCompilerAvailable());
        } finally {
            System.clearProperty("java.util.force.jre");
        }
    }

    /* --------------------------------------------------------------------- */
    /* 18. getClassName – null handling                                       */
    /* --------------------------------------------------------------------- */
    @Test
    public void getClassName_NullObject_ReturnsStringNull() {
        Assert.assertEquals("null", ReflectionUtils.getClassName(null));
    }

    @Test
    public void getClassName_NonNullObject_ReturnsClassName() {
        String s = "hello";
        Assert.assertEquals(s.getClass().getName(), ReflectionUtils.getClassName(s));
    }

    /* --------------------------------------------------------------------- */
    /* 19. Deprecated methods – they still delegate to the new ones          */
    /* --------------------------------------------------------------------- */
    @Test
    public void getDeepDeclaredFields_DelegatesToAllDeclaredFields() {
        Collection<Field> fields = ReflectionUtils.getDeepDeclaredFields(SubClass.class);
        // Should contain the same fields as getAllDeclaredFields (default filter)
        List<Field> all = ReflectionUtils.getAllDeclaredFields(SubClass.class);
        Assert.assertEquals(all.size(), fields.size());
        Assert.assertTrue(all.containsAll(fields));
    }

    @Test
    public void getDeepDeclaredFieldMap_DelegatesToAllDeclaredFieldsMap() {
        Map<String, Field> map = ReflectionUtils.getDeepDeclaredFieldMap(SubClass.class);
        Map<String, Field> expected = ReflectionUtils.getAllDeclaredFieldsMap(SubClass.class);
        Assert.assertEquals(expected, map);
    }

    @Test
    public void getDeclaredFields_DeprecatedVariantAddsToCollection() {
        Collection<Field> collection = new ArrayList<>();
        ReflectionUtils.getDeclaredFields(SubClass.class, collection);
        // collection should now contain the same fields as the non‑deprecated version
        List<Field> list = ReflectionUtils.getDeclaredFields(SubClass.class);
        Assert.assertEquals(list.size(), collection.size());
        Assert.assertTrue(list.containsAll(collection));
    }
}
