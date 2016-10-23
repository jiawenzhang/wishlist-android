/*
 *  Copyright (c) 2014, Parse, LLC. All rights reserved.
 *
 *  You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 *  copy, modify, and distribute this software in source code or binary form for use
 *  in connection with the web services and APIs provided by Parse.
 *
 *  As with any software that integrates with the Parse platform, your use of
 *  this software is subject to the Parse Terms of Service
 *  [https://www.parse.com/about/terms]. This copyright notice shall be
 *  included in all copies or substantial portions of the software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 *  FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 *  COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 *  IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *  CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package com.wish.wishlist.login;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.os.Bundle;
import android.util.Log;

import com.parse.Parse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * Configurations for the ParseLoginActivity.
 */
class ParseLoginConfig {
  private static final String APP_LOGO = "com.parse.ui.ParseLoginActivity.APP_LOGO";
  private static final String PARSE_LOGIN_ENABLED = "com.parse.ui.ParseLoginActivity.PARSE_LOGIN_ENABLED";
  private static final String PARSE_LOGIN_ALLOW_SKIP = "com.parse.ui.ParseLoginActivity.PARSE_LOGIN_ALLOW_SKIP";
  private static final String PARSE_LOGIN_BUTTON_TEXT = "com.parse.ui.ParseLoginActivity.PARSE_LOGIN_BUTTON_TEXT";
  private static final String PARSE_SIGNUP_BUTTON_TEXT = "com.parse.ui.ParseLoginActivity.PARSE_SIGNUP_BUTTON_TEXT";
  private static final String PARSE_LOGIN_HELP_TEXT = "com.parse.ui.ParseLoginActivity.PARSE_LOGIN_HELP_TEXT";
  private static final String PARSE_LOGIN_INVALID_CREDENTIALS_TOAST_TEXT = "com.parse.ui.ParseLoginActivity.PARSE_LOGIN_INVALID_CREDENTIALS_TEXT";
  private static final String PARSE_LOGIN_EMAIL_AS_USERNAME = "com.parse.ui.ParseLoginActivity.PARSE_LOGIN_EMAIL_AS_USERNAME";
  private static final String PARSE_SIGNUP_MIN_PASSWORD_LENGTH = "com.parse.ui.ParseLoginActivity.PARSE_SIGNUP_MIN_PASSWORD_LENGTH";
  private static final String PARSE_SIGNUP_SUBMIT_BUTTON_TEXT = "com.parse.ui.ParseLoginActivity.PARSE_SIGNUP_SUBMIT_BUTTON_TEXT";
  private static final String FACEBOOK_LOGIN_ENABLED = "com.parse.ui.ParseLoginActivity.FACEBOOK_LOGIN_ENABLED";
  private static final String FACEBOOK_LOGIN_BUTTON_TEXT = "com.parse.ui.ParseLoginActivity.FACEBOOK_LOGIN_BUTTON_TEXT";
  private static final String FACEBOOK_LOGIN_PERMISSIONS = "com.parse.ui.ParseLoginActivity.FACEBOOK_LOGIN_PERMISSIONS";
  private static final String TWITTER_LOGIN_ENABLED = "com.parse.ui.ParseLoginActivity.TWITTER_LOGIN_ENABLED";
  private static final String TWITTER_LOGIN_BUTTON_TEXT = "com.parse.ui.ParseLoginActivity.TWITTER_LOGIN_BUTTON_TEXT";

  // For internally serializing to/from string array (the public analog above is for resource from activity meta-data).
  private static final String FACEBOOK_LOGIN_PERMISSIONS_STRING_ARRAY = "com.parse.ui.ParseLoginActivity.FACEBOOK_LOGIN_PERMISSIONS_STRING_ARRAY";

  private static final String LOG_TAG = "ParseLoginConfig";

  // Use boxed types so that we can differentiate between a setting not set,
  // versus its default value.  This is useful for merging options set from code
  // with options set by activity metadata.
  private Integer appLogo;
  private Boolean parseLoginEnabled;
  private Boolean parseLoginAllowSkip;
  private CharSequence parseLoginButtonText;
  private CharSequence parseSignupButtonText;
  private CharSequence parseLoginHelpText;
  private CharSequence parseLoginInvalidCredentialsToastText;
  private Boolean parseLoginEmailAsUsername;
  private Integer parseSignupMinPasswordLength;
  private CharSequence parseSignupSubmitButtonText;

