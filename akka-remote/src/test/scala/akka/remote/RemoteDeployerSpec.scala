/**
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package akka.remote

import akka.testkit._
import akka.actor._
import akka.routing._
import com.typesafe.config._
import akka.ConfigurationException

object RemoteDeployerSpec {
  val deployerConf = ConfigFactory.parseString("""
      akka.actor.provider = "akka.remote.RemoteActorRefProvider"
      akka.actor.deployment {
        /service2 {
          router = round-robin
          nr-of-instances = 3
          remote = "akka://sys@wallace:2552"
          dispatcher = mydispatcher
        }
      }
      akka.remote.netty.tcp.port = 0
      """, ConfigParseOptions.defaults)

  class RecipeActor extends Actor {
    def receive = { case _ ⇒ }
  }

}

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class RemoteDeployerSpec extends AkkaSpec(RemoteDeployerSpec.deployerConf) {

  "A RemoteDeployer" must {

    "be able to parse 'akka.actor.deployment._' with specified remote nodes" in {
      val service = "/service2"
      val deployment = system.asInstanceOf[ActorSystemImpl].provider.deployer.lookup(service.split("/").drop(1))

      deployment must be(Some(
        Deploy(
          service,
          deployment.get.config,
          RoundRobinRouter(3),
          RemoteScope(Address("akka", "sys", "wallace", 2552)),
          "mydispatcher")))
    }

    "reject remote deployment when the source requires LocalScope" in {
      intercept[ConfigurationException] {
        system.actorOf(Props.empty.withDeploy(Deploy.local), "service2")
      }.getMessage must be === "configuration requested remote deployment for local-only Props at [akka://RemoteDeployerSpec/user/service2]"
    }

  }

}
