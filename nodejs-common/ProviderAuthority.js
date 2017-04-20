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

// Saturn "ProviderAuthority" object

const JsonUtil = require('webpki.org').JsonUtil;
const Keys = require('webpki.org').Keys;
const Jef = require('webpki.org').Jef;

const BaseProperties = require('./BaseProperties');
const Messages = require('./Messages');

function ProviderAuthority() {
}

ProviderAuthority.encode = function(authorityUrl,
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
    .setString(BaseProperties.SERVICE_URL_JSON, serviceUrl)
    .setArray(BaseProperties.SIGNATURE_PROFILES_JSON, 
              new JsonUtil.ArrayWriter().setString('http://webpki.org/saturn/v3/signatures#P-256.ES256'))
    .setArray(BaseProperties.ENCRYPTION_PARAMETERS_JSON, 
              new JsonUtil.ArrayWriter().setObject(new JsonUtil.ObjectWriter()
        .setString(BaseProperties.DATA_ENCRYPTION_ALGORITHM_JSON, Jef.JOSE_A128CBC_HS256_ALG_ID)
        .setString(BaseProperties.KEY_ENCRYPTION_ALGORITHM_JSON, 
                        Jef.JOSE_RSA_OAEP_256_ALG_ID : Jef.JOSE_ECDH_ES_ALG_ID)
        .setPublicKey(publicKey)))
    .setDateTime(BaseProperties.TIME_STAMP_JSON, now)
    .setDateTime(BaseProperties.EXPIRES_JSON, expires)
    .setSignature(signer);
};

/*
    public ProviderAuthority(JSONObjectReader rd, String expectedAuthorityUrl) throws IOException {
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

module.exports = ProviderAuthority;
