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

// Line item holder for the Saturn receipt system

public class LineItem {

    public enum OptionalElements {SKU, UNIT, SUBTOTAL};

    String optionalSku;
    String description;
    BigDecimal optionalSubtotal;
    BigDecimal quantity;
    String optionalUnit;

    public LineItem(String optionalSku, String description,
            BigDecimal optionalSubtotal, BigDecimal quantity,
            String optionalUnit) {
        this.optionalSku = optionalSku;
        this.description = description;
        this.optionalSubtotal = optionalSubtotal;
        this.quantity = quantity;
        this.optionalUnit = optionalUnit;
    }

    LineItem() { // Used by the decoder

    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public String getDescription() {
        return description;
    }

    public String getOptionalSku() {
        return optionalSku;
    }

    public String getOptionalUnit() {
        return optionalUnit;
    }

    public BigDecimal getOptionalSubtotal() {
        return optionalSubtotal;
    }
}
  
