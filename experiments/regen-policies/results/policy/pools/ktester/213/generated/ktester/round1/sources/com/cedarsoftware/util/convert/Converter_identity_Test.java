package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verifyNoInteractions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_identity_Test {

    /**
     * A mock {@link Converter} that can be passed to the identity method.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Simple POJO used for testing identity with a user‑defined class.
     */
    private static final class DummyObject {

        private final int id;

        private final String name;

        DummyObject(int id, String name) {
            this.id = id;
            this.name = name;
        }

        int getId() {
            return id;
        }

        String getName() {
            return name;
        }
    }

    /**
     * Simple POJO for the first group of tests.
     */
    private static final class MyPojo {

        private final String customObject;

        private final int id;

        MyPojo(String customObject, int id) {
            this.customObject = customObject;
            this.id = id;
        }

        public String getCustomObject() {
            return customObject;
        }

        public int getId() {
            return id;
        }
        // equals / hashCode are not required – we rely on reference equality.
    }

    /**
     * Another simple POJO used in the additional test group.
     */
    private static final class MyPojo2 {

        private final String name;

        private final int age;

        MyPojo2(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the mock was not used during a test.
        verifyNoInteractions(mockConverter);
    }

    private static Stream<Arguments> identityMethodTestsProvider() {
        String str = "Hello, World!";
        Integer intVal = 42;
        Object nullObj = null;
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3));
        MyPojo pojo = new MyPojo("TestObject", 1001);
        Boolean bool = Boolean.TRUE;
        return Stream.of(Arguments.of(str, null, "Verify that a non‑null String is returned unchanged when the Converter argument is null."), Arguments.of(intVal, null, "Pass an Integer value; the method should return the same Integer instance."), Arguments.of(nullObj, null, "When the source object is null, identity should return null regardless of the Converter argument."), Arguments.of(list, null, "Supply a mutable List; identity must return the exact same List reference (verified by reference equality in the test)."), Arguments.of(pojo, null, "Pass a user‑defined POJO to ensure identity returns the same POJO instance."), Arguments.of(bool, null, "Check that a Boolean value is returned unchanged, confirming generic type handling for primitive wrappers."));
    }

    private static Stream<Arguments> identityMethodAdditionalTestsProvider() {
        int[] intArray = new int[] { 1, 2, 3 };
        StringBuilder sb = new StringBuilder("test");
        Map<String, Integer> map = new HashMap<>();
        map.put("keyA", 10);
        map.put("keyB", 20);
        AtomicInteger atomicInt = new AtomicInteger(5);
        BigDecimal bigDecimal = new BigDecimal("123.456");
        LocalDateTime ldt = LocalDateTime.parse("2023-01-01T12:34:56");
        DayOfWeek dow = DayOfWeek.MONDAY;
        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        MyPojo2 pojo2 = new MyPojo2("Alice", 30);
        ConcurrentHashMap<String, String> concMap = new ConcurrentHashMap<>();
        concMap.put("alpha", "A");
        concMap.put("beta", "B");
        return Stream.of(Arguments.of(intArray, null, "Pass a primitive int array; identity should return the exact same array reference (verified by reference equality)."), Arguments.of(sb, null, "Supply a mutable StringBuilder instance; identity must return the identical object reference."), Arguments.of(map, null, "Provide a HashMap with entries; identity should return the same map instance."), Arguments.of(atomicInt, null, "Pass an AtomicInteger; identity must return the identical AtomicInteger object."), Arguments.of(bigDecimal, null, "Supply a BigDecimal instance; identity should return the same BigDecimal reference."), Arguments.of(ldt, null, "Provide a LocalDateTime object; identity must return the identical instance."), Arguments.of(dow, null, "Pass an enum constant (java.time.DayOfWeek.MONDAY); identity should return the same enum instance."), Arguments.of(uuid, null, "Supply a UUID object; identity should return the exact same UUID reference."), Arguments.of(pojo2, null, "Pass an instance of a user‑defined POJO; identity should return the same object reference."), Arguments.of(concMap, null, "Provide a ConcurrentHashMap; identity must return the identical map instance, confirming thread‑safe collections are handled unchanged."));
    }

    @Test
    void identityReturnsSameObjectReference() {
        String original = "identity-test";
        String result = Converter.identity(original, mockConverter);
        assertSame(original, result, "The identity method should return the exact same instance.");
    }

    @Test
    void identityWorksWithCustomObject() {
        DummyObject original = new DummyObject(42, "answer");
        DummyObject result = Converter.identity(original, mockConverter);
        assertSame(original, result, "The same custom object instance should be returned.");
        assertEquals(42, result.getId());
        assertEquals("answer", result.getName());
    }

    @Test
    void identityHandlesNullGracefully() {
        Object original = null;
        Object result = Converter.identity(original, mockConverter);
        assertNull(result, "Identity of null should be null.");
    }

    @Test
    void identityWithPrimitiveWrapperTypes() {
        Integer intVal = 7;
        Double doubleVal = 3.14;
        Boolean boolVal = Boolean.TRUE;
        assertSame(intVal, Converter.identity(intVal, mockConverter));
        assertSame(doubleVal, Converter.identity(doubleVal, mockConverter));
        assertSame(boolVal, Converter.identity(boolVal, mockConverter));
    }

    @Test
    void identityGenericInferenceAcrossDifferentTargetTypes() {
        Integer number = 10;
        String text = "generic";
        Integer asInteger = Converter.identity(number, mockConverter);
        String asString = Converter.identity(text, mockConverter);
        assertSame(number, asInteger, "Generic inference should allow assigning to a compatible target type.");
        assertSame(text, asString, "Generic inference should preserve the original reference.");
    }

    /**
     * Test cases from the "IdentityMethodTests" group.
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("identityMethodTestsProvider")
    void identityMethodTests(Object from, Converter converter, String description) {
        Object result = Converter.identity(from, converter);
        assertSame(from, result, description);
    }

    /**
     * Test cases from the "IdentityMethodAdditionalTests" group.
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("identityMethodAdditionalTestsProvider")
    void identityMethodAdditionalTests(Object from, Converter converter, String description) {
        Object result = Converter.identity(from, converter);
        assertSame(from, result, description);
    }
}
