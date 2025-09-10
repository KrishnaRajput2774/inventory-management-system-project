package com.rk.inventory_management_system.ui.nventory_management_system_ui.Clients;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class InvoiceClient {

    private final RestClient restClient;

    public byte[] downloadInvoice(List<Long> orderIds, String jwtToken) {

        log.debug("Downloading invoice for orders: {}", orderIds);

        StringBuilder uriBuilder = new StringBuilder("/api/invoices/download?orderIds=");

        for(int i = 0; i<orderIds.size(); i++) {
            if(i > 0) {
                uriBuilder.append(",");
            }
            uriBuilder.append(orderIds.get(i));
        }

        return restClient.get()
                .uri(uriBuilder.toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .retrieve()
                .body(byte[].class);
    }

}
