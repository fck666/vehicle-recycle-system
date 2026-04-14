package com.scrap_system.backend_api.controller;

import com.scrap_system.backend_api.controller.admin.RecyclePriceController;
import com.scrap_system.backend_api.service.RecyclePriceService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecyclePriceControllerTest {

    @Test
    void getRecycleMaterialTypesPublicReturnsTypes() {
        RecyclePriceService recyclePriceService = mock(RecyclePriceService.class);
        when(recyclePriceService.getRecycleMaterialTypes()).thenReturn(List.of("steel", "plastic"));

        RecyclePriceController controller = new RecyclePriceController(recyclePriceService);

        var response = controller.getRecycleMaterialTypesPublic();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(List.of("steel", "plastic"), response.getBody());
    }
}
