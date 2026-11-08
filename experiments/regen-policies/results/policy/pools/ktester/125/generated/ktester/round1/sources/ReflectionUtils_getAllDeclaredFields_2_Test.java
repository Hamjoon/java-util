package com.cedarsoftware.util;

import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ReflectionUtils#getAllDeclaredFields(Class)}.
 *
 * The tests cover:
 * <ul>
 *   <li>Simple classes with instance fields.</li>
 *   <li>Static fields are filtered out.</li>
 *   <li>Enum specific fields are filtered out.</li>
 *   <li>Fields declared in super‑classes are included.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getAllDeclaredFields_2_Test{

    /* --------------------------------------------------------------
     *  Lifecycle hooks – they are intentionally empty but kept for
     *  symmetry with the provided template.
     *  -------------------------------------------------------------- */
    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /* --------------------------------------------------------------
     *  Helper utilities
     *  -------------------------------------------------------------- */
    private static Set<String> fieldNames(List<Field> fields) {
        return fields.stream()
                     .map(Field::getName)
                     .collect(Collectors.toSet());
    }

    /* --------------------------------------------------------------
     *  Test cases
     *  -------------------------------------------------------------- */

    @Test
    void simpleClass_ShouldReturnAllInstanceFields() {
        class Simple {
            int intField;
            String strField;
        }

        List<Field> fields = ReflectionUtils.getAllDeclaredFields(Simple.class);
        Set<String> names = fieldNames(fields);

        assertEquals(2, names.size(), "Simple class should expose exactly two fields");
        assertTrue(names.contains("intField"));
        assertTrue(names.contains("strField"));
    }

    @Test
    void staticField_ShouldBeFilteredOut() {
        class WithStatic {
            static int staticField = 42;
            double instanceField;
        }

        List<Field> fields = ReflectionUtils.getAllDeclaredFields(WithStatic.class);
        Set<String> names = fieldNames(fields);

        assertEquals(1, names.size(), "Static fields must be filtered out");
        assertTrue(names.contains("instanceField"));
        assertFalse(names.contains("staticField"));
    }

    @Test
    void enumFields_ShouldBeFilteredAccordingToDefaultFilter() {
        enum SampleEnum {
            ONE, TWO;

            private int hidden;          // user‑declared instance field
        }

        List<Field> fields = ReflectionUtils.getAllDeclaredFields(SampleEnum.class);
        Set<String> names = fieldNames(fields);

        // The default filter removes static enum constants, ENUM$VALUES, ordinal, hash, etc.
        // Only the user‑declared instance field should remain.
        assertEquals(1, names.size(), "Only the user‑declared instance field should survive the filter");
        assertTrue(names.contains("hidden"));
    }

    @Test
    void inheritance_ShouldCollectFieldsFromSuperClasses() {
        class Super {
            long superField;
        }
        class Sub extends Super {
            boolean subField;
        }

        List<Field> fields = ReflectionUtils.getAllDeclaredFields(Sub.class);
        Set<String> names = fieldNames(fields);

        assertEquals(2, names.size(), "Fields from both subclass and superclass must be present");
        assertTrue(names.contains("superField"));
        assertTrue(names.contains("subField"));
    }

    @Test
    void interfaceFieldsAreStaticAndShouldBeIgnored() {
        interface Constants {
            int CONST = 5;   // implicitly public static final
        }
        class Implements implements Constants {
            String name;
        }

        List<Field> fields = ReflectionUtils.getAllDeclaredFields(Implements.class);
        Set<String> names = fieldNames(fields);

        // Only the instance field declared in the class should be returned.
        assertEquals(1, names.size());
        assertTrue(names.contains("name"));
        assertFalse(names.contains("CONST"));
    }
}