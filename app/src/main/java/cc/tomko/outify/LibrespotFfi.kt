package cc.tomko.outify

import android.content.Context

object LibrespotFfi {

    @JvmStatic
    external fun libInit(context: Context, clientId: String = "819a62c83de24821b2654387bc84f136", clientSecret: String = "6db424c706d34cf7810a5c8c59324182")
}