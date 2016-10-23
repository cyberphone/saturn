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
package org.webpki.saturn.common;

import java.io.IOException;

import java.util.LinkedHashMap;

import java.util.logging.Logger;

import org.webpki.json.JSONOutputFormats;

public class PayeeAuthorityList extends Thread {

    private static final Logger logger = Logger.getLogger(PayeeAuthorityList.class.getCanonicalName());

    LinkedHashMap<String,PayeeCoreProperties> payees;
    ServerX509Signer signer;
    String payeeBaseAuthorityUrl;
    String providerAuthorityUrl;
    int expiryTimeInSeconds;
    long renewCycle;

    LinkedHashMap<String,byte[]> rawAuthorityObjects = new LinkedHashMap<String,byte[]>();

    void update() throws IOException {
        for (String id : payees.keySet()) {
            PayeeCoreProperties payeeCoreProperties = payees.get(id);
            synchronized(this) {
                rawAuthorityObjects.put(id, 
                                        PayeeAuthority.encode(payeeBaseAuthorityUrl + id,
                                                              providerAuthorityUrl,
                                                              new Payee(payeeCoreProperties.getCommonName(), id),
                                                              payeeCoreProperties.getPublicKey(),
                                                              Expires.inSeconds(expiryTimeInSeconds),
                                                              signer).serializeJSONObject(JSONOutputFormats.PRETTY_PRINT));
            }
            logger.info("Updated \"" + Messages.PAYEE_AUTHORITY.toString() + "\" with id:" + id);
        }
    }

    public PayeeAuthorityList(LinkedHashMap<String,PayeeCoreProperties> payees,
                              ServerX509Signer signer,
                              String payeeBaseAuthorityUrl,
                              String providerAuthorityUrl,
                              int expiryTimeInSeconds) throws IOException {
        this.payees = payees;
        this.signer = signer;
        this.payeeBaseAuthorityUrl = payeeBaseAuthorityUrl;
        this.providerAuthorityUrl = providerAuthorityUrl;
        this.expiryTimeInSeconds = expiryTimeInSeconds;
        this.renewCycle = expiryTimeInSeconds * 500;
        update();
        start();
    }

    public synchronized byte[] getAuthorityBlob(String id) {
        return rawAuthorityObjects.get(id);
    }

    @Override
    public void run() {
        while (true) {
            try {
                sleep(renewCycle);
                update();
            } catch (Exception e) {
                break;
            }
        }
    }
}
