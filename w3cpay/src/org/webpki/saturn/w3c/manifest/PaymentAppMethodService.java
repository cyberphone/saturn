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
package org.webpki.saturn.w3c.manifest;

import java.io.IOException;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.webpki.crypto.CertificateUtil;
import org.webpki.crypto.HashAlgorithms;

import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;

import org.webpki.util.ArrayUtil;

import org.webpki.webutil.InitPropertyReader;

public class PaymentAppMethodService extends InitPropertyReader implements ServletContextListener {

    static Logger logger = Logger.getLogger(PaymentAppMethodService.class.getCanonicalName());
    
    static final String SIGNER_CERTIFICATE           = "signer-certificate.cer";
    
    static final String HOST_PATH                    = "host_path";

    static byte[] paymentManifest;
    
    byte[] getBinary(String name) throws IOException {
        return ArrayUtil.getByteArrayFromInputStream(this.getClass().getResourceAsStream(name));
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        initProperties (sce);
        try {
            byte[] certificate = getBinary(SIGNER_CERTIFICATE);

            JSONObjectWriter temp = new JSONObjectWriter();
            temp.setArray("default_applications")
                .setString(getPropertyString(HOST_PATH) + "/payment-manifest.json");
            JSONObjectWriter oneApp = new JSONObjectWriter();
            oneApp.setString("platform", "play")
                  .setString("id", "org.webpki.mobile.android")
                  .setString("min_version", "1")
                  .setArray("fingerprints")
                    .setObject()
                        .setString("type", "sha256_cert")
                        .setString("value",
                                   ArrayUtil.toHexString(HashAlgorithms.SHA256.digest(certificate),
                                                         0,
                                                         -1,
                                                         true,
                                                         ':'));
            temp.setArray("related_applications")
                .setObject(oneApp.setString("url", 
                                            "https://play.google.com/store/apps/details?id=org.webpki.mobile.android"));
            temp.setString("supported_origins", "*");
            paymentManifest = temp.serializeToBytes(JSONOutputFormats.NORMALIZED);
            
            logger.info("W3C/Android Payment App Method initiated\nSubject=" +
                    CertificateUtil.getCertificateFromBlob(certificate).getSubjectDN());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "********\n" + e.getMessage() + "\n********", e);
        }
    }
}
