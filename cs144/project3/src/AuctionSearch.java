import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringWriter;
import java.sql.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class AuctionSearch implements IAuctionSearch {

	/* 
         * You will probably have to use JDBC to access MySQL data
         * Lucene IndexSearcher class to lookup Lucene index.
         * Read the corresponding tutorial to learn about how to use these.
         *
         * Your code will need to reference the directory which contains your
	 * Lucene index files.  Make sure to read the environment variable 
         * $LUCENE_INDEX with System.getenv() to build the appropriate path.
	 *
	 * You may create helper functions or classes to simplify writing these
	 * methods. Make sure that your helper functions are not public,
         * so that they are not exposed to outside of this class.
         *
         * Any new classes that you create should be part of
         * edu.ucla.cs.cs144 package and their source files should be
         * placed at src/edu/ucla/cs/cs144.
         *
         */

    static private HashMap<String, String> fieldNameMap = new HashMap<String, String>();

    static {
        fieldNameMap.put(FieldName.BidderId, "UserID");
        fieldNameMap.put(FieldName.BuyPrice, "Buy_Price");
        fieldNameMap.put(FieldName.Category, "Category");
        fieldNameMap.put(FieldName.Description, "Description");
        fieldNameMap.put(FieldName.EndTime, "Ends");
        fieldNameMap.put(FieldName.ItemName, "Name");
        fieldNameMap.put(FieldName.SellerId, "Seller");
        fieldNameMap.put("Content", "Content");
    }

    private SimpleDateFormat xmlFormat = new SimpleDateFormat("MMM-dd-yy HH:mm:ss");

    public String escape(String s) {
        s = s.replaceAll("<", "&lt");
        s = s.replaceAll(">", "&gt");
        s = s.replaceAll("\"", "&quot");
        s = s.replaceAll("\'", "&apos");
        return s;
    }

    public SearchResult[] basicSearch(String query, int numResultsToSkip,
                                      int numResultsToReturn) {
        SearchConstraint[] searchConstraints = new SearchConstraint[1];
        searchConstraints[0] = new SearchConstraint("Content", query);
        return advancedSearch(searchConstraints, numResultsToSkip, numResultsToReturn);
    }

    public SearchResult[] advancedSearch(SearchConstraint[] constraints,
                                         int numResultsToSkip, int numResultsToReturn) {
        HashSet<SearchResult> results = new HashSet<SearchResult>();
        boolean first = true;
        for (SearchConstraint searchConstraint : constraints) {
            if (first) {
                first = false;
                String searchConstraintField = searchConstraint.getFieldName();
                if (searchConstraintField.equals(FieldName.Description)
                        || searchConstraintField.equals(FieldName.Category)
                        || searchConstraintField.equals(FieldName.ItemName)
                        || searchConstraintField.equals("Content")) {
                    IndexSearcher searcher;
                    QueryParser parser;
                    Query query;
                    Hits hits;
                    try {
                        searcher = new IndexSearcher(System.getenv("LUCENE_INDEX"));
                        parser = new QueryParser(fieldNameMap.get(searchConstraint.getFieldName()), new StandardAnalyzer());
                        query = parser.parse(searchConstraint.getValue());
                        hits = searcher.search(query);
                    } catch (IOException e) {
                        return null;
                    } catch (ParseException e) {
                        return null;
                    }

                    int stop = numResultsToSkip + numResultsToReturn;
                    if (results.isEmpty()) {
                        for (int i = numResultsToSkip; (numResultsToReturn == 0 || (i < stop)) && i < hits.length(); i++) {
                            Document doc;
                            try {
                                doc = hits.doc(i);
                            } catch (IOException e) {
                                break;
                            }
                            results.add(new SearchResult(doc.get("ID"), doc.get(fieldNameMap.get(FieldName.ItemName))));
                        }
                    } else {
                        HashSet<String> validIds = new HashSet<String>();
                        for (int i = numResultsToSkip; (numResultsToReturn == 0 || (i < stop)) && i < hits.length(); i++) {
                            Document doc;
                            try {
                                doc = hits.doc(i);
                            } catch (IOException e) {
                                break;
                            }
                            validIds.add(doc.get("ID"));
                        }
                        for (SearchResult searchResult : results)
                            if (!validIds.contains(searchResult.getItemId()))
                                results.remove(searchResult);
                    }
                } else {
                    Connection conn;
                    PreparedStatement stmt;

                    try {
                        conn = DbManager.getConnection(true);
                        String statement = "SELECT * FROM Item WHERE " + fieldNameMap.get(searchConstraint.getFieldName()) + "=?";
                        stmt = conn.prepareStatement(statement);
                        if (searchConstraint.getFieldName().equals(FieldName.BuyPrice)) {
                            stmt.setDouble(1, Double.valueOf(searchConstraint.getValue()));
                        } else {
                            SimpleDateFormat sqlFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            try {
                                stmt.setString(1, sqlFormat.format(xmlFormat.parse(searchConstraint.getValue())));
                            } catch (java.text.ParseException e) {
                                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                            }
                        }
                        ResultSet rs = stmt.executeQuery();
                        if (results.isEmpty())
                            while (rs.next())
                                results.add(new SearchResult(rs.getString("ItemID"), rs.getString("Name")));
                        else {
                            HashSet<String> validIds = new HashSet<String>();
                            while (rs.next())
                                validIds.add(rs.getString("ItemID"));
                            for (SearchResult searchResult : results)
                                if (!validIds.contains(searchResult.getItemId()))
                                    results.remove(searchResult);
                        }
                    } catch (SQLException e) {
                        System.out.println("HAPPY BIRTHDAY JENNIFER");
                        e.printStackTrace();
                        break;
                    }
                }
            }
        }
        return results.toArray(new SearchResult[results.size()]);
    }

    public String getXMLDataForItemId(String itemId) {
        Connection conn;
        PreparedStatement itemStatement;
        PreparedStatement categoryStatement;
        PreparedStatement bidStatement;
        PreparedStatement userStatement;

        // create a connection to the database to retrieve Items from MySQL
        try {
            conn = DbManager.getConnection(true);
            itemStatement = conn.prepareStatement("SELECT * FROM Item WHERE ItemID=?");
            categoryStatement = conn.prepareStatement("SELECT * FROM ItemCategory WHERE ItemID=?");
            bidStatement = conn.prepareStatement("SELECT * FROM ItemBid WHERE ItemID=?");
            userStatement = conn.prepareStatement("SELECT * FROM User WHERE UserID=?");
        } catch (SQLException ex) {
            System.out.println(ex);
            return "";
        }

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;
        try {
            docBuilder = docFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            return "";
        }

        try {
            itemStatement.setString(1, itemId);
            categoryStatement.setString(1, itemId);
            bidStatement.setString(1, itemId);
            ResultSet rs = itemStatement.executeQuery();
            if (rs.next()) {
                org.w3c.dom.Document doc = docBuilder.newDocument();
                Element rootElement = doc.createElement("Item");
                rootElement.setAttribute("ItemID", escape(itemId));
                doc.appendChild(rootElement);

                Element name = doc.createElement("Name");
                name.appendChild(doc.createTextNode(escape(rs.getString("Name"))));
                rootElement.appendChild(name);

                ResultSet categories = categoryStatement.executeQuery();
                while (categories.next()) {
                    Element category = doc.createElement("Category");
                    category.appendChild(doc.createTextNode(escape(categories.getString("Category"))));
                    rootElement.appendChild(category);
                }

                ArrayList<Element> bidArray = new ArrayList<Element>();
                ResultSet bids = bidStatement.executeQuery();
                double maxAmount = rs.getDouble("First_Bid");
                DecimalFormat df = new DecimalFormat("$#.00");
                while (bids.next()) {
                    Element bid = doc.createElement("Bid");
                    bidArray.add(bid);

                    userStatement.setString(1, bids.getString("UserID"));
                    ResultSet bidderAttributes = userStatement.executeQuery();

                    Element bidder = doc.createElement("Bidder");
                    bidder.setAttribute("UserID", escape(bidderAttributes.getString("UserID")));
                    bidder.setAttribute("Rating", escape(bidderAttributes.getString("Rating")));
                    bid.appendChild(bidder);

                    if (bidderAttributes.getString("Location") != null) {
                        Element location = doc.createElement("Location");
                        location.appendChild(doc.createTextNode(escape(bidderAttributes.getString("Location"))));
                        bidder.appendChild(location);
                    }

                    if (bidderAttributes.getString("Country") != null) {
                        Element country = doc.createElement("Country");
                        country.appendChild(doc.createTextNode(escape(bidderAttributes.getString("Country"))));
                        bidder.appendChild(country);
                    }

                    Element time = doc.createElement("Time");
                    time.appendChild(doc.createTextNode(xmlFormat.format(bids.getTimestamp("Time"))));
                    bid.appendChild(time);

                    Element amount = doc.createElement("Amount");

                    amount.appendChild(doc.createTextNode(df.format(bids.getDouble("Amount"))));
                    bid.appendChild(amount);
                    if (bids.getDouble("Amount") > maxAmount)
                        maxAmount = bids.getDouble("Amount");

                }

                Element currently = doc.createElement("Currently");
                currently.appendChild(doc.createTextNode(df.format(maxAmount)));
                rootElement.appendChild(currently);

                if (rs.getDouble("Buy_Price") > 0) {
                    Element buyPrice = doc.createElement("Buy_Price");
                    buyPrice.appendChild(doc.createTextNode(df.format(rs.getDouble("Buy_Price"))));
                    rootElement.appendChild(buyPrice);
                }

                Element firstBid = doc.createElement("FirstBid");
                firstBid.appendChild(doc.createTextNode(df.format(rs.getDouble("First_Bid"))));
                rootElement.appendChild(firstBid);

                Element numberOfBids = doc.createElement("Number_of_Bids");
                numberOfBids.appendChild(doc.createTextNode(bidArray.size() + ""));
                rootElement.appendChild(numberOfBids);

                Element bidsElement = doc.createElement("Bids");
                for (Element bid : bidArray)
                    bidsElement.appendChild(bid);
                rootElement.appendChild(bidsElement);

                userStatement.setString(1, rs.getString("Seller"));
                ResultSet sellerAttributes = userStatement.executeQuery();
                sellerAttributes.next();

                Element location = doc.createElement("Location");
                location.appendChild(doc.createTextNode(escape(sellerAttributes.getString("Location"))));
                rootElement.appendChild(location);

                Element country = doc.createElement("Country");
                country.appendChild(doc.createTextNode(escape(sellerAttributes.getString("Country"))));
                rootElement.appendChild(country);

                Element started = doc.createElement("Started");
                started.appendChild(doc.createTextNode(xmlFormat.format(rs.getTimestamp("Started"))));
                rootElement.appendChild(started);

                Element ends = doc.createElement("Ends");
                ends.appendChild(doc.createTextNode(xmlFormat.format(rs.getTimestamp("Ends"))));
                rootElement.appendChild(ends);

                Element seller = doc.createElement("Seller");
                seller.setAttribute("UserID", escape(sellerAttributes.getString("UserID")));
                seller.setAttribute("Rating", escape(sellerAttributes.getString("Rating")));
                rootElement.appendChild(seller);

                Element description = doc.createElement("Description");
                description.appendChild(doc.createTextNode(escape(rs.getString("Description"))));
                rootElement.appendChild(description);

                TransformerFactory tf = TransformerFactory.newInstance();
                Transformer transformer = null;
                try {
                    transformer = tf.newTransformer();
                } catch (TransformerConfigurationException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                StringWriter writer = new StringWriter();
                try {
                    transformer.transform(new DOMSource(doc), new StreamResult(writer));
                } catch (TransformerException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
                return writer.getBuffer().toString().replaceAll("\n|\r", "");
            } else {
                return "";
            }
        } catch (SQLException e) {
            System.out.println(e);
            e.printStackTrace();
        }

        // close the database connection
        try {
            conn.close();
        } catch (SQLException ex) {
            System.out.println(ex);
            ex.printStackTrace();
        }


        return null;
    }

    public String echo(String message) {
        return message;
    }

}
