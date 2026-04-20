package com.scrap_system.backend_api.controller;

import com.scrap_system.backend_api.model.ComponentDict;
import com.scrap_system.backend_api.repository.ComponentDictRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ComponentDictControllerTest {

    @Test
    void getEnabledComponentsFallsBackToDefaultsWhenRepositoryFails() {
        ComponentDictRepository repository = mock(ComponentDictRepository.class);
        ComponentDictController controller = new ComponentDictController(repository);

        when(repository.findByIsEnabledTrueOrderBySortOrderAscIdAsc())
                .thenThrow(new RuntimeException("table missing"));

        List<ComponentDict> body = controller.getEnabledComponents().getBody();

        assertFalse(body == null || body.isEmpty());
        assertEquals("三元催化", body.get(0).getName());
        assertEquals("油箱", body.get(body.size() - 1).getName());
    }
}
