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

import java.security.GeneralSecurityException;

import java.util.GregorianCalendar;

import org.webpki.json.JSONArrayWriter;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONSymKeyEncrypter;

import org.webpki.crypto.ContentEncryptionAlgorithms;

import org.webpki.util.ISODateTime;

public class ProviderUserResponseEncoder implements BaseProperties {
 
    public static JSONObjectWriter encode(String requester,
                                          String text,
                                          UserChallengeItem[] optionalUserChallengeItems,
                                          byte[] dataEncryptionKey,
                                          ContentEncryptionAlgorithms contentEncryptionAlgorithm)
    throws IOException, GeneralSecurityException {
        JSONObjectWriter wr = new JSONObjectWriter()
            .setString(REQUESTER_JSON, requester)
            .setString(TEXT_JSON, text);
        if (optionalUserChallengeItems != null && optionalUserChallengeItems.length > 0) {
            JSONArrayWriter aw = wr.setArray(USER_CHALLENGE_ITEMS_JSON);
            for (UserChallengeItem UserChallengeItem : optionalUserChallengeItems) {
                aw.setObject(UserChallengeItem.writeObject());
            }
        }
        wr.setDateTime(TIME_STAMP_JSON, new GregorianCalendar(), ISODateTime.UTC_NO_SUBSECONDS);
        return Messages.PROVIDER_USER_RESPONSE.createBaseMessage()
            .setObject(ENCRYPTED_MESSAGE_JSON,
                JSONObjectWriter
                    .createEncryptionObject(wr.serializeToBytes(JSONOutputFormats.NORMALIZED),
                                                                contentEncryptionAlgorithm,
                                                                new JSONSymKeyEncrypter(dataEncryptionKey)));
    }
}
