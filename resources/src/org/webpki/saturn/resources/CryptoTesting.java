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
import java.security.PrivateKey;
import java.security.PublicKey;

import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;

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
import org.webpki.json.JSONParser;

import org.webpki.util.ArrayUtil;
import org.webpki.util.Base64;

/**
 * Create test data for the Node.js encryption implementation
 *
 */
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
        KeyPair bob = getKeyPair(bobKey);
        KeyPair alice = getKeyPair(aliceKey);
        js.append("// ECDH test data\n\n" +
                  "const ECDH_RESULT_WITH_KDF    = '" + ECDH_RESULT_WITH_KDF + "';\n" +
                  "const ECDH_RESULT_WITHOUT_KDF = '" + ECDH_RESULT_WITHOUT_KDF + "';\n\n");
        createPEM("PRIVATE", createPKCS8PrivateKey(alice.getPublic().getEncoded(),
                                                   alice.getPrivate().getEncoded()));
        createPEM("PUBLIC", bob.getPublic().getEncoded());
        ArrayUtil.writeFile(args[0], js.toString().getBytes("UTF-8"));
        System.out.println(js.toString());
    }
}
