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

import java.util.GregorianCalendar;

import org.webpki.json.JSONObjectWriter;

import org.webpki.util.ISODateTime;

public class NonDirectPaymentEncoder implements BaseProperties {

    JSONObjectWriter root;

    private NonDirectPaymentEncoder() {}
    
    public JSONObjectWriter getWriter() {
        return root;
    }

    private static NonDirectPaymentEncoder common(NonDirectPaymentTypes type) {
        NonDirectPaymentEncoder nonDirectPaymentEncoder = new NonDirectPaymentEncoder();
        nonDirectPaymentEncoder.root = new JSONObjectWriter().setString(TYPE_JSON, type.toString());
        return nonDirectPaymentEncoder;
    }

    public static NonDirectPaymentEncoder reservation(ReservationSubTypes subType,
                                                      GregorianCalendar expires,
                                                      boolean fixed) {
       return common(NonDirectPaymentTypes.RESERVATION)
           .subTypeAttribute(subType)
           .fixedAttribute(fixed)
           .expireAttribute(expires);
    }

    public static NonDirectPaymentEncoder recurring(RecurringPaymentIntervals interval,
                                                    Integer optionalInstallments,
                                                    boolean fixed) {
        if (optionalInstallments == null ^ interval == RecurringPaymentIntervals.UNSPECIFIED) {
            throw new SaturnException("Invalid combination");
        }
        return common(NonDirectPaymentTypes.RECURRING)
            .intervalAttribute(interval)
            .installmentsAttribute(optionalInstallments)
            .fixedAttribute(fixed);
    }

    private NonDirectPaymentEncoder installmentsAttribute(Integer optionalInstallments) {
        if (optionalInstallments != null && optionalInstallments != 0) {
            root.setInt(INSTALLMENTS_JSON, optionalInstallments);
        }
        return this;
    }

    private NonDirectPaymentEncoder subTypeAttribute(ReservationSubTypes subType)
    {
        root.setString(SUB_TYPE_JSON, subType.toString());
        return this;
    }

    private NonDirectPaymentEncoder intervalAttribute(RecurringPaymentIntervals interval)
    {
        root.setString(INTERVAL_JSON, interval.toString());
        return this;
    }

    private NonDirectPaymentEncoder fixedAttribute(boolean fixed) {
        root.setBoolean(FIXED_JSON, fixed);
        return this;
    }

    private NonDirectPaymentEncoder expireAttribute(GregorianCalendar expires) {
        root.setDateTime(EXPIRES_JSON, expires, ISODateTime.UTC_NO_SUBSECONDS);
        return this;
    }
}
