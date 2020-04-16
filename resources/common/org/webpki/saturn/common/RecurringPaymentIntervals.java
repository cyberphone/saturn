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

import java.util.GregorianCalendar;

public enum RecurringPaymentIntervals {

    WEEKLY      (GregorianCalendar.HOUR,  7 * 24),
    BI_WEEKLY   (GregorianCalendar.HOUR,  14 * 24),     // Every two weeks
    MONTHLY     (GregorianCalendar.MONTH, 1),
    BI_MONTHLY  (GregorianCalendar.MONTH, 2),           // Every two months
    QUARTERLY   (GregorianCalendar.MONTH, 3),           // Every three months
    TRI_ANNUAL  (GregorianCalendar.MONTH, 4),           // Every four months
    SEMI_ANNUAL (GregorianCalendar.MONTH, 6),           // Every six months
    ANNUALY     (GregorianCalendar.YEAR,  1),           // Once a year
    
    UNSPECIFIED (-1, 0);

    int field;
    int quantity;

    RecurringPaymentIntervals(int field, int quantity) {
        this.field = field;
        this.quantity = quantity;
    }
}

