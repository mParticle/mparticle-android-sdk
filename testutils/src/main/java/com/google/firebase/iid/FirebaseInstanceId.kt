package com.google.firebase.iid

object FirebaseInstanceIdToken {
    var token: String? = null
}

class FirebaseInstanceId {

    companion object {
        @JvmStatic
        fun getInstance() = FirebaseInstanceId()

        @JvmStatic
        fun setToken(token: String?) {
            FirebaseInstanceIdToken.token = token
        }
    }

    fun getToken() = FirebaseInstanceIdToken.token
    fun getToken(authority: String, scope: String) = FirebaseInstanceIdToken.token
}
