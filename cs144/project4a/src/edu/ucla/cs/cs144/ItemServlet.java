package edu.ucla.cs.cs144;

import java.io.*;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ErrorHandler;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ItemServlet extends HttpServlet implements Servlet {

    public ItemServlet() {
    }


    public static Element getElementByTagNameNR(Element e, String tagName) {
        Node child = e.getFirstChild();
        while (child != null) {
            if (child instanceof Element && child.getNodeName().equals(tagName))
                return (Element) child;
            child = child.getNextSibling();
        }
        return null;
    }

    public static String getElementTextByTagNameNR(Element e, String tagName) {
        Element elem = getElementByTagNameNR(e, tagName);
        if (elem != null)
            return getElementText(elem);
        else
            return "";
    }

    public static Element[] getElementsByTagNameNR(Element e, String tagName) {
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

    public static String getElementText(Element e) {
        if (e.getChildNodes().getLength() == 1) {
            Text elementText = (Text) e.getFirstChild();
            return elementText.getNodeValue();
        } else
            return "";
    }

    static String strip(String money) {
        if (money.equals(""))
            return money;
        else {
            double am = 0.0;
            NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.US);
            try {
                am = nf.parse(money).doubleValue();
            } catch (ParseException e) {
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

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String result = AuctionSearchClient.getXMLDataForItemId(request.getParameter("id"));
        Document doc = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(result));
            doc = builder.parse(is);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            System.exit(1);
        }

        Element item = doc.getDocumentElement();
        request.setAttribute("Item_ID", item.getAttribute("ItemID"));
        request.setAttribute("Item_Name", getElementTextByTagNameNR(item, "Name"));


        Element[] categories = getElementsByTagNameNR(item, "Category");
        String[] cats = new String[categories.length];
        for (int i = 0; i < categories.length; i++) {
            Element category = categories[i];
            cats[i] = getElementText(category);
        }
        request.setAttribute("Categories", cats);

        Element seller = getElementByTagNameNR(item, "Seller");
        request.setAttribute("Seller_ID", seller.getAttribute("UserID"));
        request.setAttribute("Seller_Rating", seller.getAttribute("Rating"));
        request.setAttribute("Seller_Location", getElementTextByTagNameNR(item, "Location"));
        request.setAttribute("Seller_Country", getElementTextByTagNameNR(item, "Country"));
        request.setAttribute("Buy_Price", getElementTextByTagNameNR(item, "Buy_Price"));
        request.setAttribute("First_bid", getElementTextByTagNameNR(item, "First_Bid"));
        request.setAttribute("Currently", getElementTextByTagNameNR(item, "Currently"));
        request.setAttribute("Started", getElementTextByTagNameNR(item, "Started"));
        request.setAttribute("Ends", getElementTextByTagNameNR(item, "Ends"));
        request.setAttribute("Description", getElementTextByTagNameNR(item, "Description"));

        Element bids = getElementByTagNameNR(item, "Bids");
        Element[] allBids = getElementsByTagNameNR(bids, "Bid");

        ArrayList<Bid> bidList = new ArrayList<Bid>();

        SimpleDateFormat inputFormat = new SimpleDateFormat("MMM-dd-yy HH:mm:ss");

        for (Element bid : allBids) {
            Element bidder = getElementByTagNameNR(bid, "Bidder");

            Date dt = new Date();
            try {
                dt = inputFormat.parse(getElementTextByTagNameNR(bid, "Time"));
            } catch (ParseException e) {
                e.printStackTrace();
            }

            Bid b = new Bid();
            b.datetime = dt;
            b.amount = getElementTextByTagNameNR(bid, "Amount");
            b.bidder = bidder.getAttribute("UserID");
            b.bidder_rating = bidder.getAttribute("Rating");
            b.timeStr = formatDate(getElementTextByTagNameNR(bid, "Time"));
            b.location = getElementTextByTagNameNR(bidder, "Location");
            b.country = getElementTextByTagNameNR(bidder, "Country");
            bidList.add(b);
        }
        Bid[] bidArray = bidList.toArray(new Bid[bidList.size()]);
        Arrays.sort(bidArray, new Comparator<Bid>() {
            public int compare(Bid a, Bid b) {
                return a.datetime.compareTo(b.datetime);
            }
        });
        request.setAttribute("bids", bidArray);

        String address = getElementTextByTagNameNR(item, "Location") + " " + getElementTextByTagNameNR(item, "Country") ;
        request.setAttribute("address", address);

        request.getRequestDispatcher("/getItem.jsp").forward(request, response);
    }
}
