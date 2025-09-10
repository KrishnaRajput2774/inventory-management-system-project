package com.rk.inventory_management_system.ui.nventory_management_system_ui.controllers;

import com.rk.inventory_management_system.ui.nventory_management_system_ui.Clients.CategoriesClient;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.ProductCategoryDto;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.ProductDto;
import groovy.util.logging.Slf4j;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDate;
import java.util.*;
import java.util.stream.Collectors;

@lombok.extern.slf4j.Slf4j
@Controller
@RequestMapping("/categories")
@RequiredArgsConstructor
@Slf4j
public class CategoryController {

    private final CategoriesClient categoriesClient;

    @GetMapping
    public String list(Model model) {
        try {
            List<ProductCategoryDto> categories = categoriesClient.findAll();
            model.addAttribute("categories", categories);

            // Calculate analytics from existing data
            int totalCategories = categories.size();
            model.addAttribute("totalCategories", totalCategories);

            // Count active categories (assuming status field exists, default to all if not)
            long activeCategories = categories.stream()
                    .filter(cat -> cat.getStatus() == null || !"INACTIVE".equals(cat.getStatus()))
                    .count();
            model.addAttribute("activeCategories", activeCategories);

            // Get product counts for each category and calculate metrics
            Map<Long, Integer> productCounts = new HashMap<>();
            int totalProducts = 0;

            for (ProductCategoryDto category : categories) {
                try {
                    List<ProductDto> products = categoriesClient.findProductsByCategoryId(category.getId());
                    int productCount = products.size();
                    productCounts.put(category.getId(), productCount);
                    totalProducts += productCount;
                    log.info("total Products: {}",totalProducts);
                    // Set product count in category object for display
                    category.setProductCount(productCount);
                } catch (Exception e) {
                    log.warn("Could not fetch products for category {}: {}", category.getId(), e.getMessage());
                    productCounts.put(category.getId(), 0);
                    category.setProductCount(0);
                }
            }

            model.addAttribute("totalProducts", totalProducts);

            // Calculate categories with products
            long categoriesWithProducts = productCounts.values().stream()
                    .filter(count -> count > 0)
                    .count();
            model.addAttribute("categoriesWithProducts", categoriesWithProducts);

            // Calculate average products per category
            double avgProductsPerCategory = totalCategories > 0 ?
                    (double) totalProducts / totalCategories : 0.0;
            model.addAttribute("avgProductsPerCategory", avgProductsPerCategory);

            // Categories needing attention (no products)
            long categoriesNeedingAttention = productCounts.values().stream()
                    .filter(count -> count == 0)
                    .count();
            model.addAttribute("categoriesNeedingAttention", categoriesNeedingAttention);

            // Top categories by product count
            List<ProductCategoryDto> topCategories = categories.stream()
                    .filter(cat -> productCounts.get(cat.getId()) > 0)
                    .sorted((a, b) -> Integer.compare(
                            productCounts.get(b.getId()),
                            productCounts.get(a.getId())))
                    .limit(5)
                    .collect(Collectors.toList());
            model.addAttribute("topCategories", topCategories);

            // New categories this month (if createdDate exists)
            long newCategoriesThisMonth = categories.stream()
                    .filter(cat -> cat.getCreatedDate() != null &&
                            cat.getCreatedDate().isAfter(ChronoLocalDate.from(LocalDateTime.now().withDayOfMonth(1))))
                    .count();
            model.addAttribute("newCategoriesThisMonth", newCategoriesThisMonth);

        } catch (Exception e) {
            log.error("Error loading categories", e);
            model.addAttribute("categories", Collections.emptyList());
            model.addAttribute("errorMessage", "Failed to load categories");

            // Set default values for analytics
            model.addAttribute("totalCategories", 0);
            model.addAttribute("activeCategories", 0);
            model.addAttribute("categoriesWithProducts", 0);
            model.addAttribute("totalProducts", 0);
            model.addAttribute("avgProductsPerCategory", 0.0);
            model.addAttribute("categoriesNeedingAttention", 0);
            model.addAttribute("newCategoriesThisMonth", 0);
            model.addAttribute("topCategories", Collections.emptyList());
        }

        return "categories/list";
    }

