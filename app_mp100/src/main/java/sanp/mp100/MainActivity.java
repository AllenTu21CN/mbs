package sanp.mp100;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.ArrayList;
import java.util.List;

import io.crossbar.autobahn.wamp.Client;
import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.types.CallResult;
import io.crossbar.autobahn.wamp.types.CloseDetails;
import io.crossbar.autobahn.wamp.types.SessionDetails;
import java8.util.concurrent.CompletableFuture;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String websocketURL = "ws://192.168.1.109:8080";
    // private static final String websocketURL = "ws://10.1.0.75:8080";
    private static final String realm = "device-hub.samples";
    private static final String procedureName = "sum";

    private TextView mMsg;
    private TextView mLog;
    private Button mSum;
    private String mLogs = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMsg = (TextView) findViewById(R.id.text_result);
        mLog = (TextView) findViewById(R.id.text_log);
        mSum = (Button) findViewById(R.id.btn_sum);
        mSum.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_sum:
                mSum.setEnabled(false);
                testSum();
                break;
        }
    }

    private void testSum() {
        Session session = new Session();
        session.addOnConnectListener(this::onConnectCallback);
        session.addOnJoinListener(this::onJoinCallback);
        session.addOnLeaveListener(this::onLeaveCallback);
        session.addOnDisconnectListener(this::onDisconnectCallback);

        // finally, provide everything to a Client instance and connect
        Client client = new Client(session, websocketURL, realm);
        client.connect();
    }

    private void Log(String line) {
        mLogs += line + "\n";
        mLog.setText(mLogs);
    }

    private void onConnectCallback(Session session) {
        Log("Session connected, ID=" + session.getID());
    }

    private void onJoinCallback(Session session, SessionDetails details) {
        /*
        CompletableFuture<Registration> regFuture = session.register(
                "add2", this::add2, null);
        regFuture.thenAccept(
                registration -> Log("Registered procedure: add2"));

        CompletableFuture<Subscription> subFuture = session.subscribe(
                "oncounter", this::onCounter, null);
        subFuture.thenAccept(subscription ->
                Log(String.format("Subscribed to topic: %s", subscription.topic)));
        */

        List<Object> args = new ArrayList<>();
        for(int i = 1; i < 4 ; ++i)
            args.add(i);
        CompletableFuture<CallResult> f =
                session.call(procedureName, args, (TypeReference<CallResult>) null);
//        f.thenAccept(result -> {
//            Log(String.format("Got result: %s, ", result.results.get(0)));
//            mMsg.setText((String) result.results.get(0));
//        });
//        f.exceptionally(throwable -> {
//            Log(String.format("ERROR - call failed: %s", throwable.getMessage()));
//            return null;
//        });
        f.whenComplete((callResult, throwable) -> {
            if (throwable == null) {
                Log(String.format("Got result: %s, ", callResult.results.get(0)));
                mMsg.setText((String) callResult.results.get(0));
            } else {
                Log(String.format("ERROR - call failed: %s", throwable.getMessage()));
            }
        });
    }

    private void onLeaveCallback(Session session, CloseDetails closeDetails) {
        Log(String.format("Left reason=%s, message=%s",
                closeDetails.reason, closeDetails.message));
    }

    private void onDisconnectCallback(Session session, boolean wasClean) {
        Log(String.format("Session with ID=%s, disconnected.", session.getID()));
    }

    /*
    private CompletableFuture<InvocationResult> add2(List<Object> args, InvocationDetails details) {
        int res = (int) args.get(0) + (int) args.get(1);
        List<Object> arr = new ArrayList<>();
        arr.add(res);
        arr.add(details.session.getID());
        arr.add("Java");
        return CompletableFuture.completedFuture(new InvocationResult(arr));
    }

    private void onCounter(List<Object> args) {
        Log(String.format(
                "oncounter event, counter value=%s from component %s (%s)",
                args.get(0), args.get(1), args.get(2)));
    }
    */
}
