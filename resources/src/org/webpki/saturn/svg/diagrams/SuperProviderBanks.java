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

import org.webpki.tools.svg.SVGAnchor;
import org.webpki.tools.svg.SVGCircle;
import org.webpki.tools.svg.SVGDocument;
import org.webpki.tools.svg.SVGDoubleValue;
import org.webpki.tools.svg.SVGLine;
import org.webpki.tools.svg.SVGShaderTemplate;
import org.webpki.tools.svg.SVGText;

public class SuperProviderBanks extends SVGDocument {
    public SuperProviderBanks() {
        super(5, 5);
    }

    double increment = Math.PI / 6;
    
    static final double CENTER_X = 3 * Bank.BANK_WIDTH;
    
    static final double CENTER_Y = 2.5 * Bank.BANK_WIDTH + Bank.BANK_HEIGHT / 2;

    @Override
    public String getFilters() {
        return 
        "<defs>\n" +
        "<radialGradient id=\"super\" spreadMethod=\"pad\" cx=\"0.5\" cy=\"0.5\" r=\"1\">\n" +
        "<stop stop-color=\"#ffe100\" offset=\"0\"/>\n" +
        "<stop stop-color=\"#ff005d\" offset=\"1\"/>\n" +
        "</radialGradient>\n" +
        Bank.getBankDefs() +
        "</defs>\n";

    }
    
    double getBankX(int q) {
        return Math.cos(q * increment) * Bank.BANK_WIDTH * 2.5 + CENTER_X;
    }
    
    double getBankY(int q) {
        return Math.sin(q * increment) * Bank.BANK_WIDTH * 2.5 + CENTER_Y;
    }

    @Override
    public void generate() {
        for (int q = 0; q < 12; q++) {
            add(new SVGLine(new SVGDoubleValue(getBankX(q)),
                            new SVGDoubleValue(getBankY(q)),
                            new SVGDoubleValue(CENTER_X),
                            new SVGDoubleValue(CENTER_Y),
                            1.0,
                            "#0000ff"));
        }
        for (int q = 0; q < 12; q++) {
            new Bank.SubBank(getBankX(q), getBankY(q), 1, true)
                .setShaddow(true)
                .generate(this);
        }
        add(new SVGCircle(new SVGAnchor(new SVGDoubleValue(CENTER_X),
                                        new SVGDoubleValue(CENTER_Y),
                                        SVGAnchor.ALIGNMENT.MIDDLE_CENTER),
                          new SVGDoubleValue(Bank.BANK_WIDTH * 2),
                          1.0,
                          Bank.STROKE_COLOR,
                          "url(#super)").setShader(new SVGShaderTemplate(Bank.SHADDOW_FILTER,
                                                                         "#a0a0a0",
                                                                         Bank.X_SHADDOW * 1.5,
                                                                         Bank.Y_SHADDOW * 1.5)));
        add(new SVGText(new SVGDoubleValue(CENTER_X + 4),
                        new SVGDoubleValue(CENTER_Y - 16),
                        "Sans-serif",
                        34.0,
                        SVGText.TEXT_ANCHOR.MIDDLE,
                        "Super\nProvider"));
    }
}
