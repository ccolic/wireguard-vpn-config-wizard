/*
 * This software is GPL2 licensed, find further license information
 * in the LICENSE file located in the root directory
 *
 * Created on : 22-12-17
 * Authors    : Christian Colic, Marc Werenfels
 *
 */
package wgWizard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import wgWizard.config.Keypair;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeypairTest {
    private Keypair keypair;

    @BeforeEach
    void setUp() {
        this.keypair = new Keypair();
    }

    @Test
    void getPrivateKey() {
        String privateKey = this.keypair.getPrivateKey();
        assertEquals(44, privateKey.length());
    }

    @Test
    void getPublicKey() {
        String publicKey = this.keypair.getPublicKey();
        assertEquals(44, publicKey.length());
    }

    @Test
    void gensk() {
        String publicKey = Keypair.genpsk();
        assertEquals(44, publicKey.length());
    }
}