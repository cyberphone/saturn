/*
 *  Copyright 2006-2016 WebPKI.org (http://webpki.org).
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.webpki.saturn.resources;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.util.Vector;

import javax.crypto.KeyAgreement;

import org.webpki.asn1.ASN1OctetString;
import org.webpki.asn1.ASN1Sequence;
import org.webpki.asn1.BaseASN1Object;
import org.webpki.asn1.CompositeContextSpecific;
import org.webpki.asn1.DerDecoder;
import org.webpki.asn1.ParseUtil;
import org.webpki.crypto.AlgorithmPreferences;
import org.webpki.crypto.CustomCryptoProvider;
import org.webpki.crypto.KeyAlgorithms;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONParser;
import org.webpki.saturn.common.DecryptionKeyHolder;
import org.webpki.saturn.common.EncryptedData;
import org.webpki.saturn.common.Encryption;
import org.webpki.util.ArrayUtil;
import org.webpki.util.Base64;
import org.webpki.util.Base64URL;
import org.webpki.util.DebugFormatter;

public class CryptoTesting {
    
    static final String ECDH_RESULT_WITHOUT_KDF = "SzFxLgluXyC07Pl5D9jMfIt-LIrZC9qByyJPYsDnuaY";
    static final String ECDH_RESULT_WITH_KDF    = "hzHdlfQIAEehb8Hrd_mFRhKsKLEzPfshfXs9l6areCc";

    static StringBuffer js = new StringBuffer();

    static final String aliceKey = 
        "{\"kty\":\"EC\"," +
         "\"crv\":\"P-256\"," +
           "\"x\":\"Ze2loSV3wrroKUN_4zhwGhCqo3Xhu1td4QjeQ5wIVR0\"," +
           "\"y\":\"HlLtdXARY_f55A3fnzQbPcm6hgr34Mp8p-nuzQCE0Zw\"," +
           "\"d\":\"r_kHyZ-a06rmxM3yESK84r1otSg-aQcVStkRhA-iCM8\"" +
         "}";

     
    static final String bobKey = 
      "{\"kty\":\"EC\"," +
       "\"crv\":\"P-256\"," +
         "\"x\":\"mPUKT_bAWGHIhg0TpjjqVsP1rXWQu_vwVOHHtNkdYoA\"," +
         "\"y\":\"8BQAsImGeAS46fyWw5MhYfGTT0IjBpFw2SS34Dv4Irs\"," +
         "\"d\":\"AtH35vJsQ9SGjYfOsjUxYXQKrPH3FjZHmEtSKoSN8cM\"" +
       "}";
    
    static BigInteger getCurvePoint (JSONObjectReader rd, String property, KeyAlgorithms ec) throws IOException {
        byte[] fixed_binary = rd.getBinary (property);
        if (fixed_binary.length != (ec.getPublicKeySizeInBits () + 7) / 8) {
            throw new IOException ("Public EC key parameter \"" + property + "\" is not nomalized");
        }
        return new BigInteger (1, fixed_binary);
    }
    
    static KeyPair getKeyPair (String jwk) throws Exception {
        JSONObjectReader rd = JSONParser.parse(jwk);
        KeyAlgorithms ec = KeyAlgorithms.getKeyAlgorithmFromID (rd.getString ("crv"),
                                                                AlgorithmPreferences.JOSE);
        if (!ec.isECKey ()) {
            throw new IOException ("\"crv\" is not an EC type");
        }
        ECPoint w = new ECPoint (getCurvePoint (rd, "x", ec), getCurvePoint (rd, "y", ec));
        PublicKey publicKey = KeyFactory.getInstance ("EC").generatePublic (new ECPublicKeySpec (w, ec.getECParameterSpec ()));
        PrivateKey privateKey = KeyFactory.getInstance ("EC").generatePrivate (new ECPrivateKeySpec (getCurvePoint (rd, "d", ec), ec.getECParameterSpec ()));
        return new KeyPair (publicKey, privateKey);
    }
    
    static byte[] createPKCS8PrivateKey(byte[]publicKey, byte[]privateKey) throws IOException {
        ASN1Sequence pkcs8 = ParseUtil.sequence(DerDecoder.decode(privateKey));
        ASN1Sequence inner = ParseUtil.sequence(DerDecoder.decode(ParseUtil.octet(pkcs8.get(2))));
        return new ASN1Sequence(new BaseASN1Object[]{pkcs8.get(0), pkcs8.get(1),
                new ASN1OctetString(new ASN1Sequence(new BaseASN1Object[]{
                                     inner.get(0),
                                     inner.get(1),
                                     inner.get(2), 
                                     new CompositeContextSpecific(1, 
                                             DerDecoder.decode(publicKey).get(1))}).encode())}).encode();
    }
    
    static void createPEM(String string, byte[] encoded) {
        js.append("const ECHD_TEST_" + string + "_KEY = \n" +
                  "'-----BEGIN " + string + " KEY-----\\\n")
          .append(new Base64(true).getBase64StringFromBinary(encoded).replace("\r\n", "\\\n"))
          .append("\\\n-----END " + string + " KEY-----';\n\n");
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Missing: outputfile");
            System.exit(3);
        }
        CustomCryptoProvider.forcedLoad(true);
        System.out.println("Content Encryption");
        byte[] k = DebugFormatter.getByteArrayFromHex("000102030405060708090a0b0c0d0e0f" +
                                                      "101112131415161718191a1b1c1d1e1f");

        byte[] p = DebugFormatter.getByteArrayFromHex("41206369706865722073797374656d20" +
                                                      "6d757374206e6f742062652072657175" +
                                                      "6972656420746f206265207365637265" +
                                                      "742c20616e64206974206d7573742062" +
                                                      "652061626c6520746f2066616c6c2069" +
                                                      "6e746f207468652068616e6473206f66" +
                                                      "2074686520656e656d7920776974686f" +
                                                      "757420696e636f6e76656e69656e6365");

        byte[] iv = DebugFormatter.getByteArrayFromHex("1af38c2dc2b96ffdd86694092341bc04");

        byte[] a = DebugFormatter.getByteArrayFromHex("546865207365636f6e64207072696e63" +
                                                      "69706c65206f66204175677573746520" +
                                                      "4b6572636b686f666673");

        byte[] e = DebugFormatter.getByteArrayFromHex("c80edfa32ddf39d5ef00c0b468834279" +
                                                      "a2e46a1b8049f792f76bfe54b903a9c9" +
                                                      "a94ac9b47ad2655c5f10f9aef71427e2" +
                                                      "fc6f9b3f399a221489f16362c7032336" +
                                                      "09d45ac69864e3321cf82935ac4096c8" +
                                                      "6e133314c54019e8ca7980dfa4b9cf1b" +
                                                      "384c486f3a54c51078158ee5d79de59f" +
                                                      "bd34d848b3d69550a67646344427ade5" +
                                                      "4b8851ffb598f7f80074b9473c82e2db");

        byte[] t = DebugFormatter.getByteArrayFromHex("652c3fa36b0a7c5b3219fab3a30bc1c4");

        byte[] pout = Encryption.contentDecryption(Encryption.JOSE_A128CBC_HS256_ALG_ID,
                                                   k,
                                                   e,
                                                   iv,
                                                   a,
                                                   t);
        if (!ArrayUtil.compare(p, pout)) {
            throw new IOException ("pout 1");
        }
        Encryption.AuthEncResult aer = Encryption.contentEncryption(Encryption.JOSE_A128CBC_HS256_ALG_ID,
                                                                    k,
                                                                    p,
                                                                    a);
        pout = Encryption.contentDecryption(Encryption.JOSE_A128CBC_HS256_ALG_ID,
                                            k,
                                            aer.getCipherText(),
                                            aer.getIv(),
                                            a,
                                            aer.getTag());
        if (!ArrayUtil.compare(p, pout)) {
            throw new IOException ("pout 2");
        }
        byte[] dataEncryptionKey = Encryption.generateDataEncryptionKey(Encryption.JOSE_A128CBC_HS256_ALG_ID);
        JSONObjectReader json = JSONParser.parse(aliceKey);
        String encrec = EncryptedData.encode(new JSONObjectWriter(json),
                                             Encryption.JOSE_A128CBC_HS256_ALG_ID,
                                             dataEncryptionKey).toString();
        if (!EncryptedData.parse(JSONParser.parse(encrec), true).getDecryptedData(dataEncryptionKey).toString().equals(json.toString())) {
            throw new IOException("Symmetric");
        }
        System.out.println("ECDH begin");
        KeyPair bob = getKeyPair(bobKey);
        KeyPair alice = getKeyPair(aliceKey);
        if (!Base64URL.encode(Encryption.receiverKeyAgreement(Encryption.JOSE_ECDH_ES_ALG_ID,
                Encryption.JOSE_A128CBC_HS256_ALG_ID,
                (ECPublicKey) bob.getPublic(),
                alice.getPrivate())).equals(ECDH_RESULT_WITH_KDF)) {
            throw new IOException("Bad ECDH");
        }
        Encryption.EcdhSenderResult ecdhRes = 
            Encryption.senderKeyAgreement(Encryption.JOSE_ECDH_ES_ALG_ID,
                                          Encryption.JOSE_A128CBC_HS256_ALG_ID,
                                          alice.getPublic());
        if (!ArrayUtil.compare(ecdhRes.getSharedSecret(),
            Encryption.receiverKeyAgreement(Encryption.JOSE_ECDH_ES_ALG_ID,
                                            Encryption.JOSE_A128CBC_HS256_ALG_ID,
                                            ecdhRes.getEphemeralKey(),
                                            alice.getPrivate()))) {
            throw new IOException("Bad ECDH");
        }
        KeyPairGenerator mallet = KeyPairGenerator.getInstance("RSA", "BC");
        mallet.initialize(2048);
        KeyPair malletKeys = mallet.generateKeyPair();
        Vector<DecryptionKeyHolder> decryptionKeys = new Vector<DecryptionKeyHolder>();
        decryptionKeys.add(new DecryptionKeyHolder(alice.getPublic(), alice.getPrivate(), Encryption.JOSE_ECDH_ES_ALG_ID));
        decryptionKeys.add(new DecryptionKeyHolder(bob.getPublic(), bob.getPrivate(), Encryption.JOSE_ECDH_ES_ALG_ID));
        decryptionKeys.add(new DecryptionKeyHolder(malletKeys.getPublic(), malletKeys.getPrivate(), Encryption.JOSE_RSA_OAEP_256_ALG_ID));

        JSONObjectReader unEncJson = JSONParser.parse("{\"hi\":\"\\u20ac\\u00e5\\u00f6k\"}");
        String encJson = EncryptedData.encode(new JSONObjectWriter(unEncJson),
                                              Encryption.JOSE_A128CBC_HS256_ALG_ID,
                                              bob.getPublic(),
                                              Encryption.JOSE_ECDH_ES_ALG_ID).toString();
        if (!unEncJson.toString().equals(EncryptedData.parse(JSONParser.parse(encJson), false).getDecryptedData(decryptionKeys).toString())) {
            throw new IOException("Bad JOSE ECDH");
        }
        encJson = EncryptedData.encode(new JSONObjectWriter(unEncJson),
                                       Encryption.JOSE_A128CBC_HS256_ALG_ID,
                                       malletKeys.getPublic(),
                                       Encryption.JOSE_RSA_OAEP_256_ALG_ID).toString();
        if (!unEncJson.toString().equals(EncryptedData.parse(JSONParser.parse(encJson), false).getDecryptedData(decryptionKeys).toString())) {
            throw new IOException("Bad JOSE ECDH");
        }
        KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH", "BC");
        keyAgreement.init(alice.getPrivate());
        keyAgreement.doPhase(bob.getPublic(), true);
        if (!Base64URL.encode(keyAgreement.generateSecret()).equals(ECDH_RESULT_WITHOUT_KDF)) {
            throw new IOException("Bad ECDH");
        }
        js.append("// ECDH test data\n\n" +
                  "const ECDH_RESULT_WITH_KDF    = '" + ECDH_RESULT_WITH_KDF + "';\n" +
                  "const ECDH_RESULT_WITHOUT_KDF = '" + ECDH_RESULT_WITHOUT_KDF + "';\n\n");
        createPEM("PRIVATE", createPKCS8PrivateKey(alice.getPublic().getEncoded(),
                                                   alice.getPrivate().getEncoded()));
        createPEM("PUBLIC", bob.getPublic().getEncoded());
        ArrayUtil.writeFile(args[0], js.toString().getBytes("UTF-8"));
        System.out.println("ECDH success");
    }
}
