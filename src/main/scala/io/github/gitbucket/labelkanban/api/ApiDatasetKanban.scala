package io.github.gitbucket.labelkanban.api

import gitbucket.core.api.{ApiPath, FieldSerializable}
import gitbucket.core.util.RepositoryName
import gitbucket.core.view.helpers

case class ApiDataSetKanban(
                             issues: List[ApiIssueKanban],
                             assignees: List[ApiAssigneeKanban],
                             labels: List[ApiLabelKanban],
                             milestones: List[ApiMilestoneKanban],
                             priorities: List[ApiPriorityKanban]
                           )(repositoryName: RepositoryName)
  extends FieldSerializable {

}

