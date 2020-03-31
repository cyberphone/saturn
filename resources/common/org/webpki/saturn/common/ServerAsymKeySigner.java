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

import java.security.KeyPair;

import org.webpki.json.JSONAsymKeySigner;

public class ServerAsymKeySigner extends JSONAsymKeySigner {
    
    private static final long serialVersionUID = 1L;

    public ServerAsymKeySigner(KeyPair keyPair) throws IOException {
        super(keyPair.getPrivate(), keyPair.getPublic(), null);
    }

    public ServerAsymKeySigner(KeyStoreEnumerator kse) throws IOException {
        super(kse.getPrivateKey(), kse.getPublicKey(), null);
    }
}
