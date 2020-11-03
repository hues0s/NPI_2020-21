package com.npi.practica

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import com.npi.practica.paymentsUtils.PaymentsUtil
import com.npi.practica.paymentsUtils.microsToString
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wallet.*
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import kotlin.math.roundToLong

class PaymentsActivity : AppCompatActivity() {

    /**
     * A client for interacting with the Google Pay API.
     */
    private lateinit var paymentsClient: PaymentsClient

    private val shippingCost = (90 * 1000000).toLong()

    private lateinit var detailTitle: TextView
    private lateinit var detailPrice: TextView
    private lateinit var detailDescription: TextView
    private lateinit var googlePayButton: RelativeLayout

    /**
     * Arbitrarily-picked constant integer you define to track a request for payment data activity.
     */
    private val LOAD_PAYMENT_DATA_REQUEST_CODE = 991



    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payments)

        detailTitle = findViewById(R.id.detailTitle)
        detailPrice = findViewById(R.id.detailPrice)
        detailDescription = findViewById(R.id.detailDescription)
        googlePayButton = findViewById(R.id.googlePayButton)


        // Mostramos la información de la factura
        displayBill()

        // Inicializamos la API de Google Pay en un entorno de prueba.
        paymentsClient = PaymentsUtil.createPaymentsClient(this)
        possiblyShowGooglePayButton()

        googlePayButton.setOnClickListener { requestPayment() }
    }


    /**
     * Determine the viewer's ability to pay with a payment method supported by your app and display a
     * Google Pay payment button.
     */
    private fun possiblyShowGooglePayButton() {

        val isReadyToPayJson = PaymentsUtil.isReadyToPayRequest() ?: return
        val request = IsReadyToPayRequest.fromJson(isReadyToPayJson.toString()) ?: return

        // The call to isReadyToPay is asynchronous and returns a Task. We need to provide an
        // OnCompleteListener to be triggered when the result of the call is known.
        val task = paymentsClient.isReadyToPay(request)
        task.addOnCompleteListener { completedTask ->
            try {
                completedTask.getResult(ApiException::class.java)?.let(::setGooglePayAvailable)
            } catch (exception: ApiException) {
                // Process error
            }
        }
    }


    /**
     * If isReadyToPay returned `true`, show the button and hide the "checking" text. Otherwise,
     * notify the user that Google Pay is not available. Please adjust to fit in with your current
     * user flow. You are not required to explicitly let the user know if isReadyToPay returns `false`.
     */
    private fun setGooglePayAvailable(available: Boolean) {

        if (available) {
            googlePayButton.visibility = View.VISIBLE
        } else {
            Toast.makeText(this, resources.getString(R.string.googlepay_status_unavailable), Toast.LENGTH_LONG).show();
        }

    }


    private fun requestPayment() {

        // Disables the button to prevent multiple clicks.
        googlePayButton.isClickable = false

        // The price provided to the API should include taxes and shipping.
        // This price is not displayed to the user.
        val garmentPriceMicros = (resources.getString(R.string.bill_price).toDouble() * 1000000).roundToLong()
        val price = (garmentPriceMicros + shippingCost).microsToString()

        val paymentDataRequestJson = PaymentsUtil.getPaymentDataRequest(price)
        if (paymentDataRequestJson == null) {
            Timber.e("No se puede recuperar la solicitud de datos de pago.")
            return
        }
        val request = PaymentDataRequest.fromJson(paymentDataRequestJson.toString())

        // Since loadPaymentData may show the UI asking the user to select a payment method, we use
        // AutoResolveHelper to wait for the user interacting with it. Once completed,
        // onActivityResult will be called with the result.
        if (request != null) {
            AutoResolveHelper.resolveTask(paymentsClient.loadPaymentData(request), this, LOAD_PAYMENT_DATA_REQUEST_CODE)
        }
    }


    /**
     * Handle a resolved activity from the Google Pay payment sheet.
     */
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            // value passed in AutoResolveHelper
            LOAD_PAYMENT_DATA_REQUEST_CODE -> {
                when (resultCode) {
                    Activity.RESULT_OK ->
                        data?.let { intent ->
                            PaymentData.getFromIntent(intent)?.let(::handlePaymentSuccess)
                        }
                    Activity.RESULT_CANCELED -> {
                        // Nothing to do here normally - the user simply cancelled without selecting a
                        // payment method.
                    }

                    AutoResolveHelper.RESULT_ERROR -> {
                        AutoResolveHelper.getStatusFromIntent(data)?.let {
                            handleError(it.statusCode)
                        }
                    }
                }
                // Re-enables the Google Pay payment button.
                googlePayButton.isClickable = true
            }
        }
    }


    /**
     * PaymentData response object contains the payment information, as well as any additional
     * requested information, such as billing and shipping address.
     */
    private fun handlePaymentSuccess(paymentData: PaymentData) {

        val paymentInformation = paymentData.toJson() ?: return

        try {
            // Token will be null if PaymentDataRequest was not constructed using fromJson(String).
            val paymentMethodData = JSONObject(paymentInformation).getJSONObject("paymentMethodData")

            // If the gateway is set to "example", no payment information is returned - instead, the
            // token will only consist of "examplePaymentMethodToken".
            if (paymentMethodData
                            .getJSONObject("tokenizationData")
                            .getString("type") == "PAYMENT_GATEWAY" && paymentMethodData
                            .getJSONObject("tokenizationData")
                            .getString("token") == "examplePaymentMethodToken") {

                AlertDialog.Builder(this)
                        .setTitle("Advertencia")
                        .setMessage(resources.getString(R.string.dialog_successful_payment))
                        .setPositiveButton("OK", null)
                        .create()
                        .show()
            }

            val billingName = paymentMethodData.getJSONObject("info")
                    .getJSONObject("billingAddress").getString("name")
            Timber.d(billingName)

            Toast.makeText(this, getString(R.string.payments_show_name, billingName), Toast.LENGTH_LONG).show()

            // Logging token string.
            Timber.d(paymentMethodData.getJSONObject("tokenizationData").getString("token"))

        } catch (e: JSONException) {
            Timber.e("Error: %s", e.toString())
        }

    }


    /**
     * At this stage, the user has already seen a popup informing them an error occurred. Normally,
     * only logging is required.
     */
    private fun handleError(statusCode: Int) {
        Timber.tag("loadPaymentData failed").w(String.format("Error code: %d", statusCode))
    }


    /**
     * Link the bill info with the layout elements.
     */
    private fun displayBill() {

        detailTitle.text = resources.getString(R.string.bill_title)
        detailPrice.text = "\$${resources.getString(R.string.bill_price)}"
        detailDescription.text = resources.getString(R.string.bill_description)

    }
}