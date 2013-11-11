package edu.ucla.cs.cs144;

public class AuctionSearchTest {
	public static void main(String[] args1)
	{
		AuctionSearch as = new AuctionSearch();

		String message = "Test message";
		String reply = as.echo(message);
		System.out.println("Reply: " + reply);
		
		String query = "superman";
		SearchResult[] basicResults = as.basicSearch(query, 0, 0);
		System.out.println("Basic Search Query: " + query);
		System.out.println("Received " + basicResults.length + " results");
		//for(SearchResult result : basicResults) {
			//System.out.println(result.getItemId() + ": " + result.getName());
		//}
		
		SearchConstraint constraint =
		    new SearchConstraint(FieldName.BuyPrice, "5.99"); 
		SearchConstraint[] constraints = {constraint};
		SearchResult[] advancedResults = as.advancedSearch(constraints, 0, 20);
		System.out.println("Advanced Search");
		System.out.println("Received " + advancedResults.length + " results");
		//for(SearchResult result : advancedResults) {
			//System.out.println(result.getItemId() + ": " + result.getName());
		//}
		
		String itemId = "1497595357";
		String item = as.getXMLDataForItemId(itemId);
		System.out.println("XML data for ItemId: " + itemId);
		System.out.println(item);

		// Add your own test here

        itemId = "1497497054";
        item = as.getXMLDataForItemId(itemId);
        System.out.println("XML data for ItemId: " + itemId);
        System.out.println(item);

        /*
        query = "kitchenware";
        basicResults = as.basicSearch(query, 0, 0);
        System.out.println("Basic Search Query: " + query);
        System.out.println("Received " + basicResults.length + " results");

        query = "star trek";
        basicResults = as.basicSearch(query, 0, 0);
        System.out.println("Basic Search Query: " + query);
        System.out.println("Received " + basicResults.length + " results");

        SearchConstraint constraint1 = new SearchConstraint(FieldName.ItemName, "pan");
        SearchConstraint constraint2 = new SearchConstraint(FieldName.Category, "kitchenware");
        constraints = new SearchConstraint[]{constraint1, constraint2};
        advancedResults = as.advancedSearch(constraints, 0, 0);
        System.out.println("Advanced Search");
        System.out.println("Received " + advancedResults.length + " results");
        //for(SearchResult result : advancedResults) {
            //System.out.println(result.getItemId() + ": " + result.getName());
        //}

        constraint1 = new SearchConstraint(FieldName.ItemName, "Precious Moments");
        constraint2 = new SearchConstraint(FieldName.SellerId, "waltera317a");
        constraints = new SearchConstraint[]{constraint1, constraint2};
        advancedResults = as.advancedSearch(constraints, 0, 0);
        System.out.println("Advanced Search");
        System.out.println("Received " + advancedResults.length + " results");
        //for(SearchResult result : advancedResults) {
            //System.out.println(result.getItemId() + ": " + result.getName());
        //}

        constraint = new SearchConstraint(FieldName.EndTime, "Dec-14-01 21:00:05");
        constraints = new SearchConstraint[]{constraint};
        advancedResults = as.advancedSearch(constraints, 0, 0);
        System.out.println("Advanced Search");
        System.out.println("Received " + advancedResults.length + " results");
        //for(SearchResult result : advancedResults) {
            //System.out.println(result.getItemId() + ": " + result.getName());
        //}

        */

	}
}
