package org.bcm.spamcli

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout

import org.bcm.spam.actor.UserActor
import org.bcm.spam.payload.action.{GetUsers, RegisterUser, RoutingRequest}
import org.bcm.spam.payload.model.Message

object SpamCli extends App {
  implicit val timeout = Timeout(30, TimeUnit.SECONDS)

  val system = ActorSystem("SpamCli")
  val presence = system.actorSelection("akka.tcp://Spam@127.0.0.1:2553/user/presence")
  val router = system.actorSelection("akka.tcp://Spam@127.0.0.1:2553/user/router")

  println("Please enter your Username")
  val username = Source.stdin.getLines.next

  val user = system.actorOf(Props[UserActor])
  presence ! RegisterUser(username, user)

  presence ? GetUsers onSuccess {
    case users: Seq[String] => { 
      val otherUsers = users diff username
      println(s"found users $otherUsers")
      otherUsers.headOption foreach { listenForInput } 
    }
  }  

  def listenForInput(toUser: String) {
    println(s"Now chatting to $toUser")
    for (ln <- Source.stdin.getLines) {
      router ! RoutingRequest(username, toUser, ln)
    }
  }


  // Listen for input from user
  // Get presence of other users every X minutes
  // Send presence every X minutes
  // Send messages from User 
}

