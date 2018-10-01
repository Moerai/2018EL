package el.kr.ac.dongyang.able;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

//코드의 중복을 줄이고 로딩화면을 호출하는 메소드를 정의한 Fragment
public class BaseFragment extends Fragment {

    //데이버베이스 레퍼런스
    public DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

    //화면에 보여지는 프래그먼트 변경
    public void replaceFragment(Fragment fragment) {
        String fragmentTag;
        FragmentManager manager = getActivity().getSupportFragmentManager();
        FragmentTransaction ft;

        if (!fragment.isVisible()) {
            fragmentTag = fragment.getClass().getSimpleName();
            manager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            ft = manager.beginTransaction()
                    .replace(R.id.main_layout, fragment, fragmentTag)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .addToBackStack(null);
            ft.commit();
        }
    }

    //로딩화면 on
    public void progressOn() {
        BaseApplication.getInstance().progressOn(getActivity(), null);
    }
    public void progressOn(String message) {
        BaseApplication.getInstance().progressOn(getActivity(), message);
    }
    //로딩화면 off
    public void progressOff() {
        BaseApplication.getInstance().progressOff();
    }
}
