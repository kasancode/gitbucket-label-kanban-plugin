package io.github.gitbucket.labelkanban.controller

import labelkanban.gitbucket.html
import gitbucket.core.controller.ControllerBase
import gitbucket.core.service.RepositoryService.RepositoryInfo
import gitbucket.core.service._
import gitbucket.core.util._
import gitbucket.core.api._
import gitbucket.core.util.Implicits._
import io.github.gitbucket.labelkanban.api.{ApiIssueKanban, ApiLabelKanban, ApiMilestoneKanban, ApiPriorityKanban}
import io.github.gitbucket.labelkanban.service.LabelKanbanService
import org.scalatra.{Created, UnprocessableEntity}

class LabelKanbanController extends LabelKanbanControllerBase
  with LabelKanbanService
  with RepositoryService
  with AccountService
  with RequestCache
  with ProtectedBranchService
  with IssuesService
  with LabelsService
  with MilestonesService
  with PullRequestService
  with CommitsService
  with CommitStatusService
  with PrioritiesService
  with OwnerAuthenticator
  with UsersAuthenticator
  with GroupManagerAuthenticator
  with ReferrerAuthenticator
  with ReadableUsersAuthenticator
  with WritableUsersAuthenticator

trait LabelKanbanControllerBase extends ControllerBase {

  self: LabelKanbanService
    with RepositoryService
    with AccountService
    with RequestCache
    with ProtectedBranchService
    with IssuesService
    with LabelsService
    with MilestonesService
    with PullRequestService
    with CommitsService
    with CommitStatusService
    with PrioritiesService
    with OwnerAuthenticator
    with UsersAuthenticator
    with GroupManagerAuthenticator
    with ReferrerAuthenticator
    with ReadableUsersAuthenticator
    with WritableUsersAuthenticator =>

  get("/:owner/:repository/labalkanban/:prefix")(
    referrersOnly {
      repository: RepositoryInfo => {
        html.labelkanban(
          repository,
          params("prefix")
        )
      }
    }
  )

  get("/api/v3/repos/:owner/:repository/plugin/labelkanban/issues")(referrersOnly { repository =>
      JsonFormat(
        getOpenIssues(repository.owner, repository.name).map(issue =>
          ApiIssueKanban(
            issue,
            getIssueLabels(repository.owner, repository.name, issue.issueId).map{label =>
              label.labelName
            },
            RepositoryName(repository)
          )
        )
      )
    }
  )

  get("/api/v3/repos/:owner/:repository/plugin/labelkanban/labels")(referrersOnly { repository =>
    JsonFormat(
      getLabels(repository.owner, repository.name).map{label =>
        ApiLabelKanban(label, RepositoryName(repository))
      })
  })

  get("/api/v3/repos/:owner/:repository/plugin/labelkanban/milestones")(referrersOnly { repository =>
    JsonFormat(
      getMilestones(repository.owner, repository.name).map{milestone =>
        ApiMilestoneKanban(milestone, RepositoryName(repository))
    })
  })

  get("/api/v3/repos/:owner/:repository/plugin/labelkanban/priorities")(referrersOnly { repository =>
    JsonFormat(
      getPriorities(repository.owner, repository.name).map(priority =>
        ApiPriorityKanban(priority, RepositoryName(repository))
      )
    )
  })

  get("/api/v3/repos/:owner/:repository/plugin/labelkanban/priority/:pid/switch/issue/:iid")(readableUsersOnly{repository =>
    val issueId = params("iid").toInt
    val priorityId = params("pid").toInt match {
      case i if i > 0 => Some(i)
      case _ => None
    }

    updatePriorityId(repository.owner, repository.name, issueId, priorityId)

    val issue = getIssue(repository.owner, repository.name, issueId.toString).get

    JsonFormat(
      ApiIssueKanban(
        issue,
        getIssueLabels(repository.owner, repository.name, issue.issueId).map{label =>
          label.labelName
        },
        RepositoryName(repository)
      )
    )
  })

  get("/api/v3/repos/:owner/:repository/plugin/labelkanban/milestone/:mid/switch/issue/:iid")(readableUsersOnly{repository =>
    val issueId = params("iid").toInt
    val milestoneId = params("mid").toInt match {
      case i if i > 0 => Some(i)
      case _ => None
    }

    updateMilestoneId(repository.owner, repository.name, issueId, milestoneId)

    val issue = getIssue(repository.owner, repository.name, issueId.toString).get

    JsonFormat(
      ApiIssueKanban(
        issue,
        getIssueLabels(repository.owner, repository.name, issue.issueId).map{label =>
          label.labelName
        },
        RepositoryName(repository)
      )
    )
  })


  get("/api/v3/repos/:owner/:repository/plugin/labelkanban/label/:lid/prefix/:pre/switch/issue/:iid")(readableUsersOnly{repository =>
    val issueId = params("iid").toInt
    val labelId = params("lid").toInt
    val prefix = params("pre")

    val labels = getLabels(repository.owner, repository.name)
    labels.filter(_.labelName.startsWith(prefix)).map( label =>
      deleteIssueLabel(repository.owner, repository.name, issueId, label.labelId,true)
    )

    if(labelId > 0) {
      registerIssueLabel(repository.owner, repository.name, issueId, labelId, true)
    }

    val issue = getIssue(repository.owner, repository.name, issueId.toString).get

    JsonFormat(
      ApiIssueKanban(
        issue,
        getIssueLabels(repository.owner, repository.name, issue.issueId).map{label =>
          label.labelName
        },
        RepositoryName(repository)
      )
    )
  })

}
