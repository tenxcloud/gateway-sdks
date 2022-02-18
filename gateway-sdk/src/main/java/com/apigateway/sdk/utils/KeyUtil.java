/*
 * Licensed Materials - Property of tenxcloud.com
 * (C) Copyright 2019 TenxCloud. All Rights Reserved.
 */

package com.apigateway.sdk.utils;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import sun.security.util.DerInputStream;
import sun.security.util.DerValue;

import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * @author hpy
 * @date 2021
 */
public class KeyUtil {

    public static PrivateKey getPrivateKey(String KEY_ALGORITHM, byte[] priKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(priKey);
        return keyFactory.generatePrivate(pkcs8KeySpec);
    }

    public static PublicKey getPublicKey(String KEY_ALGORITHM, byte[] pubKey) throws InvalidKeySpecException, NoSuchAlgorithmException {
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(pubKey);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        return keyFactory.generatePublic(x509KeySpec);
    }

    /*public static PrivateKey privateKey(String privateKey) throws InvalidKeySpecException, NoSuchAlgorithmException {
        byte[] privateKeyBytes = org.apache.commons.codec.binary.Base64.decodeBase64(privateKey);
        return getPrivateKey("RSA", privateKeyBytes);
    }*/
    public static PrivateKey privateKey(String privateKey) throws GeneralSecurityException, IOException {
        byte[] encoded = org.apache.commons.codec.binary.Base64.decodeBase64(privateKey);
        DerInputStream derReader = new DerInputStream(encoded);
        DerValue[] seq = derReader.getSequence(0);
        if (seq.length < 9) {
            throw new GeneralSecurityException("Could not read private key");
        }
        // skip version seq[0];
        BigInteger modulus = seq[1].getBigInteger();
        BigInteger publicExp = seq[2].getBigInteger();
        BigInteger privateExp = seq[3].getBigInteger();
        BigInteger primeP = seq[4].getBigInteger();
        BigInteger primeQ = seq[5].getBigInteger();
        BigInteger expP = seq[6].getBigInteger();
        BigInteger expQ = seq[7].getBigInteger();
        BigInteger crtCoeff = seq[8].getBigInteger();
        RSAPrivateCrtKeySpec keySpec = new RSAPrivateCrtKeySpec(modulus, publicExp, privateExp, primeP, primeQ, expP, expQ, crtCoeff);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePrivate(keySpec);
    }

    public static PrivateKey getECPrivateKey(String privateStr) {
        try {
            Security.addProvider(new BouncyCastleProvider());
            KeyFactory keyFactory = KeyFactory.getInstance("ECDH", "BC");
            PKCS8EncodedKeySpec devicePriKeySpec = new PKCS8EncodedKeySpec(org.apache.commons.codec.binary.Base64.decodeBase64(privateStr));
            return keyFactory.generatePrivate(devicePriKeySpec);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
