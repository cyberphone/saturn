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

import java.io.IOException;
import java.math.BigDecimal;

import org.webpki.saturn.common.Currencies;

public interface ProductEntry {

    String[] getDescription();
    
    String getOptionalUnit();
    
    long getPriceX100();
    
    BigDecimal getOptionalSubtotal(BigDecimal quantity);
    
    BigDecimal getOptionalPrice();
    
    default String renderPrice(long priceX100) throws IOException {
        return Currencies.EUR.amountToDisplayString(
                new BigDecimal(priceX100).divide(new BigDecimal(100)), false);
    }
    
    default String displayPrice() throws IOException {
        return renderPrice(getPriceX100());
    }
}
