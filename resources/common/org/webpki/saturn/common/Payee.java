/*
 *  Copyright 2015-2019 WebPKI.org (http://webpki.org).
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
package org.webpki.saturn.common;

import java.io.IOException;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;

public class Payee implements BaseProperties {

    public Payee(String commonName, String id) throws IOException {
        this.commonName = commonName;
        this.id = id;
    }

    public JSONObjectWriter writeObject(JSONObjectWriter wr) throws IOException {
        return wr.setString(COMMON_NAME_JSON, commonName)
                 .setString(ID_JSON, id);
    }

    public JSONObjectWriter writeObject() throws IOException {
        return writeObject(new JSONObjectWriter());
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
