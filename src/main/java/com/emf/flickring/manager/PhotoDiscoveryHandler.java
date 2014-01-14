package com.emf.flickring.manager;

import static com.emf.flickring.deploy.DeployModule.Constant.BASE_PICS_DIR;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.configuration.Configuration;

import com.emf.flickring.Command.ShutdownListener;
import com.emf.flickring.deploy.DeployModule.Constant;
import com.flickr4java.flickr.Flickr;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

@Slf4j
public class PhotoDiscoveryHandler implements Runnable {

  private final Configuration config;
  private final ExecutorService executor;
  private final ShutdownListener listener;
  private final PhotoHandler photoHandler;
  private final DiscoveryHandler discoveryHandler;
  private final File baseDir;

  @VisibleForTesting
  public PhotoDiscoveryHandler(final Type photoHandler, final Type discoveryHandler, final File baseDir, final Flickr flickr) throws IOException {
    this.baseDir = baseDir;
    if (Void.TYPE.equals(photoHandler)) {
      this.photoHandler = new PhotoHandler(flickr, 2);
    } else {
      this.photoHandler = (PhotoHandler) photoHandler;
    }
    if (Void.TYPE.equals(discoveryHandler)) {
      this.discoveryHandler = new DiscoveryHandler();
    } else {
      this.discoveryHandler = (DiscoveryHandler) discoveryHandler;
    }
    this.config = null;
    this.executor = null;
    this.listener = null;
  }

  @Inject
  public PhotoDiscoveryHandler(final Configuration config, final Flickr flickr) throws IOException {
    final int threadSize = config.getInt(Constant.UPLOAD_THREAD_SIZE, 3);
    this.config = config;
    this.baseDir = new File(config.getString(BASE_PICS_DIR));
    this.discoveryHandler = new DiscoveryHandler();
    this.photoHandler = new PhotoHandler(flickr, threadSize);
    this.executor = Executors.newFixedThreadPool(threadSize + 1);
    this.listener = new ShutdownListener() {
      @Override
      public boolean shutdown() {
        try {
          photoHandler.queue.offer(AlbumConfigHandler.EOQ);
          discoveryHandler.watcher.close();
          executor.shutdown();
        } catch (IOException e) {
          log.error("Error closing watcher", e);
        }
        return true;
      }
    };
  }

  @Override
  public void run() {
    photoHandler.upload();
    discoveryHandler.discover();
  }

  public ShutdownListener shutdownListener() {
    return listener;
  }

  private List<File> list(final File file, final FileType filterBy) {
    Preconditions.checkNotNull(file);
    if (!file.isDirectory()) {
      throw new IllegalArgumentException("File must be a directory.");
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
      return Lists.newArrayList(file.listFiles());
    }
    
    return Lists.newArrayList(file.listFiles(fileFilter));
  }

  @SuppressWarnings("unchecked")
  private <T> WatchEvent<T> cast(WatchEvent<?> event) {
    return (WatchEvent<T>)event;
  }

  public class PhotoHandler implements Type {

    private final Flickr flickr;
    private final BlockingQueue<File> queue;

    @Inject
    public PhotoHandler(final Flickr flickr, final int queueCapacitiy) {
      this.flickr = flickr;
      this.queue = new ArrayBlockingQueue<File>(queueCapacitiy);
    }

    public void upload() {
      runAlbumConfigHandler();
      final List<File> subdirectories = list(baseDir, FileType.DIR);
      if (subdirectories == null || subdirectories.isEmpty()) {
        return;
      }
      for (final File subdirectory : subdirectories) {
        final List<File> photosInDir = list(subdirectory, FileType.FILE);
        if (photosInDir == null || photosInDir.isEmpty()) {
          continue;
        }

        for (final File photo : photosInDir) {
          runUploadHandler(photo);
        }
      }
    }

    public void runAlbumConfigHandler() {
      executor.execute(new AlbumConfigHandler(queue));
    }

    public boolean runUploadHandler(final File photo) {
      try {
        if (!AlbumConfigHandler.isUploaded(photo)) {
          executor.execute(new UploadHandler(flickr, photo, queue, config));
        }
        return true;
      } catch (IOException e) {
        log.error("Could not create phot upload stream", e);
      }
      return false;
    }

  }

  public class DiscoveryHandler implements Type {

    private final WatchService watcher;
    private final Map<WatchKey,Path> keys;

    public DiscoveryHandler() throws IOException {
      this.watcher = FileSystems.getDefault().newWatchService();
      this.keys = Maps.newHashMap();
      register(baseDir);
    }

    /** Registers a directory and sub-directories to watch changes.
     * Call this before starting handler. */
    private void register(final File baseDir) throws IOException {
      Preconditions.checkNotNull(baseDir);
      Path start = Paths.get(baseDir.getAbsolutePath());
      // register directory and sub-directories
      Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
          keys.put(dir.register(watcher, ENTRY_CREATE, ENTRY_MODIFY), dir);
          return FileVisitResult.CONTINUE;
        }
      });
    }

    public void discover() {
      WatchKey key = null;
      try {
        while((key = watcher.take()) != null) {
          final Path dir = keys.get(key);
          if (dir == null) {
            log.warn("WatchKey not recognized!!");
            continue;
          }
          for (WatchEvent<?> eventObj : key.pollEvents()) {
            final Kind<?> kind = eventObj.kind();

            if (kind == OVERFLOW) {
              continue;
            }

            final WatchEvent<Path> event = cast(eventObj);
            final Path name = event.context();
            final Path child = dir.resolve(name);

            log.info("{}: {}", event.kind().name(), child);

            if ((kind == ENTRY_CREATE)) {
              try {
                if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                  register(child.toFile());
                } else if (Files.isRegularFile(child, NOFOLLOW_LINKS)) {
                  photoHandler.runUploadHandler(child.toFile());
                }
              } catch (IOException x) {
                log.error("Failure registering directory: {}", child.toString());
              }
            }
          }
          // reset key and remove from set if directory no longer accessible
          final boolean valid = key.reset();
          if (!valid) {
            log.info("Key is removed.");
            keys.remove(key);
            // all directories are inaccessible
            if (keys.isEmpty()) {
              break;
            }
          }
        }
      } catch (ClosedWatchServiceException ex) {
        log.warn("Close signal received. Discovery handler is going down.");
      } catch (InterruptedException e) {
        log.error("Watcher directory was interrupted.", e);
      }
    }
  }

  private enum FileType {
    BOTH,
    DIR,
    FILE;
  }

}
