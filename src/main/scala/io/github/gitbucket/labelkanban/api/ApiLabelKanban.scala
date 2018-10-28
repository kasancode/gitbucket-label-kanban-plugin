package io.github.gitbucket.labelkanban.api

import gitbucket.core.api.{ApiPath, FieldSerializable}
import gitbucket.core.model.Label
import gitbucket.core.util.RepositoryName
import gitbucket.core.view.helpers

case class ApiLabelKanban(
                           userName: String,
                           labelId: Int = 0,
                           labelName: String,
                           color: String
                         )(repositoryName: RepositoryName) extends FieldSerializable {
  val html_url = ApiPath(s"/${repositoryName.fullName}/issues?labels=${helpers.urlEncode(labelName)}&state=open")
  val detach_url = ApiPath(s"/api/v3/repos/${repositoryName.fullName}/plugin/labelkanban/label/${labelId}/detach/issue/")
  val attach_url = ApiPath(s"/api/v3/repos/${repositoryName.fullName}/plugin/labelkanban/label/${labelId}/attach/issue/")
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