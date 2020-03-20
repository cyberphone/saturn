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

// Holds Saturn pre-defined signatures profiles

public enum SignatureProfiles {
    
    ES256_P256 ("https://webpki.github.io/saturn/v3/signatures#ES256.P-256"),
    RS256_2048 ("https://webpki.github.io/saturn/v3/signatures#RS256.2048");
    
    String id;
    
    SignatureProfiles(String id) {
        this.id = id;
    }
    
    public String getId() {
        return id;
    }

    public static SignatureProfiles getProfileFromString(String id) {
        for (SignatureProfiles signatureProfile : SignatureProfiles.values()) {
            if (signatureProfile.id.equals(id)) {
                return signatureProfile;
            }
        }
        // All signature profiles DO NOT need to be known by everybody
        return null;
    }
}
