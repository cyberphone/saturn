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
 
'use strict';

// Saturn "ProviderAuthority" object

const JsonUtil = require('webpki.org').JsonUtil;
const Keys     = require('webpki.org').Keys;
const Jef      = require('webpki.org').Jef;

const BaseProperties     = require('./BaseProperties');
const Messages           = require('./Messages');
const AccountDataDecoder = require('./AccountDataDecoder');

const SUPERCARD_METHOD  = 'https://supercard.com';  // Only one...
const SIGNATURE_PROFILE = 'https://webpki.github.io/saturn/v3/signatures#ES256.P-256'; // Only one

function ProviderAuthority() {
}

ProviderAuthority.encode = function(providerAuthorityUrl,
                                    homePage,
                                    serviceUrl,
                                    publicKey,
                                    expiresInSeconds,
                                    signer) {
  let now = new Date();
  let expires = new Date();
  expires.setTime(now.getTime() + expiresInSeconds * 1000);
  return Messages.createBaseMessage(Messages.PROVIDER_AUTHORITY)
    .setDynamic((wr) => {
        let ar = wr.setArray(BaseProperties.HTTP_VERSIONS_JSON);
        ar.setString('HTTP/1.1')
          .setString('HTTP/2');
        return wr;
    })
    .setString(BaseProperties.PROVIDER_AUTHORITY_URL_JSON, providerAuthorityUrl)
    .setString(BaseProperties.HOME_PAGE_JSON, homePage)
    .setString(BaseProperties.SERVICE_URL_JSON, serviceUrl)
    .setObject(BaseProperties.SUPPORTED_PAYMENT_METHODS_JSON, new JsonUtil.ObjectWriter()
        .setDynamic((wr) => {
            wr.setArray(SUPERCARD_METHOD).setString(AccountDataDecoder.SEPA_ACCOUNT);
            return wr;
        }))
    .setArray(BaseProperties.SIGNATURE_PROFILES_JSON, 
              new JsonUtil.ArrayWriter().setString(SIGNATURE_PROFILE))
    .setArray(BaseProperties.ENCRYPTION_PARAMETERS_JSON, 
              new JsonUtil.ArrayWriter().setObject(new JsonUtil.ObjectWriter()
        .setString(BaseProperties.DATA_ENCRYPTION_ALGORITHM_JSON, Jef.A128CBC_HS256_ALG_ID)
        .setString(BaseProperties.KEY_ENCRYPTION_ALGORITHM_JSON, 
                          publicKey.jwk.kty == 'RSA' ?
                        Jef.RSA_OAEP_256_ALG_ID : Jef.ECDH_ES_ALG_ID)
        .setPublicKey(publicKey)))
    .setDateTime(BaseProperties.TIME_STAMP_JSON, now)
    .setDateTime(BaseProperties.EXPIRES_JSON, expires)
    .setSignature(signer, BaseProperties.ISSUER_SIGNATURE_JSON);
};

module.exports = ProviderAuthority;
