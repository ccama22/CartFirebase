package com.example.cartfirebase.listener

import com.example.cartfirebase.model.CartModel

interface ICartLoadListener {
    fun onLoadCartSuccess(cartModelList: List<CartModel>)
    fun onLoadCartFailed(message:String?)
}