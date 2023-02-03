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
import java.io.Serializable;

public enum PaymentMethods implements Serializable {

    SUPER_CARD   (true,  "https://supercard.com",   "SuperCard"), 
    BANK_DIRECT  (false, "https://banknet2.org",    "BankNet2"),
    UNUSUAL_CARD (false, "https://unusualcard.com", "UnusualCard");

    boolean cardPayment;      // True => card processor model, false = > 3 or 4 corner distributed model
    String paymentMethodUrl;  // Method URL
    String commonName;        // What it is usually called
    
    PaymentMethods (boolean cardPayment, String paymentMethodUrl, String commonName) {
        this.cardPayment = cardPayment;
        this.paymentMethodUrl = paymentMethodUrl;
        this.commonName = commonName;
    }

    public boolean isCardPayment() {
        return cardPayment;
    }

    public String getPaymentMethodUrl() {
        return paymentMethodUrl;
    }

    public String getCommonName() {
        return commonName;
    }

    public static PaymentMethods fromTypeUrl(String paymentMethodUrl) throws IOException {
        for (PaymentMethods accountType : PaymentMethods.values()) {
            if (accountType.paymentMethodUrl.equals(paymentMethodUrl)) {
                return accountType;
            }
        }
        throw new IOException("No such payment method: " + paymentMethodUrl);
    }
}
