# Beans Wishlist

**Third-party library dependency:**<br/>
We use the following libraries in the project<br/>
- AndroidStaggeredGrid https://github.com/etsy/AndroidStaggeredGrid<br/>
- facebook-android-sdk https://developers.facebook.com/docs/android<br/>
- Parse https://parse.com/docs/android/guide<br/>
- ParseUI-Android https://github.com/ParsePlatform/ParseUI-Android<br/>
- TokenAutoComplete https://github.com/splitwise/TokenAutoComplete<br/>
- Picaso http://square.github.io/picasso/<br/>

**Get the code**<br/>
  git clone https://github.com/jiawenzhang/wishlist.git<br/>
As we use git submodules, please also run the following command to download the submodule<br/>
  cd wishlist<br/>
  git submodule update --init --recursive<br/>

**Development enviroment**<br/>
We recommen Android Studio as the IDE. You can download it from https://developer.android.com/sdk/index.html. It should also    incude Android sdk. The tools we use in the Android sdk are:
- Android sdk tools 24.3.2<br/>
- Google API 21<br/>
- Android sdk build tools 21.0.1<br/>
- Android Support Repository 15<br/>
- Google Play service 24<br/>
- Google Repository 19<br/>

**Compile and run**<br/>
- Using Android Studio<br/>
Open the project in Android Studio. (Select wishlist/wishlit/build.gradle when opening in Android Studio)<br/>
  Build->Make Project<br/>
  Run->Run 'wishlist-wishlist'<br/>

- Command line<br/>
General guide to build Android project using gradle in command line:<br/> http://developer.android.com/tools/building/building-cmdline.html

Go to wishlist folder and <br/>

   Build for debug<br/>
  ./gradlew assembleDebug<br/>
   It will download the gradle package and install it if you don't have it on your computer, be patient.<br/>

   Build for release<br/>
  ./gradlew assembleRelease<br/>
  
  Building for release will need to setup release key store file in ~/.gradle/gradle.properties<br/>
  Create a file gradle.properties in ~/.gradle/ and add the following to the file<br/>
  
  RELEASE_STORE_FILE=path/to/your/release.keystore<br/>
  RELEASE_STORE_PASSWORD="your release store password"<br/>
  RELEASE_KEY_ALIAS="your key alias"<br/>
  RELEASE_KEY_PASSWORD="your release key password"<br/>
  
  Install the apk to your Android devices or emulator (you will need adb installed and in your $PATH) 
  adb install ./wishlist/build/outputs/apk/wishlist-debug.apk<br/>
