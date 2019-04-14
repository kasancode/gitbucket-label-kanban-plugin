package io.github.gitbucket.labelkanban.api

import gitbucket.core.api.{ApiPath, FieldSerializable}
import gitbucket.core.util.RepositoryName
import gitbucket.core.view.helpers

import scala.collection.mutable

case class ApiDataSetKanban(
                             issues: List[ApiIssueKanban],
                             lanes: mutable.LinkedHashMap[String,List[ApiLaneKanban]]
                           )
  extends FieldSerializable {

}

