package com.rk.inventory_management_system.ui.nventory_management_system_ui.controllers;

import com.rk.inventory_management_system.ui.nventory_management_system_ui.Clients.InvoiceClient;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/invoices")
@RequiredArgsConstructor
@Slf4j
public class InvoiceController {

    private final InvoiceClient invoicesClient;

    @GetMapping("/select-orders")
    public String selectOrders(@RequestParam(required = false)Long customerId, Model model) {

        //For now we will show form to enter order ids.
        //TODO Show list of complete orders of this customer or all orders

        model.addAttribute("customerId",customerId);
        return "invoices/select-orders";
    }

    @PostMapping("/generate")
    public String generateInvoice(@RequestParam List<Long> orderIds,
                                  RedirectAttributes redirectAttributes) {
        try {
            // Store order IDs in session or pass as parameters for download
            redirectAttributes.addAttribute("orderIds", String.join(",",
                    orderIds.stream().map(String::valueOf).toArray(String[]::new)));
            redirectAttributes.addFlashAttribute("successMessage",
                    "Invoice generated successfully. Click download to get the PDF.");
            return "redirect:/invoices/success";
        } catch (Exception e) {

            log.error("Error generating invoice", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to generate invoice");
            return "redirect:/invoices/select-orders";
        }
    }

    @GetMapping("/success")
    public String success(@RequestParam String orderIds, Model model) {
        model.addAttribute("orderIds", orderIds);
        return "invoices/success";
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> download(@RequestParam String orderIds, HttpServletRequest request) {
        try {
            List<Long> orderIdList = List.of(orderIds.split(","))
                    .stream()
                    .map(Long::valueOf)
                    .toList();


            Cookie[] cookies = request.getCookies();
            String jwtToken = null;
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("token".equals(cookie.getName())) {
                        jwtToken = cookie.getValue();
                        break;
                    }
                }
            }
            byte[] pdfBytes = invoicesClient.downloadInvoice(orderIdList,jwtToken);

            HttpHeaders headers = new HttpHeaders();
            assert jwtToken != null;
            headers.setBearerAuth(jwtToken);
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "invoice.pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (Exception e) {
            log.error("Error downloading invoice", e);
            return ResponseEntity.internalServerError().build();
        }
    }


}
