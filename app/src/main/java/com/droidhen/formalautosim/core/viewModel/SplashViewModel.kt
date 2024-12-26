package com.droidhen.formalautosim.core.viewModel

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.droidhen.formalautosim.core.entities.User
import com.droidhen.formalautosim.data.local.SQLite
import com.droidhen.formalautosim.data.remote.Firebase
import com.droidhen.formalautosim.data.remote.InternetTool
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val firebase: Firebase,
    private val sqLite: SQLite,
    private val internet: InternetTool
) : ViewModel() {
    var isSignIn = true
    private val user = User()
    private var isPasswordShowed = mutableStateOf(false)
    private val password = mutableStateOf("")
    private val email = mutableStateOf("")
    private val name = mutableStateOf("")
    private val secondPassword = mutableStateOf("")
    private val pref = context.getSharedPreferences("FAS", Context.MODE_PRIVATE)

    fun setEmail(email: String) {
        user.setEmail(email)
        this.email.value = email
    }

    fun getEmail(): MutableState<String> {
        return email
    }

    fun getPassword() = password

    fun setPassword(password: String) {
        user.setPassword(password)
        this.password.value = password
    }

    fun setName(name: String) {
        user.setUserName(name)
        this.name.value = name
    }

    fun getName() = name

    fun getSecondPassword() = secondPassword

    fun setSecondPassword(password: String) {
        secondPassword.value = password
    }

    fun getPasswordShowed() = isPasswordShowed

    fun checkPasswordRequirements(): Boolean =
        password.value == secondPassword.value && checkSinglePasswordRequirements()

    fun checkSinglePasswordRequirements(): Boolean =
        password.value.length > 7

    fun checkEmailRequirements(): Boolean =
        email.value.contains("@") && email.value.contains(".") && email.value.length > 4

    fun checkNameRequirements(): Boolean = name.value.isNotEmpty()

    fun isButtonEnabled(): Boolean {
        return if (isSignIn) {
            checkEmailRequirements()
        } else {
            checkNameRequirements() && checkPasswordRequirements() && checkEmailRequirements()
        }
    }

    fun buttonPressed(onSuccess: () -> Unit, onWrongData: () -> Unit, onFailure: () -> Unit) {
        if(internet.isInternetAvailable()){
            if (isSignIn) {
                signIn(onSuccess, onWrongData, onFailure)
            } else {
                signUp(onSuccess, onWrongData, onFailure)
            }
        }else{
            onFailure()
        }
    }

    fun saveUserNonAuthorised(){
        pref.edit().putBoolean("skippedSignIn", true).apply()
    }

    fun checkIfUserAuthorised():Boolean = pref.getBoolean("skippedSignIn", false)



    private fun signUp(onSuccess: () -> Unit, onWrongData: () -> Unit, onFailure: () -> Unit) {
        firebase.trySignUp(user, {
            try {
                sqLite.insertUser(user)
                pref.edit().putBoolean("skippedSignIn", true).apply()
                pref.edit().putBoolean("userSignedIn", true).apply()
                onSuccess()
            } catch (e: Exception) {
                //TODO - log
                onFailure()
            }
        }, onWrongData, onFailure)
    }

    private fun signIn(onSuccess: () -> Unit, onWrongData: () -> Unit, onFailure: () -> Unit) {
            firebase.trySignIn(user, {
                try {
                    user.clone(it)
                    sqLite.insertUser(user)
                    pref.edit().putBoolean("skippedSignIn", true).apply()
                    pref.edit().putBoolean("userSignedIn", true).apply()
                    onSuccess()
                } catch (e: Exception) {
                    //TODO - log
                    onFailure()
                }
            }, onWrongData, onFailure)
    }
}