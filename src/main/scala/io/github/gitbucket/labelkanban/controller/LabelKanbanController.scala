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
import org.scalatra.{Created, NotFound, UnprocessableEntity}
import java.util.Date

import gitbucket.core.model.{Label, Milestone, Priority}
import gitbucket.core.service.IssuesService.IssueSearchCondition

import scala.collection.mutable

class LabelKanbanController extends labelKanbanControllerBase
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

trait labelKanbanControllerBase extends ControllerBase {

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

  val prefix = "@"

  get("/:owner/:repository/labalkanban")(
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
        createLanes(repository),
        createDummyLanes(repository)
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
        createSummaryLanes(repositories),
        createSummaryDummyLanes()
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

  def createDummyLanes(repository: RepositoryInfo): Map[String, ApiLaneKanban] = {
    Map(
      "None" -> createDummyLane("", "0", repository),
      "Label:" + prefix -> createDummyLane("label", "0", repository),
      "Milestones" -> createDummyLane("milestone", "0", repository),
      "Priorities" -> createDummyLane("priority", "0", repository),
      "Assignees" -> createDummyLane("assignee", "-", repository),
    )
  }

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
      }
    )
  }

  def createSummaryDummyLane(key: String, id: String): ApiLaneKanban = {
    ApiLaneKanban(
      id = id,
      name = "",
      color = "333333",
      iconImage = "",
      icon = "",
      htmlUrl = None,
      switchUrl = None
    )
  }

  def createSummaryDummyLanes(): Map[String, ApiLaneKanban] = {
    Map(
      "None" -> createSummaryDummyLane("", ""),
      "Label:" + prefix -> createSummaryDummyLane("", ""),
      "Priorities" -> createSummaryDummyLane("", ""),
      "Assignees" -> createSummaryDummyLane("", ""),
      "Repositories" -> createSummaryDummyLane("", "")
    )
  }

  def createLanes(repository: RepositoryInfo): mutable.LinkedHashMap[String, List[ApiLaneKanban]] = {
    mutable.LinkedHashMap(
      "None" ->
        List[ApiLaneKanban](),
      "Label:" + prefix ->
        getLabels(repository.owner, repository.name)
          .filter(label =>
            label.labelName.startsWith(prefix))
          .sortBy(label =>
            label.labelId)
          .map(label =>
            ApiLaneKanban(label, RepositoryName(repository))
          ),
      "Priorities" ->
        getPriorities(repository.owner, repository.name)
          .reverse
          .map(priority =>
            ApiLaneKanban(priority, RepositoryName(repository))
          ),
      "Milestones" ->
        getMilestonesWithIssueCount(repository.owner, repository.name)
          .filter(items =>
            items._2 > 0 || items._3 == 0 || (items._1.dueDate.isDefined && items._1.dueDate.get.after(new Date)))
          .reverse
          .map(items =>
            ApiLaneKanban(items._1, RepositoryName(repository))
          ),
      "Assignees" ->
        getAssignableUserNames(repository.owner, repository.name)
          .map(assignee =>
            ApiLaneKanban(assignee, RepositoryName(repository))
          )
    )
  }


  def createSummaryLanes(repositories: List[RepositoryInfo]): mutable.LinkedHashMap[String, List[ApiLaneKanban]] = {
    mutable.LinkedHashMap(
      "None" ->
        List[ApiLaneKanban](),
      "Label:" + prefix ->
        repositories.flatMap(repository =>
          getLabels(repository.owner, repository.name)
            .filter(label =>
              label.labelName.startsWith(prefix))
            .sortBy(label =>
              label.labelName)
            .map(label =>
              ApiLaneKanban(
                id = label.labelName,
                name = label.labelName,
                color = label.color,
                iconImage = "",
                icon = "",
                htmlUrl = None,
                switchUrl = None)
            ))
          .foldLeft(Nil: List[ApiLaneKanban]) {
            (acc, next) => if (acc.exists(_.id == next.id)) acc else next :: acc
          }
          .reverse,
      "Priorities" ->
        repositories.flatMap(repository =>
          getPriorities(repository.owner, repository.name)
            .reverse
            .map(priority =>
              ApiLaneKanban(
                id = priority.priorityName,
                name = priority.priorityName,
                color = priority.color,
                iconImage = "",
                icon = "",
                htmlUrl = None,
                switchUrl = None
              )
            ))
          .foldLeft(Nil: List[ApiLaneKanban]) {
            (acc, next) => if (acc.exists(_.id == next.id)) acc else next :: acc
          }
          .reverse,
      "Assignees" ->
        repositories.flatMap(repository =>
          getAssignableUserNames(repository.owner, repository.name)
          .map(assignee =>
            ApiLaneKanban(assignee, RepositoryName(repository))
          ))
          .foldLeft(Nil: List[ApiLaneKanban]) {
            (acc, next) => if (acc.exists(_.id == next.id)) acc else next :: acc
          },
      "Repositories" ->
        repositories.map(repository =>
          ApiLaneKanban(
            id = repository.name,
            name = repository.name,
            color = "838383",
            iconImage = "",
            icon = "",
            htmlUrl = Some(ApiPath(s"/${RepositoryName(repository).fullName}")),
            switchUrl = None)
        ))
  }

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
