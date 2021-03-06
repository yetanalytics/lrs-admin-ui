/**********************************************************************
 * Extern for Oidc
 * Generated by http://jmmk.github.io/javascript-externs-generator
 **********************************************************************/
var Oidc = {
  "AccessTokenEvents": function () {},
  "CheckSessionIFrame": function () {},
  "CordovaIFrameNavigator": function () {},
  "CordovaPopupNavigator": function () {},
  "Global": {
    "_testing": function () {},
    "setXMLHttpRequest": function () {}
  },
  "InMemoryWebStorage": function () {},
  "Log": {
    "debug": function () {},
    "error": function () {},
    "info": function () {},
    "reset": function () {},
    "warn": function () {}
  },
  "MetadataService": function () {},
  "OidcClient": function () {},
  "OidcClientSettings": function () {},
  "SessionMonitor": function () {},
  "TokenRevocationClient": function () {},
  "User": {
    "fromStorageString": function () {}
  },
  "UserManager": function () {},
  "Version": {},
  "WebStorageStateStore": function () {}
};
Oidc.AccessTokenEvents.prototype = {
  "addAccessTokenExpired": function () {},
  "addAccessTokenExpiring": function () {},
  "load": function () {},
  "removeAccessTokenExpired": function () {},
  "removeAccessTokenExpiring": function () {},
  "unload": function () {}
};
Oidc.CheckSessionIFrame.prototype = {
  "_message": function () {},
  "load": function () {},
  "start": function () {},
  "stop": function () {}
};
Oidc.CordovaIFrameNavigator.prototype = {
  "prepare": function () {}
};
Oidc.CordovaPopupNavigator.prototype = {
  "prepare": function () {}
};
Oidc.InMemoryWebStorage.prototype = {
  "getItem": function () {},
  "key": function () {},
  "removeItem": function () {},
  "setItem": function () {}
};
Oidc.MetadataService.prototype = {
  "_getMetadataProperty": function () {},
  "getAuthorizationEndpoint": function () {},
  "getCheckSessionIframe": function () {},
  "getEndSessionEndpoint": function () {},
  "getIssuer": function () {},
  "getKeysEndpoint": function () {},
  "getMetadata": function () {},
  "getRevocationEndpoint": function () {},
  "getSigningKeys": function () {},
  "getTokenEndpoint": function () {},
  "getUserInfoEndpoint": function () {},
  "resetSigningKeys": function () {}
};
Oidc.OidcClient.prototype = {
  "clearStaleState": function () {},
  "createSigninRequest": function () {},
  "createSignoutRequest": function () {},
  "processSigninResponse": function () {},
  "processSignoutResponse": function () {},
  "readSigninResponseState": function () {},
  "readSignoutResponseState": function () {}
};
Oidc.OidcClientSettings.prototype = {
  "getEpochTime": function () {}
};
Oidc.SessionMonitor.prototype = {
  "_callback": function () {},
  "_start": function () {},
  "_stop": function () {}
};
Oidc.TokenRevocationClient.prototype = {
  "_revoke": function () {},
  "revoke": function () {}
};
Oidc.User.prototype = {
  "toStorageString": function () {}
};
Oidc.UserManager.prototype = {
  "_loadUser": function () {},
  "_revokeAccessTokenInternal": function () {},
  "_revokeInternal": function () {},
  "_revokeRefreshTokenInternal": function () {},
  "_signin": function () {},
  "_signinCallback": function () {},
  "_signinEnd": function () {},
  "_signinSilentIframe": function () {},
  "_signinStart": function () {},
  "_signout": function () {},
  "_signoutEnd": function () {},
  "_signoutStart": function () {},
  "_useRefreshToken": function () {},
  "_validateIdTokenFromTokenRefreshToken": function () {},
  "clearStaleState": function () {},
  "createSigninRequest": function () {},
  "createSignoutRequest": function () {},
  "getUser": function () {},
  "processSigninResponse": function () {},
  "processSignoutResponse": function () {},
  "querySessionStatus": function () {},
  "readSigninResponseState": function () {},
  "readSignoutResponseState": function () {},
  "removeUser": function () {},
  "revokeAccessToken": function () {},
  "signinCallback": function () {},
  "signinPopup": function () {},
  "signinPopupCallback": function () {},
  "signinRedirect": function () {},
  "signinRedirectCallback": function () {},
  "signinSilent": function () {},
  "signinSilentCallback": function () {},
  "signoutCallback": function () {},
  "signoutPopup": function () {},
  "signoutPopupCallback": function () {},
  "signoutRedirect": function () {},
  "signoutRedirectCallback": function () {},
  "startSilentRenew": function () {},
  "stopSilentRenew": function () {},
  "storeUser": function () {}
};
Oidc.WebStorageStateStore.prototype = {
  "get": function () {},
  "getAllKeys": function () {},
  "remove": function () {},
  "set": function () {}
};
/**********************************************************************
 * End Generated Extern for Oidc
/**********************************************************************/

// Non generated externs

/** @record */
function UserManagerEvents() {};

UserManagerEvents.prototype = {
  "addUserLoaded": function () {},
  "removeUserLoaded": function () {},
  "addUserUnloaded": function () {},
  "removeUserUnloaded": function () {},
  "addAccessTokenExpiring": function () {},
  "removeAccessTokenExpiring": function () {},
  "addAccessTokenExpired": function () {},
  "removeAccessTokenExpired": function () {},
  "addSilentRenewError": function () {},
  "removeSilentRenewError": function () {},
  "addUserSignedOut": function () {},
  "removeUserSignedOut": function () {}
};

/** @type {UserManagerEvents} */
Oidc.UserManager.prototype.events;

/** @record */
function User() {};

User.prototype = {
    "id_token": function () {},
    "session_state": function () {},
    "access_token": function () {},
    "refresh_token": function () {},
    "token_type": function () {},
    "scope": function () {},
    "profile": function () {},
    "expires_at": function () {},
    "state": function () {},
    "expires_in": function () {},
    "expired": function () {},
    "scopes": function () {},
    "toStorageString": function () {},
    "fromStorageString": function () {}
};

/** @type {User} */
Oidc.UserManager.prototype.getUser;
