package com.scrap_system.backend_api.service;

import com.scrap_system.backend_api.dto.admin.RecyclePriceImportDto;
import com.scrap_system.backend_api.model.MaterialPrice;
import com.scrap_system.backend_api.repository.MaterialPriceRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecyclePriceServiceTest {

    @Test
    void getRecycleMaterialTypesFallsBackToDefaultsWhenRepositoryReturnsEmpty() {
        MaterialPriceRepository repository = mock(MaterialPriceRepository.class);
        RecyclePriceService service = new RecyclePriceService(repository);

        when(repository.findDistinctTypesByCategory("RECYCLE")).thenReturn(List.of());

        assertEquals(
                List.of("steel", "aluminum", "copper", "battery", "plastic", "rubber"),
                service.getRecycleMaterialTypes()
        );
    }

    @Test
    void saveRecyclePriceUpdatesExistingRecordById() {
        MaterialPriceRepository repository = mock(MaterialPriceRepository.class);
        RecyclePriceService service = new RecyclePriceService(repository);

        MaterialPrice existing = new MaterialPrice();
        existing.setId(7L);
        existing.setType("steel");
        existing.setPriceCategory("RECYCLE");
        existing.setEffectiveDate(LocalDate.of(2026, 4, 1));

        RecyclePriceImportDto dto = new RecyclePriceImportDto();
        dto.setId(7L);
        dto.setMaterialName("废钢");
        dto.setPrice(3.6d);
        dto.setUnit("千克");
        dto.setEffectiveDate(LocalDate.of(2026, 4, 1));

        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        when(repository.save(any(MaterialPrice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.saveRecyclePrice(dto);

        verify(repository).findById(7L);
        verify(repository, never()).findByTypeAndEffectiveDateAndPriceCategory(eq("steel"), eq(LocalDate.now()), eq("RECYCLE"));
        verify(repository).save(existing);
        assertEquals(LocalDate.of(2026, 4, 1), existing.getEffectiveDate());
        assertTrue(existing.getPricePerKg().compareTo(new BigDecimal("3.6")) == 0);
    }

    @Test
    void deleteRecyclePriceItemDeletesOnlySelectedRow() {
        MaterialPriceRepository repository = mock(MaterialPriceRepository.class);
        RecyclePriceService service = new RecyclePriceService(repository);

        MaterialPrice existing = new MaterialPrice();
        existing.setId(9L);
        existing.setType("steel");
        existing.setPriceCategory("RECYCLE");

        when(repository.findById(9L)).thenReturn(Optional.of(existing));

        service.deleteRecyclePriceItem(9L);

        verify(repository).findById(9L);
        verify(repository).deleteById(9L);
        verify(repository, never()).deleteByTypeAndPriceCategory(any(), any());
    }
}
