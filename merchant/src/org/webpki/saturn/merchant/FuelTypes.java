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

import java.math.BigDecimal;

import java.util.LinkedHashMap;

// Products sold by the Planet Gas merchant

public class FuelTypes implements ProductEntry {
    
    static final LinkedHashMap<String,ProductEntry> products = new LinkedHashMap<>();
    
    static void init(String name, String[] description, int pricePerLitreX100, String background) {
        products.put(name, new FuelTypes(name, description, pricePerLitreX100, background));
    }

    static {

        init("STD_95E",
             new String[]{"Standard 95E"},
             135, 
             "linear-gradient(to bottom, #3ab6e8 0%,#8fd6e8 35%,#a7dfed 65%,#3ab6e8 100%)");

        init("BIO_DIESEL",
             new String[]{"Bio Diesel"},
             104, 
             "linear-gradient(to bottom, #98e524 0%,#d2ff52 40%,#d2ff52 60%,#98e524 100%)");

        init("NITRO",
             new String[]{"Nitro Racing Fuel"}, 
             628, 
            "linear-gradient(to bottom, #fb9d23 0%,#ffc578 35%,#ffc578 65%,#fb9d23 100%)");
    }
    
    String name;
    String[] description;
    int pricePerLitreX100;
    String background;
        
    FuelTypes(String name, String[] description, int pricePerLitreX100, String background) {
        this.name = name;
        this.description = description;
        this.pricePerLitreX100 = pricePerLitreX100;
        this.background = background;
    }
    
    String getBackground() {
        return background;
    }

    @Override
    public String[] getDescription() {
        return description;
    }

    @Override
    public long getPriceX100() {
        return pricePerLitreX100;
    }
    
    @Override
    public String toString() {
        return name;
    }

    @Override
    public String getOptionalUnit() {
        return "Litre";
    }

    @Override
    public BigDecimal getOptionalSubtotal(BigDecimal quantity) {
        return null;
    }

    @Override
    public BigDecimal getOptionalPrice() {
        return BigDecimal.valueOf(pricePerLitreX100).divide(BigDecimal.valueOf(100l));
    }
}
