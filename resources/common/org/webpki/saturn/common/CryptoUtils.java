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

import org.webpki.crypto.AlgorithmPreferences;
import org.webpki.crypto.AsymSignatureAlgorithms;
import org.webpki.crypto.HashAlgorithms;
import org.webpki.crypto.KeyAlgorithms;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;

public class CryptoUtils {
    
    private CryptoUtils() {}
  
    public static byte[] getJsonHash(JSONObjectWriter request, 
                                     HashAlgorithms hashAlgorithm) {
        return hashAlgorithm.digest(request.serializeToBytes(JSONOutputFormats.CANONICALIZED));
    }

    public static HashAlgorithms getHashAlgorithm(JSONObjectReader rd, String keyWord) {
        return HashAlgorithms.getAlgorithmFromId(rd.getString(keyWord), AlgorithmPreferences.JOSE);
    }

    public static AsymSignatureAlgorithms getSignatureAlgorithm(JSONObjectReader rd,
                                                                String keyWord) {
        return AsymSignatureAlgorithms.getAlgorithmFromId(rd.getString(keyWord), 
                                                          AlgorithmPreferences.JOSE);
    }

    public static KeyAlgorithms getKeyAlgorithm(JSONObjectReader rd, String keyWord) {
        return KeyAlgorithms.getKeyAlgorithmFromId(rd.getString(keyWord), 
                                                   AlgorithmPreferences.JOSE);

    }
}
