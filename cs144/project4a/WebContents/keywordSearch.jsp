<!DOCTYPE html>
<html>
<head>
    <title></title>
    <meta name="viewport" content="width=device-width">
</head>
<style>
    @media (max-width: 480px) {
        html, body {
            font-size: 24px;
        }
    }
</style>

<body>
<script type="text/javascript" src="suggest.js"></script>
<script type="text/javascript">
    window.onload = function () {
        var oTextbox = new AutoSuggestControl(document.getElementById("suggest"), new Suggestions());
    }
</script>
<style>
    div.suggestions {
    -moz-box-sizing: border-box;
    box-sizing: border-box;
    border: 1px solid black;
    position: absolute;
    background-color: white;
    }

    div.suggestions div {
    cursor: default;
    padding: 0px 3px;
    }

    div.suggestions div.current {
    background-color: #3366cc;
    color: white;
    }
</style>
<form action="/eBay/search">
    <input type="text" id="suggest" name="q">
    <input type="hidden" name="numResultsToSkip" value="0">
    <input type="hidden" name="numResultsToReturn" value="20">
    <button type="submit">Submit</button>
</form>
<%@ page import="edu.ucla.cs.cs144.SearchResult" %>
<%
for (SearchResult result : (SearchResult[]) request.getAttribute("results")) {
out.println("<a href=\"/eBay/item?id=" + result.getItemId() + "\">" + result.getName() + "</a><br>");
}
%>
</body>
</html>
