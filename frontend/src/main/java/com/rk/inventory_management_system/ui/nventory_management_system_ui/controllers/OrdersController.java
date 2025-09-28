
package com.rk.inventory_management_system.ui.nventory_management_system_ui.controllers;

import com.rk.inventory_management_system.ui.nventory_management_system_ui.Clients.*;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.*;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.enums.OrderStatus;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.enums.OrderType;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.productDtos.ProductResponseDto;
import jakarta.servlet.http.HttpServletRequest;
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
@RequestMapping("/orders")
@Slf4j
@RequiredArgsConstructor
public class OrdersController {

    private final OrdersClient ordersClient;
    private final CustomersClient customersClient;
    private final ProductsClient productsClient;
    private final SuppliersClient suppliersClient;
    private final OrderItemClient orderItemClient; // Add OrderItemClient
    private final CategoriesClient categoriesClient;

    @GetMapping
    public String list(@RequestParam(required = false) String search,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String type,
                       Model model) {
        try {
            List<OrderDto> allOrders = ordersClient.findAll();

            // Apply filters
            List<OrderDto> filteredOrders = allOrders.stream()
                    .filter(order -> {
                        // Search filter
                        if (search != null && !search.trim().isEmpty()) {
                            String searchLower = search.toLowerCase();
                            return order.getOrderId().toString().contains(search) ||
                                    (order.getCustomer() != null && order.getCustomer().getName() != null &&
                                            order.getCustomer().getName().toLowerCase().contains(searchLower)) ||
                                    (order.getCustomer() != null && order.getCustomer().getEmail() != null &&
                                            order.getCustomer().getEmail().toLowerCase().contains(searchLower));
                        }
                        return true;
                    })
                    .filter(order -> {
                        // Status filter
                        if (status != null && !status.trim().isEmpty()) {
                            return order.getOrderStatus() != null && order.getOrderStatus().name().equals(status);
                        }
                        return true;
                    })
                    .filter(order -> {
                        // Type filter
                        if (type != null && !type.trim().isEmpty()) {
                            return order.getOrderType() != null && order.getOrderType().name().equals(type);
                        }
                        return true;
                    })
                    .collect(Collectors.toList());

            // Sort by order date (newest first)
            filteredOrders.sort((o1, o2) -> {
                if (o1.getCreatedAt() == null && o2.getCreatedAt() == null) return 0;
                if (o1.getCreatedAt() == null) return 1;
                if (o2.getCreatedAt() == null) return -1;
                return o2.getCreatedAt().compareTo(o1.getCreatedAt());
            });

            // Add all filtered orders to model
            model.addAttribute("orders", filteredOrders);
            model.addAttribute("totalOrders", filteredOrders.size());

            // Calculate counts for statistics cards using all orders (not filtered for accurate stats)
            long pendingCount = allOrders.stream()
                    .filter(o -> o.getOrderStatus() != null && o.getOrderStatus().name().equals("CREATED"))
                    .count();

            long processingCount = allOrders.stream()
                    .filter(o -> o.getOrderStatus() != null && o.getOrderStatus().name().equals("PROCESSING"))
                    .count();

            long completedCount = allOrders.stream()
                    .filter(o -> o.getOrderStatus() != null && o.getOrderStatus().name().equals("COMPLETED"))
                    .count();

            model.addAttribute("pendingCount", pendingCount);
            model.addAttribute("processingCount", processingCount);
            model.addAttribute("completedCount", completedCount);

            // Enhanced Analytics
            calculateAndAddAnalytics(allOrders, model);

        } catch (Exception e) {
            log.error("Error loading orders", e);
            model.addAttribute("orders", Collections.emptyList());
            model.addAttribute("errorMessage", "Failed to load orders");

            // Set default values for analytics
            setDefaultAnalytics(model);
        }

        return "orders/list";
    }

