package io.github.gitbucket.labelkanban.api

import gitbucket.core.model.Label
import gitbucket.core.util.RepositoryName

case class ApiLabelKanban(
                           userName: String,
                           labelId: Int = 0,
                           labelName: String,
                           color: String
                         )(repositoryName: RepositoryName) {
  var url = ""
  var html_url = ""
  var switch_url = ""
}

object ApiLabelKanban {
  def apply(label: Label, repositoryName: RepositoryName): ApiLabelKanban =
    ApiLabelKanban(
      userName = label.userName,
      labelId = label.labelId,
      labelName = label.labelName,
      color = label.color
    )(repositoryName)
}