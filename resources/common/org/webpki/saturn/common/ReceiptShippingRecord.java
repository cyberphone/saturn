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

import java.math.BigDecimal;

// Holds a shipping record for the Saturn receipt system

public class ReceiptShippingRecord {

    String[] description;
    BigDecimal amount;
        
    public ReceiptShippingRecord(String[] description, BigDecimal amount) {
        this.description = description;
        this.amount = amount;
    }
    
    public String[] getDescription() {
        return description;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
}
