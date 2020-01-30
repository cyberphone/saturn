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
package org.webpki.saturn.bank;

import io.interbanking.IBRequest;

import java.io.IOException;
import java.io.InputStream;

import java.sql.Connection;
import java.sql.CallableStatement;

import java.security.GeneralSecurityException;
import java.security.KeyStore;

import java.security.cert.X509Certificate;

import java.security.interfaces.RSAKey;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.GregorianCalendar;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;

import javax.servlet.http.HttpServlet;

import javax.sql.DataSource;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.webpki.crypto.CertificateUtil;
import org.webpki.crypto.CustomCryptoProvider;
import org.webpki.crypto.KeyStoreVerifier;

import org.webpki.json.JSONArrayReader;
import org.webpki.json.JSONCryptoHelper;
import org.webpki.json.JSONDecoderCache;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONParser;
import org.webpki.json.JSONX509Verifier;
import org.webpki.json.JSONDecryptionDecoder;
import org.webpki.json.DataEncryptionAlgorithms;
import org.webpki.json.KeyEncryptionAlgorithms;

import org.webpki.util.ArrayUtil;

import org.webpki.saturn.common.AuthorityObjectManager;
import org.webpki.saturn.common.HostingProvider;
import org.webpki.saturn.common.KeyStoreEnumerator;
import org.webpki.saturn.common.KnownExtensions;
import org.webpki.saturn.common.PayeeCoreProperties;
import org.webpki.saturn.common.PaymentMethods;
import org.webpki.saturn.common.ProviderAuthority;
import org.webpki.saturn.common.ServerX509Signer;
import org.webpki.saturn.common.ServerAsymKeySigner;
import org.webpki.saturn.common.SignatureProfiles;
import org.webpki.saturn.common.ExternalCalls;

import org.webpki.webutil.InitPropertyReader;

public class BankService extends InitPropertyReader implements ServletContextListener {

    static Logger logger = Logger.getLogger(BankService.class.getCanonicalName());
    
    static final String KEYSTORE_PASSWORD     = "key_password";

    static final String BANK_NAME             = "bank_name";
    static final String BANK_EECERT           = "bank_eecert";
    static final String BANK_BASE_URL         = "bank_base_url";
    static final String DECRYPTION_KEY1       = "bank_decryptionkey1";
    static final String DECRYPTION_KEY2       = "bank_decryptionkey2";

    static final String HOSTING_PROVIDER_KEY  = "hosting_provider_key";

    static final String PAYMENT_ROOT          = "payment_root";

    static final String ACQUIRER_ROOT         = "acquirer_root";

    static final String EXTENSIONS            = "provider_extensions";

    static final String USER_ACCOUNT_DB       = "user_account_db";
    
    static final String PAYEE_ACCOUNT_DB      = "payee_account_db";
    
    static final String SERVER_PORT_MAP       = "server_port_map";
    
    static final String BOUNCYCASTLE_FIRST    = "bouncycastle_first";

    static final String PAYER_INTERBANK_URL   = "payer_interbank_url";

    static final String PAYEE_INTERBANK_URL   = "payee_interbank_url";

    static final String LOGGING               = "logging";
    
    static final int PROVIDER_EXPIRATION_TIME = 3600;

    static ArrayList<JSONDecryptionDecoder.DecryptionKeyHolder> decryptionKeys =
            new ArrayList<JSONDecryptionDecoder.DecryptionKeyHolder>();
    
     static LinkedHashMap<String,PayeeCoreProperties> PayeeAccountDb =
            new LinkedHashMap<String,PayeeCoreProperties>();
    
    static String bankCommonName;

    static ServerX509Signer bankKey;
    
    static JSONX509Verifier paymentRoot;
    
    static JSONX509Verifier acquirerRoot;
    
    static X509Certificate[] bankCertificatePath;
    
    static JSONObjectReader optionalProviderExtensions;
    
    static String providerAuthorityUrl;
    
    // Static since we do not have a card or SEPA database and associated URL's
    static String payerInterbankUrl;

    static String payeeInterbankUrl;  //     -"-

    static String serviceUrl;

    static int testReferenceId;
    
    static AuthorityObjectManager authorityObjectManager;
    
    static GregorianCalendar started;
    
    static long successfulTransactions;
    
    static long rejectedTransactions;
    
    static JSONDecoderCache knownPayeeMethods = new JSONDecoderCache();

    static JSONDecoderCache knownAccountTypes = new JSONDecoderCache();
    
    static final JSONCryptoHelper.Options AUTHORIZATION_SIGNATURE_POLICY = 
            new JSONCryptoHelper.Options();
    
