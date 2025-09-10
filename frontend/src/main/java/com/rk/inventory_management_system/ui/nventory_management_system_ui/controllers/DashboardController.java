package com.rk.inventory_management_system.ui.nventory_management_system_ui.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.Clients.*;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.*;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.chartsDtos.ProductSalesData;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.chartsDtos.StockLevelData;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.enums.OrderStatus;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.enums.OrderType;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.enums.PaymentType;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.productDtos.ProductResponseDto;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.supplierDtos.SupplierResponseDto;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@Slf4j
@RequiredArgsConstructor
public class DashboardController {

    private final ProductsClient productsClient;
    private final CustomersClient customersClient;
    private final SuppliersClient suppliersClient;
    private final CategoriesClient categoriesClient;
    private final OrdersClient ordersClient;

    @GetMapping(path = "/dashboard")
    public String dashboard(Model model, HttpServletRequest request) {
        try {
            // Get real data from APIs
            List<ProductDto> products = productsClient.findAll();
            List<CustomerDto> customers = customersClient.findAll();
            List<SupplierResponseDto> suppliers = suppliersClient.findAll();
            List<ProductCategoryDto> categories = categoriesClient.findAll();
            List<OrderDto> orders = ordersClient.findAll();

            // Calculate key metrics using real data
            model.addAttribute("customerCount", customers.size());
            model.addAttribute("productCount", products.size());
            model.addAttribute("supplierCount", suppliers.size());
            model.addAttribute("categoryCount", categories.size());

            // Enhanced analytics with real data
            model.addAttribute("monthlyProfit", calculateMonthlyProfit(orders));
            model.addAttribute("monthlyRevenue", calculateMonthlyRevenue(orders));
            model.addAttribute("monthlyOrders", calculateMonthlyOrders(orders));
            model.addAttribute("lowStockCount", calculateLowStockProducts(products));

            // Add stock overview data
            model.addAttribute("stockOverviewData", getStockOverviewData(products));

        } catch (Exception exception) {
            log.error("Error loading dashboard data: ", exception);
            model.addAttribute("errorMessage", "Unable to load dashboard data");
        }

        return "dashboard/index";
    }

    @GetMapping("/dashboard/chart-data")
    @ResponseBody
    public Map<String, Object> getAnalyticsChartData() {
        log.info("Getting analytics chart data");
        Map<String, Object> chartData = new HashMap<>();

        try {
            // Get real data from APIs
            List<ProductDto> products = productsClient.findAll();
            List<OrderDto> orders = ordersClient.findAll();
            List<ProductCategoryDto> categories = categoriesClient.findAll();

            // Generate charts with real data
            chartData.put("topSelling", getTopSellingProductsData(products));
            chartData.put("profitable", getMostProfitableProductsData(products));
            chartData.put("lossMaking", getLossMakingProductsData(products));
            chartData.put("stockLevels", getStockLevelsWithStatus(products));
            chartData.put("salesTrend", getSalesTrendData(orders));
            chartData.put("categoryProfit", getCategoryProfitData(products, categories));
            chartData.put("inventoryTurnover", getInventoryTurnoverData(products, orders));
            chartData.put("ordersByChannel", getOrdersByChannelData(orders));

            log.info("Analytics chart data prepared successfully");
        } catch (Exception e) {
            log.error("Error preparing analytics chart data", e);
        }

        return chartData;
    }

    @GetMapping("/dashboard/stock-overview")
    @ResponseBody
    public Map<String, Object> getStockOverviewData() {
        log.info("Getting stock overview data");
        Map<String, Object> stockData = new HashMap<>();

        try {
            List<ProductDto> products = productsClient.findAll();
            stockData = getStockOverviewData(products);
            log.info("Stock overview data prepared successfully");
        } catch (Exception e) {
            log.error("Error preparing stock overview data", e);
        }

        return stockData;
    }

    @GetMapping("/dashboard/profit-data")
    @ResponseBody
    public Map<String, Object> getProfitData(@RequestParam(defaultValue = "month") String period) {
        log.info("Getting profit data for period: {}", period);
        Map<String, Object> profitData = new HashMap<>();

        try {
            List<OrderDto> orders = ordersClient.findAll();
            List<ProductDto> products = productsClient.findAll();

            switch (period.toLowerCase()) {
                case "week":
                    profitData = calculateWeeklyProfitData(orders, products);
                    break;
                case "year":
                    profitData = calculateYearlyProfitData(orders, products);
                    break;
                default: // month
                    profitData = calculateMonthlyProfitData(orders, products);
                    break;
            }

            log.info("Profit data prepared for period: {}", period);
        } catch (Exception e) {
            log.error("Error preparing profit data for period: {}", period, e);
        }

        return profitData;
    }

