package com.example.imagepro;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import java.util.Random;
import android.os.Build;
import android.security.keystore.StrongBoxUnavailableException;
import android.text.PrecomputedText;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import androidx.annotation.RequiresApi;
import org.checkerframework.checker.units.qual.A;
import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.features2d.Feature2D;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.Params;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.FeatureDetector;
import org.tensorflow.lite.task.core.vision.ImageProcessingOptions;
import java.util.stream.Collectors;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.opencv.features2d.SimpleBlobDetector;


public class objectDetectorClass {
    // should start from small letter
    // this is used to load model and predict

    private double thresholdStep = 2;
    private double thresholdMin = 5;
    private double thresholdMax = 220;
    private int repeatability = 2;
    private double distBetweenBlobs = 10;
    private boolean color = false;
    private double colorValue = 0;
    private boolean area = true;
    private double areaMin = 20;
    private double areaMax = 30000;
    private boolean circularity = true;
    private double circularityMin = 0.8;
    private double circularityMax = 1;
    private boolean inertia = false;
    private double inertiaRatioMin = 1.0000000149011612E-001;
    private double inertiaRatioMax = -1;
    private boolean convexity = false;
    private double convexityMin = 9.4999998807907104E-001;
    private double convexityMax = -1;


    private Interpreter interpreter;
    // store all label in array
    private List<String> labelList;
    private int INPUT_SIZE;
    private int PIXEL_SIZE = 3; // for RGB
    private int IMAGE_MEAN = 0;
    private float IMAGE_STD = 255.0f;

    // use to initialize gpu in app
    private GpuDelegate gpuDelegate;
    private int height = 0;
    private int width = 0;

    objectDetectorClass(AssetManager assetManager, String modelPath, String labelPath, int inputSize) throws IOException {
        INPUT_SIZE = inputSize;
        // use to define gpu or cpu // no. of threads
        Interpreter.Options options = new Interpreter.Options();
        //gpuDelegate=new GpuDelegate();
        //options.addDelegate(gpuDelegate);
        options.setNumThreads(4); // set it according to your phone
        // loading model
        interpreter = new Interpreter(loadModelFile(assetManager, modelPath), options);
        // load labelmap
        labelList = loadLabelList(assetManager, labelPath);

    }

    private List<String> loadLabelList(AssetManager assetManager, String labelPath) throws IOException {
        // to store label
        List<String> labelList = new ArrayList<>();
        // create a new reader
        BufferedReader reader = new BufferedReader(new InputStreamReader(assetManager.open(labelPath)));
        String line;
        // loop through each line and store it to labelList
        while ((line = reader.readLine()) != null) {
            labelList.add(line);
        }
        reader.close();
        return labelList;
    }

