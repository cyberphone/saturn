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

import java.util.GregorianCalendar;
import java.util.LinkedHashMap;

import org.webpki.json.JSONArrayReader;
import org.webpki.json.JSONObjectReader;

import org.webpki.util.ISODateTime;

public class EncryptedMessage implements BaseProperties {
     
    public EncryptedMessage(JSONObjectReader rd) throws IOException {
        this.root = rd;
        requester = rd.getString(REQUESTER_JSON);
        text = rd.getString(TEXT_JSON);
        if (rd.hasProperty(USER_CHALLENGE_ITEMS_JSON)) {
            LinkedHashMap<String,UserChallengeItem> items = new LinkedHashMap<>();
            JSONArrayReader ar = rd.getArray(USER_CHALLENGE_ITEMS_JSON);
             do {
                UserChallengeItem userChallengeItem = new UserChallengeItem(ar.getObject());
                if (items.put(userChallengeItem.getName(), userChallengeItem) != null) {
                    throw new IOException("Duplicate: " + userChallengeItem.getName());
                }
            } while (ar.hasMore());
            optionalUserChallengeItems = items.values().toArray(new UserChallengeItem[0]);
        }
        dateTime = rd.getDateTime(TIME_STAMP_JSON, ISODateTime.COMPLETE);
        rd.checkForUnread();
    }

    GregorianCalendar dateTime;

    JSONObjectReader root;
    public JSONObjectReader getRoot() {
        return root;
    }

    String requester;
    public String getRequester() {
        return requester;
    }

    String text;
    public String getText() {
        return text;
    }

    UserChallengeItem[] optionalUserChallengeItems;
    public UserChallengeItem[] getOptionalUserChallengeItems() {
        return optionalUserChallengeItems;
    }
}
