package com.mparticle.modernization

import com.mparticle.identity.IdentityApiResult

abstract class MParticleCallback <S, E> {
    var isSuccessFul : Boolean = false
    var isCompleted : Boolean = false

     fun onSuccess(result : S) {
         isSuccessFul = true
         isCompleted = true
     }

     fun onError(error : E){
         isSuccessFul = false
         isCompleted = true
     }
}