<!DOCTYPE html>
<html>
<head>
    <title></title>
</head>
<body>
<form action="/eBay/item">
    <input type="text" name="id">
    <button type="submit">Submit</button>
</form>
<%@ page import="edu.ucla.cs.cs144.Bid" %>

<h1> Item: <%=request.getAttribute("Item_ID")%>  <%=request.getAttribute("Item_Name")%> </h1>
<br> Sold By: <%=request.getAttribute("Seller_ID")%> (Rating: <%=request.getAttribute("Seller_Rating")%>)
<br> Location: <%=request.getAttribute("Seller_Location")%>  Country: <%=request.getAttribute("Seller_Country")%>
<p>
Currently: <%=request.getAttribute("Currently")%>
<br>Buy Price: <%=request.getAttribute("Buy Price")%>
<br>First Bid: <%=request.getAttribute("First_bid")%>
<br>Open From: <%=request.getAttribute("Started")%> to <%=request.getAttribute("Ends")%>
<p>
<%=request.getAttribute("Description")%>
<br>
<br>
<h2> Bids for this Item </h2>
<dl>
<% for(Bid bid : request.getAttribute("bids") ) { %>
<dt><b> <%=bid.amount %> </b> by: <%=bid.bidder %> (Rating: <% =bid.bidder_rating %>) </dt>
<dd>Time: <% =bid.timeStr %>
Location: <% =bid.location %> Country: <%=bid.country %> </dd>

<% } %>
</dl>


</body>
</html>
