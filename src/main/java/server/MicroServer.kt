package server
import com.google.gson.Gson
import spark.Service
import spark.Service.ignite

class MicroServer(val port: Int) : Thread() {

    private lateinit var http: Service

    override fun run() {
        val gson = Gson()
        http = ignite().port(port)

        http.get("/statics", { _, _ -> listOf("a", "b", "c") }, gson::toJson)

    }

    fun delete(){
        http.stop()
    }
}