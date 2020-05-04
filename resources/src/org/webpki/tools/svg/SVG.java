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

import org.webpki.util.ArrayUtil;

public class SVG {
    
    static StringBuilder svgText = new StringBuilder();
    
    static SVGDocument doc;
    
    private static void _writeAttribute(String optionalAttribute, String value) {
        if (value != null) {
            svgText.append(' ')
                   .append(optionalAttribute)
                   .append("=\"")
                   .append(value)
                   .append('"');
        }
    }
    
    static void writeSVGObject(SVGObject svgObject) {
        doc.findLargestSize(svgObject);
        for (SVGObject dependencyElement : svgObject.beforeDependencyElements) {
            writeSVGObject(dependencyElement);
        }
        if (svgObject.linkUrl != null) {
            svgText.append("<a");
            if (svgObject.linkUrl.contains("=\"")) {
                svgText.append(' ').append(svgObject.linkUrl);
            } else {
                _writeAttribute("xlink:href", svgObject.linkUrl);
            }
            _writeAttribute("xlink:title", svgObject.linkToolTip);
            if (svgObject.linkReplace != null) {
                _writeAttribute("xlink:show", svgObject.linkReplace ? "replace" : "new");
            }
            svgText.append(">\n");
        }
        if (svgObject.invisible) {
            return;
        }
        if (svgObject instanceof SVGEndGlobal) {
            svgText.append("</g>\n");
            return;
        }
        svgText.append("<").append(svgObject.getTag());
        for (SVGAttributes svgAttribute : svgObject.getSVGAttributes().keySet()) {
            _writeAttribute(svgAttribute.toString(), svgObject.getAttribute(svgAttribute).getStringRepresentation());
        }
        if (svgObject.hasBody()) {
            if (svgObject.getBody() != null) {
                svgText.append(">")
                       .append(svgObject.getBody())
                       .append("</")
                       .append(svgObject.getTag());
            }
        } else if (!(svgObject instanceof SVGBeginGlobal)) {
            svgText.append("/");
        }
        svgText.append(">\n");
        for (SVGObject dependencyElement : svgObject.afterDependencyElements) {
            writeSVGObject(dependencyElement);
        }
        if (svgObject.hasBody() && svgObject.getBody() == null && !(svgObject instanceof SVGTransform)) {
            svgText.append("</")
                   .append(svgObject.getTag())
                   .append(">\n");
        }
        if (svgObject.endLink) {
            svgText.append("</a>\n");
        }
    }

    public static void main(String[] args) {
        if (args.length != 2 && args.length != 3) {
            System.out.println("Arguments outout-file class [filter-file]");
            System.exit(3);
        }
        try {
            doc = (SVGDocument) Class.forName(args[1]).newInstance();
            String filters = "";
            if (args.length == 3) {
                filters = new String(ArrayUtil.readFile(args[2]), "UTF-8");
            }
            doc.generate();
            filters += doc.getFilters();
            for (SVGObject svgObject : SVGDocument.svgObjects) {
                writeSVGObject(svgObject);
            }
            svgText.append("</svg>");
            StringBuilder total = new StringBuilder("<svg ");
            if (doc.useViewBox()) {
                total.append("viewBox=\"0 0 ")
                     .append((long)(doc.currentMaxX + SVGDocument.marginX))
                     .append(' ')
                     .append((long)(doc.currentMaxY +  + SVGDocument.marginY));
            } else {
                total.append("width=\"")
                     .append((long)(doc.currentMaxX + SVGDocument.marginX))
                     .append("\" height=\"")
                     .append((long)(doc.currentMaxY +  + SVGDocument.marginY));
            }
            total.append("\" xmlns=\"http://www.w3.org/2000/svg\"")
                 .append(SVGDocument.linksUsed ? " xmlns:xlink=\"http://www.w3.org/1999/xlink\"" : "")
                 .append(">\n")
                 .append(filters)
                 .append(svgText);
            ArrayUtil.writeFile(args[0], total.toString().getBytes("UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
