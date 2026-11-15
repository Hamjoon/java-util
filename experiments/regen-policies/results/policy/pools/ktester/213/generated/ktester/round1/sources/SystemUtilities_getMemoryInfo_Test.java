package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.function.Executable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@ExtendWith(MockitoExtension.class)
class SystemUtilities_getMemoryInfo_Test {

    /**
     * Verifies that {@link SystemUtilities#getMemoryInfo()} returns a {@link SystemUtilities.MemoryInfo}
     * instance whose values are consistent with the current {@link Runtime} state.
     */
    @Test
    void testGetMemoryInfoReturnsConsistentValues() {
        // given – obtain the Runtime snapshot that the method under test will also use
        Runtime runtime = Runtime.getRuntime();
        long expectedTotal = runtime.totalMemory();
        long expectedFree = runtime.freeMemory();
        long expectedMax = runtime.maxMemory();
        // when – invoke the focal method
        SystemUtilities.MemoryInfo info = SystemUtilities.getMemoryInfo();
        // then – validate the returned data
        assertNotNull(info, "MemoryInfo should never be null");
        assertEquals(expectedTotal, info.getTotalMemory(), "Total memory reported by MemoryInfo should match Runtime.totalMemory()");
        assertEquals(expectedFree, info.getFreeMemory(), "Free memory reported by MemoryInfo should match Runtime.freeMemory()");
        assertEquals(expectedMax, info.getMaxMemory(), "Max memory reported by MemoryInfo should match Runtime.maxMemory()");
        // Additional sanity checks (mirroring existing test expectations)
        assertTrue(info.getTotalMemory() >= 0, "Total memory must be >= 0");
        assertTrue(info.getFreeMemory() >= 0, "Free memory must be >= 0");
        assertTrue(info.getMaxMemory() >= -1, "Max memory must be >= -1 (undefined)");
        assertTrue(info.getFreeMemory() <= info.getTotalMemory(), "Free memory cannot exceed total memory");
        assertTrue(info.getMaxMemory() == -1 || info.getTotalMemory() <= info.getMaxMemory(), "If max memory is defined, total memory cannot exceed it");
    }

    /**
     * Consistency across multiple invocations – two successive calls should produce
     * monotonically non‑decreasing totalMemory and each result must respect the invariants.
     */
    @Test
    void testMultipleInvocationsConsistency() {
        SystemUtilities.MemoryInfo first = SystemUtilities.getMemoryInfo();
        SystemUtilities.MemoryInfo second = SystemUtilities.getMemoryInfo();
        assertNotNull(first);
        assertNotNull(second);
        // totalMemory may stay the same or increase (e.g., after GC or heap expansion)
        assertTrue(second.getTotalMemory() >= first.getTotalMemory(), "Second totalMemory should be >= first totalMemory");
        // freeMemory must always be within its totalMemory bounds
        assertAll(() -> assertTrue(first.getFreeMemory() <= first.getTotalMemory(), "First freeMemory <= first totalMemory"), () -> assertTrue(second.getFreeMemory() <= second.getTotalMemory(), "Second freeMemory <= second totalMemory"));
    }

    /**
     * Thread‑safety under concurrent access – invoke {@code getMemoryInfo()} from many threads
     * simultaneously and ensure every returned instance satisfies the same invariants.
     */
    @Test
    void testConcurrentAccess() throws InterruptedException, ExecutionException {
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<SystemUtilities.MemoryInfo>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                // signal this thread is ready
                ready.countDown();
                // wait for the go‑signal
                start.await();
                return SystemUtilities.getMemoryInfo();
            }));
        }
        // Wait until all threads are ready, then release them together
        ready.await();
        start.countDown();
        // Collect results and verify invariants
        for (Future<SystemUtilities.MemoryInfo> f : futures) {
            // may throw if method fails
            SystemUtilities.MemoryInfo info = f.get();
            assertNotNull(info, "MemoryInfo from concurrent call must not be null");
            assertAll(() -> assertTrue(info.getTotalMemory() >= 0, "totalMemory >= 0"), () -> assertTrue(info.getFreeMemory() >= 0, "freeMemory >= 0"), () -> assertTrue(info.getFreeMemory() <= info.getTotalMemory(), "freeMemory <= totalMemory"), () -> assertTrue(info.getMaxMemory() >= -1, "maxMemory >= -1 (undefined)"), () -> assertTrue(info.getMaxMemory() == -1 || info.getTotalMemory() <= info.getMaxMemory(), "if maxMemory defined, totalMemory <= maxMemory"));
        }
        executor.shutdownNow();
    }

    /**
     * Behavior when {@link Runtime#maxMemory()} reports an undefined value (-1).
     * In a normal JVM this is rare, but the test asserts that if such a value is observed,
     * the other invariants still hold.
     */
    @Test
    void testUndefinedMaxMemoryBehavior() {
        SystemUtilities.MemoryInfo info = SystemUtilities.getMemoryInfo();
        assertNotNull(info);
        // The test does not force -1 (cannot mock Runtime), but validates the contract:
        if (info.getMaxMemory() == -1) {
            // When maxMemory is undefined, only the free/total relationship matters
            assertTrue(info.getFreeMemory() <= info.getTotalMemory(), "freeMemory must be <= totalMemory even when maxMemory is undefined");
        } else {
            // Normal case – also verify total <= max
            assertTrue(info.getTotalMemory() <= info.getMaxMemory(), "totalMemory must be <= maxMemory when maxMemory is defined");
        }
    }

    /**
     * Memory usage after allocation – allocate a sizable byte array and verify that
     * {@code freeMemory} decreases (or stays the same) and that {@code totalMemory}
     * does not shrink.
     */
    @Test
    void testMemoryUsageAfterAllocation() {
        SystemUtilities.MemoryInfo before = SystemUtilities.getMemoryInfo();
        // Allocate ~10 MiB and keep a strong reference
        byte[] payload = new byte[10 * 1024 * 1024];
        SystemUtilities.MemoryInfo after = SystemUtilities.getMemoryInfo();
        assertAll(() -> assertTrue(after.getTotalMemory() >= before.getTotalMemory(), "totalMemory should not decrease after allocation"), () -> assertTrue(after.getFreeMemory() <= before.getFreeMemory(), "freeMemory should not increase after allocation"));
        // Prevent compiler optimisations
        assertNotNull(payload);
    }

    /**
     * Memory reclaimed after explicit {@code System.gc()} – after nullifying the large array
     * and invoking garbage collection, {@code freeMemory} should be greater than or equal
     * to the value measured just before the GC.
     */
    @Test
    void testMemoryReclaimedAfterGC() throws InterruptedException {
        // Allocate and keep reference
        byte[] payload = new byte[10 * 1024 * 1024];
        SystemUtilities.MemoryInfo beforeGc = SystemUtilities.getMemoryInfo();
        // Release reference and request GC
        payload = null;
        System.gc();
        // Give the GC a little time (not guaranteed but helps on most JVMs)
        Thread.sleep(200);
        SystemUtilities.MemoryInfo afterGc = SystemUtilities.getMemoryInfo();
        assertAll(() -> assertTrue(afterGc.getFreeMemory() >= beforeGc.getFreeMemory(), "freeMemory should be >= before GC after reclamation"), () -> assertTrue(afterGc.getTotalMemory() >= beforeGc.getTotalMemory(), "totalMemory should not shrink after GC"));
    }
}
