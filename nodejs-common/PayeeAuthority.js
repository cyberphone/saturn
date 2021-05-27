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

// Saturn "PayeeAuthority" object

const JsonUtil = require('webpki.org').JsonUtil;
const Keys     = require('webpki.org').Keys;
const Jef      = require('webpki.org').Jef;
const Jsf      = require('webpki.org').Jsf;

const BaseProperties = require('./BaseProperties');
const Messages       = require('./Messages');
const RequestHash    = require('./RequestHash');

function PayeeAuthority() {
}

PayeeAuthority.encode = function(payeeAuthorityUrl,
                                 providerAuthorityUrl,
                                 payeeCoreProperties,
                                 now,
                                 expiresInSeconds,
                                 signer) {
  let expires = new Date();
  expires.setTime(now.getTime() + expiresInSeconds * 1000);
  return Messages.createBaseMessage(Messages.PAYEE_AUTHORITY)
    .setString(BaseProperties.PAYEE_AUTHORITY_URL_JSON, payeeAuthorityUrl)
    .setString(BaseProperties.PROVIDER_AUTHORITY_URL_JSON, providerAuthorityUrl)
    .setString(BaseProperties.LOCAL_PAYEE_ID_JSON, payeeCoreProperties[BaseProperties.LOCAL_PAYEE_ID_JSON])
    .setString(BaseProperties.COMMON_NAME_JSON, payeeCoreProperties[BaseProperties.COMMON_NAME_JSON])
    .setString(BaseProperties.HOME_PAGE_JSON, payeeCoreProperties[BaseProperties.HOME_PAGE_JSON])
    .setDynamic((wr) => {
      if (payeeCoreProperties[BaseProperties.HASHED_PAYEE_ACCOUNTS_JSON]) {
        let hashedPayeeAccounts = wr.setObject(BaseProperties.ACCOUNT_VERIFIER_JSON);
        hashedPayeeAccounts.setString(Jsf.ALGORITHM_JSON, RequestHash.SHA_256_ALG_ID);
        let array = hashedPayeeAccounts.setArray(BaseProperties.HASHED_PAYEE_ACCOUNTS_JSON);
        payeeCoreProperties[BaseProperties.HASHED_PAYEE_ACCOUNTS_JSON].forEach((entry) => {
          array.setBinary(entry);
        });
      }
      return wr;
    })
    .setDynamic((wr) => {
        let array = wr.setArray(BaseProperties.SIGNATURE_PARAMETERS_JSON);
        payeeCoreProperties.signatureParameters.forEach((entry) => {
          array.setObject()
                 .setString(Jsf.ALGORITHM_JSON, entry[Jsf.ALGORITHM_JSON])
                 .setPublicKey(entry[Jsf.PUBLIC_KEY_JSON]);
        });
        return wr;
      })
    .setDateTime(BaseProperties.TIME_STAMP_JSON, now)
    .setDateTime(BaseProperties.EXPIRES_JSON, expires)
    .setSignature(signer, BaseProperties.ISSUER_SIGNATURE_JSON);
};

module.exports = PayeeAuthority;
