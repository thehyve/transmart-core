package transmart

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import scala.util.Random

class v1_observations extends Simulation {

  val httpConf = http
    .baseURL("http://localhost:9090")
    .acceptHeader("application/json")

  object Oauth { //get token needed for all other calls, stored in session var accessToken
    val testUser2 = exec(
              http("POST auth request")
                  .post("/oauth/token?grant_type=password&client_id=glowingbear-js&client_secret=&username=admin&password=admin")
                  .check(jsonPath("$.access_token").saveAs("accessToken"))
      )
  }

  //utility calls for getting all concept names and patient ids
  object parse {
    val parsePatientIds = exec(
      http("get subjects")
          .get("/studies/BigStudy/subjects") 
          .header("Authorization", "Bearer ${accessToken}")
          .check(jsonPath("$.subjects[*].id").findAll.saveAs("subjectIds"))
          )

    val parseConceptPaths = exec(
      http("get conceptPaths")
        .get("/studies/BigStudy/concepts")
        .header("Authorization", "Bearer ${accessToken}")
        .check(jsonPath("$.ontology_terms[*].fullName").findAll.saveAs("conceptPaths"))
        )
  }

  object Calls{
    //studyIds used in the benchmark
    var feeder = Array(
                      Map("studyId" -> "BigStudy"),
                      Map("studyId" -> "BigStudy2")).circular

    val getObservations = repeat(1){ 
      exec(feed(feeder))
      .exec(
          http("GET observations")
                  .get("/studies/${studyId}/observations")
                  .header("Authorization", "Bearer ${accessToken}")
      )
    }

    val getHighdim = repeat(1){ 
      exec(feed(feeder))
      .exec(
          http("GET Highdim")
                  .get("/studies/${studyId}/concepts/${conceptPath}/highdim")
                  .header("Authorization", "Bearer ${accessToken}")
                  .queryParam("dataType", "RNA-Seq" )
                  .queryParam("projection", "all-data" )
      )
    }
  }

  //definision of the scenario to be run
  val test1 = scenario("get test").exec(Oauth.testUser2, Calls.getObservations)

  setUp(test1.inject(atOnceUsers(1)).protocols(httpConf))// do test scenario with n users
}