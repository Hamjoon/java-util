import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.Permission;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getField_Test {

    /**
     * Simple class with various fields used in the tests.
     */
    static class SampleBase {

        private int privateInt;

        protected String protectedString;

        public static final double STATIC_DOUBLE = 3.14;
    }

    /**
     * Subclass of {@link SampleBase} to test inheritance handling.
     */
    static class SampleSub extends SampleBase {

        public long publicLong;
    }

    /**
     * Base class for inheritance tests.
     */
    static class Base {

        private int baseField;

        static String staticField = "base";
    }

    /**
     * Subclass that inherits from {@link Base}.
     */
    static class Derived extends Base {

        public long derivedField;

        static int staticDerived = 42;
    }

    /**
     * Class with fields of every Java visibility level.
     */
    static class VisibilityDemo {

        public int publicField;

        protected int protectedField;

        /* package‑private */
        int packageField;

        private int privateField;
    }

    /**
     * Superclass that declares a field which will be hidden in the subclass.
     */
    static class ShadowSuper {

        int shadowed = 1;
    }

    /**
     * Subclass that hides {@code shadowed} from {@link ShadowSuper}.
     */
    static class ShadowSub extends ShadowSuper {

        // hides the superclass field
        int shadowed = 2;
    }

    /**
     * Interface that defines a constant (implicitly public static final).
     */
    interface InterfaceWithConstant {

        int CONST_VALUE = 99;
    }

    /**
     * Outer class containing a non‑static inner class – used to locate the synthetic this$0 field.
     */
    static class Outer {

        // No explicit fields; the compiler adds a synthetic field "this$0".
        class Inner {
        }
    }

    // No explicit fields; the compiler adds a synthetic field "this$0".
    class Inner {
    }

    // No explicit fields; the compiler adds a synthetic field "this$0".
    class Inner {
    }

    /**
     * Class used for the security‑manager test.
     */
    static class SecureClass {

        public int secureField;
    }

    @BeforeEach
    void clearCache() throws Exception {
        // ReflectionUtils.FIELD_NAME_CACHE is a private static final AtomicReference.
        Field cacheField = ReflectionUtils.class.getDeclaredField("FIELD_NAME_CACHE");
        cacheField.setAccessible(true);
        // Replace with a fresh, empty ConcurrentHashMap.
        cacheField.set(null, new AtomicReference<>(new ConcurrentHashMap<>()));
    }

    static Stream<Arguments> nullCheckProvider() {
        return Stream.of(Arguments.of(null, "any", "class cannot be null"), Arguments.of(String.class, null, "fieldName cannot be null"));
    }

    static Stream<Arguments> fieldRetrievalProvider() {
        return Stream.of(Arguments.of(Base.class, "baseField", "Retrieve a field declared directly in the supplied class.", Base.class, int.class, false), Arguments.of(Derived.class, "baseField", "Retrieve a field that is declared in a superclass; getAllDeclaredFields walks the hierarchy.", Base.class, int.class, false), Arguments.of(Derived.class, "staticDerived", "Even though the field is static, getField does not filter it out; it should be returned when the name matches.", Derived.class, int.class, true), Arguments.of(Derived.class, "nonExisting", "When no field with the given name exists, the method should return null.", null, null, false));
    }

    static Stream<Arguments> visibilityProvider() {
        return Stream.of(Arguments.of("publicField", VisibilityDemo.class, int.class, Modifier.PUBLIC), Arguments.of("protectedField", VisibilityDemo.class, int.class, Modifier.PROTECTED), Arguments.of("packageField", VisibilityDemo.class, int.class, 0), Arguments.of("privateField", VisibilityDemo.class, int.class, Modifier.PRIVATE));
    }

    static Stream<Arguments> enumFieldProvider() {
        return Stream.of(Arguments.of("ordinal", SampleEnum.class, int.class), Arguments.of("$VALUES", SampleEnum.class, SampleEnum[].class));
    }

    @Test
    void getField_returnsDeclaredField() {
        Field f = ReflectionUtils.getField(SampleBase.class, "privateInt");
        assertNotNull(f, "Field should be found");
        assertEquals("privateInt", f.getName());
        assertEquals(int.class, f.getType());
    }

    @Test
    void getField_findsFieldInSuperclass() {
        Field f = ReflectionUtils.getField(SampleSub.class, "protectedString");
        assertNotNull(f, "Inherited field should be found");
        assertEquals("protectedString", f.getName());
        assertEquals(String.class, f.getType());
    }

    @Test
    void getField_returnsNullWhenFieldDoesNotExist() {
        Field f = ReflectionUtils.getField(SampleBase.class, "nonExistent");
        assertNull(f, "Non‑existent field should yield null");
    }

    @Test
    void getField_throwsWhenClassIsNull() {
        assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getField(null, "anyName"), "Null class argument should cause IllegalArgumentException");
    }

    @Test
    void getField_throwsWhenFieldNameIsNull() {
        assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getField(SampleBase.class, null), "Null fieldName argument should cause IllegalArgumentException");
    }

    @Test
    void getField_cachesResult() {
        // First call – field is computed and cached.
        Field first = ReflectionUtils.getField(SampleBase.class, "privateInt");
        // Second call – should retrieve the same Field instance from the cache.
        Field second = ReflectionUtils.getField(SampleBase.class, "privateInt");
        assertSame(first, second, "Subsequent calls should return the cached Field instance");
    }

    @ParameterizedTest(name = "[{index}] class={0}, fieldName={1}")
    @MethodSource("nullCheckProvider")
    void getField_nullChecks(Class<?> clazz, String fieldName, String expectedMessage) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getField(clazz, fieldName));
        assertTrue(ex.getMessage().contains(expectedMessage));
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("fieldRetrievalProvider")
    void getField_fieldRetrieval(Class<?> clazz, String fieldName, String description, Class<?> expectedDeclaringClass, Class<?> expectedType, boolean expectStatic) {
        Field f = ReflectionUtils.getField(clazz, fieldName);
        assertNotNull(f, description);
        assertEquals(fieldName, f.getName(), "Field name must match");
        assertEquals(expectedDeclaringClass, f.getDeclaringClass(), "Declaring class must be as expected");
        assertEquals(expectedType, f.getType(), "Field type must be as expected");
        assertEquals(expectStatic, Modifier.isStatic(f.getModifiers()), "Static flag must match expectation");
    }

    @Test
    void getField_fieldRetrieval_nonExisting_returnsNull() {
        Field f = ReflectionUtils.getField(Derived.class, "nonExisting");
        assertNull(f, "Non‑existent field should yield null");
    }

    @ParameterizedTest(name = "[{index}] visibility={0}")
    @MethodSource("visibilityProvider")
    void getField_visibilityVariants(String fieldName, Class<?> declaringClass, Class<?> expectedType, int expectedModifiers) {
        Field f = ReflectionUtils.getField(VisibilityDemo.class, fieldName);
        assertNotNull(f, "Field '" + fieldName + "' should be found");
        assertEquals(declaringClass, f.getDeclaringClass());
        assertEquals(expectedType, f.getType());
        // mask out only the visibility bits for comparison
        int visibilityMask = Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE;
        assertEquals(expectedModifiers, f.getModifiers() & visibilityMask);
    }

    @Test
    void getField_shadowedField_subclassWins() {
        Field f = ReflectionUtils.getField(ShadowSub.class, "shadowed");
        assertNotNull(f);
        assertEquals(ShadowSub.class, f.getDeclaringClass(), "Subclass's field should be returned, not the superclass's");
    }

    @Test
    void getField_shadowedField_superclass() {
        Field f = ReflectionUtils.getField(ShadowSuper.class, "shadowed");
        assertNotNull(f);
        assertEquals(ShadowSuper.class, f.getDeclaringClass(), "Superclass's field should be returned when queried against the superclass");
    }

    @ParameterizedTest(name = "[{index}] enumField={0}")
    @MethodSource("enumFieldProvider")
    void getField_enumSyntheticFields(String fieldName, Class<?> expectedDeclaringClass, Class<?> expectedType) {
        Field f = ReflectionUtils.getField(SampleEnum.class, fieldName);
        assertNotNull(f, "Synthetic enum field '" + fieldName + "' should be found");
        assertEquals(expectedDeclaringClass, f.getDeclaringClass());
        assertEquals(expectedType, f.getType());
    }

    @Test
    void getField_enumSynthetic_nonExisting_returnsNull() {
        assertNull(ReflectionUtils.getField(SampleEnum.class, "nonExistingEnumField"));
    }

    @Test
    void getField_interfaceConstant() {
        Field f = ReflectionUtils.getField(InterfaceWithConstant.class, "CONST_VALUE");
        assertNotNull(f);
        assertEquals(InterfaceWithConstant.class, f.getDeclaringClass());
        assertEquals(int.class, f.getType());
        assertTrue(Modifier.isPublic(f.getModifiers()));
        assertTrue(Modifier.isStatic(f.getModifiers()));
        assertTrue(Modifier.isFinal(f.getModifiers()));
    }

    // last one wrong case
    @ParameterizedTest(name = "[{index}] invalidName=''{0}''")
    @ValueSource(strings = { "", "   ", "basefield" })
    void getField_invalidNames(String fieldName) {
        Field f = ReflectionUtils.getField(Derived.class, fieldName);
        assertNull(f, "Invalid field name should result in null");
    }

    @Test
    void getField_primitiveType_returnsNull() {
        assertNull(ReflectionUtils.getField(int.class, "ANY"));
    }

    @Test
    void getField_arrayType_returnsNull() {
        assertNull(ReflectionUtils.getField(String[].class, "length"));
    }

    @Test
    void getField_voidClass_staticField() {
        Field f = ReflectionUtils.getField(Void.class, "TYPE");
        assertNotNull(f);
        assertEquals(Class.class, f.getType());
        assertTrue(Modifier.isStatic(f.getModifiers()));
    }

    @Test
    void getField_syntheticInnerClassField() {
        Class<?> innerClass = Outer.Inner.class;
        Field f = ReflectionUtils.getField(innerClass, "this$0");
        assertNotNull(f, "Synthetic field 'this$0' should be found");
        assertEquals(Outer.class, f.getType());
    }

    @Test
    void getField_securityManagerDeniesAccess() {
        SecurityManager original = System.getSecurityManager();
        // Install a SecurityManager that blocks reflective member access for SecureClass
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkMemberAccess(Class<?> clazz, int which) {
                if (clazz == SecureClass.class) {
                    throw new SecurityException("Access denied for SecureClass");
                }
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else
            }
        });
        try {
            SecurityException ex = assertThrows(SecurityException.class, () -> ReflectionUtils.getField(SecureClass.class, "secureField"));
            assertTrue(ex.getMessage().contains("Access denied"));
        } finally {
            // Restore original security manager to avoid side‑effects on other tests
            System.setSecurityManager(original);
        }
    }
}
