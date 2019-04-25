/*
 *  Copyright 2015-2018 WebPKI.org (http://webpki.org).
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

// Credential initiator to the Payment Agent (a.k.a. Wallet) application

package org.webpki.saturn.resources;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.security.KeyPair;
import java.security.PublicKey;

import java.security.interfaces.RSAPublicKey;

import java.util.EnumSet;

import org.webpki.crypto.AlgorithmPreferences;
import org.webpki.crypto.AsymSignatureAlgorithms;
import org.webpki.crypto.CertificateUtil;
import org.webpki.crypto.KeyAlgorithms;

import org.webpki.json.DataEncryptionAlgorithms;
import org.webpki.json.JSONArrayWriter;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONParser;
import org.webpki.json.KeyEncryptionAlgorithms;

import org.webpki.keygen2.KeyGen2URIs;

import org.webpki.saturn.common.BaseProperties;
import org.webpki.saturn.common.CardDataEncoder;
import org.webpki.saturn.common.KeyStoreEnumerator;
import org.webpki.saturn.common.PaymentMethods;

import org.webpki.sks.AppUsage;
import org.webpki.sks.BiometricProtection;
import org.webpki.sks.DeleteProtection;
import org.webpki.sks.EnumeratedKey;
import org.webpki.sks.ExportProtection;
import org.webpki.sks.Extension;
import org.webpki.sks.Grouping;
import org.webpki.sks.InputMethod;
import org.webpki.sks.PassphraseFormat;
import org.webpki.sks.PatternRestriction;
import org.webpki.sks.SKSException;
import org.webpki.sks.SecureKeyStore;
import org.webpki.sks.Device;
import org.webpki.sks.GenKey;
import org.webpki.sks.KeySpecifier;
import org.webpki.sks.PINPol;
import org.webpki.sks.ProvSess;
import org.webpki.sks.SKSReferenceImplementation;

import org.webpki.util.ArrayUtil;

public class InitWallet {

    public static void main(String[] args) throws Exception {
        if (args.length != 11) {
            System.out.println("\nUsage: " +
                               InitWallet.class.getCanonicalName() +
                               "sksFile userDb clientCertFile certFilePassword pin accountType/@ accountId" +
                               " authorityUrl keyEncryptionKey imageFile dataEncrytionAlgorithm");
            System.exit(-3);
        }
  
        // Read importedKey/certificate to be imported
        KeyStoreEnumerator importedKey = new KeyStoreEnumerator(new FileInputStream(args[2]), args[3]);
        boolean rsa_flag = importedKey.getPublicKey() instanceof RSAPublicKey;
        String[] endorsed_algs = rsa_flag ?
                new String[] {AsymSignatureAlgorithms.RSA_SHA256.getAlgorithmId(AlgorithmPreferences.SKS)} 
                                          : 
                new String[] {AsymSignatureAlgorithms.ECDSA_SHA256.getAlgorithmId(AlgorithmPreferences.SKS)};

        // Setup keystore (SKS)
        SKSReferenceImplementation sks = null;
        try {
            sks = (SKSReferenceImplementation) new ObjectInputStream(new FileInputStream(args[0])).readObject();
            System.out.println("SKS found, restoring it");
        } catch (Exception e) {
            sks = new SKSReferenceImplementation();
            System.out.println("SKS not found, creating it");
        }
        Device device = new Device(sks);

        // Check for duplicates
        EnumeratedKey ek = new EnumeratedKey();
        while ((ek = sks.enumerateKeys(ek.getKeyHandle())) != null) {
            if (sks.getKeyAttributes(ek.getKeyHandle()).getCertificatePath()[0].equals(importedKey.getCertificatePath()[0])) {
                throw new IOException("Duplicate entry - importedKey #" + ek.getKeyHandle());
            }
        }

        // Start process by creating a session
        ProvSess sess = new ProvSess(device, 0);
        sess.setInputMethod(InputMethod.ANY);
        PINPol pin_policy = sess.createPINPolicy("PIN", 
                                                 PassphraseFormat.STRING,
                                                 EnumSet.noneOf(PatternRestriction.class),
                                                 Grouping.NONE,
                                                 1 /* min_length */,
                                                 50 /* max_length */,
                                                 (short) 3 /* retry_limit */,
                                                 null /* puk_policy */);

        GenKey surrogateKey = sess.createKey("Key",
                                             SecureKeyStore.ALGORITHM_KEY_ATTEST_1,
                                             null /* server_seed */,
                                             pin_policy, 
                                             args[4] /* PIN value */,
                                             BiometricProtection.NONE /* biometric_protection */,
                                             ExportProtection.NON_EXPORTABLE /* export_policy */,
                                             DeleteProtection.NONE /* delete_policy */,
                                             false /* enable_pin_caching */,
                                             AppUsage.SIGNATURE,
                                             "" /* friendly_name */, 
                                             new KeySpecifier(KeyAlgorithms.NIST_P_256), endorsed_algs);

        surrogateKey.setCertificatePath(importedKey.getCertificatePath());
        surrogateKey.setPrivateKey(new KeyPair(importedKey.getPublicKey(), importedKey.getPrivateKey()));
        JSONObjectWriter ow = null;
        if (!args[5].equals("@")) {
            PaymentMethods paymentMethod = PaymentMethods.valueOf(args[5]);
            String accountId = args[6];
            String authorityUrl = args[7];
            DataEncryptionAlgorithms dataEncryptionAlgorithm = DataEncryptionAlgorithms.getAlgorithmFromId(args[10]);
            PublicKey encryptionKey = CertificateUtil.getCertificateFromBlob(ArrayUtil.readFile(args[8])).getPublicKey();
            KeyEncryptionAlgorithms keyEncryptionAlgorithm = encryptionKey instanceof RSAPublicKey ?
                KeyEncryptionAlgorithms.JOSE_RSA_OAEP_256_ALG_ID 
                    : 
                KeyEncryptionAlgorithms.JOSE_ECDH_ES_ALG_ID;
            if (accountId.startsWith("!")) {
                accountId = accountId.substring(1);
            }
            ow = CardDataEncoder.encode(paymentMethod.getPaymentMethodUri(), 
                                        accountId, 
                                        authorityUrl,
                                        rsa_flag ?
                                             AsymSignatureAlgorithms.RSA_SHA256
                                                               :
                                             AsymSignatureAlgorithms.ECDSA_SHA256,
                                        dataEncryptionAlgorithm, 
                                        keyEncryptionAlgorithm, 
                                        encryptionKey, 
                                        null,
                                        null);
            surrogateKey.addExtension(BaseProperties.SATURN_WEB_PAY_CONTEXT_URI,
                                      SecureKeyStore.SUB_TYPE_EXTENSION,
                                      "",
                                      ow.serializeToBytes(JSONOutputFormats.NORMALIZED));
            surrogateKey.addExtension(KeyGen2URIs.LOGOTYPES.CARD,
                                      SecureKeyStore.SUB_TYPE_LOGOTYPE,
                                      "image/png",
                                      ArrayUtil.readFile(args[9]));
        }
        sess.closeSession();
        
        // Serialize the updated SKS
        ObjectOutputStream oos = new ObjectOutputStream (new FileOutputStream(args[0]));
        oos.writeObject (sks);
        oos.close ();

        // Write the database
        JSONArrayWriter aw = new JSONArrayWriter();
        ek = new EnumeratedKey();
        while ((ek = sks.enumerateKeys(ek.getKeyHandle())) != null) {
            Extension ext = null;
            try {
                ext = sks.getExtension(ek.getKeyHandle(),
                                       BaseProperties.SATURN_WEB_PAY_CONTEXT_URI);
            } catch (SKSException e) {
                if (e.getError() == SKSException.ERROR_OPTION) {
                    continue;
                }
                throw new Exception(e);
            }
            JSONObjectReader rd = JSONParser.parse(ext.getExtensionData(SecureKeyStore.SUB_TYPE_EXTENSION));
            String url = rd.getString(BaseProperties.PROVIDER_AUTHORITY_URL_JSON);
            url = url.substring(0, url.lastIndexOf('/'));  // Remove "/authority"
            if (args[1].substring(args[1].lastIndexOf(File.separator) + 7)
                    .startsWith(url.substring(url.lastIndexOf('/') + 1))) {
                aw.setObject(new JSONObjectWriter(rd).setPublicKey(
                        sks.getKeyAttributes(ek.getProvisioningHandle()).getCertificatePath()[0].getPublicKey()));
            }
        }
        ArrayUtil.writeFile(args[1], aw.serializeToBytes(JSONOutputFormats.PRETTY_PRINT));

        // Report
        System.out.println("Imported Subject: " +
                importedKey.getCertificatePath()[0].getSubjectX500Principal().getName() +
                "\nID=#" + surrogateKey.keyHandle + ", " + (rsa_flag ? "RSA" : "EC") +
                (ow == null ? ", Not a card" : ", Card=\n" + ow));
    }
}
