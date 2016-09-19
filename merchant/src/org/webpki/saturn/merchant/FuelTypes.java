/*
 *  Copyright 2006-2016 WebPKI.org (http://webpki.org).
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.webpki.saturn.merchant;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;

import org.webpki.saturn.common.Currencies;

public enum FuelTypes implements Serializable {

    STD_95E    ("Standard 95E",      135, "#c8e3ff"), 
    BIO_DIESEL ("Bio Diesel",        104, "#ddfca9"),
    NITRO      ("Nitro Racing Fuel", 628, "#f2ddbf");

    String commonName;
    int pricePerLitreX100;
    String color;

    FuelTypes (String commonName, int pricePerLitreX100, String color) {
        this.commonName = commonName;
        this.pricePerLitreX100 = pricePerLitreX100;
        this.color = color;
    }
    
    String displayPrice() throws IOException {
        return Currencies.USD.amountToDisplayString(new BigDecimal(pricePerLitreX100).divide(new BigDecimal(100)), false);
    }
}
