package io.github.gitbucket.labelkanban.controller

import labelkanban.gitbucket.html

import gitbucket.core.service.IssuesService._
import gitbucket.core.service._
import gitbucket.core.util.SyntaxSugars._
import gitbucket.core.util.Implicits._
import gitbucket.core.util._
import gitbucket.core.view
import gitbucket.core.view.Markdown

import gitbucket.core.controller.ControllerBase
import gitbucket.core.service.RepositoryService.RepositoryInfo
import gitbucket.core.service._
import gitbucket.core.util._
import gitbucket.core.api._
import gitbucket.core.util.Implicits._
import io.github.gitbucket.labelkanban.api._
import io.github.gitbucket.labelkanban.service.LabelKanbanService
import org.scalatra.{Created, NotFound, UnprocessableEntity}
import java.util.Date

import gitbucket.core.model.{Label, Milestone, Priority}
import gitbucket.core.service.IssuesService._
import gitbucket.core.util.SyntaxSugars.defining

import scala.collection.mutable

class LabelKanbanController extends labelKanbanControllerBase
  with LabelKanbanService
  with IssuesService
  with RepositoryService
  with AccountService
  with LabelsService
  with MilestonesService
  with ActivityService
  with IssueCreationService
  with WebHookIssueCommentService
  with WebHookPullRequestService
  with ReadableUsersAuthenticator
  with ReferrerAuthenticator
  with WritableUsersAuthenticator
  with PrioritiesService
  with PullRequestService
  with CommitsService
  with WebHookService
  with MergeService
  with WebHookPullRequestReviewCommentService

trait labelKanbanControllerBase extends ControllerBase {

  self: LabelKanbanService
    with IssuesService
    with RepositoryService
    with AccountService
    with LabelsService
    with MilestonesService
    with ActivityService
    with IssueCreationService
    with WebHookIssueCommentService
    with WebHookPullRequestService
    with ReadableUsersAuthenticator
    with ReferrerAuthenticator
    with WritableUsersAuthenticator
    with PrioritiesService =>

  val prefix = "@"

  get("/:owner/:repository/labelkanban")(
    referrersOnly {
      repository: RepositoryInfo => {
        html.repository(
          prefix,
          repository
        )
      }
    }
  )

  get("/summarykanban") {
    redirect(s"/summarykanban/${context.loginAccount.map(_.userName).getOrElse("")}")
  }

  get("/summarykanban/:owner") {
    val account = getAccountByUserName(params("owner"))
    val repos = getVisibleRepositories(context.loginAccount, withoutPhysicalInfo = true)

    html.summary(prefix, repos, account.get)
  }

  get("/summarykanban/:owner/profile") {
    val owner = params("owner")
    getAccountByUserName(owner).map { account =>
      val extraMailAddresses = getAccountExtraMailAddresses(owner)
      html.profile(
        prefix,
        account,
        if (account.isGroupAccount) Nil else getGroupsByUserName(owner),
        extraMailAddresses
      )
    }.getOrElse(NotFound())
  }

  get("/:owner/:repository/labelkanban/issues/new")(readableUsersOnly { repository =>
    if (isIssueEditable(repository)) {
      val labelIds = multiParams("label")
      val milestoneId = params.get("milestone")
      val priorityId = params.get("priority")
      val assigneeName = params.get("assignee")

      defining(repository.owner, repository.name) {
        case (owner, name) =>
          html.newissue(
            getAssignableUserNames(owner, name),
            getMilestones(owner, name),
            getPriorities(owner, name),
            getDefaultPriority(owner, name),
            getLabels(owner, name),
            isIssueManageable(repository),
            getContentTemplate(repository, "ISSUE_TEMPLATE"),
            repository,
            assigneeName,
            milestoneId,
            priorityId,
            labelIds
          )
      }
    } else Unauthorized()
  })

  get("/api/v3/repos/:owner/:repository/plugin/labelkanban/dataset")(referrersOnly { repository =>
    JsonFormat(
      ApiDataSetKanban(
        getOpenIssues(repository.owner, repository.name)
          .map(issue =>
            ApiIssueKanban(
              issue,
              getIssueLabels(repository.owner, repository.name, issue.issueId),
              prefix,
              RepositoryName(repository)
            )
          )
        ,
        createLanes(repository)
      )
    )
  }
  )

