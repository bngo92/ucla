<!DOCTYPE html>
<html>
<head>
    <title></title>
</head>
<body>
<h1>Credit Card Input Page</h1>
<p>ItemID: <%=request.getAttribute("ItemID")%></p>
<p>ItemName: <%=request.getAttribute("ItemName")%></p>
<p>Buy_Price: <%=request.getAttribute("Buy_Price")%></p>
<form method="post">
    Credit Card:
    <input type="text" name="Credit Card">
    <button type="submit">Submit</button>
</form>
</body>
</html>
