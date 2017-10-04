package com.example.ble_test;

import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.hardware.Sensor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.app.Activity;
import android.bluetooth.*;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.UUID;

//インプリメントとはインターフェイスの実装クラスを宣言するための予約語
//「class クラス名 implements インターフェイス名
//	interface→実装のないクラス。フィールドを持たず、実装のないメソッドを持つ。interfaceという予約語で宣言する。
//  メソッドが実装されていないため、インターフェイス単体では何の機能も持たない。implementsという予約語を使用して
// 「継承」のように実装クラスを作り、インターフェイスのメソッドを「オーバーライド」するように実装する。
//「実装」メソッドの「本体」。メソッドの宣言直後にある{}で囲まれた箇所を「メソッドの実装」と呼ぶ。
//メソッド→　クラス内に「戻り値の型 メソッド名( 引数の型 引数の変数名 ){ 実装 }」という書式でメソッドを作ることができる。
public class MainActivity extends Activity implements BluetoothAdapter.LeScanCallback {
//    BluetoothAdapterクラスのLeScanCallbackインターフェイスを実装
    /** BLE 機器スキャンタイムアウト (ミリ秒) */
    private static final long SCAN_PERIOD = 10000;
    /** 検索機器の機器名 */
    private static final String DEVICE_NAME = "Sensor";
    /** 対象のサービスUUID */
    private static final String DEVICE_BUTTON_SENSOR_SERVICE_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb";
    /** 対象のキャラクタリスティックUUID */
    private static final String DEVICE_BUTTON_SENSOR_CHARACTERISTIC_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb";
    /** キャラクタリスティック設定UUID */
    private static final String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    private static final String TAG = "BLE_Test";
    private BleStatus mStatus = BleStatus.DISCONNECTED;
    private Handler mHandler;
    private BluetoothAdapter mBluetoothAdapter;
//    下記二つのクラスをインポートしたからそれに対して宣言をする
//   import android.bluetooth.BluetoothAdapter;
//   import android.bluetooth.BluetoothManager;
//  クラスをインポートしてインスタンス化するときの名前の付け方のパターン→「mクラス名」が一般的なのかもしれない
    private BluetoothManager mBluetoothManager;
    private BluetoothGatt mBluetoothGatt;
    private TextView mStatusText;

