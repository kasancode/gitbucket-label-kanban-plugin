package io.github.gitbucket.labelkanban.api

import java.util.Date

import gitbucket.core.api._
import gitbucket.core.model.{Issue, Label}
import gitbucket.core.util.RepositoryName
import gitbucket.core.view.helpers

import scala.collection.mutable

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
                           labelNames:List[String],
                           metrics: Map[String,String],
                           show: Boolean = false,
                         )(repositoryName: RepositoryName)
  extends FieldSerializable {
  val htmlUrl = ApiPath(s"/${repositoryName.fullName}/issues/${issueId}")
  val comments_url = ApiPath(s"/api/v3/repos/${repositoryName.fullName}/issues/${issueId}/comments")
}


object ApiIssueKanban {
  def apply(issue: Issue, labels:List[Label], repositoryName: RepositoryName): ApiIssueKanban =
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
      labelNames = labels.map(_.labelName),
      metrics = createMetrics(
        milestoneId = issue.milestoneId,
        priorityId = issue.priorityId,
        assignedUserName = issue.assignedUserName,
        labels = labels
      )
    )(repositoryName)

  def createMetrics(
                     milestoneId: Option[Int],
                     priorityId: Option[Int],
                     assignedUserName: Option[String],
                     labels:List[Label]
                   ) : Map[String, String] = {
    Map(
      "None" -> "0",
      "Label:@" -> labels.find(_.labelName.startsWith("@")).map(_.labelId).getOrElse(0).toString,
      "Milestones"->milestoneId.getOrElse(0).toString(),
      "Priorities"->priorityId.getOrElse(0).toString(),
      "Assignees"->(assignedUserName match {
        case Some(s) if s.length > 0 => s
        case _ => "-"
      })
    )
  }
}