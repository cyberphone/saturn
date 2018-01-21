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

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;

import org.webpki.saturn.common.Currencies;

public enum FuelTypes implements Serializable {

    STD_95E    ("Standard 95E",      135, "linear-gradient(to bottom, #3ab6e8 0%,#8fd6e8 35%,#a7dfed 65%,#3ab6e8 100%)"), 
    BIO_DIESEL ("Bio Diesel",        104, "linear-gradient(to bottom, #98e524 0%,#d2ff52 40%,#d2ff52 60%,#98e524 100%)"),
    NITRO      ("Nitro Racing Fuel", 628, "linear-gradient(to bottom, #fb9d23 0%,#ffc578 35%,#ffc578 65%,#fb9d23 100%)");

    String commonName;
    int pricePerLitreX100;
    String background;

    FuelTypes (String commonName, int pricePerLitreX100, String background) {
        this.commonName = commonName;
        this.pricePerLitreX100 = pricePerLitreX100;
        this.background = background;
    }
    
    String displayPrice() throws IOException {
        return Currencies.EUR.amountToDisplayString(new BigDecimal(pricePerLitreX100).divide(new BigDecimal(100)), false);
    }
}
