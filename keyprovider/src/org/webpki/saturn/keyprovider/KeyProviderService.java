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
package org.webpki.saturn.keyprovider;

import java.io.IOException;
import java.io.InputStream;

import java.math.BigDecimal;

import java.security.PrivateKey;
import java.security.PublicKey;

import java.security.cert.X509Certificate;

import java.util.Vector;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.webpki.crypto.CertificateUtil;
import org.webpki.crypto.CustomCryptoProvider;
import org.webpki.crypto.AsymSignatureAlgorithms;

import org.webpki.json.JSONArrayReader;
import org.webpki.json.JSONDecoderCache;
import org.webpki.json.DataEncryptionAlgorithms;
import org.webpki.json.JSONParser;
import org.webpki.json.KeyEncryptionAlgorithms;

import org.webpki.keygen2.CredentialDiscoveryResponseDecoder;
import org.webpki.keygen2.InvocationResponseDecoder;
import org.webpki.keygen2.KeyCreationResponseDecoder;
import org.webpki.keygen2.ProvisioningFinalizationResponseDecoder;
import org.webpki.keygen2.ProvisioningInitializationResponseDecoder;

import org.webpki.util.ArrayUtil;

import org.webpki.saturn.common.KeyStoreEnumerator;
import org.webpki.saturn.common.PaymentMethods;
import org.webpki.saturn.common.TemporaryCardDBDecoder;

import org.webpki.webutil.InitPropertyReader;

public class KeyProviderService extends InitPropertyReader implements ServletContextListener {

    static Logger logger = Logger.getLogger(KeyProviderService.class.getCanonicalName());
    
    static final String SATURN_LOGO           = "saturn_logotype";

    static final String VERSION_CHECK         = "android_webpki_versions";

    static final String KEYSTORE_PASSWORD     = "key_password";

    static final String PAYER_BANK_HOST       = "payer_bank_host";
    
    static final String KEYPROV_KMK           = "keyprov_kmk";
    
    static final String TLS_CERTIFICATE       = "server_tls_certificate";

    static final String LOGGING               = "logging";

    static final String ACCOUNTS              = "accounts";
    
    static final String BOUNCYCASTLE_FIRST    = "bouncycastle_first";

    static KeyStoreEnumerator keyManagementKey;
    
    static Integer serverPortMapping;

    static JSONDecoderCache keygen2JSONCache;
    
    static X509Certificate serverCertificate;

    static String grantedVersions;
    
    static boolean logging;

    static class PaymentCredential {
        PrivateKey signatureKey;
        AsymSignatureAlgorithms signatureAlgorithm;
        X509Certificate[] dummyCertificatePath;
        String paymentMethod;
        String accountId;
        String cardHolder;
        boolean cardFormatted;
        byte[] optionalServerPin;
        String authorityUrl;
        String svgCardImage;
        PublicKey encryptionKey;
        DataEncryptionAlgorithms dataEncryptionAlgorithm;
        KeyEncryptionAlgorithms keyEncryptionAlgorithm;
        
        BigDecimal tempBalanceFix;
    }

    static Vector<PaymentCredential> paymentCredentials = new Vector<PaymentCredential>();

    static String saturnLogotype;

    InputStream getResource(String name) throws IOException {
        InputStream is = this.getClass().getResourceAsStream(name);
        if (is == null) {
            throw new IOException("Resource fail for: " + name);
        }
        return is;
    }

