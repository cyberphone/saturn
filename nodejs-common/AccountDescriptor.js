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

//Saturn "AccountDescriptor" object

const JsonUtil = require('webpki.org').JsonUtil;

const BaseProperties = require('./BaseProperties');

const FIELDS = [BaseProperties.FIELD1_JSON, BaseProperties.FIELD2_JSON, BaseProperties.FIELD3_JSON];
  
function AccountDescriptor(type_or_rd, id, optionalFields) {
  if (type_or_rd instanceof JsonUtil.ObjectReader) {
    this.type = type_or_rd.getString(BaseProperties.TYPE_JSON);
    this.id = type_or_rd.getString(BaseProperties.ID_JSON);
    this.optionalFields = [];
    for (var q = 0; q < FIELDS.length; q++) {
      if (type_or_rd.hasProperty(FIELDS[q])) {
        this.optionalFields.push(type_or_rd.getString(FIELDS[q]));
      } else {
        break;
      }
    }
  } else {
    this.type = type_or_rd;
    this.id = id;
    this.optionalFields = optionalFields === undefined ? [] : optionalFields;
    if (optionalFields.length > FIELDS.length) {
      throw new IOException('There can be' +  FIELDS.length + ' fields max');
    }
  }
}

AccountDescriptor.prototype.writeObject = function() {
  var wr = new JsonUtil.ObjectWriter()
    .setString(BaseProperties.TYPE_JSON, this.type)
    .setString(BaseProperties.ID_JSON, this.id);
  for (var q = 0; q < this.optionalFields.length; q++) {
    wr.setString(FIELDS[q], this.optionalFields[q]);
  }
  return wr;
};

AccountDescriptor.prototype.getAccountType = function() {
  return this.type;
};

AccountDescriptor.prototype.getAccountId = function() {
  return this.id;
};


module.exports = AccountDescriptor;
