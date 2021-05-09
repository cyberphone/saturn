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
import java.security.GeneralSecurityException;
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
    
    ReceiptTaxRecord taxRecordDecoder(JSONObjectReader rd, 
                                      Currencies currency) throws IOException {
        if (rd.hasProperty(TAX_JSON)) {
            JSONObjectReader taxObject = rd.getObject(TAX_JSON);
            return new ReceiptTaxRecord(taxObject.getMoney(AMOUNT_JSON, currency.decimals),
                                 taxObject.getBigDecimal(PERCENTAGE_JSON));
        }
        return null;
    }

    public ReceiptDecoder(JSONObjectReader rd) throws IOException, GeneralSecurityException {
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

        if (rd.hasProperty(SHIPPING_JSON)) {
            JSONObjectReader shippingRecord = rd.getObject(SHIPPING_JSON);
            optionalShippingRecord = new ReceiptShippingRecord(
                    shippingRecord.getStringArray(DESCRIPTION_JSON),
                    shippingRecord.getMoney(AMOUNT_JSON, currency.decimals));
        }
        if (rd.hasProperty(SUBTOTAL_JSON)) {
            optionalSubtotal = rd.getMoney(SUBTOTAL_JSON, currency.decimals);
        }
        if (rd.hasProperty(DISCOUNT_JSON)) {
            optionalDiscount = rd.getMoney(DISCOUNT_JSON, currency.decimals);
        }
        optionalTaxRecord = taxRecordDecoder(rd, currency);

        if (rd.hasProperty(BARCODE_JSON)) {
            JSONObjectReader barcodeObject = rd.getObject(BARCODE_JSON);
            barcode = new ReceiptBarcode(
                    barcodeObject.getString(VALUE_JSON),
                    ReceiptBarcode.BarcodeTypes.valueOf(barcodeObject.getString(TYPE_JSON)));
        }

        optionalFreeText = rd.getStringArrayConditional(FREE_TEXT_JSON);

        JSONArrayReader lineItemsArray = rd.getArray(LINE_ITEMS_JSON);
        lineItems = new ArrayList<>();
        optionalLineItemElements = EnumSet.noneOf(ReceiptLineItem.OptionalElements.class);
        do {
            JSONObjectReader lineItemObject = lineItemsArray.getObject();
            ReceiptLineItem lineItem = new ReceiptLineItem();
            lineItems.add(lineItem);
            if (lineItemObject.hasProperty(SKU_JSON)) {
                optionalLineItemElements.add(ReceiptLineItem.OptionalElements.SKU);
                lineItem.optionalSku = lineItemObject.getString(SKU_JSON);
            }

            lineItem.description = lineItemObject.getStringArray(DESCRIPTION_JSON);
            lineItem.quantity = lineItemObject.getBigDecimal(QUANTITY_JSON);

            if (lineItemObject.hasProperty(UNIT_JSON)) {
                optionalLineItemElements.add(ReceiptLineItem.OptionalElements.UNIT);
                lineItem.optionalUnit = lineItemObject.getString(UNIT_JSON);
            }
            if (lineItemObject.hasProperty(PRICE_JSON)) {
                optionalLineItemElements.add(ReceiptLineItem.OptionalElements.PRICE);
                lineItem.optionalPrice = 
                        lineItemObject.getMoney(PRICE_JSON, currency.decimals);
            }
            if (lineItemObject.hasProperty(SUBTOTAL_JSON)) {
                optionalLineItemElements.add(ReceiptLineItem.OptionalElements.SUBTOTAL);
                lineItem.optionalSubtotal = 
                        lineItemObject.getMoney(SUBTOTAL_JSON, currency.decimals);
            }
            if (lineItemObject.hasProperty(DISCOUNT_JSON)) {
                optionalLineItemElements.add(ReceiptLineItem.OptionalElements.DISCOUNT);
                lineItem.optionalDiscount = 
                        lineItemObject.getMoney(DISCOUNT_JSON, currency.decimals);
            }
            lineItem.optionalTaxRecord = taxRecordDecoder(lineItemObject, currency);
            if (lineItem.optionalTaxRecord != null) {
                optionalLineItemElements.add(ReceiptLineItem.OptionalElements.TAX);
            }
            
        } while (lineItemsArray.hasMore());

        paymentMethodName = rd.getString(PAYMENT_METHOD_NAME_JSON);
        optionalAccountReference = rd.getStringConditional(ACCOUNT_REFERENCE_JSON);
        payeeAuthorityUrl = rd.getString(PAYEE_AUTHORITY_URL_JSON);
        JSONObjectReader payerProviderData = rd.getObject(PAYER_PROVIDER_DATA_JSON);
        providerAuthorityUrl = payerProviderData.getString(PROVIDER_AUTHORITY_URL_JSON);
        providerCommonName = payerProviderData.getString(COMMON_NAME_JSON);
        providerReferenceId = payerProviderData.getString(REFERENCE_ID_JSON);
        payeeRequestId = payerProviderData.getString(PAYEE_REQUEST_ID_JSON);
        providerTimeStamp = payerProviderData.getDateTime(TIME_STAMP_JSON, ISODateTime.COMPLETE);
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

    private BigDecimal optionalDiscount;
    public BigDecimal getOptionalDiscount() {
        return optionalDiscount;
    }

    BigDecimal optionalSubtotal;
    public BigDecimal getOptionalSubtotal() {
        return optionalSubtotal;
    }

    ReceiptShippingRecord optionalShippingRecord;
    public ReceiptShippingRecord getOptionalShippingRecord() {
        return optionalShippingRecord;
    }

    ReceiptTaxRecord optionalTaxRecord;
    public ReceiptTaxRecord getOptionalTaxRecord() {
        return optionalTaxRecord;
    }

    EnumSet<ReceiptLineItem.OptionalElements> optionalLineItemElements;
    public EnumSet<ReceiptLineItem.OptionalElements> getOptionalLineItemElements() {
        return optionalLineItemElements;
    }

    ArrayList<ReceiptLineItem> lineItems;
    public List<ReceiptLineItem> getLineItems() {
        return lineItems;
    }

    ReceiptBarcode barcode;
    public ReceiptBarcode getOptionalBarcode() {
        return barcode;
    }

    String[] optionalFreeText;
    public String[] getOptionalFreeText() {
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
