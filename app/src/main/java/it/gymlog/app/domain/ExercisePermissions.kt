package it.gymlog.app.domain

fun canEditShared(remoteId: String?, creatorId: String?, uid: String?, isAdmin: Boolean): Boolean =
    remoteId == null || isAdmin || (uid != null && creatorId == uid)
