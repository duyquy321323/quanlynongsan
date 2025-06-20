/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.quanlynongsan.controller;

import com.mycompany.quanlynongsan.model.User;
import com.mycompany.quanlynongsan.repository.OrderProductRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author joyboy
 */

@WebServlet(urlPatterns = "/secured/user/my-order")
public class MyOrderServlet extends HttpServlet{
    
    private OrderProductRepository orderProductRepository = new OrderProductRepository();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession();
        User user = (User) session.getAttribute("user");
        OrderProductRepository repo = new OrderProductRepository();
        List<OrderProductRepository.OrderSummary> orders = repo.getOrderSummariesByUserId(user.getUserId());
        req.setAttribute("orders", orders);
        req.getRequestDispatcher("/user/my-order.jsp").forward(req, resp);
    }
    
}
