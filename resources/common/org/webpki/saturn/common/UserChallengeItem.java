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

public class UserChallengeItem implements BaseProperties {

    public static enum TYPE {

        NUMERIC(true),
        ALPHANUMERIC(true),
        NUMERIC_SECRET(true),
        ALPHANUMERIC_SECRET(true),
        GPS_COORDINATES(false),
        SMS_CALLBACK(true);
        
        boolean needsLength;

        TYPE(boolean needsLength) {
            this.needsLength = needsLength;                   
        }
    }

    private void testLength() throws IOException {
        if (type.needsLength ^ (optionalLength != null)) {
            throw new IOException("Incorrect use of \"" + LENGTH_JSON + "\".");
        }
    }

    public UserChallengeItem(String id,
                             TYPE type,
                             Integer optionalLength,
                             String optionalLabel) {
        this.id = id;
        this.type = type;
        this.optionalLength = optionalLength;
        this.optionalLabel = optionalLabel;
    }
 
    public JSONObjectWriter writeObject() throws IOException {
        testLength();
        return new JSONObjectWriter()
            .setString(ID_JSON, id)
            .setString(TYPE_JSON, type.toString())
            .setDynamic((wr) -> optionalLength == null ? wr : wr.setInt(LENGTH_JSON, optionalLength))
            .setDynamic((wr) -> optionalLabel == null ? wr : wr.setString(LABEL_JSON, optionalLabel));
    }

    TYPE type;
    public TYPE getType() {
        return type;
    }

    Integer optionalLength;
    public Integer getOptionalLength() {
        return optionalLength;
    }

    String id;
    public String getId() {
        return id;
    }

    String optionalLabel;
    public String getOptionalLabel() {
        return optionalLabel;
    }

    public UserChallengeItem(JSONObjectReader rd) throws IOException {
        id = rd.getString(ID_JSON);
        type = TYPE.valueOf(rd.getString(TYPE_JSON));
        if (rd.hasProperty(LENGTH_JSON)) {
            optionalLength = rd.getInt(LENGTH_JSON);
        }
        optionalLabel = rd.getStringConditional(LABEL_JSON);
        testLength();
    }
}
