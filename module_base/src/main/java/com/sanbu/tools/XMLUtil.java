package com.sanbu.tools;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class XMLUtil {

    public static Document getXmlDocument(String fileUrl) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        Document doc = null;
        File file  = new File(fileUrl);
        builder = dbf.newDocumentBuilder();
        doc = builder.parse(file);
        return doc;
    }

    public static String getXmlString(String fileUrl, String encoding) {
        try {
            Document document = getXmlDocument(fileUrl);
            return getXmlString(document, encoding);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getXmlString(Document doc, String encoding) {
        try {
            Source source = new DOMSource(doc);
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            OutputStreamWriter write = new OutputStreamWriter(outStream);
            Result result = new StreamResult(write);
            Transformer xformer = TransformerFactory.newInstance().newTransformer();
            xformer.setOutputProperty(OutputKeys.ENCODING, encoding);
            xformer.transform(source, result);
            return outStream.toString();
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
            return null;
        } catch (TransformerException e) {
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Document stringToDoc(String xmlStr, String encoding) {
        try {
            xmlStr = new String(xmlStr.getBytes(),encoding);
            StringReader sr = new StringReader(xmlStr);
            InputSource is = new InputSource(sr);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder;
            builder = factory.newDocumentBuilder();
            return builder.parse(is);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            return null;
        } catch (SAXException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void writeStringToXmlFile(String strXml, String fileUrl, String encoding)
            throws TransformerException, FileNotFoundException {
        Document document = stringToDoc(strXml, encoding);
        writeDocToXmlFile(document, fileUrl, encoding);
    }

    public static void writeDocToXmlFile(Node document, String fileUrl, String encoding)
            throws TransformerException, FileNotFoundException {
        //将DOM对象document写入到xml文件中
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        DOMSource source = new DOMSource(document);
        transformer.setOutputProperty(OutputKeys.ENCODING, encoding);
        transformer.setOutputProperty(OutputKeys.VERSION, "1.0");
        transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
        PrintWriter pw = new PrintWriter(new FileOutputStream(fileUrl));
        StreamResult result = new StreamResult(pw);
        transformer.transform(source, result);     //关键转换
    }
}
