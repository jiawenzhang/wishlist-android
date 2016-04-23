/**
 * Created by jiawen on 2016-04-23.
 */

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.RenamingDelegatingContext;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SignUpCallback;
import com.wish.wishlist.R;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.model.WishItemManager;
import com.wish.wishlist.util.Tester;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ParseServerTest {

    private static final String TAG = "ParseServerTest";
    Context mMockContext;
    static public CountDownLatch mSignal = null;

    @Before
    public void setUp() {
        mMockContext = new RenamingDelegatingContext(InstrumentationRegistry.getInstrumentation().getTargetContext(), "test_");
        mSignal = new CountDownLatch(1);
    }

    @Test
    public void User() throws InterruptedException {
        Log.d(TAG, "User");
        final ParseUser user = new ParseUser();
        Log.d(TAG, "User created");

        // Set standard fields
        final String username = "username";
        user.setUsername(username);
        user.setPassword("123456");
        user.setEmail("user@test.com");
        user.signUpInBackground(new SignUpCallback() {
            @Override
            public void done(ParseException e) {
                Log.d(TAG, "done");
                if (e == null) {
                    if (user.getBoolean("emailVerified")) {
                        Log.d(TAG, "signup success");
                        //loadingFinish();
                        //signupSuccess();
                    } else {
                        String message = "Please check " + username + " to verify your email. Once verified, you can login and enjoy:";
                        Log.d(TAG, "signup failed");
                        //verifyEmail(message);
                    }
                } else {
                    //loadingFinish();
                    Log.d(TAG, mMockContext.getString(R.string.com_parse_ui_login_warning_parse_signup_failed) + e.toString());
                    switch (e.getCode()) {
                        case ParseException.INVALID_EMAIL_ADDRESS:
                            Log.d(TAG, mMockContext.getString(R.string.com_parse_ui_invalid_email_toast));
                            break;
                        case ParseException.USERNAME_TAKEN:
                            Log.d(TAG, mMockContext.getString(R.string.com_parse_ui_username_taken_toast));
                            break;
                        case ParseException.EMAIL_TAKEN:
                            Log.d(TAG, mMockContext.getString(R.string.com_parse_ui_email_taken_toast));
                            break;
                        default:
                            Log.d(TAG, mMockContext.getString(R.string.com_parse_ui_signup_failed_unknown_toast));
                    }
                }
                ParseServerTest.mSignal.countDown();
            }
        });

        try {
            mSignal.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void Wish() throws InterruptedException {
        WishItem item = Tester.generateWish();
        item.saveToLocal();

        WishItem savedItem = WishItemManager.getInstance().getItemById(item.getId());
        assertTrue(item.equals(savedItem));
    }
}
