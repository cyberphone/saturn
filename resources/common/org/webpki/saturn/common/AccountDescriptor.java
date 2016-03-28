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

import java.util.Vector;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;

// Holds payer and payee account data

public class AccountDescriptor implements BaseProperties {
    String type;              // URI
    String id;                // Account ID
    String[] optionalFields;  // 0..2.  In case the primary aren't enough...
    
    static final String[] fields = {FIELD1_JSON, FIELD2_JSON, FIELD3_JSON};
    
    public AccountDescriptor(String type, String id, String[] optionalFields) throws IOException {
        this.type = type;
        this.id = id;
        this.optionalFields = optionalFields;
        if (optionalFields.length > 3) {
            throw new IOException("There can be 3 fields max");
        }
    }

    public AccountDescriptor(String type, String id) throws IOException {
        this(type, id, new String[0]);
    }

    public AccountDescriptor(JSONObjectReader rd) throws IOException {
        type = rd.getString(TYPE_JSON);
        id = rd.getString(ID_JSON);
        Vector<String> optionalFields = new Vector<String>();
        for (String field : fields) {
            if (rd.hasProperty(field)) {
                optionalFields.add(rd.getString(field));
            } else {
                break;
            }
        }
        this.optionalFields = optionalFields.toArray(new String[0]);
    }

    public JSONObjectWriter writeObject() throws IOException {
        JSONObjectWriter wr = new JSONObjectWriter()
            .setString(TYPE_JSON, type)
            .setString(ID_JSON, id);
        int q = 0;
        for (String field : optionalFields) {
            wr.setString(fields[q++], field);
        }
        return wr;
    }

    public String getAccountType() {
        return type;
    }

    public String getAccountId() {
        return id;
    }
}
