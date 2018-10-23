package io.github.gitbucket.labelkanban.api

import java.util.Date

import gitbucket.core.api._
import gitbucket.core.model.Issue
import gitbucket.core.util.RepositoryName
import gitbucket.core.view.helpers

case class ApiIssueKanban(
                           number: Int,
                           title: String,
                           userName: String,
                           state: String,
                           created_at: Date,
                           updated_at: Date,
                           body: String,
                           labelNames: List[String],
                           milestoneId: Int,
                           priorityId: Int,
                           isPullRequest: Boolean
                         )(repositoryName: RepositoryName) extends FieldSerializable
{
  val html_url = ApiPath(s"/${repositoryName.fullName}/issues/${number}")
  val comments_url = ApiPath(s"/api/v3/repos/${repositoryName.fullName}/issues/${number}/comments")
}


object ApiIssueKanban {
  def apply(issue: Issue, labelNames: List[String], repositoryName: RepositoryName): ApiIssueKanban =
    ApiIssueKanban(
      number = issue.issueId,
      title = issue.title,
      userName = issue.userName,
      state = if (issue.closed) {
        "closed"
      } else {
        "open"
      },
      labelNames = labelNames,
      body = issue.content.getOrElse(""),
      created_at = issue.registeredDate,
      updated_at = issue.updatedDate,
      milestoneId = issue.milestoneId.getOrElse(-1),
      priorityId = issue.priorityId.getOrElse(-1),
      isPullRequest = issue.isPullRequest
    )(repositoryName)
}