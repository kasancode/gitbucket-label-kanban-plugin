package io.github.gitbucket.labelkanban.api

import gitbucket.core.api.ApiPath
import gitbucket.core.model.{Milestone}
import gitbucket.core.util.RepositoryName
import gitbucket.core.view.helpers

case class ApiMilestone(  userName: String,
                          milestoneId: Int = 0,
                          title: String,
                          description: Option[String],
                          dueDate: Option[java.util.Date],
                          closedDate: Option[java.util.Date]
                       )(
                          repositoryName: RepositoryName
                       )
{
  var url = ApiPath(s"/api/v3/repos/${repositoryName.fullName}/plugin/label/kanban/milestones/${helpers.urlEncode(title)}")
  var html_url = ApiPath(s"/${repositoryName.fullName}/issues/issues?milestone=${helpers.urlEncode(title)}&state=open")

}

object ApiMilestone {
  def apply(milestone: Milestone, repositoryName: RepositoryName): ApiMilestone =
    ApiMilestone(
      userName = milestone.userName,
      milestoneId = milestone.milestoneId,
      title = milestone.title,
      description = milestone.description,
      dueDate = milestone.dueDate,
      closedDate = milestone.closedDate
    )(repositoryName)
}