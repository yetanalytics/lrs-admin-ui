(ns com.yetanalytics.lrs-admin-ui.language)

(def language
  {;;Footer   
   :footer.attribution     {:en-US "Yet Analytics Inc."
                            :zh-CN "Yet分析有限公司"}
   :footer.license         {:en-US "Licensed under the Apache 2.0 License"
                            :zh-CN "特许在Apache2.0特许下"}
   :footer.contribute      {:en-US "Contribute on "
                            :zh-CN "贡献在"}
   :footer.contact-note    {:en-US "Contact us to learn about Enterprise Support options."
                            :zh-CN "跟我们联系"}
   :footer.nav.logout      {:en-US "LOGOUT"
                            :zh-CN "登出"}
   :footer.nav.browser     {:en-US "BROWSER"
                            :zh-CN "浏览"}
   :footer.nav.accounts    {:en-US "ACCOUNTS"
                            :zh-CN "账户"}
   :footer.nav.credentials {:en-US "CREDENTIALS"
                            :zh-CN "凭证"}
   ;;Header
   :header.welcome         {:en-US "Welcome, "
                            :zh-CN "欢迎你，"}
   :header.logout          {:en-US "Logout"
                            :zh-CN "登出"}
   :header.nav.credentials {:en-US "Credentials Management"
                            :zh-CN "凭证管理"}
   :header.nav.accounts    {:en-US "Account Management"
                            :zh-CN "账户管理"}
   :header.nav.browser     {:en-US "Statement Browser"
                            :zh-CN "句子浏览"}
   :header.nav.monitor     {:en-US "LRS Monitor"
                            :zh-CN "LRS监视"}
   :header.nav.data        {:en-US "Data Management"
                            :zh-CN "数据管理"}
   :header.nav.reactions   {:en-US "Reactions"
                            :zh-CN "反应管理"}
   ;;Login Page
   :login.title        {:en-US "LOGIN"
                        :zh-CN "登录"}
   :login.username     {:en-US "Username"
                        :zh-CN "账号"}
   :login.password     {:en-US "Password"
                        :zh-CN "密码"}
   :login.login-button {:en-US "LOGIN"
                        :zh-CN "登录"}
   :login.trouble      {:en-US "Trouble logging in? See provided documentation about account management or contact your system administrator.123"
                        :zh-CN "发现登录问题吗？查看账户管理的文档，后者联系您的系统管理员。"}
   :login.oidc-button  {:en-US "OIDC LOGIN"
                        :zh-CN "OIDC登录"}
   ;;Credentials Page 
   :credentials.title       {:en-US "CREDENTIALS MANAGEMENT"
                             :zh-CN "凭证管理"}
   :credentials.tenant.add  {:en-US "ADD NEW CREDENTIALS"
                             :zh-CN "加入凭证"}
   :credentials.tenant.number  {:en-US "Number of Credentials: "
                                :zh-CN "凭证多少："}
   :credentials.tenant.key  {:en-US "Api Key"
                             :zh-CN "API密钥"}
   :credentials.key.aria {:en-US "Show/Hide Api Key Details"
                          :zh-CN "显示/隐藏API密钥"}
   :credentials.key.permissions {:en-US "Permissions "
                                 :zh-CN "权限"}
   :credentials.key.secret {:en-US "API Key Secret"
                            :zh-CN "API秘密"}
   :credentials.key.hide {:en-US "Hide"
                          :zh-CN "隐藏"}
   :credentials.key.show {:en-US "Show Secret Key"
                          :zh-CN "显示秘密"}
   :credentials.key.permissions.save {:en-US "Save"
                                      :zh-CN "保存"}
   :credentials.key.permissions.cancel {:en-US "Cancel"
                                        :zh-CN "取消"}
   :credentials.key.edit {:en-US "Edit"
                          :zh-CN "编辑"}
   :credentials.key.delete {:en-US "Delete"
                            :zh-CN "删除"}
   :credentials.key.delete.confirm {:en-US "Are you sure?"
                                    :zh-CN "确认？"}
   ;;Accounts Page
   :accounts.title {:en-US "Account Management"
                    :zh-CN "账户管理"}
   :accounts.table-header {:en-US "Account"
                           :zh-CN "账户"}
   :accounts.delete {:en-US "Delete"
                     :zh-CN "删除"}
   :accounts.delete.confirm {:en-US "Are you sure?"
                             :zh-CN "确认？"}
   :accounts.password.update {:en-US "Update Password"
                              :zh-CN "编辑密码"}
   :accounts.new.subtitle {:en-US "Create New Account"
                           :zh-CN "创建新账户"}
   :accounts.new.username {:en-US "Username:"
                           :zh-CN "账号："}
   :accounts.new.username.note {:en-US "Username must be %d or more alphanumeric characters"
                                :zh-CN "账号需要有%d以上字符"}
   :accounts.new.password {:en-US "Password:"
                           :zh-CN "密码："}
   :accounts.new.password.note {:en-US "Password must be %d or more characters and contain uppercase, lowercase, numbers, and special characters (%s). Be sure to note or copy the new password as it will not be accessible after creation."
                                :zh-CN "密码需要有%d以上字号而包含大写、小写、数字、特别字符（%s）"}
   :accounts.new.password.hide {:en-US "Hide"
                                :zh-CN "隐藏"}
   :accounts.new.password.show {:en-US "Show"
                                :zh-CN "显示"}
   :accounts.new.password.copy {:en-US "Copy"
                                :zh-CN "复制"}
   :accounts.new.password.generate {:en-US "Generate Password"
                                    :zh-CN "生成密码"}
   :accounts.new {:en-US "CREATE ACCOUNT"
                  :zh-CN "创建账户"}
   ;;Browser
   :browser.title {:en-US "DATA BROWSER"
                   :zh-CN "数据浏览"}
   :browser.credentials {:en-US "Credentials to Use:"
                         :zh-CN "使用的凭证："}
   :browser.query {:en-US "Current Query:"
                   :zh-CN "现在的查询："}
   :browser.key-note {:en-US "Please Choose an API Key Above to Browse LRS Data"
                      :zh-CN "使用为LRS数据监视的API密钥"}
   ;;Monitor
   :monitor.title {:en-US "LRS Monitor"
                   :zh-CN "LRS监视"}
   :monitor.no-data {:en-US "No Statement Data"
                     :zh-CN "没有句子数据"}
   :monitor.refresh {:en-US "REFRESH"
                     :zh-CN "刷新"}

   :monitor.statements.title {:en-US "STATEMENTS"
                              :zh-CN "句子"}
   :monitor.actors.title {:en-US "ACTORS"
                          :zh-CN "主语"}
   :monitor.last-statement.title {:en-us "LAST STATEMENT AT"
                                  :zh-CN "前句子在"}
   :monitor.timeline.title {:en-US "TIMELINE"
                            :zh-CN "时间线"}
   :monitor.timeline.unit {:en-US "Time Unit"
                           :zh-CN "时间单位"}
   :monitor.timeline.since {:en-US "Since"
                            :zh-CN "从"}
   :monitor.timeline.until {:en-US "Until"
                            :zh-CN "至"}

   :monitor.platform.title {:en-US "PLATFORM"
                            :zh-CN "平台"}
   :monitor.platform.tooltip {:en-US "This metric requires proper use of the “context.platform” field in associated xAPI Statements. If you do not see your connected system represented here, it is possible that it is posting statements that are not using this field."
                              :zh-CN "句子之一"}

   ;;Data Management
   :datamgmt.title {:en-US "DATA MANAGEMENT"
                    :zh-CN "数据管理"}
   :datamgmt.delete.title {:en-US "Delete Actor"
                           :zh-CN "删除主语"}
   :datamgmt.delete.button {:en-US "DELETE"
                            :zh-CN "删除"}

   ;;Reactions
   :reactions.title {:en-us "REACTIONS"
                     :zh-CN "反应"}
   :reactions.title.beta {:en-US " (Beta)"
                          :zh-CN " （测试版）"}
   :reactions.add {:en-US "ADD NEW REACTION"
                   :zh-CN "加入反应"}
   :reactions.col.title {:en-US "Title"
                         :zh-CN "标题"}
   :reactions.col.conds {:en-US "# of Conditions"
                         :zh-CN "条件多少"}
   :reactions.col.created {:en-US "Created"
                           :zh-CN "创建时间"}
   :reactions.col.modified {:en-US "Modified"
                            :zh-CN "改变时间"}
   :reactions.col.status {:en-US "Status"
                          :zh-CN "状态"}
   :reactions.col.error {:en-US "Error"
                         :zh-CN "错误"}
   :reactions.col.action {:en-US "Action"
                          :zh-CN "行动"}
   :reactions.action.edit {:en-US "Edit"
                           :zh-CN "编辑"}
   :reactions.action.delete {:en-US "Delete"
                             :zh-CN "删除"}

   :reactions.new.title {:en-US "New Reaction"
                         :zh-CN "新反应"}
   :reactions.edit.title {:en-US "Edit Reaction"
                          :zh-CN "编辑反应"}
   :reactions.focus.title {:en-US "Reaction Details"
                           :zh-CN "反应详情"}

   :reactions.details.created {:en-US "Created"
                               :zh-CN "创建"}
   :reactions.details.modified {:en-US "Modified"
                                :zh-CN "改变"}
   :reactions.details.error {:en-US "Error"
                             :zh-CN "错误"}
   :reactions.details.title {:en-US "Error"
                             :zh-CN "错误"}
   :reactions.details.id {:en-US "ID"
                          :zh-CN "识别码"}
   :reactions.details.status {:en-US "Status"
                              :zh-CN "状态"}
   :reactions.details.ruleset {:en-US "Ruleset"
                               :zh-CN "规则集"}
   :reactions.details.ruleset.conditions {:en-US "Conditions"
                                          :zh-CN "条件"}
   :reactions.details.conditions.delete-button {:en-US "Delete %s "
                                                :zh-CN "删除%s"}
   :reactions.details.conditions.add-clause {:en-US "Add %sclause to %s"
                                             :zh-CN "家%s分句在%s上"}
   :reactions.details.conditions.add-condition {:en-US "Add New Condition "
                                                :zh-CN "加入条件"}
   :reactions.details.conditions.and-instructions {:en-US "This `Boolean AND` clause must contain at least one sub-clause. Please add either Statement Criteria or a nested Boolean operation below."
                                                   :zh-CN "`Boolean AND`分句起码有一个从句。加入句子标准或者嵌套布尔运算。"}
   :reactions.details.conditions.or-instructions {:en-US "This `Boolean OR` clause must contain at least one sub-clause. Please add either Statement Criteria or a nested Boolean operation below."
                                                  :zh-CN "`Boolean OR`分句起码有一个从句。加入句子标准或者嵌套布尔运算。"}
   :reactions.details.conditions.not-instructions {:en-US "This `Boolean NOT` clause must contain at least one sub-clause. Please add either Statement Criteria or a nested Boolean operation below."
                                                   :zh-CN "`Boolean NOT`分句起码有一个从句。加入句子标准或者嵌套布尔运算。"}
   :reactions.details.conditions.statement-path {:en-US "Statement Path"
                                                 :zh-CN "句子路径"}
   :reactions.details.conditions.operation {:en-US "Operation"
                                            :zh-CN "运算"}
   :reactions.details.conditions.value {:en-US "Value"
                                        :zh-CN "价值"}
   :reactions.details.conditions.reference {:en-US "Reference"
                                            :zh-CN "参考"}

   :reactions.identity-paths {:en-US "Identity Paths (Advanced)"
                              :zh-CN "识别码路径"}
   :reactions.identity-paths.add {:en-US "Add New Identity Path "
                                  :zh-CN "加入识别码路径"}
   :reactions.buttons.edit {:en-US "EDIT"
                            :zh-CN "编辑"}
   :reactions.buttons.save {:en-US "SAVE"
                            :zh-CN "保存"}
   :reactions.buttons.create {:en-US "CREATE"
                              :zh-CN "创建"}
   :reactions.buttons.back {:en-US "BACK"
                            :zh-CN "返回"}
   :reactions.buttons.revert {:en-US "REVERT CHANGES"
                              :zh-CN "取消变化"}

   :reactions.template.title {:en-US "Template"
                              :zh-CN "模板"}
   :reactions.template.template-json {:en-US "Template JSON"
                                      :zh-CN "模板JSON"}
   :reactions.template.dynamic {:en-US "Dynamic Variables"
                                :zh-CN "动态变量"}
   :reactions.template.dynamic.instruction1 {:en-US "Reactions templates can be made dynamic by the use of injectable variables. These variables must come from a statement matching one of the conditions above."
                                             :zh-CN "可注入变量使反应模板动态化。变量必须从匹配条件的句子来的。"}
   :reactions.template.dynamic.instruction2 {:en-US "Variables use a syntax with a JSON object containing a key of `$templatePath` and an array of the path in the statement of the value to extract, starting with which condition. For instance:"
                                             :zh-CN "变量的句法是个JSON对象。对象键是`$templatePath`，对象值是路径数组。比如说："}
   :reactions.template.dynamic.instruction3 {:en-US "The above example will retrieve (if it exists) the value of `$.result.success` from the statement matching `condition_XYZ` if the Reaction is successfully fired."
                                             :zh-CN "如果反应成功激活，前例子会从匹配`condition_XYZ`的句子取回`$.result.success`的价值。"}
   :reactions.template.dynamic.step1 {:en-US "Step 1: Select Condition"
                                      :zh-CN "选者条件"}
   :reactions.template.dynamic.step2 {:en-US "Step 2: Select Path"
                                      :zh-CN "选者路径"}
   :reactions.template.dynamic.step3 {:en-US "Step 3: Copy Variable Code"
                                      :zh-CN "复制变量代码"}
   :reactions.template.dynamic.step3-text {:en-US "Use the copy button to get the variable declaration and paste it where you want it in the statement Template. You can also build these declarations yourself using the syntax shown."
                                           :zh-CN "复制变量或者自己写变量。"}

   :reactions.errors.incomplete-path {:en-US "Incomplete path."
                                      :zh-CN "路径没完成。"}
   :reactions.errors.like-string {:en-US "The 'like' op only supports string values."
                                  :zh-CN "'like'运算只能有字串。"}
   :reactions.errors.invalid {:en-US "Reaction is invalid see below."
                              :zh-CN "反应有错误"}
   :reactions.errors.one-condition {:en-US "Ruleset must specify at least one condition."
                                    :zh-CN "规则集起码有一个条件。"}
   :reactions.errors.one-clause {:en-US "Condition must have at least one clause."
                                 :zh-CN "条件起码有一个分句。"}

   ;;Account Management
   :account-mgmt.update-password.title {:en-US "Update Password"
                                        :zh-CN "调整密码"}
   :account-mgmt.update-password.cancel {:en-US "CANCEL"
                                         :zh-CN "取消"}
   :account-mgmt.update-password.update {:en-US "UPDATE"
                                         :zh-CN "编辑"}
   :account-mgmt.update-password.guidelines {:en-US "New password must be different from old password and be %d or more characters and contain uppercase, lowercase, numbers, and special characters (%s). Be sure to note or copy the new password as it will not be accessible after creation."
                                             :zh-CN "新密码必须比久密码不养。新密码也需要有%d以上字号而包含大写、小写、数字、特别字符（%s）"}
   :account-mgmt.update-password.password.show {:en-US "Show"
                                                :zh-CN "显示"}
   :account-mgmt.update-password.password.hide {:en-US "Hide"
                                                :zh-CN "隐藏"}
   :account-mgmt.update-password.password.old {:en-US "Old Password"
                                               :zh-CN "老密码"}
   :account-mgmt.update-password.password.new {:en-US "New Password"
                                               :zh-CN "新密码"}
   :account-mgmt.update-password.password.copy {:en-US "Copy"
                                                :zh-CN "复制"}
   :account-mgmt.update-password.password.generate {:en-US "Generate Password"
                                                    :zh-CN "生成密码"}
   

   ;;Tooltips
   :tooltip.reactions.title {:en-US "Reactions is a new functionality for SQL LRS that allows for the generation of custom xAPI statements triggered by other statements posted to the LRS. An administrator can configure rulesets that match one or more incoming xAPI statement(s), based on conditions, and generate a custom statement which is added to the LRS. -- This can be used for statement transformation (e.g. integration with systems expecting a certain statement format the provider does not make) and statement aggregation (e.g. generate summary statements or assertions about groups of statements)."
                             :zh-CN "句子之二"}
   :tooltip.reactions.statement-path {:en-US "Path is how you identify which part of a matching statement you are comparing. For instance `$.object.id` means we are comparing the statement object's id field. These are limited to xAPI specification except for extensions where you can write in the variable part of the path directly."
                                      :zh-CN "句子之三"}
   :tooltip.reactions.operation {:en-US "Operation represents the method with which to compare the values. For instance `Equals` means the value at the statement path above must exactly match the Value or Reference below."
                                 :zh-CN "句子之四"}
   :tooltip.reactions.comparator {:en-US "This field determines what kind of data we are comparing the statement field to. It can either be a literal `Value` manually entered here or a `Reference` to a field in another matching condition to produce interdependent conditions. For `Value` entries, the data type may be automatically assigned based on xAPI Specification."
                                  :zh-CN "句子之五"}
   :tooltip.reactions.condition-title {:en-US "This is the title of the Condition. It is given a generated name on creation but can be customized. It may be used in `Logic Clauses` to reference between Conditions."
                                       :zh-CN "句子之六"}
   :tooltip.reactions.identity-path {:en-US "USE WITH CAUTION. Identity Paths are a method of grouping statements for which you are attempting to match conditions. Typically, Reactions may revolve around actor, e.g. `$.actor.mbox` or `$.actor.account.name` which is equivalent to saying \"For a given Actor, look for statements that match the Conditions above\". This is what the default is set to. Alternative approaches to Identity Path may be used by modifying this section, for instance `$.context.registration` to group statements by learning session."
                                     :zh-CN "句子之七"}
   :tooltip.reactions.ruleset.conditions {:en-US "This part of a ruleset controls the criteria for which statements match in a Reaction. An easy way to think about it is each `Condition` should match one expected xAPI Statement. Each condition can have as much criteria and logic as is required to identify the correct kind of statement."
                                          :zh-CN "句子之八"}
   :tooltip.reactions.template {:en-US "This is where you design the custom statement to be generated and stored in the event of matching statements for this Reaction. Variables from the statements matching individual conditions can be injected into the custom statement."
                                :zh-CN "句子之九"}
   :tooltip.reactions.reaction-title {:en-US "This is the title of the Reaction you are creating/editing. It has no effect on Reaction functionality."
                                      :zh-CN "句子之十"}
   :tooltip.reactions.reaction-id {:en-US "This is the system ID of the Reaction you are creating/editing. It has no effect on Reaction functionality, but may be useful for error tracing."
                                   :zh-CN "句子之十一"}
   :tooltip.reactions.reaction-status {:en-US "This field sets whether the Reaction is turned on or not. If set to Active it will generate statements based on the rulesets provided."
                                       :zh-CN "句子之十二"}

   :tooltip.reactions.template.dynamic {:en-US "You can use this tool to create variable declarations referencing the statements which match the condition(s) above, and then use them in your template to create a dynamic xAPI Reaction Statement."
                                        :zh-CN "句子之十三"}
   :tooltip.reactions.template.json {:en-US "The following is the JSON template which will be used to create the Reaction statement if the above conditions are met. You can customize this statement template to produce any valid xAPI Statement. Invalid xAPI will cause a Reaction error upon firing."
                                     :zh-CN "句子之十四"}

   :tooltip.reactions.clause-type.and   {:en-US "AND Clause: All sub-clauses must be true for the statement to match this clause. Requires at least 1 sub-clause."
                                         :zh-CN "句子之十五"}
   :tooltip.reactions.clause-type.or    {:en-US "OR Clause: One of the sub-clauses must be true for the statement to match this clause. Requires at leat 1 sub-clause."
                                         :zh-CN "句子之十六"}
   :tooltip.reactions.clause-type.not   {:en-US "NOT Clause: The single sub-clause must return false for the statement to match this clause. Requires one sub-clause."
                                         :zh-CN "句子之十七"}
   :tooltip.reactions.clause-type.logic {:en-US "Statement Criteria Clause: The comparison detailed in this clause must resolve to true for the statement to match this clause."
                                         :zh-CN "句子之十八"}

   ;;Notifications
   :notification.credentials.key-copied {:en-US "Copied API Key!"
                                         :zh-CN "复制了API密钥！"}
   :notification.credentials.secret-copied {:en-US "Copied Secret Key!"
                                            :zh-CN "复制了API秘密！"}
   :notification.accounts.password-copied {:en-US "Copied New Password!"
                                           :zh-CN "复制了新密码！"}
   :notification.reactions.copied-template-var {:en-US "Copied Template Variable to Clipboard!"
                                                :zh-CN "复制了新模版变量！"}
   :notification.account-mgmt.copied-password {:en-US "Copied New Password!"
                                               :zh-CN "复制了新密码！"}})
