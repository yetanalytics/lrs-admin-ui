(ns com.yetanalytics.lrs-admin-ui.language)

(def language
  {;;Footer   
   :footer.attribution     {:en-US "Yet Analytics Inc.##"}
   :footer.license         {:en-US "Licensed under the Apache 2.0 License##"}
   :footer.contribute      {:en-US "Contribute on ##"}
   :footer.contact-note    {:en-US "Contact us to learn about Enterprise Support options.##"}
   :footer.nav.logout      {:en-US "LOGOUT##"}
   :footer.nav.browser     {:en-US "BROWSER##"}
   :footer.nav.accounts    {:en-US "ACCOUNTS##"}
   :footer.nav.credentials {:en-US "CREDENTIALS##"}
   ;;Header
   :header.welcome         {:en-US "Welcome, ##"}
   :header.logout          {:en-US "Logout##"}
   :header.nav.credentials {:en-US "Credentials Management##"}
   :header.nav.accounts    {:en-US "Account Management##"}
   :header.nav.browser     {:en-US "Statement Browser##"}
   :header.nav.monitor     {:en-US "LRS Monitor##"}
   :header.nav.data        {:en-US "Data Management##"}
   :header.nav.reactions   {:en-US "Reactions##"}
   ;;Login Page
   :login.title        {:en-US "LOGIN##"}
   :login.username     {:en-US "Username##"}
   :login.password     {:en-US "Password##"}
   :login.login-button {:en-US "LOGIN##"}
   :login.trouble      {:en-US "##Trouble logging in? See provided documentation about account management or contact your system administrator.##123"}
   :login.oidc-button  {:en-US "OIDC LOGIN##"}
   ;;Credentials Page 
   :credentials.title       {:en-US "CREDENTIALS MANAGEMENT##"}
   :credentials.tenant.add  {:en-US "ADD NEW CREDENTIALS##"}
   :credentials.tenant.number  {:en-US "Number of Credentials: ##"}
   :credentials.tenant.key  {:en-US "Api Key##"}
   :credentials.key.aria {:en-US "Show/Hide Api Key Details##"}
   :credentials.key.permissions {:en-US "Permissions ##"}
   :credentials.key.secret {:en-US "API Key Secret##"}
   :credentials.key.hide {:en-US "Hide##"}
   :credentials.key.show {:en-US "Show Secret Key##"}
   :credentials.key.permissions.save {:en-US "Save##"}
   :credentials.key.permissions.cancel {:en-US "Cancel##"}
   :credentials.key.edit {:en-US "Edit##"}
   :credentials.key.delete {:en-US "Delete##"}
   :credentials.key.delete.confirm {:en-US "Are you sure?##"}
   ;;Accounts Page
   :accounts.delete {:en-US "Delete##"}
   :accounts.delete.confirm {:en-US "Are you sure?##"}
   :accounts.password.update {:en-US "Update Password##"}
   :accounts.new.username {:en-US "Username:##"}
   :accounts.new.username.note {:en-US "Username must be %d or more alphanumeric characters##"}
   :accounts.new.password {:en-US "Password:##"}
   :accounts.new.password.note {:en-US "Password must be %d or more characters and contain uppercase, lowercase, numbers, and special characters (%s). Be sure to note or copy the new password as it will not be accessible after creation.##"}
   :accounts.new.password.hide {:en-US "Hide##"}
   :accounts.new.password.show {:en-US "Show##"}
   :accounts.new.password.copy {:en-US "Copy##"}
   :accounts.new.password.generate {:en-US "Generate Password##"}
   :accounts.new {:en-US "CREATE ACCOUNT##"}
   ;;Browser
   :browser.title {:en-US "DATA BROWSER##"}
   :browser.credentials {:en-US "Credentials to Use:##"}
   :browser.query {:en-US "Current Query:##"}
   :browser.key-note {:en-US "Please Choose an API Key Above to Browse LRS Data##"}
   ;;Monitor
   :monitor.title {:en-US "LRS Monitor##"}
   :monitor.no-data {:en-US "No Statement Data##"}
   :monitor.refresh {:en-US "REFRESH##"}

   :monitor.statements.title {:en-US "STATEMENTS##"}
   :monitor.actors.title {:en-US "ACTORS##"}
   :monitor.last-statement.title {:en-us "LAST STATEMENT AT##"}
   :monitor.timeline.title {:en-US "TIMELINE##"}
   :monitor.timeline.unit {:en-US "Time Unit##"}
   :monitor.timeline.since {:en-US "Since##"}
   :monitor.timeline.until {:en-US "Until##"}

   :monitor.platform.title {:en-US "PLATFORM##"}
   :monitor.platform.tooltip {:en-US "This metric requires proper use of the “context.platform” field in associated xAPI Statements. If you do not see your connected system represented here, it is possible that it is posting statements that are not using this field.##"}

   ;;Data Management
   :datamgmt.title {:en-US "DATA MANAGEMENT##"}
   :datamgmt.delete.title {:en-US "Delete Actor##"}
   :datamgmt.delete.button {:en-US "DELETE##"}

   ;;Reactions
   :reactions.title {:en-us "REACTIONS##"}
   :reactions.title.beta {:en-US " (Beta)##"}
   :reactions.add {:en-US "ADD NEW REACTION##"}
   :reactions.col.title {:en-US "Title##"}
   :reactions.col.conds {:en-US "# of Conditions##"}
   :reactions.col.created {:en-US "Created##"}
   :reactions.col.modified {:en-US "Modified##"}
   :reactions.col.status {:en-US "Status##"}
   :reactions.col.error {:en-US "Error##"}
   :reactions.col.action {:en-US "Action##"}
   :reactions.action.edit {:en-US "Edit##"}
   :reactions.action.delete {:en-US "Delete##"}

   :reactions.new.title {:en-US "New Reaction##"}
   :reactions.edit.title {:en-US "Edit Reaction##"}
   :reactions.focus.title {:en-US "Reaction Details##"}

   :reactions.details.created {:en-US "Created##"}
   :reactions.details.modified {:en-US "Modified##"}
   :reactions.details.error {:en-US "Error##"}
   :reactions.details.title {:en-US "Error##"}
   :reactions.details.id {:en-US "ID##"}
   :reactions.details.status {:en-US "Status##"}
   :reactions.details.ruleset {:en-US "Ruleset##"}
   :reactions.details.ruleset.conditions {:en-US "Conditions##"}
   :reactions.details.conditions.delete-button {:en-US "Delete %s ##"}
   :reactions.details.conditions.add-clause {:en-US "Add %sclause to %s##"}
   :reactions.details.conditions.add-condition {:en-US "Add New Condition ##"}
   :reactions.details.conditions.and-instructions {:en-US "This `Boolean AND` clause must contain at least one sub-clause. Please add either Statement Criteria or a nested Boolean operation below.##"}
   :reactions.details.conditions.or-instructions {:en-US "This `Boolean OR` clause must contain at least one sub-clause. Please add either Statement Criteria or a nested Boolean operation below.##"}
   :reactions.details.conditions.not-instructions {:en-US "This `Boolean NOT` clause must contain at least one sub-clause. Please add either Statement Criteria or a nested Boolean operation below.##"}
   :reactions.details.conditions.statement-path {:en-US "Statement Path##"}
   :reactions.details.conditions.operation {:en-US "Operation##"}
   :reactions.details.conditions.value {:en-US "Value##"}
   :reactions.details.conditions.reference {:en-US "Reference##"}

   :reactions.identity-paths {:en-US "Identity Paths (Advanced)##"}
   :reactions.identity-paths.add {:en-US "Add New Identity Path ##"}
   :reactions.buttons.edit {:en-US "EDIT##"}
   :reactions.buttons.save {:en-US "SAVE##"}
   :reactions.buttons.create {:en-US "CREATE##"}
   :reactions.buttons.back {:en-US "BACK##"}
   :reactions.buttons.revert {:en-US "REVERT CHANGES##"}
   
   
   :reactions.errors.incomplete-path {:en-US "Incomplete path.##"}
   :reactions.errors.like-string {:en-US "The 'like' op only supports string values.##"}
   :reactions.errors.invalid {:en-US "Reaction is invalid see below.##"}
   :reactions.errors.one-condition {:en-US "Ruleset must specify at least one condition.##"}
   :reactions.errors.one-clause {:en-US "Condition must have at least one clause.##"}



   ;;Notifications
   :notification.credentials.key-copied {:en-US "Copied API Key!##"}
   :notification.credentials.secret-copied {:en-US "Copied Secret Key!##"}
   :notification.accounts.password-copied {:en-US "Copied New Password!##"}})
