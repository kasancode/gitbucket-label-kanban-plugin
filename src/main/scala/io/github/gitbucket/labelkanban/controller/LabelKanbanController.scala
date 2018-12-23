package io.github.gitbucket.labelkanban.controller

import labelkanban.gitbucket.html
import gitbucket.core.controller.ControllerBase
import gitbucket.core.service.RepositoryService.RepositoryInfo
import gitbucket.core.service._
import gitbucket.core.util._
import gitbucket.core.api._
import gitbucket.core.util.Implicits._
import io.github.gitbucket.labelkanban.api._
import io.github.gitbucket.labelkanban.service.LabelKanbanService
import org.scalatra.{Created, UnprocessableEntity}
import java.util.Date

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

  get("/:owner/:repository/labalkanban")(
    referrersOnly {
      repository: RepositoryInfo => {
        html.labelkanban(
          repository
        )
      }
    }
  )

  get("/api/v3/repos/:owner/:repository/plugin/labelkanban/dataset")(referrersOnly { repository =>
    JsonFormat(
      ApiDataSetKanban(
        getOpenIssues(repository.owner, repository.name)
          .map(issue =>
            ApiIssueKanban(
              issue,
              getIssueLabels(repository.owner, repository.name, issue.issueId).map { label =>
                label.labelName
              },
              RepositoryName(repository)
            )
          )
        ,
        getAssignableUserNames(repository.owner, repository.name)
          .map(assignee =>
            ApiAssigneeKanban(assignee)(RepositoryName(repository))
          )
        ,
        getLabels(repository.owner, repository.name)
          .map(label =>
            ApiLabelKanban(label, RepositoryName(repository))
          )
        ,
        getMilestonesWithIssueCount(repository.owner, repository.name)
          .filter(items =>
            items._2 > 0 || items._3 == 0 || (items._1.dueDate.isDefined && items._1.dueDate.get.after(new Date)))
          .reverse
          .map(items =>
            ApiMilestoneKanban(items._1, RepositoryName(repository))
          )
        ,
        getPriorities(repository.owner, repository.name)
          .reverse
          .map(priority =>
            ApiPriorityKanban(priority, RepositoryName(repository))
          )
      )(RepositoryName(repository))
    )
  }

  )

  get("/api/v3/repos/:owner/:repository/plugin/labelkanban/issues")(referrersOnly { repository =>
    JsonFormat(
      getOpenIssues(repository.owner, repository.name).map(issue =>
        ApiIssueKanban(
          issue,
          getIssueLabels(repository.owner, repository.name, issue.issueId).map { label =>
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
      getLabels(repository.owner, repository.name).map(label =>
        ApiLabelKanban(label, RepositoryName(repository))
      ))
  })

  get("/api/v3/repos/:owner/:repository/plugin/labelkanban/milestones")(referrersOnly { repository =>
    JsonFormat(
      getMilestonesWithIssueCount(repository.owner, repository.name)
        .filter(items =>
          items._2 > 0 || items._3 == 0 || (items._1.dueDate.isDefined && items._1.dueDate.get.after(new Date)))
        .reverse
        .map(items =>
          ApiMilestoneKanban(items._1, RepositoryName(repository))
        ))
  })

  get("/api/v3/repos/:owner/:repository/plugin/labelkanban/priorities")(referrersOnly { repository =>
    JsonFormat(
      getPriorities(repository.owner, repository.name)
        .reverse
        .map(priority =>
          ApiPriorityKanban(priority, RepositoryName(repository))
        )
    )
  })

  get("/api/v3/repos/:owner/:repository/plugin/labelkanban/assignees")(referrersOnly { repository =>
    JsonFormat(
      getAssignableUserNames(repository.owner, repository.name).map(assignee =>
        ApiAssigneeKanban(assignee)(RepositoryName(repository))
      )
    )
  })

  get("/api/v3/repos/:owner/:repository/plugin/labelkanban/priority/:pid/switch/issue/:iid")(readableUsersOnly { repository =>
    val issueId = params("iid").toInt
    val priorityId = params("pid").toInt match {
      case i if i > 0 => Some(i)
      case _ => None
    }

    updatePriorityId(repository.owner, repository.name, issueId, priorityId, true)

    getApiIssue(issueId, repository)
  })

  get("/api/v3/repos/:owner/:repository/plugin/labelkanban/milestone/:mid/switch/issue/:iid")(readableUsersOnly { repository =>
    val issueId = params("iid").toInt
    val milestoneId = params("mid").toInt match {
      case i if i > 0 => Some(i)
      case _ => None
    }

    updateMilestoneId(repository.owner, repository.name, issueId, milestoneId, true)

    getApiIssue(issueId, repository)
  })


  get("/api/v3/repos/:owner/:repository/plugin/labelkanban/assignee/:assignee/attach/issue/:iid")(readableUsersOnly { repository =>
    val issueId = params("iid").toInt
    val assignee = params("assignee") match {
      case s: String if s.length > 0 => Some(s)
      case _ => None
    }

    updateAssignedUserName(repository.owner, repository.name, issueId, assignee, true)

    getApiIssue(issueId, repository)
  })

  get("/api/v3/repos/:owner/:repository/plugin/labelkanban/assignee/detach/issue/:iid")(readableUsersOnly { repository =>
    val issueId = params("iid").toInt

    updateAssignedUserName(repository.owner, repository.name, issueId, None, true)

    getApiIssue(issueId, repository)
  })

  get("/api/v3/repos/:owner/:repository/plugin/labelkanban/label/:lid/detach/issue/:iid")(readableUsersOnly { repository =>
    val issueId = params("iid").toInt
    val labelId = params("lid").toInt

    deleteIssueLabel(repository.owner, repository.name, issueId, labelId, true)

    getApiIssue(issueId, repository)
  })

  get("/api/v3/repos/:owner/:repository/plugin/labelkanban/label/:lid/attach/issue/:iid")(readableUsersOnly { repository =>
    val issueId = params("iid").toInt
    val labelId = params("lid").toInt

    registerIssueLabel(repository.owner, repository.name, issueId, labelId, true)

    getApiIssue(issueId, repository)
  })

  def getApiIssue(issueId: Int, repository: RepositoryInfo): String = {
    val issue = getIssue(repository.owner, repository.name, issueId.toString).get

    JsonFormat(
      ApiIssueKanban(
        issue,
        getIssueLabels(repository.owner, repository.name, issue.issueId).map { label =>
          label.labelName
        },
        RepositoryName(repository)
      )
    )
  }
}
