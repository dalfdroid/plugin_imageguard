package blind.al.fileguardplugin;

import android.location.Location;
import android.media.ExifInterface;
import android.os.IPluginStorageInterposer;
import android.os.RemoteException;
import android.util.Log;

import java.io.IOException;

public class PluginStorageInterposer extends IPluginStorageInterposer.Stub {

    Location mSecureLocation;

    // Defines secure region
    public double mSecureLatitude = 36.0;
    public double mSecureLongitude = -78.94;
    public float mSecureRadius = 10000; // meters

    public PluginStorageInterposer(){

        // Set secure location
        mSecureLocation = new Location("network");
        mSecureLocation.setLatitude(mSecureLatitude);
        mSecureLocation.setLongitude(mSecureLongitude);
    }

    @Override
    // To allow return null or given filepath
    // To deny return empty string
    // To modify return location/path to the modified file
    public String beforeFileOpen(String targetAppPkg, String filepath) throws RemoteException {

        /**
         * Allow access to all files that are not images.
         * Allow access to images captured outside the specified secure region
         * Block access if location information not available.
         * Note: Only .jpg images are supported
         */
        // Non-images are allowed by default
        if (!isImage(filepath)) {
            return null;
        }

        if (isInSecureRegion(filepath)) {
            if (PluginMain.DEBUG) {
                Log.d(PluginMain.TAG,"Deny access to image " + filepath);
            }
            return "";
        } else {
            if (PluginMain.DEBUG) {
                Log.d(PluginMain.TAG,"Allow access to image " + filepath);
            }
            return null;
        }

    }

    private boolean isInSecureRegion(String filepath) {
        try {
            final ExifInterface exifInterface = new ExifInterface(filepath);
            float[] latLong = new float[2];
            if (exifInterface.getLatLong(latLong)) {
                Location location = new Location("network");
                location.setLatitude(latLong[0]);
                location.setLongitude(latLong[1]);
                float distanceToSecurelocation = mSecureLocation.distanceTo(location);
                return (distanceToSecurelocation < mSecureRadius);
            } else {
                // Location information not available
                // Assume secure region and block access
                if (PluginMain.DEBUG) {
                    Log.d(PluginMain.TAG,"Location info not available for file " + filepath);
                }
                return true;
            }
        } catch (IOException e) {
            // Error in extracting exif data
            // Assume secure region and block access
            if (PluginMain.DEBUG) {
                Log.d(PluginMain.TAG,"Error in extracting exif data for file " + filepath);
            }
            return true;
        }
    }

    // FIXME: Better checking to identify image files
    private boolean isImage(String filepath) {
        return filepath.endsWith(".jpg");
    }

}
