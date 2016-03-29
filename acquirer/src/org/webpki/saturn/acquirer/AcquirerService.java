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
package org.webpki.saturn.acquirer;

import java.io.IOException;
import java.io.InputStream;

import java.security.GeneralSecurityException;
import java.security.KeyStore;

import java.security.interfaces.RSAPublicKey;

import java.util.Vector;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.webpki.crypto.CertificateUtil;
import org.webpki.crypto.CustomCryptoProvider;
import org.webpki.crypto.KeyStoreVerifier;

import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONX509Verifier;

import org.webpki.util.ArrayUtil;

import org.webpki.saturn.common.Authority;
import org.webpki.saturn.common.DecryptionKeyHolder;
import org.webpki.saturn.common.Encryption;
import org.webpki.saturn.common.Expires;
import org.webpki.saturn.common.KeyStoreEnumerator;
import org.webpki.saturn.common.ServerX509Signer;

import org.webpki.webutil.InitPropertyReader;

public class AcquirerService extends InitPropertyReader implements ServletContextListener {

    static Logger logger = Logger.getLogger(AcquirerService.class.getCanonicalName());
    
    static final String KEYSTORE_PASSWORD     = "key_password";

    static final String ACQUIRER_EECERT       = "acquirer_eecert";
    static final String ACQUIRER_HOST         = "acquirer_host";
    static final String DECRYPTION_KEY1       = "acquirer_decryptionkey1";  // PUBLISHED
    static final String DECRYPTION_KEY2       = "acquirer_decryptionkey2";

    static final String MERCHANT_ROOT         = "merchant_root";
    static final String MERCHANT_DN           = "merchant_dn";  // The acquirer's only customer...

    static final String PAYMENT_ROOT          = "payment_root";
    
    static Vector<DecryptionKeyHolder> decryptionKeys = new Vector<DecryptionKeyHolder>();

    static JSONX509Verifier merchantRoot;

    static JSONX509Verifier paymentRoot;

    static ServerX509Signer acquirerKey;
    
    static String merchantDN;
    
    static byte[] publishedAuthorityData;
    
    InputStream getResource(String name) throws IOException {
        return this.getClass().getResourceAsStream(getPropertyString(name));
    }
    
    void addDecryptionKey(String name) throws IOException {
        KeyStoreEnumerator keyStoreEnumerator = new KeyStoreEnumerator(getResource(name),
                                                                       getPropertyString(KEYSTORE_PASSWORD));
        decryptionKeys.add(new DecryptionKeyHolder(keyStoreEnumerator.getPublicKey(),
                                                   keyStoreEnumerator.getPrivateKey(),
                                                   keyStoreEnumerator.getPublicKey() instanceof RSAPublicKey ?
                Encryption.JOSE_RSA_OAEP_256_ALG_ID : Encryption.JOSE_ECDH_ES_ALG_ID));
    }

    JSONX509Verifier getRoot(String name) throws IOException, GeneralSecurityException {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load (null, null);
        keyStore.setCertificateEntry ("mykey",
                                      CertificateUtil.getCertificateFromBlob (
                                           ArrayUtil.getByteArrayFromInputStream (getResource(name))));        
        return new JSONX509Verifier(new KeyStoreVerifier(keyStore));
    }
    
    @Override
    public void contextDestroyed(ServletContextEvent event) {
    }

    @Override
    public void contextInitialized(ServletContextEvent event) {
        initProperties (event);
         try {
            CustomCryptoProvider.forcedLoad (false);

            acquirerKey = new ServerX509Signer(new KeyStoreEnumerator(getResource(ACQUIRER_EECERT),
                                                                      getPropertyString(KEYSTORE_PASSWORD)));
//TODO
/*
            merchantRoot = getRoot(MERCHANT_ROOT);
            merchantDN = getPropertyString(MERCHANT_DN);
*/
            paymentRoot = getRoot(PAYMENT_ROOT);


            addDecryptionKey(DECRYPTION_KEY1);
            addDecryptionKey(DECRYPTION_KEY2);
            
            String aquirerHost = getPropertyString(ACQUIRER_HOST);
            publishedAuthorityData =
                Authority.encode(aquirerHost + "/authority",
                                 aquirerHost + "/transact",
                                 decryptionKeys.get(0).getPublicKey(),
                                 Expires.inDays(365),
                                 acquirerKey).serializeJSONObject(JSONOutputFormats.PRETTY_PRINT);

            logger.info("Web2Native Bridge Acquirer-server initiated");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "********\n" + e.getMessage() + "\n********", e);
        }
    }
}