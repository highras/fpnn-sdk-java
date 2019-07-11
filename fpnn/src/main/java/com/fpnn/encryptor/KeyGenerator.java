package com.fpnn.encryptor;

import com.fpnn.ErrorRecorder;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

class KeyGenerator {

    public class EncryptionKit {

        public byte[] selfPublicKey;

        public Cipher encryptor;
        public Cipher decryptor;

        public boolean streamMode;
        public int keyLength;
    }

    private boolean streamMode;
    private int keyLength;

    private ECPublicKey serverPublicKey;
    private KeyPairGenerator keyPairGen;

    KeyGenerator(String curve, byte[] peerPublicKey, boolean isStreamMode, boolean reinforce, boolean bcLib)
            throws GeneralSecurityException {

        streamMode = isStreamMode;

        if (reinforce)
            keyLength = 256;
        else
            keyLength = 128;

        KeyFactory kf = KeyFactory.getInstance("EC");
        X509EncodedKeySpec pkSpec = new X509EncodedKeySpec(peerPublicKey);
        PublicKey puk = kf.generatePublic(pkSpec);
        serverPublicKey = (ECPublicKey) puk;

        if (bcLib){
            keyPairGen = KeyPairGenerator.getInstance("EC", "BC");
        }else{
            keyPairGen = KeyPairGenerator.getInstance("EC", "SunEC");
        }

        keyPairGen.initialize(new ECGenParameterSpec(curve));
    }

    static KeyGenerator create(String curve, byte[] keyBytes, boolean isStreamMode, boolean reinforce, boolean bcLib)
            throws GeneralSecurityException {
        return new KeyGenerator(curve, keyBytes, isStreamMode, reinforce, bcLib);
    }

    EncryptionKit gen() throws GeneralSecurityException {

        //-- Generate Shared Secret
        KeyPair keyPair = keyPairGen.generateKeyPair();
        PublicKey puk = keyPair.getPublic();

        KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        ka.init(keyPair.getPrivate());
        ka.doPhase(serverPublicKey, true);

        EncryptionKit encKit = new EncryptionKit();

        byte[] sharedSecret = ka.generateSecret();

        //-- Generate Self Public Key
        ECPublicKey ecpk = (ECPublicKey) puk;
        ECPoint ecpoint = ecpk.getW();
        BigInteger bigIntegerX = ecpoint.getAffineX();
        BigInteger bigIntegerY = ecpoint.getAffineY();
        byte[] xba = bigIntegerX.toByteArray();
        byte[] yba = bigIntegerY.toByteArray();

        int xIdx = 0, yIdx = 0;
        if (xba[0] == 0)
            xIdx = 1;
        if (yba[0] == 0)
            yIdx = 1;

        encKit.selfPublicKey = new byte[xba.length + yba.length - xIdx - yIdx];
        System.arraycopy(xba, xIdx, encKit.selfPublicKey, 0, xba.length - xIdx);
        System.arraycopy(yba, yIdx, encKit.selfPublicKey, xba.length - xIdx, yba.length - yIdx);

        //-- Generate AES Encrypt/Decrypt Key
        byte[] encryptKey = genEncryptKey(sharedSecret);

        //-- Generate AES Encrypt/Decrypt IV
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(sharedSecret);
        byte[] encryptIV = md5.digest();

        encKit.encryptor = buildAESEncryptor(encryptIV, encryptKey);
        encKit.decryptor = buildAESDecryptor(encryptIV, encryptKey);

        encKit.keyLength = keyLength;
        encKit.streamMode = streamMode;

        return encKit;
    }

    private byte[] genEncryptKey(byte[] sharedSecret) throws GeneralSecurityException {

        if (keyLength == 128) {
            byte[] key = new byte[16];
            System.arraycopy(sharedSecret, 0, key, 0, 16);
            return key;
        }

        if (sharedSecret.length == 32)
            return sharedSecret;

        MessageDigest hash = MessageDigest.getInstance("SHA-256");
        hash.update(sharedSecret);
        return hash.digest();
    }

    private Cipher buildAESEncryptor(byte[] iv, byte[] key) {
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");

        try {
            Cipher cipher = Cipher.getInstance("AES/CFB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivSpec);
            return cipher;
        } catch (GeneralSecurityException ex) {

            ErrorRecorder.getInstance().recordError(ex);
        }

        return null;
    }

    private Cipher buildAESDecryptor(byte[] iv, byte[] key) {
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");

        try {
            Cipher cipher = Cipher.getInstance("AES/CFB/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);
            return cipher;
        } catch (GeneralSecurityException ex) {

            ErrorRecorder.getInstance().recordError(ex);
        }

        return null;
    }
}
