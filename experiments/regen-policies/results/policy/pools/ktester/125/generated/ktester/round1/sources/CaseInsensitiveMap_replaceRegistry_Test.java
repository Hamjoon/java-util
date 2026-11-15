package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.function.Function;
import java.util.AbstractMap.SimpleEntry;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.concurrent.*;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveMap_replaceRegistry_Test {

    /**
     * An empty registry used to reset the static state after each test.
     */
    private static final List<Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> EMPTY_REGISTRY = Collections.emptyList();

    @BeforeEach
    void setupBeforeEach() {
        // Ensure a clean state before each test – start from the default empty registry.
        // If the library has a default non‑empty registry this call will simply replace it
        // with an empty one, which is safe for the purpose of isolated testing.
        assertDoesNotThrow(() -> CaseInsensitiveMap.replaceRegistry(EMPTY_REGISTRY));
    }

    @AfterEach
    void teardownAfterEach() {
        // Restore the empty registry so that other test classes are not affected.
        assertDoesNotThrow(() -> CaseInsensitiveMap.replaceRegistry(EMPTY_REGISTRY));
    }

    @Test
    void replaceRegistry_nullList_throwsNPE() {
        assertThrows(NullPointerException.class, () -> CaseInsensitiveMap.replaceRegistry(null), "Expected NPE when the registry list is null");
    }

    @Test
    void replaceRegistry_containsNullEntry_throwsNPE() {
        List<Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> list = new ArrayList<>();
        // null entry
        list.add(null);
        assertThrows(NullPointerException.class, () -> CaseInsensitiveMap.replaceRegistry(list), "Expected NPE when the registry contains a null entry");
    }

    @Test
    void replaceRegistry_entryWithNullKey_throwsNPE() {
        List<Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> list = new ArrayList<>();
        // key (Class) is null
        Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>> entry = new SimpleEntry<>(null, size -> new HashMap<>(size));
        list.add(entry);
        assertThrows(NullPointerException.class, () -> CaseInsensitiveMap.replaceRegistry(list), "Expected NPE when an entry has a null key (Class)");
    }

    @Test
    void replaceRegistry_entryWithNullValue_throwsNPE() {
        List<Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> list = new ArrayList<>();
        // value (Function) is null
        Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>> entry = new SimpleEntry<>(HashMap.class, null);
        list.add(entry);
        assertThrows(NullPointerException.class, () -> CaseInsensitiveMap.replaceRegistry(list), "Expected NPE when an entry has a null value (Function)");
    }

    @Test
    void replaceRegistry_duplicateClass_throwsIllegalArgumentException() {
        List<Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> list = new ArrayList<>();
        // Two entries with the same Class (HashMap) – should trigger duplicate detection.
        Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>> e1 = new SimpleEntry<>(HashMap.class, size -> new HashMap<>(size));
        Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>> e2 = new SimpleEntry<>(HashMap.class, size -> new TreeMap<>());
        list.add(e1);
        list.add(e2);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> CaseInsensitiveMap.replaceRegistry(list));
        assertTrue(ex.getMessage().contains("Duplicate map type"), "Exception message should indicate a duplicate map type");
    }

    @Test
    void replaceRegistry_validList_replacesSuccessfully() {
        // First valid registry
        List<Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> first = new ArrayList<>();
        first.add(new SimpleEntry<>(HashMap.class, size -> new HashMap<>(size)));
        first.add(new SimpleEntry<>(TreeMap.class, size -> new TreeMap<>()));
        // Second valid registry (different classes)
        List<Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> second = new ArrayList<>();
        second.add(new SimpleEntry<>(LinkedHashMap.class, size -> new LinkedHashMap<>(size)));
        second.add(new SimpleEntry<>(ConcurrentHashMap.class, size -> new ConcurrentHashMap<>(size)));
        // Apply first registry – should not throw.
        assertDoesNotThrow(() -> CaseInsensitiveMap.replaceRegistry(first));
        // Apply second registry – also should not throw, proving that the previous
        // registry was replaced atomically.
        assertDoesNotThrow(() -> CaseInsensitiveMap.replaceRegistry(second));
    }

    @Test
    void replaceRegistry_emptyList_resultsInEmptyUnmodifiableRegistry() throws Exception {
        List<Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> empty = Collections.emptyList();
        assertDoesNotThrow(() -> CaseInsensitiveMap.replaceRegistry(empty));
        // Use reflection to read the private static field `mapRegistry`.
        Field f = CaseInsensitiveMap.class.getDeclaredField("mapRegistry");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> stored = (List<Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>>) f.get(null);
        assertNotNull(stored, "mapRegistry should not be null");
        assertTrue(stored.isEmpty(), "mapRegistry should be empty for an empty input list");
        assertThrows(UnsupportedOperationException.class, () -> stored.add(null), "The stored registry must be unmodifiable");
    }

    @Test
    void replaceRegistry_singleEntry_isStoredCorrectly() throws Exception {
        List<Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> list = new ArrayList<>();
        list.add(new SimpleEntry<>(HashMap.class, size -> new HashMap<>(size)));
        assertDoesNotThrow(() -> CaseInsensitiveMap.replaceRegistry(list));
        Field f = CaseInsensitiveMap.class.getDeclaredField("mapRegistry");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> stored = (List<Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>>) f.get(null);
        assertEquals(1, stored.size(), "Exactly one entry should be stored");
        assertEquals(HashMap.class, stored.get(0).getKey(), "Stored entry key should match the supplied class");
        assertNotNull(stored.get(0).getValue(), "Stored entry value (function) should not be null");
    }

    @Test
    void replaceRegistry_invalidOrder_throwsIllegalArgumentException() {
        // Assuming validateMappings enforces a specific ordering (e.g., more generic before
        // concurrent types). The exact message is not defined, so we only assert the type.
        List<Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> list = new ArrayList<>();
        list.add(new SimpleEntry<>(ConcurrentHashMap.class, size -> new ConcurrentHashMap<>(size)));
        list.add(new SimpleEntry<>(HashMap.class, size -> new HashMap<>(size)));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> CaseInsensitiveMap.replaceRegistry(list));
        assertTrue(ex.getMessage().toLowerCase().contains("order"), "Exception message should mention ordering");
    }

    @Test
    void replaceRegistry_functionReturnsNull_throwsIllegalArgumentException() {
        // Function that returns null – validateMappings is expected to invoke it for a sample size.
        List<Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> list = new ArrayList<>();
        list.add(new SimpleEntry<>(HashMap.class, size -> null));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> CaseInsensitiveMap.replaceRegistry(list));
        assertTrue(ex.getMessage().toLowerCase().contains("null"), "Exception should indicate that the mapping function returned null");
    }

    @Test
    void replaceRegistry_invalidMapClass_throwsIllegalArgumentException() {
        // String does not implement Map – should be rejected by validateMappings.
        List<Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> list = new ArrayList<>();
        list.add(new SimpleEntry<>(String.class, size -> new HashMap<>(size)));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> CaseInsensitiveMap.replaceRegistry(list));
        assertTrue(ex.getMessage().toLowerCase().contains("map"), "Exception should indicate that the key class is not a Map implementation");
    }

    @Test
    void replaceRegistry_mappingFunctionThrowsRuntimeException_propagates() {
        List<Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> list = new ArrayList<>();
        list.add(new SimpleEntry<>(HashMap.class, size -> {
            throw new RuntimeException("boom");
        }));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> CaseInsensitiveMap.replaceRegistry(list));
        assertEquals("boom", ex.getMessage(), "The original RuntimeException should be propagated unchanged");
    }

    @Test
    void replaceRegistry_concurrentInvocations_resultsInConsistentState() throws Exception {
        // Three distinct registries that will be applied concurrently.
        List<Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> regA = List.of(new SimpleEntry<>(HashMap.class, size -> new HashMap<>(size)));
        List<Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> regB = List.of(new SimpleEntry<>(TreeMap.class, size -> new TreeMap<>()));
        List<Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> regC = List.of(new SimpleEntry<>(LinkedHashMap.class, size -> new LinkedHashMap<>(size)));
        ExecutorService exec = Executors.newFixedThreadPool(3);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(3);
        Runnable taskA = () -> {
            try {
                startLatch.await();
                CaseInsensitiveMap.replaceRegistry(regA);
            } catch (Exception ignored) {
            } finally {
                doneLatch.countDown();
            }
        };
        Runnable taskB = () -> {
            try {
                startLatch.await();
                CaseInsensitiveMap.replaceRegistry(regB);
            } catch (Exception ignored) {
            } finally {
                doneLatch.countDown();
            }
        };
        Runnable taskC = () -> {
            try {
                startLatch.await();
                CaseInsensitiveMap.replaceRegistry(regC);
            } catch (Exception ignored) {
            } finally {
                doneLatch.countDown();
            }
        };
        exec.submit(taskA);
        exec.submit(taskB);
        exec.submit(taskC);
        // Release all threads almost simultaneously.
        startLatch.countDown();
        // Wait for all to finish.
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "All replacement tasks should finish");
        // Verify that the final static field is a fully‑formed, unmodifiable list.
        Field f = CaseInsensitiveMap.class.getDeclaredField("mapRegistry");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> stored = (List<Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>>) f.get(null);
        assertNotNull(stored, "mapRegistry must not be null after concurrent updates");
        assertFalse(stored.isEmpty(), "mapRegistry should contain at least one entry");
        // The stored list must be unmodifiable.
        assertThrows(UnsupportedOperationException.class, () -> stored.add(null));
        // The stored registry must be exactly one of the three we submitted.
        Set<Class<?>> possible = Set.of(HashMap.class, TreeMap.class, LinkedHashMap.class);
        assertEquals(1, stored.size(), "Only one entry should be present");
        assertTrue(possible.contains(stored.get(0).getKey()), "Stored entry should be from one of the registries used concurrently");
        exec.shutdownNow();
    }

    @Test
    void replaceRegistry_immutableStoredRegistry_cannotBeModified() throws Exception {
        List<Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> list = new ArrayList<>();
        list.add(new SimpleEntry<>(Hashtable.class, size -> new Hashtable<>(size)));
        assertDoesNotThrow(() -> CaseInsensitiveMap.replaceRegistry(list));
        Field f = CaseInsensitiveMap.class.getDeclaredField("mapRegistry");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> stored = (List<Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>>) f.get(null);
        assertThrows(UnsupportedOperationException.class, () -> stored.remove(0), "Attempting to modify the stored registry should throw UnsupportedOperationException");
    }
}
