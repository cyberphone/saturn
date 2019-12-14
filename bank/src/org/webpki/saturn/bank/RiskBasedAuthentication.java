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
package org.webpki.saturn.bank;

import java.math.BigDecimal;

import java.security.PublicKey;

import java.util.GregorianCalendar;

public class RiskBasedAuthentication {
    String lastIpAddress;
    int consecutiveChallenge;
    String expectedResponse;
    PublicKey payeePublicKey;
    String payeeId;
    BigDecimal amount;
    GregorianCalendar when;

    public RiskBasedAuthentication() {
    }

    public String getPayeeId() {
         return payeeId;
    }

    public PublicKey getPayeePublicKey() {
        return payeePublicKey;
    }

    public void clear() {
        
    }
}
