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

import java.security.KeyPair;

import org.webpki.asn1.ASN1OctetString;
import org.webpki.asn1.ASN1Sequence;
import org.webpki.asn1.BaseASN1Object;
import org.webpki.asn1.CompositeContextSpecific;
import org.webpki.asn1.DerDecoder;
import org.webpki.asn1.ParseUtil;

import org.webpki.crypto.CustomCryptoProvider;
import org.webpki.crypto.HashAlgorithms;

import org.webpki.json.encryption.DataEncryptionAlgorithms;
import org.webpki.json.encryption.KeyEncryptionAlgorithms;

import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONParser;

import org.webpki.util.ArrayUtil;
import org.webpki.util.Base64;
import org.webpki.util.Base64URL;

/**
 * Create test data for the Node.js encryption implementation
 *
 */
public class CryptoTesting {
    
    static final String ECDH_RESULT_WITHOUT_KDF = "SzFxLgluXyC07Pl5D9jMfIt-LIrZC9qByyJPYsDnuaY";
    static final String ECDH_RESULT_WITH_KDF    = "hzHdlfQIAEehb8Hrd_mFRhKsKLEzPfshfXs9l6areCc";

    static final String JEF_TEST_STRING = "Hello encrypted world!";
    static final String JEF_RSA_KEY_ID  = "20170101:mybank:rsa";
    static final String JEF_EC_KEY_ID   = "20170101:mybank:ec";

