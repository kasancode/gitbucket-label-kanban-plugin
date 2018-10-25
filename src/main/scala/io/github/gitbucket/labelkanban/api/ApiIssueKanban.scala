package io.github.gitbucket.labelkanban.api

import java.util.Date

import gitbucket.core.api._
import gitbucket.core.model.Issue
import gitbucket.core.util.RepositoryName
import gitbucket.core.view.helpers

case class ApiIssueKanban(
                           userName: String,
                           issueId: Int,
                           openedUserName: String,
                           milestoneId: Int,
                           priorityId: Int,
                           assignedUserName: String,
                           title: String,
                           content: String,
                           closed: Boolean,
                           registeredDate: java.util.Date,
                           updatedDate: java.util.Date,
                           isPullRequest: Boolean,
                           labelNames: List[String]
                         )(repositoryName: RepositoryName)
  extends FieldSerializable {
  val html_url = ApiPath(s"/${repositoryName.fullName}/issues/${issueId}")
  val comments_url = ApiPath(s"/api/v3/repos/${repositoryName.fullName}/issues/${issueId}/comments")
}


object ApiIssueKanban {
  def apply(issue: Issue, labelNames: List[String], repositoryName: RepositoryName): ApiIssueKanban =
    ApiIssueKanban(
      userName = issue.userName,
      issueId = issue.issueId,
      openedUserName = issue.openedUserName,
      milestoneId = issue.milestoneId.getOrElse(-1),
      priorityId = issue.priorityId.getOrElse(-1),
      assignedUserName = issue.assignedUserName.getOrElse(""),
      title = issue.title,
      content = issue.content.getOrElse(""),
      closed = issue.closed,
      registeredDate = issue.registeredDate,
      updatedDate = issue.updatedDate,
      isPullRequest = issue.isPullRequest,

      labelNames = labelNames
    )(repositoryName)
}