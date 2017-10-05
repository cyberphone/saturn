/*
 *  Copyright 2006-2016 WebPKI.org (http://webpki.org).
 *
 *  Licensed under the Apache License, Version 2.0 (the \"License\");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an \"AS IS\" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.webpki.saturn.svg.diagrams;

import org.webpki.tools.svg.SVGDocument;
import org.webpki.tools.svg.SVGPathValues;
import org.webpki.tools.svg.SVGSubDocument;
import org.webpki.tools.svg.SVGDoubleValue;
import org.webpki.tools.svg.SVGEmbeddedText;
import org.webpki.tools.svg.SVGPath;
import org.webpki.tools.svg.SVGRect;
import org.webpki.tools.svg.SVGShaderTemplate;
import org.webpki.tools.svg.SVGText;

public class Bank extends SVGDocument {

    public Bank() {
        super(5, 5);
    }

    static final int BANK_HEIGHT = 70;
    static final int BANK_WIDTH = 100;
    
    static final int ROOF_WIDTH = 90;
    
    static final int PILLAR_WIDTH_3 = 13;
    
    static final int PILLAR_WIDTH_4 = 10;

    static final int STAIRS_HEIGHT = 6;
    
    static final int OUTER_PILLAR_GAP = 7;
    
    static final int ROOF_HEIGHT = 15;
    
    static final int TOP_BAR_HEIGHT = 10;
    
    static final double STROKE_WIDTH = 2;
    
    static final String FILL_COLOR = "#f2f2f2";
    
    static final String STROKE_COLOR = "#a0a0a0";

    static final String BANK_PILLAR = 
            "<linearGradient y2=\"0.6\" x2=\"1\" y1=\"0.6\" x1=\"0\" id=\"bankPillar\">\n" +
            "<stop stop-opacity=\"0.98438\" stop-color=\"#707070\" offset=\"0\"/>\n" +
            "<stop offset=\"0.37323\" stop-opacity=\"0.97656\" stop-color=\"#e8e8e8\"/>\n" +
            "<stop stop-opacity=\"0.99219\" stop-color=\"#f4f4f2\" offset=\"0.65448\"/>\n" +
            "<stop stop-color=\"#969494\" stop-opacity=\"0.98438\" offset=\"0.99823\"/>\n" +
            "</linearGradient>\n" +
            "<filter width=\"200%\" height=\"200%\" x=\"-50%\" y=\"-50%\" id=\"bankBlur\">\n" +
            "<feGaussianBlur stdDeviation=\"2\"/>\n" +
            "</filter>\n";

    @Override
    public String getFilters() {
        return 
        "<defs>\n" +
         Bank.BANK_PILLAR +
        "</defs>\n";

    }

    public static class SubBank extends SVGSubDocument {
    
        double x, y, size;
        boolean fourPillars;
        double strokeWeight;
        String strokeColor = STROKE_COLOR;
   
        public SubBank(double x, double y, double size, boolean fourPillars) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.fourPillars = fourPillars;
            this.strokeWeight = STROKE_WIDTH * size;

        }
    
        public SubBank setStrokeWeight(double strokeWeight) {
            this.strokeWeight = strokeWeight;
            return this;
        }

        public SubBank setStrokeColor(String strokeColor) {
            this.strokeColor = strokeColor;
            return this;
        }

        @Override
        public void generate() {
            int numberOfPillars = fourPillars ? 4 : 3;
            double pillarWidth = fourPillars ? PILLAR_WIDTH_4 : PILLAR_WIDTH_3;
            double roofLR = (ROOF_WIDTH * size) / 2;
            double roofLow = (ROOF_HEIGHT - BANK_HEIGHT / 2) * size;
            double pillarX = x - roofLR + OUTER_PILLAR_GAP * size;
            double pillarXincrement = (roofLR + roofLR - (OUTER_PILLAR_GAP * 2 + pillarWidth) * size) / (numberOfPillars - 1);
            double pillarHeight = (BANK_HEIGHT - STAIRS_HEIGHT * 2 - ROOF_HEIGHT - TOP_BAR_HEIGHT) * size - strokeWeight * 1.5;
            double inset = ((BANK_WIDTH - ROOF_WIDTH) / 2) * size;
            SVGPathValues background = new SVGPathValues()
                .moveAbsolute(-roofLR - strokeWeight / 2, roofLow - strokeWeight / 2)
                .lineToAbsolute(0, -BANK_HEIGHT * size * 0.5 - strokeWeight / 2)
                .lineToAbsolute(roofLR + strokeWeight / 2, roofLow - strokeWeight / 2)
                .lineToAbsolute(roofLR + strokeWeight / 2, roofLow + TOP_BAR_HEIGHT * size + strokeWeight)
                .lineToAbsolute(roofLR - OUTER_PILLAR_GAP * size, roofLow + TOP_BAR_HEIGHT * size + strokeWeight)
                .lineToAbsolute(roofLR - OUTER_PILLAR_GAP * size, roofLow + TOP_BAR_HEIGHT * size + strokeWeight + pillarHeight)
                .lineToAbsolute((BANK_WIDTH / 2) * size - inset + strokeWeight / 2, roofLow + TOP_BAR_HEIGHT * size + strokeWeight + pillarHeight)
                .lineToAbsolute((BANK_WIDTH / 2) * size - inset + strokeWeight / 2, (BANK_HEIGHT / 2 - STAIRS_HEIGHT) * size - strokeWeight / 2)
                .lineToAbsolute((BANK_WIDTH / 2) * size + strokeWeight / 2, (BANK_HEIGHT / 2 - STAIRS_HEIGHT) * size - strokeWeight / 2)
                .lineToAbsolute((BANK_WIDTH / 2) * size + strokeWeight / 2, (BANK_HEIGHT / 2) * size + strokeWeight / 2)
                .lineToAbsolute(-(BANK_WIDTH / 2) * size - strokeWeight / 2, (BANK_HEIGHT / 2) * size + strokeWeight / 2)
                .lineToAbsolute(-(BANK_WIDTH / 2) * size - strokeWeight / 2,  (BANK_HEIGHT / 2 - STAIRS_HEIGHT) * size - strokeWeight / 2)
                .lineToAbsolute(-(BANK_WIDTH / 2) * size + inset - strokeWeight / 2,(BANK_HEIGHT / 2 - STAIRS_HEIGHT) * size - strokeWeight / 2)
                .lineToAbsolute(-(BANK_WIDTH / 2) * size + inset - strokeWeight / 2, roofLow + TOP_BAR_HEIGHT * size + strokeWeight + pillarHeight)
                .lineToAbsolute(-roofLR + OUTER_PILLAR_GAP * size, roofLow + TOP_BAR_HEIGHT * size + strokeWeight + pillarHeight)
                .lineToAbsolute(-roofLR + OUTER_PILLAR_GAP * size, roofLow + TOP_BAR_HEIGHT * size + strokeWeight)
                .lineToAbsolute(-roofLR - strokeWeight / 2, roofLow + TOP_BAR_HEIGHT * size + strokeWeight);
        add(new SVGPath(
                new SVGDoubleValue(x + 2.5),
                new SVGDoubleValue(y + 2.5),
                background,
                null,
                null,
                "#7f7f7f").setFilter("url(#bankBlur)"));
            add(new SVGPath(
                    new SVGDoubleValue(x),
                    new SVGDoubleValue(y),
                    new SVGPathValues().moveAbsolute(-roofLR, roofLow)
                                       .lineToAbsolute(0, -BANK_HEIGHT * size * 0.5)
                                       .lineToAbsolute(roofLR, roofLow),
                    strokeWeight,
                    strokeColor,
                    "#f2f2f2").setRoundLineCap());
            add(new SVGRect(
                    new SVGDoubleValue(x - roofLR),
                    new SVGDoubleValue(y + roofLow + strokeWeight / 2),
                    new SVGDoubleValue(roofLR + roofLR),
                    new SVGDoubleValue(TOP_BAR_HEIGHT * size),
                    strokeWeight,
                    strokeColor,
                    FILL_COLOR));
            add(new SVGRect(
                    new SVGDoubleValue(pillarX),
                    new SVGDoubleValue(y + roofLow + strokeWeight + TOP_BAR_HEIGHT * size),
                    new SVGDoubleValue(roofLR + roofLR - OUTER_PILLAR_GAP * 2 * size),
                    new SVGDoubleValue(pillarHeight * size),
                    null,
                    null,
                    "#ffffff"));
            for (int i = 0; i < numberOfPillars; i++) {
                add(new SVGRect(
                        new SVGDoubleValue(pillarX),
                        new SVGDoubleValue(y + roofLow + strokeWeight + TOP_BAR_HEIGHT * size),
                        new SVGDoubleValue(pillarWidth * size),
                        new SVGDoubleValue(pillarHeight * size),
                        null,
                        null,
                        "url(#bankPillar)"));
                pillarX += pillarXincrement;
            }
            add(new SVGRect(
                    new SVGDoubleValue(x - (BANK_WIDTH / 2) * size),
                    new SVGDoubleValue(y + (BANK_HEIGHT / 2 - STAIRS_HEIGHT) * size),
                    new SVGDoubleValue(BANK_WIDTH * size),
                    new SVGDoubleValue(STAIRS_HEIGHT * size),
                    strokeWeight,
                    strokeColor,
                    FILL_COLOR));
            add(new SVGRect(
                    new SVGDoubleValue(x - (BANK_WIDTH / 2) * size + inset),
                    new SVGDoubleValue(y + (BANK_HEIGHT / 2 - STAIRS_HEIGHT * 2) * size),
                    new SVGDoubleValue(BANK_WIDTH * size - inset * 2),
                    new SVGDoubleValue(STAIRS_HEIGHT * size),
                    strokeWeight,
                    strokeColor,
                    FILL_COLOR));
        }
    }
    
    @Override
    public void generate() {
       new SubBank(BANK_WIDTH /2, BANK_HEIGHT / 2, 1, true).generate(this);        
    }
}
