package com.cedarsoftware.util;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.stream.Stream;
// import org.junit.*;
// import org.junit.Test;
// import org.junit.runner.RunWith;
// import static org.junit.Assert.*;

// @ExtendWith(MockitoExtension.class)
class ReflectionUtils_getConstructor_Test {

    //     @RunWith(Parameterized.class)
    public static class ParameterizedConstructorLookup {

        //         @Parameterized.Parameters(name = "{0}")
        //         public static Collection<Object[]> data() {
        //             List<Object[]> list = new ArrayList<>();
        //             provideConstructorScenarios().forEach(holder -> list.add(new Object[]{holder}));
        //             return list;
        //         }
        private final ArgumentsHolder holder;

        public ParameterizedConstructorLookup(ArgumentsHolder holder) {
            this.holder = holder;
        }
        //         @Test
        //         public void parameterizedConstructorLookup() {
        //             Constructor<?> ctor = ReflectionUtils.getConstructor(holder.clazz, holder.parameterTypes);
        //             if (holder.shouldExist) {
        //                 assertNotNull("Expected constructor to be found for " + holder.clazz.getSimpleName(), ctor);
        //             } else {
        //                 assertNull("Expected no constructor for given signature on " + holder.clazz.getSimpleName(), ctor);
        //             }
        //         }
    }

    /* --------------------------------------------------------------------- */
    static class NoArgClass {

        NoArgClass() {
        }
    }

    static class OneIntConstructor {

        OneIntConstructor(int i) {
        }
    }

    static class TwoArgsConstructor {

        TwoArgsConstructor(String s, int i) {
        }
    }

    static class PrivateConstructor {

        private PrivateConstructor() {
        }
    }

    static class ComparatorConstructor {

        ComparatorConstructor(Comparator<?> comparator) {
        }
    }

    // --------------------------------------------------------------------- //
    static class ParamClass {

        ParamClass(int i, String s) {
        }
    }

    static class PrivateConstructorClass {

        private PrivateConstructorClass(int i) {
        }
    }

    static class PackagePrivateConstructorClass {

        PackagePrivateConstructorClass() {
        }
    }

    static class NumberConstructorClass {

        NumberConstructorClass(Number n) {
        }
    }

    static class VarArgsClass {

        VarArgsClass(String... args) {
        }
    }

    static abstract class AbstractClassWithCtor {

        protected AbstractClassWithCtor(String s) {
        }
    }

    static class ArrayParamClass {

        ArrayParamClass(int[][] matrix) {
        }
    }

    static class TwoParamClass {

        TwoParamClass(String s, int i) {
        }
        // No overload with (int, String)
    }

    /**
     * Simple holder for parameterized test arguments.
     */
    private static class ArgumentsHolder {

        final Class<?> clazz;

        final Class<?>[] parameterTypes;

        final boolean shouldExist;

        ArgumentsHolder(Class<?> clazz, Class<?>[] parameterTypes, boolean shouldExist) {
            this.clazz = clazz;
            this.parameterTypes = parameterTypes;
            this.shouldExist = shouldExist;
        }

        @Override
        public String toString() {
            return clazz.getSimpleName() + " " + Arrays.toString(parameterTypes);
        }
    }

    /* --------------------------------------------------------------------- */
    private static Stream<ArgumentsHolder> provideConstructorScenarios() {
        return Stream.of(new ArgumentsHolder(NoArgClass.class, new Class<?>[] {}, true), new ArgumentsHolder(OneIntConstructor.class, new Class<?>[] { int.class }, true), new ArgumentsHolder(ParamClass.class, new Class<?>[] { int.class, String.class }, true), new ArgumentsHolder(ParamClass.class, new Class<?>[] { Integer.class, String.class }, false), new ArgumentsHolder(NumberConstructorClass.class, new Class<?>[] { Integer.class }, false), new ArgumentsHolder(VarArgsClass.class, new Class<?>[] { String[].class }, true));
    }

//     @Test
//     void shouldThrowWhenClassArgumentIsNull() {
//         try {
//             ReflectionUtils.getConstructor(null);
//             fail("Expected IllegalArgumentException for null class argument");
//         } catch (IllegalArgumentException ignored) {
//             // expected
//         }
//     }
}