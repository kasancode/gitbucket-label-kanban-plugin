package io.github.gitbucket.labelkanban.service

import gitbucket.core.model.{
  Issue
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
