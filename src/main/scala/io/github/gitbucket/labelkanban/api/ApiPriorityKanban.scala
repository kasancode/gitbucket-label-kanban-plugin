package io.github.gitbucket.labelkanban.api

import gitbucket.core.api.{ApiPath, FieldSerializable}
import gitbucket.core.util.RepositoryName
import gitbucket.core.model.Priority
import gitbucket.core.view.helpers.urlEncode

case class ApiPriorityKanban(
                        userName: String,
                        priorityId: Int = 0,
                        priorityName: String,
                        description: String,
                        isDefault: Boolean,
                        ordering: Int = 0,
                        color: String
                      )(repositoryName: RepositoryName) extends FieldSerializable {
  val html_url = ApiPath(s"/${repositoryName.fullName}/issues?priority=${urlEncode(priorityName)}&state=open")
  val detach_url = ""
  val attach_url =  ApiPath(s"/api/v3/repos/${repositoryName.fullName}/plugin/labelkanban/priority/${priorityId}/switch/issue/")
}

object ApiPriorityKanban {
  def apply(priority: Priority, repositoryName: RepositoryName): ApiPriorityKanban =
    ApiPriorityKanban(
      userName = priority.userName,
      priorityId = priority.priorityId,
      priorityName = priority.priorityName,
      description = priority.description.getOrElse(""),
      isDefault = priority.isDefault,
      ordering = priority.ordering,
      color = priority.color)(repositoryName: RepositoryName)
}

