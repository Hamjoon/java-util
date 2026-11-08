package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getNonOverloadedMethod_Test {

    /* -------------------------------------------------------------
     *  Test fixtures
     * ------------------------------------------------------------- */
    static class Simple {

        @Override
        public String toString() {
            return "Simple";
        }
    }

    static class Overloaded {

        public void foo() {
        }

        public void foo(String s) {
        }
    }

    static class NoMethod {
    }

    // Inheritance scenario
    static class BaseClass {

        public void baseMethod() {
        }
    }

    static class SubClass extends BaseClass {
    }

    // Interface default method scenario
    interface InterfaceWithDefault {

        default void defaultMethod() {
        }
    }

    static class ImplClass implements InterfaceWithDefault {
    }

    // Static method scenario
    static class ClassWithStaticMethod {

        public static void staticMethod() {
        }
    }

    // Private overload – should NOT be considered an overload because getMethods() returns only public methods
    static class ClassWithPrivateOverload {

        public void foo() {
        }

        @SuppressWarnings("unused")
        private void foo(String ignored) {
        }
    }

    // Method name with surrounding whitespace – should be treated as a literal name and therefore not found
    static class ClassWithMethod {

        public void foo() {
        }
    }

    // Bridge method scenario (generic subclass)
    static class GenericBase<T> {

        public T get() {
            return null;
        }
    }

    static class GenericSub extends GenericBase<String> {

        @Override
        public String get() {
            return "";
        }
    }

    /* -------------------------------------------------------------
     *  Lifecycle
     * ------------------------------------------------------------- */
    @BeforeAll
    static void setupBeforeAll() {
        // Ensure a clean cache before the suite runs
        ReflectionUtils.setMethodCache(new ConcurrentHashMap<>());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Reset the cache for any other test suites that might run later
        ReflectionUtils.setMethodCache(new ConcurrentHashMap<>());
    }

    @Test
    void shouldReturnMethodWhenNotOverloaded() {
        Method m = ReflectionUtils.getNonOverloadedMethod(Simple.class, "toString");
        assertNotNull(m, "Method should be found");
        assertEquals("toString", m.getName());
        // second call should hit the cache – same Method instance
        Method cached = ReflectionUtils.getNonOverloadedMethod(Simple.class, "toString");
        assertSame(m, cached, "Method should be cached and therefore identical");
    }

    @Test
    void shouldThrowWhenClassIsNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getNonOverloadedMethod(null, "any"));
        assertTrue(ex.getMessage().contains("null class"));
    }

    @Test
    void shouldThrowWhenMethodNameIsNullOrBlank() {
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getNonOverloadedMethod(Simple.class, null));
        assertTrue(ex1.getMessage().contains("null or blank"));
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getNonOverloadedMethod(Simple.class, "   "));
        assertTrue(ex2.getMessage().contains("null or blank"));
    }

    @Test
    void shouldThrowWhenMethodIsOverloaded() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getNonOverloadedMethod(Overloaded.class, "foo"));
        assertTrue(ex.getMessage().contains("overloaded methods"));
    }

    @Test
    void shouldThrowWhenMethodDoesNotExist() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getNonOverloadedMethod(NoMethod.class, "missing"));
        assertTrue(ex.getMessage().contains("is not found"));
    }

    @Test
    void shouldFindInheritedPublicMethod() {
        Method m = ReflectionUtils.getNonOverloadedMethod(SubClass.class, "baseMethod");
        assertNotNull(m, "Inherited method should be found");
        assertEquals("baseMethod", m.getName());
        assertEquals(BaseClass.class, m.getDeclaringClass(), "Declaring class should be the superclass where the method is defined");
    }

    @Test
    void shouldFindInterfaceDefaultMethod() {
        Method m = ReflectionUtils.getNonOverloadedMethod(ImplClass.class, "defaultMethod");
        assertNotNull(m, "Default method from interface should be found");
        assertEquals("defaultMethod", m.getName());
        // The declaring class for a default method is the interface
        assertEquals(InterfaceWithDefault.class, m.getDeclaringClass(), "Declaring class should be the interface that defines the default method");
    }

    @Test
    void shouldFindStaticPublicMethod() {
        Method m = ReflectionUtils.getNonOverloadedMethod(ClassWithStaticMethod.class, "staticMethod");
        assertNotNull(m, "Static method should be found");
        assertEquals("staticMethod", m.getName());
        assertTrue(java.lang.reflect.Modifier.isStatic(m.getModifiers()), "Returned method should be static");
    }

    @Test
    void shouldIgnorePrivateOverloadAndReturnPublicMethod() {
        Method m = ReflectionUtils.getNonOverloadedMethod(ClassWithPrivateOverload.class, "foo");
        assertNotNull(m, "Public method should be found despite a private overload");
        assertEquals("foo", m.getName());
        assertTrue(java.lang.reflect.Modifier.isPublic(m.getModifiers()));
    }

    @Test
    void shouldThrowWhenMethodNameHasLeadingWhitespace() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getNonOverloadedMethod(ClassWithMethod.class, " foo"));
        assertTrue(ex.getMessage().contains("is not found"));
    }

    @Test
    void shouldThrowWhenPrimitiveClassHasNoMethod() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getNonOverloadedMethod(int.class, "intValue"));
        assertTrue(ex.getMessage().contains("is not found"));
    }

    @Test
    void shouldFindCloneMethodOnArrayClass() {
        Method m = ReflectionUtils.getNonOverloadedMethod(String[].class, "clone");
        assertNotNull(m, "Array class should expose public clone() method");
        assertEquals("clone", m.getName());
        // The declaring class for the clone method on arrays is Object
        assertEquals(Object.class, m.getDeclaringClass(), "clone method is declared on java.lang.Object");
    }

    @Test
    void shouldThrowWhenBridgeMethodCreatesOverloadAmbiguity() {
        // GenericSub has a bridge method for get() returning Object and a concrete method returning String.
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getNonOverloadedMethod(GenericSub.class, "get"));
        assertTrue(ex.getMessage().contains("overloaded methods"), "Bridge method should cause overload detection");
    }

    /* -------------------------------------------------------------
     *  Tests – cache reuse for a different class (ensures cache key includes class)
     * ------------------------------------------------------------- */
    @Test
    void cacheShouldBeKeyedByClassAndMethodName() {
        // First lookup for Simple.toString (cached)
        Method simpleMethod = ReflectionUtils.getNonOverloadedMethod(Simple.class, "toString");
        // Different class but same method name – should NOT return the same Method instance
        Method staticMethod = ReflectionUtils.getNonOverloadedMethod(ClassWithStaticMethod.class, "staticMethod");
        assertNotSame(simpleMethod, staticMethod, "Cache must differentiate entries by both class and method name");
    }
}
