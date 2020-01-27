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

// Saturn "TransactionResponse" object

const JsonUtil = require('webpki.org').JsonUtil;

const BaseProperties = require('./BaseProperties');
const Messages       = require('./Messages');
const Software       = require('./Software');
    
const SOFTWARE_NAME    = "WebPKI.org - Acquirer(js)";
const SOFTWARE_VERSION = "1.00";

function TransactionResponse() {
}

TransactionResponse.encode = function(transactionRequest,
                                      referenceId,
                                      optionalLogData,
                                      signer) {
  return Messages.createBaseMessage(Messages.TRANSACTION_RESPONSE)
    .setString(BaseProperties.REFERENCE_ID_JSON, referenceId)
    .setDynamic((wr) => {
        return optionalLogData == null ? wr : wr.setString(BaseProperties.LOG_DATA_JSON, optionalLogData)
      })
    .setDateTime(BaseProperties.TIME_STAMP_JSON, new Date())
    .setObject(BaseProperties.SOFTWARE_JSON, Software.encode(SOFTWARE_NAME, SOFTWARE_VERSION))
    .setObject(Messages.getLowerCamelCase(Messages.TRANSACTION_REQUEST), transactionRequest.root)
    .setSignature(signer, BaseProperties.AUTHORIZATION_SIGNATURE_JSON);
};

module.exports = TransactionResponse;

