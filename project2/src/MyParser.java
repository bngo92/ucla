/* CS144
 *
 * Parser skeleton for processing item-???.xml files. Must be compiled in
 * JDK 1.5 or above.
 *
 * Instructions:
 *
 * This program processes all files passed on the command line (to parse
 * an entire diectory, type "java MyParser myFiles/*.xml" at the shell).
 *
 * At the point noted below, an individual XML file has been parsed into a
 * DOM Document node. You should fill in code to process the node. Java's
 * interface for the Document Object Model (DOM) is in package
 * org.w3c.dom. The documentation is available online at
 *
 * http://java.sun.com/j2se/1.5.0/docs/api/index.html
 *
 * A tutorial of Java's XML Parsing can be found at:
 *
 * http://java.sun.com/webservices/jaxp/
 *
 * Some auxiliary methods have been written for you. You may find them
 * useful.
 */

//package edu.ucla.cs.cs144;

import java.io.*;
import java.text.*;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.*;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ErrorHandler;


class MyParser {
    
    static final String columnSeparator = "|*|";
    static DocumentBuilder builder;
    
    static final String[] typeName = {
	"none",
	"Element",
	"Attr",
	"Text",
	"CDATA",
	"EntityRef",
	"Entity",
	"ProcInstr",
	"Comment",
	"Document",
	"DocType",
	"DocFragment",
	"Notation",
    };
    
    static class MyErrorHandler implements ErrorHandler {
        
        public void warning(SAXParseException exception)
        throws SAXException {
            fatalError(exception);
        }
        
        public void error(SAXParseException exception)
        throws SAXException {
            fatalError(exception);
        }
        
        public void fatalError(SAXParseException exception)
        throws SAXException {
            exception.printStackTrace();
            System.out.println("There should be no errors " +
                               "in the supplied XML files.");
            System.exit(3);
        }
        
    }
    