    @Override
// サブクラスでメソッドの上書きをする機能。
// サブクラスでスーパークラスと同じメソッド名、引数、戻り値のメソッドを定義することで、スーパークラスのメソッドではなく、サブクラスのメソッドが呼ばれるようにする機能
    public void onCreate(Bundle savedInstanceState) {
//              onCreateメソッドの引数Bundle savedInstanceStateの意味はactivityが再構築されるときに入れる決まりのもの
        super.onCreate(savedInstanceState);
//      スーパークラスにあるonCreateメソッドを最初に呼び出して、スーパークラス側の処理を全部済ませてから、自分の処理を行うようにしているのです。それが、super.onCreate……という部分
        setContentView(R.layout.activity_main);
//      setContentView()メソッドは、文字列などのViewを画面(Activity)に追加します

        mBluetoothManager = (BluetoothManager)getSystemService(BLUETOOTH_SERVICE);
//      getSystemService(BLUETOOTH_SERVICE);→Object getSystemService (String name)_システムレベルのサービスのハンドルを取得するメソッドです。
//      object→すべてのクラスはスーパークラスのオブジェクト
//      BluetoothManagerはここではObjectなのか？
        mBluetoothAdapter = mBluetoothManager.getAdapter();
//      mBluetoothManagerについてのみ更新をかける意味のようだ

        findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {
//            button.setOnClickListener(new OnClickListener() {
//            findViewByIdで特定したオブジェクト→ボタンのbtn_connectに対してクリック処理を設定
//            Viewクラスの抽象クラスOnClickListener()を設定
//            ”new”が何を表すのか一言で言うとメモリ内に変数（コンストラクタ）を扱うための領域（オブジェクト領域）を作成します。
//            new=コンストラクタの作成
//            id="@+id/名前"でレイアウトファイルの中でIDが設定できる
            @Override
            public void onClick(View v) {
//                onClickメソッドはView型の引数をとり、その変数名をvとするということです。
//                戻り型　メソッド名　（引数型　引数名）　｛メソッド本体 ｝；
                connect();
//                メソッドconnectの呼び出し
            }
        });
        findViewById(R.id.btn_disconnect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnect();
            }
        });

        mStatusText = (TextView)findViewById(R.id.text_status);

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                mStatusText.setText(((BleStatus) msg.obj).name());
            }
        };
    }

    /** BLE機器を検索する */
    private void connect() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mBluetoothAdapter.stopLeScan(MainActivity.this);
                if (BleStatus.SCANNING.equals(mStatus)) {
                    setStatus(BleStatus.SCAN_FAILED);
                }
            }
        }, SCAN_PERIOD);

        mBluetoothAdapter.stopLeScan(this);
        mBluetoothAdapter.startLeScan(this);
        setStatus(BleStatus.SCANNING);
    }

    /** BLE 機器との接続を解除する */
    private void disconnect() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
            setStatus(BleStatus.CLOSED);
        }
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        Log.d(TAG, "device found: " + device.getName());
        if (DEVICE_NAME.equals(device.getName())) {
            setStatus(BleStatus.DEVICE_FOUND);

// 省電力のためスキャンを停止する
            mBluetoothAdapter.stopLeScan(this);

// GATT接続を試みる
            mBluetoothGatt = device.connectGatt(this, false, mBluetoothGattCallback);
        }
    }

    private final BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(TAG,"onConnectionStateChange:" + status + " -> " + newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
// GATTへ接続成功
// サービスを検索する
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
// GATT通信から切断された
                setStatus(BleStatus.DISCONNECTED);
                mBluetoothGatt = null;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "onServicesDiscovered received: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(UUID.fromString(DEVICE_BUTTON_SENSOR_SERVICE_UUID));
                if (service == null) {
// サービスが見つからなかった
                    setStatus(BleStatus.SERVICE_NOT_FOUND);
                } else {
// サービスを見つけた
                    setStatus(BleStatus.SERVICE_FOUND);

                    BluetoothGattCharacteristic characteristic =
                            service.getCharacteristic(UUID.fromString(DEVICE_BUTTON_SENSOR_CHARACTERISTIC_UUID));

                    if (characteristic == null) {
// キャラクタリスティックが見つからなかった
                        setStatus(BleStatus.CHARACTERISTIC_NOT_FOUND);
                    } else {
// キャラクタリスティックを見つけた

// Notification を要求する
                        boolean registered = gatt.setCharacteristicNotification(characteristic, true);

// Characteristic の Notification 有効化
                        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                                UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(descriptor);

                        if (registered) {
// Characteristics通知設定完了
                            setStatus(BleStatus.NOTIFICATION_REGISTERED);
                        } else {
                            setStatus(BleStatus.NOTIFICATION_REGISTER_FAILED);
                        }
                    }
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            Log.d(TAG, "onCharacteristicRead: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
// READ成功
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "onCharacteristicChanged");
// Characteristicの値更新通知

            if (DEVICE_BUTTON_SENSOR_CHARACTERISTIC_UUID.equals(characteristic.getUuid().toString())) {
                Byte value = characteristic.getValue()[0];
                boolean left = (0 < (value & 0x02)); boolean right = (0 < (value & 0x01)); updateButtonState(left, right); } } };
    private void updateButtonState(final boolean left, final boolean right) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                View leftView = findViewById(R.id.left);
                View rightView = findViewById(R.id.right);
                leftView.setBackgroundColor( (left ? Color.BLUE : Color.TRANSPARENT) );
                rightView.setBackgroundColor( (right ? Color.BLUE : Color.TRANSPARENT) );
            }
        });
    }
    private void setStatus(BleStatus status) {
        mStatus = status; mHandler.sendMessage(status.message());
    }

    private enum BleStatus {
        DISCONNECTED, SCANNING, SCAN_FAILED, DEVICE_FOUND,
        SERVICE_NOT_FOUND, SERVICE_FOUND, CHARACTERISTIC_NOT_FOUND,
        NOTIFICATION_REGISTERED, NOTIFICATION_REGISTER_FAILED, CLOSED ;

        public Message message() {
        Message message = new Message();
        message.obj = this;
        return message;
        }
    }

}