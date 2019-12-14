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
import org.webpki.tools.svg.SVGEmbeddedText;
import org.webpki.tools.svg.SVGPath;
import org.webpki.tools.svg.SVGRect;
import org.webpki.tools.svg.SVGText;
import org.webpki.tools.svg.SVGText.FONT_WEIGHTS;

public class SuperCardSquare extends SVGDocument implements CardImageData {
    public SuperCardSquare() {
        super(0, 0);
    }

    final static String CORE_COLOR = "#6d8838";
    final static String S_COLOR    = "#9e0a11";
    
    final static double NAME_Y_COORDINATE = STANDARD_HEIGHT * 0.76;
    
    @Override
    public String getFilters() {
        return 
        "<defs>\n" +
        "  <linearGradient id=\"supercardGradient\" x1=\"0\" y1=\"0\" x2=\"1\" y2=\"1\">\n" +
        "  <stop stop-color=\"#ffb115\" offset=\"0\"/>\n" +
        "  <stop stop-color=\"#ffff00\" offset=\"0.5\"/>\n" +
        "  <stop stop-color=\"#ffb115\" offset=\"1\"/>\n" +
        "  </linearGradient>\n" +
        "</defs>\n";

    }
    @Override
    public void generate() {
        add(new SVGRect(new SVGDoubleValue(0), new SVGDoubleValue(0),
            new SVGDoubleValue(STANDARD_WIDTH), new SVGDoubleValue(STANDARD_HEIGHT),
        null,
        null,
        "url(#supercardGradient)"));
        
        add(new SVGText(new SVGDoubleValue(STANDARD_TEXT_LEFT),
                        new SVGDoubleValue(NAME_Y_COORDINATE),
                        "Sans-serif",
                        STANDARD_NAME_FONT_SIZE,
                        null,
                        STANDARD_NAME));

        add(new SVGText(new SVGDoubleValue(STANDARD_TEXT_LEFT),
                        new SVGDoubleValue(NAME_Y_COORDINATE + STANDARD_TEXT_Y_OFFSET),
                        "Noto Sans",
                        STANDARD_ACCOUNT_FONT_SIZE,
                        null,
                        STANDARD_ACCOUNT).setFontWeight(SVGText.FONT_WEIGHTS.W500));

        SVGEmbeddedText et = new SVGEmbeddedText(org.webpki.saturn.svg.diagrams.SupercardGlyphs.class);

        String superCardString = "SuperCard";

        double x = STANDARD_WIDTH / 12;
        
        String color = S_COLOR;
        
        double transformation = STANDARD_HEIGHT / 5;
        double y = STANDARD_HEIGHT / 2.5;
        double gutter = STANDARD_WIDTH / 16;
        boolean first = true;
        double lineWidth = STANDARD_HEIGHT / 80;
        
        for (char c : superCardString.toCharArray()) {
            
            SVGEmbeddedText.DecodedGlyph dg = et.getDecodedGlyph(c, transformation);

            add(new SVGPath(new SVGDoubleValue(x), new SVGDoubleValue(y),
                    dg.getSVGPathValues(),
                    null,
                    null,
                    color));
            if (first) {
                add(new SVGRect(new SVGDoubleValue(x - 2 * lineWidth),
                                new SVGDoubleValue(y -  STANDARD_HEIGHT / 19 - transformation),
                        new SVGDoubleValue(dg.getXAdvance() + 6 * lineWidth), 
                        new SVGDoubleValue(STANDARD_HEIGHT / 3.5),
                    lineWidth,
                    "#31859c",
                    null));
                first = false;
            }
            x += dg.getXAdvance() + gutter;
            color = CORE_COLOR;
            transformation = STANDARD_HEIGHT / 3.6;
            y = STANDARD_HEIGHT / 2.6;
            gutter = STANDARD_WIDTH / 70;
        }
        add(new SVGText(new SVGDoubleValue(x),
                new SVGDoubleValue(y - STANDARD_HEIGHT / 10),
                "Sans-serif",
                STANDARD_HEIGHT / 10,
                SVGText.TEXT_ANCHOR.START,
                "TM").setFontWeight(SVGText.FONT_WEIGHTS.BOLD)
                     .setFontColor(CORE_COLOR)
                     .setLetterSpacing(2));
    }
 }
