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
package org.webpki.saturn.common;

import java.io.IOException;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;

public class Software implements BaseProperties {

    String name;
    String version;
    
    public Software(JSONObjectReader rd) throws IOException {
        rd = rd.getObject(SOFTWARE_JSON);
        name = rd.getString(NAME_JSON);
        version = rd.getString(VERSION_JSON);
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public static JSONObjectWriter encode(JSONObjectWriter wr, String name, String version) throws IOException {
        return wr.setObject(SOFTWARE_JSON, new JSONObjectWriter()
            .setString(NAME_JSON, name)
            .setString(VERSION_JSON, version));
    }
}
