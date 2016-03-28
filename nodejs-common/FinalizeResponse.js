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

// FinalizeResponse object

const JsonUtil = require('webpki.org').JsonUtil;
const Jcs = require('webpki.org').Jcs;

const BaseProperties = require('./BaseProperties');
const ErrorReturn = require('./ErrorReturn');
const RequestHash = require('./RequestHash');
const Software = require('./Software');
const Messages = require('./Messages');

function FinalizeResponse(rd) {
  Messages.parseBaseMessage(Messages.FINALIZE_RESPONSE, rd);
  if (rd.hasProperty(BaseProperties.ERROR_CODE_JSON)) {
    this.errorReturn = new ErrorReturn(rd);
    return;
  }
  this.requestHash = RequestHash.parse(rd);
  this.referenceId = rd.getString(BaseProperties.REFERENCE_ID_JSON);
  this.timeStamp = rd.getDateTime(BaseProperties.TIME_STAMP_JSON);
  this.software = new Software(rd);
  this.signatureDecoder = rd.getSignature();
  rd.checkForUnread();
}

FinalizeResponse.prototype.success = function() {
  return this.errorReturn == null;
};

FinalizeResponse.prototype.getErrorReturn = function() {
  return this.errorReturn;
};

FinalizeResponse.prototype.getRequestHash = function() {
  return this.requestHash;
};
  
FinalizeResponse.prototype.getSignatureDecoder = function() {
  return this.signatureDecoder;
};

FinalizeResponse.prototype.getReferenceId = function() {
  return this.referenceId;
};

FinalizeResponse.prototype.getTimeStamp = function() {
  return this.timeStamp;
};

FinalizeResponse.prototype.getSoftware = function() {
  return this.software;
};

FinalizeResponse.SOFTWARE_NAME    = 'WebPKI.org - Bank';
FinalizeResponse.SOFTWARE_VERSION = '1.00';

FinalizeResponse.encode = function(errorReturn_or_finalizeRequest,
                                   referenceId, 
                                   signer) {
  if (errorReturn_or_finalizeRequest instanceof ErrorReturn) {
    return errorReturn_or_finalizeRequest.write(Messages.createBaseMessage(Messages.FINALIZE_RESPONSE));
  }
  return Messages.createBaseMessage(Messages.FINALIZE_RESPONSE)
    .setObject(BaseProperties.REQUEST_HASH_JSON, new JsonUtil.ObjectWriter()
      .setString(Jcs.ALGORITHM_JSON, RequestHash.JOSE_SHA_256_ALG_ID)
      .setBinary(Jcs.VALUE_JSON, RequestHash.getRequestHash(errorReturn_or_finalizeRequest.root)))
    .setString(BaseProperties.REFERENCE_ID_JSON, referenceId)
    .setDateTime(BaseProperties.TIME_STAMP_JSON, new Date())
    .setObject(BaseProperties.SOFTWARE_JSON, 
               Software.encode(FinalizeResponse.SOFTWARE_NAME, FinalizeResponse.SOFTWARE_VERSION))
    .setSignature(signer);
};


module.exports = FinalizeResponse;