  get("/api/v3/:owner/plugin/summarykanban/dataset") {
    val user = params("owner")
    val groups = user :: getGroupsByUserName(user)
    val repositories = getVisibleRepositories(context.loginAccount, withoutPhysicalInfo = true)
      .filter(r =>
        (groups.contains(r.owner) ||
          getCollaborators(r.owner, r.repository.repositoryName).exists(c => c._1.collaboratorName == user)) &&
          countIssue(IssueSearchCondition(), false, (r.owner, r.repository.repositoryName)) > 0
      )

    JsonFormat(
      ApiDataSetKanban(
        repositories
          .flatMap(repository =>
            getOpenIssues(repository.owner, repository.name)
              .map(issue =>
                ApiIssueKanban.applySummary(
                  issue,
                  getIssueLabels(repository.owner, repository.name, issue.issueId),
                  prefix,
                  getPriorities(repository.owner, repository.name)
                )
              )
          )
        ,
        createSummaryLanes(repositories)
      )
    )
  }

  get("/api/v3/repos/:owner/:repository/plugin/labelkanban/issues")(referrersOnly { repository =>
    JsonFormat(
      getOpenIssues(repository.owner, repository.name).map(issue =>
        ApiIssueKanban(
          issue,
          getIssueLabels(repository.owner, repository.name, issue.issueId),
          prefix,
          RepositoryName(repository)
        )
      )
    )
  }
  )