    @GetMapping("/all")
    @ResponseBody
    public ResponseEntity<List<ProductCategoryDto>> getAllCategories() {
        try {
            List<ProductCategoryDto> categories = categoriesClient.findAll();
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            log.error("Error fetching all categories", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/analytics")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCategoryAnalytics() {
        try {
            List<ProductCategoryDto> categories = categoriesClient.findAll();

            Map<String, Object> analytics = new HashMap<>();
            analytics.put("totalCategories", categories.size());

            // Calculate active categories
            long activeCategories = categories.stream()
                    .filter(cat -> cat.getStatus() == null || !"INACTIVE".equals(cat.getStatus()))
                    .count();
            analytics.put("activeCategories", activeCategories);

            // Calculate total products across all categories
            int totalProducts = 0;
            for (ProductCategoryDto category : categories) {
                try {
                    List<ProductDto> products = categoriesClient.findProductsByCategoryId(category.getId());
                    totalProducts += products.size();
                } catch (Exception e) {
                    log.warn("Could not fetch products for category {}", category.getId());
                }
            }

            analytics.put("totalProducts", totalProducts);
            analytics.put("avgProductsPerCategory", categories.isEmpty() ? 0 :
                    (double) totalProducts / categories.size());

            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            log.error("Error fetching category analytics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("category", new ProductCategoryDto());
        return "categories/create";
    }

    @PostMapping("/create")
    public String create(@ModelAttribute @Valid ProductCategoryDto categoryDto,
                         BindingResult result,
                         RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return "categories/create";
        }

        try {
            ProductCategoryDto created = categoriesClient.create(categoryDto);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Category '" + created.getName() + "' created successfully");
            return "redirect:/categories";

        } catch (Exception exception) {
            log.error("Failed to create category", exception);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to create category: " + exception.getMessage());
            return "categories/create";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            ProductCategoryDto category = categoriesClient.findById(id);
            List<ProductDto> products = categoriesClient.findProductsByCategoryId(id);

            // Basic metrics
            int totalProducts = products.size();
            int productCount = totalProducts; // For backward compatibility

            // Financial analytics
            double totalInvestment = products.stream()
                    .filter(p -> p.getActualPrice() != null)
                    .mapToDouble(ProductDto::getActualPrice)
                    .sum();

            double totalPotentialRevenue = products.stream()
                    .filter(p -> p.getSellingPrice() != null)
                    .mapToDouble(ProductDto::getSellingPrice)
                    .sum();

            double totalPotentialProfit = totalPotentialRevenue - totalInvestment;

            double avgSellingPrice = products.isEmpty() ? 0.0 :
                    products.stream()
                            .filter(p -> p.getSellingPrice() != null)
                            .mapToDouble(ProductDto::getSellingPrice)
                            .average()
                            .orElse(0.0);

            // Stock analytics
            int totalStock = products.stream()
                    .filter(p -> p.getStockQuantity() != null)
                    .mapToInt(ProductDto::getStockQuantity)
                    .sum();

            int lowStockCount = (int) products.stream()
                    .filter(p -> p.getStockQuantity() != null &&
                            p.getLowStockThreshold() != null &&
                            p.getStockQuantity() <= p.getLowStockThreshold())
                    .count();

            int outOfStockCount = (int) products.stream()
                    .filter(p -> p.getStockQuantity() == null || p.getStockQuantity() == 0)
                    .count();

            int inStockCount = totalProducts - outOfStockCount;

            // Sales analytics
            int totalSales = products.stream()
                    .filter(p -> p.getQuantitySold() != null)
                    .mapToInt(ProductDto::getQuantitySold)
                    .sum();

            // Performance metrics
            double avgProfitMargin = products.stream()
                    .filter(p -> p.getActualPrice() != null && p.getSellingPrice() != null && p.getSellingPrice() > 0)
                    .mapToDouble(p -> {
                        double profit = p.getSellingPrice() - p.getActualPrice();
                        return (profit / p.getSellingPrice()) * 100;
                    })
                    .average()
                    .orElse(0.0);

            long healthyProducts = products.stream()
                    .filter(p -> p.getStockQuantity() > p.getLowStockThreshold())
                    .count();

            double stockHealthPercentage = totalProducts > 0 ?
                    ((double) healthyProducts / totalProducts) * 100 : 100.0;

            // Brand and supplier analytics
            Map<String, Long> brandDistribution = products.stream()
                    .collect(Collectors.groupingBy(
                            product -> product.getBrandName() != null && !product.getBrandName().isEmpty() ?
                                    product.getBrandName() : "No Brand",
                            Collectors.counting()));

            Map<String, Long> supplierDistribution = products.stream()
                    .collect(Collectors.groupingBy(
                            product -> {
                                if (product.getSupplier() != null && product.getSupplier().getName() != null) {
                                    return product.getSupplier().getName();
                                }
                                return "No Supplier";
                            },
                            Collectors.counting()));

            // Price range distribution
            Map<String, Long> priceRangeDistribution = products.stream()
                    .filter(p -> p.getSellingPrice() != null)
                    .collect(Collectors.groupingBy(
                            product -> {
                                double price = product.getSellingPrice();
                                if (price < 100) return "Under $100";
                                else if (price < 500) return "$100 - $500";
                                else if (price < 1000) return "$500 - $1000";
                                else if (price < 5000) return "$1000 - $5000";
                                else return "Above $5000";
                            },
                            Collectors.counting()));

            // Top performing products
            List<ProductDto> topSellingProducts = products.stream()
                    .filter(p -> p.getQuantitySold() != null)
                    .sorted((p1, p2) -> Integer.compare(p2.getQuantitySold(), p1.getQuantitySold()))
                    .limit(5)
                    .collect(Collectors.toList());

            List<ProductDto> mostProfitableProducts = products.stream()
                    .filter(p -> p.getActualPrice() != null && p.getSellingPrice() != null)
                    .sorted((p1, p2) -> {
                        double profit1 = p1.getSellingPrice() - p1.getActualPrice();
                        double profit2 = p2.getSellingPrice() - p2.getActualPrice();
                        return Double.compare(profit2, profit1);
                    })
                    .limit(5)
                    .collect(Collectors.toList());

            List<ProductDto> lowStockProducts = products.stream()
                    .filter(p -> p.getStockQuantity() != null)
                    .sorted(Comparator.comparing(ProductDto::getStockQuantity))
                    .limit(5)
                    .collect(Collectors.toList());

            // Recent products (assuming you have a creation date field)
            List<ProductDto> recentProducts = products.stream()
                    .limit(5)
                    .collect(Collectors.toList());

            // Category health score (0-100)
            double categoryHealthScore = calculateCategoryHealthScore(
                    stockHealthPercentage, avgProfitMargin, totalProducts, inStockCount);

            // Add all attributes to model
            model.addAttribute("category", category);
            model.addAttribute("products", products);
            model.addAttribute("productCount", productCount);
            model.addAttribute("totalProducts", totalProducts);

            // Financial metrics
            model.addAttribute("totalInvestment", totalInvestment);
            model.addAttribute("totalPotentialRevenue", totalPotentialRevenue);
            model.addAttribute("totalPotentialProfit", totalPotentialProfit);
            model.addAttribute("avgSellingPrice", avgSellingPrice);
            model.addAttribute("avgProfitMargin", avgProfitMargin);

            // Stock metrics
            model.addAttribute("totalStock", totalStock);
            model.addAttribute("lowStockCount", lowStockCount);
            model.addAttribute("outOfStockCount", outOfStockCount);
            model.addAttribute("inStockCount", inStockCount);
            model.addAttribute("stockHealthPercentage", stockHealthPercentage);

            // Sales metrics
            model.addAttribute("totalSales", totalSales);

            // Distributions
            model.addAttribute("brandDistribution", brandDistribution);
            model.addAttribute("supplierDistribution", supplierDistribution);
            model.addAttribute("priceRangeDistribution", priceRangeDistribution);

            // Top performers
            model.addAttribute("topSellingProducts", topSellingProducts);
            model.addAttribute("mostProfitableProducts", mostProfitableProducts);
            model.addAttribute("lowStockProducts", lowStockProducts);
            model.addAttribute("recentProducts", recentProducts);

            // Overall health
            model.addAttribute("categoryHealthScore", categoryHealthScore);

            return "categories/detail";

        } catch (Exception e) {
            log.error("Error loading category details for id: " + id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Category not found or could not be loaded");
            return "redirect:/categories";
        }
    }

    private double calculateCategoryHealthScore(double stockHealth, double profitMargin,
                                                int totalProducts, int inStockCount) {
        if (totalProducts == 0) return 0.0;

        // Stock health weight: 40%
        double stockScore = (stockHealth / 100.0) * 40;

        // Profit margin weight: 30% (normalize to 0-1 range, assuming 50% is excellent)
        double profitScore = Math.min(profitMargin / 50.0, 1.0) * 30;

        // Product availability weight: 20%
        double availabilityScore = ((double) inStockCount / totalProducts) * 20;

        // Product diversity weight: 10% (more products = better diversity)
        double diversityScore = Math.min(totalProducts / 50.0, 1.0) * 10;

        return Math.min(stockScore + profitScore + availabilityScore + diversityScore, 100.0);
    }

    @GetMapping("/{id}/products")
    public String products(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            ProductCategoryDto category = categoriesClient.findById(id);
            List<ProductDto> products = categoriesClient.findProductsByCategoryId(id);

            // Calculate comprehensive product analytics
            int totalProducts = products.size();

            // Calculate total investment and potential revenue
            double totalInvestment = products.stream()
                    .filter(p -> p.getActualPrice() != null)
                    .mapToDouble(ProductDto::getActualPrice)
                    .sum();

            double totalPotentialRevenue = products.stream()
                    .filter(p -> p.getSellingPrice() != null)
                    .mapToDouble(ProductDto::getSellingPrice)
                    .sum();

            double totalPotentialProfit = totalPotentialRevenue - totalInvestment;

            // Calculate average prices
            double avgActualPrice = products.isEmpty() ? 0.0 :
                    products.stream()
                            .filter(p -> p.getActualPrice() != null)
                            .mapToDouble(ProductDto::getActualPrice)
                            .average()
                            .orElse(0.0);

            double avgSellingPrice = products.isEmpty() ? 0.0 :
                    products.stream()
                            .filter(p -> p.getSellingPrice() != null)
                            .mapToDouble(ProductDto::getSellingPrice)
                            .average()
                            .orElse(0.0);

            // Calculate stock metrics
            int totalStock = products.stream()
                    .filter(p -> p.getStockQuantity() != null)
                    .mapToInt(ProductDto::getStockQuantity)
                    .sum();

            int lowStockCount = (int) products.stream()
                    .filter(p -> p.getStockQuantity() != null &&
                            p.getLowStockThreshold() != null &&
                            p.getStockQuantity() <= p.getLowStockThreshold())
                    .count();

            int outOfStockProducts = (int) products.stream()
                    .filter(p -> p.getStockQuantity() == null || p.getStockQuantity() == 0)
                    .count();

            // Find most profitable products (highest margin)
            List<ProductDto> mostProfitableProducts = products.stream()
                    .filter(p -> p.getActualPrice() != null && p.getSellingPrice() != null)
                    .sorted((p1, p2) -> {
                        double profit1 = p1.getSellingPrice() - p1.getActualPrice();
                        double profit2 = p2.getSellingPrice() - p2.getActualPrice();
                        return Double.compare(profit2, profit1);
                    })
                    .limit(5)
                    .collect(Collectors.toList());

            // Find lowest stock products
            List<ProductDto> lowStockProducts = products.stream()
                    .filter(p -> p.getStockQuantity() != null)
                    .sorted(Comparator.comparing(ProductDto::getStockQuantity))
                    .limit(5)
                    .collect(Collectors.toList());

            // Calculate brand distribution
            Map<String, Long> productsByBrand = products.stream()
                    .collect(Collectors.groupingBy(
                            product -> product.getBrandName() != null && !product.getBrandName().isEmpty() ?
                                    product.getBrandName() : "No Brand",
                            Collectors.counting()));

            // Calculate supplier distribution (handle null suppliers safely)
            Map<String, Long> productsBySupplier = products.stream()
                    .collect(Collectors.groupingBy(
                            product -> {
                                if (product.getSupplier() != null && product.getSupplier().getName() != null) {
                                    return product.getSupplier().getName();
                                }
                                return "No Supplier";
                            },
                            Collectors.counting()));

            // Calculate price range distribution
            Map<String, Long> priceRangeDistribution = products.stream()
                    .filter(p -> p.getSellingPrice() != null)
                    .collect(Collectors.groupingBy(
                            product -> {
                                double price = product.getSellingPrice();
                                if (price < 100) return "Under $100";
                                else if (price < 500) return "$100 - $500";
                                else if (price < 1000) return "$500 - $1000";
                                else if (price < 5000) return "$1000 - $5000";
                                else return "Above $5000";
                            },
                            Collectors.counting()));

            // Calculate performance metrics
            double avgProfitMargin = products.stream()
                    .filter(p -> p.getActualPrice() != null && p.getSellingPrice() != null && p.getSellingPrice() > 0)
                    .mapToDouble(p -> {
                        double profit = p.getSellingPrice() - p.getActualPrice();
                        return (profit / p.getSellingPrice()) * 100;
                    })
                    .average()
                    .orElse(0.0);

            // Stock health percentage
            double stockHealthPercentage = totalProducts > 0 ?
                    ((double)(totalProducts - lowStockCount) / totalProducts) * 100 : 100.0;

            // Legacy calculations for backward compatibility
            double averagePrice = products.stream()
                    .filter(product -> product.getSellingPrice() != null)
                    .mapToDouble(ProductDto::getSellingPrice)
                    .average()
                    .orElse(0.0);

            double totalValue = products.stream()
                    .filter(product -> product.getSellingPrice() != null && product.getStockQuantity() != null)
                    .mapToDouble(product -> product.getSellingPrice() * product.getStockQuantity())
                    .sum();

            // Top brands in this category (legacy)
            Map<String, Long> brandCounts = products.stream()
                    .filter(product -> product.getBrandName() != null && !product.getBrandName().isEmpty())
                    .collect(Collectors.groupingBy(ProductDto::getBrandName, Collectors.counting()));

            List<Map.Entry<String, Long>> topBrands = brandCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(5)
                    .collect(Collectors.toList());

            // Add all attributes to model
            model.addAttribute("category", category);
            model.addAttribute("products", products);

            // Enhanced analytics
            model.addAttribute("totalProducts", totalProducts);
            model.addAttribute("totalInvestment", totalInvestment);
            model.addAttribute("totalPotentialRevenue", totalPotentialRevenue);
            model.addAttribute("totalPotentialProfit", totalPotentialProfit);
            model.addAttribute("avgActualPrice", avgActualPrice);
            model.addAttribute("avgSellingPrice", avgSellingPrice);
            model.addAttribute("totalStock", totalStock);
            model.addAttribute("lowStockCount", lowStockCount);
            model.addAttribute("outOfStockProducts", outOfStockProducts);
            model.addAttribute("mostProfitableProducts", mostProfitableProducts);
            model.addAttribute("lowStockProducts", lowStockProducts);
            model.addAttribute("productsByBrand", productsByBrand);
            model.addAttribute("productsBySupplier", productsBySupplier);
            model.addAttribute("priceRangeDistribution", priceRangeDistribution);
            model.addAttribute("avgProfitMargin", avgProfitMargin);
            model.addAttribute("stockHealthPercentage", stockHealthPercentage);

            // Legacy attributes for backward compatibility
            model.addAttribute("averagePrice", averagePrice);
            model.addAttribute("totalValue", totalValue);
            model.addAttribute("topBrands", topBrands);
            model.addAttribute("lowStockProducts", lowStockProducts); // For legacy template compatibility

            return "categories/products";

        } catch (Exception e) {
            log.error("Error loading products for category: " + id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to load products for this category");
            return "redirect:/categories";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         RedirectAttributes redirectAttributes) {

        try {
            ProductCategoryDto deleted = categoriesClient.delete(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Category '" + deleted.getName() + "' deleted successfully");

        } catch (Exception exception) {
            log.error("Error deleting category with id: " + id, exception);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to delete category: " + exception.getMessage());
        }
        return "redirect:/categories";
    }

    @GetMapping("/export")
    public ResponseEntity<String> exportCategories() {
        try {
            List<ProductCategoryDto> categories = categoriesClient.findAll();

            StringBuilder csv = new StringBuilder();
            csv.append("ID,Name,Description,Status,Product Count,Created Date\n");

            for (ProductCategoryDto category : categories) {
                // Get product count for each category
                int productCount = 0;
                try {
                    List<ProductDto> products = categoriesClient.findProductsByCategoryId(category.getId());
                    productCount = products.size();
                } catch (Exception e) {
                    log.warn("Could not fetch products for category {} during export", category.getId());
                }

                csv.append(String.format("%d,\"%s\",\"%s\",\"%s\",%d,\"%s\"\n",
                        category.getId(),
                        category.getName() != null ? category.getName().replace("\"", "\"\"") : "",
                        category.getDescription() != null ? category.getDescription().replace("\"", "\"\"") : "",
                        category.getStatus() != null ? category.getStatus() : "ACTIVE",
                        productCount,
                        category.getCreatedDate() != null ? category.getCreatedDate().toString() : ""
                ));
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment",
                    "categories_export_" + LocalDate.now() + ".csv");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(csv.toString());

        } catch (Exception e) {
            log.error("Error exporting categories", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}