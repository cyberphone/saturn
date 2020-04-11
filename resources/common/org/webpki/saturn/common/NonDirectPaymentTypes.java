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

/* This is a complex issue and the solutions here are preliminary
 * but there are basically two kinds of non-direct payments:
 * 
 * 1. Single payment where the specified amount is reserved and thus secured.
 *    In the case the amount to pay is above the one specified, like when a
 *    hotel guest puts additional items on the bill, the underlying payment
 *    scheme must support that.
 * 
 * 2. Recurring payments that are not guaranteed to succeed since the payer may 
 *    not have enough money to fulfill a request.  What will happen in the case
 *    of failure depends on the service the payment is associated with.  For
 *    e-commerce it would simply be that the order is rejected while electricity
 *    providers may (presumably after a warning), turn off the supply.
 *    
 *    Recurring payments is an optional feature requiring support from the
 *    underlying payment scheme to function.
 *    
 *    Note: Saturn is not a payment scheme, it only authorizes payment operations!
 *    
 */
public enum NonDirectPaymentTypes {

    RESERVATION,  // Single shot payment to be activated later
 
    RECURRING;    // Auto transactions, terminated by a zero-amount request
                  // or end of engagement period
}
