package edu.ucla.cs.cs144;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.Vector;

public class ProxyServlet extends HttpServlet implements Servlet {
       
    public ProxyServlet() {}

    static Element[] getElementsByTagNameNR(Element e, String tagName) {
        Vector<Element> elements = new Vector<Element>();
        Node child = e.getFirstChild();
        while (child != null) {
            if (child instanceof Element && child.getNodeName().equals(tagName)) {
                elements.add((Element) child);
            }
            child = child.getNextSibling();
        }
        Element[] result = new Element[elements.size()];
        elements.copyInto(result);
        return result;
    }

    /* Returns the first subelement of e matching the given tagName, or
     * null if one does not exist. NR means Non-Recursive.
     */
    static Element getElementByTagNameNR(Element e, String tagName) {
        Node child = e.getFirstChild();
        while (child != null) {
            if (child instanceof Element && child.getNodeName().equals(tagName))
                return (Element) child;
            child = child.getNextSibling();
        }
        return null;
    }

    String[] getSuggestions(String query) {
        Document doc = null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            doc = db.parse(new URL("http://google.com/complete/search?output=toolbar&q=" + query).openStream());
        } catch (ParserConfigurationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (SAXException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


        String[] strs = new String[0];
        if (doc == null)
            return strs;

        try {
            Element toplevel = doc.getDocumentElement();
            Element[] suggestions = getElementsByTagNameNR(toplevel, "CompleteSuggestion");
            strs = new String[suggestions.length];
            for (int i = 0; i < suggestions.length; i++) {
                Element e = getElementByTagNameNR(suggestions[i], "Suggestion");
                String s = e.getAttribute("data");
                strs[i] = s;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return strs;
        }

    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        /*
        HttpURLConnection connection = (HttpURLConnection) new URL("http://google.com/complete/search?output=toolbar&q=" + request.getParameter("q")).openConnection();
        InputStream in = new BufferedInputStream(connection.getInputStream());
        response.setContentType("text/xml");
        response.getWriter().write(new Scanner(in).useDelimiter("\\A").next());
        connection.disconnect();
        in.close()
        */

        String[] s = getSuggestions(request.getParameter("q"));
        String toWrite = "StateSuggestions() { this.states=[";
        if(s.length>0)
        {
            toWrite += "\""+s[0]+"\"";
            for(int i=1; i<s.length; s++)
            {
                toWrite+= ",\""+s[0]+"\"";
            }
        }
        toWrite+="];}";

        response.setContentType("text/json");
        response.getWriter().write(toWrite);



    }
}
