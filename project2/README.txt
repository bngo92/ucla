1 (relational schema design).
User(key:UserID, Rating, Location, Country)
Item(key:ItemID, Name, Buy_Price, First_Bid, Started, Seller, Ends, Description)
ItemCategory(ItemID, Category)
ItemBid(key:ItemID, Bidder, key:Time, Amount)

2.
None; all functional dependencies specify keys

3.
Our relations are BCNF because there are no nontrivial functional dependencies that do not specify keys on any of our relations.