    @GetMapping("/dashboard/revenue-data")
    @ResponseBody
    public Map<String, Object> getRevenueData() {
        log.info("Getting detailed revenue data");
        Map<String, Object> revenueData = new HashMap<>();

        try {
            List<OrderDto> orders = ordersClient.findAll();
            List<CustomerDto> customers = customersClient.findAll();

            revenueData.put("monthly", calculateDetailedMonthlyRevenue(orders));
            revenueData.put("yearly", calculateDetailedYearlyRevenue(orders));
            revenueData.put("weekly", calculateDetailedWeeklyRevenue(orders));
            revenueData.put("averageOrderValue", calculateAverageOrderValue(orders));
            revenueData.put("totalOrders", calculateTotalOrders(orders));
            revenueData.put("contributingCustomers", calculateContributingCustomers(orders, customers));
            revenueData.put("channelBreakdown", calculateRevenueByChannel(orders));

            log.info("Detailed revenue data prepared successfully");
        } catch (Exception e) {
            log.error("Error preparing detailed revenue data", e);
        }

        return revenueData;
    }

    @PostMapping("/dashboard/products")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createProduct(@RequestBody ProductDto request) {
        log.info("Creating new product: {}", request.getName());
        Map<String, Object> response = new HashMap<>();

        try {
            ProductResponseDto newProduct = productsClient.create(request);

            response.put("success", true);
            response.put("message", "Product created successfully");
            response.put("productId", newProduct.getProductId());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating product", e);
            response.put("success", false);
            response.put("message", "Failed to create product: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Helper methods for enhanced analytics

    private Map<String, Object> calculateMonthlyProfit(List<OrderDto> orders) {
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);

        double currentMonthProfit = orders.stream()
                .filter(order -> order.getCreatedAt().isAfter(startOfMonth))
                .filter(order -> order.getOrderStatus() == OrderStatus.COMPLETED)
                .filter(order -> order.getOrderType() == OrderType.SALE)
                .flatMap(order -> order.getOrderItems().stream())
                .mapToDouble(item -> {
                    double profit = item.getPriceAtOrderTime() - item.getProductDto().getActualPrice();
                    return profit * item.getQuantity();
                })
                .sum();

        // Calculate previous month for comparison
        LocalDateTime startOfPrevMonth = startOfMonth.minusMonths(1);
        LocalDateTime endOfPrevMonth = startOfMonth.minusDays(1);

        double prevMonthProfit = orders.stream()
                .filter(order -> order.getCreatedAt().isAfter(startOfPrevMonth)
                        && order.getCreatedAt().isBefore(endOfPrevMonth))
                .filter(order -> order.getOrderStatus() == OrderStatus.COMPLETED)
                .filter(order -> order.getOrderType() == OrderType.SALE)
                .flatMap(order -> order.getOrderItems().stream())
                .mapToDouble(item -> {
                    double profit = item.getPriceAtOrderTime() - item.getProductDto().getActualPrice();
                    return profit * item.getQuantity();
                })
                .sum();

        double percentageChange = prevMonthProfit != 0 ?
                ((currentMonthProfit - prevMonthProfit) / prevMonthProfit) * 100 : 0;

        Map<String, Object> result = new HashMap<>();
        result.put("amount", currentMonthProfit);
        result.put("percentageChange", percentageChange);
        result.put("isIncrease", percentageChange >= 0);

        return result;
    }

    private Map<String, Object> calculateMonthlyRevenue(List<OrderDto> orders) {
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);

        double currentMonthRevenue = orders.stream()
                .filter(order -> order.getCreatedAt().isAfter(startOfMonth))
                .filter(order -> order.getOrderStatus() == OrderStatus.COMPLETED)
                .filter(order -> order.getOrderType() == OrderType.SALE)
                .mapToDouble(OrderDto::getTotalPrice)
                .sum();

        // Calculate previous month for comparison
        LocalDateTime startOfPrevMonth = startOfMonth.minusMonths(1);
        LocalDateTime endOfPrevMonth = startOfMonth.minusDays(1);

        double prevMonthRevenue = orders.stream()
                .filter(order -> order.getCreatedAt().isAfter(startOfPrevMonth)
                        && order.getCreatedAt().isBefore(endOfPrevMonth))
                .filter(order -> order.getOrderStatus() == OrderStatus.COMPLETED)
                .filter(order -> order.getOrderType() == OrderType.SALE)
                .mapToDouble(OrderDto::getTotalPrice)
                .sum();

        double percentageChange = prevMonthRevenue != 0 ?
                ((currentMonthRevenue - prevMonthRevenue) / prevMonthRevenue) * 100 : 0;

        Map<String, Object> result = new HashMap<>();
        result.put("amount", currentMonthRevenue);
        result.put("percentageChange", percentageChange);
        result.put("isIncrease", percentageChange >= 0);

        return result;
    }

    private Map<String, Object> calculateMonthlyOrders(List<OrderDto> orders) {
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);

        long currentMonthOrders = orders.stream()
                .filter(order -> order.getCreatedAt().isAfter(startOfMonth))
                .filter(order -> order.getOrderType() == OrderType.SALE)
                .count();

        // Calculate new orders this week
        LocalDateTime startOfWeek = LocalDateTime.now().minusDays(7);
        long newOrdersThisWeek = orders.stream()
                .filter(order -> order.getCreatedAt().isAfter(startOfWeek))
                .filter(order -> order.getOrderType() == OrderType.SALE)
                .count();

        Map<String, Object> result = new HashMap<>();
        result.put("count", currentMonthOrders);
        result.put("newOrders", newOrdersThisWeek);

        return result;
    }

