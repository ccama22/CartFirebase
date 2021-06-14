package com.example.cartfirebase

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cartfirebase.adapter.MyCartAdapter
import com.example.cartfirebase.listener.ICartLoadListener
import com.example.cartfirebase.model.CartModel
import com.example.kotlincartfirebase.eventbus.UpdateCartEvent
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_cart.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.lang.StringBuilder

class CartActivity : AppCompatActivity(), ICartLoadListener {
    var cartLoadListener:ICartLoadListener?=null

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }
    override fun onStop() {
        super.onStop()
        if(EventBus.getDefault().hasSubscriberForEvent(UpdateCartEvent::class.java))
            EventBus.getDefault().removeStickyEvent(UpdateCartEvent::class.java)
        EventBus.getDefault().unregister(this)
    }
    @Subscribe(threadMode = ThreadMode.MAIN,sticky = true)
    fun onUpdateCartEvent(event: UpdateCartEvent){
        loadCartFromFirebase()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)
        init()
        loadCartFromFirebase()
    }

    private fun loadCartFromFirebase() {
        val cartModels:MutableList<CartModel> = ArrayList()
        FirebaseDatabase.getInstance().getReference("Cart").child("UNIQUE_USER_ID").addListenerForSingleValueEvent(object:
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for(cartSnapshot in snapshot.children){
                    val cartModel=cartSnapshot.getValue(CartModel::class.java)
                    cartModel!!.Key=cartSnapshot.key
                    cartModels.add(cartModel)
                }
                cartLoadListener!!.onLoadCartSuccess(cartModels)
            }

            override fun onCancelled(error: DatabaseError) {
                cartLoadListener!!.onLoadCartFailed(error.message)

            }

        })
    }

    private fun init(){
        cartLoadListener=this
        val layoutManayer = LinearLayoutManager(this)
        recycler_cart!!.layoutManager=layoutManayer
        recycler_cart!!.addItemDecoration(DividerItemDecoration(this,layoutManayer.orientation))
        btnBack!!.setOnClickListener { finish() }
    }

    override fun onLoadCartSuccess(cartModelList: List<CartModel>) {
        var sum=0.0
        for(carModel in cartModelList!!){
            sum+=carModel!!.totalPrice

        }
        txtTotal.text = StringBuilder("$").append(sum)
        val adapter = MyCartAdapter(this,cartModelList)
        recycler_cart!!.adapter=adapter
    }

    override fun onLoadCartFailed(message: String?) {
        Snackbar.make(findViewById(android.R.id.content),message!!, Snackbar.LENGTH_SHORT).show()
    }
}