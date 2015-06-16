package org.bcm.spamcli

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout

import org.bcm.spam.actor.UserActor
import org.bcm.spam.payload.action.{GetUsers, Register, RoutingRequest, Unregister}
import org.bcm.spam.payload.model.{Input, Message, Output, Presence}

import org.bcm.spamcli.actor.{ClientActor, ShellOutputActor}
import org.bcm.spamcli.payload.action.{Login, PrintHelp, PrintUsers}

object SpamCli extends App {
  implicit val timeout = Timeout(30, TimeUnit.SECONDS)

  val system = ActorSystem("SpamCli")
  val presence = system.actorSelection("akka.tcp://Spam@127.0.0.1:2553/user/presence")
  val router = system.actorSelection("akka.tcp://Spam@127.0.0.1:2553/user/router")
  val output = system.actorOf(Props[ShellOutputActor])

  output ! Output("Please enter your User ID")
  val userId = Source.stdin.getLines.next

  val client = system.actorOf(Props(classOf[ClientActor], userId, output, presence))
  client ? Login onSuccess {
    case p: Presence => {
      listenForInput(p.user.ref)
    }
  }

  val chatToPattern = "(:c )(.*)".r
  var chattingTo: Option[String] = None

  def chattingTo(to: String) {
    output ! Output(s"Now chatting to: $to")
    chattingTo = Some(to)
  }

  def listenForInput(user: ActorRef) {
    output ! Output("")
    output ! Output("== Let Spamming commence ==")
    for (ln <- Source.stdin.getLines) {
      ln match {
        case ":h" => client ! PrintHelp
        case ":u" => client ! PrintUsers
        case chatToPattern(_, user) => chattingTo(user)
        case _ => chattingTo foreach { user ! Input(_, ln) }
      }
    }
  }
}
