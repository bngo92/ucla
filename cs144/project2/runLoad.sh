#!/bin/bash

# Run the drop.sql batch file to drop existing tables
# Inside the drop.sql, you should check whether the table exists. Drop them ONLY if they exists.
mysql CS144 < drop.sql

# Run the create.sql batch file to create the database and tables
mysql CS144 < create.sql

# Compile and run the parser to generate the appropriate load files
ant run-all

# If the Java code does not handle duplicate removal, do this now
sort -u Item.dat -o Item.dat
sort -u ItemBid.dat -o ItemBid.dat
sort -u ItemCategory.dat -o ItemCategory.dat
sort -u User.dat -o User.dat

# Run the load.sql batch file to load the data
mysql CS144 < load.sql

# Remove all temporary files
ant clean
rm *.dat
