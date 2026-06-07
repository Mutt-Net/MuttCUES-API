package net.muttcode.spring.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigIpTest {

    @Autowired
    MockMvc mvc;

    @Test
    void jobsProcess_fromExternalIp_isForbidden() throws Exception {
        mvc.perform(post("/api/jobs/process").content("{}").contentType("application/json")
                .with(r -> { r.setRemoteAddr("3.174.141.96"); return r; }))
           .andExpect(status().isForbidden());
    }

    @Test
    void jobsProcess_fromLocalhost_isNotIpForbidden() throws Exception {
        int code = mvc.perform(post("/api/jobs/process").content("{}").contentType("application/json")
                .with(r -> { r.setRemoteAddr("127.0.0.1"); return r; }))
           .andReturn().getResponse().getStatus();
        Assertions.assertNotEquals(403, code);
    }
}
