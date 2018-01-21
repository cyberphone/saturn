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
import java.io.Serializable;

public enum NonDirectPayments implements Serializable {

    GAS_STATION  (),  // Single shot payment up to specified amount
    BOOKING      (),  // Single shot payment up to specified amount
    DEPOSIT      (),  // Single shot payment up to specified amount
    REOCCURRING  (),  // Auto transactions, terminated by a zero-amount request
    OTHER        ();

    NonDirectPayments () {
    }

    public static NonDirectPayments fromType(String type) throws IOException {
        for (NonDirectPayments nonDirectPayment : NonDirectPayments.values()) {
            if (nonDirectPayment.toString().equals(type)) {
                return nonDirectPayment;
            }
        }
        throw new IOException("No such type: " + type);
    }
}
