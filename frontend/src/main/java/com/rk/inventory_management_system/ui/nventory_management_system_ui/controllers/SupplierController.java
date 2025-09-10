package com.rk.inventory_management_system.ui.nventory_management_system_ui.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.Clients.ProductsClient;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.Clients.SuppliersClient;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.CustomerDto;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.ProductDto;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.SupplierDto;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.supplierDtos.SupplierProductsResponseDto;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.supplierDtos.SupplierResponseDto;
import groovy.util.logging.Slf4j;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/suppliers")
public class SupplierController {

    private static final Logger log = LoggerFactory.getLogger(SupplierController.class);
    private final SuppliersClient suppliersClient;
    private final ProductsClient productsClient;

    @GetMapping
    public String list(Model model) {
        try {
            List<SupplierResponseDto> suppliers = suppliersClient.findAll();

            // Calculate basic metrics
            int totalSuppliers = suppliers.size();

            // Calculate total products across all suppliers
            int totalProducts = suppliers.stream()
                    .mapToInt(supplier -> supplier.getProductsCount() != null ?
                            supplier.getProductsCount() : 0)
                    .sum();

            // Calculate average products per supplier
            double avgProductsPerSupplier = suppliers.isEmpty() ? 0.0 :
                    (double) totalProducts / suppliers.size();

            // Find top performing suppliers (by product count)
            List<SupplierResponseDto> topSuppliers = suppliers.stream()
                    .sorted((s1, s2) -> Integer.compare(
                            s2.getProductsCount() != null ? s2.getProductsCount() : 0,
                            s1.getProductsCount() != null ? s1.getProductsCount() : 0))
                    .limit(5)
                    .collect(Collectors.toList());

            // Calculate partnership duration statistics (if createdAt is available)
            double avgPartnershipMonths = calculateAveragePartnershipDuration(suppliers);

            // Find suppliers needing attention (low product count)
            int suppliersNeedingAttention = (int) suppliers.stream()
                    .filter(supplier -> supplier.getProductsCount() != null &&
                            supplier.getProductsCount() < 2)
                    .count();

            // Calculate total supplier value/score
            double totalSupplierValue = suppliers.stream()
                    .mapToDouble(this::calculateSupplierValue)
                    .sum();

            // Add all attributes to model
            model.addAttribute("suppliers", suppliers);
            model.addAttribute("totalSuppliers", totalSuppliers);
            model.addAttribute("totalProducts", totalProducts);
            model.addAttribute("avgProductsPerSupplier", avgProductsPerSupplier);
            model.addAttribute("topSuppliers", topSuppliers);
            model.addAttribute("avgPartnershipMonths", avgPartnershipMonths);
            model.addAttribute("suppliersNeedingAttention", suppliersNeedingAttention);
            model.addAttribute("totalSupplierValue", totalSupplierValue);

            return "suppliers/list";

        } catch (Exception e) {
            log.error("Error fetching suppliers list", e);
            model.addAttribute("error", "Unable to fetch suppliers data");
            return "suppliers/list";
        }
    }

    @GetMapping("/create")
    public String create(Model model) {
        model.addAttribute("supplier", new SupplierDto());
        return "suppliers/create";
    }

