package org.apache.cordova.mqtt;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import java.sql.Timestamp;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

/**
 *
 */
public class MqTTPlugin extends CordovaPlugin implements MqttCallback {
    private static final String LOG_TAG = "MqttPlugin";
    CallbackContext connectcbctx = null;
    CallbackContext pubbishcbctx = null;
    CallbackContext subscribecbctx = null;
    // Private instance variables
    private MqttClient client;
    private String brokerUrl;
    private boolean quietMode;
    private MqttConnectOptions conOpt;
    private boolean clean;
    private String password;
    private String userName;
    private String port = "1883";

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("connect")) {
            String url, cid, userName, password;
            boolean cleanSession;
            url = args.getString(0);
            cid = args.getString(1);
            cleanSession = args.getBoolean(2);
            userName = args.getString(3);
            password = args.getString(4);
            this.connect(url, cid, cleanSession, userName, password, callbackContext);
            return true;
        }
        if (action.equals("publish")) {
            this.publish(args, callbackContext);
            return true;
        }
        if (action.equals("subscribe")) {
            this.subscribe(args, callbackContext);
            return true;
        }
        if (action.equals("disconnect")) {
            this.disconnect(callbackContext);
            return true;
        }
        return false;
    }

    private void connect(String url, String clientId, boolean cleanSession, String userName, String password, CallbackContext cbctx) {
        this.connectcbctx = cbctx;
        if (url != null && url.length() > 0) {
            // With a valid set of arguments, the real work of
            // driving the client API can begin
            try {
                // Create an instance of this class
                this.brokerUrl = "tcp://" + url + ":" + this.port;
                this.quietMode = true;
                this.clean = cleanSession;
                this.password = password;
                this.userName = userName;

                //This sample stores in a temporary directory... where messages temporarily
                // stored until the message has been delivered to the server.
                //..a real application ought to store them somewhere
                // where they are not likely to get deleted or tampered with
                String tmpDir = System.getProperty("java.io.tmpdir");
                MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(tmpDir);


                // Construct the connection options object that contains connection parameters
                // such as cleanSession and LWT
                conOpt = new MqttConnectOptions();
                conOpt.setCleanSession(clean);
                conOpt.setKeepAliveInterval(30);
                if (password != null) {
                    conOpt.setPassword(this.password.toCharArray());
                }
                if (userName != null) {
                    conOpt.setUserName(this.userName);
                }

                // Construct an MQTT blocking mode client
                client = new MqttClient(this.brokerUrl, clientId, dataStore);

                // Set this wrapper as the callback handler
                client.setCallback(this);

                // Connect to the MQTT server
                log("Connecting to " + this.brokerUrl + " with client ID " + client.getClientId());
                client.connect(conOpt);
                log("Connected");
                cbctx.success("connected");
            } catch (MqttException me) {
                // Display full details of any exception that occurs
                System.out.println("reason " + me.getReasonCode());
                System.out.println("msg " + me.getMessage());
                System.out.println("loc " + me.getLocalizedMessage());
                System.out.println("cause " + me.getCause());
                System.out.println("excep " + me);
                me.printStackTrace();
                cbctx.error(me.getMessage());
            }
        } else {
            cbctx.error("Expected one non-empty string argument.");
        }
    }

    private void publish(final JSONArray args, final CallbackContext cbctx) throws JSONException {
        this.pubbishcbctx = cbctx;
        final String topicName = args.getString(0);
        final int qos = args.getInt(1);
        final String message = args.getString(2);
        final byte[] payload = message.getBytes();
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                String time = new Timestamp(System.currentTimeMillis()).toString();
                log("Publishing at: " + time + " to topic \"" + topicName + "\" qos " + qos);

                // Create and configure a message
                MqttMessage mqttmessage = new MqttMessage(payload);
                mqttmessage.setQos(qos);

                // Send the message to the server, control is not returned until
                // it has been delivered to the server meeting the specified
                // quality of service.
                try {
                    client.publish(topicName, mqttmessage);
                } catch (MqttException me) {
                    pubbishcbctx.error(me.getMessage());
                }
            }
        });
    }

    private void subscribe(final JSONArray args, final CallbackContext cbctx) throws JSONException {
        this.subscribecbctx = cbctx;
        final String topicName = args.getString(0);
        final int qos = args.getInt(1);
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                // Subscribe to the requested topic
                // The QoS specified is the maximum level that messages will be sent to the client at.
                // For instance if QoS 1 is specified, any messages originally published at QoS 2 will
                // be downgraded to 1 when delivering to the client but messages published at 1 and 0
                // will be received at the same level they were published at.
                log("Subscribing to topic \"" + topicName + "\" qos " + qos);
                try {
                    client.subscribe(topicName, qos);
                } catch (MqttException me) {
                    subscribecbctx.error(me.getMessage());
                }
            }
        });
    }

    private void disconnect(final CallbackContext cbctx) throws JSONException {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                // Disconnect the client
                try {
                    client.disconnect();
                } catch (MqttException me) {
                    cbctx.error(me.getMessage());
                }
                log("Disconnected");
                cbctx.success("disconnected");
            }
        });
    }

    /**
     * Utility method to handle logging. If 'quietMode' is set, this method does nothing
     *
     * @param message the message to log
     */

    private void log(String message) {
        if (!quietMode) {
            System.out.println(message);
        }
    }

    /****************************************************************/
    /* Methods to implement the MqttCallback interface              */
    /****************************************************************/

    /**
     * @see MqttCallback#connectionLost(Throwable)
     */
    public void connectionLost(Throwable cause) {
        // Called when the connection to the server has been lost.
        // An application may choose to implement reconnection
        // logic at this point. This sample simply exits.
        log("Connection to " + brokerUrl + " lost!" + cause);
        if (this.connectcbctx != null) {
            this.connectcbctx.error("disconnected");
        }
    }

    /**
     * @see MqttCallback#deliveryComplete(IMqttDeliveryToken)
     */
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Called when a message has been delivered to the
        // server. The token passed in here is the same one
        // that was passed to or returned from the original call to publish.
        // This allows applications to perform asynchronous
        // delivery without blocking until delivery completes.
        //
        // This sample demonstrates asynchronous deliver and
        // uses the token.waitForCompletion() call in the main thread which
        // blocks until the delivery has completed.
        // Additionally the deliveryComplete method will be called if
        // the callback is set on the client
        //
        // If the connection to the server breaks before delivery has completed
        // delivery of a message will complete after the client has re-connected.
        // The getPendingTokens method will provide tokens for any messages
        // that are still to be delivered.
        log("deliverCompleted");
        if (this.pubbishcbctx != null) {
            this.pubbishcbctx.success("published");
        }
    }

    /**
     * @see MqttCallback#messageArrived(String, MqttMessage)
     */
    public void messageArrived(String topic, MqttMessage message) throws MqttException {
        // Called when a message arrives from the server that matches any
        // subscription made by the client
        String time = new Timestamp(System.currentTimeMillis()).toString();
        System.out.println("Time:\t" + time +
                "  Topic:\t" + topic +
                "  Message:\t" + new String(message.getPayload()) +
                "  QoS:\t" + message.getQos());

        JSONObject info = getInfo(topic, message);
        if (this.subscribecbctx != null) {
            PluginResult result = new PluginResult(PluginResult.Status.OK, info);
            result.setKeepCallback(true);
            this.subscribecbctx.sendPluginResult(result);
        }
    }

    private JSONObject getInfo(String topic, MqttMessage message) {
        JSONObject obj = new JSONObject();
        String time = new Timestamp(System.currentTimeMillis()).toString();
        try {
            obj.put("time", time);
            obj.put("topic", topic);
            obj.put("content", new String(message.getPayload()));
            obj.put("qos", message.getQos());
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
        }
        return obj;
    }
}
