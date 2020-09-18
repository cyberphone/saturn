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

import java.math.BigDecimal;

import java.util.GregorianCalendar;

import org.webpki.json.JSONObjectReader;

import org.webpki.util.ISODateTime;

public class ReceiptDecoder implements BaseProperties {
    
    public enum Status {PENDING, AVAILABLE, FAILED, DELETED}

    public ReceiptDecoder(JSONObjectReader rd) throws IOException {
        Messages.RECEIPT.parseBaseMessage(rd);
        status = Status.valueOf(rd.getString(STATUS_JSON));
        if (status != Status.AVAILABLE) {
            return;
        }
        payeeReferenceId = rd.getString(REFERENCE_ID_JSON);
        payeeTimeStamp = rd.getDateTime(TIME_STAMP_JSON, ISODateTime.COMPLETE);
        payeeCommonName = rd.getString(COMMON_NAME_JSON);
        currency = Currencies.valueOf(rd.getString(CURRENCY_JSON));
        amount = rd.getMoney(AMOUNT_JSON, currency.decimals);
        paymentMethodName = rd.getString(PAYMENT_METHOD_NAME_JSON);
        optionalAccountReference = rd.getStringConditional(ACCOUNT_REFERENCE_JSON);
        payeeAuthorityUrl = rd.getString(PAYEE_AUTHORITY_URL_JSON);
        JSONObjectReader providerData = rd.getObject(PROVIDER_DATA_JSON);
        providerAuthorityUrl = providerData.getString(PROVIDER_AUTHORITY_URL_JSON);
        providerCommonName = providerData.getString(COMMON_NAME_JSON);
        providerTimeStamp = providerData.getDateTime(TIME_STAMP_JSON, ISODateTime.COMPLETE);
        providerReferenceId = providerData.getString(REFERENCE_ID_JSON);
        rd.checkForUnread();
    }
    
    Status status;
    public Status getStatus() {
        return status;
    }

    GregorianCalendar payeeTimeStamp;
    public GregorianCalendar getPayeeTimeStamp() {
        return payeeTimeStamp;
    }

    String payeeAuthorityUrl;
    public String getPayeeAuthorityUrl() {
        return payeeAuthorityUrl;
    }
    
    String providerAuthorityUrl;
    public String getProviderAuthorityUrl() {
        return providerAuthorityUrl;
    }

    String payeeCommonName;
    public String getPayeeCommonName() {
        return payeeCommonName;
    }

    String providerCommonName;
    public String getProviderCommonName() {
        return providerCommonName;
    }

    GregorianCalendar providerTimeStamp;
    public GregorianCalendar getProviderTimeStamp() {
        return providerTimeStamp;
    }

    String optionalAccountReference;
    public String getOptionalAccountReference() {
        return optionalAccountReference;

    }

    String providerReferenceId;
    public String getProviderReferenceId() {
        return providerReferenceId;
    }

    String payeeReferenceId;
    public String getPayeeReferenceId() {
        return payeeReferenceId;
    }

    BigDecimal amount;
    public BigDecimal getAmount() {
        return amount;
    }

    Currencies currency;
    public Currencies getCurrency() {
        return currency;
    }

    String paymentMethodName;
    public String getPaymentMethodName() {
        return paymentMethodName;
    }
}
