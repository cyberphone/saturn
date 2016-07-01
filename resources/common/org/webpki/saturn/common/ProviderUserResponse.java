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

import java.security.GeneralSecurityException;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;

import org.webpki.json.JSONArrayReader;
import org.webpki.json.JSONArrayWriter;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONEncryption;

public class ProviderUserResponse implements BaseProperties {

    public class PrivateMessage {
        
        
        private PrivateMessage(JSONObjectReader rd) {
            this.root = rd;
        }

        GregorianCalendar dateTime;

        JSONObjectReader root;
        public JSONObjectReader getRoot() {
            return root;
        }

        String commonName;
        public String getCommonName() {
            return commonName;
        }

        String text;
        public String getText() {
            return text;
        }

        ChallengeField[] optionalChallengeFields;
        public ChallengeField[] getOptionalChallengeFields() {
            return optionalChallengeFields;
        }
    }

    public ProviderUserResponse(JSONObjectReader rd) throws IOException {
        Messages.parseBaseMessage(Messages.PROVIDER_USER_RESPONSE, rd);
        encryptedData = JSONEncryption.parse(rd.getObject(PRIVATE_MESSAGE_JSON), true);
        rd.checkForUnread();
    }

    JSONEncryption encryptedData;
    
    public PrivateMessage getPrivateMessage(byte[] dataEncryptionKey,
                                            String dataEncryptionAlgorithm)
    throws IOException, GeneralSecurityException {
        JSONObjectReader rd = encryptedData.getDecryptedData(dataEncryptionKey); 
        PrivateMessage privateMessage = new PrivateMessage(rd);
        privateMessage.commonName = rd.getString(COMMON_NAME_JSON);
        privateMessage.text = rd.getString(TEXT_JSON);
        if (rd.hasProperty(CHALLENGE_FIELDS_JSON)) {
            LinkedHashMap<String,ChallengeField> fields = new LinkedHashMap<String,ChallengeField>();
            JSONArrayReader ar = rd.getArray(CHALLENGE_FIELDS_JSON);
             do {
                ChallengeField challengeField = new ChallengeField(ar.getObject());
                if (fields.put(challengeField.getId(), challengeField) != null) {
                    throw new IOException("Duplicate: " + challengeField.getId());
                }
            } while (ar.hasMore());
             privateMessage.optionalChallengeFields = fields.values().toArray(new ChallengeField[0]);
        }
        privateMessage.dateTime = rd.getDateTime(TIME_STAMP_JSON);
        rd.checkForUnread();
        return privateMessage;
    }

    public static JSONObjectWriter encode(String commonName,
                                          String text,
                                          ChallengeField[] optionalChallengeFields,
                                          byte[] dataEncryptionKey,
                                          String dataEncryptionAlgorithm) throws IOException, GeneralSecurityException {
        JSONObjectWriter wr = new JSONObjectWriter()
            .setString(COMMON_NAME_JSON, commonName)
            .setString(TEXT_JSON, text);
        if (optionalChallengeFields != null && optionalChallengeFields.length > 0) {
            JSONArrayWriter aw = wr.setArray(CHALLENGE_FIELDS_JSON);
            for (ChallengeField challengeField : optionalChallengeFields) {
                aw.setObject(challengeField.writeObject());
            }
        }
        wr.setDateTime(TIME_STAMP_JSON, new Date(), true);
        return Messages.createBaseMessage(Messages.PROVIDER_USER_RESPONSE)
            .setObject(PRIVATE_MESSAGE_JSON, JSONEncryption.encode(wr, dataEncryptionAlgorithm, dataEncryptionKey));
     }
}
