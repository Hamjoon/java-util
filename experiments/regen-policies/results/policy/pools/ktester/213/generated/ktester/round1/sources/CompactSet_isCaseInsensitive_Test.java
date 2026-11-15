package com.cedarsoftware.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Arrays;
// import org.junit.Test;
// import org.junit.runner.RunWith;
// import org.junit.runners.Parameterized;
// import org.junit.Assert;

// @ExtendWith(MockitoExtension.class)
class CompactSet_isCaseInsensitive_Test {

    /**
     * Subclass that overrides the method to make the set case‑insensitive.
     */
    private static class CaseInsensitiveSet<E> extends CompactSet<E> {

        @Override
        protected boolean isCaseInsensitive() {
            return true;
        }

        /**
         * Expose the overridden method for the test.
         */
        boolean isCaseInsensitivePublic() {
            return super.isCaseInsensitive();
        }
    }

    /**
     * Simple subclass that does not change the default behaviour.
     */
    private static class DefaultCaseSet<E> extends CompactSet<E> {

        /**
         * Expose the protected method for the test.
         */
        boolean isCaseInsensitivePublic() {
            return super.isCaseInsensitive();
        }
    }

    /**
     * Subclass whose case‑sensitivity is driven by a constructor flag.
     * Allows us to test both true and false outcomes with a single class.
     */
    private static class ConfigurableCaseSet<E> extends CompactSet<E> {

        private final boolean caseInsensitive;

        ConfigurableCaseSet(boolean caseInsensitive) {
            this.caseInsensitive = caseInsensitive;
        }

        @Override
        protected boolean isCaseInsensitive() {
            return caseInsensitive;
        }

        /**
         * Expose the method for the test.
         */
        boolean isCaseInsensitivePublic() {
            return super.isCaseInsensitive();
        }
    }

    /**
     * Subclass that deliberately throws a RuntimeException from the
     * overridden {@code isCaseInsensitive()} method.
     */
    private static class ExceptionThrowingSet<E> extends CompactSet<E> {

        @Override
        protected boolean isCaseInsensitive() {
            throw new IllegalStateException("forced");
        }

        /**
         * Expose the method for the test.
         */
        boolean isCaseInsensitivePublic() {
            return super.isCaseInsensitive();
        }
    }

    /**
     * Parameterised test for the configurable subclass (true / false).
     */
//     @RunWith(Parameterized.class)
    public static class ConfigurableCaseSetTest {

//         @Parameterized.Parameter(0)
        public boolean flag;

//         @Parameterized.Parameter(1)
        public boolean expected;

//         @Parameterized.Parameters(name = "ConfigurableCaseSet(flag={0}) → expected={1}")
//         public static Collection<Object[]> data() {
//             return Arrays.asList(new Object[][] { { true, true }, { false, false } });
//         }

//         @Test
//         public void configurableSubclassRespectsConstructorFlag() {
//             ConfigurableCaseSet<String> set = new ConfigurableCaseSet<>(flag);
//             Assert.assertEquals(expected, set.isCaseInsensitivePublic(), "ConfigurableCaseSet should return the flag value supplied to its constructor.");
//         }
    }

//     @Test
//     void defaultImplementationIsCaseSensitive() {
//         DefaultCaseSet<String> set = new DefaultCaseSet<>();
//         // The default implementation returns false → case‑sensitive.
//         Assert.assertFalse(set.isCaseInsensitivePublic(), "CompactSet should be case‑sensitive by default (isCaseInsensitive() == false).");
//     }

//     @Test
//     void overriddenImplementationIsCaseInsensitive() {
//         CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
//         // Overridden method returns true → case‑insensitive.
//         Assert.assertTrue(set.isCaseInsensitivePublic(), "Overridden CompactSet should be case‑insensitive (isCaseInsensitive() == true).");
//     }

//     @Test
//     void caseInsensitiveAffectsUnderlyingMapConfiguration() {
//         // When case‑insensitive, the underlying CompactMap should be created with
//         // caseSensitive = false. We verify this indirectly via behaviour of the set.
//         CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
//         set.add("Apple");
//         // duplicate when case‑insensitive
//         set.add("apple");
//         Assert.assertEquals(1, set.size(), "Case‑insensitive set should treat \"Apple\" and \"apple\" as the same element.");
//         Assert.assertTrue(set.contains("APPLE"), "Case‑insensitive set should contain \"APPLE\" after adding \"Apple\".");
//     }

