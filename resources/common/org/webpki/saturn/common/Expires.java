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
package org.webpki.saturn.common;

import java.util.Date;

public class Expires {

    private Expires() {}

    public static Date inSeconds(int seconds) {
        return new Date(((new Date().getTime() + 999) / 1000 + seconds) * 1000);
    }

    public static Date inMinutes(int minutes) {
        return new Date(((new Date().getTime() + 59000) / 60000 + minutes) * 60000);
    }

    public static Date inHours(int hours) {
        return new Date(((new Date().getTime() + 3540000) / 3600000 + hours) * 3600000);
    }

    public static Date inDays(int days) {
        return new Date(((new Date().getTime() + 82800000l) / 86400000l + days) * 86400000l);
    }
}
