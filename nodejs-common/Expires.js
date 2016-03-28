/*
 *  Copyright 2006-2016 WebPKI.org (http://webpki.org).
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
 
'use strict';

// Date expires methods

function update(newTime) {
  var dateTime = new Date();
  dateTime.setTime(newTime);
  return dateTime;
}

const Expires = {


  inSeconds : function(seconds) {
    return update(((new Date().getTime() + 999) / 1000 + seconds) * 1000);
  },

  inMinutes : function(minutes) {
    return update(((new Date().getTime() + 59000) / 60000 + minutes) * 60000);
  },

  inHours : function(hours) {
    return update(((new Date().getTime() + 3540000) / 3600000 + hours) * 3600000);
  },

  inDays : function(days) {
    return update(((new Date().getTime() + 82800000) / 86400000 + days) * 86400000);
  }
};

module.exports = Expires;
