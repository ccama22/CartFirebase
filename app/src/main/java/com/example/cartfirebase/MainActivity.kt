package com.example.cartfirebase

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import com.example.cartfirebase.adapter.MyDrinkAdapter
import com.example.cartfirebase.listener.ICartLoadListener
import com.example.cartfirebase.listener.IDrinkLoadListener
import com.example.cartfirebase.model.CartModel
import com.example.cartfirebase.model.DrinkModel
import com.example.kotlincartfirebase.Utils.SpaceItemDecoration
import com.example.kotlincartfirebase.eventbus.UpdateCartEvent
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_main.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class MainActivity : AppCompatActivity(), IDrinkLoadListener,ICartLoadListener {
    lateinit var drinkLoadListener: IDrinkLoadListener
    lateinit var cartLoadListener: ICartLoadListener
    private val mFireStore = FirebaseFirestore.getInstance()

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
    fun onUpdateCartEvent(event:UpdateCartEvent){
        countCartFromFirebase()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()
        loadDrinkFromFirestore()
        countCartFromFirebase()
    }

    private fun countCartFromFirebase() {
        val cartModels:MutableList<CartModel> = ArrayList()
        FirebaseDatabase.getInstance().getReference("Cart").child("UNIQUE_USER_ID").addListenerForSingleValueEvent(object:
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for(cartSnapshot in snapshot.children){
                    val cartModel=cartSnapshot.getValue(CartModel::class.java)
                    cartModel!!.Key=cartSnapshot.key
                    cartModels.add(cartModel)
                }
                cartLoadListener.onLoadCartSuccess(cartModels)
            }

            override fun onCancelled(error: DatabaseError) {
                cartLoadListener.onLoadCartFailed(error.message)

            }

        })
    }

    private fun loadDrinkFromFirestore() {
        val drinkModels:MutableList<DrinkModel> =ArrayList()
        mFireStore.collection("Drink").get()
            .addOnSuccessListener {document->
                Toast.makeText(this, "datosActual"+document.documents, Toast.LENGTH_SHORT).show()
                Log.e("Drink list", document.documents.toString())
                for(i in document.documents){
                    val drinkModel=i.toObject(DrinkModel::class.java)
                    drinkModel!!.Key=i.id
                    drinkModels.add(drinkModel)
                }
                drinkLoadListener.onDrinkLoadSuccess(drinkModels)
            }
            .addOnFailureListener {
                drinkLoadListener.onDrinkLoadFailed("Bebida no existe")
            }
    }

    private fun init(){
        drinkLoadListener=this
        cartLoadListener=this
        val gridLayoutManager= GridLayoutManager(this,2)
        recycler_drink.layoutManager=gridLayoutManager
        recycler_drink.addItemDecoration(SpaceItemDecoration())

        btnCart.setOnClickListener { startActivity(Intent(this,CartActivity::class.java)) }

    }

    override fun onDrinkLoadSuccess(drinkModelList: List<DrinkModel>?) {
        val adapter= MyDrinkAdapter(this,drinkModelList!!,cartLoadListener)
        recycler_drink.adapter=adapter
    }

    override fun onDrinkLoadFailed(message: String?) {
        Snackbar.make(findViewById(android.R.id.content),message!!, Snackbar.LENGTH_SHORT).show()
    }

    override fun onLoadCartSuccess(cartModelList: List<CartModel>) {
        var cartSum=0
        for(cartModel in cartModelList!!) cartSum+= cartModel!!.quantity
        badge!!.setNumber(cartSum)
    }

    override fun onLoadCartFailed(message: String?) {
        Snackbar.make(findViewById(android.R.id.content),message!!, Snackbar.LENGTH_SHORT).show()
    }
}