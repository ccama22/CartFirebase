package com.example.cartfirebase.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.view.menu.ActionMenuItemView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.cartfirebase.R
import com.example.cartfirebase.listener.ICartLoadListener
import com.example.cartfirebase.listener.IRecyclerClickListener
import com.example.cartfirebase.model.CartModel
import com.example.cartfirebase.model.DrinkModel
import com.example.kotlincartfirebase.eventbus.UpdateCartEvent
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import org.greenrobot.eventbus.EventBus

class MyDrinkAdapter(
    private val context:Context,
    private val list:List<DrinkModel>,
    private val cartListener:ICartLoadListener
):RecyclerView.Adapter<MyDrinkAdapter.MyDrinkViewHolder>() {
    class MyDrinkViewHolder(itemView: View): RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        var imageView: ImageView?=null
        var txtName: TextView?=null
        var txtPrice: TextView?=null
        var imageCart: ImageView?=null

        private var clickListener:IRecyclerClickListener?=null
        fun setOnclickListener(clickListener: IRecyclerClickListener){
            this.clickListener=clickListener
        }

        init {
            imageView=itemView.findViewById(R.id.imageView) as ImageView
            txtName=itemView.findViewById(R.id.txtName) as TextView
            txtPrice=itemView.findViewById(R.id.txtPrice) as TextView
            imageCart=itemView.findViewById(R.id.txtcart) as ImageView

            itemView.setOnClickListener(this)

        }

        override fun onClick(v: View?) {
            clickListener!!.onItemClickListener(v,adapterPosition)
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyDrinkViewHolder {
        return MyDrinkViewHolder(LayoutInflater.from(context).inflate(R.layout.layout_drink_item,parent,false))
    }

    override fun onBindViewHolder(holder: MyDrinkViewHolder, position: Int) {
        Glide.with(context).load(list[position].image).into(holder.imageView!!)
        holder.txtName!!.text=StringBuilder().append(list[position].name)
        holder.txtPrice!!.text=StringBuilder("$").append(list[position].price)
        holder.setOnclickListener(object:IRecyclerClickListener{
            override fun onItemClickListener(view: View?, position: Int) {
                addToCart(list[position])
            }

        })

    }

    private fun addToCart(drinkModel: DrinkModel) {
        val userCart= FirebaseDatabase.getInstance().getReference("Cart").child("UNIQUE_USER_ID") // Here is simular user ID, you can use Firebase Auth uid here
        userCart.child(drinkModel.Key!!).addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists()){
                    val cartModel=snapshot.getValue(CartModel::class.java)
                    val updateData:MutableMap<String,Any> = HashMap()
                    cartModel!!.quantity=cartModel!!.quantity+1
                    updateData["quantity"]= cartModel!!.quantity
                    updateData["totalPrice"]=cartModel!!.quantity*cartModel.price!!.toFloat()

                    userCart.child(drinkModel.Key!!).updateChildren(updateData)
                        .addOnSuccessListener {
                            EventBus.getDefault().postSticky(UpdateCartEvent())
                            cartListener.onLoadCartFailed("Éxito añadir al carrito")
                        }
                        .addOnFailureListener { e->
                            cartListener.onLoadCartFailed(e.message)
                        }

                }
                else{
                    val cartModel=CartModel()
                    cartModel.Key=drinkModel.Key
                    cartModel.name=drinkModel.name
                    cartModel.image=drinkModel.image
                    cartModel.price=drinkModel.price
                    cartModel.quantity=1
                    cartModel.totalPrice=drinkModel.price!!.toFloat()
                    userCart.child(drinkModel.Key!!).setValue(cartModel)
                        .addOnSuccessListener {
                            EventBus.getDefault().postSticky(UpdateCartEvent())
                            cartListener.onLoadCartFailed("Éxito añadir al carrito")
                        }
                        .addOnFailureListener { e->
                            cartListener.onLoadCartFailed(e.message)
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }

    override fun getItemCount(): Int {
        return list.size
    }

}