  private Boolean facebookLoginEnabled;
  private CharSequence facebookLoginButtonText;
  private Collection<String> facebookLoginPermissions;

  private Boolean twitterLoginEnabled;
  private CharSequence twitterLoginButtonText;

  public Integer getAppLogo() {
    return appLogo;
  }

  void setAppLogo(Integer appLogo) {
    this.appLogo = appLogo;
  }

  boolean isParseLoginEnabled() {
    if (parseLoginEnabled != null) {
      return parseLoginEnabled;
    } else {
      return false;
    }
  }

  void setParseLoginEnabled(boolean parseLoginEnabled) {
    this.parseLoginEnabled = parseLoginEnabled;
  }

  boolean parseLoginAllowSkip() {
    if (parseLoginAllowSkip != null) {
      return parseLoginAllowSkip;
    } else {
      return false;
    }
  }

  void setParseLoginAllowSkip(boolean allow) {
    this.parseLoginAllowSkip = allow;
  }

  CharSequence getParseLoginButtonText() {
    return parseLoginButtonText;
  }

  void setParseLoginButtonText(CharSequence parseLoginButtonText) {
    this.parseLoginButtonText = parseLoginButtonText;
  }

  CharSequence getParseSignupButtonText() {
    return parseSignupButtonText;
  }

  void setParseSignupButtonText(CharSequence parseSignupButtonText) {
    this.parseSignupButtonText = parseSignupButtonText;
  }

  CharSequence getParseLoginHelpText() {
    return parseLoginHelpText;
  }

  void setParseLoginHelpText(CharSequence parseLoginHelpText) {
    this.parseLoginHelpText = parseLoginHelpText;
  }

  CharSequence getParseLoginInvalidCredentialsToastText() {
    return parseLoginInvalidCredentialsToastText;
  }

  void setParseLoginInvalidCredentialsToastText(
      CharSequence parseLoginInvalidCredentialsToastText) {
    this.parseLoginInvalidCredentialsToastText = parseLoginInvalidCredentialsToastText;
  }

  boolean isParseLoginEmailAsUsername() {
    if (parseLoginEmailAsUsername != null) {
      return parseLoginEmailAsUsername;
    } else {
      return false;
    }
  }

  void setParseLoginEmailAsUsername(boolean parseLoginEmailAsUsername) {
    this.parseLoginEmailAsUsername = parseLoginEmailAsUsername;
  }

  Integer getParseSignupMinPasswordLength() {
    return parseSignupMinPasswordLength;
  }

  void setParseSignupMinPasswordLength(Integer parseSignupMinPasswordLength) {
    this.parseSignupMinPasswordLength = parseSignupMinPasswordLength;
  }

  CharSequence getParseSignupSubmitButtonText() {
    return parseSignupSubmitButtonText;
  }

  void setParseSignupSubmitButtonText(
      CharSequence parseSignupSubmitButtonText) {
    this.parseSignupSubmitButtonText = parseSignupSubmitButtonText;
  }

  boolean isFacebookLoginEnabled() {
    if (facebookLoginEnabled != null) {
      return facebookLoginEnabled;
    } else {
      return false;
    }
  }

  void setFacebookLoginEnabled(boolean facebookLoginEnabled) {
    this.facebookLoginEnabled = facebookLoginEnabled;
  }

  CharSequence getFacebookLoginButtonText() {
    return facebookLoginButtonText;
  }

  void setFacebookLoginButtonText(CharSequence facebookLoginButtonText) {
    this.facebookLoginButtonText = facebookLoginButtonText;
  }

  Collection<String> getFacebookLoginPermissions() {
    if (facebookLoginPermissions != null) {
      return Collections.unmodifiableCollection(facebookLoginPermissions);
    } else {
      return null;
    }
  }

  void setFacebookLoginPermissions(Collection<String> permissions) {
    if (permissions != null) {
      facebookLoginPermissions = new ArrayList<String>(permissions.size());
      facebookLoginPermissions.addAll(permissions);
    }
  }

  /* package */ boolean isFacebookLoginNeedPublishPermissions() {
    if (facebookLoginPermissions != null) {
      return facebookLoginPermissions.contains("publish_actions") ||
          facebookLoginPermissions.contains("publish_pages");
    } else {
      return false;
    }
  }