    private int calculateLowStockProducts(List<ProductDto> products) {
        return (int) products.stream()
                .filter(product -> product.getLowStockThreshold() != null ?
                        product.getStockQuantity() <= product.getLowStockThreshold() :
                        product.getStockQuantity() < 10)
                .count();
    }

    private Map<String, Object> getStockOverviewData(List<ProductDto> products) {
        Map<String, Object> stockData = new HashMap<>();

        // Stock status distribution
        List<Map<String, Object>> stockItems = products.stream()
                .map(product -> {
                    Map<String, Object> item = new HashMap<>();
                    int threshold = product.getLowStockThreshold() != null ? product.getLowStockThreshold() : 10;
                    String status;
                    String statusColor;

                    if (product.getStockQuantity() <= threshold) {
                        status = "LOW";
                        statusColor = "#ef4444";
                    } else if (product.getStockQuantity() <= threshold * 3) {
                        status = "MEDIUM";
                        statusColor = "#f59e0b";
                    } else {
                        status = "GOOD";
                        statusColor = "#10b981";
                    }

                    item.put("name", product.getName());
                    item.put("quantity", product.getStockQuantity());
                    item.put("threshold", threshold);
                    item.put("status", status);
                    item.put("statusColor", statusColor);
                    item.put("category", product.getCategory() != null ? product.getCategory().getName() : "Other");

                    return item;
                })
                .sorted((a, b) -> {
                    // Sort by status priority (LOW first, then MEDIUM, then GOOD)
                    String statusA = (String) a.get("status");
                    String statusB = (String) b.get("status");
                    if (!statusA.equals(statusB)) {
                        if (statusA.equals("LOW")) return -1;
                        if (statusB.equals("LOW")) return 1;
                        if (statusA.equals("MEDIUM")) return -1;
                        if (statusB.equals("MEDIUM")) return 1;
                    }
                    return Integer.compare((Integer) a.get("quantity"), (Integer) b.get("quantity"));
                })
                .limit(10) // Show top 10 items
                .collect(Collectors.toList());

        // Calculate stock statistics
        long lowStockCount = products.stream()
                .filter(p -> {
                    int threshold = p.getLowStockThreshold() != null ? p.getLowStockThreshold() : 10;
                    return p.getStockQuantity() <= threshold;
                })
                .count();

        long mediumStockCount = products.stream()
                .filter(p -> {
                    int threshold = p.getLowStockThreshold() != null ? p.getLowStockThreshold() : 10;
                    return p.getStockQuantity() > threshold && p.getStockQuantity() <= threshold * 3;
                })
                .count();

        long goodStockCount = products.stream()
                .filter(p -> {
                    int threshold = p.getLowStockThreshold() != null ? p.getLowStockThreshold() : 10;
                    return p.getStockQuantity() > threshold * 3;
                })
                .count();

        // Recent stock movements (mock data since we don't have movement tracking)
        List<Map<String, Object>> recentMovements = products.stream()
                .filter(p -> p.getLowStockThreshold() != null ?
                        p.getStockQuantity() <= p.getLowStockThreshold() :
                        p.getStockQuantity() < 10)
                .limit(5)
                .map(product -> {
                    Map<String, Object> movement = new HashMap<>();
                    movement.put("productName", product.getName());
                    movement.put("currentStock", product.getStockQuantity());
                    movement.put("threshold", product.getLowStockThreshold() != null ? product.getLowStockThreshold() : 10);
                    movement.put("category", product.getCategory() != null ? product.getCategory().getName() : "Other");
                    return movement;
                })
                .collect(Collectors.toList());

        stockData.put("stockItems", stockItems);
        stockData.put("lowStockCount", lowStockCount);
        stockData.put("mediumStockCount", mediumStockCount);
        stockData.put("goodStockCount", goodStockCount);
        stockData.put("totalProducts", products.size());
        stockData.put("recentMovements", recentMovements);

        return stockData;
    }

