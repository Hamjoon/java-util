package com.cedarsoftware.util;

import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.DefaultConverterOptions;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_newInstance_Test {

    private Converter converter;

    /* -------------------------------------------------------------
     *  Helper test classes
     * ------------------------------------------------------------- */
    static class NoArgConstructor {

        NoArgConstructor() {
        }
    }

    static class SingleArgConstructor {

        private final String value;

        SingleArgConstructor(String value) {
            this.value = value;
        }

        String getValue() {
            return value;
        }
    }

    static class MultiArgConstructor {

        private final String str;

        private final int num;

        MultiArgConstructor(String str, int num) {
            this.str = str;
            this.num = num;
        }

        String getStr() {
            return str;
        }

        int getNum() {
            return num;
        }
    }

    static class OverloadedConstructors {

        private final String value;

        private final int number;

        OverloadedConstructors() {
            this("default", 0);
        }

        OverloadedConstructors(String value) {
            this(value, 0);
        }

        OverloadedConstructors(String value, int number) {
            this.value = value;
            this.number = number;
        }

        String getValue() {
            return value;
        }

        int getNumber() {
            return number;
        }
    }

    static class Outer {

        class Inner {

            String msg = "inner";

            Inner() {
            }
        }
    }

    class Inner {

        String msg = "inner";

        Inner() {
        }
    }

    interface TestInterface {
    }

    abstract static class AbstractTestClass {
    }

    class PrivateOnly {

        private PrivateOnly() {
        }
    }

    /**
     * Simple class with a public no‑arg constructor – used for cache‑related tests.
     */
    static class SimpleNoArg {

        SimpleNoArg() {
        }
    }

    /**
     * Constructor that accepts a nullable {@link Integer}.
     */
    static class ClassWithNullableIntegerConstructor {

        final Integer value;

        ClassWithNullableIntegerConstructor(Integer value) {
            this.value = value;
        }
    }

    /**
     * Person class used for argument‑matching tests.
     */
    static class Person {

        final String name;

        final int age;

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    /**
     * Class with two public constructors; the no‑arg one throws.
     */
    static class FailingFirstCtor {

        String createdVia;

        FailingFirstCtor() {
            throw new RuntimeException("boom");
        }

        FailingFirstCtor(String s) {
            this.createdVia = s;
        }
    }

    /**
     * Class with a private constructor that throws – forces unsafe fallback.
     */
    static class NoAccessibleConstructor {

        boolean constructed = false;

        private NoAccessibleConstructor() {
            throw new RuntimeException("should not be called");
        }
    }

    /**
     * Class whose only constructors are private and throw – used for the “cannot instantiate” path.
     */
    static class ImpossibleToInstantiate {

        private ImpossibleToInstantiate() {
            throw new RuntimeException("nope");
        }
    }

    /**
     * Class with a public no‑arg constructor and a public Integer constructor.
     */
    static class TwoCtorClass {

        Integer value;

        TwoCtorClass() {
            this.value = null;
        }

        TwoCtorClass(Integer v) {
            this.value = v;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // real converter – cheap and fully functional
        this.converter = new Converter(new DefaultConverterOptions());
        // Ensure the constructor cache is empty before each test to avoid cross‑test interference
        clearConstructorCache();
    }

    @AfterEach
    void teardownAfterEach() {
        // reset unsafe flag to its default (enabled) after each test that may have changed it
        ClassUtilities.setUseUnsafe(true);
    }

    /**
     * Utility that clears the private SUCCESSFUL_CONSTRUCTOR_CACHE via reflection.
     */
    private static void clearConstructorCache() {
        try {
            Field cacheField = ClassUtilities.class.getDeclaredField("SUCCESSFUL_CONSTRUCTOR_CACHE");
            cacheField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<Class<?>, Constructor<?>> cache = (Map<Class<?>, Constructor<?>>) cacheField.get(null);
            cache.clear();
        } catch (Exception e) {
            throw new RuntimeException("Unable to clear constructor cache", e);
        }
    }

    /* -------------------------------------------------------------
     *  Tests for successful instantiation
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("newInstance – no‑arg constructor")
    void testNewInstance_NoArgConstructor() {
        Object obj = ClassUtilities.newInstance(converter, NoArgConstructor.class, Collections.emptyList());
        assertInstanceOf(NoArgConstructor.class, obj);
    }

    @Test
    @DisplayName("newInstance – single argument constructor")
    void testNewInstance_SingleArgConstructor() {
        List<String> args = List.of("hello");
        Object obj = ClassUtilities.newInstance(converter, SingleArgConstructor.class, args);
        assertInstanceOf(SingleArgConstructor.class, obj);
        assertEquals("hello", ((SingleArgConstructor) obj).getValue());
    }

    @Test
    @DisplayName("newInstance – multiple argument constructor")
    void testNewInstance_MultiArgConstructor() {
        List<Object> args = List.of("text", 42);
        Object obj = ClassUtilities.newInstance(converter, MultiArgConstructor.class, args);
        assertInstanceOf(MultiArgConstructor.class, obj);
        MultiArgConstructor mac = (MultiArgConstructor) obj;
        assertEquals("text", mac.getStr());
        assertEquals(42, mac.getNum());
    }

    @Test
    @DisplayName("newInstance – overloaded constructors (no args)")
    void testNewInstance_Overloaded_NoArgs() {
        Object obj = ClassUtilities.newInstance(converter, OverloadedConstructors.class, Collections.emptyList());
        assertInstanceOf(OverloadedConstructors.class, obj);
        OverloadedConstructors oc = (OverloadedConstructors) obj;
        assertEquals("default", oc.getValue());
        assertEquals(0, oc.getNumber());
    }

    @Test
    @DisplayName("newInstance – overloaded constructors (single arg)")
    void testNewInstance_Overloaded_SingleArg() {
        List<String> args = List.of("custom");
        Object obj = ClassUtilities.newInstance(converter, OverloadedConstructors.class, args);
        assertInstanceOf(OverloadedConstructors.class, obj);
        OverloadedConstructors oc = (OverloadedConstructors) obj;
        assertEquals("custom", oc.getValue());
        assertEquals(0, oc.getNumber());
    }

    @Test
    @DisplayName("newInstance – overloaded constructors (two args)")
    void testNewInstance_Overloaded_TwoArgs() {
        List<Object> args = List.of("custom", 7);
        Object obj = ClassUtilities.newInstance(converter, OverloadedConstructors.class, args);
        assertInstanceOf(OverloadedConstructors.class, obj);
        OverloadedConstructors oc = (OverloadedConstructors) obj;
        assertEquals("custom", oc.getValue());
        assertEquals(7, oc.getNumber());
    }

    @Test
    @DisplayName("newInstance – inner non‑static class")
    void testNewInstance_NonStaticInnerClass() {
        // The inner class type token
        Class<?> innerClass = Outer.Inner.class;
        Object instance = ClassUtilities.newInstance(converter, innerClass, Collections.emptyList());
        assertInstanceOf(innerClass, instance);
        // Verify that the synthetic outer reference is non‑null
        try {
            java.lang.reflect.Field outerField = innerClass.getDeclaredField("this$0");
            outerField.setAccessible(true);
            Object outerInstance = outerField.get(instance);
            assertNotNull(outerInstance, "Enclosing instance should be injected");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Failed to inspect inner class instance", e);
        }
    }

    @Test
    @DisplayName("newInstance – null argument collection treated as empty")
    void testNewInstance_NullArgumentCollection() {
        Object obj = ClassUtilities.newInstance(converter, NoArgConstructor.class, null);
        assertInstanceOf(NoArgConstructor.class, obj);
    }

    /* -------------------------------------------------------------
     *  Tests for illegal usage / error handling
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("newInstance – cannot instantiate interface")
    void testNewInstance_InterfaceNotAllowed() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.newInstance(converter, TestInterface.class, Collections.emptyList()));
        assertTrue(ex.getMessage().contains("Cannot instantiate interface"));
    }

    @Test
    @DisplayName("newInstance – cannot instantiate abstract class")
    void testNewInstance_AbstractClassNotAllowed() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.newInstance(converter, AbstractTestClass.class, Collections.emptyList()));
        assertTrue(ex.getMessage().contains("Cannot instantiate abstract class"));
    }

    @Test
    @DisplayName("newInstance – class with only private constructor should fail")
    void testNewInstance_PrivateConstructorOnly() {
        class PrivateOnly {

            private PrivateOnly() {
            }
        }
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.newInstance(converter, PrivateOnly.class, Collections.emptyList()));
        // The exact message may vary; we only assert that an IllegalArgumentException is thrown
        assertNotNull(ex.getMessage());
    }

    @Test
    @DisplayName("Null class argument should trigger NullPointerException")
    void testNullClass() {
        NullPointerException npe = assertThrows(NullPointerException.class, () -> ClassUtilities.newInstance(converter, null, null));
        assertTrue(npe.getMessage().contains("Class cannot be null"));
    }

    @Test
    @DisplayName("Security‑blocked class (Runtime) must be rejected")
    void testSecurityBlockedClass() {
        SecurityException se = assertThrows(SecurityException.class, () -> ClassUtilities.newInstance(null, Runtime.class, null));
        assertTrue(se.getMessage().contains("json-io does not allow instantiation of"));
    }

    @Test
    @DisplayName("Cached constructor – first call caches, second call uses cached (non‑null path)")
    void testCachedConstructorSuccessNonNull() {
        // First call – caches the public no‑arg constructor
        Object first = ClassUtilities.newInstance(converter, SimpleNoArg.class, Collections.emptyList());
        assertInstanceOf(SimpleNoArg.class, first);
        // Second call – should hit the cache and succeed on the non‑null matching attempt
        Object second = ClassUtilities.newInstance(converter, SimpleNoArg.class, Collections.emptyList());
        assertInstanceOf(SimpleNoArg.class, second);
    }

    @Test
    @DisplayName("Cached constructor – allow‑null path when argument is null")
    void testCachedConstructorSuccessAllowNull() {
        // First call caches the Integer constructor
        Object first = ClassUtilities.newInstance(converter, ClassWithNullableIntegerConstructor.class, List.of((Integer) 5));
        assertInstanceOf(ClassWithNullableIntegerConstructor.class, first);
        assertEquals(5, ((ClassWithNullableIntegerConstructor) first).value);
        // Second call supplies a null – the cached constructor is reused, non‑null matching fails,
        // then the allow‑null branch succeeds.
        Object second = ClassUtilities.newInstance(converter, ClassWithNullableIntegerConstructor.class, List.of((Object) null));
        assertInstanceOf(ClassWithNullableIntegerConstructor.class, second);
        assertNull(((ClassWithNullableIntegerConstructor) second).value);
    }

    @Test
    @DisplayName("Constructor with arguments – Person(String, int)")
    void testConstructorWithArguments() {
        List<Object> args = List.of("John", 30);
        Object obj = ClassUtilities.newInstance(converter, Person.class, args);
        assertInstanceOf(Person.class, obj);
        Person p = (Person) obj;
        assertEquals("John", p.name);
        assertEquals(30, p.age);
    }

    @Test
    @DisplayName("Constructor fallback – allow‑null supplies default int")
    void testConstructorFallbackAllowNull() {
        // Only name provided; int should be supplied as default 0 via allow‑null handling
        List<Object> args = List.of("John");
        Object obj = ClassUtilities.newInstance(converter, Person.class, args);
        assertInstanceOf(Person.class, obj);
        Person p = (Person) obj;
        assertEquals("John", p.name);
        assertEquals(0, p.age);
    }

    @Test
    @DisplayName("Unsafe instantiation fallback when no accessible constructor")
    void testUnsafeInstantiationFallback() {
        // No constructor can be called successfully; unsafe allocation should succeed
        Object obj = ClassUtilities.newInstance(null, NoAccessibleConstructor.class, Collections.emptyList());
        assertInstanceOf(NoAccessibleConstructor.class, obj);
        // The field 'constructed' stays false because constructor body was never executed
        assertFalse(((NoAccessibleConstructor) obj).constructed);
    }

    @Test
    @DisplayName("Instantiation failure – all constructors throw and unsafe disabled")
    void testInstantiationFailure() {
        // Disable unsafe fallback to force the error‑aggregation path
        ClassUtilities.setUseUnsafe(false);
        IllegalArgumentException iae = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.newInstance(null, ImpossibleToInstantiate.class, Collections.emptyList()));
        String msg = iae.getMessage();
        assertTrue(msg.startsWith("Unable to instantiate"));
        assertTrue(msg.contains("All constructors either throw or are inaccessible"));
    }

    @Test
    @DisplayName("Primitive class (int) cannot be instantiated")
    void testPrimitiveClassInstantiation() {
        IllegalArgumentException iae = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.newInstance(null, int.class, null));
        assertTrue(iae.getMessage().startsWith("Unable to instantiate: int"));
    }

    @Test
    @DisplayName("Array class (String[]) cannot be instantiated")
    void testArrayClassInstantiation() {
        IllegalArgumentException iae = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.newInstance(null, String[].class, null));
        assertTrue(iae.getMessage().startsWith("Unable to instantiate: java.lang.String[]"));
    }

    @Test
    @DisplayName("Enum class cannot be instantiated")
    void testEnumClassInstantiation() {
        IllegalArgumentException iae = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.newInstance(null, SampleEnum.class, null));
        assertTrue(iae.getMessage().startsWith("Unable to instantiate: com.cedarsoftware.util.ClassUtilities_newInstance_Test$SampleEnum"));
    }

    @Test
    @DisplayName("Cached constructor argument mismatch – fallback to alternative ctor")
    void testCachedConstructorArgumentMismatch() {
        // First call caches the no‑arg constructor
        Object first = ClassUtilities.newInstance(converter, TwoCtorClass.class, Collections.emptyList());
        assertInstanceOf(TwoCtorClass.class, first);
        assertNull(((TwoCtorClass) first).value);
        // Second call provides an Integer argument; cached no‑arg ctor will fail,
        // method must fall back to the Integer constructor and succeed.
        Object second = ClassUtilities.newInstance(converter, TwoCtorClass.class, List.of(10));
        assertInstanceOf(TwoCtorClass.class, second);
        assertEquals(10, ((TwoCtorClass) second).value);
    }

    @Test
    @DisplayName("Constructor throws – fallback to second constructor")
    void testConstructorThrowsExceptionFallback() {
        Object obj = ClassUtilities.newInstance(converter, FailingFirstCtor.class, Collections.emptyList());
        assertInstanceOf(FailingFirstCtor.class, obj);
        FailingFirstCtor f = (FailingFirstCtor) obj;
        // The successful constructor was the one taking a String; we passed no args,
        // so the fallback used the String ctor with a default empty string (converter supplies "")
        assertEquals("", f.createdVia);
    }
}
