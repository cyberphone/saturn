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

import java.util.GregorianCalendar;

import org.webpki.json.JSONObjectReader;

import org.webpki.util.ISODateTime;

public class NonDirectPaymentDecoder implements BaseProperties {

    NonDirectPaymentTypes type;
    NonDirectPaymentTypes getType() {
        return type;
    }

    GregorianCalendar expires;
    public GregorianCalendar getExpiration() {
        nullCheck(expires);
        return expires;
    }

    boolean fixed;
    public boolean isFixedAmount() {
        return fixed;
    }

    ReservationSubTypes subType;
    public ReservationSubTypes getReservationSubType() {
        nullCheck(subType);
        return subType;
    }

    RecurringPaymentIntervals interval;
    public RecurringPaymentIntervals getInterval() {
        nullCheck(interval);
        return interval;
    }
    
    Integer installments;
    public int getInstallments() throws IOException {
        nullCheck(installments);
        return installments;
    }

    private void nullCheck(Object object) {
        if (object == null) {
            throw new UnsupportedOperationException(
                    "Invalid method for this kind of non-direct payment");
        }
    }

    public NonDirectPaymentDecoder (JSONObjectReader rd, GregorianCalendar timeStamp)
    throws IOException {
        switch (type = NonDirectPaymentTypes.valueOf(rd.getString(TYPE_JSON))) {
            case RESERVATION:
                subType = ReservationSubTypes.valueOf(rd.getString(SUB_TYPE_JSON));
                expires = rd.getDateTime(EXPIRES_JSON, ISODateTime.COMPLETE);
                break;
                
            case RECURRING:
                interval = RecurringPaymentIntervals.valueOf(rd.getString(INTERVAL_JSON));
                if (interval != RecurringPaymentIntervals.UNSPECIFIED) {
                    installments = rd.getInt(INSTALLMENTS_JSON);
                    expires = (GregorianCalendar) timeStamp.clone();
                    expires.add(interval.field, interval.quantity * installments);
                }
                break;
        }
        fixed = rd.getBoolean(FIXED_JSON);
    }
}
