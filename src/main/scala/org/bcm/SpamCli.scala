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

import org.bcm.spamcli.actor.ShellOutputActor

object SpamCli extends App {
  implicit val timeout = Timeout(30, TimeUnit.SECONDS)

  val system = ActorSystem("SpamCli")
  val presence = system.actorSelection("akka.tcp://Spam@127.0.0.1:2553/user/presence")
  val router = system.actorSelection("akka.tcp://Spam@127.0.0.1:2553/user/router")
  val output = system.actorOf(Props[ShellOutputActor])

  output ! Output("Please enter your User ID")
  val userId = Source.stdin.getLines.next

  val chatToPattern = "(:c )(.*)".r
  var chattingTo: Option[String] = None

  presence ? Register(userId, output) onSuccess {
    case p: Presence => {
      sys addShutdownHook { presence ! Unregister(userId) }
      printUsers
      listenForInput(p.user.ref)
    }
  }

  def printHelp {
    output ! Output("type :u to print a list of Online Users")
    output ! Output("type :c [User Id] to chat to a particular User")
    output ! Output("type :h to show this help")
  }

  def printUsers {
    presence ? GetUsers onSuccess {
      case users: Iterable[String] => {
        val otherUsers = users.toSeq diff Seq(userId)
        val usersWithIndex = otherUsers.sorted.zipWithIndex.toMap
        val usersWithIndexPlusOne = usersWithIndex map { case (u, i) => u -> (i + 1) }
        Output("Online Users :")
        usersWithIndexPlusOne foreach { case (u, i) => output ! Output(s"  $i. $u") }
      }
    }
  }

  def chattingTo(to: String) {
    output ! Output(s"Now chatting to: $to")
    chattingTo = Some(to)
  }

  def listenForInput(user: ActorRef) {
    output ! Output("== SPAM away fine Sir ==")
    for (ln <- Source.stdin.getLines) {
      ln match {
        case ":h" => printHelp
        case ":u" => printUsers
        case chatToPattern(_, user) => chattingTo(user)
        case _ => chattingTo foreach { user ! Input(_, ln) }
      }
    }
  }
}
