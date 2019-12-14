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

import org.webpki.tools.svg.SVGDocument;
import org.webpki.tools.svg.SVGDoubleValue;
import org.webpki.tools.svg.SVGRect;
import org.webpki.tools.svg.SVGText;

public class PINKeyBoard extends SVGDocument {
    public PINKeyBoard() {
        super(1, 1);
    }

    final static double BUTTON_WIDTH  = 44;
    final static double BUTTON_HEIGHT = 36;
    final static double BUTTON_MARGIN = 2;
    final static double CLICK_MARGIN  = 6; 
    final static double BUTTON_HSPACE = 36;
    final static double BUTTON_VSPACE = 26;
    final static double VALIDATE_WIDTH = 126;
    final static double VALIDATE_HEIGHT = 60;
    
    final static String CORE_COLOR    = "#a5a5a5";
    final static String S_COLOR       = "#969191";
    final static String FRONT_COLOR   = "#fcfcfc";

    @Override
    public String getFilters() {
        return "<defs>\n" +
               "<filter id=\"actorsBlur\" x=\"-25%\" y=\"-25%\" width=\"150%\" height=\"150%\">\n" +
               "<feGaussianBlur stdDeviation=\"3\"/>\n" +
               "</filter>\n" +
               "</defs>\n";
    }

    @Override
    public boolean useViewBox() {
        return true;
    }

    SVGText basicButton(double x, double y, double buttonWidth, double buttonHeight, 
                        String fontFamily, int fontSize, String text, int yOffset,
                        String javascript) {
        add(new SVGRect(
                new SVGDoubleValue(x),
                new SVGDoubleValue(y),
                new SVGDoubleValue(buttonWidth + CLICK_MARGIN + CLICK_MARGIN),
                new SVGDoubleValue(buttonHeight + CLICK_MARGIN + CLICK_MARGIN),
                null,
                null,
                "#ffffff").setLink("javascript:" + javascript, null, null));
                x += CLICK_MARGIN;
                y += CLICK_MARGIN;
        add(new SVGRect(
                new SVGDoubleValue(x),
                new SVGDoubleValue(y),
                new SVGDoubleValue(buttonWidth),
                new SVGDoubleValue(buttonHeight),
                1.0,
                S_COLOR,
                CORE_COLOR).setRadiusX(10).setRadiusY(10));
        add(new SVGRect(
                new SVGDoubleValue(x + BUTTON_MARGIN),
                new SVGDoubleValue(y + BUTTON_MARGIN),
                new SVGDoubleValue(buttonWidth - BUTTON_MARGIN - BUTTON_MARGIN),
                new SVGDoubleValue(buttonHeight - BUTTON_MARGIN - BUTTON_MARGIN),
                null,
                null,
                FRONT_COLOR).setRadiusX(9).setRadiusY(9)
                            .setFilter("url(#actorsBlur)"));
        SVGText svgText = new SVGText(
                new SVGDoubleValue(x + (buttonWidth + 1) / 2),
                new SVGDoubleValue(y + buttonHeight / 2 + yOffset),
                fontFamily,
                fontSize,
                SVGText.TEXT_ANCHOR.MIDDLE,
                text);
        add(svgText.endLink());
        return svgText;
    }

    @Override
    public void generate() {
        double x = 0;
        double y = 0;
        for (int digit = 0; digit < 10; digit++) {
            String value = String.valueOf(digit);
            basicButton(x, y, BUTTON_WIDTH, BUTTON_HEIGHT,
                        "Roboto", 26, value, 9, "addDigit('" + value + "')")
               .setFontWeight(SVGText.FONT_WEIGHTS.BOLD);
            x += BUTTON_WIDTH + BUTTON_HSPACE;
            if (digit == 3 || digit == 6) {
                x = 0;
                y += BUTTON_HEIGHT + BUTTON_VSPACE;
            }
        }
        double validateX = 3.5 * BUTTON_WIDTH + 3 * BUTTON_HSPACE;
        basicButton(validateX + VALIDATE_WIDTH - BUTTON_WIDTH, 0, BUTTON_WIDTH, BUTTON_HEIGHT,
                    "Roboto", 45, "&#171;", 12, "deleteDigit()")
            .setFontColor("#be1018");
        
        basicButton(validateX, y - VALIDATE_HEIGHT + BUTTON_HEIGHT, VALIDATE_WIDTH, VALIDATE_HEIGHT,
                    "Roboto", 26, "Validate", 9, "validatePin()")
            .setFontWeight(SVGText.FONT_WEIGHTS.BOLD)
            .setFontColor("#009900");
    }
}
