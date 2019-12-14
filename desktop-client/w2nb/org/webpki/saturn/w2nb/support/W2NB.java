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
package org.webpki.saturn.w2nb.support;

import java.io.IOException;

import org.webpki.json.JSONDecoderCache;
import org.webpki.json.JSONObjectWriter;

import org.webpki.saturn.common.BaseProperties;

// For the Web2Native Bridge wallet 

public class W2NB {
    
    private W2NB() {};

    public static final String WINDOW_JSON             = "window";
    public static final String HEIGHT_JSON             = "height";
    public static final String WIDTH_JSON              = "width";
    
    public static final String PAYMENT_CLIENT_IS_READY = "PaymentClientIsReady";   // PaymentClient to payee Web page message
    
    public static JSONObjectWriter createReadyMessage() throws IOException {
        return new JSONObjectWriter()
          .setString(JSONDecoderCache.CONTEXT_JSON, BaseProperties.SATURN_WEB_PAY_CONTEXT_URI)  
          .setString(JSONDecoderCache.QUALIFIER_JSON, PAYMENT_CLIENT_IS_READY);
    }
}
