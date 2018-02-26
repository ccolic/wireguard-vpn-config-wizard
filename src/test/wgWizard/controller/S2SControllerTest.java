package wgWizard.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class S2SControllerTest {

    @Test
    void validateIPv4() {
        boolean result = S2SController.validateIPv4("8.8.8.8");
        assertEquals(true, result);

        result = S2SController.validateIPv4("192.168.1.255");
        assertEquals(true, result);

        result = S2SController.validateIPv4("255.1.1.1");
        assertEquals(true, result);

        result = S2SController.validateIPv4("172.16.10.0");
        assertEquals(true, result);

        result = S2SController.validateIPv4("192.168.1.256");
        assertEquals(false, result);

        result = S2SController.validateIPv4("192.3.1");
        assertEquals(false, result);

        result = S2SController.validateIPv4("0.0.20.300");
        assertEquals(false, result);
    }

    @Test
    void validateListenPort() {

        boolean result = S2SController.validateListenPort("10000");
        assertEquals(true, result);

        result = S2SController.validateListenPort("1");
        assertEquals(true, result);

        result = S2SController.validateListenPort("65535");
        assertEquals(true, result);

        result = S2SController.validateListenPort("9999");
        assertEquals(true, result);

        result = S2SController.validateListenPort("0");
        assertEquals(false, result);

        result = S2SController.validateListenPort("65536");
        assertEquals(false, result);

        result = S2SController.validateListenPort("999999");
        assertEquals(false, result);
    }

    @Test
    void validateInterfaceName() {
        boolean result = S2SController.validateInterfaceName("wg0");
        assertEquals(true, result);

        result = S2SController.validateInterfaceName("wg10");
        assertEquals(true, result);

        result = S2SController.validateInterfaceName("wg200");
        assertEquals(true, result);

        result = S2SController.validateInterfaceName("wg255");
        assertEquals(true, result);

        result = S2SController.validateInterfaceName("wg");
        assertEquals(false, result);

        result = S2SController.validateInterfaceName("wg256");
        assertEquals(false, result);

        result = S2SController.validateInterfaceName("wg999");
        assertEquals(false, result);
    }

    @Test
    void validateKeepaliveSec() {
        boolean result = S2SController.validateKeepaliveSec("1");
        assertEquals(true, result);

        result = S2SController.validateKeepaliveSec("120");
        assertEquals(true, result);

        result = S2SController.validateKeepaliveSec("65535");
        assertEquals(true, result);

        result = S2SController.validateKeepaliveSec("0");
        assertEquals(false, result);

        result = S2SController.validateKeepaliveSec("-1");
        assertEquals(false, result);

        result = S2SController.validateKeepaliveSec("65536");
        assertEquals(false, result);

        result = S2SController.validateKeepaliveSec("99999");
        assertEquals(false, result);
    }

    @Test
    void validateNetmask() {
        boolean result = S2SController.validateNetmask("255.255.255.0");
        assertEquals(true, result);

        result = S2SController.validateNetmask("255.0.0.0");
        assertEquals(true, result);

        result = S2SController.validateNetmask("255.255.255.252");
        assertEquals(true, result);

        result = S2SController.validateNetmask("0.0.0.0");
        assertEquals(false, result);

        result = S2SController.validateNetmask("255.255.256.0");
        assertEquals(false, result);

        result = S2SController.validateNetmask("255.255.a.0");
        assertEquals(false, result);

        result = S2SController.validateNetmask("");
        assertEquals(false, result);

        result = S2SController.validateNetmask("128.0.0.0");
        assertEquals(false, result);

        result = S2SController.validateNetmask("1.1.1.1");
        assertEquals(false, result);

        result = S2SController.validateNetmask("8");
        assertEquals(true, result);

        result = S2SController.validateNetmask("16");
        assertEquals(true, result);

        result = S2SController.validateNetmask("24");
        assertEquals(true, result);

        result = S2SController.validateNetmask("31");
        assertEquals(true, result);

        result = S2SController.validateNetmask("32");
        assertEquals(true, result);

        result = S2SController.validateNetmask("0");
        assertEquals(false, result);

        result = S2SController.validateNetmask("5");
        assertEquals(false, result);

        result = S2SController.validateNetmask("7");
        assertEquals(false, result);

        result = S2SController.validateNetmask("33");
        assertEquals(false, result);
    }
}