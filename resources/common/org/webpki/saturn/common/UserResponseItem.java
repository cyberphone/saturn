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

public class UserResponseItem implements BaseProperties {

    public UserResponseItem(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public JSONObjectWriter writeObject() throws IOException {
        return new JSONObjectWriter()
            .setString(NAME_JSON, name)
            .setString(VALUE_JSON, value);
    }

    String name;
    public String getName() {
        return name;
    }

    String value;
    public String getValue() {
        return value;
    }

    public UserResponseItem(JSONObjectReader rd) throws IOException {
        name = rd.getString(NAME_JSON);
        value = rd.getStringConditional(VALUE_JSON);
    }
}
