# Beans Wishlist

**Third-party library dependency:**<br/>
We use the following libraries in the project<br/>
- facebook-android-sdk https://developers.facebook.com/docs/android<br/>
- Parse https://parse.com/docs/android/guide<br/>
- ParseUI-Android https://github.com/ParsePlatform/ParseUI-Android<br/>
- TokenAutoComplete https://github.com/splitwise/TokenAutoComplete<br/>
- Android-Cloud-TagView-Plus https://github.com/kaedea/android-tagview
- ViewPageIndicator https://github.com/JakeWharton/ViewPagerIndicator
- SimpleCropView https://github.com/IsseiAoki/SimpleCropView
- CircleImageView https://github.com/hdodenhof/CircleImageView
- AndroidPriorityJobqueue https://github.com/yigit/android-priority-jobqueue
- Picaso http://square.github.io/picasso/<br/>

See the complete list in build.gradle

**Get the code**<br/>
  git clone https://github.com/jiawenzhang/wishlist.git<br/>
As we use git submodules, please also run the following command to download the submodule<br/>
  cd wishlist<br/>
  git submodule update --init --recursive<br/>

**Development enviroment**<br/>
We recommend Android Studio as the IDE. You can download it from https://developer.android.com/sdk/index.html. It should also    incude Android sdk.

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
  
Go to wishlist folder and run<br/>
  ./gradlew<br/>
  ./gradlew assembleDebug<br/>

  Install the apk to your Android devices or emulator (you will need adb installed and in your $PATH)<br/> 
  adb install ./wishlist/build/outputs/apk/wishlist-debug.apk<br/>
