package com.emf.flickring.manager;

import java.io.File;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lombok.extern.slf4j.Slf4j;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import com.emf.flickring.deploy.DeployModule.Constant;
import com.google.common.io.Files;

@Test
@Slf4j
public class AlbumControllerTest {

  private final AlbumController albumController;
  private final BlockingQueue<File> queue;
  private final ExecutorService executor;

  public AlbumControllerTest() {
    this.queue = new ArrayBlockingQueue<File>(2);
    this.albumController = new AlbumController(queue);
    this.executor = Executors.newSingleThreadExecutor();
  }
  
  @Test
  public void testFileControllerCreation() throws InterruptedException {
    this.executor.execute(albumController);

    final File tempDir = Files.createTempDir();
    log.info("Temp dir is {}", tempDir.getAbsolutePath());

    File pic1 = new File(tempDir,"A1.jpg");
    queue.add(pic1);
    queue.add(AlbumController.EOQ);

    final File controllerFile = new File(pic1.getParent(), Constant.DIR_CONTROLLER_FILE_NAME);
    int count = 0;
    while (!controllerFile.exists() && count++ < 3) {
      Thread.sleep(2000);
      log.info("Counting {}...", count);
    }
    Assert.assertTrue(controllerFile.exists());
  }

  @AfterMethod
  public void tearDown() {
    executor.shutdown();
  }

}
