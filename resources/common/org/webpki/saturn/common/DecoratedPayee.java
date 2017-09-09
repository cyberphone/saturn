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
package org.webpki.saturn.common;

import java.io.IOException;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;

public class DecoratedPayee extends Payee {

    public DecoratedPayee(String homePage, String commonName, String id) throws IOException {
        super(commonName, id);
        this.homePage = homePage;
    }

    public JSONObjectWriter writeObject(JSONObjectWriter wr) throws IOException {
        wr.setString(HOME_PAGE_JSON, homePage);
        return super.writeObject(wr);
    }

    public JSONObjectWriter writeObject() throws IOException {
        return writeObject(new JSONObjectWriter());
    }

    String homePage;
    public String getHomePage() {
        return homePage;
    }

    public DecoratedPayee(JSONObjectReader rd) throws IOException {
        super(rd);
        homePage = rd.getString(HOME_PAGE_JSON);
     }
}
