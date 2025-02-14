```Java
    // 获取 MediaProjectionManager
    MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);

    private MediaProjection[] mediaProjection = new MediaProjection[1];
    private final ActivityResultLauncher<Intent> mediaProjectionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    try {
                        // 获取申请到的 MediaProjection
                        mediaProjection[0] = mediaProjectionManager
                                .getMediaProjection(result.getResultCode(), result.getData());
                        // 需要在 startScreenCapture 之前调用
                        engine.setExternalMediaProjection(mediaProjection[0]);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
    );

    // 请求屏幕捕获
    private void requestScreenCapture() {
        Intent intent = mediaProjectionManager.createScreenCaptureIntent();
        mediaProjectionLauncher.launch(intent);
    }
```