  get("/api/v3/repos/:owner/:repository/plugin/labelkanban/labels")(referrersOnly { repository =>
    JsonFormat(
      getLabels(repository.owner, repository.name)
        .sortBy(label => label.labelId)
        .map(label =>
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
    val priorityId = tryToInt(params("pid")) match {
      case Some(i) if i > 0 => Some(i)
      case _ => None
    }

    updatePriorityId(repository.owner, repository.name, issueId, priorityId, true)
    getApiIssue(issueId, repository)
  })

  get("/api/v3/repos/:owner/:repository/plugin/labelkanban/milestone/:mid/switch/issue/:iid")(readableUsersOnly { repository =>
    val issueId = params("iid").toInt
    val milestoneId = tryToInt(params("mid")) match {
      case Some(i) if i > 0 => Some(i)
      case _ => None
    }

    updateMilestoneId(repository.owner, repository.name, issueId, milestoneId, true)

    getApiIssue(issueId, repository)
  })


  get("/api/v3/repos/:owner/:repository/plugin/labelkanban/assignee/:assignee/switch/issue/:iid")(readableUsersOnly { repository =>
    val issueId = params("iid").toInt
    val assignee = params("assignee") match {
      case "-" => None
      case s: String if s.length > 0 => Some(s)
      case _ => None
    }

    updateAssignedUserName(repository.owner, repository.name, issueId, assignee, true)

    getApiIssue(issueId, repository)
  })

  get("/api/v3/repos/:owner/:repository/plugin/labelkanban/label/:lid/switch/issue/:iid")(readableUsersOnly { repository =>
    val issueId = params("iid").toInt
    val labelId = tryToInt(params("lid"), 0)

    getIssueLabels(repository.owner, repository.name, issueId)
      .filter(_.labelName.startsWith(prefix))
      .map(label => deleteIssueLabel(repository.owner, repository.name, issueId, label.labelId, true))

    if (labelId > 0)
      registerIssueLabel(repository.owner, repository.name, issueId, labelId, true)

    getApiIssue(issueId, repository)
  })


  def createDummyLane(key: String, id: String, repository: RepositoryInfo): ApiLaneKanban = {
    ApiLaneKanban(
      id = id,
      name = "",
      color = "333333",
      iconImage = "",
      icon = "",
      htmlUrl = None,
      switchUrl = key match {
        case s if s.length > 0 =>
          Some(ApiPath(s"/api/v3/repos/${RepositoryName(repository).fullName}/plugin/labelkanban/${key}/-/switch/issue/"))
        case _ =>
          None
      },
      paramKey = "",
      order = 0
    )
  }

  def createSummaryDummyLane(id: String): ApiLaneKanban = {
    ApiLaneKanban(
      id = id,
      name = "",
      color = "333333",
      iconImage = "",
      icon = "",
      htmlUrl = None,
      switchUrl = None,
      paramKey = "",
      order = 0
    )
  }

  def createLanes(repository: RepositoryInfo): mutable.LinkedHashMap[String, List[ApiLaneKanban]] = {
    mutable.LinkedHashMap(
      "None" ->
        List[ApiLaneKanban](createDummyLane("", "0", repository))
      ,
      "Label:" + prefix -> (
        createDummyLane("label", "0", repository) ::
          getLabels(repository.owner, repository.name)
            .filter(label =>
              label.labelName.startsWith(prefix))
            .sortBy(label =>
              label.labelId)
            .zipWithIndex
            .map { case (label, index) =>
              ApiLaneKanban(label, RepositoryName(repository), index + 1)
            }
        )
      ,
      "Priorities" -> (
        createDummyLane("priority", "0", repository) ::
          getPriorities(repository.owner, repository.name)
            .reverse
            .zipWithIndex
            .map { case (priority, index) =>
              ApiLaneKanban(priority, RepositoryName(repository), index + 1)
            }
        )
      ,
      "Milestones" -> (
        createDummyLane("milestone", "0", repository) ::
          getMilestonesWithIssueCount(repository.owner, repository.name)
            .filter(items =>
              items._2 > 0 || items._3 == 0 || (items._1.dueDate.isDefined && items._1.dueDate.get.after(new Date)))
            .reverse
            .zipWithIndex
            .map { case (items, index) =>
              ApiLaneKanban(items._1, RepositoryName(repository), index + 1)
            }
        )
      ,
      "Assignees" -> (
        createDummyLane("assignee", "-", repository) ::
          getAssignableUserNames(repository.owner, repository.name)
            .zipWithIndex
            .map { case (assignee, index) =>
              ApiLaneKanban(assignee, RepositoryName(repository), index + 1)
            }
        )
    )
  }

  def createSummaryLanes(repositories: List[RepositoryInfo]): mutable.LinkedHashMap[String, List[ApiLaneKanban]] = mutable.LinkedHashMap(
    "None" ->
      List[ApiLaneKanban](createSummaryDummyLane("-")),
    "Label:" + prefix -> (
      createSummaryDummyLane("-") ::
        repositories.flatMap(repository =>
          getLabels(repository.owner, repository.name)
        )
          .filter(label =>
            label.labelName.startsWith(prefix))
          .sortBy(label =>
            label.labelName)
          .reverse
          .foldLeft(Nil: List[Label]) {
            (acc, next) => if (acc.exists(_.labelName == next.labelName)) acc else next :: acc
          }
          .zipWithIndex
          .map { case (label, index) =>
            ApiLaneKanban(
              id = label.labelName,
              name = label.labelName,
              color = label.color,
              iconImage = "",
              icon = "",
              htmlUrl = None,
              switchUrl = None,
              paramKey = "",
              order = index + 1
            )
          }
      )

    ,
    "Priorities" -> (
      createSummaryDummyLane("-") ::
        repositories.flatMap(repository =>
          getPriorities(repository.owner, repository.name)
        )
          .foldLeft(Nil: List[Priority]) {
            (acc, next) => if (acc.exists(_.priorityName == next.priorityName)) acc else next :: acc
          }
          .zipWithIndex
          .map { case (priority, index) =>
            ApiLaneKanban(
              id = priority.priorityName, // avoid priorityName == "-"
              name = priority.priorityName,
              color = priority.color,
              iconImage = "",
              icon = "",
              htmlUrl = None,
              switchUrl = None,
              paramKey = "",
              order = index + 1
            )
          }
      )

    ,
    "Assignees" -> (
      createSummaryDummyLane("-") ::
        repositories.flatMap(repository =>
          getAssignableUserNames(repository.owner, repository.name)
        )
          .foldLeft(Nil: List[String]) {
            (acc, next) => if (acc.contains(next)) acc else next :: acc
          }
          .zipWithIndex
          .map { case (assignee, index) =>
            ApiLaneKanban(
              id = assignee,
              name = assignee,
              color = "838383",
              iconImage = "",
              icon = "",
              htmlUrl = None,
              switchUrl = None,
              paramKey = "",
              order = index + 1
            )
          }
      )

    ,
    "Repositories" ->
      repositories
        .zipWithIndex
        .map { case (repository, index) =>
          ApiLaneKanban(
            id = repository.name,
            name = repository.name,
            color = "838383",
            iconImage = "",
            icon = "",
            htmlUrl = Some(ApiPath(s"/${RepositoryName(repository).fullName}")),
            switchUrl = None,
            paramKey = "",
            order = index)
        }
  )

  def getApiIssue(issueId: Int, repository: RepositoryInfo): String = {
    val issue = getIssue(repository.owner, repository.name, issueId.toString).get

    JsonFormat(
      ApiIssueKanban(
        issue,
        getIssueLabels(repository.owner, repository.name, issue.issueId),
        prefix,
        RepositoryName(repository)
      )
    )
  }

  def tryToInt(text: String): Option[Int] = try {
    Some(text.toInt)
  } catch {
    case _: java.lang.NumberFormatException => None
  }

  def tryToInt(text: String, default: Int): Int = try {
    text.toInt
  } catch {
    case _: java.lang.NumberFormatException => default
  }
}
