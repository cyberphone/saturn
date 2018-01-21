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
 
'use strict';

// Saturn "ProviderAuthority" object

const JsonUtil = require('webpki.org').JsonUtil;
const Keys     = require('webpki.org').Keys;
const Jef      = require('webpki.org').Jef;

const BaseProperties = require('./BaseProperties');
const Messages       = require('./Messages');

const PAYMENT_METHOD    = 'https://supercard.com';  // Only one...
const SIGNATURE_PROFILE = 'http://webpki.org/saturn/v3/signatures#P-256.ES256'; // Only one

function ProviderAuthority() {
}

ProviderAuthority.encode = function(authorityUrl,
                                    homePage,
                                    serviceUrl,
                                    publicKey,
                                    expiresInSeconds,
                                    signer) {
  var now = new Date();
  var expires = new Date();
  expires.setTime(now.getTime() + expiresInSeconds * 1000);
  return Messages.createBaseMessage(Messages.PROVIDER_AUTHORITY)
    .setString(BaseProperties.HTTP_VERSION_JSON, "HTTP/1.1")
    .setString(BaseProperties.AUTHORITY_URL_JSON, authorityUrl)
    .setString(BaseProperties.HOME_PAGE_JSON, homePage)
    .setString(BaseProperties.SERVICE_URL_JSON, serviceUrl)
    .setArray(BaseProperties.PAYMENT_METHODS_JSON, 
              new JsonUtil.ArrayWriter().setString(PAYMENT_METHOD))
    .setArray(BaseProperties.SIGNATURE_PROFILES_JSON, 
              new JsonUtil.ArrayWriter().setString(SIGNATURE_PROFILE))
    .setArray(BaseProperties.ENCRYPTION_PARAMETERS_JSON, 
              new JsonUtil.ArrayWriter().setObject(new JsonUtil.ObjectWriter()
        .setString(BaseProperties.DATA_ENCRYPTION_ALGORITHM_JSON, Jef.JOSE_A128CBC_HS256_ALG_ID)
        .setString(BaseProperties.KEY_ENCRYPTION_ALGORITHM_JSON, 
                          publicKey.jwk.kty == 'RSA' ?
                        Jef.JOSE_RSA_OAEP_256_ALG_ID : Jef.JOSE_ECDH_ES_ALG_ID)
        .setPublicKey(publicKey)))
    .setDateTime(BaseProperties.TIME_STAMP_JSON, now)
    .setDateTime(BaseProperties.EXPIRES_JSON, expires)
    .setSignature(signer);
};

module.exports = ProviderAuthority;
