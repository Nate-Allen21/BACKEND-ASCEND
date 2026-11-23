package br.com.ascend.the_system.controller;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.util.JsonPathExpectationsHelper;
import org.springframework.test.context.TestPropertySource;
import br.com.ascend.the_system.repository.UserSessionRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @TestPropertySource(properties = "ASCEND_HASH_NEW_SESSION_TOKENS=true")
class CalendarControllerTest {
 @Autowired MockMvc mvc;
 @Autowired UserSessionRepository sessions;
 String register() throws Exception {
  String body = "{\"name\":\"Pessoa Teste\",\"email\":\"" + java.util.UUID.randomUUID() + "@example.com\",\"password\":\"senha1234\"}";
  String result = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
  return "Bearer " + new JsonPathExpectationsHelper("$.token").evaluateJsonPath(result, String.class);
 }
 @Test void persistsAndRestrictsEventsToOwner() throws Exception {
  String a=register(), b=register();
  String body="{\"title\":\"Estudo\",\"date\":\"2026-11-19\",\"startTime\":\"09:00\",\"endTime\":\"10:00\",\"recurrence\":\"weekly\",\"reminderMinutes\":15,\"protectedEvent\":true}";
  String result=mvc.perform(post("/api/calendar/events").header("Authorization",a).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  Integer id=new JsonPathExpectationsHelper("$.id").evaluateJsonPath(result, Integer.class);
  mvc.perform(get("/api/calendar/events").header("Authorization",b)).andExpect(status().isOk()).andExpect(content().json("[]"));
  mvc.perform(put("/api/calendar/events/{id}",id).header("Authorization",b).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
  mvc.perform(delete("/api/calendar/events/{id}",id).header("Authorization",b)).andExpect(status().isBadRequest());
  mvc.perform(put("/api/calendar/events/{id}",id).header("Authorization",a).contentType(MediaType.APPLICATION_JSON).content(body.replace("09:00","08:00"))).andExpect(status().isOk()).andExpect(jsonPath("$.startTime").value("08:00"));
  mvc.perform(get("/api/calendar/events").header("Authorization",a)).andExpect(status().isOk()).andExpect(jsonPath("$[0].reminderMinutes").value(15));
  mvc.perform(delete("/api/calendar/events/{id}",id).header("Authorization",a)).andExpect(status().isNoContent());
  mvc.perform(get("/api/calendar/events").header("Authorization",a)).andExpect(content().json("[]"));
  mvc.perform(get("/api/calendar/events")).andExpect(status().isUnauthorized());
 }
 @Test void savedGeneratedEditSurvivesNewSessionAndDeletionKeepsOverride() throws Exception {
  String email = java.util.UUID.randomUUID() + "@example.com";
  String credentials = "{\"email\":\"" + email + "\",\"password\":\"senha1234\"}";
  String registration = "{\"name\":\"Pessoa Teste\",\"email\":\"" + email + "\",\"password\":\"senha1234\"}";
  String registered = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registration)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
  String token = "Bearer " + new JsonPathExpectationsHelper("$.token").evaluateJsonPath(registered, String.class);
  String body = "{\"title\":\"Alteracao manual\",\"date\":\"2026-11-20\",\"sourceKey\":\"2026-11-19:mission\",\"recurrence\":\"none\",\"protectedEvent\":true}";
  String saved = mvc.perform(post("/api/calendar/events").header("Authorization",token).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  Integer id = new JsonPathExpectationsHelper("$.id").evaluateJsonPath(saved, Integer.class);
  mvc.perform(post("/api/auth/logout").header("Authorization",token)).andExpect(status().isNoContent());
  String loggedIn = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(credentials)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  String nextToken = "Bearer " + new JsonPathExpectationsHelper("$.token").evaluateJsonPath(loggedIn, String.class);
  mvc.perform(get("/api/calendar/events").header("Authorization",nextToken)).andExpect(status().isOk()).andExpect(jsonPath("$[0].title").value("Alteracao manual")).andExpect(jsonPath("$[0].protectedEvent").value(true));
  mvc.perform(delete("/api/calendar/events/{id}",id).header("Authorization",nextToken)).andExpect(status().isNoContent());
  mvc.perform(get("/api/calendar/events").header("Authorization",nextToken)).andExpect(status().isOk()).andExpect(jsonPath("$[0].deleted").value(true)).andExpect(jsonPath("$[0].sourceKey").value("2026-11-19:mission"));
 }
 @Test void newSessionTokenIsStoredHashedAndHashCannotAuthenticate() throws Exception {
  String bearer=register(); String raw=bearer.substring("Bearer ".length());
  String stored="sha256:"+HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
  assertTrue(sessions.findByToken(raw).isEmpty());
  assertTrue(sessions.findByToken(stored).isPresent());
  mvc.perform(get("/api/auth/me").header("Authorization","Bearer "+stored)).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/auth/me").header("Authorization",bearer)).andExpect(status().isOk());
  mvc.perform(post("/api/auth/logout").header("Authorization",bearer)).andExpect(status().isNoContent());
  assertTrue(sessions.findByToken(stored).isEmpty());
 }
 @Test void reminderRequiresStartAndTimesMustFitTheDatabaseColumn() throws Exception {
  String token=register();
  String base="{\"title\":\"Estudo\",\"date\":\"2026-11-19\",\"recurrence\":\"none\",\"reminderMinutes\":15";
  mvc.perform(post("/api/calendar/events").header("Authorization",token).contentType(MediaType.APPLICATION_JSON).content(base+"}"))
      .andExpect(status().isBadRequest());
  mvc.perform(post("/api/calendar/events").header("Authorization",token).contentType(MediaType.APPLICATION_JSON).content(base+",\"startTime\":\"09:00:30\"}"))
      .andExpect(status().isBadRequest());
 }
}
