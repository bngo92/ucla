CREATE TABLE User (
    UserID          VARCHAR(400) Primary Key NOT NULL,
    Rating          INT NOT NULL,
    Location        VARCHAR(100),
    Country         VARCHAR(100)

CREATE TABLE Item (
    ItemID          INT Primary Key NOT NULL,
    Name            VARCHAR(400),
    Buy_Price       DECIMAL(8,2),
    First_Bid       DECIMAL(8,2) NOT NULL,
    Started         TIMESTAMP,
    Seller          ForeignKey(UserID) references User.UserID,
    Ends            TIMESTAMP,
    Description     VARCHAR(4000)
);

CREATE TABLE ItemCategory ( 
    ItemID          ForeignKey(ItemID) references Item(ItemID) Primary Key NOT NULL,
    Category        VARCHAR(400) Primary Key NOT NULL
);

CREATE TABLE ItemBid ( 
    ItemID          ForeignKey(ItemID) references Item(ItemID) Primary Key NOT NULL,
    Bidder          ForeignKey(UserID) references User(UserID)Primary Key NOT NULL,
    Time            TIMESTAMP Primary Key NOT NULL,
    Amount          DECIMAL(8,2)
);
