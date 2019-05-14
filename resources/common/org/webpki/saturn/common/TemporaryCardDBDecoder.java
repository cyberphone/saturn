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

import java.security.PrivateKey;
import java.security.PublicKey;

import java.security.cert.X509Certificate;

import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONObjectReader;

// This class holds a temporary card/account database in the waiting for a real one...

public class TemporaryCardDBDecoder {
    
    public static String CARD_PRIVATE_KEY_JSON       = "cardPrivateKey";        // For keyprovider
    public static String CARD_DUMMY_CERTIFICATE_JSON = "cardDummyCertificate";  // For keyprovider
    public static String CARD_PIN_JSON               = "cardPIN";               // For keyprovider
    public static String CARD_HOLDER_JSON            = "cardHolder";            // For keyprovider
    public static String LOGOTYPE_NAME_JSON          = "logotypeName";          // For keyprovider (SVG)
    public static String FORMAT_ACCOUNT_AS_CARD_JSON = "formatAccountAsCard";   // For keyprovider (formatting)
    public static String CORE_CARD_DATA_JSON         = "coreCardData";          // SKS + Used everywhere

    public PrivateKey cardPrivateKey;
    public X509Certificate cardDummyCertificate;
    public PublicKey cardPublicKey;
    public String cardPIN;
    public CardDataDecoder coreCardData;
    public String logotypeName;
    public String cardHolder;
    public boolean formatAccountAsCard;

    public TemporaryCardDBDecoder(JSONObjectReader rd) throws IOException {
        coreCardData = new CardDataDecoder(rd.getObject(CORE_CARD_DATA_JSON)
                .serializeToBytes(JSONOutputFormats.NORMALIZED));
        rd.scanAway(CORE_CARD_DATA_JSON);
        cardPrivateKey = rd.getObject(CARD_PRIVATE_KEY_JSON).getKeyPair().getPrivate();
        cardPIN = rd.getString(CARD_PIN_JSON);
        cardDummyCertificate = rd.getArray(CARD_DUMMY_CERTIFICATE_JSON).getCertificatePath()[0];
        cardPublicKey = cardDummyCertificate.getPublicKey();
        cardHolder = rd.getString(CARD_HOLDER_JSON);
        logotypeName = rd.getString(LOGOTYPE_NAME_JSON);
        formatAccountAsCard = rd.getBoolean(FORMAT_ACCOUNT_AS_CARD_JSON);
        rd.checkForUnread();
    }
}
