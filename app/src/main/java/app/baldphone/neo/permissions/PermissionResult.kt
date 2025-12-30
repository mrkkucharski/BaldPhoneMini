package app.baldphone.neo.permissions

sealed interface PermissionResult {
    data object Denied : PermissionResult
    data object Error : PermissionResult
    data object Granted : PermissionResult
    data object PermanentlyDenied : PermissionResult
}
