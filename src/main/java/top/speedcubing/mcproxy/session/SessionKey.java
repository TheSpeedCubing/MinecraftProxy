package top.speedcubing.mcproxy.session;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import javax.crypto.SecretKey;

public class SessionKey {
    public SecretKey secretKey;
    public KeyPair keyPair;
    public byte[] serverPublicKey;

    public SessionKey() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(1024);
            this.keyPair = keyGen.generateKeyPair();
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }

    }
}
