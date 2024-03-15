(ns com.yetanalytics.lrs-admin-ui.language)

(def language
  {;;Footer   
   :footer.attribution     {:en-US "Yet Analytics Inc.#123"}
   :footer.license         {:en-US "Licensed under the Apache 2.0 License#123"}
   :footer.contribute      {:en-US "Contribute on #123"}
   :footer.contact-note    {:en-US "Contact us to learn about Enterprise Support options.#123"}
   :footer.nav.logout      {:en-US "LOGOUT#123"}
   :footer.nav.browser     {:en-US "BROWSER#123"}
   :footer.nav.accounts    {:en-US "ACCOUNTS#123"}
   :footer.nav.credentials {:en-US "CREDENTIALS#123"}
   ;;Header
   :header.welcome         {:en-US "Welcome, #123"}
   :header.logout          {:en-US "Logout#123"}
   :header.nav.credentials {:en-US "Credentials Management#123"}
   :header.nav.accounts    {:en-US "Account Management#123"}
   :header.nav.browser     {:en-US "Statement Browser#123"}
   :header.nav.monitor     {:en-US "LRS Monitor#123"}
   :header.nav.data        {:en-US "Data Management#123"}
   :header.nav.reactions   {:en-US "Reactions#123"}
   ;;Login Page
   :login.title        {:en-US "LOGIN#123"}
   :login.username     {:en-US "Username#123"}
   :login.password     {:en-US "Password#123"}
   :login.login-button {:en-US "LOGIN#123"}
   :login.trouble      {:en-US "#123Trouble logging in? See provided documentation about account management or contact your system administrator.#123123"}
   :login.oidc-button  {:en-US "OIDC LOGIN#123"}
   ;;Credentials Page 
   :credentials.title       {:en-US "CREDENTIALS MANAGEMENT#123"}
   :credentials.tenant.add  {:en-US "ADD NEW CREDENTIALS#123"}
   :credentials.tenant.number  {:en-US "Number of Credentials: #123"}
   :credentials.tenant.key  {:en-US "Api Key#123"}
   :credentials.key.aria {:en-US "Show/Hide Api Key Details#123"}
   :credentials.key.permissions {:en-US "Permissions #123"}
   :credentials.key.secret {:en-US "API Key Secret#123"}
   :credentials.key.hide {:en-US "Hide#123"}
   :credentials.key.show {:en-US "Show Secret Key#123"}
   :credentials.key.permissions.save {:en-US "Save#123"}
   :credentials.key.permissions.cancel {:en-US "Cancel#123"}
   :credentials.key.edit {:en-US "Edit#123"}
   :credentials.key.delete {:en-US "Delete#123"}
   :credentials.key.delete.confirm {:en-US "Are you sure?#123"}
   ;;Accounts Page
   :accounts.delete {:en-US "Delete#123"}
   :accounts.delete.confirm {:en-US "Are you sure?#123"}
   :accounts.password.update {:en-US "Update Password#123"}
   :accounts.new.username {:en-US "Username:#123"}
   :accounts.new.username.note {:en-US "Username must be %d or more alphanumeric characters#123"}
   :accounts.new.password {:en-US "Password:#123"}
   :accounts.new.password.note {:en-US "Password must be %d or more characters and contain uppercase, lowercase, numbers, and special characters (%s). Be sure to note or copy the new password as it will not be accessible after creation.#123"}
   :accounts.new.password.hide {:en-US "Hide#123"}
   :accounts.new.password.show {:en-US "Show#123"}
   :accounts.new.password.copy {:en-US "Copy#123"}
   :accounts.new.password.generate {:en-US "Generate Password#123"}
   :accounts.new {:en-US "CREATE ACCOUNT#123"}
   ;;Browser
   :browser.title {:en-US "DATA BROWSER#123"}
   :browser.credentials {:en-US "Credentials to Use:#123"}
   :browser.query {:en-US "Current Query:#123"}
   :browser.key-note {:en-US "Please Choose an API Key Above to Browse LRS Data#123"}
   ;;Monitor
   :monitor.title {:en-US "LRS Monitor#123"}
   :monitor.no-data {:en-US "No Statement Data#123"}
   :monitor.refresh {:en-US "REFRESH#123"}

   :monitor.statements.title {:en-US "STATEMENTS#123"}
   :monitor.actors.title {:en-US "ACTORS#123"}
   :monitor.last-statement.title {:en-us "LAST STATEMENT AT#123"}
   :monitor.timeline.title {:en-US "TIMELINE#123"}
   :monitor.timeline.unit {:en-US "Time Unit#123"}
   :monitor.timeline.since {:en-US "Since#123"}
   :monitor.timeline.until {:en-US "Until#123"}

   :monitor.platform.title {:en-US "PLATFORM#123"}
   :monitor.platform.tooltip {:en-US "This metric requires proper use of the “context.platform” field in associated xAPI Statements. If you do not see your connected system represented here, it is possible that it is posting statements that are not using this field.#123"}

   ;;Data Management
   :datamgmt.title {:en-US "DATA MANAGEMENT#123"}
   :datamgmt.delete.title {:en-US "Delete Actor#123"}
   :datamgmt.delete.button {:en-US "DELETE#123"}

   ;;Notifications
   :notification.credentials.key-copied {:en-US "Copied API Key!#123"}
   :notification.credentials.secret-copied {:en-US "Copied Secret Key!#123"}
   :notification.accounts.password-copied {:en-US "Copied New Password!#123"}})
