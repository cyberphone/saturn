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
import org.webpki.tools.svg.SVGEmbeddedText;
import org.webpki.tools.svg.SVGPath;
import org.webpki.tools.svg.SVGRect;
import org.webpki.tools.svg.SVGShaderTemplate;
import org.webpki.tools.svg.SVGText;

public class SuperCard extends SVGDocument {
    public SuperCard() {
        super(5, 5);
    }

    final static double WIDTH  = 300;
    final static double HEIGHT = 180;
    
    final static String CORE_COLOR = "#6d8838";
    final static String S_COLOR    = "#9e0a11";
    
    @Override
    public String getFilters() {
        return 
        "<defs>\n" +
        "  <linearGradient id=\"supercardGradient\" x1=\"0\" y1=\"0\" x2=\"1\" y2=\"1\">\n" +
        "  <stop stop-color=\"#ffb115\" offset=\"0\"/>\n" +
        "  <stop stop-color=\"#ffff00\" offset=\"0.5\"/>\n" +
        "  <stop stop-color=\"#ffb115\" stop-opacity=\"0.99219\" offset=\"1\"/>\n" +
        "  </linearGradient>\n" +
        "  <filter width=\"200%\" height=\"200%\" x=\"-50%\" y=\"-50%\" id=\"messageBlur\">\n" +
        "  <feGaussianBlur stdDeviation=\"2\"/>\n" +
        "  </filter>\n" + 
        "</defs>\n";

    }
    @Override
    public void generate() {
        add(new SVGRect(new SVGDoubleValue(0), new SVGDoubleValue(0),
            new SVGDoubleValue(WIDTH), new SVGDoubleValue(HEIGHT),
        1.2,
        "#A0A0A0",
        "url(#supercardGradient)")
           .setRadiusX(20)
           .setRadiusY(20)
           .setShader(new SVGShaderTemplate("url(#messageBlur)", "#afafaf", 6, 6)));
        
        add(new SVGText(new SVGDoubleValue(WIDTH/2),
                        new SVGDoubleValue(HEIGHT * 0.8),
                        "Sans-serif",
                        26,
                        SVGText.TEXT_ANCHOR.MIDDLE,
                        "Luke Skywalker"));
        SVGEmbeddedText et = new SVGEmbeddedText(org.webpki.saturn.svg.diagrams.SupercardGlyphs.class);

        String superCardString = "SuperCard";

        double x = WIDTH / 12;
        
        String color = S_COLOR;
        
        double transformation = HEIGHT / 5;
        double y = HEIGHT / 2.5;
        double gutter = WIDTH / 16;
        boolean first = true;
        double lineWidth = HEIGHT / 80;
        
        for (char c : superCardString.toCharArray()) {
            
            SVGEmbeddedText.DecodedGlyph dg = et.getDecodedGlyph(c, transformation);

            add(new SVGPath(new SVGDoubleValue(x), new SVGDoubleValue(y),
                    dg.getSVGPathValues(),
                    null,
                    null,
                    color));
            if (first) {
                add(new SVGRect(new SVGDoubleValue(x - 2 * lineWidth),
                                new SVGDoubleValue(y -  HEIGHT / 19 - transformation),
                        new SVGDoubleValue(dg.getXAdvance() + 6 * lineWidth), 
                        new SVGDoubleValue(HEIGHT / 3.5),
                    lineWidth,
                    "#31859c",
                    null));
                first = false;
            }
            x += dg.getXAdvance() + gutter;
            color = CORE_COLOR;
            transformation = HEIGHT / 3.6;
            y = HEIGHT / 2.6;
            gutter = WIDTH / 70;
        }
        add(new SVGText(new SVGDoubleValue(x),
                new SVGDoubleValue(y - HEIGHT / 10),
                "Sans-serif",
                HEIGHT / 10,
                SVGText.TEXT_ANCHOR.START,
                "TM").setFontWeight(SVGText.FONT_WEIGHTS.BOLD)
                     .setFontColor(CORE_COLOR)
                     .setLetterSpacing(2));
    }
 }
