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
package org.webpki.saturn.bank;

import java.io.IOException;
import java.io.InputStream;

import java.security.GeneralSecurityException;
import java.security.KeyStore;

import java.security.cert.X509Certificate;

import java.security.interfaces.RSAPublicKey;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;

import org.webpki.crypto.CertificateUtil;
import org.webpki.crypto.CustomCryptoProvider;
import org.webpki.crypto.KeyStoreVerifier;

import org.webpki.json.JSONArrayReader;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONParser;
import org.webpki.json.JSONX509Verifier;

import org.webpki.json.encryption.DecryptionKeyHolder;
import org.webpki.json.encryption.KeyEncryptionAlgorithms;

import org.webpki.util.ArrayUtil;

import org.webpki.saturn.common.AuthorityObjectManager;
import org.webpki.saturn.common.KeyStoreEnumerator;
import org.webpki.saturn.common.KnownExtensions;
import org.webpki.saturn.common.PayeeCoreProperties;
import org.webpki.saturn.common.ServerX509Signer;
import org.webpki.saturn.common.UserAccountEntry;

import org.webpki.webutil.InitPropertyReader;

public class BankService extends InitPropertyReader implements ServletContextListener {

    static Logger logger = Logger.getLogger(BankService.class.getCanonicalName());
    
    static final String KEYSTORE_PASSWORD     = "key_password";

    static final String BANK_NAME             = "bank_name";
    static final String BANK_EECERT           = "bank_eecert";
    static final String BANK_HOST             = "bank_host";
    static final String DECRYPTION_KEY1       = "bank_decryptionkey1";
    static final String DECRYPTION_KEY2       = "bank_decryptionkey2";
    static final String REFERENCE_ID_START    = "bank_reference_id_start";
    
    static final String PAYMENT_ROOT          = "payment_root";

    static final String ACQUIRER_ROOT         = "acquirer_root";

    static final String EXTENSIONS            = "provider_extensions";

    static final String USER_ACCOUNT_DB       = "user_account_db";
    
    static final String MERCHANT_ACCOUNT_DB   = "merchant_account_db";

    static final String SERVER_PORT_MAP       = "server_port_map";
    
    static final String BOUNCYCASTLE_FIRST    = "bouncycastle_first";

    static final String LOGGING               = "logging";
    
    static final int PROVIDER_EXPIRATION_TIME = 3600;

    static Vector<DecryptionKeyHolder> decryptionKeys = new Vector<DecryptionKeyHolder>();
    
    static LinkedHashMap<String,UserAccountEntry> userAccountDb = new LinkedHashMap<String,UserAccountEntry>();
    
    static LinkedHashMap<String,PayeeCoreProperties> merchantAccountDb = new LinkedHashMap<String,PayeeCoreProperties>();
    
    static String bankCommonName;

    static ServerX509Signer bankKey;
    
    static JSONX509Verifier paymentRoot;
    
    static JSONX509Verifier acquirerRoot;
    
    static X509Certificate[] bankCertificatePath;
    
    static JSONObjectReader optionalProviderExtensions;
    
    static Integer serverPortMapping;
    
    static String authorityUrl;
    
    static String serviceUrl;

    static int referenceId;
    
    static AuthorityObjectManager authorityObjectManager;
    
    static boolean logging;

    InputStream getResource(String name) throws IOException {
        return this.getClass().getResourceAsStream(getPropertyString(name));
    }

    void addDecryptionKey(String name) throws IOException {
        KeyStoreEnumerator keyStoreEnumerator = new KeyStoreEnumerator(getResource(name),
                                                                       getPropertyString(KEYSTORE_PASSWORD));
        decryptionKeys.add(new DecryptionKeyHolder(keyStoreEnumerator.getPublicKey(),
                                                   keyStoreEnumerator.getPrivateKey(),
                                                   keyStoreEnumerator.getPublicKey() instanceof RSAPublicKey ?
                                                            KeyEncryptionAlgorithms.JOSE_RSA_OAEP_256_ALG_ID : KeyEncryptionAlgorithms.JOSE_ECDH_ES_ALG_ID));
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
        initProperties (sce);
        try {
            logging = getPropertyBoolean(LOGGING);

            CustomCryptoProvider.forcedLoad(getPropertyBoolean(BOUNCYCASTLE_FIRST));

            if (getPropertyString(SERVER_PORT_MAP).length () > 0) {
                serverPortMapping = getPropertyInt(SERVER_PORT_MAP);
            }

            bankCommonName = getPropertyString(BANK_NAME);

            KeyStoreEnumerator bankcreds = new KeyStoreEnumerator(getResource(BANK_EECERT),
                                                                  getPropertyString(KEYSTORE_PASSWORD));
            bankCertificatePath = bankcreds.getCertificatePath();
            bankKey = new ServerX509Signer(bankcreds);

            paymentRoot = getRoot(PAYMENT_ROOT);

            acquirerRoot = getRoot(ACQUIRER_ROOT);
            
            JSONArrayReader accounts = JSONParser.parse(
                                          ArrayUtil.getByteArrayFromInputStream (getResource(USER_ACCOUNT_DB))
                                                       ).getJSONArrayReader();
            while (accounts.hasMore()) {
                UserAccountEntry account = new UserAccountEntry(accounts.getObject());
                userAccountDb.put(account.getId(), account);
            }

            accounts = JSONParser.parse(
                                   ArrayUtil.getByteArrayFromInputStream (getResource(MERCHANT_ACCOUNT_DB))
                                       ).getJSONArrayReader();
            while (accounts.hasMore()) {
                PayeeCoreProperties account = new PayeeCoreProperties(accounts.getObject());
                merchantAccountDb.put(account.getId(), account);
            }

            addDecryptionKey(DECRYPTION_KEY1);
            addDecryptionKey(DECRYPTION_KEY2);

            referenceId = getPropertyInt(REFERENCE_ID_START);
            
            String bankHost = getPropertyString(BANK_HOST);
            
            String extensions =
                new String(ArrayUtil.getByteArrayFromInputStream(getResource(EXTENSIONS)), "UTF-8").trim();

            if (extensions.length() > 0) {
                extensions = extensions.replace("${host}", bankHost);
                optionalProviderExtensions = JSONParser.parse(extensions);
            }

            authorityObjectManager = 
                new AuthorityObjectManager(authorityUrl = bankHost + "/authority",
                                           serviceUrl = bankHost + "/service",
                                           optionalProviderExtensions,
                                           new String[]{"https://swift.com", "https://ultragiro.se"},
                                           decryptionKeys.get(0).getPublicKey(),

                                           merchantAccountDb,
                                           bankHost + "/payees/",

                                           PROVIDER_EXPIRATION_TIME,
                                           bankKey,

                                           logging);
            if (optionalProviderExtensions != null &&
                optionalProviderExtensions.hasProperty(KnownExtensions.HYBRID_PAYMENT)) {
                final ServletContext servletContext = sce.getServletContext();
                final ServletRegistration.Dynamic dynamic =
                        servletContext.addServlet("Hybrid Payment Servlet", HybridPaymentServlet.class);
                String url = optionalProviderExtensions.getString(KnownExtensions.HYBRID_PAYMENT);
                dynamic.addMapping(url.substring(url.lastIndexOf('/')));
         
                final Map<String, ? extends ServletRegistration> map = servletContext.getServletRegistrations();
                for (String key : map.keySet()) {
                    logger.info("Registered Servlet: " + map.get(key).getName());
                }
            }

            logger.info("Saturn \"" + bankCommonName + "\" server initiated");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "********\n" + e.getMessage() + "\n********", e);
        }
    }
}