    static {
        AUTHORIZATION_SIGNATURE_POLICY.setPublicKeyOption(JSONCryptoHelper.PUBLIC_KEY_OPTIONS.REQUIRED);
        AUTHORIZATION_SIGNATURE_POLICY.setKeyIdOption(JSONCryptoHelper.KEY_ID_OPTIONS.FORBIDDEN);
    }
    
    static DataSource jdbcDataSource;

    static boolean logging;
    
    static ExternalCalls externalCalls;
    
    
    class AccountRestorer implements Runnable {

        @Override
        public void run() {
            while(true) {
                Connection connection = null;
                try {
                    Thread.sleep(300000);
                    connection = jdbcDataSource.getConnection();
                    try (CallableStatement stmt = 
                            connection.prepareCall("{call RestoreAccountsSP(FALSE)}");) {
                        stmt.execute();
                    }
                    connection.close();
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Database problem", e);
                    if (connection != null) {
                        try {
                            connection.close();
                        } catch (Exception e1) {
                        }
                    }
                }
            }
        }
    }

    void initDataBaseEnums(Connection connection) throws Exception {
        DataBaseOperations.initiateStaticTypes(connection);
        connection.close();
    }

    InputStream getResource(String name) throws IOException {
        return this.getClass().getResourceAsStream(getPropertyString(name));
    }

    void addDecryptioKey(KeyStoreEnumerator keyStoreEnumerator, 
                         KeyEncryptionAlgorithms keyEncryptionAlgorithm) {
        decryptionKeys.add(new JSONDecryptionDecoder.DecryptionKeyHolder(
                keyStoreEnumerator.getPublicKey(),
                keyStoreEnumerator.getPrivateKey(),
                keyEncryptionAlgorithm,
                null));
    }

    void addDecryptionKey(String name) throws IOException {
        KeyStoreEnumerator keyStoreEnumerator = 
                new KeyStoreEnumerator(getResource(name),
                                       getPropertyString(KEYSTORE_PASSWORD));
        if (keyStoreEnumerator.getPublicKey() instanceof RSAKey) {
            addDecryptioKey(keyStoreEnumerator, 
                            KeyEncryptionAlgorithms.JOSE_RSA_OAEP_256_ALG_ID);
        } else {
            addDecryptioKey(keyStoreEnumerator, 
                            KeyEncryptionAlgorithms.JOSE_ECDH_ES_ALG_ID);
            addDecryptioKey(keyStoreEnumerator,
                            KeyEncryptionAlgorithms.JOSE_ECDH_ES_A128KW_ALG_ID);
        }
    }

    JSONX509Verifier getRoot(String name) throws IOException, GeneralSecurityException {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load (null, null);
        keyStore.setCertificateEntry("mykey",
                                     CertificateUtil.getCertificateFromBlob(
                                         ArrayUtil.getByteArrayFromInputStream(getResource(name))));        
        return new JSONX509Verifier(new KeyStoreVerifier(keyStore));
    }
    
    static void registerServlet(ServletContextEvent sce,
                               String path,
                               Class<? extends HttpServlet> servlet,
                               String description) {
        final ServletContext servletContext = sce.getServletContext();
        final ServletRegistration.Dynamic dynamic = servletContext.addServlet(description, servlet);
        dynamic.addMapping(path);
        logger.info("Dynamically registered servlet: " + description);
    }

