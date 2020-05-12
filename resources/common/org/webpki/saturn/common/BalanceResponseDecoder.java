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

import org.webpki.json.JSONObjectReader;


public class BalanceResponseDecoder implements BaseProperties {
    
    public BalanceResponseDecoder(JSONObjectReader rd) throws IOException {
        Messages.BALANCE_RESPONSE.parseBaseMessage(rd);
        accountId = rd.getString(ACCOUNT_ID_JSON);
        currency = Currencies.valueOf(rd.getString(CURRENCY_JSON));
        amount = rd.getMoney(AMOUNT_JSON, currency.getDecimals());
        software = new Software(rd);
        rd.checkForUnread();
    }

    String accountId;
    public String getAccountId() {
        return accountId;
    }

    BigDecimal amount;
    public BigDecimal getAmount() {
        return amount;
    }

    Currencies currency;
    public Currencies getCurrency() {
        return currency;
    }

    Software software;
    public Software getSoftware() {
        return software;
    }
}