    private ByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException {
        // use to get description of file
        AssetFileDescriptor fileDescriptor = assetManager.openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();

        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    // create new Mat function
    public Mat recognizeImage(Mat mat_image) {
        // Rotate original image by 90 degree get get portrait frame

        // This change was done in video: Does Your App Keep Crashing? | Watch This Video For Solution.
        // This will fix crashing problem of the app

        Mat rotated_mat_image = new Mat();

        Mat a = mat_image.t();
        Core.flip(a, rotated_mat_image, 1);
        // Release mat
        a.release();

        // if you do not do this process you will get improper prediction, less no. of object
        // now convert it to bitmap
        Bitmap bitmap = null;
        bitmap = Bitmap.createBitmap(rotated_mat_image.cols(), rotated_mat_image.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(rotated_mat_image, bitmap);
        // define height and width
        height = bitmap.getHeight();
        width = bitmap.getWidth();

        // scale the bitmap to input size of model
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);

        // convert bitmap to bytebuffer as model input should be in it
        ByteBuffer byteBuffer = convertBitmapToByteBuffer(scaledBitmap);

        // defining output
        // 10: top 10 object detected
        // 4: there coordinate in image
        //  float[][][]result=new float[1][10][4];

        Object[] input = new Object[1];
        input[0] = byteBuffer;

        Map<Integer, Object> output_map = new TreeMap<>();

        // we are not going to use this method of output
        // instead we create treemap of three array (boxes,score,classes)
        float[][][] boxes = new float[1][50][4];

        // 10: top 10 object detected
        // 4: there coordinate in image
        float[][] scores = new float[1][50];
        // stores scores of 10 object
        float[][] classes = new float[1][50];
        // stores class of object

        // add it to object_map;
        output_map.put(0, boxes);
        output_map.put(1, classes);
        output_map.put(2, scores);

        // now predict
        interpreter.runForMultipleInputsOutputs(input, output_map);
        // Before watching this video please watch my previous 2 video of
        //      1. Loading tensorflow lite model
        //      2. Predicting object
        // In this video we will draw boxes and label it with it's name

        Object value = output_map.get(0);
        Object Object_class = output_map.get(1);
        Object score = output_map.get(2);

        // loop through each object
        // as output has only 10 boxes

        for (int i = 0; i < 10 ; i++) {
            float class_value = (float) Array.get(Array.get(Object_class, 0), i);
            float score_value = (float) Array.get(Array.get(score, 0), i);

            // define threshold for score
            // Here you can change threshold according to your model
            // Now we will do some change to improve app
            if (score_value > 0.4) {
                Object box1 = Array.get(Array.get(value, 0), i);
                // we are multiplying it with Original height and width of frame

                float top = (float) Array.get(box1, 0) * height;
                float left = (float) Array.get(box1, 1) * width;
                float bottom = (float) Array.get(box1, 2) * height;
                float right = (float) Array.get(box1, 3) * width;

                //////////////////////////////////////////////

                // Use these points to extract image patch and run a simple blob detector and draw blobs
                int BoxHeight = (int) (bottom - top);
                int BoxWidth = (int) (right -left);

                // Use these points to extract image patch and run a simple blob detector and draw blobs
                Rect roi= new Rect((int) left, (int) top, BoxWidth, BoxHeight);
                if((0 <= roi.x && 0 <= roi.width && roi.x + roi.width <= mat_image.cols() && 0 <= roi.y && 0 <= roi.height && roi.y + roi.height <= mat_image.rows()))
                {
                    Mat roiMat = mat_image.submat(roi).clone();
                    final Mat processedroi = processImage(roiMat);

                    // Mark outer contour
                    markOuterContour(processedroi, roiMat);
                   // detect_blob(processedroi, roiMat);

                    // now copy the result to the original mat
                    Mat dst = mat_image.submat(roi); // do not clone this submat
                    roiMat.copyTo(dst);
                }

                ///////////////////////////////////////////
                // draw rectangle in Original frame //  starting point    // ending point of box  // color of box       thickness
                Imgproc.rectangle(rotated_mat_image, new Point(left, top), new Point(right, bottom), new Scalar(255, 255, 255, 255), 2);
                // write text on frame
                // string of class name of object  // starting point                         // color of text           // size of text
                Imgproc.putText(rotated_mat_image, labelList.get((int) class_value), new Point(left, top), 3, 1, new Scalar(255, 0, 0, 255), 2);
            }

        }
        // select device and run

        // before returning rotate back by -90 degree

        // Do same here
        Mat b = rotated_mat_image.t();
        Core.flip(b, mat_image, 0);
        b.release();
        // Now for second change go to CameraBridgeViewBase
        return mat_image;
    }

    public Mat recognizePhoto(Mat mat_image) {

        boolean detectVoids = true;

        // if you do not do this process you will get improper prediction, less no. of object
        // now convert it to bitmap
        Bitmap bitmap = null;
        bitmap = Bitmap.createBitmap(mat_image.cols(), mat_image.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat_image, bitmap);
        // define height and width
        height = bitmap.getHeight();
        width = bitmap.getWidth();

        // scale the bitmap to input size of model
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);

        // convert bitmap to bytebuffer as model input should be in it
        ByteBuffer byteBuffer = convertBitmapToByteBuffer(scaledBitmap);

        // defining output
        // 10: top 10 object detected
        // 4: there coordinate in image
        //  float[][][]result=new float[1][10][4];

        Object[] input = new Object[1];
        input[0] = byteBuffer;

        Map<Integer, Object> output_map = new TreeMap<>();

        // we are not going to use this method of output
        // instead we create treemap of three array (boxes,score,classes)
        float[][][] boxes = new float[1][50][4];

        // 10: top 10 object detected
        // 4: there coordinate in image
        float[][] scores = new float[1][50];
        // stores scores of 10 object
        float[][] classes = new float[1][50];
        // stores class of object

        // add it to object_map;
        output_map.put(0, boxes);
        output_map.put(1, classes);
        output_map.put(2, scores);

        // now predict
        interpreter.runForMultipleInputsOutputs(input, output_map);
        // Before watching this video please watch my previous 2 video of
        //      1. Loading tensorflow lite model
        //      2. Predicting object
        // In this video we will draw boxes and label it with it's name

        Object value = output_map.get(0);
        Object Object_class = output_map.get(1);
        Object score = output_map.get(2);

        // loop through each object
        // as output has only 10 boxes

        for (int i = 0; i < 50; i++) {
            float class_value = (float) Array.get(Array.get(Object_class, 0), i);
            float score_value = (float) Array.get(Array.get(score, 0), i);
            // define threshold for score

            // Here you can change threshold according to your model
            // Now we will do some change to improve app
            if (score_value > 0.3) {
                Object box1 = Array.get(Array.get(value, 0), i);
                // we are multiplying it with Original height and width of frame

                float top = (float) Array.get(box1, 0) * height;
                float left = (float) Array.get(box1, 1) * width;
                float bottom = (float) Array.get(box1, 2) * height;
                float right = (float) Array.get(box1, 3) * width;

                /////////////////////////////////////////

                // Use these points to extract image patch and run a simple blob detector and draw blobs
                int BoxHeight = (int) (bottom - top);
                int BoxWidth = (int) (right -left);

                // Use these points to extract image patch and run a simple blob detector and draw blobs
                Rect roi= new Rect((int) left, (int) top, BoxWidth, BoxHeight);
                if((0 <= roi.x && 0 <= roi.width && roi.x + roi.width <= mat_image.cols() && 0 <= roi.y && 0 <= roi.height && roi.y + roi.height <= mat_image.rows()))
                {
                    Mat roiMat = mat_image.submat(roi).clone();
                    final Mat processedroi = processImage(roiMat);

                    // Mark outer contour
                    // markOuterContour(processedroi, roiMat);
                    detect_blob(roiMat, roiMat);

                    // now copy the result to the original mat
                    Mat dst = mat_image.submat(roi); // do not clone this submat
                    roiMat.copyTo(dst);
                }
                /////////////////////////////////////////

                // draw rectangle in Original frame //  starting point    // ending point of box  // color of box       thickness
                Imgproc.rectangle(mat_image, new Point(left, top), new Point(right, bottom), new Scalar(0, 255, 0, 255 ), 2);
                // write text on frame

                // string of class name of object  // starting point                         // color of text           // size of text
                //Imgproc.putText(mat_image, labelList.get((int) class_value), new Point(left, top), 3, 1, new Scalar(255, 0, 0, 255), 1);
            }

        }

        return mat_image;
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer;
        // some model input should be quant=0  for some quant=1
        // for this quant=0
        // Change quant=1
        // As we are scaling image from 0-255 to 0-1
        int quant = 1;
        int size_images = INPUT_SIZE;
        if (quant == 0) {
            byteBuffer = ByteBuffer.allocateDirect(1 * size_images * size_images * 3);
        } else {
            byteBuffer = ByteBuffer.allocateDirect(4 * 1 * size_images * size_images * 3);
        }
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[size_images * size_images];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        int pixel = 0;

        // some error
        //now run
        for (int i = 0; i < size_images; ++i) {
            for (int j = 0; j < size_images; ++j) {
                final int val = intValues[pixel++];
                if (quant == 0) {
                    byteBuffer.put((byte) ((val >> 16) & 0xFF));
                    byteBuffer.put((byte) ((val >> 8) & 0xFF));
                    byteBuffer.put((byte) (val & 0xFF));
                } else {
                    // paste this
                    byteBuffer.putFloat((((val >> 16) & 0xFF)) / 255.0f);
                    byteBuffer.putFloat((((val >> 8) & 0xFF)) / 255.0f);
                    byteBuffer.putFloat((((val) & 0xFF)) / 255.0f);
                }
            }
        }
        return byteBuffer;
    }

