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

// Holder for server signature keys

const Jsf = require('webpki.org').Jsf;

function ServerCertificateSigner(privateKey, certificatePath) {
  this.privateKey = privateKey;
  this.certificatePath = certificatePath;
}

ServerCertificateSigner.prototype.sign = function(jsonObject, optionalSignatureLabel) {
  let signer = new Jsf.Signer(this.privateKey);
  signer.setCertificatePath(this.certificatePath);
  if (optionalSignatureLabel !== undefined) {
    signer.setSignatureLabel(optionalSignatureLabel);
  }
  return signer.sign(jsonObject);
};

module.exports = ServerCertificateSigner;
