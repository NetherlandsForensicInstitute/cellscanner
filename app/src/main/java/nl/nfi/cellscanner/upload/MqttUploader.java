package nl.nfi.cellscanner.upload;

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.InputStream;
import java.net.URI;

import nl.nfi.cellscanner.CellScannerApp;

public class MqttUploader implements Uploader {
    private static final MqttClientPersistence persistence = new MemoryPersistence();

    private static void publish(String broker, String topic, String username, String password, byte[] payload) throws Exception {
        //Log.d("cellscanner", String.format("broker=%s; topic=%s; username=%s; password=%s", broker, topic, username, password));
        MqttClient client = new MqttClient(broker, MqttClient.generateClientId(), persistence);

        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setCleanSession(true);
        if (username != null)
            opts.setUserName(username);
        if (password != null)
            opts.setPassword(password.toCharArray());
        client.connect(opts);

        MqttMessage message = new MqttMessage(payload);
        message.setQos(2); // only once, guaranteed
        message.setRetained(true);

        client.publish(topic, message);
        client.disconnect();
    }

    /**
     *
     * @param uri an URI with mqtt scheme, such as mqtt:tcp://hostname:1883 for unsecure or mqtt:ssl://hostname:8883 for secure connections
     * @return
     * @throws Exception
     */
    @Override
    public URI validate(URI uri) throws Exception {
        if (!uri.getScheme().equals("mqtt"))
            throw new Exception("protocol must be mqtt; is: "+uri.getScheme());

        new URI(uri.getRawSchemeSpecificPart());
        return uri;
    }

    @Override
    public void upload(Context ctx, URI full_uri, InputStream source, String dest_filename) throws Exception {
        byte[] payload = UploadUtils.readBytes(source);
        URI mqtt_uri = new URI(full_uri.getRawSchemeSpecificPart());
        String[] credentials = UploadUtils.getUsernameAndPasswordFromURI(mqtt_uri);
        URI broker_uri = new URI(mqtt_uri.getScheme(), null, mqtt_uri.getHost(), mqtt_uri.getPort(), mqtt_uri.getPath(), null, null);
        publish(broker_uri.toString(), CellScannerApp.MQTT_DEFAULT_TOPIC, credentials[0], credentials[1], payload);
    }
}
