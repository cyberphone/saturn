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
package se.bankgirot;

import java.io.IOException;

import org.webpki.saturn.common.AccountDataEncoder;

public final class BGAccountDataEncoder extends AccountDataEncoder {

    BGAccountDataEncoder() {
    }
    
    public BGAccountDataEncoder(String bgNumber) throws IOException {
        setInternal(BGAccountDataDecoder.CONTEXT)
            .setString(BGAccountDataDecoder.BG_NUMBER_JSON, bgNumber);
    }

    @Override
    public String getPartialAccountIdentifier(String bgNumber) {
        return bgNumber.substring(0, 2) + '*' + bgNumber.substring(bgNumber.length() - 4);
    }
}
