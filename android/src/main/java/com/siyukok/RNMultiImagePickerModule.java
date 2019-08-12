
package com.siyukok;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.siyukok.util.ExifExtractor;
import com.siyukok.util.FileUtil;
import com.siyukok.util.ImageUtil;
import com.siyukok.util.RealPathUtil;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropActivity;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.filter.Filter;
import com.zhihu.matisse.internal.entity.CaptureStrategy;
import com.zhihu.matisse.listener.OnCheckedListener;
import com.zhihu.matisse.listener.OnSelectedListener;

import java.io.File;
import java.util.List;
import java.util.UUID;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

import static android.app.Activity.RESULT_OK;

public class RNMultiImagePickerModule extends ReactContextBaseJavaModule implements ActivityEventListener {

    private static final String TAG = "RNMultiImagePicker";

    private static final int REQUEST_CODE_CHOOSE = 10003;

    //user cancel
    private static final String E_PICKER_CANCELLED_KEY = "E_PICKER_CANCELLED";
    private static final String E_PICKER_CANCELLED_MSG = "User cancelled image selection";

    //Light Blue 500
    private static final String DEFAULT_WIDGET_COLOR = "#03A9F4";

    private ResultCollector resultCollector = new ResultCollector();

    private int cropWidth = 0;
    private int cropHeight = 0;

    private boolean multiple = false;
    private boolean includeBase64 = false;
    private boolean includeExif = false;
    private int maxNum = 1;
    private boolean cropping = false;
    private boolean cropperCircleOverlay = false;
    private boolean freeStyleCropEnabled = false;
    private boolean showCropGuidelines = true;
    private boolean hideBottomControls = false;
    private boolean enableRotationGesture = false;
    private boolean disableCropperColorSetters = false;

    private String authority = "";

    private String captureDir = "";

    private ReadableMap options;

    //Grey 800
    private final String DEFAULT_TINT = "#424242";
    private String cropperActiveWidgetColor = DEFAULT_TINT;
    private String cropperStatusBarColor = DEFAULT_TINT;
    private String cropperToolbarColor = DEFAULT_TINT;
    private String cropperToolbarTitle = null;

    private final ReactApplicationContext reactContext;

    public RNMultiImagePickerModule(ReactApplicationContext reactContext) {
        super(reactContext);
        reactContext.addActivityEventListener(this);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return TAG;
    }