    private Map<String, Object> getCategoryProfitData(List<ProductDto> products, List<ProductCategoryDto> categories) {
        Map<String, Object> data = new HashMap<>();

        // Calculate profit by category using actual sold quantities
        Map<String, Double> categoryProfits = products.stream()
                .filter(p -> p.getSellingPrice() > p.getActualPrice())
                .filter(p -> p.getQuantitySold() != null && p.getQuantitySold() > 0)
                .collect(Collectors.groupingBy(
                        p -> p.getCategory() != null ? p.getCategory().getName() : "Other",
                        Collectors.summingDouble(p -> {
                            double profitPerUnit = p.getSellingPrice() - p.getActualPrice();
                            return profitPerUnit * p.getQuantitySold();
                        })
                ));

        // If no profits calculated, provide empty data
        if (categoryProfits.isEmpty()) {
            data.put("labels", new ArrayList<>());
            data.put("data", new ArrayList<>());
            data.put("colors", new ArrayList<>());
            return data;
        }

        List<String> labels = new ArrayList<>(categoryProfits.keySet());
        List<Double> profitData = labels.stream()
                .map(categoryProfits::get)
                .collect(Collectors.toList());

        List<String> colors = labels.stream()
                .map(this::getCategoryColor)
                .collect(Collectors.toList());

        data.put("labels", labels);
        data.put("data", profitData);
        data.put("colors", colors);

        return data;
    }

    private String getCategoryColor(String categoryName) {
        switch (categoryName.toLowerCase()) {
            case "electronics": return "#3b82f6";
            case "clothing": return "#ec4899";
            case "food": case "food & beverages": return "#10b981";
            case "books": return "#f59e0b";
            case "home": case "home & garden": return "#8b5cf6";
            default: return "#6b7280";
        }
    }

    private Map<String, Object> getTopSellingProductsData(List<ProductDto> products) {
        Map<String, Object> data = new HashMap<>();

        List<ProductDto> topProducts = products.stream()
                .filter(p -> p.getQuantitySold() != null && p.getQuantitySold() > 0)
                .sorted((p1, p2) -> Integer.compare(p2.getQuantitySold(), p1.getQuantitySold()))
                .limit(10)
                .collect(Collectors.toList());

        if (topProducts.isEmpty()) {
            data.put("labels", new ArrayList<>());
            data.put("data", new ArrayList<>());
            return data;
        }

        List<String> labels = topProducts.stream()
                .map(ProductDto::getName)
                .collect(Collectors.toList());

        List<Integer> soldData = topProducts.stream()
                .map(ProductDto::getQuantitySold)
                .collect(Collectors.toList());

        data.put("labels", labels);
        data.put("data", soldData);
        return data;
    }

    private Map<String, Object> getMostProfitableProductsData(List<ProductDto> products) {
        Map<String, Object> data = new HashMap<>();

        List<ProductSalesData> profitableProducts = products.stream()
                .filter(p -> p.getSellingPrice() > p.getActualPrice())
                .filter(p -> p.getQuantitySold() != null && p.getQuantitySold() > 0)
                .map(p -> {
                    double profitPerUnit = p.getSellingPrice() - p.getActualPrice();
                    double totalProfit = profitPerUnit * p.getQuantitySold();
                    double revenue = p.getSellingPrice() * p.getQuantitySold();
                    return new ProductSalesData(p.getName(), p.getQuantitySold(), revenue, totalProfit);
                })
                .sorted((p1, p2) -> Double.compare(p2.getProfit(), p1.getProfit()))
                .limit(10)
                .collect(Collectors.toList());

        if (profitableProducts.isEmpty()) {
            data.put("labels", new ArrayList<>());
            data.put("data", new ArrayList<>());
            return data;
        }

        List<String> labels = profitableProducts.stream()
                .map(ProductSalesData::getProductName)
                .collect(Collectors.toList());

        List<Double> profitData = profitableProducts.stream()
                .map(ProductSalesData::getProfit)
                .collect(Collectors.toList());

        data.put("labels", labels);
        data.put("data", profitData);
        return data;
    }

