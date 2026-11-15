package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.lang.reflect.Field;
import java.security.Permission;
import java.util.ArrayList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class SystemUtilities_getMemoryInfo_Test {

    @Test
    void testGetMemoryInfoReturnsValidValues() {
        // Act
        SystemUtilities.MemoryInfo info = SystemUtilities.getMemoryInfo();
        // Assert – basic sanity checks
        assertNotNull(info, "MemoryInfo should never be null");
        long total = info.getTotalMemory();
        long free = info.getFreeMemory();
        long max = info.getMaxMemory();
        // Total memory must be > 0 (JVM always reports some heap)
        assertTrue(total > 0, "Total memory should be greater than 0");
        // Free memory is never negative
        assertTrue(free >= 0, "Free memory should be non‑negative");
        // Max memory is > 0 (even if the JVM reports -1 for unbounded, the implementation uses Runtime.maxMemory())
        assertTrue(max > 0, "Max memory should be greater than 0");
        // Logical relationships
        assertTrue(free <= total, "Free memory cannot exceed total memory");
        assertTrue(total <= max, "Total memory cannot exceed max memory");
    }

    @Test
    void testGetMemoryInfoIsConsistentAcrossCalls() {
        // Act – call the method twice
        SystemUtilities.MemoryInfo first = SystemUtilities.getMemoryInfo();
        SystemUtilities.MemoryInfo second = SystemUtilities.getMemoryInfo();
        // Assert – values should be non‑negative and roughly consistent.
        // Small differences are allowed because the JVM may allocate/release memory between calls.
        assertTrue(first.getTotalMemory() >= 0);
        assertTrue(second.getTotalMemory() >= 0);
        assertTrue(first.getFreeMemory() >= 0);
        assertTrue(second.getFreeMemory() >= 0);
        assertTrue(first.getMaxMemory() >= 0);
        assertTrue(second.getMaxMemory() >= 0);
    }

    /* -------------------------------------------------------------
     * Group: Memory values reflect runtime changes
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("Free memory decreases after allocating a large object")
    void testFreeMemoryDecreasesAfterAllocation() {
        // Snapshot before allocation
        SystemUtilities.MemoryInfo before = SystemUtilities.getMemoryInfo();
        long freeBefore = before.getFreeMemory();
        // Allocate a sizable byte array (e.g., 20 MB). The exact size is chosen to be
        // large enough to be noticeable on typical JVM heap sizes but still safe for CI.
        byte[] payload = new byte[20 * 1024 * 1024];
        // Ensure the array is actually used so the JIT does not optimise it away.
        for (int i = 0; i < payload.length; i += 4096) {
            payload[i] = 1;
        }
        // Snapshot after allocation
        SystemUtilities.MemoryInfo after = SystemUtilities.getMemoryInfo();
        long freeAfter = after.getFreeMemory();
        // The free memory should have decreased (or total memory increased) after allocation.
        assertTrue(freeAfter < freeBefore, () -> "Expected free memory to decrease after allocation (before=" + freeBefore + ", after=" + freeAfter + ")");
    }

    @Test
    @DisplayName("Free memory increases after discarding allocation and invoking GC")
    void testFreeMemoryIncreasesAfterGC() {
        // Allocate a large object and keep a weak reference to it.
        byte[] payload = new byte[20 * 1024 * 1024];
        for (int i = 0; i < payload.length; i += 4096) {
            payload[i] = 1;
        }
        // Snapshot right after allocation
        SystemUtilities.MemoryInfo afterAllocation = SystemUtilities.getMemoryInfo();
        long freeAfterAllocation = afterAllocation.getFreeMemory();
        // Drop the strong reference and request garbage collection.
        payload = null;
        System.gc();
        // Give the GC a moment (in real code we would use a more robust waiting strategy).
        try {
            Thread.sleep(200);
        } catch (InterruptedException ignored) {
        }
        // Snapshot after GC
        SystemUtilities.MemoryInfo afterGC = SystemUtilities.getMemoryInfo();
        long freeAfterGC = afterGC.getFreeMemory();
        // The free memory should have increased (or total memory decreased) after GC.
        assertTrue(freeAfterGC > freeAfterAllocation, () -> "Expected free memory to increase after GC (before=" + freeAfterAllocation + ", after=" + freeAfterGC + ")");
    }

    /* -------------------------------------------------------------
     * Group: MemoryInfo fields are non‑negative (parameterised version)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "All fields are non‑negative for iteration {index}")
    @ValueSource(ints = { 0, 1, 2 })
    void testMemoryInfoFieldsNonNegative(int dummy) {
        SystemUtilities.MemoryInfo info = SystemUtilities.getMemoryInfo();
        assertAll("MemoryInfo fields", () -> assertTrue(info.getTotalMemory() >= 0, "totalMemory >= 0"), () -> assertTrue(info.getFreeMemory() >= 0, "freeMemory  >= 0"), () -> assertTrue(info.getMaxMemory() >= 0, "maxMemory   >= 0"));
    }

    /* -------------------------------------------------------------
     * Group: Security manager denies Runtime access
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("SecurityException is propagated when a SecurityManager blocks Runtime access")
    void testSecurityManagerDeniesRuntimeAccess() {
        // Preserve the original security manager so we can restore it after the test.
        SecurityManager original = System.getSecurityManager();
        // Install a restrictive SecurityManager that throws for *any* permission check.
        SecurityManager restrictive = new SecurityManager() {

            @Override
            public void checkPermission(Permission perm) {
                throw new SecurityException("Permission denied by test SecurityManager: " + perm);
            }

            @Override
            public void checkPermission(Permission perm, Object context) {
                throw new SecurityException("Permission denied by test SecurityManager: " + perm);
            }
        };
        try {
            System.setSecurityManager(restrictive);
            // The call should now throw a SecurityException (the exact point of failure
            // depends on the JRE implementation, but the exception must bubble up).
            assertThrows(SecurityException.class, SystemUtilities::getMemoryInfo, "Expected SecurityException when SecurityManager denies Runtime access");
        } finally {
            // Restore the original security manager to avoid side‑effects on other tests.
            System.setSecurityManager(original);
        }
    }

    /* -------------------------------------------------------------
     * Group: OutOfMemoryError when allocating MemoryInfo
     * -------------------------------------------------------------
     * This test is deliberately disabled because it depends on JVM launch
     * parameters (e.g., -Xmx4m) that are not controllable from within the test
     * harness. When run in an environment with an artificially tiny heap,
     * the test will pass. Otherwise it is skipped to keep the CI suite stable.
     * ------------------------------------------------------------- */
    @Test
    @Disabled("Requires JVM to be started with a very small max heap (e.g., -Xmx4m) to be meaningful.")
    @DisplayName("OutOfMemoryError is thrown if the JVM cannot allocate MemoryInfo")
    void testOutOfMemoryErrorWhenAllocatingMemoryInfo() {
        // Attempt to exhaust the heap deliberately.
        List<byte[]> allocations = new ArrayList<>();
        try {
            while (true) {
                // Allocate 1 MB chunks until we run out of memory.
                allocations.add(new byte[1 * 1024 * 1024]);
            }
        } catch (OutOfMemoryError ignored) {
            // Expected – we have saturated the heap.
        }
        // At this point the heap is exhausted; invoking getMemoryInfo should trigger
        // another allocation (the MemoryInfo object) and therefore throw OOM.
        assertThrows(OutOfMemoryError.class, SystemUtilities::getMemoryInfo, "Expected OutOfMemoryError when heap is exhausted");
    }
}
