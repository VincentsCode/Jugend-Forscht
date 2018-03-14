package jufo.vincent.de.app;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Kamera extends AppCompatActivity {
    public final int REQUEST_ID = 200;
    public CameraCaptureSession cameraCaptureSession;

    String IP_ADDRESS = "192.168.2.109";
    int PORT_NO = 9999;

    String camID;
    Size imageSize;
    private TextureView textureView;
    private CameraDevice camera;
    private CaptureRequest.Builder captBuilder;
    private Handler handler;
    private CameraDevice.StateCallback stateCallBack = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            camera = cameraDevice;
            createPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            camera.close();
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            camera.close();
        }
    };
    private TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
            openCam();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };

    private void createPreview() {
        try {
            SurfaceTexture tex = textureView.getSurfaceTexture();
            tex.setDefaultBufferSize(imageSize.getWidth(), imageSize.getHeight());
            Surface surface = new Surface(tex);
            captBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captBuilder.addTarget(surface);
            camera.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession camCaptureSession) {
                    if (camera == null) {
                        return;
                    }
                    cameraCaptureSession = camCaptureSession;
                    captBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                    try {
                        cameraCaptureSession.setRepeatingRequest(captBuilder.build(), null, handler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCam() {
        CameraManager camMan = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            assert camMan != null;
            camID = camMan.getCameraIdList()[0];
            CameraCharacteristics cameraCharacteristics = camMan.getCameraCharacteristics(camID);
            StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert streamConfigurationMap != null;
            imageSize = streamConfigurationMap.getOutputSizes(SurfaceTexture.class)[0];
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(Kamera.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_ID);
                return;
            }
            camMan.openCamera(camID, stateCallBack, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void takePicture() {
        if (camera == null) {
            return;
        }
        CameraManager camMan = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            assert camMan != null;
            CameraCharacteristics cameraCharacteristics = camMan.getCameraCharacteristics(camera.getId());
            int width = 1080;
            int height = 1920;
            ImageReader imageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            new Surface(textureView.getSurfaceTexture());
            List<Surface> list = new ArrayList<>();
            list.add(new Surface(textureView.getSurfaceTexture()));
            list.add(imageReader.getSurface());
            Surface output = imageReader.getSurface();
            final CaptureRequest.Builder builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            builder.addTarget(output);
            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);


            final File file = new File(Environment.getExternalStorageDirectory() + "/meinBild.jpg");
            ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
                Image image = null;

                @Override
                public void onImageAvailable(ImageReader imageReader) {
                    image = imageReader.acquireLatestImage();
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] byteArray = new byte[buffer.capacity()];
                    buffer.get(byteArray);
                    try {
                        Socket sock = new Socket(IP_ADDRESS, PORT_NO);
                        DataOutputStream dos = new DataOutputStream(sock.getOutputStream());
                        Log.d("ASD", "Array Length - " + byteArray.length);
                        dos.write(byteArray);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                        Log.d("ASD", reader.readLine());
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
            imageReader.setOnImageAvailableListener(onImageAvailableListener, handler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest captureRequest, @NonNull TotalCaptureResult totalCaptureResult) {
                    super.onCaptureCompleted(session, captureRequest, totalCaptureResult);
                    createPreview();
                    Toast.makeText(Kamera.this, "Erfolgreich gespeichert " + file, Toast.LENGTH_SHORT).show();
                }
            };
            camera.createCaptureSession(list, new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    try {
                        cameraCaptureSession.capture(builder.build(), captureListener, handler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                }
            }, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        HandlerThread hThread = new HandlerThread("Background Handler");
        hThread.start();
        handler = new Handler(hThread.getLooper());
        textureView = (TextureView) findViewById(R.id.texture);
        textureView.setSurfaceTextureListener(textureListener);
        Button takePictureBtn = (Button) findViewById(R.id.foto);
        takePictureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePicture();
            }
        });

    }

}