    private Map<String, Object> getLossMakingProductsData(List<ProductDto> products) {
        Map<String, Object> data = new HashMap<>();

        List<ProductDto> lossProducts = products.stream()
                .filter(p -> p.getSellingPrice() < p.getActualPrice())
                .filter(p -> p.getQuantitySold() != null && p.getQuantitySold() > 0)
                .sorted((p1, p2) -> {
                    double loss1 = (p1.getActualPrice() - p1.getSellingPrice()) * p1.getQuantitySold();
                    double loss2 = (p2.getActualPrice() - p2.getSellingPrice()) * p2.getQuantitySold();
                    return Double.compare(loss2, loss1);
                })
                .limit(8)
                .collect(Collectors.toList());

        if (lossProducts.isEmpty()) {
            data.put("labels", new ArrayList<>());
            data.put("data", new ArrayList<>());
            return data;
        }

        List<String> labels = lossProducts.stream()
                .map(ProductDto::getName)
                .collect(Collectors.toList());

        List<Double> lossData = lossProducts.stream()
                .map(p -> -((p.getActualPrice() - p.getSellingPrice()) * p.getQuantitySold()))
                .collect(Collectors.toList());

        data.put("labels", labels);
        data.put("data", lossData);
        return data;
    }

    private Map<String, Object> getStockLevelsWithStatus(List<ProductDto> products) {
        Map<String, Object> data = new HashMap<>();

        List<StockLevelData> stockData = products.stream()
                .map(p -> {
                    int threshold = p.getLowStockThreshold() != null ? p.getLowStockThreshold() : 10;
                    String status;
                    if (p.getStockQuantity() <= threshold) status = "LOW";
                    else if (p.getStockQuantity() <= threshold * 3) status = "MEDIUM";
                    else status = "GOOD";

                    return new StockLevelData(p.getName(), p.getStockQuantity(), status, threshold);
                })
                .sorted(Comparator.comparingInt(StockLevelData::getStockQuantity))
                .limit(15)
                .collect(Collectors.toList());

        if (stockData.isEmpty()) {
            data.put("labels", new ArrayList<>());
            data.put("data", new ArrayList<>());
            data.put("colors", new ArrayList<>());
            return data;
        }

        List<String> labels = stockData.stream()
                .map(StockLevelData::getProductName)
                .collect(Collectors.toList());

        List<Integer> quantities = stockData.stream()
                .map(StockLevelData::getStockQuantity)
                .collect(Collectors.toList());

        List<String> colors = stockData.stream()
                .map(s -> {
                    switch (s.getStockStatus()) {
                        case "LOW": return "#ef4444";
                        case "MEDIUM": return "#f59e0b";
                        default: return "#10b981";
                    }
                })
                .collect(Collectors.toList());

        data.put("labels", labels);
        data.put("data", quantities);
        data.put("colors", colors);
        return data;
    }

    private Map<String, Object> getSalesTrendData(List<OrderDto> orders) {
        Map<String, Object> data = new HashMap<>();

        // Filter only SALE orders for sales trend
        List<OrderDto> salesOrders = orders.stream()
                .filter(order -> order.getOrderType() == OrderType.SALE)
                .filter(order -> order.getOrderStatus() == OrderStatus.COMPLETED)
                .collect(Collectors.toList());

        // Weekly data (last 7 days)
        Map<String, Object> weekData = new HashMap<>();
        List<String> weekLabels = Arrays.asList("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");
        List<Double> weekSales = calculateWeeklySales(salesOrders);
        weekData.put("labels", weekLabels);
        weekData.put("data", weekSales);

        // Monthly data (last 4 weeks)
        Map<String, Object> monthData = new HashMap<>();
        List<String> monthLabels = Arrays.asList("Week 1", "Week 2", "Week 3", "Week 4");
        List<Double> monthSales = calculateMonthlySales(salesOrders);
        monthData.put("labels", monthLabels);
        monthData.put("data", monthSales);

        // Yearly data (last 4 quarters)
        Map<String, Object> yearData = new HashMap<>();
        List<String> yearLabels = Arrays.asList("Q1", "Q2", "Q3", "Q4");
        List<Double> yearSales = calculateYearlySales(salesOrders);
        yearData.put("labels", yearLabels);
        yearData.put("data", yearSales);

        data.put("week", weekData);
        data.put("month", monthData);
        data.put("year", yearData);

        return data;
    }

