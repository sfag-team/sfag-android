package com.droidhen.formalautosim.core.entities

class User {
    private var email: String? = null
    private var password: String? = null
    private var name: String? = null

    fun setEmail(email: String?) {
        this.email = email
    }

    fun setPassword(password: String?){
         this.password = password
    }

    fun setUserName(name: String?) {
        this.name = name
    }

    fun getLogInData(): Pair<String?, String?> = email to password

    fun getName() = name

    fun transformUserToMap(): Map<String, String> {
        return mapOf(
            "email" to email.toString(),
            "password" to password.toString(),
            "name" to name.toString()
        )
    }

    fun clone(user: User) {
        this.name = user.name
        this.password = user.password
        this.email = user.email
    }

    companion object {
        fun transformServerResponseToUser(response: Map<String, String>): AutomataUser {
            val result = AutomataUser().apply {
                setUserName(response["name"])
                setEmail(response["email"])
                setPassword(response["password"])
            }
            return result
        }
    }
}