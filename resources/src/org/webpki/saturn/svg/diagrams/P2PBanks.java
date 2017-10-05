/*
 *  Copyright 2006-2016 WebPKI.org (http://webpki.org).
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import org.webpki.tools.svg.SVGLine;
import org.webpki.tools.svg.SVGPath;
import org.webpki.tools.svg.SVGRect;
import org.webpki.tools.svg.SVGShaderTemplate;
import org.webpki.tools.svg.SVGText;
import org.webpki.tools.svg.SVGValue;

public class P2PBanks extends SVGDocument {
    public P2PBanks() {
        super(5, 5);
    }

    double increment = Math.PI / 6;

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
        Bank.BANK_PILLAR +
        "</defs>\n";

    }
    
    double getBankX(int q) {
        return Math.cos(q * increment) * Bank.BANK_WIDTH * 2.5 + 3 * Bank.BANK_WIDTH;
    }
    
    double getBankY(int q) {
        return Math.sin(q * increment) * Bank.BANK_WIDTH * 2.5 + 2.5 * Bank.BANK_WIDTH + Bank.BANK_HEIGHT / 2;
    }
/*
SVGValue x1,
                   SVGValue y1,
                   SVGValue x2,
                   SVGValue y2,
                   Double strokeWidth,
                   String strokeColor
                    */
    @Override
    public void generate() {
        for (int q = 0; q < 12; q++) {
            for (int l = q + 1; l < 12; l++) {
                add(new SVGLine(new SVGDoubleValue(getBankX(q)),
                                new SVGDoubleValue(getBankY(q)),
                                new SVGDoubleValue(getBankX(l)),
                                new SVGDoubleValue(getBankY(l)),
                                1.0,
                                "#0000ff"));
            }
        }
        for (int q = 0; q < 12; q++) {
            new Bank.SubBank(getBankX(q), getBankY(q), 1, true).setStrokeWeight(1).setStrokeColor("#404040").generate(this);
        }
    }
}
