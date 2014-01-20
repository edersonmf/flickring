package com.emf.flickring.manager;

import static com.emf.flickring.Command.Response.END;
import static com.emf.flickring.deploy.DeployModule.Constant.API_KEY;
import static com.emf.flickring.deploy.DeployModule.Constant.BASE_PICS_DIR;
import static com.emf.flickring.deploy.DeployModule.Constant.SECRET;
import static com.emf.flickring.deploy.DeployModule.Constant.SECRET_KEY;
import static com.emf.flickring.deploy.DeployModule.Constant.TOKEN;
import static com.emf.flickring.deploy.DeployModule.Constant.USER_ID;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.FileConfiguration;
import org.scribe.exceptions.OAuthException;
import org.scribe.model.Token;
import org.scribe.model.Verifier;

import com.emf.flickring.Command;
import com.emf.flickring.deploy.DeployModule;
import com.emf.flickring.model.ConfigInput;
import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.RequestContext;
import com.flickr4java.flickr.auth.Auth;
import com.flickr4java.flickr.auth.AuthInterface;
import com.flickr4java.flickr.auth.Permission;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.io.Files;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

@Slf4j
public class ApiKeysCommand implements Command {

  private final FileConfiguration config;
  private final Flickr flickr;

  @Inject
  public ApiKeysCommand(final Configuration config, final Flickr flickr) {
    this.config = (FileConfiguration) config;
    this.flickr = flickr;
  }

  @Override
  public Response process(final Chain chain) {
    String apikey = config.getString(API_KEY, null);
    String secret = config.getString(SECRET_KEY, null);
    String tokenValue = config.getString(TOKEN, null);
    String basePicsDir = config.getString(BASE_PICS_DIR, null);
    String userId = config.getString(USER_ID, null);

    final File configFile = config.getFile();
    if (!configFile.exists()) {
      try {
        Files.touch(configFile);
      } catch (IOException e) {
        log.warn("Could not create config file. {}", e.getMessage());
        return END;
      }
    }

    final Scanner scanner = new Scanner(System.in);
    try {
      // Check api key
      if (Strings.isNullOrEmpty(apikey)) {
        final ConfigInput apiKeyInput = ConfigInput.builder().label("Enter your Flickr api key:").scanner(scanner).build();
        apiKeyInput.read();
        apikey = apiKeyInput.getInputedValue();
        if (Strings.isNullOrEmpty(apikey)) {
          log.warn("Api key is empty...");
          return END;
        } else {
          config.addProperty(API_KEY, apikey);
        }
      }
  
      // Check secret key
      if (Strings.isNullOrEmpty(secret)) {
        final ConfigInput secretKeyInput = ConfigInput.builder().label("Enter your Flickr secret key:").scanner(scanner).build();
        secretKeyInput.read();
        secret = secretKeyInput.getInputedValue();
        if (Strings.isNullOrEmpty(secret)) {
          log.warn("Secret key is empty...");
          return END;
        } else {
          config.addProperty(SECRET_KEY, secret);
        }
      }

      // Check token
      final RequestContext requestContext = RequestContext.getRequestContext();
      Flickr.debugStream = false;
      flickr.setApiKey(config.getString(API_KEY));
      flickr.setSharedSecret(config.getString(SECRET_KEY));
      final AuthInterface authInterface = flickr.getAuthInterface();
      if (Strings.isNullOrEmpty(tokenValue)) {

        final Token token = authInterface.getRequestToken();
        final String url = authInterface.getAuthorizationUrl(token, Permission.WRITE);

        final StringBuilder label = new StringBuilder("Follow this URL to authorise yourself on Flickr");
        label.append("\n")
             .append(url).append("\n")
             .append("Paste in the token it gives you:").append("\n");
        final ConfigInput tokenInput = ConfigInput.builder().label(label.toString()).scanner(scanner).build();
        tokenInput.read();
        tokenValue = tokenInput.getInputedValue();

        final Token requestToken = authInterface.getAccessToken(token, new Verifier(tokenValue));

        // Check userId
        if (Strings.isNullOrEmpty(userId)) {
          try {
            final Auth auth = authInterface.checkToken(requestToken);
            userId = auth.getUser().getId();
            if (Strings.isNullOrEmpty(userId)) {
              log.error("Could not get user id.");
              return END;
            }
            config.addProperty(USER_ID, userId);
            requestContext.setAuth(auth);
            log.info("User is authenticated.");
          } catch (FlickrException e) {
            log.error("Could not get user id", e);
            return END;
          }
        }

        if (requestToken == null || requestToken.isEmpty()) {
          log.warn("User token is empty...");
          return END;
        } else {
          config.addProperty(TOKEN, requestToken.getToken());
          config.addProperty(SECRET, requestToken.getSecret());
          log.info("Authentication success");
        }
      } else {
        Auth auth;
        try {
          auth = authInterface.checkToken(config.getString(TOKEN), config.getString(SECRET));
          requestContext.setAuth(auth);
          log.info("User is autheticated.");
        } catch (FlickrException e) {
          log.error("Could not authenticate user", e);
        }
      }

      // Check base pictures dir
      if (Strings.isNullOrEmpty(basePicsDir)) {
        final ConfigInput basePicsDirInput = ConfigInput.builder().label("Enter your pictures folder location:").scanner(scanner).build();
        basePicsDirInput.read();
        basePicsDir = basePicsDirInput.getInputedValue();
        if (Strings.isNullOrEmpty(basePicsDir)) {
          log.warn("Base pictures location is empty...");
          return END;
        } else {
          config.addProperty(BASE_PICS_DIR, basePicsDir);
        }
      }
    } catch (OAuthException ex) {
      log.error("Could not authenticate user.", ex);
    } finally {
      scanner.close();
    }

    return chain.execute();
  }

  @Override
  public void stop() {
    // Does nothing
  }

  public static void main(final String[] args) {
    Preconditions.checkNotNull(args);
    Preconditions.checkPositionIndex(0, 1);
    final Injector injector = Guice.createInjector(new DeployModule(args[0]));
    final ApiKeysCommand command = injector.getInstance(ApiKeysCommand.class);
    command.process(new Chain() {
      
      @SuppressWarnings("unchecked")
      @Override
      public <T> T execute() {
        return (T) Response.SUCCESS;
      }

      @Override
      public void breakIt() {
      }
    });
  }

}
