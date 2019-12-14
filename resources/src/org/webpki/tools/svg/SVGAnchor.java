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

public class SVGAnchor {
    
    public enum ALIGNMENT {TOP_LEFT      (true,  false, true,  false),
                           TOP_CENTER    (false, false, true,  false),
                           TOP_RIGHT     (false, true,  true,  false),
                           MIDDLE_LEFT   (true,  false, false, false),
                           MIDDLE_CENTER (false, false, false, false),
                           MIDDLE_RIGHT  (false,  true, false, false),
                           BOTTOM_LEFT   (true,  false, false, true),
                           BOTTOM_CENTER (false, false, false, true),
                           BOTTOM_RIGHT  (false,  true, false, true);
    
        boolean leftAlign;
        boolean rightAlign;
        boolean topAlign;
        boolean bottomAlign;
        
        ALIGNMENT(boolean leftAlign, boolean rightAlign, boolean topAlign, boolean bottomAlign) {
            this.leftAlign = leftAlign;
            this.rightAlign = rightAlign;
            this.topAlign = topAlign;
            this.bottomAlign = bottomAlign;
        }
    };

    SVGValue x;
    SVGValue y;
    ALIGNMENT alignment;
    
    public SVGAnchor(SVGValue x, SVGValue y, ALIGNMENT alignment) {
        this.x = x;
        this.y = y;
        this.alignment = alignment;
    }
    
    public SVGAnchor(SVGObject xElement,
                     SVGObject yElement,
                     ALIGNMENT alignment) {
        this(xAlignment(xElement.getPrimaryX(), new SVGNegate(xElement.getPrimaryWidth()), alignment),
             yAlignment(yElement.getPrimaryY(), new SVGNegate(yElement.getPrimaryHeight()), alignment),
             ALIGNMENT.TOP_LEFT);
    }

    public SVGAnchor derive(SVGValue xOffset, SVGValue yOffset, ALIGNMENT alignment) {
        return new SVGAnchor(new SVGAddDouble(x, xOffset), new SVGAddDouble(y, yOffset), alignment);
    }

    static SVGValue xAlignment(SVGValue x, SVGValue width, ALIGNMENT alignment) {
        if (alignment.leftAlign) {
            return x;
        } else if (alignment.rightAlign) {
            return new SVGSubtractDouble(x, width);
        }
        return new SVGCenter(x, x, width);
    }

    static SVGValue yAlignment(SVGValue y, SVGValue height, ALIGNMENT alignment) {
        if (alignment.topAlign) {
            return y;
        } else if (alignment.bottomAlign) {
            return new SVGSubtractDouble(y, height);
        }
        return new SVGCenter(y, y, height);
    }
    
    SVGValue xAlignment(SVGValue width) {
        return xAlignment(x, width, alignment);
    }

    SVGValue yAlignment(SVGValue height) {
        return yAlignment(y, height, alignment);
    }
}
