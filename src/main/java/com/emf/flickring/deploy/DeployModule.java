package com.emf.flickring.deploy;

import static com.emf.flickring.deploy.DeployModule.Constant.API_KEY;
import static com.emf.flickring.deploy.DeployModule.Constant.NOT_AVAILABLE;
import static com.emf.flickring.deploy.DeployModule.Constant.SECRET_KEY;

import java.io.File;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import com.emf.flickring.Engine;
import com.emf.flickring.manager.ApiKeysCommand;
import com.emf.flickring.manager.Chain;
import com.emf.flickring.manager.ExecutionChain;
import com.emf.flickring.manager.FlickrEngine;
import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.REST;
import com.google.common.base.Strings;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class DeployModule extends AbstractModule {

  private final String configFolder;

  public DeployModule(final String configFolder) {
    if (Strings.isNullOrEmpty(configFolder)) {
      this.configFolder = Constant.DEFAULT_CONF_FOLDER;
    } else {
      this.configFolder = configFolder;
    }
  }

  @Override
  protected void configure() {
    bind(Engine.class).to(FlickrEngine.class);
    bind(Chain.class).to(ExecutionChain.class);
    bind(ApiKeysCommand.class);
  }

  @Provides
  @Singleton
  public Configuration getConfig() throws ConfigurationException {
    final PropertiesConfiguration config = new PropertiesConfiguration(new File(configFolder, Constant.DEFAULT_CONF_FILE_NAME));
    config.setAutoSave(true);
    return config;
  }

  @Provides
  @Singleton
  public Flickr getFlickr(final Configuration config) {
    return new Flickr(config.getString(API_KEY, NOT_AVAILABLE), config.getString(SECRET_KEY, NOT_AVAILABLE), new REST());
  }

  public static final class Constant {
    private static final String DEFAULT_CONF_FOLDER = "/opt/flickring/resources";
    private static final String DEFAULT_CONF_FILE_NAME = "flickring.conf";   

    public static final String API_KEY = "flickr.apikey";
    public static final String SECRET_KEY = "flickr.secret";
    public static final String TOKEN = "flickr.u.token";
    public static final String SECRET = "flickr.u.secret";
    public static final String USER_ID = "flickr.u";
    public static final String BASE_PICS_DIR = "fs.base.pics.dir";
    public static final String NOT_AVAILABLE = "na";
    public static final String UPLOAD_THREAD_SIZE = "upload.thread.size";
    public static final String DIR_CONTROLLER_FILE_NAME = ".flickring";
  }

}
