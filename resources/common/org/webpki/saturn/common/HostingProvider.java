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
import org.webpki.json.JSONObjectWriter;

public class HostingProvider implements BaseProperties {

    public HostingProvider(JSONObjectReader rd) throws IOException {
        entityHomePage = rd.getString(HOME_PAGE_JSON);
        publicKey = rd.getPublicKey();
    }

    public HostingProvider(String entityHomePage, PublicKey publicKey) {
        this.entityHomePage = entityHomePage;
        this.publicKey = publicKey;
    }

    PublicKey publicKey;
    public PublicKey getPublicKey() {
        return publicKey;
    }

    String entityHomePage;
    public String getEntityHomePage() {
        return entityHomePage;
    }

    public JSONObjectWriter writeObject() throws IOException {
        return new JSONObjectWriter()
                       .setString(HOME_PAGE_JSON, entityHomePage)
                       .setPublicKey(publicKey);
    }
}
