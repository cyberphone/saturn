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

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;

public class WalletAlertMessage implements BaseProperties {
    
    public WalletAlertMessage(JSONObjectReader rd) throws IOException {
        root = Messages.PAYMENT_CLIENT_ALERT.parseBaseMessage(rd);
        text = rd.getString(TEXT_JSON);
        rd.checkForUnread();
    }

    JSONObjectReader root;
    public JSONObjectReader getRoot() {
        return root;
    }

    String text;
    public String getText() {
        return text;
    }

    public static JSONObjectWriter encode(String text) throws IOException {
        return Messages.PAYMENT_CLIENT_ALERT.createBaseMessage()
            .setString(TEXT_JSON, text);
    }
}
