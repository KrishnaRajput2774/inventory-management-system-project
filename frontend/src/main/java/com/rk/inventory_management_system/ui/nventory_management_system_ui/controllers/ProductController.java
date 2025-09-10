package com.rk.inventory_management_system.ui.nventory_management_system_ui.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.Clients.CategoriesClient;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.Clients.OrdersClient;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.Clients.ProductsClient;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.Clients.SuppliersClient;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.*;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.productDtos.ProductResponseDto;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.supplierDtos.SupplierResponseDto;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final SuppliersClient suppliersClient;
    private final CategoriesClient categoriesClient;
    private final ProductsClient productsClient;
    private final OrdersClient ordersClient;

    @GetMapping
    public String list(Model model) {
        try {
            // Fetch all products - no server-side filtering needed anymore
            List<ProductDto> allProducts = productsClient.findAll();

            // Get categories for any future use (not needed for current filtering but keeping for consistency)
            List<ProductCategoryDto> categories = categoriesClient.findAll();

            // Extract unique suppliers from products
            List<SupplierDto> suppliers = allProducts.stream()
                    .map(ProductDto::getSupplier)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

            // Calculate statistics based on all products
            long totalProducts = allProducts.size();
            long inStockProducts = allProducts.stream()
                    .filter(p -> p.getStockQuantity() != null && p.getStockQuantity() >= 50)
                    .count();
            long lowStockProducts = allProducts.stream()
                    .filter(p -> p.getStockQuantity() != null &&
                            p.getStockQuantity() >= 10 && p.getStockQuantity() < 50)
                    .count();
            long criticalStockProducts = allProducts.stream()
                    .filter(p -> p.getStockQuantity() != null && p.getStockQuantity() < 10)
                    .count();

            // Sort products by name by default (client-side filtering will handle re-sorting)
            allProducts.sort(Comparator.comparing(ProductDto::getName, String.CASE_INSENSITIVE_ORDER));

            // Add all data to model
            model.addAttribute("products", allProducts);
            model.addAttribute("categories", categories);
            model.addAttribute("suppliers", suppliers);
            model.addAttribute("totalProducts", totalProducts);
            model.addAttribute("inStockProducts", inStockProducts);
            model.addAttribute("lowStockProducts", lowStockProducts);
            model.addAttribute("criticalStockProducts", criticalStockProducts);

            // Add some metadata for the frontend
            model.addAttribute("lastUpdated", LocalDateTime.now());

            return "products/list";

        } catch (Exception e) {
            // Log the error
            log.error("Error fetching products list", e);

            // Add error message to model
            model.addAttribute("errorMessage", "Unable to load products. Please try again later.");

            // Return with empty data to prevent template errors
            model.addAttribute("products", Collections.emptyList());
            model.addAttribute("categories", Collections.emptyList());
            model.addAttribute("suppliers", Collections.emptyList());
            model.addAttribute("totalProducts", 0);
            model.addAttribute("inStockProducts", 0);
            model.addAttribute("lowStockProducts", 0);
            model.addAttribute("criticalStockProducts", 0);

            return "products/list";
        }
    }

    //For real time updates
    @GetMapping("/api/products/stats")
    @ResponseBody
    public Map<String, Long> getProductStats() {
        try {
            List<ProductDto> allProducts = productsClient.findAll();

            Map<String, Long> stats = new HashMap<>();
            stats.put("total", (long) allProducts.size());
            stats.put("inStock", allProducts.stream()
                    .filter(p -> p.getStockQuantity() != null && p.getStockQuantity() >= 50)
                    .count());
            stats.put("lowStock", allProducts.stream()
                    .filter(p -> p.getStockQuantity() != null &&
                            p.getStockQuantity() >= 10 && p.getStockQuantity() < 50)
                    .count());
            stats.put("critical", allProducts.stream()
                    .filter(p -> p.getStockQuantity() != null && p.getStockQuantity() < 10)
                    .count());

            return stats;
        } catch (Exception e) {
            log.error("Error fetching product stats", e);
            return Map.of("error", 1L);
        }
    }
    @GetMapping("/create")
    public String createForm(Model model) {

        model.addAttribute("product", new ProductDto());
        model.addAttribute("suppliers", suppliersClient.findAll());
        model.addAttribute("categories", categoriesClient.findAll());
        return "products/create";
    }

    @PostMapping("/create")
    public String create(
            @Valid @ModelAttribute("product") ProductDto product,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        if (result.hasErrors()) {
            log.error("Validation errors: {}", result.getAllErrors());
            model.addAttribute("product", product);
            model.addAttribute("suppliers", suppliersClient.findAll());
            model.addAttribute("categories", categoriesClient.findAll());
            return "products/create";
        }

        try {
            log.info("Attempting to create product: {}", product.getName());
            ProductResponseDto created = productsClient.create(product);

            // Use redirectAttributes for redirect scenarios
            redirectAttributes.addFlashAttribute("successMessage",
                    "Product '" + (created != null ? created.getName() : product.getName()) + "' created successfully!");
            return "redirect:/products";

        } catch (Exception exception) {
            log.error("Error creating product: {}", exception.getMessage(), exception);

            // Check if it's a response parsing issue (product likely created successfully)
            if (exception.getMessage().contains("Error while extracting response")) {
                log.warn("Product likely created successfully, but response parsing failed");
                redirectAttributes.addFlashAttribute("warningMessage",
                        "Product '" + product.getName() + "' appears to have been created, but there was a response issue. Please check the products list.");
                return "redirect:/products";
            }

            // For other errors, use model attributes (not redirectAttributes) since we're not redirecting
            model.addAttribute("errorMessage", "Failed to create product: " + exception.getMessage());
            model.addAttribute("product", product);
            model.addAttribute("suppliers", suppliersClient.findAll());
            model.addAttribute("categories", categoriesClient.findAll());
            return "products/create";
        }
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model) {

        model.addAttribute("product",productsClient.findById(id));
        return "products/detail";
    }

    @GetMapping("/{id}/stock")
    public String stock(@PathVariable Long id, Model model) {
        model.addAttribute("product",productsClient.findById(id));
        model.addAttribute("stockResponse",productsClient.findProductStockById(id));
        return "products/stock";
    }

