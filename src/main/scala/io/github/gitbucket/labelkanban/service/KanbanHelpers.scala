package io.github.gitbucket.labelkanban.service

import java.security.MessageDigest

package object KanbanHelpers {
  def toColorString(text:String):String =
    MessageDigest
      .getInstance("MD5")
      .digest(text.getBytes)
      .map("%02x".format(_))
      .mkString
      .toUpperCase
      .substring(0, 6)
}
