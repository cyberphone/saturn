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
 
'use strict';

// Saturn "PayeeAuthority" object

const JsonUtil = require('webpki.org').JsonUtil;
const Keys     = require('webpki.org').Keys;
const Jef      = require('webpki.org').Jef;
const Jcs      = require('webpki.org').Jcs;

const BaseProperties = require('./BaseProperties');
const Messages       = require('./Messages');

function PayeeAuthority() {
}

PayeeAuthority.encode = function(authorityUrl,
                                 providerAuthorityUrl,
                                 payeeCoreProperties,
                                 now,
                                 expiresInSeconds,
                                 signer) {
  var expires = new Date();
  expires.setTime(now.getTime() + expiresInSeconds * 1000);
  return Messages.createBaseMessage(Messages.PAYEE_AUTHORITY)
    .setString(BaseProperties.AUTHORITY_URL_JSON, authorityUrl)
    .setString(BaseProperties.PROVIDER_AUTHORITY_URL_JSON, providerAuthorityUrl)
    .setString(BaseProperties.COMMON_NAME_JSON, payeeCoreProperties[BaseProperties.COMMON_NAME_JSON])
    .setString(BaseProperties.ID_JSON, payeeCoreProperties[BaseProperties.ID_JSON])
    .setDynamic((wr) => {
        var array = wr.setArray(BaseProperties.SIGNATURE_PARAMETERS_JSON);
        payeeCoreProperties.signatureParameters.forEach((entry) => {
          array.setObject()
                 .setString(Jcs.ALGORITHM_JSON, entry[Jcs.ALGORITHM_JSON])
                 .setPublicKey(entry[Jcs.PUBLIC_KEY_JSON]);
        });
        return wr;
      })
    .setDateTime(BaseProperties.TIME_STAMP_JSON, now)
    .setDateTime(BaseProperties.EXPIRES_JSON, expires)
    .setSignature(signer);
};

/*
    public PayeeAuthority(JSONObjectReader rd, String expectedAuthorityUrl) throws IOException {
        root = Messages.parseBaseMessage(Messages.AUTHORITY, rd);
        authorityUrl = rd.getString(AUTHORITY_URL_JSON);
        if (!authorityUrl.equals(expectedAuthorityUrl)) {
            throw new IOException("\"" + AUTHORITY_URL_JSON + "\" mismatch, read=" + authorityUrl +
                                  " expected=" + expectedAuthorityUrl);
        }
        serviceUrl = rd.getString(TRANSACTION_URL_JSON);
        JSONObjectReader encryptionParameters = rd.getObject(ENCRYPTION_PARAMETERS_JSON);
        dataEncryptionAlgorithm = encryptionParameters.getString(DATA_ENCRYPTION_ALGORITHM_JSON);
        keyEncryptionAlgorithm = encryptionParameters.getString(KEY_ENCRYPTION_ALGORITHM_JSON);
        publicKey = encryptionParameters.getPublicKey(AlgorithmPreferences.JOSE);
        timeStamp = rd.getDateTime(TIME_STAMP_JSON);
        expires = rd.getDateTime(EXPIRES_JSON);
        signatureDecoder = rd.getSignature(AlgorithmPreferences.JOSE);
        signatureDecoder.verify(JSONSignatureTypes.X509_CERTIFICATE);
        rd.checkForUnread();
    }
    
    String authorityUrl;
    public String getAuthorityUrl() {
        return authorityUrl;
    }

    String serviceUrl;
    public String getTransactionUrl() {
        return serviceUrl;
    }

    String dataEncryptionAlgorithm;
    public String getDataEncryptionAlgorithm() {
        return dataEncryptionAlgorithm;
    }

    String keyEncryptionAlgorithm;
    public String getKeyEncryptionAlgorithm() {
        return keyEncryptionAlgorithm;
    }

    PublicKey publicKey;
    public PublicKey getPublicKey() {
        return publicKey;
    }

    GregorianCalendar expires;
    public GregorianCalendar getExpires() {
        return expires;
    }

    GregorianCalendar timeStamp;
     public GregorianCalendar getTimeStamp() {
        return timeStamp;
    }

    JSONSignatureDecoder signatureDecoder;
    public JSONSignatureDecoder getSignatureDecoder() {
        return signatureDecoder;
    }

    JSONObjectReader root;
    public JSONObjectReader getRoot() {
        return root;
    }
*/

module.exports = PayeeAuthority;
