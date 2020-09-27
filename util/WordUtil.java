package top.mayiweiguishi.practice_online.server.util;

import lombok.extern.slf4j.Slf4j;
import net.arnx.wmf2svg.gdi.svg.SvgGdi;
import net.arnx.wmf2svg.gdi.wmf.WmfParser;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * word文件工具类
 */
@Slf4j
public class WordUtil {
//    private static StringBuilder stringBuilder = new StringBuilder();

    /**
     * 将word文件解压，用于下一步操作
     *
     * @param inputStream 要解压的word文件
     * @param outPath     要解压到的路径
     * @return
     */
    public static ArrayList unZIPWord(InputStream inputStream, String outPath) {
        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            byte[] ch = new byte[1024];
            while ((entry = zipInputStream.getNextEntry()) != null) {
                File zFile = new File(outPath + entry.getName());
                if (entry.isDirectory()) {
                    if (!zFile.exists()) {
                        zFile.mkdirs();
                    }
                    zipInputStream.closeEntry();
                } else {
                    File fpath = new File(zFile.getParent());
                    if (!fpath.exists()) {
                        fpath.mkdirs();
                    }
                    FileOutputStream outputStream = new FileOutputStream(zFile);
                    int i;
                    while ((i = zipInputStream.read(ch)) != -1) {
                        outputStream.write(ch, 0, i);
                        outputStream.flush();
                    }
                    zipInputStream.closeEntry();
                    outputStream.close();
                }
            }
            File file = new File(outPath + "word/media/");
            System.out.println(file.getAbsolutePath());
            for (File file1 : file.listFiles()) {
                if (file1.getAbsolutePath().endsWith("wmf")) {
                    wmfToSvg(file1.getAbsolutePath());
                }
            }
            return xml2JSON(outPath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 将解压过后该路径下{outPath/word/media}中的wmf格式转化为svg格式
     */
    private static void wmfToSvg(String src) {
        try {
            InputStream in = new FileInputStream(src);
            WmfParser parser = new WmfParser();
            final SvgGdi gdi = new SvgGdi(false);
            parser.parse(in, gdi);
            String dest = src.replaceFirst("wmf", "svg");
            Document doc = gdi.getDocument();
            OutputStream out = new FileOutputStream(dest);
            if (dest.endsWith(".svgz")) {
                out = new GZIPOutputStream(out);
            }
            output(doc, out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 将wmf转换输出为svg
     *
     * @param doc
     * @param out
     * @throws Exception
     */
    private static void output(Document doc, OutputStream out) throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "-//W3C//DTD SVG 1.0//EN");

        transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "http://www.w3.org/TR/2001/REC-SVG-20010904/DTD/svg10.dtd");
        transformer.transform(new DOMSource(doc), new StreamResult(out));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(doc), new StreamResult(bos));
        out.flush();
        out.close();
    }

    /**
     * 读取图片
     *
     * @return
     */
    private static Map readImg(String outPath) {
        /* --------- 实现获取图片 --------*/
        File file = new File(outPath + "word/_rels/document.xml.rels");
        SAXReader saxReader = new SAXReader();
        Map map = new HashMap();
        try {
            org.dom4j.Document document = saxReader.read(file);
            org.dom4j.Element element = document.getRootElement();
            Iterator iterator = element.elementIterator();
            while (iterator.hasNext()) {
                org.dom4j.Element element1 = (org.dom4j.Element) iterator.next();
                String key = element1.attribute("Id").getValue();
                String value = element1.attribute("Target").getValue();
                if (value.endsWith("bin") || value.endsWith("xml")) {
                    continue;
                }
                if (value.endsWith("wmf")) {
                    value = value.replaceAll("wmf", "svg");
                }
                map.put(key, value);
            }
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        return map;
    }

    private static ArrayList xml2JSON(String outPath) throws IOException, ParserConfigurationException, SAXException {
        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        SAXParser saxParser = saxParserFactory.newSAXParser();
        XMLParseHandler defaultHandler = new XMLParseHandler();
        defaultHandler.setMap(readImg(outPath));
        saxParser.parse(outPath + "word/document.xml", defaultHandler);
//        WordToJSON wordToJSON = new WordToJSON();
        WordToJSON2 wordToJSON = new WordToJSON2();
        wordToJSON.word2JSON(defaultHandler.toString());
        return wordToJSON.getQuestions();
    }

//    private static void readXML(String sourcePath, Map map) {
//        File file = new File(sourcePath);
//        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
//        builderFactory.setValidating(false);
//        try {
//            DocumentBuilder builder = builderFactory.newDocumentBuilder();
//            InputStream inputStream = new FileInputStream(file);
//            Document document = builder.parse(inputStream);
//            Element element = document.getDocumentElement();
//            NodeList nodeList = element.getChildNodes();
//            forEachNode(nodeList, map);
//        } catch (ParserConfigurationException e) {
//            e.printStackTrace();
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (SAXException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private static void forEachNode(NodeList nodelist, Map map) {
//        for (int i = 0; i < nodelist.getLength(); i++) {
//            Node node = nodelist.item(i);
//            if (node instanceof Element) {
//                loadBeanDefinition((Element) node, map);
//            }
//            if (node.hasChildNodes() && hasTextContent(node)) {
//                forEachNode(node.getChildNodes(), map);
//            }
//        }
//    }
//
//    private static void loadBeanDefinition(Element node, Map map) {
//        if (node.getNodeName().equals("w:p")) {
//            stringBuilder.append("<div>");
//            return;
//        }
//        if (node.getNodeName().equals("w:r")) {
//            stringBuilder.append("<p>" + node.getTextContent());
//            return;
//        }
//        if (node.getNodeName().equals("v:imagedata")) {
//            stringBuilder.append("<img src=" + map.get(node.getAttribute("r:id")) + ">");
//            return;
//        }
//    }

//    private static final boolean hasTextContent(Node child) {
//        return child.getNodeType() != 8 && child.getNodeType() != 7 && (child.getNodeType() != 3 || !((TextImpl) child).isIgnorableWhitespace());
//    }

}
