package com.emf.flickring.manager;

import static com.emf.flickring.Command.Response.SUCCESS;
import static com.emf.flickring.deploy.DeployModule.Constant.BASE_PICS_DIR;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.configuration.Configuration;

import com.emf.flickring.Command;
import com.emf.flickring.deploy.DeployModule.Constant;
import com.flickr4java.flickr.Flickr;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

@Slf4j
public class SyncCommand implements Command {

  private final Configuration config;
  private final Flickr flickr;
  private final BlockingQueue<File> queue;
  private final ExecutorService executor;

  @Inject
  public SyncCommand(final Configuration config, final Flickr flickr) {
    this.config = config;
    this.flickr = flickr;
    final int threadSize = config.getInt(Constant.UPLOAD_THREAD_SIZE, 3);
    this.queue = new ArrayBlockingQueue<File>(threadSize);
    this.executor = Executors.newFixedThreadPool(threadSize + 1);
  }

  @Override
  public Response process(final Chain chain) {
    executor.execute(new AlbumController(queue));
    final List<File> subdirectories = listSubdirectories(new File(config.getString(BASE_PICS_DIR)), FileType.DIR);
    if (subdirectories == null || subdirectories.isEmpty()) {
      return SUCCESS;
    }
    for (final File subdirectory : subdirectories) {
      final List<File> photosInDir = listSubdirectories(subdirectory, FileType.FILE);
      if (photosInDir == null || photosInDir.isEmpty()) {
        continue;
      }

      for (final File photo : photosInDir) {
        try {
          if (!AlbumController.isUploaded(photo)) {
            executor.execute(new UploadHandler(flickr, photo, queue, config));
          }
        } catch (IOException e) {
          log.error("Could not create phot upload stream", e);
        }
      }
    }
    return SUCCESS;
  }

  @Override
  public void stop() {
//    queue.offer(AlbumController.EOQ);
    executor.shutdown();
  }

  private List<File> listSubdirectories(final File dir, final FileType filterBy) {
    if (!dir.isDirectory()) {
      throw new IllegalArgumentException("File must be a directory.");
    }
    final String picturesDir = config.getString(Constant.BASE_PICS_DIR);
    if (Strings.isNullOrEmpty(picturesDir)) {
      throw new IllegalArgumentException("Base pictures directory cannot be empty.");
    }
    FileFilter fileFilter = null;
    switch (filterBy) {
    case DIR:
      fileFilter = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
          return pathname.isDirectory();
        }
      };
      break;
    case FILE:
      fileFilter = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
          return !pathname.isDirectory()
                 && !pathname.getName().equals(Constant.DIR_CONTROLLER_FILE_NAME);
        }
      };
      break;
    case BOTH:
    default:
      return Lists.newArrayList(dir.listFiles());
    }
    return Lists.newArrayList(dir.listFiles(fileFilter));
  }

  private enum FileType {
    BOTH,
    DIR,
    FILE;
  }

}
