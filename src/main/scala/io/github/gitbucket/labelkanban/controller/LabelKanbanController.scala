package io.github.gitbucket.labelkanban.controller

import labelkanban.gitbucket.html
import gitbucket.core.controller.ControllerBase
import gitbucket.core.service.RepositoryService.RepositoryInfo
import gitbucket.core.service._
import gitbucket.core.util._

import gitbucket.core.api.{ApiComment, ApiUser,ApiLabel, CreateAComment, JsonFormat}
import gitbucket.core.util.Implicits._

class TagKanbanController extends TagKanbanControllerBase
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

trait TagKanbanControllerBase extends ControllerBase {

  self: RepositoryService
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

  post("/api/v3/repos/:owner/:repository/issues/:id/plugin/labelkanban/labels/new/:labelName")(readableUsersOnly { repository =>
    try {
      val issueId = params("id")
      val issue = getIssue(repository.owner, repository.name, issueId)
      val labels = getLabels(repository.owner, repository.name)
      val issueLabels = getIssueLabels(repository.owner, repository.name, issueId.toInt)
      val labelName = params("labelName")

      val id = labels.find(_.labelName == labelName).get.labelId
      registerIssueLabel(repository.owner, repository.name, issueId.toInt, id)

      JsonFormat(
        getIssueLabels(repository.owner, repository.name, issueId.toInt).map { label =>
          ApiLabel(label, RepositoryName(repository))
        })
    }catch {
      case e:Exception =>
      {
        JsonFormat(e.getLocalizedMessage -> e.fillInStackTrace)
      }
    }
  })

  post("/api/v3/repos/:owner/:repository/issues/:id/plugin/labelkanban/labels/delete/:labelName")(readableUsersOnly { repository =>
    try {
      val issueId = params("id")
      val issue = getIssue(repository.owner, repository.name, issueId)
      val labels = getLabels(repository.owner, repository.name)
      val issueLabels = getIssueLabels(repository.owner, repository.name, issueId.toInt)
      val labelName = params("labelName")

      val id = labels.find(_.labelName == labelName).get.labelId
      deleteIssueLabel(repository.owner, repository.name, issueId.toInt, id)

      JsonFormat(
        getIssueLabels(repository.owner, repository.name, issueId.toInt).map { label =>
          ApiLabel(label, RepositoryName(repository))
        })
    }catch {
      case e:Exception =>
      {
        JsonFormat(e.getLocalizedMessage -> e.fillInStackTrace)
      }
    }
  })
}
