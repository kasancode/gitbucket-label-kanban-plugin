package io.github.gitbucket.labelkanban.api

import gitbucket.core.api.{ApiPath, FieldSerializable}
import gitbucket.core.model.Milestone
import gitbucket.core.util.RepositoryName
import gitbucket.core.view.helpers
import gitbucket.core.view.helpers.urlEncode

case class ApiMilestoneKanban(userName: String,
                              milestoneId: Int = 0,
                              title: String,
                              description: Option[String],
                              dueDate: Option[java.util.Date],
                              closedDate: Option[java.util.Date]
                       )(
                          repositoryName: RepositoryName
                       ) extends FieldSerializable
{
  val html_url = ApiPath(s"/${repositoryName.fullName}/issues?milestone=${helpers.urlEncode(title)}&state=open")
  val switch_url = ApiPath(s"/api/v3/repos/${repositoryName.fullName}/plugin/labelkanban/milestone/${milestoneId}/switch/issue/")
}

object ApiMilestoneKanban {
  def apply(milestone: Milestone, repositoryName: RepositoryName): ApiMilestoneKanban =
    ApiMilestoneKanban(
      userName = milestone.userName,
      milestoneId = milestone.milestoneId,
      title = milestone.title,
      description = milestone.description,
      dueDate = milestone.dueDate,
      closedDate = milestone.closedDate
    )(repositoryName)
}