package com.example.gramatika

enum class GrammarType(private val displayName: String) {
    REGULAR("Regular Grammar"),
    CONTEXT_FREE("Context-Free Grammar"),
    CONTEXT_SENSITIVE("Context-Sensitive Grammar"),
    UNRESTRICTED("Unrestricted Grammar");

    override fun toString(): String = displayName
}


fun testForReg(rules: List<GrammarRule>): Boolean{
    for(rule in rules){
        if(rule.left.length == 1 && rule.left.first().isUpperCase()){
            if(rule.right.length <= 2){
                if(rule.right.length == 1 && (rule.right.first().isLowerCase() || rule.right.first().isDigit() )) {
                    continue
                }else if(rule.right[0].isUpperCase().xor(rule.right[1].isUpperCase())){
                    continue
                }
            }
        }
        return false
    }
    return true
}

fun testForContextFree(rules: List<GrammarRule>): Boolean{
    for(rule in rules){
        if (rule.left.length != 1 || !rule.left.first().isUpperCase()) {
            return false
        }
    }
    return true
}

fun testForContextSens(rules: List<GrammarRule>): Boolean{
    for (rule in rules){
        if(rule.right.length < rule.left.length){
            return false
        }
    }
    return true
}