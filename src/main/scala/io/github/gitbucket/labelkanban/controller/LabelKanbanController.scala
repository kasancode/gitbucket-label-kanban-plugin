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

import gitbucket.core.model.{Label, Milestone, Priority}

import scala.collection.mutable

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
              getIssueLabels(repository.owner, repository.name, issue.issueId),
              RepositoryName(repository)
            )
          )
        ,
        getLanes(repository),
        createDummyLanes(repository)
      )(RepositoryName(repository))
    )
  }

  )

  get("/api/v3/repos/:owner/:repository/plugin/labelkanban/issues")(referrersOnly { repository =>
    JsonFormat(
      getOpenIssues(repository.owner, repository.name).map(issue =>
        ApiIssueKanban(
          issue,
          getIssueLabels(repository.owner, repository.name, issue.issueId),
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
      .filter(_.labelName.startsWith("@"))
      .map(label => deleteIssueLabel(repository.owner, repository.name, issueId, label.labelId, true))

    if (labelId > 0)
      registerIssueLabel(repository.owner, repository.name, issueId, labelId, true)

    getApiIssue(issueId, repository)
  })

  def createDummyLanes(repository: RepositoryInfo): Map[String, ApiLaneKanban] = {
    Map(
      "None" -> createDummyLane("", "0", repository),
      "Label:@" -> createDummyLane("label","0", repository),
      "Milestones" -> createDummyLane("milestone","0", repository),
      "Priorities" -> createDummyLane("priority","0", repository),
      "Assignees" -> createDummyLane("assignee","-", repository),
    )
  }

  def createDummyLane(key: String, id: String = "0", repository: RepositoryInfo): ApiLaneKanban = {
    ApiLaneKanban(
      id = id,
      name = "",
      color = "333333",
      iconImage = "",
      icon = "",
      htmlUrl = "",
      switchUrl = key match {
        case s if s.length > 0 =>
          ApiPath(s"/api/v3/repos/${RepositoryName(repository).fullName}/plugin/labelkanban/${key}/-/switch/issue/").path
        case _ =>
          ""
      }
    )(RepositoryName(repository))
  }

  def getLanes(repository: RepositoryInfo): mutable.LinkedHashMap[String, List[ApiLaneKanban]] = {
    val prefix = "@"

    mutable.LinkedHashMap(
      "None" ->
        List[ApiLaneKanban](),

      "Label:" + prefix ->
        getLabels(repository.owner, repository.name)
          .filter(label =>
            label.labelName.startsWith(prefix))
          .sortBy(label =>
            label.labelName)
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

  def getApiIssue(issueId: Int, repository: RepositoryInfo): String = {
    val issue = getIssue(repository.owner, repository.name, issueId.toString).get

    JsonFormat(
      ApiIssueKanban(
        issue,
        getIssueLabels(repository.owner, repository.name, issue.issueId),
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
