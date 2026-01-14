(ns com.yetanalytics.lrs-admin-ui.language)

(def language
  {;;Footer   
   :footer.attribution     {:en-US "Yet Analytics Inc."}
   :footer.license         {:en-US "Licensed under the Apache 2.0 License"}
   :footer.contribute      {:en-US "Contribute on "}
   :footer.contact-note    {:en-US "Contact us to learn about Enterprise Support options."}
   :footer.nav.logout      {:en-US "LOGOUT"}
   :footer.nav.browser     {:en-US "BROWSER"}
   :footer.nav.accounts    {:en-US "ACCOUNTS"}
   :footer.nav.credentials {:en-US "CREDENTIALS"}
   ;;Header
   :header.welcome         {:en-US "Welcome, "}
   :header.logout          {:en-US "Logout"}
   :header.nav.credentials {:en-US "Credentials Management"}
   :header.nav.accounts    {:en-US "Account Management"}
   :header.nav.browser     {:en-US "Statement Browser"}
   :header.nav.monitor     {:en-US "LRS Monitor"}
   :header.nav.data        {:en-US "Data Management"}
   :header.nav.reactions   {:en-US "Reactions"}
   ;;404 Page
   :not-found.title {:en-US "404 Not Found"}
   :not-found.body  {:en-US "The page that you are looking for cannot be found."}
   ;;Login Page
   :login.title        {:en-US "LOGIN"}
   :login.username     {:en-US "Username"}
   :login.password     {:en-US "Password"}
   :login.login-button {:en-US "LOGIN"}
   :login.trouble      {:en-US "Trouble logging in? See provided documentation about account management or contact your system administrator."}
   :login.oidc-button  {:en-US "OIDC LOGIN"}
   ;;Credentials Page 
   :credentials.title       {:en-US "CREDENTIALS MANAGEMENT"}
   :credentials.tenant.add  {:en-US "ADD NEW CREDENTIALS"}
   :credentials.tenant.number  {:en-US "Number of Credentials: "}
   :credentials.tenant.key  {:en-US "Api Key"}
   :credentials.key.aria {:en-US "Show/Hide Api Key Details"}
   :credentials.key.label {:en-US "Label:"}
   :credentials.key.permissions {:en-US "Permissions:"}
   :credentials.key.secret {:en-US "API Key Secret"}
   :credentials.key.hide {:en-US "Hide"}
   :credentials.key.show {:en-US "Show Secret Key"}
   :credentials.key.permissions.save {:en-US "Save"}
   :credentials.key.permissions.cancel {:en-US "Cancel"}
   :credentials.key.actions {:en-US "Actions"}
   :credentials.key.edit-credential {:en-US "Edit Credential"}
   :credentials.key.edit {:en-US "Edit"}
   :credentials.key.delete {:en-US "Delete"}
   :credentials.key.delete.confirm {:en-US "Are you sure?"}
   ;;Accounts Page
   :accounts.title {:en-US "Account Management"}
   :accounts.table-header {:en-US "Account"}
   :accounts.delete {:en-US "Delete"}
   :accounts.delete.confirm {:en-US "Are you sure?"}
   :accounts.password.update {:en-US "Update Password"}
   :accounts.new.subtitle {:en-US "Create New Account"}
   :accounts.new.username {:en-US "Username:"}
   :accounts.new.username.note {:en-US "Username must be %d or more alphanumeric characters"}
   :accounts.new.password {:en-US "Password:"}
   :accounts.new.password.note {:en-US "Password must be %d or more characters and contain uppercase, lowercase, numbers, and special characters (%s). Be sure to note or copy the new password as it will not be accessible after creation."}
   :accounts.new.password.hide {:en-US "Hide"}
   :accounts.new.password.show {:en-US "Show"}
   :accounts.new.password.copy {:en-US "Copy"}
   :accounts.new.password.generate {:en-US "Generate Password"}
   :accounts.new {:en-US "CREATE ACCOUNT"}
   ;;Browser
   :browser.title {:en-US "DATA BROWSER"}
   :browser.credentials {:en-US "Credentials to Use:"}
   :browser.query {:en-US "Current Query:"}
   :browser.key-note {:en-US "Please Choose an API Key Above to Browse LRS Data"}
   :browser.refresh {:en-US "Refresh"}
   :browser.filters {:en-US "Filters:"}
   ;;CSV Download 
   :csv.property-paths.title {:en-US "CSV Columns"}
   :csv.property-paths.add {:en-US "Add CSV Column "}
   :csv.property-paths.instructions {:en-US "To export your data, select the xAPI statement property paths as CSV columns below."}
   :csv.filters {:en-US "Filters:"}
   ;;JSON File Upload
   :statements.file-upload.title {:en-US "Upload Statements"}
   :statements.file-upload.button {:en-US "Upload"}
   :statements.file-upload.choose-file-button {:en-US "Choose file"}
   :statements.file-upload.XAPI-version {:en-US "XAPI Version"}
   :statements.file-upload.key-note {:en-US "Please Choose an API Key Above to Upload Statements File"}
   ;;Monitor
   :monitor.title {:en-US "LRS Monitor"}
   :monitor.no-data {:en-US "No Statement Data"}
   :monitor.refresh {:en-US "REFRESH"}

   :monitor.statements.title {:en-US "STATEMENTS"}
   :monitor.actors.title {:en-US "ACTORS"}
   :monitor.last-statement.title {:en-us "LAST STATEMENT AT"}
   :monitor.timeline.title {:en-US "TIMELINE"}
   :monitor.timeline.unit {:en-US "Time Unit"}
   :monitor.timeline.since {:en-US "Since"}
   :monitor.timeline.until {:en-US "Until"}

   :monitor.platform.title {:en-US "PLATFORM"}
   :monitor.platform.tooltip {:en-US "This metric requires proper use of the “context.platform” field in associated xAPI Statements. If you do not see your connected system represented here, it is possible that it is posting statements that are not using this field."}

   ;;Data Management
   :datamgmt.title {:en-US "DATA MANAGEMENT"}
   :datamgmt.delete.title {:en-US "Delete Actor"}
   :datamgmt.delete.button {:en-US "DELETE"}
   :datamgmt.download.title {:en-US "Download CSV"}
   :datamgmt.download.button {:en-US "DOWNLOAD"}

   ;;Reactions
   :reactions.title {:en-us "REACTIONS"}
   :reactions.title.beta {:en-US " (Beta)"}
   :reactions.add {:en-US "ADD NEW REACTION"}
   :reactions.download-all {:en-US "DOWNLOAD ALL"} ; Note: Currently unimplemented
   :reactions.col.title {:en-US "Title"}
   :reactions.col.conds {:en-US "# of Conditions"}
   :reactions.col.created {:en-US "Created"}
   :reactions.col.modified {:en-US "Modified"}
   :reactions.col.status {:en-US "Status"}
   :reactions.col.error {:en-US "Error"}
   :reactions.col.action {:en-US "Action"}
   :reactions.action.edit {:en-US "Edit"}
   :reactions.action.delete {:en-US "Delete"}

   :reactions.not-found {:en-US "The Reaction cannot be found."}

   :reactions.new.title {:en-US "New Reaction"}
   :reactions.edit.title {:en-US "Edit Reaction"}
   :reactions.focus.title {:en-US "Reaction Details"}

   :reactions.details.created {:en-US "Created"}
   :reactions.details.modified {:en-US "Modified"}
   :reactions.details.error {:en-US "Error"}
   :reactions.details.title {:en-US "Title"}
   :reactions.details.id {:en-US "ID"}
   :reactions.details.status {:en-US "Status"}
   :reactions.details.ruleset.conditions {:en-US "Ruleset Conditions"}
   :reactions.details.conditions.delete-button {:en-US "Delete %s "}
   :reactions.details.conditions.add-clause {:en-US "Add %sclause to %s"}
   :reactions.details.conditions.add-condition {:en-US "Add New Condition "}
   :reactions.details.conditions.and-instructions {:en-US "This `Boolean AND` clause must contain at least one sub-clause. Please add either Statement Criteria or a nested Boolean operation below."}
   :reactions.details.conditions.or-instructions {:en-US "This `Boolean OR` clause must contain at least one sub-clause. Please add either Statement Criteria or a nested Boolean operation below."}
   :reactions.details.conditions.not-instructions {:en-US "This `Boolean NOT` clause must contain at least one sub-clause. Please add either Statement Criteria or a nested Boolean operation below."}
   :reactions.details.conditions.statement-path {:en-US "Statement Path"}
   :reactions.details.conditions.operation {:en-US "Operation"}
   :reactions.details.conditions.value {:en-US "Value"}
   :reactions.details.conditions.reference {:en-US "Reference"}

   :reactions.identity-paths {:en-US "Ruleset Identity Paths (Advanced)"}
   :reactions.identity-paths.add {:en-US "Add New Identity Path "}
   :reactions.buttons.edit {:en-US "EDIT"}
   :reactions.buttons.save {:en-US "SAVE"}
   :reactions.buttons.create {:en-US "CREATE"}
   :reactions.buttons.back {:en-US "BACK"}
   :reactions.buttons.revert {:en-US "REVERT CHANGES"}
   :reactions.buttons.download {:en-US "DOWNLOAD"}
   :reactions.buttons.upload {:en-US "UPLOAD"}

   :reactions.template.title {:en-US "Ruleset Template"}
   :reactions.template.template-json {:en-US "Template JSON"}
   :reactions.template.dynamic {:en-US "Dynamic Variables"}
   :reactions.template.dynamic.instruction1 {:en-US "Reactions templates can be made dynamic by the use of injectable variables. These variables must come from a statement matching one of the conditions above."}
   :reactions.template.dynamic.instruction2 {:en-US "Variables use a syntax with a JSON object containing a key of `$templatePath` and an array of the path in the statement of the value to extract, starting with which condition. For instance:"}
   :reactions.template.dynamic.instruction3 {:en-US "The above example will retrieve (if it exists) the value of `$.result.success` from the statement matching `condition_XYZ` if the Reaction is successfully fired."}
   :reactions.template.dynamic.step1 {:en-US "Step 1: Select Condition"}
   :reactions.template.dynamic.step2 {:en-US "Step 2: Select Path"}
   :reactions.template.dynamic.step3 {:en-US "Step 3: Copy Variable Code"}
   :reactions.template.dynamic.step3-text {:en-US "Use the copy button to get the variable declaration and paste it where you want it in the statement Template. You can also build these declarations yourself using the syntax shown."}

   :reactions.errors.incomplete-path {:en-US "Incomplete path."}
   :reactions.errors.like-string {:en-US "The 'like' op only supports string values."}
   :reactions.errors.invalid {:en-US "Reaction is invalid see below."}
   :reactions.errors.one-condition {:en-US "Ruleset must specify at least one condition."}
   :reactions.errors.dupe-condition-names {:en-US "Ruleset cannot have duplicate condition names."}
   :reactions.errors.invalid-condition-name {:en-US "Condition name must not include spaces or `/` characters, or start with `@` symbols."}
   :reactions.errors.one-clause {:en-US "Condition must have at least one clause."}

   ;;Account Management
   :account-mgmt.update-password.title {:en-US "Update Password"}
   :account-mgmt.update-password.cancel {:en-US "CANCEL"}
   :account-mgmt.update-password.update {:en-US "UPDATE"}
   :account-mgmt.update-password.guidelines {:en-US "New password must be different from old password and be %d or more characters and contain uppercase, lowercase, numbers, and special characters (%s). Be sure to note or copy the new password as it will not be accessible after creation."}
   :account-mgmt.update-password.password.show {:en-US "Show"}
   :account-mgmt.update-password.password.hide {:en-US "Hide"}
   :account-mgmt.update-password.password.old {:en-US "Old Password"}
   :account-mgmt.update-password.password.new {:en-US "New Password"}
   :account-mgmt.update-password.password.copy {:en-US "Copy"}
   :account-mgmt.update-password.password.generate {:en-US "Generate Password"}


   ;;Tooltips
   :tooltip.reactions.title {:en-US "Reactions is a new functionality for SQL LRS that allows for the generation of custom xAPI statements triggered by other statements posted to the LRS. An administrator can configure rulesets that match one or more incoming xAPI statement(s), based on conditions, and generate a custom statement which is added to the LRS. -- This can be used for statement transformation (e.g. integration with systems expecting a certain statement format the provider does not make) and statement aggregation (e.g. generate summary statements or assertions about groups of statements)."}
   :tooltip.reactions.statement-path {:en-US "Path is how you identify which part of a matching statement you are comparing. For instance `$.object.id` means we are comparing the statement object's id field. These are limited to xAPI specification except for extensions where you can write in the variable part of the path directly."}
   :tooltip.reactions.operation {:en-US "Operation represents the method with which to compare the values. For instance `Equals` means the value at the statement path above must exactly match the Value or Reference below."}
   :tooltip.reactions.comparator {:en-US "This field determines what kind of data we are comparing the statement field to. It can either be a literal `Value` manually entered here or a `Reference` to a field in another matching condition to produce interdependent conditions. For `Value` entries, the data type may be automatically assigned based on xAPI Specification."}
   :tooltip.reactions.condition-title {:en-US "This is the title of the Condition. It is given a generated name on creation but can be customized. It may be used in `Logic Clauses` to reference between Conditions."}
   :tooltip.reactions.identity-path {:en-US "USE WITH CAUTION. Identity Paths are a method of grouping statements for which you are attempting to match conditions. Typically, Reactions may revolve around actor, e.g. `$.actor.mbox` or `$.actor.account.name` which is equivalent to saying \"For a given Actor, look for statements that match the Conditions above\". This is what the default is set to. Alternative approaches to Identity Path may be used by modifying this section, for instance `$.context.registration` to group statements by learning session."}
   :tooltip.reactions.ruleset.conditions {:en-US "This part of a ruleset controls the criteria for which statements match in a Reaction. An easy way to think about it is each `Condition` should match one expected xAPI Statement. Each condition can have as much criteria and logic as is required to identify the correct kind of statement."}
   :tooltip.reactions.template {:en-US "This is where you design the custom statement to be generated and stored in the event of matching statements for this Reaction. Variables from the statements matching individual conditions can be injected into the custom statement."}
   :tooltip.reactions.reaction-title {:en-US "This is the title of the Reaction you are creating/editing. It has no effect on Reaction functionality."}
   :tooltip.reactions.reaction-id {:en-US "This is the system ID of the Reaction you are creating/editing. It has no effect on Reaction functionality, but may be useful for error tracing."}
   :tooltip.reactions.reaction-status {:en-US "This field sets whether the Reaction is turned on or not. If set to Active it will generate statements based on the rulesets provided."}

   :tooltip.reactions.template.dynamic {:en-US "You can use this tool to create variable declarations referencing the statements which match the condition(s) above, and then use them in your template to create a dynamic xAPI Reaction Statement."}
   :tooltip.reactions.template.json {:en-US "The following is the JSON template which will be used to create the Reaction statement if the above conditions are met. You can customize this statement template to produce any valid xAPI Statement. Invalid xAPI will cause a Reaction error upon firing."}

   :tooltip.reactions.clause-type.and   {:en-US "AND Clause: All sub-clauses must be true for the statement to match this clause. Requires at least 1 sub-clause."}
   :tooltip.reactions.clause-type.or    {:en-US "OR Clause: One of the sub-clauses must be true for the statement to match this clause. Requires at leat 1 sub-clause."}
   :tooltip.reactions.clause-type.not   {:en-US "NOT Clause: The single sub-clause must return false for the statement to match this clause. Requires one sub-clause."}
   :tooltip.reactions.clause-type.logic {:en-US "Statement Criteria Clause: The comparison detailed in this clause must resolve to true for the statement to match this clause."}

   ;;Notifications
   :notification.credentials.key-copied {:en-US "Copied API Key!"}
   :notification.credentials.secret-copied {:en-US "Copied Secret Key!"}
   :notification.accounts.password-copied {:en-US "Copied New Password!"}
   :notification.reactions.copied-template-var {:en-US "Copied Template Variable to Clipboard!"}
   :notification.account-mgmt.copied-password {:en-US "Copied New Password!"}

   ;;Form Components
   :form.search-or-add {:en-US "Search or Add"}
   :form.add {:en-US "Add"}})
