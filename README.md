
# react-native-multi-image-picker

## Getting started

`$ npm install react-native-multi-image-picker --save`

### Mostly automatic installation

`$ react-native link react-native-multi-image-picker`

### Manual installation


#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-multi-image-picker` and add `RNMultiImagePicker.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNMultiImagePicker.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import com.siyukok.RNMultiImagePickerPackage;` to the imports at the top of the file
  - Add `new RNMultiImagePickerPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-multi-image-picker'
  	project(':react-native-multi-image-picker').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-multi-image-picker/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-multi-image-picker')
  	```
4. Insert the following lines inside the repositories block in `android/build.gradle`:
    ```
      maven { url "https://jitpack.io" }
    ```

## Usage
```javascript
import RNMultiImagePicker from 'react-native-multi-image-picker';

RNMultiImagePicker.openPicker({
    maxNum: 1,
    cropping: true
});
```
