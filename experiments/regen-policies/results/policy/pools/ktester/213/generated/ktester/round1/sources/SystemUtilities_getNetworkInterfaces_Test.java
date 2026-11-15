package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemUtilities_getNetworkInterfaces_Test {

    /**
     * -----------------------------------------------------------------------
     *  Test that only interfaces that are up are returned and that the data
     *  collected (name, display name, addresses, loopback flag) matches the
     *  mocked values.
     *  ----------------------------------------------------------------------- **
     */
    @Test
    void testGetNetworkInterfacesFiltersAndMapsCorrectly() throws Exception {
        // ----- Arrange ---------------------------------------------------------
        // Mock two network interfaces: one up (to be included) and one down (to be ignored)
        NetworkInterface upInterface = mock(NetworkInterface.class);
        when(upInterface.isUp()).thenReturn(true);
        when(upInterface.getName()).thenReturn("up0");
        when(upInterface.getDisplayName()).thenReturn("Up Interface 0");
        when(upInterface.isLoopback()).thenReturn(false);
        InetAddress upAddr1 = InetAddress.getByName("10.0.0.1");
        InetAddress upAddr2 = InetAddress.getByName("fe80::1");
        when(upInterface.getInetAddresses()).thenReturn(Collections.enumeration(Arrays.asList(upAddr1, upAddr2)));
        NetworkInterface downInterface = mock(NetworkInterface.class);
        // will be filtered out
        when(downInterface.isUp()).thenReturn(false);
        when(downInterface.getName()).thenReturn("down0");
        when(downInterface.getDisplayName()).thenReturn("Down Interface 0");
        when(downInterface.isLoopback()).thenReturn(false);
        when(downInterface.getInetAddresses()).thenReturn(Collections.emptyEnumeration());
        // Mock the static call NetworkInterface.getNetworkInterfaces()
        Enumeration<NetworkInterface> mockedEnumeration = Collections.enumeration(Arrays.asList(upInterface, downInterface));
        try (MockedStatic<NetworkInterface> mockedStatic = mockStatic(NetworkInterface.class)) {
            mockedStatic.when(NetworkInterface::getNetworkInterfaces).thenReturn(mockedEnumeration);
            // ----- Act -----------------------------------------------------------
            List<SystemUtilities.NetworkInfo> result = SystemUtilities.getNetworkInterfaces();
            // ----- Assert --------------------------------------------------------
            assertNotNull(result, "Result should never be null");
            assertEquals(1, result.size(), "Only the interface that is up should be returned");
            SystemUtilities.NetworkInfo info = result.get(0);
            assertEquals("up0", info.getName());
            assertEquals("Up Interface 0", info.getDisplayName());
            assertFalse(info.isLoopback(), "Loopback flag should reflect the mocked value");
            List<InetAddress> expectedAddrs = Arrays.asList(upAddr1, upAddr2);
            assertEquals(expectedAddrs, info.getAddresses(), "Collected addresses must match the mocked enumeration");
        }
    }

    /**
     * -----------------------------------------------------------------------
     *  Verify that a SocketException bubbling up from the underlying JDK call is
     *  propagated unchanged by the utility method.
     *  ----------------------------------------------------------------------- **
     */
    @Test
    void testGetNetworkInterfacesPropagatesSocketException() throws Exception {
        // ----- Arrange ---------------------------------------------------------
        try (MockedStatic<NetworkInterface> mockedStatic = mockStatic(NetworkInterface.class)) {
            mockedStatic.when(NetworkInterface::getNetworkInterfaces).thenThrow(new SocketException("simulated failure"));
            // ----- Act & Assert --------------------------------------------------
            SocketException thrown = assertThrows(SocketException.class, SystemUtilities::getNetworkInterfaces, "Method should re‑throw the SocketException from NetworkInterface");
            assertEquals("simulated failure", thrown.getMessage());
        }
    }

    /* -----------------------------------------------------------------------
     *  1. No network interfaces available – empty enumeration
     * ----------------------------------------------------------------------- */
    @Test
    void testNoNetworkInterfacesAvailable() throws Exception {
        // Arrange – empty enumeration
        Enumeration<NetworkInterface> emptyEnum = Collections.emptyEnumeration();
        try (MockedStatic<NetworkInterface> mocked = mockStatic(NetworkInterface.class)) {
            mocked.when(NetworkInterface::getNetworkInterfaces).thenReturn(emptyEnum);
            // Act
            List<SystemUtilities.NetworkInfo> result = SystemUtilities.getNetworkInterfaces();
            // Assert
            assertNotNull(result, "Result must never be null");
            assertTrue(result.isEmpty(), "When no interfaces exist the list should be empty");
        }
    }

    /* -----------------------------------------------------------------------
     *  2. Single interface that is down – should be filtered out
     * ----------------------------------------------------------------------- */
    @Test
    void testSingleDownInterfaceIsFiltered() throws Exception {
        NetworkInterface down = mock(NetworkInterface.class);
        when(down.isUp()).thenReturn(false);
        when(down.getName()).thenReturn("down0");
        when(down.getDisplayName()).thenReturn("Down Interface");
        when(down.isLoopback()).thenReturn(false);
        when(down.getInetAddresses()).thenReturn(Collections.emptyEnumeration());
        Enumeration<NetworkInterface> enumeration = Collections.enumeration(List.of(down));
        try (MockedStatic<NetworkInterface> mocked = mockStatic(NetworkInterface.class)) {
            mocked.when(NetworkInterface::getNetworkInterfaces).thenReturn(enumeration);
            List<SystemUtilities.NetworkInfo> result = SystemUtilities.getNetworkInterfaces();
            assertNotNull(result);
            assertTrue(result.isEmpty(), "Down interface must be filtered out, leaving an empty list");
        }
    }

    /* -----------------------------------------------------------------------
     *  3. Single up, non‑loopback interface with a single IPv4 address
     * ----------------------------------------------------------------------- */
    @Test
    void testSingleUpInterfaceWithOneAddress() throws Exception {
        NetworkInterface up = mock(NetworkInterface.class);
        when(up.isUp()).thenReturn(true);
        when(up.getName()).thenReturn("eth0");
        when(up.getDisplayName()).thenReturn("Ethernet 0");
        when(up.isLoopback()).thenReturn(false);
        InetAddress addr = InetAddress.getByName("192.168.1.10");
        when(up.getInetAddresses()).thenReturn(Collections.enumeration(List.of(addr)));
        try (MockedStatic<NetworkInterface> mocked = mockStatic(NetworkInterface.class)) {
            mocked.when(NetworkInterface::getNetworkInterfaces).thenReturn(Collections.enumeration(List.of(up)));
            List<SystemUtilities.NetworkInfo> result = SystemUtilities.getNetworkInterfaces();
            assertEquals(1, result.size());
            SystemUtilities.NetworkInfo info = result.get(0);
            assertEquals("eth0", info.getName());
            assertEquals("Ethernet 0", info.getDisplayName());
            assertFalse(info.isLoopback());
            assertEquals(List.of(addr), info.getAddresses());
        }
    }

    /* -----------------------------------------------------------------------
     *  4. Multiple interfaces – mixed up/down and loopback
     * ----------------------------------------------------------------------- */
    @Test
    void testMultipleMixedInterfaces() throws Exception {
        // lo – up, loopback, no addresses
        NetworkInterface lo = mock(NetworkInterface.class);
        when(lo.isUp()).thenReturn(true);
        when(lo.isLoopback()).thenReturn(true);
        when(lo.getName()).thenReturn("lo");
        when(lo.getDisplayName()).thenReturn("Loopback");
        when(lo.getInetAddresses()).thenReturn(Collections.emptyEnumeration());
        // wlan0 – up, not loopback, one address
        NetworkInterface wlan0 = mock(NetworkInterface.class);
        when(wlan0.isUp()).thenReturn(true);
        when(wlan0.isLoopback()).thenReturn(false);
        when(wlan0.getName()).thenReturn("wlan0");
        when(wlan0.getDisplayName()).thenReturn("Wi‑Fi");
        InetAddress wlanAddr = InetAddress.getByName("10.0.0.5");
        when(wlan0.getInetAddresses()).thenReturn(Collections.enumeration(List.of(wlanAddr)));
        // eth0 – down, should be ignored
        NetworkInterface eth0 = mock(NetworkInterface.class);
        when(eth0.isUp()).thenReturn(false);
        when(eth0.getName()).thenReturn("eth0");
        when(eth0.getDisplayName()).thenReturn("Ethernet 0");
        when(eth0.isLoopback()).thenReturn(false);
        when(eth0.getInetAddresses()).thenReturn(Collections.emptyEnumeration());
        try (MockedStatic<NetworkInterface> mocked = mockStatic(NetworkInterface.class)) {
            mocked.when(NetworkInterface::getNetworkInterfaces).thenReturn(Collections.enumeration(List.of(lo, wlan0, eth0)));
            List<SystemUtilities.NetworkInfo> result = SystemUtilities.getNetworkInterfaces();
            assertEquals(2, result.size(), "Only the two up interfaces should be returned");
            // Preserve enumeration order
            SystemUtilities.NetworkInfo loInfo = result.get(0);
            assertEquals("lo", loInfo.getName());
            assertTrue(loInfo.isLoopback());
            assertTrue(loInfo.getAddresses().isEmpty());
            SystemUtilities.NetworkInfo wlanInfo = result.get(1);
            assertEquals("wlan0", wlanInfo.getName());
            assertFalse(wlanInfo.isLoopback());
            assertEquals(List.of(wlanAddr), wlanInfo.getAddresses());
        }
    }

    /* -----------------------------------------------------------------------
     *  5. Interface up but with no InetAddress entries
     * ----------------------------------------------------------------------- */
    @Test
    void testUpInterfaceWithNoAddresses() throws Exception {
        NetworkInterface iface = mock(NetworkInterface.class);
        when(iface.isUp()).thenReturn(true);
        when(iface.isLoopback()).thenReturn(false);
        when(iface.getName()).thenReturn("eth1");
        when(iface.getDisplayName()).thenReturn("Ethernet 1");
        when(iface.getInetAddresses()).thenReturn(Collections.emptyEnumeration());
        try (MockedStatic<NetworkInterface> mocked = mockStatic(NetworkInterface.class)) {
            mocked.when(NetworkInterface::getNetworkInterfaces).thenReturn(Collections.enumeration(List.of(iface)));
            List<SystemUtilities.NetworkInfo> result = SystemUtilities.getNetworkInterfaces();
            assertEquals(1, result.size());
            SystemUtilities.NetworkInfo info = result.get(0);
            assertEquals("eth1", info.getName());
            assertEquals("Ethernet 1", info.getDisplayName());
            assertFalse(info.isLoopback());
            assertTrue(info.getAddresses().isEmpty(), "Addresses list must be empty");
        }
    }

    /* -----------------------------------------------------------------------
     *  6. Underlying SocketException is thrown
     * ----------------------------------------------------------------------- */
    @Test
    void testSocketExceptionPropagation() throws Exception {
        try (MockedStatic<NetworkInterface> mocked = mockStatic(NetworkInterface.class)) {
            mocked.when(NetworkInterface::getNetworkInterfaces).thenThrow(new SocketException("simulated failure"));
            SocketException ex = assertThrows(SocketException.class, SystemUtilities::getNetworkInterfaces, "Method must re‑throw the SocketException from the JDK call");
            assertEquals("simulated failure", ex.getMessage());
        }
    }

    /* -----------------------------------------------------------------------
     *  7. Loopback interface up with address
     * ----------------------------------------------------------------------- */
    @Test
    void testLoopbackInterfaceWithAddress() throws Exception {
        NetworkInterface lo = mock(NetworkInterface.class);
        when(lo.isUp()).thenReturn(true);
        when(lo.isLoopback()).thenReturn(true);
        when(lo.getName()).thenReturn("lo0");
        when(lo.getDisplayName()).thenReturn("Loopback Interface");
        InetAddress addr = InetAddress.getByName("127.0.0.1");
        when(lo.getInetAddresses()).thenReturn(Collections.enumeration(List.of(addr)));
        try (MockedStatic<NetworkInterface> mocked = mockStatic(NetworkInterface.class)) {
            mocked.when(NetworkInterface::getNetworkInterfaces).thenReturn(Collections.enumeration(List.of(lo)));
            List<SystemUtilities.NetworkInfo> result = SystemUtilities.getNetworkInterfaces();
            assertEquals(1, result.size());
            SystemUtilities.NetworkInfo info = result.get(0);
            assertEquals("lo0", info.getName());
            assertEquals("Loopback Interface", info.getDisplayName());
            assertTrue(info.isLoopback());
            assertEquals(List.of(addr), info.getAddresses());
        }
    }

    /* -----------------------------------------------------------------------
     *  8. Interface up with multiple addresses (IPv4 & IPv6)
     * ----------------------------------------------------------------------- */
    @Test
    void testInterfaceWithMultipleAddresses() throws Exception {
        NetworkInterface iface = mock(NetworkInterface.class);
        when(iface.isUp()).thenReturn(true);
        when(iface.isLoopback()).thenReturn(false);
        when(iface.getName()).thenReturn("eth0");
        when(iface.getDisplayName()).thenReturn("Ethernet 0");
        InetAddress ipv4 = InetAddress.getByName("192.168.1.10");
        InetAddress ipv6 = InetAddress.getByName("fe80::1");
        when(iface.getInetAddresses()).thenReturn(Collections.enumeration(List.of(ipv4, ipv6)));
        try (MockedStatic<NetworkInterface> mocked = mockStatic(NetworkInterface.class)) {
            mocked.when(NetworkInterface::getNetworkInterfaces).thenReturn(Collections.enumeration(List.of(iface)));
            List<SystemUtilities.NetworkInfo> result = SystemUtilities.getNetworkInterfaces();
            assertEquals(1, result.size());
            SystemUtilities.NetworkInfo info = result.get(0);
            assertEquals("eth0", info.getName());
            assertEquals("Ethernet 0", info.getDisplayName());
            assertFalse(info.isLoopback());
            assertEquals(List.of(ipv4, ipv6), info.getAddresses());
        }
    }

    /* -----------------------------------------------------------------------
     *  9. Interface up with null name and displayName
     * ----------------------------------------------------------------------- */
    @Test
    void testInterfaceWithNullNameAndDisplayName() throws Exception {
        NetworkInterface iface = mock(NetworkInterface.class);
        when(iface.isUp()).thenReturn(true);
        when(iface.isLoopback()).thenReturn(false);
        when(iface.getName()).thenReturn(null);
        when(iface.getDisplayName()).thenReturn(null);
        when(iface.getInetAddresses()).thenReturn(Collections.emptyEnumeration());
        try (MockedStatic<NetworkInterface> mocked = mockStatic(NetworkInterface.class)) {
            mocked.when(NetworkInterface::getNetworkInterfaces).thenReturn(Collections.enumeration(List.of(iface)));
            List<SystemUtilities.NetworkInfo> result = SystemUtilities.getNetworkInterfaces();
            assertEquals(1, result.size());
            SystemUtilities.NetworkInfo info = result.get(0);
            assertNull(info.getName(), "Name should be null as mocked");
            assertNull(info.getDisplayName(), "DisplayName should be null as mocked");
            assertFalse(info.isLoopback());
            assertTrue(info.getAddresses().isEmpty());
        }
    }

    /* -----------------------------------------------------------------------
     * 10. NetworkInterface.getNetworkInterfaces() returns null – NPE expected
     * ----------------------------------------------------------------------- */
    @Test
    void testNetworkInterfacesReturnsNullThrowsNPE() throws Exception {
        try (MockedStatic<NetworkInterface> mocked = mockStatic(NetworkInterface.class)) {
            mocked.when(NetworkInterface::getNetworkInterfaces).thenReturn(null);
            NullPointerException npe = assertThrows(NullPointerException.class, SystemUtilities::getNetworkInterfaces, "A null enumeration should cause a NullPointerException");
            // Message is JDK‑dependent; we only assert the type.
            assertNotNull(npe);
        }
    }
}
