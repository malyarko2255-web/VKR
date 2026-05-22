package ru.finuniversity.advance

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

class AdvanceLoadTest extends Simulation {

  // ── HTTP protocol ──────────────────────────────────────────────────────────

  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .disableCaching

  // ── Feeders ────────────────────────────────────────────────────────────────

  val authFeeder = csv("test-users.csv").circular

  // ── Login chains ───────────────────────────────────────────────────────────

  val keycloakUrl = "http://localhost:8180/realms/advance/protocol/openid-connect/token"

  val driverLogin = exec(
    http("Get Driver Token")
      .post(keycloakUrl)
      .formParam("client_id",  "advance-frontend")
      .formParam("grant_type", "password")
      .formParam("username",   "#{username}")
      .formParam("password",   "#{password}")
      .check(status.is(200))
      .check(jsonPath("$.access_token").saveAs("accessToken"))
  )

  // ── Driver scenario ────────────────────────────────────────────────────────

  val createAdvanceBody = StringBody(
    """{"driverId":"#{driverId}","routeId":"#{routeId}","tripStage":"IN_TRANSIT","advanceType":"FUEL","amount":3500}"""
  )

  val driverScenario = scenario("Driver - Request Advance")
    .feed(authFeeder)
    .exec(driverLogin)
    .pause(1)
    .exec(
      http("Create Advance Request")
        .post("/api/v1/advances")
        .header("Authorization", "Bearer #{accessToken}")
        .body(createAdvanceBody)
        .check(status.in(201, 409))
        .check(jsonPath("$.id").optional.saveAs("advanceId"))
    )
    .pause(2)
    .doIf(session => session.contains("advanceId")) {
      exec(
        http("Check Advance Status")
          .get("/api/v1/advances/#{advanceId}")
          .header("Authorization", "Bearer #{accessToken}")
          .check(status.is(200))
      )
    }

  // ── Dispatcher scenario ────────────────────────────────────────────────────

  val dispatcherLogin = exec(
    http("Get Dispatcher Token")
      .post(keycloakUrl)
      .formParam("client_id",  "advance-frontend")
      .formParam("grant_type", "password")
      .formParam("username",   "dispatcher1")
      .formParam("password",   "test")
      .check(status.is(200))
      .check(jsonPath("$.access_token").saveAs("dispToken"))
  )

  val approveBody = StringBody("""{"comment":"Approved by load test"}""")

  val dispatcherScenario = scenario("Dispatcher - Approve Queue")
    .exec(dispatcherLogin)
    .pause(3)
    .repeat(5) {
      exec(
        http("Get Pending Queue")
          .get("/api/v1/advances?status=DISPATCHER_REVIEW&size=10")
          .header("Authorization", "Bearer #{dispToken}")
          .check(status.is(200))
          .check(jsonPath("$.content[0].id").optional.saveAs("pendingId"))
      )
      .doIf(session => session.contains("pendingId")) {
        exec(
          http("Approve Advance")
            .put("/api/v1/advances/#{pendingId}/approve")
            .header("Authorization", "Bearer #{dispToken}")
            .body(approveBody)
            .check(status.in(200, 409))
        )
      }
      .pause(1)
    }

  // ── Simulation setup ───────────────────────────────────────────────────────

  setUp(
    driverScenario.inject(
      rampUsers(50).during(30.seconds),
      constantUsersPerSec(10).during(60.seconds)
    ),
    dispatcherScenario.inject(
      nothingFor(5.seconds),
      rampUsers(10).during(20.seconds)
    )
  ).protocols(httpProtocol)
}
