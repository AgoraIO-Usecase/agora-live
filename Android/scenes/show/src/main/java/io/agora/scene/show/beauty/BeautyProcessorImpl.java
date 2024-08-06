package io.agora.scene.show.beauty;

import static io.agora.scene.show.beauty.BeautyConstantsKt.ITEM_ID_AR_HASHIQI;
import static io.agora.scene.show.beauty.BeautyConstantsKt.ITEM_ID_AR_KAOLA;
import static io.agora.scene.show.beauty.BeautyConstantsKt.ITEM_ID_AR_NONE;
import static io.agora.scene.show.beauty.BeautyConstantsKt.ITEM_ID_BEAUTY_BRIGHT_EYE;
import static io.agora.scene.show.beauty.BeautyConstantsKt.ITEM_ID_BEAUTY_BROW_POSITION;
import static io.agora.scene.show.beauty.BeautyConstantsKt.ITEM_ID_BEAUTY_BROW_THICKNESS;
import static io.agora.scene.show.beauty.BeautyConstantsKt.ITEM_ID_BEAUTY_CHEEKBONE;
import static io.agora.scene.show.beauty.BeautyConstantsKt.ITEM_ID_BEAUTY_CHIN;
import static io.agora.scene.show.beauty.BeautyConstantsKt.ITEM_ID_BEAUTY_CONTOURING;
import static io.agora.scene.show.beauty.BeautyConstantsKt.ITEM_ID_BEAUTY_EYE;
import static io.agora.scene.show.beauty.BeautyConstantsKt.ITEM_ID_BEAUTY_EYE_DISTANCE;
import static io.agora.scene.show.beauty.BeautyConstantsKt.ITEM_ID_BEAUTY_EYE_DOWNTURN;
import static io.agora.scene.show.beauty.BeautyConstantsKt.ITEM_ID_BEAUTY_FOREHEAD;
import static io.agora.scene.show.beauty.BeautyConstantsKt.ITEM_ID_BEAUTY_JAWBONE;
import static io.agora.scene.show.beauty.BeautyConstantsKt.ITEM_ID_BEAUTY_MOUTH;
import static io.agora.scene.show.beauty.BeautyConstantsKt.ITEM_ID_BEAUTY_MOUTH_POSITION;
import static io.agora.scene.show.beauty.BeautyConstantsKt.ITEM_ID_BEAUTY_NOSE;
import static io.agora.scene.show.beauty.BeautyConstantsKt.ITEM_ID_BEAUTY_NOSE_LIFT;
import static io.agora.scene.show.beauty.BeautyConstantsKt.ITEM_ID_BEAUTY_OVERALL;
import static io.agora.scene.show.beauty.BeautyConstantsKt.ITEM_ID_BEAUTY_REDDEN;
import static io.agora.scene.show.beauty.BeautyConstantsKt.ITEM_ID_BEAUTY_REMOVE_DARK_CIRCLES;
import static io.agora.scene.show.beauty.BeautyConstantsKt.ITEM_ID_BEAUTY_REMOVE_NASOLABIAL_FOLDS;
import static io.agora.scene.show.beauty.BeautyConstantsKt.ITEM_ID_BEAUTY_ROUND_EYE;
import static io.agora.scene.show.beauty.BeautyConstantsKt.ITEM_ID_BEAUTY_SHORT_WIDTH;
import static io.agora.scene.show.beauty.BeautyConstantsKt.ITEM_ID_BEAUTY_SMOOTH;
import static io.agora.scene.show.beauty.BeautyConstantsKt.ITEM_ID_BEAUTY_TEETH;
import static io.agora.scene.show.beauty.BeautyConstantsKt.ITEM_ID_BEAUTY_THICK_LIPS;
import static io.agora.scene.show.beauty.BeautyConstantsKt.ITEM_ID_BEAUTY_UPPER_WIDTH;
import static io.agora.scene.show.beauty.BeautyConstantsKt.ITEM_ID_BEAUTY_VSHAPE;
import static io.agora.scene.show.beauty.BeautyConstantsKt.ITEM_ID_BEAUTY_WHITEN;
import static io.agora.scene.show.beauty.BeautyConstantsKt.ITEM_ID_EFFECT_NONE;
import static io.agora.scene.show.beauty.BeautyConstantsKt.ITEM_ID_EFFECT_SEXY;
import static io.agora.scene.show.beauty.BeautyConstantsKt.ITEM_ID_EFFECT_TIANMEI;
import static io.agora.scene.show.beauty.BeautyConstantsKt.ITEM_ID_STICKER_CAT;
import static io.agora.scene.show.beauty.BeautyConstantsKt.ITEM_ID_STICKER_ELK;
import static io.agora.scene.show.beauty.BeautyConstantsKt.ITEM_ID_STICKER_NONE;

