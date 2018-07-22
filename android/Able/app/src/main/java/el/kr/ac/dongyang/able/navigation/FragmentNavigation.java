package el.kr.ac.dongyang.able.navigation;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.skt.Tmap.TMapData;
import com.skt.Tmap.TMapGpsManager;
import com.skt.Tmap.TMapMarkerItem;
import com.skt.Tmap.TMapPOIItem;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapPolyLine;
import com.skt.Tmap.TMapTapi;
import com.skt.Tmap.TMapView;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.support.v4.app.FragmentTransaction;

import el.kr.ac.dongyang.able.BusProvider;
import el.kr.ac.dongyang.able.R;
import el.kr.ac.dongyang.able.friend.FragmentUserlist;


/**
 * Created by impro on 2018-03-30.
 * 지도 맵 띄움.
 * 출발지 포인트랑 목적지 포인트 받으면 라인이랑 마커 띄움
 * 지도 레벨이 변경될때 마커 크기 유동적으로 바뀌도록 만들어야함
 * 돋보기 버튼 누르면 프래그먼트네비리스트 를 띄움 - 차일드 프래그먼트
 */
public class FragmentNavigation extends android.support.v4.app.Fragment {

    WebView web;
    private Handler mHandler = new Handler();
    double latitude;
    double longitude;
    String fragmentTag;
    FragmentTransaction ft;
    String bussett;
    Double startlist[] = new Double[2];

    private Context mContext = null;
    private boolean m_bTrackingMode = true;
    private TMapGpsManager tmapgps = null;
    private TMapView tMapView = null;
    private static String mApiKey = "2bcf226b-36b6-49da-82cc-5f00acee90a2"; // 발급받은 appKey
    private static int mMarkerID;
    Fragment FragmentNaviList;
    FragmentTransaction transaction;

    private ArrayList<String> mArrayMarkerID = new ArrayList<String>();

    public ArrayList<TMapPOIItem> startList = new ArrayList<TMapPOIItem>();
    public ArrayList<String> naviList = new ArrayList<String>();
    public ArrayList<TMapPOIItem> endList = new ArrayList<TMapPOIItem>();
    public List<String> busset = new ArrayList<>();

    private static final String LOG_TAG = "FragmentNavigation";

    Button nSearchlist;

    private Bus busProvider = BusProvider.getInstance();
    Timer t = new Timer(true);

