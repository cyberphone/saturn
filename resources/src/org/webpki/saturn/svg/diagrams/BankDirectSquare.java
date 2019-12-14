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
package org.webpki.saturn.svg.diagrams;

import org.webpki.saturn.common.CardImageData;

import org.webpki.tools.svg.SVGDocument;
import org.webpki.tools.svg.SVGDoubleValue;
import org.webpki.tools.svg.SVGEllipse;
import org.webpki.tools.svg.SVGRect;
import org.webpki.tools.svg.SVGText;
import org.webpki.tools.svg.SVGText.FONT_WEIGHTS;

public class BankDirectSquare extends SVGDocument implements CardImageData {
    public BankDirectSquare() {
        super(0, 0);
    }
    
    final static double WHITE_SQUARE_WIDTH  = 138;
    final static double WHITE_SQUARE_HEIGHT = 98;
    final static double WHITE_SQUARE_PAD    = 14;
    
    final static double RED_ELLIPSE_PAD     = 9;

    final static double BANK_PAD            = 7;
    
    final static double BANK_SIZE           = 0.34;
    
    final static double NAME_Y_COORDINATE   = STANDARD_HEIGHT * 0.785;

    @Override
    public String getFilters() {
        return 
        "<defs>\n" +
        "<linearGradient y2=\"1\" x2=\"1\" y1=\"0\" x1=\"0\" id=\"bankdirectGradient\">\n" +
        "<stop offset=\"0\" stop-color=\"#c9daa7\"/>\n" +
        "<stop offset=\"0.5\" stop-color=\"#ebf1de\"/>\n" +
        "<stop offset=\"1\" stop-color=\"#c9daa7\"/>\n" +
        "</linearGradient>\n" +
        Bank.getBankDefs() +
        "</defs>\n";

    }

    @Override
    public void generate() {
        add(new SVGRect(
                new SVGDoubleValue(0),
                new SVGDoubleValue(0),
                new SVGDoubleValue(STANDARD_WIDTH),
                new SVGDoubleValue(STANDARD_HEIGHT),
                null,
                null,
                "url(#bankdirectGradient)"));

        add(new SVGText(
                new SVGDoubleValue(STANDARD_TEXT_LEFT),
                new SVGDoubleValue(NAME_Y_COORDINATE),
                "Sans-serif",
                STANDARD_NAME_FONT_SIZE,
                null,
                STANDARD_NAME));

        add(new SVGText(
                new SVGDoubleValue(STANDARD_TEXT_LEFT),
                new SVGDoubleValue(NAME_Y_COORDINATE + STANDARD_TEXT_Y_OFFSET),
                "Noto Sans",
                STANDARD_ACCOUNT_FONT_SIZE,
                null,
                STANDARD_ACCOUNT).setFontWeight(SVGText.FONT_WEIGHTS.W500));

        add(new SVGText(
                new SVGDoubleValue((STANDARD_WIDTH + WHITE_SQUARE_WIDTH + WHITE_SQUARE_PAD) / 2),
                new SVGDoubleValue(WHITE_SQUARE_PAD + 10 + WHITE_SQUARE_HEIGHT / 2),
                "Serif",
                28,
                SVGText.TEXT_ANCHOR.MIDDLE,
                "MyBank").setFontColor("#558ed5" ).setFontWeight(SVGText.FONT_WEIGHTS.BOLD));

        add(new SVGRect(
                new SVGDoubleValue(WHITE_SQUARE_PAD),
                new SVGDoubleValue(WHITE_SQUARE_PAD),
                new SVGDoubleValue(WHITE_SQUARE_WIDTH),
                new SVGDoubleValue(WHITE_SQUARE_HEIGHT),
                1.2,
                "#000000",
                "#ffffff"));

        add(new SVGEllipse(
                new SVGDoubleValue(WHITE_SQUARE_PAD + RED_ELLIPSE_PAD),
                new SVGDoubleValue(WHITE_SQUARE_PAD + RED_ELLIPSE_PAD),
                new SVGDoubleValue(WHITE_SQUARE_WIDTH - RED_ELLIPSE_PAD * 2),
                new SVGDoubleValue(WHITE_SQUARE_HEIGHT - RED_ELLIPSE_PAD * 2),
                2.0,
                "#f20707",
                null));

        add(new SVGText(
                new SVGDoubleValue(WHITE_SQUARE_WIDTH / 2 + WHITE_SQUARE_PAD),
                new SVGDoubleValue(WHITE_SQUARE_HEIGHT / 2  + WHITE_SQUARE_PAD - 4),
                "Sans-serif",
                18,
                SVGText.TEXT_ANCHOR.MIDDLE,
                "BANK"));

        add(new SVGText(
                new SVGDoubleValue(WHITE_SQUARE_WIDTH / 2 + WHITE_SQUARE_PAD),
                new SVGDoubleValue(WHITE_SQUARE_HEIGHT / 2  + WHITE_SQUARE_PAD + 14),
                "Sans-serif",
                18,
                SVGText.TEXT_ANCHOR.MIDDLE,
                "DIRECT"));

        bank(BANK_PAD, BANK_PAD);
        bank(BANK_PAD, WHITE_SQUARE_HEIGHT - BANK_PAD);
        bank(WHITE_SQUARE_WIDTH - BANK_PAD, BANK_PAD);
        bank(WHITE_SQUARE_WIDTH - BANK_PAD, WHITE_SQUARE_HEIGHT - BANK_PAD);
    }

    void bank(double x, double y) {
        double x_offset = Bank.BANK_WIDTH * BANK_SIZE / 2;
        if (x > BANK_PAD) x_offset = -x_offset;
        double y_offset = Bank.BANK_HEIGHT * BANK_SIZE / 2;
        if (y > BANK_PAD) y_offset = - y_offset;
        new Bank.SubBank(WHITE_SQUARE_PAD + x + x_offset, WHITE_SQUARE_PAD + y + y_offset, BANK_SIZE, true)
            .setShaddow(false)
            .setStrokeWeight(0.5)
            .generate(this);
    }
 }
