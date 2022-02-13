/*
 *  Copyright 2006-2016 WebPKI.org (http://webpki.org).
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.webpki.saturn.clitester;

import java.io.IOException;
import java.io.InputStream;
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
import java.util.LinkedHashMap;
import java.util.Properties;
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
import org.webpki.util.ArrayUtil;
import org.webpki.util.Base64;
import org.webpki.util.Base64URL;
import org.webpki.util.HexaDecimal;
import org.webpki.saturn.common.DecryptionKeyHolder;
import org.webpki.saturn.common.EncryptedData;
import org.webpki.saturn.common.Encryption;
import org.webpki.saturn.common.KeyStoreEnumerator;
import org.webpki.saturn.common.PayerAccountTypes;
import org.webpki.saturn.common.ServerAsymKeySigner;

public class CliTester {
    
    enum STEP {payee2payeebank, payee2payerbank};
    
    static Properties properties = new Properties();
    
    static LinkedHashMap<PayerAccountTypes,Credential> credentials = new LinkedHashMap<PayerAccountTypes,Credential>();
    
    static class PropReader { }
    
    static InputStream getResource(String name) throws IOException {
        InputStream is = new PropReader().getClass().getResourceAsStream(name);
        if (is == null) {
            throw new IOException("Null returned for: " + name);
        }
        return is;
    }
        
    static InputStream getPropertyResource(String name) throws IOException {
        return getResource(properties.getProperty(name));
    }
    
    TwoKeys merchantKey;

    void execute() throws Exception {
        CustomCryptoProvider.forcedLoad(true);
        properties.load(getResource("common.properties"));
        merchantKey = new TwoKeys("merchantkey");
        addCredential(PayerAccountTypes.SUPER_CARD, "super");
        addCredential(PayerAccountTypes.BANK_DIRECT, "bankdir");
        addCredential(PayerAccountTypes.UNUSUAL_CARD, "unusual");
    }
    
     void addCredential(PayerAccountTypes payerAccountTypes, String cardType) throws Exception {
        credentials.put(payerAccountTypes, new Credential(cardType));
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Missing: outputfile");
            System.exit(3);
        }
        try {
            CliTester t = new CliTester();
            t.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