    static StringBuffer js = new StringBuffer();

   
    static KeyPair getKeyPair(String name) throws IOException {
        return JSONParser.parse(ArrayUtil.getByteArrayFromInputStream(CryptoTesting.class.getResourceAsStream(name))).getKeyPair();
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
    
    static void createPEM(boolean rsa, String string, byte[] encoded) {
        js.append("const " + (rsa ? "RSA" : "ECHD") + "_TEST_" + string + "_KEY = \n" +
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
        KeyPair bob = getKeyPair("bobkey.jwk");
        KeyPair alice = getKeyPair("alicekey.jwk");
        KeyPair rsa = getKeyPair("rsakey.jwk");
        byte[] symkey = HashAlgorithms.SHA256.digest(Base64URL.decode(ECDH_RESULT_WITH_KDF));
        js.append("// JEF test data\n\n" +
                  "const ECDH_RESULT_WITH_KDF    = '" + ECDH_RESULT_WITH_KDF + "';\n" +
                  "const ECDH_RESULT_WITHOUT_KDF = '" + ECDH_RESULT_WITHOUT_KDF + "';\n\n" +
                  "const JEF_TEST_STRING         = ByteArray.stringToUtf8('" + JEF_TEST_STRING + "');\n" +
                  "const JEF_SYM_KEY             = '" + Base64URL.encode(symkey) + "';\n" +
                  "const JEF_EC_KEY_ID           = '" + JEF_EC_KEY_ID + "';\n" +
                  "const JEF_RSA_KEY_ID          = '" + JEF_RSA_KEY_ID + "';\n" +
                  "const JEF_ECDH_OBJECT_2 = ");
        JSONObjectWriter encryptedData  =
            JSONObjectWriter.createEncryptionObject(JEF_TEST_STRING.getBytes("UTF-8"),
                                                    DataEncryptionAlgorithms.JOSE_A128CBC_HS256_ALG_ID,
                                                    alice.getPublic(),
                                                    null,
                                                    KeyEncryptionAlgorithms.JOSE_ECDH_ES_ALG_ID);
        js.append(encryptedData.serializeToString(JSONOutputFormats.PRETTY_JS_NATIVE))
          .append(";\n\n" +
                 "const JEF_ECDH_OBJECT_1 = ");
        encryptedData =
              JSONObjectWriter.createEncryptionObject(JEF_TEST_STRING.getBytes("UTF-8"),
                                                      DataEncryptionAlgorithms.JOSE_A128CBC_HS256_ALG_ID,
                                                      alice.getPublic(),
                                                      JEF_EC_KEY_ID,
                                                      KeyEncryptionAlgorithms.JOSE_ECDH_ES_ALG_ID);
        js.append(encryptedData.serializeToString(JSONOutputFormats.PRETTY_JS_NATIVE))
        .append(";\n\n" +
               "const JEF_ECDH_OBJECT_3 = ");
        encryptedData =
            JSONObjectWriter.createEncryptionObject(JEF_TEST_STRING.getBytes("UTF-8"),
                                                    DataEncryptionAlgorithms.JOSE_A128GCM_ALG_ID,
                                                    alice.getPublic(),
                                                    JEF_EC_KEY_ID,
                                                    KeyEncryptionAlgorithms.JOSE_ECDH_ES_A128KW_ALG_ID);
        js.append(encryptedData.serializeToString(JSONOutputFormats.PRETTY_JS_NATIVE))
        .append(";\n\n" +
               "const JEF_ECDH_OBJECT_4 = ");
        encryptedData =
            JSONObjectWriter.createEncryptionObject(JEF_TEST_STRING.getBytes("UTF-8"),
                                                    DataEncryptionAlgorithms.JOSE_A256CBC_HS512_ALG_ID,
                                                    alice.getPublic(),
                                                    JEF_EC_KEY_ID,
                                                    KeyEncryptionAlgorithms.JOSE_ECDH_ES_A256KW_ALG_ID);
        js.append(encryptedData.serializeToString(JSONOutputFormats.PRETTY_JS_NATIVE))
        .append(";\n\n" +
               "const JEF_ECDH_OBJECT_5 = ");
        encryptedData =
            JSONObjectWriter.createEncryptionObject(JEF_TEST_STRING.getBytes("UTF-8"),
                                                    DataEncryptionAlgorithms.JOSE_A192CBC_HS384_ALG_ID,
                                                    alice.getPublic(),
                                                    JEF_EC_KEY_ID,
                                                    KeyEncryptionAlgorithms.JOSE_ECDH_ES_A192KW_ALG_ID);
        js.append(encryptedData.serializeToString(JSONOutputFormats.PRETTY_JS_NATIVE))
          .append(";\n\n" +
                  "const JEF_RSA_OBJECT_2 = ");
        encryptedData =
              JSONObjectWriter.createEncryptionObject(JEF_TEST_STRING.getBytes("UTF-8"),
                                                      DataEncryptionAlgorithms.JOSE_A128CBC_HS256_ALG_ID,
                                                      rsa.getPublic(),
                                                      null,
                                                      KeyEncryptionAlgorithms.JOSE_RSA_OAEP_256_ALG_ID);
        js.append(encryptedData.serializeToString(JSONOutputFormats.PRETTY_JS_NATIVE))
          .append(";\n\n" +
                  "const JEF_RSA_OBJECT_1 = ");
        encryptedData =
              JSONObjectWriter.createEncryptionObject(JEF_TEST_STRING.getBytes("UTF-8"),
                                                      DataEncryptionAlgorithms.JOSE_A128CBC_HS256_ALG_ID,
                                                      rsa.getPublic(),
                                                      JEF_RSA_KEY_ID,
                                                      KeyEncryptionAlgorithms.JOSE_RSA_OAEP_256_ALG_ID);
      js.append(encryptedData.serializeToString(JSONOutputFormats.PRETTY_JS_NATIVE))
        .append(";\n\n" +
                  "const JEF_SYM_OBJECT = ");
      encryptedData =
            JSONObjectWriter.createEncryptionObject(JEF_TEST_STRING.getBytes("UTF-8"),
                                                    DataEncryptionAlgorithms.JOSE_A128CBC_HS256_ALG_ID,
                                                    null,
                                                    symkey);
    js.append(encryptedData.serializeToString(JSONOutputFormats.PRETTY_JS_NATIVE))
      .append(";\n\n");
        createPEM(false, "PRIVATE", createPKCS8PrivateKey(alice.getPublic().getEncoded(),
                                                          alice.getPrivate().getEncoded()));
        createPEM(false, "PUBLIC", bob.getPublic().getEncoded());
        createPEM(true, "PRIVATE", rsa.getPrivate().getEncoded());
        ArrayUtil.writeFile(args[0], js.toString().getBytes("UTF-8"));
        System.out.println(js.toString());
    }
}
