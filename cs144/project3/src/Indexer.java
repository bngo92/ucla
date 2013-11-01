import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.Document;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;

public class Indexer {
    
    /** Creates a new instance of Indexer */
    public Indexer() {
    }

    private IndexWriter indexWriter = null;

    public IndexWriter getIndexWriter(boolean create) throws IOException {
        if (indexWriter == null) {
            indexWriter = new IndexWriter(System.getenv("LUCENE_INDEX") + "/index-directory", new StandardAnalyzer(), create);
        }
        return indexWriter;
    }

    public void closeIndexWriter() throws IOException {
        if (indexWriter != null) {
            indexWriter.close();
        }
    }

    public void rebuildIndexes() throws IOException {

        getIndexWriter(true);

        Connection conn;
        Statement stmt;

        // create a connection to the database to retrieve Items from MySQL
        try {
            conn = DbManager.getConnection(true);
            stmt = conn.createStatement();
        } catch (SQLException ex) {
            System.out.println(ex);
            return;
        }



        ResultSet rs;
        try {
            rs = stmt.executeQuery("SELECT ItemID, Name, Description FROM Item");
            while(rs.next()) {
                String itemID = rs.getInt("ItemID")+"";
                IndexWriter writer = getIndexWriter(false);
                Document doc = new Document();
                doc.add(new Field("ID", itemID, Field.Store.YES, Field.Index.NO));
                String name = rs.getString("Name");
                doc.add(new Field("Name", name, Field.Store.YES, Field.Index.TOKENIZED));
                String description = rs.getString("Description");
                doc.add(new Field("Description", description, Field.Store.YES, Field.Index.TOKENIZED));

                ResultSet cats = stmt.executeQuery("SELECT Category FROM ItemCategory WHERE itemID="+itemID);
                String category = "";
                while(cats.next()) {
                    category = category +  " " + cats.getString("Category");
                }
                doc.add(new Field("Category", category, Field.Store.NO, Field.Index.TOKENIZED));

                doc.add(new Field("Content", name+category+description, Field.Store.NO, Field.Index.TOKENIZED));

                writer.addDocument(doc);

            }
        } catch (SQLException e) {
            System.out.println(e);
            return;
        }
        // close the database connection
        try {
            conn.close();
        } catch (SQLException ex) {
            System.out.println(ex);
        }
    }

    public static void main(String args[]) {
        Indexer idx = new Indexer();
        try {
            idx.rebuildIndexes();
        } catch (IOException e) {

        }
    }
}
