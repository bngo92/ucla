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
<%= request.getAttribute("result") %>
</body>
</html>
