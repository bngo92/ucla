Bryan Ngo (UID: 503901486)
Rachel Fang (UID: 104001868)

We chose to use mysql indexes for individual numeric fields that we needed to search for, which were end time and buy price.
Because indexes are created by default for primary keys, we did not need to create them for itemid and userid (for bidder and seller).
We used lucene indexes for string fields, for search purposes.
We have itemid to query the database, and then name, category, and description.
The latter three are listed as separate fields for the advanced search.
We also have a content field that aggregates all three so that the basic search can be run over the union of the three.
We did not store any fields besides itemID since the values are all in the database.
