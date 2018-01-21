/*
 *  Copyright 2015-2018 WebPKI.org (http://webpki.org).
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

import org.webpki.crypto.HashAlgorithms;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONSignatureDecoder;

public class RequestHash implements BaseProperties {
    
    public static final String JOSE_SHA_256_ALG_ID = "S256";              // Well, not really JOSE but "similar" :-)

    public static byte[] getRequestHash(byte[] request) throws IOException {
        return HashAlgorithms.SHA256.digest(request);
    }

    public static byte[] getRequestHash(JSONObjectWriter request) throws IOException {
        return getRequestHash(request.serializeToBytes(JSONOutputFormats.NORMALIZED));
    }

    public static byte[] parse(JSONObjectReader rd) throws IOException {
        rd = rd.getObject(REQUEST_HASH_JSON);
        if (!rd.getString(JSONSignatureDecoder.ALGORITHM_JSON).equals(RequestHash.JOSE_SHA_256_ALG_ID)) {
            throw new IOException("Expected algorithm: " + JOSE_SHA_256_ALG_ID);
        }
        return rd.getBinary(JSONSignatureDecoder.VALUE_JSON);
    }
}
