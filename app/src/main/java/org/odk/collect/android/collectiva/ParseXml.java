package org.odk.collect.android.collectiva;

import android.util.Log;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import javax.xml.parsers.DocumentBuilderFactory;

public class ParseXml{
    //dapetin instance berdasarkan idINstance dan variabel form
    public static HashMap<String, String> getLoadedXmlValues(String dir, HashSet<String> arvar){
        int eventType;
        String name;
        XmlPullParserFactory xmlPullParserFactory;

        HashMap<String, String> informasi = new HashMap<>();
        try {
            xmlPullParserFactory = XmlPullParserFactory.newInstance();
            xmlPullParserFactory.setNamespaceAware(false);
            XmlPullParser parser = xmlPullParserFactory.newPullParser();
            // access the xml file and convert it to input stream
            FileInputStream is = new FileInputStream(new File(dir));
            parser.setInput(is, null);
            eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT){
                if(eventType == XmlPullParser.START_TAG){
                    name = parser.getName();
                    if(arvar.contains(name)){
                        informasi.put(name, parser.nextText());
                    }
                }
                eventType = parser.next();
            }
        } catch (Exception e) {
            Log.d("DEBUGC","error happen when parser value ");
        }
        return informasi;
    }

//cari variabel apa aja yg ada di form
    public static ArrayList<String> getVariabelForm (String dir, String ids){
        ArrayList<String> arrayVariabel = new ArrayList<>();
        try {
            FileInputStream is = new FileInputStream(new File(dir));
            Document doc =
                    DocumentBuilderFactory
                            .newInstance()
                            .newDocumentBuilder()
                            .parse(is);

            NodeList nl = doc.getElementsByTagName(ids);

            NodeList nld;
            if(nl.item(0)!=null) {
                nld = nl.item(0).getChildNodes();
            }else {
                nl = doc.getElementsByTagName("data");
                nld = nl.item(0).getChildNodes();
            }

            addChildNode(arrayVariabel, nld);

        } catch (Exception e) {
            Log.d("DEBUGCOLL", "error because "+e.toString());
        }
        return arrayVariabel;
    }

    private static void addChildNode(ArrayList<String> arrayLists, NodeList nodeList){
        for (int j=0; j<nodeList.getLength(); j++){
            Node ndcc = nodeList.item(j);

//            if(ndcc.getAttributes()!=null && ndcc.getAttributes().getNamedItem("jr:template")!=null) {
//                Log.d("DEBUGCOLL","attr contain jr template, dont add to array");
//            }else {
//                if (ndcc.getChildNodes().getLength() > 0) {
//                    addChildNode(arrayLists, ndcc.getChildNodes());
//                } else if (!ndcc.getNodeName().startsWith("#")) {
//                    arrayLists.add(ndcc.getNodeName());
//                }
//            }

            if (ndcc.getChildNodes().getLength() > 0) {
                addChildNode(arrayLists, ndcc.getChildNodes());
            } else if (!ndcc.getNodeName().startsWith("#")) {
                arrayLists.add(ndcc.getNodeName());
            }
        }
    }
}
