package com.rk.inventory_management_system.ui.nventory_management_system_ui.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.Clients.CustomersClient;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.Clients.OrdersClient;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.CustomerDto;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.OrderDto;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.dtos.enums.OrderStatus;
import com.rk.inventory_management_system.ui.nventory_management_system_ui.utils.FormatUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
    @Controller
    @Slf4j
    @RequiredArgsConstructor
    @RequestMapping("/customers")
    public class CustomerController {

        private final CustomersClient customersClient;
        private final OrdersClient ordersClient;

        @GetMapping
        public String list(Model model) {
            try {
                List<CustomerDto> customers = customersClient.findAll();

                // Calculate comprehensive customer analytics
                int totalCustomers = customers.size();
                int activeCustomers = 0;
                int vipCustomers = 0;
                double totalRevenue = 0.0;
                int totalOrders = 0;

                // Enhanced analytics for each customer - calculate all metrics in controller
                for (CustomerDto customer : customers) {
                    try {
                        List<OrderDto> customerOrders = ordersClient.findAllOrdersOfCustomer(customer.getCustomerId());
                        customer.setOrders(customerOrders);

                        // Calculate customer revenue (excluding cancelled orders)
                        double customerRevenue = 0.0;
                        int customerOrderCount = customerOrders.size();

                        for (OrderDto order : customerOrders) {
                            if (!OrderStatus.CANCELLED.equals(order.getOrderStatus()) && order.getTotalPrice() != null) {
                                customerRevenue += order.getTotalPrice();
                            }
                        }

                        // Check for recent orders (last 3 months)
                        boolean hasRecentOrder = false;
                        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
                        for (OrderDto order : customerOrders) {
                            if (order.getCreatedAt() != null && order.getCreatedAt().isAfter(threeMonthsAgo)) {
                                hasRecentOrder = true;
                                break;
                            }
                        }

                        // Determine customer status
                        String customerStatus;
                        if (customerRevenue > 50000) {
                            customerStatus = "VIP";
                        } else if (customerRevenue > 10000 && hasRecentOrder) {
                            customerStatus = "PREMIUM";
                        } else if (hasRecentOrder) {
                            customerStatus = "ACTIVE";
                        } else if (customerOrderCount == 0) {
                            customerStatus = "NEW";
                        } else {
                            customerStatus = "INACTIVE";
                        }

                        // Set calculated values on customer object for Thymeleaf
                        customer.setRevenue(customerRevenue);
                        customer.setHasRecentOrder(hasRecentOrder);
                        customer.setCustomerStatus(customerStatus);
                        customer.setOrderCount(customerOrderCount);

                        // Count active customers and VIP customers
                        if (hasRecentOrder && customerOrderCount > 0) {
                            activeCustomers++;
                        }

                        if (customerRevenue > 10000 || customerOrderCount > 5) {
                            vipCustomers++;
                        }

                        totalRevenue += customerRevenue;
                        totalOrders += customerOrderCount;

                    } catch (Exception e) {
                        log.warn("Could not fetch orders for customer {}: {}", customer.getCustomerId(), e.getMessage());
                        // Set default values for customers with no orders
                        customer.setRevenue(0.0);
                        customer.setHasRecentOrder(false);
                        customer.setCustomerStatus("NEW");
                        customer.setOrderCount(0);
                    }
                }

                // Calculate average metrics
                double avgRevenuePerCustomer = totalCustomers > 0 ? totalRevenue / totalCustomers : 0.0;
                double avgOrdersPerCustomer = totalCustomers > 0 ? (double) totalOrders / totalCustomers : 0.0;

                // Calculate new customers this month
                int newCustomersThisMonth = 0;
                LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
                for (CustomerDto customer : customers) {
                    if (customer.getCreatedAt() != null && customer.getCreatedAt().isAfter(oneMonthAgo)) {
                        newCustomersThisMonth++;
                    }
                }

                // Calculate retention rate (customers with orders in last 6 months)
                int retainedCustomers = 0;
                LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
                for (CustomerDto customer : customers) {
                    if (customer.getOrders() != null) {
                        boolean hasRecentActivity = false;
                        for (OrderDto order : customer.getOrders()) {
                            if (order.getCreatedAt() != null && order.getCreatedAt().isAfter(sixMonthsAgo)) {
                                hasRecentActivity = true;
                                break;
                            }
                        }
                        if (hasRecentActivity) {
                            retainedCustomers++;
                        }
                    }
                }

                double retentionRate = totalCustomers > 0 ? (double) retainedCustomers / totalCustomers * 100 : 0.0;

                // Find top 5 customers by revenue
                List<CustomerDto> topCustomers = new ArrayList<>();
                customers.sort((c1, c2) -> Double.compare(c2.getRevenue(), c1.getRevenue()));
                for (int i = 0; i < Math.min(5, customers.size()); i++) {
                    if (customers.get(i).getRevenue() > 0) {
                        topCustomers.add(customers.get(i));
                    }
                }

                int customersNeedingAttention = totalCustomers - activeCustomers;

                // Add all attributes to model
                model.addAttribute("customers", customers);
                model.addAttribute("totalCustomers", totalCustomers);
                model.addAttribute("activeCustomers", activeCustomers);
                model.addAttribute("vipCustomers", vipCustomers);
                model.addAttribute("totalRevenue", totalRevenue);
                model.addAttribute("totalOrders", totalOrders);
                model.addAttribute("avgRevenuePerCustomer", avgRevenuePerCustomer);
                model.addAttribute("avgOrdersPerCustomer", avgOrdersPerCustomer);
                model.addAttribute("newCustomersThisMonth", newCustomersThisMonth);
                model.addAttribute("retentionRate", retentionRate);
                model.addAttribute("topCustomers", topCustomers);
                model.addAttribute("customersNeedingAttention", customersNeedingAttention);

                return "customers/list";

            } catch (Exception e) {
                log.error("Error fetching customers list", e);
                model.addAttribute("error", "Unable to fetch customers data");
                return "customers/list";
            }
        }

        @GetMapping("/create")
        public String addForm(Model model) {
            CustomerDto customer = null;
            List<CustomerDto> customers = customersClient.findAll();
            Integer totalCustomers = customers.size();

            Long newCustomerThisMonth = customers.stream().filter(customer1->
                            customer1.getCreatedAt()!=null)
                    .filter(customer1 -> {
                        LocalDate createdDate = customer1.getCreatedAt().toLocalDate();
                        LocalDate today = LocalDate.now();
                        return createdDate.getYear() == today.getYear()
                                && createdDate.getMonth() == today.getMonth();
                    }).count();

            model.addAttribute("totalCustomers",totalCustomers);
            model.addAttribute("newCustomerThisMonth",newCustomerThisMonth);

            model.addAttribute("customer", new CustomerDto());
            return "customers/create";
        }

        @PostMapping("/create")
        public String create(
                @Valid @ModelAttribute CustomerDto formDto,
                BindingResult result,
                Model model,
                RedirectAttributes redirectAttributes,
                HttpServletRequest request) {

            CustomerDto customer = null;
            List<CustomerDto> customers = customersClient.findAll();
            Integer totalCustomers = customers.size();

             Long newCustomerThisMonth = customers.stream().filter(customer1->
                    customer1.getCreatedAt()!=null)
                    .filter(customer1 -> {
                        LocalDate createdDate = customer1.getCreatedAt().toLocalDate();
                        LocalDate today = LocalDate.now();
                        return createdDate.getYear() == today.getYear()
                                && createdDate.getMonth() == today.getMonth();
                    }).count();

            model.addAttribute("totalCustomers",totalCustomers);
            model.addAttribute("newCustomerThisMonth",newCustomerThisMonth);

            try {
                if (request.getContentType() != null &&
                        request.getContentType().contains("application/json")) {
                    String json = request.getReader().lines().collect(Collectors.joining());
                    ObjectMapper mapper = new ObjectMapper();
                    customer = mapper.readValue(json, CustomerDto.class);
                } else {
                    customer = formDto;
                }
            } catch (Exception e) {
                log.error("Error reading request body", e);
                redirectAttributes.addFlashAttribute("error", "Invalid request format");
                return "redirect:/customers/create";
            }

            // Custom validation
            if (customer != null) {
                // Check for duplicate email
                try {
                    List<CustomerDto> existingCustomers = customersClient.findAll();
                    CustomerDto finalCustomer = customer;
                    boolean emailExists = existingCustomers.stream()
                            .anyMatch(c -> c.getEmail() != null &&
                                    c.getEmail().equalsIgnoreCase(finalCustomer.getEmail()));

                    if (emailExists) {
                        result.rejectValue("email", "error.customer",
                                "A customer with this email already exists");
                    }
                } catch (Exception e) {
                    log.warn("Could not check for duplicate email", e);
                }

                // Validate contact number format
                if (customer.getContactNumber() != null && !customer.getContactNumber().isEmpty()) {
                    String phonePattern = "^\\+91\\s?[6-9]\\d{9}$";
                    if (!customer.getContactNumber().matches(phonePattern)) {
                        result.rejectValue("contactNumber", "error.customer",
                                "Please enter a valid Indian phone number (e.g. +91 7895125364)");
                    }
                }

                // Validate name length
                if (customer.getName() != null && customer.getName().trim().length() < 2) {
                    result.rejectValue("name", "error.customer",
                            "Customer name must be at least 2 characters long");
                }
            }

            if (result.hasErrors()) {
                model.addAttribute("customer", customer);

                return "customers/create";
            }

            try {
                CustomerDto created = customersClient.create(customer);
                redirectAttributes.addFlashAttribute("success",
                        "Customer '" + created.getName() + "' created successfully! " +
                                "You can now create orders for this customer.");

                // Optionally redirect to customer details or orders creation
                String redirect = request.getParameter("redirect");
                if ("orders".equals(redirect)) {
                    return "redirect:/orders/create?customerId=" + created.getCustomerId();
                }

                return "redirect:/customers";

            } catch (Exception exception) {
                log.error("Error creating customer", exception);

                // Handle specific error types
                String errorMessage = "Failed to create customer. Please try again.";
                if (exception.getMessage() != null) {
                    if (exception.getMessage().contains("email")) {
                        errorMessage = "Email address is already in use.";
                    } else if (exception.getMessage().contains("phone")) {
                        errorMessage = "Phone number is already in use.";
                    }
                }

                model.addAttribute("customer", customer);
                model.addAttribute("error", errorMessage);

                return "customers/create";
            }
        }

        @GetMapping("/{id}")
        public String details(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
            try {
                CustomerDto customer = customersClient.findCustomerById(id);
                List<OrderDto> orders = ordersClient.findAllOrdersOfCustomer(id);
                customer.setOrders(orders);

                // Calculate comprehensive customer metrics
                double totalSpent = 0.0;
                for (OrderDto order : orders) {
                    if (!OrderStatus.CANCELLED.equals(order.getOrderStatus()) && order.getTotalPrice() != null) {
                        totalSpent += order.getTotalPrice();
                    }
                }

                double avgOrderValue = orders.isEmpty() ? 0.0 : totalSpent / orders.size();

                OrderDto lastOrder = null;
                for (OrderDto order : orders) {
                    if (order.getCreatedAt() != null) {
                        if (lastOrder == null || order.getCreatedAt().isAfter(lastOrder.getCreatedAt())) {
                            lastOrder = order;
                        }
                    }
                }

                int completedOrders = 0;
                for (OrderDto order : orders) {
                    if (OrderStatus.COMPLETED.equals(order.getOrderStatus())) {
                        completedOrders++;
                    }
                }

                double loyaltyScore = calculateCustomerLoyaltyScore(customer, orders);
                String customerStatus = determineCustomerStatus(orders, totalSpent);

                model.addAttribute("customer", customer);
                model.addAttribute("totalSpent", totalSpent);
                model.addAttribute("avgOrderValue", avgOrderValue);
                model.addAttribute("completedOrders", completedOrders);
                model.addAttribute("loyaltyScore", loyaltyScore);
                model.addAttribute("customerStatus", customerStatus);

                if (lastOrder != null) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
                    String formattedDate = lastOrder.getCreatedAt().format(formatter);
                    model.addAttribute("lastOrder", formattedDate);
                } else {
                    model.addAttribute("lastOrder", "No orders found");
                }

                return "customers/detail";

            } catch (Exception e) {
                log.error("Error fetching customer details for ID: {}", id, e);
                redirectAttributes.addFlashAttribute("error", "Customer not found");
                return "redirect:/customers";
            }
        }

        @GetMapping("/{customerId}/orders")
        public String orders(@PathVariable Long customerId, Model model) {
            try {
                CustomerDto customer = customersClient.findCustomerById(customerId);
                List<OrderDto> orders = ordersClient.findAllOrdersOfCustomer(customerId);

                // Calculate statistics
                int totalOrders = orders.size();
                int completedOrders = 0;
                int pendingOrders = 0;
                int createdOrders = 0;
                int cancelledOrders = 0;
                double completedRevenue = 0.0;
                double lifetimeRevenue = 0.0;

                for (OrderDto order : orders) {
                    switch (order.getOrderStatus()) {
                        case COMPLETED:
                            completedOrders++;
                            if (order.getTotalPrice() != null) {
                                completedRevenue += order.getTotalPrice();
                            }
                            break;
                        case PROCESSING:
                            pendingOrders++;
                            break;
                        case CREATED:
                            createdOrders++;
                            pendingOrders++;
                            break;
                        case CANCELLED:
                            cancelledOrders++;
                            break;
                    }

                    if (!OrderStatus.CANCELLED.equals(order.getOrderStatus()) && order.getTotalPrice() != null) {
                        lifetimeRevenue += order.getTotalPrice();
                    }
                }

                double avgOrderValue = (totalOrders - cancelledOrders) > 0 ?
                        lifetimeRevenue / (totalOrders - cancelledOrders) : 0.0;
                double avgProcessingDays = 2.3; // Default value

                model.addAttribute("customer", customer);
                model.addAttribute("orders", orders);
                model.addAttribute("totalOrders", totalOrders);
                model.addAttribute("completedOrders", completedOrders);
                model.addAttribute("pendingOrders", pendingOrders);
                model.addAttribute("createdOrders", createdOrders);
                model.addAttribute("cancelledOrders", cancelledOrders);
                model.addAttribute("completedRevenue", completedRevenue);
                model.addAttribute("lifetimeRevenue", lifetimeRevenue);
                model.addAttribute("avgOrderValue", avgOrderValue);
                model.addAttribute("avgProcessingDays", avgProcessingDays);

                return "customers/orders";

            } catch (Exception e) {
                log.error("Error fetching customer orders for customer ID: {}", customerId, e);
                model.addAttribute("error", "Unable to fetch customer orders");
                return "error";
            }
        }

        // Helper methods
        private double calculateCustomerLoyaltyScore(CustomerDto customer, List<OrderDto> orders) {
            double score = 0.0;

            // Order frequency score (max 40 points)
            score += Math.min(orders.size() * 5, 40);

            // Revenue score (max 30 points)
            double totalSpent = 0.0;
            for (OrderDto order : orders) {
                if (!OrderStatus.CANCELLED.equals(order.getOrderStatus()) && order.getTotalPrice() != null) {
                    totalSpent += order.getTotalPrice();
                }
            }
            score += Math.min(totalSpent / 1000 * 5, 30);

            // Recency score (max 20 points)
            LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
            for (OrderDto order : orders) {
                if (order.getCreatedAt() != null && order.getCreatedAt().isAfter(threeMonthsAgo)) {
                    score += 20;
                    break;
                }
            }

            // Completion rate score (max 10 points)
            if (!orders.isEmpty()) {
                int completedCount = 0;
                for (OrderDto order : orders) {
                    if (OrderStatus.COMPLETED.equals(order.getOrderStatus())) {
                        completedCount++;
                    }
                }
                double completionRate = (double) completedCount / orders.size();
                score += completionRate * 10;
            }

            return Math.min(score, 100.0);
        }

        private String determineCustomerStatus(List<OrderDto> orders, double totalSpent) {
            if (orders.isEmpty()) return "NEW";

            LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
            boolean hasRecentOrder = false;
            for (OrderDto order : orders) {
                if (order.getCreatedAt() != null && order.getCreatedAt().isAfter(threeMonthsAgo)) {
                    hasRecentOrder = true;
                    break;
                }
            }

            if (totalSpent > 50000) return "VIP";
            if (totalSpent > 10000 && hasRecentOrder) return "PREMIUM";
            if (hasRecentOrder) return "ACTIVE";
            return "INACTIVE";
        }
    }