package nl.nfi.cellscanner.upload;

import android.content.Context;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.InputStream;
import java.net.URI;

import nl.nfi.cellscanner.Preferences;

public class MqttUploader implements Uploader {
    /**
     *
     * @param uri an URI with mqtt scheme, such as mqtt:tcp://hostname:1883 for unsecure or mqtt:ssl://hostname:8883 for secure connections
     * @return
     * @throws Exception
     */
    @Override
    public URI validate(URI uri) throws Exception {
        new URI(uri.getRawSchemeSpecificPart());
        return uri;
    }

    @Override
    public void upload(Context ctx, URI uri, InputStream source, String dest_filename) throws Exception {
        String client_id = Preferences.getInstallID(ctx); // must be unique on server
        int qos = 2; // only once, guaranteed
        String topic = "cellscanner";
        MemoryPersistence persistence = new MemoryPersistence();

        byte[] content = UploadUtils.readBytes(source);

        MqttClient client = new MqttClient(uri.getSchemeSpecificPart(), client_id, persistence);
        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setCleanSession(true);
        client.connect(opts);
        MqttMessage message = new MqttMessage(content);
        message.setQos(qos);
        client.publish(topic, message);
        client.disconnect();
    }
}