    @PostMapping("/create")
    public String create(@Valid @ModelAttribute SupplierDto formDto,
                         BindingResult result,
                         RedirectAttributes redirectAttributes,
                         HttpServletRequest request,
                         Model model) {

        SupplierDto supplier = null;

        try {
            if (request.getContentType() != null &&
                    request.getContentType().contains("application/json")) {
                // Read JSON manually
                String json = request.getReader().lines().collect(Collectors.joining());
                ObjectMapper mapper = new ObjectMapper();
                supplier = mapper.readValue(json, SupplierDto.class);
            } else {
                // Handle normal form submission
                supplier = formDto;
            }
        } catch (Exception e) {
            log.error("Error reading request body", e);
            redirectAttributes.addFlashAttribute("error", "Invalid request format");
            return "redirect:/suppliers/create";
        }

        if (result.hasErrors()) {
            model.addAttribute("supplier", supplier);
            return "suppliers/create";
        }

        try {
            SupplierDto created = suppliersClient.create(supplier);
            redirectAttributes.addFlashAttribute("success",
                    "Supplier '" + created.getName() + "' created successfully!");
            return "redirect:/suppliers";

        } catch (Exception exception) {
            log.error("Error creating supplier", exception);
            redirectAttributes.addFlashAttribute("error",
                    "Failed to create supplier. Please try again.");
            model.addAttribute("supplier", supplier);
            return "suppliers/create";
        }
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            SupplierResponseDto supplier = suppliersClient.findSupplierById(id);
            List<SupplierProductsResponseDto> products = suppliersClient.findAllProductsBySupplierId(id);

            // Calculate supplier-specific metrics using ACTUAL PRICE (cost from supplier)
            int totalProducts = products.size();

            // Use actualPrice for supplier cost analysis
            double totalSupplierCost = products.stream()
                    .mapToDouble(product -> product.getActualPrice() != null ? product.getActualPrice() : 0.0)
                    .sum();

            // Calculate potential revenue using selling price
            double totalPotentialRevenue = products.stream()
                    .mapToDouble(product -> product.getSellingPrice() != null ? product.getSellingPrice() : 0.0)
                    .sum();

            // Calculate average costs and prices
            double avgSupplierCost = products.isEmpty() ? 0.0 : totalSupplierCost / products.size();
            double avgSellingPrice = products.isEmpty() ? 0.0 : totalPotentialRevenue / products.size();

            // Calculate potential profit margin
            double potentialProfit = totalPotentialRevenue - totalSupplierCost;
            double profitMarginPercentage = totalPotentialRevenue > 0 ?
                    (potentialProfit / totalPotentialRevenue) * 100 : 0.0;

            // Calculate supplier performance score
            double performanceScore = calculateSupplierPerformanceScore(supplier, products);

            model.addAttribute("supplier", supplier);
            model.addAttribute("products", products);
            model.addAttribute("totalProducts", totalProducts);
            model.addAttribute("totalSupplierCost", totalSupplierCost);
            model.addAttribute("totalPotentialRevenue", totalPotentialRevenue);
            model.addAttribute("avgSupplierCost", avgSupplierCost);
            model.addAttribute("avgSellingPrice", avgSellingPrice);
            model.addAttribute("potentialProfit", potentialProfit);
            model.addAttribute("profitMarginPercentage", profitMarginPercentage);
            model.addAttribute("performanceScore", performanceScore);

            return "suppliers/detail";

        } catch (Exception e) {
            log.error("Error fetching supplier details for ID: {}", id, e);
            redirectAttributes.addFlashAttribute("error", "Supplier not found");
            return "redirect:/suppliers";
        }
    }

    @GetMapping("/{id}/products")
    public String products(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            SupplierResponseDto supplier = suppliersClient.findSupplierById(id);
            List<SupplierProductsResponseDto> products = suppliersClient.findAllProductsBySupplierId(id);

            // Calculate comprehensive product analytics
            int totalProducts = products.size();

            // Calculate total investment and potential revenue
            double totalInvestment = products.stream()
                    .mapToDouble(p -> p.getActualPrice() != null ? p.getActualPrice() : 0.0)
                    .sum();

            double totalPotentialRevenue = products.stream()
                    .mapToDouble(p -> p.getSellingPrice() != null ? p.getSellingPrice() : 0.0)
                    .sum();

            double totalPotentialProfit = totalPotentialRevenue - totalInvestment;

            // Calculate average prices
            double avgActualPrice = products.isEmpty() ? 0.0 : totalInvestment / products.size();
            double avgSellingPrice = products.isEmpty() ? 0.0 : totalPotentialRevenue / products.size();

            // Calculate stock metrics
            int totalStock = products.stream()
                    .mapToInt(p -> p.getStockQuantity() != null ? p.getStockQuantity() : 0)
                    .sum();

            int lowStockCount = (int) products.stream()
                    .filter(p -> p.getStockQuantity() != null && p.getStockQuantity() < p.getLowStockThreshold())
                    .count();

            // Find top products by ACTUAL PRICE (most expensive to source)
            List<SupplierProductsResponseDto> topCostProducts = products.stream()
                    .sorted((p1, p2) -> Double.compare(
                            p2.getActualPrice() != null ? p2.getActualPrice() : 0.0,
                            p1.getActualPrice() != null ? p1.getActualPrice() : 0.0))
                    .limit(5)
                    .collect(Collectors.toList());

            // Find most profitable products (highest margin)
            List<SupplierProductsResponseDto> mostProfitableProducts = products.stream()
                    .filter(p -> p.getActualPrice() != null && p.getSellingPrice() != null)
                    .sorted((p1, p2) -> {
                        double profit1 = p1.getSellingPrice() - p1.getActualPrice();
                        double profit2 = p2.getSellingPrice() - p2.getActualPrice();
                        return Double.compare(profit2, profit1);
                    })
                    .limit(5)
                    .collect(Collectors.toList());

            // Find lowest stock products
            List<SupplierProductsResponseDto> lowStockProducts = products.stream()
                    .filter(p -> p.getStockQuantity() != null)
                    .sorted((p1, p2) -> Integer.compare(
                            p1.getStockQuantity(), p2.getStockQuantity()))
                    .limit(5)
                    .collect(Collectors.toList());

            // Calculate category distribution (handle null categories safely)
            Map<String, Long> productsByCategory = products.stream()
                    .collect(Collectors.groupingBy(
                            product -> {
                                if (product.getCategory() != null && product.getCategory().getName() != null) {
                                    return product.getCategory().getName();
                                }
                                return "Uncategorized";
                            },
                            Collectors.counting()));

            // Calculate brand distribution
            Map<String, Long> productsByBrand = products.stream()
                    .collect(Collectors.groupingBy(
                            product -> product.getBrandName() != null ?
                                    product.getBrandName() : "No Brand",
                            Collectors.counting()));

            // Calculate price range distribution
            Map<String, Long> priceRangeDistribution = products.stream()
                    .filter(p -> p.getSellingPrice() != null)
                    .collect(Collectors.groupingBy(
                            product -> {
                                double price = product.getSellingPrice();
                                if (price < 100) return "Under ₹100";
                                else if (price < 500) return "₹100 - ₹500";
                                else if (price < 1000) return "₹500 - ₹1000";
                                else if (price < 5000) return "₹1000 - ₹5000";
                                else return "Above ₹5000";
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

            // Add all attributes to model
            model.addAttribute("supplier", supplier);
            model.addAttribute("products", products);
            model.addAttribute("totalProducts", totalProducts);
            model.addAttribute("totalInvestment", totalInvestment);
            model.addAttribute("totalPotentialRevenue", totalPotentialRevenue);
            model.addAttribute("totalPotentialProfit", totalPotentialProfit);
            model.addAttribute("avgActualPrice", avgActualPrice);
            model.addAttribute("avgSellingPrice", avgSellingPrice);
            model.addAttribute("totalStock", totalStock);
            model.addAttribute("lowStockCount", lowStockCount);
            model.addAttribute("topCostProducts", topCostProducts);
            model.addAttribute("mostProfitableProducts", mostProfitableProducts);
            model.addAttribute("lowStockProducts", lowStockProducts);
            model.addAttribute("productsByCategory", productsByCategory);
            model.addAttribute("productsByBrand", productsByBrand);
            model.addAttribute("priceRangeDistribution", priceRangeDistribution);
            model.addAttribute("avgProfitMargin", avgProfitMargin);
            model.addAttribute("stockHealthPercentage", stockHealthPercentage);

            return "suppliers/products";

        } catch (Exception e) {
            log.error("Error fetching supplier products for ID: {}", id, e);
            redirectAttributes.addFlashAttribute("error", "Unable to fetch supplier products");
            return "redirect:/suppliers";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            SupplierDto deleted = suppliersClient.delete(id);
            redirectAttributes.addFlashAttribute("success",
                    "Supplier '" + deleted.getName() + "' deleted successfully!");

        } catch (Exception exception) {
            log.error("Error deleting supplier with ID: {}", id, exception);
            redirectAttributes.addFlashAttribute("error",
                    "Failed to delete supplier. It may have associated products.");
        }

        return "redirect:/suppliers";
    }

    // Helper methods
    private double calculateAveragePartnershipDuration(List<SupplierResponseDto> suppliers) {
        LocalDateTime now = LocalDateTime.now();
        List<SupplierResponseDto> suppliersWithDates = suppliers.stream()
                .filter(supplier -> supplier.getCreatedAt() != null)
                .collect(Collectors.toList());

        if (suppliersWithDates.isEmpty()) {
            return 0.0;
        }

        return suppliersWithDates.stream()
                .mapToLong(supplier -> ChronoUnit.MONTHS.between(supplier.getCreatedAt(), now))
                .average()
                .orElse(0.0);
    }

    private double calculateSupplierValue(SupplierResponseDto supplier) {
        double score = 0.0;

        if (supplier.getProductsCount() != null) {
            score += supplier.getProductsCount() * 10;
        }

        if (supplier.getCreatedAt() != null) {
            long monthsActive = ChronoUnit.MONTHS.between(supplier.getCreatedAt(), LocalDateTime.now());
            score += monthsActive * 2;
        }

        score += 20; // Base score

        return score;
    }

    private double calculateSupplierPerformanceScore(SupplierResponseDto supplier,
                                                     List<SupplierProductsResponseDto> products) {
        double score = 0.0;

        // Product diversity score
        score += products.size() * 5;

        // Cost efficiency score (lower average cost = better)
        // Use actualPrice for supplier cost analysis
        double avgCost = products.stream()
                .mapToDouble(p -> p.getActualPrice() != null ? p.getActualPrice() : 0.0)
                .average()
                .orElse(0.0);

        // Inverse scoring - lower cost gets higher score (capped for reasonableness)
        if (avgCost > 0) {
            score += Math.min(1000 / avgCost, 20); // Max 20 points for cost efficiency
        }

        // Profit potential score
        double avgProfitMargin = products.stream()
                .filter(p -> p.getActualPrice() != null && p.getSellingPrice() != null)
                .mapToDouble(p -> {
                    double profit = p.getSellingPrice() - p.getActualPrice();
                    return p.getSellingPrice() > 0 ? (profit / p.getSellingPrice()) * 100 : 0;
                })
                .average()
                .orElse(0.0);

        score += avgProfitMargin / 2; // Add half of profit margin percentage

        // Partnership duration score
        if (supplier.getCreatedAt() != null) {
            long months = ChronoUnit.MONTHS.between(supplier.getCreatedAt(), LocalDateTime.now());
            score += Math.min(months * 1.5, 30); // Max 30 points for longevity
        }

        return Math.min(score, 100.0); // Cap at 100
    }
}