package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactLinkedMap;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.DayOfWeek;
import java.time.Month;
import java.util.concurrent.TimeUnit;

@ExtendWith(MockitoExtension.class)
class EnumConversions_toMap_Test {

    /**
     * The Converter instance required by the method signature. It is not used by the implementation,
     *  therefore a mock is sufficient.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Outer class containing a static inner enum – used to verify handling of inner enum constants.
     */
    private static class OuterClass {

        enum InnerEnum {

            INNER_VALUE
        }
    }

    @Test
    void toMap_returnsMapContainingEnumName() {
        // GIVEN
        SampleEnum enumInstance = SampleEnum.SECOND;
        // WHEN
        Map<String, Object> result = EnumConversions.toMap(enumInstance, mockConverter);
        // THEN
        assertNotNull(result, "Resulting map should not be null");
        // The implementation uses CompactLinkedMap, but we only need to verify behaviour.
        assertTrue(result instanceof CompactLinkedMap, "Result should be a CompactLinkedMap");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertEquals("SECOND", result.get("name"), "Map should contain the enum name under key \"name\"");
    }

    @Test
    void toMap_withNonEnum_throwsClassCastException() {
        // GIVEN
        Object notAnEnum = "I am not an enum";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> EnumConversions.toMap(notAnEnum, mockConverter), "Providing a non‑Enum object should result in ClassCastException");
    }

    @Test
    void toMap_returnsMapContainingEnumName_forDayOfWeekMonday() {
        // GIVEN
        DayOfWeek enumInstance = DayOfWeek.MONDAY;
        // WHEN
        Map<String, Object> result = EnumConversions.toMap(enumInstance, mockConverter);
        // THEN
        assertNotNull(result, "Resulting map should not be null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertEquals("MONDAY", result.get("name"), "Map should contain the enum name under key \"name\"");
    }

    @Test
    void toMap_returnsMapContainingEnumName_forMonthDecember() {
        // GIVEN
        Month enumInstance = Month.DECEMBER;
        // WHEN
        Map<String, Object> result = EnumConversions.toMap(enumInstance, mockConverter);
        // THEN
        assertNotNull(result);
        assertEquals("DECEMBER", result.get("name"));
    }

    @Test
    void toMap_nullFrom_throwsNullPointerException() {
        // GIVEN
        Object nullEnum = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> EnumConversions.toMap(nullEnum, mockConverter), "Passing null as the enum instance should throw NullPointerException");
    }

    @Test
    void toMap_customEnumWithFields_ignoresExtraFields() {
        // GIVEN
        TestEnumWithFields enumInstance = TestEnumWithFields.VALUE_A;
        // WHEN
        Map<String, Object> result = EnumConversions.toMap(enumInstance, mockConverter);
        // THEN
        assertEquals(1, result.size(), "Map should contain only the 'name' entry");
        assertEquals("VALUE_A", result.get("name"));
    }

    @Test
    void toMap_withNonNullConverter_doesNotThrow() {
        // GIVEN
        TimeUnit enumInstance = TimeUnit.SECONDS;
        // A simple concrete Converter implementation (the method is never called by toMap)
        Converter realConverter = new Converter() {

            @Override
            public Object convert(Object o, Class<?> c) {
                return null;
            }
        };
        // WHEN
        Map<String, Object> result = EnumConversions.toMap(enumInstance, realConverter);
        // THEN
        assertEquals("SECONDS", result.get("name"));
        assertEquals(1, result.size());
    }

    @Test
    void toMap_innerEnumConstant_isConvertedCorrectly() {
        // GIVEN
        OuterClass.InnerEnum enumInstance = OuterClass.InnerEnum.INNER_VALUE;
        // WHEN
        Map<String, Object> result = EnumConversions.toMap(enumInstance, mockConverter);
        // THEN
        assertEquals("INNER_VALUE", result.get("name"));
    }

    @Test
    void toMap_returnedMapIsMutable() {
        // GIVEN
        DayOfWeek enumInstance = DayOfWeek.FRIDAY;
        // WHEN
        Map<String, Object> result = EnumConversions.toMap(enumInstance, mockConverter);
        // mutate the map
        result.put("extra", "added");
        // THEN
        assertEquals(2, result.size(), "Map should contain both the original and the added entry");
        assertEquals("FRIDAY", result.get("name"));
        assertEquals("added", result.get("extra"));
    }

    @Test
    void toMap_returnsCompactLinkedMapImplementation() {
        // GIVEN
        Month enumInstance = Month.JANUARY;
        // WHEN
        Map<String, Object> result = EnumConversions.toMap(enumInstance, mockConverter);
        // THEN
        assertInstanceOf(CompactLinkedMap.class, result, "The concrete implementation should be CompactLinkedMap");
    }
}