import android.content.Context;

import androidx.annotation.NonNull;

import com.faceunity.core.callback.OperateCallback;
import com.faceunity.core.entity.FUBundleData;
import com.faceunity.core.enumeration.FUAITypeEnum;
import com.faceunity.core.faceunity.FUAIKit;
import com.faceunity.core.faceunity.FURenderConfig;
import com.faceunity.core.faceunity.FURenderKit;
import com.faceunity.core.faceunity.FURenderManager;
import com.faceunity.core.model.facebeauty.FaceBeauty;
import com.faceunity.core.model.facebeauty.FaceBeautyBlurTypeEnum;
import com.faceunity.core.model.makeup.SimpleMakeup;
import com.faceunity.core.model.prop.Prop;
import com.faceunity.core.model.prop.animoji.Animoji;
import com.faceunity.core.model.prop.sticker.Sticker;
import com.faceunity.core.utils.FULogger;
import com.faceunity.wrapper.faceunity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;

import io.agora.beautyapi.faceunity.CameraConfig;
import io.agora.beautyapi.faceunity.CaptureMode;
import io.agora.beautyapi.faceunity.Config;
import io.agora.beautyapi.faceunity.FaceUnityBeautyAPI;
import io.agora.beautyapi.faceunity.FaceUnityBeautyAPIKt;
import io.agora.beautyapi.faceunity.utils.FuDeviceUtils;
import io.agora.rtc2.RtcEngine;
import io.agora.scene.base.component.AgoraApplication;
import io.agora.scene.show.BuildConfig;
import io.agora.scene.show.ShowLogger;

/**
 * The type Beauty processor.
 */
public class BeautyProcessorImpl extends IBeautyProcessor {
    /**
     * The constant TAG.
     */
    private final static String TAG = "BeautyProcessorImpl";

    private final static float FINE_CONFIDENCE_SCORE = 0.95f;
    /**
     * The M context.
     */
    private final Context mContext;
    /**
     * The M fuai kit.
     */
    private final FUAIKit mFUAIKit = FUAIKit.getInstance();
    /**
     * The M fu render kit.
     */
    private final FURenderKit mFURenderKit = FURenderKit.getInstance();
    /**
     * The M fu face beauty.
     */
    private FaceBeauty mFUFaceBeauty = new FaceBeauty(new FUBundleData("graphics" + File.separator + "face_beautification.bundle"));
    /**
     * The Bundle ai face.
     */
    private final String BUNDLE_AI_FACE = "model" + File.separator + "ai_face_processor.bundle";
    /**
     * The Bundle ai human.
     */
    private final String BUNDLE_AI_HUMAN = "model" + File.separator + "ai_human_processor.bundle";

    /**
     * The Is released.
     */
    private volatile boolean isReleased = false;
    /**
     * The Device level.
     */
    private int deviceLevel = FuDeviceUtils.DEVICEINFO_UNKNOWN;