    public FragmentNavigation() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BusProvider.getInstance().register(this);
    }

    @Nullable
    @Override
    @SuppressLint("JavascriptInterface")
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_navigation,container,false);
        getActivity().setTitle("Navigation");

        TMapTapi tmaptapi = new TMapTapi(getActivity());
        tmaptapi.setSKTMapAuthentication ("2414ee00-3784-4c78-913d-32bf5fa9c107");

        //Button button1 = (Button) findViewById(R.id.button1);
        RelativeLayout rl = view.findViewById(R.id.searchLayout);
        EditText et1 = view.findViewById(R.id.naviLocation);
        rl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*FragmentNaviList = new FragmentNaviList();
                transaction = getChildFragmentManager().beginTransaction();
                transaction.add(R.id.child_fragment_container, FragmentNaviList)
                        .addToBackStack("navilist").commit();*/
                Fragment fragment = new FragmentNaviList();
                fragmentTag = fragment.getClass().getSimpleName();  //FragmentLogin
                Log.i("fagmentTag", fragmentTag);
                getActivity().getSupportFragmentManager().popBackStack(fragmentTag, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                ft=getActivity().getSupportFragmentManager().beginTransaction();
                ft.add(R.id.main_layout, fragment);
                ft.addToBackStack(fragmentTag);
                ft.commit();

            }
        });

        web = (WebView)view.findViewById(R.id.web);
        web.setWebViewClient(new WebViewClient());
        WebSettings webSet = web.getSettings();
        webSet.setJavaScriptEnabled(true);
        webSet.setUseWideViewPort(true);
        webSet.setBuiltInZoomControls(false);
        webSet.setAllowUniversalAccessFromFileURLs(true);
        webSet.setJavaScriptCanOpenWindowsAutomatically(true);
        webSet.setSupportMultipleWindows(true);
        webSet.setSaveFormData(false);
        webSet.setSavePassword(false);
        webSet.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient()); //페이지 로딩을 마쳤을 경우 작업
        web.loadUrl("file:///android_asset/index.html"); //웹뷰로드
        //layout.addView(web);
        //setContentView(layout);

        web.setHorizontalScrollBarEnabled(false);
        web.setVerticalScrollBarEnabled(false);

        web.getSettings().setLoadWithOverviewMode(true);
        web.getSettings().setUseWideViewPort(true);

        web.getSettings().setJavaScriptEnabled(true); //자바스크립트 허용
        web.addJavascriptInterface(new TMapBridge(),"tmap");

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        //내 gps를 계속 전달. log에 뜸
        setGps();

        //현 위치의 다음 노드의 좌표값을 라즈베리로 넘김.
        double latitude = 37.50727379276449;
        int latitudeint = (int)(latitude * 100000);
        double longitude = 126.88502567374903;
        int longitudeint = (int)(longitude * 100000);
        String nextPoint = "";

        Log.d("geo", "lat : " + Double.toString(latitude) + "lon : " + Double.toString(longitude));
        //web.loadUrl("javascript:setXY('s','\" + latitude + \"', '\" + longitude + \"')");
        //web.loadUrl("javascript:geoLo('" + latitude + "', '" + longitude + "')");

        if(!naviList.isEmpty()) {
            String startPoint = "";
            //처음 한번만
            if(startPoint.isEmpty()){
                try {
                    startPoint = naviList.get(0);
                    busProvider.post(startPoint);
                    Thread.sleep(1000);
                }   catch (InterruptedException e) {
                }
                /*for(int i=1; i< naviList.size(); i++) {
                    try {
                        BusProvider.getInstance().post(naviList.get(i));
                        Thread.sleep(4000);
                    } catch (InterruptedException e) {
                    }
                }*/
            }
            for(int i=0; i<naviList.size(); i++) {

                String lonandlat = naviList.get(i);
                String target = "lon=";
                int target_num = lonandlat.indexOf(target);
                String result;
                result = lonandlat.substring(target_num, (lonandlat.indexOf(",")));
                String resultlon = result.substring(4);

                String lonandlat2 = naviList.get(i);
                String target2 = "lat=";
                int target_num2 = lonandlat2.indexOf(target2);
                String result2;
                result2 = lonandlat2.substring(target_num2);
                String resultlat = result2.substring(4);

                Log.d("처음 lon : ", resultlon);
                Log.d("처음 lat : ", resultlat);

                int resultlat2p = (int)((Double.parseDouble(resultlat.substring(0,7))+0.00050)*100000);
                int resultlat2m = (int)((Double.parseDouble(resultlat.substring(0,7))-0.00050)*100000);

                int resultlon2p = (int)((Double.parseDouble(resultlon.substring(0,8))+0.00050)*100000);
                int resultlon2m = (int)((Double.parseDouble(resultlon.substring(0,8))-0.00050)*100000);

                if(latitudeint >= resultlat2m && latitudeint <= resultlat2p ){
                    if(longitudeint >= resultlon2m && longitudeint <= resultlon2p ) {
                        nextPoint = naviList.get(i+1);
                        //BusProvider.getInstance().post(nextPoint);
                        busProvider.post(nextPoint);
                        Log.d("otto_lonlat : ", "" + nextPoint);
                        break;
                    }
                }
            }
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        //String bus = getPost();
    }

    private class TMapBridge{
        int i = 0;
        @JavascriptInterface
        public void setMessage(final String arg) {
            mHandler.post(new Runnable() {
                public void run() {
                    naviList.add(i + " : " + arg + "\n");
                    //Log.d("index - ", naviList.toString());
                    i+=1;
                }
            });
            //Log.d("index - ", naviList.toString());
        }
    }

    /*본인 gps 얻어서 맵의 메인에 넣어주는 코드*/
    private final LocationListener mLocationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            if (location != null) {
                double latitude_r = location.getLatitude();
                double longitude_r = location.getLongitude();
                String lonlat = "";
                startlist[0] = longitude_r;
                startlist[1] = latitude_r;
                Log.d("geo", "lat : " + Double.toString(latitude_r) + "lon : " + Double.toString(longitude_r));
                //현재위치 마커생성했는데 계속 변하는건 맞지

                web.loadUrl("javascript:geoLo('" + latitude_r + "', '" + longitude_r + "')");
                /*
                if(!naviList.isEmpty()) {
                    for(int i=0; i<naviList.size(); i++) {

                        String lonandlat = naviList.get(i);
                        String target = "lon=";
                        int target_num = lonandlat.indexOf(target);
                        String result;
                        result = lonandlat.substring(target_num, (lonandlat.indexOf(",")));
                        String resultlon = result.substring(4);

                        String lonandlat2 = naviList.get(i);
                        String target2 = "lat=";
                        int target_num2 = lonandlat2.indexOf(target2);
                        String result2;
                        result2 = lonandlat2.substring(target_num2);
                        String resultlat = result2.substring(4);

                        Log.d("처음 lon : ", resultlon);
                        Log.d("처음 lat : ", resultlat);

                        if(latitude >= Double.parseDouble(resultlat.substring(0,8))-0.00050 && latitude <= Double.parseDouble(resultlat.substring(0,8))+0.00050 ){
                            if(longitude >= Double.parseDouble(resultlon.substring(0,8))-0.00050 && longitude <= Double.parseDouble(resultlat.substring(0,8))+0.00050 ) {
                                lonlat = resultlon + "," + resultlat;
                                BusProvider.getInstance().post(lonlat);
                                Log.d("otto_lonlat : ", "" + lonlat);
                                break;
                            }
                        }
                    }
                }*/
            }
        }
        public void onProviderDisabled(String provider) {
        }
        public void onProviderEnabled(String provider) {
        }
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };

    public void setGps() {
        final LocationManager lm = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, // 등록할 위치제공자(실내에선 NETWORK_PROVIDER 권장)
                1000, // 통지사이의 최소 시간간격 (miliSecond)
                1, // 통지사이의 최소 변경거리 (m)
                mLocationListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getActivity().setTitle("Able");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        BusProvider.getInstance().unregister(this);
    }

    @Subscribe
    public void getPost(String msg){
        Log.d("otto_lonlat_set : ", ""+msg);
        bussett = msg;
        String[] msglist = bussett.split(",");
        Double[] endlist = {Double.parseDouble(msglist[0]), Double.parseDouble(msglist[1])};
        Log.d("list", startlist[0] + " " + startlist[1] + " " + endlist[0] + " " + endlist[1]);
        web.loadUrl("javascript:distance('" + startlist[0] + "', '" + startlist[1] + "', '" + endlist[0] + "', '" + endlist[1] + "')");
        Log.d("setbus : ", bussett.toString());
        }
}