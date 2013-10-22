# Num users in database
SELECT COUNT(*) FROM User; 

#num sellers where location is New York
SELECT COUNT(*) 
FROM (SELECT DISTINCT Seller FROM Item) AS I 
    INNER JOIN User
    ON I.Seller=User.UserID
WHERE binary User.Location="New York";

# auctions with four categories
SELECT COUNT(*) 
FROM 
    (
        SELECT ItemID, COUNT(Category) 
        FROM ItemCategory
        GROUP BY ItemID
        HAVING COUNT(Category) = 4
    ) AS Auctions;

# ID with highest bid, unsold 
SELECT DISTINCT ItemID
FROM
    (
        SELECT MAX(Amount) AS Amount
        FROM Item INNER JOIN ItemBid 
        USING (ItemID)
        WHERE Started <  '2001-12-20 00:00:01' AND Ends > '2001-12-20 00:00:01'
    ) AS Highest_Bid 
    INNER JOIN ItemBid 
    USING (Amount);

#num sellers
SELECT COUNT(*) 
FROM ( SELECT DISTINCT seller FROM Item INNER JOIN User
ON Item.Seller = User.UserID 
WHERE User.Rating > 1000 ) AS foo;

#num bidders and sellers 
SELECT COUNT(*) 
FROM (SELECT DISTINCT Seller FROM Item) AS i  INNER JOIN (SELECT DISTINCT Bidder FROM ItemBid) AS b
ON i.Seller=b.Bidder;

#num categories with at least one item with a bid of more than $100
SELECT COUNT(*)
FROM ( 
SELECT Category 
FROM ItemCategory INNER JOIN ItemBid 
USING (ItemID)
GROUP BY Category
Having MAX(Amount)>100.00 ) AS foo;

