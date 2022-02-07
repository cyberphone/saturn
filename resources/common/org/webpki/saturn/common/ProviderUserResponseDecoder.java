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

import org.webpki.json.JSONCryptoHelper;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONDecryptionDecoder;
import org.webpki.json.JSONParser;

import org.webpki.crypto.ContentEncryptionAlgorithms;

public class ProviderUserResponseDecoder implements BaseProperties {
 
    public ProviderUserResponseDecoder(JSONObjectReader rd)
            throws IOException, GeneralSecurityException {
        Messages.PROVIDER_USER_RESPONSE.parseBaseMessage(rd);
        encryptedData = rd.getObject(ENCRYPTED_MESSAGE_JSON)
                .getEncryptionObject(new JSONCryptoHelper.Options()
                    .setKeyIdOption(JSONCryptoHelper.KEY_ID_OPTIONS.FORBIDDEN)
                    .setPublicKeyOption(JSONCryptoHelper.PUBLIC_KEY_OPTIONS.PLAIN_ENCRYPTION));
        rd.checkForUnread();
    }

    JSONDecryptionDecoder encryptedData;
    
    public EncryptedMessage getEncryptedMessage(byte[] dataEncryptionKey,
                                                ContentEncryptionAlgorithms contentEncryptionAlgorithm)
    throws IOException, GeneralSecurityException {
        if (encryptedData.getContentEncryptionAlgorithm() != contentEncryptionAlgorithm) {
            throw new IOException("Unexpected data encryption algorithm:" + 
                                  encryptedData.getContentEncryptionAlgorithm().toString());
        }
        return new EncryptedMessage(JSONParser.parse(encryptedData.getDecryptedData(dataEncryptionKey))); 
    }
}
