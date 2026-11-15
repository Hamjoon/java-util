package com.cedarsoftware.util;

import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import java.util.*;
import static org.junit.Assert.*;

@ExtendWith(MockitoExtension.class)
@RunWith(Parameterized.class)
class DeepEquals_hasCustomEquals_Test {

    private final Class<?> clazz;

    private final boolean expected;

    private final String description;

    /**
     * No custom {@code equals} implementation – inherits {@code Object.equals}.
     */
    static class NoEquals {

        int value;

        NoEquals(int value) {
            this.value = value;
        }
    }

    /**
     * Provides its own {@code equals} implementation.
     */
    static class WithEquals {

        int value;

        WithEquals(int value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof WithEquals))
                return false;
            return ((WithEquals) o).value == this.value;
        }
    }

    /**
     * Sub‑class that inherits {@code equals} from {@link WithEquals}.
     */
    static class SubClassInheritsEquals extends WithEquals {

        SubClassInheritsEquals(int value) {
            super(value);
        }
    }

    /**
     * Class with a custom {@code equals}.
     */
    static class WithCustomEquals {

        int value;

        WithCustomEquals(int v) {
            this.value = v;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof WithCustomEquals))
                return false;
            return ((WithCustomEquals) o).value == this.value;
        }
    }

    /**
     * Abstract base class without {@code equals}.
     */
    abstract static class BaseNoEquals {
    }

    /**
     * Interface – cannot declare {@code equals}.
     */
    interface InterfaceOnly {
    }

    /**
     * Sub‑class that overrides {@code equals} itself.
     */
    static class SubClassOverridesEquals extends NoEquals {

        SubClassOverridesEquals(int v) {
            super(v);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof SubClassOverridesEquals && ((SubClassOverridesEquals) o).value == this.value;
        }
    }

    /**
     * Grand‑parent provides custom {@code equals}; middle class does not.
     */
    static class GrandParentWithEquals {

        @Override
        public boolean equals(Object o) {
            return this == o;
        }
    }

    static class ParentNoEquals extends GrandParentWithEquals {
    }

    static class SubClassInheritsCustomEqualsFromGrandParent extends ParentNoEquals {
    }

    /**
     * Class used to trigger a {@link SecurityException} from {@link ReflectionUtils}.
     */
    static class NoAccessClass {
    }

    /* -------------------------------------------------
       Parameter source for the parameterized test
       ------------------------------------------------- */
    private static Stream<Arguments> booleanCases() {
        return Arrays.asList(new Object[][] { { Integer.class, true, "Integer overrides Object.equals(Object)" }, { WithCustomEquals.class, true, "User‑defined class overrides equals" }, { SubClassInheritsEquals.class, true, "Subclass inherits overridden equals" }, { Object.class, false, "Object itself has no custom equals" }, { java.util.HashMap.class, false, "HashMap inherits equals from AbstractMap (no custom)" }, { BaseNoEquals.class, false, "Abstract base class without equals" }, { InterfaceOnly.class, false, "Interface type" }, { Enum.class, false, "Enum class does not override equals" }, { SubClassOverridesEquals.class, true, "Subclass overrides equals itself" }, { SubClassInheritsCustomEqualsFromGrandParent.class, true, "Subclass inherits custom equals from grand‑parent" } });
    }

    @Test
    @DisplayName("Class that overrides equals should be reported as having a custom equals")
    void classWithCustomEqualsReturnsTrue() {
        assertTrue("Expected WithEquals to be recognised as having a custom equals implementation", DeepEquals.hasCustomEquals(WithEquals.class));
    }

    @Test
    @DisplayName("Class that does not override equals should be reported as not having a custom equals")
    void classWithoutCustomEqualsReturnsFalse() {
        assertFalse("Expected NoEquals to be recognised as NOT having a custom equals implementation", DeepEquals.hasCustomEquals(NoEquals.class));
    }

    @Test
    @DisplayName("Subclass inheriting a custom equals should be reported as having a custom equals")
    void subclassInheritingCustomEqualsReturnsTrue() {
        assertTrue("Expected SubClassInheritsEquals to be recognised as having a custom equals (inherited)", DeepEquals.hasCustomEquals(SubClassInheritsEquals.class));
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("booleanCases")
    @DisplayName("hasCustomEquals returns expected boolean for various class hierarchies")
    void testHasCustomEqualsBoolean(Class<?> clazz, boolean expected, String description) {
        assertEquals(description, expected, DeepEquals.hasCustomEquals(clazz));
    }

    @Test
    @DisplayName("Passing null should result in NullPointerException")
    void nullClassThrowsNPE() {
        try {
            DeepEquals.hasCustomEquals(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException ignored) {
        }
    }

    @Test
    @DisplayName("Primitive type class should cause IllegalArgumentException")
    void primitiveClassThrowsIllegalArgument() {
        try {
            DeepEquals.hasCustomEquals(int.class);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    @DisplayName("SecurityException from ReflectionUtils propagates")
    void securityExceptionIsPropagated() {
        try (MockedStatic<ReflectionUtils> mocked = Mockito.mockStatic(ReflectionUtils.class)) {
            mocked.when(() -> ReflectionUtils.getMethod(NoAccessClass.class, "equals", Object.class)).thenThrow(new SecurityException("Access denied"));
            SecurityException ex = assertThrows(SecurityException.class, () -> DeepEquals.hasCustomEquals(NoAccessClass.class));
            assertEquals("Access denied", ex.getMessage());
        }
    }
}
