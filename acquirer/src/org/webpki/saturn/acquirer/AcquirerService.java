/*
 *  Copyright 2015-2020 WebPKI.org (http://webpki.org).
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
package org.webpki.saturn.acquirer;

import io.interbanking.IBRequest;

import java.io.IOException;
import java.io.InputStream;

import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.interfaces.RSAKey;

import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.Vector;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;

import javax.servlet.http.HttpServlet;

import org.webpki.crypto.CertificateUtil;
import org.webpki.crypto.CustomCryptoProvider;
import org.webpki.crypto.KeyStoreVerifier;

import org.webpki.json.JSONArrayReader;
import org.webpki.json.JSONDecryptionDecoder;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONParser;
import org.webpki.json.JSONX509Verifier;
import org.webpki.json.JSONDecoderCache;
import org.webpki.json.DataEncryptionAlgorithms;
import org.webpki.json.KeyEncryptionAlgorithms;

import org.webpki.util.ArrayUtil;

import org.webpki.saturn.common.AuthorityObjectManager;
import org.webpki.saturn.common.KnownExtensions;
import org.webpki.saturn.common.PayeeCoreProperties;
import org.webpki.saturn.common.KeyStoreEnumerator;
import org.webpki.saturn.common.PaymentMethods;
import org.webpki.saturn.common.ProviderAuthority;
import org.webpki.saturn.common.ServerX509Signer;
import org.webpki.saturn.common.ServerAsymKeySigner;
import org.webpki.saturn.common.SignatureProfiles;

import org.webpki.webutil.InitPropertyReader;

public class AcquirerService extends InitPropertyReader implements ServletContextListener {

    static Logger logger = Logger.getLogger(AcquirerService.class.getCanonicalName());
    
    static final String KEYSTORE_PASSWORD     = "key_password";

    static final String ACQUIRER_EECERT       = "acquirer_eecert";
    static final String ACQUIRER_BASE_URL     = "acquirer_base_url";
    static final String DECRYPTION_KEY1       = "acquirer_decryptionkey1";  // PUBLISHED
    static final String DECRYPTION_KEY2       = "acquirer_decryptionkey2";

    static final String PAYMENT_ROOT          = "payment_root";
    
    static final String EXTENSIONS            = "provider_extensions";

    static final String MERCHANT_ACCOUNT_DB   = "merchant_account_db";

    static final String BOUNCYCASTLE_FIRST    = "bouncycastle_first";

    static final String LOGGING               = "logging";

    static final String PAYER_INTERBANK_URL   = "payer_interbank_url";

    static final int PROVIDER_EXPIRATION_TIME = 3600;

    static Vector<JSONDecryptionDecoder.DecryptionKeyHolder> decryptionKeys = 
            new Vector<JSONDecryptionDecoder.DecryptionKeyHolder>();

    static LinkedHashMap<String,PayeeCoreProperties> merchantAccountDb =
            new LinkedHashMap<String,PayeeCoreProperties>();

    static JSONX509Verifier paymentRoot;

    static ServerX509Signer acquirerKey;
    
    static String providerAuthorityUrl;
    
    static String payerInterbankUrl;  // Static since we do not have a card database and associated URL's

    static long transactionCount;
    
    static GregorianCalendar started;
    
    static JSONObjectReader optionalProviderExtensions;
    
    static AuthorityObjectManager authorityObjectManager;
    
    static JSONDecoderCache knownAccountTypes = new JSONDecoderCache();
    
    static boolean logging;

    static JSONDecoderCache knownPayeeMethods = new JSONDecoderCache();;

    
    InputStream getResource(String name) throws IOException {
        return this.getClass().getResourceAsStream(getPropertyString(name));
    }
    
    void addDecryptionKey(String name) throws IOException {
        KeyStoreEnumerator keyStoreEnumerator = new KeyStoreEnumerator(getResource(name),
                                                                       getPropertyString(KEYSTORE_PASSWORD));
        decryptionKeys.add(new JSONDecryptionDecoder.DecryptionKeyHolder(
                                                   keyStoreEnumerator.getPublicKey(),
                                                   keyStoreEnumerator.getPrivateKey(),
                                                   keyStoreEnumerator.getPublicKey() instanceof RSAKey ?
                                                            KeyEncryptionAlgorithms.JOSE_RSA_OAEP_256_ALG_ID : 
                                                            KeyEncryptionAlgorithms.JOSE_ECDH_ES_ALG_ID,
                                                   keyStoreEnumerator.getKeyId()));
    }

    JSONX509Verifier getRoot(String name) throws IOException, GeneralSecurityException {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load (null, null);
        keyStore.setCertificateEntry ("mykey",
                                      CertificateUtil.getCertificateFromBlob (
                                           ArrayUtil.getByteArrayFromInputStream (getResource(name))));        
        return new JSONX509Verifier(new KeyStoreVerifier(keyStore));
    }
    
    static void dynamicServlet(ServletContextEvent sce, 
                               String extension,
                               Class<? extends HttpServlet> servlet,
                               String description) throws IOException {
        if (optionalProviderExtensions != null
                && optionalProviderExtensions.hasProperty(extension)) {
            final ServletContext servletContext = sce.getServletContext();
            final ServletRegistration.Dynamic dynamic = servletContext.addServlet(description, servlet);
            String url = optionalProviderExtensions.getString(extension);
            dynamic.addMapping(url.substring(url.lastIndexOf('/')));
            logger.info("Dynamically registered servlet: " + description);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (authorityObjectManager != null) {
            try {
                authorityObjectManager.interrupt();
                authorityObjectManager.join();
            } catch (InterruptedException e) {
            }
        }
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        initProperties(sce);
        try {
            logging = getPropertyBoolean(LOGGING);

            CustomCryptoProvider.forcedLoad(getPropertyBoolean(BOUNCYCASTLE_FIRST));
            
            knownAccountTypes.addToCache(com.supercard.SupercardAccountDataDecoder.class);

            knownPayeeMethods.addToCache(org.payments.sepa.SEPAPaymentBackendMethodDecoder.class);

            KeyStoreEnumerator acquirercreds = new KeyStoreEnumerator(getResource(ACQUIRER_EECERT),
                                                                      getPropertyString(KEYSTORE_PASSWORD));
            acquirerKey = new ServerX509Signer(acquirercreds);

            paymentRoot = getRoot(PAYMENT_ROOT);

            JSONArrayReader accounts = JSONParser.parse(
                    ArrayUtil.getByteArrayFromInputStream (getResource(MERCHANT_ACCOUNT_DB))
                                                       ).getJSONArrayReader();
            String acquirerBaseUrl = getPropertyString(ACQUIRER_BASE_URL);

            while (accounts.hasMore()) {
                PayeeCoreProperties account = PayeeCoreProperties.init(accounts.getObject(),
                                                                       acquirerBaseUrl + "/payees/",
                                                                       knownPayeeMethods,
                                                                       false);
                merchantAccountDb.put(account.getPayeeAuthorityUrl(), account);
            }

            addDecryptionKey(DECRYPTION_KEY1);
            addDecryptionKey(DECRYPTION_KEY2);

            String extensions =
                    new String(ArrayUtil.getByteArrayFromInputStream(getResource(EXTENSIONS)), "UTF-8").trim();

            if (extensions.length() > 0) {
                extensions = extensions.replace("${host}", acquirerBaseUrl);
                optionalProviderExtensions = JSONParser.parse(extensions);
            }

            authorityObjectManager = new AuthorityObjectManager(
                providerAuthorityUrl = acquirerBaseUrl + "/authority",
                acquirerBaseUrl,
                acquirerBaseUrl + "/service",
                new ProviderAuthority.PaymentMethodDeclarations()
                    .add(new ProviderAuthority.PaymentMethodDeclaration(
                                       PaymentMethods.SUPER_CARD.getPaymentMethodUrl())
                                .add(org.payments.sepa.SEPAPaymentBackendMethodDecoder.class)),
                optionalProviderExtensions,
                new SignatureProfiles[]{SignatureProfiles.P256_ES256},
                new ProviderAuthority.EncryptionParameter[]{
                    new ProviderAuthority.EncryptionParameter(
                            DataEncryptionAlgorithms.JOSE_A128CBC_HS256_ALG_ID,
                            decryptionKeys.get(0).getKeyEncryptionAlgorithm(), 
                            decryptionKeys.get(0).getPublicKey())},
                null,
                acquirerKey,

                merchantAccountDb.values(), 
                new ServerAsymKeySigner(acquirercreds),

                PROVIDER_EXPIRATION_TIME,
                logging);

            dynamicServlet(sce,
                           KnownExtensions.REFUND_REQUEST,
                           RefundServlet.class,
                           "Refund Servlet");

            payerInterbankUrl = getPropertyString(PAYER_INTERBANK_URL);
            IBRequest.setLogging(logging, logger);

            started = new GregorianCalendar();

            logger.info("Saturn Acquirer-server initiated");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "********\n" + e.getMessage() + "\n********", e);
        }
    }
}
