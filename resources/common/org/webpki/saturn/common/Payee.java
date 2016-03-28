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

public class Payee implements BaseProperties {


    private Payee() {}
    
    public static Payee init(String commonName, String id) throws IOException {
        Payee payee = new Payee();
        payee.commonName = commonName;
        payee.id = id;
        return payee;
    }

    public JSONObjectWriter writeObject() throws IOException {
        return new JSONObjectWriter()
            .setString(COMMON_NAME_JSON, commonName)
            .setString(ID_JSON, id);
    }

    String commonName;
    public String getCommonName() {
        return commonName;
    }

    String id;
    public String getId() {
        return id;
    }

    public Payee(JSONObjectReader rd) throws IOException {
        commonName = rd.getString(COMMON_NAME_JSON);
        id = rd.getString(ID_JSON);
    }
}
