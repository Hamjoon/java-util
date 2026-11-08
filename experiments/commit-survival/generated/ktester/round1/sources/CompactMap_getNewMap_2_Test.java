package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Constructor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import java.util.function.Supplier;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class CompactMap_getNewMap_2_Test {

    /**
     * Holds the static mock for {@link ReflectionUtils}.
     */
    private MockedStatic<ReflectionUtils> reflectionMock;

    /**
     * Instance of the concrete test class.
     */
    private TestCompactMap mapUnderTest;

    /**
     * Concrete subclass used for testing – supplies a HashMap as the backing map.
     */
    private static class TestCompactMap extends CompactMap<String, String> {

        @Override
        protected Map<String, String> getNewMap() {
            // Default map used when the reflective construction fails.
            return new HashMap<>();
        }
    }

    /**
     * Simple subclass that delegates {@code getNewMap()} to a {@link Supplier}.
     */
    private static class FlexibleCompactMap extends CompactMap<String, String> {

        private final Supplier<Map<String, String>> supplier;

        FlexibleCompactMap(Supplier<Map<String, String>> supplier) {
            this.supplier = supplier;
        }

        @Override
        protected Map<String, String> getNewMap() {
            return supplier.get();
        }
    }

    /**
     * Subclass whose {@code getNewMap()} throws a runtime exception immediately.
     */
    private static class ThrowingCompactMap extends CompactMap<String, String> {

        @Override
        protected Map<String, String> getNewMap() {
            throw new IllegalArgumentException("getNewMap failure");
        }
    }

    /**
     * Map with a public int‑argument constructor that deliberately throws an exception.
     */
    private static class BadMap<K, V> extends HashMap<K, V> {

        public BadMap(int size) {
            super(size);
            throw new IllegalStateException("BadMap constructor failure");
        }
    }

    /**
     * Map whose int‑argument constructor throws an {@link OutOfMemoryError}.
     */
    private static class ErrorThrowingMap<K, V> extends HashMap<K, V> {

        public ErrorThrowingMap(int size) {
            super(size);
            throw new OutOfMemoryError("simulated OOME");
        }
    }

    /**
     * Map with a constructor that takes {@link Integer} (object) instead of primitive int.
     */
    private static class IntegerCtorMap<K, V> extends HashMap<K, V> {

        public IntegerCtorMap(Integer size) {
            super(size);
        }
    }

    /**
     * Map with a *private* int‑argument constructor.
     */
    private static class PrivateCtorMap<K, V> extends HashMap<K, V> {

        private PrivateCtorMap(int size) {
            super(size);
        }
    }

    /**
     * Class that has a public int‑argument constructor but does **not** implement {@link Map}.
     */
    private static class NonMapWithIntCtor {

        public NonMapWithIntCtor(int size) {
        }
    }

    /**
     * Abstract map subclass – reflection finds its constructor but instantiation fails.
     */
    private static abstract class AbstractMapSubclass<K, V> extends AbstractMap<K, V> {

        public AbstractMapSubclass(int size) {
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            return Collections.emptySet();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance for each test.
        mapUnderTest = new TestCompactMap();
        // Mock static calls to ReflectionUtils.
        reflectionMock = Mockito.mockStatic(ReflectionUtils.class);
        reflectionMock = Mockito.mockStatic(ReflectionUtils.class);
    }

    @AfterEach
    void teardownAfterEach() {
        // Release the static mock to avoid side‑effects on other tests.
        reflectionMock.close();
        reflectionMock.close();
    }

    /**
     * Verifies that when a constructor accepting an {@code int} is available,
     * {@code getNewMap(int)} returns a new map instance created with that size.
     */
    @Test
    void getNewMap_usesIntConstructor_whenAvailable() throws Exception {
        // Arrange – prepare the constructor that HashMap provides (public HashMap(int initialCapacity))
        Constructor<?> hashMapIntCtor = HashMap.class.getConstructor(int.class);
        // Mock ReflectionUtils to return the above constructor.
        reflectionMock.when(() -> ReflectionUtils.getConstructor(eq(HashMap.class), eq(Integer.TYPE))).thenReturn(hashMapIntCtor);
        // Act – invoke the protected method via reflection (since it's protected we can call it from the subclass).
        Map<String, String> result = mapUnderTest.getNewMap(42);
        // Assert – a new HashMap should have been created with the requested capacity.
        assertNotNull(result, "Resulting map must not be null");
        assertTrue(result instanceof HashMap, "Result should be a HashMap instance");
        // The size of a freshly created HashMap is 0 regardless of capacity, but we can verify that the constructor was used.
        // This is done by checking that the mock was invoked exactly once.
        reflectionMock.verify(() -> ReflectionUtils.getConstructor(eq(HashMap.class), eq(Integer.TYPE)), times(1));
    }

    /**
     * Verifies that when the reflective call fails (e.g., constructor not found or any exception),
     * {@code getNewMap(int)} falls back to the map returned by {@code getNewMap()}.
     */
    @Test
    void getNewMap_fallsBackToDefaultMap_whenReflectionFails() {
        // Arrange – make ReflectionUtils throw an exception to simulate a failure.
        reflectionMock.when(() -> ReflectionUtils.getConstructor(any(Class.class), any(Class.class))).thenThrow(new RuntimeException("simulated failure"));
        // Act
        Map<String, String> result = mapUnderTest.getNewMap(10);
        // Assert – the fallback map (a new HashMap from getNewMap()) should be returned.
        assertNotNull(result, "Resulting map must not be null");
        assertTrue(result instanceof HashMap, "Fallback map should be a HashMap");
        // Verify that the static method was indeed called and caused an exception.
        reflectionMock.verify(() -> ReflectionUtils.getConstructor(any(Class.class), any(Class.class)), times(1));
    }

    /* --------------------------------------------------------------
     * 2. No int constructor – fallback to original map (TreeMap)
     * -------------------------------------------------------------- */
    @Test
    void getNewMap_fallbackWhenNoIntConstructor() {
        // TreeMap has no public constructor taking an int.
        reflectionMock.when(() -> ReflectionUtils.getConstructor(any(Class.class), eq(Integer.TYPE))).thenThrow(new NoSuchMethodException());
        CompactMap<String, String> map = new FlexibleCompactMap(TreeMap::new);
        // the map we expect to get back
        Map<String, String> original = map.getNewMap();
        Map<String, String> result = map.getNewMap(5);
        assertSame(original, result, "Should return the original TreeMap instance");
        reflectionMock.verify(() -> ReflectionUtils.getConstructor(eq(TreeMap.class), eq(Integer.TYPE)), times(1));
    }

    /* --------------------------------------------------------------
     * 3. Constructor throws an exception – fallback to original map
     * -------------------------------------------------------------- */
    @Test
    void getNewMap_fallbackWhenConstructorThrowsException() throws Exception {
        Constructor<?> ctor = BadMap.class.getConstructor(int.class);
        reflectionMock.when(() -> ReflectionUtils.getConstructor(eq(BadMap.class), eq(Integer.TYPE))).thenReturn(ctor);
        CompactMap<String, String> map = new FlexibleCompactMap(() -> new BadMap<>(0));
        Map<String, String> original = map.getNewMap();
        Map<String, String> result = map.getNewMap(3);
        assertSame(original, result, "Fallback to the original map when constructor throws");
        reflectionMock.verify(() -> ReflectionUtils.getConstructor(eq(BadMap.class), eq(Integer.TYPE)), times(1));
    }

    /* --------------------------------------------------------------
     * 4. Original map supplied by getNewMap() is null
     * -------------------------------------------------------------- */
    @Test
    void getNewMap_originalMapIsNull() {
        CompactMap<String, String> map = new FlexibleCompactMap(() -> null);
        // No need to mock ReflectionUtils – a NullPointerException will be caught.
        Map<String, String> result = map.getNewMap(1);
        assertNull(result, "When original map is null the method should return null");
    }

    /* --------------------------------------------------------------
     * 5. Zero size parameter – still uses HashMap(int)
     * -------------------------------------------------------------- */
    @Test
    void getNewMap_zeroSizeCreatesEmptyMap() throws Exception {
        Constructor<?> ctor = HashMap.class.getConstructor(int.class);
        reflectionMock.when(() -> ReflectionUtils.getConstructor(eq(HashMap.class), eq(Integer.TYPE))).thenReturn(ctor);
        CompactMap<String, String> map = new FlexibleCompactMap(HashMap::new);
        Map<String, String> result = map.getNewMap(0);
        assertNotNull(result);
        assertTrue(result instanceof HashMap);
        assertEquals(0, result.size());
    }

    /* --------------------------------------------------------------
     * 6. Negative size – HashMap throws IllegalArgumentException, fallback
     * -------------------------------------------------------------- */
    @Test
    void getNewMap_negativeSizeFallsBack() throws Exception {
        Constructor<?> ctor = HashMap.class.getConstructor(int.class);
        reflectionMock.when(() -> ReflectionUtils.getConstructor(eq(HashMap.class), eq(Integer.TYPE))).thenReturn(ctor);
        CompactMap<String, String> map = new FlexibleCompactMap(HashMap::new);
        // empty HashMap
        Map<String, String> original = map.getNewMap();
        Map<String, String> result = map.getNewMap(-5);
        assertSame(original, result, "Negative size should cause fallback to original map");
    }

    /* --------------------------------------------------------------
     * 7. Constructor expects Integer (object) – NoSuchMethodException fallback
     * -------------------------------------------------------------- */
    @Test
    void getNewMap_integerObjectConstructorMismatch() {
        reflectionMock.when(() -> ReflectionUtils.getConstructor(eq(IntegerCtorMap.class), eq(Integer.TYPE))).thenThrow(new NoSuchMethodException());
        CompactMap<String, String> map = new FlexibleCompactMap(IntegerCtorMap::new);
        Map<String, String> original = map.getNewMap();
        Map<String, String> result = map.getNewMap(7);
        assertSame(original, result);
    }

    /* --------------------------------------------------------------
     * 8. Private int constructor – NoSuchMethodException fallback
     * -------------------------------------------------------------- */
    @Test
    void getNewMap_privateIntConstructorFallsBack() {
        reflectionMock.when(() -> ReflectionUtils.getConstructor(eq(PrivateCtorMap.class), eq(Integer.TYPE))).thenThrow(new NoSuchMethodException());
        CompactMap<String, String> map = new FlexibleCompactMap(PrivateCtorMap::new);
        Map<String, String> original = map.getNewMap();
        Map<String, String> result = map.getNewMap(4);
        assertSame(original, result);
    }

    /* --------------------------------------------------------------
     * 9. SecurityException during reflection – fallback
     * -------------------------------------------------------------- */
    @Test
    void getNewMap_securityExceptionDuringReflection() {
        reflectionMock.when(() -> ReflectionUtils.getConstructor(any(Class.class), any(Class.class))).thenThrow(new SecurityException("access denied"));
        CompactMap<String, String> map = new FlexibleCompactMap(HashMap::new);
        Map<String, String> original = map.getNewMap();
        Map<String, String> result = map.getNewMap(12);
        assertSame(original, result);
    }

    /* --------------------------------------------------------------
     * 10. Immutable empty map fallback (Collections.emptyMap())
     * -------------------------------------------------------------- */
    @Test
    void getNewMap_immutableEmptyMapFallback() {
        reflectionMock.when(() -> ReflectionUtils.getConstructor(eq(Collections.emptyMap().getClass()), eq(Integer.TYPE))).thenThrow(new NoSuchMethodException());
        CompactMap<String, String> map = new FlexibleCompactMap(Collections::emptyMap);
        Map<String, String> original = map.getNewMap();
        Map<String, String> result = map.getNewMap(3);
        assertSame(original, result);
        assertTrue(result.isEmpty());
    }

    /* --------------------------------------------------------------
     * 11. Constructor throws an Error – error propagates
     * -------------------------------------------------------------- */
    @Test
    void getNewMap_constructorThrowsError_propagates() throws Exception {
        Constructor<?> ctor = ErrorThrowingMap.class.getConstructor(int.class);
        reflectionMock.when(() -> ReflectionUtils.getConstructor(eq(ErrorThrowingMap.class), eq(Integer.TYPE))).thenReturn(ctor);
        CompactMap<String, String> map = new FlexibleCompactMap(ErrorThrowingMap::new);
        assertThrows(OutOfMemoryError.class, () -> map.getNewMap(2));
    }

    /* --------------------------------------------------------------
     * 12. getNewMap() itself throws – exception propagates
     * -------------------------------------------------------------- */
    @Test
    void getNewMap_getNewMapThrowsRuntimeException_propagates() {
        CompactMap<String, String> map = new ThrowingCompactMap();
        assertThrows(IllegalArgumentException.class, () -> map.getNewMap(8));
    }

    /* --------------------------------------------------------------
     * 13. Constructor returns non‑Map – ClassCastException caught, fallback
     * -------------------------------------------------------------- */
    @Test
    void getNewMap_nonMapReturnedFromConstructor_fallback() throws Exception {
        Constructor<?> ctor = NonMapWithIntCtor.class.getConstructor(int.class);
        reflectionMock.when(() -> ReflectionUtils.getConstructor(eq(NonMapWithIntCtor.class), eq(Integer.TYPE))).thenReturn(ctor);
        CompactMap<String, String> map = new FlexibleCompactMap(() -> new HashMap<>());
        Map<String, String> original = map.getNewMap();
        Map<String, String> result = map.getNewMap(15);
        assertSame(original, result);
    }

    /* --------------------------------------------------------------
     * 14. newInstance throws IllegalAccessException – fallback
     * -------------------------------------------------------------- */
    @Test
    void getNewMap_newInstanceIllegalAccessException_fallback() throws Exception {
        // Mock a Constructor that throws IllegalAccessException on newInstance()
        @SuppressWarnings("unchecked")
        Constructor<Map<String, String>> mockCtor = mock(Constructor.class);
        when(mockCtor.newInstance(anyInt())).thenThrow(new IllegalAccessException("not accessible"));
        reflectionMock.when(() -> ReflectionUtils.getConstructor(any(Class.class), eq(Integer.TYPE))).thenReturn(mockCtor);
        CompactMap<String, String> map = new FlexibleCompactMap(HashMap::new);
        Map<String, String> original = map.getNewMap();
        Map<String, String> result = map.getNewMap(20);
        assertSame(original, result);
    }

    /* --------------------------------------------------------------
     * 15. newInstance throws InstantiationException – fallback
     * -------------------------------------------------------------- */
    @Test
    void getNewMap_newInstanceInstantiationException_fallback() throws Exception {
        @SuppressWarnings("unchecked")
        Constructor<Map<String, String>> mockCtor = mock(Constructor.class);
        when(mockCtor.newInstance(anyInt())).thenThrow(new InstantiationException("cannot instantiate"));
        reflectionMock.when(() -> ReflectionUtils.getConstructor(eq(AbstractMapSubclass.class), eq(Integer.TYPE))).thenReturn(mockCtor);
        // The original map can be any map; we use a HashMap here.
        CompactMap<String, String> map = new FlexibleCompactMap(HashMap::new);
        Map<String, String> original = map.getNewMap();
        Map<String, String> result = map.getNewMap(3);
        assertSame(original, result);
    }
}
