<!DOCTYPE html>
<html>
<head>
    <title></title>
    <script type="text/javascript"
        src="http://maps.google.com/maps/api/js?sensor=false">
    </script>
    <script type="text/javascript">
      function initialize() {
 	var latlng = new google.maps.LatLng(34.063509, -118.44541);
        var myOptions = { 
	  zoom: 0,
	  center:latlng,
	  mapTypeId: google.maps.MapTypeId.ROADMAP
        }; 
	var map = new google.maps.Map(document.getElementById("map_canvas"), myOptions);
	
	var geocoder = new google.maps.Geocoder();
	var address = "<%=request.getAttribute("address") %>";
	
	geocoder.geocode({'address':address }, function(results,status) {
          if(status==google.maps.GeocoderStatus.OK) {
  	    map.setCenter(results[0].geometry.location);
	    map.setZoom(8);
     	  }
	} );

      }
      
    </script>
</head>
<body onload="initialize()">
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
<br>Buy Price: <%=request.getAttribute("Buy_Price")%>
<br>First Bid: <%=request.getAttribute("First_bid")%>
<br>Open From: <%=request.getAttribute("Started")%> to <%=request.getAttribute("Ends")%>
<p>
<%=request.getAttribute("Description")%>
<br>
<br>
<div id="map_canvas" style="width:300px; height:300px"></div>
<h2> Bids for this Item </h2>
<dl>
<%
for(Bid bid : (Bid[]) request.getAttribute("bids")) {
out.println(String.format("<dt><b>%s</b> by: %s (Rating: %s)</dt>", bid.amount, bid.bidder, bid.bidder_rating));
out.println(String.format("<dd>Time: %s Location: %s Country: %s</dd>", bid.timeStr, bid.location, bid.country));
}
%>
</dl>


</body>
</html>
