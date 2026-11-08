package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Method;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getNonOverloadedMethod_Test {

    /**
     * Simple class with a single, non‑overloaded method.
     */
    static class Simple {

        public String greet() {
            return "hello";
        }
    }

    /**
     * Class that declares two methods with the same name – overload scenario.
     */
    static class Overloaded {

        public void work() {
        }

        public void work(int i) {
        }
    }

    /**
     * Class that does not contain the requested method.
     */
    static class Empty {
    }

    /**
     * Class with exactly one public no‑arg method.
     */
    static class SingleMethodClass {

        public void doWork() {
        }
    }

    /**
     * Class with two overloads – one public, one private.
     */
    static class PublicAndPrivateOverload {

        public void foo() {
        }

        @SuppressWarnings("unused")
        private void foo(int i) {
        }
    }

    /**
     * Base class providing a public method that will be inherited.
     */
    static class BaseClass {

        public void baseMethod() {
        }
    }

    /**
     * Subclass that inherits {@code baseMethod} from {@link BaseClass}.
     */
    static class SubClass extends BaseClass {
    }

    /**
     * Interface with a default method.
     */
    interface InterfaceWithDefault {

        default void defaultMethod() {
        }
    }

    /**
     * Class implementing the interface with a default method.
     */
    static class InterfaceImpl implements InterfaceWithDefault {
    }

    /**
     * Class containing a static public method.
     */
    static class StaticMethodClass {

        public static void staticAction() {
        }
    }

    /**
     * Class with overloaded methods where one overload is private (should not be seen).
     */
    static class OverloadedWithPrivate {

        public void foo() {
        }

        @SuppressWarnings("unused")
        private void foo(int i) {
        }
    }

    /**
     * First class for cache isolation test.
     */
    static class FirstClass {

        public void firstMethod() {
        }
    }

    /**
     * Second class for cache isolation test.
     */
    static class SecondClass {

        public void secondMethod() {
        }
    }

    /**
     * Superclass with a public method.
     */
    static class InheritedOverloadSuperclass {

        public void process() {
        }
    }

    /**
     * Subclass that overrides the method from the superclass – appears as two methods.
     */
    static class InheritedOverloadSubclass extends InheritedOverloadSuperclass {

        @Override
        public void process() {
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Reset the internal cache before each test to guarantee isolation.
        ReflectionUtils.setMethodCache(new HashMap<>());
    }

    static Stream<Arguments> emptyMethodNameProvider() {
        return Stream.of(arguments(""), arguments("   "));
    }

    @Test
    void shouldReturnMethodWhenExactlyOneMatches() {
        Method m = ReflectionUtils.getNonOverloadedMethod(SingleMethodClass.class, "doWork");
        assertNotNull(m, "Method should be found");
        assertEquals("doWork", m.getName());
        assertEquals(void.class, m.getReturnType());
    }

    @Test
    void shouldCacheMethodInstance() {
        Method first = ReflectionUtils.getNonOverloadedMethod(Simple.class, "greet");
        Method second = ReflectionUtils.getNonOverloadedMethod(Simple.class, "greet");
        // The cache should return the exact same Method object.
        assertSame(first, second, "Method instances should be cached and identical");
    }

    @Test
    void shouldThrowWhenClassIsNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getNonOverloadedMethod(null, "anyMethod"));
        assertTrue(ex.getMessage().contains("Attempted to call getMethod() [anyMethod]"), "Exception message should mention null class");
    }

    @Test
    void shouldThrowWhenMethodNameIsNullOrBlank() {
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getNonOverloadedMethod(Simple.class, null));
        assertTrue(ex1.getMessage().contains("null or blank method name"));
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getNonOverloadedMethod(Simple.class, "   "));
        assertTrue(ex2.getMessage().contains("null or blank method name"));
    }

    @Test
    void shouldThrowWhenMethodNotFound() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getNonOverloadedMethod(SingleMethodClass.class, "nonExistingMethod"));
        assertTrue(ex.getMessage().contains("is not found on class"), "Exception should indicate that the method cannot be found");
    }

    @Test
    void shouldThrowWhenMethodIsOverloaded() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getNonOverloadedMethod(Overloaded.class, "work"));
        assertTrue(ex.getMessage().contains("overloaded methods"), "Exception should mention overloaded methods");
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "{index}: methodName=''{0}''")
    @MethodSource("emptyMethodNameProvider")
    void shouldThrowWhenMethodNameIsEmptyOrBlank(String methodName) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getNonOverloadedMethod(Simple.class, methodName));
        assertTrue(ex.getMessage().contains("Attempted to call getMethod() with a null or blank method name"), "Exception message should mention blank method name");
    }

    // --------------------------------------------------------------------- //
    @Test
    void shouldThrowWhenMethodNameIsNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getNonOverloadedMethod(Simple.class, null));
        assertTrue(ex.getMessage().contains("Attempted to call getMethod() with a null or blank method name"));
    }

    // --------------------------------------------------------------------- //
    @Test
    void shouldThrowWhenMethodNameHasSpaces() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getNonOverloadedMethod(SingleMethodClass.class, " doWork "));
        assertTrue(ex.getMessage().contains("Method:  doWork () is not found on class"), "Exception should contain the exact method name with spaces");
    }

    // --------------------------------------------------------------------- //
    @Test
    void shouldFindInheritedPublicMethod() {
        Method m = ReflectionUtils.getNonOverloadedMethod(SubClass.class, "baseMethod");
        assertNotNull(m, "Inherited method should be found");
        assertEquals("baseMethod", m.getName());
        assertEquals(BaseClass.class, m.getDeclaringClass(), "Declaring class should be the superclass where the method is defined");
    }

    // --------------------------------------------------------------------- //
    @Test
    void shouldFindInterfaceDefaultMethod() {
        Method m = ReflectionUtils.getNonOverloadedMethod(InterfaceImpl.class, "defaultMethod");
        assertNotNull(m, "Default method from interface should be found");
        assertEquals("defaultMethod", m.getName());
        // The declaring class for default methods is the interface itself.
        assertEquals(InterfaceWithDefault.class, m.getDeclaringClass(), "Declaring class should be the interface that defines the default method");
    }

    // --------------------------------------------------------------------- //
    @Test
    void shouldFindStaticMethod() {
        Method m = ReflectionUtils.getNonOverloadedMethod(StaticMethodClass.class, "staticAction");
        assertNotNull(m, "Static method should be found");
        assertTrue(Modifier.isStatic(m.getModifiers()), "Method should be static");
    }

    // --------------------------------------------------------------------- //
    @Test
    void shouldReturnPublicMethodWhenPrivateOverloadExists() {
        Method m = ReflectionUtils.getNonOverloadedMethod(PublicAndPrivateOverload.class, "foo");
        assertNotNull(m, "Public method should be returned despite a private overload");
        assertEquals("foo", m.getName());
        assertEquals(PublicAndPrivateOverload.class, m.getDeclaringClass());
    }

    // --------------------------------------------------------------------- //
    @Test
    void shouldThrowWhenClassIsPrimitive() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getNonOverloadedMethod(int.class, "intValue"));
        assertTrue(ex.getMessage().contains("is not found on class"), "Primitive types have no public methods; expect not‑found exception");
    }

    // --------------------------------------------------------------------- //
    @Test
    void shouldCreateSeparateCacheEntriesForDifferentClasses() {
        Method first = ReflectionUtils.getNonOverloadedMethod(FirstClass.class, "firstMethod");
        Method second = ReflectionUtils.getNonOverloadedMethod(SecondClass.class, "secondMethod");
        assertNotNull(first);
        assertNotNull(second);
        assertNotSame(first, second, "Cache entries for different classes must be distinct");
        assertEquals("firstMethod", first.getName());
        assertEquals("secondMethod", second.getName());
        // Re‑invoke the first lookup to ensure a cache hit.
        Method firstAgain = ReflectionUtils.getNonOverloadedMethod(FirstClass.class, "firstMethod");
        assertSame(first, firstAgain, "Repeated lookup should hit the cache");
    }

    // --------------------------------------------------------------------- //
    @Test
    void shouldThrowWhenOverloadedMethodIsInheritedAndOverridden() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getNonOverloadedMethod(InheritedOverloadSubclass.class, "process"));
        assertTrue(ex.getMessage().contains("overloaded methods"), "Overridden method appears twice (superclass & subclass) and should trigger overload exception");
    }
}