    public static Mat processImage(final Mat mat) {
        final Mat processed = new Mat(mat.height(), mat.width(), mat.type());
        // Blur an image using a Gaussian filter
        Imgproc.GaussianBlur(mat, processed, new Size(5, 5), 1);

        // Switch from RGB to GRAY
        Imgproc.cvtColor(processed, processed, Imgproc.COLOR_RGB2GRAY);

        // Find edges in an image using the Canny algorithm
        Imgproc.Canny(processed, processed, 200, 25);

        // Dilate an image by using a specific structuring element
        // https://en.wikipedia.org/wiki/Dilation_(morphology)
        Imgproc.dilate(processed, processed, new Mat(), new Point(-1, -1), 1);

        return processed;
    }

    public static void markOuterContour(final Mat processedImage,
                                        final Mat originalImage) {
        // Find contours of an image
        final List<MatOfPoint> allContours = new ArrayList<>();
        Imgproc.findContours(
                processedImage,
                allContours,
                new Mat(processedImage.size(), processedImage.type()),
                Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_NONE
        );

        // Filter out noise and display contour area value
        final List<MatOfPoint> filteredContours = new ArrayList<>();
        double maxVal = 200;
        int maxValIdx = 0;

        for(int contourIdx = 0; contourIdx < allContours.size(); contourIdx++){
            double contourArea = Imgproc.contourArea(allContours.get(contourIdx));
            boolean isConvex = Imgproc.isContourConvex(allContours.get(contourIdx));
            if ((contourArea > maxVal))
            {
                filteredContours.add(allContours.get(contourIdx));
            }
        }

        // Mark contours
        Imgproc.drawContours(
                originalImage,
                filteredContours,
                -1, // Negative value indicates that we want to draw all of contours
                new Scalar(124, 252, 0), // Green color
                1
        );
    }

