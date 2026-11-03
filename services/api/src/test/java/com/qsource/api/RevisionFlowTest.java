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
        MvcResult signup = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"tester@example.com",
                                  "password":"password123",
                                  "displayName":"Tester",
                                  "organizationName":"Acme"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode auth = mapper.readTree(signup.getResponse().getContentAsString());
        String token = auth.get("accessToken").asText();

        MvcResult orgs = mockMvc.perform(get("/api/orgs").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        String orgId = mapper.readTree(orgs.getResponse().getContentAsString()).get(0).get("id").asText();

        MvcResult projectResult = mockMvc.perform(post("/api/orgs/" + orgId + "/projects")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Actor-Source", "web")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test\",\"description\":\"d\",\"visibility\":\"ORG\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String projectId = mapper.readTree(projectResult.getResponse().getContentAsString()).get("id").asText();

        MvcResult featureResult = mockMvc.perform(post("/api/projects/" + projectId + "/features")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Actor-Source", "web")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Auth later",
                                  "outcome":"Users can sign in",
                                  "requirements":"JWT",
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
                        .header("Authorization", "Bearer " + token)
                        .header("X-Actor-Source", "agent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Auth later",
                                  "outcome":"Users can sign in",
                                  "requirements":"JWT; actor attribution",
                                  "successCriteria":"Login works",
                                  "changeMessage":"agent notes"
                                }
                                """))
                .andExpect(status().isOk());

        MvcResult history = mockMvc.perform(get("/api/features/" + featureId + "/revisions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode revisions = mapper.readTree(history.getResponse().getContentAsString());
        assertThat(revisions).hasSize(2);
        assertThat(revisions.get(0).get("version").asInt()).isEqualTo(2);
        assertThat(revisions.get(0).get("actorName").asText()).isEqualTo("Tester");
        assertThat(revisions.get(0).get("source").asText()).isEqualTo("AGENT");
    }
}
