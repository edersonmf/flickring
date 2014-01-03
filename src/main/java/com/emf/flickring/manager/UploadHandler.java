package com.emf.flickring.manager;

import static com.emf.flickring.deploy.DeployModule.Constant.SECRET;
import static com.emf.flickring.deploy.DeployModule.Constant.TOKEN;
import static com.emf.flickring.deploy.DeployModule.Constant.USER_ID;

import java.io.File;
import java.util.concurrent.BlockingQueue;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.configuration.Configuration;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.RequestContext;
import com.flickr4java.flickr.photosets.Photoset;
import com.flickr4java.flickr.photosets.Photosets;
import com.flickr4java.flickr.photosets.PhotosetsInterface;
import com.flickr4java.flickr.uploader.UploadMetaData;
import com.flickr4java.flickr.uploader.Uploader;

@Slf4j
public class UploadHandler implements Runnable {

  private final BlockingQueue<File> queue;
  private final Flickr flickr;
  private final Configuration config;

  public UploadHandler(final Flickr flickr, final BlockingQueue<File> queue, final Configuration config) {
    this.flickr = flickr;
    this.queue = queue;
    this.config = config;
  }

  @Override
  public void run() {
    try {
      File photo = queue.take();
      try {
        RequestContext.getRequestContext().setAuth(flickr.getAuthInterface().checkToken(config.getString(TOKEN), config.getString(SECRET)));
        final File parent = photo.getParentFile();

        // Start uploading
        final Uploader uploader = flickr.getUploader();
        log.info("*** Uploading photo {}", photo.getName());
        final String photoId = uploader.upload(photo, new UploadMetaData().setPublicFlag(false));
        
        // Check album exists
        final PhotosetsInterface photosetsInterface = flickr.getPhotosetsInterface();
        String photosetId = null;
        final Photosets photosets = photosetsInterface.getList(config.getString(USER_ID));
        for (final Photoset photoset : photosets.getPhotosets()) {
          if (photoset.getTitle().equalsIgnoreCase(parent.getName())) {
            photosetId = photoset.getId();
            break;
          }
        }
        //
        
        if (photosetId == null) {
          final Photoset photoset = photosetsInterface.create(parent.getName(), null, photoId);
          photosetId = photoset.getId();
          log.info("*** Album '{}' created", parent.getName());
        } else {
          photosetsInterface.addPhoto(photosetId, photoId);
        }
        log.info("*** Photo '{}' added to {}", photoId, photosetId);

      } catch (FlickrException ex) {
        log.error("Unable to communicate to Flickr.", ex);
      }
    } catch (InterruptedException e) {
      log.error("Upload thread was interrupted.", e);
    }
  }

}