    final void detect_blob(Mat mat, Mat out_mat)  {

        Random random = new Random();


        SimpleBlobDetector blobDet = SimpleBlobDetector.create();
        File outputFile = null;
        try {
            outputFile = File.createTempFile("SimpleBlobDetector", ".YAML");
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            writeToFile(outputFile,
                    "%YAML:1.0" // java
                            // parameter
                            // backdoor
                            + "\nthresholdStep: " + thresholdStep + "\nminThreshold: " + thresholdMin
                            + "\nmaxThreshold: " + thresholdMax + "\nminRepeatability: " + repeatability
                            + "\nminDistBetweenBlobs: " + distBetweenBlobs + "\nfilterByColor: "
                            + (color ? 1 : 0) + "\nblobColor: " + colorValue + "\nfilterByArea: "
                            + (area ? 1 : 0) + "\nminArea: " + areaMin + "\nmaxArea: "
                            + (areaMax < 0. ? 3.4028234663852886E+038 : areaMax)
                            + "\nfilterByCircularity: " + (circularity ? 1 : 0) + "\nminCircularity: "
                            + circularityMin + "\nmaxCircularity: "
                            + (circularityMax < 0. ? 3.4028234663852886E+038 : circularityMax)
                            + "\nfilterByInertia: " + (inertia ? 1 : 0) + "\nminInertiaRatio: "
                            + inertiaRatioMin + "\nmaxInertiaRatio: "
                            + (inertiaRatioMax < 0. ? 3.4028234663852886E+038 : inertiaRatioMax)
                            + "\nfilterByConvexity: " + (convexity ? 1 : 0) + "\nminConvexity: "
                            + convexityMin + "\nmaxConvexity: "
                            + (convexityMax < 0. ? 3.4028234663852886E+038 : convexityMax) + "\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
        blobDet.read(outputFile.getAbsolutePath());
        outputFile.delete();

        MatOfKeyPoint points = new MatOfKeyPoint();
        blobDet.detect(mat, points);

        KeyPoint[] ArrpPoints = points.toArray();
        List<KeyPoint> ListPoints = points.toList();

        for (int i = 0; i < ListPoints.size(); i++) {

            int red = random.nextInt(256);
            int green = random.nextInt(256);;
            int blue = random.nextInt(256);


            System.out.println("This is 1 : \n");
            System.out.println(ListPoints.get(i));
            Imgproc.circle(out_mat, ListPoints.get(i).pt, (int) (ListPoints.get(i).size * 0.65), new Scalar(green, red, blue, 255), 2);

        }

        //Features2d.drawKeypoints(mat, points, out_mat);

        }

    private void writeToFile(File file, String data) throws Exception {
        FileOutputStream stream = new FileOutputStream(file);
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(stream);
        outputStreamWriter.write(data);
        outputStreamWriter.close();
        stream.close();
    }


}
