package com.droidhen.formalautosim.data.remote

import android.util.Log
import com.droidhen.formalautosim.core.entities.AutomataUser
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import javax.inject.Inject

class Firebase @Inject constructor() {
    private val db = Firebase.firestore

    fun getUserData(onSuccess: () -> Unit, onFailure: () -> Unit) {

    }

    fun setUser(user: AutomataUser, onSuccess: () -> Unit, onFailure: () -> Unit) {

    }

    fun trySignUp(user: AutomataUser, onSuccess: () -> Unit, onFailure: () -> Unit, onTechnicalProblem:()->Unit) {
        db.collection("users").apply {
            get().addOnSuccessListener { result ->
                for (document in result) {
                    if (document.data["email"] == user.getLogInData().first) {
                        onFailure()
                        return@addOnSuccessListener
                    }
                }
                add(user.transformUserToMap()).addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { onFailure() }
            }.addOnFailureListener {
                onTechnicalProblem() }
        }
    }

    fun trySignIn(user: AutomataUser, onSuccess: (user:AutomataUser) -> Unit, onFailure: () -> Unit, onTechnicalProblem:()->Unit) {
        Log.e("sasha", "request sent")
        db.collection("users").apply {
            get().addOnSuccessListener { result ->
                for (document in result) {
                    if (document.data["email"] == user.getLogInData().first) {
                        if(document.data["password"] == user.getLogInData().second){
                            Log.e("sasha","response received success")
                            onSuccess(AutomataUser.transformServerResponseToUser(document.data as Map<String, String>))
                            return@addOnSuccessListener
                        }else{
                            Log.e("sasha", "response received failure")
                            onFailure()
                            return@addOnSuccessListener
                        }
                    }
                }
                onFailure()
            }.addOnFailureListener { onTechnicalProblem() }
        }
    }
}