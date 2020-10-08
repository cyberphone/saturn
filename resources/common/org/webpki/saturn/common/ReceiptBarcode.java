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

public class ReceiptBarcode {

    public enum BarcodeTypes {UPC_A, UPC_E, EAN_8, EAN_13, UPC_EAN_EXTENSION, 
                              CODE_39, CODE_93, CODE_128, CODABAR, ITF, QR_CODE};

    BarcodeTypes barcodeType;
        
    String barcodeValue;
    
    public ReceiptBarcode(String barcodeValue, BarcodeTypes barcodeType) {
        this.barcodeType = barcodeType;
        this.barcodeValue = barcodeValue;
    }
    
    public BarcodeTypes getBarcodeType() {
        return barcodeType;
    }
    
    public String getBarcodeValue() {
        return barcodeValue;
    }
}
