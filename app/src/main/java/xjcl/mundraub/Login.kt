package xjcl.mundraub

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.kittinunf.fuel.Fuel
import kotlinx.android.synthetic.main.activity_login.*

class Login : AppCompatActivity() {
    val loginData = mutableMapOf(
        "form_id" to "user_login_form",
        "op" to "Anmelden"
    )

    // migrate the user's old settings (new in v13) -- can delete this a few weeks out
    private fun migrateSettings() {
        val sharedPref = this.getSharedPreferences("global", Context.MODE_PRIVATE)
        val sharedPrefOld = this.getSharedPreferences("AddPlantActivity", Context.MODE_PRIVATE)
        val newName = sharedPref.getString("name", "") ?: ""
        val newPass = sharedPref.getString("pass", "") ?: ""
        val oldName = sharedPrefOld.getString("name", newName) ?: newName
        val oldPass = sharedPrefOld.getString("pass", newPass) ?: newPass
        with (sharedPref.edit()) {
            putString("name", oldName)
            putString("pass", oldPass)
            apply()
        }
        with (sharedPrefOld.edit()) {
            remove("name")
            remove("pass")
            apply()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        migrateSettings()

        val sharedPref = this.getSharedPreferences("global", Context.MODE_PRIVATE)
        val name = sharedPref.getString("name", "") ?: ""

        if (hasLoginCookie(this) && name.isNotBlank()) {
            this.top_info.visibility = View.VISIBLE
            this.top_info.setText( getString(R.string.loggedInAs, name) )
        }

        this.name_inner.setText( name )
        this.pass_inner.setText( sharedPref.getString("pass", "") ?: "" )
    }

    fun onLoginClick(view : View) {
        loginData["name"] = this.name_inner.text.toString()
        loginData["pass"] = this.pass_inner.text.toString()
        if (loginData["name"]!!.isBlank() || loginData["pass"]!!.isBlank())
            { Toast.makeText(this, getString(R.string.errMsgLoginInfo), Toast.LENGTH_SHORT).show(); return }

        Fuel.post("https://mundraub.org/user/login", loginData.toList()).allowRedirects(false).responseString { request, response, result ->

            when (response.statusCode) {
                -1 -> {runOnUiThread { Toast.makeText(this, getString(R.string.errMsgNoInternet), Toast.LENGTH_SHORT).show() }; return@responseString}
                303 -> {}
                else -> {runOnUiThread { Toast.makeText(this, getString(R.string.errMsgLogin), Toast.LENGTH_SHORT).show() }; return@responseString}
            }

            val cook = response.headers["Set-Cookie"].first()

            val sharedPref = this.getSharedPreferences("global", Context.MODE_PRIVATE)
            with (sharedPref.edit()) {
                putString("name", loginData["name"])
                putString("pass", loginData["pass"])
                putString("cookie", cook)
                apply()
            }

            Log.e("cook", "success with cookie $cook")
            runOnUiThread { Toast.makeText(this, getString(R.string.errMsgLoginSuccess), Toast.LENGTH_SHORT).show() }
            finish()
        }
    }

    fun onRegisterClick(view : View) {
        startActivityForResult(Intent(this, Register::class.java), 56)
        finish()
    }
}
// TODO: "currently logged in, this is just for changing accounts" message