    @ReactMethod
    public void openPicker(ReadableMap params, Promise promise) {
        setConfigurations(params);
        resultCollector.setup(promise, maxNum > 1);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_CHOOSE) {
                Log.e("OnActivityResult ", String.valueOf(Matisse.obtainOriginalState(data)));
                Log.e("OnActivityResult ", Matisse.obtainResult(data).get(0).toString());
                Log.e("OnActivityResult ", Matisse.obtainPathResult(data).get(0));
                Activity activity = getCurrentActivity();
                Uri firstUri = Matisse.obtainResult(data).get(0);
                if (activity != null && firstUri != null) {
                    if (cropping) {
                        startCropping(activity, firstUri);
                    } else {
                        try {
                            resultCollector.notifySuccess(getImage(activity, RealPathUtil.getRealPathFromURI(activity, firstUri)));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else if (requestCode == UCrop.REQUEST_CROP) {
                final Uri resultUri = UCrop.getOutput(data);
                if (resultUri != null) {
                    Log.e("OnActivityResult crop", resultUri.toString());
                }
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            final Throwable cropError = UCrop.getError(data);
            if (cropError != null) {
                Log.e("OnActivityResult crop", cropError.getLocalizedMessage());
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            resultCollector.notifyProblem(E_PICKER_CANCELLED_KEY, E_PICKER_CANCELLED_MSG);
        }
    }

    public void setConfigurations(ReadableMap options) {
        cropping = options.hasKey("cropping") && options.getBoolean("cropping");
        cropWidth = options.hasKey("cropWidth") ? options.getInt("cropWidth") : cropWidth;
        cropHeight = options.hasKey("cropHeight") ? options.getInt("cropHeight") : cropHeight;
        maxNum = options.hasKey("maxNum") ? options.getInt("maxNum") : maxNum;
        cropperActiveWidgetColor = options.hasKey("cropperActiveWidgetColor") ? options.getString("cropperActiveWidgetColor") : DEFAULT_TINT;
        cropperStatusBarColor = options.hasKey("cropperStatusBarColor") ? options.getString("cropperStatusBarColor") : DEFAULT_TINT;
        cropperToolbarColor = options.hasKey("cropperToolbarColor") ? options.getString("cropperToolbarColor") : DEFAULT_TINT;
        cropperToolbarTitle = options.hasKey("cropperToolbarTitle") ? options.getString("cropperToolbarTitle") : null;
        cropperCircleOverlay = options.hasKey("cropperCircleOverlay") && options.getBoolean("cropperCircleOverlay");
        freeStyleCropEnabled = options.hasKey("freeStyleCropEnabled") && options.getBoolean("freeStyleCropEnabled");
        showCropGuidelines = !options.hasKey("showCropGuidelines") || options.getBoolean("showCropGuidelines");
        hideBottomControls = options.hasKey("hideBottomControls") && options.getBoolean("hideBottomControls");
        enableRotationGesture = options.hasKey("enableRotationGesture") && options.getBoolean("enableRotationGesture");
        disableCropperColorSetters = options.hasKey("disableCropperColorSetters") && options.getBoolean("disableCropperColorSetters");
        this.options = options;
    }

    public void openGallary() {
        final Activity activity = getCurrentActivity();
        if (activity != null) {
            RxPermissions rxPermissions = new RxPermissions(activity);
            rxPermissions.request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
                    .subscribe(new Observer<Boolean>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onNext(Boolean aBoolean) {
                            if (aBoolean) {
                                Matisse.from(activity)
                                        .choose(MimeType.ofAll(), false)
                                        .theme(R.style.Matisse_Zhihu)
                                        .countable(true)
                                        .capture(true)
                                        .captureStrategy(
                                                new CaptureStrategy(true, authority, captureDir))
                                        .maxSelectable(maxNum)
                                        .addFilter(new GifSizeFilter(10, 10, 5 * Filter.K * Filter.K))
                                        .gridExpectedSize(
                                                activity.getResources().getDimensionPixelSize(R.dimen.grid_expected_size))
                                        .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                                        .thumbnailScale(0.85f)
//                                            .imageEngine(new GlideEngine())  // for glide-V3
                                        .imageEngine(new Glide4Engine())    // for glide-V4
                                        .setOnSelectedListener(new OnSelectedListener() {
                                            @Override
                                            public void onSelected(@NonNull List<Uri> uriList, @NonNull List<String> pathList) {
                                            }
                                        })
                                        .originalEnable(true)
                                        .maxOriginalSize(10)
                                        .autoHideToolbarOnSingleTap(true)
                                        .setOnCheckedListener(new OnCheckedListener() {
                                            @Override
                                            public void onCheck(boolean isChecked) {
                                                Log.i("onCheck", "onCheck: " + isChecked);
                                            }
                                        })
                                        .forResult(REQUEST_CODE_CHOOSE);
                            } else {
                                Toast.makeText(activity, R.string.permission_request_denied, Toast.LENGTH_LONG)
                                        .show();
                            }
                        }

                        @Override
                        public void onError(Throwable e) {

                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        }
    }

    private void configureCropperColors(UCrop.Options options) {
        int activeWidgetColor = Color.parseColor(cropperActiveWidgetColor);
        int toolbarColor = Color.parseColor(cropperToolbarColor);
        int statusBarColor = Color.parseColor(cropperStatusBarColor);
        options.setToolbarColor(toolbarColor);
        options.setStatusBarColor(statusBarColor);
        if (activeWidgetColor == Color.parseColor(DEFAULT_TINT)) {
            /*
            Default tint is grey => use a more flashy color that stands out more as the call to action
            Here we use 'Light Blue 500' from https://material.google.com/style/color.html#color-color-palette
            */
            options.setActiveWidgetColor(Color.parseColor(DEFAULT_WIDGET_COLOR));
        } else {
            //If they pass a custom tint color in, we use this for everything
            options.setActiveWidgetColor(activeWidgetColor);
        }
    }

    private void startCropping(Activity activity, Uri uri) {
        UCrop.Options options = new UCrop.Options();
        options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
        options.setCompressionQuality(100);
        options.setCircleDimmedLayer(cropperCircleOverlay);
        options.setFreeStyleCropEnabled(freeStyleCropEnabled);
        options.setShowCropGrid(showCropGuidelines);
        options.setHideBottomControls(hideBottomControls);
        if (cropperToolbarTitle != null) {
            options.setToolbarTitle(cropperToolbarTitle);
        }
        if (enableRotationGesture) {
            // UCropActivity.ALL = enable both rotation & scaling
            options.setAllowedGestures(
                    UCropActivity.ALL, // When 'scale'-tab active
                    UCropActivity.ALL, // When 'rotate'-tab active
                    UCropActivity.ALL  // When 'aspect ratio'-tab active
            );
        }
        if (!disableCropperColorSetters) {
            configureCropperColors(options);
        }

        UCrop uCrop = UCrop
                .of(uri, Uri.fromFile(new File(this.getTmpDir(activity), UUID.randomUUID().toString() + ".jpg")))
                .withOptions(options);

        if (cropWidth > 0 && cropHeight > 0) {
            uCrop.withAspectRatio(cropWidth, cropHeight);
        }

        uCrop.start(activity);
    }

    private String getTmpDir(Activity activity) {
        String tmpDir = activity.getCacheDir() + "/react-native-multi-image-picker";
        new File(tmpDir).mkdir();

        return tmpDir;
    }

    private WritableMap getImage(final Activity activity, String path) throws Exception {
        WritableMap image = new WritableNativeMap();

        if (path.startsWith("http://") || path.startsWith("https://")) {
            throw new Exception("Cannot select remote files");
        }
        BitmapFactory.Options original = ImageUtil.validateImage(path);

        // if compression options are provided image will be compressed. If none options is provided,
        // then original image will be returned
        File compressedImage = ImageUtil.compressImage(options, path, original);
        String compressedImagePath = compressedImage.getPath();
        BitmapFactory.Options options = ImageUtil.validateImage(compressedImagePath);
        long modificationDate = new File(path).lastModified();

        image.putString("path", "file://" + compressedImagePath);
        image.putInt("width", options.outWidth);
        image.putInt("height", options.outHeight);
        image.putString("mime", options.outMimeType);
        image.putInt("size", (int) new File(compressedImagePath).length());
        image.putString("modificationDate", String.valueOf(modificationDate));

        if (includeBase64) {
            image.putString("data", FileUtil.getBase64StringFromFile(compressedImagePath));
        }

        if (includeExif) {
            try {
                WritableMap exif = ExifExtractor.extract(path);
                image.putMap("exif", exif);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return image;
    }


}