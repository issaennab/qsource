package com.qsource.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "qsource.seed=false",
        "spring.datasource.url=jdbc:h2:mem:qsourcetest;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class RevisionFlowTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;

    @Test
    void createFeatureAndPublishRevision() throws Exception {
        MvcResult projectResult = mockMvc.perform(post("/api/projects")
                        .header("X-Actor-Name", "tester")
                        .header("X-Actor-Source", "web")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test\",\"description\":\"d\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String projectId = mapper.readTree(projectResult.getResponse().getContentAsString()).get("id").asText();

        MvcResult featureResult = mockMvc.perform(post("/api/projects/" + projectId + "/features")
                        .header("X-Actor-Name", "tester")
                        .header("X-Actor-Source", "web")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Auth later",
                                  "outcome":"Users can sign in",
                                  "requirements":"JWT later",
                                  "successCriteria":"Login works",
                                  "changeMessage":"v1"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode feature = mapper.readTree(featureResult.getResponse().getContentAsString());
        String featureId = feature.get("id").asText();
        assertThat(feature.get("currentContent").get("title").asText()).isEqualTo("Auth later");

        mockMvc.perform(post("/api/features/" + featureId + "/revisions")
                        .header("X-Actor-Name", "agent-1")
                        .header("X-Actor-Source", "agent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Auth later",
                                  "outcome":"Users can sign in",
                                  "requirements":"JWT later; actor headers for now",
                                  "successCriteria":"Login works",
                                  "changeMessage":"agent notes"
                                }
                                """))
                .andExpect(status().isOk());

        MvcResult history = mockMvc.perform(get("/api/features/" + featureId + "/revisions")
                        .header("X-Actor-Name", "tester"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode revisions = mapper.readTree(history.getResponse().getContentAsString());
        assertThat(revisions).hasSize(2);
        assertThat(revisions.get(0).get("version").asInt()).isEqualTo(2);
        assertThat(revisions.get(0).get("actorName").asText()).isEqualTo("agent-1");
        assertThat(revisions.get(0).get("source").asText()).isEqualTo("AGENT");
    }
}
