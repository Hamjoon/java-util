import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getAllConstructors_Test {

    class Sample {

        Sample() {
        }

        public Sample(int a) {
        }

        protected Sample(String s, int i) {
        }
    }

    class CacheDemo {

        CacheDemo() {
        }

        CacheDemo(int x) {
        }
    }

    class Outer {

        class Inner {

            Inner() {
            }

            Inner(String s) {
            }
        }
    }

    class Inner {

        Inner() {
        }

        Inner(String s) {
        }
    }

    class Sample {

        Sample() {
        }

        public Sample(int a) {
        }

        protected Sample(String s, int i) {
        }
    }

    class CacheDemo {

        CacheDemo() {
        }

        CacheDemo(int x) {
        }
    }

    class Outer {

        class Inner {

            Inner() {
            }

            Inner(String s) {
            }
        }
    }

    class Inner {

        Inner() {
        }

        Inner(String s) {
        }
    }

    class Inner {

        Inner() {
        }

        Inner(String s) {
        }
    }

    /**
     * Simple class with a single public no‑arg constructor.
     */
    static class SimpleClass {

        public SimpleClass() {
        }
    }

    /**
     * Class with four constructors of different visibility and signatures.
     */
    static class MultiConstructorClass {

        public MultiConstructorClass() {
        }

        public MultiConstructorClass(int a) {
        }

        protected MultiConstructorClass(String s) {
        }

        private MultiConstructorClass(int a, String s) {
        }
    }

    /**
     * Abstract base class with a protected no‑arg constructor.
     */
    abstract static class AbstractBase {

        protected AbstractBase() {
        }
    }

    class Sample {

        Sample() {
        }

        public Sample(int a) {
        }

        protected Sample(String s, int i) {
        }
    }

    class CacheDemo {

        CacheDemo() {
        }

        CacheDemo(int x) {
        }
    }

    class Outer {

        class Inner {

            Inner() {
            }

            Inner(String s) {
            }
        }
    }

    class Inner {

        Inner() {
        }

        Inner(String s) {
        }
    }

    class Inner {

        Inner() {
        }

        Inner(String s) {
        }
    }

    /**
     * Reset the static constructor cache before each test so that tests are isolated
     * and we can safely test caching behaviour (reference equality) without interference
     * from previous executions.
     */
    @BeforeEach
    void setupBeforeEach() {
        // The public setter replaces the whole cache with a fresh, empty map.
        ReflectionUtils.setSortedConstructorsCache(new HashMap<>());
    }

    private static Stream<Arguments> edgeCaseProvider() {
        return Stream.of(Arguments.of(null, "null input"), Arguments.of(int.class, "primitive type"), Arguments.of(String[].class, "array type"), Arguments.of(List.class, "interface type"));
    }

    @Test
    void getAllConstructors_nullClass_returnsEmptyArray() {
        Constructor<?>[] result = ReflectionUtils.getAllConstructors(null);
        assertNotNull(result, "Result should never be null");
        assertEquals(0, result.length, "Null input must yield an empty array");
    }

    @Test
    void getAllConstructors_simpleClass_returnsAllConstructors() {
        // Local class with three constructors of varying visibility/parameters.
        class Sample {

            Sample() {
            }

            public Sample(int a) {
            }

            protected Sample(String s, int i) {
            }
        }
        Constructor<?>[] constructors = ReflectionUtils.getAllConstructors(Sample.class);
        assertNotNull(constructors);
        // Sample has exactly three declared constructors.
        assertEquals(3, constructors.length, "All declared constructors should be returned");
        boolean hasNoArg = false;
        boolean hasInt = false;
        boolean hasStringInt = false;
        for (Constructor<?> c : constructors) {
            Class<?>[] p = c.getParameterTypes();
            if (p.length == 0) {
                hasNoArg = true;
            } else if (p.length == 1 && p[0] == int.class) {
                hasInt = true;
            } else if (p.length == 2 && p[0] == String.class && p[1] == int.class) {
                hasStringInt = true;
            }
        }
        assertTrue(hasNoArg, "Expected no‑arg constructor");
        assertTrue(hasInt, "Expected constructor with a single int parameter");
        assertTrue(hasStringInt, "Expected constructor with (String, int) parameters");
    }

    @Test
    void getAllConstructors_caching_returnsSameArrayInstance() {
        class CacheDemo {

            CacheDemo() {
            }

            CacheDemo(int x) {
            }
        }
        Constructor<?>[] firstCall = ReflectionUtils.getAllConstructors(CacheDemo.class);
        // Second call – should hit the cache and return the *same* array instance.
        Constructor<?>[] secondCall = ReflectionUtils.getAllConstructors(CacheDemo.class);
        assertSame(firstCall, secondCall, "Subsequent calls for the same class must return the cached array instance");
    }

    @Test
    void getAllConstructors_nonStaticInnerClass_includesEnclosingInstanceParameter() {
        class Outer {

            class Inner {

                Inner() {
                }

                Inner(String s) {
                }
            }
        }
        Constructor<?>[] constructors = ReflectionUtils.getAllConstructors(Outer.Inner.class);
        assertNotNull(constructors);
        assertTrue(constructors.length >= 1, "Inner class should have at least one constructor");
        boolean foundSynthetic = false;
        for (Constructor<?> c : constructors) {
            Class<?>[] params = c.getParameterTypes();
            if (params.length > 0 && params[0] == Outer.class) {
                foundSynthetic = true;
                break;
            }
        }
        assertTrue(foundSynthetic, "At least one constructor of a non‑static inner class must have the enclosing class as its first parameter");
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("edgeCaseProvider")
    void getAllConstructors_edgeCases_returnsEmptyArray(Class<?> clazz, String description) {
        Constructor<?>[] result = ReflectionUtils.getAllConstructors(clazz);
        assertNotNull(result, description + " – result must not be null");
        assertEquals(0, result.length, description + " – expected empty constructor array");
    }

    @Test
    void getAllConstructors_simpleClass_returnsSingleConstructor() {
        Constructor<?>[] constructors = ReflectionUtils.getAllConstructors(ReflectionUtils_getAllConstructors_Test.SimpleClass.class);
        assertNotNull(constructors);
        assertEquals(1, constructors.length, "SimpleClass should expose exactly one constructor");
        assertEquals(ReflectionUtils_getAllConstructors_Test.SimpleClass.class, constructors[0].getDeclaringClass(), "Returned constructor must belong to SimpleClass");
        assertEquals(0, constructors[0].getParameterCount(), "SimpleClass constructor must be the no‑arg one");
    }

    @Test
    void getAllConstructors_multiConstructorClass_returnsAllFourConstructors() {
        Constructor<?>[] constructors = ReflectionUtils.getAllConstructors(ReflectionUtils_getAllConstructors_Test.MultiConstructorClass.class);
        assertNotNull(constructors);
        assertEquals(4, constructors.length, "MultiConstructorClass declares four constructors of varying visibility");
        boolean hasNoArg = false;
        boolean hasInt = false;
        boolean hasString = false;
        boolean hasIntString = false;
        for (Constructor<?> c : constructors) {
            Class<?>[] p = c.getParameterTypes();
            if (p.length == 0) {
                hasNoArg = true;
            } else if (p.length == 1 && p[0] == int.class) {
                hasInt = true;
            } else if (p.length == 1 && p[0] == String.class) {
                hasString = true;
            } else if (p.length == 2 && p[0] == int.class && p[1] == String.class) {
                hasIntString = true;
            }
        }
        assertTrue(hasNoArg, "Missing no‑arg constructor");
        assertTrue(hasInt, "Missing constructor with single int parameter");
        assertTrue(hasString, "Missing constructor with single String parameter");
        assertTrue(hasIntString, "Missing constructor with (int, String) parameters");
    }

    @Test
    void getAllConstructors_enum_returnsSyntheticConstructors() {
        Constructor<?>[] constructors = ReflectionUtils.getAllConstructors(SampleEnum.class);
        assertNotNull(constructors);
        // The JVM always provides exactly two synthetic constructors for enums.
        assertEquals(2, constructors.length, "Enum should expose the two synthetic (String, int) constructors");
        for (Constructor<?> c : constructors) {
            Class<?>[] p = c.getParameterTypes();
            assertEquals(2, p.length, "Each enum constructor must have two parameters");
            assertEquals(String.class, p[0], "First parameter must be String (name)");
            assertEquals(int.class, p[1], "Second parameter must be int (ordinal)");
        }
    }

    @Test
    void getAllConstructors_abstractClass_returnsProtectedConstructor() {
        Constructor<?>[] constructors = ReflectionUtils.getAllConstructors(ReflectionUtils_getAllConstructors_Test.AbstractBase.class);
        assertNotNull(constructors);
        assertEquals(1, constructors.length, "AbstractBase defines a single protected no‑arg constructor");
        Constructor<?> ctor = constructors[0];
        assertTrue(Modifier.isProtected(ctor.getModifiers()), "Returned constructor must be protected");
        assertEquals(0, ctor.getParameterCount(), "Constructor should have no parameters");
    }
}
