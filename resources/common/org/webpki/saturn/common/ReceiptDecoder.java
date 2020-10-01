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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.List;

import org.webpki.json.JSONArrayReader;
import org.webpki.json.JSONCryptoHelper;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONSignatureDecoder;

import org.webpki.util.ISODateTime;

// Decodes receipts in the Saturn specific JSON format

public class ReceiptDecoder implements BaseProperties {
    
    public enum Status {PENDING, AVAILABLE, FAILED, DELETED}
    
    static final JSONCryptoHelper.Options signatureOptions = new JSONCryptoHelper.Options()
            .setPublicKeyOption(JSONCryptoHelper.PUBLIC_KEY_OPTIONS.REQUIRED)
            .setKeyIdOption(JSONCryptoHelper.KEY_ID_OPTIONS.FORBIDDEN);

    public ReceiptDecoder(JSONObjectReader rd) throws IOException {
        Messages.RECEIPT.parseBaseMessage(rd);
        status = Status.valueOf(rd.getString(STATUS_JSON));
        if (status != Status.AVAILABLE) {
            return;
        }
        payeeReferenceId = rd.getString(REFERENCE_ID_JSON);
        payeeTimeStamp = rd.getDateTime(TIME_STAMP_JSON, ISODateTime.COMPLETE);
        payeeCommonName = rd.getString(COMMON_NAME_JSON);
        optionalPhysicalAddress = rd.getStringArrayConditional(PHYSICAL_ADDRESS_JSON);
        optionalPhoneNumber = rd.getStringConditional(PHONE_NUMBER_JSON);
        optionalEmailAddress = rd.getStringConditional(EMAIL_ADDRESS_JSON);
        currency = Currencies.valueOf(rd.getString(CURRENCY_JSON));
        amount = rd.getMoney(AMOUNT_JSON, currency.decimals);
        JSONArrayReader lineItemsArray = rd.getArray(LINE_ITEMS_JSON);
        lineItems = new ArrayList<>();
        optionalLineItemElements = EnumSet.noneOf(LineItem.OptionalElements.class);
        do {
            JSONObjectReader lineItemObject = lineItemsArray.getObject();
            LineItem lineItem = new LineItem();
            lineItems.add(lineItem);
            lineItem.quantity = lineItemObject.getBigDecimal(QUANTITY_JSON);
            lineItem.description = lineItemObject.getString(DESCRIPTION_JSON);
            if (lineItemObject.hasProperty(SKU_JSON)) {
                optionalLineItemElements.add(LineItem.OptionalElements.SKU);
                lineItem.optionalSku = lineItemObject.getString(SKU_JSON);
            }
            if (lineItemObject.hasProperty(UNIT_JSON)) {
                optionalLineItemElements.add(LineItem.OptionalElements.UNIT);
                lineItem.optionalUnit = lineItemObject.getString(UNIT_JSON);
            }
            if (lineItemObject.hasProperty(SUBTOTAL_JSON)) {
                optionalLineItemElements.add(LineItem.OptionalElements.SUBTOTAL);
                lineItem.optionalSubtotal = 
                        lineItemObject.getMoney(SUBTOTAL_JSON, currency.decimals);
            }
        } while (lineItemsArray.hasMore());
        if (rd.hasProperty(BARCODE_JSON)) {
            barcode = new Barcode();
            barcode.barcodeString = rd.getString(BARCODE_JSON);
            barcode.barcodeType = Barcode.BarcodeTypes.valueOf(rd.getString(BARCODE_TYPE_JSON));
        }
        optionalFreeText = rd.getStringConditional(FREE_TEXT_JSON);
        paymentMethodName = rd.getString(PAYMENT_METHOD_NAME_JSON);
        optionalAccountReference = rd.getStringConditional(ACCOUNT_REFERENCE_JSON);
        payeeAuthorityUrl = rd.getString(PAYEE_AUTHORITY_URL_JSON);
        JSONObjectReader providerData = rd.getObject(PROVIDER_DATA_JSON);
        providerAuthorityUrl = providerData.getString(PROVIDER_AUTHORITY_URL_JSON);
        providerCommonName = providerData.getString(COMMON_NAME_JSON);
        providerReferenceId = providerData.getString(REFERENCE_ID_JSON);
        payeeRequestId = providerData.getString(PAYEE_REQUEST_ID_JSON);
        providerTimeStamp = providerData.getDateTime(TIME_STAMP_JSON, ISODateTime.COMPLETE);
        signatureDecoder = rd.getSignature(RECEIPT_SIGNATURE_JSON, signatureOptions);
        rd.checkForUnread();
    }
    
    Status status;
    public Status getStatus() {
        return status;
    }

    GregorianCalendar payeeTimeStamp;
    public GregorianCalendar getPayeeTimeStamp() {
        return payeeTimeStamp;
    }

    String payeeAuthorityUrl;
    public String getPayeeAuthorityUrl() {
        return payeeAuthorityUrl;
    }
    
    String providerAuthorityUrl;
    public String getProviderAuthorityUrl() {
        return providerAuthorityUrl;
    }

    String payeeCommonName;
    public String getPayeeCommonName() {
        return payeeCommonName;
    }

    String[] optionalPhysicalAddress;
    public String[] getOptionalPhysicalAddress() {
        return optionalPhysicalAddress;
    }

    String optionalPhoneNumber;
    public String getOptionalPhoneNumber() {
        return optionalPhoneNumber;
    }

    String optionalEmailAddress;
    public String getOptionalEmailAddress() {
        return optionalEmailAddress;
    }

    String providerCommonName;
    public String getProviderCommonName() {
        return providerCommonName;
    }

    GregorianCalendar providerTimeStamp;
    public GregorianCalendar getProviderTimeStamp() {
        return providerTimeStamp;
    }

    String optionalAccountReference;
    public String getOptionalAccountReference() {
        return optionalAccountReference;

    }

    String providerReferenceId;
    public String getProviderReferenceId() {
        return providerReferenceId;
    }

    String payeeRequestId;
    public String getPayeeRequestId() {
        return payeeRequestId;
    }

    String payeeReferenceId;
    public String getPayeeReferenceId() {
        return payeeReferenceId;
    }

    BigDecimal amount;
    public BigDecimal getAmount() {
        return amount;
    }

    Currencies currency;
    public Currencies getCurrency() {
        return currency;
    }
    
    EnumSet<LineItem.OptionalElements> optionalLineItemElements;
    public EnumSet<LineItem.OptionalElements> getOptionalLineItemElements() {
        return optionalLineItemElements;
    }

    ArrayList<LineItem> lineItems;
    public List<LineItem> getLineItems() {
        return lineItems;
    }

    Barcode barcode;
    public Barcode getOptionalBarcode() {
        return barcode;
    }

    String optionalFreeText;
    public String getOptionalFreeText() {
        return optionalFreeText;
    }

    String paymentMethodName;
    public String getPaymentMethodName() {
        return paymentMethodName;
    }

    JSONSignatureDecoder signatureDecoder;
    public JSONSignatureDecoder getSignatureDecoder() {
        return signatureDecoder;
    }
}
