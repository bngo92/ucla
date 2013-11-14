<!DOCTYPE html>
<html>
<head>
    <title></title>
</head>
<body>
<form action="/eBay/search">
    <input type="text" name="q">
    <input type="hidden" name="numResultsToSkip" value="0">
    <input type="hidden" name="numResultsToReturn" value="20">
    <button type="submit">Submit</button>
</form>
<%@ page import="edu.ucla.cs.cs144.SearchResult" %>
<%
for (SearchResult result : (SearchResult[]) request.getAttribute("results")) {
out.println("<a href=\"/eBay/item?id=" + result.getItemId() + "\">" + result.getName() + "</a>");
}
%>
</body>
</html>
