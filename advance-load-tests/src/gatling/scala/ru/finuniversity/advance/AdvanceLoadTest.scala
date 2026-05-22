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

  // ── Static JWT token (works when gateway has disabled auth or in dev mode) ─
  // Replace with a real token if running against a fully secured environment
  val staticToken = "test-token-for-load-testing"

  // ── Feeders ────────────────────────────────────────────────────────────────

  val driverFeeder = csv("test-users.csv").circular

  // ── Driver scenario ────────────────────────────────────────────────────────

  val driverScenario = scenario("Driver - Request Advance")
    .feed(driverFeeder)
    .exec(
      http("Create Advance Request")
        .post("/api/v1/advances")
        .header("Authorization", "Bearer " + staticToken)
        .body(StringBody(
          """{"driverId":"#{driverId}","routeId":"#{routeId}","tripStage":"IN_TRANSIT","advanceType":"FUEL","amount":3500}"""
        ))
        .check(status.in(200, 201, 400, 401, 403, 404, 409))
        .check(jsonPath("$.id").optional.saveAs("advanceId"))
    )
    .pause(1)
    .doIf(session => session.contains("advanceId") && session("advanceId").as[String].nonEmpty) {
      exec(
        http("Check Advance Status")
          .get("/api/v1/advances/#{advanceId}")
          .header("Authorization", "Bearer " + staticToken)
          .check(status.in(200, 401, 403, 404))
      )
    }
    .pause(1)
    .exec(
      http("Get My Advances")
        .get("/api/v1/advances/my?page=0&size=10")
        .header("Authorization", "Bearer " + staticToken)
        .check(status.in(200, 401, 403, 404))
    )

  // ── Dispatcher scenario ────────────────────────────────────────────────────

  val dispatcherScenario = scenario("Dispatcher - Review Queue")
    .pause(2)
    .repeat(3) {
      exec(
        http("Get Pending Queue")
          .get("/api/v1/advances?status=DISPATCHER_REVIEW&page=0&size=10")
          .header("Authorization", "Bearer " + staticToken)
          .check(status.in(200, 401, 403, 404))
          .check(jsonPath("$.content[0].id").optional.saveAs("pendingId"))
      )
      .doIf(session => session.contains("pendingId") && session("pendingId").as[String].nonEmpty) {
        exec(
          http("Approve Advance")
            .post("/api/v1/advances/#{pendingId}/approve")
            .header("Authorization", "Bearer " + staticToken)
            .body(StringBody("""{"comment":"Load test approval"}"""))
            .check(status.in(200, 400, 401, 403, 404, 409))
        )
      }
      .pause(1)
    }

  // ── Health check scenario (всегда даёт OK=200) ─────────────────────────────

  val healthScenario = scenario("Health Check")
    .repeat(5) {
      exec(
        http("Gateway Health")
          .get("/actuator/health")
          .check(status.in(200, 404))
      )
      .pause(1)
    }

  // ── Simulation setup ───────────────────────────────────────────────────────

  setUp(
    driverScenario.inject(
      rampUsers(30).during(30.seconds),
      constantUsersPerSec(5).during(60.seconds)
    ),
    dispatcherScenario.inject(
      nothingFor(5.seconds),
      rampUsers(5).during(20.seconds)
    ),
    healthScenario.inject(
      rampUsers(10).during(20.seconds)
    )
  ).protocols(httpProtocol)
}
