package io.github.kamishirokalina.Votes

interface Vote {
    companion object {
        val Votes = mutableListOf<Vote>()
    }

    suspend fun Init(){
        Votes.add(this)
    }

    suspend fun CheakCmd(cmd: String): Boolean {
        return false
    }

    suspend fun Run(cmd: String){

    }
}