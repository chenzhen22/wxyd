package com.chenzhen.test;

import com.chenzhen.mapper.mysqlMapper.MysqlMapper;
import com.chenzhen.pojo.Doc;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.annotation.Resource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

@WebAppConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class ParseClTest {
    @Resource
    MysqlMapper mysqlMapper;

    @Test
    public void test() throws Exception {
        File fileDir = new File("D:\\apps\\data\\wxyd\\doc\\cl\\");
        if(!fileDir.exists()) {
            return;
        }
        File[] files = fileDir.listFiles();
        for(File file : files) {
            Document document = createDocument(file.getAbsolutePath());
            Element root = document.getDocumentElement();
            NodeList nl = root.getChildNodes();
            for (int i = 0; i < nl.getLength(); ++i) {
                Node node = nl.item(i);
                if (node instanceof Element) {
                    Element ele = (Element)node;
                    if ("cl:mvcAction".equals(ele.getTagName())) {
                        String id = ele.getAttribute("id");
                        String bid = ele.getAttribute("bid");
                        if(bid.contains(".")) {
                            Doc dd = mysqlMapper.queryDoc(id);
                            if(dd != null) {
                                continue;
                            }
                            Doc doc = new Doc(id, "", "transBank", bid, "web");
                            mysqlMapper.addDoc(doc);
                        }
                    }
                }
            }
        }


    }

    public static Document createDocument(String xmlFile)
            throws ParserConfigurationException, SAXException, IOException
    {
        Document document = null;
        try
        {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            FileInputStream xmlInputStream = new FileInputStream(xmlFile);
            DocumentBuilder builder = builderFactory.newDocumentBuilder();

            document = builder.parse(xmlInputStream);
        }
        catch (ParserConfigurationException e)
        {
            throw e;
        }
        catch (FileNotFoundException e) {
            throw e;
        }
        catch (SAXException e) {
            throw e;
        }
        catch (IOException e) {
            throw e;
        }

        return document;
    }
}
