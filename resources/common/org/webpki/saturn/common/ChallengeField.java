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

public class ChallengeField implements BaseProperties {

    public static enum TYPE {NUMERIC, ALPHANUMERIC};

    public ChallengeField(String id,
                      TYPE type,
                      int length,
                      String optionalLabel) {
        this.id = id;
        this.type = type;
        this.length = length;
        this.optionalLabel = optionalLabel;
    }
 
    public JSONObjectWriter writeObject() throws IOException {
        JSONObjectWriter wr = new JSONObjectWriter()
            .setString(ID_JSON, id)
            .setString(TYPE_JSON, type.toString())
            .setInt(LENGTH_JSON, length);
        if (optionalLabel != null) {
            wr.setString(LABEL_JSON, optionalLabel);
        }
        return wr;
    }

    TYPE type;
    public TYPE getType() {
        return type;
    }

    int length;
    public int getLength() {
        return length;
    }

    String id;
    public String getId() {
        return id;
    }

    String optionalLabel;
    public String getOptionalLabel() {
        return optionalLabel;
    }

    public ChallengeField(JSONObjectReader rd) throws IOException {
        id = rd.getString(ID_JSON);
        type = TYPE.valueOf(rd.getString(TYPE_JSON));
        length = rd.getInt(LENGTH_JSON);
        optionalLabel = rd.getStringConditional(LABEL_JSON);
    }
}
