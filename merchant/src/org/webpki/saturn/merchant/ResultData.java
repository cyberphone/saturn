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
package org.webpki.saturn.merchant;

import java.io.Serializable;

import java.math.BigDecimal;

import org.webpki.saturn.common.AuthorizationResponse;
import org.webpki.saturn.common.Currencies;
import org.webpki.saturn.common.PaymentMethods;
import org.webpki.saturn.common.TransactionResponse.ERROR;

public class ResultData implements Serializable {
    
    private static final long serialVersionUID = 1L;

    public BigDecimal amount;

    public Currencies currency;

    public String referenceId;

    public String accountReference;

    public PaymentMethods paymentMethod;

    public ERROR transactionError;
    
    public AuthorizationResponse optionalRefund;
    
}
