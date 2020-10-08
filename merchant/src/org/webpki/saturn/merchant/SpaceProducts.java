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

// Products currently sold by the Space Shop

public class SpaceProducts implements ProductEntry {
    
    static final LinkedHashMap<String,ProductEntry> products = new LinkedHashMap<>();

    static void init(String name, String[] description, int unitPriceX100, String imageUrl) {
        products.put(name, new SpaceProducts(name, description, unitPriceX100, imageUrl));
    }

    static {

        init("7d688", 
             new String[]{"Model Rocket", "SpaceX Falcon Heavy", "Scale 1:200"}, 
             49999, 
             "spacex-starship-heavy.png"); 

        init("90555", 
             new String[]{"Nasa T-Shirt", "Grey, Size: L"}, 
             1525,  
             "t-shirt-nasa-grey.png"); 
    }

    String name;
    String[] description;
    int unitPriceX100;
    String imageUrl;

    SpaceProducts(String name, String[] description, int unitPriceX100, String imageUrl) {
        this.name = name;
        this.description = description;
        this.unitPriceX100 = unitPriceX100;
        this.imageUrl = imageUrl;
    }

    @Override
    public String[] getDescription() {
        return description;
    }

    @Override
    public long getPriceX100() {
        return unitPriceX100;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public String getOptionalUnit() {
        return null;
    }

    @Override
    public BigDecimal getOptionalSubtotal(BigDecimal quantity) {
        return quantity.multiply(BigDecimal.valueOf(unitPriceX100)).divide(BigDecimal.valueOf(100));
    }

    @Override
    public BigDecimal getOptionalPrice() {
        return null;
    }
}
