package io.github.gitbucket.labelkanban.api

import gitbucket.core.api.{ApiPath, FieldSerializable}
import gitbucket.core.util.RepositoryName
import gitbucket.core.view.helpers

case class ApiDataSetKanban(
                             issues: List[ApiIssueKanban],
                             lanes: Map[String,List[ApiLaneKanban]],
                             dummyLanes: Map[String, ApiLaneKanban]
                           )(repositoryName: RepositoryName)
  extends FieldSerializable {

}

