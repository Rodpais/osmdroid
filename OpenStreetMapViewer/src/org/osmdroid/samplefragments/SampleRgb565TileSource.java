package org.osmdroid.samplefragments;

import org.osmdroid.ResourceProxy;
import org.osmdroid.ResourceProxyImpl;
import org.osmdroid.tileprovider.BitmapPool;
import org.osmdroid.tileprovider.ExpirableBitmapDrawable;
import org.osmdroid.tileprovider.MapTileProviderBasic;
import org.osmdroid.tileprovider.ReusableBitmapDrawable;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.tileprovider.util.CloudmadeUtil;
import org.osmdroid.views.MapView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class SampleRgb565TileSource extends BaseSampleFragment {

	public static final String TITLE = "RGB565 Tile Source";

	@Override
	public String getSampleTitle() {
		return TITLE;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Context context = inflater.getContext();
		mResourceProxy = new ResourceProxyImpl(context.getApplicationContext());

		final ITileSource tileSource = new XYTileSourceRGB565("Mapnik-RGB565",
			null, 0, 18, 256, ".png", "http://tile.openstreetmap.org/");
		final MapTileProviderBasic tileProvider = new MapTileProviderBasic(context, tileSource);
		mMapView = new MapView(context, 256, mResourceProxy, tileProvider);
		mMapView.setUseSafeCanvas(true);
		return mMapView;
	}

	/**
	 * XYTileSource with RGB565 bitmap config and auto-retry after out-of-memory error.
	 */
	public static class XYTileSourceRGB565 extends XYTileSource {
		private static final Logger logger = LoggerFactory.getLogger(XYTileSourceRGB565.class);

		public XYTileSourceRGB565(String aName, ResourceProxy.string aResourceId, int aZoomMinLevel,
								  int aZoomMaxLevel, int aTileSizePixels,
								  String aImageFilenameEnding, String... aBaseUrl) {
			super(aName, aResourceId, aZoomMinLevel, aZoomMaxLevel, aTileSizePixels,
				aImageFilenameEnding, aBaseUrl);
		}

		@Override
		public Drawable getDrawable(final String aFilePath) {
			Bitmap bitmap;
			BitmapFactory.Options bitmapOptions;
			try {
				// default implementation will load the file as a bitmap and create
				// a BitmapDrawable from it
				bitmapOptions = getBitmapOptions();
				BitmapPool.getInstance().applyReusableOptions(bitmapOptions);

				try {
					bitmap = BitmapFactory.decodeFile(aFilePath, bitmapOptions);
				} catch (IllegalArgumentException cannotReuseBitmapException) {
					logger.debug("Cannot reuse bitmap", cannotReuseBitmapException);
					bitmapOptions = getBitmapOptions();
					bitmap = BitmapFactory.decodeFile(aFilePath, bitmapOptions);
				}
				if (bitmap != null) {
					return new ReusableBitmapDrawable(bitmap);
				} else {
					// if we couldn't load it then it's invalid - delete it
					try {
						new File(aFilePath).delete();
					} catch (final Throwable e) {
						logger.error("Error deleting invalid file: " + aFilePath, e);
					}
				}
			} catch (final OutOfMemoryError e) {
				logger.error("OutOfMemoryError loading bitmap: " + aFilePath);
				System.gc();
			}
			return null;
		}

		@Override
		public Drawable getDrawable(final InputStream aFileInputStream) throws LowMemoryException {
			Bitmap bitmap;
			BitmapFactory.Options bitmapOptions;
			try {
				// default implementation will load the file as a bitmap and create
				// a BitmapDrawable from it
				bitmapOptions = getBitmapOptions();
				BitmapPool.getInstance().applyReusableOptions(bitmapOptions);
				try {
					bitmap = BitmapFactory.decodeStream(aFileInputStream, null, bitmapOptions);
				} catch (IllegalArgumentException cannotReuseBitmapException) {
					logger.debug("Cannot reuse bitmap", cannotReuseBitmapException);
					try {
						aFileInputStream.reset();
					} catch (IOException ioe) {
						logger.info("aFileInputStream.reset() error", ioe);
					}
					bitmapOptions = new BitmapFactory.Options();
					bitmap = BitmapFactory.decodeStream(aFileInputStream, null, bitmapOptions);
				}
				if (bitmap != null) {
					return new ReusableBitmapDrawable(bitmap);
				}
			} catch (final OutOfMemoryError e) {
				logger.error("OutOfMemoryError loading bitmap");
				System.gc();
				throw new LowMemoryException(e);
			}
			return null;
		}

		private BitmapFactory.Options getBitmapOptions() {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inPreferredConfig = Bitmap.Config.RGB_565;
			return options;
		}
	}
}
