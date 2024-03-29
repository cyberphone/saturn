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

import org.webpki.saturn.common.MobileProxyParameters;

import org.webpki.util.ArrayUtil;
import org.webpki.util.IO;

import org.webpki.webutil.InitPropertyReader;

public class PaymentAppMethodService extends InitPropertyReader implements ServletContextListener {

    static Logger logger = Logger.getLogger(PaymentAppMethodService.class.getCanonicalName());
    
    static final String SIGNER_CERTIFICATE           = "signer-certificate.cer";
    
    static final String HOST_PATH                    = "host_path";

    static final String LOGGING                      = "logging";

    static byte[] paymentManifest;

    static boolean logging;
    
    static String linkArgument;
    
    byte[] getBinary(String name) throws IOException {
        return IO.getByteArrayFromInputStream(this.getClass().getResourceAsStream(name));
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        initProperties (sce);
        try {
            logging = getPropertyBoolean(LOGGING);
            
            linkArgument = "<" + getPropertyString(HOST_PATH) + ">" +
                    "; rel=\"payment-method-manifest\"";

            byte[] certificate = getBinary(SIGNER_CERTIFICATE);

            JSONObjectWriter temp = new JSONObjectWriter();
// See https://issues.chromium.org/issues/325593948
            temp.setArray("default_applications").setString(getPropertyString(HOST_PATH));
            JSONObjectWriter oneApp = new JSONObjectWriter();
            oneApp.setString("platform", "play")
                  .setString("id", MobileProxyParameters.ANDROID_PACKAGE_NAME)
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
                                            "https://play.google.com/store/apps/details?id=" +
                                                    MobileProxyParameters.ANDROID_PACKAGE_NAME));
//            temp.setString("supported_origins", "*");
            paymentManifest = temp.serializeToBytes(JSONOutputFormats.NORMALIZED);
            
            logger.info("W3C/Android Payment App Method=" + getPropertyString(HOST_PATH) + ", Subject=" +
                    CertificateUtil.getCertificateFromBlob(certificate).getSubjectX500Principal());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "********\n" + e.getMessage() + "\n********", e);
        }
    }
}
