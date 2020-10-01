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

import java.io.IOException;

import java.math.BigDecimal;

import java.util.GregorianCalendar;
import java.util.List;

import org.webpki.json.JSONArrayWriter;
import org.webpki.json.JSONObjectWriter;

import org.webpki.util.ISODateTime;

// Encodes receipts in the Saturn specific JSON format

public class ReceiptEncoder implements BaseProperties {
    
    public ReceiptEncoder(String payeeReferenceId,
                          GregorianCalendar payeeTimeStamp, 
                          String payeeCommonName,
                          String[] optionalPhysicalAddress, 
                          String optionalPhoneNumber, 
                          String optionalEmailAddress, 
                          BigDecimal amount,
                          Currencies currency,
                          List<LineItem> lineItems,
                          Barcode optionalBarcode,
                          String optionalFreeText,
                          String paymentMethodName,
                          String optionalAccountReference, 
                          String payeeAuthorityUrl,
                          String providerCommonName,
                          String providerAuthorityUrl,
                          String providerReferenceId,
                          String payeeRequestId,
                          GregorianCalendar providerTimeStamp,
                          ServerAsymKeySigner signer) throws IOException {
        this(ReceiptDecoder.Status.AVAILABLE);
        receiptDocument
            .setString(REFERENCE_ID_JSON, payeeReferenceId)
            .setDateTime(TIME_STAMP_JSON, payeeTimeStamp, ISODateTime.LOCAL_NO_SUBSECONDS)
            .setString(COMMON_NAME_JSON, payeeCommonName)
            .setDynamic((wr) -> optionalPhysicalAddress == null ?
                            wr : wr.setStringArray(PHYSICAL_ADDRESS_JSON,
                                                   optionalPhysicalAddress))
            .setDynamic((wr) -> optionalPhoneNumber == null ?
                            wr : wr.setString(PHONE_NUMBER_JSON,
                                              optionalPhoneNumber))
            .setDynamic((wr) -> optionalEmailAddress == null ?
                            wr : wr.setString(EMAIL_ADDRESS_JSON,
                                              optionalEmailAddress))
            .setMoney(AMOUNT_JSON, amount, currency.getDecimals())
            .setString(CURRENCY_JSON, currency.toString())
            .setDynamic((wr) -> {
                JSONArrayWriter lineItemsArray = wr.setArray(LINE_ITEMS_JSON);
                for (LineItem lineItem : lineItems) {
                    lineItemsArray.setObject()
                        .setBigDecimal(QUANTITY_JSON, lineItem.quantity)
                        .setString(DESCRIPTION_JSON, lineItem.description)
                        .setDynamic((li) -> lineItem.optionalSubtotal == null ? li :
                            li.setMoney(SUBTOTAL_JSON, 
                                        lineItem.optionalSubtotal, 
                                        currency.getDecimals()))
                        .setDynamic((li) -> lineItem.optionalUnit == null ? li :
                            li.setString(UNIT_JSON, lineItem.optionalUnit))
                        .setDynamic((li) -> lineItem.optionalSku == null ? li :
                            li.setString(SKU_JSON, lineItem.optionalSku));
                     
                }
                return wr;
            })
            .setDynamic((wr) -> optionalBarcode == null ? wr :
                wr.setString(BARCODE_JSON, optionalBarcode.barcodeString)
                  .setString(BARCODE_TYPE_JSON, optionalBarcode.barcodeType.toString()))
            .setDynamic((wr) -> optionalFreeText == null ? wr : 
                wr.setString(FREE_TEXT_JSON, optionalFreeText))
            .setString(PAYMENT_METHOD_NAME_JSON, paymentMethodName)
            .setDynamic((wr) -> optionalAccountReference == null ?
                            wr : wr.setString(ACCOUNT_REFERENCE_JSON,
                                              optionalAccountReference))
            .setString(PAYEE_AUTHORITY_URL_JSON, payeeAuthorityUrl)
            .setObject(PROVIDER_DATA_JSON, new JSONObjectWriter()
                    .setString(COMMON_NAME_JSON, providerCommonName)
                    .setString(PROVIDER_AUTHORITY_URL_JSON, providerAuthorityUrl)
                    .setString(REFERENCE_ID_JSON, providerReferenceId)
                    .setString(PAYEE_REQUEST_ID_JSON, payeeRequestId)
                    .setDateTime(TIME_STAMP_JSON, 
                                 providerTimeStamp, 
                                 ISODateTime.LOCAL_NO_SUBSECONDS))
            .setSignature(RECEIPT_SIGNATURE_JSON, signer);
    }

    public ReceiptEncoder(ReceiptDecoder.Status notAvailableStatus) throws IOException {
        receiptDocument = Messages.RECEIPT.createBaseMessage()
                .setString(STATUS_JSON, notAvailableStatus.toString());
    }

    JSONObjectWriter receiptDocument;
    public JSONObjectWriter getReceiptDocument() throws IOException {
        return receiptDocument;
    }
}
