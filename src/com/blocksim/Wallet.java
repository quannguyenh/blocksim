package com.blocksim;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.*;
import java.security.spec.ECGenParameterSpec;


// TODO: Add unspent txs
public class Wallet {
    PublicKey pb;
    PrivateKey pr;

    public Wallet() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
        Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA", "BC");
//		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA");
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        ECGenParameterSpec ecSpec = new ECGenParameterSpec("prime192v1");
        keyGen.initialize(ecSpec, random);
        KeyPair keyPair = keyGen.generateKeyPair();

        pr = keyPair.getPrivate();
        pb = keyPair.getPublic();
    }
}
