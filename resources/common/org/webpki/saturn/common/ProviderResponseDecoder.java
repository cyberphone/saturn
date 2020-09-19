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
import org.webpki.json.JSONOutputFormats;

public abstract class ProviderResponseDecoder implements BaseProperties {

    public abstract BigDecimal getAmount();
    
    public final Currencies getCurrency() {
        return getPaymentRequest().currency;
    }
    
    public abstract AuthorizationResponseDecoder getAuthorizationResponse();
    
    public final PaymentRequestDecoder getPaymentRequest() {
        return getAuthorizationResponse().authorizationRequest.paymentRequest;
    }

    public final String getPayeeReferenceId() {
        return getPaymentRequest().referenceId;
    }
    
    public abstract String getProviderReferenceId();

    public final String getPayeeAuthorityUrl() {
        return getAuthorizationResponse().authorizationRequest.payeeAuthorityUrl;
    }

    public final String getPaymentMethodName() {
        return getPaymentMethod().getCommonName();
    }

    public final PaymentMethods getPaymentMethod() {
        return getAuthorizationResponse().authorizationRequest.getPaymentMethod();
    }

    public final String getAccountReference() {
        return getAuthorizationResponse().optionalAccountReference;
    }

    public abstract GregorianCalendar getProviderTimeStamp();
    
    public final GregorianCalendar getPayeeTimeStamp() {
        return getAuthorizationResponse().authorizationRequest.timeStamp;
    }

    public abstract String getPayeeRequestId();

    abstract JSONObjectReader getRoot();
    
    public final String getJsonString() throws IOException {
        return getRoot().serializeToString(JSONOutputFormats.NORMALIZED);
    }

    public final String getPayeeCommonName() {
        return getPaymentRequest().payeeCommonName;
    }
}