    static void dynamicServlet(ServletContextEvent sce,
                               String extension,
                               Class<? extends HttpServlet> servlet,
                               String description) throws IOException {
        if (optionalProviderExtensions != null && optionalProviderExtensions.hasProperty(extension)) {
            String url = optionalProviderExtensions.getString(extension);
            registerServlet(sce, url.substring(url.lastIndexOf('/')), servlet, description);
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

            knownPayeeMethods.addToCache(org.payments.sepa.SEPAAccountDataDecoder.class);
            knownPayeeMethods.addToCache(se.bankgirot.BGAccountDataDecoder.class);

            knownAccountTypes.addToCache(org.payments.sepa.SEPAAccountDataDecoder.class);

            bankCommonName = getPropertyString(BANK_NAME);

            KeyStoreEnumerator bankcreds =
                    new KeyStoreEnumerator(getResource(BANK_EECERT),
                                                       getPropertyString(KEYSTORE_PASSWORD));
            bankCertificatePath = bankcreds.getCertificatePath();
            bankKey = new ServerX509Signer(bankcreds);
            
            HostingProvider hostingProvider = null;
            String hostingProviderKeyName = getPropertyString(HOSTING_PROVIDER_KEY);
            if (!hostingProviderKeyName.isEmpty()) {
                hostingProvider = 
                    new HostingProvider(
                            "https://hosting.com",
                            new KeyStoreEnumerator(
                                    getResource(HOSTING_PROVIDER_KEY),
                                    getPropertyString(KEYSTORE_PASSWORD)).getPublicKey());
            }

            paymentRoot = getRoot(PAYMENT_ROOT);

            acquirerRoot = getRoot(ACQUIRER_ROOT);
            
            String bankBaseUrl = getPropertyString(BANK_BASE_URL);
            
            if (hostingProvider == null) {
                JSONArrayReader accounts = 
                    JSONParser.parse(ArrayUtil
                        .getByteArrayFromInputStream(getResource(PAYEE_ACCOUNT_DB)))
                            .getJSONArrayReader();
                while (accounts.hasMore()) {
                    PayeeCoreProperties account = 
                            PayeeCoreProperties.init(accounts.getObject(),
                                                     bankBaseUrl + "/payees/",
                                                     knownPayeeMethods);
                    PayeeAccountDb.put(account.getPayeeAuthorityUrl(), account);
                }
            }

            addDecryptionKey(DECRYPTION_KEY1);
            addDecryptionKey(DECRYPTION_KEY2);

            testReferenceId = 10000;
            
            String extensions =
                new String(ArrayUtil.getByteArrayFromInputStream(getResource(EXTENSIONS)), 
                           "UTF-8").trim();

            if (!extensions.isEmpty()) {
                extensions = extensions.replace("${host}", bankBaseUrl);
                optionalProviderExtensions = JSONParser.parse(extensions);
            }

            authorityObjectManager = new AuthorityObjectManager(
                providerAuthorityUrl = bankBaseUrl + "/authority",
                bankBaseUrl,
                serviceUrl = bankBaseUrl + "/service",
                new ProviderAuthority.PaymentMethodDeclarations()
                    .add(new ProviderAuthority.PaymentMethodDeclaration(
                            PaymentMethods.BANK_DIRECT.getPaymentMethodUrl())
                                .add(org.payments.sepa.SEPAAccountDataDecoder.class)
                                .add(se.bankgirot.BGAccountDataDecoder.class))
                    .add(new ProviderAuthority.PaymentMethodDeclaration(
                            PaymentMethods.SUPER_CARD.getPaymentMethodUrl())
                                .add(org.payments.sepa.SEPAAccountDataDecoder.class)),
                optionalProviderExtensions,
                new SignatureProfiles[]{SignatureProfiles.P256_ES256},
                new ProviderAuthority.EncryptionParameter[]{
                    new ProviderAuthority.EncryptionParameter(
                            DataEncryptionAlgorithms.JOSE_A128CBC_HS256_ALG_ID,
                            decryptionKeys.get(0).getKeyEncryptionAlgorithm(), 
                            decryptionKeys.get(0).getPublicKey())},
                hostingProvider,
                bankKey,

                PayeeAccountDb.values(),
                hostingProvider == null ? new ServerAsymKeySigner(bankcreds) : null,

                PROVIDER_EXPIRATION_TIME,
                logging);

            dynamicServlet(sce,
                           KnownExtensions.HYBRID_PAYMENT,
                           HybridPaymentServlet.class,
                           "Hybrid Payment Servlet");
            dynamicServlet(sce,
                           KnownExtensions.REFUND_REQUEST,
                           RefundServlet.class,
                           "Refund Servlet");
            
            externalCalls = 
                    new ExternalCalls(logging,
                                      logger,
                                      getPropertyString(SERVER_PORT_MAP).length () > 0 ?
                                                       getPropertyInt(SERVER_PORT_MAP) : null);
            
            String userDataBase = getPropertyString(USER_ACCOUNT_DB);
            if (!userDataBase.isEmpty()) {
                Context initContext = new InitialContext();
                Context envContext  = (Context)initContext.lookup("java:/comp/env");
                jdbcDataSource = (DataSource)envContext.lookup(userDataBase);
                
                initDataBaseEnums(jdbcDataSource.getConnection());
                
                new Thread(new AccountRestorer()).start();
                
                registerServlet(sce, 
                                "/transactions",
                                TransactionListingServlet.class, 
                                "List transactions");
            }

            payerInterbankUrl = getPropertyString(PAYER_INTERBANK_URL);
            payeeInterbankUrl = getPropertyString(PAYEE_INTERBANK_URL);
            IBRequest.setLogging(logging, logger);

            started = new GregorianCalendar();

            logger.info("Saturn \"" + bankCommonName + "\" server initiated");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "********\n" + e.getMessage() + "\n********", e);
        }
    }
}