    private void calculateAndAddAnalytics(List<OrderDto> orders, Model model) {
        try {
            // Calculate total revenue from completed orders
            double totalRevenue = orders.stream()
                    .filter(o -> o.getOrderStatus() != null &&
                            o.getOrderStatus().name().equals("COMPLETED") &&
                            o.getTotalPrice() != null)
                    .mapToDouble(OrderDto::getTotalPrice)
                    .sum();
            model.addAttribute("totalRevenue", totalRevenue);

            // Calculate average order value
            double avgOrderValue = orders.isEmpty() ? 0.0 :
                    orders.stream()
                            .filter(o -> o.getTotalPrice() != null)
                            .mapToDouble(OrderDto::getTotalPrice)
                            .average()
                            .orElse(0.0);
            model.addAttribute("avgOrderValue", avgOrderValue);

            // Count orders this month
            LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            long ordersThisMonth = orders.stream()
                    .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isAfter(startOfMonth))
                    .count();
            model.addAttribute("ordersThisMonth", ordersThisMonth);

            // Calculate processing efficiency
            long totalOrdersProcessed = orders.stream()
                    .filter(o -> o.getOrderStatus() != null &&
                            (o.getOrderStatus().name().equals("COMPLETED") ||
                                    o.getOrderStatus().name().equals("PROCESSING")))
                    .count();

            double processingEfficiency = orders.isEmpty() ? 0.0 :
                    (double) totalOrdersProcessed / orders.size() * 100;
            model.addAttribute("processingEfficiency", processingEfficiency);

            // Revenue by order type
            Map<String, Double> revenueByType = orders.stream()
                    .filter(o -> o.getOrderStatus() != null &&
                            o.getOrderStatus().name().equals("COMPLETED") &&
                            o.getTotalPrice() != null &&
                            o.getOrderType() != null)
                    .collect(Collectors.groupingBy(
                            o -> o.getOrderType().name(),
                            Collectors.summingDouble(OrderDto::getTotalPrice)
                    ));
            model.addAttribute("revenueByType", revenueByType);

            // Most valuable customers
            List<CustomerRevenueDto> topCustomers = orders.stream()
                    .filter(o -> o.getOrderStatus() != null &&
                            o.getOrderStatus().name().equals("COMPLETED") &&
                            o.getTotalPrice() != null &&
                            o.getCustomer() != null)
                    .collect(Collectors.groupingBy(
                            o -> o.getCustomer().getName(),
                            Collectors.summingDouble(OrderDto::getTotalPrice)
                    ))
                    .entrySet().stream()
                    .map(entry -> new CustomerRevenueDto(entry.getKey(), entry.getValue()))
                    .sorted((a, b) -> Double.compare(b.getRevenue(), a.getRevenue()))
                    .limit(5)
                    .collect(Collectors.toList());
            model.addAttribute("topCustomers", topCustomers);

            log.info("Successfully calculated analytics - Total Revenue: {}, Avg Order Value: {}, Orders This Month: {}",
                    totalRevenue, avgOrderValue, ordersThisMonth);

        } catch (Exception e) {
            log.warn("Error calculating analytics", e);
            setDefaultAnalytics(model);
        }
    }

    private void setDefaultAnalytics(Model model) {
        model.addAttribute("totalOrders", 0);
        model.addAttribute("pendingCount", 0);
        model.addAttribute("processingCount", 0);
        model.addAttribute("completedCount", 0);
        model.addAttribute("totalRevenue", 0.0);
        model.addAttribute("avgOrderValue", 0.0);
        model.addAttribute("ordersThisMonth", 0);
        model.addAttribute("processingEfficiency", 0.0);
        model.addAttribute("revenueByType", Collections.emptyMap());
        model.addAttribute("topCustomers", Collections.emptyList());
    }

    public static class CustomerRevenueDto {
        private String customerName;
        private Double revenue;

        public CustomerRevenueDto(String customerName, Double revenue) {
            this.customerName = customerName;
            this.revenue = revenue;
        }

        // Getters
        public String getCustomerName() { return customerName; }
        public Double getRevenue() { return revenue; }

        // Setters
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public void setRevenue(Double revenue) { this.revenue = revenue; }
    }


    @GetMapping("/customer/{customerId}")
    public String listByCustomer(@PathVariable Long customerId, Model model) {
        model.addAttribute("customer", customersClient.findCustomerById(customerId));
        model.addAttribute("suppliers", suppliersClient.findAll());
        model.addAttribute("orders", ordersClient.findAllOrdersOfCustomer(customerId));
        return "orders/list-by-customer";
    }

    // Sales Order Routes
    @GetMapping("/sales/create")
    public String createSalesOrderForm(@RequestParam(required = false) Long customerId, Model model) {
        OrderDto order = new OrderDto();
        order.setOrderType(OrderType.SALE); // Set the order type

        model.addAttribute("order", order);
        model.addAttribute("products", productsClient.findAll());
        model.addAttribute("customers", customersClient.findAll());
        model.addAttribute("categories", categoriesClient.findAll());

        if (customerId != null) {
            model.addAttribute("selectedCustomerId", customerId);
        }

        return "orders/sales/create";
    }

    @PostMapping("/sales/create")
    public String createSalesOrder(@ModelAttribute OrderDto order,
                                   Model model,
                                   BindingResult result,
                                   RedirectAttributes redirectAttributes) {

        // Ensure order type is set to SALE
        order.setOrderType(OrderType.SALE);

        if (result.hasErrors()) {
            model.addAttribute("order", order);
            model.addAttribute("customers", customersClient.findAll());
            model.addAttribute("products", productsClient.findAll());
            model.addAttribute("categories", categoriesClient.findAll());
            return "orders/sales/create";
        }

        try {
            OrderDto created = ordersClient.create(order);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Sales Order #" + created.getOrderId() + " created successfully");
            return "redirect:/orders/" + created.getOrderId();

        } catch (Exception exception) {
            log.error("Error creating sales order", exception);

            // Handle stock errors specifically
            if (exception.getMessage() != null && exception.getMessage().contains("stock")) {
                model.addAttribute("stockError", exception.getMessage());
            } else {
                result.rejectValue("orderType", "error.order", "Failed to create sales order: " + exception.getMessage());
            }

            model.addAttribute("order", order);
            model.addAttribute("customers", customersClient.findAll());
            model.addAttribute("products", productsClient.findAll());
            model.addAttribute("categories", categoriesClient.findAll());
            return "orders/sales/create";
        }
    }




    // Purchase Order Routes
    @GetMapping("/purchase/create")
    public String createPurchaseOrderForm(@RequestParam(required = false) Long supplierId, Model model) {
        OrderDto order = new OrderDto();
        order.setOrderType(OrderType.PURCHASE); // Set the order type

        model.addAttribute("order", order);
        model.addAttribute("products", productsClient.findAll());
        model.addAttribute("suppliers", suppliersClient.findAll());
        model.addAttribute("categories", categoriesClient.findAll());

        if (supplierId != null) {
            model.addAttribute("selectedSupplierId", supplierId);
        }

        return "orders/purchase/create";
    }

    @PostMapping("/purchase/create")
    public String createPurchaseOrder(@ModelAttribute OrderDto order,
                                      Model model,
                                      BindingResult result,
                                      RedirectAttributes redirectAttributes) {

        // Ensure order type is set to PURCHASE
        order.setOrderType(OrderType.PURCHASE);

        if (result.hasErrors()) {
            model.addAttribute("order", order);
            model.addAttribute("suppliers", suppliersClient.findAll());
            model.addAttribute("products", productsClient.findAll());
            model.addAttribute("categories", categoriesClient.findAll());
            return "orders/purchase/create";
        }

        try {
            OrderDto created = ordersClient.create(order);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Purchase Order #" + created.getOrderId() + " created successfully");
            // CHANGED: Redirect to purchase order detail page instead of generic detail page
            return "redirect:/orders/purchase/" + created.getOrderId();

        } catch (Exception exception) {
            log.error("Error creating purchase order", exception);
            result.rejectValue("orderType", "error.order", "Failed to create purchase order: " + exception.getMessage());
            model.addAttribute("order", order);
            model.addAttribute("suppliers", suppliersClient.findAll());
            model.addAttribute("products", productsClient.findAll());
            model.addAttribute("categories", categoriesClient.findAll());
            return "orders/purchase/create";
        }
    }

    @GetMapping("/purchase/{id}")
    public String purchaseOrderDetail(@PathVariable Long id, Model model) {
        try {
            OrderDto orderDto = ordersClient.findOrderById(id);
            log.info("OrderID: {}",orderDto.getOrderId());
            // Verify this is actually a purchase order
            if (orderDto.getOrderType() != OrderType.PURCHASE) {
                return "redirect:/orders/" + id; // Redirect to regular detail page if not a purchase order
            }

            // Calculate purchase-specific analytics
            PurchaseAnalyticsResult analytics = calculatePurchaseAnalytics(orderDto);

            // Add data to model
            model.addAttribute("order", orderDto);
            model.addAttribute("totalOrderValue", analytics.getTotalOrderValue());
            model.addAttribute("totalItems", analytics.getTotalItems());
            model.addAttribute("totalQuantity", analytics.getTotalQuantity());
            model.addAttribute("averageItemCost", analytics.getAverageItemCost());
            model.addAttribute("lowStockItems", analytics.getLowStockItems());
            model.addAttribute("categoryBreakdown", analytics.getCategoryBreakdown());

            log.info("Purchase Order {} Details - Total Value: ₹{}, Items: {}, Supplier: {}",
                    id, analytics.getTotalOrderValue(), analytics.getTotalItems(),
                    orderDto.getSupplier() != null ? orderDto.getSupplier().getName() : "N/A");

        } catch (Exception e) {
            log.error("Error loading purchase order details for ID: {}", id, e);
            model.addAttribute("errorMessage", "Failed to load purchase order details");
            setDefaultPurchaseAnalytics(model);
        }

        return "orders/purchase/detail"; // Return the purchase order detail template
    }

    private PurchaseAnalyticsResult calculatePurchaseAnalytics(OrderDto orderDto) {
        PurchaseAnalyticsResult result = new PurchaseAnalyticsResult();

        double totalOrderValue = orderDto.getTotalPrice();
        int totalItems = orderDto.getOrderItems().size();
        int totalQuantity = orderDto.getOrderItems().stream()
                .mapToInt(OrderItemDto::getQuantity)
                .sum();
        double averageItemCost = totalItems > 0 ? totalOrderValue / totalItems : 0.0;

        // Find low stock items
        List<String> lowStockItems = orderDto.getOrderItems().stream()
                .filter(item -> item.getProductDto().getStockQuantity() <= item.getProductDto().getLowStockThreshold())
                .map(item -> item.getProductDto().getName())
                .collect(Collectors.toList());

        // Category breakdown
        Map<String, Integer> categoryBreakdown = orderDto.getOrderItems().stream()
                .collect(Collectors.groupingBy(
                        item -> item.getProductDto().getCategory() != null ?
                                item.getProductDto().getCategory().getName() : "Uncategorized",
                        Collectors.summingInt(OrderItemDto::getQuantity)
                ));

        result.setTotalOrderValue(totalOrderValue);
        result.setTotalItems(totalItems);
        result.setTotalQuantity(totalQuantity);
        result.setAverageItemCost(averageItemCost);
        result.setLowStockItems(lowStockItems);
        result.setCategoryBreakdown(categoryBreakdown);

        return result;
    }

    private void setDefaultPurchaseAnalytics(Model model) {
        model.addAttribute("totalOrderValue", 0.0);
        model.addAttribute("totalItems", 0);
        model.addAttribute("totalQuantity", 0);
        model.addAttribute("averageItemCost", 0.0);
        model.addAttribute("lowStockItems", Collections.emptyList());
        model.addAttribute("categoryBreakdown", Collections.emptyMap());
    }

    public static class PurchaseAnalyticsResult {
        private double totalOrderValue;
        private int totalItems;
        private int totalQuantity;
        private double averageItemCost;
        private List<String> lowStockItems;
        private Map<String, Integer> categoryBreakdown;

        // Getters and setters
        public double getTotalOrderValue() { return totalOrderValue; }
        public void setTotalOrderValue(double totalOrderValue) { this.totalOrderValue = totalOrderValue; }

        public int getTotalItems() { return totalItems; }
        public void setTotalItems(int totalItems) { this.totalItems = totalItems; }

        public int getTotalQuantity() { return totalQuantity; }
        public void setTotalQuantity(int totalQuantity) { this.totalQuantity = totalQuantity; }

        public double getAverageItemCost() { return averageItemCost; }
        public void setAverageItemCost(double averageItemCost) { this.averageItemCost = averageItemCost; }

        public List<String> getLowStockItems() { return lowStockItems; }
        public void setLowStockItems(List<String> lowStockItems) { this.lowStockItems = lowStockItems; }

        public Map<String, Integer> getCategoryBreakdown() { return categoryBreakdown; }
        public void setCategoryBreakdown(Map<String, Integer> categoryBreakdown) { this.categoryBreakdown = categoryBreakdown; }
    }

    // Keep the existing generic create method for backward compatibility
    @GetMapping("/create")
    public String createForm(@RequestParam(required = false) Long customerId, Model model) {
        model.addAttribute("order", new OrderDto());
        model.addAttribute("products", productsClient.findAll());
        model.addAttribute("customers", customersClient.findAll());
        model.addAttribute("suppliers", suppliersClient.findAll());
        model.addAttribute("categories", categoriesClient.findAll());

        if (customerId != null) {
            model.addAttribute("selectedCustomerId", customerId);
        }

        return "orders/create";
    }

    // Update the existing POST /create method to handle both types based on orderType
    @PostMapping("/create")
    public String create(@ModelAttribute OrderDto order,
                         Model model,
                         BindingResult result,
                         RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("order", order);
            model.addAttribute("customers", customersClient.findAll());
            model.addAttribute("suppliers", suppliersClient.findAll());
            model.addAttribute("products", productsClient.findAll());
            model.addAttribute("categories", categoriesClient.findAll());
            return "orders/create";
        }

        try {
            OrderDto created = ordersClient.create(order);
            String orderTypeText = order.getOrderType() == OrderType.SALE ? "Sales" : "Purchase";
            redirectAttributes.addFlashAttribute("successMessage",
                    orderTypeText + " Order #" + created.getOrderId() + " created successfully");
            return "redirect:/orders/" + created.getOrderId();

        } catch (Exception exception) {
            log.error("Error creating order", exception);

            // Handle stock errors specifically for sales orders
            if (order.getOrderType() == OrderType.SALE &&
                    exception.getMessage() != null && exception.getMessage().contains("stock")) {
                model.addAttribute("stockError", exception.getMessage());
            } else {
                result.rejectValue("orderType", "error.order", "Failed to create order: " + exception.getMessage());
            }

            model.addAttribute("order", order);
            model.addAttribute("customers", customersClient.findAll());
            model.addAttribute("suppliers", suppliersClient.findAll());
            model.addAttribute("products", productsClient.findAll());
            model.addAttribute("categories", categoriesClient.findAll());
            return "orders/create";
        }
    }

    // Add this to your products controller or create a separate REST controller
    @PostMapping("/api/products/stock-status")
    @ResponseBody
    public ResponseEntity<List<ProductStockResponseDto>> getStockStatus(@RequestBody List<Long> productIds) {
        try {
            List<ProductStockResponseDto> stockStatus = new ArrayList<>();

            for (Long productId : productIds) {
                // Call your backend service to get stock details for this product
                ProductStockResponseDto stockResponse = productsClient.findProductStockById(productId);
                if (stockResponse != null) {
                    stockStatus.add(stockResponse);
                }
            }

            return ResponseEntity.ok(stockStatus);
        } catch (Exception e) {
            log.error("Error fetching stock status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("{id}")
    public String detail(@PathVariable Long id, Model model) {
        try {
            OrderDto orderDto = ordersClient.findOrderById(id);

            if(orderDto.getOrderType().equals(OrderType.PURCHASE)) {
                return "redirect:/orders/purchase/"+id;
            }
            // Enhanced profit/loss calculations
            AnalyticsResult analytics = calculateAdvancedAnalytics(orderDto);

            // Add all data to model
            model.addAttribute("order", orderDto);
            model.addAttribute("totalCostPrice", analytics.getTotalCostPrice());
            model.addAttribute("totalProfitLoss", analytics.getTotalProfitLoss());
            model.addAttribute("profitMargin", analytics.getProfitMargin());
            model.addAttribute("profitableItemsCount", analytics.getProfitableItemsCount());
            model.addAttribute("lossItemsCount", analytics.getLossItemsCount());

            // New enhanced analytics
            model.addAttribute("avgItemProfit", analytics.getAvgItemProfit());
            model.addAttribute("bestPerformingItem", analytics.getBestPerformingItem());
            model.addAttribute("worstPerformingItem", analytics.getWorstPerformingItem());
            model.addAttribute("profitPerformanceRating", analytics.getProfitPerformanceRating());
            model.addAttribute("itemProfitBreakdown", analytics.getItemProfitBreakdown());
            model.addAttribute("categoryAnalysis", analytics.getCategoryAnalysis());

            log.info("Order {} Analytics - Total Cost: ₹{}, Total Selling: ₹{}, Profit/Loss: ₹{}, Margin: {}%, Performance: {}",
                    id, analytics.getTotalCostPrice(), orderDto.getTotalPrice(),
                    analytics.getTotalProfitLoss(), analytics.getProfitMargin(), analytics.getProfitPerformanceRating());

        } catch (Exception e) {
            log.error("Error loading order details for ID: {}", id, e);
            model.addAttribute("errorMessage", "Failed to load order details");
            setDefaultAnalyticsForDetail(model);
        }

        return "orders/detail";
    }

    private AnalyticsResult calculateAdvancedAnalytics(OrderDto orderDto) {
        AnalyticsResult result = new AnalyticsResult();

        double totalCostPrice = 0.0;
        double totalSellingPrice = orderDto.getTotalPrice();
        int profitableItemsCount = 0;
        int lossItemsCount = 0;

        List<ItemProfitInfo> itemProfits = new ArrayList<>();
        Map<String, CategoryProfit> categoryProfits = new HashMap<>();

        // Analyze each order item
        for (OrderItemDto item : orderDto.getOrderItems()) {
            double itemCostPrice = item.getProductDto().getActualPrice();
            double itemSellingPrice = item.getPriceAtOrderTime() != null ?
                    item.getPriceAtOrderTime() : item.getProductDto().getSellingPrice();
            int quantity = item.getQuantity();

            // Calculate totals for this item
            double itemTotalCost = itemCostPrice * quantity;
            double itemTotalSelling = itemSellingPrice * quantity;
            double itemProfitLoss = itemTotalSelling - itemTotalCost;
            double itemProfitMargin = itemCostPrice > 0 ? (itemProfitLoss / itemTotalCost) * 100 : 0.0;

            totalCostPrice += itemTotalCost;

            // Count profitable vs loss items
            if (itemProfitLoss > 0) {
                profitableItemsCount++;
            } else if (itemProfitLoss < 0) {
                lossItemsCount++;
            }

            // Store item profit info for best/worst analysis
            ItemProfitInfo profitInfo = new ItemProfitInfo(
                    item.getProductDto().getName(),
                    itemProfitLoss,
                    itemProfitMargin,
                    item.getProductDto().getBrandName()
            );
            itemProfits.add(profitInfo);

            // Category analysis (if category info available)
            String category = item.getProductDto().getCategory().getName() != null ?
                    item.getProductDto().getCategory().getName() : "Uncategorized";
            categoryProfits.computeIfAbsent(category, k -> new CategoryProfit(k))
                    .addItem(itemProfitLoss, itemTotalSelling);
        }

        // Calculate overall metrics
        double totalProfitLoss = totalSellingPrice - totalCostPrice;
        double profitMargin = totalCostPrice > 0 ? (totalProfitLoss / totalCostPrice) * 100 : 0.0;
        double avgItemProfit = itemProfits.isEmpty() ? 0.0 :
                itemProfits.stream().mapToDouble(ItemProfitInfo::getProfitLoss).average().orElse(0.0);

        // Find best and worst performing items
        String bestPerformingItem = itemProfits.stream()
                .max((a, b) -> Double.compare(a.getProfitLoss(), b.getProfitLoss()))
                .map(ItemProfitInfo::getProductName)
                .orElse("N/A");

        String worstPerformingItem = itemProfits.stream()
                .filter(item -> item.getProfitLoss() < 0)
                .min((a, b) -> Double.compare(a.getProfitLoss(), b.getProfitLoss()))
                .map(ItemProfitInfo::getProductName)
                .orElse(null);

        // Performance rating
        String performanceRating = calculatePerformanceRating(profitMargin, profitableItemsCount, lossItemsCount);

        // Set all results
        result.setTotalCostPrice(totalCostPrice);
        result.setTotalProfitLoss(totalProfitLoss);
        result.setProfitMargin(profitMargin);
        result.setProfitableItemsCount(profitableItemsCount);
        result.setLossItemsCount(lossItemsCount);
        result.setAvgItemProfit(avgItemProfit);
        result.setBestPerformingItem(bestPerformingItem);
        result.setWorstPerformingItem(worstPerformingItem);
        result.setProfitPerformanceRating(performanceRating);
        result.setItemProfitBreakdown(itemProfits);
        result.setCategoryAnalysis(new ArrayList<>(categoryProfits.values()));

        return result;
    }

    private String calculatePerformanceRating(double profitMargin, int profitableItems, int lossItems) {
        if (profitMargin >= 30 && lossItems == 0) return "Excellent";
        if (profitMargin >= 20 && lossItems <= 1) return "Very Good";
        if (profitMargin >= 15) return "Good";
        if (profitMargin >= 10) return "Average";
        if (profitMargin >= 5) return "Below Average";
        if (profitMargin >= 0) return "Poor";
        return "Loss Making";
    }

    private void setDefaultAnalyticsForDetail(Model model) {
        model.addAttribute("totalCostPrice", 0.0);
        model.addAttribute("totalProfitLoss", 0.0);
        model.addAttribute("profitMargin", 0.0);
        model.addAttribute("profitableItemsCount", 0);
        model.addAttribute("lossItemsCount", 0);
        model.addAttribute("avgItemProfit", 0.0);
        model.addAttribute("bestPerformingItem", "N/A");
        model.addAttribute("worstPerformingItem", null);
        model.addAttribute("profitPerformanceRating", "Unknown");
        model.addAttribute("itemProfitBreakdown", Collections.emptyList());
        model.addAttribute("categoryAnalysis", Collections.emptyList());
    }

    // Helper classes for analytics
    public static class AnalyticsResult {
        private double totalCostPrice;
        private double totalProfitLoss;
        private double profitMargin;
        private int profitableItemsCount;
        private int lossItemsCount;
        private double avgItemProfit;
        private String bestPerformingItem;
        private String worstPerformingItem;
        private String profitPerformanceRating;
        private List<ItemProfitInfo> itemProfitBreakdown;
        private List<CategoryProfit> categoryAnalysis;

        // Getters and setters
        public double getTotalCostPrice() { return totalCostPrice; }
        public void setTotalCostPrice(double totalCostPrice) { this.totalCostPrice = totalCostPrice; }

        public double getTotalProfitLoss() { return totalProfitLoss; }
        public void setTotalProfitLoss(double totalProfitLoss) { this.totalProfitLoss = totalProfitLoss; }

        public double getProfitMargin() { return profitMargin; }
        public void setProfitMargin(double profitMargin) { this.profitMargin = profitMargin; }

        public int getProfitableItemsCount() { return profitableItemsCount; }
        public void setProfitableItemsCount(int profitableItemsCount) { this.profitableItemsCount = profitableItemsCount; }

        public int getLossItemsCount() { return lossItemsCount; }
        public void setLossItemsCount(int lossItemsCount) { this.lossItemsCount = lossItemsCount; }

        public double getAvgItemProfit() { return avgItemProfit; }
        public void setAvgItemProfit(double avgItemProfit) { this.avgItemProfit = avgItemProfit; }

        public String getBestPerformingItem() { return bestPerformingItem; }
        public void setBestPerformingItem(String bestPerformingItem) { this.bestPerformingItem = bestPerformingItem; }

        public String getWorstPerformingItem() { return worstPerformingItem; }
        public void setWorstPerformingItem(String worstPerformingItem) { this.worstPerformingItem = worstPerformingItem; }

        public String getProfitPerformanceRating() { return profitPerformanceRating; }
        public void setProfitPerformanceRating(String profitPerformanceRating) { this.profitPerformanceRating = profitPerformanceRating; }

        public List<ItemProfitInfo> getItemProfitBreakdown() { return itemProfitBreakdown; }
        public void setItemProfitBreakdown(List<ItemProfitInfo> itemProfitBreakdown) { this.itemProfitBreakdown = itemProfitBreakdown; }

        public List<CategoryProfit> getCategoryAnalysis() { return categoryAnalysis; }
        public void setCategoryAnalysis(List<CategoryProfit> categoryAnalysis) { this.categoryAnalysis = categoryAnalysis; }
    }

    public static class ItemProfitInfo {
        private String productName;
        private double profitLoss;
        private double profitMargin;
        private String brandName;

        public ItemProfitInfo(String productName, double profitLoss, double profitMargin, String brandName) {
            this.productName = productName;
            this.profitLoss = profitLoss;
            this.profitMargin = profitMargin;
            this.brandName = brandName;
        }

        // Getters
        public String getProductName() { return productName; }
        public double getProfitLoss() { return profitLoss; }
        public double getProfitMargin() { return profitMargin; }
        public String getBrandName() { return brandName; }
    }

    public static class CategoryProfit {
        private String categoryName;
        private double totalProfit = 0.0;
        private double totalRevenue = 0.0;
        private int itemCount = 0;

        public CategoryProfit(String categoryName) {
            this.categoryName = categoryName;
        }

        public void addItem(double itemProfit, double itemRevenue) {
            this.totalProfit += itemProfit;
            this.totalRevenue += itemRevenue;
            this.itemCount++;
        }

        // Getters
        public String getCategoryName() { return categoryName; }
        public double getTotalProfit() { return totalProfit; }
        public double getTotalRevenue() { return totalRevenue; }
        public int getItemCount() { return itemCount; }
        public double getAvgProfitPerItem() { return itemCount > 0 ? totalProfit / itemCount : 0.0; }
    }


    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        try {
            OrderDto orderDto = ordersClient.findOrderById(id);
            model.addAttribute("order", orderDto);
            model.addAttribute("products", productsClient.findAll()); // Add products for item management
            model.addAttribute("newOrderItem", new OrderItemDto()); // For adding new items
        } catch (Exception e) {
            log.error("Error fetching order for edit: " + id, e);
            model.addAttribute("errorMessage", "Order not found");
            return "orders/list";
        }
        return "orders/edit";
    }

    // Add item to order
    @PostMapping("/{orderId}/items/add")
    public String addItemToOrder(@PathVariable Long orderId,
                                 @RequestParam Long productId,
                                 @RequestParam Integer quantity,
                                 @RequestParam Double priceAtOrderTime,
                                 RedirectAttributes redirectAttributes) {
        try {
            // Create OrderItemDto
            OrderItemDto orderItemDto = new OrderItemDto();
            ProductDto productDto = productsClient.findById(productId);
            orderItemDto.setProductDto(productDto);
            orderItemDto.setQuantity(quantity);
            orderItemDto.setPriceAtOrderTime(priceAtOrderTime);

            // Add item to order
            OrderDto response = orderItemClient.addItemToOrder(orderId, orderItemDto);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Item added successfully: " + productDto.getName() + " x" + quantity);
        } catch (Exception e) {
            log.error("Error adding item to order: " + orderId, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to add item: " + e.getMessage());
        }

        return "redirect:/orders/" + orderId + "/edit";
    }

    // Remove item from order
    @PostMapping("/{orderId}/items/remove")
    public String removeItemFromOrder(@PathVariable Long orderId,
                                      @RequestParam Long orderItemId,
                                      @RequestParam(defaultValue = "1") Integer quantityToRemove,
                                      RedirectAttributes redirectAttributes) {
        try {
            RemoveOrderItemRequestDto removeRequest = new RemoveOrderItemRequestDto();
            removeRequest.setOrderItemId(orderItemId);
            removeRequest.setQuantityToRemove(quantityToRemove);

            List<OrderItemDto> updatedItems = orderItemClient.removeItemFromOrder(orderId, removeRequest);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Item removed successfully. Quantity: " + quantityToRemove);
        } catch (Exception e) {
            log.error("Error removing item from order: " + orderId, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to remove item: " + e.getMessage());
        }

        return "redirect:/orders/" + orderId + "/edit";
    }

    // Update item quantity in order
    @PostMapping("/{orderId}/items/{orderItemId}/update-quantity")
    public String updateItemQuantity(@PathVariable Long orderId,
                                     @PathVariable Long orderItemId,
                                     @RequestParam Integer newQuantity,
                                     @RequestParam Integer currentQuantity,
                                     RedirectAttributes redirectAttributes) {
        try {
            if (newQuantity <= 0) {
                // Remove item completely
                RemoveOrderItemRequestDto removeRequest = new RemoveOrderItemRequestDto();
                removeRequest.setOrderItemId(orderItemId);
                removeRequest.setQuantityToRemove(currentQuantity);

                orderItemClient.removeItemFromOrder(orderId, removeRequest);
                redirectAttributes.addFlashAttribute("successMessage", "Item removed from order");
            } else if (newQuantity < currentQuantity) {
                // Reduce quantity
                int quantityToRemove = currentQuantity - newQuantity;
                RemoveOrderItemRequestDto removeRequest = new RemoveOrderItemRequestDto();
                removeRequest.setOrderItemId(orderItemId);
                removeRequest.setQuantityToRemove(quantityToRemove);

                orderItemClient.removeItemFromOrder(orderId, removeRequest);
                redirectAttributes.addFlashAttribute("successMessage",
                        "Quantity updated from " + currentQuantity + " to " + newQuantity);
            } else if (newQuantity > currentQuantity) {
                // This would require adding quantity - you might need a separate endpoint for this
                // or handle it by finding the product and adding the difference
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Increasing quantity not supported through this method. Please add a new item.");
            } else {
                redirectAttributes.addFlashAttribute("infoMessage", "No changes made - quantity is the same");
            }
        } catch (Exception e) {
            log.error("Error updating item quantity in order: " + orderId, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update quantity: " + e.getMessage());
        }

        return "redirect:/orders/" + orderId + "/edit";
    }

    // AJAX endpoint for getting product details
    @GetMapping("/products/{productId}/details")
    @ResponseBody
    public ProductDto getProductDetails(@PathVariable Long productId) {
        try {
            return productsClient.findById(productId);
        } catch (Exception e) {
            log.error("Error fetching product details: " + productId, e);
            return null;
        }
    }

    // ... rest of your existing methods remain unchanged ...

    @PostMapping("/{id}/update-status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam String status,
                               RedirectAttributes redirectAttributes) {
        try {
            // Handle different status updates
            switch (status.toUpperCase()) {
                case "PROCESSING":
                    ordersClient.updateStatus(id, "PROCESSING");
                    redirectAttributes.addFlashAttribute("successMessage",
                            "Order status updated to Processing successfully");
                    break;
                case "COMPLETED":
                    ordersClient.complete(id);
                    redirectAttributes.addFlashAttribute("successMessage",
                            "Order completed successfully");
                    break;
                case "CANCELLED":
                    ordersClient.cancel(id);
                    redirectAttributes.addFlashAttribute("successMessage",
                            "Order cancelled successfully");
                    break;
                case "CREATED":
                    try {
                        ordersClient.updateStatus(id, "CREATED");
                    } catch (Exception e) {
                        log.warn("updateStatus method not available for CREATED status");
                        throw new UnsupportedOperationException("Status update to CREATED not implemented");
                    }
                    redirectAttributes.addFlashAttribute("successMessage",
                            "Order status updated to Created successfully");
                    break;
                default:
                    redirectAttributes.addFlashAttribute("errorMessage",
                            "Invalid status: " + status);
            }
        } catch (Exception exception) {
            log.error("Error updating order status", exception);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to update order status: " + exception.getMessage());
        }

        return "redirect:/orders/" + id + "/edit";
    }

    @PostMapping("/{orderId}/complete")
    public String complete(@PathVariable Long orderId, RedirectAttributes redirectAttributes) {
        try {
            ordersClient.complete(orderId);
            redirectAttributes.addFlashAttribute("successMessage", "Order completed successfully");
        } catch (Exception exception) {
            log.error("Error completing order", exception);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to complete order");
        }

        return "redirect:/orders/" + orderId;
    }

    @PostMapping("/{orderId}/cancel")
    public String cancel(@PathVariable Long orderId,
                         @RequestParam(required = false) String reason,
                         RedirectAttributes redirectAttributes) {
        try {
            ordersClient.cancel(orderId);
            String message = "Order cancelled successfully";
            if (reason != null && !reason.trim().isEmpty()) {
                message += ". Reason: " + reason;
                log.info("Order {} cancelled with reason: {}", orderId, reason);
            }
            redirectAttributes.addFlashAttribute("successMessage", message);
        } catch (Exception exception) {
            log.error("Error cancelling order", exception);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to cancel order");
        }

        return "redirect:/orders/" + orderId;
    }

    // Bulk operations for the orders list page
    @PostMapping("/bulk/cancel")
    @ResponseBody
    public String bulkCancel(@RequestBody List<Long> orderIds) {
        try {
            for (Long orderId : orderIds) {
                try {
                    ordersClient.cancel(orderId);
                } catch (Exception e) {
                    log.error("Failed to cancel order: " + orderId, e);
                    // Continue with other orders even if one fails
                }
            }
            return "success";
        } catch (Exception exception) {
            log.error("Error bulk cancelling orders", exception);
            return "error";
        }
    }

    @PostMapping("/bulk/process")
    @ResponseBody
    public String bulkProcess(@RequestBody List<Long> orderIds) {
        try {
            for (Long orderId : orderIds) {
                try {
                    ordersClient.updateStatus(orderId, "PROCESSING");
                } catch (Exception e) {
                    log.error("Failed to process order: " + orderId, e);
                    // Continue with other orders even if one fails
                }
            }
            return "success";
        } catch (Exception exception) {
            log.error("Error bulk processing orders", exception);
            return "error";
        }
    }

    // AJAX endpoint for status updates from the orders list
    @PostMapping("/{id}/status")
    @ResponseBody
    public String updateOrderStatus(@PathVariable Long id, @RequestParam String status) {
        try {
            switch (status.toUpperCase()) {
                case "PROCESSING":
                    ordersClient.updateStatus(id, status);
                    break;
                case "COMPLETED":
                    ordersClient.complete(id);
                    break;
                case "CANCELLED":
                    ordersClient.cancel(id);
                    break;
                default:
                    return "error";
            }
            return "success";
        } catch (Exception exception) {
            log.error("Error updating order status via AJAX", exception);
            return "error";
        }
    }

    // Helper method to redirect to edit page after status updates
    @PostMapping("/{orderId}/complete-and-edit")
    public String completeAndRedirectToEdit(@PathVariable Long orderId, RedirectAttributes redirectAttributes) {
        try {
            ordersClient.complete(orderId);
            redirectAttributes.addFlashAttribute("successMessage", "Order completed successfully");
        } catch (Exception exception) {
            log.error("Error completing order", exception);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to complete order");
        }

        return "redirect:/orders/" + orderId + "/edit";
    }

    @PostMapping("/{orderId}/cancel-and-edit")
    public String cancelAndRedirectToEdit(@PathVariable Long orderId,
                                          @RequestParam(required = false) String reason,
                                          RedirectAttributes redirectAttributes) {
        try {
            ordersClient.cancel(orderId);
            String message = "Order cancelled successfully";
            if (reason != null && !reason.trim().isEmpty()) {
                message += ". Reason: " + reason;
            }
            redirectAttributes.addFlashAttribute("successMessage", message);
        } catch (Exception exception) {
            log.error("Error cancelling order", exception);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to cancel order");
        }

        return "redirect:/orders/" + orderId + "/edit";
    }



    @PostMapping("/customers/create")
    @ResponseBody
    public CustomerDto createCustomerAjax(@RequestBody CustomerDto customerDto) {
        return customersClient.create(customerDto);
    }

    @PostMapping("/suppliers/create")
    @ResponseBody
    public SupplierDto createSupplierAjax(@RequestBody SupplierDto supplierDto) {
        return suppliersClient.create(supplierDto);
    }

    @PostMapping("/products/create")
    @ResponseBody
    public ProductResponseDto createProductAjax(@RequestBody ProductDto productDto, HttpServletRequest request) {
        return productsClient.create(productDto);
    }
}
