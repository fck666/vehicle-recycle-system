package com.scrap_system.backend_api.controller;

import com.scrap_system.backend_api.model.MaterialPrice;
import com.scrap_system.backend_api.repository.MaterialPriceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecyclePriceControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MaterialPriceRepository materialPriceRepository;

    @BeforeEach
    void setUp() {
        materialPriceRepository.deleteAll();

        MaterialPrice price = new MaterialPrice();
        price.setType("steel");
        price.setPricePerKg(new BigDecimal("3.50"));
        price.setCurrency("CNY");
        price.setUnit("kg");
        price.setEffectiveDate(LocalDate.of(2026, 4, 21));
        price.setFetchedAt(LocalDateTime.of(2026, 4, 21, 9, 0));
        price.setSourceName("test");
        price.setPriceCategory("RECYCLE");
        materialPriceRepository.save(price);
    }

    @Test
    void publicRecyclePriceTypesEndpointIsAvailableWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/recycle-prices/types"))
                .andExpect(status().isOk())
                .andExpect(content().json("[\"steel\"]"));
    }
}
