package com.cedarsoftware.util;

// import org.junit.Test;
// import static org.junit.Assert.*;

// @ExtendWith(MockitoExtension.class)
class DeepEquals_hasCustomEquals_Test {

    /**
     * Simple class that provides its own {@code equals} implementation.
     */
    private static class CustomEquals {

        private final int id;

        CustomEquals(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof CustomEquals))
                return false;
            return id == ((CustomEquals) o).id;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(id);
        }
    }

    /**
     * Class that does **not** override {@code equals}.
     */
    private static class NoEquals {

        private final String name;

        NoEquals(String name) {
            this.name = name;
        }
    }

    /**
     * Superclass that overrides {@code equals}.
     */
    private static class SuperCustom {

        private final String value;

        SuperCustom(String value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof SuperCustom))
                return false;
            return java.util.Objects.equals(value, ((SuperCustom) o).value);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hashCode(value);
        }
    }

    /**
     * Subclass that inherits the custom {@code equals} from {@link SuperCustom}.
     */
    private static class SubCustom extends SuperCustom {

        SubCustom(String value) {
            super(value);
        }
    }

    /**
     * Parent class that overrides {@code equals}. Used in JSON‑related test cases.
     */
    private static class ParentWithEquals {

        private final String data;

        ParentWithEquals(String data) {
            this.data = data;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof ParentWithEquals))
                return false;
            return java.util.Objects.equals(data, ((ParentWithEquals) o).data);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hashCode(data);
        }
    }

    /**
     * Child class that does **not** override {@code equals} – inherits from parent.
     */
    private static class ChildWithoutEquals extends ParentWithEquals {

        ChildWithoutEquals(String data) {
            super(data);
        }
    }

    /* -------------------------------------------------------
     * Helper method used by the bulk verification tests
     * ------------------------------------------------------- */
//     private static void verify(Class<?> clazz, boolean expected, String description) {
//         assertEquals(description, expected, DeepEquals.hasCustomEquals(clazz));
//     }

//     @Test
//     void hasCustomEquals_returnsTrueWhenClassOverridesEquals() {
//         assertTrue("CustomEquals overrides equals, so hasCustomEquals should be true", DeepEquals.hasCustomEquals(CustomEquals.class));
//     }

//     @Test
//     void hasCustomEquals_returnsFalseWhenClassDoesNotOverrideEquals() {
//         assertFalse("NoEquals does not override equals, so hasCustomEquals should be false", DeepEquals.hasCustomEquals(NoEquals.class));
//     }

//     @Test
//     void hasCustomEquals_returnsTrueWhenSuperclassOverridesEquals() {
//         assertTrue("SubCustom inherits a custom equals from SuperCustom, so hasCustomEquals should be true", DeepEquals.hasCustomEquals(SubCustom.class));
//     }

//     @Test
//     void hasCustomEquals_returnsFalseForObjectClass() {
//         assertFalse("Object's equals is the default implementation, so hasCustomEquals should be false", DeepEquals.hasCustomEquals(Object.class));
//     }

//     @Test
//     void hasCustomEquals_isConsistentAcrossMultipleCalls() {
//         // Re‑invoke the method to ensure any internal caching does not affect the result.
//         for (int i = 0; i < 3; i++) {
//             assertTrue(DeepEquals.hasCustomEquals(CustomEquals.class));
//             assertFalse(DeepEquals.hasCustomEquals(NoEquals.class));
//         }
//     }

    /* -------------------------------------------------------
     * Bulk verification (originally parameterised) – kept as
     * ordinary calls so they work with JUnit 4.
     * ------------------------------------------------------- */
//     @Test
//     public void bulkVerification() {
//         // Primitive / core Object cases
//         verify(Object.class, false, "Object does not override equals");
//         verify(String.class, true, "String overrides equals");
//         verify(java.util.List.class, false, "Interface List does not provide its own equals");
//         verify(int[].class, false, "Array class does not override equals");
//         verify(java.util.AbstractList.class, true, "AbstractList inherits equals from AbstractCollection");
//         // JDK concrete classes
//         verify(Integer.class, true, "Wrapper Integer overrides equals");
//         verify(java.time.LocalDate.class, true, "LocalDate overrides equals");
//         verify(java.util.ArrayList.class, true, "ArrayList overrides equals");
//         verify(java.time.DayOfWeek.class, true, "Enum DayOfWeek inherits Enum.equals (not Object)");
//         // Custom helper classes
//         verify(CustomEquals.class, true, "CustomEquals overrides equals");
//         verify(NoEquals.class, false, "NoEquals does not override equals");
//         verify(SubCustom.class, true, "SubCustom inherits custom equals from SuperCustom");
//         verify(SuperCustom.class, true, "SuperCustom overrides equals");
//         // Classes defined for JSON test cases
//         verify(ParentWithEquals.class, true, "ParentWithEquals overrides equals");
//         verify(ChildWithoutEquals.class, true, "ChildWithoutEquals inherits custom equals");
//     }

    /* -------------------------------------------------------
     * Exception handling tests
     * ------------------------------------------------------- */
//     @Test(expected = NullPointerException.class)
//     public void hasCustomEquals_nullClass_throwsNPE() {
//         DeepEquals.hasCustomEquals(null);
//     }

//     @Test(expected = IllegalArgumentException.class)
//     public void hasCustomEquals_primitiveInt_throwsIAE() {
//         DeepEquals.hasCustomEquals(int.class);
//     }

//     @Test(expected = IllegalArgumentException.class)
//     public void hasCustomEquals_voidClass_throwsIAE() {
//         DeepEquals.hasCustomEquals(void.class);
//     }
}