    /* Non-recursive (NR) version of Node.getElementsByTagName(...)
     */
    static Element[] getElementsByTagNameNR(Element e, String tagName) {
        Vector< Element > elements = new Vector< Element >();
        Node child = e.getFirstChild();
        while (child != null) {
            if (child instanceof Element && child.getNodeName().equals(tagName))
            {
                elements.add( (Element)child );
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
    
    /* Returns the text associated with the given element (which must have
     * type #PCDATA) as child, or "" if it contains no text.
     */
    static String getElementText(Element e) {
        if (e.getChildNodes().getLength() == 1) {
            Text elementText = (Text) e.getFirstChild();
            return elementText.getNodeValue();
        }
        else
            return "";
    }
    
    /* Returns the text (#PCDATA) associated with the first subelement X
     * of e with the given tagName. If no such X exists or X contains no
     * text, "" is returned. NR means Non-Recursive.
     */
    static String getElementTextByTagNameNR(Element e, String tagName) {
        Element elem = getElementByTagNameNR(e, tagName);
        if (elem != null)
            return getElementText(elem);
        else
            return "";
    }
    
    /* Returns the amount (in XXXXX.xx format) denoted by a money-string
     * like $3,453.23. Returns the input if the input is an empty string.
     */
    static String strip(String money) {
        if (money.equals(""))
            return money;
        else {
            double am = 0.0;
            NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.US);
            try { am = nf.parse(money).doubleValue(); }
            catch (ParseException e) {
                System.out.println("This method should work for all " +
                                   "money values you find in our data.");
                System.exit(20);
            }
            nf.setGroupingUsed(false);
            return nf.format(am).substring(1);
        }
    }

    static String formatDate(String date) {
        if (date.equals(""))
            return date;
        else {
            SimpleDateFormat inputFormat = new SimpleDateFormat("MMM-dd-yy HH:mm:ss");
            try {
                Date date1 = inputFormat.parse(date);
                SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                return outputFormat.format(date1);
            } catch (ParseException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        return "";
    }
    
    /* Process one items-???.xml file.
     */
    static void processFile(File xmlFile) throws FileNotFoundException {
        Document doc = null;
        try {
            doc = builder.parse(xmlFile);
        }
        catch (IOException e) {
            e.printStackTrace();
            System.exit(3);
        }
        catch (SAXException e) {
            System.out.println("Parsing error on file " + xmlFile);
            System.out.println("  (not supposed to happen with supplied XML files)");
            e.printStackTrace();
            System.exit(3);
        }
        
        /* At this point 'doc' contains a DOM representation of an 'Items' XML
         * file. Use doc.getDocumentElement() to get the root Element. */
        System.out.println("Successfully parsed - " + xmlFile);
        
        /* Fill in code here (you will probably need to write auxiliary
            methods). */
        
        
        /**************************************************************/

        PrintWriter itemDat = new PrintWriter(new FileOutputStream("Item.dat"), true);
        PrintWriter userDat = new PrintWriter(new FileOutputStream("User.dat"), true);
        PrintWriter itemBidDat = new PrintWriter(new FileOutputStream("ItemBid.dat"), true);
        PrintWriter itemCategoryDat = new PrintWriter(new FileOutputStream("ItemCategory.dat"), true);

        Element root = doc.getDocumentElement();
        Element[] items = getElementsByTagNameNR(root, "Item");
        for (Element item : items) {
            Element seller = getElementByTagNameNR(item, "Seller");
            String userId = seller.getAttribute("UserID");
            String rating = seller.getAttribute("Rating");
            String location = getElementTextByTagNameNR(item, "Location");
            String country = getElementTextByTagNameNR(item, "Country");
            userDat.printf("%s,%s,%s,%s\n", userId, rating, location, country);

            String itemId = item.getAttribute("ItemID");
            String name = getElementTextByTagNameNR(item, "Name");
            String currently = strip(getElementTextByTagNameNR(item, "Currently"));
            String firstBid = strip(getElementTextByTagNameNR(item, "First_Bid"));
            String started = formatDate(getElementTextByTagNameNR(item, "Started"));
            String ends = formatDate(getElementTextByTagNameNR(item, "Ends"));
            String description = getElementTextByTagNameNR(item, "Description");
            itemDat.printf("%s,%s,%s,%s,%s,%s,%s,%s\n", itemId, name, currently, firstBid, started, userId, ends, description);

            Element[] categories = getElementsByTagNameNR(item, "Category");
            for (Element category : categories) {
                String categoryName = getElementText(category);
                itemCategoryDat.printf("%s,%s\n", itemId, categoryName);
            }

            Element bids = getElementByTagNameNR(item, "Bids");
            for (Element bid : getElementsByTagNameNR(bids, "Bid")) {
                Element bidder = getElementByTagNameNR(bid, "Bidder");
                String bidderId = bidder.getAttribute("UserID");
                String time = formatDate(getElementTextByTagNameNR(bid, "Time"));
                String amount = strip(getElementTextByTagNameNR(bid, "Amount"));
                itemBidDat.printf("%s,%s,%s,%s\n", itemId, bidderId, time, amount);
            }
        }
    }
    
    public static void recursiveDescent(Node n, int level) {
        // adjust indentation according to level
        for(int i=0; i<4*level; i++)
            System.out.print(" ");
        
        // dump out node name, type, and value  
        String ntype = typeName[n.getNodeType()];
        String nname = n.getNodeName();
        String nvalue = n.getNodeValue();
        
        System.out.println("Type = " + ntype + ", Name = " + nname + ", Value = " + nvalue);
        
        // dump out attributes if any
        org.w3c.dom.NamedNodeMap nattrib = n.getAttributes();
        if(nattrib != null && nattrib.getLength() > 0)
            for(int i=0; i<nattrib.getLength(); i++)
                recursiveDescent(nattrib.item(i),  level+1);
        
        // now walk through its children list
        org.w3c.dom.NodeList nlist = n.getChildNodes();
        
        for(int i=0; i<nlist.getLength(); i++)
            recursiveDescent(nlist.item(i), level+1);
    }  
    
    public static void main (String[] args) throws FileNotFoundException {
        if (args.length == 0) {
            System.out.println("Usage: java MyParser [file] [file] ...");
            System.exit(1);
        }
        
        /* Initialize parser. */
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setIgnoringElementContentWhitespace(true);      
            builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new MyErrorHandler());
        }
        catch (FactoryConfigurationError e) {
            System.out.println("unable to get a document builder factory");
            System.exit(2);
        } 
        catch (ParserConfigurationException e) {
            System.out.println("parser was unable to be configured");
            System.exit(2);
        }
        
        /* Process all files listed on command line. */
        for (int i = 0; i < args.length; i++) {
            File currentFile = new File(args[i]);
            processFile(currentFile);
        }
    }
}
