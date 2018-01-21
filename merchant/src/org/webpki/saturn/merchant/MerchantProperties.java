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

public interface MerchantProperties {

    public String WALLET_REQUEST_SESSION_ATTR   = "WALLREQ";
    public String DEBUG_DATA_SESSION_ATTR       = "DBGDATA";
    public String SHOPPING_CART_SESSION_ATTR    = "SHOPCART";
    public String RESULT_DATA_SESSION_ATTR      = "RESDAT";
    public String QR_SESSION_ID_ATTR            = "QRSESS";
    public String GAS_STATION_SESSION_ATTR      = "GASSTAT";
    public String GAS_STATION_RES_SESSION_ATTR  = "GASRESV";

    public String DEBUG_MODE_SESSION_ATTR       = "debug";
    public String REFUND_MODE_SESSION_ATTR      = "refund";
    public String TAP_CONNECT_MODE_SESSION_ATTR = "tapcon";
    
    public String SHOPPING_CART_FORM_ATTR       = "shopcart";

}