  boolean isTwitterLoginEnabled() {
    if (twitterLoginEnabled != null) {
      return twitterLoginEnabled;
    } else {
      return false;
    }
  }

  void setTwitterLoginEnabled(boolean twitterLoginEnabled) {
    this.twitterLoginEnabled = twitterLoginEnabled;
  }

  CharSequence getTwitterLoginButtonText() {
    return twitterLoginButtonText;
  }

  void setTwitterLoginButtonText(CharSequence twitterLoginButtonText) {
    this.twitterLoginButtonText = twitterLoginButtonText;
  }

  /**
   * Converts this object into a Bundle object. For options that are not
   * explicitly set, we do not include them in the Bundle so that this bundle
   * can be merged with any default configurations and override only those keys
   * that are explicitly set.
   *
   * @return The Bundle object containing configurations.
   */
  Bundle toBundle() {
    Bundle bundle = new Bundle();

    if (appLogo != null) {
      bundle.putInt(APP_LOGO, appLogo);
    }

    if (parseLoginEnabled != null) {
      bundle.putBoolean(PARSE_LOGIN_ENABLED, parseLoginEnabled);
    }
    if (parseLoginAllowSkip != null) {
      bundle.putBoolean(PARSE_LOGIN_ALLOW_SKIP, parseLoginAllowSkip);
    }
    if (parseLoginButtonText != null) {
      bundle.putCharSequence(PARSE_LOGIN_BUTTON_TEXT, parseLoginButtonText);
    }
    if (parseSignupButtonText != null) {
      bundle.putCharSequence(PARSE_SIGNUP_BUTTON_TEXT, parseSignupButtonText);
    }
    if (parseLoginHelpText != null) {
      bundle.putCharSequence(PARSE_LOGIN_HELP_TEXT, parseLoginHelpText);
    }
    if (parseLoginInvalidCredentialsToastText != null) {
      bundle.putCharSequence(PARSE_LOGIN_INVALID_CREDENTIALS_TOAST_TEXT,
          parseLoginInvalidCredentialsToastText);
    }
    if (parseLoginEmailAsUsername != null) {
      bundle.putBoolean(PARSE_LOGIN_EMAIL_AS_USERNAME,
          parseLoginEmailAsUsername);
    }
    if (parseSignupMinPasswordLength != null) {
      bundle.putInt(PARSE_SIGNUP_MIN_PASSWORD_LENGTH,
          parseSignupMinPasswordLength);
    }
    if (parseSignupSubmitButtonText != null) {
      bundle.putCharSequence(PARSE_SIGNUP_SUBMIT_BUTTON_TEXT,
          parseSignupSubmitButtonText);
    }

    if (facebookLoginEnabled != null) {
      bundle.putBoolean(FACEBOOK_LOGIN_ENABLED, facebookLoginEnabled);
    }
    if (facebookLoginButtonText != null) {
      bundle.putCharSequence(FACEBOOK_LOGIN_BUTTON_TEXT,
          facebookLoginButtonText);
    }
    if (facebookLoginPermissions != null) {
      bundle.putStringArray(FACEBOOK_LOGIN_PERMISSIONS_STRING_ARRAY,
          facebookLoginPermissions.toArray(new String[0]));
    }

    if (twitterLoginEnabled != null) {
      bundle.putBoolean(TWITTER_LOGIN_ENABLED, twitterLoginEnabled);
    }
    if (twitterLoginButtonText != null) {
      bundle.putCharSequence(TWITTER_LOGIN_BUTTON_TEXT, twitterLoginButtonText);
    }

    return bundle;
  }

