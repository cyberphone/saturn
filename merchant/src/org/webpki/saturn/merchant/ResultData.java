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

import java.io.Serializable;

import java.math.BigDecimal;

import java.util.GregorianCalendar;

import org.webpki.saturn.common.AuthorizationResponseDecoder;
import org.webpki.saturn.common.Currencies;
import org.webpki.saturn.common.PaymentMethods;
import org.webpki.saturn.common.TransactionResponseDecoder;

public class ResultData implements Serializable {
    
    private static final long serialVersionUID = 1L;

    public BigDecimal amount;

    public Currencies currency;

    public String orderId;

    public String accountReference;

    public PaymentMethods paymentMethod;
    
    public String providerAuthorityUrl;
    
    public String providerReferenceId;
    
    public GregorianCalendar providerTimeStamp;

    public TransactionResponseDecoder.ERROR transactionError;
    
    public AuthorizationResponseDecoder optionalRefund;  // Not exactly how it would look in a real setting.

}
