import gitbucket.core.api.ApiPath
import io.github.gitbucket.solidbase.model.Version
import gitbucket.core.controller.{Context, ControllerBase}
import gitbucket.core.model.Account
import gitbucket.core.plugin.{Link, PluginRegistry}
import gitbucket.core.service.RepositoryService.RepositoryInfo
import gitbucket.core.service.SystemSettingsService.SystemSettings
import gitbucket.core.view.helpers
import io.github.gitbucket.labelkanban.controller.LabelKanbanController
import javax.servlet.ServletContext

class Plugin extends gitbucket.core.plugin.Plugin {
  override val pluginId: String = "labelkanban"
  override val pluginName: String = "Label Kanban Plugin"
  override val description: String = "Provide Kanban style issue management."
  override val versions: List[Version] = List(
    new Version("1.0.0"),
    new Version("1.0.1"),
    new Version("1.0.2"),
    new Version("1.0.3"),
    new Version("1.0.4"),
    new Version("1.0.5"),
    new Version("2.0.0"),
    new Version("2.0.1"),
    new Version("2.0.2"),
    new Version("2.0.3"),
    new Version("2.0.4"),
    new Version("2.0.5"),
    new Version("3.0.0"),
    new Version("3.0.1"),
    new Version("3.1.0"),
    new Version("3.2.0"),
    new Version("3.3.0"),
  )

  override val controllers = Seq(
    "/*" -> new LabelKanbanController()
  )

  override val assetsMappings = Seq("/labelkanban" -> "/plugins/labelkanban/assets")

  // "override val" is difficult to resolve url

  override val globalMenus = Seq(
    (context: Context) => if (context.loginAccount.isDefined) Some(Link("summarykanban", "Summary board", s"summarykanban/${context.loginAccount.get.userName}/")) else None
  )

  override val repositoryMenus = Seq(
    (repositoryInfo: RepositoryInfo, context: Context) => Some(Link("labelkanban", "Kanban", "/labelkanban", Some("inbox")))
  )

  override val profileTabs = Seq(
    (account: Account, context: Context) => Some(Link("summarykanban", "Summary board", s"summarykanban/${account.userName}/profile"))
  )

  override val dashboardTabs = Seq(
    (context: Context) => if (context.loginAccount.isDefined) Some(Link("summarykanban", "Summary board", s"summarykanban/${context.loginAccount.get.userName}/")) else None
  )
}
