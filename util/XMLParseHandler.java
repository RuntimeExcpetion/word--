package top.mayiweiguishi.practice_online.server.util;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.Map;

/**
 * XMLParseHandler
 *
 * @blame Android Team
 */
public class XMLParseHandler extends DefaultHandler {
    private Map map;

    public void setMap(Map map) {
        this.map = map;
    }

    StringBuilder builder = new StringBuilder();

    @Override
    public void startDocument() throws SAXException {
        super.startDocument();
    }

    @Override
    public void endDocument() throws SAXException {
        super.endDocument();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (null != qName) {
            if ("v:imagedata".equals(qName)) {
                builder.append("<img src= 'http://cdn.mayiweiguishi.top/test/" + map.get(attributes.getValue("r:id")) + "' height='40' width='85'>");
            }
            if ("a:blip".equals(qName)) {
                builder.append("<img src= 'http://cdn.mayiweiguishi.top/test/" + map.get(attributes.getValue("r:embed")) + "' height='40' width='85'>");
            }
        }

    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        String contents = new String(ch, start, length);
        if (contents.trim().equals("QUOTE")) {
            return;
        }
        if (contents.trim().equals("false")) {
            return;
        }
        builder.append(contents);
    }

    @Override
    public String toString() {
        return builder.toString();
    }
}
