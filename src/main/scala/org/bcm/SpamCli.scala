package org.bcm.spamcli

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout

import org.bcm.spam.actor.UserActor
import org.bcm.spam.payload.action.{GetUsers, Register, RoutingRequest}
import org.bcm.spam.payload.model.{Input, Message, Presence}

import org.bcm.spamcli.actor.ShellOutputActor

object SpamCli extends App {
  implicit val timeout = Timeout(30, TimeUnit.SECONDS)

  val system = ActorSystem("SpamCli")
  val presence = system.actorSelection("akka.tcp://Spam@127.0.0.1:2553/user/presence")
  val router = system.actorSelection("akka.tcp://Spam@127.0.0.1:2553/user/router")
  val outputActor = system.actorOf(Props[ShellOutputActor])

  println("Please enter your Username")
  val userId = Source.stdin.getLines.next

  presence ? Register(userId, outputActor) onSuccess {
    case p: Presence => {
      listenForInput(p.user.ref)
    }
  }

  presence ? GetUsers onSuccess {
    case users: Iterable[String] => {
      val otherUsers = users.toSeq diff Seq(userId)
      otherUsers.headOption foreach { id => router ! RoutingRequest(userId, id, "Welcome") }
    }
  }

  def listenForInput(userActor: ActorRef) {
    println("[Start chatting...]")
    for (ln <- Source.stdin.getLines) {
      userActor ! Input(ln)
    }
  }

  // Listen for input from user
  // Get presence of other users every X minutes
  // Send presence every X minutes
  // Send messages from User
}
