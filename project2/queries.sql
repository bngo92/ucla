SELECT COUNT(*) FROM User; 

SELECT COUNT(*) 
FROM (SELECT Seller FROM Item) AS I 
	JOIN User
	ON I.Seller=User.UserID
WHERE User.Location="New York";

SELECT COUNT(*) 
FROM 
	(SELECT ItemID, COUNT(Category) 
		FROM ItemCategory
		GROUP BY ItemID
		HAVING COUNT(Category) = 4
	) AS Auctions;
