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

// Holds a barcode for the Saturn receipt system

public class Barcode {

    public enum BarcodeTypes {EAN, QR};  // Place-holder at this stage

    String barcodeString;
 
    BarcodeTypes barcodeType;
        
    public Barcode(String barcodeString, BarcodeTypes barcodeType) {
        this.barcodeString = barcodeString;
        this.barcodeType = barcodeType;
    }
        
    Barcode() { // only used by the decoder

    }
}
