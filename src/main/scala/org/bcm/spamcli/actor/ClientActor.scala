package org.bcm.spamcli.actor

import scala.concurrent.ExecutionContext.Implicits.global
import java.util.concurrent.TimeUnit

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout

import org.bcm.spam.payload.model.{Output, Presence}
import org.bcm.spam.payload.action.{GetUsers, Register, Unregister}
import org.bcm.spamcli.payload.action.{Login, PrintHelp, PrintUsers}

class ClientActor(userId: String, output: ActorRef, presence: ActorSelection) extends Actor {
  implicit val timeout = Timeout(30, TimeUnit.SECONDS)

  def receive = {
    case Login => {
      val loginSender = sender
      presence ? Register(userId, output) onSuccess {
        case p: Presence => {
          sys addShutdownHook { presence ! Unregister(userId) }
          self ! PrintUsers
          self ! PrintHelp
          loginSender ! p
        }
      }
    }
    case PrintHelp => {
      output ! Output("")
      output ! Output("type :u to print a list of Online Users")
      output ! Output("type :c [User Id] to chat to a particular User")
      output ! Output("type :h to show this help")
    }
    case PrintUsers => {
      presence ? GetUsers onSuccess {
        case users: Iterable[String] => {
          val otherUsers = users.toSeq diff Seq(userId)
          val usersWithIndex = otherUsers.sorted.zipWithIndex.toMap
          val usersWithIndexPlusOne = usersWithIndex map { case (u, i) => u -> (i + 1) }
          output ! Output("")
          output ! Output(s"${usersWithIndexPlusOne.size} User(s) online:")
          usersWithIndexPlusOne foreach { case (u, i) => output ! Output(s"  $i. $u") }
        }
      }
    }
    case _ => // do nothing
  }
}