    /**
     * Verify that a subclass without an override inherits the default (false).
     */
//     @Test
//     void subclassWithoutOverrideReturnsFalse() {
//         DefaultCaseSet<Integer> set = new DefaultCaseSet<>();
//         Assert.assertFalse(set.isCaseInsensitivePublic(), "Subclass without override must return false (case‑sensitive).");
//     }

    /**
     * Parameterized test for the configurable subclass (true / false).
     */
//     @ParameterizedTest(name = "ConfigurableCaseSet(flag={0}) → expected={1}")
//     @CsvSource({ "true, true", "false, false" })
//     void configurableSubclassRespectsConstructorFlag(boolean flag, boolean expected) {
//         ConfigurableCaseSet<String> set = new ConfigurableCaseSet<>(flag);
//         assertEquals(expected, set.isCaseInsensitivePublic(), "ConfigurableCaseSet should return the flag value supplied to its constructor.");
//     }

    /**
     * Direct reflection call with setAccessible(true) – should return false.
     */
//     @Test
//     void reflectionInvokesProtectedMethodSuccessfully() throws Exception {
//         CompactSet<String> set = new CompactSet<>() {
//         };
//         Method m = CompactSet.class.getDeclaredMethod("isCaseInsensitive");
//         m.setAccessible(true);
//         Object result = m.invoke(set);
//         Assert.assertTrue(result instanceof Boolean);
//         Assert.assertFalse((Boolean) result, "Reflection with setAccessible(true) must return false (default case‑sensitive).");
//     }

    /**
     * Attempt to invoke without setAccessible – expect IllegalAccessException.
     */
//     @Test
//     void reflectionWithoutAccessibleThrowsIllegalAccessException() throws Exception {
//         CompactSet<String> set = new CompactSet<>() {
//         };
//         Method m = CompactSet.class.getDeclaredMethod("isCaseInsensitive");
//         // deliberately NOT calling m.setAccessible(true)
//         Assert.assertThrows(IllegalAccessException.class, () -> m.invoke(set));
//     }

    /**
     * Supplying a non‑null argument array to a zero‑arg method – expect IllegalArgumentException.
     */
//     @Test
//     void reflectionWithExtraArgumentsThrowsIllegalArgumentException() throws Exception {
//         CompactSet<String> set = new CompactSet<>() {
//         };
//         Method m = CompactSet.class.getDeclaredMethod("isCaseInsensitive");
//         m.setAccessible(true);
//         Assert.assertThrows(IllegalArgumentException.class, () -> m.invoke(set, new Object[] { "unused" }));
//     }

    /**
     * Invoking the method with a null target – expect NullPointerException.
     */
//     @Test
//     void reflectionWithNullTargetThrowsNullPointerException() throws Exception {
//         Method m = CompactSet.class.getDeclaredMethod("isCaseInsensitive");
//         m.setAccessible(true);
//         Assert.assertThrows(NullPointerException.class, () -> m.invoke(null));
//     }

    /**
     * The overridden method that throws a RuntimeException must propagate it.
     */
//     @Test
//     void overriddenMethodThrowsRuntimeException() {
//         ExceptionThrowingSet<String> set = new ExceptionThrowingSet<>();
//         IllegalStateException ex = Assert.assertThrows(IllegalStateException.class, set::isCaseInsensitivePublic);
//         Assert.assertEquals("forced", ex.getMessage(), "Exception message should match the one thrown in the override.");
//     }
}