    // New chart data methods
    private Map<String, Object> getInventoryTurnoverData(List<ProductDto> products, List<OrderDto> orders) {
        Map<String, Object> data = new HashMap<>();

        // Calculate inventory turnover for products with sales
        List<Map<String, Object>> turnoverData = products.stream()
                .filter(p -> p.getQuantitySold() != null && p.getQuantitySold() > 0)
                .filter(p -> p.getStockQuantity() > 0)
                .map(product -> {
                    Map<String, Object> item = new HashMap<>();
                    double turnoverRatio = (double) product.getQuantitySold() / product.getStockQuantity();
                    item.put("name", product.getName());
                    item.put("turnoverRatio", turnoverRatio);
                    item.put("category", product.getCategory() != null ? product.getCategory().getName() : "Other");
                    return item;
                })
                .sorted((a, b) -> Double.compare((Double) b.get("turnoverRatio"), (Double) a.get("turnoverRatio")))
                .limit(10)
                .collect(Collectors.toList());

        List<String> labels = turnoverData.stream()
                .map(item -> (String) item.get("name"))
                .collect(Collectors.toList());

        List<Double> ratios = turnoverData.stream()
                .map(item -> (Double) item.get("turnoverRatio"))
                .collect(Collectors.toList());

        data.put("labels", labels);
        data.put("data", ratios);
        return data;
    }

    private Map<String, Object> getOrdersByChannelData(List<OrderDto> orders) {
        Map<String, Object> data = new HashMap<>();

        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);

        Map<String, Long> channelOrders = orders.stream()
                .filter(order -> order.getCreatedAt().isAfter(startOfMonth))
                .filter(order -> order.getOrderStatus() == OrderStatus.COMPLETED)
                .filter(order -> order.getOrderType() == OrderType.SALE)
                .collect(Collectors.groupingBy(
                        order -> getChannelNameFromPaymentType(order.getPaymentType()),
                        Collectors.counting()
                ));

        if (channelOrders.isEmpty()) {
            data.put("labels", new ArrayList<>());
            data.put("data", new ArrayList<>());
            data.put("colors", new ArrayList<>());
            return data;
        }

        List<String> labels = new ArrayList<>(channelOrders.keySet());
        List<Long> orderCounts = labels.stream()
                .map(channelOrders::get)
                .collect(Collectors.toList());

        List<String> colors = labels.stream()
                .map(this::getChannelColor)
                .collect(Collectors.toList());

