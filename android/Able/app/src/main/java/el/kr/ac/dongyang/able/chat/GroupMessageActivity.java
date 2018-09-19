package el.kr.ac.dongyang.able.chat;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import el.kr.ac.dongyang.able.BaseActivity;
import el.kr.ac.dongyang.able.R;
import el.kr.ac.dongyang.able.model.ChatModel;
import el.kr.ac.dongyang.able.model.NotificationModel;
import el.kr.ac.dongyang.able.model.UserModel;
import el.kr.ac.dongyang.able.navigation.NavigationActivity;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GroupMessageActivity extends BaseActivity {
    Map<String, UserModel> users = new HashMap<>();
    String destinationRoom;
    String uid;
    EditText editText;
    Button groupRidingMapBtn;

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA);
    private RecyclerView recyclerView;

    List<ChatModel.Comment> comments = new ArrayList<>();

    int peopleCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_message);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        groupRidingMapBtn = findViewById(R.id.groupRidingBtn);
        groupRidingMapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(GroupMessageActivity.this, NavigationActivity.class);
                intent.putExtra("clickBtn", "share");
                intent.putExtra("uid", uid);
                intent.putExtra("destinationRoom", destinationRoom);
                startActivity(intent);
            }
        });

        destinationRoom = getIntent().getStringExtra("destinationRoom");
        uid = firebaseUser.getUid();
        editText = findViewById(R.id.groupMessageActivity_editText);
        recyclerView = findViewById(R.id.groupMessageActivity_recyclerview);

        reference.child("USER").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot item : dataSnapshot.getChildren()) {
                    users.put(item.getKey(), item.getValue(UserModel.class));
                }

                Button button = findViewById(R.id.groupMessageActivity_button);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ChatModel.Comment comment =
                                new ChatModel.Comment(uid, editText.getText().toString(), ServerValue.TIMESTAMP, false);
                        //채팅 보낼때마다 디비 저장
                        reference.child("CHATROOMS").child(destinationRoom).child("comments").push().setValue(comment).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                //gcm 전송
                                sendGcmUsers();
                            }
                        });
                    }
                });
                recyclerView.setAdapter(new GroupMessageRecyclerViewAdapter());
                recyclerView.setLayoutManager(new LinearLayoutManager(GroupMessageActivity.this));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void sendGcmUsers() {
        reference.child("CHATROOMS")
                .child(destinationRoom)
                .child("users")
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Map<String, Boolean> map = (Map<String, Boolean>) dataSnapshot.getValue();
                for (String item : map.keySet()) {
                    if (item.equals(uid)) {
                        continue;
                    }
                    gcmSetting(users.get(item).getPushToken());
                }
                editText.setText("");
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    void gcmSetting(String pushToken) {

        Gson gson = new Gson();

        String userName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        NotificationModel notificationModel = new NotificationModel();
        notificationModel.to = pushToken;
        notificationModel.notification.title = userName;
        //그룹라이딩 지도 넣으면 종료됨
        notificationModel.notification.text = editText.getText().toString();
        notificationModel.data.title = userName;
        notificationModel.data.text = editText.getText().toString();

        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf8"), gson.toJson(notificationModel));

        Request request = new Request.Builder()
                .header("Content-Type", "application/json")
                .addHeader("Authorization", "key=AIzaSyAXArVX1TeAhf2L9MNlTuKgumJgPK1Y0BU")
                .url("https://gcm-http.googleapis.com/gcm/send")
                .post(requestBody)
                .build();
        OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
            }
        });
    }

    class GroupMessageRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        public GroupMessageRecyclerViewAdapter() {
            getMessageList();
        }

        void getMessageList() {
            reference.child("CHATROOMS").child(destinationRoom).child("comments").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    comments.clear();
                    Map<String, Object> readUsersMap = new HashMap<>();
                    for (DataSnapshot item : dataSnapshot.getChildren()) {
                        String key = item.getKey();
                        ChatModel.Comment comment_origin = item.getValue(ChatModel.Comment.class);
                        ChatModel.Comment comment_modify = item.getValue(ChatModel.Comment.class);
                        if (comment_modify != null) {
                            comment_modify.readUsers.put(uid, true);
                        }

                        readUsersMap.put(key, comment_modify);
                        comments.add(comment_origin);
                    }
                    if (comments.size() == 0) {
                    } else {
                        if (!comments.get(comments.size() - 1).readUsers.containsKey(uid)) {
                            reference.child("CHATROOMS").child(destinationRoom).child("comments").updateChildren(readUsersMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    notifyDataSetChanged();
                                    recyclerView.scrollToPosition(comments.size() - 1);
                                }
                            });
                        } else {
                            notifyDataSetChanged();
                            recyclerView.scrollToPosition(comments.size() - 1);
                        }
                        //메세지가 갱신
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }


        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);

            return new GroupMessageViewHodler(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            GroupMessageViewHodler messageViewHolder = ((GroupMessageViewHodler) holder);
            ChatModel.Comment comment = comments.get(position);
            //내가보낸 메세지
            if (comment.uid.equals(uid)) {
                messageViewHolder.textView_message.setText(comment.message);
                messageViewHolder.linearLayout_text_btn.setBackgroundResource(R.drawable.rightbubble);
                messageViewHolder.linearLayout_destination.setVisibility(View.INVISIBLE);
                messageViewHolder.textView_message.setTextSize(20);
                messageViewHolder.linearLayout_main.setGravity(Gravity.RIGHT);
                setReadCounter(position, messageViewHolder.textView_readCounter_left);
            } else {
                //상대방이 보낸 메세지
                Glide
                        .with(holder.itemView.getContext())
                        .load(users.get(comment.uid).getProfileImageUrl())
                        .apply(new RequestOptions().circleCrop())
                        .into(messageViewHolder.imageView_profile);
                messageViewHolder.textView_name.setText(users.get(comment.uid).getUserName());
                messageViewHolder.linearLayout_destination.setVisibility(View.VISIBLE);
                messageViewHolder.linearLayout_text_btn.setBackgroundResource(R.drawable.leftbubble);
                messageViewHolder.textView_message.setText(comment.message);
                messageViewHolder.textView_message.setTextSize(20);
                messageViewHolder.linearLayout_main.setGravity(Gravity.LEFT);
                setReadCounter(position, messageViewHolder.textView_readCounter_right);
            }

            if(comment.naviShare) {
                messageViewHolder.groupRidingStartBtn.setVisibility(View.VISIBLE);
                messageViewHolder.groupRidingStartBtn.setGravity(Gravity.CENTER);
            }

            long unixTime = (long) comment.timestamp;
            Date date = new Date(unixTime);
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
            String time = simpleDateFormat.format(date);
            messageViewHolder.textView_timestamp.setText(time);


        }

        void setReadCounter(final int position, final TextView textView) {
            if (peopleCount == 0) {
                reference.child("CHATROOMS").child(destinationRoom).child("users")
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Map<String, Boolean> users = (Map<String, Boolean>) dataSnapshot.getValue();
                        peopleCount = users.size();
                        int count = peopleCount - comments.get(position).readUsers.size();
                        if (count > 0) {
                            textView.setVisibility(View.VISIBLE);
                            textView.setText(String.valueOf(count));
                        } else {
                            textView.setVisibility(View.INVISIBLE);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            } else {
                int count = peopleCount - comments.get(position).readUsers.size();
                if (count > 0) {
                    textView.setVisibility(View.VISIBLE);
                    textView.setText(String.valueOf(count));
                } else {
                    textView.setVisibility(View.INVISIBLE);
                }
            }

        }

        @Override
        public int getItemCount() {
            return comments.size();
        }

        public class GroupMessageViewHodler extends RecyclerView.ViewHolder {

            TextView textView_message;
            TextView textView_name;
            ImageView imageView_profile;
            LinearLayout linearLayout_destination;
            LinearLayout linearLayout_main;
            LinearLayout linearLayout_text_btn;
            TextView textView_timestamp;
            TextView textView_readCounter_left;
            TextView textView_readCounter_right;
            Button groupRidingStartBtn;

            GroupMessageViewHodler(View view) {
                super(view);

                textView_message = view.findViewById(R.id.messageItem_textView_message);
                textView_name = view.findViewById(R.id.messageItem_textview_name);
                imageView_profile = view.findViewById(R.id.messageItem_imageview_profile);
                linearLayout_destination = view.findViewById(R.id.messageItem_linearlayout_destination);
                linearLayout_main = view.findViewById(R.id.messageItem_linearlayout_main);
                linearLayout_text_btn = view.findViewById(R.id.messageItem_linearlayout_msg_btn);
                textView_timestamp = view.findViewById(R.id.messageItem_textview_timestamp);
                textView_readCounter_left = view.findViewById(R.id.messageItem_textview_readCounter_left);
                textView_readCounter_right = view.findViewById(R.id.messageItem_textview_readCounter_right);
                groupRidingStartBtn = view.findViewById(R.id.groupRidingStartBtn);
                groupRidingStartBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        reference.child("CHATROOMS").child(destinationRoom).child("groupUsers").child(uid).setValue(true);
                        ChatModel.Comment comment = comments.get(getAdapterPosition());
                        Intent intent = new Intent(GroupMessageActivity.this, NavigationActivity.class);
                        intent.putExtra("clickBtn", "shareStart");
                        intent.putExtra("comment", comment);
                        intent.putExtra("uid", uid);
                        intent.putExtra("destinationRoom", destinationRoom);
                        startActivity(intent);
                    }
                });
            }
        }
    }
}
