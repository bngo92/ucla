import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
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

	public SearchResult[] basicSearch(String query, int numResultsToSkip, 
			int numResultsToReturn) {
        SearchConstraint[] searchConstraints = new SearchConstraint[1];
        searchConstraints[0] = new SearchConstraint("content", query);
        return advancedSearch(searchConstraints, numResultsToSkip, numResultsToReturn);
    }

	public SearchResult[] advancedSearch(SearchConstraint[] constraints, 
			int numResultsToSkip, int numResultsToReturn) {
        HashSet<SearchResult> results = new HashSet<SearchResult>();
        for (SearchConstraint searchConstraint : constraints) {
            IndexSearcher searcher;
            QueryParser parser;
            Query query;
            Hits hits;
            try {
                searcher = new IndexSearcher(System.getenv("LUCENE_INDEX"));
                parser = new QueryParser(searchConstraint.getFieldName(), new StandardAnalyzer());
                query = parser.parse(searchConstraint.getValue());
                hits = searcher.search(query);
            } catch (IOException e) {
                return null;
            } catch (ParseException e) {
                return null;
            }

            int stop = numResultsToSkip + numResultsToReturn;
            for (int i = numResultsToSkip; i < stop; i++) {
                Document doc;
                try {
                    doc = hits.doc(i);
                } catch (IOException e) {
                    break;
                }
                results.add(new SearchResult(doc.get("ItemID"), doc.get(FieldName.ItemName)));
            }
        }
        return (SearchResult[]) results.toArray();
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
            itemStatement = conn.prepareStatement("SELECT * FROM Items WHERE ItemID=?");
            categoryStatement = conn.prepareStatement("SELECT * FROM ItemCategory WHERE ItemID=?");
            bidStatement = conn.prepareStatement("SELECT * FROM ItemBids WHERE ItemID=?");
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
                rootElement.setAttribute("ItemID", itemId);
                doc.appendChild(rootElement);

                Element name = doc.createElement("Name");
                name.appendChild(doc.createTextNode(rs.getString("Name")));
                rootElement.appendChild(name);

                ResultSet categories = categoryStatement.executeQuery();
                while (categories.next()) {
                    Element category = doc.createElement("Category");
                    category.appendChild(doc.createTextNode(categories.getString("Category")));
                    rootElement.appendChild(category);
                }

                Element currently = doc.createElement("Currently");
                currently.appendChild(doc.createTextNode(rs.getString("Currently")));
                rootElement.appendChild(currently);

                Element firstBid = doc.createElement("FirstBid");
                firstBid.appendChild(doc.createTextNode(rs.getString("FirstBid")));
                rootElement.appendChild(firstBid);

                ArrayList<Element> bidArray = new ArrayList<Element>();
                ResultSet bids = bidStatement.executeQuery();
                while (bids.next()) {
                    Element bid = doc.createElement("Bid");
                    bidArray.add(bid);

                    userStatement.setString(1, bids.getString("UserID"));
                    ResultSet bidderAttributes = userStatement.executeQuery();

                    Element bidder = doc.createElement("Bidder");
                    bidder.setAttribute("UserID", bidderAttributes.getString("UserID"));
                    bidder.setAttribute("Rating", bidderAttributes.getString("Rating"));
                    bid.appendChild(bidder);

                    Element location = doc.createElement("Location");
                    location.appendChild(doc.createTextNode(bidderAttributes.getString("Location")));
                    bidder.appendChild(location);

                    Element country = doc.createElement("Country");
                    country.appendChild(doc.createTextNode(bidderAttributes.getString("Country")));
                    bidder.appendChild(country);

                    Element time = doc.createElement("Time");
                    time.appendChild(doc.createTextNode(bids.getString("Time")));
                    bid.appendChild(time);

                    Element amount = doc.createElement("Amount");
                    amount.appendChild(doc.createTextNode(bids.getString("Amount")));
                    bid.appendChild(amount);
                }

                Element numberOfBids = doc.createElement("Number_of_Bids");
                numberOfBids.appendChild(doc.createTextNode(bidArray.size() + ""));
                rootElement.appendChild(numberOfBids);

                for (Element bid : bidArray)
                    rootElement.appendChild(bid);

                userStatement.setString(1, rs.getString("UserID"));
                ResultSet sellerAttributes = userStatement.executeQuery();

                Element location = doc.createElement("Location");
                location.appendChild(doc.createTextNode(sellerAttributes.getString("Location")));
                rootElement.appendChild(location);

                Element country = doc.createElement("Country");
                country.appendChild(doc.createTextNode(sellerAttributes.getString("Country")));
                rootElement.appendChild(country);

                Element seller = doc.createElement("Seller");
                seller.setAttribute("UserID", sellerAttributes.getString("UserID"));
                seller.setAttribute("Rating", sellerAttributes.getString("Rating"));
                rootElement.appendChild(seller);

                Element description = doc.createElement("Description");
                description.appendChild(doc.createTextNode(rs.getString("Description")));
                rootElement.appendChild(description);
            } else {
                return "";
            }
        } catch (SQLException e) {
            System.out.println(e);
        }

        // close the database connection
        try {
            conn.close();
        } catch (SQLException ex) {
            System.out.println(ex);
        }


		return null;
	}
	
	public String echo(String message) {
		return message;
	}

}
