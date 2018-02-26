/*
 * This software is GPL2 licensed, find further license information
 * in the LICENSE file located in the root directory
 *
 * Created on : 22-12-17
 * Authors    : Christian Colic, Marc Werenfels
 *
 */

package wgWizard.config;

import javafx.util.Pair;
import wgWizard.helper.Log;

import java.security.SecureRandom;
import java.util.Base64;

public class Keypair {
    private Base64.Encoder encoder = Base64.getEncoder();
    private SecureRandom random = new SecureRandom();

    private String privateKey;
    private String publicKey;

    /**
     * Generate a new keypair
     */
    public Keypair() {
        Pair<String, String> pair = genkey();
        this.privateKey = pair.getKey();
        this.publicKey = pair.getValue();
    }


    /**
     * Generates  a random private key in base64 in the same way as "wg genkey" would
     * The information was provided by Jason A. Donenfeld at https://lists.zx2c4.com/pipermail/wireguard/2017-September/001761.html
     *
     * @return a Pair with the private key and public key in base64 as Strings
     */
    private Pair<String, String> genkey() {
        Log.getInstance().info("Generating a new private key");
        byte privateKey[] = new byte[32];
        random.nextBytes(privateKey);
        privateKey[0] &= 248;
        privateKey[31] &= 127;
        privateKey[31] |= 64;

        return new Pair<>(encoder.encodeToString(privateKey), pubkey(privateKey));
    }

    /**
     * Calculates a public key and prints it in base64 in the same way as "wg pubkey" would
     * The information was provided by Jason A. Donenfeld at https://lists.zx2c4.com/pipermail/wireguard/2017-September/001761.html
     *
     * @param priv the private key as byte-array
     * @return the public key in base64 as String
     */
    private String pubkey(byte[] priv) {
        Log.getInstance().info("Generating a new public key");
        byte publicKey[] = new byte[32];
        Curve25519.eval(publicKey, 0, priv, null);

        return encoder.encodeToString(publicKey);
    }

    /**
     * Generates a random pre-shared key in base64
     * The information was provided by Jason A. Donenfeld at https://lists.zx2c4.com/pipermail/wireguard/2017-September/001761.html
     *
     * @return the pre-shared key in base64 as String
     */
    public static String genpsk() {
        SecureRandom random = new SecureRandom();
        Base64.Encoder encoder = Base64.getEncoder();
        byte privateKey[] = new byte[32];
        random.nextBytes(privateKey);
        return encoder.encodeToString(privateKey);
    }

    /**
     * Get the privateKey as String
     * @return privateKey as String
     */
    public String getPrivateKey() {
        return this.privateKey;
    }

    /**
     * Get the publicKey as String
     * @return publicKey as String
     */
    public String getPublicKey() {
        return this.publicKey;
    }
}
