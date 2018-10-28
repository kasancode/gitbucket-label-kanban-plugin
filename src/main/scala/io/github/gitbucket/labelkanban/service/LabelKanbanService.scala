package io.github.gitbucket.labelkanban.service

import java.util.{Date,Calendar}
import gitbucket.core.model.{
  Issue,
  PullRequest,
  IssueComment,
  IssueLabel,
  Label,
  Account,
  Repository,
  CommitState,
  Role,
  Milestone
}
import gitbucket.core.model.Profile._
import gitbucket.core.model.Profile.profile.blockingApi._
import gitbucket.core.model.Profile.dateColumnType


trait LabelKanbanService {
  def getOpenIssues(owner: String, repository: String)(implicit s: Session): List[Issue] = {
    Issues.filter(
      r =>
        r.userName === owner &&
        r.repositoryName === repository &&
        r.closed === false)
      .sortBy(r => r.registeredDate.desc)
      .list
  }
}
