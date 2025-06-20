/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.quanlynongsan.controller;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.mycompany.quanlynongsan.config.CloudinaryConfig;
import com.mycompany.quanlynongsan.model.ImageProduct;
import com.mycompany.quanlynongsan.model.Product;
import com.mycompany.quanlynongsan.model.User;
import com.mycompany.quanlynongsan.repository.CategoryRepository;
import com.mycompany.quanlynongsan.repository.ImageProductRepository;
import com.mycompany.quanlynongsan.repository.ProductRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 *
 * @author joyboy
 */
@MultipartConfig
@WebServlet(urlPatterns = "/secured/user/edit-product")
public class EditProductServlet extends HttpServlet {

    private ProductRepository productRepository = new ProductRepository();
    
    private CategoryRepository categoryRepository = new CategoryRepository();
    
    private ImageProductRepository imageProductRepository = new ImageProductRepository();
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int productId = Integer.parseInt(req.getParameter("id"));
        Product product = productRepository.findById(productId); // lấy sản phẩm
        List<com.mycompany.quanlynongsan.model.Category> allCategories = categoryRepository.findAll(); // lấy toàn bộ danh mục
        List<com.mycompany.quanlynongsan.model.Category> selectedCategoryIds = categoryRepository.findCategoriesByProductId(productId); // lấy danh mục đã chọn
        List<ImageProduct> imageProducts = imageProductRepository.findAllByProductId(productId);

        req.setAttribute("product", product);
        req.setAttribute("allCategories", allCategories);
        req.setAttribute("selectedCategoryIds", selectedCategoryIds);
        List<String> imageUrls = new ArrayList<>();
        for(ImageProduct imageProduct : imageProducts){
            imageUrls.add(imageProduct.getUrlImage());
        }
            req.setAttribute("imageUrls", imageUrls);
        req.getRequestDispatcher("/user/edit-product.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession();
        User user = (User) session.getAttribute("user");
            req.setCharacterEncoding("UTF-8");
            resp.setContentType("text/html;charset=UTF-8");

            try {
                // 1. Lấy thông tin sản phẩm từ request
                String name = req.getParameter("name");
                String description = req.getParameter("description");
                BigDecimal price = new BigDecimal(req.getParameter("price"));
                Integer quantity = Integer.parseInt(req.getParameter("quantity"));
                String status = req.getParameter("status");
                Boolean isSell = Boolean.parseBoolean(req.getParameter("is_sell"));
                Boolean isBrowse = Boolean.parseBoolean(req.getParameter("is_browse"));
                String placeOfManufacture = req.getParameter("placeOfManufacture");
                String[] categoryIdsParam = req.getParameterValues("category_ids");
                Integer productId = Integer.valueOf(req.getParameter("product_id"));
                Integer[] categories = null;

                if (categoryIdsParam != null) {
                    categories = new Integer[categoryIdsParam.length];
                    for (int i = 0; i < categoryIdsParam.length; i++) {
                        categories[i] = Integer.parseInt(categoryIdsParam[i]);
                    }
                }
                Boolean isActive = Boolean.parseBoolean(req.getParameter("is_active"));
                Integer holderId = user.getUserId();
                Date createdDate = new Date();

                Product product = new Product(productId, name, description, price, quantity, status, isSell, isBrowse, placeOfManufacture, isActive, holderId, createdDate);

                // 2. Xử lý upload ảnh
                Collection<Part> fileParts = req.getParts();
                List<String> imageUrls = new ArrayList<>();
                Cloudinary cloudinary = CloudinaryConfig.getInstance();

                for (Part filePart : fileParts) {
                    String fileName = filePart.getSubmittedFileName();
                    if (fileName != null && !fileName.isEmpty()) {
                        // Tạo file tạm
                        File tempFile = File.createTempFile("upload-", fileName);
                        try (InputStream fileContent = filePart.getInputStream();
                             OutputStream out = new FileOutputStream(tempFile)) {
                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = fileContent.read(buffer)) != -1) {
                                out.write(buffer, 0, bytesRead);
                            }
                        }

                        // Upload lên Cloudinary
                        try {
                            Map uploadResult = cloudinary.uploader().upload(tempFile, ObjectUtils.emptyMap());
                            String publicUrl = (String) uploadResult.get("url");
                            imageUrls.add(publicUrl);
                        } catch (Exception e) {
                            e.printStackTrace();
                            req.setAttribute("error", "Upload failed: " + e.getMessage());
                            req.getRequestDispatcher("/error.jsp").forward(req, resp);
                            return;
                        } finally {
                            tempFile.delete(); // Xóa file tạm
                        }
                    }
                }

                // 3. Lưu vào database
                ProductRepository repo = new ProductRepository();
                boolean success = repo.update(product, imageUrls, categories);

                if (success) {
                    resp.sendRedirect(req.getContextPath() + "/secured/user/my-products"); // Ví dụ chuyển hướng về trang danh sách sản phẩm
                } else {
                    req.setAttribute("error", "Failed to create product.");
                    req.getRequestDispatcher("/error.jsp").forward(req, resp);
                }
            } catch (Exception e) {
                e.printStackTrace();
                req.setAttribute("error", "Server error: " + e.getMessage());
                req.getRequestDispatcher("/error.jsp").forward(req, resp);
            }
    }
    
}
