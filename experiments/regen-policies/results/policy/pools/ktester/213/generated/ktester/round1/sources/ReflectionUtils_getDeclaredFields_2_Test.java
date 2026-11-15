package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getDeclaredFields_2_Test {

    class WithStatic {

        int instanceField = 1;

        static int staticField = 2;
    }

    class Base {

        int baseField = 10;
    }

    class Sub extends Base {

        int subField = 20;
    }

    class AllStatic {

        static int a = 1;

        static String b = "b";
    }

    class WithStatic {

        int instanceField = 1;

        static int staticField = 2;
    }

    class Base {

        int baseField = 10;
    }

    class Sub extends Base {

        int subField = 20;
    }

    class AllStatic {

        static int a = 1;

        static String b = "b";
    }

    class WithStatic {

        int instanceField = 1;

        static int staticField = 2;
    }

    class Base {

        int baseField = 10;
    }

    class Sub extends Base {

        int subField = 20;
    }

    class AllStatic {

        static int a = 1;

        static String b = "b";
    }

    class WithStatic {

        int instanceField = 1;

        static int staticField = 2;
    }

    class Base {

        int baseField = 10;
    }

    class Sub extends Base {

        int subField = 20;
    }

    class AllStatic {

        static int a = 1;

        static String b = "b";
    }

    class WithStatic {

        int instanceField = 1;

        static int staticField = 2;
    }

    class Base {

        int baseField = 10;
    }

    class Sub extends Base {

        int subField = 20;
    }

    class AllStatic {

        static int a = 1;

        static String b = "b";
    }

    @Test
    void testGetDeclaredFieldsFiltersStatic() {
        class WithStatic {

            int instanceField = 1;

            static int staticField = 2;
        }
        List<Field> fields = ReflectionUtils.getDeclaredFields(WithStatic.class);
        List<String> names = fields.stream().map(Field::getName).collect(Collectors.toList());
        assertEquals("Only the instance field should be returned", 1, fields.size());
        assertTrue("Instance field must be present", names.contains("instanceField"));
        assertFalse("Static field must be filtered out", names.contains("staticField"));
    }

    @Test
    void testGetDeclaredFieldsEnumInternal() {
        ???;
        List<String> names = fields.stream().map(Field::getName).collect(Collectors.toList());
        assertTrue("Custom field must be present", names.contains("customField"));
        assertFalse("Synthetic field 'internal' should be filtered out", names.contains("internal"));
        assertFalse("Synthetic field 'ENUM$VALUES' should be filtered out", names.contains("ENUM$VALUES"));
    }

    @Test
    void testGetDeclaredFieldsDoesNotIncludeInherited() {
        class Base {

            int baseField = 10;
        }
        class Sub extends Base {

            int subField = 20;
        }
        List<Field> fields = ReflectionUtils.getDeclaredFields(Sub.class);
        List<String> names = fields.stream().map(Field::getName).collect(Collectors.toList());
        assertEquals("Only fields declared directly in Sub should be returned", 1, fields.size());
        assertTrue("Sub field must be present", names.contains("subField"));
        assertFalse("Base field must not be present", names.contains("baseField"));
    }

    @Test
    void testGetDeclaredFieldsAllStatic() {
        class AllStatic {

            static int a = 1;

            static String b = "b";
        }
        List<Field> fields = ReflectionUtils.getDeclaredFields(AllStatic.class);
        assertTrue("All static fields should be filtered out, resulting in an empty list", fields.isEmpty());
    }
}
