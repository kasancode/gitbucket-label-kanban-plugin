package io.github.gitbucket.labelkanban.api

import gitbucket.core.api.{ApiPath, FieldSerializable}
import gitbucket.core.util.RepositoryName
import gitbucket.core.view.helpers

case class ApiAssigneeKanban(userName: String)(repositoryName: RepositoryName)
  extends FieldSerializable {
  val html_url = ApiPath(s"/${repositoryName.fullName}/issues?assigned=${helpers.urlEncode(userName)}&state=open")
  val detach_url = ""
  val attach_url = ApiPath(s"/api/v3/repos/${repositoryName.fullName}/plugin/labelkanban/assignee/${userName}/switch/issue/")
}

