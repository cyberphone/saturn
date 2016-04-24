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
import java.io.Serializable;

public enum PayerAccountTypes implements Serializable {

    SUPER_CARD   (true,  "https://supercard.com",   "SuperCard"), 
    BANK_DIRECT  (false, "https://bankdirect.net",  "Bank Direct"),
    UNUSUAL_CARD (false, "https://unusualcard.com", "UnusualCard");

    boolean acquirerBased;  // True => card processor model, false = > 3 or 4 corner distributed model
    String typeUri;            // A brand URI
    String commonName;      // What it is usually called
    
    PayerAccountTypes (boolean acquirerBased, String typeUri, String commonName) {
        this.acquirerBased = acquirerBased;
        this.typeUri = typeUri;
        this.commonName = commonName;
    }

    public boolean isAcquirerBased() {
        return acquirerBased;
    }

    public String getTypeUri() {
        return typeUri;
    }

    public String getCommonName() {
        return commonName;
    }

    public static PayerAccountTypes fromTypeUri(String typeUri) throws IOException {
        for (PayerAccountTypes accountType : PayerAccountTypes.values()) {
            if (accountType.typeUri.equals(typeUri)) {
                return accountType;
            }
        }
        throw new IOException("No such account type: " + typeUri);
    }
}
