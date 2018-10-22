package io.github.gitbucket.labelkanban.api

import gitbucket.core.util.RepositoryName
import gitbucket.core.model.Priority

case class ApiPriority(
                        userName: String,
                        priorityId: Int = 0,
                        priorityName: String,
                        description: String,
                        isDefault: Boolean,
                        ordering: Int = 0,
                        color: String
                      )(repositoryName: RepositoryName) {
  var url = ""
  var html_url = ""
}

object ApiPriority {
  def apply(priority: Priority, repositoryName: RepositoryName): ApiPriority =
    ApiPriority(
      userName = priority.userName,
      priorityId = priority.priorityId,
      priorityName = priority.priorityName,
      description = priority.description.getOrElse(""),
      isDefault = priority.isDefault,
      ordering = priority.ordering,
      color = priority.color)(repositoryName: RepositoryName)

}

