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
package org.webpki.saturn.merchant;

import java.util.LinkedHashMap;

import org.webpki.crypto.HashAlgorithms;

import org.webpki.saturn.common.AccountDataEncoder;
import org.webpki.saturn.common.ServerAsymKeySigner;

public class PaymentMethodDescriptor {
    
    String localId;
    ServerAsymKeySigner signer;
    byte[] keyHashValue;              // For binding a user authorization to a payee request signature key
    HashAlgorithms keyHashAlgorithm;  // Using this algorithm
    LinkedHashMap<String, AccountDataEncoder> receiverAccounts;
    LinkedHashMap<String, AccountDataEncoder> sourceAccounts;
    
    PaymentMethodDescriptor(String localId,
                            ServerAsymKeySigner signer,
                            HashAlgorithms keyHashAlgorithm,
                            byte[] keyHashValue,
                            LinkedHashMap<String, AccountDataEncoder> receiverAccounts,
                            LinkedHashMap<String, AccountDataEncoder> sourceAccounts) {
        this.localId = localId;
        this.signer = signer;
        this.keyHashAlgorithm = keyHashAlgorithm;
        this.keyHashValue = keyHashValue;
        this.receiverAccounts = receiverAccounts;
        this.sourceAccounts = sourceAccounts;
    }
}
