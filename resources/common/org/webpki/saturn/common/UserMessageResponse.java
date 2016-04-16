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

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;

import org.webpki.json.JSONArrayReader;
import org.webpki.json.JSONArrayWriter;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;

public class UserMessageResponse implements BaseProperties {
    
    public UserMessageResponse(JSONObjectReader rd) throws IOException {
        Messages.parseBaseMessage(Messages.USER_MESSAGE_RESPONSE, rd);
        text = rd.getString(TEXT_JSON);
        if (rd.hasProperty(INPUT_FIELDS_JSON)) {
            LinkedHashMap<String,InputField> fields = new LinkedHashMap<String,InputField>();
            JSONArrayReader ar = rd.getArray(INPUT_FIELDS_JSON);
             do {
                InputField inputField = new InputField(ar.getObject());
                if (fields.put(inputField.getId(), inputField) != null) {
                    throw new IOException("Duplicate: " + inputField.getId());
                }
            } while (ar.hasMore());
            optionalInputFields = fields.values().toArray(new InputField[0]);
        }
        dateTime = rd.getDateTime(TIME_STAMP_JSON);
        rd.checkForUnread();
    }

    Software software;
    
    GregorianCalendar dateTime;

    String text;
    public String getText() {
        return text;
    }

    InputField[] optionalInputFields;
    public InputField[] getOptionalInputFields() {
        return optionalInputFields;
    }

    public static JSONObjectWriter encode(String text,
                                          InputField[] optionalInputFields) throws IOException {
        JSONObjectWriter wr = Messages.createBaseMessage(Messages.USER_MESSAGE_RESPONSE)
            .setString(TEXT_JSON, text);
        if (optionalInputFields != null && optionalInputFields.length > 0) {
            JSONArrayWriter aw = wr.setArray(INPUT_FIELDS_JSON);
            for (InputField inputField : optionalInputFields) {
                aw.setObject(inputField.writeObject());
            }
        }
        return wr.setDateTime(TIME_STAMP_JSON, new Date(), true);
     }
}
