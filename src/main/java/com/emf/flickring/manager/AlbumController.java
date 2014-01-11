package com.emf.flickring.manager;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import lombok.extern.slf4j.Slf4j;

import com.emf.flickring.deploy.DeployModule.Constant;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.io.Files;

@Slf4j
public class AlbumController implements Runnable {

  /** End of queue. Means that no other file will be processed by the queue controller. */
  public static final File EOQ = new File("file://end-of-queue");

  private final BlockingQueue<File> queue;
 
  public AlbumController(final BlockingQueue<File> queue) {
    Preconditions.checkNotNull(queue);
    this.queue = queue;
  }

  @Override
  public void run() {
    log.info("Album controller is started.");
    try {
      File photo = null;
      while (notEndOfQueue(photo = queue.take())) {
        log.info("{} was taken", photo.getAbsolutePath());
        final String photoName = photo.getName();
        final File parentDir = photo.getParentFile();
        final File controllerFile = new File(parentDir, Constant.DIR_CONTROLLER_FILE_NAME);
        try {
          if (!controllerFile.exists()) {
              Files.touch(controllerFile);
              log.info("Controller file was created {}", controllerFile.getAbsolutePath());
          }
          Files.append(photoName + System.getProperty("line.separator"), controllerFile, Charsets.UTF_8);
          log.info("File name appended to the controller file.");
        } catch (IOException e) {
          log.error("Could not update controller file " + controllerFile.getAbsolutePath(), e);
        }
      }
    } catch (InterruptedException ex) {
      log.error("Queue processing was interrupted.", ex);
    }
    log.info("Album controller will exit.");
  }

  private boolean notEndOfQueue(final File file) {
    final String name1 = file.getAbsolutePath();
    final String name2 = EOQ.getAbsolutePath();
    return !name1.equals(name2);
  }

  /**
   * Checks the current controller file for the photo name. If found indicates photo was uploaded.
   * @return TRUE if photo was uploaded. FALSE otherwise.
   */
  public static boolean isUploaded(final File photo) {
    final File controllerFile = new File(photo.getParentFile(), Constant.DIR_CONTROLLER_FILE_NAME);
    if (!controllerFile.exists()) {
      return false;
    }
    try {
      final List<String> lines = Files.readLines(controllerFile, Charsets.UTF_8);
      for (String line : lines) {
        if (!Strings.isNullOrEmpty(line) && line.equals(photo.getName())) {
          return true;
        }
      }
      return false;
    } catch (IOException e) {
      log.error("Could not read controller file", e);
    }
    return true; // In the case of not being  able to determine if file is uploaded, return TRUEto prevent duplicity.
  }
}
