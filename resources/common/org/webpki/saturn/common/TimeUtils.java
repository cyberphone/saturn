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
package org.webpki.saturn.common;

import java.text.SimpleDateFormat;

import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class TimeUtils {

    private TimeUtils() {}
    
    private static GregorianCalendar upwardTime(long upfactor, int argument, long divisor) {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTimeInMillis((((gregorianCalendar.getTimeInMillis() + upfactor) / divisor) +
                                           argument) * divisor);
        return gregorianCalendar;
    }

    public static GregorianCalendar inSeconds(int seconds) {
        return upwardTime(999, seconds, 1000);
    }

    public static GregorianCalendar inMinutes(int minutes) {
        return upwardTime(59000, minutes, 60000);
    }

    public static GregorianCalendar inHours(int hours) {
        return upwardTime(3540000, hours, 3600000);
    }

    public static GregorianCalendar inDays(int days) {
        return upwardTime(82800000l, days, 86400000l);
    }

    public static String displayUtcTime(GregorianCalendar dateTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(dateTime.getTime());
    }
}
