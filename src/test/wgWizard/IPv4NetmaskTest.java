package wgWizard;
/*
 * This software is GPL2 licensed, find further license information
 * in the LICENSE file located in the root directory
 *
 * Created on : 22-12-17
 * Authors    : Christian Colic, Marc Werenfels
 *
 */

import org.junit.jupiter.api.Test;
import wgWizard.config.IPv4Netmask;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IPv4NetmaskTest {

    @Test
    void validateIPv4Netmask() {
        boolean result = IPv4Netmask.validateIPv4Netmask("255.255.255.0");
        assertEquals(true, result);

        result = IPv4Netmask.validateIPv4Netmask("255.0.0.0");
        assertEquals(true, result);

        result = IPv4Netmask.validateIPv4Netmask("255.255.255.252");
        assertEquals(true, result);

        result = IPv4Netmask.validateIPv4Netmask("0.0.0.0");
        assertEquals(false, result);

        result = IPv4Netmask.validateIPv4Netmask("255.255.256.0");
        assertEquals(false, result);

        result = IPv4Netmask.validateIPv4Netmask("255.255.a.0");
        assertEquals(false, result);

        result = IPv4Netmask.validateIPv4Netmask("");
        assertEquals(false, result);

        result = IPv4Netmask.validateIPv4Netmask("128.0.0.0");
        assertEquals(false, result);

        result = IPv4Netmask.validateIPv4Netmask("1.1.1.1");
        assertEquals(false, result);

    }

    @Test
    void validateIPv4NetmaskPrefix() {
        boolean resultPrefix = IPv4Netmask.validateIPv4NetmaskPrefix("24");
        assertEquals(true, resultPrefix);

        resultPrefix = IPv4Netmask.validateIPv4NetmaskPrefix("24");
        assertEquals(true, resultPrefix);

        resultPrefix = IPv4Netmask.validateIPv4NetmaskPrefix("32");
        assertEquals(true, resultPrefix);

        resultPrefix = IPv4Netmask.validateIPv4NetmaskPrefix("02");
        assertEquals(false, resultPrefix);

        resultPrefix = IPv4Netmask.validateIPv4NetmaskPrefix("77");
        assertEquals(false, resultPrefix);

        resultPrefix = IPv4Netmask.validateIPv4NetmaskPrefix("7");
        assertEquals(false, resultPrefix);

        resultPrefix = IPv4Netmask.validateIPv4NetmaskPrefix("2");
        assertEquals(false, resultPrefix);

        resultPrefix = IPv4Netmask.validateIPv4NetmaskPrefix("1");
        assertEquals(false, resultPrefix);

        resultPrefix = IPv4Netmask.validateIPv4NetmaskPrefix("0");
        assertEquals(false, resultPrefix);


    }

    @Test
    void getNetworkAddress() {
        String networkAddress = IPv4Netmask.getNetworkAddress("10.0.1.0", new IPv4Netmask("24"));
        assertEquals("10.0.1.0", networkAddress);

        networkAddress = IPv4Netmask.getNetworkAddress("10.4.48.0", new IPv4Netmask("255.255.255.252"));
        assertEquals("10.4.48.0", networkAddress);

        networkAddress = IPv4Netmask.getNetworkAddress("192.168.12.0", new IPv4Netmask("23"));
        assertEquals("192.168.12.0", networkAddress);

        networkAddress = IPv4Netmask.getNetworkAddress("1.1.1.1", new IPv4Netmask("32"));
        assertEquals("1.1.1.1", networkAddress);

        networkAddress = IPv4Netmask.getNetworkAddress("255.255.255.255", new IPv4Netmask("24"));
        assertEquals("255.255.255.0", networkAddress);

        networkAddress = IPv4Netmask.getNetworkAddress("65.4.0.0", new IPv4Netmask("255.254.0.0"));
        assertEquals("65.4.0.0", networkAddress);
    }

    @Test
    void getNetmask() {
        String netmask = new IPv4Netmask("23").getNetmask();
        assertEquals("255.255.254.0", netmask);

        netmask = new IPv4Netmask("9").getNetmask();
        assertEquals("255.128.0.0", netmask);

        netmask = new IPv4Netmask("10").getNetmask();
        assertEquals("255.192.0.0", netmask);

        netmask = new IPv4Netmask("32").getNetmask();
        assertEquals("255.255.255.255", netmask);
    }

    @Test
    void getPrefix() {
        String netmask = new IPv4Netmask("255.255.254.0").getPrefix();
        assertEquals("23", netmask);

        netmask = new IPv4Netmask("255.128.0.0").getPrefix();
        assertEquals("9", netmask);

        netmask = new IPv4Netmask("255.192.0.0").getPrefix();
        assertEquals("10", netmask);

        netmask = new IPv4Netmask("255.255.255.255").getPrefix();
        assertEquals("32", netmask);

        netmask = new IPv4Netmask("255.0.0.0").getPrefix();
        assertEquals("8", netmask);

        netmask = new IPv4Netmask("255.255.0.0").getPrefix();
        assertEquals("16", netmask);

    }
}