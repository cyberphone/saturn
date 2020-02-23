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
package org.webpki.tools.svg;

import java.util.ArrayList;

public abstract class SVGDocument {

    static ArrayList<SVGObject> svgObjects = new ArrayList<>();
    
    double currentMaxX;
    double currentMaxY;

    static double marginX;
    static double marginY;
    
    static boolean linksUsed;
    
    protected SVGDocument(double marginX, double marginY) {
        SVGDocument.marginX = marginX;
        SVGDocument.marginY = marginY;
    }
    
    public abstract void generate();

    public SVGObject add(SVGObject svgObject) {
        svgObjects.add(svgObject);
        return svgObject;
    }

    public SVGAnchor createDocumentAnchor(double x,double y, SVGAnchor.ALIGNMENT alignment) {
        return new SVGAnchor(new SVGDoubleValue(x), new SVGDoubleValue(y), alignment);
    }

    void findLargestSize(SVGObject svgObject) {
        double maxX = svgObject.getMaxX();
        if (maxX > currentMaxX) {
            currentMaxX = maxX;
        }
        double maxY = svgObject.getMaxY();
        if (maxY > currentMaxY) {
            currentMaxY = maxY;
        }
    }

    public String getFilters() {
        return "";
    }
    
    public boolean useViewBox() {
        return false;
    }
}