    String getResourceAsString(String propertyName) throws IOException {
        return new String(ArrayUtil.getByteArrayFromInputStream(getResource(getPropertyString(propertyName))),
                          "UTF-8");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        initProperties (sce);
        try {
            CustomCryptoProvider.forcedLoad(getPropertyBoolean(BOUNCYCASTLE_FIRST));

            ////////////////////////////////////////////////////////////////////////////////////////////
            // KeyGen2
            ////////////////////////////////////////////////////////////////////////////////////////////
            keygen2JSONCache = new JSONDecoderCache();
            keygen2JSONCache.addToCache(InvocationResponseDecoder.class);
            keygen2JSONCache.addToCache(ProvisioningInitializationResponseDecoder.class);
            keygen2JSONCache.addToCache(CredentialDiscoveryResponseDecoder.class);
            keygen2JSONCache.addToCache(KeyCreationResponseDecoder.class);
            keygen2JSONCache.addToCache(ProvisioningFinalizationResponseDecoder.class);

            ////////////////////////////////////////////////////////////////////////////////////////////
            // Credentials
            ////////////////////////////////////////////////////////////////////////////////////////////
            String bankHost = getPropertyString(PAYER_BANK_HOST); // For next iteration...

            for (String accountFile : getPropertyStringList(ACCOUNTS)) {
                JSONArrayReader ar = 
                        JSONParser.parse(ArrayUtil.getByteArrayFromInputStream(getResource(accountFile)))
                                .getJSONArrayReader();
                while (ar.hasMore()) {
                    PaymentCredential paymentCredential = new PaymentCredential();
                    paymentCredentials.add(paymentCredential);
                    TemporaryCardDBDecoder temp = new TemporaryCardDBDecoder(ar.getObject());
                    paymentCredential.authorityUrl = temp.coreCardData.getAuthorityUrl();
                    paymentCredential.optionalServerPin = temp.cardPIN.equals("@") ? 
                                                                              null : temp.cardPIN.getBytes("utf-8");
                    paymentCredential.signatureKey = temp.cardPrivateKey;
                    paymentCredential.signatureAlgorithm = temp.coreCardData.getSignatureAlgorithm();
                    paymentCredential.dummyCertificatePath = new X509Certificate[]{temp.cardDummyCertificate};
                    PaymentMethods.fromTypeUri(paymentCredential.paymentMethod = temp.coreCardData.getPaymentMethod());
                    paymentCredential.accountId = temp.coreCardData.getAccountId();
                    paymentCredential.cardHolder = temp.cardHolder;
                    paymentCredential.cardFormatted = temp.formatAccountAsCard;
                    paymentCredential.svgCardImage = 
                            new String(ArrayUtil.getByteArrayFromInputStream(getResource(temp.logotypeName)), "utf-8");
                    paymentCredential.encryptionKey = temp.coreCardData.getEncryptionKey();
                    paymentCredential.keyEncryptionAlgorithm = temp.coreCardData.getKeyEncryptionAlgorithm();
                    paymentCredential.dataEncryptionAlgorithm = temp.coreCardData.getDataEncryptionAlgorithm();
                    paymentCredential.tempBalanceFix = temp.coreCardData.getTempBalanceFix();
                }
            }


            ////////////////////////////////////////////////////////////////////////////////////////////
            // SKS key management key
            ////////////////////////////////////////////////////////////////////////////////////////////
            keyManagementKey = new KeyStoreEnumerator(getResource(getPropertyString(KEYPROV_KMK)),
                                                                    getPropertyString(KEYSTORE_PASSWORD));

            ////////////////////////////////////////////////////////////////////////////////////////////
            // Saturn logotype
            ////////////////////////////////////////////////////////////////////////////////////////////
            saturnLogotype = getResourceAsString(SATURN_LOGO);

            ////////////////////////////////////////////////////////////////////////////////////////////
            // Android WebPKI version check (vlow-vhigh)
            ////////////////////////////////////////////////////////////////////////////////////////////
            grantedVersions = getPropertyString(VERSION_CHECK);

            ////////////////////////////////////////////////////////////////////////////////////////////
            // Get TLS server certificate
            ////////////////////////////////////////////////////////////////////////////////////////////
            serverCertificate = CertificateUtil.getCertificateFromBlob(
                    ArrayUtil.readFile(getPropertyString(TLS_CERTIFICATE)));

            ////////////////////////////////////////////////////////////////////////////////////////////
            // Are we logging?
            ////////////////////////////////////////////////////////////////////////////////////////////
            logging = getPropertyBoolean(LOGGING);
            
            logger.info("Saturn KeyProvider-server initiated: " + serverCertificate.getSubjectX500Principal().getName());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "********\n" + e.getMessage() + "\n********", e);
        }
    }
}
