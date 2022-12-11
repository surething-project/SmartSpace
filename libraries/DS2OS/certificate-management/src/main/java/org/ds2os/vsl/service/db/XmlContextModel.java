package org.ds2os.vsl.service.db;

import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;

public class XmlContextModel implements IContextModel {
    private final String path;

    private final Document xmlDocument;

    public XmlContextModel(String absolutePath) throws Exception {
        this.path = absolutePath;
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        this.xmlDocument = builder.parse(new FileInputStream(absolutePath));
    }

    public XmlContextModel(String modelName, InputStream inputStream) throws Exception {
        this.path = modelName;
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        this.xmlDocument = builder.parse(inputStream);
    }

    public byte[] getContent() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        StreamResult result = new StreamResult(bos);
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(this.xmlDocument);
        transformer.transform(source, result);
        return bos.toByteArray();
    }

    public String getPath() {
        return this.path;
    }
}
