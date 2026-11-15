import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.DefaultConverterOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_newInstance_Test {

    private Converter converter;

    class PrivateConstructor {

        private PrivateConstructor(String s) {
        }
    }

//     class PrivateConstructor {

        private PrivateConstructor(String s) {
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

        public OverloadedConstructors() {
            this("default", 0);
        }

        public OverloadedConstructors(String value) {
            this(value, 0);
        }

        public OverloadedConstructors(String value, int number) {
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

    // Used to verify unsafe‑instantiation fallback
    static class PrivateNoArg {

        private PrivateNoArg() {
        }
    }

    // Simple POJO with a no‑arg constructor
    static class NoArgConstructor {

        NoArgConstructor() {
        }
    }

    // Non‑static inner class – used to verify automatic creation of the enclosing instance
    static class Outer {

        class Inner {

            Inner() {
            }
        }

        static class StaticInner {

            StaticInner() {
            }
        }
    }

    class Inner {

        Inner() {
        }
    }

    static class StaticInner {

        StaticInner() {
        }
    }

//     class Inner {

        Inner() {
        }
    }

//     static class StaticInner {

        StaticInner() {
        }
    }

    // Class with a private constructor – used for unsafe‑instantiation test
    static class PrivateCtor {

        private PrivateCtor(String s) {
        }
    }

    // Class that always throws in its no‑arg ctor
    static class FailingCtor {

        FailingCtor() {
            throw new RuntimeException("boom");
        }
    }

    /**
     * Class with a failing no‑arg constructor and a var‑args constructor that succeeds.
     * This allows {@code ClassUtilities.newInstance(...)} to fall back from the
     * throwing ctor to the var‑args ctor when no arguments are supplied.
     */
    static class ExceptionThrowingCtor {

        ExceptionThrowingCtor() {
            throw new RuntimeException("first ctor fails");
        }

        // var‑args fallback – can be invoked with zero arguments
        ExceptionThrowingCtor(int... ignored) {
        }
    }

    // Class used for cached‑fallback test (String‑arg then no‑arg)
    static class CachedFallbackPojo {

        private final String value;

        CachedFallbackPojo() {
            this.value = "default";
        }

        CachedFallbackPojo(String value) {
            this.value = value;
        }

        String getValue() {
            return value;
        }
    }

    // Class where only part of the arguments are supplied – second param should become null
    static class PartialArgsPojo {

        private final String text;

        private final Integer number;

        PartialArgsPojo(String text, Integer number) {
            this.text = text;
            this.number = number;
        }

        String getText() {
            return text;
        }

        Integer getNumber() {
            return number;
        }
    }

    // Varargs constructor example
    static class VarargsCtor {

        private final List<String> values;

        VarargsCtor(String... values) {
            this.values = Arrays.asList(values);
        }

        List<String> getValues() {
            return values;
        }
    }

    // Multi‑argument POJO used for conversion tests
    static class MultiArgPojo {

        private final int i;

        private final boolean b;

        private final LocalDate d;

        MultiArgPojo(int i, boolean b, LocalDate d) {
            this.i = i;
            this.b = b;
            this.d = d;
        }

        int getI() {
            return i;
        }

        boolean isB() {
            return b;
        }

        LocalDate getD() {
            return d;
        }
    }

    // POJO with a long constructor – used to test Converter conversion from String
    static class ConverterPojo {

        private final long v;

        ConverterPojo(long v) {
            this.v = v;
        }

        long getV() {
            return v;
        }
    }

    // Class with two constructors: one String‑arg (cached) and a no‑arg fallback
    static class MultiCtor {

        private final String value;

        MultiCtor() {
            this.value = "no‑arg";
        }

        MultiCtor(String value) {
            this.value = value;
        }

        String getValue() {
            return value;
        }
    }

//     class PrivateConstructor {

        private PrivateConstructor(String s) {
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use the real Converter – it can handle the simple type conversions required.
        converter = new Converter(new DefaultConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Clear the internal constructor cache so that each test starts fresh.
//         Field cacheField = ClassUtilities.class.getDeclaredField("SUCCESSFUL_CONSTRUCTOR_CACHE");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Class<?>, ?> cache = ((Map<Class<?>, ?>) (cacheField.get(null)));
        cache.clear();
        // Reset unsafe flag to default (false) after each test that may have changed it.
//         ClassUtilities.setUseUnsafe(false);
    }

//     @Test
//     @DisplayName("newInstance creates an object using a no‑arg constructor")
//     void testNoArgConstructor() {
//         Object o = ClassUtilities.newInstance(converter, ClassUtilities_newInstance_Test.NoArgConstructor.class, null);
//         assertInstanceOf(ClassUtilities_newInstance_Test.NoArgConstructor.class, o);
//     }

//     @Test
//     @DisplayName("newInstance creates an object using a single‑arg constructor")
//     void testSingleArgConstructor() {
//         List<Object> args = List.of("hello");
//         Object o = ClassUtilities.newInstance(converter, ClassUtilities_newInstance_Test.SingleArgConstructor.class, args);
//         assertInstanceOf(ClassUtilities_newInstance_Test.SingleArgConstructor.class, o);
//         assertEquals("hello", ((SingleArgConstructor) (o)).getValue());
//     }

//     @Test
//     @DisplayName("newInstance creates an object using a multi‑arg constructor")
//     void testMultiArgConstructor() {
//         List<Object> args = List.of("text", 42);
//         Object o = ClassUtilities.newInstance(converter, ClassUtilities_newInstance_Test.MultiArgConstructor.class, args);
//         assertInstanceOf(ClassUtilities_newInstance_Test.MultiArgConstructor.class, o);
//         MultiArgConstructor mc = ((MultiArgConstructor) (o));
//         assertEquals("text", mc.getStr());
//         assertEquals(42, mc.getNum());
//     }

//     @Test
//     @DisplayName("newInstance selects the appropriate overloaded constructor")
//     void testOverloadedConstructors() {
//         // 0‑arg – should end up with defaults ("default",0)
//         Object o0 = ClassUtilities.newInstance(converter, ClassUtilities_newInstance_Test.OverloadedConstructors.class, Collections.emptyList());
//         OverloadedConstructors oc0 = ((OverloadedConstructors) (o0));
//         assertEquals("default", oc0.getValue());
//         assertEquals(0, oc0.getNumber());
//         // 1‑arg – value supplied, number defaults to 0
//         Object o1 = ClassUtilities.newInstance(converter, ClassUtilities_newInstance_Test.OverloadedConstructors.class, List.of("provided"));
//         OverloadedConstructors oc1 = ((OverloadedConstructors) (o1));
//         assertEquals("provided", oc1.getValue());
//         assertEquals(0, oc1.getNumber());
//         // 2‑arg – both supplied
//         Object o2 = ClassUtilities.newInstance(converter, ClassUtilities_newInstance_Test.OverloadedConstructors.class, List.of("x", 7));
//         OverloadedConstructors oc2 = ((OverloadedConstructors) (o2));
//         assertEquals("x", oc2.getValue());
//         assertEquals(7, oc2.getNumber());
//     }

//     @Test
//     @DisplayName("newInstance can instantiate a non‑static inner class")
//     void testInnerClassInstantiation() {
//         // Outer$Inner is a non‑static inner class; the utility should create the
//         // enclosing Outer instance automatically.
//         Class<?> innerClass = ClassUtilities_newInstance_Test.Outer.Inner.class;
//         Object inner = ClassUtilities.newInstance(converter, innerClass, Collections.emptyList());
//         assertInstanceOf(innerClass, inner);
//     }

//     @Test
//     @DisplayName("newInstance rejects interface types")
//     void testInterfaceRejection() {
//         IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.newInstance(converter, List.class, Collections.emptyList()));
//         assertTrue(ex.getMessage().contains("Cannot instantiate interface"));
//     }

//     @Test
//     @DisplayName("newInstance rejects abstract classes")
//     void testAbstractClassRejection() {
//         IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.newInstance(converter, AbstractList.class, Collections.emptyList()));
//         assertTrue(ex.getMessage().contains("Cannot instantiate abstract class"));
//     }

//     @Test
//     @DisplayName("newInstance rejects security‑blocked classes")
//     void testSecurityBlockedClass() {
//         // ProcessBuilder is on the hard‑coded block list
//         SecurityException se = assertThrows(SecurityException.class, () -> ClassUtilities.newInstance(converter, ProcessBuilder.class, Collections.emptyList()));
//         assertTrue(se.getMessage().contains("json-io does not allow instantiation"));
//     }

//     @Test
//     @DisplayName("newInstance throws when no suitable constructor exists")
//     void testNoSuitableConstructor() {
//         // PrivateConstructor has a private constructor that cannot be accessed without
//         // the utility making it accessible; however, we deliberately pass an argument
//         // that does not match any constructor to force failure.
//         class PrivateConstructor {
// 
//             private PrivateConstructor(String s) {
//             }
//         }
//         IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.newInstance(converter, PrivateConstructor.class, List.of()));
//         assertTrue(ex.getMessage().contains("Unable to instantiate"));
//     }

//     @Test
//     @DisplayName("newInstance caches a successful constructor for subsequent calls")
//     void testConstructorCacheReuse() {
//         // First call – cache will be populated
//         Object first = ClassUtilities.newInstance(converter, ClassUtilities_newInstance_Test.SingleArgConstructor.class, List.of("cached"));
//         assertInstanceOf(ClassUtilities_newInstance_Test.SingleArgConstructor.class, first);
//         assertEquals("cached", ((SingleArgConstructor) (first)).getValue());
//         // Second call – should reuse the cached constructor
//         Object second = ClassUtilities.newInstance(converter, ClassUtilities_newInstance_Test.SingleArgConstructor.class, List.of("second"));
//         assertInstanceOf(ClassUtilities_newInstance_Test.SingleArgConstructor.class, second);
//         assertEquals("second", ((SingleArgConstructor) (second)).getValue());
//     }

//     @Test
//     @DisplayName("newInstance can instantiate a static inner class")
//     void testStaticInnerClassInstantiation() {
//         Class<?> staticInner = ClassUtilities_newInstance_Test.Outer.StaticInner.class;
//         Object inner = ClassUtilities.newInstance(converter, staticInner, Collections.emptyList());
//         assertInstanceOf(staticInner, inner);
//     }

//     @Test
//     @DisplayName("newInstance correctly normalises a null argumentValues collection")
//     void testArgumentValuesNullHandling() {
//         Object o = ClassUtilities.newInstance(converter, ClassUtilities_newInstance_Test.NoArgConstructor.class, null);
//         assertInstanceOf(ClassUtilities_newInstance_Test.NoArgConstructor.class, o);
//     }

//     @Test
//     @DisplayName("newInstance converts String arguments to required types (int, boolean, LocalDate)")
//     void testConstructorWithMultipleArgumentsMatchingCollection() {
//         List<Object> args = List.of("42", "true", "2023-01-01");
//         Object o = ClassUtilities.newInstance(converter, ClassUtilities_newInstance_Test.MultiArgPojo.class, args);
//         assertInstanceOf(ClassUtilities_newInstance_Test.MultiArgPojo.class, o);
//         MultiArgPojo pojo = ((MultiArgPojo) (o));
//         assertEquals(42, pojo.getI());
//         assertTrue(pojo.isB());
//         assertEquals(LocalDate.of(2023, 1, 1), pojo.getD());
//     }

//     @Test
//     @DisplayName("newInstance uses Converter to convert a String to long")
//     void testArgumentConversionViaConverter() {
//         List<Object> args = List.of("12345");
//         Object o = ClassUtilities.newInstance(converter, ClassUtilities_newInstance_Test.ConverterPojo.class, args);
//         assertInstanceOf(ClassUtilities_newInstance_Test.ConverterPojo.class, o);
//         assertEquals(12345L, ((ConverterPojo) (o)).getV());
//     }

//     @Test
//     @DisplayName("newInstance handles varargs constructor")
//     void testVarargsConstructorHandling() {
//         List<Object> args = List.of("a", "b", "c");
//         Object o = ClassUtilities.newInstance(converter, ClassUtilities_newInstance_Test.VarargsCtor.class, args);
//         assertInstanceOf(ClassUtilities_newInstance_Test.VarargsCtor.class, o);
//         VarargsCtor vc = ((VarargsCtor) (o));
//         assertEquals(List.of("a", "b", "c"), vc.getValues());
//     }

//     @Test
//     @DisplayName("newInstance falls back to unsafe instantiation for a private constructor")
//     void testUnsafeInstantiationFallbackForPrivateCtor() {
//         // Enable unsafe mode for this test
//         ClassUtilities.setUseUnsafe(true);
//         Object o = ClassUtilities.newInstance(converter, ClassUtilities_newInstance_Test.PrivateNoArg.class, Collections.emptyList());
//         assertInstanceOf(ClassUtilities_newInstance_Test.PrivateNoArg.class, o);
//     }

//     @Test
//     @DisplayName("newInstance rejects a null Class argument")
//     void testNullClassValidation() {
//         IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.newInstance(converter, null, null));
//         assertTrue(ex.getMessage().contains("Class cannot be null"));
//     }

//     @Test
//     @DisplayName("newInstance rejects security‑blocked classes (Runtime)")
//     void testSecurityBlockedClassCheck() {
//         SecurityException se = assertThrows(SecurityException.class, () -> ClassUtilities.newInstance(converter, Runtime.class, Collections.emptyList()));
//         assertTrue(se.getMessage().contains("json-io does not allow instantiation"));
//     }

//     @Test
//     @DisplayName("newInstance rejects classes blocked by name (ProcessImpl)")
//     void testSecurityBlockedClassNameCheck() {
//         // ProcessImpl is listed in SECURITY_BLOCKED_CLASS_NAMES
//         Class<?> procImpl;
//         try {
//             procImpl = Class.forName("java.lang.ProcessImpl");
//         } catch (ClassNotFoundException e) {
//             // If the JDK does not expose this class, the test is not applicable.
//             return;
//         }
//         SecurityException se = assertThrows(SecurityException.class, () -> ClassUtilities.newInstance(converter, procImpl, Collections.emptyList()));
//         assertTrue(se.getMessage().contains("json-io does not allow instantiation"));
//     }

//     @Test
//     @DisplayName("newInstance throws when all constructors fail")
//     void testAllConstructorsFail() {
//         IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.newInstance(converter, FailingCtor.class, Collections.emptyList()));
//         assertTrue(ex.getMessage().contains("Unable to instantiate"));
//         assertTrue(ex.getMessage().contains("All constructor errors"));
//     }

//     @Test
//     @DisplayName("newInstance falls back from cached constructor (non‑null then null args)")
//     void testCachedConstructorFallbackNonNullThenNull() {
//         // First call – matches String‑arg constructor and caches it
//         Object first = ClassUtilities.newInstance(converter, ClassUtilities_newInstance_Test.MultiCtor.class, List.of("cached"));
//         assertInstanceOf(ClassUtilities_newInstance_Test.MultiCtor.class, first);
//         assertEquals("cached", ((MultiCtor) (first)).getValue());
//         // Second call – supplies no arguments; cached String‑arg ctor fails,
//         // method should fall back to the no‑arg constructor.
//         Object second = ClassUtilities.newInstance(converter, ClassUtilities_newInstance_Test.MultiCtor.class, Collections.emptyList());
//         assertInstanceOf(ClassUtilities_newInstance_Test.MultiCtor.class, second);
//         assertEquals("no‑arg", ((MultiCtor) (second)).getValue());
//     }

//     @Test
//     @DisplayName("newInstance uses cached constructor and replaces it when it fails")
//     void testCachedConstructorFailureFallbackToOther() {
//         // First call caches the String‑arg constructor
//         Object first = ClassUtilities.newInstance(converter, ClassUtilities_newInstance_Test.CachedFallbackPojo.class, List.of("initial"));
//         assertInstanceOf(ClassUtilities_newInstance_Test.CachedFallbackPojo.class, first);
//         assertEquals("initial", ((CachedFallbackPojo) (first)).getValue());
//         // Second call provides no arguments – cached String‑arg ctor fails,
//         // fallback to no‑arg ctor should succeed and update the cache.
//         Object second = ClassUtilities.newInstance(converter, ClassUtilities_newInstance_Test.CachedFallbackPojo.class, Collections.emptyList());
//         assertInstanceOf(ClassUtilities_newInstance_Test.CachedFallbackPojo.class, second);
//         assertEquals("default", ((CachedFallbackPojo) (second)).getValue());
//     }

//     @Test
//     @DisplayName("newInstance allows nulls when arguments are insufficient")
//     void testAllowNullsFallbackWhenArgumentsInsufficient() {
//         // Only one argument supplied, but constructor needs two.
//         Object o = ClassUtilities.newInstance(converter, ClassUtilities_newInstance_Test.PartialArgsPojo.class, List.of("onlyOne"));
//         assertInstanceOf(ClassUtilities_newInstance_Test.PartialArgsPojo.class, o);
//         PartialArgsPojo pojo = ((PartialArgsPojo) (o));
//         assertEquals("onlyOne", pojo.getText());
//         // second parameter should be null
//         assertNull(pojo.getNumber());
//     }

//     @Test
//     @DisplayName("newInstance proceeds to next constructor when first throws")
//     void testConstructorThrowsExceptionProceedToNext() {
//         // The first (no‑arg) constructor throws; the utility should try the next viable one.
//         Object o = ClassUtilities.newInstance(converter, ClassUtilities_newInstance_Test.ExceptionThrowingCtor.class, Collections.emptyList());
//         assertInstanceOf(ClassUtilities_newInstance_Test.ExceptionThrowingCtor.class, o);
//     }
}