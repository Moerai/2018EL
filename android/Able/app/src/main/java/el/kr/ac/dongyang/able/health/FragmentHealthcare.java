package el.kr.ac.dongyang.able.health;


import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import com.github.lzyzsd.circleprogress.ArcProgress;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.shrikanthravi.collapsiblecalendarview.data.Day;
import com.shrikanthravi.collapsiblecalendarview.widget.CollapsibleCalendar;

import java.lang.String;

import el.kr.ac.dongyang.able.R;
import el.kr.ac.dongyang.able.model.HealthModel;
import el.kr.ac.dongyang.able.model.UserModel;

/**
 * Created by user on 2018-05-13.
 * <p>
 * 클릭시 데이터가 있으면 데이터있는 화면을 표시
 * 데이터가 없으면 오늘은 운동을 안했다고 표시
 *
 * <p>
 * 몸무게 받아와서 칼로리 계산은 성공.
 */

public class FragmentHealthcare extends android.support.v4.app.Fragment{
    private static final String LOG_TAG = "FragmentNavigation";

    ConstraintLayout constraintLayoutHealth, constraintLayoutNone;

    String date, uid, cal2;
    TextView speedTextView, kcalTextView, distanceTextView;
    FirebaseUser user;
    UserModel userModel;
    HealthModel healthModel;

    ArcProgress arcProgress;

    DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
    private Handler mHandler;

    public FragmentHealthcare() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_healthcare, container, false);
        getActivity().setTitle("Health care");

        constraintLayoutHealth = view.findViewById(R.id.constraintLayoutHealth);
        constraintLayoutHealth.setVisibility(View.GONE);
        constraintLayoutNone = view.findViewById(R.id.constraintLayoutNone);
        constraintLayoutNone.setVisibility(View.GONE);

        arcProgress = view.findViewById(R.id.arc_progress);
        mHandler = new Handler();

        //칼로리 부분

        speedTextView = view.findViewById(R.id.speed_text);
        kcalTextView = view.findViewById(R.id.burnUpTextView);
        distanceTextView = view.findViewById(R.id.distanceTextView);

        user = FirebaseAuth.getInstance().getCurrentUser();
        if(user != null){
            uid = user.getUid();
        }
        userModel = new UserModel();
        healthModel = new HealthModel();

        final CollapsibleCalendar collapsibleCalendar = view.findViewById(R.id.collapsibleCalendarView);
        collapsibleCalendar.setState(0);
        collapsibleCalendar.setCalendarListener(new CollapsibleCalendar.CalendarListener() {
            @Override
            public void onDaySelect() {
                Day day = collapsibleCalendar.getSelectedDay();
                final String dayFormat = day.getYear() + "-" + (day.getMonth() + 1) + "-" + day.getDay();

                collapsibleCalendar.collapse(0);

                FirebaseDatabase.getInstance().getReference().child("HEALTH").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                    HealthModel healthModel = new HealthModel();
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                            healthModel = dataSnapshot.child(dayFormat).getValue(HealthModel.class);

                            if(healthModel != null) {
                                constraintLayoutNone.setVisibility(View.GONE);
                                constraintLayoutHealth.setVisibility(View.VISIBLE);
                                kcalTextView.setText(healthModel.getKcal());
                                distanceTextView.setText(healthModel.getDistance());
                                speedTextView.setText(healthModel.getSpeed());

                                Thread t = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                //수치가 올라가는 코드
                                                ObjectAnimator anim = ObjectAnimator.ofInt(arcProgress, "progress", 0, 50);
                                                anim.setInterpolator(new DecelerateInterpolator());
                                                anim.setDuration(500);
                                                anim.start();

                                                //페이드인 되는 코드
                                                /*AnimatorSet set = (AnimatorSet) AnimatorInflater.loadAnimator(getActivity(), R.animator.progress_anim);
                                                set.setInterpolator(new DecelerateInterpolator());
                                                set.setTarget(arcProgress);
                                                set.start();*/
                                            }
                                        });
                                    }
                                });
                                t.start();
                            } else {
                                constraintLayoutHealth.setVisibility(View.GONE);
                                constraintLayoutNone.setVisibility(View.VISIBLE);
                            }
                        }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.d("databaseError", databaseError.getMessage());
                    }
                });
            }

            @Override
            public void onItemClick(View view) {

            }
            @Override
            public void onDataUpdate() {

            }
            @Override
            public void onMonthChange() {

            }
            @Override
            public void onWeekChange(int i) {

            }
        });

        return view;
    }

    public void setdate(String isdate){
        if (user != null) {
            // User is signed in
            if (mDatabase.child("HEALTH").child(uid).getKey() != null) {
                mDatabase.child("HEALTH").child(uid).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        healthModel = dataSnapshot.child(date).getValue(HealthModel.class);
                        if (healthModel != null) {
                            kcalTextView.setText(healthModel.getKcal() + "kcal");
                            speedTextView.setText(healthModel.getSpeed() + "km");
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
            } else {
            }
        } else {
            // No user is signed in
        }
    }
    private void handleMessage(Message msg) { // 핸들러가 gui수정
        kcalTextView.setText(cal2 + "kcal");
        speedTextView.setText(speedTextView + "km");
        Log.d(LOG_TAG, "mhcal2:  " + cal2+"mhspeed: "+cal2);
    }
    public void onStart() {
        super.onStart();

        if(user != null) {
            mDatabase.child("HEALTH").child(uid).child("2018-08-13").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    healthModel = dataSnapshot.getValue(HealthModel.class);
                    if (healthModel != null) {
                        cal2 = healthModel.getKcal();
                        Log.d(LOG_TAG, "cal2:  " + cal2 + "speed: " + cal2);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.d("databaseError", databaseError.getMessage());
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getActivity().setTitle("Able");
    }
}
