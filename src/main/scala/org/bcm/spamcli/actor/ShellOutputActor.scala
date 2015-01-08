package org.bcm.spamcli.actor

import scala.io.Source

import akka.actor._

import org.bcm.spam.payload.model.Output

class ShellOutputActor extends Actor {

  def receive = {
    case o: Output => println(o.content)
    case _ => println
  }
}