    private static byte[] readFromFileToByteArray(String filePath) {
        File file = new File(filePath);
        try (FileInputStream inputStream = new FileInputStream(file); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return outputStream.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Initialize.
     *
     * @param rtcEngine the rtc engine
     */
    @Override
    public void initialize(@NonNull RtcEngine rtcEngine) {
        FURenderManager.setKitDebug(FULogger.LogLevel.DEBUG);
        FURenderManager.setCoreDebug(FULogger.LogLevel.ERROR);


        if (useLocalBeautyResource) {
            mFUFaceBeauty = new FaceBeauty(new FUBundleData("graphics" + File.separator + "face_beautification.bundle"));
        } else {
            mFUFaceBeauty = new FaceBeauty(new FUBundleData(mContext.getExternalFilesDir(null).getAbsolutePath() + "/assets/beauty_faceunity/graphics" + File.separator + "face_beautification.bundle"));
        }

        byte[] auth;
        if (useLocalBeautyResource) {
            try {
                auth = getAuth(); // 假设 getAuth() 是一个已定义的方法，返回 byte 数组
            } catch (Exception e) {
                ShowLogger.e(TAG, e, "Error getting auth"); // Log.w 需要三个参数：标签，消息，异常
                return;
            }
        } else {
            String filePath = mContext.getExternalFilesDir(null).getAbsolutePath() + "/assets/beauty_faceunity/license/fu.txt";
            auth = readFromFileToByteArray(filePath);
        }

        FURenderManager.registerFURender(mContext, auth, new OperateCallback() {
            @Override
            public void onSuccess(int code, @NonNull String msg) {
                ShowLogger.d("FURenderManager", "onSuccess >> code" + code + ", msg=" + msg);
                if (code == FURenderConfig.OPERATE_SUCCESS_AUTH) {
                    faceunity.fuSetUseTexAsync(1);
                    if (useLocalBeautyResource) {
                        mFUAIKit
                                .loadAIProcessor(BUNDLE_AI_FACE, FUAITypeEnum.FUAITYPE_FACEPROCESSOR);
                        mFUAIKit.loadAIProcessor(
                                BUNDLE_AI_HUMAN,
                                FUAITypeEnum.FUAITYPE_HUMAN_PROCESSOR
                        );
                    } else {
                        mFUAIKit.loadAIProcessor(
                                mContext.getExternalFilesDir("").getAbsolutePath() + "/assets/beauty_faceunity/" + BUNDLE_AI_FACE,
                                FUAITypeEnum.FUAITYPE_FACEPROCESSOR);
                        mFUAIKit.loadAIProcessor(
                                mContext.getExternalFilesDir("").getAbsolutePath() + "/assets/beauty_faceunity/" + BUNDLE_AI_HUMAN,
                                FUAITypeEnum.FUAITYPE_HUMAN_PROCESSOR
                        );
                    }

                    if (deviceLevel == FuDeviceUtils.DEVICEINFO_UNKNOWN) {
                        deviceLevel = FuDeviceUtils.judgeDeviceLevel(mContext);
                        FUAIKit.getInstance().faceProcessorSetFaceLandmarkQuality(deviceLevel);
                        if (deviceLevel > FuDeviceUtils.DEVICE_LEVEL_MID) {
                            FUAIKit.getInstance().fuFaceProcessorSetDetectSmallFace(true);
                        }
                    }
                    if (deviceLevel > FuDeviceUtils.DEVICE_LEVEL_MID) {
                        float score = FUAIKit.getInstance().getFaceProcessorGetConfidenceScore(0);
                        if (score > FINE_CONFIDENCE_SCORE) {
                            mFUFaceBeauty.setBlurType(FaceBeautyBlurTypeEnum.EquallySkin);
                            mFUFaceBeauty.setEnableBlurUseMask(true);
                        } else {
                            mFUFaceBeauty.setBlurType(FaceBeautyBlurTypeEnum.FineSkin);
                            mFUFaceBeauty.setEnableBlurUseMask(false);
                        }
                    } else {
                        mFUFaceBeauty.setBlurType(FaceBeautyBlurTypeEnum.FineSkin);
                        mFUFaceBeauty.setEnableBlurUseMask(false);
                    }

                    mFURenderKit.setFaceBeauty(mFUFaceBeauty);
                }
            }

            @Override
            public void onFail(int code, @NonNull String msg) {
                ShowLogger.e("FURenderManager", null, "onFail >> code" + code + ", msg=" + msg);
            }
        });

        getBeautyAPI().initialize(new Config(
                mContext,
                rtcEngine,
                mFURenderKit,
                null,
                CaptureMode.Agora,
                1000,
                false, new CameraConfig()));
        restore();
    }

    /**
     * Get auth byte [ ].
     *
     * @return the byte [ ]
     */
    private static byte[] getAuth() {
        try {
            Class<?> authpack = Class.forName("io.agora.scene.show.beauty.authpack");
            Method aMethod = authpack.getDeclaredMethod("A");
            aMethod.setAccessible(true);
            return (byte[]) aMethod.invoke(null);
        } catch (Exception e) {
            return new byte[0];
        }
    }

    /**
     * The Inner fu beauty api.
     */
    private FaceUnityBeautyAPI innerFUBeautyApi;

    private Boolean useLocalBeautyResource = true;

    /**
     * Gets sense time beauty api.
     *
     * @return the sense time beauty api
     */
    @NonNull
    @Override
    public FaceUnityBeautyAPI getBeautyAPI() {
        if (innerFUBeautyApi == null) {
            innerFUBeautyApi = FaceUnityBeautyAPIKt.createFaceUnityBeautyAPI();
        }
        return innerFUBeautyApi;
    }

    /**
     * Instantiates a new Beauty processor.
     *
     * @param context the context
     */
    public BeautyProcessorImpl(Context context, Boolean useLocalBeautyResource) {
        mContext = context.getApplicationContext();
        this.useLocalBeautyResource = useLocalBeautyResource;
        restore();
    }

    /**
     * Sets beauty enable.
     *
     * @param beautyEnable the beauty enable
     */
    @Override
    public void setBeautyEnable(boolean beautyEnable) {
        super.setBeautyEnable(beautyEnable);
        getBeautyAPI().enable(beautyEnable);
    }

    /**
     * Release.
     */
    @Override
    public void release() {
        super.release();
        if (innerFUBeautyApi != null) {
            innerFUBeautyApi.release();
            innerFUBeautyApi = null;
        }
        mFURenderKit.release();
        isReleased = true;
    }

    /**
     * Sets face beautify after cached.
     *
     * @param itemId    the item id
     * @param intensity the intensity
     */
    @Override
    protected void setFaceBeautifyAfterCached(int itemId, float intensity) {
        if (isReleased) {
            return;
        }

        ShowLogger.d(TAG, "setFaceBeautifyAfterCached >> itemId=" + itemId + ", intensity=" + intensity);
        mFUFaceBeauty.setBlurIntensity(BeautyCache.INSTANCE.getItemValue(ITEM_ID_BEAUTY_SMOOTH) * 6);
        mFUFaceBeauty.setColorIntensity(BeautyCache.INSTANCE.getItemValue(ITEM_ID_BEAUTY_WHITEN) * 2);
        mFUFaceBeauty.setRedIntensity(BeautyCache.INSTANCE.getItemValue(ITEM_ID_BEAUTY_REDDEN) * 2);
        mFUFaceBeauty.setFaceThreeIntensity(BeautyCache.INSTANCE.getItemValue(ITEM_ID_BEAUTY_CONTOURING));
        mFUFaceBeauty.setCheekThinningIntensity(BeautyCache.INSTANCE.getItemValue(ITEM_ID_BEAUTY_OVERALL));
        mFUFaceBeauty.setCheekVIntensity(BeautyCache.INSTANCE.getItemValue(ITEM_ID_BEAUTY_VSHAPE));
        mFUFaceBeauty.setCheekNarrowIntensity(BeautyCache.INSTANCE.getItemValue(ITEM_ID_BEAUTY_UPPER_WIDTH));
        mFUFaceBeauty.setCheekShortIntensity(BeautyCache.INSTANCE.getItemValue(ITEM_ID_BEAUTY_SHORT_WIDTH));
        mFUFaceBeauty.setCheekBonesIntensity(BeautyCache.INSTANCE.getItemValue(ITEM_ID_BEAUTY_CHEEKBONE));
        mFUFaceBeauty.setChinIntensity(BeautyCache.INSTANCE.getItemValue(ITEM_ID_BEAUTY_CHIN));
        mFUFaceBeauty.setForHeadIntensity(BeautyCache.INSTANCE.getItemValue(ITEM_ID_BEAUTY_FOREHEAD));
        mFUFaceBeauty.setEyeEnlargingIntensity(BeautyCache.INSTANCE.getItemValue(ITEM_ID_BEAUTY_EYE));
        mFUFaceBeauty.setEyeBrightIntensity(BeautyCache.INSTANCE.getItemValue(ITEM_ID_BEAUTY_BRIGHT_EYE));
        mFUFaceBeauty.setEyeCircleIntensity(BeautyCache.INSTANCE.getItemValue(ITEM_ID_BEAUTY_ROUND_EYE));
        mFUFaceBeauty.setEyeSpaceIntensity(BeautyCache.INSTANCE.getItemValue(ITEM_ID_BEAUTY_EYE_DISTANCE));
        mFUFaceBeauty.setEyeLidIntensity(BeautyCache.INSTANCE.getItemValue(ITEM_ID_BEAUTY_EYE_DOWNTURN));
        mFUFaceBeauty.setRemovePouchIntensity(BeautyCache.INSTANCE.getItemValue(ITEM_ID_BEAUTY_REMOVE_DARK_CIRCLES));
        mFUFaceBeauty.setBrowHeightIntensity(BeautyCache.INSTANCE.getItemValue(ITEM_ID_BEAUTY_BROW_POSITION));
        mFUFaceBeauty.setBrowThickIntensity(BeautyCache.INSTANCE.getItemValue(ITEM_ID_BEAUTY_BROW_THICKNESS));
        mFUFaceBeauty.setNoseIntensity(BeautyCache.INSTANCE.getItemValue(ITEM_ID_BEAUTY_NOSE));
        mFUFaceBeauty.setRemoveLawPatternIntensity(BeautyCache.INSTANCE.getItemValue(ITEM_ID_BEAUTY_REMOVE_NASOLABIAL_FOLDS));
        mFUFaceBeauty.setPhiltrumIntensity(BeautyCache.INSTANCE.getItemValue(ITEM_ID_BEAUTY_MOUTH_POSITION));
        mFUFaceBeauty.setLongNoseIntensity(BeautyCache.INSTANCE.getItemValue(ITEM_ID_BEAUTY_NOSE_LIFT));
        mFUFaceBeauty.setLowerJawIntensity(BeautyCache.INSTANCE.getItemValue(ITEM_ID_BEAUTY_JAWBONE));
        mFUFaceBeauty.setMouthIntensity(BeautyCache.INSTANCE.getItemValue(ITEM_ID_BEAUTY_MOUTH));
        mFUFaceBeauty.setLipThickIntensity(BeautyCache.INSTANCE.getItemValue(ITEM_ID_BEAUTY_THICK_LIPS));
        mFUFaceBeauty.setToothIntensity(BeautyCache.INSTANCE.getItemValue(ITEM_ID_BEAUTY_TEETH));
    }

    /**
     * Sets effect after cached.
     *
     * @param itemId    the item id
     * @param intensity the intensity
     */
    @Override
    protected void setEffectAfterCached(int itemId, float intensity) {
        if (isReleased) {
            return;
        }
        if (useLocalBeautyResource) {
            if (itemId == ITEM_ID_EFFECT_SEXY) {
                SimpleMakeup makeup = new SimpleMakeup(new FUBundleData(
                        "graphics" + File.separator + "face_makeup.bundle"
                ));
                makeup.setCombinedConfig(new FUBundleData(
                        "beauty_faceunity/makeup/xinggan.bundle"
                ));
                makeup.setMakeupIntensity(intensity);
                mFURenderKit.setMakeup(makeup);
            } else if (itemId == ITEM_ID_EFFECT_TIANMEI) {
                SimpleMakeup makeup = new SimpleMakeup(new FUBundleData(
                        "graphics" + File.separator + "face_makeup.bundle"
                ));
                makeup.setCombinedConfig(new FUBundleData(
                        "beauty_faceunity/makeup/tianmei.bundle"
                ));
                makeup.setMakeupIntensity(intensity);
                mFURenderKit.setMakeup(makeup);
            } else if (itemId == ITEM_ID_EFFECT_NONE) {
                mFURenderKit.setMakeup(null);
            }
        } else {
            if (itemId == ITEM_ID_EFFECT_SEXY) {
                SimpleMakeup makeup = new SimpleMakeup(new FUBundleData(
                        mContext.getExternalFilesDir(null).getAbsolutePath() + "/assets/beauty_faceunity/graphics" + File.separator + "face_makeup.bundle"
                ));
                makeup.setCombinedConfig(new FUBundleData(
                        mContext.getExternalFilesDir(null).getAbsolutePath() + "/assets/beauty_faceunity/makeup" + File.separator + "xinggan.bundle"
                ));
                makeup.setMakeupIntensity(intensity);
                mFURenderKit.setMakeup(makeup);
            } else if (itemId == ITEM_ID_EFFECT_TIANMEI) {
                SimpleMakeup makeup = new SimpleMakeup(new FUBundleData(
                        mContext.getExternalFilesDir(null).getAbsolutePath() + "/assets/beauty_faceunity/graphics" + File.separator + "face_makeup.bundle"
                ));
                makeup.setCombinedConfig(new FUBundleData(
                        mContext.getExternalFilesDir(null).getAbsolutePath() + "/assets/beauty_faceunity/makeup" + File.separator + "tianmei.bundle"
                ));
                makeup.setMakeupIntensity(intensity);
                mFURenderKit.setMakeup(makeup);
            } else if (itemId == ITEM_ID_EFFECT_NONE) {
                mFURenderKit.setMakeup(null);
            }
        }
    }

    /**
     * The Old prop.
     */
    private Prop oldProp = null;

    /**
     * Sets sticker after cached.
     *
     * @param itemId the item id
     */
    @Override
    protected void setStickerAfterCached(int itemId) {
        if (isReleased) {
            return;
        }

        if (useLocalBeautyResource) {
            if (itemId == ITEM_ID_STICKER_NONE) {
                if (oldProp != null) {
                    mFURenderKit.getPropContainer().removeProp(oldProp);
                    oldProp = null;
                }
            } else if (itemId == ITEM_ID_STICKER_CAT) {
                Sticker newProp = new Sticker(new FUBundleData("beauty_faceunity/sticker/cat_sparks.bundle"));
                if (oldProp == null || !oldProp.getControlBundle().getPath().equals(newProp.getControlBundle().getPath())) {
                    mFURenderKit.getPropContainer().replaceProp(oldProp, newProp);
                    oldProp = newProp;
                }
            } else if (itemId == ITEM_ID_STICKER_ELK) {
                Sticker newProp = new Sticker(new FUBundleData("beauty_faceunity/sticker/sdlu.bundle"));
                if (oldProp == null || !oldProp.getControlBundle().getPath().equals(newProp.getControlBundle().getPath())) {
                    mFURenderKit.getPropContainer().replaceProp(oldProp, newProp);
                    oldProp = newProp;
                }
            }
        } else {
            if (itemId == ITEM_ID_STICKER_NONE) {
                if (oldProp != null) {
                    mFURenderKit.getPropContainer().removeProp(oldProp);
                    oldProp = null;
                }
            } else if (itemId == ITEM_ID_STICKER_CAT) {
                Sticker newProp = new Sticker(new FUBundleData(
                        mContext.getExternalFilesDir(null).getAbsolutePath() + "/assets/beauty_faceunity/sticker" + File.separator + "cat_sparks.bundle"
                ));
                if (oldProp == null || !oldProp.getControlBundle().getPath().equals(newProp.getControlBundle().getPath())) {
                    mFURenderKit.getPropContainer().replaceProp(oldProp, newProp);
                    oldProp = newProp;
                }
            } else if (itemId == ITEM_ID_STICKER_ELK) {
                Sticker newProp = new Sticker(new FUBundleData(
                        mContext.getExternalFilesDir(null).getAbsolutePath() + "/assets/beauty_faceunity/sticker" + File.separator + "sdlu.bundle"
                ));
                if (oldProp == null || !oldProp.getControlBundle().getPath().equals(newProp.getControlBundle().getPath())) {
                    mFURenderKit.getPropContainer().replaceProp(oldProp, newProp);
                    oldProp = newProp;
                }
            }
        }
    }

    /**
     * Sets ar mark after cached.
     *
     * @param itemId the item id
     */
    @Override
    protected void setARMarkAfterCached(int itemId) {
        if (isReleased) {
            return;
        }

        if (useLocalBeautyResource) {
            if (itemId == ITEM_ID_AR_NONE) {
                if (oldProp != null) {
                    mFURenderKit.getPropContainer().removeProp(oldProp);
                    oldProp = null;
                }
            } else if (itemId == ITEM_ID_AR_KAOLA) {
                Animoji newProp = new Animoji(new FUBundleData("beauty_faceunity/animoji/kaola_Animoji.bundle"));
                if (oldProp == null || !oldProp.getControlBundle().getPath().equals(newProp.getControlBundle().getPath())) {
                    mFURenderKit.getPropContainer().replaceProp(oldProp, newProp);
                    oldProp = newProp;
                }
            } else if (itemId == ITEM_ID_AR_HASHIQI) {
                Animoji newProp = new Animoji(new FUBundleData("beauty_faceunity/animoji/hashiqi_Animoji.bundle"));
                if (oldProp == null || !oldProp.getControlBundle().getPath().equals(newProp.getControlBundle().getPath())) {
                    mFURenderKit.getPropContainer().replaceProp(oldProp, newProp);
                    oldProp = newProp;
                }
            }
        } else {
            if (itemId == ITEM_ID_AR_NONE) {
                if (oldProp != null) {
                    mFURenderKit.getPropContainer().removeProp(oldProp);
                    oldProp = null;
                }
            } else if (itemId == ITEM_ID_AR_KAOLA) {
                Animoji newProp = new Animoji(new FUBundleData(
                        mContext.getExternalFilesDir(null).getAbsolutePath() + "/assets/beauty_faceunity/animoji" + File.separator + "kaola_Animoji.bundle"
                ));
                if (oldProp == null || !oldProp.getControlBundle().getPath().equals(newProp.getControlBundle().getPath())) {
                    mFURenderKit.getPropContainer().replaceProp(oldProp, newProp);
                    oldProp = newProp;
                }
            } else if (itemId == ITEM_ID_AR_HASHIQI) {
                Animoji newProp = new Animoji(new FUBundleData(
                        mContext.getExternalFilesDir(null).getAbsolutePath() + "/assets/beauty_faceunity/animoji" + File.separator + "hashiqi_Animoji.bundle"
                ));
                if (oldProp == null || !oldProp.getControlBundle().getPath().equals(newProp.getControlBundle().getPath())) {
                    mFURenderKit.getPropContainer().replaceProp(oldProp, newProp);
                    oldProp = newProp;
                }
            }
        }

    }
}
