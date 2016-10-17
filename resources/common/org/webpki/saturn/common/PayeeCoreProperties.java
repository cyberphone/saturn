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
import java.security.PublicKey;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONSignatureDecoder;

public class PayeeCoreProperties implements BaseProperties {
    PublicKey publicKey;
    String commonName;
    String id;
    String issuer;

    public PayeeCoreProperties(JSONObjectReader rd) throws IOException {
        commonName = rd.getObject(PAYEE_JSON).getString(COMMON_NAME_JSON);
        id = rd.getObject(PAYEE_JSON).getString(ID_JSON);
        publicKey = rd.getPublicKey();
        issuer = rd.getString(JSONSignatureDecoder.ISSUER_JSON);
    }

    public String getCommonName() {
        return commonName;
    }

    public String getId() {
        return id;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public String getIssuer() {
        return issuer;
    }
}