  /**
   * Constructs a ParseLoginConfig object from a bundle. Unrecognized keys are
   * ignored.
   * <p/>
   * This can be used to pass an ParseLoginConfig object between activities, or
   * to read settings from an activity's meta-data in Manefest.xml.
   *
   * @param bundle
   *     The Bundle representation of the ParseLoginConfig object.
   * @param context
   *     The context for resolving resource IDs.
   * @return The ParseLoginConfig instance.
   */
  static ParseLoginConfig fromBundle(Bundle bundle, Context context) {
    ParseLoginConfig config = new ParseLoginConfig();
    Set<String> keys = bundle.keySet();

    if (keys.contains(APP_LOGO)) {
      config.setAppLogo(bundle.getInt(APP_LOGO));
    }

    if (keys.contains(PARSE_LOGIN_ENABLED)) {
      config.setParseLoginEnabled(bundle.getBoolean(PARSE_LOGIN_ENABLED));
    }
    if (keys.contains(PARSE_LOGIN_ALLOW_SKIP)) {
      config.setParseLoginAllowSkip(bundle.getBoolean(PARSE_LOGIN_ALLOW_SKIP));
    }
    if (keys.contains(PARSE_LOGIN_BUTTON_TEXT)) {
      config.setParseLoginButtonText(bundle.getCharSequence(PARSE_LOGIN_BUTTON_TEXT));
    }
    if (keys.contains(PARSE_SIGNUP_BUTTON_TEXT)) {
      config.setParseSignupButtonText(bundle.getCharSequence(PARSE_SIGNUP_BUTTON_TEXT));
    }
    if (keys.contains(PARSE_LOGIN_HELP_TEXT)) {
      config.setParseLoginHelpText(bundle.getCharSequence(PARSE_LOGIN_HELP_TEXT));
    }
    if (keys.contains(PARSE_LOGIN_INVALID_CREDENTIALS_TOAST_TEXT)) {
      config.setParseLoginInvalidCredentialsToastText(bundle
          .getCharSequence(PARSE_LOGIN_INVALID_CREDENTIALS_TOAST_TEXT));
    }
    if (keys.contains(PARSE_LOGIN_EMAIL_AS_USERNAME)) {
      config.setParseLoginEmailAsUsername(bundle.getBoolean(PARSE_LOGIN_EMAIL_AS_USERNAME));
    }
    if (keys.contains(PARSE_SIGNUP_MIN_PASSWORD_LENGTH)) {
      config.setParseSignupMinPasswordLength(bundle.getInt(PARSE_SIGNUP_MIN_PASSWORD_LENGTH));
    }
    if (keys.contains(PARSE_SIGNUP_SUBMIT_BUTTON_TEXT)) {
      config.setParseSignupSubmitButtonText(bundle.getCharSequence(PARSE_SIGNUP_SUBMIT_BUTTON_TEXT));
    }

    if (keys.contains(FACEBOOK_LOGIN_ENABLED)) {
      config.setFacebookLoginEnabled(bundle.getBoolean(FACEBOOK_LOGIN_ENABLED));
    }
    if (keys.contains(FACEBOOK_LOGIN_BUTTON_TEXT)) {
      config.setFacebookLoginButtonText(bundle.getCharSequence(FACEBOOK_LOGIN_BUTTON_TEXT));
    }
    if (keys.contains(FACEBOOK_LOGIN_PERMISSIONS) &&
        bundle.getInt(FACEBOOK_LOGIN_PERMISSIONS) != 0) {
      // Only for converting from activity meta-data.
      try {
        config.setFacebookLoginPermissions(stringArrayToCollection(context
            .getResources().getStringArray(
                bundle.getInt(FACEBOOK_LOGIN_PERMISSIONS))));
      } catch (NotFoundException e) {
        if (Parse.getLogLevel() <= Parse.LOG_LEVEL_ERROR) {
          Log.w(LOG_TAG, "Facebook permission string array resource not found");
        }
      }
    } else if (keys.contains(FACEBOOK_LOGIN_PERMISSIONS_STRING_ARRAY)) {
      // For converting from a bundle produced by this class's toBundle()
      config.setFacebookLoginPermissions(stringArrayToCollection(bundle
          .getStringArray(FACEBOOK_LOGIN_PERMISSIONS_STRING_ARRAY)));
    }

    if (keys.contains(TWITTER_LOGIN_ENABLED)) {
      config.setTwitterLoginEnabled(bundle.getBoolean(TWITTER_LOGIN_ENABLED));
    }
    if (keys.contains(TWITTER_LOGIN_BUTTON_TEXT)) {
      config.setTwitterLoginButtonText(bundle
          .getCharSequence(TWITTER_LOGIN_BUTTON_TEXT));
    }

    return config;
  }

  private static Collection<String> stringArrayToCollection(String[] array) {
    if (array == null) {
      return null;
    }
    return Arrays.asList(array);
  }
}
