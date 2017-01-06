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

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;

// Holds Saturn payer and payee account data

public class AccountDescriptor implements BaseProperties {
    String typeUri;           // URI
    String id;                // Account ID
    
    public AccountDescriptor(String type, String id) {
        this.typeUri = type;
        this.id = id;
    }

    public AccountDescriptor(JSONObjectReader rd) throws IOException {
        typeUri = rd.getString(TYPE_JSON);
        id = rd.getString(ID_JSON);
    }

    public JSONObjectWriter writeObject() throws IOException {
        return new JSONObjectWriter()
            .setString(TYPE_JSON, typeUri)
            .setString(ID_JSON, id);
    }

    public String getType() {
        return typeUri;
    }

    public String getId() {
        return id;
    }
}
