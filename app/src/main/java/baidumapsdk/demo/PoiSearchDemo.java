package baidumapsdk.demo;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.cloud.CloudListener;
import com.baidu.mapapi.cloud.CloudManager;
import com.baidu.mapapi.cloud.CloudPoiInfo;
import com.baidu.mapapi.cloud.CloudSearchResult;
import com.baidu.mapapi.cloud.DetailSearchResult;
import com.baidu.mapapi.cloud.NearbySearchInfo;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.Overlay;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.model.LatLngBounds;
import com.baidu.mapapi.overlayutil.DrivingRouteOverlay;
import com.baidu.mapapi.overlayutil.OverlayManager;
import com.baidu.mapapi.overlayutil.PoiOverlay;
import com.baidu.mapapi.overlayutil.TransitRouteOverlay;
import com.baidu.mapapi.overlayutil.WalkingRouteOverlay;
import com.baidu.mapapi.search.core.CityInfo;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.RouteLine;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiDetailSearchOption;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.mapapi.search.route.DrivingRouteLine;
import com.baidu.mapapi.search.route.DrivingRouteResult;
import com.baidu.mapapi.search.route.OnGetRoutePlanResultListener;
import com.baidu.mapapi.search.route.PlanNode;
import com.baidu.mapapi.search.route.RoutePlanSearch;
import com.baidu.mapapi.search.route.TransitRouteLine;
import com.baidu.mapapi.search.route.TransitRouteResult;
import com.baidu.mapapi.search.route.WalkingRouteLine;
import com.baidu.mapapi.search.route.WalkingRoutePlanOption;
import com.baidu.mapapi.search.route.WalkingRouteResult;
import com.baidu.mapapi.search.sug.OnGetSuggestionResultListener;
import com.baidu.mapapi.search.sug.SuggestionResult;
import com.baidu.mapapi.search.sug.SuggestionSearch;
import com.baidu.mapapi.map.MyLocationConfiguration.LocationMode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 演示poi搜索功能
 */
public class PoiSearchDemo extends FragmentActivity{
	private static final String TAG = "PoiSearchDemo";

	//UI相关组件
	TextView percentage ;
	Switch switchButton;
	//蓝牙相关
	//Bluetooth_service btService ;
	ServiceConnection conn;
	Bluetooth_service.myBinder myBinder;

	public Handler handler = new Handler() {
		public void handleMessage(Message msg){
			switch (msg.what){
				case Constants.MESSAGE_STATE_CHANGE:
					if(msg.arg1==-1){
						Toast.makeText(PoiSearchDemo.this,"blurtooth connection break",Toast.LENGTH_SHORT).show();
						if (conn != null) {
							unbindService(conn);
							conn = null;
						}
					}else
					Toast.makeText(PoiSearchDemo.this,"bluetooth connection status changed",Toast.LENGTH_SHORT).show();
					break;
//				case Constants.MESSAGE_DEVICE_NAME:
//					Toast.makeText(PoiSearchDemo.this,"scan bluetooth device overtime",Toast.LENGTH_SHORT).show();
//					break;
				case Constants.MESSAGE_READ:
					switch (msg.arg2){
						case 1:
							percentage.setText("Ack");
							break;
						case 2:
							try{
								percentage.setText(Integer.toString(msg.arg1));
							}catch (Exception e){
								e.printStackTrace();
							}
						default:
							break;
					}
					//Toast.makeText(PoiSearchDemo.this,new String((byte[])msg.obj),Toast.LENGTH_SHORT).show();
					break;
				case Constants.MESSAGE_SCAN_OVERTIME:
					Toast.makeText(PoiSearchDemo.this,"scan bluetooth device overtime",Toast.LENGTH_SHORT).show();
					break;
				default:
					break;
			}
		}
	};


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_poisearch);
		Log.v(TAG, "poi Starts");

		switchButton = (Switch)findViewById(R.id.switch1);
		switchButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked){
					myBinder.send_Char('a');
				}else{
					myBinder.send_Char('b');
				}
			}
		});
		/**
		 * 此处进行Bluetooth_service的绑定
		 * bindService需要ServiceConnection类作为参数
		 * 该类里的方法指定了绑定后和绑定解除后的操作
		 */
		Intent intent = new Intent(PoiSearchDemo.this,Bluetooth_service.class);
		conn = new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				Log.v(TAG, "onServiceConnected");
				myBinder = (Bluetooth_service.myBinder) service;
				myBinder.set_handler(handler);
				myBinder.start_Bluetooth();
			}

			@Override
			public void onServiceDisconnected(ComponentName name) {
				myBinder = null;
				Log.v(TAG, "onServiceDisconnected");
			}
		};
		bindService(intent , conn , BIND_AUTO_CREATE);

		//初始化UI相关组件
		Log.v(TAG, "init component");
	}


	@Override
	protected void onPause() {
		super.onPause();
		if (conn != null) {
			unbindService(conn);
			conn = null;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
//		if(btService != null){
//			if(btService.getState()==Bluetooth_service.STATE_NONE){
//				btService.start();
//			}
//		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (conn != null) {
			unbindService(conn);
			conn = null;
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
	}

}

