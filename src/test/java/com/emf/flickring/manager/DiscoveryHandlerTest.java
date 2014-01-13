package com.emf.flickring.manager;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.emf.flickring.manager.PhotoDiscoveryHandler.PhotoHandler;
import com.google.common.io.Files;
@Test
public class DiscoveryHandlerTest {

  private final PhotoDiscoveryHandler handler;
  private final File baseDir;
  private final File photo;
  private final BlockingQueue<File> checkingQueue;
  private final ExecutorService executor;

  public DiscoveryHandlerTest() throws IOException {
    this.checkingQueue = new ArrayBlockingQueue<File>(1);
    final PhotoHandler photoHandler = mock(PhotoHandler.class);
    this.baseDir = Files.createTempDir();
    this.photo = new File(baseDir, "A1.txt");
    when(photoHandler.runUploadHandler(photo)).then(new Answer<Boolean>() {
      @Override
      public Boolean answer(InvocationOnMock invocation) throws Throwable {
        if (invocation.getArguments().length != 1) {
          throw new IllegalStateException("Invalid arguments");
        }
        final File value = (File) invocation.getArguments()[0];
        checkingQueue.offer(value);
        return true;
      }
    });
    this.handler = new PhotoDiscoveryHandler(photoHandler, Void.TYPE, baseDir, null);
    this.executor = Executors.newSingleThreadExecutor();
  }

  @Test
  public void testDirWatcher() throws InterruptedException, IOException {
    executor.execute(handler);
    Thread.sleep(2000);
    Files.touch(photo);
    final File posted = checkingQueue.take();
    Assert.assertTrue(Files.equal(photo, posted));
  }
}