        data.put("labels", labels);
        data.put("data", orderCounts);
        data.put("colors", colors);
        return data;
    }

    private String getChannelColor(String channelName) {
        switch (channelName) {
            case "Online Store": return "#3b82f6";
            case "Retail Store": return "#10b981";
            case "Mobile App": return "#f59e0b";
            default: return "#6b7280";
        }
    }

    // Additional helper methods for profit calculation with real data
    private List<Map<String, Object>> getTopProfitableProductsForPeriod(List<OrderDto> orders, List<ProductDto> products, LocalDateTime startDate) {
        Map<String, Double> productProfits = orders.stream()
                .filter(order -> order.getCreatedAt().isAfter(startDate))
                .filter(order -> order.getOrderStatus() == OrderStatus.COMPLETED)
                .filter(order -> order.getOrderType() == OrderType.SALE)
                .flatMap(order -> order.getOrderItems().stream())
                .collect(Collectors.groupingBy(
                        item -> item.getProductDto().getName(),
                        Collectors.summingDouble(item -> {
                            double profit = item.getPriceAtOrderTime() - item.getProductDto().getActualPrice();
                            return profit * item.getQuantity();
                        })
                ));

        if (productProfits.isEmpty()) {
            return new ArrayList<>();
        }

        return productProfits.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5)
                .map(entry -> {
                    Map<String, Object> productData = new HashMap<>();
                    productData.put("name", entry.getKey());
                    productData.put("profit", entry.getValue());

                    double totalProfit = productProfits.values().stream().mapToDouble(Double::doubleValue).sum();
                    double percentage = totalProfit > 0 ? (entry.getValue() / totalProfit) * 100 : 0;
                    productData.put("percentage", percentage);

                    return productData;
                })
                .collect(Collectors.toList());
    }

    private Map<String, Object> calculateWeeklyProfitData(List<OrderDto> orders, List<ProductDto> products) {
        Map<String, Object> data = new HashMap<>();
        LocalDateTime startOfWeek = LocalDateTime.now().minusDays(7);

        double weekProfit = orders.stream()
                .filter(order -> order.getCreatedAt().isAfter(startOfWeek))
                .filter(order -> order.getOrderStatus() == OrderStatus.COMPLETED)
                .filter(order -> order.getOrderType() == OrderType.SALE)
                .flatMap(order -> order.getOrderItems().stream())
                .mapToDouble(item -> {
                    double profit = item.getPriceAtOrderTime() - item.getProductDto().getActualPrice();
                    return profit * item.getQuantity();
                })
                .sum();

        data.put("totalProfit", weekProfit);
        data.put("period", "Week");
        data.put("topProducts", getTopProfitableProductsForPeriod(orders, products, startOfWeek));

        return data;
    }

    private Map<String, Object> calculateYearlyProfitData(List<OrderDto> orders, List<ProductDto> products) {
        Map<String, Object> data = new HashMap<>();
        LocalDateTime startOfYear = LocalDateTime.now().withDayOfYear(1).withHour(0).withMinute(0).withSecond(0);

        double yearProfit = orders.stream()
                .filter(order -> order.getCreatedAt().isAfter(startOfYear))
                .filter(order -> order.getOrderStatus() == OrderStatus.COMPLETED)
                .filter(order -> order.getOrderType() == OrderType.SALE)
                .flatMap(order -> order.getOrderItems().stream())
                .mapToDouble(item -> {
                    double profit = item.getPriceAtOrderTime() - item.getProductDto().getActualPrice();
                    return profit * item.getQuantity();
                })
                .sum();

        data.put("totalProfit", yearProfit);
        data.put("period", "Year");
        data.put("topProducts", getTopProfitableProductsForPeriod(orders, products, startOfYear));

        return data;
    }

    private Map<String, Object> calculateMonthlyProfitData(List<OrderDto> orders, List<ProductDto> products) {
        Map<String, Object> data = new HashMap<>();
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);

        double monthProfit = orders.stream()
                .filter(order -> order.getCreatedAt().isAfter(startOfMonth))
                .filter(order -> order.getOrderStatus() == OrderStatus.COMPLETED)
                .filter(order -> order.getOrderType() == OrderType.SALE)
                .flatMap(order -> order.getOrderItems().stream())
                .mapToDouble(item -> {
                    double profit = item.getPriceAtOrderTime() - item.getProductDto().getActualPrice();
                    return profit * item.getQuantity();
                })
                .sum();

        data.put("totalProfit", monthProfit);
        data.put("period", "Month");
        data.put("topProducts", getTopProfitableProductsForPeriod(orders, products, startOfMonth));

        return data;
    }

    // Revenue calculation methods
    private double calculateDetailedMonthlyRevenue(List<OrderDto> orders) {
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);

        return orders.stream()
                .filter(order -> order.getCreatedAt().isAfter(startOfMonth))
                .filter(order -> order.getOrderStatus() == OrderStatus.COMPLETED)
                .filter(order -> order.getOrderType() == OrderType.SALE)
                .mapToDouble(OrderDto::getTotalPrice)
                .sum();
    }

    private double calculateDetailedYearlyRevenue(List<OrderDto> orders) {
        LocalDateTime startOfYear = LocalDateTime.now().withDayOfYear(1).withHour(0).withMinute(0).withSecond(0);

        return orders.stream()
                .filter(order -> order.getCreatedAt().isAfter(startOfYear))
                .filter(order -> order.getOrderStatus() == OrderStatus.COMPLETED)
                .filter(order -> order.getOrderType() == OrderType.SALE)
                .mapToDouble(OrderDto::getTotalPrice)
                .sum();
    }

    private double calculateDetailedWeeklyRevenue(List<OrderDto> orders) {
        LocalDateTime startOfWeek = LocalDateTime.now().minusDays(7);

        return orders.stream()
                .filter(order -> order.getCreatedAt().isAfter(startOfWeek))
                .filter(order -> order.getOrderStatus() == OrderStatus.COMPLETED)
                .filter(order -> order.getOrderType() == OrderType.SALE)
                .mapToDouble(OrderDto::getTotalPrice)
                .sum();
    }

    private double calculateAverageOrderValue(List<OrderDto> orders) {
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);

        List<OrderDto> monthlyOrders = orders.stream()
                .filter(order -> order.getCreatedAt().isAfter(startOfMonth))
                .filter(order -> order.getOrderStatus() == OrderStatus.COMPLETED)
                .filter(order -> order.getOrderType() == OrderType.SALE)
                .collect(Collectors.toList());

        if (monthlyOrders.isEmpty()) {
            return 0.0;
        }

        double totalRevenue = monthlyOrders.stream().mapToDouble(OrderDto::getTotalPrice).sum();
        return totalRevenue / monthlyOrders.size();
    }

    private long calculateTotalOrders(List<OrderDto> orders) {
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);

        return orders.stream()
                .filter(order -> order.getCreatedAt().isAfter(startOfMonth))
                .filter(order -> order.getOrderType() == OrderType.SALE)
                .count();
    }

    private long calculateContributingCustomers(List<OrderDto> orders, List<CustomerDto> customers) {
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);

        Set<Long> customerIds = orders.stream()
                .filter(order -> order.getCreatedAt().isAfter(startOfMonth))
                .filter(order -> order.getOrderStatus() == OrderStatus.COMPLETED)
                .filter(order -> order.getOrderType() == OrderType.SALE)
                .map(order -> order.getCustomer().getCustomerId())
                .collect(Collectors.toSet());

        return customerIds.size();
    }

    private List<Map<String, Object>> calculateRevenueByChannel(List<OrderDto> orders) {
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);

        // Group orders by payment type as a proxy for channel
        Map<PaymentType, Double> paymentTypeRevenue = orders.stream()
                .filter(order -> order.getCreatedAt().isAfter(startOfMonth))
                .filter(order -> order.getOrderStatus() == OrderStatus.COMPLETED)
                .filter(order -> order.getOrderType() == OrderType.SALE)
                .collect(Collectors.groupingBy(
                        OrderDto::getPaymentType,
                        Collectors.summingDouble(OrderDto::getTotalPrice)
                ));

        double totalRevenue = paymentTypeRevenue.values().stream().mapToDouble(Double::doubleValue).sum();

        List<Map<String, Object>> channelData = new ArrayList<>();

        paymentTypeRevenue.forEach((paymentType, revenue) -> {
            Map<String, Object> channelInfo = new HashMap<>();
            String channelName = getChannelNameFromPaymentType(paymentType);
            channelInfo.put("channel", channelName);
            channelInfo.put("revenue", revenue);

            double percentage = totalRevenue > 0 ? (revenue / totalRevenue) * 100 : 0;
            channelInfo.put("percentage", percentage);

            channelData.add(channelInfo);
        });

        return channelData;
    }

    private String getChannelNameFromPaymentType(PaymentType paymentType) {
        switch (paymentType) {
            case CARD:
                return "Online Store";
            case CASH:
                return "Retail Store";
            case UPI:
                return "Mobile App";
            default:
                return "Other";
        }
    }

    // Sales trend calculation methods with real data
    private List<Double> calculateWeeklySales(List<OrderDto> orders) {
        LocalDateTime now = LocalDateTime.now();
        List<Double> weeklySales = new ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            LocalDateTime dayStart = now.minusDays(i).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime dayEnd = dayStart.plusDays(1);

            double daySales = orders.stream()
                    .filter(order -> order.getCreatedAt().isAfter(dayStart)
                            && order.getCreatedAt().isBefore(dayEnd))
                    .filter(order -> order.getOrderStatus() == OrderStatus.COMPLETED)
                    .mapToDouble(OrderDto::getTotalPrice)
                    .sum();

            weeklySales.add(daySales);
        }

        return weeklySales;
    }

    private List<Double> calculateMonthlySales(List<OrderDto> orders) {
        LocalDateTime now = LocalDateTime.now();
        List<Double> monthlySales = new ArrayList<>();

        for (int week = 3; week >= 0; week--) {
            LocalDateTime weekStart = now.minusWeeks(week).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime weekEnd = weekStart.plusWeeks(1);

            double weekSales = orders.stream()
                    .filter(order -> order.getCreatedAt().isAfter(weekStart)
                            && order.getCreatedAt().isBefore(weekEnd))
                    .filter(order -> order.getOrderStatus() == OrderStatus.COMPLETED)
                    .mapToDouble(OrderDto::getTotalPrice)
                    .sum();

            monthlySales.add(weekSales);
        }

        return monthlySales;
    }

    private List<Double> calculateYearlySales(List<OrderDto> orders) {
        LocalDateTime now = LocalDateTime.now();
        List<Double> yearlySales = new ArrayList<>();

        for (int quarter = 3; quarter >= 0; quarter--) {
            LocalDateTime quarterStart = now.minusMonths(quarter * 3L).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime quarterEnd = quarterStart.plusMonths(3);

            double quarterSales = orders.stream()
                    .filter(order -> order.getCreatedAt().isAfter(quarterStart)
                            && order.getCreatedAt().isBefore(quarterEnd))
                    .filter(order -> order.getOrderStatus() == OrderStatus.COMPLETED)
                    .mapToDouble(OrderDto::getTotalPrice)
                    .sum();

            yearlySales.add(quarterSales);
        }

        return yearlySales;
    }
}