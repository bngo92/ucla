package edu.ucla.cs.cs144;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class ProxyServlet extends HttpServlet implements Servlet {
       
    public ProxyServlet() {}

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        HttpURLConnection connection = (HttpURLConnection) new URL("http://google.com/complete/search?output=toolbar&q=" + request.getParameter("q")).openConnection();
        InputStream in = new BufferedInputStream(connection.getInputStream());
        response.setContentType("text/xml");
        response.getWriter().write(new Scanner(in).useDelimiter("\\A").next());
        connection.disconnect();
        in.close();
    }
}
