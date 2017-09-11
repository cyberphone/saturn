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
package org.webpki.saturn.hosting;

import java.io.IOException;
import java.io.InputStream;

import java.security.GeneralSecurityException;
import java.security.KeyStore;

import java.util.GregorianCalendar;
import java.util.TreeMap;
import java.util.SortedMap;
import java.util.Comparator;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.webpki.crypto.CertificateUtil;
import org.webpki.crypto.CustomCryptoProvider;
import org.webpki.crypto.KeyStoreVerifier;

import org.webpki.json.JSONArrayReader;
import org.webpki.json.JSONParser;
import org.webpki.json.JSONX509Verifier;

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

    static final String HOSTING_HOST          = "hosting_host";

    static final String PROVIDER_HOST         = "provider_host";

    static final String MERCHANT_ACCOUNT_DB   = "merchant_account_db";

    static final String BOUNCYCASTLE_FIRST    = "bouncycastle_first";

    static final String LOGGING               = "logging";

    static final int PROVIDER_EXPIRATION_TIME = 3600;

    static SortedMap<String,PayeeCoreProperties> merchantAccountDb =
        new TreeMap<String,PayeeCoreProperties>(new Comparator<String>() {
            @Override
            public int compare(String arg0, String arg1) {
                if (arg0.length() > arg1.length()) {
                    return 1;
                }
                return arg0.compareTo(arg1);
            }});

    static String providerAuthorityUrl;
    
    static String payeeAuthorityBaseUrl;

    static GregorianCalendar started;
    
    static AuthorityObjectManager authorityObjectManager;
    
    static boolean logging;

    
    InputStream getResource(String name) throws IOException {
        return this.getClass().getResourceAsStream(getPropertyString(name));
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
        initProperties(sce);
        try {
            logging = getPropertyBoolean(LOGGING);
             
            CustomCryptoProvider.forcedLoad(getPropertyBoolean(BOUNCYCASTLE_FIRST));

            JSONArrayReader accounts = JSONParser.parse(
                    ArrayUtil.getByteArrayFromInputStream (getResource(MERCHANT_ACCOUNT_DB))
                                                       ).getJSONArrayReader();
            while (accounts.hasMore()) {
                PayeeCoreProperties account = new PayeeCoreProperties(accounts.getObject());
                merchantAccountDb.put(account.getDecoratedPayee().getId(), account);
            }

            authorityObjectManager =
                new AuthorityObjectManager(providerAuthorityUrl = getPropertyString(PROVIDER_HOST) + "/authority",
                                           getPropertyString(HOSTING_HOST),
                                           null,
                                           null,
                                           null,
                                           null,
                                           null,
                                           null,
                                           null,

                                           merchantAccountDb, 
                                           payeeAuthorityBaseUrl = getPropertyString(HOSTING_HOST) + "/payees/",
                                           new ServerAsymKeySigner(new KeyStoreEnumerator(getResource(HOSTING_KEY),
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