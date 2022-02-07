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
package org.webpki.saturn.hosting;

import java.io.IOException;
import java.io.InputStream;

import java.security.GeneralSecurityException;
import java.security.KeyStore;

import java.util.GregorianCalendar;
import java.util.LinkedHashMap;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.webpki.crypto.CertificateUtil;
import org.webpki.crypto.CustomCryptoProvider;
import org.webpki.crypto.HashAlgorithms;
import org.webpki.crypto.KeyStoreVerifier;

import org.webpki.json.JSONArrayReader;
import org.webpki.json.JSONParser;
import org.webpki.json.JSONX509Verifier;
import org.webpki.json.JSONDecoderCache;

import org.webpki.util.ArrayUtil;

import org.webpki.saturn.common.AuthorityObjectManager;
import org.webpki.saturn.common.PayeeCoreProperties;
import org.webpki.saturn.common.KeyStoreEnumerator;
import org.webpki.saturn.common.ServerAsymKeySigner;

import org.webpki.webutil.InitPropertyReader;

public class HostingService extends InitPropertyReader implements ServletContextListener {

    static Logger logger = Logger.getLogger(HostingService.class.getCanonicalName());
    
    static final String KEYSTORE_PASSWORD     = "key_password";

    static final String HOSTING_KEY           = "hosting_key";

    static final String HOSTING_BASE_URL      = "hosting_base_url";

    static final String PROVIDER_BASE_URL     = "provider_base_url";

    static final String MERCHANT_ACCOUNT_DB   = "merchant_account_db";

    static final String LOGGING               = "logging";

    static final int PROVIDER_EXPIRATION_TIME = 3600;

    static LinkedHashMap<String,PayeeCoreProperties> merchantAccountDb = new LinkedHashMap<>();

    static String providerAuthorityUrl;
    
    static String payeeAuthorityBaseUrl;

    static GregorianCalendar started;
    
    static AuthorityObjectManager authorityObjectManager;
    
    static boolean logging;

    static JSONDecoderCache knownPayeeMethods = new JSONDecoderCache();

    
    InputStream getResource(String name) throws IOException {
        return this.getClass().getResourceAsStream(getPropertyString(name));
    }
    
    JSONX509Verifier getRoot(String name) throws IOException, GeneralSecurityException {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load (null, null);
        keyStore.setCertificateEntry ("mykey",
                                      CertificateUtil.getCertificateFromBlob (
                                           ArrayUtil.getByteArrayFromInputStream(getResource(name))));        
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
        initProperties(sce);
        CustomCryptoProvider.forcedLoad(false);
        try {
            logging = getPropertyBoolean(LOGGING);
             
            knownPayeeMethods.addToCache(org.payments.sepa.SEPAAccountDataDecoder.class);
            knownPayeeMethods.addToCache(se.bankgirot.BGAccountDataDecoder.class);

            JSONArrayReader accounts = JSONParser.parse(
                    ArrayUtil.getByteArrayFromInputStream (getResource(MERCHANT_ACCOUNT_DB))
                                                       ).getJSONArrayReader();

            while (accounts.hasMore()) {
                PayeeCoreProperties account = 
                        PayeeCoreProperties.init(accounts.getObject(),
                                                 getPropertyString(HOSTING_BASE_URL) + "/payees/",
                                                 HashAlgorithms.SHA256,
                                                 knownPayeeMethods);
                merchantAccountDb.put(account.getPayeeAuthorityUrl(), account);
            }

            authorityObjectManager =
                new AuthorityObjectManager(providerAuthorityUrl = 
                                               getPropertyString(PROVIDER_BASE_URL) + "/authority",
                                           null,
                                           null,
                                           null,
                                           null,
                                           null,
                                           null,
                                           null,
                                           null,
                                           null,
                                           null,

                                           merchantAccountDb.values(), 
                                           new ServerAsymKeySigner(
                                               new KeyStoreEnumerator(
                                                   getResource(HOSTING_KEY),
                                                   getPropertyString(KEYSTORE_PASSWORD))),

                                           PROVIDER_EXPIRATION_TIME,
                                           logging);

      
            started = new GregorianCalendar();

            logger.info("Saturn Hosting-server initiated");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "********\n" + e.getMessage() + "\n********", e);
        }
    }
}
