package edu.ucla.cs.cs144;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PayServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null)
            return;
        request.setAttribute("ItemID", session.getAttribute("ItemID"));
        request.setAttribute("ItemName", session.getAttribute("ItemName"));
        request.setAttribute("Buy_Price", session.getAttribute("Buy_Price"));
        request.getRequestDispatcher("/pay.jsp").forward(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null)
            return;
        request.setAttribute("ItemID", session.getAttribute("ItemID"));
        request.setAttribute("ItemName", session.getAttribute("ItemName"));
        request.setAttribute("Buy_Price", session.getAttribute("Buy_Price"));
        request.setAttribute("Credit Card", request.getParameter("Credit Card"));
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        request.setAttribute("Time", dateFormat.format(new Date()));
        request.getRequestDispatcher("/confirm.jsp").forward(request, response);
    }

}
