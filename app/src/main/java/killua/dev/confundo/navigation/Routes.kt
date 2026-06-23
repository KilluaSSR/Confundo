package killua.dev.confundo.navigation

object Routes {
    const val MAIN_GRAPH = "main_graph"
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val APP_DETAIL = "app_detail/{pkg}"
    const val TEMPLATE_MANAGE = "template_manage"
    const val TEMPLATE_DETAIL = "template_detail/{templateId}"

    fun appDetail(pkg: String) = "app_detail/$pkg"
    fun templateDetail(templateId: String) = "template_detail/$templateId"
}
