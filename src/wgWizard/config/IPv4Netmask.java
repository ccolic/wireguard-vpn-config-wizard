/*
 * This software is GPL2 licensed, find further license information
 * in the LICENSE file located in the root directory
 *
 * Created on : 22-12-17
 * Authors    : Christian Colic, Marc Werenfels
 *
 */

package wgWizard.config;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents an IPv4 subnet mask
 * This stores the netmask as full notation, eg: 255.255.255.0
 * as well as in the prefix/slash notation, eg: /24
 */
public class IPv4Netmask {
    // Validation regex patterns
    private static final String IPV4_NETMASK_PATTERN =
            "^(((255\\.){3}(255|254|252|248|240|224|192|128|0+))" +
                    "|((255\\.){2}(255|254|252|248|240|224|192|128|0+)\\.0)" +
                    "|((255\\.)(255|254|252|248|240|224|192|128|0+)(\\.0+){2})" +
                    "|((255|254|252|248|240|224|192|128|0+)(\\.0+){3}))$";
    private static final String IPV4_NETMASK_PREFIX_PATTERN =
            "^([0-9]|[1-2][0-9]|3[0-2])$";

    private String netmask;
    private int prefix;

    /**
     * Creates a subnet mask object
     *
     * @param netmask The netmask either in full format "255.255.255.0" or as prefix "24"
     */
    public IPv4Netmask(String netmask) {
        if (validateIPv4Netmask(netmask)) {
            setNetmask(netmask);
            netmaskToPrefix();
        } else if (validateIPv4NetmaskPrefix(netmask)) {
            setPrefix(Integer.parseInt(netmask));
            prefixToNetmask();
        }
    }

    /**
     * Validate a netmask (full-format) x.x.x.x
     *
     * @param netmask The netmask in the full-format, eg: 255.255.255.0
     * @return true if valid, false if invalid
     */
    public static boolean validateIPv4Netmask(String netmask) {
        Pattern pattern = Pattern.compile(IPV4_NETMASK_PATTERN);
        Matcher matcher = pattern.matcher(netmask);
        if (matcher.matches() && netmask.substring(0, 3).equals("255")) {
            return true;
        }
        return false;
    }

    /**
     * Validate a netmask (prefix-format) /xx
     *
     * @param netmask The netmask in prefix-format, eg: 24
     * @return true if valid, false if invalid
     */
    public static boolean validateIPv4NetmaskPrefix(String netmask) {
        Pattern pattern = Pattern.compile(IPV4_NETMASK_PREFIX_PATTERN);
        Matcher matcher = pattern.matcher(netmask);
        if (matcher.matches() && Integer.parseInt(netmask) >= 8) {
            return true;
        }
        return false;
    }

    /**
     * Calculate the network address for any given IP and netmask
     *
     * @param ip   IP as String
     * @param mask Netmask as IPV4Netmask
     * @return the network address IP as String
     */
    public static String getNetworkAddress(String ip, IPv4Netmask mask) {
        String[] ipAddrParts = ip.split("\\.");
        String[] maskParts = mask.getNetmask().split("\\.");
        String networkAddr = "";

        for (int i = 0; i < 4; i++) {
            int x = Integer.parseInt(ipAddrParts[i]);
            int y = Integer.parseInt(maskParts[i]);
            int z = x & y;
            networkAddr += z + ".";
        }

        return networkAddr.substring(0, networkAddr.length() - 1);
    }

    /**
     * Calculate the prefix from the netmask
     */
    private void netmaskToPrefix() {

        /* Netmask */
        String[] st = netmask.split("\\.");

        if (st.length != 4) {
            throw new NumberFormatException("Invalid netmask address: " + netmask);
        }

        int i = 24;
        int netmaskNumeric = 0;

        if (Integer.parseInt(st[0]) < 255) {
            throw new NumberFormatException(
                    "The first byte of netmask can not be less than 255");
        }

        for (String aSt : st) {
            int value = Integer.parseInt(aSt);
            if (value != (value & 0xff)) {
                throw new NumberFormatException("Invalid netmask address: " + netmask);
            }
            netmaskNumeric += value << i;
            i -= 8;
        }

        /*
         * see if there are zeroes inside netmask, like: 1111111101111 This is
         * illegal, throw exception if encountered. Netmask should always have
         * only ones, then only zeroes, like: 11111111110000
         */
        boolean encounteredOne = false;
        int ourMaskBitPattern = 1;

        for (i = 0; i < 32; i++) {

            if ((netmaskNumeric & ourMaskBitPattern) != 0) {

                encounteredOne = true; // the bit is 1
            } else { // the bit is 0
                if (encounteredOne) {
                    throw new NumberFormatException("Invalid netmask: " +
                            netmask + " (bit " + (i + 1) + ")");
                }
            }

            ourMaskBitPattern = ourMaskBitPattern << 1;
        }

        int j;
        for (j = 0; j < 32; j++) {
            if ((netmaskNumeric << j) == 0)
                break;
        }
        this.prefix = j;
    }

    /**
     * Calculate the netmask from the prefix
     */
    private void prefixToNetmask() {
        final int bits = 32 - prefix;
        final int mask = 0xFFFFFFFF - ((1 << bits) - 1);

        if (prefix == 0) {
            this.netmask = "0.0.0.0";
        } else {
            this.netmask = Integer.toString(mask >> 24 & 0xFF, 10) + "." +  //                         11111111 & 0xFF = 0xFF
                    Integer.toString(mask >> 16 & 0xFF, 10) + "." +         //                 1111111111111111 & 0xFF = 0xFF
                    Integer.toString(mask >> 8 & 0xFF, 10) + "." +          //         111111111111111111111110 & 0xFF = 0xFE
                    Integer.toString(mask >> 0 & 0xFF, 10);                 // 11111111111111111111111000000000 & 0xFF = 0x00
        }

    }

    /**
     * Get the netmask in full format as String
     *
     * @return netmask in full format as String
     */
    public String getNetmask() {
        return this.netmask;
    }

    /**
     * Set the netmask in full format
     *
     * @param netmask netmask in full format as String
     */
    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    /**
     * Get the prefix as String
     *
     * @return the Prefix as String, for example: '24'
     */
    public String getPrefix() {
        return Integer.toString(this.prefix);
    }

    /**
     * Set the prefix
     *
     * @param prefix prefix as int
     */
    public void setPrefix(int prefix) {
        this.prefix = prefix;
    }
}
