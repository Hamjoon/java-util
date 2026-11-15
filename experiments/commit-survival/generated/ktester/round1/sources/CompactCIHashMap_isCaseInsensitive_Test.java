package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.*;
import java.lang.reflect.Method;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CompactCIHashMap_isCaseInsensitive_Test {

    private CompactCIHashMap<String, Integer> ciMap;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance before each test
        ciMap = new CompactCIHashMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Cleanup instance reference
        ciMap = null;
    }

    private static Stream<CompactCIHashMap<?, ?>> genericMapProvider() {
        return Stream.of(new CompactCIHashMap<Integer, Double>(), new CompactCIHashMap<Object, Object>());
    }

    @Test
    void isCaseInsensitive_ShouldReturnTrue() {
        // Given: a newly created CompactCIHashMap (setup in @BeforeEach)
        // When: invoking the protected method isCaseInsensitive()
        // The test class resides in the same package, so it can access the protected method directly.
        boolean result = ciMap.isCaseInsensitive();
        // Then: the method must always return true
        assertTrue(result, "CompactCIHashMap.isCaseInsensitive() should always return true");
    }

    @Test
    void isCaseInsensitive_ShouldRemainTrueAfterAddingEntries() {
        // Given: a map with some entries
        ciMap.put("KeyOne", 1);
        ciMap.put("KEYTWO", 2);
        ciMap.put("keyThree", 3);
        // When: invoking the protected method again
        boolean result = ciMap.isCaseInsensitive();
        // Then: it should still return true
        assertTrue(result, "isCaseInsensitive() must remain true after adding entries");
    }

    @Test
    void isCaseInsensitive_ShouldRemainTrueAfterRemoveOperations() {
        // Given: a map populated with entries
        ciMap.put("Alpha", 10);
        ciMap.put("Beta", 20);
        ciMap.put("Gamma", 30);
        // When: some entries are removed
        ciMap.remove("Beta");
        // no‑op removal
        ciMap.remove("NonExistingKey");
        // Then: the flag is still true
        assertTrue(ciMap.isCaseInsensitive(), "isCaseInsensitive() must remain true after remove operations");
    }

    @Test
    void isCaseInsensitive_ShouldRemainTrueAfterClear() {
        // Given: a populated map
        ciMap.put("One", 1);
        ciMap.put("Two", 2);
        ciMap.put("Three", 3);
        // When: the map is cleared
        ciMap.clear();
        // Then: the method still reports case‑insensitivity
        assertTrue(ciMap.isCaseInsensitive(), "isCaseInsensitive() must remain true after clear()");
    }

    @ParameterizedTest(name = "isCaseInsensitive on map of type {0}")
    @MethodSource("genericMapProvider")
    void isCaseInsensitive_WithDifferentGenerics_ShouldReturnTrue(CompactCIHashMap<?, ?> map) {
        // When: invoking the method on a freshly created map with arbitrary generic types
        boolean result = map.isCaseInsensitive();
        // Then: it must be true irrespective of the type parameters
        assertTrue(result, "isCaseInsensitive() should return true for any generic type configuration");
    }

    @Test
    void isCaseInsensitive_ReflectionInvocation_ShouldReturnTrue() throws Exception {
        // Obtain the protected method via reflection
        Method method = CompactCIHashMap.class.getDeclaredMethod("isCaseInsensitive");
        // allow invocation despite protected visibility
        method.setAccessible(true);
        // Invoke on the test instance
        Object returned = method.invoke(ciMap);
        // Verify the result
        assertTrue(returned instanceof Boolean, "Returned value must be a Boolean");
        assertTrue((Boolean) returned, "Reflection call must yield true");
    }

    @Test
    void isCaseInsensitive_AfterSerializationDeserialization_ShouldRemainTrue() throws IOException, ClassNotFoundException {
        // Populate the map with a few entries
        ciMap.put("First", 100);
        ciMap.put("Second", 200);
        ciMap.put("Third", 300);
        // Serialize to a byte array
        byte[] serialized;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(ciMap);
            out.flush();
            serialized = bos.toByteArray();
        }
        // Deserialize back to a new instance
        CompactCIHashMap<String, Integer> deserialized;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(serialized);
            ObjectInputStream in = new ObjectInputStream(bis)) {
            @SuppressWarnings("unchecked")
            CompactCIHashMap<String, Integer> tmp = (CompactCIHashMap<String, Integer>) in.readObject();
            deserialized = tmp;
        }
        // Verify that the deserialized map still reports case‑insensitivity
        assertTrue(deserialized.isCaseInsensitive(), "isCaseInsensitive() must remain true after serialization round‑trip");
        // Additionally, ensure the data survived the round‑trip (sanity check)
        assertEquals(3, deserialized.size(), "Deserialized map should contain the original number of entries");
    }
}
