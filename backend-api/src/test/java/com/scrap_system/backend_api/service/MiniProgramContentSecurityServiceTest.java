package com.scrap_system.backend_api.service;

import cn.binarywang.wx.miniapp.api.WxMaSecurityService;
import cn.binarywang.wx.miniapp.api.WxMaService;
import com.scrap_system.backend_api.exception.ContentSecurityException;
import com.scrap_system.backend_api.model.VehicleDismantleRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MiniProgramContentSecurityServiceTest {

    private WxMaService wxMaService;
    private WxMaSecurityService wxMaSecurityService;
    private MiniProgramContentSecurityService service;

    @BeforeEach
    void setUp() {
        wxMaService = mock(WxMaService.class);
        wxMaSecurityService = mock(WxMaSecurityService.class);
        when(wxMaService.getSecurityService()).thenReturn(wxMaSecurityService);
        service = new MiniProgramContentSecurityService(wxMaService);
        ReflectionTestUtils.setField(service, "enabled", true);
    }

    @Test
    void validateUsernamePassesSafeContent() throws Exception {
        when(wxMaSecurityService.checkMessage("合规用户名")).thenReturn(true);

        assertDoesNotThrow(() -> service.validateUsername(" 合规用户名 "));
        verify(wxMaSecurityService).checkMessage("合规用户名");
    }

    @Test
    void validateUsernameRejectsUnsafeContent() throws Exception {
        when(wxMaSecurityService.checkMessage("违规内容")).thenReturn(false);

        assertThrows(ContentSecurityException.class, () -> service.validateUsername("违规内容"));
    }

    @Test
    void validateDismantleRecordChecksNestedRemark() throws Exception {
        VehicleDismantleRecord record = new VehicleDismantleRecord();
        record.setRemark("正常备注");
        record.setDetailsJson("""
                {"items":[
                  {"category":"PART","partName":"三元催化","remark":"走二手件"},
                  {"category":"PART","partName":"电池包","remark":""}
                ]}
                """);
        when(wxMaSecurityService.checkMessage(anyString())).thenReturn(true);

        assertDoesNotThrow(() -> service.validateDismantleRecord(record));
        verify(wxMaSecurityService).checkMessage("正常备注");
        verify(wxMaSecurityService).checkMessage("三元催化");
        verify(wxMaSecurityService).checkMessage("走二手件");
    }
}
