CREATE TABLE User (
    UserID          VARCHAR(400) Primary Key NOT NULL,
    Rating          INT NOT NULL,
    Location        VARCHAR(100),
    Country         VARCHAR(100)
);

CREATE TABLE Item (
    ItemID          INT Primary Key NOT NULL,
    Name            VARCHAR(400),
    Buy_Price       DECIMAL(8,2),
    First_Bid       DECIMAL(8,2) NOT NULL,
    Started         TIMESTAMP,
    Seller          VARCHAR(400),
    Ends            TIMESTAMP,
    Description     VARCHAR(4000)
);

CREATE TABLE ItemCategory ( 
    ItemID          VARCHAR(400) NOT NULL,
    Category        VARCHAR(400) NOT NULL,
    primary key (ItemID, Category)
);

CREATE TABLE ItemBid ( 
    ItemID          VARCHAR(400) NOT NULL,
    Bidder          VARCHAR(400) NOT NULL,
    Time            TIMESTAMP NOT NULL,
    Amount          DECIMAL(8,2) NOT NULL,
    primary key (ItemID, Time)
);
