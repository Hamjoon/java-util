package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import java.net.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemUtilities_getNetworkInterfaces_Test {

    /**
     * Mocked static holder for {@link NetworkInterface}.
     */
    private static MockedStatic<NetworkInterface> networkInterfaceStaticMock;

    @BeforeAll
    static void setupBeforeAll() {
        // Enable static mocking for NetworkInterface class for the whole test suite.
        networkInterfaceStaticMock = mockStatic(NetworkInterface.class);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the static mock after each test so that stubbings from one test do not leak.
        networkInterfaceStaticMock.reset();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static mock – important to avoid side‑effects on other tests.
        networkInterfaceStaticMock.close();
    }

    /**
     * Verifies that only network interfaces that are {@code up} are returned and that all
     * fields of {@link SystemUtilities.NetworkInfo} are populated correctly.
     */
    @Test
    void testGetNetworkInterfacesReturnsOnlyUpInterfaces() throws Exception {
        // ---- arrange ---------------------------------------------------------
        // Mock a network interface that is up.
        NetworkInterface upIf = mock(NetworkInterface.class);
        when(upIf.isUp()).thenReturn(true);
        when(upIf.getName()).thenReturn("eth0");
        when(upIf.getDisplayName()).thenReturn("Ethernet 0");
        when(upIf.isLoopback()).thenReturn(false);
        InetAddress address = InetAddress.getByName("192.168.1.2");
        Enumeration<InetAddress> addressEnum = Collections.enumeration(Collections.singletonList(address));
        when(upIf.getInetAddresses()).thenReturn(addressEnum);
        // Mock a network interface that is down – it should be filtered out.
        NetworkInterface downIf = mock(NetworkInterface.class);
        when(downIf.isUp()).thenReturn(false);
        // The remaining methods are irrelevant because the interface will be ignored,
        // but we stub them to avoid unexpected NullPointerExceptions.
        when(downIf.getName()).thenReturn("down0");
        when(downIf.getDisplayName()).thenReturn("Down Interface");
        when(downIf.isLoopback()).thenReturn(false);
        when(downIf.getInetAddresses()).thenReturn(Collections.emptyEnumeration());
        // Create an enumeration that yields the two interfaces.
        Enumeration<NetworkInterface> niEnum = mock(Enumeration.class);
        when(niEnum.hasMoreElements()).thenReturn(true, true, false);
        when(niEnum.nextElement()).thenReturn(upIf, downIf);
        // Stub the static call.
        networkInterfaceStaticMock.when(NetworkInterface::getNetworkInterfaces).thenReturn(niEnum);
        // ---- act -------------------------------------------------------------
        List<SystemUtilities.NetworkInfo> result = SystemUtilities.getNetworkInterfaces();
        // ---- assert ----------------------------------------------------------
        assertNotNull(result, "Result must not be null");
        assertEquals(1, result.size(), "Only the interface that is up should be returned");
        SystemUtilities.NetworkInfo info = result.get(0);
        assertEquals("eth0", info.getName());
        assertEquals("Ethernet 0", info.getDisplayName());
        assertEquals(Collections.singletonList(address), info.getAddresses());
        assertFalse(info.isLoopback(), "Loopback flag must match the mock");
    }

    /**
     * When {@link NetworkInterface#getNetworkInterfaces()} throws a {@link SocketException},
     * the utility method should propagate it.
     */
    @Test
    void testGetNetworkInterfacesPropagatesSocketException() throws Exception {
        // ---- arrange ---------------------------------------------------------
        networkInterfaceStaticMock.when(NetworkInterface::getNetworkInterfaces).thenThrow(new SocketException("simulated failure"));
        // ---- act & assert ----------------------------------------------------
        SocketException ex = assertThrows(SocketException.class, SystemUtilities::getNetworkInterfaces, "SocketException should be propagated");
        assertEquals("simulated failure", ex.getMessage());
    }

    /**
     * When the underlying enumeration is empty, the method should return an empty list.
     */
    @Test
    void testGetNetworkInterfacesEmptyEnumeration() throws Exception {
        // ---- arrange ---------------------------------------------------------
        Enumeration<NetworkInterface> emptyEnum = Collections.emptyEnumeration();
        networkInterfaceStaticMock.when(NetworkInterface::getNetworkInterfaces).thenReturn(emptyEnum);
        // ---- act -------------------------------------------------------------
        List<SystemUtilities.NetworkInfo> result = SystemUtilities.getNetworkInterfaces();
        // ---- assert ----------------------------------------------------------
        assertNotNull(result);
        assertTrue(result.isEmpty(), "Result should be empty when no interfaces are present");
    }

    /**
     * Mixed interface states: two interfaces are up (eth0 and lo) and one is down (wlan0).
     * Verifies that only the up interfaces are returned and that the loopback flag
     * is propagated correctly.
     */
    @Test
    void testMixedInterfaceStates() throws Exception {
        // eth0 – up, not loopback, one address
        NetworkInterface eth0 = mock(NetworkInterface.class);
        when(eth0.isUp()).thenReturn(true);
        when(eth0.getName()).thenReturn("eth0");
        when(eth0.getDisplayName()).thenReturn("Ethernet 0");
        when(eth0.isLoopback()).thenReturn(false);
        InetAddress ethAddr = InetAddress.getByName("192.168.1.10");
        when(eth0.getInetAddresses()).thenReturn(Collections.enumeration(Collections.singletonList(ethAddr)));
        // lo – up, loopback, one address
        NetworkInterface lo = mock(NetworkInterface.class);
        when(lo.isUp()).thenReturn(true);
        when(lo.getName()).thenReturn("lo");
        when(lo.getDisplayName()).thenReturn("Loopback");
        when(lo.isLoopback()).thenReturn(true);
        InetAddress loAddr = InetAddress.getByName("127.0.0.1");
        when(lo.getInetAddresses()).thenReturn(Collections.enumeration(Collections.singletonList(loAddr)));
        // wlan0 – down, should be ignored
        NetworkInterface wlan0 = mock(NetworkInterface.class);
        when(wlan0.isUp()).thenReturn(false);
        when(wlan0.getName()).thenReturn("wlan0");
        when(wlan0.getDisplayName()).thenReturn("Wi‑Fi");
        when(wlan0.isLoopback()).thenReturn(false);
        when(wlan0.getInetAddresses()).thenReturn(Collections.emptyEnumeration());
        // Enumeration order: eth0, lo, wlan0
        Enumeration<NetworkInterface> en = mock(Enumeration.class);
        when(en.hasMoreElements()).thenReturn(true, true, true, false);
        when(en.nextElement()).thenReturn(eth0, lo, wlan0);
        networkInterfaceStaticMock.when(NetworkInterface::getNetworkInterfaces).thenReturn(en);
        List<SystemUtilities.NetworkInfo> result = SystemUtilities.getNetworkInterfaces();
        assertNotNull(result);
        assertEquals(2, result.size(), "Only the two up interfaces should be returned");
        // Verify eth0 entry
        SystemUtilities.NetworkInfo ethInfo = result.stream().filter(i -> "eth0".equals(i.getName())).findFirst().orElseThrow(() -> new AssertionError("eth0 not found"));
        assertEquals("Ethernet 0", ethInfo.getDisplayName());
        assertEquals(Collections.singletonList(ethAddr), ethInfo.getAddresses());
        assertFalse(ethInfo.isLoopback());
        // Verify lo entry
        SystemUtilities.NetworkInfo loInfo = result.stream().filter(i -> "lo".equals(i.getName())).findFirst().orElseThrow(() -> new AssertionError("lo not found"));
        assertEquals("Loopback", loInfo.getDisplayName());
        assertEquals(Collections.singletonList(loAddr), loInfo.getAddresses());
        assertTrue(loInfo.isLoopback());
    }

    /**
     * All interfaces are down – the method should return an empty list.
     */
    @Test
    void testAllInterfacesDown() throws Exception {
        NetworkInterface eth0 = mock(NetworkInterface.class);
        when(eth0.isUp()).thenReturn(false);
        when(eth0.getName()).thenReturn("eth0");
        when(eth0.getDisplayName()).thenReturn("Ethernet 0");
        when(eth0.isLoopback()).thenReturn(false);
        when(eth0.getInetAddresses()).thenReturn(Collections.emptyEnumeration());
        NetworkInterface wlan0 = mock(NetworkInterface.class);
        when(wlan0.isUp()).thenReturn(false);
        when(wlan0.getName()).thenReturn("wlan0");
        when(wlan0.getDisplayName()).thenReturn("Wi‑Fi");
        when(wlan0.isLoopback()).thenReturn(false);
        when(wlan0.getInetAddresses()).thenReturn(Collections.emptyEnumeration());
        Enumeration<NetworkInterface> en = mock(Enumeration.class);
        when(en.hasMoreElements()).thenReturn(true, true, false);
        when(en.nextElement()).thenReturn(eth0, wlan0);
        networkInterfaceStaticMock.when(NetworkInterface::getNetworkInterfaces).thenReturn(en);
        List<SystemUtilities.NetworkInfo> result = SystemUtilities.getNetworkInterfaces();
        assertNotNull(result);
        assertTrue(result.isEmpty(), "All interfaces are down, result must be empty");
    }

    /**
     * An up interface that has no assigned IP addresses – the returned {@link NetworkInfo}
     * should contain an empty address list.
     */
    @Test
    void testUpInterfaceWithNoAddresses() throws Exception {
        NetworkInterface eth0 = mock(NetworkInterface.class);
        when(eth0.isUp()).thenReturn(true);
        when(eth0.getName()).thenReturn("eth0");
        when(eth0.getDisplayName()).thenReturn("Ethernet 0");
        when(eth0.isLoopback()).thenReturn(false);
        when(eth0.getInetAddresses()).thenReturn(Collections.emptyEnumeration());
        Enumeration<NetworkInterface> en = mock(Enumeration.class);
        when(en.hasMoreElements()).thenReturn(true, false);
        when(en.nextElement()).thenReturn(eth0);
        networkInterfaceStaticMock.when(NetworkInterface::getNetworkInterfaces).thenReturn(en);
        List<SystemUtilities.NetworkInfo> result = SystemUtilities.getNetworkInterfaces();
        assertNotNull(result);
        assertEquals(1, result.size());
        SystemUtilities.NetworkInfo info = result.get(0);
        assertEquals("eth0", info.getName());
        assertTrue(info.getAddresses().isEmpty(), "Address list should be empty");
        assertFalse(info.isLoopback());
    }

    /**
     * Single loopback interface that is up – verifies that the {@code loopback}
     * flag in {@link NetworkInfo} is true.
     */
    @Test
    void testSingleLoopbackInterfaceUp() throws Exception {
        NetworkInterface lo = mock(NetworkInterface.class);
        when(lo.isUp()).thenReturn(true);
        when(lo.getName()).thenReturn("lo");
        when(lo.getDisplayName()).thenReturn("Loopback");
        when(lo.isLoopback()).thenReturn(true);
        InetAddress loAddr = InetAddress.getByName("127.0.0.1");
        when(lo.getInetAddresses()).thenReturn(Collections.enumeration(Collections.singletonList(loAddr)));
        Enumeration<NetworkInterface> en = mock(Enumeration.class);
        when(en.hasMoreElements()).thenReturn(true, false);
        when(en.nextElement()).thenReturn(lo);
        networkInterfaceStaticMock.when(NetworkInterface::getNetworkInterfaces).thenReturn(en);
        List<SystemUtilities.NetworkInfo> result = SystemUtilities.getNetworkInterfaces();
        assertNotNull(result);
        assertEquals(1, result.size());
        SystemUtilities.NetworkInfo info = result.get(0);
        assertEquals("lo", info.getName());
        assertTrue(info.isLoopback(), "Loopback flag must be true");
        assertEquals(Collections.singletonList(loAddr), info.getAddresses());
    }

    /**
     * The static call returns {@code null}. The method does not guard against this
     * and a {@link NullPointerException} is expected.
     */
    @Test
    void testNullEnumerationThrowsNPE() {
        networkInterfaceStaticMock.when(NetworkInterface::getNetworkInterfaces).thenReturn(null);
        assertThrows(NullPointerException.class, SystemUtilities::getNetworkInterfaces, "When the enumeration is null a NullPointerException should be thrown");
    }

    /**
     * {@link NetworkInterface#isUp()} throws a {@link SocketException}. The exception
     * should propagate unchanged.
     */
    @Test
    void testIsUpThrowsSocketException() throws Exception {
        NetworkInterface problematic = mock(NetworkInterface.class);
        when(problematic.isUp()).thenThrow(new SocketException("isUp failure"));
        // Other methods are stubbed to avoid NPE if they are accessed before the exception.
        when(problematic.getName()).thenReturn("bad0");
        when(problematic.getDisplayName()).thenReturn("Bad Interface");
        when(problematic.isLoopback()).thenReturn(false);
        when(problematic.getInetAddresses()).thenReturn(Collections.emptyEnumeration());
        Enumeration<NetworkInterface> en = mock(Enumeration.class);
        when(en.hasMoreElements()).thenReturn(true, false);
        when(en.nextElement()).thenReturn(problematic);
        networkInterfaceStaticMock.when(NetworkInterface::getNetworkInterfaces).thenReturn(en);
        SocketException ex = assertThrows(SocketException.class, SystemUtilities::getNetworkInterfaces, "SocketException from isUp() should propagate");
        assertEquals("isUp failure", ex.getMessage());
    }
}