//
@PostMapping("{id}/stock/increase")
public String increaseStock(@PathVariable Long id,
                            @RequestParam Integer quantity,
                            RedirectAttributes redirectAttributes) {
    try {
        productsClient.increaseStock(id, quantity);
        redirectAttributes.addFlashAttribute("successMessage",
                "Stock increased by '" + quantity + "' units");
    } catch (Exception exception) {
        log.error("Error increasing stock", exception);
        redirectAttributes.addFlashAttribute("errorMessage", "Failed to increase the stock");
    }

    return "redirect:/products/" + id + "/stock";
}

    @PostMapping("{id}/stock/reduce")
    public String reduceStock(@PathVariable Long id,
                              @RequestParam Integer quantity,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        try {
            List<ProductDto> affectedProducts = productsClient.reduceStock(id, quantity);

            if (affectedProducts != null && !affectedProducts.isEmpty()) {
                // Create a detailed success message showing which suppliers were affected
                StringBuilder messageBuilder = new StringBuilder();
                messageBuilder.append("Stock reduced by '").append(quantity).append("' units from ");

                if (affectedProducts.size() == 1) {
                    messageBuilder.append("supplier: ").append(affectedProducts.get(0).getSupplier().getName());
                } else {
                    messageBuilder.append(affectedProducts.size()).append(" suppliers: ");
                    List<String> supplierNames = affectedProducts.stream()
                            .map(productDto -> productDto.getSupplier().getName())
                            .distinct()
                            .collect(Collectors.toList());
                    messageBuilder.append(String.join(", ", supplierNames));
                }

                redirectAttributes.addFlashAttribute("successMessage", messageBuilder.toString());

                // Add the affected products as flash attribute for additional display if needed
                redirectAttributes.addFlashAttribute("affectedProducts", affectedProducts);

            } else {
                redirectAttributes.addFlashAttribute("warningMessage",
                        "Stock reduction completed, but no products were affected");
            }

        } catch (Exception exception) {
            log.error("Error reducing stock", exception);

            // Check if it's an insufficient stock error
            if (exception.getMessage() != null &&
                    (exception.getMessage().contains("insufficient") ||
                            exception.getMessage().contains("not enough"))) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Insufficient stock available. Cannot reduce by '" + quantity + "' units");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Failed to reduce the stock");
            }
        }

        return  "redirect:/products/" + id + "/stock";
    }

}
